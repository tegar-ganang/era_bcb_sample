package org.armedbear.lisp;

import static org.armedbear.lisp.Nil.NIL;
import static org.armedbear.lisp.Lisp.*;

public class Condition extends StandardObjectImpl {

    protected String message;

    public Condition() throws ConditionThrowable {
        super(StandardClass.CONDITION);
        LispObject[] slots = getSlots();
        Debug.assertTrue(slots.length == 2);
        setFormatArguments(NIL);
    }

    protected Condition(LispClass cls) throws ConditionThrowable {
        super(cls);
        LispObject[] slots = getSlots();
        Debug.assertTrue(slots.length >= 2);
        setFormatArguments(NIL);
    }

    public Condition(LispClass cls, int length) {
        super(cls, length);
    }

    public Condition(LispObject initArgs) throws ConditionThrowable {
        super(StandardClass.CONDITION);
        LispObject[] slots = getSlots();
        Debug.assertTrue(slots.length == 2);
        initialize(initArgs);
    }

    protected void initialize(LispObject initArgs) throws ConditionThrowable {
        LispObject control = null;
        LispObject arguments = null;
        LispObject first, second;
        while (initArgs instanceof Cons) {
            first = initArgs.CAR();
            initArgs = initArgs.CDR();
            second = initArgs.CAR();
            initArgs = initArgs.CDR();
            if (first == Keyword.FORMAT_CONTROL) {
                if (control == null) control = second;
            } else if (first == Keyword.FORMAT_ARGUMENTS) {
                if (arguments == null) arguments = second;
            }
        }
        if (control != null) setFormatControl(control);
        if (arguments == null) arguments = NIL;
        setFormatArguments(arguments);
    }

    public Condition(String message) {
        super(StandardClass.CONDITION);
        LispObject[] slots = getSlots();
        Debug.assertTrue(slots.length == 2);
        try {
            setFormatControl(message);
            setFormatArguments(NIL);
        } catch (Throwable t) {
            Debug.trace(t);
        }
    }

    public final LispObject getFormatControl() throws ConditionThrowable {
        return getInstanceSlotValue(SymbolConstants.FORMAT_CONTROL);
    }

    public final void setFormatControl(LispObject formatControl) throws ConditionThrowable {
        setInstanceSlotValue(SymbolConstants.FORMAT_CONTROL, formatControl);
    }

    public final void setFormatControl(String s) throws ConditionThrowable {
        setFormatControl(new SimpleString(s));
    }

    public final LispObject getFormatArguments() throws ConditionThrowable {
        return getInstanceSlotValue(SymbolConstants.FORMAT_ARGUMENTS);
    }

    public final void setFormatArguments(LispObject formatArguments) throws ConditionThrowable {
        setInstanceSlotValue(SymbolConstants.FORMAT_ARGUMENTS, formatArguments);
    }

    public String getMessage() throws ConditionThrowable {
        return message;
    }

    @Override
    public LispObject typeOf() {
        LispClass c = getLispClass();
        if (c != null) return c.getSymbol();
        return SymbolConstants.CONDITION;
    }

    @Override
    public LispObject classOf() {
        LispClass c = getLispClass();
        if (c != null) return c;
        return StandardClass.CONDITION;
    }

    @Override
    public LispObject typep(LispObject type) throws ConditionThrowable {
        if (type == SymbolConstants.CONDITION) return T;
        if (type == StandardClass.CONDITION) return T;
        return super.typep(type);
    }

    public String getConditionReport() throws ConditionThrowable {
        String s = getMessage();
        if (s != null) return s;
        LispObject formatControl = getFormatControl();
        if (formatControl != NIL) {
            try {
                return format(formatControl, getFormatArguments());
            } catch (Throwable t) {
            }
        }
        return unreadableString(typeOf().writeToString());
    }

    @Override
    public String writeToString() throws ConditionThrowable {
        final LispThread thread = LispThread.currentThread();
        if (SymbolConstants.PRINT_ESCAPE.symbolValue(thread) == NIL) {
            String s = getMessage();
            if (s != null) return s;
            LispObject formatControl = getFormatControl();
            if (formatControl instanceof Function) {
                StringOutputStream stream = new StringOutputStream();
                SymbolConstants.APPLY.execute(formatControl, stream, getFormatArguments());
                return stream.getStringOutputString().getStringValue();
            }
            if (formatControl instanceof AbstractString) {
                LispObject f = SymbolConstants.FORMAT.getSymbolFunction();
                if (f == null || f instanceof Autoload) return format(formatControl, getFormatArguments());
                return SymbolConstants.APPLY.execute(f, NIL, formatControl, getFormatArguments()).getStringValue();
            }
        }
        final int maxLevel;
        LispObject printLevel = SymbolConstants.PRINT_LEVEL.symbolValue(thread);
        if (printLevel instanceof Fixnum) maxLevel = printLevel.intValue(); else maxLevel = Integer.MAX_VALUE;
        LispObject currentPrintLevel = _CURRENT_PRINT_LEVEL_.symbolValue(thread);
        int currentLevel = currentPrintLevel.intValue();
        if (currentLevel >= maxLevel) return "#";
        return unreadableString(typeOf().writeToString());
    }
}
