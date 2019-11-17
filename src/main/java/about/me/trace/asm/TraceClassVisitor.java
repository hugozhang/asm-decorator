package about.me.trace.asm;

import about.me.cache.asm.RedisCacheMethodVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;


public class TraceClassVisitor extends ClassVisitor {

    private String owner;

    private boolean isInterface;

    public TraceClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM5,cv);
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        isInterface = Modifier.isInterface(access);
        this.owner = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (isInterface || "<init>".equals(name) || "<clinit>".equals(name)
                || Modifier.isNative(access) || Modifier.isAbstract(access) || !Modifier.isPublic(access)) {
            return mv;
        }
//        return new TraceMethodVisitor(mv, access, owner, name, desc);

        return new RedisCacheMethodVisitor(mv, access, name, desc);

    }
}
