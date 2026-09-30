package mc;

import mc.entity.Player;
import mc.util.RayCast;
import mc.world.Block;
import mc.world.World;
import mc.world.WorldStorage;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks player movement against Minecraft's known values, plus collisions, raycasts and swimming. */
class PlayerPhysicsTest {
    private static void check(boolean ok, String msg) {
        assertTrue(ok, msg);
    }

    @Test
    void movementMatchesMinecraft() throws Exception {
        Path dir = Files.createTempDirectory("mctest");
        World w = new World(42, new WorldStorage(dir));
        w.loadAreaBlocking(0, 0, 3);
        // Build a flat test platform at y=100
        for (int x = -20; x < 36; x++) for (int z = -20; z < 36; z++) { for (int y = 100; y < 110; y++) w.setBlock(x, y, z, 0); w.setBlock(x, 99, z, Block.STONE.id); }
        Player p = new Player();
        p.setPos(0.5, 100, 0.5);
        for (int i = 0; i < 5; i++) p.tick(w, 0, 0, false, false, false);
        check(p.onGround && Math.abs(p.y - 100) < 1e-6, "stands on ground y=" + p.y);

        // Walk forward (+Z at yaw 0) for 2 seconds: Minecraft walking speed is ~4.317 m/s
        p.setPos(0.5, 100, 0.5); p.yaw = 0;
        for (int i = 0; i < 40; i++) p.tick(w, 1, 0, false, false, false);
        double speed = (p.z - 0.5) / 2.0;
        check(speed > 3.9 && speed < 4.5, String.format("walk speed %.3f m/s", speed));

        p.setPos(0.5, 100, 0.5); p.motionX = p.motionZ = 0;
        for (int i = 0; i < 40; i++) p.tick(w, 1, 0, false, false, true);
        speed = (p.z - 0.5) / 2.0;
        check(speed > 5.2 && speed < 5.9, String.format("sprint speed %.3f m/s", speed));

        // Jump height ~1.25 blocks
        p.setPos(0.5, 100, 0.5); p.motionZ = 0;
        p.tick(w, 0, 0, false, false, false);
        double maxY = 0;
        for (int i = 0; i < 20; i++) { p.tick(w, 0, 0, i == 0, false, false); maxY = Math.max(maxY, p.y - 100); }
        check(maxY > 1.2 && maxY < 1.3, String.format("jump height %.3f", maxY));

        // Wall collision
        for (int y = 100; y < 103; y++) for (int x = -5; x < 6; x++) w.setBlock(x, y, 5, Block.STONE.id);
        p.setPos(0.5, 100, 0.5);
        for (int i = 0; i < 40; i++) p.tick(w, 1, 0, false, false, false);
        check(Math.abs(p.z - 4.7) < 1e-6, "blocked by wall at z=" + p.z);

        // Step up half... full block requires jump; walk into a 1-high step and jump
        p.setPos(0.5, 100, 10.5);
        w.setBlock(0, 100, 13, Block.STONE.id);
        for (int i = 0; i < 20; i++) p.tick(w, 1, 0, false, false, false);
        check(Math.abs(p.z - 12.7) < 1e-6, "1-block step stops walking z=" + p.z);
        for (int i = 0; i < 20; i++) p.tick(w, 1, 0, true, false, false);
        check(p.y >= 101 - 1e-6 && p.z > 13, "jumping climbs the step y=" + p.y);

        // Sneaking prevents falling off an edge
        for (int x = 20; x < 36; x++) for (int z = 20; z < 36; z++) w.setBlock(x, 99, z, 0);
        p.setPos(15.5, 100, 25.5); p.yaw = -90; // facing +X
        for (int i = 0; i < 160; i++) p.tick(w, 1, 0, false, true, false);
        check(p.y == 100 && p.x < 20.3 && p.x > 20.0, "sneak stops at edge x=" + p.x);
        for (int i = 0; i < 40; i++) p.tick(w, 1, 0, false, false, false);
        check(p.y < 99, "walking without sneak falls off y=" + p.y);

        // Raycast from above hits the platform top face
        RayCast.Hit h = RayCast.cast(w, 0.5, 101.6, 0.5, 0, -1, 0, 5);
        check(h != null && h.y == 99 && h.ny == 1, "raycast hits top face");
        RayCast.Hit h2 = RayCast.cast(w, 0.5, 100.5, 0.5, 0, 0, 1, 5);
        check(h2 != null && h2.z == 5 && h2.nz == -1, "raycast hits wall side face");

        // Swimming: water slows falling
        for (int y = 100; y < 106; y++) for (int x = -12; x < -8; x++) for (int z = -12; z < -8; z++) w.setBlock(x, y, z, Block.WATER.id);
        p.setPos(-10, 105, -10); p.motionY = 0;
        for (int i = 0; i < 10; i++) p.tick(w, 0, 0, false, false, false);
        check(p.inWater && p.y > 103, "sinks slowly in water y=" + p.y);
        for (int i = 0; i < 60; i++) p.tick(w, 0, 0, true, false, false);
        check(p.y > 104.5, "swims up holding jump y=" + p.y);

        // Terrain sanity near origin
        int top = -1;
        for (int y = 255; y > 0; y--) if (w.getBlock(40, y, 40) != 0) { top = y; break; }
        check(top > 30 && top < 200, "terrain surface present at 40,40: " + top);
        w.shutdown();
    }
}
