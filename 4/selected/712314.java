package com.bluemarsh.jswat.command.commands;

import com.bluemarsh.jswat.command.AbstractCommand;
import com.bluemarsh.jswat.command.CommandArguments;
import com.bluemarsh.jswat.command.CommandContext;
import com.bluemarsh.jswat.command.CommandException;
import com.bluemarsh.jswat.command.MissingArgumentsException;
import com.bluemarsh.jswat.core.context.DebuggingContext;
import com.bluemarsh.jswat.core.expr.EvaluationException;
import com.bluemarsh.jswat.core.expr.Evaluator;
import com.bluemarsh.jswat.core.util.Classes;
import com.bluemarsh.jswat.core.util.Strings;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.CharValue;
import com.sun.jdi.ClassType;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.openide.util.NbBundle;

/**
 * Displays the elements in a collection in the debuggee.
 *
 * @author Nathan Fiedler
 */
public class ElementsCommand extends AbstractCommand {

    /** An empty List object of type Value. */
    private static final List<Value> EMPTY_LIST = Collections.emptyList();

    public String getName() {
        return "elements";
    }

    /**
     * Calls the toString() method fo the given object.
     *
     * @param  obj     object from which to get string.
     * @param  thread  thread on which to invoke method.
     * @return  String, or null if error.
     */
    private static String invokeToString(ObjectReference obj, ThreadReference thread) throws Exception {
        ClassType type = (ClassType) obj.referenceType();
        List<Method> methods = type.methodsByName("toString", "()Ljava/lang/String;");
        Method method = methods.get(0);
        Object o = Classes.invokeMethod(obj, type, thread, method, EMPTY_LIST);
        String s = o == null ? "null" : o.toString();
        return Strings.trimQuotes(s);
    }

    public void perform(CommandContext context, CommandArguments arguments) throws CommandException, MissingArgumentsException {
        PrintWriter writer = context.getWriter();
        DebuggingContext dc = context.getDebuggingContext();
        ThreadReference thread = dc.getThread();
        int frame = dc.getFrame();
        int start = 0;
        try {
            start = Integer.parseInt(arguments.peek());
            arguments.nextToken();
            if (!arguments.hasMoreTokens()) {
                throw new MissingArgumentsException();
            }
        } catch (NumberFormatException nfe) {
        }
        int end = -1;
        try {
            end = Integer.parseInt(arguments.peek());
            arguments.nextToken();
            if (!arguments.hasMoreTokens()) {
                throw new MissingArgumentsException();
            }
        } catch (NumberFormatException nfe) {
        }
        arguments.returnAsIs(true);
        String expr = arguments.rest();
        Evaluator eval = new Evaluator(expr);
        Object o = null;
        try {
            o = eval.evaluate(thread, frame);
        } catch (EvaluationException ee) {
            throw new CommandException(NbBundle.getMessage(getClass(), "ERR_EvaluationError", ee.getMessage()));
        }
        if (o instanceof ArrayReference) {
            try {
                writer.println(printArray((ArrayReference) o, start, end, thread));
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
                    writer.println(printCollection(or, start, end, thread));
                } catch (Exception e) {
                    throw new CommandException(e.toString(), e);
                }
            } else if (isaMap) {
                if (start > 0 || end >= 0) {
                    throw new CommandException(NbBundle.getMessage(getClass(), "ERR_elements_MapNoIndex"));
                }
                try {
                    writer.println(printMap(or, thread));
                } catch (Exception e) {
                    throw new CommandException(e.toString(), e);
                }
            } else {
                throw new CommandException(NbBundle.getMessage(getClass(), "ERR_elements_NotCollection"));
            }
        } else if (o == null) {
            writer.println(NbBundle.getMessage(getClass(), "ERR_elements_IsNull"));
        } else {
            throw new CommandException(NbBundle.getMessage(getClass(), "ERR_elements_NotCollection"));
        }
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
    private static String printArray(ArrayReference array, int start, int end, ThreadReference thread) throws Exception {
        if (end < 0 || end >= array.length()) {
            end = array.length();
        } else {
            end++;
        }
        StringBuilder sb = new StringBuilder(80);
        for (int ii = start; ii < end; ii++) {
            sb.append(ii);
            sb.append(": ");
            Value v = array.getValue(ii);
            sb.append(printValue(v, thread, ", "));
            sb.append('\n');
        }
        int l = sb.length();
        if (l > 0) {
            sb.delete(l - 1, l);
        }
        return sb.toString();
    }

