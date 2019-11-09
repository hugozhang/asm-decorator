package about.me.cache.asm;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;

public class CacheAnnotationVisitor extends AnnotationVisitor {

    public Map<String,Object> annMap = new HashMap<>();

    public CacheAnnotationVisitor(AnnotationVisitor av) {
        super(Opcodes.ASM5,av);
    }

    @Override
    public void visit(String name, Object value) {
        super.visit(name, value);
        annMap.put(name,value);
    }

    @Override
    public void visitEnum(String name, String desc, String value) {
        super.visitEnum(name, desc, value);
        annMap.put(name,value);
    }
}
