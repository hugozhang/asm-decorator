package about.me.trace.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class TraceMethodVisitor extends MethodVisitor {

    private String className;

    private String methodName;

    public TraceMethodVisitor(MethodVisitor mv, String className, String methodName) {
        super(Opcodes.ASM5, mv);
        this.className = className;
        this.methodName = methodName;
    }

    @Override
    public void visitCode() {
        mv.visitCode();
        mv.visitLdcInsn(this.className.replace('/','.') + "." + this.methodName);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/trace/core/Trace", "enter", "(Ljava/lang/String;)V", false);
    }

    @Override
    public void visitInsn(int opcode) {
        if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)
                || opcode == Opcodes.ATHROW) {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "about/me/trace/core/Trace", "exit", "()V", false);
        }
        mv.visitInsn(opcode);
    }
}
