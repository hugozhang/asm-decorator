package about.me.trace.test.frame;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.*;

/**
 * https://blog.csdn.net/ljz2016/article/details/83345673
 */

public class TimeCountAdapater extends ClassVisitor implements Opcodes {

    private String owner;
    private boolean isInterface;

    public TimeCountAdapater(ClassVisitor classVisitor) {
        super(ASM6, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        owner = name;
        isInterface = (access & ACC_INTERFACE) != 0;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv=cv.visitMethod(access, name, descriptor, signature, exceptions);

        if (!isInterface && mv != null && !name.equals("<init>")) {
            AddTimerMethodAdapter at = new AddTimerMethodAdapter(mv);
            at.lvs = new LocalVariablesSorter(access, descriptor, at);
            at.aa = new AnalyzerAdapter(owner, access, name, descriptor, at.lvs);

            return at.aa;
        }

        return mv;
    }

    public void visitEnd() {
        cv.visitEnd();
    }

    class AddTimerMethodAdapter extends MethodVisitor {
        private int time;
        private int maxStack;
        public LocalVariablesSorter lvs;
        public AnalyzerAdapter aa;

        public AddTimerMethodAdapter(MethodVisitor methodVisitor) {
            super(ASM6, methodVisitor);
        }


        @Override
        public void visitCode() {
            mv.visitCode();
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
            time=lvs.newLocal(Type.LONG_TYPE);
            mv.visitVarInsn(LSTORE, time);
//            maxStack=4;
        }

        @Override
        public void visitInsn(int opcode) {
            if ((opcode >= IRETURN && opcode <= RETURN) || opcode == ATHROW) {

                mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
                mv.visitVarInsn(LLOAD, time);
                mv.visitInsn(LSUB);
                mv.visitVarInsn(LSTORE, time);

                mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                mv.visitVarInsn(LLOAD, time);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(J)V", false);
//                maxStack=Math.max(aa.stack.size()+4,maxStack);
            }
            mv.visitInsn(opcode);
        }

//        @Override
//        public void visitMaxs(int maxStack, int maxLocals) {
//            super.visitMaxs(Math.max(maxStack,this.maxStack), maxLocals);
//        }
    }

    public static void main(String[] args) throws IOException {
        ClassWriter cw=new ClassWriter(ClassWriter.COMPUTE_MAXS);
        TraceClassVisitor tv=new TraceClassVisitor(cw,new PrintWriter(System.out));
        TimeCountAdapater addFiled=new TimeCountAdapater(tv);
        ClassReader classReader=new ClassReader("about.me.trace.test.frame.Receiver");
        classReader.accept(addFiled,ClassReader.EXPAND_FRAMES);

        File file=new File("/Users/hugozxh/workspace/trace/target/classes/about/me/trace/test/frame/Receiver.class");
        String parent=file.getParent();
        File parent1=new File(parent);
        parent1.mkdirs();
        file.createNewFile();
        FileOutputStream fileOutputStream=new FileOutputStream(file);
        fileOutputStream.write(cw.toByteArray());


        new Receiver().do1();

    }
}
