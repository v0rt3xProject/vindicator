package ru.v0rt3x.vindicator.modules.flags;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.v0rt3x.vindicator.VindicatorCore;
import ru.v0rt3x.vindicator.common.Utils;
import ru.v0rt3x.vindicator.service.Service;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class FlagSender implements Service {

    private static final String[] THEMIS_ERRORS = new String[] {
        "Submitted flag has been accepted",
        "Generic error",
        "The attacker does not appear to be a team",
        "Contest has not been started yet",
        "Contest has been paused",
        "Contest has been completed",
        "Submitted data has invalid format",
        "Attack attempts limit exceeded",
        "Submitted flag has expired",
        "Submitted flag belongs to the attacking team and therefore won't be accepted",
        "Submitted flag has been accepted already",
        "Submitted flag has not been found",
        "The attacking team service is not up and therefore flags from the same services of other teams won't be accepted"
    };
    private static final String[] PRIORITY = new String[] { "LO", "NO", "HI" };

    private MongoCollection<Document> flags;
    private boolean isRunning = true;

    private static Logger logger = LoggerFactory.getLogger(FlagSender.class);

    @Override
    public void onInit() {
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
                Document flag = flags.find(new Document("state", 0))
                    .projection(new Document("flag", 1).append("priority", 1))
                    .sort(new Document("priority", -1).append("timestamp", 1))
                    .first();

                if (flag != null) {
                    try {
                        JSONArray result = sendFlag(flag.getString("flag"));

                        if (result == null) {
                            Utils.sleep(3000);
                            continue;
                        }

                        int responseCode = (int) (long) result.get(0);

                        switch (responseCode) {
                            case 0:
                                logger.info(String.format(
                                    "Flag[%s:%s] sent!",
                                    PRIORITY[flag.getInteger("priority")], flag.getString("flag")
                                ));
                                flags.updateOne(flag, new Document(
                                    "$set", new Document("state", 1).append("updateTime", System.currentTimeMillis())
                                ));
                                break;
                            case 1:
                            case 3:
                            case 4:
                            case 7:
                            case 12:
                                logger.info(String.format(
                                    "Flag[%s:%s] was not accepted: %s",
                                    PRIORITY[flag.getInteger("priority")], flag.getString("flag"),
                                    THEMIS_ERRORS[responseCode]
                                ));
                                Utils.sleep(5000);
                                break;
                            case 6:
                            case 8:
                            case 9:
                            case 10:
                            case 11:
                                logger.info(String.format(
                                    "Flag[%s:%s] is invalid: %s",
                                    PRIORITY[flag.getInteger("priority")], flag.getString("flag"),
                                    THEMIS_ERRORS[responseCode]
                                ));
                                flags.updateOne(flag, new Document(
                                    "$set", new Document("state", 2).append("updateTime", System.currentTimeMillis())
                                ));
                                break;
                            case 5:
                                logger.error("Contest is finished, so no flags accepted");
                                Utils.sleep(15000);
                                break;
                            case 2:
                                logger.error("Something wrong with network configuration!");
                                Utils.sleep(15000);
                                break;
                        }
                    } catch (IOException e) {
                        logger.error("unable to process flag: {}", e.getMessage());
                    }
                } else {
                    Thread.sleep(300);
                }
            } catch (InterruptedException ignored) {}
        }
    }

    @SuppressWarnings("unchecked")
    private JSONArray sendFlag(String flag) throws IOException {
        Document themisConfig = VindicatorCore.getInstance()
            .getDataBase()
            .getCollection("config")
            .find(new Document("configType", "themis"))
            .first();

        if (themisConfig == null) {
            logger.error("Themis URL is not configured");
            return null;
        }

        themisConfig = (Document) themisConfig.get("themis");

        URL checkerURL = new URL(String.format(
            "%s://%s:%s/api/submit",
            themisConfig.getString("protocol"),
            themisConfig.getString("host"),
            themisConfig.getString("port")
        ));

        HttpURLConnection httpConnection = (HttpURLConnection) checkerURL.openConnection();

        httpConnection.setRequestMethod("POST");
        httpConnection.addRequestProperty("Content-Type", "application/json");
        httpConnection.setDoOutput(true);

        JSONArray flagJSON = new JSONArray();
        flagJSON.add(flag);

        DataOutputStream wr = new DataOutputStream(httpConnection.getOutputStream());

        wr.writeBytes(flagJSON.toJSONString());
        wr.flush();
        wr.close();

        JSONParser parser = new JSONParser();
        JSONArray response = null;

        try {
            response = (JSONArray) parser.parse(new InputStreamReader(httpConnection.getInputStream()));
        } catch (ParseException e) {
            logger.error("Unable to parse Themis response: {}", e.getMessage());
        }

        return response;
    }
}
