package org.armedbear.lisp;

import static org.armedbear.lisp.Nil.NIL;
import static org.armedbear.lisp.Lisp.*;

public class StandardObjectImpl extends AbstractStandardObject implements StandardObject {

    private LispObject[] slots;

    public LispObject[] getSlots() {
        return slots;
    }

    public void setSlots(LispObject[] lispObjects) {
        slots = lispObjects;
    }

    public int getInstanceSlotLength() throws ConditionThrowable {
        return slots.length;
    }

    public Layout getLayout() {
        return layout;
    }

    public void setLayout(Layout checkLayout) {
        layout = checkLayout;
    }

    public LispObject getSlot(int index) {
        try {
            return slots[index];
        } catch (ArrayIndexOutOfBoundsException e) {
            return type_error(Fixnum.makeFixnum(index), list(SymbolConstants.INTEGER, Fixnum.ZERO, Fixnum.makeFixnum(getInstanceSlotLength())));
        }
    }

    public void setSlot(int index, LispObject value) {
        try {
            slots[index] = value;
        } catch (ArrayIndexOutOfBoundsException e) {
            type_error(Fixnum.makeFixnum(index), list(SymbolConstants.INTEGER, Fixnum.ZERO, Fixnum.makeFixnum(getInstanceSlotLength())));
        }
    }

    protected StandardObjectImpl(LispClass cls, int length) {
        super(cls.getClassLayout());
        slots = new LispObject[length];
        for (int i = slots.length; i-- > 0; ) slots[i] = UNBOUND_VALUE;
    }

    protected StandardObjectImpl(LispClass cls) {
        super(cls.getClassLayout());
        slots = new LispObject[layout.getLength()];
        for (int i = slots.length; i-- > 0; ) slots[i] = UNBOUND_VALUE;
    }

    @Override
    public LispObject getParts() throws ConditionThrowable {
        LispObject parts = NIL;
        if (layout != null) {
            ensureLayoutValid();
        }
        parts = parts.push(makeCons("LAYOUT", layout));
        if (layout != null) {
            LispObject[] slotNames = layout.getSlotNames();
            if (slotNames != null) {
                for (int i = 0; i < slotNames.length; i++) {
                    parts = parts.push(makeCons(slotNames[i], slots[i]));
                }
            }
        }
        return parts.nreverse();
    }

    public final LispClass getLispClass() {
        return layout.lispClass;
    }

    @Override
    public LispObject typeOf() {
        final LispClass c1 = layout.lispClass;
        final Symbol symbol = c1.getSymbol();
        if (symbol != NIL) {
            final LispObject c2 = findLispClass(symbol);
            if (c2 == c1) return symbol;
        }
        return c1;
    }

    @Override
    public LispObject classOf() {
        return layout.lispClass;
    }

    @Override
    public LispObject typep(LispObject type) throws ConditionThrowable {
        if (type == SymbolConstants.STANDARD_OBJECT) return T;
        if (type == StandardClass.STANDARD_OBJECT) return T;
        LispClass cls = layout != null ? layout.lispClass : null;
        if (cls != null) {
            if (type == cls) return T;
            if (type == cls.getSymbol()) return T;
            LispObject cpl = cls.getCPL();
            while (cpl != NIL) {
                if (type == cpl.CAR()) return T;
                if (type == ((LispClass) cpl.CAR()).getSymbol()) return T;
                cpl = cpl.CDR();
            }
        }
        return super.typep(type);
    }

    @Override
    public String writeToString() throws ConditionThrowable {
        final LispThread thread = LispThread.currentThread();
        int maxLevel = Integer.MAX_VALUE;
        LispObject printLevel = SymbolConstants.PRINT_LEVEL.symbolValue(thread);
        if (printLevel instanceof Fixnum) maxLevel = printLevel.intValue();
        LispObject currentPrintLevel = _CURRENT_PRINT_LEVEL_.symbolValue(thread);
        int currentLevel = currentPrintLevel.intValue();
        if (currentLevel >= maxLevel) return "#";
        if (typep(SymbolConstants.CONDITION) != NIL) {
            StringOutputStream stream = new StringOutputStream();
            SymbolConstants.PRINT_OBJECT.execute(this, stream);
            return stream.getStringOutputString().getStringValue();
        }
        return unreadableString(typeOf().writeToString());
    }

