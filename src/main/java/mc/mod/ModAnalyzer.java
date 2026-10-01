package mc.mod;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads a mod jar's bytecode: finds its entry points (@Mod, @EventBusSubscriber) and checks that every Minecraft,
 * NeoForge and Mojang class, method and field the mod can reach from them exists in reamc-compat, so a mod that can't
 * work is reported before it runs instead of failing halfway through a game.
 *
 * <p>Only code reachable from the entry points counts: mixins (reamc doesn't apply them), integrations with other mods
 * (found by those mods, never loaded here) and data generators (run by a build task, not the game) are ignored. A class
 * that is reached has its static initializer and every instance method reached, since the game may call any override;
 * static methods are reached only when called.
 */
public final class ModAnalyzer {
    /** A class with @EventBusSubscriber and the sides it applies to. */
    public record Subscriber(String className, boolean client, boolean server, List<Listener> listeners) { }

    /** A static @SubscribeEvent method of a subscriber class. */
    public record Listener(String name, String desc, String eventClass, String priority, boolean receiveCanceled) { }

    public final List<String> modClasses = new ArrayList<>();
    public final List<Subscriber> subscribers = new ArrayList<>();
    /** Unsupported references, as "owner.name descriptor" (empty = the mod only uses what reamc provides). */
    public final Set<String> missing = new TreeSet<>();
    /** For each missing reference, one mod class that uses it (for reports). */
    public final Map<String, String> missingFrom = new HashMap<>();
    public int checkedReferences;
    public boolean usesMixins;
    /** Mod classes reachable from the entry points. */
    public final Set<String> reachedClasses = new TreeSet<>();

    private static final class MethodInfo {
        final Set<String> classRefs = new LinkedHashSet<>(), fieldRefs = new LinkedHashSet<>(), methodRefs = new LinkedHashSet<>();
        /** Mod classes this method mentions (types, owners), and mod methods it calls or makes method handles to. */
        final Set<String> internalClasses = new LinkedHashSet<>(), internalMethods = new LinkedHashSet<>();
        boolean isStatic;
    }

    private static final class ClassInfo {
        String name, superName;
        String[] interfaces = new String[0];
        final Set<String> headerRefs = new LinkedHashSet<>();
        final Map<String, MethodInfo> methods = new HashMap<>();
    }

    private final Map<String, ClassInfo> classes = new HashMap<>();

    /** The jar's own classes; everything else (Minecraft, NeoForge, libraries, the JDK) must be provided. */
    private final Set<String> own = new HashSet<>();

    private boolean external(String owner) {
        return !own.contains(owner);
    }

    public static ModAnalyzer analyze(ZipFile jar, ClassLoader compat) throws Exception {
        ModAnalyzer a = new ModAnalyzer();
        jar.stream().filter(e -> e.getName().endsWith(".class") && !e.getName().startsWith("META-INF/"))
                .forEach(e -> a.own.add(e.getName().substring(0, e.getName().length() - 6)));
        var en = jar.entries();
        while (en.hasMoreElements()) {
            ZipEntry e = en.nextElement();
            if (e.getName().endsWith(".class") && !e.getName().startsWith("META-INF/")) {
                try (var in = jar.getInputStream(e)) {
                    new ClassReader(in.readAllBytes()).accept(a.visitor(), ClassReader.SKIP_FRAMES);
                }
            }
            if (e.getName().endsWith(".mixins.json")) a.usesMixins = true;
        }
        a.check(compat, a.reach(compat));
        return a;
    }

    // ------------------------------------------------------------------ reading

    private ClassVisitor visitor() {
        return new ClassVisitor(Opcodes.ASM9) {
            ClassInfo info;

            @Override
            public void visit(int version, int access, String n, String sig, String superName, String[] interfaces) {
                info = new ClassInfo();
                info.name = n;
                info.superName = superName;
                if (interfaces != null) info.interfaces = interfaces;
                classes.put(n, info);
                headerType(superName);
                if (interfaces != null) for (String i : interfaces) headerType(i);
            }

            private void headerType(String internal) {
                if (internal != null && external(internal)) info.headerRefs.add(internal);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (desc.equals("Lnet/neoforged/fml/common/Mod;")) modClasses.add(info.name.replace('/', '.'));
                if (desc.equals("Lnet/neoforged/fml/common/EventBusSubscriber;")) {
                    boolean[] sides = {true, true};
                    String owner = info.name;
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public AnnotationVisitor visitArray(String n) {
                            if (!n.equals("value")) return null;
                            sides[0] = sides[1] = false;
                            return new AnnotationVisitor(Opcodes.ASM9) {
                                @Override
                                public void visitEnum(String n2, String d, String value) {
                                    if (value.equals("CLIENT")) sides[0] = true;
                                    if (value.equals("DEDICATED_SERVER")) sides[1] = true;
                                }
                            };
                        }

                        @Override
                        public void visitEnd() { subscribers.add(new Subscriber(owner.replace('/', '.'), sides[0], sides[1], new ArrayList<>())); }
                    };
                }
                return null;
            }

            @Override
            public FieldVisitor visitField(int access, String n, String desc, String sig, Object value) { return null; }

            @Override
            public MethodVisitor visitMethod(int access, String n, String desc, String sig, String[] ex) {
                MethodInfo m = new MethodInfo();
                m.isStatic = (access & Opcodes.ACC_STATIC) != 0;
                info.methods.put(n + desc, m);
                for (Type t : Type.getArgumentTypes(desc)) type(m, t.getDescriptor());
                type(m, Type.getReturnType(desc).getDescriptor());
                String owner = info.name;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String adesc, boolean visible) {
                        if (!adesc.equals("Lnet/neoforged/bus/api/SubscribeEvent;") || !m.isStatic) return null;
                        Type[] args = Type.getArgumentTypes(desc);
                        if (args.length != 1 || args[0].getSort() != Type.OBJECT) return null;
                        String[] priority = {"NORMAL"};
                        boolean[] canceled = {false};
                        return new AnnotationVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitEnum(String an, String d, String value) { if (an.equals("priority")) priority[0] = value; }

                            @Override
                            public void visit(String an, Object value) { if (an.equals("receiveCanceled")) canceled[0] = (Boolean) value; }

                            @Override
                            public void visitEnd() {
                                pendingListeners.computeIfAbsent(owner, k -> new ArrayList<>())
                                        .add(new Listener(n, desc, args[0].getClassName(), priority[0], canceled[0]));
                            }
                        };
                    }

                    @Override public void visitTypeInsn(int op, String t) { type(m, t); }

                    @Override public void visitMultiANewArrayInsn(String d, int dims) { type(m, d); }

                    @Override
                    public void visitTryCatchBlock(org.objectweb.asm.Label s, org.objectweb.asm.Label e, org.objectweb.asm.Label h, String t) { type(m, t); }

                    @Override
                    public void visitFieldInsn(int op, String fo, String n2, String d) {
                        type(m, fo);
                        if (external(fo)) m.fieldRefs.add(fo + "." + n2 + " " + d);
                    }

                    @Override
                    public void visitMethodInsn(int op, String mo, String n2, String d, boolean itf) {
                        if (mo.startsWith("[")) return;
                        type(m, mo);
                        if (external(mo)) m.methodRefs.add(mo + "." + n2 + " " + d);
                        else m.internalMethods.add(mo + "." + n2 + d);
                        for (Type t : Type.getArgumentTypes(d)) type(m, t.getDescriptor());
                        type(m, Type.getReturnType(d).getDescriptor());
                    }

                    @Override
                    public void visitInvokeDynamicInsn(String n2, String d, Handle bsm, Object... args) {
                        for (Type t : Type.getArgumentTypes(d)) type(m, t.getDescriptor());
                        type(m, Type.getReturnType(d).getDescriptor());
                        for (Object o : args) {
                            if (o instanceof Handle h) handle(m, h);
                            if (o instanceof Type t && t.getSort() == Type.METHOD) {
                                for (Type at : t.getArgumentTypes()) type(m, at.getDescriptor());
                                type(m, t.getReturnType().getDescriptor());
                            }
                        }
                    }

                    @Override
                    public void visitLdcInsn(Object v) {
                        if (v instanceof Type t && t.getSort() == Type.OBJECT) type(m, t.getInternalName());
                        if (v instanceof Handle h) handle(m, h);
                    }
                };
            }
        };
    }

    private final Map<String, List<Listener>> pendingListeners = new HashMap<>();

    private void type(MethodInfo m, String internal) {
        if (internal == null || internal.length() == 1) return; // primitive descriptors
        Type t = internal.startsWith("[") || internal.startsWith("L") && internal.endsWith(";") ? Type.getType(internal) : Type.getObjectType(internal);
        while (t.getSort() == Type.ARRAY) t = t.getElementType();
        if (t.getSort() != Type.OBJECT) return;
        if (external(t.getInternalName())) m.classRefs.add(t.getInternalName());
        else m.internalClasses.add(t.getInternalName());
    }

    private void handle(MethodInfo m, Handle h) {
        type(m, h.getOwner());
        if (!external(h.getOwner())) {
            m.internalMethods.add(h.getOwner() + "." + h.getName() + h.getDesc());
            return;
        }
        if (h.getTag() <= Opcodes.H_PUTSTATIC) m.fieldRefs.add(h.getOwner() + "." + h.getName() + " " + h.getDesc());
        else m.methodRefs.add(h.getOwner() + "." + h.getName() + " " + h.getDesc());
    }

    // ------------------------------------------------------------------ reachability

    private final ArrayDeque<String[]> work = new ArrayDeque<>();
    private final Set<String> reachedMethods = new HashSet<>();
    private final Set<String> reachedInternal = new HashSet<>();

    /** Walks from the entry points; returns the external references that reachable code makes, with a user each. */
    private Map<String, String> reach(ClassLoader compat) {
        for (Subscriber s : subscribers) {
            String internal = s.className().replace('.', '/');
            for (Listener l : pendingListeners.getOrDefault(internal, List.of())) {
                // A listener for an event reamc doesn't have is never called, so it isn't an entry point
                if (l.eventClass().startsWith("net.") && load(compat, l.eventClass().replace('.', '/')) == null) continue;
                s.listeners().add(l);
                reachMethod(internal, l.name() + l.desc());
            }
        }
        for (String c : modClasses) reachClass(c.replace('.', '/'));
        Map<String, String> refs = new HashMap<>();
        while (!work.isEmpty()) {
            String[] item = work.poll();
            ClassInfo ci = classes.get(item[0]);
            if (item[1] == null) {
                for (String h : ci.headerRefs) refs.putIfAbsent("C" + h, ci.name);
                if (ci.superName != null) reachClass(ci.superName);
                for (String i : ci.interfaces) reachClass(i);
                for (Map.Entry<String, MethodInfo> e : ci.methods.entrySet()) {
                    if (!e.getValue().isStatic || e.getKey().startsWith("<clinit>")) reachMethod(ci.name, e.getKey());
                }
                continue;
            }
            MethodInfo m = ci.methods.get(item[1]);
            for (String c : m.classRefs) refs.putIfAbsent("C" + c, ci.name);
            for (String f : m.fieldRefs) refs.putIfAbsent("F" + f, ci.name);
            for (String r : m.methodRefs) refs.putIfAbsent("M" + r, ci.name);
            for (String c : m.internalClasses) reachClass(c);
            for (String r : m.internalMethods) {
                int dot = r.indexOf('.');
                reachInherited(r.substring(0, dot), r.substring(dot + 1));
            }
        }
        return refs;
    }

    private void reachClass(String internal) {
        if (!classes.containsKey(internal) || !reachedInternal.add(internal)) return;
        reachedClasses.add(internal.replace('/', '.'));
        work.add(new String[]{internal, null});
    }

    private void reachMethod(String owner, String nameDesc) {
        reachClass(owner);
        ClassInfo ci = classes.get(owner);
        if (ci == null || !ci.methods.containsKey(nameDesc) || !reachedMethods.add(owner + "." + nameDesc)) return;
        work.add(new String[]{owner, nameDesc});
    }

    /** A call names the class it was compiled against; the method may be declared in a superclass. */
    private void reachInherited(String owner, String nameDesc) {
        for (String c = owner; c != null && classes.containsKey(c); c = classes.get(c).superName) {
            reachClass(c);
            if (classes.get(c).methods.containsKey(nameDesc)) {
                reachMethod(c, nameDesc);
                return;
            }
        }
    }

    // ------------------------------------------------------------------ checking

    private void check(ClassLoader cl, Map<String, String> refs) {
        for (Map.Entry<String, String> e : refs.entrySet()) {
            String r = e.getKey().substring(1);
            checkedReferences++;
            String miss = switch (e.getKey().charAt(0)) {
                case 'C' -> load(cl, r) == null ? r.replace('/', '.') : null;
                case 'F' -> {
                    String owner = r.substring(0, r.lastIndexOf('.', r.indexOf(' ')));
                    String name = r.substring(owner.length() + 1, r.indexOf(' '));
                    Class<?> c = load(cl, owner);
                    yield c != null && findField(c, name) == null ? owner.replace('/', '.') + "." + name : null;
                }
                default -> {
                    String owner = r.substring(0, r.lastIndexOf('.', r.indexOf(' ')));
                    String name = r.substring(owner.length() + 1, r.indexOf(' ')), desc = r.substring(r.indexOf(' ') + 1);
                    Class<?> c = load(cl, owner);
                    yield c != null && !hasMethod(c, name, desc) ? owner.replace('/', '.') + "." + name + desc : null;
                }
            };
            if (miss != null) {
                missing.add(miss);
                missingFrom.putIfAbsent(miss, e.getValue().replace('/', '.'));
            }
        }
    }

    private static Class<?> load(ClassLoader cl, String internal) {
        try {
            return Class.forName(internal.replace('/', '.'), false, cl);
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    private static Field findField(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            for (Field f : k.getDeclaredFields()) if (f.getName().equals(name)) return f;
            for (Class<?> i : k.getInterfaces()) {
                Field f = findField(i, name);
                if (f != null) return f;
            }
        }
        return null;
    }

    private static boolean hasMethod(Class<?> c, String name, String desc) {
        if (name.equals("<init>")) {
            for (Constructor<?> k : c.getDeclaredConstructors()) if (Type.getConstructorDescriptor(k).equals(desc)) return true;
            return false;
        }
        if (name.equals("<clinit>")) return true;
        List<Class<?>> todo = new ArrayList<>();
        todo.add(c);
        Set<Class<?>> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            Class<?> k = todo.remove(todo.size() - 1);
            if (k == null || !seen.add(k)) continue;
            for (Method m : k.getDeclaredMethods()) if (m.getName().equals(name) && Type.getMethodDescriptor(m).equals(desc)) return true;
            todo.add(k.getSuperclass());
            todo.addAll(List.of(k.getInterfaces()));
        }
        // Object's methods are inherited by interfaces too
        for (Method m : Object.class.getDeclaredMethods()) if (m.getName().equals(name) && Type.getMethodDescriptor(m).equals(desc)) return true;
        return false;
    }
}
