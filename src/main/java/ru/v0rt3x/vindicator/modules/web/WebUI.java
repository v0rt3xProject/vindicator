package ru.v0rt3x.vindicator.modules.web;

import ru.v0rt3x.vindicator.common.AbstractServer;
import ru.v0rt3x.vindicator.component.Component;
import ru.v0rt3x.vindicator.modules.web.websocket.WebSocketChannelInitializer;

public class WebUI implements Component {

    private AbstractServer webServer;

    @Override
    public void onInit() {
        String webServerHost = core.config().getString("vindicator.web.host", "0.0.0.0");
        Integer webServerPort = core.config().getInt("vindicator.web.port", 65430);

        webServer = new AbstractServer(webServerHost, webServerPort);
        webServer.setChannelHandler(new WebSocketChannelInitializer());

        core.executeThread(webServer);
    }

    @Override
    public void onShutDown() {
        webServer.stop();
    }
}
