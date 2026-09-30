package mc.render;

import mc.entity.*;
import mc.item.Item;
import mc.world.Block;
import mc.world.World;
import org.joml.Matrix4f;

import java.util.EnumMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33C.*;

/** Draws mobs, dropped items, falling blocks, TNT and arrows into the HDR scene. */
public final class EntityRenderer {
    public interface LightFn { float light(double x, double y, double z); }

    private final Map<MobType, MobModel> models = new EnumMap<>(MobType.class);
    public ItemRenderer items;

    private MobModel model(MobType t) {
        return models.computeIfAbsent(t, MobModel::create);
    }

    public void render(World world, WorldRenderer wr, float pt, LightFn light) {
        glDisable(GL_CULL_FACE);
        for (Entity e : world.entities()) {
            double ex = e.interpX(pt) - wr.camX, ey = e.interpY(pt) - wr.camY, ez = e.interpZ(pt) - wr.camZ;
            if (ex * ex + ey * ey + ez * ez > wr.fogEnd * wr.fogEnd) continue;
            float l = light.light(e.x, e.y + e.height * 0.5, e.z);
            if (e.fireTicks > 0) l = Math.max(l, 1.5f);
            if (e instanceof Mob m) renderMob(m, wr, pt, ex, ey, ez, l);
            else if (e instanceof ItemEntity it) renderItem(it, wr, pt, ex, ey, ez, l);
            else if (e instanceof FallingBlockEntity fb) renderBlock(Block.get(fb.blockId), wr, ex, ey + 0.49, ez, 0.98f, l, 0xFFFFFF);
            else if (e instanceof TntEntity tnt) {
                float s = tnt.fuse < 10 ? 1 + (10 - tnt.fuse) * 0.02f : 1;
                int flash = (tnt.fuse / 5) % 2 == 0 ? 0xFFFFFF : 0xFFFFFF;
                renderBlock(Block.TNT, wr, ex, ey + 0.49, ez, 0.98f * s, (tnt.fuse / 5) % 2 == 0 ? l * 3 : l, flash);
            } else if (e instanceof ArrowEntity a) renderArrow(a, wr, pt, ex, ey, ez, l);
        }
        glEnable(GL_CULL_FACE);
    }

    private static float lerpAngle(float a, float b, float t) {
        float d = b - a;
        while (d < -180) d += 360;
        while (d >= 180) d -= 360;
        return a + d * t;
    }

    private void renderMob(Mob m, WorldRenderer wr, float pt, double ex, double ey, double ez, float light) {
        MobModel model = model(m.type);
        float bodyYaw = lerpAngle(m.prevBodyYaw, m.bodyYaw, pt);
        float headYaw = lerpAngle(m.prevHeadYaw, m.headYaw, pt) - bodyYaw;
        float swing = m.limbSwing - m.limbSwingAmount * (1 - pt);
        float amount = m.prevLimbSwingAmount + (m.limbSwingAmount - m.prevLimbSwingAmount) * pt;
        if (amount > 1) amount = 1;
        float legA = (float) Math.cos(swing * 0.6662) * 1.4f * amount;
        float legB = (float) Math.cos(swing * 0.6662 + Math.PI) * 1.4f * amount;
        float time = m.age + pt;
        for (MobModel.Part p : model.parts) { p.rx = p.ry = p.rz = 0; }
        MobModel.Part head = model.get("head");
        if (head != null) {
            head.ry = (float) Math.toRadians(-headYaw);
            head.rx = (float) Math.toRadians(m.pitch);
        }
        switch (m.type) {
            case ZOMBIE, SKELETON -> {
                model.get("legL").rx = legA;
                model.get("legR").rx = legB;
                boolean aggressive = m.type == MobType.ZOMBIE || m.aggressive || !m.world.isDaytime();
                float armBase = aggressive ? (float) -Math.PI / 2 : 0;
                model.get("armL").rx = armBase + (aggressive ? 0 : legB) + (float) Math.sin(time * 0.067) * 0.05f;
                model.get("armR").rx = armBase + (aggressive ? 0 : legA) - (float) Math.sin(time * 0.067) * 0.05f;
                model.get("armL").rz = -(float) (Math.cos(time * 0.09) * 0.05 + 0.05);
                model.get("armR").rz = (float) (Math.cos(time * 0.09) * 0.05 + 0.05);
            }
            case CHICKEN -> {
                model.get("legL").rx = legA;
                model.get("legR").rx = legB;
                float flap = m.onGround ? 0 : (float) Math.sin(time * 1.5) * 0.9f + 0.9f;
                model.get("wingL").rz = flap;
                model.get("wingR").rz = -flap;
            }
            case SPIDER -> {
                for (int i = 0; i < 4; i++) {
                    float fan = (float) Math.toRadians((i - 1.5) * 20);
                    float w = (float) Math.sin(swing * 0.6662 * 2 + i * Math.PI / 2) * 0.4f * amount;
                    model.get("legL" + i).ry = -fan + w;
                    model.get("legR" + i).ry = fan - w;
                    model.get("legL" + i).rz = (float) Math.toRadians(-40) + Math.abs(w) * 0.5f;
                    model.get("legR" + i).rz = (float) Math.toRadians(40) - Math.abs(w) * 0.5f;
                }
            }
            default -> {
                MobModel.Part l0 = model.get("leg0");
                if (l0 != null) {
                    l0.rx = legA;
                    model.get("leg1").rx = legB;
                    model.get("leg2").rx = legB;
                    model.get("leg3").rx = legA;
                }
            }
        }
        Matrix4f mat = new Matrix4f().translate((float) ex, (float) ey, (float) ez).rotateY((float) Math.toRadians(-bodyYaw));
        if (m.deathTime > 0) {
            float d = Math.min(1, (float) Math.sqrt((m.deathTime + pt - 1) / 20f * 1.6f));
            mat.rotateZ((float) (d * Math.PI / 2));
        }
        float scale = 1 / 16f;
        int tint = 0xFFFFFF;
        if (m.type == MobType.CREEPER && m.fuse > 0) {
            float f = (m.prevFuse + (m.fuse - m.prevFuse) * pt) / 30f;
            float s = 1 + (float) Math.sin(f * 100) * f * 0.01f;
            f = Math.min(1, Math.max(0, f));
            f *= f;
            scale *= (1 + f * 0.4f) * s;
            if ((int) (f * 10) % 2 == 0) light *= 1 + f * 2;
        }
        if (m.type == MobType.SHEEP && m.sheepColor != 0) {
            tint = new int[]{0xFFFFFF, 0x404048, 0xf0d040, 0xd05050, 0x6060e0}[m.sheepColor];
        }
        if (m.hurtTime > 0 || m.deathTime > 0) tint = 0xFF8080;
        mat.scale(scale);
        wr.setupBasic(wr.projView, true, 0, 0.1f);
        wr.basicShader.set("uLight", light);
        glActiveTexture(GL_TEXTURE0);
        model.skin.bind();
        wr.batch.begin(GL_TRIANGLES);
        model.render(wr.batch, mat, tint);
        wr.batch.end();
    }