    /**
     * Prints the given range of elements from the collection to a
     * String, separated by newline characters.
     *
     * @param  object  object reference (implements Collection).
     * @param  start   first element to print.
     * @param  end     last element to print.
     * @param  thread  thread on which to invoke methods.
     * @return  collection elements in a string.
     * @throws  Exception
     *          if anything goes wrong.
     */
    private static String printCollection(ObjectReference object, int start, int end, ThreadReference thread) throws Exception {
        ClassType type = (ClassType) object.referenceType();
        List methods = type.methodsByName("size", "()I");
        if (methods.size() == 0) {
            throw new IllegalArgumentException("no size() method");
        }
        Method sizeMeth = (Method) methods.get(0);
        IntegerValue size = (IntegerValue) Classes.invokeMethod(object, type, thread, sizeMeth, EMPTY_LIST);
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
        ObjectReference iter = (ObjectReference) Classes.invokeMethod(object, type, thread, iterMeth, EMPTY_LIST);
        StringBuilder sb = new StringBuilder(80);
        if (iter != null) {
            ClassType iterType = (ClassType) iter.referenceType();
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
            BooleanValue bool = (BooleanValue) Classes.invokeMethod(iter, iterType, thread, hasNextMeth, EMPTY_LIST);
            int count = 0;
            while (bool != null && bool.value() && count < start) {
                Classes.invokeMethod(iter, iterType, thread, nextMeth, EMPTY_LIST);
                bool = (BooleanValue) Classes.invokeMethod(iter, iterType, thread, hasNextMeth, EMPTY_LIST);
                count++;
            }
            while (bool != null && bool.value() && count < end) {
                ObjectReference obj = (ObjectReference) Classes.invokeMethod(iter, iterType, thread, nextMeth, EMPTY_LIST);
                sb.append(count);
                sb.append(": ");
                if (obj != null) {
                    sb.append(invokeToString(obj, thread));
                } else {
                    sb.append("null");
                }
                sb.append('\n');
                bool = (BooleanValue) Classes.invokeMethod(iter, iterType, thread, hasNextMeth, EMPTY_LIST);
                count++;
            }
            int l = sb.length();
            if (l > 0) {
                sb.delete(l - 1, l);
            }
        }
        return sb.toString();
    }

    /**
     * Prints all the elements from the map to a String, separated by
     * newline characters.
     *
     * @param  object  object reference (implements Map).
     * @param  thread  thread on which to invoke methods.
     * @return  map elements in a string.
     * @throws  Exception
     *          if anything goes wrong.
     */
    private static String printMap(ObjectReference object, ThreadReference thread) throws Exception {
        ClassType type = (ClassType) object.referenceType();
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
        ObjectReference set = (ObjectReference) Classes.invokeMethod(object, type, thread, keyMeth, EMPTY_LIST);
        ClassType setType = (ClassType) set.referenceType();
        methods = setType.methodsByName("iterator", "()Ljava/util/Iterator;");
        if (methods.size() == 0) {
            throw new IllegalArgumentException("no iterator() method");
        }
        Method iterMeth = (Method) methods.get(0);
        ObjectReference iter = (ObjectReference) Classes.invokeMethod(set, setType, thread, iterMeth, EMPTY_LIST);
        StringBuilder sb = new StringBuilder(80);
        if (iter != null) {
            ClassType iterType = (ClassType) iter.referenceType();
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
            BooleanValue bool = (BooleanValue) Classes.invokeMethod(iter, iterType, thread, hasNextMeth, EMPTY_LIST);
            List<Value> arguments = new LinkedList<Value>();
            while (bool != null && bool.value()) {
                ObjectReference key = (ObjectReference) Classes.invokeMethod(iter, iterType, thread, nextMeth, EMPTY_LIST);
                arguments.add(key);
                ObjectReference value = (ObjectReference) Classes.invokeMethod(object, type, thread, getMeth, arguments);
                arguments.clear();
                sb.append(invokeToString(key, thread));
                sb.append(": ");
                if (value != null) {
                    sb.append(invokeToString(value, thread));
                } else {
                    sb.append("null");
                }
                sb.append('\n');
                bool = (BooleanValue) Classes.invokeMethod(iter, iterType, thread, hasNextMeth, EMPTY_LIST);
            }
            int l = sb.length();
            if (l > 0) {
                sb.delete(l - 1, l);
            }
        }
        return sb.toString();
    }

    /**
     * Print the value as a pretty string. If the value is an array,
     * prints the array values. If the value is an object that is not a
     * String, calls to toString() on that object.
     *
     * <p>Note that this method may invalidate the current stack frame.
     * It may be necessary to retrieve the stack frame again.</p>
     *
     * @param  value   value to print.
     * @param  thread  thread for calling toString().
     * @param  arrsep  array element separator string.
     * @return  value as a String.
     * @throws  Exception
     *          literally anything could go wrong.
     */
    private static String printValue(Value value, ThreadReference thread, String arrsep) throws Exception {
        StringBuilder sb = new StringBuilder(80);
        if (value instanceof ArrayReference) {
            ArrayReference ar = (ArrayReference) value;
            if (ar.length() > 0) {
                sb.append(0);
                sb.append(": ");
                Value v = ar.getValue(0);
                sb.append(v == null ? "null" : v.toString());
                for (int i = 1; i < ar.length(); i++) {
                    sb.append(arrsep);
                    sb.append(i);
                    sb.append(": ");
                    v = ar.getValue(i);
                    sb.append(v == null ? "null" : v.toString());
                }
            }
        } else if (value instanceof StringReference) {
            sb.append(value.toString());
        } else if (value instanceof ObjectReference) {
            String s = invokeToString((ObjectReference) value, thread);
            sb.append(s != null ? s : "null");
        } else if (value instanceof CharValue) {
            CharValue cv = (CharValue) value;
            sb.append("\\u");
            sb.append(Strings.toHexString(cv.value()));
        } else if (value == null) {
            sb.append("null");
        } else {
            sb.append(value.toString());
        }
        return sb.toString();
    }

    public boolean requiresArguments() {
        return true;
    }

    public boolean requiresDebuggee() {
        return true;
    }

    public boolean requiresThread() {
        return true;
    }
}
