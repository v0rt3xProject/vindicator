package ru.v0rt3x.vindicator.modules.flags;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.v0rt3x.vindicator.VindicatorCore;
import ru.v0rt3x.vindicator.service.Service;

public class FlagInvalidator implements Service {

    private boolean isRunning = true;
    private MongoCollection<Document> flags;
    private long flagTimeToLive;

    private static Logger logger = LoggerFactory.getLogger(FlagInvalidator.class);

    @Override
    public void onInit() {
        flags = VindicatorCore.getInstance().getDataBase().getCollection("flags");
        flagTimeToLive = VindicatorCore.getInstance().config().getLong("vindicator.flags.ttl", 300L) * 1000L;
    }

    @Override
    public void onShutDown() {
        isRunning = false;
    }

    @Override
    public void run() {
        while (isRunning) {
            long invalidated = flags.updateMany(
                new Document("timestamp", new Document("$lt", System.currentTimeMillis() - flagTimeToLive))
                    .append("state", 0),
                new Document("$set", new Document("state", 2).append("updateTime", System.currentTimeMillis()))
            ).getModifiedCount();

            if (invalidated > 0L) {
                logger.info("{} flags expired.", invalidated);
            }

            try { Thread.sleep(3000); } catch(InterruptedException ignored) {}
        }
    }
}
