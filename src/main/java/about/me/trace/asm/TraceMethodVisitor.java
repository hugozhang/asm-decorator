package about.me.trace.asm;

import about.me.cache.annotation.Cache;
import about.me.cache.asm.CacheAnnotationVisitor;
import org.objectweb.asm.*;

import java.util.concurrent.TimeUnit;


public class TraceMethodVisitor extends MethodVisitor {

    private String className;

    private String methodName;

    private Type returnType;

    private Object group;

    private Object key;

    private Object timeUnit;

    private CacheAnnotationVisitor cacheAnnotationVisitor;

    public TraceMethodVisitor(MethodVisitor mv,String className, String methodName,String methodDesc) {
        super(Opcodes.ASM5,mv);
        this.className = className;
        this.methodName = methodName;
        this.returnType = Type.getReturnType(methodDesc);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
        if (Type.getDescriptor(Cache.class).equals(descriptor)) {
            cacheAnnotationVisitor = new CacheAnnotationVisitor(av);
            return cacheAnnotationVisitor;
        }
        return av;
    }

    @Override
    public void visitCode() {
        mv.visitCode();
        //trace
        mv.visitLdcInsn(this.className.replace('/','.') + "." + this.methodName);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/trace/core/Trace", "enter", "(Ljava/lang/String;)V", false);
        //cache
        if (cacheAnnotationVisitor != null) {
            group = cacheAnnotationVisitor.annMap.get("group");
            key = cacheAnnotationVisitor.annMap.get("key");
            timeUnit = cacheAnnotationVisitor.annMap.get("timeUnit");

            mv.visitLdcInsn(group);
            mv.visitLdcInsn(key);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/cache/redis/HessianRedisTemplate", "getObject", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;", false);

            //检查缓存对象是否为空前,先检查返回数据类型 redis返回的是Object 要与方法的返回类型匹配

            mv.visitVarInsn(Opcodes.ASTORE, 1);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            Label l1 = new Label();
            mv.visitJumpInsn(Opcodes.IFNULL, l1);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitLabel(l1);
            mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/lang/Object"}, 0, null);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)
                || opcode == Opcodes.ATHROW) {
            if (cacheAnnotationVisitor != null) {
                //当前方法的返回类型如果是基本类型要包装，再放入redis，因为redis方法接收的是Object
                int locVar = 2;
                mv.visitInsn(Opcodes.DUP);
                //复制
                mv.visitVarInsn(Opcodes.ASTORE,locVar);
                mv.visitLdcInsn(group);
                mv.visitLdcInsn(key);
                mv.visitVarInsn(Opcodes.ALOAD, locVar);
                mv.visitInsn(Opcodes.LCONST_1);
                mv.visitFieldInsn(Opcodes.GETSTATIC, "java/util/concurrent/TimeUnit", timeUnit.toString(), "Ljava/util/concurrent/TimeUnit;");
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/cache/redis/HessianRedisTemplate", "putObject", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;JLjava/util/concurrent/TimeUnit;)V", false);
            }
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/trace/core/Trace", "exit", "()V", false);
        }
        mv.visitInsn(opcode);
    }



    public static void main(String[] args) {
        System.out.println(Type.getInternalName(TimeUnit.class));
    }
}
