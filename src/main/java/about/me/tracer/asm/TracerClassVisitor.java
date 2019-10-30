package about.me.tracer.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;

public class TracerClassVisitor extends ClassVisitor {

    private String className;

    public TracerClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM5,cv);
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if ("<init>".equals(name) || "<clinit>".equals(name) || Modifier.isNative(access) || Modifier.isAbstract(access) || !Modifier.isPublic(access)) {
            return mv;
        }
        return new TracerMethodVisitor(mv,className,name);
    }


}
