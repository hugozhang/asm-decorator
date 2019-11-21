package about.me.utils;

import about.me.cache.asm.bean.MethodArg;
import about.me.cache.asm.bean.ClassField;
import lombok.extern.slf4j.Slf4j;
import org.objectweb.asm.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class AsmUtils {

    public static Map<String, MethodArg> readMethodArg(String owner) {
        try {
            return readArg(owner);
        } catch (IOException e) {
            log.error(e.getMessage(),e);
        }
        return new HashMap();
    }

    public static Map<String, ClassField> readClassField(String owner) {
        try {
            return readField(owner);
        } catch (IOException e) {
            log.error(e.getMessage(),e);
        }
        return new HashMap();
    }

    private static Map<String, ClassField> readField(String owner) throws IOException {
        final Map<String,ClassField> classFieldMap = new HashMap();
        ClassReader cr = new ClassReader(owner);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(Opcodes.ASM5,cw) {
            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                ClassField klassField = new ClassField();
                klassField.name = name;
                klassField.desc = descriptor;
                classFieldMap.put(name,klassField);
                return super.visitField(access, name, descriptor, signature, value);
            }
        }, 0);
        return classFieldMap;
    }

    private static Map<String, MethodArg> readArg(String owner) throws IOException {
        final Map<String, MethodArg> argMap = new HashMap();
        ClassReader cr = new ClassReader(owner);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cr.accept(new ClassVisitor(Opcodes.ASM5,cw) {
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM5,mv) {
                    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
                        MethodArg arg = new MethodArg();
                        arg.name = name;
                        arg.desc = desc;
                        arg.index = index;
                        argMap.put(name,arg);
                        super.visitLocalVariable(name, desc, signature, start, end, index);
                    }
                };
            }
        }, 0);
        return argMap;
    }

}
