package mc.world;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/** Stores each chunk as a small deflate-compressed file. Writes happen on a background IO thread. */
public final class WorldStorage {
    private static final int VERSION = 2;
    private final Path chunkDir;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Chunk IO");
        t.setDaemon(true);
        return t;
    });
    /** Chunks queued for writing; readers consult this first so they never see stale files. */
    private final ConcurrentHashMap<Long, byte[]> pending = new ConcurrentHashMap<>();

    public WorldStorage(Path dir) throws IOException {
        this.chunkDir = dir.resolve("chunks");
        Files.createDirectories(chunkDir);
    }

    private Path file(int cx, int cz) {
        return chunkDir.resolve("c." + cx + "." + cz + ".bin");
    }

    public void save(Chunk chunk) {
        byte[] data = new byte[Chunk.VOLUME * 2 + 1];
        data[0] = (byte) chunk.state;
        System.arraycopy(chunk.blocks, 0, data, 1, Chunk.VOLUME);
        System.arraycopy(chunk.meta, 0, data, 1 + Chunk.VOLUME, Chunk.VOLUME);
        long key = chunk.key();
        int cx = chunk.cx, cz = chunk.cz;
        pending.put(key, data);
        io.execute(() -> {
            Path f = file(cx, cz);
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(Files.newOutputStream(tmp))))) {
                out.writeInt(VERSION);
                out.write(data);
            } catch (IOException e) {
                System.err.println("Failed to save chunk " + cx + "," + cz + ": " + e);
                return;
            }
            try {
                Files.move(tmp, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                try { Files.move(tmp, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING); } catch (IOException ignored) { }
            }
            pending.remove(key, data);
        });
    }

    /** Returns [state byte + blocks + meta] or null. Safe to call from worker threads. */
    public byte[] load(int cx, int cz) {
        byte[] p = pending.get(Chunk.key(cx, cz));
        if (p != null) return p.clone();
        Path f = file(cx, cz);
        if (!Files.exists(f)) return null;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new InflaterInputStream(Files.newInputStream(f))))) {
            int version = in.readInt();
            byte[] data = new byte[Chunk.VOLUME * 2 + 1];
            if (version == 1) in.readFully(data, 0, Chunk.VOLUME + 1);
            else if (version == VERSION) in.readFully(data);
            else return null;
            return data;
        } catch (IOException e) {
            System.err.println("Corrupt chunk " + cx + "," + cz + ": " + e);
            return null;
        }
    }

    private final ConcurrentHashMap<Long, String> pendingEntities = new ConcurrentHashMap<>();

    private Path entityFile(int cx, int cz) {
        return chunkDir.resolve("e." + cx + "." + cz + ".json");
    }

    /** Writes a chunk's entities as JSON; an empty list deletes the file. */
    public void saveEntities(int cx, int cz, String json, boolean empty) {
        long key = Chunk.key(cx, cz);
        String value = empty ? "" : json;
        pendingEntities.put(key, value);
        io.execute(() -> {
            Path f = entityFile(cx, cz);
            try {
                if (value.isEmpty()) Files.deleteIfExists(f);
                else Files.writeString(f, value);
            } catch (IOException e) {
                System.err.println("Failed to save entities " + cx + "," + cz + ": " + e);
            }
            pendingEntities.remove(key, value);
        });
    }

    /** JSON of a chunk's saved entities, or null. Safe to call from worker threads. */
    public String loadEntities(int cx, int cz) {
        String p = pendingEntities.get(Chunk.key(cx, cz));
        if (p != null) return p.isEmpty() ? null : p;
        Path f = entityFile(cx, cz);
        if (!Files.exists(f)) return null;
        try {
            return Files.readString(f);
        } catch (IOException e) {
            return null;
        }
    }

    public void flush() {
        io.shutdown();
        try {
            io.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }
}
