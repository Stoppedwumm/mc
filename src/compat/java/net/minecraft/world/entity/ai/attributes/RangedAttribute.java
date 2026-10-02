package net.minecraft.world.entity.ai.attributes;

public class RangedAttribute extends Attribute {
    private final double min, max;

    public RangedAttribute(String descriptionId, double defaultValue, double min, double max) {
        super(descriptionId, defaultValue);
        this.min = min;
        this.max = max;
    }

    public double getMinValue() { return min; }
    public double getMaxValue() { return max; }
    @Override public double sanitizeValue(double v) { return Double.isNaN(v) ? min : Math.max(min, Math.min(max, v)); }
}
