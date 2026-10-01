package mc.client;

import mc.entity.*;
import mc.item.Item;
import mc.item.ItemStack;
import mc.item.Enchantment;
import mc.util.AABB;
import mc.util.RayCast;
import mc.world.Block;
import mc.world.BlockEntity;
import mc.world.Chunk;
import mc.world.Liquids;
import mc.world.Shapes;
import mc.world.Portal;
import mc.world.Rails;
import mc.world.Redstone;
import mc.world.World;

import java.util.List;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.*;

/** Everything the player does with the mouse: mining, placing, combat and using items. */
final class Interaction {
    private final Game g;
    RayCast.Hit hit;
    LivingEntity targetEntity;
    /** Minecart or boat under the crosshair. */
    Vehicle targetVehicle;
    float breakProgress;
    private int breakX, breakY, breakZ = Integer.MIN_VALUE;
    private int breakDelay, placeDelay, hitSoundTimer;
    float swing, prevSwing;
    private int swingTicks = -1;
    float equip, prevEquip;
    Item shownItem;
    private int shownSlot = -1;
    int useTicks, useType;
    private boolean lastUseWasPlace;
    private final Random random = new Random();

    Interaction(Game g) {
        this.g = g;
    }

    private Player p() { return g.player; }

    private World w() { return g.world; }

    ItemStack held() { return p().inventory.held(); }

    double reach() { return p().creative ? 5.0 : 4.5; }

