package de.jassda.jabyba.backend.event;

import static de.jassda.jabyba.backend.util.Constants.Logging.*;
import java.util.ArrayList;
import de.jassda.jabyba.backend.Backend;
import de.jassda.jabyba.backend.runtime.RuntimeThread;
import de.jassda.jabyba.backend.util.ByteUtil;
import de.jassda.jabyba.backend.util.PacketFactory;
import de.jassda.jabyba.jdwp.JDWP;
import de.jassda.jabyba.jdwp.Packet;

/**
 * A ClassPrepareEvent is used to signal that a class is being prepared. This event is supposed to
 * be triggered by the {@link de.jassda.core.bytecode.ClassLoader ClassLoader}. 
 * @author <a href="mailto:johannes.rieken@informatik.uni-oldenburg.de">riejo</a> */
public class ClassPrepareEvent extends Event {

    /**
	 * Creates a new event. Nothing special here.
	 * @see Event#Event(int, byte[], Backend)
	 * @param requestID	The request id for this event.
	 * @param data			The modifier data applying to this event. 
	 * @param backend 	Reference to the backend. */
    public ClassPrepareEvent(int requestID, byte[] data, Backend backend) {
        super(requestID, data, backend);
    }

    /**
	 * Checks the passed event data for confirmity. The ClassPrepareEvent expected 2 objects. 
	 * The first represents the class being loaded and the second is an integer representing the
	 * object identifier. 
	 * @see de.jassda.jabyba.backend.event.Event#checkEventData(java.lang.Object[])
	 * @param data The event data passed to the event.
	 * @return Returns <code>true</code> if the passed array fulfils the conditions named above. 	 */
    protected boolean checkEventData(Object... data) {
        return data.length == 2 && data[0] instanceof Class && data[1] instanceof Integer;
    }

    /**
	 * Filter for this event. The filter applies if the inherited filter applies or if the name of the class this event triggers 
	 * for does not match any of the patterns.
	 * @see de.jassda.jabyba.backend.event.Event#filter(de.jassda.jabyba.backend.runtime.RuntimeThread, java.lang.Object[])
	 * @return <code>true</code> if the filter applies and inhibits the event from triggering, <code>false</code> otherwise.	 */
    public boolean filter(RuntimeThread thread, Object... o) {
        boolean filter = super.filter(thread, o);
        String className = ((Class) o[0]).getName();
        ArrayList<String> patterns = getModifierClassMatch();
        boolean patternFilter = false;
        for (String pattern : patterns) {
            if (pattern.equals("*")) patternFilter = false; else if (pattern.startsWith("*")) patternFilter = !className.endsWith(pattern.substring(1)); else if (pattern.endsWith("*")) patternFilter = !className.startsWith(pattern.substring(0, pattern.length() - 1)); else patternFilter = !className.equals(pattern);
            if (!patternFilter) break;
        }
        return filter || patternFilter;
    }

    /**
	 * The implementation of the actual event. What happens is that a packet is composed which 
	 * contains a description of the class this event was triggered for. 
	 * @see de.jassda.backend.core.event.Event#trigger(java.lang.Object...) */
    protected void triggerImpl(RuntimeThread thread, Object... o) {
        Packet p = PacketFactory.emptyComposite();
        Class clazz = (Class) o[0];
        int referenceTypeID = (Integer) o[1];
        String signatureStr = "L" + clazz.getName().replace('.', '/') + ";";
        byte[] signature = ByteUtil.writeString(signatureStr);
        p.data = ByteUtil.merge(new byte[] { getSuspendPolicy(), 0, 0, 0, 1, getEventKind() }, ByteUtil.writeInteger(getRequestID()), ByteUtil.writeInteger(thread.getIdentifier()), new byte[] { (byte) (clazz.isInterface() ? JDWP.TypeTag.INTERFACE : JDWP.TypeTag.CLASS) }, ByteUtil.writeInteger(referenceTypeID), signature, ByteUtil.writeInteger(JDWP.ClassStatus.PREPARED));
        pWriter.sendPacket(p);
        logJDWP.info("Send packet from " + this + "\nPacket=" + p.toStringFully());
    }
}
