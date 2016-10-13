package ru.v0rt3x.vindicator.modules.web.handler;

import com.mongodb.client.MongoCollection;
import org.apache.velocity.VelocityContext;
import org.bson.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import ru.v0rt3x.vindicator.VindicatorCore;
import ru.v0rt3x.vindicator.common.GenericMethod;
import ru.v0rt3x.vindicator.common.Queue;
import ru.v0rt3x.vindicator.modules.web.WebUIPageHandler;

public class Index implements WebUIPageHandler {

    private MongoCollection<Document> flags;
    private MongoCollection<Document> exploits;
    private MongoCollection<Document> agents;
    private MongoCollection<Document> services;
    private MongoCollection<Document> teams;

    private Queue<String> flagQueue;

    @SuppressWarnings("unchecked")
    public Index() {
        flags = VindicatorCore.getInstance().getDataBase().getCollection("flags");
        exploits = VindicatorCore.getInstance().getDataBase().getCollection("exploits");
        agents = VindicatorCore.getInstance().getDataBase().getCollection("agents");
        services = VindicatorCore.getInstance().getDataBase().getCollection("service");
        teams = VindicatorCore.getInstance().getDataBase().getCollection("teams");

        VindicatorCore.getInstance().createQueue(String.class, "flags");
        flagQueue = (Queue<String>) VindicatorCore.getInstance().getQueue("flags");
    }

    @Override
    public void prepareContext(VelocityContext ctx) {
        ctx.internalPut("PROCESSING", flagQueue.size());

        ctx.internalPut("QUEUED_HI", flags.count(new Document("state", 0).append("priority", 2)));
        ctx.internalPut("QUEUED_NO", flags.count(new Document("state", 0).append("priority", 1)));
        ctx.internalPut("QUEUED_LO", flags.count(new Document("state", 0).append("priority", 0)));

        ctx.internalPut("ACCEPTED", flags.count(new Document("state", 1)));
        ctx.internalPut("REJECTED", flags.count(new Document("state", 2)));

        ctx.internalPut("EXPLOITS", exploits.count(
            new Document("lastActivity", new Document("$gt", System.currentTimeMillis() - 20000L))
        ));

        ctx.internalPut("REMOTE_AGENTS", agents.count(
            new Document("lastSeen", new Document("$gt", System.currentTimeMillis() - 20000L))
        ));

        ctx.internalPut("SERVICES", services.count());
        ctx.internalPut("TEAMS", teams.count());
    }

    @SuppressWarnings("unchecked")
    @GenericMethod("overview")
    public void overview(JSONObject request, JSONObject response) {
        JSONObject stats = new JSONObject();
        JSONObject queued = new JSONObject();

        queued.put("high", flags.count(new Document("state", 0).append("priority", 2)));
        queued.put("normal", flags.count(new Document("state", 0).append("priority", 1)));
        queued.put("low", flags.count(new Document("state", 0).append("priority", 0)));

        stats.put("processing", flagQueue.size());
        stats.put("queued", queued);
        stats.put("sent", flags.count(new Document("state", 1)));
        stats.put("invalid", flags.count(new Document("state", 2)));

        response.put("stats", stats);
        response.put("exploits", exploits.count(
            new Document("lastActivity", new Document("$gt", System.currentTimeMillis() - 20000L))
        ));
        response.put("remote_agents", agents.count(
            new Document("lastSeen", new Document("$gt", System.currentTimeMillis() - 20000L))
        ));
        response.put("services", services.count());
        response.put("teams", teams.count());
    }
}
