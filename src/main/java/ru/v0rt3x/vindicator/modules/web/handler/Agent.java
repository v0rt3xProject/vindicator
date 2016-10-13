package ru.v0rt3x.vindicator.modules.web.handler;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import org.apache.velocity.VelocityContext;
import org.bson.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import ru.v0rt3x.vindicator.VindicatorCore;
import ru.v0rt3x.vindicator.common.GenericMethod;
import ru.v0rt3x.vindicator.modules.web.WebUIPageHandler;

import java.util.ArrayList;
import java.util.List;

public class Agent implements WebUIPageHandler {

    private MongoCollection<Document> agents;

    public Agent() {
        agents = VindicatorCore.getInstance().getDataBase().getCollection("agents");
        agents.createIndex(new Document("agentId", 1), new IndexOptions().unique(true));
    }

    public static class AgentObject {

        private String id;
        private String type;
        private String hostName;
        private String osName;
        private String osArch;
        private String osVersion;
        private String userName;
        private String userHome;
        private String workingDir;
        private String task;
        private String taskState;
        private Long lastSeen;

        public AgentObject(String id, String type, String hostName, String osName, String osArch, String osVersion, String userName, String userHome, String workingDir, String task, String taskState, Long lastSeen) {
            this.id = id;
            this.type = type;
            this.hostName = hostName;
            this.osName = osName;
            this.osArch = osArch;
            this.osVersion = osVersion;
            this.userName = userName;
            this.userHome = userHome;
            this.workingDir = workingDir;
            this.lastSeen = lastSeen;
            this.task = task;
            this.taskState = taskState;
        }

        public String id() {
            return id;
        }

        public String type() {
            return type;
        }

        public String hostname() {
            return hostName;
        }

        public String os() {
            return osName;
        }

        public String arch() {
            return osArch;
        }

        public String version() {
            return osVersion;
        }

        public String user() {
            return userName;
        }

        public String home() {
            return userHome;
        }

        public String workingDir() {
            return workingDir;
        }

        public Long lastSeen() {
            return lastSeen;
        }

        public String task() {
            return task;
        }

        public String taskState() {
            return taskState;
        }

        static AgentObject fromBSON(Document doc) {
            Document agentInfo = (Document) doc.get("agentInfo");
            Document taskInfo = (Document) doc.get("taskInfo");

            return new AgentObject(
                doc.getString("agentId"),
                doc.getString("agentType"),
                agentInfo.getString("hostName"),
                agentInfo.getString("osName"),
                agentInfo.getString("osArch"),
                agentInfo.getString("osVersion"),
                agentInfo.getString("userName"),
                agentInfo.getString("userHome"),
                agentInfo.getString("workingDir"),
                taskInfo.getString("name"),
                taskInfo.getString("state"),
                doc.getLong("lastSeen")
            );
        }
    }

    @Override
    public void prepareContext(VelocityContext ctx) {
        List<AgentObject> agentObjectList = new ArrayList<>();

        for (Document doc: agents.find()) {
            agentObjectList.add(AgentObject.fromBSON(doc));
        }

        ctx.internalPut("AGENTS", agentObjectList);
    }

    @SuppressWarnings("unchecked")
    @GenericMethod("list")
    public void list(JSONObject request, JSONObject response) {
        JSONArray agentList = new JSONArray();

        for (Document agent: agents.find()) {
            JSONObject agentData = new JSONObject();
            Document agentInfo = (Document) agent.get("agentInfo");
            Document taskInfo = (Document) agent.get("taskInfo");

            agentData.put("id", agent.getString("agentId"));
            agentData.put("type", agent.getString("agentType"));
            agentData.put("hostName", agentInfo.getString("hostName"));
            agentData.put("osName", agentInfo.getString("osName"));
            agentData.put("osArch", agentInfo.getString("osArch"));
            agentData.put("osVersion", agentInfo.getString("osVersion"));
            agentData.put("userName", agentInfo.getString("userName"));
            agentData.put("userHome", agentInfo.getString("userHome"));
            agentData.put("workingDir", agentInfo.getString("workingDir"));
            agentData.put("task", taskInfo.getString("name"));
            agentData.put("taskState", taskInfo.getString("state"));
            agentData.put("lastSeen", agent.getLong("lastSeen"));

            agentList.add(agentData);
        }

        response.put("agents", agentList);
        response.put("timestamp", System.currentTimeMillis());
    }
}
