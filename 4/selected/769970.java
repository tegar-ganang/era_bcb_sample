package de.jassda.jabyba.backend.event;

import static de.jassda.jabyba.backend.util.Constants.Logging.logJDWP;
import static de.jassda.jabyba.backend.util.Constants.*;
import de.jassda.jabyba.backend.Backend;
import de.jassda.jabyba.backend.runtime.RuntimeThread;
import de.jassda.jabyba.backend.util.ByteUtil;
import de.jassda.jabyba.backend.util.PacketFactory;
import de.jassda.jabyba.jdwp.JDWP;
import de.jassda.jabyba.jdwp.Packet;

/**
 * A ExceptionEvent is triggered whenever a exception is detected. 
 * 
 * @author <a href="mailto:johannes.rieken@informatik.uni-oldenburg.de">riejo</a> */
public class ExceptionEvent extends Event {

    /**
	 * @param requestID
	 * @param data
	 * @param backend */
    public ExceptionEvent(int requestID, byte[] data, Backend backend) {
        super(requestID, data, backend);
    }

    protected boolean checkEventData(Object... data) {
        return data.length == 3 && data[0] instanceof Throwable && data[1] instanceof String && data[2] instanceof String;
    }

    /**
	 * @see de.jassda.jabyba.backend.event.Event#triggerImpl(java.lang.Thread, java.lang.Object...) */
    protected void triggerImpl(RuntimeThread thread, Object... data) {
        Throwable exception = (Throwable) data[0];
        String className = (String) data[1];
        String methodName = (String) data[2];
        int exceptionID = backend.getRuntimeRegistry().newObject(exception);
        int referenceID = backend.getRuntimeRegistry().getReferenceID(className);
        int methodID = backend.getRuntimeRegistry().getMethodID(className, methodName, null);
        byte[] throwLocation = ByteUtil.merge(new byte[] { JDWP.TypeTag.CLASS }, ByteUtil.writeInteger(referenceID), ByteUtil.writeInteger(methodID), ByteUtil.writeLong(Method.lastLineOpcode));
        byte[] emptyCatchLocation = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        Packet p = PacketFactory.emptyComposite();
        p.data = ByteUtil.merge(new byte[] { getSuspendPolicy(), 0, 0, 0, 1, getEventKind() }, ByteUtil.writeInteger(getRequestID()), ByteUtil.writeInteger(thread.getIdentifier()), throwLocation, new byte[] { JDWP.Tag.OBJECT }, ByteUtil.writeInteger(exceptionID), emptyCatchLocation);
        pWriter.sendPacket(p);
        logJDWP.info("Send packet from " + this + "\nPacket=" + p.toStringFully());
    }

    /**
	 * This method is called staticly from a patched class. The call to this method happens
	 * in a catch block. 
	 * @param t The Throwable-Object.
	 * @param requestID The request which requested this exception event.
	 * @param thread The thread in which the exception occured.	 */
    public static void exceptionOccured(Throwable t, int requestID, Thread thread) {
        Event event = eventRegistry.get(requestID);
        StackTraceElement ste = t.getStackTrace()[0];
        event.trigger(RuntimeThread.newInstance(thread), t, ste.getClassName(), ste.getMethodName());
    }
}
