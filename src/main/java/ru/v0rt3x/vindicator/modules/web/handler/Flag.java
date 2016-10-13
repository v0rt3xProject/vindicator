package ru.v0rt3x.vindicator.modules.web.handler;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import org.apache.velocity.VelocityContext;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import ru.v0rt3x.vindicator.VindicatorCore;
import ru.v0rt3x.vindicator.common.Queue;
import ru.v0rt3x.vindicator.common.GenericMethod;
import ru.v0rt3x.vindicator.modules.web.WebUIPageHandler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Flag implements WebUIPageHandler {

    private MongoCollection<Document> flags;
    private Queue<String> flagQueue;

    @SuppressWarnings("unchecked")
    public Flag() {
        flags = VindicatorCore.getInstance().getDataBase().getCollection("flags");
        flags.createIndex(new Document("flag", 1), new IndexOptions().unique(true));

        VindicatorCore.getInstance().createQueue(String.class, "flags");
        flagQueue = (Queue<String>) VindicatorCore.getInstance().getQueue("flags");
    }

    public static class FlagObject {

        private ObjectId id;
        private String flag;
        private Integer state;
        private Integer priority;
        private Long time;

        private FlagObject(ObjectId id, String flag, Integer state, Integer priority, Long time) {
            this.id = id;
            this.flag = flag;
            this.state = state;
            this.priority = priority;
            this.time = time;
        }

        public String id() {
            if (id == null)
                return "N/A";
            return id.toHexString();
        }

        public String flag() {
            if (flag == null)
                return "N/A";
            return flag;
        }

        public String state() {
            if (state == null)
                return "N/A";

            switch (state) {
                case 0:
                    return "Queued";
                case 1:
                    return "Accepted";
                case 2:
                    return "Rejected";
                default:
                    return "Unknown";
            }
        }

        public String priority() {
            if (priority == null)
                return "N/A";

            switch (priority) {
                case 0:
                    return "Low";
                case 1:
                    return "Normal";
                case 2:
                    return "High";
                default:
                    return "Unknown";
            }
        }

        public String time() {
            if (time == null)
                return "N/A";

            return new Date(time).toString();
        }

        @SuppressWarnings("unchecked")
        public JSONObject toJSON() {
            JSONObject data = new JSONObject();

            data.put("id", id());
            data.put("flag", flag());
            data.put("state", state());
            data.put("priority", priority());
            data.put("time", time());

            return data;
        }

        public static FlagObject fromBSON(Document doc) {
            return new FlagObject(
                doc.getObjectId("_id"),
                doc.getString("flag"),
                doc.getInteger("state"),
                doc.getInteger("priority"),
                doc.getLong("updateTime")
            );
        }
    }


    @Override
    public void prepareContext(VelocityContext ctx) {
        ctx.internalPut("PROCESSING", flagQueue.size());

        ctx.internalPut("QUEUED_HI", flags.count(new Document("state", 0).append("priority", 2)));
        ctx.internalPut("QUEUED_NO", flags.count(new Document("state", 0).append("priority", 1)));
        ctx.internalPut("QUEUED_LO", flags.count(new Document("state", 0).append("priority", 0)));

        ctx.internalPut("ACCEPTED", flags.count(new Document("state", 1)));
        ctx.internalPut("REJECTED", flags.count(new Document("state", 2)));

        ctx.internalPut("TIME_STAMP", new Date(System.currentTimeMillis()).toString());

        List<FlagObject> flagObjectList = new ArrayList<>();

        for (Document flag: flags.find().sort(new Document("updateTime", -1)).limit(10)) {
            flagObjectList.add(FlagObject.fromBSON(flag));
        }

        ctx.internalPut("FLAGS", flagObjectList);
    }

    @SuppressWarnings("unchecked")
    @GenericMethod("submit")
    public void submit(JSONObject request, JSONObject response) {
        flagQueue.push(String.format("%s:2", request.get("flag")));

        response.put("success", true);
        response.put("message", "FlagObject submitted");
    }

    @SuppressWarnings("unchecked")
    @GenericMethod("list")
    public void list(JSONObject request, JSONObject response) {
        response.put("status", "ok");
        JSONObject stats = new JSONObject();
        JSONObject queued = new JSONObject();

        queued.put("high", flags.count(new Document("state", 0).append("priority", 2)));
        queued.put("normal", flags.count(new Document("state", 0).append("priority", 1)));
        queued.put("low", flags.count(new Document("state", 0).append("priority", 0)));

        stats.put("processing", flagQueue.size());
        stats.put("queued", queued);
        stats.put("sent", flags.count(new Document("state", 1)));
        stats.put("invalid", flags.count(new Document("state", 2)));

        JSONArray flagList = new JSONArray();

        for (Document flag: flags.find().sort(new Document("updateTime", -1)).limit(10)) {
            flagList.add(FlagObject.fromBSON(flag).toJSON());
        }

        response.put("flags", flagList);
        response.put("stats", stats);
        response.put("timestamp", new Date(System.currentTimeMillis()).toString());
    }
}
