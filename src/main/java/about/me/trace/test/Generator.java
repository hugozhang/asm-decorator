package about.me.trace.test;

//import org.apache.dubbo.common.timer.HashedWheelTimer;
//import org.apache.dubbo.common.timer.Timeout;
//import org.apache.dubbo.common.timer.TimerTask;
import about.me.trace.asm.TraceClassVisitor;
import about.me.trace.test.bean.TimerTest;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class Generator {
    public static void main(String[] args) {


//        HashedWheelTimer hashedWheelTimer = new HashedWheelTimer(2, TimeUnit.SECONDS);
//
//        hashedWheelTimer.newTimeout(new TimerTask(){
//
//            @Override
//            public void run(Timeout timeout) throws Exception {
//                System.out.println("222");
//            }
//        },1,TimeUnit.SECONDS);

//        new TimerTest().get();
//
        try {
            ClassReader cr = new ClassReader("about/me/trace/test/bean/TimerTest");
            // ClassWriter extends ClassVisitor
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassVisitor classAdapter = new TraceClassVisitor(cw);
            // 使给定的访问者访问Java类的ClassReader
            cr.accept(classAdapter, ClassReader.EXPAND_FRAMES);
            byte[] data = cw.toByteArray();
            File file = new File("/Users/hugozxh/workspace/trace/target/classes/about/me/trace/test/bean/TimerTest.class");
            FileOutputStream fout = new FileOutputStream(file);
            fout.write(data);
            fout.close();
            System.out.println("success!");

            User user = new User();
            user.setName("java");

            System.out.println(new TimerTest().get(user));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } /*catch (Exception e) {
            e.printStackTrace();
            Trace.exit(e.getMessage());
        }*/
    }
}
