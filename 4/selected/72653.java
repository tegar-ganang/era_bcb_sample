package gnu.classpath.jdwp.processor;

import gnu.classpath.jdwp.JdwpConstants;
import gnu.classpath.jdwp.VMVirtualMachine;
import gnu.classpath.jdwp.exception.JdwpException;
import gnu.classpath.jdwp.exception.JdwpInternalErrorException;
import gnu.classpath.jdwp.exception.NotImplementedException;
import gnu.classpath.jdwp.id.ObjectId;
import gnu.classpath.jdwp.id.ReferenceTypeId;
import gnu.classpath.jdwp.util.JdwpString;
import gnu.classpath.jdwp.util.Signature;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

/**
 * A class representing the VirtualMachine Command Set.
 * 
 * @author Aaron Luchko <aluchko@redhat.com>
 */
public class VirtualMachineCommandSet extends CommandSet {

    public boolean runCommand(ByteBuffer bb, DataOutputStream os, byte command) throws JdwpException {
        boolean shutdown = false;
        try {
            switch(command) {
                case JdwpConstants.CommandSet.VirtualMachine.VERSION:
                    executeVersion(bb, os);
                    break;
                case JdwpConstants.CommandSet.VirtualMachine.CLASSES_BY_SIGNATURE:
                    executeClassesBySignature(bb, os);
                    break;
                case JdwpConstants.CommandSet.VirtualMachine.ALL_CLASSES:
                    executeAllClasses(bb, os);
                    break;
                case JdwpConstants.CommandSet.VirtualMachine.ALL_THREADS:
                    executeAllThreads(bb, os);
                    break;
                case JdwpConstants.CommandSet.VirtualMachine.TOP_LEVEL_THREAD_GROUPS:
                    executeTopLevelThreadGroups(bb, os);
                    break;
                case JdwpConstants.CommandSet.VirtualMachine.IDSIZES:
                    executeIDsizes(bb, os);
                    break;
                case JdwpConstants.CommandSet.VirtualMachine.DISPOSE:
                    shutdown = true;
                    executeDispose(bb, os);
                    break;
                case JdwpConstants.CommandSet.VirtualMachine.SUSPEND:
                    executeSuspend(bb, os);
                    break;
                case JdwpConstants.CommandSet.VirtualMachine.RESUME:
                    executeResume(bb, os);
                    break;
                case JdwpConstants.CommandSet.VirtualMachine.EXIT:
                    shutdown = true;
                    executeExit(bb, os);
                    break;
                case JdwpConstants.CommandSet.VirtualMachine.CREATE_STRING:
                    executeCreateString(bb, os);
                    break;
                case JdwpConstants.CommandSet.VirtualMachine.CAPABILITIES:
                    executeCapabilities(bb, os);
                    break;
                case JdwpConstants.CommandSet.VirtualMachine.CLASS_PATHS:
                    executeClassPaths(bb, os);
                    break;
                case JdwpConstants.CommandSet.VirtualMachine.DISPOSE_OBJECTS:
                    executeDisposeObjects(bb, os);
                    break;
                case JdwpConstants.CommandSet.VirtualMachine.HOLD_EVENTS:
                    executeHoldEvents(bb, os);
                    break;
                case JdwpConstants.CommandSet.VirtualMachine.RELEASE_EVENTS:
                    executeReleaseEvents(bb, os);
                    break;
                case JdwpConstants.CommandSet.VirtualMachine.CAPABILITIES_NEW:
                    executeCapabilitiesNew(bb, os);
                    break;
                case JdwpConstants.CommandSet.VirtualMachine.REDEFINE_CLASSES:
                    executeRedefineClasses(bb, os);
                    break;
                case JdwpConstants.CommandSet.VirtualMachine.SET_DEFAULT_STRATUM:
                    executeSetDefaultStratum(bb, os);
                    break;
                case JdwpConstants.CommandSet.VirtualMachine.ALL_CLASSES_WITH_GENERIC:
                    executeAllClassesWithGeneric(bb, os);
                    break;
                default:
                    throw new NotImplementedException("Command " + command + " not found in VirtualMachine Command Set.");
            }
        } catch (IOException ex) {
            throw new JdwpInternalErrorException(ex);
        }
        return shutdown;
    }

    private void executeVersion(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        Properties props = System.getProperties();
        int jdwpMajor = JdwpConstants.Version.MAJOR;
        int jdwpMinor = JdwpConstants.Version.MINOR;
        String description = "JDWP version " + jdwpMajor + "." + jdwpMinor + ", JVM version " + props.getProperty("java.vm.name") + " " + props.getProperty("java.vm.version") + " " + props.getProperty("java.version");
        String vmVersion = props.getProperty("java.version");
        String vmName = props.getProperty("java.vm.name");
        JdwpString.writeString(os, description);
        os.writeInt(jdwpMajor);
        os.writeInt(jdwpMinor);
        JdwpString.writeString(os, vmName);
        JdwpString.writeString(os, vmVersion);
    }

    private void executeClassesBySignature(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        String sig = JdwpString.readString(bb);
        ArrayList allMatchingClasses = new ArrayList();
        Iterator iter = VMVirtualMachine.getAllLoadedClasses();
        while (iter.hasNext()) {
            Class clazz = (Class) iter.next();
            String clazzSig = Signature.computeClassSignature(clazz);
            if (clazzSig.equals(sig)) allMatchingClasses.add(clazz);
        }
        os.writeInt(allMatchingClasses.size());
        for (int i = 0; i < allMatchingClasses.size(); i++) {
            Class clazz = (Class) allMatchingClasses.get(i);
            ReferenceTypeId id = idMan.getReferenceTypeId(clazz);
            id.writeTagged(os);
            int status = VMVirtualMachine.getClassStatus(clazz);
            os.writeInt(status);
        }
    }

