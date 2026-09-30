package mc.entity;

public enum MobType {
    //        name        hostile  hp  width height walk   chase
    PIG("Pig", false, 10, 0.9f, 0.9f, 0.032f, 0.075f),
    COW("Cow", false, 10, 0.9f, 1.4f, 0.030f, 0.07f),
    SHEEP("Sheep", false, 8, 0.9f, 1.3f, 0.032f, 0.075f),
    CHICKEN("Chicken", false, 4, 0.4f, 0.7f, 0.03f, 0.07f),
    ZOMBIE("Zombie", true, 20, 0.6f, 1.95f, 0.03f, 0.068f),
    SKELETON("Skeleton", true, 20, 0.6f, 1.99f, 0.03f, 0.07f),
    CREEPER("Creeper", true, 20, 0.6f, 1.7f, 0.03f, 0.07f),
    SPIDER("Spider", true, 16, 1.4f, 0.9f, 0.035f, 0.092f);

    public final String displayName;
    public final boolean hostile;
    public final int maxHealth;
    public final float width, height, walkSpeed, chaseSpeed;

    MobType(String displayName, boolean hostile, int maxHealth, float width, float height, float walkSpeed, float chaseSpeed) {
        this.displayName = displayName;
        this.hostile = hostile;
        this.maxHealth = maxHealth;
        this.width = width;
        this.height = height;
        this.walkSpeed = walkSpeed;
        this.chaseSpeed = chaseSpeed;
    }

    public static final MobType[] PASSIVE = {PIG, COW, SHEEP, CHICKEN};
    public static final MobType[] HOSTILE = {ZOMBIE, ZOMBIE, SKELETON, SKELETON, CREEPER, SPIDER};
}
