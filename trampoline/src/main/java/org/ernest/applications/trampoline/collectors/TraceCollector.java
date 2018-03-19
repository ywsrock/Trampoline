package org.ernest.applications.trampoline.collectors;

import org.ernest.applications.trampoline.entities.Instance;
import org.ernest.applications.trampoline.entities.TraceActuator;
import org.ernest.applications.trampoline.exceptions.CreatingSettingsFolderException;
import org.ernest.applications.trampoline.exceptions.ReadingEcosystemException;
import org.ernest.applications.trampoline.services.EcosystemManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Component
public class TraceCollector {

    @Autowired
    EcosystemManager ecosystemManager;

    public List<TraceActuator> getTraces(String idInstance) throws CreatingSettingsFolderException, ReadingEcosystemException, JSONException {
        List<TraceActuator> traces = new ArrayList<>();

        Instance instance = ecosystemManager.getEcosystem().getInstances().stream().filter(i -> i.getId().equals(idInstance)).findAny().get();
        JSONArray traceArrayJson;

        try {
            traceArrayJson = new JSONArray(new RestTemplate().getForObject("http://127.0.0.1:" + instance.getPort() + instance.getActuatorPrefix() + "/trace", String.class));
            buildTracesV1x(traces, traceArrayJson);
        }catch (Exception e){
            traceArrayJson = new JSONObject(new RestTemplate().getForObject("http://127.0.0.1:" + instance.getPort() + instance.getActuatorPrefix() + "/httptrace", String.class)).getJSONArray("traces");
            buildTracesV2x(traces, traceArrayJson);
        }

        return traces;
    }

    private void buildTracesV2x(List<TraceActuator> traces, JSONArray traceArrayJson) throws JSONException {
        for (int i = 0; i < traceArrayJson.length(); i++) {
            JSONObject traceJson = traceArrayJson.getJSONObject(i);

            org.ernest.applications.trampoline.entities.TraceActuator traceActuator = new org.ernest.applications.trampoline.entities.TraceActuator();
            traceActuator.setDate(traceJson.getString("timestamp"));
            traceActuator.setMethod(traceJson.getJSONObject("request").getString("method"));
            traceActuator.setPath(traceJson.getJSONObject("request").getString("uri"));
            traceActuator.setStatus(traceJson.getJSONObject("response").getString("status"));
            traces.add(traceActuator);
        }

    }

    private void buildTracesV1x(List<TraceActuator> traces, JSONArray traceArrayJson) throws JSONException {
        for (int i = 0; i < traceArrayJson.length(); i++) {
            JSONObject traceJson = traceArrayJson.getJSONObject(i);

            TraceActuator traceActuator = new TraceActuator();
            traceActuator.setDate(new SimpleDateFormat("HH:mm:ss").format(traceJson.getLong("timestamp")));
            traceActuator.setMethod(traceJson.getJSONObject("info").getString("method"));
            traceActuator.setPath(traceJson.getJSONObject("info").getString("path"));
            traceActuator.setStatus(traceJson.getJSONObject("info").getJSONObject("headers").getJSONObject("response").getString("status"));
            traces.add(traceActuator);
        }
    }
}
