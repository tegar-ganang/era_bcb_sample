package com.bluemarsh.jswat.command;

import com.bluemarsh.jswat.ContextManager;
import com.bluemarsh.jswat.Defaults;
import com.bluemarsh.jswat.Log;
import com.bluemarsh.jswat.Session;
import com.bluemarsh.jswat.expr.EvaluationException;
import com.bluemarsh.jswat.expr.Evaluator;
import com.bluemarsh.jswat.util.Classes;
import com.bluemarsh.jswat.util.Variables;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.ClassType;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Defines the class that handles the 'elements' command.
 *
 * @author  Nathan Fiedler
 */
public class elementsCommand extends JSwatCommand {

    /** An empty List object. */
    private static final List EMPTY_LIST = new LinkedList();

    /**
     * Perform the 'elements' command.
     *
     * @param  session  JSwat session on which to operate.
     * @param  args     Tokenized string of command arguments.
     * @param  out      Output to write messages to.
     */
    public void perform(Session session, CommandArguments args, Log out) {
        if (!session.isActive()) {
            throw new CommandException(Bundle.getString("noActiveSession"));
        }
        ContextManager ctxtman = (ContextManager) session.getManager(ContextManager.class);
        ThreadReference thread = ctxtman.getCurrentThread();
        if (thread == null) {
            throw new CommandException(Bundle.getString("noCurrentThread"));
        }
        int frame = ctxtman.getCurrentFrame();
        if (!args.hasMoreTokens()) {
            throw new MissingArgumentsException();
        }
        Preferences prefs = Preferences.userRoot().node("com/bluemarsh/jswat/util");
        int timeout = prefs.getInt("invocationTimeout", Defaults.INVOCATION_TIMEOUT);
        int start = 0;
        try {
            start = Integer.parseInt(args.peek());
            args.nextToken();
            if (!args.hasMoreTokens()) {
                throw new MissingArgumentsException();
            }
        } catch (NumberFormatException nfe) {
        }
        int end = -1;
        try {
            end = Integer.parseInt(args.peek());
            args.nextToken();
            if (!args.hasMoreTokens()) {
                throw new MissingArgumentsException();
            }
        } catch (NumberFormatException nfe) {
        }
        args.returnAsIs(true);
        String expr = args.rest();
        Evaluator eval = new Evaluator(expr);
        Object o = null;
        try {
            o = eval.evaluate(thread, frame);
        } catch (EvaluationException ee) {
            throw new CommandException(Bundle.getString("evalError") + ' ' + ee.getMessage());
        }
        if (o instanceof ArrayReference) {
            try {
                out.writeln(printArray((ArrayReference) o, start, end, thread));
            } catch (Exception e) {
                throw new CommandException(e.toString(), e);
            }
        } else if (o instanceof ObjectReference) {
            boolean isaCollection = false;
            boolean isaMap = false;
            ObjectReference or = (ObjectReference) o;
            ReferenceType rt = or.referenceType();
            if (rt instanceof ClassType) {
                ClassType ct = (ClassType) rt;
                List interfaces = ct.allInterfaces();
                if (interfaces.size() > 0) {
                    Iterator iter = interfaces.iterator();
                    while (iter.hasNext()) {
                        ReferenceType intf = (ReferenceType) iter.next();
                        String name = intf.name();
                        if (name.equals("java.util.Collection")) {
                            isaCollection = true;
                            break;
                        } else if (name.equals("java.util.Map")) {
                            isaMap = true;
                            break;
                        }
                    }
                }
            }
            if (isaCollection) {
                try {
                    out.writeln(printCollection(or, start, end, thread, timeout));
                } catch (Exception e) {
                    throw new CommandException(e.toString(), e);
                }
            } else if (isaMap) {
                if (start > 0 || end >= 0) {
                    throw new CommandException(Bundle.getString("elements.mapNoIndex"));
                }
                try {
                    out.writeln(printMap(or, thread, timeout));
                } catch (Exception e) {
                    throw new CommandException(e.toString(), e);
                }
            } else {
                throw new CommandException(Bundle.getString("elements.whatIsIt"));
            }
        } else if (o == null) {
            out.writeln(Bundle.getString("elements.isNull"));
        } else {
            throw new CommandException(Bundle.getString("elements.whatIsIt"));
        }
    }