    private void renderItem(ItemEntity it, WorldRenderer wr, float pt, double ex, double ey, double ez, float light) {
        Item item = it.stack.item;
        float t = it.age + pt;
        float bob = (float) Math.sin(t / 10f + it.spin) * 0.1f + 0.1f;
        int copies = it.stack.count > 20 ? 3 : it.stack.count > 1 ? 2 : 1;
        wr.setupBasic(wr.projView, true, 0, 0.1f);
        wr.basicShader.set("uLight", light);
        items.bindFor(item, wr);
        wr.batch.begin(GL_TRIANGLES);
        for (int i = 0; i < copies; i++) {
            Matrix4f m = new Matrix4f().translate((float) ex, (float) ey + bob + 0.125f, (float) ez)
                    .rotateY(t / 20f + it.spin);
            if (ItemRenderer.isCube(item)) m.translate(i * 0.06f, i * 0.06f, i * 0.04f).scale(0.25f);
            else m.translate(0, 0.05f, i * 0.07f).scale(0.5f);
            items.emit(wr.batch, m, item, ItemRenderer.tint(item));
        }
        wr.batch.end();
    }

    private void renderBlock(Block b, WorldRenderer wr, double ex, double ey, double ez, float size, float light, int tint) {
        wr.setupBasic(wr.projView, true, 0, 0.1f);
        wr.basicShader.set("uLight", light);
        glActiveTexture(GL_TEXTURE0);
        wr.atlas.bind();
        wr.batch.begin(GL_TRIANGLES);
        items.emit(wr.batch, new Matrix4f().translate((float) ex, (float) ey, (float) ez).scale(size), Item.of(b), tint);
        wr.batch.end();
    }

    private void renderArrow(ArrowEntity a, WorldRenderer wr, float pt, double ex, double ey, double ez, float light) {
        wr.setupBasic(wr.projView, true, 0, 0.1f);
        wr.basicShader.set("uLight", light);
        items.bindFor(Item.ARROW, wr);
        wr.batch.begin(GL_TRIANGLES);
        float yaw = lerpAngle(a.prevYaw, a.yaw, pt), pitch = a.prevPitch + (a.pitch - a.prevPitch) * pt;
        // The sprite is drawn diagonally; rotate it so its tip points along +Z, then orient along the flight path
        Matrix4f m = new Matrix4f().translate((float) ex, (float) ey, (float) ez)
                .rotateY((float) Math.toRadians(-yaw)).rotateX((float) Math.toRadians(pitch))
                .rotateY((float) Math.toRadians(-90)).rotateZ((float) Math.toRadians(-45)).scale(0.7f);
        items.emit(wr.batch, m, Item.ARROW, 0xFFFFFF);
        items.emit(wr.batch, new Matrix4f(m).rotateX((float) Math.toRadians(90)), Item.ARROW, 0xFFFFFF);
        wr.batch.end();
    }
}
