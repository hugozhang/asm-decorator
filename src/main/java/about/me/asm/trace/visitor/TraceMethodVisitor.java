package about.me.asm.trace.visitor;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.concurrent.TimeUnit;

public class TraceMethodVisitor extends AdviceAdapter {

    private String owner;

    private String methodName;

    public TraceMethodVisitor(MethodVisitor mv,int access,String owner, String methodName,String methodDesc) {
        super(Opcodes.ASM5,mv,access,methodName,methodDesc);
        this.owner = owner;
        this.methodName = methodName;
    }

    @Override
    public void onMethodEnter() {
        //trace
        push(this.owner.replace('/', '.') + "." + this.methodName);
        visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/asm/trace/bean/Trace", "enter", "(Ljava/lang/String;)V", false);
    }


    //方法退出的时候都会执行它（包括if,exception,正常退出）如果方法里面有try{if(true){throw new RuntimeException("123");}}catch(Exception e){throw e;} 会执行两次exit
    @Override
    public void onMethodExit(int opcode) {
        //不能放在visitEnd()
        if (opcode == Opcodes.ATHROW) {
            dup();
            visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "getMessage", "()Ljava/lang/String;", false);
            visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/asm/trace/bean/Trace", "exit", "(Ljava/lang/String;)V", false);
        } else {
            visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/asm/trace/bean/Trace", "exit", "()V", false);
        }
    }

    public static void main(String[] args) {
        System.out.println(Type.getInternalName(TimeUnit.class));
    }
}
