package about.me.core;

import about.me.cache.asm.RedisCacheMethodVisitor;
import about.me.trace.asm.TraceMethodVisitor;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.*;

import java.lang.reflect.Modifier;

@Slf4j
public class SuperClassVisitor extends ClassVisitor {

    private String owner;

    private boolean isInterface;

    public SuperClassVisitor(ClassVisitor cv) {
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
                || Modifier.isNative(access) || Modifier.isAbstract(access) || Modifier.isStatic(access) || !Modifier.isPublic(access)) {
            return mv;
        }

//        java.lang.IllegalArgumentException: LocalVariablesSorter only accepts expanded frames (see ClassReader.EXPAND_FRAMES)
//        也可以用继承来实现 责任链式调用
        TraceMethodVisitor mv1 = new TraceMethodVisitor(mv,access,owner,name, desc);
        RedisCacheMethodVisitor mv2 = new RedisCacheMethodVisitor(mv1,access,owner, name, desc);
        return mv2;
    }
}
