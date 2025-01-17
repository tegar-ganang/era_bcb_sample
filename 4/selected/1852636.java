package org.armedbear.lisp;

import static org.armedbear.lisp.Nil.NIL;
import static org.armedbear.lisp.Lisp.*;

public class StructureObjectImpl extends AbstractLispObject implements StructureObject {

    private final StructureClass structureClass;

    final LispObject[] slots;

    public StructureObjectImpl(Symbol symbol, LispObject[] slots) throws ConditionThrowable {
        structureClass = (StructureClass) findLispClass(symbol);
        this.slots = slots;
    }

    public StructureObjectImpl(Symbol symbol, LispObject obj0) throws ConditionThrowable {
        structureClass = (StructureClass) findLispClass(symbol);
        LispObject[] slots = new LispObject[1];
        slots[0] = obj0;
        this.slots = slots;
    }

    public StructureObjectImpl(Symbol symbol, LispObject obj0, LispObject obj1) throws ConditionThrowable {
        structureClass = (StructureClass) findLispClass(symbol);
        LispObject[] slots = new LispObject[2];
        slots[0] = obj0;
        slots[1] = obj1;
        this.slots = slots;
    }

    public StructureObjectImpl(Symbol symbol, LispObject obj0, LispObject obj1, LispObject obj2) throws ConditionThrowable {
        structureClass = (StructureClass) findLispClass(symbol);
        LispObject[] slots = new LispObject[3];
        slots[0] = obj0;
        slots[1] = obj1;
        slots[2] = obj2;
        this.slots = slots;
    }

    public StructureObjectImpl(Symbol symbol, LispObject obj0, LispObject obj1, LispObject obj2, LispObject obj3) throws ConditionThrowable {
        structureClass = (StructureClass) findLispClass(symbol);
        LispObject[] slots = new LispObject[4];
        slots[0] = obj0;
        slots[1] = obj1;
        slots[2] = obj2;
        slots[3] = obj3;
        this.slots = slots;
    }

    public StructureObjectImpl(Symbol symbol, LispObject obj0, LispObject obj1, LispObject obj2, LispObject obj3, LispObject obj4) throws ConditionThrowable {
        structureClass = (StructureClass) findLispClass(symbol);
        LispObject[] slots = new LispObject[5];
        slots[0] = obj0;
        slots[1] = obj1;
        slots[2] = obj2;
        slots[3] = obj3;
        slots[4] = obj4;
        this.slots = slots;
    }

    public StructureObjectImpl(Symbol symbol, LispObject obj0, LispObject obj1, LispObject obj2, LispObject obj3, LispObject obj4, LispObject obj5) throws ConditionThrowable {
        structureClass = (StructureClass) findLispClass(symbol);
        LispObject[] slots = new LispObject[6];
        slots[0] = obj0;
        slots[1] = obj1;
        slots[2] = obj2;
        slots[3] = obj3;
        slots[4] = obj4;
        slots[5] = obj5;
        this.slots = slots;
    }

    public StructureObjectImpl(StructureObject obj) throws ConditionThrowable {
        this.structureClass = obj.getStructureClass();
        slots = new LispObject[obj.getSlotLength()];
        for (int i = slots.length; i-- > 0; ) slots[i] = obj.getSlotValue(i);
    }

    @Override
    public LispObject typeOf() {
        return structureClass.getSymbol();
    }

    @Override
    public LispObject classOf() {
        return structureClass;
    }

    public int getSlotLength() {
        return slots.length;
    }

    public LispObject[] getSlots() {
        return slots;
    }

    public StructureClass getStructureClass() {
        return structureClass;
    }

