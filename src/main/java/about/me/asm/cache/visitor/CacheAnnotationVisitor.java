package about.me.asm.cache.visitor;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

public class CacheAnnotationVisitor extends AnnotationVisitor {

    public CacheParam cacheParam = new CacheParam();

    public CacheAnnotationVisitor(AnnotationVisitor av) {
        super(Opcodes.ASM5,av);
    }

    @Override
    public void visit(String name, Object value) {
        switch (name) {
            case "group":
                cacheParam.group = (String) value;
                break;
            case "key" :
                cacheParam.key = (String) value;
                break;
            case "expire" :
                cacheParam.expire = (long) value;

        }
    }

    @Override
    public void visitEnum(String name, String desc, String value) {
        cacheParam.timeUnit = value;
    }

    public class CacheParam {
        public String group;
        public String key;
        public long expire;
        public String timeUnit;
    }
}
