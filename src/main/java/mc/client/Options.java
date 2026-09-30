package mc.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** User settings, persisted as options.json next to the saves folder. */
public final class Options {
    public int renderDistance = 10;
    public float fov = 70;
    public float sensitivity = 0.5f;
    public float gamma = 0.5f;
    public boolean viewBobbing = true;
    public boolean vsync = true;
    public boolean clouds = true;
    public float volume = 1f;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Options load(Path file) {
        try {
            if (Files.exists(file)) {
                Options o = GSON.fromJson(Files.readString(file), Options.class);
                if (o != null) return o;
            }
        } catch (Exception e) {
            System.err.println("Could not read options: " + e);
        }
        return new Options();
    }

    public void save(Path file) {
        try {
            Files.writeString(file, GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("Could not save options: " + e);
        }
    }

    /** World metadata (level.json). */
    public static final class Level {
        public long seed;
        public long time = 1000;
        public double x, y, z;
        public float yaw, pitch;
        public boolean flying, creative, spawned;
        public int[] hotbar;
        public int selected;

        public static Level load(Path file) {
            try {
                if (Files.exists(file)) return GSON.fromJson(Files.readString(file), Level.class);
            } catch (Exception e) {
                System.err.println("Could not read level: " + e);
            }
            return null;
        }

        public void save(Path file) {
            try {
                Files.writeString(file, GSON.toJson(this));
            } catch (IOException e) {
                System.err.println("Could not save level: " + e);
            }
        }
    }
}
