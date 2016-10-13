package ru.v0rt3x.vindicator.modules.flags;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import ru.v0rt3x.vindicator.VindicatorCore;
import ru.v0rt3x.vindicator.common.AbstractServer;
import ru.v0rt3x.vindicator.component.Component;

public class FlagListener implements Component {

    private AbstractServer hiServer;
    private AbstractServer noServer;
    private AbstractServer loServer;

    @Override
    public void onInit() {
        String host = VindicatorCore.getInstance().config().getString("vindicator.tcp.host", "0.0.0.0");

        int hiPort = VindicatorCore.getInstance().config().getInt("vindicator.tcp.hi_port", 65322);
        int noPort = VindicatorCore.getInstance().config().getInt("vindicator.tcp.no_port", 65321);
        int loPort = VindicatorCore.getInstance().config().getInt("vindicator.tcp.lo_port", 65320);

        hiServer = new AbstractServer(host, hiPort);
        noServer = new AbstractServer(host, noPort);
        loServer = new AbstractServer(host, loPort);

        hiServer.setChannelHandler(
            new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(
                        new LineBasedFrameDecoder(32768),
                        new StringDecoder(),
                        new FlagHandler(2)
                    );
                }
            }
        );

        noServer.setChannelHandler(
            new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(
                        new LineBasedFrameDecoder(32768),
                        new StringDecoder(),
                        new FlagHandler(1)
                    );
                }
            }
        );

        loServer.setChannelHandler(
            new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(
                        new LineBasedFrameDecoder(32768),
                        new StringDecoder(),
                        new FlagHandler(0)
                    );
                }
            }
        );

        VindicatorCore.getInstance().executeThread(hiServer);
        VindicatorCore.getInstance().executeThread(noServer);
        VindicatorCore.getInstance().executeThread(loServer);
    }

    @Override
    public void onShutDown() {
        hiServer.stop();
        noServer.stop();
        loServer.stop();
    }
}
