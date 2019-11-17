package about.me.cache.asm;

import about.me.cache.annotation.Cache;
import about.me.trace.test.stati.Print;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.concurrent.TimeUnit;


public class RedisCacheMethodVisitor extends AdviceAdapter {

    private Type returnType;

    private CacheAnnotationVisitor cacheAnnotation;

    private boolean isCache = Boolean.FALSE;

    public RedisCacheMethodVisitor(MethodVisitor mv, int access, String methodName, String methodDesc) {
        super(Opcodes.ASM5,mv,access,methodName,methodDesc);
        this.returnType = Type.getReturnType(methodDesc);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
        if (Type.getDescriptor(Cache.class).equals(descriptor)) {
            cacheAnnotation = new CacheAnnotationVisitor(av);
            isCache = Boolean.TRUE;
            return cacheAnnotation;
        }
        return av;
    }

    @Override
    public void visitCode() {
        if (!isCache) return;
        //cache
        push(cacheAnnotation.cacheParam.group);
        push(cacheAnnotation.cacheParam.key);
        visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/cache/redis/HessianRedisTemplate", "getObject", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;", false);
        //因为redis返回的是Object 需要与方法的返回类型匹配
        //不匹配 1.如果是对象类型就强转为返回值  2.如果是基本类型则要拆箱
        int cacheLocal = newLocal(Type.getType(Object.class));
        storeLocal(cacheLocal);

        loadLocal(cacheLocal);
        Label l0 = new Label();
        ifNull(l0);
        loadLocal(cacheLocal);
        unbox(returnType);//内部进行是强转与拆箱操作
        //重点
        returnValue();//与返回类型匹配
        visitLabel(l0);
        //有分支语句 必须有这个
        mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/lang/Object"}, 0, null);
    }

    @Override
    public void onMethodExit(int opcode) {
        if (opcode == Opcodes.ATHROW || !isCache) return;
        //有返回值并且有@Cache
        push(cacheAnnotation.cacheParam.group);
        push(cacheAnnotation.cacheParam.key);
        dup();
        //当前方法的返回类型如果是基本类型要包装，再放入redis，因为redis方法接收的是Object
        box(returnType);
        push(cacheAnnotation.cacheParam.expire);
        visitFieldInsn(Opcodes.GETSTATIC, "java/util/concurrent/TimeUnit", cacheAnnotation.cacheParam.timeUnit, "Ljava/util/concurrent/TimeUnit;");
        visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/cache/redis/HessianRedisTemplate", "putObject", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;JLjava/util/concurrent/TimeUnit;)V", false);
//        visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Print.class), "print", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;J)V", false);
    }

    public static void main(String[] args) {
        System.out.println(Type.getInternalName(TimeUnit.class));
    }
}
