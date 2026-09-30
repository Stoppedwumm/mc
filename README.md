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
| [Gson 2.10](https://github.com/google/gson) | `options.json`, `level.json` | same |

All textures and sounds are generated procedurally at startup, so no Mojang assets are included or needed.

## Features

**World generation**
- Infinite terrain from continentalness / erosion / peaks noise, a 3D density field for cliffs and overhangs, and river valleys. It's sampled on a coarse grid and interpolated, like modern Minecraft.
- Biomes: plains, forest, birch forest, taiga, snowy taiga, desert, badlands, mountains, snowy peaks, beaches, rivers, oceans and frozen oceans, with smooth biome grass and foliage tints.
- Spaghetti and cheese caves, lava lakes near bedrock, and ore veins (coal, iron, gold, diamond) plus granite, diorite, andesite, dirt and gravel pockets.
- Oak, big oak, birch and spruce trees, plus tall grass, ferns, flowers, cactus, dead bushes, sugar cane and pumpkins. These are placed in a separate decoration pass so they can cross chunk borders.

**Rendering**
- Chunk meshes built on worker threads, with face culling, frustum culling and front-to-back sorting.
- Sky light and block light flood-filled over each chunk's 3×3 neighbourhood, with Minecraft-style smooth lighting and ambient occlusion.
- Day/night cycle: square sun and moon, stars, sunrise and sunset glow, distance fog, and "fancy" 3D clouds.
- Translucent water and ice, cutout leaves, glass and plants, and lava and torches that emit light.
- View bobbing, sprint and fly FOV changes, held-block rendering with a swing animation, block-breaking cracks and break particles, and an underwater fog and tint.

**Gameplay**
- Player physics at 20 ticks per second using Minecraft's constants: 0.42 jump velocity, 0.08 gravity, 0.98 drag, 0.546 ground friction, sprinting and sneaking (you can't walk off edges while sneaking), 0.6-block step-up and swimming.
- **Survival mode** (breaking takes time based on block hardness) and **Creative mode** (instant breaking, double-tap Space to fly).
- Simple block updates: unsupported plants and torches pop off, sand and gravel fall, and water flows into gaps.
- A creative block inventory, 9-slot hotbar, pick-block, a chat with commands, an F3 debug screen, a pause menu with options, screenshots and fullscreen.
- Worlds are saved automatically: compressed chunk files plus `level.json`.

## Build & run

Requirements: JDK 17+ and Maven. The Maven build downloads LWJGL natives for Windows, Linux and macOS (x64 and arm64) into one runnable jar.

```sh
mvn package
java -jar target/mc.jar
```

On macOS the game relaunches itself with `-XstartOnFirstThread`, which GLFW requires.

Command-line options:

```
--world <name>          world folder inside run/saves (default: world)
--seed <number|text>    seed for a new world
--renderDistance <n>    override the render distance
--gameDir <path>        where saves/options/screenshots go (default: ./run)
--cmd "/command"        run a chat command after loading (repeatable; "/wait" waits for chunks)
--screenshot <file>     render, save a screenshot once the world is idle, and exit
```

## Controls

| Key | Action |
|---|---|
| W A S D | move |
| Space | jump / swim up (Creative: double-tap to fly, hold to ascend) |
| Left Shift | sneak / descend while flying |
| Left Ctrl or double-tap W | sprint |
| Mouse | look |
| Left click (hold) | break block |
| Right click | place block |
| Middle click | pick block |
| 1–9 / mouse wheel | select hotbar slot |
| E | block inventory |
| T or / | chat / commands |
| Esc | pause menu (settings, Save and Quit) |
| F1 | hide HUD |
| F2 | screenshot (saved to `run/screenshots`) |
| F3 | debug screen |
| F4 | toggle Survival / Creative |
| F11 | fullscreen |

### Commands

```
/time set <day|noon|sunset|night|midnight|sunrise|ticks>   /time add <ticks>
/gamemode <creative|survival>     /tp <x> <y> <z>   (~ relative coordinates work)
/give <block_name>                /setblock <x> <y> <z> <block>
/fill <x1> <y1> <z1> <x2> <y2> <z2> <block>
/fly   /seed   /rd <chunks>   /help
```

## Code layout

```
mc/Main.java                 entry point, arguments, macOS relaunch
mc/client/                   Game loop, Window (GLFW), Input, Sound (OpenAL), Options (Gson)
mc/entity/Player.java        movement physics and collision
mc/world/                    Block registry, Chunk, World (chunk streaming + worker pool), WorldStorage
mc/world/gen/                Noise, TerrainGenerator, Decorator (trees/plants), Biome
mc/render/                   ChunkMesher (lighting + meshing), WorldRenderer, TextureGen, GUI, Font, Particles
src/main/resources/shaders/  GLSL 330 shaders (chunk, sky, basic)
```

Run the tests with `mvn test`. They check the player physics against Minecraft's values, plus collisions, sneaking, swimming and raycasting.