    @Override
    public LispObject getParts() throws ConditionThrowable {
        LispObject result = NIL;
        result = result.push(makeCons("class", structureClass));
        LispObject effectiveSlots = structureClass.getSlotDefinitions();
        LispObject[] effectiveSlotsArray = effectiveSlots.copyToArray();
        Debug.assertTrue(effectiveSlotsArray.length == slots.length);
        for (int i = 0; i < slots.length; i++) {
            SimpleVector slotDefinition = (SimpleVector) effectiveSlotsArray[i];
            LispObject slotName = slotDefinition.AREF(1);
            result = result.push(makeCons(slotName, slots[i]));
        }
        return result.nreverse();
    }

    @Override
    public LispObject typep(LispObject type) throws ConditionThrowable {
        if (type instanceof StructureClass) return memq(type, structureClass.getCPL()) ? T : NIL;
        if (type == structureClass.getSymbol()) return T;
        if (type == SymbolConstants.STRUCTURE_OBJECT) return T;
        if (type == BuiltInClass.STRUCTURE_OBJECT) return T;
        if (type instanceof Symbol) {
            LispClass c = findLispClass((Symbol) type);
            if (c != null) return memq(c, structureClass.getCPL()) ? T : NIL;
        }
        return super.typep(type);
    }

    @Override
    public boolean equalp(LispObject obj) throws ConditionThrowable {
        if (this == obj) return true;
        if (obj instanceof StructureObject) {
            StructureObject o = (StructureObject) obj;
            if (structureClass != o.getStructureClass()) return false;
            for (int i = 0; i < slots.length; i++) {
                if (!slots[i].equalp(o.getSlotValue(i))) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public LispObject getSlotValue_0() throws ConditionThrowable {
        try {
            return slots[0];
        } catch (ArrayIndexOutOfBoundsException e) {
            return badIndex(0);
        }
    }

    @Override
    public LispObject getSlotValue_1() throws ConditionThrowable {
        try {
            return slots[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            return badIndex(1);
        }
    }

    @Override
    public LispObject getSlotValue_2() throws ConditionThrowable {
        try {
            return slots[2];
        } catch (ArrayIndexOutOfBoundsException e) {
            return badIndex(2);
        }
    }

    @Override
    public LispObject getSlotValue_3() throws ConditionThrowable {
        try {
            return slots[3];
        } catch (ArrayIndexOutOfBoundsException e) {
            return badIndex(3);
        }
    }

    @Override
    public LispObject getSlotValue(int index) throws ConditionThrowable {
        try {
            return slots[index];
        } catch (ArrayIndexOutOfBoundsException e) {
            return badIndex(index);
        }
    }

    @Override
    public int getFixnumSlotValue(int index) throws ConditionThrowable {
        try {
            return slots[index].intValue();
        } catch (ArrayIndexOutOfBoundsException e) {
            badIndex(index);
            return 0;
        }
    }

    @Override
    public boolean getSlotValueAsBoolean(int index) throws ConditionThrowable {
        try {
            return slots[index] != NIL ? true : false;
        } catch (ArrayIndexOutOfBoundsException e) {
            badIndex(index);
            return false;
        }
    }

    @Override
    public void setSlotValue_0(LispObject value) throws ConditionThrowable {
        try {
            slots[0] = value;
        } catch (ArrayIndexOutOfBoundsException e) {
            badIndex(0);
        }
    }

    @Override
    public void setSlotValue_1(LispObject value) throws ConditionThrowable {
        try {
            slots[1] = value;
        } catch (ArrayIndexOutOfBoundsException e) {
            badIndex(1);
        }
    }

    @Override
    public void setSlotValue_2(LispObject value) throws ConditionThrowable {
        try {
            slots[2] = value;
        } catch (ArrayIndexOutOfBoundsException e) {
            badIndex(2);
        }
    }

    @Override
    public void setSlotValue_3(LispObject value) throws ConditionThrowable {
        try {
            slots[3] = value;
        } catch (ArrayIndexOutOfBoundsException e) {
            badIndex(3);
        }
    }

    @Override
    public void setSlotValue(int index, LispObject value) throws ConditionThrowable {
        try {
            slots[index] = value;
        } catch (ArrayIndexOutOfBoundsException e) {
            badIndex(index);
        }
    }

    private LispObject badIndex(int n) throws ConditionThrowable {
        FastStringBuffer sb = new FastStringBuffer("Invalid slot index ");
        sb.append(Fixnum.makeFixnum(n).writeToString());
        sb.append(" for ");
        sb.append(writeToString());
        return error(new LispError(sb.toString()));
    }

    @Override
    public final int psxhash() {
        return psxhash(4);
    }

    @Override
    public final int psxhash(int depth) {
        int result = mix(structureClass.sxhash(), 7814971);
        if (depth > 0) {
            int limit = slots.length;
            if (limit > 4) limit = 4;
            for (int i = 0; i < limit; i++) result = mix(slots[i].psxhash(depth - 1), result);
        }
        return result & 0x7fffffff;
    }

    @Override
    public String writeToString() throws ConditionThrowable {
        try {
            final LispThread thread = LispThread.currentThread();
            if (typep(SymbolConstants.RESTART) != NIL) {
                Symbol PRINT_RESTART = PACKAGE_SYS.intern("PRINT-RESTART");
                LispObject fun = PRINT_RESTART.getSymbolFunction();
                StringOutputStream stream = new StringOutputStream();
                thread.execute(fun, this, stream);
                return stream.getStringOutputString().getStringValue();
            }
            if (_PRINT_STRUCTURE_.symbolValue(thread) == NIL) return unreadableString(structureClass.getSymbol().writeToString());
            int maxLevel = Integer.MAX_VALUE;
            LispObject printLevel = SymbolConstants.PRINT_LEVEL.symbolValue(thread);
            if (printLevel instanceof Fixnum) maxLevel = printLevel.intValue();
            LispObject currentPrintLevel = _CURRENT_PRINT_LEVEL_.symbolValue(thread);
            int currentLevel = currentPrintLevel.intValue();
            if (currentLevel >= maxLevel && slots.length > 0) return "#";
            FastStringBuffer sb = new FastStringBuffer("#S(");
            sb.append(structureClass.getSymbol().writeToString());
            if (currentLevel < maxLevel) {
                LispObject effectiveSlots = structureClass.getSlotDefinitions();
                LispObject[] effectiveSlotsArray = effectiveSlots.copyToArray();
                Debug.assertTrue(effectiveSlotsArray.length == slots.length);
                final LispObject printLength = SymbolConstants.PRINT_LENGTH.symbolValue(thread);
                final int limit;
                if (printLength instanceof Fixnum) limit = Math.min(slots.length, printLength.intValue()); else limit = slots.length;
                final boolean printCircle = (SymbolConstants.PRINT_CIRCLE.symbolValue(thread) != NIL);
                for (int i = 0; i < limit; i++) {
                    sb.append(' ');
                    SimpleVector slotDefinition = (SimpleVector) effectiveSlotsArray[i];
                    LispObject slotName = slotDefinition.AREF(1);
                    Debug.assertTrue(slotName instanceof Symbol);
                    sb.append(':');
                    sb.append(((Symbol) slotName).getName());
                    sb.append(' ');
                    if (printCircle) {
                        StringOutputStream stream = new StringOutputStream();
                        thread.execute(SymbolConstants.OUTPUT_OBJECT.getSymbolFunction(), slots[i], stream);
                        sb.append(stream.getStringOutputString().getStringValue());
                    } else sb.append(slots[i].writeToString());
                }
                if (limit < slots.length) sb.append(" ...");
            }
            sb.append(')');
            return sb.toString();
        } catch (StackOverflowError e) {
            error(new StorageCondition("Stack overflow."));
            return null;
        }
    }

    private static final Primitive STRUCTURE_OBJECT_P = new Primitive("structure-object-p", PACKAGE_SYS, true, "object") {

        @Override
        public LispObject execute(LispObject arg) {
            return arg instanceof StructureObject ? T : NIL;
        }
    };

    private static final Primitive STRUCTURE_LENGTH = new Primitive("structure-length", PACKAGE_SYS, true, "instance") {

        @Override
        public LispObject execute(LispObject arg) throws ConditionThrowable {
            if (arg instanceof StructureObject) return Fixnum.makeFixnum(((StructureObject) arg).getSlotLength());
            return type_error(arg, SymbolConstants.STRUCTURE_OBJECT);
        }
    };

    private static final Primitive STRUCTURE_REF = new Primitive("structure-ref", PACKAGE_SYS, true) {

        @Override
        public LispObject execute(LispObject first, LispObject second) throws ConditionThrowable {
            if (first instanceof StructureObject) try {
                return ((StructureObject) first).getSlotValue(second.intValue());
            } catch (ArrayIndexOutOfBoundsException e) {
                return error(new LispError("Internal error."));
            }
            return type_error(first, SymbolConstants.STRUCTURE_OBJECT);
        }
    };

    private static final Primitive STRUCTURE_SET = new Primitive("structure-set", PACKAGE_SYS, true) {

        @Override
        public LispObject execute(LispObject first, LispObject second, LispObject third) throws ConditionThrowable {
            if (first instanceof StructureObject) try {
                ((StructureObject) first).setSlotValue(second.intValue(), third);
                return third;
            } catch (ArrayIndexOutOfBoundsException e) {
                return error(new LispError("Internal error."));
            }
            return type_error(first, SymbolConstants.STRUCTURE_OBJECT);
        }
    };

    private static final Primitive MAKE_STRUCTURE = new Primitive("make-structure", PACKAGE_SYS, true) {

        @Override
        public LispObject execute(LispObject first, LispObject second) throws ConditionThrowable {
            return new StructureObjectImpl(checkSymbol(first), second);
        }

        @Override
        public LispObject execute(LispObject first, LispObject second, LispObject third) throws ConditionThrowable {
            return new StructureObjectImpl(checkSymbol(first), second, third);
        }

        @Override
        public LispObject execute(LispObject first, LispObject second, LispObject third, LispObject fourth) throws ConditionThrowable {
            return new StructureObjectImpl(checkSymbol(first), second, third, fourth);
        }

        @Override
        public LispObject execute(LispObject first, LispObject second, LispObject third, LispObject fourth, LispObject fifth) throws ConditionThrowable {
            return new StructureObjectImpl(checkSymbol(first), second, third, fourth, fifth);
        }

        @Override
        public LispObject execute(LispObject first, LispObject second, LispObject third, LispObject fourth, LispObject fifth, LispObject sixth) throws ConditionThrowable {
            return new StructureObjectImpl(checkSymbol(first), second, third, fourth, fifth, sixth);
        }

        @Override
        public LispObject execute(LispObject first, LispObject second, LispObject third, LispObject fourth, LispObject fifth, LispObject sixth, LispObject seventh) throws ConditionThrowable {
            return new StructureObjectImpl(checkSymbol(first), second, third, fourth, fifth, sixth, seventh);
        }
    };

    private static final Primitive _MAKE_STRUCTURE = new Primitive("%make-structure", PACKAGE_SYS, true) {

        @Override
        public LispObject execute(LispObject first, LispObject second) throws ConditionThrowable {
            return new StructureObjectImpl(checkSymbol(first), second.copyToArray());
        }
    };

    private static final Primitive COPY_STRUCTURE = new Primitive(SymbolConstants.COPY_STRUCTURE, "structure") {

        @Override
        public LispObject execute(LispObject arg) throws ConditionThrowable {
            if (arg instanceof StructureObject) return new StructureObjectImpl((StructureObject) arg);
            return type_error(arg, SymbolConstants.STRUCTURE_OBJECT);
        }
    };
}
