package de.jassda.jabyba.backend.event;

import static de.jassda.jabyba.backend.util.Constants.EventModifiers.*;
import static de.jassda.jabyba.backend.util.Constants.Logging.*;
import de.jassda.jabyba.backend.Backend;
import de.jassda.jabyba.backend.bytecode.BreakpointPatcher;
import de.jassda.jabyba.backend.runtime.RuntimeThread;
import de.jassda.jabyba.backend.runtime.StackFrame;
import de.jassda.jabyba.backend.util.ByteUtil;
import de.jassda.jabyba.backend.util.PacketFactory;
import de.jassda.jabyba.jdwp.Packet;

/**
 * This class models the event which is triggered when a <i>Bytecode-Breakpoint</i> is hit.
 * <i>Bytecode-Breakpoint</i> means the bytecode modification responsible for this event.
 * <br/>A BreakpointEvent needs the location modifier. The location modifiers contains information
 * about the class and the method to patch. 
 * @author <a href="mailto:johannes.rieken@informatik.uni-oldenburg.de">riejo</a> */
public class BreakpointEvent extends Event {

    /**
	 * Creates a new BreakpointEvent. The event is created form data retrieved 
	 * by the backend. Therefore the data part of the incoming JDWP packet is 
	 * requiered. The data part must contain a location attribute, otherwise it's
	 * not possible to set a breakpoint!
	 * @see Event#Event(int, byte[], Backend)
	 * @param requestID The id of the initiating event request.
	 * @param data The data send with the initiating packet.
	 * @param backend Reference to the backend.
	 * @throws IllegalArgumentException If this event has no location modifier.	 */
    public BreakpointEvent(int requestID, byte[] data, Backend backend) {
        super(requestID, data, backend);
        if (!hasModifier(LOCATION_ONLY)) throw new IllegalArgumentException("Location modfier is missing");
    }

    /**
	 * @see de.jassda.jabyba.backend.event.Event#checkEventData(java.lang.Object[])	 
	 * @return Returns <code>true</code> if the event data consits of one object. */
    protected boolean checkEventData(Object... data) {
        return data.length == 1;
    }

    /**
	 * Implementation for the breakpoint event. A new frame with the return value for the
	 * event thread is created. After that a packet is composed and send to the debugger. 
	 * @see de.jassda.jabyba.backend.event.Event#triggerImpl(de.jassda.jabyba.backend.runtime.RuntimeThread, java.lang.Object[])	 */
    protected void triggerImpl(RuntimeThread thread, Object... data) {
        StackFrame.createStackFrame(data[0], getModifierLocationOnly(), thread);
        Packet p = PacketFactory.emptyComposite();
        p.data = ByteUtil.merge(new byte[] { getSuspendPolicy(), 0, 0, 0, 1, getEventKind() }, ByteUtil.writeInteger(getRequestID()), ByteUtil.writeInteger(thread.getIdentifier()), getModifierLocationOnly());
        pWriter.sendPacket(p);
        logJDWP.info("Send packet from " + this + "\nPacket=" + p.toStringFully());
    }

    /**
	 * This method is supposed to be called from modified classes. Therefore it is static
	 * and requires the requestID as parameter.
	 * @param result The result of a method invokation or <code>null</code>.
	 * @param requestID The requestID which identifies a event.
	 * @param thread The event thread.	 */
    public static void hit(Object result, int requestID, Thread thread) {
        Event event = eventRegistry.get(requestID);
        if (event != null) {
            RuntimeThread rtThread = RuntimeThread.newInstance(thread);
            event.trigger(rtThread, result);
        }
    }

    /**
	 * Convenience method.
	 * @see #hit(Object, int, Thread)
	 * @param result	The result is a floating point value..	 */
    public static void hit(float result, int requestID, Thread thread) {
        hit(new Float(result), requestID, thread);
    }

    /**
	 * Convenience method.
	 * @see #hit(Object, int, Thread)
	 * @param result	The result is a floating point value..	 */
    public static void hit(double result, int requestID, Thread thread) {
        hit(new Double(result), requestID, thread);
    }

    /**
	 * Convenience method.
	 * @see #hit(Object, int, Thread)
	 * @param result	The result is a integer point value..	 */
    public static void hit(int result, int requestID, Thread thread) {
        hit(new Integer(result), requestID, thread);
    }

    /**
	 * 	Convenience method.
	 * @see #hit(Object, int, Thread)
	 * @param result The result is a integer value.	 */
    public static void hit(long result, int requestID, Thread thread) {
        hit(new Long(result), requestID, thread);
    }

    /**
	 * Convenience method. To be used if there's no return type. Either
	 * method entrys or void returns.
	 * @see #hit(Object, int, Thread)	 */
    public static void hit(int requestID, Thread thread) {
        hit(null, requestID, thread);
    }
}
