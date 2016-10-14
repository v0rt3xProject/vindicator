package ru.v0rt3x.vindicator.modules.agent;

import com.mongodb.client.MongoCollection;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.SystemPropertyUtil;
import org.bson.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import ru.v0rt3x.vindicator.VindicatorCore;
import ru.v0rt3x.vindicator.common.AbstractServer;
import ru.v0rt3x.vindicator.common.GenericDispatcher;
import ru.v0rt3x.vindicator.common.GenericMethod;
import ru.v0rt3x.vindicator.common.Queue;
import ru.v0rt3x.vindicator.component.Component;
import ru.v0rt3x.vindicator.modules.web.WebUIPages;
import ru.v0rt3x.vindicator.modules.web.handler.Agent;
import ru.v0rt3x.vindicator.modules.web.handler.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AgentController implements Component {

    private AbstractServer agentServer;

    private MongoCollection<Document> agents;
    private MongoCollection<Document> exploits;
    private MongoCollection<Document> teams;
    private MongoCollection<Document> config;
    private MongoCollection<Document> services;
    private MongoCollection<Document> traffic;

    private Queue<String> flagQueue;

    @Override
    @SuppressWarnings("unchecked")
    public void onInit() {
        String agentServerHost = core.config().getString("vindicator.agent.host", "0.0.0.0");
        Integer agentServerPort = core.config().getInt("vindicator.agent.port", 65431);

        agentServer = new AbstractServer(agentServerHost, agentServerPort);
        agentServer.setChannelHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                pipeline.addLast("frameDecoder", new LineBasedFrameDecoder(65536));
                pipeline.addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));
                pipeline.addLast("stringEncoder", new StringEncoder(CharsetUtil.UTF_8));
                pipeline.addLast(new AgentHandler());
            }
        });

        agents = VindicatorCore.getInstance().getDataBase().getCollection("agents");
        exploits = VindicatorCore.getInstance().getDataBase().getCollection("exploits");
        teams = VindicatorCore.getInstance().getDataBase().getCollection("teams");
        config = VindicatorCore.getInstance().getDataBase().getCollection("config");
        services = VindicatorCore.getInstance().getDataBase().getCollection("service");
        traffic = VindicatorCore.getInstance().getDataBase().getCollection("traffic");

        VindicatorCore.getInstance().createQueue(String.class, "flags");
        flagQueue = (Queue<String>) VindicatorCore.getInstance().getQueue("flags");

        core.executeThread(agentServer);
    }

    @Override
    public void onShutDown() {
        agentServer.stop();
    }

    @SuppressWarnings("unchecked")
    public JSONObject handle(JSONObject request) {
        String agentId = (String) request.getOrDefault("agentId", null);

        if (agentId != null) {
            agents.updateOne(
                new Document("agentId", agentId),
                new Document("$set", new Document("lastSeen", System.currentTimeMillis()))
            );
        }

        JSONObject response = new JSONObject();
        GenericDispatcher.dispatch(this, request, response);
        return response;
    }

    @SuppressWarnings("unchecked")
    @GenericMethod("register")
    public void register(JSONObject request, JSONObject response) {
        String agentId = (String) request.getOrDefault("agentId", null);
        Document agentInfo = new Document((JSONObject) request.get("agentInfo"));

        if (agentId == null) {
            agentId = UUID.randomUUID().toString();

            agents.insertOne(
                new Document("agentId", agentId)
                    .append("lastSeen", System.currentTimeMillis())
                    .append("nextTask", System.currentTimeMillis())
                    .append("agentInfo", agentInfo)
                    .append("agentType", request.get("agentType"))
                    .append("taskInfo", new Document("name", null).append("state", "Idle"))
            );
        } else {
            Document agentData = agents.find(new Document("agentId", agentId)).first();
            agents.updateOne(agentData, new Document(
                "$set", new Document("agentInfo", agentInfo)
            ));
        }

        response.put("success", true);
        response.put("id", agentId);
    }

    @SuppressWarnings("unchecked")
    @GenericMethod("update")
    public void update(JSONObject request, JSONObject response) {
        String agentId = (String) request.getOrDefault("agentId", null);
        if (agentId != null) {
            Document agentData = agents.find(new Document("agentId", agentId)).first();
            String agentType = agentData.getString("agentType");

            JSONObject task = (JSONObject) request.get("task");

            if (task != null) {
                updateTask(agentId, task);
            }

            switch (agentType) {
                case "executor":
                    if (task == null) {
                        updateExploits(agentId, request);
                        prepareExploitTask(agentId, response);
                    }
                    break;
                case "monitor":
                    prepareMonitorTask(agentId, response);
                    break;
                default:
                    response.put("success", false);
                    response.put("message", String.format("agent type '%s' is not supported", agentType));
                    break;
            }
        } else {
            response.put("success", false);
            response.put("message", "agent not registered");
        }
    }

    @SuppressWarnings("unchecked")
    @GenericMethod("monitor")
    public void monitor(JSONObject request, JSONObject response) {
        if (request.get("data") != null) {
            traffic.insertOne(
                new Document("service", request.get("service"))
                    .append("clientHost", ((JSONObject) request.get("client")).get("host"))
                    .append("clientPort", ((JSONObject) request.get("client")).get("port"))
                    .append("direction", request.get("direction"))
                    .append("time", request.get("time"))
                    .append("data", request.get("data"))
                    .append("sequenceId", request.get("sequenceId"))
            );
        }
    }

    @SuppressWarnings("unchecked")
    private void updateTask(String agentId, JSONObject task) {
        String taskState = (String) task.get("state");

        Document agentData = agents.find(new Document("agentId", agentId)).first();
        String agentType = agentData.getString("agentType");

        if (taskState.equals("Complete")) {
            switch (agentType) {
                case "executor":
                    JSONObject result = (JSONObject) task.get("result");
                    List<String> flags = (List<String>) result.get("flags");

                    flags.forEach(flag -> flagQueue.push(flag + ":1"));
                    break;
            }

            agents.updateOne(
                new Document("agentId", agentId),
                new Document("$set", new Document("taskInfo", new Document("name", null).append("state", "Idle")))
            );
        } else {
            agents.updateOne(
                new Document("agentId", agentId),
                new Document("$set", new Document("taskInfo", new Document(task)))
            );
        }
    }

    @SuppressWarnings("unchecked")
    public void updateExploits(String agentId, JSONObject request) {
        List<JSONObject> exploitList = (JSONArray) request.get("exploits");
        if (exploitList != null) {
            for (JSONObject exploitObject : exploitList) {
                String exploitName = (String) exploitObject.get("name");
                String exploitCommand = (String) exploitObject.get("command");

                if (exploits.count(new Document("agentId", agentId).append("name", exploitName)) == 0) {
                    exploits.insertOne(
                        new Document("agentId", agentId)
                            .append("name", exploitName)
                            .append("command", exploitCommand)
                            .append("lastActivity", System.currentTimeMillis())
                    );
                } else {
                    exploits.updateOne(
                        new Document("name", exploitName).append("agentId", agentId),
                        new Document(
                            "$set",
                            new Document("command", exploitCommand)
                                .append("lastActivity", System.currentTimeMillis())
                        )
                    );
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void prepareExploitTask(String agentId, JSONObject response) {
        String ownTeamId = null;
        Integer interval = 60;

        Document xploitConfig = config.find(new Document("configType", "xploit")).first();
        if (xploitConfig != null) {
            Document config = (Document) xploitConfig.get("xploit");
            ownTeamId = config.getString("team");
            interval = Integer.parseInt(config.getString("interval"));
        }

        JSONObject task = null;
        Document agentData = agents.find(new Document("agentId", agentId)).first();
        if (agentData.getLong("nextTask") < System.currentTimeMillis()) {
            agents.updateOne(
                new Document("agentId", agentId),
                new Document("$set", new Document("nextTask", System.currentTimeMillis() + (interval * 1000L)))
            );

            JSONArray targetList = new JSONArray();
            for (Document teamData: teams.find(new Document("id", new Document("$ne", ownTeamId)))) {
                JSONObject target = new JSONObject();

                target.put("ip", teamData.getString("ip"));
                target.put("name", teamData.getString("name"));

                targetList.add(target);
            }

            JSONObject taskData = new JSONObject();
            taskData.put("target", targetList);

            task = new JSONObject();
            task.put("name", "exploit_exec");
            task.put("data", taskData);

        }

        response.put("success", true);
        response.put("task", task);
    }

    @SuppressWarnings("unchecked")
    public void prepareMonitorTask(String agentId, JSONObject response) {
        JSONObject task = new JSONObject();
        JSONObject data = new JSONObject();

        ((Service) WebUIPages.getWebPage("service").handler()).list(null, data);

        String teamId = "1";
        Document exploitsConfig = config.find(new Document("configType", "xploits")).first();
        if (exploitsConfig != null) {
            teamId = ((Document) exploitsConfig.get("xploits")).getString("team");
        }
        Document teamInfo = teams.find(new Document("id", teamId)).first();
        if (teamInfo != null) {
            data.put("host", teamInfo.getString("ip"));
        } else {
            data.put("host", null);
        }

        task.put("name", "traffic_scanner");
        task.put("data", data);

        response.put("success", true);
        response.put("task", task);
    }
}
