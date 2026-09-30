package mc.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.AttributeKey;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

/** Netty bootstrap for clients and servers: length-prefixed frames over TCP with a read timeout. */
public final class Net {
    private Net() { }

    public static final int DEFAULT_PORT = 25565;
    static final AttributeKey<Connection> CONNECTION = AttributeKey.valueOf("mc.connection");
    private static EventLoopGroup group;

    private static synchronized EventLoopGroup group() {
        if (group == null) {
            ThreadFactory tf = r -> {
                Thread t = new Thread(r, "Netty IO");
                t.setDaemon(true);
                return t;
            };
            group = new NioEventLoopGroup(2, tf);
        }
        return group;
    }

    private static ChannelInitializer<SocketChannel> pipeline(Consumer<Connection> onActive) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.config().setOption(ChannelOption.TCP_NODELAY, true);
                ch.pipeline()
                        .addLast("timeout", new ReadTimeoutHandler(30))
                        .addLast("splitter", new LengthFieldBasedFrameDecoder(32 << 20, 0, 4, 0, 4))
                        .addLast("prepender", new LengthFieldPrepender(4))
                        .addLast("handler", new Connection.Handler() {
                            @Override
                            public void channelActive(io.netty.channel.ChannelHandlerContext ctx) {
                                super.channelActive(ctx);
                                if (onActive != null) onActive.accept(connection);
                            }
                        });
            }
        };
    }

    /** A listening server socket; accepted connections are queued for the game thread. */
    public static final class Listener {
        private final Channel channel;
        private final ConcurrentLinkedQueue<Connection> accepted = new ConcurrentLinkedQueue<>();
        public final int port;

        private Listener(int port) throws InterruptedException {
            ServerBootstrap b = new ServerBootstrap()
                    .group(group())
                    .channel(NioServerSocketChannel.class)
                    .childHandler(pipeline(accepted::add));
            ChannelFuture f = b.bind(port).sync();
            channel = f.channel();
            this.port = ((java.net.InetSocketAddress) channel.localAddress()).getPort();
        }

        /** Next newly accepted connection, or null. */
        public Connection poll() {
            return accepted.poll();
        }

        public void close() {
            channel.close();
        }
    }

    /** Starts listening on a port (0 picks a free one). */
    public static Listener listen(int port) throws InterruptedException {
        return new Listener(port);
    }

    /** Connects to a server; the future completes once the connection is established. */
    public static CompletableFuture<Connection> connect(String host, int port) {
        CompletableFuture<Connection> result = new CompletableFuture<>();
        Bootstrap b = new Bootstrap()
                .group(group())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(pipeline(result::complete));
        b.connect(host, port).addListener((ChannelFuture f) -> {
            if (!f.isSuccess()) {
                Throwable c = f.cause();
                String msg = c == null ? "Connection failed" : c.getMessage() != null ? c.getMessage() : c.getClass().getSimpleName();
                result.completeExceptionally(new java.io.IOException(msg, c));
            }
        });
        return result;
    }

    /** Splits "host", "host:port" or "[ipv6]:port" (default port 25565). */
    public static String[] hostPort(String address) {
        String a = address.trim();
        if (a.startsWith("[")) {
            int end = a.indexOf(']');
            String host = a.substring(1, Math.max(1, end));
            String port = end >= 0 && end + 2 <= a.length() && a.charAt(end + 1) == ':' ? a.substring(end + 2) : "";
            return new String[]{host, port.isEmpty() ? String.valueOf(DEFAULT_PORT) : port};
        }
        int colon = a.lastIndexOf(':');
        if (colon > 0 && a.indexOf(':') == colon) return new String[]{a.substring(0, colon), a.substring(colon + 1)};
        return new String[]{a, String.valueOf(DEFAULT_PORT)};
    }

    public static int port(String[] hp) {
        try { return Integer.parseInt(hp[1].trim()); } catch (NumberFormatException e) { return DEFAULT_PORT; }
    }
}
