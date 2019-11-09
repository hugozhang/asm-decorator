package about.me.cache.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

public class MyArrayAnnotationVisitor extends AnnotationVisitor {

    public MyArrayAnnotationVisitor(AnnotationVisitor av) {
        super(Opcodes.ASM5, av);
    }

    @Override
    public void visit(String name, Object value) {
        super.visit(name, value);
        System.out.println(name + " = " + value );
    }
}
