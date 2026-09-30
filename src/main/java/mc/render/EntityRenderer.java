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

    private MobModel playerModel;
    private final MobModel[] armorModels = new MobModel[5];

    private MobModel armorModel(int material) {
        if (armorModels[material] == null) armorModels[material] = MobModel.createArmor(material, ItemTextureGen.ARMOR_COLORS[material]);
        return armorModels[material];
    }

    /** Draws worn armor over a biped model that has already been posed. */
    private void renderArmor(mc.item.ItemStack[] armor, MobModel body, Matrix4f mat, WorldRenderer wr, int tint) {
        if (armor == null) return;
        for (int slot = 0; slot < 4; slot++) {
            if (mc.item.ItemStack.isEmpty(armor[slot])) continue;
            MobModel am = armorModel(armor[slot].item.armorMaterial);
            am.copyPose(body);
            glActiveTexture(GL_TEXTURE0);
            am.skin.bind();
            wr.batch.begin(GL_TRIANGLES);
            am.render(wr.batch, mat, tint, 1 << slot);
            wr.batch.end();
        }
    }

    /** An item held in the right hand of a posed biped. */
    private void renderHandItem(Item item, MobModel body, Matrix4f mat, WorldRenderer wr) {
        if (item == null) return;
        // Models face +Z, so their right hand is the -X arm ("armL"). Sprites stand in the arm's y-z plane with the
        // handle in the fist and the tip pointing forward and up.
        Matrix4f hand = body.partMatrix(mat, "armL").translate(0, -11, 0);
        if (ItemRenderer.isCube(item)) hand.translate(0, 0, -1).scale(6f);
        else hand.rotateY((float) Math.toRadians(-90)).scale(10f).translate(0.3f, 0.3f, 0);
        items.bindFor(item, wr);
        wr.batch.begin(GL_TRIANGLES);
        items.emit(wr.batch, hand, item, ItemRenderer.tint(item));
        wr.batch.end();
    }

    /** The local player in third-person view. swing = attack swing progress 0-1. */
    public void renderPlayer(Player p, WorldRenderer wr, float pt, float light, float swing, Item held) {
        if (playerModel == null) playerModel = MobModel.createPlayer();
        MobModel model = playerModel;
        double ex = p.interpX(pt) - wr.camX, ey = p.interpY(pt) - wr.camY, ez = p.interpZ(pt) - wr.camZ;
        float yaw = lerpAngle(p.prevYaw, p.yaw, pt);
        float s = p.limbSwing - p.limbSwingAmount * (1 - pt);
        float amount = Math.min(1, p.prevLimbSwingAmount + (p.limbSwingAmount - p.prevLimbSwingAmount) * pt);
        float legA = (float) Math.cos(s * 0.6662) * 1.4f * amount, legB = (float) Math.cos(s * 0.6662 + Math.PI) * 1.4f * amount;
        for (MobModel.Part part : model.parts) part.rx = part.ry = part.rz = 0;
        model.get("head").rx = (float) Math.toRadians(p.prevPitch + (p.pitch - p.prevPitch) * pt);
        model.get("legL").rx = legA;
        model.get("legR").rx = legB;
        model.get("armR").rx = legB * 0.8f;
        model.get("armL").rx = legA * 0.8f - (float) Math.sin(swing * Math.PI) * 1.4f - (held != null ? 0.3f : 0);
        model.get("armL").rz = -0.05f;
        model.get("armR").rz = 0.05f;
        if (p.sneaking) {
            model.get("body").rx = 0.5f;
            model.get("head").rx += 0.3f;
        }
        Matrix4f mat = new Matrix4f().translate((float) ex, (float) ey - (p.sneaking ? 0.2f : 0), (float) ez)
                .rotateY((float) Math.toRadians(-yaw));
        if (p.deathTime > 0) mat.rotateZ((float) (Math.min(1, Math.sqrt((p.deathTime + pt - 1) / 20f * 1.6f)) * Math.PI / 2));
        mat.scale(1 / 16f * 0.9375f);
        int tint = p.hurtTime > 0 || p.isDead() ? 0xFF8080 : 0xFFFFFF;
        wr.setupBasic(wr.projView, true, 0, 0.1f);
        wr.basicShader.set("uLight", light);
        glDisable(GL_CULL_FACE);
        glActiveTexture(GL_TEXTURE0);
        model.skin.bind();
        wr.batch.begin(GL_TRIANGLES);
        model.render(wr.batch, mat, tint);
        wr.batch.end();
        renderArmor(p.inventory.armor, model, mat, wr, tint);
        renderHandItem(held, model, mat, wr);
        glEnable(GL_CULL_FACE);
    }

    /** Inventory preview: the player (with armor) turned towards the mouse. Size is the model height in GUI pixels. */
    public void renderPlayerGui(Gui gui, Player p, float cx, float bottom, float size, float lookX, float lookY) {
        if (playerModel == null) playerModel = MobModel.createPlayer();
        MobModel model = playerModel;
        for (MobModel.Part part : model.parts) part.rx = part.ry = part.rz = 0;
        model.get("head").ry = (float) Math.atan(lookX / 40f) * -0.8f;
        model.get("head").rx = (float) Math.atan(lookY / 40f) * 0.6f;
        model.get("armL").rz = -0.1f;
        model.get("armR").rz = 0.1f;
        float s = size / 32f;
        Matrix4f m = new Matrix4f().translate(cx, bottom, 50).scale(s, -s, s)
                .rotateY((float) Math.atan(lookX / 40f) * -0.4f).rotateX((float) Math.atan(lookY / 40f) * 0.2f);
        gui.model(model.skin, m, b -> model.render(b, new Matrix4f(), 0xFFFFFF));
        for (int slot = 0; slot < 4; slot++) {
            if (mc.item.ItemStack.isEmpty(p.inventory.armor[slot])) continue;
            MobModel am = armorModel(p.inventory.armor[slot].item.armorMaterial);
            am.copyPose(model);
            int mask = 1 << slot;
            gui.model(am.skin, m, b -> am.render(b, new Matrix4f(), 0xFFFFFF, mask));
        }
    }

    /** A flat item sprite that always faces the camera. */
    private void renderSprite(Item item, WorldRenderer wr, float pt, double ex, double ey, double ez, float light, float size) {
        renderSprite(item, 0, wr, pt, ex, ey, ez, light, size);
    }

    private void renderSprite(Item item, int meta, WorldRenderer wr, float pt, double ex, double ey, double ez, float light, float size) {
        wr.setupBasic(wr.projView, true, 0, 0.1f);
        wr.basicShader.set("uLight", light);
        items.bindFor(item, wr);
        Matrix4f m = new Matrix4f().translate((float) ex, (float) ey + 0.1f, (float) ez)
                .rotateY((float) Math.toRadians(-wr.camYaw + 180)).rotateX((float) Math.toRadians(-wr.camPitch)).scale(size * 2);
        wr.batch.begin(GL_TRIANGLES);
        items.emit(wr.batch, m, item, ItemRenderer.tint(item));
        items.emitPotion(wr.batch, m, item, meta);
        wr.batch.end();
    }

    private void renderOrb(XpOrbEntity o, WorldRenderer wr, float pt, double ex, double ey, double ez) {
        float t = o.age + pt + o.phase;
        float size = 0.12f + Math.min(0.2f, (float) Math.log(o.value + 1) * 0.05f);
        float g = (float) (Math.sin(t * 0.5) + 1) * 0.5f;
        int color = 0xFF000000 | (int) (128 + g * 127) << 16 | 0xFF << 8 | (int) (g * 60);
        wr.setupBasic(wr.projView, true, 0, 0.1f);
        wr.basicShader.set("uLight", 2.5f);
        glActiveTexture(GL_TEXTURE0);
        items.itemAtlas.bind();
        Matrix4f m = new Matrix4f().translate((float) ex, (float) ey + 0.15f + (float) Math.sin(t * 0.1) * 0.05f, (float) ez)
                .rotateY((float) Math.toRadians(-wr.camYaw + 180)).rotateX((float) Math.toRadians(-wr.camPitch)).scale(size);
        int tile = ItemTextureGen.XP_ORB;
        float u0 = (tile & 15) / 16f, v0 = (tile >> 4) / 16f, u1 = u0 + 1 / 16f, v1 = v0 + 1 / 16f;
        org.joml.Vector3f a = m.transformPosition(new org.joml.Vector3f(-1, 1, 0)), b = m.transformPosition(new org.joml.Vector3f(-1, -1, 0));
        org.joml.Vector3f c = m.transformPosition(new org.joml.Vector3f(1, -1, 0)), d = m.transformPosition(new org.joml.Vector3f(1, 1, 0));
        wr.batch.begin(GL_TRIANGLES);
        wr.batch.quad(a.x, a.y, a.z, u0, v0, b.x, b.y, b.z, u0, v1, c.x, c.y, c.z, u1, v1, d.x, d.y, d.z, u1, v0, color);
        wr.batch.end();
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
            else if (e instanceof XpOrbEntity o) renderOrb(o, wr, pt, ex, ey, ez);
            else if (e instanceof ThrownEntity t) renderSprite(t.item, t.potionMeta, wr, pt, ex, ey, ez, l, 0.25f);
            else if (e instanceof FireballEntity) renderSprite(Item.FIRE_CHARGE, wr, pt, ex, ey + 0.3, ez, 3f, ((FireballEntity) e).small ? 0.3f : 0.6f);
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
        for (MobModel.Part p : model.parts) p.reset();
        MobModel.Part head = model.get("head");
        if (head != null) {
            head.ry = (float) Math.toRadians(-headYaw);
            head.rx = (float) Math.toRadians(m.pitch);
        }
        Matrix4f mat = new Matrix4f().translate((float) ex, (float) ey, (float) ez).rotateY((float) Math.toRadians(-bodyYaw));
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
            case ZOMBIE_PIGMAN -> {
                model.get("legL").rx = legA;
                model.get("legR").rx = legB;
                boolean angry = m.angerTicks > 0;
                model.get("armL").rx = angry ? (float) -Math.PI / 2 : legB * 0.8f;
                model.get("armR").rx = angry ? (float) -Math.PI / 2 : legA * 0.8f;
            }
            case GHAST -> {
                model.get("angry").visible = m.ghastCharge > 0;
                for (int i = 0; i < 9; i++) model.get("t" + i).rx = (float) Math.sin(time * 0.1 + i) * 0.2f + 0.2f;
                mat.translate(0, 0.3f, 0);
            }
            case MAGMA_CUBE -> light = Math.max(light, 2.2f);
            case BLAZE -> {
                light = Math.max(light, 2.4f);
                // Three rings of rods spinning in alternating directions and bobbing
                for (int i = 0; i < 12; i++) {
                    int ring = i / 4;
                    float dir = ring == 1 ? -1 : 1;
                    MobModel.Part rod = model.get("rod" + i);
                    rod.ry = (float) (dir * time * 0.05 * (ring + 1) * 0.6 + (i % 4) * Math.PI / 2 + ring * 0.4);
                }
                mat.translate(0, (float) Math.sin(time * 0.1) * 0.08f, 0);
            }
            case ENDERMAN -> {
                model.get("legL").rx = legA * 0.5f;
                model.get("legR").rx = legB * 0.5f;
                boolean carrying = m.carriedBlock != 0;
                model.get("armL").rx = carrying ? -0.5f : legB * 0.5f;
                model.get("armR").rx = carrying ? -0.5f : legA * 0.5f;
                model.get("armL").rz = carrying ? 0.05f : -0.05f;
                model.get("armR").rz = carrying ? -0.05f : 0.05f;
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
            case SQUID -> {
                float tent = m.prevTentacleAngle + (m.tentacleAngle - m.prevTentacleAngle) * pt;
                for (int i = 0; i < 8; i++) {
                    MobModel.Part t = model.get("t" + i);
                    double a = i * Math.PI / 4;
                    t.ry = (float) (-a - Math.PI / 2);
                    t.rx = tent;
                }
                float sp = m.prevSquidPitch + (m.squidPitch - m.prevSquidPitch) * pt;
                mat.translate(0, 0.8f, 0).rotateX((float) Math.toRadians(90 - sp)).translate(0, -0.8f, 0);
            }
            case SLIME -> { }
            case VILLAGER -> {
                model.get("legL").rx = legA * 0.5f;
                model.get("legR").rx = legB * 0.5f;
                model.get("arms").rx = -0.75f;
                model.get("body").tint = model.get("arms").tint = VILLAGER_ROBES[Math.floorMod(m.profession, VILLAGER_ROBES.length)];
            }
            case IRON_GOLEM -> {
                model.get("legL").rx = legA * 0.6f;
                model.get("legR").rx = legB * 0.6f;
                float atk = m.attackAnim > 0 ? (m.attackAnim - pt) / 10f : 0;
                float armSwing = (float) Math.sin(swing * 0.33) * 0.6f * amount;
                model.get("armL").rx = atk > 0 ? -2f * (float) Math.sin(atk * Math.PI) : -armSwing;
                model.get("armR").rx = atk > 0 ? -2f * (float) Math.sin(atk * Math.PI) : armSwing;
            }
            case SNOW_GOLEM -> {
                model.get("armL").rz = -0.3f + (float) Math.sin(time * 0.1) * 0.1f;
                model.get("armR").rz = 0.3f - (float) Math.sin(time * 0.1) * 0.1f;
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
        if (m.type == MobType.SHEEP) {
            model.get("wool").visible = model.get("headWool").visible = !m.sheared;
            int c = SHEEP_COLORS[m.sheepColor];
            model.get("wool").tint = model.get("headWool").tint = c;
            model.get("leg0").tint = model.get("leg1").tint = model.get("leg2").tint = model.get("leg3").tint = -1;
            if (m.eatGrassTicks > 0) {
                float e = Math.min(1, Math.min(m.eatGrassTicks, 40 - m.eatGrassTicks) / 4f);
                head.rx = e * 1.2f;
                model.get("headWool").rx = head.rx;
            } else model.get("headWool").rx = head.rx;
            model.get("headWool").ry = head.ry;
        }
        if (m.type == MobType.WOLF) {
            MobModel.Part tail = model.get("tail");
            float hp = m.health / m.maxHealth;
            tail.rx = m.tamed ? 1.6f + hp * 0.8f : m.angerTicks > 0 ? 2.3f : 1.8f;
            tail.ry = m.tamed ? (float) Math.sin(time * 0.6) * 0.3f * hp : 0;
            model.get("collar").visible = m.tamed;
            model.get("collar").tint = 0xd02020;
            if (m.sitting) {
                // Front raised, hind legs folded forward, front legs straight down
                mat.translate(0, -0.18f, -0.12f).rotateX((float) Math.toRadians(-35));
                model.get("leg0").rx = model.get("leg1").rx = -0.96f;
                model.get("leg2").rx = model.get("leg3").rx = 0.61f;
                tail.rx = 1.1f;
                if (head != null) head.rx -= 0.6f;
            }
        }
        if (m.deathTime > 0) {
            float d = Math.min(1, (float) Math.sqrt((m.deathTime + pt - 1) / 20f * 1.6f));
            mat.rotateZ((float) (d * Math.PI / 2));
        }
        float scale = 1 / 16f;
        if (m.isBaby()) {
            scale *= 0.5f;
            if (head != null) head.scale = 1.5f;
            MobModel.Part hw = model.get("headWool");
            if (hw != null) hw.scale = 1.5f;
        }
        int tint = 0xFFFFFF;
        if (m.type == MobType.CREEPER && m.fuse > 0) {
            float f = (m.prevFuse + (m.fuse - m.prevFuse) * pt) / 30f;
            float s = 1 + (float) Math.sin(f * 100) * f * 0.01f;
            f = Math.min(1, Math.max(0, f));
            f *= f;
            scale *= (1 + f * 0.4f) * s;
            if ((int) (f * 10) % 2 == 0) light *= 1 + f * 2;
        }
        if (m.hurtTime > 0 || m.deathTime > 0) tint = 0xFF8080;
        if (m.type == MobType.GHAST) scale *= 4;
        if (m.type.isSlime()) {
            float sq = m.prevSquish + (m.squish - m.prevSquish) * pt;
            float k = m.slimeSize * 1.02f;
            mat.scale(scale * k / (sq + 1), scale * k * (sq + 1), scale * k / (sq + 1));
        } else mat.scale(scale);
        wr.setupBasic(wr.projView, true, 0, 0.1f);
        wr.basicShader.set("uLight", light);
        glActiveTexture(GL_TEXTURE0);
        model.skin.bind();
        if (m.type == MobType.SLIME) {
            // Opaque core first, then the translucent jelly
            model.get("outer").visible = false;
            wr.batch.begin(GL_TRIANGLES);
            model.render(wr.batch, mat, tint);
            wr.batch.end();
            model.get("outer").visible = true;
            model.get("inner").visible = false;
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            wr.basicShader.set("uAlphaCut", 0.01f);
            wr.batch.begin(GL_TRIANGLES);
            model.render(wr.batch, mat, tint);
            wr.batch.end();
            glDisable(GL_BLEND);
            return;
        }
        wr.batch.begin(GL_TRIANGLES);
        model.render(wr.batch, mat, tint);
        wr.batch.end();
        if (m.type.isUndead()) {
            renderArmor(m.armor, model, mat, wr, tint);
            if (m.type == MobType.SKELETON) renderHandItem(Item.BOW, model, mat, wr);
        }
        if (m.type == MobType.ZOMBIE_PIGMAN) renderHandItem(Item.GOLDEN_SWORD, model, mat, wr);
        if (m.type == MobType.ENDERMAN && m.carriedBlock != 0) {
            Item carried = Item.get(m.carriedBlock);
            if (carried != null) {
                Matrix4f bm = new Matrix4f(mat).translate(0, 24, 9).scale(8f);
                items.bindFor(carried, wr);
                wr.batch.begin(GL_TRIANGLES);
                items.emit(wr.batch, bm, carried, ItemRenderer.tint(carried));
                wr.batch.end();
            }
        }
    }

    private static final int[] SHEEP_COLORS = {0xFFFFFF, 0x404048, 0xf0d040, 0xd05050, 0x6060e0};
    /** Robe colours: farmer, librarian, priest, smith, butcher. */
    private static final int[] VILLAGER_ROBES = {0x8a6a44, 0xf0f0f0, 0x8a4ab0, 0x3a3a3a, 0xe0d8d0};

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
            items.emitPotion(wr.batch, m, item, it.stack.damage);
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
