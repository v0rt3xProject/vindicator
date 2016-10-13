package ru.v0rt3x.vindicator.modules.web.handler;

import com.mongodb.client.MongoCollection;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.velocity.VelocityContext;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import ru.v0rt3x.vindicator.VindicatorCore;
import ru.v0rt3x.vindicator.common.GenericMethod;
import ru.v0rt3x.vindicator.modules.web.WebUIPageHandler;
import ru.v0rt3x.vindicator.modules.web.WebUIPages;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Traffic implements WebUIPageHandler {

    private MongoCollection<Document> traffic;

    public Traffic() {
        traffic = VindicatorCore.getInstance().getDataBase().getCollection("traffic");
    }

    public static class PacketObject {

        private static MongoCollection<Document> services = VindicatorCore.getInstance()
            .getDataBase()
            .getCollection("service");

        private ObjectId id;

        private Service.ServiceObject service;
        private boolean direction;

        private String clientHost;
        private Integer clientPort;

        private byte[] data;
        private Long time;

        private PacketObject(ObjectId id, Document service, boolean direction, String clientHost, Integer clientPort, String data, Long time) {
            this.id = id;
            this.service = Service.ServiceObject.fromBSON(service);
            this.direction = direction;
            this.clientHost = clientHost;
            this.clientPort = clientPort;
            this.data = (data != null) ? Base64.getDecoder().decode(data) : new byte[0];
            this.time = time;
        }

        public String id() {
            return id.toHexString();
        }

        public Service.ServiceObject service() {
            return service;
        }

        public boolean direction() {
            return direction;
        }

        public String client() {
            return String.format("%s:%d", clientHost, clientPort);
        }

        public String clientHost() {
            return clientHost;
        }

        public Integer clientPort() {
            return clientPort;
        }

        public Integer length() {
            return data.length;
        }

        public byte[] data() {
            return data;
        }

        public String hex() {
            String hexString = "";
            String asciiString = "";

            final int perRow = 16;

            int col = 0;
            int row = 0;

            for (int i = 0; i < data.length; i++) {
                if ((col >= perRow)) {
                    hexString += " " + StringEscapeUtils.escapeHtml(asciiString);

                    col = 0;
                    row++;
                }

                if (col == 0) {
                    hexString += String.format("\n%06X", row * perRow);
                    asciiString = "";
                }

                hexString += String.format(" %02X", data[i]);
                asciiString += ((32 <= data[i])&&(data[i] <= 126)) ? String.format("%c", data[i]) : ".";

                if ((i == data.length - 1)&&(col < perRow)) {
                    int cols = perRow - col - 1;

                    if (cols > 0) {
                        hexString += String.format(String.format("%%%ds", cols * 3), " ");
                        hexString += " " + StringEscapeUtils.escapeHtml(asciiString);
                    }

                    break;
                }

                col++;
            }

            return hexString;
        }

        public String ascii() {
            return StringEscapeUtils.escapeHtml(new String(data));
        }

        public Long time() {
            return time;
        }

        public static PacketObject fromBSON(Document doc) {
            return new PacketObject(
                doc.getObjectId("_id"),
                services.find(new Document("name", doc.getString("service"))).first(),
                doc.getBoolean("direction"),
                doc.getString("clientHost"),
                Math.toIntExact(doc.getLong("clientPort")),
                doc.getString("data"),
                doc.getLong("time")
            );
        }
    }


    @Override
    public void prepareContext(VelocityContext ctx) {
        WebUIPages.getWebPage("service").handler().prepareContext(ctx);

        List<PacketObject> packetObjects = new ArrayList<>();
        for (Document packetData: traffic.find().sort(new Document("time", 1)).limit(50)) {
            packetObjects.add(PacketObject.fromBSON(packetData));
        }

        ctx.internalPut("PACKETS", packetObjects);
    }

    @SuppressWarnings("unchecked")
    @GenericMethod("list")
    public void list(JSONObject request, JSONObject response) {
        JSONObject filter = (JSONObject) request.get("filter");

        String service = (String) filter.get("service");
        String direction = (String) filter.get("direction");
        String client = (String) filter.get("client");
        String length = (String) filter.get("length");
        String data = (String) filter.get("data");

        Integer limit = Math.toIntExact((Long) request.get("limit"));
        Integer offset = Math.toIntExact((Long) request.get("offset"));

        Document trafficFilter = new Document();

        if (!service.equals("all")) {
            trafficFilter.append("service", service);
        }

        if (direction.equals("in")) {
            trafficFilter.append("direction", true);
        } else if (direction.equals("out")) {
            trafficFilter.append("direction", false);
        }

        if (client != null) {
            String[] clientInfo = client.split(":");
            String clientHost = clientInfo[0];
            Integer clientPort = null;

            if (clientInfo.length > 1) {
                clientPort = Math.toIntExact(Long.parseLong(clientInfo[1]));
            }

            if (clientHost.length() > 0) {
                trafficFilter.append("clientHost", new Document("$regex", clientHost));
            }

            if (clientPort != null) {
                trafficFilter.append("clientPort", clientPort);
            }
        }

        if ((data != null)&&(data.length() > 0)) {
            trafficFilter.append("data", new Document("$regex", data));
        }

        JSONArray packetList = new JSONArray();
        for (Document packetData: traffic.find(trafficFilter).skip(offset).limit(limit)) {
            JSONObject packetObject = new JSONObject();
            PacketObject packet = PacketObject.fromBSON(packetData);

            packetObject.put("id", packet.id());

            JSONObject serviceObject = new JSONObject();

            serviceObject.put("name", packet.service().name());
            serviceObject.put("port", packet.service().port());

            packetObject.put("service", serviceObject);
            packetObject.put("direction", packet.direction());
            packetObject.put("client", packet.client());
            packetObject.put("length", packet.length());

            JSONObject dataObject = new JSONObject();

            dataObject.put("hex", packet.hex());
            dataObject.put("ascii", packet.ascii());

            packetObject.put("data", dataObject);

            packetList.add(packetObject);
        }

        response.put("traffic", packetList);
    }
}
