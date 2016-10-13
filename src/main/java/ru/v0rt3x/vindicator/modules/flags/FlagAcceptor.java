package ru.v0rt3x.vindicator.modules.flags;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.v0rt3x.vindicator.VindicatorCore;
import ru.v0rt3x.vindicator.common.Queue;
import ru.v0rt3x.vindicator.service.Service;

import java.util.regex.Pattern;

public class FlagAcceptor implements Service {

    private MongoCollection<Document> flags;
    private Queue<String> flagQueue;
    private boolean isRunning = true;

    private final static Pattern FLAG_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{32}=$"
    );

    private static Logger logger = LoggerFactory.getLogger(FlagAcceptor.class);

    @Override
    @SuppressWarnings("unchecked")
    public void onInit() {
        VindicatorCore.getInstance().createQueue(String.class, "flags");
        flagQueue = (Queue<String>) VindicatorCore.getInstance().getQueue("flags");

        flags = VindicatorCore.getInstance().getDataBase().getCollection("flags");
        flags.createIndex(new Document("flag", 1), new IndexOptions().unique(true));
    }

    @Override
    public void onShutDown() {
        isRunning = false;
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                String flagRecord = flagQueue.pop();
                if (flagRecord != null) {
                    String[] flagData = flagRecord.split(":");
                    if (FLAG_PATTERN.matcher(flagData[0]).matches()) {
                        try { acceptFlag(flagData[0], Integer.parseInt(flagData[1])); } catch (InterruptedException ignored) {}
                    }
                }
            } catch (Exception e) {
                logger.error("Error[{}]: {}", e.getClass().getSimpleName(), e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void acceptFlag(String flag, Integer priority) throws InterruptedException {
        if (flags.find(new Document("flag", flag)).first() == null) {
            flags.insertOne(
                new Document("flag", flag)
                    .append("state", 0)
                    .append("priority", priority)
                    .append("timestamp", System.currentTimeMillis())
                    .append("updateTime", System.currentTimeMillis())
            );
        }
    }
}
