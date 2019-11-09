package about.me.trace.asm;

import about.me.cache.annotation.Cache;
import about.me.cache.asm.CacheAnnotationVisitor;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.concurrent.TimeUnit;


public class TraceMethodVisitor extends AdviceAdapter {

    private String className;

    private String methodName;

    private Type returnType;

    private CacheAnnotationVisitor cacheAv;

    public TraceMethodVisitor(MethodVisitor mv,int access,String className, String methodName,String methodDesc) {
        super(Opcodes.ASM5,mv,access,methodName,methodDesc);
        this.className = className;
        this.methodName = methodName;
        this.returnType = Type.getReturnType(methodDesc);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
        if (Type.getDescriptor(Cache.class).equals(descriptor)) {
            cacheAv = new CacheAnnotationVisitor(av);
            return cacheAv;
        }
        return av;
    }

    @Override
    public void visitCode() {
        mv.visitCode();
        //trace
        push(this.className.replace('/','.') + "." + this.methodName);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/trace/core/Trace", "enter", "(Ljava/lang/String;)V", false);
        //cache
        if (cacheAv != null) {
            push(cacheAv.cacheParam.group);
            push(cacheAv.cacheParam.key);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/cache/redis/HessianRedisTemplate", "getObject", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;", false);
            //因为redis返回的是Object 需要与方法的返回类型匹配
            //不匹配 1.如果是对象类型就强转为返回值  2.如果是基本类型则要拆箱
            int cacheLocal = newLocal(Type.getType(Object.class));
            storeLocal(cacheLocal);
            loadLocal(cacheLocal);

            Label l1 = new Label();
            mv.visitJumpInsn(Opcodes.IFNULL, l1);
            loadLocal(cacheLocal);

//            checkCast(returnType);
            unbox(returnType);//内部进行是强转与拆箱操作
            returnValue();//与返回类型匹配
//            mv.visitInsn(Opcodes.IRETURN);
            mv.visitLabel(l1);
            mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/lang/Object"}, 0, null);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)
                || opcode == Opcodes.ATHROW) {
            if (cacheAv != null) {
                //当前方法的返回类型如果是基本类型要包装，再放入redis，因为redis方法接收的是Object
                int returnLocal = newLocal(returnType);
                mv.visitInsn(Opcodes.DUP);
                //复制
                storeLocal(returnLocal);
                push(cacheAv.cacheParam.group);
                push(cacheAv.cacheParam.key);
                loadLocal(returnLocal);
                //如果当前方法的返回类型是基本类型需要装箱
                box(returnType);
                push(cacheAv.cacheParam.expire);
                mv.visitFieldInsn(Opcodes.GETSTATIC, "java/util/concurrent/TimeUnit", cacheAv.cacheParam.timeUnit, "Ljava/util/concurrent/TimeUnit;");
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
