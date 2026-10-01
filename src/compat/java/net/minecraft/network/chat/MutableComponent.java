package net.minecraft.network.chat;

import java.util.ArrayList;
import java.util.List;

/** reamc-compat text component: a literal or a translation key, plus appended siblings and a colour. */
public class MutableComponent implements Component {
    private final String literal, key;
    private final Object[] args;
    private final List<Component> siblings = new ArrayList<>();
    private Integer color;

    MutableComponent(String literal, String key, Object[] args) {
        this.literal = literal;
        this.key = key;
        this.args = args;
    }

    @Override
    public String getString() {
        StringBuilder sb = new StringBuilder();
        if (literal != null) sb.append(literal);
        else {
            Object[] a = new Object[args.length];
            for (int i = 0; i < a.length; i++) a[i] = args[i] instanceof Component c ? c.getString() : args[i];
            sb.append(mc.mod.Lang.format(key, a));
        }
        for (Component c : siblings) sb.append(c.getString());
        return sb.toString();
    }

    public MutableComponent append(Component c) { siblings.add(c); return this; }
    public MutableComponent append(String s) { return append(Component.literal(s)); }
    public MutableComponent withStyle(Object style) { return this; }
    public MutableComponent withStyle(Object... styles) { return this; }
    public MutableComponent withColor(int rgb) { color = rgb; return this; }
    public Integer reamc$color() { return color; }

    @Override public String toString() { return getString(); }
}
