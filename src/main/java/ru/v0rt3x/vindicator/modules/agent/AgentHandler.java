package ru.v0rt3x.vindicator.modules.agent;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.v0rt3x.vindicator.VindicatorCore;
import ru.v0rt3x.vindicator.component.ComponentManager;

public class AgentHandler  extends SimpleChannelInboundHandler<String> {

    private static final Logger logger = LoggerFactory.getLogger(AgentHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String message) throws Exception {
        JSONObject request = (JSONObject) new JSONParser().parse(message);
        JSONObject response = VindicatorCore.getInstance()
            .getManager(ComponentManager.class)
            .get(AgentController.class)
            .handle(request);

        ctx.writeAndFlush(response.toJSONString()+"\n");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Unable to handle client request: [{}]: {}", cause.getClass().getSimpleName(), cause.getMessage());
        ctx.close();
    }
}
