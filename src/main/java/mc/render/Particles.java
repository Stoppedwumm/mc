package mc.render;

import mc.world.Block;
import mc.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Block-break "terrain" particles: tiny textured quads with gravity and collision. */
public final class Particles {
    private static final class P {
        double x, y, z, px, py, pz, vx, vy, vz;
        float u, v, size, brightness;
        int tint, age, life;
        boolean colored;
        double gravity = 0.04;
        float grow;
    }

    private final List<P> list = new ArrayList<>();
    private final Random random = new Random();

    public void spawnBreak(World world, int bx, int by, int bz, Block block, int tint, float brightness) {
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                for (int k = 0; k < 4; k++) {
                    if (random.nextInt(2) == 0) continue;
                    double x = bx + (i + 0.5) / 4, y = by + (j + 0.5) / 4, z = bz + (k + 0.5) / 4;
                    add(x, y, z, x - bx - 0.5, y - by - 0.5, z - bz - 0.5, block, tint, brightness, 0.15);
                }
    }

    public void spawnHit(int bx, int by, int bz, int nx, int ny, int nz, Block block, int tint, float brightness) {
        double x = bx + 0.5 + nx * 0.55 + (nx == 0 ? random.nextDouble() - 0.5 : 0) * 0.9;
        double y = by + 0.5 + ny * 0.55 + (ny == 0 ? random.nextDouble() - 0.5 : 0) * 0.9;
        double z = bz + 0.5 + nz * 0.55 + (nz == 0 ? random.nextDouble() - 0.5 : 0) * 0.9;
        add(x, y, z, nx * 0.3, ny * 0.3 + 0.1, nz * 0.3, block, tint, brightness, 0.08);
    }

    private void add(double x, double y, double z, double dx, double dy, double dz, Block block, int tint, float brightness, double speed) {
        P p = new P();
        p.x = p.px = x; p.y = p.py = y; p.z = p.pz = z;
        p.vx = dx * speed * 2 + (random.nextDouble() - 0.5) * 0.08;
        p.vy = dy * speed * 2 + random.nextDouble() * 0.12;
        p.vz = dz * speed * 2 + (random.nextDouble() - 0.5) * 0.08;
        int tex = block.texSide;
        p.u = ((tex & 15) * 16 + random.nextInt(12)) / 256f;
        p.v = ((tex >> 4) * 16 + random.nextInt(12)) / 256f;
        p.size = 0.06f + random.nextFloat() * 0.05f;
        p.tint = tint;
        p.brightness = brightness;
        p.life = 12 + random.nextInt(18);
        list.add(p);
    }

    /** Untextured effect particles: smoke, explosions, flames, crits, bubbles, splashes... */
    public void spawn(String type, double x, double y, double z) {
        P p = new P();
        p.x = p.px = x; p.y = p.py = y; p.z = p.pz = z;
        p.colored = true;
        p.brightness = 1;
        p.vx = (random.nextDouble() - 0.5) * 0.05;
        p.vy = (random.nextDouble() - 0.5) * 0.05;
        p.vz = (random.nextDouble() - 0.5) * 0.05;
        p.size = 0.08f;
        p.life = 20 + random.nextInt(10);
        p.gravity = 0;
        switch (type) {
            case "smoke" -> { int g = 40 + random.nextInt(60); p.tint = g << 16 | g << 8 | g; p.vy = 0.03; p.size = 0.1f; p.grow = 0.004f; p.brightness = 0.6f; }
            case "explosion" -> { int g = 180 + random.nextInt(70); p.tint = g << 16 | g << 8 | g; p.size = 0.5f + random.nextFloat() * 0.5f; p.life = 8 + random.nextInt(6); p.grow = 0.03f; p.brightness = 1.4f; }
            case "poof" -> { p.tint = 0xe0e0e0; p.vy = 0.05 + random.nextDouble() * 0.05; p.vx *= 3; p.vz *= 3; p.size = 0.15f; p.life = 12; }
            case "flame" -> { p.tint = 0xffa030; p.vy = 0.01; p.size = 0.06f; p.life = 12; p.brightness = 4; p.grow = -0.004f; }
            case "crit" -> { p.tint = 0xe8e0c0; p.vx *= 6; p.vy = 0.15 + random.nextDouble() * 0.1; p.vz *= 6; p.gravity = 0.03; p.size = 0.05f; p.life = 10; }
            case "bubble" -> { p.tint = 0xa0c8ff; p.vy = 0.08; p.size = 0.04f; p.life = 16; }
            case "splash" -> { p.tint = 0x6090e0; p.vy = 0.2 + random.nextDouble() * 0.1; p.vx *= 4; p.vz *= 4; p.gravity = 0.04; p.size = 0.05f; p.life = 14; }
            case "heart" -> { p.tint = 0xff2040; p.vy = 0.05; p.size = 0.1f; p.life = 20; p.brightness = 1.5f; }
            case "happy" -> { p.tint = 0x40ff40; p.vy = 0.02; p.size = 0.05f; p.life = 20; p.brightness = 1.5f; }
            case "drip" -> { p.tint = 0x3060e0; p.vx = p.vz = 0; p.vy = -0.2; p.gravity = 0.02; p.size = 0.03f; p.life = 12; }
            default -> p.tint = 0xffffff;
        }
        list.add(p);
    }

