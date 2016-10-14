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

public class Service implements WebUIPageHandler {

    private MongoCollection<Document> services;

    public Service() {
        services = VindicatorCore.getInstance().getDataBase().getCollection("service");
        services.createIndex(new Document("name", 1), new IndexOptions().unique(true));
        services.createIndex(new Document("port", 1), new IndexOptions().unique(true));
    }

    public static class ServiceObject {

        private ObjectId id;
        private String name;
        private Integer port;
        private boolean available;

        private ServiceObject(ObjectId id, String name, Integer port, boolean available) {
            this.id = id;
            this.name = name;
            this.port = port;
            this.available = available;
        }

        public String id() {
            return id.toHexString();
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
                doc.getObjectId("_id"),
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
        JSONArray serviceList = new JSONArray();

        for (Document service: services.find()) {
            JSONObject serviceData = new JSONObject();

            serviceData.put("id", service.getObjectId("_id").toHexString());
            serviceData.put("name", service.getString("name"));
            serviceData.put("port", service.getInteger("port"));
            serviceData.put("available", service.getBoolean("available"));

            serviceList.add(serviceData);
        }

        response.put("service", serviceList);
    }


    @SuppressWarnings("unchecked")
    @GenericMethod("delete")
    public void delete(JSONObject request, JSONObject response) {
        String target = (String) request.get("target");

        Document filter = new Document("_id", new ObjectId(target));

        response.put("notify", true);
        if (services.deleteOne(filter).getDeletedCount() > 0) {
            response.put("success", true);
            response.put("message", String.format("Service %s deleted", target));

            response.put("target", target);
        } else {
            response.put("success", false);
            response.put("message", String.format("Unable to delete Service %s", target));
        }
    }
}