    private void executeAllClasses(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        int classCount = VMVirtualMachine.getAllLoadedClassesCount();
        os.writeInt(classCount);
        Iterator iter = VMVirtualMachine.getAllLoadedClasses();
        int count = 0;
        while (iter.hasNext() && count++ < classCount) {
            Class clazz = (Class) iter.next();
            ReferenceTypeId id = idMan.getReferenceTypeId(clazz);
            id.writeTagged(os);
            String sig = Signature.computeClassSignature(clazz);
            JdwpString.writeString(os, sig);
            int status = VMVirtualMachine.getClassStatus(clazz);
            os.writeInt(status);
        }
    }

    private void executeAllThreads(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        ThreadGroup jdwpGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup root = getRootThreadGroup(jdwpGroup);
        int numThreads = root.activeCount();
        Thread allThreads[] = new Thread[numThreads];
        root.enumerate(allThreads);
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
    }

    private void executeTopLevelThreadGroups(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        ThreadGroup jdwpGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup root = getRootThreadGroup(jdwpGroup);
        os.writeInt(1);
        idMan.getObjectId(root);
    }

    private void executeDispose(ByteBuffer bb, DataOutputStream os) throws JdwpException {
        throw new NotImplementedException("Command VirtualMachine.Dispose not implemented");
    }

    private void executeIDsizes(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        ObjectId oid = new ObjectId();
        os.writeInt(oid.size());
        os.writeInt(oid.size());
        os.writeInt(oid.size());
        os.writeInt(new ReferenceTypeId((byte) 0x00).size());
        os.writeInt(oid.size());
    }

    private void executeSuspend(ByteBuffer bb, DataOutputStream os) throws JdwpException {
        VMVirtualMachine.suspendAllThreads();
    }

    private void executeResume(ByteBuffer bb, DataOutputStream os) throws JdwpException {
        VMVirtualMachine.resumeAllThreads();
    }

    private void executeExit(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        int exitCode = bb.getInt();
        System.exit(exitCode);
    }

    private void executeCreateString(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        String string = JdwpString.readString(bb);
        ObjectId stringId = idMan.getObjectId(string);
        stringId.disableCollection();
        stringId.write(os);
    }

    private void executeCapabilities(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        os.writeBoolean(false);
        os.writeBoolean(false);
        os.writeBoolean(false);
        os.writeBoolean(false);
        os.writeBoolean(false);
        os.writeBoolean(false);
        os.writeBoolean(false);
    }

    private void executeClassPaths(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        String baseDir = System.getProperty("user.dir");
        JdwpString.writeString(os, baseDir);
        String classPath = System.getProperty("java.class.path");
        String[] paths = classPath.split(":");
        os.writeInt(paths.length);
        for (int i = 0; i < paths.length; i++) JdwpString.writeString(os, paths[i]);
        String bootPath = System.getProperty("sun.boot.class.path");
        paths = bootPath.split(":");
        os.writeInt(paths.length);
        for (int i = 0; i < paths.length; i++) JdwpString.writeString(os, paths[i]);
    }

    private void executeDisposeObjects(ByteBuffer bb, DataOutputStream os) throws JdwpException {
    }

    private void executeHoldEvents(ByteBuffer bb, DataOutputStream os) throws JdwpException {
        throw new NotImplementedException("Command VirtualMachine.HoldEvents not implemented");
    }

    private void executeReleaseEvents(ByteBuffer bb, DataOutputStream os) throws JdwpException {
        throw new NotImplementedException("Command VirtualMachine.ReleaseEvents not implemented");
    }

    private void executeCapabilitiesNew(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        final int CAPABILITIES_NEW_SIZE = 32;
        os.writeBoolean(false);
        os.writeBoolean(false);
        os.writeBoolean(false);
        os.writeBoolean(false);
        os.writeBoolean(false);
        os.writeBoolean(false);
        os.writeBoolean(false);
        os.writeBoolean(false);
        os.writeBoolean(false);
        os.writeBoolean(false);
        os.writeBoolean(false);
        os.writeBoolean(false);
        os.writeBoolean(false);
        os.writeBoolean(false);
        os.writeBoolean(false);
        for (int i = 15; i < CAPABILITIES_NEW_SIZE; i++) os.writeBoolean(false);
    }

    private void executeRedefineClasses(ByteBuffer bb, DataOutputStream os) throws JdwpException {
        throw new NotImplementedException("Command VirtualMachine.RedefineClasses not implemented");
    }

    private void executeSetDefaultStratum(ByteBuffer bb, DataOutputStream os) throws JdwpException {
        throw new NotImplementedException("Command VirtualMachine.SetDefaultStratum not implemented");
    }

    private void executeAllClassesWithGeneric(ByteBuffer bb, DataOutputStream os) throws JdwpException {
        throw new NotImplementedException("Command VirtualMachine.AllClassesWithGeneric not implemented");
    }

    /**
   * Find the root ThreadGroup of this ThreadGroup
   */
    private ThreadGroup getRootThreadGroup(ThreadGroup group) {
        ThreadGroup parent = group.getParent();
        while (parent != null) {
            group = parent;
            parent = group.getParent();
        }
        return group;
    }
}
