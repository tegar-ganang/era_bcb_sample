package com.bonkabonka.queue;

import java.lang.reflect.Method;

/**
 * Root-level administrative queue.
 * 
 * @author jamesc
 */
public final class SystemQueue extends StatusQueue {

    public static final String VERSION = "NGDC fork";

    private transient SystemCommandHandler cmds;

    /**
	 * This attaches the SystemCommandHandler to the bucket and sets the version
	 * string.
	 * 
	 * @param bucket
	 *            QueueBucket to attach the SystemCommandHandler to
	 */
    @Override
    public void init(QueueBucket bucket, String name) {
        super.init(bucket, name);
        cmds = new SystemCommandHandler(bucket);
        super.push("");
    }

    /**
	 * This processes all incoming system commands. Reflection is used to expose
	 * the SystemCommandHandler methods through this interface.
	 * 
	 * @see SystemCommandHandler
	 * 
	 * @param message
	 *            String containing the command to execute
	 * @return String with the result of the command
	 */
    @Override
    public String push(String message) {
        String result = null;
        if (message != null && !message.trim().equals("")) {
            Class stringClass = message.getClass();
            String[] data = message.split("\\s+");
            Class[] argTypes = new Class[data.length - 1];
            for (int i = 0; i < argTypes.length; i++) {
                argTypes[i] = stringClass;
            }
            Object[] args = new Object[data.length - 1];
            for (int i = 0; i < args.length; i++) {
                args[i] = data[i + 1];
            }
            try {
                Method m = cmds.getClass().getMethod(data[0], argTypes);
                if (m == null) {
                    StringBuffer err = new StringBuffer();
                    err.append("no method named '");
                    err.append(data[0]);
                    err.append("' taking ");
                    err.append(data.length - 1);
                    err.append(" strings as arguments");
                    throw new NoSuchMethodException(err.toString());
                }
                Object o = m.invoke(cmds, args);
                if (o == null) {
                    result = "OK";
                } else {
                    result = o.toString();
                }
            } catch (Throwable t) {
                t.printStackTrace();
                result = "ERR: " + t.toString();
            }
        }
        return result;
    }
}
