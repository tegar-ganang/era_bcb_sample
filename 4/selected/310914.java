package gnu.classpath.jdwp.processor;

import gnu.classpath.jdwp.JdwpConstants;
import gnu.classpath.jdwp.VMFrame;
import gnu.classpath.jdwp.VMVirtualMachine;
import gnu.classpath.jdwp.exception.InvalidObjectException;
import gnu.classpath.jdwp.exception.JdwpException;
import gnu.classpath.jdwp.exception.JdwpInternalErrorException;
import gnu.classpath.jdwp.exception.NotImplementedException;
import gnu.classpath.jdwp.id.ObjectId;
import gnu.classpath.jdwp.id.ThreadId;
import gnu.classpath.jdwp.util.JdwpString;
import gnu.classpath.jdwp.util.Location;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * A class representing the ThreadReference Command Set.
 * 
 * @author Aaron Luchko <aluchko@redhat.com>
 */
public class ThreadReferenceCommandSet extends CommandSet {

    public boolean runCommand(ByteBuffer bb, DataOutputStream os, byte command) throws JdwpException {
        try {
            switch(command) {
                case JdwpConstants.CommandSet.ThreadReference.NAME:
                    executeName(bb, os);
                    break;
                case JdwpConstants.CommandSet.ThreadReference.SUSPEND:
                    executeSuspend(bb, os);
                    break;
                case JdwpConstants.CommandSet.ThreadReference.RESUME:
                    executeResume(bb, os);
                    break;
                case JdwpConstants.CommandSet.ThreadReference.STATUS:
                    executeStatus(bb, os);
                    break;
                case JdwpConstants.CommandSet.ThreadReference.THREAD_GROUP:
                    executeThreadGroup(bb, os);
                    break;
                case JdwpConstants.CommandSet.ThreadReference.FRAMES:
                    executeFrames(bb, os);
                    break;
                case JdwpConstants.CommandSet.ThreadReference.FRAME_COUNT:
                    executeFrameCount(bb, os);
                    break;
                case JdwpConstants.CommandSet.ThreadReference.OWNED_MONITORS:
                    executeOwnedMonitors(bb, os);
                    break;
                case JdwpConstants.CommandSet.ThreadReference.CURRENT_CONTENDED_MONITOR:
                    executeCurrentContendedMonitor(bb, os);
                    break;
                case JdwpConstants.CommandSet.ThreadReference.STOP:
                    executeStop(bb, os);
                    break;
                case JdwpConstants.CommandSet.ThreadReference.INTERRUPT:
                    executeInterrupt(bb, os);
                    break;
                case JdwpConstants.CommandSet.ThreadReference.SUSPEND_COUNT:
                    executeSuspendCount(bb, os);
                    break;
                default:
                    throw new NotImplementedException("Command " + command + " not found in Thread Reference Command Set.");
            }
        } catch (IOException ex) {
            throw new JdwpInternalErrorException(ex);
        }
        return false;
    }

    private void executeName(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        ThreadId tid = (ThreadId) idMan.readObjectId(bb);
        Thread thread = tid.getThread();
        JdwpString.writeString(os, thread.getName());
    }

    private void executeSuspend(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        ThreadId tid = (ThreadId) idMan.readObjectId(bb);
        Thread thread = tid.getThread();
        VMVirtualMachine.suspendThread(thread);
    }

    private void executeResume(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        ThreadId tid = (ThreadId) idMan.readObjectId(bb);
        Thread thread = tid.getThread();
        VMVirtualMachine.suspendThread(thread);
    }

    private void executeStatus(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        ThreadId tid = (ThreadId) idMan.readObjectId(bb);
        Thread thread = tid.getThread();
        int threadStatus = VMVirtualMachine.getThreadStatus(thread);
        int suspendStatus = JdwpConstants.SuspendStatus.SUSPENDED;
        os.writeInt(threadStatus);
        os.writeInt(suspendStatus);
    }

    private void executeThreadGroup(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        ThreadId tid = (ThreadId) idMan.readObjectId(bb);
        Thread thread = tid.getThread();
        ThreadGroup group = thread.getThreadGroup();
        ObjectId groupId = idMan.getObjectId(group);
        groupId.write(os);
    }

    private void executeFrames(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        ThreadId tid = (ThreadId) idMan.readObjectId(bb);
        Thread thread = tid.getThread();
        int startFrame = bb.getInt();
        int length = bb.getInt();
        ArrayList frames = VMVirtualMachine.getFrames(thread, startFrame, length);
        os.writeInt(frames.size());
        for (int i = 0; i < frames.size(); i++) {
            VMFrame frame = (VMFrame) frames.get(i);
            os.writeLong(frame.getId());
            Location loc = frame.getLocation();
            loc.write(os);
        }
    }

    private void executeFrameCount(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        ThreadId tid = (ThreadId) idMan.readObjectId(bb);
        Thread thread = tid.getThread();
        int frameCount = VMVirtualMachine.getFrameCount(thread);
        os.writeInt(frameCount);
    }

    private void executeOwnedMonitors(ByteBuffer bb, DataOutputStream os) throws JdwpException {
        throw new NotImplementedException("Command OwnedMonitors not implemented.");
    }

    private void executeCurrentContendedMonitor(ByteBuffer bb, DataOutputStream os) throws JdwpException {
        throw new NotImplementedException("Command CurrentContentedMonitors not implemented.");
    }

    private void executeStop(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        ThreadId tid = (ThreadId) idMan.readObjectId(bb);
        Thread thread = tid.getThread();
        ObjectId exception = idMan.readObjectId(bb);
        Throwable throwable = (Throwable) exception.getObject();
        thread.stop(throwable);
    }

    private void executeInterrupt(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        ThreadId tid = (ThreadId) idMan.readObjectId(bb);
        Thread thread = tid.getThread();
        thread.interrupt();
    }

    private void executeSuspendCount(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        ThreadId tid = (ThreadId) idMan.readObjectId(bb);
        Thread thread = tid.getThread();
        int suspendCount = VMVirtualMachine.getSuspendCount(thread);
        os.writeInt(suspendCount);
    }
}
