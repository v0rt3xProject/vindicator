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

public class Service implements WebUIPageHandler {

    private MongoCollection<Document> services;

    public Service() {
        services = VindicatorCore.getInstance().getDataBase().getCollection("service");
        services.createIndex(new Document("name", 1), new IndexOptions().unique(true));
        services.createIndex(new Document("port", 1), new IndexOptions().unique(true));
    }

    public static class ServiceObject {

        private String name;
        private Integer port;
        private boolean available;

        private ServiceObject(String name, Integer port, boolean available) {
            this.name = name;
            this.port = port;
            this.available = available;
        }

        public String name() {
            return name;
        }

        public Integer port() {
            return port;
        }

        public boolean available() {
            return available;
        }

        public static ServiceObject fromBSON(Document doc) {
            return new ServiceObject(
                doc.getString("name"),
                doc.getInteger("port"),
                doc.getBoolean("available")
            );
        }
    }

    @Override
    public void prepareContext(VelocityContext ctx) {
        List<ServiceObject> serviceObjectList = new ArrayList<>();

        for (Document doc: services.find()) {
            serviceObjectList.add(ServiceObject.fromBSON(doc));
        }

        ctx.internalPut("SERVICES", serviceObjectList);
    }

    @SuppressWarnings("unchecked")
    @GenericMethod("add")
    public void add(JSONObject request, JSONObject response) {
        String srcHost = (String) request.get("name");
        String srcPort = (String) request.get("port");

        services.insertOne(new Document("name", srcHost).append("port", Integer.parseInt(srcPort)));

        response.put("success", true);
        response.put("notify", true);
        response.put("message", "Service created");
    }

    @SuppressWarnings("unchecked")
    @GenericMethod("list")
    public void list(JSONObject request, JSONObject response) {
        JSONArray forwardingList = new JSONArray();

        for (Document forwarding: services.find()) {
            JSONObject forwardingData = new JSONObject();

            forwardingData.put("name", forwarding.getString("name"));
            forwardingData.put("port", forwarding.getInteger("port"));
            forwardingData.put("available", forwarding.getBoolean("available"));

            forwardingList.add(forwardingData);
        }

        response.put("service", forwardingList);
    }
}
