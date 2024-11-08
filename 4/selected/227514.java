package gnu.classpath.jdwp.processor;

import gnu.classpath.jdwp.JdwpConstants;
import gnu.classpath.jdwp.exception.JdwpException;
import gnu.classpath.jdwp.exception.JdwpInternalErrorException;
import gnu.classpath.jdwp.exception.NotImplementedException;
import gnu.classpath.jdwp.id.ObjectId;
import gnu.classpath.jdwp.util.JdwpString;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A class representing the ThreadGroupReference Command Set.
 * 
 * @author Aaron Luchko <aluchko@redhat.com>
 */
public class ThreadGroupReferenceCommandSet extends CommandSet {

    public boolean runCommand(ByteBuffer bb, DataOutputStream os, byte command) throws JdwpException {
        try {
            switch(command) {
                case JdwpConstants.CommandSet.ThreadGroupReference.NAME:
                    executeName(bb, os);
                    break;
                case JdwpConstants.CommandSet.ThreadGroupReference.PARENT:
                    executeParent(bb, os);
                    break;
                case JdwpConstants.CommandSet.ThreadGroupReference.CHILDREN:
                    executeChildren(bb, os);
                    break;
                default:
                    throw new NotImplementedException("Command " + command + " not found in ThreadGroupReference Command Set.");
            }
        } catch (IOException ex) {
            throw new JdwpInternalErrorException(ex);
        }
        return false;
    }

    private void executeName(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        ObjectId oid = idMan.readObjectId(bb);
        ThreadGroup group = (ThreadGroup) oid.getObject();
        JdwpString.writeString(os, group.getName());
    }

    private void executeParent(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        ObjectId oid = idMan.readObjectId(bb);
        ThreadGroup group = (ThreadGroup) oid.getObject();
        ThreadGroup parent = group.getParent();
        ObjectId parentId = idMan.getObjectId(parent);
        parentId.write(os);
    }

    private void executeChildren(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        ObjectId oid = idMan.readObjectId(bb);
        ThreadGroup group = (ThreadGroup) oid.getObject();
        ThreadGroup jdwpGroup = Thread.currentThread().getThreadGroup();
        int numThreads = group.activeCount();
        Thread allThreads[] = new Thread[numThreads];
        group.enumerate(allThreads, false);
        numThreads = 0;
        for (int i = 0; i < allThreads.length; i++) {
            Thread thread = allThreads[i];
            if (thread == null) break;
            if (!thread.getThreadGroup().equals(jdwpGroup)) numThreads++;
        }
        os.writeInt(numThreads);
        for (int i = 0; i < allThreads.length; i++) {
            Thread thread = allThreads[i];
            if (thread == null) break;
            if (!thread.getThreadGroup().equals(jdwpGroup)) idMan.getObjectId(thread).write(os);
        }
        int numGroups = group.activeCount();
        ThreadGroup allGroups[] = new ThreadGroup[numGroups];
        group.enumerate(allGroups, false);
        numGroups = 0;
        for (int i = 0; i < allGroups.length; i++) {
            ThreadGroup tgroup = allGroups[i];
            if (tgroup == null) break;
            if (!tgroup.equals(jdwpGroup)) numGroups++;
        }
        os.writeInt(numGroups);
        for (int i = 0; i < allGroups.length; i++) {
            ThreadGroup tgroup = allGroups[i];
            if (tgroup == null) break;
            if (!tgroup.equals(jdwpGroup)) idMan.getObjectId(tgroup).write(os);
        }
    }
}