    /**
     * Prints the given range of elements from the collection to a
     * String, separated by newline characters.
     *
     * @param  object   object reference (implements Collection).
     * @param  start    first element to print.
     * @param  end      last element to print.
     * @param  thread   thread on which to invoke methods.
     * @param  timeout  method call timeout in milliseconds.
     * @return  collection elements in a string.
     * @throws  Exception
     *          if anything goes wrong.
     */
    protected String printCollection(ObjectReference object, int start, int end, ThreadReference thread, int timeout) throws Exception {
        ReferenceType type = object.referenceType();
        List methods = type.methodsByName("size", "()I");
        if (methods.size() == 0) {
            throw new IllegalArgumentException("no size() method");
        }
        Method sizeMeth = (Method) methods.get(0);
        IntegerValue size = (IntegerValue) Classes.invokeMethod(object, type, thread, sizeMeth, EMPTY_LIST, timeout);
        if (size == null) {
            throw new Exception("size() returned null");
        }
        if (end < 0 || end >= size.value()) {
            end = size.value();
        } else {
            end++;
        }
        methods = type.methodsByName("iterator", "()Ljava/util/Iterator;");
        if (methods.size() == 0) {
            throw new IllegalArgumentException("no iterator() method");
        }
        Method iterMeth = (Method) methods.get(0);
        ObjectReference iter = (ObjectReference) Classes.invokeMethod(object, type, thread, iterMeth, EMPTY_LIST, timeout);
        StringBuffer buf = new StringBuffer(80);
        if (iter != null) {
            ReferenceType iterType = iter.referenceType();
            methods = iterType.methodsByName("hasNext", "()Z");
            if (methods.size() == 0) {
                throw new IllegalArgumentException("no hasNext() method");
            }
            Method hasNextMeth = (Method) methods.get(0);
            methods = iterType.methodsByName("next", "()Ljava/lang/Object;");
            if (methods.size() == 0) {
                throw new IllegalArgumentException("no next() method");
            }
            Method nextMeth = (Method) methods.get(0);
            BooleanValue bool = (BooleanValue) Classes.invokeMethod(iter, iterType, thread, hasNextMeth, EMPTY_LIST, timeout);
            int count = 0;
            while (bool != null && bool.value() && count < start) {
                Classes.invokeMethod(iter, iterType, thread, nextMeth, EMPTY_LIST, timeout);
                bool = (BooleanValue) Classes.invokeMethod(iter, iterType, thread, hasNextMeth, EMPTY_LIST, timeout);
                count++;
            }
            while (bool != null && bool.value() && count < end) {
                ObjectReference obj = (ObjectReference) Classes.invokeMethod(iter, iterType, thread, nextMeth, EMPTY_LIST, timeout);
                buf.append(count);
                buf.append(": ");
                if (obj != null) {
                    buf.append(Classes.callToString(obj, thread));
                } else {
                    buf.append("null");
                }
                buf.append('\n');
                bool = (BooleanValue) Classes.invokeMethod(iter, iterType, thread, hasNextMeth, EMPTY_LIST, timeout);
                count++;
            }
            int l = buf.length();
            if (l > 0) {
                buf.delete(l - 1, l);
            }
        }
        return buf.toString();
    }

    /**
     * Prints all the elements from the map to a String, separated by
     * newline characters.
     *
     * @param  object   object reference (implements Map).
     * @param  thread   thread on which to invoke methods.
     * @param  timeout  method call timeout in milliseconds.
     * @return  map elements in a string.
     * @throws  Exception
     *          if anything goes wrong.
     */
    protected String printMap(ObjectReference object, ThreadReference thread, int timeout) throws Exception {
        ReferenceType type = object.referenceType();
        List methods = type.methodsByName("get", "(Ljava/lang/Object;)Ljava/lang/Object;");
        if (methods.size() == 0) {
            throw new IllegalArgumentException("no get() method");
        }
        Method getMeth = (Method) methods.get(0);
        methods = type.methodsByName("keySet", "()Ljava/util/Set;");
        if (methods.size() == 0) {
            throw new IllegalArgumentException("no keySet() method");
        }
        Method keyMeth = (Method) methods.get(0);
        ObjectReference set = (ObjectReference) Classes.invokeMethod(object, type, thread, keyMeth, EMPTY_LIST, timeout);
        ReferenceType setType = set.referenceType();
        methods = setType.methodsByName("iterator", "()Ljava/util/Iterator;");
        if (methods.size() == 0) {
            throw new IllegalArgumentException("no iterator() method");
        }
        Method iterMeth = (Method) methods.get(0);
        ObjectReference iter = (ObjectReference) Classes.invokeMethod(set, setType, thread, iterMeth, EMPTY_LIST, timeout);
        StringBuffer buf = new StringBuffer(80);
        if (iter != null) {
            ReferenceType iterType = iter.referenceType();
            methods = iterType.methodsByName("hasNext", "()Z");
            if (methods.size() == 0) {
                throw new IllegalArgumentException("no hasNext() method");
            }
            Method hasNextMeth = (Method) methods.get(0);
            methods = iterType.methodsByName("next", "()Ljava/lang/Object;");
            if (methods.size() == 0) {
                throw new IllegalArgumentException("no next() method");
            }
            Method nextMeth = (Method) methods.get(0);
            BooleanValue bool = (BooleanValue) Classes.invokeMethod(iter, iterType, thread, hasNextMeth, EMPTY_LIST, timeout);
            List args = new LinkedList();
            while (bool != null && bool.value()) {
                ObjectReference key = (ObjectReference) Classes.invokeMethod(iter, iterType, thread, nextMeth, EMPTY_LIST, timeout);
                args.add(key);
                ObjectReference value = (ObjectReference) Classes.invokeMethod(object, type, thread, getMeth, args, timeout);
                args.clear();
                buf.append(Classes.callToString(key, thread));
                buf.append(": ");
                if (value != null) {
                    buf.append(Classes.callToString(value, thread));
                } else {
                    buf.append("null");
                }
                buf.append('\n');
                bool = (BooleanValue) Classes.invokeMethod(iter, iterType, thread, hasNextMeth, EMPTY_LIST, timeout);
            }
            int l = buf.length();
            if (l > 0) {
                buf.delete(l - 1, l);
            }
        }
        return buf.toString();
    }

    /**
     * Prints the given range of elements from the array to a
     * String, separated by newline characters.
     *
     * @param  array   array reference.
     * @param  start   first element to print.
     * @param  end     last element to print.
     * @param  thread  thread from which to get values.
     * @return  array elements in a string.
     * @throws  Exception
     *          because Variables.printValue() can.
     */
    protected String printArray(ArrayReference array, int start, int end, ThreadReference thread) throws Exception {
        if (end < 0 || end >= array.length()) {
            end = array.length();
        } else {
            end++;
        }
        StringBuffer buf = new StringBuffer(80);
        for (int ii = start; ii < end; ii++) {
            buf.append(ii);
            buf.append(": ");
            Value v = array.getValue(ii);
            buf.append(Variables.printValue(v, thread, ", "));
            buf.append('\n');
        }
        int l = buf.length();
        if (l > 0) {
            buf.delete(l - 1, l);
        }
        return buf.toString();
    }
}