    public void tick(World world) {
        for (int i = list.size() - 1; i >= 0; i--) {
            P p = list.get(i);
            p.px = p.x; p.py = p.y; p.pz = p.z;
            if (++p.age > p.life) { list.remove(i); continue; }
            p.size = Math.max(0.01f, p.size + p.grow);
            if (p.colored) {
                p.vy -= p.gravity;
                p.x += p.vx; p.y += p.vy; p.z += p.vz;
                p.vx *= 0.96; p.vy *= 0.96; p.vz *= 0.96;
                continue;
            }
            p.vy -= p.gravity;
            double ny = p.y + p.vy;
            if (Block.get(world.getBlock((int) Math.floor(p.x), (int) Math.floor(ny), (int) Math.floor(p.z))).solid) {
                p.vy = 0;
                p.vx *= 0.7;
                p.vz *= 0.7;
            } else p.y = ny;
            double nx = p.x + p.vx, nz = p.z + p.vz;
            if (!Block.get(world.getBlock((int) Math.floor(nx), (int) Math.floor(p.y), (int) Math.floor(p.z))).solid) p.x = nx; else p.vx = 0;
            if (!Block.get(world.getBlock((int) Math.floor(p.x), (int) Math.floor(p.y), (int) Math.floor(nz))).solid) p.z = nz; else p.vz = 0;
            p.vx *= 0.98; p.vy *= 0.98; p.vz *= 0.98;
        }
    }

    public boolean isEmpty() { return list.isEmpty(); }

    public int size() { return list.size(); }

    /** Emits camera-facing quads relative to the camera; colored selects the untextured particle pass. */
    public void render(Batch batch, double cx, double cy, double cz, float yaw, float pitch, float pt, boolean colored) {
        double ry = Math.toRadians(yaw), rp = Math.toRadians(pitch);
        // Camera right and up vectors
        float rx = (float) Math.cos(ry), rz = (float) Math.sin(ry);
        float ux = (float) (Math.sin(ry) * Math.sin(rp)), uy = (float) Math.cos(rp), uz = (float) (-Math.cos(ry) * Math.sin(rp));
        float us = 4 / 256f;
        for (P p : list) {
            if (p.colored != colored) continue;
            float x = (float) (p.px + (p.x - p.px) * pt - cx);
            float y = (float) (p.py + (p.y - p.py) * pt - cy);
            float z = (float) (p.pz + (p.z - p.pz) * pt - cz);
            float s = p.size;
            if (colored) {
                float fade = 1 - (float) p.age / p.life;
                int a = (int) (Math.min(1, fade * 1.5f) * 255);
                float br = p.brightness;
                int col = a << 24 | Math.min(255, (int) ((p.tint >> 16 & 255) * Math.min(1, br))) << 16 | Math.min(255, (int) ((p.tint >> 8 & 255) * Math.min(1, br))) << 8 | Math.min(255, (int) ((p.tint & 255) * Math.min(1, br)));
                batch.quad(x - rx * s + ux * s, y + uy * s, z - rz * s + uz * s, 0, 0,
                        x - rx * s - ux * s, y - uy * s, z - rz * s - uz * s, 0, 1,
                        x + rx * s - ux * s, y - uy * s, z + rz * s - uz * s, 1, 1,
                        x + rx * s + ux * s, y + uy * s, z + rz * s + uz * s, 1, 0, col);
                continue;
            }
            int tr = (int) ((p.tint >> 16 & 255) * p.brightness), tg = (int) ((p.tint >> 8 & 255) * p.brightness), tb = (int) ((p.tint & 255) * p.brightness);
            int color = 0xFF000000 | tr << 16 | tg << 8 | tb;
            batch.quad(
                    x - rx * s + ux * s, y + uy * s, z - rz * s + uz * s, p.u, p.v,
                    x - rx * s - ux * s, y - uy * s, z - rz * s - uz * s, p.u, p.v + us,
                    x + rx * s - ux * s, y - uy * s, z + rz * s - uz * s, p.u + us, p.v + us,
                    x + rx * s + ux * s, y + uy * s, z + rz * s + uz * s, p.u + us, p.v, color);
        }
    }
}
