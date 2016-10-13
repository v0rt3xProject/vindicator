package ru.v0rt3x.vindicator.modules.monitor;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.slf4j.LoggerFactory;
import ru.v0rt3x.vindicator.VindicatorCore;
import ru.v0rt3x.vindicator.common.Utils;
import ru.v0rt3x.vindicator.service.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ServiceMonitor implements Service {

    private MongoCollection<Document> services;
    private MongoCollection<Document> config;
    private MongoCollection<Document> teams;

    private boolean isRunning = true;

    @Override
    public void onInit() {
        services = VindicatorCore.getInstance().getDataBase().getCollection("service");
        config = VindicatorCore.getInstance().getDataBase().getCollection("config");
        teams = VindicatorCore.getInstance().getDataBase().getCollection("teams");
    }

    @Override
    public void onShutDown() {
        isRunning = false;
    }

    @Override
    public void run() {
        while (isRunning) {
            Document configData = config.find(new Document("configType", "xploit")).first();
            if (configData != null) {
                configData = (Document) configData.get("xploit");

                String host = teams.find(new Document("id", configData.getString("team"))).first().getString("ip");

                for (Document serviceData : services.find()) {
                    boolean serviceRunning = isServiceRunning(host, serviceData.getInteger("port"));
                    services.updateOne(serviceData, new Document("$set", new Document("available", serviceRunning)));
                }
            }

            Utils.sleep(10000);
        }
    }

    private boolean isServiceRunning(String host, Integer port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
