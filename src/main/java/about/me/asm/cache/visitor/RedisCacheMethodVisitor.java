package about.me.asm.cache.visitor;

import about.me.asm.cache.annotation.MyCache;
import about.me.asm.cache.annotation.MyCacheEvict;
import about.me.asm.cache.bean.MethodArg;
import about.me.asm.cache.bean.ClassField;
import about.me.utils.AsmUtils;
import about.me.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisCacheMethodVisitor extends AdviceAdapter {

    private String owner;

    private String methodName;

    private String methodDesc;

    private String target;

    private CacheAnnotationVisitor cacheAnnotation;

    private boolean isCache = Boolean.FALSE;

    private boolean isCacheEvict = Boolean.FALSE;

    private Map<String, MethodArg> methodArg;

    public RedisCacheMethodVisitor(MethodVisitor mv, int access,String owner, String methodName, String methodDesc) {
        super(ASM5,mv,access,methodName,methodDesc);
        this.owner = owner;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.methodArg = AsmUtils.readMethodArg(owner);
        this.target = this.owner.replace('/', '.') + "." + this.methodName;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
        if (Type.getDescriptor(MyCache.class).equals(descriptor)) {
            isCache = Boolean.TRUE;
            cacheAnnotation = new CacheAnnotationVisitor(av);
            return cacheAnnotation;
        } else if (Type.getDescriptor(MyCacheEvict.class).equals(descriptor)) {
            isCacheEvict = Boolean.TRUE;
            cacheAnnotation = new CacheAnnotationVisitor(av);
            return cacheAnnotation;
        }
        return av;
    }

    //缓存的暴露接口参数用Object，就不用转了
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
                if (!type.getClassName().equals(String.class.getName())) {
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "toString", "()Ljava/lang/String;", false);
                }
                break;
        }
    }

    private void parseKey () {
        String cacheKey = cacheAnnotation.cacheParam.key;
        String[] keys = cacheKey.split("\\.");
        int len = keys.length;
        MethodArg arg = this.methodArg.get(keys[0]);
        if (!this.methodArg.containsKey(keys[0])) {
            throw new IllegalArgumentException("argument `" + keys[0] + "` not exist,target is `" + this.target + "`.");
        }
        Type type = Type.getType(arg.desc);
        loadArg(arg.index - 1);//非静态方法第一个参数是this
        if (len == 1) {
            box(type);//基本类型需要装箱
        } else if (len == 2) {
            Map<String, ClassField> classField = AsmUtils.readClassField(type.getInternalName());
            if (!classField.containsKey(keys[1])) {
                throw new IllegalArgumentException("argument `" +keys[0] + "` -> `" + this.target + "` not exist field -> `" + keys[1] + "`.");
            }
            ClassField cf = classField.get(keys[1]);
            visitMethodInsn(Opcodes.INVOKEVIRTUAL, arg.desc, "get"+ StringUtils.capitalize(keys[1]), "()" + cf.desc, false);
            box(Type.getType(cf.desc));
        } else {
            throw new IllegalArgumentException("argument `" + cacheKey + "` invalid,target is `" + this.target + "`.");
        }
    }

    @Override
    public void onMethodEnter() {
        if (isCache && isCacheEvict) {
            throw new IllegalArgumentException("@MyCache and @MyCacheEvict can't on the method at the same time,target is `" + this.target + "`.");
        }
        cacheOnMethodEnter();
    }

    private void cacheOnMethodEnter() {
        if (!isCache) return;
        //cache
        push(cacheAnnotation.cacheParam.group);
        parseKey();
        visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/asm/cache/redis/HessianRedisTemplate", "getObject", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
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
        //mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/lang/Object"}, 0, null);
    }

    @Override
    public void onMethodExit(int opcode) {
        cacheOnMethodExit(opcode);
        cacheEvictOnMethodExit(opcode);
    }

    private void cacheOnMethodExit(int opcode) {
        if (opcode == Opcodes.ATHROW || opcode == Opcodes.RETURN || !isCache) return;
        //有返回值并且有@MyCache
        dup();
        box(Type.getReturnType(this.methodDesc));//主要解决返回基本类型的情况
        //如果不想存临时变量，就使用把栈顶元素交换两次 swap
        int returnLocal = newLocal(Type.getType(Object.class));
        storeLocal(returnLocal);
        push(cacheAnnotation.cacheParam.group);
        /*swap();*/
        parseKey();
        /*swap();*/
        loadLocal(returnLocal);
        push(cacheAnnotation.cacheParam.expire);
        visitFieldInsn(Opcodes.GETSTATIC, "java/util/concurrent/TimeUnit", cacheAnnotation.cacheParam.timeUnit, "Ljava/util/concurrent/TimeUnit;");
        visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/asm/cache/redis/HessianRedisTemplate", "putObject", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;JLjava/util/concurrent/TimeUnit;)V", false);
        /*
        以下两个是测试方法
        visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Print.class), "print", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;J)V", false);
        visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Print.class), "print2", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;J)V", false);
        */
    }

    private void cacheEvictOnMethodExit(int opcode) {
        if (opcode == Opcodes.ATHROW || !isCacheEvict) return;
        if (opcode != Opcodes.RETURN) {
            throw new IllegalArgumentException("@MyCacheEvict invalid return value,target is `" + this.target + "`.");
        }
        push(cacheAnnotation.cacheParam.group);
        parseKey();
        visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/asm/cache/redis/HessianRedisTemplate", "removeObject", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Long;", false);
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