    /** Per-frame target selection (blocks and entities under the crosshair). */
    void pick(float pt) {
        Player p = p();
        double ry = Math.toRadians(p.yaw), rp = Math.toRadians(p.pitch);
        double dx = -Math.sin(ry) * Math.cos(rp), dy = -Math.sin(rp), dz = Math.cos(ry) * Math.cos(rp);
        double ex = p.interpX(pt), ey = p.interpY(pt) + p.eyeHeight, ez = p.interpZ(pt);
        hit = RayCast.cast(w(), ex, ey, ez, dx, dy, dz, reach());
        targetEntity = null;
        targetVehicle = null;
        double best = hit != null ? hit.distance : reach();
        double entityReach = p.creative ? 5 : 3;
        List<Entity> candidates = new java.util.ArrayList<>(w().entities());
        candidates.addAll(w().networkPlayers());
        for (Entity e : candidates) {
            if (e instanceof Vehicle v && !v.removed && v != p.vehicle) {
                AABB b = v.box();
                double t = rayBox(ex, ey, ez, dx, dy, dz, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
                if (t >= 0 && t < best && t <= entityReach) {
                    best = t;
                    targetVehicle = v;
                    targetEntity = null;
                }
                continue;
            }
            if (!(e instanceof LivingEntity le) || le.isDead() || e == p.vehicle || e.vehicle == p.vehicle && p.vehicle != null) continue;
            AABB b = e.box();
            double t = rayBox(ex, ey, ez, dx, dy, dz, b.minX - 0.1, b.minY - 0.1, b.minZ - 0.1, b.maxX + 0.1, b.maxY + 0.1, b.maxZ + 0.1);
            if (t >= 0 && t < best && t <= entityReach) {
                best = t;
                targetEntity = le;
                targetVehicle = null;
            }
        }
        if (targetEntity != null || targetVehicle != null) hit = null;
    }

    static double rayBox(double ox, double oy, double oz, double dx, double dy, double dz,
                         double x0, double y0, double z0, double x1, double y1, double z1) {
        double tmin = 0, tmax = 1e9;
        double[] o = {ox, oy, oz}, d = {dx, dy, dz}, mn = {x0, y0, z0}, mx = {x1, y1, z1};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-9) {
                if (o[i] < mn[i] || o[i] > mx[i]) return -1;
            } else {
                double t1 = (mn[i] - o[i]) / d[i], t2 = (mx[i] - o[i]) / d[i];
                if (t1 > t2) { double t = t1; t1 = t2; t2 = t; }
                tmin = Math.max(tmin, t1);
                tmax = Math.min(tmax, t2);
                if (tmin > tmax) return -1;
            }
        }
        return tmin;
    }

    void startSwing() {
        if (swingTicks < 0 || swingTicks >= 3) {
            swingTicks = 0;
            if (g.multiplayer != null) g.multiplayer.swing();
            if (g.lanServer != null) g.lanServer.hostSwing();
        }
    }

    /** Left click pressed this frame. */
    void attackClick() {
        startSwing();
        breakDelay = 0;
        if (targetEntity != null) attack(targetEntity);
        else if (targetVehicle != null) hitVehicle(targetVehicle);
    }

    private void attack(LivingEntity target) {
        Player p = p();
        ItemStack h = held();
        float dmg = ItemStack.isEmpty(h) ? 1 : h.item.attackDamage;
        boolean crit = p.fallDistance > 0 && !p.onGround && !p.inWater && p.motionY < 0;
        if (crit) dmg *= 1.5f;
        int str = p.amplifier(mc.entity.Effect.STRENGTH), weak = p.amplifier(mc.entity.Effect.WEAKNESS);
        if (str >= 0) dmg += 3 * (str + 1);
        if (weak >= 0) dmg = Math.max(0, dmg - 4 * (weak + 1));
        int sharp = ItemStack.level(h, Enchantment.SHARPNESS), smite = ItemStack.level(h, Enchantment.SMITE);
        if (sharp > 0) dmg += 0.5f * sharp + 0.5f;
        if (smite > 0 && target instanceof Mob um && um.type.isUndead()) dmg += 2.5f * smite;
        if (target instanceof Mob lm) lm.lootingBonus = ItemStack.level(h, Enchantment.LOOTING);
        int kb = ItemStack.level(h, Enchantment.KNOCKBACK);
        int fire = ItemStack.level(h, Enchantment.FIRE_ASPECT);
        boolean remote = g.multiplayer != null;
        if (remote) {
            // The server applies the hit; only the attacker's own effects happen here
            if (target.hurtTime > 0) return;
            g.multiplayer.attack(target, dmg, 0.5f * kb + (p.sprinting ? 0.5f : 0), fire > 0 ? 80 * fire : 0, ItemStack.level(h, Enchantment.LOOTING));
        }
        if (remote || target.damage(DamageSource.ATTACK, dmg, p)) {
            if (!remote) {
                w().commandWolves(target);
                if (kb > 0) {
                    double r = Math.toRadians(p.yaw);
                    target.knockback(0.5 * kb, Math.sin(r), -Math.cos(r));
                }
                if (fire > 0 && !target.fireImmune()) target.fireTicks = Math.max(target.fireTicks, 80 * fire);
            }
            if (sharp + smite > 0) for (int i = 0; i < 6; i++) g.particles.spawn("magic", target.x, target.y + target.height * 0.7, target.z);
            if (p.sprinting) {
                double r = Math.toRadians(p.yaw);
                if (!remote) target.knockback(0.5, Math.sin(r), -Math.cos(r));
                p.sprinting = false;
            }
            if (crit) for (int i = 0; i < 12; i++) g.particles.spawn("crit", target.x, target.y + target.height * 0.7, target.z);
            g.sound.play("hit", target.x, target.y, target.z, 0.7f, 1f);
            if (!ItemStack.isEmpty(h) && h.item.isTool() && !p.creative) {
                if (h.damageTool(h.item.tool == Item.Tool.SWORD ? 1 : 2)) breakHeld();
            }
            p.addExhaustion(0.1f);
        }
    }

    /** Punching a minecart or boat: a few hits break it (one in creative) and drop it as an item. */
    private void hitVehicle(Vehicle v) {
        ItemStack h = held();
        float dmg = ItemStack.isEmpty(h) ? 1 : h.item.attackDamage;
        g.sound.play("hit", v.x, v.y, v.z, 0.6f, 1.2f);
        if (g.multiplayer != null) {
            g.multiplayer.attack(v, dmg, 0, 0, 0);
            return;
        }
        v.hit(p(), dmg);
    }

    private void breakHeld() {
        g.sound.playUi("hit", 1, 0.6f);
        p().inventory.cleanup();
    }

    /** Right click pressed this frame. */
    void useClick() {
        placeDelay = 0;
        use(true);
    }

    void tick(boolean play) {
        prevSwing = swing;
        if (swingTicks >= 0) {
            swingTicks++;
            if (swingTicks >= 6) { swingTicks = -1; swing = 0; prevSwing = 0; }
            else swing = swingTicks / 6f;
        }
        prevEquip = equip;
        ItemStack h = held();
        Item cur = ItemStack.isEmpty(h) ? null : h.item;
        if (cur != shownItem || p().inventory.selected != shownSlot) {
            equip = Math.min(1, equip + 0.4f);
            if (equip >= 1) { shownItem = cur; shownSlot = p().inventory.selected; }
        } else equip = Math.max(0, equip - 0.4f);
        if (breakDelay > 0) breakDelay--;
        if (placeDelay > 0) placeDelay--;
        if (!play || p().isDead()) {
            breakProgress = 0;
            if (useType == 2) releaseBow();
            useType = 0;
            return;
        }
        Input in = g.input;
        tickMining(in.button(GLFW_MOUSE_BUTTON_LEFT));
        boolean useHeld = in.button(GLFW_MOUSE_BUTTON_RIGHT);
        if (useType != 0) {
            if (!useHeld || ItemStack.isEmpty(h) || (useType == 1 && !h.item.isFood() && h.item != Item.MILK_BUCKET && h.item != Item.POTION) || (useType == 2 && h.item != Item.BOW)) {
                if (useType == 2) releaseBow();
                useType = 0;
                useTicks = 0;
            } else {
                useTicks++;
                if (useType == 1) tickEating(h);
            }
        } else if (useHeld && placeDelay == 0 && lastUseWasPlace) {
            use(false);
        }
    }

    private void tickEating(ItemStack h) {
        Player p = p();
        if (useTicks > 7 && useTicks % 4 == 0) {
            g.sound.play("eat", p.x, p.y + 1.5, p.z, 0.5f, 0.9f + random.nextFloat() * 0.2f);
            double r = Math.toRadians(p.yaw);
            for (int i = 0; i < 3; i++) g.particles.spawn("poof", p.x - Math.sin(r) * 0.4, p.eyeY() - 0.2, p.z + Math.cos(r) * 0.4);
        }
        if (useTicks >= 32 && h.item == Item.POTION) {
            mc.item.Potions.apply(p, h.damage, 1);
            if (!p.creative) {
                if (h.count <= 1) p.inventory.setHeld(new ItemStack(Item.GLASS_BOTTLE, 1));
                else { h.count--; giveOrDrop(new ItemStack(Item.GLASS_BOTTLE, 1)); }
            }
            g.sound.play("burp", p.x, p.y + 1.5, p.z, 0.5f, 1.3f);
            useType = 0;
            useTicks = 0;
            return;
        }
        if (useTicks >= 32 && h.item == Item.MILK_BUCKET) {
            p.fireTicks = 0;
            p.clearEffects();
            if (!p.creative) p.inventory.setHeld(new ItemStack(Item.BUCKET, 1));
            g.sound.play("burp", p.x, p.y + 1.5, p.z, 0.5f, 1.2f);
            useType = 0;
            useTicks = 0;
            return;
        }
        if (useTicks >= 32) {
            p.eat(h.item.food, h.item.saturation);
            if (h.item == Item.GOLDEN_APPLE) {
                p.addEffect(mc.entity.Effect.REGENERATION, 1, 100);
                p.addEffect(mc.entity.Effect.ABSORPTION, 0, 2400);
            }
            if (h.item == Item.SPIDER_EYE) p.addEffect(mc.entity.Effect.POISON, 0, 100);
            if (h.item == Item.PUFFERFISH) {
                p.addEffect(mc.entity.Effect.POISON, 3, 1200);
                p.food = Math.max(0, p.food - 3);
            }
            if (h.item == Item.MUSHROOM_STEW && !p.creative) {
                p.inventory.setHeld(new ItemStack(Item.BOWL, 1));
                g.sound.play("burp", p.x, p.y + 1.5, p.z, 0.5f, 0.9f + random.nextFloat() * 0.1f);
                useType = 0;
                useTicks = 0;
                return;
            }
            if (h.item == Item.ROTTEN_FLESH && random.nextInt(5) < 4) p.food = Math.max(0, p.food - 1);
            if (!p.creative) h.count--;
            p.inventory.cleanup();
            g.sound.play("burp", p.x, p.y + 1.5, p.z, 0.5f, 0.9f + random.nextFloat() * 0.1f);
            useType = 0;
            useTicks = 0;
        }
    }

    float bowPower() {
        float f = useTicks / 20f;
        f = (f * f + f * 2) / 3;
        return Math.min(1, f);
    }

    private void releaseBow() {
        Player p = p();
        float power = bowPower();
        useTicks = 0;
        if (power < 0.1f) return;
        int slot = -1;
        for (int i = 0; i < 36; i++) if (p.inventory.slots[i] != null && p.inventory.slots[i].item == Item.ARROW) { slot = i; break; }
        if (slot < 0 && !p.creative) return;
        ArrowEntity a = new ArrowEntity(p);
        a.setPos(p.x, p.eyeY() - 0.1, p.z);
        double ry = Math.toRadians(p.yaw), rp = Math.toRadians(p.pitch);
        a.shoot(-Math.sin(ry) * Math.cos(rp), -Math.sin(rp), Math.cos(ry) * Math.cos(rp), power * 3, 1);
        ItemStack bowStack = held();
        boolean infinity = ItemStack.level(bowStack, Enchantment.INFINITY) > 0;
        a.pickup = !p.creative && !infinity;
        if (power >= 1) a.damage = 2.5f;
        int pw = ItemStack.level(bowStack, Enchantment.POWER);
        if (pw > 0) a.damage += 0.5f * pw + 0.5f;
        a.punch = ItemStack.level(bowStack, Enchantment.PUNCH);
        if (ItemStack.level(bowStack, Enchantment.FLAME) > 0) a.fireTicks = 2000;
        g.world.addEntity(a);
        g.sound.play("bow", p.x, p.y + 1.5, p.z, 1, 1.2f + power * 0.5f);
        if (!p.creative) {
            if (!infinity) p.inventory.slots[slot].count--;
            ItemStack bow = held();
            if (bow != null && bow.damageTool(1)) breakHeld();
            p.inventory.cleanup();
        }
    }

    // ------------------------------------------------------------------ mining

    private void tickMining(boolean attack) {
        Player p = p();
        if (!attack || hit == null) {
            breakProgress = 0;
            breakZ = Integer.MIN_VALUE;
            return;
        }
        if (breakDelay > 0) return;
        Block b = Block.get(w().getBlock(hit.x, hit.y, hit.z));
        if (hit.x != breakX || hit.y != breakY || hit.z != breakZ) {
            breakX = hit.x; breakY = hit.y; breakZ = hit.z;
            breakProgress = 0;
        }
        startSwing();
        if (b == Block.REDSTONE_ORE) w().setBlock(hit.x, hit.y, hit.z, Block.LIT_REDSTONE_ORE.id, 0, false);
        if (p.creative) {
            ItemStack h = held();
            if (!ItemStack.isEmpty(h) && h.item.tool == Item.Tool.SWORD) return;
            destroy(hit.x, hit.y, hit.z, false);
            breakDelay = 5;
            return;
        }
        if (b.hardness < 0) return;
        float speed = breakSpeed(b);
        breakProgress += speed;
        if (hitSoundTimer++ % 4 == 0) {
            g.sound.hit(b, hit.x + 0.5, hit.y + 0.5, hit.z + 0.5);
            g.particles.spawnHit(hit.x, hit.y, hit.z, hit.nx, hit.ny, hit.nz, b, g.tintOf(b, hit.x, hit.z), g.lightAt(hit.x + hit.nx, hit.y + hit.ny, hit.z + hit.nz));
        }
        if (breakProgress >= 1) {
            destroy(hit.x, hit.y, hit.z, true);
            breakProgress = 0;
            breakDelay = 5;
        }
    }

    /** Minecraft's dig speed: tool speed / hardness / (30 if harvestable else 100), slowed underwater or airborne. */
    float breakSpeed(Block b) {
        if (b.hardness == 0) return 1;
        Player p = p();
        ItemStack h = held();
        Item t = ItemStack.isEmpty(h) ? null : h.item;
        int required = Item.requiredTier(b);
        boolean canHarvest = required < 0 || (t != null && t.tool == Item.Tool.PICKAXE && t.tier >= required);
        float speed = 1;
        if (t != null && t.tool != Item.Tool.NONE && t.tool == Item.effectiveTool(b)) speed = t.miningSpeed;
        int eff = ItemStack.level(h, Enchantment.EFFICIENCY);
        if (eff > 0 && speed > 1) speed += eff * eff + 1;
        if (t != null && t.tool == Item.Tool.SWORD) speed = 1.5f;
        if (t == Item.SHEARS && (b.tint == Block.Tint.FOLIAGE || b.tint == Block.Tint.BIRCH || b.tint == Block.Tint.SPRUCE)) speed = 15;
        if (t == Item.SHEARS && b.sound == Block.SoundType.CLOTH && b.shape == Block.Shape.NONE) speed = 5;
        int haste = p.amplifier(mc.entity.Effect.HASTE);
        if (haste >= 0) speed *= 1 + 0.2f * (haste + 1);
        float s = speed / b.hardness / (canHarvest ? 30f : 100f);
        if (p.eyeInBlock(Block.WATER.id)) s /= 5;
        if (!p.onGround && !p.flying) s /= 5;
        return s;
    }

    private void destroy(int x, int y, int z, boolean survival) {
        Player p = p();
        int id = w().getBlock(x, y, z);
        Block b = Block.get(id);
        if (b == Block.AIR) return;
        ItemStack h = held();
        w().breakBlock(x, y, z, h, survival);
        if (survival) {
            p.addExhaustion(0.005f);
            if (!ItemStack.isEmpty(h) && h.item.isTool() && b.hardness > 0) {
                if (h.damageTool(h.item.tool == Item.Tool.SWORD ? 2 : 1)) breakHeld();
            }
        }
        // Ice leaves water behind
        if (b == Block.ICE && survival && Block.get(w().getBlock(x, y - 1, z)).solid) w().setBlock(x, y, z, Block.WATER.id);
    }

    // ------------------------------------------------------------------ using items

    private void consume(ItemStack h) {
        if (!p().creative) {
            h.count--;
            p().inventory.cleanup();
        }
    }

    private void use(boolean fresh) {
        Player p = p();
        ItemStack h = held();
        lastUseWasPlace = false;
        if (fresh && targetVehicle != null) {
            // Get in (an empty vehicle; whoever rides it stays)
            if (p.vehicle == null && targetVehicle.passenger == null) {
                if (g.multiplayer != null) g.multiplayer.interact(targetVehicle, h == null ? null : h.copy());
                else targetVehicle.mount(p);
                startSwing();
            }
            return;
        }
        if (fresh && targetEntity instanceof Mob v && v.type == MobType.VILLAGER && !v.isBaby()) {
            g.screens.openTrading(v);
            return;
        }
        if (fresh && targetEntity instanceof Mob m && g.multiplayer != null) {
            // Shearing, taming, breeding, milking... happen on the server, which sends back the held item
            g.multiplayer.interact(m, h == null ? null : h.copy());
            startSwing();
            return;
        }
        if (fresh && targetEntity instanceof Mob m && m.interact(p)) {
            startSwing();
            return;
        }
        // Interactive blocks first (sneak to place against them instead)
        if (hit != null && !p.sneaking && fresh) {
            int id = w().getBlock(hit.x, hit.y, hit.z);
            if (id == Block.CRAFTING_TABLE.id) { g.screens.openCrafting(); startSwing(); return; }
            if (id == Block.ENCHANTING_TABLE.id) { g.screens.openEnchanting(hit.x, hit.y, hit.z); return; }
            if (id == Block.ANVIL.id) { g.screens.openAnvil(); return; }
            if (id == Block.BREWING_STAND.id) {
                g.screens.openBrewing((BlockEntity.BrewingStand) w().getOrCreateBlockEntity(hit.x, hit.y, hit.z));
                return;
            }
            if (id == Block.LEVER.id) { Redstone.toggleLever(w(), hit.x, hit.y, hit.z); startSwing(); return; }
            if (Block.get(id).shape == Block.Shape.BUTTON) { Redstone.pressButton(w(), hit.x, hit.y, hit.z); startSwing(); return; }
            if (id == Block.REPEATER.id || id == Block.POWERED_REPEATER.id) {
                int m = w().getMeta(hit.x, hit.y, hit.z);
                w().setBlock(hit.x, hit.y, hit.z, id, (m & 3) | ((((m >> 2) + 1) & 3) << 2), false);
                g.sound.play("click", hit.x + 0.5, hit.y + 0.5, hit.z + 0.5, 0.3f, 0.55f);
                startSwing();
                return;
            }
            if (id == Block.IRON_DOOR.id) return;
            if (toggle(hit.x, hit.y, hit.z)) return;
            if (id == Block.BED.id && w().dimension == mc.world.Dimension.NETHER) {
                w().breakBlock(hit.x, hit.y, hit.z, null, false);
                w().explode(hit.x + 0.5, hit.y + 0.5, hit.z + 0.5, 5, null);
                return;
            }
            if (id == Block.BED.id) { g.sleep(hit.x, hit.y, hit.z); startSwing(); return; }
            if (id == Block.CAKE.id && (p.food < 20 || p.creative)) {
                int bites = w().getMeta(hit.x, hit.y, hit.z);
                p.eat(2, 0.4f);
                if (bites >= 6) w().setBlock(hit.x, hit.y, hit.z, 0);
                else w().setBlock(hit.x, hit.y, hit.z, id, bites + 1, false);
                g.sound.play("eat", hit.x + 0.5, hit.y + 0.5, hit.z + 0.5, 0.5f, 1);
                startSwing();
                return;
            }
            if (id == Block.FURNACE.id || id == Block.LIT_FURNACE.id) {
                g.screens.openFurnace((BlockEntity.Furnace) w().getOrCreateBlockEntity(hit.x, hit.y, hit.z));
                return;
            }
            if (id == Block.CHEST.id) {
                g.screens.openChest((BlockEntity.Chest) w().getOrCreateBlockEntity(hit.x, hit.y, hit.z));
                return;
            }
            if (id == Block.TNT.id && !ItemStack.isEmpty(h) && h.item == Item.FLINT_AND_STEEL) {
                w().setBlock(hit.x, hit.y, hit.z, 0);
                TntEntity t = new TntEntity(80);
                t.setPos(hit.x + 0.5, hit.y, hit.z + 0.5);
                w().addEntity(t);
                w().playSound("fuse", hit.x, hit.y, hit.z, 1, 1);
                if (!p.creative && h.damageTool(1)) breakHeld();
                startSwing();
                return;
            }
        }
        if (ItemStack.isEmpty(h)) return;
        Item item = h.item;
        if ((item == Item.CARROT || item == Item.POTATO) && hit != null && w().getBlock(hit.x, hit.y, hit.z) == Block.FARMLAND.id && hit.ny == 1) {
            // Planting takes precedence over eating
        } else if (item.isFood()) {
            if (fresh && (p.food < 20 || p.creative)) { useType = 1; useTicks = 0; }
            return;
        }
        if (item == Item.BOW) {
            if (fresh && (p.creative || p.inventory.count(Item.ARROW) > 0)) { useType = 2; useTicks = 0; }
            return;
        }
        if (item == Item.EGG || item == Item.SNOWBALL || item == Item.ENDER_PEARL || item == Item.EXPERIENCE_BOTTLE) {
            if (fresh) throwItem(h);
            return;
        }
        if (item == Item.MILK_BUCKET || item == Item.POTION) {
            if (fresh) { useType = 1; useTicks = 0; }
            return;
        }
        if (item == Item.SPLASH_POTION) {
            if (fresh) throwItem(h);
            return;
        }
        if (item == Item.GLASS_BOTTLE) {
            if (fresh) fillBottle(h);
            return;
        }
        if (item == Item.NETHER_WART) {
            if (hit != null && w().getBlock(hit.x, hit.y, hit.z) == Block.SOUL_SAND.id && hit.ny == 1 && w().getBlock(hit.x, hit.y + 1, hit.z) == 0) {
                w().setBlock(hit.x, hit.y + 1, hit.z, Block.NETHER_WART.id);
                g.sound.dig(Block.TALL_GRASS, hit.x + 0.5, hit.y + 1, hit.z + 0.5);
                consume(h);
                startSwing();
            }
            return;
        }
        if ((item == Item.FLINT_AND_STEEL || item == Item.FIRE_CHARGE) && hit != null) {
            if (fresh) ignite(h);
            return;
        }
        if (item.isArmor()) {
            if (fresh) {
                // Swap with whatever is worn in that slot
                ItemStack worn = p.inventory.armor[item.armorSlot];
                p.inventory.armor[item.armorSlot] = h;
                p.inventory.setHeld(worn);
                g.sound.play("armor", p.x, p.y + 1, p.z, 0.6f, 1);
                startSwing();
            }
            return;
        }
        if (item == Item.BUCKET) { if (fresh) fillBucket(h); return; }
        if (item == Item.BOAT) { if (fresh) placeBoat(h); return; }
        if (item == Item.FISHING_ROD) { if (fresh) useRod(h); return; }
        if (item == Item.MINECART) { if (fresh) placeMinecart(h); return; }
        if (item == Item.WATER_BUCKET || item == Item.LAVA_BUCKET) { if (fresh) emptyBucket(h, item == Item.WATER_BUCKET ? Block.WATER : Block.LAVA); return; }
        if (hit == null) return;
        int target = w().getBlock(hit.x, hit.y, hit.z);
        if (item.tool == Item.Tool.HOE) {
            if ((target == Block.GRASS.id || target == Block.DIRT.id) && hit.ny >= 0 && w().getBlock(hit.x, hit.y + 1, hit.z) == 0) {
                w().setBlock(hit.x, hit.y, hit.z, Block.FARMLAND.id);
                g.sound.dig(Block.DIRT, hit.x + 0.5, hit.y + 1, hit.z + 0.5);
                startSwing();
                if (!p.creative && h.damageTool(1)) breakHeld();
            }
            return;
        }
        if (item == Item.SEEDS || item == Item.CARROT || item == Item.POTATO) {
            Block crop = item == Item.SEEDS ? Block.WHEAT : item == Item.CARROT ? Block.CARROTS : Block.POTATOES;
            if (target == Block.FARMLAND.id && hit.ny == 1 && w().getBlock(hit.x, hit.y + 1, hit.z) == 0) {
                w().setBlock(hit.x, hit.y + 1, hit.z, crop.id);
                g.sound.dig(Block.TALL_GRASS, hit.x + 0.5, hit.y + 1, hit.z + 0.5);
                consume(h);
                startSwing();
            }
            return;
        }
        if (item == Item.REDSTONE) {
            if (hit == null) return;
            Block t = Block.get(target);
            int x = hit.x, y = hit.y, z = hit.z;
            if (!t.replaceable) { x += hit.nx; y += hit.ny; z += hit.nz; }
            if (!Block.get(w().getBlock(x, y, z)).replaceable || !w().sturdyTop(x, y - 1, z)) return;
            w().setBlock(x, y, z, Block.REDSTONE_WIRE.id, 0, true);
            g.sound.dig(Block.STONE, x + 0.5, y, z + 0.5);
            consume(h);
            startSwing();
            lastUseWasPlace = true;
            placeDelay = 4;
            return;
        }
        if (item == Item.BONE_MEAL) {
            if (fresh) boneMeal(h, target);
            return;
        }
        if (item.isBlock()) {
            if (placeBlock(h)) {
                lastUseWasPlace = true;
                placeDelay = 4;
            }
        }
    }

    /** Flint and steel / fire charge: light a portal frame, or set fire to the block face. */
    private void ignite(ItemStack h) {
        Player p = p();
        int x = hit.x + hit.nx, y = hit.y + hit.ny, z = hit.z + hit.nz;
        int here = w().getBlock(x, y, z);
        if (here != 0 && !Block.get(here).replaceable) return;
        boolean lit = Portal.tryLight(w(), x, y, z);
        if (!lit) {
            Block below = Block.get(w().getBlock(x, y - 1, z));
            if (!below.solid && !w().nextToFlammablePublic(x, y, z)) return;
            w().setBlock(x, y, z, Block.FIRE.id);
        }
        g.sound.play("ignite", x + 0.5, y + 0.5, z + 0.5, 1, 0.9f + random.nextFloat() * 0.2f);
        startSwing();
        if (!p.creative) {
            if (h.item == Item.FIRE_CHARGE) consume(h);
            else if (h.damageTool(1)) breakHeld();
        }
    }

    private void giveOrDrop(ItemStack s) {
        ItemStack left = p().inventory.add(s);
        if (left.count > 0) w().spawnItem(p().x, p().y + 1, p().z, left);
    }

    /** Glass bottle on water: a water bottle. */
    private void fillBottle(ItemStack h) {
        Player p = p();
        double ry = Math.toRadians(p.yaw), rp = Math.toRadians(p.pitch);
        double dx = -Math.sin(ry) * Math.cos(rp), dy = -Math.sin(rp), dz = Math.cos(ry) * Math.cos(rp);
        for (double t = 0; t < reach(); t += 0.1) {
            int bx = (int) Math.floor(p.x + dx * t), by = (int) Math.floor(p.eyeY() + dy * t), bz = (int) Math.floor(p.z + dz * t);
            int id = w().getBlock(bx, by, bz);
            if (id == Block.WATER.id) {
                g.sound.play("bucket", bx, by, bz, 0.6f, 1.4f);
                ItemStack water = new ItemStack(Item.POTION, 1, 0);
                if (!p.creative) {
                    if (h.count == 1) p.inventory.setHeld(water);
                    else { h.count--; giveOrDrop(water); }
                } else giveOrDrop(water);
                startSwing();
                return;
            }
            if (id != 0 && Block.get(id).solid) return;
        }
    }

    private void throwItem(ItemStack h) {
        Player p = p();
        ThrownEntity t = new ThrownEntity(h.item, p);
        t.potionMeta = h.damage;
        t.setPos(p.x, p.eyeY() - 0.1, p.z);
        double ry = Math.toRadians(p.yaw), rp = Math.toRadians(p.pitch);
        t.shoot(-Math.sin(ry) * Math.cos(rp), -Math.sin(rp), Math.cos(ry) * Math.cos(rp), 1.5);
        t.motionX += p.motionX;
        t.motionZ += p.motionZ;
        w().addEntity(t);
        g.sound.play("bow", p.x, p.y + 1.5, p.z, 0.5f, 0.4f / (random.nextFloat() * 0.4f + 0.8f));
        consume(h);
        startSwing();
    }

    private void boneMeal(ItemStack h, int target) {
        boolean used = false;
        if (Block.get(target).isCrop()) {
            int age = w().getMeta(hit.x, hit.y, hit.z);
            if (age < 7) { w().setBlock(hit.x, hit.y, hit.z, target, Math.min(7, age + 2 + random.nextInt(4)), false); used = true; }
        } else if (target == Block.SAPLING.id) {
            if (random.nextFloat() < 0.45f) w().growTree(hit.x, hit.y, hit.z);
            used = true;
        } else if (target == Block.GRASS.id) {
            for (int i = 0; i < 24; i++) {
                int x = hit.x + random.nextInt(7) - 3, z = hit.z + random.nextInt(7) - 3;
                if (w().getBlock(x, hit.y, z) == Block.GRASS.id && w().getBlock(x, hit.y + 1, z) == 0)
                    w().setBlock(x, hit.y + 1, z, random.nextInt(8) == 0 ? (random.nextBoolean() ? Block.POPPY.id : Block.DANDELION.id) : Block.TALL_GRASS.id);
            }
            used = true;
        }
        if (used) {
            for (int i = 0; i < 10; i++) g.particles.spawn("happy", hit.x + random.nextDouble(), hit.y + 1 + random.nextDouble() * 0.5, hit.z + random.nextDouble());
            consume(h);
            startSwing();
        }
    }

    private void fillBucket(ItemStack h) {
        Player p = p();
        double ry = Math.toRadians(p.yaw), rp = Math.toRadians(p.pitch);
        double dx = -Math.sin(ry) * Math.cos(rp), dy = -Math.sin(rp), dz = Math.cos(ry) * Math.cos(rp);
        double x = p.x, y = p.eyeY(), z = p.z;
        for (double t = 0; t < reach(); t += 0.1) {
            int bx = (int) Math.floor(x + dx * t), by = (int) Math.floor(y + dy * t), bz = (int) Math.floor(z + dz * t);
            int id = w().getBlock(bx, by, bz);
            if (id == 0) continue;
            Block b = Block.get(id);
            if (b.isLiquid() && w().getMeta(bx, by, bz) == 0) {
                w().setBlock(bx, by, bz, 0);
                g.sound.play("bucket", bx, by, bz, 1, 1);
                Item filled = b == Block.WATER ? Item.WATER_BUCKET : Item.LAVA_BUCKET;
                if (p.creative) return;
                if (h.count == 1) p.inventory.setHeld(new ItemStack(filled, 1));
                else {
                    h.count--;
                    ItemStack left = p.inventory.add(new ItemStack(filled, 1));
                    if (left.count > 0) w().spawnItem(p.x, p.y + 1, p.z, left);
                }
                startSwing();
                return;
            }
            if (b.solid) return;
        }
    }

    /** Our bobber, if one is out (in multiplayer it is the server's copy, recognised by its owner's name). */
    private FishingBobberEntity ownBobber() {
        Player p = p();
        if (g.multiplayer == null) return p.fishing != null && !p.fishing.removed ? p.fishing : null;
        for (Entity e : w().entities()) if (e instanceof FishingBobberEntity f && !f.removed && f.ownerName.equals(p.name)) return f;
        return null;
    }

    /** Casts the line, or reels it in (catching whatever bit, or pulling a hooked mob). */
    private void useRod(ItemStack h) {
        Player p = p();
        FishingBobberEntity out = ownBobber();
        if (out != null) {
            int damage;
            if (g.multiplayer != null) {
                g.multiplayer.reel();
                damage = 1;
            } else damage = out.reel();
            p.fishing = null;
            g.sound.play("bow", p.x, p.y + 1, p.z, 0.4f, 0.6f);
            if (damage > 0 && !p.creative && h.damageTool(damage)) breakHeld();
        } else {
            FishingBobberEntity b = new FishingBobberEntity(p);
            b.world = w();
            b.cast();
            w().addEntity(b);
            if (g.multiplayer == null) p.fishing = b;
            g.sound.play("bow", p.x, p.y + 1, p.z, 0.5f, 0.4f);
        }
        startSwing();
    }

    /** Minecarts go on rails (at the track's height on slopes). */
    private void placeMinecart(ItemStack h) {
        if (hit == null) return;
        int id = w().getBlock(hit.x, hit.y, hit.z);
        if (!Rails.isRail(id)) return;
        boolean slope = Rails.ascending(Rails.shape(id, w().getMeta(hit.x, hit.y, hit.z)));
        MinecartEntity cart = new MinecartEntity();
        cart.setPos(hit.x + 0.5, hit.y + 0.0625 + (slope ? 0.5 : 0), hit.z + 0.5);
        cart.yaw = cart.prevYaw = Math.round(p().yaw / 90f) * 90f;
        w().addEntity(cart);
        g.sound.dig(Block.RAIL, hit.x + 0.5, hit.y, hit.z + 0.5);
        consume(h);
        startSwing();
    }

    /** Boats go on the water surface the player looks at (or on top of a block). */
    private void placeBoat(ItemStack h) {
        Player p = p();
        double ry = Math.toRadians(p.yaw), rp = Math.toRadians(p.pitch);
        double dx = -Math.sin(ry) * Math.cos(rp), dy = -Math.sin(rp), dz = Math.cos(ry) * Math.cos(rp);
        double ex = p.x, ey = p.y + p.eyeHeight, ez = p.z;
        double px = 0, py = 0, pz = 0;
        boolean found = false;
        for (double t = 0; t <= reach(); t += 0.05) {
            double x = ex + dx * t, y = ey + dy * t, z = ez + dz * t;
            int bx = (int) Math.floor(x), by = (int) Math.floor(y), bz = (int) Math.floor(z);
            Block b = Block.get(w().getBlock(bx, by, bz));
            if (b == Block.WATER) { px = x; py = by + 0.6; pz = z; found = true; break; }
            if (b.solid) {
                if (hit != null) { px = hit.px; py = hit.y + 1; pz = hit.pz; found = hit.ny == 1; }
                break;
            }
        }
        if (!found) return;
        BoatEntity boat = new BoatEntity();
        boat.setPos(px, py, pz);
        boat.yaw = boat.prevYaw = p.yaw;
        boat.world = w();
        if (!boat.fitsAnywhere()) return;
        w().addEntity(boat);
        g.sound.dig(Block.PLANKS, px, py, pz);
        consume(h);
        startSwing();
    }

    private void emptyBucket(ItemStack h, Block liquid) {
        if (hit == null) return;
        int x = hit.x, y = hit.y, z = hit.z;
        if (!Block.get(w().getBlock(x, y, z)).replaceable) { x += hit.nx; y += hit.ny; z += hit.nz; }
        Block existing = Block.get(w().getBlock(x, y, z));
        if (!existing.replaceable && !existing.isLiquid()) return;
        if (liquid == Block.WATER && w().dimension == mc.world.Dimension.NETHER) {
            // Water boils away in the Nether
            g.sound.play("fizz", x + 0.5, y + 0.5, z + 0.5, 0.5f, 2.6f);
            for (int i = 0; i < 8; i++) g.particles.spawn("smoke", x + random.nextDouble(), y + random.nextDouble(), z + random.nextDouble());
            if (!p().creative) p().inventory.setHeld(new ItemStack(Item.BUCKET, 1));
            return;
        }
        w().setBlock(x, y, z, liquid.id, 0, true);
        w().scheduleTick(x, y, z, Liquids.delay(liquid.id));
        g.sound.play("bucket", x, y, z, 1, 0.8f);
        if (!p().creative) p().inventory.setHeld(new ItemStack(Item.BUCKET, 1));
        startSwing();
    }

    private static double frac(double v) { return v - Math.floor(v); }

    /** Horizontal facing (0 south, 1 west, 2 north, 3 east) pointing from the block towards the player. */
    private int facingToPlayer() {
        return (Math.floorMod(Math.round(p().yaw / 90f), 4) + 2) & 3;
    }

    private boolean fitsEntities(int x, int y, int z) {
        AABB cell = new AABB(x, y, z, x + 1, y + 1, z + 1);
        if (cell.intersects(p().box())) return false;
        for (Entity e : w().entities()) if (e instanceof LivingEntity && cell.intersects(e.box())) return false;
        return true;
    }

    private boolean replaceableAt(int x, int y, int z) {
        return y >= 0 && y < Chunk.HEIGHT && Block.get(w().getBlock(x, y, z)).replaceable;
    }

    private boolean placeBlock(ItemStack h) {
        Player p = p();
        if (hit == null) return false;
        Block b = h.item.block;
        Block target = Block.get(w().getBlock(hit.x, hit.y, hit.z));
        int targetMeta = w().getMeta(hit.x, hit.y, hit.z);
        // Slab onto the matching half of the same slab: make a double slab
        if (b.isSlab() && target == b && ((targetMeta == 0 && hit.ny == 1) || (targetMeta == 1 && hit.ny == -1))) {
            return finishPlace(h, b, hit.x, hit.y, hit.z, 2);
        }
        // More snow on a snow layer
        if (b.shape == Block.Shape.SNOW_LAYER && target == b && hit.ny == 1) {
            if (targetMeta >= 6) return finishPlace(h, Block.SNOW, hit.x, hit.y, hit.z, 0);
            return finishPlace(h, b, hit.x, hit.y, hit.z, targetMeta + 1);
        }
        int x = hit.x, y = hit.y, z = hit.z;
        if (!target.replaceable) {
            x += hit.nx; y += hit.ny; z += hit.nz;
        }
        if (y < 0 || y >= Chunk.HEIGHT) return false;
        Block existing = Block.get(w().getBlock(x, y, z));
        if (b.isSlab() && existing == b && (w().getMeta(x, y, z) & 3) != 2) {
            if (!fitsEntities(x, y, z)) return false;
            return finishPlace(h, b, x, y, z, 2);
        }
        if (!existing.replaceable) return false;
        Block below = Block.get(w().getBlock(x, y - 1, z));
        boolean upperHalf = hit.ny == -1 || (hit.ny == 0 && frac(hit.py) > 0.5);
        int meta = 0;
        if (b == Block.TORCH || b == Block.REDSTONE_TORCH) {
            if (hit.ny == 1 || (hit.ny == 0 && hit.nx == 0 && hit.nz == 0)) {
                if (!w().sturdyTop(x, y - 1, z)) return false;
            } else if (hit.ny == 0) {
                int wall = Shapes.facingOf(-hit.nx, -hit.nz);
                if (!w().sturdy(x + Shapes.DX[wall], y, z + Shapes.DZ[wall])) return false;
                meta = wall + 1;
            } else return false;
        }
        if (b.model == Block.Model.CROSS) {
            boolean soil = below == Block.GRASS || below == Block.DIRT || below == Block.COARSE_DIRT || below == Block.SNOWY_GRASS || below == Block.FARMLAND
                    || (b == Block.DEAD_BUSH && (below == Block.SAND || below == Block.TERRACOTTA))
                    || (b == Block.SUGAR_CANE && (below == Block.SUGAR_CANE || below == Block.SAND));
            if (!soil) return false;
        }
        if (b == Block.CACTUS && below != Block.SAND && below != Block.CACTUS) return false;
        if (b.solid && b.shape != Block.Shape.CARPET && b.shape != Block.Shape.SNOW_LAYER && !fitsEntities(x, y, z)) return false;
        if (b.hasFacing()) meta = facingToPlayer();
        if (b == Block.OAK_LEAVES || b == Block.BIRCH_LEAVES || b == Block.SPRUCE_LEAVES) meta = 1;
        switch (b.shape) {
            case SLAB -> meta = upperHalf ? 1 : 0;
            case STAIRS -> meta = facingToPlayer() | (upperHalf ? 4 : 0);
            case GATE -> meta = facingToPlayer();
            case TRAPDOOR -> {
                if (hit.ny == 0) meta = Shapes.facingOf(-hit.nx, -hit.nz) | (frac(hit.py) > 0.5 ? 8 : 0);
                else meta = Shapes.opposite(facingToPlayer()) | (hit.ny == -1 ? 8 : 0);
            }
            case LADDER -> {
                if (hit.ny != 0) return false;
                meta = Shapes.facingOf(-hit.nx, -hit.nz);
                if (!Block.get(w().getBlock(x + Shapes.DX[meta], y, z + Shapes.DZ[meta])).opaque) return false;
            }
            case CARPET -> { if (below == Block.AIR || below.isLiquid()) return false; }
            case LEVER, BUTTON -> {
                int sx = x - hit.nx, sy = y - hit.ny, sz = z - hit.nz;
                if (!(hit.ny == 1 ? w().sturdyTop(sx, sy, sz) : w().sturdy(sx, sy, sz))) return false;
                if (hit.ny == 1) meta = 0;
                else if (hit.ny == -1) meta = 5;
                else meta = Shapes.facingOf(-hit.nx, -hit.nz) + 1;
            }
            case PLATE -> { if (!below.solid || below.isLiquid()) return false; }
            case REPEATER -> {
                if (!w().sturdyTop(x, y - 1, z)) return false;
                meta = Shapes.opposite(facingToPlayer());
            }
            case PISTON -> {
                if (p.pitch > 50) meta = 1;
                else if (p.pitch < -50) meta = 0;
                else meta = Shapes.H_TO_6[facingToPlayer()];
            }
            case SNOW_LAYER -> { if (!below.opaque) return false; }
            case RAIL -> {
                if (!w().sturdyTop(x, y - 1, z)) return false;
                meta = Rails.placementShape(w(), x, y, z, b.id, Rails.fromFacing(facingToPlayer()));
                finishPlace(h, b, x, y, z, meta);
                Rails.connectNeighbours(w(), x, y, z);
                if (b == Block.POWERED_RAIL) Rails.updatePowered(w(), x, y, z);
                return true;
            }
            case DOOR -> {
                if (!w().sturdyTop(x, y - 1, z) || !replaceableAt(x, y + 1, z) || !fitsEntities(x, y + 1, z)) return false;
                int f = facingToPlayer();
                // Pair with a door on the left to form a double door
                int left = (f + 1) & 3;
                boolean right = w().getBlock(x + Shapes.DX[left], y, z + Shapes.DZ[left]) == b.id;
                meta = f | (right ? 16 : 0);
                w().setBlock(x, y + 1, z, b.id, meta | 8, false);
                return finishPlace(h, b, x, y, z, meta);
            }
            case BED -> {
                int f = Shapes.opposite(facingToPlayer());
                int hx = x + Shapes.DX[f], hz = z + Shapes.DZ[f];
                if (!below.opaque || !replaceableAt(hx, y, hz) || !Block.get(w().getBlock(hx, y - 1, hz)).opaque || !fitsEntities(hx, y, hz)) return false;
                w().setBlock(hx, y, hz, b.id, f | 4, false);
                return finishPlace(h, b, x, y, z, f);
            }
            default -> { }
        }
        return finishPlace(h, b, x, y, z, meta);
    }

    private boolean finishPlace(ItemStack h, Block b, int x, int y, int z, int meta) {
        w().setBlock(x, y, z, b.id, meta, true);
        g.sound.dig(b, x + 0.5, y + 0.5, z + 0.5);
        startSwing();
        consume(h);
        w().checkFalling(x, y, z);
        if (b == Block.PUMPKIN) w().trySpawnGolem(x, y, z);
        return true;
    }

    /** Right click on doors, gates and trapdoors. Returns true if something was toggled. */
    private boolean toggle(int x, int y, int z) {
        Block b = Block.get(w().getBlock(x, y, z));
        int meta = w().getMeta(x, y, z);
        switch (b.shape) {
            case DOOR -> {
                if (b == Block.IRON_DOOR) return false;
                int other = (meta & 8) != 0 ? y - 1 : y + 1;
                int nm = meta ^ 4;
                w().setBlock(x, y, z, b.id, nm, false);
                if (w().getBlock(x, other, z) == b.id) w().setBlock(x, other, z, b.id, w().getMeta(x, other, z) ^ 4, false);
                g.sound.play((nm & 4) != 0 ? "door_open" : "door_close", x + 0.5, y + 0.5, z + 0.5, 1, 0.9f + random.nextFloat() * 0.1f);
            }
            case GATE -> {
                int nm = meta ^ 4;
                // Opening swings away from the player
                if ((nm & 4) != 0) {
                    int f = facingToPlayer();
                    if ((f & 1) == (meta & 1)) nm = (nm & ~3) | f;
                }
                w().setBlock(x, y, z, b.id, nm, false);
                g.sound.play((nm & 4) != 0 ? "door_open" : "door_close", x + 0.5, y + 0.5, z + 0.5, 1, 1.1f);
            }
            case TRAPDOOR -> {
                w().setBlock(x, y, z, b.id, meta ^ 4, false);
                g.sound.play((meta & 4) == 0 ? "door_open" : "door_close", x + 0.5, y + 0.5, z + 0.5, 1, 1.2f);
            }
            default -> { return false; }
        }
        startSwing();
        return true;
    }

    /** Q drops one item (or the whole stack with ctrl) in front of the player. */
    void drop(boolean all) {
        Player p = p();
        ItemStack h = held();
        if (ItemStack.isEmpty(h)) return;
        ItemStack out = h.split(all ? h.count : 1);
        p.inventory.cleanup();
        throwStack(out);
        startSwing();
    }

    void throwStack(ItemStack out) {
        Player p = p();
        ItemEntity e = new ItemEntity(out);
        e.setPos(p.x, p.eyeY() - 0.3, p.z);
        double ry = Math.toRadians(p.yaw), rp = Math.toRadians(p.pitch);
        e.motionX = -Math.sin(ry) * Math.cos(rp) * 0.3;
        e.motionZ = Math.cos(ry) * Math.cos(rp) * 0.3;
        e.motionY = -Math.sin(rp) * 0.3 + 0.1;
        e.pickupDelay = 40;
        w().addEntity(e);
    }

    void pickBlock() {
        if (hit == null) return;
        int id = w().getBlock(hit.x, hit.y, hit.z);
        if (id == Block.LIT_FURNACE.id) id = Block.FURNACE.id;
        Item it = id == Block.WHEAT.id ? Item.SEEDS : id == Block.CARROTS.id ? Item.CARROT : id == Block.POTATOES.id ? Item.POTATO : Item.get(id);
        if (it == null) return;
        var inv = p().inventory;
        for (int i = 0; i < 9; i++) if (inv.slots[i] != null && inv.slots[i].item == it) { inv.selected = i; return; }
        if (p().creative) inv.slots[inv.selected] = new ItemStack(it, it.maxStack);
        else for (int i = 9; i < 36; i++) if (inv.slots[i] != null && inv.slots[i].item == it) {
            ItemStack t = inv.slots[inv.selected];
            inv.slots[inv.selected] = inv.slots[i];
            inv.slots[i] = t;
            return;
        }
    }
}
