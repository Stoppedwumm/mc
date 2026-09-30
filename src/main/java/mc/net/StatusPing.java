package mc.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Server list ping: connects, asks for the status (message of the day, player count) and times the reply. */
public final class StatusPing {
    private StatusPing() { }

    public record Result(String motd, int online, int max, long pingMs, String error) { }

    public static CompletableFuture<Result> pingAsync(String address) {
        return CompletableFuture.supplyAsync(() -> ping(address));
    }

    public static Result ping(String address) {
        String[] hp = Net.hostPort(address);
        Connection c = null;
        try {
            long start = System.nanoTime();
            c = Net.connect(hp[0], Net.port(hp)).get(5, TimeUnit.SECONDS);
            ByteBuf hello = Protocol.packet(Protocol.C_HELLO);
            hello.writeInt(Protocol.VERSION);
            hello.writeByte(Protocol.INTENT_STATUS);
            Protocol.writeString(hello, "");
            c.sendNow(hello);
            long deadline = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < deadline) {
                ByteBuf b = c.poll();
                if (b == null) {
                    if (!c.isOpen()) break;
                    Thread.sleep(5);
                    continue;
                }
                try {
                    if (b.readUnsignedByte() != Protocol.S_STATUS) continue;
                    long ms = (System.nanoTime() - start) / 1_000_000;
                    JsonObject o = JsonParser.parseString(Protocol.readString(b)).getAsJsonObject();
                    return new Result(o.get("motd").getAsString(), o.get("online").getAsInt(), o.get("max").getAsInt(), ms, null);
                } finally {
                    b.release();
                }
            }
            return new Result("", 0, 0, -1, "No response");
        } catch (Exception e) {
            Throwable t = e.getCause() != null ? e.getCause() : e;
            return new Result("", 0, 0, -1, t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage());
        } finally {
            if (c != null) c.close("Done");
        }
    }
}