    public Layout updateLayout() throws ConditionThrowable {
        Debug.assertTrue(layout.isInvalid());
        Layout oldLayout = layout;
        LispClass cls = oldLayout.lispClass;
        Layout newLayout = cls.getClassLayout();
        Debug.assertTrue(!newLayout.isInvalid());
        StandardObjectImpl newInstance = new StandardObjectImpl(cls);
        Debug.assertTrue(newInstance.layout == newLayout);
        LispObject added = NIL;
        LispObject discarded = NIL;
        LispObject plist = NIL;
        LispObject[] oldSlotNames = oldLayout.getSlotNames();
        for (int i = 0; i < oldSlotNames.length; i++) {
            LispObject slotName = oldSlotNames[i];
            int j = newLayout.getSlotIndex(slotName);
            if (j >= 0) newInstance.slots[j] = slots[i]; else {
                discarded = discarded.push(slotName);
                if (slots[i] != UNBOUND_VALUE) {
                    plist = plist.push(slotName);
                    plist = plist.push(slots[i]);
                }
            }
        }
        LispObject rest = oldLayout.getSharedSlots();
        if (rest != null) {
            while (rest != NIL) {
                LispObject location = rest.CAR();
                LispObject slotName = location.CAR();
                int i = newLayout.getSlotIndex(slotName);
                if (i >= 0) newInstance.slots[i] = location.CDR();
                rest = rest.CDR();
            }
        }
        LispObject[] newSlotNames = newLayout.getSlotNames();
        for (int i = 0; i < newSlotNames.length; i++) {
            LispObject slotName = newSlotNames[i];
            int j = oldLayout.getSlotIndex(slotName);
            if (j >= 0) continue;
            LispObject location = oldLayout.getSharedSlotLocation(slotName);
            if (location != null) continue;
            added = added.push(slotName);
        }
        LispObject[] tempSlots = slots;
        slots = newInstance.slots;
        newInstance.slots = tempSlots;
        Layout tempLayout = layout;
        layout = newInstance.layout;
        newInstance.layout = tempLayout;
        Debug.assertTrue(!layout.isInvalid());
        SymbolConstants.UPDATE_INSTANCE_FOR_REDEFINED_CLASS.execute(this, added, discarded, plist);
        return newLayout;
    }

    public LispObject getInstanceSlotValue(LispObject slotName) throws ConditionThrowable {
        ensureLayoutValid();
        int index = layout.getSlotIndex(slotName);
        if (!(index >= 0)) return invalidInstanceSlot(this, slotName, SymbolConstants.SLOT_VALUE);
        return slots[index];
    }

    public void setInstanceSlotValue(LispObject slotName, LispObject newValue) throws ConditionThrowable {
        ensureLayoutValid();
        int index = layout.getSlotIndex(slotName);
        if (!(index >= 0)) {
            invalidInstanceSlot(this, slotName, SymbolConstants.SET_STD_SLOT_VALUE);
            return;
        }
        slots[index] = newValue;
    }

    @Override
    public LispObject SLOT_VALUE(LispObject slotName) throws ConditionThrowable {
        ensureLayoutValid();
        LispObject value;
        final LispObject index = layout.slotTable.get(slotName);
        if (index != null) {
            value = slots[index.intValue()];
        } else {
            LispObject location = layout.getSharedSlotLocation(slotName);
            if (location == null) return invalidInstanceSlot(this, slotName, SymbolConstants.SLOT_VALUE);
            value = location.CDR();
        }
        if (value == UNBOUND_VALUE) {
            value = SymbolConstants.SLOT_UNBOUND.execute(getLispClass(), this, slotName);
            LispThread.currentThread()._values = null;
        }
        return value;
    }

    @Override
    public void setSlotValue(LispObject slotName, LispObject newValue) throws ConditionThrowable {
        ensureLayoutValid();
        final LispObject index = layout.slotTable.get(slotName);
        if (index != null) {
            slots[index.intValue()] = newValue;
            return;
        }
        LispObject location = layout.getSharedSlotLocation(slotName);
        if (location != null) {
            location.setCdr(newValue);
            return;
        }
        LispObject[] args = new LispObject[5];
        args[0] = getLispClass();
        args[1] = this;
        args[2] = slotName;
        args[3] = SymbolConstants.SETF;
        args[4] = newValue;
        SymbolConstants.SLOT_MISSING.execute(args);
    }

