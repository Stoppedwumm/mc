package net.minecraft.world.level;

public interface LevelTimeAccess extends LevelReader {
    long dayTime();

    default float getMoonBrightness() { return 1; }

    default float getTimeOfDay(float partial) { return ((dayTime() % 24000) / 24000f + 0.75f) % 1; }

    default int getMoonPhase() { return (int) (dayTime() / 24000L % 8L + 8L) % 8; }
}
