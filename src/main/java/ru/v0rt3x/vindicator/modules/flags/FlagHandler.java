package ru.v0rt3x.vindicator.modules.flags;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.v0rt3x.vindicator.VindicatorCore;
import ru.v0rt3x.vindicator.common.Queue;

@ChannelHandler.Sharable
public class FlagHandler extends ChannelInboundHandlerAdapter {

    private int priority;

    private Queue<String> flagQueue;
    private static Logger logger = LoggerFactory.getLogger(FlagHandler.class);

    @SuppressWarnings("unchecked")
    public FlagHandler(int priority) {
        this.priority = priority;

        flagQueue = (Queue<String>) VindicatorCore.getInstance().getQueue("ctfFlags");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        flagQueue.push(String.format("%s:%d", msg, priority));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Unable to process request: [{}]: {}", cause.getClass().getSimpleName(), cause.getMessage());
        ctx.close();
    }
}
