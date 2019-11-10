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

    private CacheAnnotationVisitor cacheAnnotation;

    private Label tryCatchStart = new Label();

    private Label tryCatchEnd = new Label();

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
            cacheAnnotation = new CacheAnnotationVisitor(av);
            return cacheAnnotation;
        }
        return av;
    }

    @Override
    public void onMethodEnter() {

        mark(tryCatchStart);

        {
            //trace
            push(this.className.replace('/', '.') + "." + this.methodName);
            visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/trace/core/Trace", "enter", "(Ljava/lang/String;)V", false);
        }

        {
            //cache
            if (cacheAnnotation == null) return;
            push(cacheAnnotation.cacheParam.group);
            push(cacheAnnotation.cacheParam.key);
            visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/cache/redis/HessianRedisTemplate", "getObject", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;", false);
            //因为redis返回的是Object 需要与方法的返回类型匹配
            //不匹配 1.如果是对象类型就强转为返回值  2.如果是基本类型则要拆箱
            int cacheLocal = newLocal(Type.getType(Object.class));
            storeLocal(cacheLocal);
            loadLocal(cacheLocal);

            Label l3 = new Label();
            visitJumpInsn(Opcodes.IFNULL, l3);
            loadLocal(cacheLocal);
            unbox(returnType);//内部进行是强转与拆箱操作
            returnValue();//与返回类型匹配
            visitLabel(l3);
            //重点
            mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/lang/Object"}, 0, null);
        }

    }


    @Override
    public void onMethodExit(int opcode) {
        //有返回值并且有@Cache
        if (opcode == Opcodes.ATHROW || cacheAnnotation == null) return;
        dup();
        //当前方法的返回类型如果是基本类型要包装，再放入redis，因为redis方法接收的是Object
        int returnLocal = newLocal(returnType);
        //复制
        storeLocal(returnLocal);
        push(cacheAnnotation.cacheParam.group);
        push(cacheAnnotation.cacheParam.key);
        loadLocal(returnLocal);
        //如果当前方法的返回类型是基本类型需要装箱
        box(returnType);
        push(cacheAnnotation.cacheParam.expire);
        visitFieldInsn(Opcodes.GETSTATIC, "java/util/concurrent/TimeUnit", cacheAnnotation.cacheParam.timeUnit, "Ljava/util/concurrent/TimeUnit;");
        visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/cache/redis/HessianRedisTemplate", "putObject", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;JLjava/util/concurrent/TimeUnit;)V", false);
        visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/trace/core/Trace", "exit", "()V", false);
    }

    @Override
    public void visitEnd() {
        mark(tryCatchEnd);
        catchException(tryCatchStart,tryCatchEnd,Type.getType(RuntimeException.class));
        //重点
        mv.visitFrame(Opcodes.F_FULL, 1, new Object[]{className}, 1, new Object[]{"java/lang/RuntimeException"});
        dup();
        storeLocal(1);
        loadLocal(1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Exception", "getMessage", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/trace/core/Trace", "exit", "(Ljava/lang/String;)V", false);
        throwException();
    }

    public static void main(String[] args) {
        System.out.println(Type.getInternalName(TimeUnit.class));
    }
}
