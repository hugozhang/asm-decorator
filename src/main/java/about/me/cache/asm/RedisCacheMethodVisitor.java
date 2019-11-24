package about.me.cache.asm;

import about.me.cache.annotation.Cache;
import about.me.cache.asm.bean.MethodArg;
import about.me.cache.asm.bean.ClassField;
import about.me.trace.asm.TraceMethodVisitor;
import about.me.utils.AsmUtils;
import about.me.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisCacheMethodVisitor extends TraceMethodVisitor {

    private String methodDesc;

    private CacheAnnotationVisitor cacheAnnotation;

    private boolean isCache = Boolean.FALSE;

    private Map<String, MethodArg> methodArg;

    public RedisCacheMethodVisitor(MethodVisitor mv,String owner, int access, String methodName, String methodDesc) {
        super(mv,access,owner,methodName,methodDesc);
        this.methodDesc = methodDesc;
        this.methodArg = AsmUtils.readMethodArg(owner);
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
    private void toString(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                throw new IllegalArgumentException("argument type boolean isn't support.");
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(I)Ljava/lang/String;", false);
                break;
            case Type.FLOAT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(F)Ljava/lang/String;", false);
                break;
            case Type.LONG:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(J)Ljava/lang/String;", false);
                break;
            case Type.DOUBLE:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(D)Ljava/lang/String;", false);
                break;
            case Type.OBJECT:
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;", false);
                break;
        }
    }

    @Override
    public void onMethodEnter() {
        super.onMethodEnter();
        if (!isCache) return;
        //cache
        push(cacheAnnotation.cacheParam.group);
        String cacheKey = cacheAnnotation.cacheParam.key;
        String[] keys = cacheKey.split("\\.");
        int len = keys.length;
        MethodArg arg = this.methodArg.get(keys[0]);
        Type type = Type.getType(arg.desc);
        if (!this.methodArg.containsKey(keys[0])) {
            throw new IllegalArgumentException("argument " + keys[0] + " not exist.");
        }
        loadArg(arg.index - 1);//非静态方法第一个参数是this
        if (len == 1) {
            toString(type);
        } else if (len == 2) {
            Map<String, ClassField> classField = AsmUtils.readClassField(type.getInternalName());
            if (!classField.containsKey(keys[1])) {
                throw new IllegalArgumentException("argument " +keys[0] + " -> " + type.getClassName() + " not exist field -> " + keys[1] + ".");
            }
            ClassField cf = classField.get(keys[1]);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, arg.desc, "get"+ StringUtils.capitalize(keys[1]), "()" + cf.desc, false);
            toString(Type.getType(cf.desc));
        } else {
            throw new IllegalArgumentException("argument " + cacheKey + " invalid.");
        }
        visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/cache/redis/HessianRedisTemplate", "getObject", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;", false);
        //因为redis返回的是Object 需要与方法的返回类型匹配
        //不匹配 1.如果是对象类型就强转为返回值  2.如果是基本类型则要拆箱
        int cacheLocal = newLocal(Type.getType(Object.class));
        storeLocal(cacheLocal);
        loadLocal(cacheLocal);
        Label l0 = new Label();
        ifNull(l0);
        loadLocal(cacheLocal);
        unbox(Type.getReturnType(this.methodDesc));//内部进行是强转与拆箱操作
        //重点
        returnValue();//与返回类型匹配
        visitLabel(l0);
        //有分支语句 必须有visitFrame这个检查class文件  jdk 1.7以后 没有会提示以下类似的错误
        //java.lang.VerifyError: Expecting a stackmap frame at branch target
        mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/lang/Object"}, 0, null);
    }

    @Override
    public void onMethodExit(int opcode) {
        super.onMethodExit(opcode);
        if (opcode == Opcodes.ATHROW || opcode == Opcodes.RETURN || !isCache) return;
        //有返回值并且有@Cache
        dup();
        box(Type.getReturnType(this.methodDesc));//主要解决返回基本类型的情况
        //如果不想存临时变量，就使用把栈顶元素交换两次 swap
        int returnLocal = newLocal(Type.getType(Object.class));
        storeLocal(returnLocal);
        push(cacheAnnotation.cacheParam.group);
//        swap();
        push(cacheAnnotation.cacheParam.key);
//        swap();
        loadLocal(returnLocal);
        push(cacheAnnotation.cacheParam.expire);
        visitFieldInsn(Opcodes.GETSTATIC, "java/util/concurrent/TimeUnit", cacheAnnotation.cacheParam.timeUnit, "Ljava/util/concurrent/TimeUnit;");
        visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/cache/redis/HessianRedisTemplate", "putObject", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;JLjava/util/concurrent/TimeUnit;)V", false);
//        以下两个是测试方法
//        visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Print.class), "print", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;J)V", false);
//        visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Print.class), "print2", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;J)V", false);
    }

    public static void main(String[] args) {

        System.out.println(Type.getInternalName(TimeUnit.class));

        System.out.println("a.b".split("\\.")[0]);

        String a = "asdasd[99]";
        if (a.indexOf("[") < 0 || a.indexOf("]") < 0) {

        }
        System.out.println(a.substring(a.indexOf("[") + 1,a.indexOf("]")));

        System.out.println(a.indexOf("["));
        System.out.println(a.indexOf("]"));
    }
}
