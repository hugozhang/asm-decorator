package about.me.asm;

import about.me.utils.AsmConstants;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.Method;

import java.util.*;

public class MyMethodVisitor extends MethodVisitor {

    private static final Object TOP_EXT = -2;

    private static final class LocalVarSlot {
        final int idx;
        final Object type;
        private boolean expired = false;

        LocalVarSlot(int idx, Object type) {
            this.idx = Math.abs(idx);
            this.type = type;
        }

        void expire() {
            expired = true;
        }

        boolean isExpired() {
            return expired;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 97 * hash + this.idx;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LocalVarSlot other = (LocalVarSlot) obj;
            if (this.idx != other.idx) {
                return false;
            }
            return true;
        }
    }

    private static final class SimulatedStack {
        private static final int DEFAULT_CAPACITY = 16;
        private int stackPtr = 0;
        private int maxStack = 0;
        private Object[] stack = new Object[DEFAULT_CAPACITY];

        public SimulatedStack() {
        }

        SimulatedStack(Object[] other) {
            replaceWith(other);
        }

        private void fitResize(int ptr) {
            if (ptr >= stack.length) {
                stack = Arrays.copyOf(stack, Math.max(stack.length * 2, stackPtr + 1));
            }
        }

        public void push1(Object val) {
            fitResize(stackPtr);
            stack[stackPtr++] = val;
            maxStack = Math.max(stackPtr, maxStack);
        }

        public void push(Object val) {
            fitResize(stackPtr);

            stack[stackPtr++] = val;
            if (val == Opcodes.LONG || val == Opcodes.DOUBLE) {
                fitResize(stackPtr);
                stack[stackPtr++] = TOP_EXT;
            }
            maxStack = Math.max(stackPtr, maxStack);
        }

        public Object pop1() {
            if (!isEmpty()) {
                return stack[--stackPtr];
            }
            return Opcodes.TOP;
        }

        public Object pop() {
            if (!isEmpty()) {
                Object val = stack[--stackPtr];
                if (val == TOP_EXT) {
                    val = stack[--stackPtr];
                }
                return val;
            }
            return Opcodes.TOP;
        }

        public Object peek() {
            if (!isEmpty()) {
                return stack[stackPtr - 1];
            }
            return Opcodes.TOP;
        }


        public boolean isEmpty() {
            return stackPtr == 0;
        }


        public void reset() {
            stackPtr = 0;
            stack = new Object[DEFAULT_CAPACITY];
        }

        public Object[] toArray() {
            return toArray(false);
        }

        public Object[] toArray(boolean compress) {
            Object[] ret = new Object[stackPtr];
            int localCnt = 0;
            for (int i = 0; i < stackPtr; i++) {
                Object o = stack[i];
                if (o != null) {
                    if (!compress || o != TOP_EXT) {
                        ret[localCnt++] = o;
                    }
                }
            }
            return Arrays.copyOf(ret, localCnt);
        }

        public void replaceWith(Object[] other) {
            if (other.length > 0) {
                Object[] arr = new Object[other.length * 2];
                int idx = 0;
                for (int ptr = 0; ptr < other.length; ptr++) {
                    Object o = other[ptr];
                    arr[idx++] = o;
                    if (o == Opcodes.DOUBLE || o == Opcodes.LONG) {
                        int next = ptr + 1;
                        if (next >= other.length || (other[next] != null && other[next] != TOP_EXT)) {
                            arr[idx++] = TOP_EXT;
                        }
                    }
                }
                stack = Arrays.copyOf(arr, idx);
                stackPtr = idx;
            } else {
                reset();
            }
            maxStack = Math.max(stackPtr, maxStack);
        }
    }

    private static class LocalVarTypes {
        private static final int DEFAULT_SIZE = 4;
        private Object[] locals;
        private int lastVarPtr = -1;
        private int maxVarPtr = -1;

        LocalVarTypes() {
            locals = new Object[DEFAULT_SIZE];
        }

        LocalVarTypes(Object[] vars) {
            replaceWith(vars);
        }

        public void setType(int idx, Type t) {
            int padding = t.getSize() - 1;
            if ((idx + padding) >= locals.length) {
                locals = Arrays.copyOf(locals, Math.round((idx + padding + 1) * 1.5f));
            }
            locals[idx] = toSlotType(t);
            if (padding == 1) {
                locals[idx + 1] = TOP_EXT;
            }
            setLastVarPtr(Math.max(idx + padding, lastVarPtr));
        }

        public Object getType(int idx) {
            return idx < locals.length ? locals[idx] : null;
        }

        public final void replaceWith(Object[] other) {
            Object[] arr = new Object[other.length * 2];
            int idx = 0;
            for (int i = 0; i < other.length; i ++) {
                Object o = other[i];
                arr[idx++] = o;
                if (o == Opcodes.LONG || o == Opcodes.DOUBLE) {
                    int lookup = i + 1;
                    if (lookup == other.length || other[lookup] != TOP_EXT) {
                        arr[idx++] = TOP_EXT;
                    }
                }
            }
            locals = Arrays.copyOf(arr, idx);
            setLastVarPtr(idx - 1);
        }

        public void mergeWith(Object[] other) {
            Object[] arr = new Object[Math.max(other.length * 2, Math.max(lastVarPtr + 1, DEFAULT_SIZE))];
            int idx = 0;
            for (Object o : other) {
                arr[idx++] = o == null ? Opcodes.TOP : o;
            }
            while (idx <= lastVarPtr) {
                arr[idx++] = Opcodes.TOP;
            }
            locals = arr;
            setLastVarPtr(idx - 1);
        }

