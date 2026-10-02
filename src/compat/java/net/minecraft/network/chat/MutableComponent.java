package net.minecraft.network.chat;

import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/** reamc-compat text component: a literal or a translation key, with a style and appended siblings. */
public class MutableComponent implements Component {
    private final String literal, key;
    private final Object[] args;
    private final List<Component> siblings = new ArrayList<>();
    private Style style = Style.EMPTY;

    MutableComponent(String literal, String key, Object[] args) {
        this.literal = literal;
        this.key = key;
        this.args = args;
    }

    static MutableComponent reamc$copyOf(Component c) {
        MutableComponent m;
        if (c instanceof MutableComponent o) m = new MutableComponent(o.literal, o.key, o.args.clone());
        else m = new MutableComponent(c.getString(), null, new Object[0]);
        m.style = c.getStyle();
        for (Component s : c.getSiblings()) m.siblings.add(s.copy());
        return m;
    }

    public String reamc$literal() { return literal; }
    public String reamc$key() { return key; }
    public Object[] reamc$args() { return args; }

    private String ownText() {
        if (literal != null) return literal;
        Object[] a = new Object[args.length];
        for (int i = 0; i < a.length; i++) a[i] = args[i] instanceof Component c ? c.getString() : args[i];
        return mc.mod.Lang.format(key, a);
    }

    @Override
    public String getString() {
        StringBuilder sb = new StringBuilder(ownText());
        for (Component c : siblings) sb.append(c.getString());
        return sb.toString();
    }

    /** Text with § colour codes, for reamc's GUI. */
    public String reamc$formatted() {
        StringBuilder sb = new StringBuilder(style.reamc$codes()).append(ownText());
        for (Component c : siblings) {
            sb.append(c instanceof MutableComponent m ? m.reamc$formatted() : c.getString());
            if (!style.reamc$codes().isEmpty()) sb.append(style.reamc$codes());
        }
        return sb.toString();
    }

    @Override public Style getStyle() { return style; }
    @Override public List<Component> getSiblings() { return siblings; }

    public MutableComponent setStyle(Style s) { style = s; return this; }
    public MutableComponent append(Component c) { siblings.add(c); return this; }
    public MutableComponent append(String s) { return append(Component.literal(s)); }
    public MutableComponent withStyle(Style s) { style = s.applyTo(style); return this; }
    public MutableComponent withStyle(UnaryOperator<Style> f) { style = f.apply(style); return this; }
    public MutableComponent withStyle(ChatFormatting f) { style = style.applyFormat(f); return this; }
    public MutableComponent withStyle(ChatFormatting... fs) { style = style.applyFormats(fs); return this; }
    public MutableComponent withColor(int rgb) { style = style.withColor(rgb); return this; }
    public Integer reamc$color() { return style.getColor() == null ? null : style.getColor().getValue(); }

    @Override public String toString() { return getString(); }
}
