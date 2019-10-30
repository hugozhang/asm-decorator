package about.me.tracer.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TracerMethodVisitor extends MethodVisitor {

    private String className;

    private String methodName;

    public TracerMethodVisitor(MethodVisitor mv,String className,String methodName) {
        super(Opcodes.ASM5, mv);
        this.className = className;
        this.methodName = methodName;
    }

    @Override
    public void visitCode() {
        mv.visitCode();
        mv.visitLdcInsn(this.className.replace('/','.') + "." + this.methodName);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/tracer/core/Tracer", "enter", "(Ljava/lang/String;)V", true);
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)
                || opcode == Opcodes.ATHROW) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/tracer/core/Tracer", "exit", "()V", true);
        }
        mv.visitInsn(opcode);
    }
}