        public Object[] toArray() {
            return toArray(false);
        }

        public Object[] toArray(boolean compress) {
            Object[] ret = new Object[size()];
            int localCnt = 0;
            for (int i = 0; i <= lastVarPtr; i++) {
                Object o = locals[i];
                if (o != null) {
                    if (!compress || o != TOP_EXT) {
                        ret[localCnt++] = o;
                    }
                } else {
                    ret[localCnt++] = Opcodes.TOP;
                }
            }
            return Arrays.copyOf(ret, localCnt);
        }

        public void reset() {
            locals = new Object[DEFAULT_SIZE];
            setLastVarPtr(-1);
        }

        public int size() {
            return lastVarPtr + 1;
        }

        public int maxSize() {
            return maxVarPtr + 1;
        }

        private void setLastVarPtr(int ptr) {
            lastVarPtr = ptr;
            maxVarPtr = Math.max(lastVarPtr, maxVarPtr);
        }
    }

    private static final class SavedState {
        static final int CONDITIONAL = 0;
        static final int UNCONDITIONAL = 1;
        static final int EXCEPTION = 2;

        private final LocalVarTypes lvTypes;
        private final SimulatedStack sStack;
        private final Collection<LocalVarSlot> newLocals;
        private final int kind;

        SavedState(LocalVarTypes lvTypes, SimulatedStack sStack, Collection<LocalVarSlot> newLocals, int kind) {
            this.lvTypes = new LocalVarTypes(lvTypes.toArray());
            this.sStack = new SimulatedStack(sStack.toArray());
            this.newLocals = new HashSet<>(newLocals);
            this.kind = kind;
        }

    }

    private int nextMappedVar = 0;
    private int[] mapping = new int[8];

    private final SimulatedStack stack = new SimulatedStack();
    private final List<Object> locals = new ArrayList<>();
    private final Set<LocalVarSlot> newLocals = new HashSet<>(3);
    private final LocalVarTypes localTypes = new LocalVarTypes();
    private final Map<Label, SavedState> jumpTargetStates = new HashMap<>();
    private final Map<Label, Label> tryCatchHandlerMap = new HashMap<>();

    private int argsSize = 0;
    private int localsTailPtr = 0;
    private int firstLocal;

    private int access;

    private String owner, desc;

    private int pc = 0, lastFramePc = Integer.MIN_VALUE;

    private Type[] argumentTypes;

    private Type returnType;

    public MyMethodVisitor(int access, String owner, String name, String desc, MethodVisitor mv) {
        super(Opcodes.ASM5, mv);
        this.access = access;
        this.owner = owner;
        this.desc = desc;
        this.returnType = Type.getReturnType(desc);
        this.argumentTypes = Type.getArgumentTypes(desc);

        initLocals((access & Opcodes.ACC_STATIC) == 0);
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        if (lastFramePc == pc) {
            return;
        }
        lastFramePc = pc;

        switch (type) {
            case Opcodes.F_NEW: // fallthrough
            case Opcodes.F_FULL: {
                this.locals.clear();
                this.stack.reset();

                for (int i = 0; i < nLocal; i++) {
                    Object e = local[i];
                    this.locals.add(e);
                }
                localsTailPtr = nLocal;

                for (int i = 0; i < nStack; i++) {
                    Object e = stack[i];
                    this.stack.push(e);
                }
                break;
            }
            case Opcodes.F_SAME: {
                this.stack.reset();
                break;
            }
            case Opcodes.F_SAME1: {
                this.stack.reset();
                Object e = stack[0];
                this.stack.push(e);
                break;
            }
            case Opcodes.F_APPEND: {
                this.stack.reset();
                int top = this.locals.size();
                for (int i = 0; i < nLocal; i++) {
                    Object e = local[i];
                    if (localsTailPtr < top) {
                        this.locals.set(localsTailPtr, e);
                    } else {
                        this.locals.add(e);
                    }
                    localsTailPtr++;
                }
                break;
            }
            case Opcodes.F_CHOP: {
                this.stack.reset();
                for (int i = 0; i < nLocal; i++) {
                    this.locals.remove(--localsTailPtr);
                }
                break;
            }
        }

        Object[] localsArr = computeFrameLocals();
        localTypes.replaceWith(localsArr);

        int off = 0;
        for (int i = 0; i < localsArr.length; i++) {
            Object val = localsArr[i];
            if (val == TOP_EXT) {
                off++;
                continue;
            }
            if (off > 0) {
                localsArr[i - off] = localsArr[i];
            }
        }
        localsArr = Arrays.copyOf(localsArr, localsArr.length - off);
        Object[] tmpStack = this.stack.toArray(true);

        super.visitFrame(Opcodes.F_NEW, localsArr.length, localsArr, tmpStack.length, tmpStack);
    }

    @Override
    public void visitMultiANewArrayInsn(String type, int dims) {
        for (int i = 0; i < dims; i++) {
            stack.pop();
        }
        stack.push(type);
        super.visitMultiANewArrayInsn(type, dims);
        pc++;
    }

    @Override
    public void visitLookupSwitchInsn(Label label, int[] ints, Label[] labels) {
        stack.pop();
        super.visitLookupSwitchInsn(label, ints, labels);
        pc++;
    }

    @Override
    public void visitTableSwitchInsn(int i, int i1, Label label, Label... labels) {
        stack.pop();
        super.visitTableSwitchInsn(i, i1, label, labels);
        pc++;
    }

