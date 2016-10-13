package ru.v0rt3x.vindicator.modules.web.handler;

import org.apache.velocity.VelocityContext;
import org.json.simple.JSONObject;
import ru.v0rt3x.vindicator.common.GenericMethod;
import ru.v0rt3x.vindicator.modules.web.WebUIPageHandler;

import java.util.ArrayList;
import java.util.List;

public class Terminal implements WebUIPageHandler {
    @Override
    public void prepareContext(VelocityContext ctx) {

    }

    @SuppressWarnings("unchecked")
    @GenericMethod("execute")
    public void execute(JSONObject request, JSONObject response) {
        String command = (String) request.getOrDefault("cmd", "");
        List<String> args = (List<String>) request.getOrDefault("args", new ArrayList<String>());
        String result;

        switch (command) {
            default:
                result = String.format("Command '%s' not found", command);
                break;
        }

        response.put("result", result);
    }
}
