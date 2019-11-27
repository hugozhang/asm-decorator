package about.me.utils;

import org.objectweb.asm.Type;

public abstract class AsmConstants {

    public static final Type NULL_TYPE = Type.getType(Object.class);
    public static final Type TOP_TYPE = Type.getType(Object.class);

    public static final String OBJECT_INTERNAL = "java/lang/Object";
    public static final String OBJECT_DESC = "L" + OBJECT_INTERNAL + ";";
    public static final Type OBJECT_TYPE = Type.getType(OBJECT_DESC);

    public static final String THROWABLE_INTERNAL = "java/lang/Throwable";
    public static final String THROWABLE_DESC = "L" + THROWABLE_INTERNAL + ";";
    public static final Type THROWABLE_TYPE = Type.getType(THROWABLE_DESC);

}
