package mc.world;

import mc.entity.Player;
import mc.util.RayCast;

/**
 * Where the engine calls into loaded mods (see mc.mod). Only called where the world is simulated
 * (singleplayer, a LAN host or a server), never on a multiplayer client's copy of the world.
 */
public interface ModHooks {
    /** A block changed and the old or new block comes from a mod (block entities, removal callbacks). */
    void blockChanged(World w, int x, int y, int z, int oldId, int oldMeta, int newId, int newMeta);

    /** Right click on a mod block; true if the block used the click (nothing is placed then). */
    boolean useBlock(World w, Player p, int x, int y, int z, RayCast.Hit hit);

    /** Metadata (block state) of a mod block a player is placing. */
    int placementMeta(World w, Block b, Player p, int x, int y, int z, RayCast.Hit hit);

    /** After every tick of a simulated world. */
    void worldTicked(World w);

    /** The world is being saved. */
    void worldSaved(World w);
}
