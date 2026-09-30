package mc.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * One end of a network connection. Netty's I/O threads queue incoming packets; the game thread polls them
 * each tick, so all game logic stays single-threaded (like Minecraft's packet handling on the main thread).
 */
public final class Connection {
    private final Channel channel;
    private final ConcurrentLinkedQueue<ByteBuf> inbound = new ConcurrentLinkedQueue<>();
    private volatile String closeReason;

    Connection(Channel channel) {
        this.channel = channel;
    }

    /** Queues a packet (sent on the next flush). */
    public void send(ByteBuf packet) {
        if (channel.isActive()) channel.write(packet, channel.voidPromise());
        else packet.release();
    }

    /** Sends a packet immediately. */
    public void sendNow(ByteBuf packet) {
        send(packet);
        flush();
    }

    public void flush() {
        if (channel.isActive()) channel.flush();
    }

    /** Next received packet, or null. The caller must release it. */
    public ByteBuf poll() {
        return inbound.poll();
    }

    public boolean isOpen() {
        return channel.isActive();
    }

    public void close(String reason) {
        if (closeReason == null) closeReason = reason;
        channel.close();
        ByteBuf b;
        while ((b = inbound.poll()) != null) b.release();
    }

    /** Why the connection ended (null while open). */
    public String closeReason() {
        return closeReason != null ? closeReason : channel.isActive() ? null : "Disconnected";
    }

    public String remoteAddress() {
        return String.valueOf(channel.remoteAddress());
    }

    /** Pipeline end: hands complete frames to the game thread. */
    static class Handler extends SimpleChannelInboundHandler<ByteBuf> {
        Connection connection;

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            connection = new Connection(ctx.channel());
            ctx.channel().attr(Net.CONNECTION).set(connection);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            connection.inbound.add(msg.retain());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (connection != null && connection.closeReason == null) connection.closeReason = "Connection closed";
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (connection != null && connection.closeReason == null) {
                connection.closeReason = cause instanceof io.netty.handler.timeout.ReadTimeoutException ? "Timed out"
                        : cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
            }
            ctx.close();
        }
    }
}
