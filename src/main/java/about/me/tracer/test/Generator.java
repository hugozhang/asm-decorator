package about.me.tracer.test;

import about.me.tracer.asm.TracerClassVisitor;
import about.me.tracer.core.ThreadLocalTracer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Generator {
    public static void main(String[] args) {
        try {
            ClassReader cr = new ClassReader("about/me/tracer/test/TimerTest");
            // ClassWriter extends ClassVisitor
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassVisitor classAdapter = new TracerClassVisitor(cw);
            // 使给定的访问者访问Java类的ClassReader
            cr.accept(classAdapter, ClassReader.SKIP_DEBUG);
            byte[] data = cw.toByteArray();
            File file = new File("/Users/hugozxh/workspace/tracer/target/classes/about/me/tracer/test/TimerTest.class");
            FileOutputStream fout = new FileOutputStream(file);
            fout.write(data);
            fout.close();
            System.out.println("success!");

            //new TimerTest().m();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
