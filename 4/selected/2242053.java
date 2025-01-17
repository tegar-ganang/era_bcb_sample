package com.google.gwt.dev.shell;

import com.google.gwt.dev.shell.BrowserChannel.InvokeOnServerMessage;
import com.google.gwt.dev.shell.BrowserChannel.JavaObjectRef;
import com.google.gwt.dev.shell.BrowserChannel.ReturnMessage;
import com.google.gwt.dev.shell.BrowserChannel.Value;
import com.google.gwt.dev.shell.BrowserChannel.SessionHandler.ExceptionOrReturnValue;
import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Function;
import net.sourceforge.htmlunit.corejs.javascript.Scriptable;
import net.sourceforge.htmlunit.corejs.javascript.ScriptableObject;
import net.sourceforge.htmlunit.corejs.javascript.Undefined;
import java.io.IOException;

/**
 * Class to encapsulate JavaObject on the server side.
 */
public class JavaObject extends ScriptableObject implements Function {

    private static final long serialVersionUID = -7923090130737830902L;

    private static final ExceptionOrReturnValue DEFAULT_VALUE = new ExceptionOrReturnValue(true, new Value());

    public static JavaObject getOrCreateJavaObject(JavaObjectRef javaRef, SessionData sessionData, Context context) {
        return sessionData.getSessionHandler().getOrCreateJavaObject(javaRef.getRefid(), context);
    }

    /**
   * @param cx the Context
   */
    static ExceptionOrReturnValue getReturnFromJavaMethod(Context cx, HtmlUnitSessionHandler sessionHandler, BrowserChannelClient channel, int dispatchId, Value thisValue, Value valueArgs[]) {
        synchronized (sessionHandler.getSynchronizationObject()) {
            try {
                new InvokeOnServerMessage(channel, dispatchId, thisValue, valueArgs).send();
            } catch (IOException e) {
                return DEFAULT_VALUE;
            }
            try {
                ReturnMessage returnMessage = channel.reactToMessagesWhileWaitingForReturn(sessionHandler);
                return new ExceptionOrReturnValue(returnMessage.isException(), returnMessage.getReturnValue());
            } catch (IOException e) {
                return DEFAULT_VALUE;
            } catch (BrowserChannelException e) {
                return DEFAULT_VALUE;
            }
        }
    }

    /**
   * @param jsContext the Context
   */
    static boolean isJavaObject(Context jsContext, ScriptableObject javaObject) {
        return javaObject instanceof JavaObject;
    }

    private Context jsContext;

    private final int objectRef;

    private final SessionData sessionData;

    public JavaObject(Context jsContext, SessionData sessionData, int objectRef) {
        this.objectRef = objectRef;
        this.sessionData = sessionData;
        this.jsContext = jsContext;
    }

    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
        if (args.length < 2) {
            return Undefined.instance;
        }
        Value valueArgs[] = new Value[args.length - 2];
        for (int i = 0; i < valueArgs.length; i++) {
            valueArgs[i] = sessionData.getSessionHandler().makeValueFromJsval(cx, args[i + 2]);
        }
        Value thisValue = sessionData.getSessionHandler().makeValueFromJsval(cx, args[1]);
        int dispatchId = ((Number) args[0]).intValue();
        ExceptionOrReturnValue returnValue = getReturnFromJavaMethod(cx, sessionData.getSessionHandler(), sessionData.getChannel(), dispatchId, thisValue, valueArgs);
        Object ret[] = new Object[2];
        ret[0] = returnValue.isException();
        ret[1] = sessionData.getSessionHandler().makeJsvalFromValue(cx, returnValue.getReturnValue());
        return ret;
    }

    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        throw Context.reportRuntimeError("JavaObject can't be used as a " + "constructor");
    }

    @Override
    public Object get(int index, Scriptable start) {
        Value value = ServerMethods.getProperty(sessionData.getChannel(), sessionData.getSessionHandler(), objectRef, index);
        return sessionData.getSessionHandler().makeJsvalFromValue(jsContext, value);
    }

    @Override
    public Object get(String name, Scriptable start) {
        if ("toString".equals(name)) {
            return sessionData.getSessionHandler().getToStringTearOff(jsContext);
        }
        if ("id".equals(name)) {
            return objectRef;
        }
        if ("__noSuchMethod__".equals(name)) {
            return Undefined.instance;
        }
        System.err.println("Unknown property name in get " + name);
        return Undefined.instance;
    }

    @Override
    public String getClassName() {
        return "Class JavaObject";
    }

    @Override
    public void put(int dispatchId, Scriptable start, Object value) {
        HtmlUnitSessionHandler sessionHandler = sessionData.getSessionHandler();
        if (!ServerMethods.setProperty(sessionData.getChannel(), sessionHandler, objectRef, dispatchId, sessionHandler.makeValueFromJsval(jsContext, value))) {
            throw new RuntimeException("setProperty failed");
        }
    }

    int getRefId() {
        return objectRef;
    }
}
