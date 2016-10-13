package ru.v0rt3x.vindicator.modules.web.handler;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import org.apache.velocity.VelocityContext;
import org.bson.Document;
import org.json.simple.JSONObject;

import ru.v0rt3x.vindicator.VindicatorCore;
import ru.v0rt3x.vindicator.common.GenericMethod;
import ru.v0rt3x.vindicator.modules.web.WebUIPageHandler;
import ru.v0rt3x.vindicator.modules.web.WebUIPages;

public class Config implements WebUIPageHandler {

    private MongoCollection<Document> config;

    public Config() {
        config = VindicatorCore.getInstance().getDataBase().getCollection("config");
        config.createIndex(new Document("configType", 1), new IndexOptions().unique(true));
    }

    @Override
    public void prepareContext(VelocityContext ctx) {
        Document themisConfig = config.find(new Document("configType", "themis")).first();

        if (themisConfig != null) {
            ctx.internalPut("THEMIS_PROTOCOL", ((Document)themisConfig.get("themis")).getString("protocol"));
            ctx.internalPut("THEMIS_HOST", ((Document)themisConfig.get("themis")).getString("host"));
            ctx.internalPut("THEMIS_PORT", ((Document)themisConfig.get("themis")).getString("port"));
        }

        Document xploitConfig = config.find(new Document("configType", "xploit")).first();

        if (xploitConfig != null) {
            ctx.internalPut("XPLOIT_TEAM", ((Document)xploitConfig.get("xploit")).getString("team"));
            ctx.internalPut("XPLOIT_INTERVAL", ((Document)xploitConfig.get("xploit")).getString("interval"));
        }

        WebUIPages.getWebPage("team").handler().prepareContext(ctx);
    }

    @SuppressWarnings("unchecked")
    @GenericMethod("set")
    public void set(JSONObject request, JSONObject response) {
        String configType = (String) request.get("configType");
        JSONObject configValue = (JSONObject) request.get("configValue");

        Document currentConfig = config.find(new Document("configType", configType)).first();
        if (currentConfig != null) {
            config.updateOne(currentConfig, new Document(
                "$set", new Document(configValue)
            ));
        } else {
            config.insertOne(new Document(configValue).append("configType", configType));
        }

        response.put("success", true);
        response.put("notify", true);
        response.put("message", String.format("Config[%s]: updated", configType));
    }
}