    @Override
    public void visitLdcInsn(Object o) {
        Type t = Type.getType(o.getClass());
        switch (t.getInternalName()) {
            case "java/lang/Integer": {
                pushToStack(Type.INT_TYPE);
                break;
            }
            case "java/lang/Long": {
                pushToStack(Type.LONG_TYPE);
                break;
            }
            case "java/lang/Byte": {
                pushToStack(Type.BYTE_TYPE);
                break;
            }
            case "java/lang/Short": {
                pushToStack(Type.SHORT_TYPE);
                break;
            }
            case "java/lang/Character": {
                pushToStack(Type.CHAR_TYPE);
                break;
            }
            case "java/lang/Boolean": {
                pushToStack(Type.BOOLEAN_TYPE);
                break;
            }
            case "java/lang/Float": {
                pushToStack(Type.FLOAT_TYPE);
                break;
            }
            case "java/lang/Double": {
                pushToStack(Type.DOUBLE_TYPE);
                break;
            }
            default: {
                pushToStack(t);
            }
        }
        super.visitLdcInsn(o);
        pc++;
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
        pc++;
        switch(opcode) {
            case Opcodes.IFEQ:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
            case Opcodes.IFLT:
            case Opcodes.IFNE:
            case Opcodes.IFNONNULL:
            case Opcodes.IFNULL: {
                stack.pop();
                break;
            }
            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE:
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT:
            case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPNE: {
                stack.pop();
                stack.pop();
                break;
            }
        }
        jumpTargetStates.put(label, new SavedState(
                        localTypes, stack, newLocals,
                        opcode == Opcodes.GOTO || opcode == Opcodes.JSR ?
                                SavedState.UNCONDITIONAL : SavedState.CONDITIONAL
                )
        );
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle handle, Object... bsmArgs) {
        Type[] args = Type.getArgumentTypes(desc);
        Type ret = Type.getReturnType(desc);

        for(int i = args.length - 1; i >= 0; i--) {
            if (!args[i].equals(Type.VOID_TYPE)) {
                popFromStack(args[i]);
            }
        }
        super.visitInvokeDynamicInsn(name, desc, handle, bsmArgs);
        pc++;

        if (!ret.equals(Type.VOID_TYPE)) {
            pushToStack(ret);
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
        Type[] args = Type.getArgumentTypes(desc);
        Type ret = Type.getReturnType(desc);

        for(int i = args.length - 1; i >= 0; i--) {
            if (!args[i].equals(Type.VOID_TYPE)) {
                popFromStack(args[i]);
            }
        }

        if (opcode != Opcodes.INVOKESTATIC) {
            stack.pop();
        }
        super.visitMethodInsn(opcode, owner, name, desc, isInterface);
        pc++;

        if (!ret.equals(Type.VOID_TYPE)) {
            pushToStack(ret);
        }
        if (opcode == Opcodes.INVOKESPECIAL && name.equals("<init>")) {
            if (stack.peek() instanceof Label) {
                stack.pop();
                pushToStack(Type.getObjectType(owner));
            }
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        Type t = Type.getType(desc);
        super.visitFieldInsn(opcode, owner, name, desc);
        pc++;

        if (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC) {
            popFromStack(t);
        }
        if (opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD) {
            stack.pop(); // pop 'this'
        }
        if (opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC) {
            pushToStack(t);
        }
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
        pc++;

        switch (opcode) {
            case Opcodes.NEW: {
                pushToStack(Type.getObjectType(type));
                break;
            }
            case Opcodes.ANEWARRAY: {
                stack.pop();

                pushToStack(Type.getType("[L" + type + ";"));
                break;
            }
            case Opcodes.INSTANCEOF: {
                stack.pop();
                pushToStack(Type.BOOLEAN_TYPE);
                break;
            }
            case Opcodes.CHECKCAST: {
                stack.pop();
                pushToStack(Type.getObjectType(type));
                break;
            }
        }
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        int size = 1;

        switch (opcode) {
            case Opcodes.DLOAD:
            case Opcodes.LLOAD:
            case Opcodes.DSTORE:
            case Opcodes.LSTORE: {
                size++;
                break;
            }
        }
        var = remap(var, size);

        boolean isPush = false;
        Type opType = null;
        switch (opcode) {
            case Opcodes.ILOAD: {
                opType = Type.INT_TYPE;
                isPush = true;
                break;
            }
            case Opcodes.LLOAD: {
                opType = Type.LONG_TYPE;
                isPush = true;
                break;
            }
            case Opcodes.FLOAD: {
                opType = Type.FLOAT_TYPE;
                isPush = true;
                break;
            }
            case Opcodes.DLOAD: {
                opType = Type.DOUBLE_TYPE;
                isPush = true;
                break;
            }
            case Opcodes.ALOAD: {
                Object o = localTypes.getType(var);
                opType = fromSlotType(o);
                isPush = true;
                break;
            }
            case Opcodes.ISTORE: {
                opType = Type.INT_TYPE;
                break;
            }
            case Opcodes.LSTORE: {
                opType = Type.LONG_TYPE;
                break;
            }
            case Opcodes.FSTORE: {
                opType = Type.FLOAT_TYPE;
                break;
            }
            case Opcodes.DSTORE: {
                opType = Type.DOUBLE_TYPE;
                break;
            }
            case Opcodes.ASTORE: {
                opType = fromSlotType(stack.peek());
                break;
            }
        }

        if (isPush) {
            pushToStack(opType);
        } else {
            popFromStack(opType);
            localTypes.setType(var, opType);
        }

        super.visitVarInsn(opcode, var);
        pc++;
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
        pc++;

        switch (opcode) {
            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH: {
                stack.push(Opcodes.INTEGER);
                break;
            }
            case Opcodes.NEWARRAY: {
                popFromStack(Type.INT_TYPE); // size
                switch (operand) {
                    case Opcodes.T_BOOLEAN: {
                        pushToStack(Type.getObjectType("[Z"));
                        break;
                    }
                    case Opcodes.T_CHAR: {
                        pushToStack(Type.getObjectType("[C"));
                        break;
                    }
                    case Opcodes.T_FLOAT: {
                        pushToStack(Type.getObjectType("[F"));
                        break;
                    }
                    case Opcodes.T_DOUBLE: {
                        pushToStack(Type.getObjectType("[D"));
                        break;
                    }
                    case Opcodes.T_BYTE: {
                        pushToStack(Type.getObjectType("[B"));
                        break;
                    }
                    case Opcodes.T_SHORT: {
                        pushToStack(Type.getObjectType("[S"));
                        break;
                    }
                    case Opcodes.T_INT: {
                        pushToStack(Type.getObjectType("[I"));
                        break;
                    }
                    case Opcodes.T_LONG: {
                        pushToStack(Type.getObjectType("[J"));
                        break;
                    }
                }
                break;
            }
        }
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        pc++;

        switch (opcode) {
            case Opcodes.ACONST_NULL: {
                stack.push(Opcodes.NULL);
                break;
            }
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
            case Opcodes.ICONST_M1: {
                pushToStack(Type.INT_TYPE);
                break;
            }
            case Opcodes.FCONST_0:
            case Opcodes.FCONST_1:
            case Opcodes.FCONST_2: {
                pushToStack(Type.FLOAT_TYPE);
                break;
            }
            case Opcodes.LCONST_0:
            case Opcodes.LCONST_1: {
                pushToStack(Type.LONG_TYPE);
                break;
            }
            case Opcodes.DCONST_0:
            case Opcodes.DCONST_1: {
                pushToStack(Type.DOUBLE_TYPE);
                break;
            }
            case Opcodes.AALOAD: {
                stack.pop(); // index
                Object target = stack.pop();

                if (target instanceof String) {
                    Type t;
                    String typeStr = (String)target;
                    if (typeStr.startsWith("[")) {
                        if (typeStr.contains("/") && !typeStr.endsWith(";")) {
                            typeStr += ";";
                        }
                        t = Type.getType(typeStr);
                    } else {
                        t = Type.getObjectType(typeStr);
                    }
                    pushToStack(t.getElementType());
                } else if (target == Opcodes.NULL) {
                    pushToStack(AsmConstants.NULL_TYPE);
                } else {
                    pushToStack(AsmConstants.OBJECT_TYPE);
                }
                break;
            }
            case Opcodes.IALOAD: {
                stack.pop();
                stack.pop();

                pushToStack(Type.INT_TYPE);
                break;
            }
            case Opcodes.FALOAD: {
                stack.pop();
                stack.pop();

                pushToStack(Type.FLOAT_TYPE);
                break;
            }
            case Opcodes.BALOAD: {
                stack.pop();
                stack.pop();

                pushToStack(Type.BYTE_TYPE);
                break;
            }
            case Opcodes.CALOAD: {
                stack.pop();
                stack.pop();

                pushToStack(Type.CHAR_TYPE);
                break;
            }
            case Opcodes.SALOAD: {
                stack.pop();
                stack.pop();

                pushToStack(Type.SHORT_TYPE);
                break;
            }
            case Opcodes.LALOAD: {
                stack.pop();
                stack.pop();

                pushToStack(Type.LONG_TYPE);
                break;
            }
            case Opcodes.DALOAD: {
                stack.pop();
                stack.pop();

                pushToStack(Type.DOUBLE_TYPE);
                break;
            }
            case Opcodes.AASTORE:
            case Opcodes.IASTORE:
            case Opcodes.FASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE:
            case Opcodes.LASTORE:
            case Opcodes.DASTORE: {
                stack.pop(); // val
                stack.pop(); // index
                stack.pop(); // arrayref

                break;
            }
            case Opcodes.POP: {
                stack.pop1();
                break;
            }
            case Opcodes.POP2: {
                stack.pop1();
                stack.pop1();
                break;
            }
            case Opcodes.DUP: {
                stack.push1(stack.peek());
                break;
            }
            case Opcodes.DUP_X1: {
                Object x = stack.pop1();
                Object y = stack.pop1();
                stack.push1(x);
                stack.push1(y);
                stack.push1(x);
                break;
            }
            case Opcodes.DUP_X2: {
                Object x = stack.pop1();
                Object y = stack.pop1();
                Object z = stack.pop1();
                stack.push1(x);
                stack.push1(z);
                stack.push1(y);
                stack.push1(x);
                break;
            }
            case Opcodes.DUP2: {
                Object x = stack.pop1();
                Object y = stack.peek();
                stack.push1(x);
                stack.push1(y);
                stack.push1(x);
                break;
            }
            case Opcodes.DUP2_X1: {
                Object x2 = stack.pop1();
                Object x1 = stack.pop1();
                Object y = stack.pop1();
                stack.push1(x1);
                stack.push1(x2);
                stack.push1(y);
                stack.push1(x1);
                stack.push1(x2);
                break;
            }
            case Opcodes.DUP2_X2: {
                Object x2 = stack.pop1();
                Object x1 = stack.pop1();
                Object y2 = stack.pop1();
                Object y1 = stack.pop1();
                stack.push1(x1);
                stack.push1(x2);
                stack.push1(y1);
                stack.push1(y2);
                stack.push1(x1);
                stack.push1(x2);
                break;
            }
            case Opcodes.SWAP: {
                Object x = stack.pop1();
                Object y = stack.pop1();
                stack.push1(x);
                stack.push1(y);
                break;
            }
            case Opcodes.IADD:
            case Opcodes.ISUB:
            case Opcodes.IMUL:
            case Opcodes.IDIV:
            case Opcodes.IREM:
            case Opcodes.IAND:
            case Opcodes.IOR:
            case Opcodes.IXOR:
            case Opcodes.ISHR:
            case Opcodes.ISHL:
            case Opcodes.IUSHR: {
                popFromStack(Type.INT_TYPE);
                popFromStack(Type.INT_TYPE);
                pushToStack(Type.INT_TYPE);
                break;
            }
            case Opcodes.FADD:
            case Opcodes.FSUB:
            case Opcodes.FMUL:
            case Opcodes.FDIV:
            case Opcodes.FREM: {
                popFromStack(Type.FLOAT_TYPE);
                popFromStack(Type.FLOAT_TYPE);
                pushToStack(Type.FLOAT_TYPE);
                break;
            }
            case Opcodes.LADD:
            case Opcodes.LSUB:
            case Opcodes.LMUL:
            case Opcodes.LDIV:
            case Opcodes.LREM:
            case Opcodes.LAND:
            case Opcodes.LOR:
            case Opcodes.LXOR:
            case Opcodes.LSHR:
            case Opcodes.LSHL:
            case Opcodes.LUSHR: {
                popFromStack(Type.LONG_TYPE);
                popFromStack(Type.LONG_TYPE);
                pushToStack(Type.LONG_TYPE);
                break;
            }
            case Opcodes.DADD:
            case Opcodes.DSUB:
            case Opcodes.DMUL:
            case Opcodes.DDIV:
            case Opcodes.DREM: {
                popFromStack(Type.DOUBLE_TYPE);
                popFromStack(Type.DOUBLE_TYPE);
                break;
            }
            case Opcodes.I2L: {
                popFromStack(Type.INT_TYPE);
                pushToStack(Type.LONG_TYPE);
                break;
            }
            case Opcodes.I2F: {
                popFromStack(Type.INT_TYPE);
                pushToStack(Type.FLOAT_TYPE);
                break;
            }
            case Opcodes.I2B: {
                popFromStack(Type.INT_TYPE);
                pushToStack(Type.BYTE_TYPE);
                break;
            }
            case Opcodes.I2C: {
                popFromStack(Type.INT_TYPE);
                pushToStack(Type.CHAR_TYPE);
                break;
            }
            case Opcodes.I2S: {
                popFromStack(Type.INT_TYPE);
                pushToStack(Type.SHORT_TYPE);
                break;
            }
            case Opcodes.I2D: {
                popFromStack(Type.INT_TYPE);
                pushToStack(Type.DOUBLE_TYPE);
                break;
            }
            case Opcodes.L2I: {
                popFromStack(Type.LONG_TYPE);
                pushToStack(Type.INT_TYPE);
                break;
            }
            case Opcodes.L2F: {
                popFromStack(Type.LONG_TYPE);
                pushToStack(Type.FLOAT_TYPE);
                break;
            }
            case Opcodes.L2D: {
                popFromStack(Type.LONG_TYPE);
                pushToStack(Type.DOUBLE_TYPE);
                break;
            }
            case Opcodes.F2I: {
                popFromStack(Type.FLOAT_TYPE);
                pushToStack(Type.INT_TYPE);
                break;
            }
            case Opcodes.F2L: {
                popFromStack(Type.FLOAT_TYPE);
                pushToStack(Type.LONG_TYPE);
                break;
            }
            case Opcodes.F2D: {
                popFromStack(Type.FLOAT_TYPE);
                pushToStack(Type.DOUBLE_TYPE);
                break;
            }
            case Opcodes.D2I: {
                popFromStack(Type.DOUBLE_TYPE);
                pushToStack(Type.INT_TYPE);
                break;
            }
            case Opcodes.D2F: {
                popFromStack(Type.DOUBLE_TYPE);
                pushToStack(Type.FLOAT_TYPE);
                break;
            }
            case Opcodes.D2L: {
                popFromStack(Type.DOUBLE_TYPE);
                pushToStack(Type.LONG_TYPE);
                break;
            }
            case Opcodes.LCMP: {
                popFromStack(Type.LONG_TYPE);
                popFromStack(Type.LONG_TYPE);

                pushToStack(Type.INT_TYPE);
                break;
            }
            case Opcodes.FCMPL:
            case Opcodes.FCMPG: {
                popFromStack(Type.FLOAT_TYPE);
                popFromStack(Type.FLOAT_TYPE);

                pushToStack(Type.INT_TYPE);
                break;
            }
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:{
                popFromStack(Type.DOUBLE_TYPE);
                popFromStack(Type.DOUBLE_TYPE);

                pushToStack(Type.INT_TYPE);
                break;
            }
            case Opcodes.IRETURN: {
                popFromStack(Type.INT_TYPE);

                onMethodExit(opcode);
                break;
            }
            case Opcodes.LRETURN: {
                popFromStack(Type.LONG_TYPE);

                onMethodExit(opcode);
                break;
            }
            case Opcodes.FRETURN: {
                popFromStack(Type.FLOAT_TYPE);

                onMethodExit(opcode);
                break;
            }
            case Opcodes.DRETURN: {
                popFromStack(Type.DOUBLE_TYPE);

                onMethodExit(opcode);
                break;
            }
            case Opcodes.RETURN: {
                onMethodExit(opcode);
                break;
            }
            case Opcodes.ARETURN: {
                popFromStack(Type.getReturnType(desc));

                onMethodExit(opcode);
                break;
            }
            case Opcodes.ATHROW: {
                popFromStack(AsmConstants.THROWABLE_TYPE);
                onMethodExit(opcode);
                break;
            }
            case Opcodes.ARRAYLENGTH: {
                stack.pop();
                pushToStack(Type.INT_TYPE);
                break;
            }
            case Opcodes.MONITORENTER:
            case Opcodes.MONITOREXIT: {
                stack.pop();
                break;
            }
        }
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
        super.visitIincInsn(remap(var, 1), increment);
        pc++;
    }

    @Override
    public void visitLocalVariable(final String name, final String desc,
                                   final String signature, final Label start, final Label end,
                                   final int index) {
        int newIndex = map(index);
        if (newIndex != 0) {
            super.visitLocalVariable(name, desc, signature, start, end, newIndex == Integer.MIN_VALUE ? 0 : Math.abs(newIndex));
        }
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
        Type t = Type.getType(desc);
        int cnt = 0;
        int[] newIndex = new int[index.length];
        for (int i = 0; i < newIndex.length; ++i) {
            int idx = map(index[i]);
            if (idx != 0) {
                newIndex[cnt++] = idx == Integer.MIN_VALUE ? 0 : Math.abs(idx);
            }
        }
        return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, Arrays.copyOf(newIndex, cnt), desc, visible);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String exception) {
        tryCatchHandlerMap.put(start, handler);
        super.visitTryCatchBlock(start, end, handler, exception);
    }

    @Override
    public void visitLabel(Label label) {
        SavedState ss = jumpTargetStates.get(label);
        if (ss != null) {
            if (ss.kind != SavedState.CONDITIONAL) {
                reset();
            }
            localTypes.mergeWith(ss.lvTypes.toArray());
            stack.replaceWith(ss.sStack.toArray());
            if (ss.kind == SavedState.EXCEPTION) {
                stack.push(toSlotType(AsmConstants.THROWABLE_TYPE));
            }
            for (LocalVarSlot lvs : newLocals) {
                if (!ss.newLocals.contains(lvs)) {
                    lvs.expire();
                }
            }
            newLocals.addAll(ss.newLocals);
        }
        Label handler = tryCatchHandlerMap.get(label);
        if (handler != null) {
            if (!jumpTargetStates.containsKey(handler)) {
                jumpTargetStates.put(handler, new SavedState(localTypes, stack, newLocals, SavedState.EXCEPTION));
            }
        }
        super.visitLabel(label);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(Math.max(stack.maxStack, maxStack), localTypes.maxSize());
    }

    private void initLocals(boolean isInstance) {
        if (isInstance) {
            locals.add(owner);
            nextMappedVar++;
            localsTailPtr++;
        }
        for (Type t : Type.getArgumentTypes(desc)) {
            locals.add(toSlotType(t));
            nextMappedVar += t.getSize();
            localsTailPtr++;
        }
        localTypes.replaceWith(locals.toArray(new Object[0]));
        argsSize = nextMappedVar;
        firstLocal = nextMappedVar;
    }

    private Object[] computeFrameLocals() {
        Object[] localsArr;
        if (nextMappedVar > argsSize) {
            int arrSize = Math.max(locals.size(), nextMappedVar);
            localsArr = new Object[arrSize];
            int idx = 0;
            Iterator<Object> iter = locals.iterator();
            while (iter.hasNext()) {
                Object e = iter.next();
                if (idx < argsSize) {
                    localsArr[idx] = e;
                    if (e == Opcodes.LONG || e == Opcodes.DOUBLE) {
                        localsArr[++idx] = TOP_EXT;
                    }
                } else {
                    int var = mapping[idx - argsSize];
                    if (var < 0) {
                        var = var == Integer.MIN_VALUE ? 0 : -var;
                        localsArr[var] = e;
                        if (e == Opcodes.LONG || e == Opcodes.DOUBLE) {
                            int off = var + 1;
                            if (off == localsArr.length) {
                                localsArr = Arrays.copyOf(localsArr, localsArr.length + 1);
                            }
                            localsArr[off] = TOP_EXT;
                            idx++;
                        }
                    }
                }
                idx++;
            }
            for (LocalVarSlot lvs : newLocals) {
                int ptr = lvs.idx != Integer.MIN_VALUE ? lvs.idx : 0;
                localsArr[ptr] = lvs.isExpired() ? Opcodes.TOP : lvs.type;
                if (lvs.type == Opcodes.LONG || lvs.type == Opcodes.DOUBLE) {
                    localsArr[ptr + 1] = TOP_EXT;
                }
            }
        } else {
            localsArr = locals.toArray(new Object[0]);
        }
        for (int m : mapping) {
            if (m != 0) {
                m = m == Integer.MIN_VALUE ? 0 : Math.abs(m);
                if (localsArr[m] == null) {
                    localsArr[m] = Opcodes.TOP;
                }
            }
        }
        Object[] tmp = new Object[localsArr.length];
        int idx = 0;
        for (Object o : localsArr) {
            if (o != null) {
                tmp[idx++] = o;
            }
        }
        return Arrays.copyOf(tmp, idx);
    }

    private void reset() {
        localTypes.reset();
        stack.reset();
        newLocals.clear();
    }

    private void setMapping(int from, int to, int padding) {
        if (mapping.length <= from + padding) {
            mapping = Arrays.copyOf(mapping, Math.max(mapping.length * 2, from + padding + 1));
        }
        mapping[from] = to;
        if (padding > 0) {
            mapping[from + padding] = Math.abs(to) + padding; // padding
        }
    }

    private int remap(int var, int size) {
        int mappedVar = map(var);
        if (mappedVar == 0) {
            int offset = var - argsSize;
            var = newVarIdx(size);
            setMapping(offset, var, size -1);
            mappedVar = var;
        }
        var = mappedVar == Integer.MIN_VALUE ? 0 : Math.abs(mappedVar);
        // adjust the mapping pointer if remapping with variable occupying 2 slots
        nextMappedVar = Math.max(var + size, nextMappedVar);
        return var;
    }

    private int map(int var) {
        if (var < 0) {
            return var;
        }
        int idx = (var - argsSize);
        if (idx >= 0) {
            if (mapping.length <= idx) {
                mapping = Arrays.copyOf(mapping, mapping.length * 2);
                return 0;
            }
            return mapping[idx];
        }
        return var == 0 ? Integer.MIN_VALUE : var;
    }

    private Object popFromStack(Type t) {
        return stack.pop();
    }

    private void pushToStack(Type t) {
        stack.push(toSlotType(t));
    }

    public int newVarIdx(int size) {
        int var = nextMappedVar;
        nextMappedVar += size;
        return var == 0 ? Integer.MIN_VALUE : var;
    }

    private Type fromSlotType(Object slotType) {
        if (slotType == Opcodes.INTEGER) {
            return Type.INT_TYPE;
        }
        if (slotType == Opcodes.FLOAT) {
            return Type.FLOAT_TYPE;
        }
        if (slotType == Opcodes.LONG) {
            return Type.LONG_TYPE;
        }
        if (slotType == Opcodes.DOUBLE) {
            return Type.DOUBLE_TYPE;
        }
        if (slotType == Opcodes.UNINITIALIZED_THIS) {
            return Type.getObjectType(owner);
        }
        if (slotType == Opcodes.NULL) {
            return AsmConstants.NULL_TYPE;
        }
        if (slotType == Opcodes.TOP) {
            return AsmConstants.TOP_TYPE;
        }
        return slotType != null ? Type.getObjectType((String)slotType) : AsmConstants.OBJECT_TYPE;
    }


//    public int storeAsNew() {
//        Type t = fromSlotType(peekFromStack());
//        int idx = newVar(t);
//        visitVarInsn(t.getOpcode(Opcodes.ISTORE), idx);
//        return idx;
//    }

    public final int newVar(Type t) {
        int idx = newVarIdx(t.getSize());

        newLocals.add(new LocalVarSlot(idx, toSlotType(t)));
        int var = idx == Integer.MIN_VALUE ? 0 : Math.abs(idx);
        localTypes.setType(var, t);

        return idx;
    }

    private static Object toSlotType(Type t) {
        if (t == null) {
            return null;
        }
        switch (t.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT: {
                return Opcodes.INTEGER;
            }
            case Type.FLOAT: {
                return Opcodes.FLOAT;
            }
            case Type.LONG: {
                return Opcodes.LONG;
            }
            case Type.DOUBLE: {
                return Opcodes.DOUBLE;
            }
            default: {

//                Serializable serializable = t == AsmConstants.NULL_TYPE ? Opcodes.NULL : t == AsmConstants.TOP_TYPE ? Opcodes.TOP : t.getInternalName();
//                System.out.println(serializable);
                return t.getInternalName();
            }
        }
    }

    private static final Type BYTE_TYPE = Type.getObjectType("java/lang/Byte");

    private static final Type BOOLEAN_TYPE = Type.getObjectType("java/lang/Boolean");

    private static final Type SHORT_TYPE = Type.getObjectType("java/lang/Short");

    private static final Type CHARACTER_TYPE = Type.getObjectType("java/lang/Character");

    private static final Type INTEGER_TYPE = Type.getObjectType("java/lang/Integer");

    private static final Type FLOAT_TYPE = Type.getObjectType("java/lang/Float");

    private static final Type LONG_TYPE = Type.getObjectType("java/lang/Long");

    private static final Type DOUBLE_TYPE = Type.getObjectType("java/lang/Double");

    private static final Type NUMBER_TYPE = Type.getObjectType("java/lang/Number");

    private static final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");

    private static final Method BOOLEAN_VALUE = Method.getMethod("boolean booleanValue()");

    private static final Method CHAR_VALUE = Method.getMethod("char charValue()");

    private static final Method INT_VALUE = Method.getMethod("int intValue()");

    private static final Method FLOAT_VALUE = Method.getMethod("float floatValue()");

    private static final Method LONG_VALUE = Method.getMethod("long longValue()");

    private static final Method DOUBLE_VALUE = Method.getMethod("double doubleValue()");

    public void push(final Object value) {
        if (value == null) {
            visitInsn(Opcodes.ACONST_NULL);
        } else {
            visitLdcInsn(value);
        }
    }


    public void ifNull(final Label label) {
        visitJumpInsn(Opcodes.IFNULL, label);
    }

    private void loadInsn(final Type type, final int index) {
        visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
    }

    private void storeInsn(final Type type, final int index) {
        visitVarInsn(type.getOpcode(Opcodes.ISTORE), index);
    }

    public void storeLocal(final int local) {
        storeInsn(getLocalType(local), local);
    }

    public void loadLocal(final int local) {
        loadInsn(getLocalType(local), local);
    }

    public Type getLocalType(final int local) {
        return fromSlotType(localTypes.getType(local));
    }

    public void unbox(final Type type) {
        Type boxedType = NUMBER_TYPE;
        Method unboxMethod;
        switch (type.getSort()) {
            case Type.VOID:
                return;
            case Type.CHAR:
                boxedType = CHARACTER_TYPE;
                unboxMethod = CHAR_VALUE;
                break;
            case Type.BOOLEAN:
                boxedType = BOOLEAN_TYPE;
                unboxMethod = BOOLEAN_VALUE;
                break;
            case Type.DOUBLE:
                unboxMethod = DOUBLE_VALUE;
                break;
            case Type.FLOAT:
                unboxMethod = FLOAT_VALUE;
                break;
            case Type.LONG:
                unboxMethod = LONG_VALUE;
                break;
            case Type.INT:
            case Type.SHORT:
            case Type.BYTE:
                unboxMethod = INT_VALUE;
                break;
            default:
                unboxMethod = null;
                break;
        }
        if (unboxMethod == null) {
            checkCast(type);
        } else {
            checkCast(boxedType);
            invokeVirtual(boxedType, unboxMethod);
        }
    }

    private void invokeInsn(
            final int opcode, final Type type, final Method method, final boolean isInterface) {
        String owner = type.getSort() == Type.ARRAY ? type.getDescriptor() : type.getInternalName();
        visitMethodInsn(opcode, owner, method.getName(), method.getDescriptor(), isInterface);
    }

    public void invokeVirtual(final Type owner, final Method method) {
        invokeInsn(Opcodes.INVOKEVIRTUAL, owner, method, false);
    }

    private void typeInsn(final int opcode, final Type type) {
        visitTypeInsn(opcode, type.getInternalName());
    }

    public void checkCast(final Type type) {
        if (!type.equals(OBJECT_TYPE)) {
            typeInsn(Opcodes.CHECKCAST, type);
        }
    }

    private int getArgIndex(final int arg) {
        int index = (access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
        for (int i = 0; i < arg; i++) {
            index += argumentTypes[i].getSize();
        }
        return index;
    }

    public void loadArg(final int arg) {
        loadInsn(argumentTypes[arg], getArgIndex(arg));
    }

    public void returnValue() {
        visitInsn(returnType.getOpcode(Opcodes.IRETURN));
    }

    public void box(final Type type) {
        if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
            return;
        }
        if (type == Type.VOID_TYPE) {
            push(null);
        } else {
            Type boxedType = getBoxedType(type);
            newInstance(boxedType);
            if (type.getSize() == 2) {
                // Pp -> Ppo -> oPpo -> ooPpo -> ooPp -> o
                dupX2();
                dupX2();
                pop();
            } else {
                // p -> po -> opo -> oop -> o
                dupX1();
                swap();
            }
            invokeConstructor(boxedType, new Method("<init>", Type.VOID_TYPE, new Type[] {type}));
        }
    }

    public void newInstance(final Type type) {
        typeInsn(Opcodes.NEW, type);
    }

    public void invokeConstructor(final Type type, final Method method) {
        invokeInsn(Opcodes.INVOKESPECIAL, type, method, false);
    }

    private static Type getBoxedType(final Type type) {
        switch (type.getSort()) {
            case Type.BYTE:
                return BYTE_TYPE;
            case Type.BOOLEAN:
                return BOOLEAN_TYPE;
            case Type.SHORT:
                return SHORT_TYPE;
            case Type.CHAR:
                return CHARACTER_TYPE;
            case Type.INT:
                return INTEGER_TYPE;
            case Type.FLOAT:
                return FLOAT_TYPE;
            case Type.LONG:
                return LONG_TYPE;
            case Type.DOUBLE:
                return DOUBLE_TYPE;
            default:
                return type;
        }
    }

    // -----------------------------------------------------------------------------------------------
    // Instructions to manage the stack
    // -----------------------------------------------------------------------------------------------

    /** Generates a POP instruction. */
    public void pop() {
        visitInsn(Opcodes.POP);
    }

    /** Generates a POP2 instruction. */
    public void pop2() {
        visitInsn(Opcodes.POP2);
    }

    /** Generates a DUP instruction. */
    public void dup() {
        visitInsn(Opcodes.DUP);
    }

    /** Generates a DUP2 instruction. */
    public void dup2() {
        visitInsn(Opcodes.DUP2);
    }

    /** Generates a DUP_X1 instruction. */
    public void dupX1() {
        visitInsn(Opcodes.DUP_X1);
    }

    /** Generates a DUP_X2 instruction. */
    public void dupX2() {
        visitInsn(Opcodes.DUP_X2);
    }

    /** Generates a DUP2_X1 instruction. */
    public void dup2X1() {
        visitInsn(Opcodes.DUP2_X1);
    }

    /** Generates a DUP2_X2 instruction. */
    public void dup2X2() {
        visitInsn(Opcodes.DUP2_X2);
    }

    /** Generates a SWAP instruction. */
    public void swap() {
        visitInsn(Opcodes.SWAP);
    }

    protected void onMethodEnter() {}

    protected void onMethodExit(final int opcode) {}



}