    private static final Primitive SWAP_SLOTS = new Primitive("swap-slots", PACKAGE_SYS, true, "instance-1 instance-2") {

        @Override
        public LispObject execute(LispObject first, LispObject second) throws ConditionThrowable {
            final StandardObject obj1 = checkStandardObject(first);
            final StandardObject obj2 = checkStandardObject(second);
            LispObject[] temp = obj1.getSlots();
            obj1.setSlots(obj2.getSlots());
            obj2.setSlots(temp);
            return NIL;
        }
    };

    private static final Primitive STD_INSTANCE_LAYOUT = new Primitive("std-instance-layout", PACKAGE_SYS, true) {

        @Override
        public LispObject execute(LispObject arg) throws ConditionThrowable {
            final StandardObject instance = checkStandardObject(arg);
            Layout layout = instance.getLayout();
            if (layout.isInvalid()) {
                layout = instance.updateLayout();
            }
            return layout;
        }
    };

    private static final Primitive _SET_STD_INSTANCE_LAYOUT = new Primitive("%set-std-instance-layout", PACKAGE_SYS, true) {

        @Override
        public LispObject execute(LispObject first, LispObject second) throws ConditionThrowable {
            checkStandardObject(first).setLayout(checkLayout(second));
            return second;
        }
    };

    private static final Primitive STD_INSTANCE_CLASS = new Primitive("std-instance-class", PACKAGE_SYS, true) {

        @Override
        public LispObject execute(LispObject arg) throws ConditionThrowable {
            return checkStandardObject(arg).getLispClass();
        }
    };

    private static final Primitive STANDARD_INSTANCE_ACCESS = new Primitive("standard-instance-access", PACKAGE_SYS, true, "instance location") {

        @Override
        public LispObject execute(LispObject first, LispObject second) throws ConditionThrowable {
            final StandardObject instance = checkStandardObject(first);
            final int index;
            if (second instanceof Fixnum) {
                index = second.intValue();
            } else {
                return type_error(second, list(SymbolConstants.INTEGER, Fixnum.ZERO, Fixnum.makeFixnum(instance.getInstanceSlotLength())));
            }
            LispObject value = instance.getSlot(index);
            if (value == UNBOUND_VALUE) {
                LispObject slotName = instance.getLayout().getSlotNames()[index];
                value = SymbolConstants.SLOT_UNBOUND.execute(instance.getLispClass(), instance, slotName);
                LispThread.currentThread()._values = null;
            }
            return value;
        }
    };

    private static final Primitive _SET_STANDARD_INSTANCE_ACCESS = new Primitive("%set-standard-instance-access", PACKAGE_SYS, true) {

        @Override
        public LispObject execute(LispObject first, LispObject second, LispObject third) throws ConditionThrowable {
            checkStandardObject(first).setSlot(second.intValue(), third);
            return third;
        }
    };

    private static final Primitive STD_SLOT_BOUNDP = new Primitive(SymbolConstants.STD_SLOT_BOUNDP, "instance slot-name") {

        @Override
        public LispObject execute(LispObject first, LispObject second) throws ConditionThrowable {
            final StandardObject instance = checkStandardObject(first);
            Layout layout = instance.getLayout();
            if (layout.isInvalid()) {
                layout = instance.updateLayout();
            }
            final LispObject index = layout.slotTable.get(second);
            if (index != null) {
                return instance.getSlot(index.intValue()) != UNBOUND_VALUE ? T : NIL;
            }
            final LispObject location = layout.getSharedSlotLocation(second);
            if (location != null) return location.CDR() != UNBOUND_VALUE ? T : NIL;
            final LispThread thread = LispThread.currentThread();
            LispObject value = thread.execute(SymbolConstants.SLOT_MISSING, instance.getLispClass(), instance, second, SymbolConstants.SLOT_BOUNDP);
            thread._values = null;
            return value != NIL ? T : NIL;
        }
    };

    private static final Primitive STD_SLOT_VALUE = new Primitive(SymbolConstants.STD_SLOT_VALUE, "instance slot-name") {

        @Override
        public LispObject execute(LispObject first, LispObject second) throws ConditionThrowable {
            return first.SLOT_VALUE(second);
        }
    };

    private static final Primitive SET_STD_SLOT_VALUE = new Primitive(SymbolConstants.SET_STD_SLOT_VALUE, "instance slot-name new-value") {

        @Override
        public LispObject execute(LispObject first, LispObject second, LispObject third) throws ConditionThrowable {
            first.setSlotValue(second, third);
            return third;
        }
    };
}
