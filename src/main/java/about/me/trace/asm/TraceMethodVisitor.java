package about.me.trace.asm;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.concurrent.TimeUnit;

public class TraceMethodVisitor extends AdviceAdapter {

    private String className;

    private String methodName;

    public TraceMethodVisitor(MethodVisitor mv,int access,String className, String methodName,String methodDesc) {
        super(Opcodes.ASM5,mv,access,methodName,methodDesc);
        this.className = className;
        this.methodName = methodName;
    }

    @Override
    public void onMethodEnter() {
        //trace
        push(this.className.replace('/', '.') + "." + this.methodName);
        visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/trace/core/Trace", "enter", "(Ljava/lang/String;)V", false);
    }


    //方法退出的时候都会执行它（包括if,exception,正常退出）如果方法里面有try{if(true){throw new RuntimeException("123");}}catch(Exception e){throw e;} 会执行两次exit
    @Override
    public void onMethodExit(int opcode) {
        //不能放在visitEnd()
        if (opcode == ATHROW) {
            dup();
            visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "getMessage", "()Ljava/lang/String;", false);
            visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/trace/core/Trace", "exit", "(Ljava/lang/String;)V", false);
        } else {
            visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/trace/core/Trace", "exit", "()V", false);
        }
    }

    public static void main(String[] args) {
        System.out.println(Type.getInternalName(TimeUnit.class));
    }
}
