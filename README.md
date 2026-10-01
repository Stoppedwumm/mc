# mc4 — a Minecraft-like voxel game in Java

A from-scratch recreation of Minecraft: Java Edition's look and feel, built on **the same libraries Minecraft uses**:

| Library | Used for | Minecraft uses it for |
|---|---|---|
| [LWJGL 3.3.3](https://www.lwjgl.org/) — GLFW | window, input | same |
| LWJGL — OpenGL (3.3 core) | rendering | same |
| LWJGL — OpenAL | 3D positional sound | same |
| LWJGL — STB | PNG screenshots | same (stb_image etc.) |
| [JOML 1.10.5](https://github.com/JOML-CI/JOML) | matrices, frustum culling | same |
| [fastutil 8.5](https://fastutil.di.unimi.it/) | primitive-keyed chunk maps | same |
| [Gson 2.10](https://github.com/google/gson) | `options.json`, `level.json`, entity data on the wire | same |
| [Netty 4.1](https://netty.io/) | multiplayer networking | same |

All textures and sounds are generated procedurally at startup, so no Mojang assets are included or needed.

## Features

**Menus and multiplayer**
- Title screen over a slowly turning view of a generated landscape (like Minecraft's panorama), with a stone logo and splash texts
- Singleplayer world list: play, create (name, seed, Survival/Creative) and delete worlds; "Save and Quit to Title"
- Options screen with your multiplayer name and video settings
- Multiplayer server list with live status (message of the day, players online, ping bars), Add Server, Direct Connection
- **Open to LAN** from the pause menu: friends on the network see the game in their server list (Minecraft's LAN announcement on 224.0.2.60:4445) and join it
- **Dedicated server** (`--server`): no window, 20 ticks per second, console commands, autosave, saved player data per name
- The server simulates the world (mobs, redstone, water, crops, furnaces); each client moves its own player and sends its edits.
  Other players appear with name tags and armor, and swing their arms. Chunks are streamed compressed. Mobs, items and projectiles are interpolated.
  Mob hits, knockback, potion effects and teleports reach the right player. Item and experience pickups, chests and furnaces are shared, and chat is broadcast.

**Graphics (shader-pack style renderer)**
- **Distant terrain** (level of detail, using techniques from Distant Horizons and Voxy) out to 256 chunks, 4096 blocks, beyond the normal render distance:
  - a quadtree of 32×32-cell tiles whose cells double in size with distance (1 to 128 blocks)
  - tiles are built on background threads, nearest first; a parent tile stays visible until its children are ready
  - places you have visited use real chunk summaries (top block, height, water, the ground under tree crowns), saved per world in `lod/` region files like Voxy's database
  - unexplored land is estimated straight from the terrain noise (biomes, beaches, snow, sea level, forests), like Distant Horizons' fast generator mode
  - drawn in its own pass with a long-range projection before the normal terrain; a per-chunk mask hides it wherever real chunks are drawn
  - columns have cliff walls, skirts that close cracks between detail levels, shading in valleys, floating tree crowns, and water with Fresnel sky reflection that darkens with depth
  - fog and haze stretch to the new view distance, so mountains fade into the atmosphere
- HDR rendering with ACES filmic tonemapping, bloom, vignette and automatic eye adaptation (caves and nights adapt, stepping into daylight is briefly bright)
- Real-time sun/moon shadows (stabilised 2048² shadow map with soft PCF filtering) plus Minecraft-style smooth lighting and ambient occlusion
- Atmospheric sky: blue zenith-to-horizon gradient, sunrise/sunset glow, round sun with halo, cratered moon, twinkling stars and soft drifting procedural clouds; distance fog uses the sky colour
- Reflective water with procedural waves, Fresnel reflections of the sky, sun glints and depth-based colour absorption (shallow water is clear, deep water turns blue)
- Swaying leaves and grass, glowing lava and torches, warm torch light, underwater fog and distortion
- Rain and snow with overcast skies and rain ambience; damage flash, view bobbing, FOV changes when sprinting, flying or drawing a bow

**Survival gameplay**
- Items and a 36-slot inventory, drops, and dropped items you can pick up (Q to drop)
- Crafting (2×2 in the inventory, 3×3 on a crafting table) with 60+ recipes: planks, sticks, tools, torches, furnace, chest, bread, bow, arrows, bucket, TNT, bricks and more
- Furnace smelting with fuel (ores, sand→glass, food, logs→charcoal…) and chests; both keep their contents in the save
- Tools with wood/stone/iron/gold/diamond tiers: Minecraft's mining-speed formula, harvest levels (for example, diamond ore needs an iron pickaxe) and durability
- Health, hunger, saturation and exhaustion, natural regeneration, fall damage, drowning (air bubbles), lava, fire and cactus damage, death screen and respawn
- Combat: melee with critical hits and knockback, bows with charge-up and arrows you can pick back up
- Eating, buckets (water/lava), farming (hoe → farmland → seeds → wheat, which grows over time), saplings that grow into trees, bone meal, flint & steel with TNT

**Mobs**
- Pigs, cows, sheep (in colours) and chickens (which lay eggs) spawn on grass. They wander, panic when hit and drop meat, leather, wool and feathers.
- Zombies, skeletons, creepers and spiders spawn in the dark:
  - Zombies chase you and hit you.
  - Skeletons keep their distance and shoot arrows.
  - Creepers hiss, swell and explode, breaking blocks.
  - Spiders climb walls and leap at you, and are neutral in daylight.
  - Zombies and skeletons burn in sunlight.
- Cuboid models with procedurally painted skins, walk animations, head tracking, hurt flashes and death animations.

**Progression**
- Armour (leather, chainmail, iron, gold, diamond) with Minecraft's damage reduction, shown on the player and mobs
- Experience orbs from mobs, mining and smelting, the XP bar and levels
- Enchanting table with bookshelves, enchantment rolls, enchanted books and lapis; anvils for repairing and combining.
  Enchantments include Sharpness, Smite, Knockback, Fire Aspect, Looting, Efficiency, Silk Touch, Fortune, Unbreaking,
  Protection, Feather Falling, Thorns, Respiration, Power, Punch, Flame, Infinity, Lure and Luck of the Sea.
- Status effects: Speed, Slowness, Haste, Strength, Weakness, Regeneration, Poison, Resistance, Fire Resistance,
  Water Breathing, Invisibility, Night Vision, Jump Boost and Absorption. They show in the HUD, emit coloured particles
  and are saved with the world.
- Brewing stands fuelled by blaze powder. They follow Minecraft's recipes: nether wart gives an awkward potion; redstone
  makes an effect last longer; glowstone makes it level II; a fermented spider eye corrupts it. Drinkable and splash potions.
- Golden apples, golden carrots, mushroom stew, milk (clears effects) and more foods

**Redstone**
- Wire with signal strength, torches, levers, buttons, pressure plates, repeaters, lamps, TNT
- Pistons and sticky pistons, and powered doors, trapdoors and gates

**Rails, minecarts, boats and fishing**
- Rails, powered rails and detector rails (crafted like Minecraft's). Placed rails join their neighbours: they make
  straight lines, curves at corners, and slopes up to a rail one block higher. A rail pops off if its floor is removed.
- Minecarts follow Minecraft's rail physics:
  - their speed stays along the track and carries round corners
  - slopes speed them up or slow them down
  - powered rails boost a cart, or brake it when they have no power
  - a detector rail gives off a redstone signal while a cart is on it
- Right-click a cart to get in and Shift to get out. Pressing W gives a stopped cart a push in the direction you look.
  Carts keep their momentum when the track ends: they roll on over the ground, slowing gradually, and fly off the
  top of a rising ramp. An empty cart that is moving picks up mobs it runs into. Carts push each other and the entities they bump.
  A few punches break a cart back into an item.
- Boats float on water and are steered by where you look (Minecraft 1.8 handling). They speed up gradually and have
  rowing paddles. Crashing into something at speed smashes a boat into planks and sticks.
- Fishing rods cast a bobber with a sagging line:
  - after a wait, a fish's trail of bubbles heads for the bobber, then it bites with a splash
  - reel in during the bite to catch fish (cod, salmon, clownfish, pufferfish), junk or enchanted treasure, plus experience
  - Lure makes bites come sooner; Luck of the Sea improves the odds of treasure
  - the rod can also hook a mob and pull it towards you
- Fish can be cooked in a furnace. Pufferfish poison you.
- In multiplayer you can ride, steer and break carts and boats, and fish.

**The Nether**
- Obsidian portals lit with flint and steel; travel scales 8:1 and links to portals on the other side
- Netherrack caverns with lava seas, soul sand, glowstone, quartz, nether wart and fire
- Ghasts, zombie pigmen, magma cubes and blazes

**World**
- Villages with houses, farms, wells and villagers you can trade emeralds with; dungeons with spawners and loot chests
- Building blocks: slabs, stairs, fences, gates, doors, trapdoors, ladders, panes, walls, carpets, beds (sleep through the night)
- More animals and mobs: wolves (tameable), squid, endermen, slimes, iron and snow golems
- Infinite terrain from continentalness/erosion/peaks noise with 3D overhangs, rivers, 13 biomes, caves, ores and trees
- Flowing water and lava with levels (water spreads 7 blocks, lava 3, both seek the nearest drop), infinite water sources, and lava + water → obsidian/cobblestone
- Falling sand and gravel, TNT and creeper explosions that chain-react
- Random ticks: grass spreads, crops grow, saplings grow into trees, leaves decay, sugar cane and cactus grow
- Weather cycle with rain and snow

## Build & run

Requirements: JDK 17+ and Maven. The Maven build downloads LWJGL natives for Windows, Linux and macOS (x64 and arm64) into one runnable jar.

```sh
mvn package
java -jar target/mc.jar
```

On macOS the game relaunches itself with `-XstartOnFirstThread`, which GLFW requires.

Without arguments the game opens the title screen. To host a dedicated server:

```sh
java -jar target/mc.jar --server --port 25565 --world world
```

Players join with Multiplayer → Direct Connection (or `--connect host:port`). The server console accepts
`stop`, `save`, `list`, `say <message>`, `time set <day|night|ticks>`, `weather <clear|rain>`, `kick <player>` and `seed`.

Command-line options:

```
--world <name>          open this world folder inside run/saves directly (skips the title screen)
--seed <number|text>    seed for a new world
--renderDistance <n>    override the render distance
--gameDir <path>        where saves/options/screenshots go (default: ./run)
--cmd "/command"        run a chat command after loading (repeatable; "/wait" waits for chunks)
--screenshot <file>     render, save a screenshot once the world is idle, and exit
--connect <host[:port]> join a server directly
--server                run a dedicated server instead of the game
--port <n>              server port (default 25565)
--motd <text>           server description shown in the server list
--maxPlayers <n>        player limit (default 8)
--viewDistance <n>      furthest chunk distance the server sends (default 10)
```

## Mods (NeoForge 1.21.1)

Simple NeoForge 1.21.1 mods run through a compatibility layer: put the mod jars in `<gameDir>/mods` (`run/mods` by
default) for both the game and a dedicated server. Mods compiled for Java 21 need a Java 21 runtime.

The layer (`src/compat/java`) is a clean-room set of classes with the Minecraft and NeoForge names that mods link
against, written on top of this engine. Supported:
- `@Mod` entry points, `@EventBusSubscriber` classes, the mod and game event buses, common/client/server setup events
- `DeferredRegister` for blocks, items, block entity types, menus, creative tabs, sounds and armor materials
- simple blocks, directional blocks with blockstate rotations, block entities with ticking, NBT saving and containers
- items, tools and armor with custom tiers and materials, rarity, food
- textures, block and item models, blockstates and language files from the jar
- shaped, shapeless and smelting recipes, loot tables for block drops, mining tags (`mineable/*`, `needs_*_tool`)
- ore generation from `neoforge:add_features` biome modifiers
- menus and container screens (`AbstractContainerMenu`, `AbstractContainerScreen`, `MenuScreens`), item handlers,
  capabilities, payload registration (delivered in-process)
- `ServerTickEvent`

Before loading, each jar is scanned with ASM: every Minecraft/NeoForge class, field and method it references must exist
in the layer, and mixins are rejected. A mod that fails the check is skipped with the list of what is missing;
`-Dreamc.forceMods=true` loads it anyway. Mod screens and block interactions work in singleplayer and for the LAN
host; clients joining a server see modded blocks and items but cannot open their menus.

## Controls

| Key | Action |
|---|---|
| W A S D | move |
| Space | jump / swim up (Creative: double-tap to fly, hold to ascend) |
| Left Shift | sneak / descend while flying / get out of a minecart or boat |
| Left Ctrl or double-tap W | sprint |
| Mouse | look |
| Left click (hold) | break block / attack |
| Right click | place block, use item (eat, draw bow, bucket, hoe, seeds, cast/reel a fishing rod, put a cart on rails or a boat on water…), get in a minecart or boat, open crafting table / furnace / chest |
| Middle click | pick block |
| 1–9 / mouse wheel | select hotbar slot |
| Q / Ctrl+Q | drop one item / the whole stack |
| E | inventory (creative: all items) |
| T or / | chat / commands |
| Esc | pause menu (settings including Distant Terrain, Open to LAN, Save and Quit to Title / Disconnect) |
| F1 | hide HUD |
| F2 | screenshot (saved to `run/screenshots`) |
| F3 | debug screen |
| F4 | toggle Survival / Creative |
| F11 | fullscreen |

### Commands

```
/time set <day|noon|sunset|night|midnight|sunrise|ticks>   /time add <ticks>
/gamemode <creative|survival>     /tp <x> <y> <z>   (~ relative coordinates work)
/give <item_name> [count]         /setblock <x> <y> <z> <block>
/summon <mob> [x y z] [baby|tamed|angry|<armor material>]   /summon <minecart|boat> [x y z] [ride]   /use (right click)
/effect <effect|clear> [seconds] [amplifier]   /potion <type> [splash]   /enchant <name> [level]
/xp <points>   /armor <material>   /dimension   /locate village [tp]   /perspective <0-2>
/kill [@e]   /weather <clear|rain>   /heal   /clear   /spawnpoint
/fill <x1> <y1> <z1> <x2> <y2> <z2> <block>
/fly   /seed   /rd <chunks>   /lod <chunks> (distant terrain, 0 = off)   /publish (open to LAN)   /menu <worlds|multiplayer|options>   /help
```

In multiplayer, `/time`, `/weather`, `/list`, `/say`, `/kick` and `/tp <player>` run on the server; the other commands affect your own player.

## Code layout

```
mc/Main.java                 entry point, arguments, macOS relaunch
mc/client/                   Game loop, Interaction (mining/combat/items), Screens (containers, pause), Menus (title,
                             worlds, server list), MultiplayerSession, Hud, Commands, Weather, Window (GLFW), Input,
                             Sound (OpenAL), Options (Gson)
mc/net/                      Netty connections, packet ids and encoding, server list ping, LAN discovery
mc/server/                   Server (LAN or dedicated), Session (one client), NetPlayer, DedicatedServer
mc/entity/                   Entity physics, LivingEntity (health/damage), Player (hunger), Mob AI, items, arrows, TNT,
                             Vehicle (riding), MinecartEntity, BoatEntity, FishingBobberEntity
mc/item/                     Item registry, ItemStack, Inventory, Recipes (crafting + smelting)
mc/world/                    Block registry, Chunk, World (streaming, ticks, explosions), Liquids, Drops, Rails, Redstone,
                             BlockEntity (furnace/chest), MobSpawner, WorldStorage
mc/world/gen/                Noise, TerrainGenerator, Decorator (trees/plants), Biome
mc/render/                   ChunkMesher, WorldRenderer (shadows/sky/water), PostProcess (HDR/bloom), MobModel,
                             EntityRenderer, ItemRenderer, TextureGen/ItemTextureGen, Gui, Font, Particles
mc/mod/                      mod loading: ModLoader (jars, assets, data), ModAnalyzer (ASM link check), Bridge
                             (compat classes <-> engine), ModEventBus, ModUi
src/compat/java/             net.minecraft / net.neoforged / com.mojang compatibility classes for NeoForge mods
mc/render/lod/               distant terrain: LodData (chunk summaries + noise estimates), LodMesher, LodRenderer
src/main/resources/shaders/  GLSL 330 shaders (chunk lighting & water, shadow, sky, post-processing)
```

Run the tests with `mvn test`. They check:
- player physics against Minecraft's values, plus collisions, sneaking, swimming and raycasting
- crafting and smelting
- water flow, draining, infinite sources and lava + water
- harvest rules, falling sand, explosions, zombie AI, mob loot, hunger and fall damage
- building blocks, animals, villages and dungeons, the Nether, redstone circuits, enchanting and brewing
- multiplayer over a real local socket: login, chunk streaming, block edits both ways, chat, mob tracking,
  forwarded damage, mining with item pickup, duplicate names, the status ping, and client-side remote worlds
- rails joining into lines, corners and slopes; carts round corners, down slopes, braked and boosted by powered rails;
  detector rails; riding and breaking carts; boats floating and rowing; fishing bites and catches; riding over the network
- mods: a fixture mod compiled at test time is loaded, its blocks, items, textures, recipes, loot, tags, events,
  block entity saving and placement facing are checked, and the analyzer reports missing members
  (`-Dreamc.testJar=<jar>` also loads a real mod)
- distant terrain: chunk summaries (trees, water, flowers ignored), saving and reloading them, noise estimates, tile meshes

## Not implemented

The real game is far larger than this project. Missing so far:
- the End, nether fortresses, strongholds
- signs, paintings, maps, storage/TNT/hopper minecarts, activator rails
- Nether travel in multiplayer (a server hosts the Overworld only), sleeping through the night in multiplayer
