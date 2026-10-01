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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads a mod jar's bytecode: finds its entry points (@Mod, @EventBusSubscriber) and checks that every Minecraft,
 * NeoForge and Mojang class, method and field it uses exists in reamc-compat, so a mod that can't work is reported
 * before it runs instead of failing halfway through a game.
 */
public final class ModAnalyzer {
    /** A class with @EventBusSubscriber and the sides it applies to. */
    public record Subscriber(String className, boolean client, boolean server) { }

    public final List<String> modClasses = new ArrayList<>();
    public final List<Subscriber> subscribers = new ArrayList<>();
    /** Unsupported references, as "owner.name descriptor" (empty = the mod only uses what reamc provides). */
    public final Set<String> missing = new TreeSet<>();
    public int checkedReferences;
    public boolean usesMixins;

    private final Set<String> fieldRefs = new HashSet<>(), methodRefs = new HashSet<>(), classRefs = new HashSet<>();

    private static boolean external(String owner) {
        return owner.startsWith("net/minecraft/") || owner.startsWith("net/neoforged/") || owner.startsWith("com/mojang/");
    }

    public static ModAnalyzer analyze(ZipFile jar, ClassLoader compat) throws Exception {
        ModAnalyzer a = new ModAnalyzer();
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
        a.check(compat);
        return a;
    }

    private void type(String internal) {
        if (internal == null) return;
        Type t = internal.startsWith("[") || internal.startsWith("L") && internal.endsWith(";") ? Type.getType(internal) : Type.getObjectType(internal);
        while (t.getSort() == Type.ARRAY) t = t.getElementType();
        if (t.getSort() == Type.OBJECT && external(t.getInternalName())) classRefs.add(t.getInternalName());
    }

    private void handle(Handle h) {
        if (!external(h.getOwner())) return;
        if (h.getTag() <= Opcodes.H_PUTSTATIC) fieldRefs.add(h.getOwner() + "." + h.getName() + " " + h.getDesc());
        else methodRefs.add(h.getOwner() + "." + h.getName() + " " + h.getDesc());
    }

    private ClassVisitor visitor() {
        return new ClassVisitor(Opcodes.ASM9) {
            String name;

            @Override
            public void visit(int version, int access, String n, String sig, String superName, String[] interfaces) {
                name = n;
                type(superName);
                if (interfaces != null) for (String i : interfaces) type(i);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (desc.equals("Lnet/neoforged/fml/common/Mod;")) modClasses.add(name.replace('/', '.'));
                if (desc.equals("Lnet/neoforged/fml/common/EventBusSubscriber;")) {
                    boolean[] sides = {true, true, false};
                    String owner = name;
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public AnnotationVisitor visitArray(String n) {
                            if (!n.equals("value")) return null;
                            sides[0] = sides[1] = false;
                            sides[2] = true;
                            return new AnnotationVisitor(Opcodes.ASM9) {
                                @Override
                                public void visitEnum(String n2, String d, String value) {
                                    if (value.equals("CLIENT")) sides[0] = true;
                                    if (value.equals("DEDICATED_SERVER")) sides[1] = true;
                                }
                            };
                        }

                        @Override
                        public void visitEnd() { subscribers.add(new Subscriber(owner.replace('/', '.'), sides[0], sides[1])); }
                    };
                }
                return null;
            }

            @Override
            public FieldVisitor visitField(int access, String n, String desc, String sig, Object value) {
                type(desc);
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String n, String desc, String sig, String[] ex) {
                for (Type t : Type.getArgumentTypes(desc)) type(t.getDescriptor());
                type(Type.getReturnType(desc).getDescriptor());
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitTypeInsn(int op, String t) { type(t); }

                    @Override
                    public void visitFieldInsn(int op, String owner, String n2, String d) {
                        if (external(owner)) fieldRefs.add(owner + "." + n2 + " " + d);
                    }

                    @Override
                    public void visitMethodInsn(int op, String owner, String n2, String d, boolean itf) {
                        if (owner.startsWith("[")) return;
                        if (external(owner)) methodRefs.add(owner + "." + n2 + " " + d);
                    }

                    @Override
                    public void visitInvokeDynamicInsn(String n2, String d, Handle bsm, Object... args) {
                        for (Object o : args) if (o instanceof Handle h) handle(h);
                    }

                    @Override
                    public void visitLdcInsn(Object v) {
                        if (v instanceof Type t && t.getSort() == Type.OBJECT) type(t.getInternalName());
                    }
                };
            }
        };
    }

    private void check(ClassLoader cl) {
        for (String c : classRefs) {
            checkedReferences++;
            if (load(cl, c) == null) missing.add(c.replace('/', '.'));
        }
        for (String r : fieldRefs) {
            checkedReferences++;
            String owner = r.substring(0, r.lastIndexOf('.', r.indexOf(' ')));
            String name = r.substring(owner.length() + 1, r.indexOf(' '));
            Class<?> c = load(cl, owner);
            if (c != null && findField(c, name) == null) missing.add(owner.replace('/', '.') + "." + name);
        }
        for (String r : methodRefs) {
            checkedReferences++;
            String owner = r.substring(0, r.lastIndexOf('.', r.indexOf(' ')));
            String name = r.substring(owner.length() + 1, r.indexOf(' ')), desc = r.substring(r.indexOf(' ') + 1);
            Class<?> c = load(cl, owner);
            if (c != null && !hasMethod(c, name, desc)) missing.add(owner.replace('/', '.') + "." + name + desc);
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
