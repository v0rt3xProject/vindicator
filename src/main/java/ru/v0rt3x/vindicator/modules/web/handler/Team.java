package ru.v0rt3x.vindicator.modules.web.handler;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import org.apache.velocity.VelocityContext;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import ru.v0rt3x.vindicator.VindicatorCore;
import ru.v0rt3x.vindicator.common.GenericMethod;
import ru.v0rt3x.vindicator.modules.web.WebUIPageHandler;

import java.util.ArrayList;
import java.util.List;

public class Team implements WebUIPageHandler {

    private MongoCollection<Document> teams;

    public Team() {
        teams = VindicatorCore.getInstance().getDataBase().getCollection("teams");
        teams.createIndex(new Document("name", 1), new IndexOptions().unique(true));
        teams.createIndex(new Document("ip", 1), new IndexOptions().unique(true));
    }

    public static class TeamObject {

        private String id;
        private String name;
        private String ip;

        private TeamObject(String id, String name, String ip) {
            this.id = id;
            this.name = name;
            this.ip = ip;
        }

        public String id() {
            return id;
        }

        public String name() {
            return name;
        }

        public String ip() {
            return ip;
        }

        static TeamObject fromBSON(Document doc) {
            return new TeamObject(
                doc.getString("id"),
                doc.getString("name"),
                doc.getString("ip")
            );
        }
    }

    @Override
    public void prepareContext(VelocityContext ctx) {
        List<TeamObject> teamObjectList = new ArrayList<>();

        for (Document doc: teams.find()) {
            teamObjectList.add(TeamObject.fromBSON(doc));
        }

        ctx.internalPut("TEAMS", teamObjectList);
    }

    @SuppressWarnings("unchecked")
    @GenericMethod("add")
    public void add(JSONObject request, JSONObject response) {
        teams.createIndex(new Document("id", 1), new IndexOptions().unique(true));
        teams.createIndex(new Document("name", 1), new IndexOptions().unique(true));
        teams.createIndex(new Document("ip", 1), new IndexOptions().unique(true));

        response.put("notify", true);

        JSONObject teamJSON = (JSONObject) request.get("team");

        String id = (String) teamJSON.get("id");
        String name = (String) teamJSON.get("name");
        String ip = (String) teamJSON.get("ip");

        if (teams.count(new Document("name", name)) > 0) {
            response.put("success", false);
            response.put("message", String.format("TeamObject with name %s already exists!", name));
        } else if (teams.count(new Document("ip", ip)) > 0) {
            response.put("success", false);
            response.put("message", String.format("TeamObject with IP %s already exists!", ip));
        } else if (teams.count(new Document("id", id)) > 0) {
            response.put("success", false);
            response.put("message", String.format("TeamObject with ID %s already exists!", id));
        } else {
            teams.insertOne(new Document("name", name).append("id", id).append("ip", ip));

            response.put("success", true);
            response.put("message", "TeamObject added");
        }
    }

    @SuppressWarnings("unchecked")
    @GenericMethod("list")
    public void list(JSONObject request, JSONObject response) {
        JSONArray teamList = new JSONArray();

        for (Document team: teams.find()) {
            JSONObject teamData = new JSONObject();

            teamData.put("id", team.getString("id"));
            teamData.put("name", team.getString("name"));
            teamData.put("ip", team.getString("ip"));

            teamList.add(teamData);
        }

        response.put("teams", teamList);
    }


    @SuppressWarnings("unchecked")
    @GenericMethod("delete")
    public void delete(JSONObject request, JSONObject response) {
        String target = (String) request.get("target");

        Document filter = new Document("id", target);

        response.put("notify", true);
        if (teams.deleteOne(filter).getDeletedCount() > 0) {
            response.put("success", true);
            response.put("message", String.format("Team %s deleted", target));

            response.put("target", target);
        } else {
            response.put("success", false);
            response.put("message", String.format("Unable to delete Team %s", target));
        }
    }
}
