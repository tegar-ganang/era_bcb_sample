package de.jassda.jabyba.backend.util;

import de.jassda.jabyba.jdwp.JDWP;
import de.jassda.jabyba.jdwp.Packet;

/**
 * Contains some useful methods to quickly create JDWP-packets.
 * @author <a href="mailto:johannes.rieken@informatik.uni-oldenburg.de">riejo</a> */
public class PacketFactory {

    /**
	 * @param threadID Thread ID (here always the threads hash code) in which the event occured.
	 * @return Returns a gerenic compostite packet which signals the start of the vm/backend. */
    public static Packet packetVMStart(int threadID) {
        Packet p = emptyComposite();
        p.data = new byte[] { JDWP.SuspendPolicy.NONE, 0, 0, 0, 1, JDWP.EventKind.VM_START, 0, 0, 0, 0 };
        p.data = ByteUtil.merge(p.data, ByteUtil.writeInteger(threadID));
        return p;
    }

    /**
	 * Create a packet signalling that the vm is dead. This event is not requested.
	 * @return	Packet according to {@link http://java.sun.com/j2se/1.5.0/docs/guide/jpda/jdwp/jdwp-protocol.html#JDWP_Event_Composite 
	 * VM_Death event} */
    public static Packet packetVMDeath() {
        Packet p = emptyComposite();
        p.data = new byte[] { JDWP.SuspendPolicy.NONE, 0, 0, 0, 1, JDWP.EventKind.VM_DEATH, 0, 0, 0, 0 };
        return p;
    }

    /**
	 * @return Returns a gerenic empty compostite packet. Id, commandset, and command are set.*/
    public static Packet emptyComposite() {
        Packet p = new Packet();
        p.commandset = JDWP.CommandSet.Event.ID;
        p.command = JDWP.CommandSet.Event.Composite;
        return p;
    }

    /**
	 * Takes the passed packet, deletes its data part, switches its flag to REPLY, and
	 * sets the error code to NONE. The ID stays untouched.
	 * @return Empty reply packet.  */
    public static Packet changeToReply(Packet cmd) {
        cmd.errorCode = JDWP.Error.NONE;
        cmd.flag = Packet.REPLY;
        cmd.data = new byte[] {};
        return cmd;
    }

    /**
	 * 
	 * @see #changeToReply(Packet)
	 * @param cmd The request which is can not be dispatched...
	 * @return Returns a reply packet which carries the NOT_IMPLEMENTED
	 * 		error flag.
	 */
    public static Packet notImplemented(Packet cmd) {
        cmd = changeToReply(cmd);
        cmd.errorCode = JDWP.Error.NOT_IMPLEMENTED;
        return cmd;
    }
}
