package net.sf.agentopia.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.agentopia.core.AgentopiaAgentMarker;
import net.sf.agentopia.core.AgentopiaConstants;

/**
 * Transmits class bytecode and object state.
 * <p>
 * This makes it ideal to transfer agents whose class code is unknown to the
 * other JVM.
 * <p>
 * Implicit rules:
 * <ul>
 * <li>You cannot load any class from a <code>net.sf.agentopia.*</code> package
 * or below.</li>
 * <li>File or network permissions are initially not given. You can influence
 * this.</li>
 * </ul>
 * 
 * @author <a href="mailto:kain@land-of-kain.de">Kai Ruhl</a>
 * @since 2008
 */
public class ClassedObjectTransmitter {

    /** No dynamic loading for classes from this package. */
    private static final String NO_DYNAMIC_READING_PACKAGE_START = "net.sf.agentopia.";

    /** Whether network access is allowed for new agents. */
    private boolean isAllowNet;

    /** Whether file access is allowed for new agents. */
    private boolean isAllowFile;

    /**
     * Creates a new classed object transmitter.
     * <p>
     * Initially, agents loaded by this transmitter have no rights at all.
     */
    public ClassedObjectTransmitter() {
    }

    /**
     * Creates any object from its byte array representation.
     * <p>
     * Any objects loaded together with their classes receive minimum security
     * permissions, i.e. they cannot access network, files, etc.
     * 
     * @param dataBytes The object in form of a byte array.
     * @param status A status receiver. May be null.
     * @return The object made from the bytes.
     * @throws IOException If transmission failed.
     */
    public Object turnClassedBytesToObject(byte[] dataBytes, ClassedObjectTransmitterStatus status) throws IOException {
        try {
            final ProtectionDomain protectionDomain = createProtectionDomain(isAllowNet, isAllowFile);
            ClassedObjectInputStream objectIn = new ClassedObjectInputStream(new ByteArrayInputStream(dataBytes), protectionDomain, status);
            Object dataObject = objectIn.readClassedObject();
            objectIn.close();
            return dataObject;
        } catch (IOException exc) {
            String msg = "Classed object rebuilding failed.";
            Logger.getLogger().warn(exc, msg);
            throw new IOException(msg);
        } catch (ClassNotFoundException exc) {
            String msg = "Classed object source class not found. Rebuilding failed.";
            Logger.getLogger().warn(exc, msg);
            throw new IOException(msg);
        } catch (Error err) {
            String msg = "Classed object rebuilding produced severe error.";
            Logger.getLogger().warn(err, msg);
            throw new IOException(msg);
        }
    }

    /**
     * Turns any object into a byte array so it can be written to any
     * destination.
     * 
     * @param dataObject The object to be torn into a byte array.
     * @param status A status receiver. May be null.
     * @return The object as byte array.
     * @throws IOException If transmission failed.
     */
    public byte[] turnClassedObjectToBytes(Object dataObject, ClassedObjectTransmitterStatus status) throws IOException {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ClassedObjectOutputStream objectOut = new ClassedObjectOutputStream(byteOut, status);
            objectOut.writeClassedObject(dataObject);
            byte[] dataBytes = byteOut.toByteArray();
            objectOut.close();
            return dataBytes;
        } catch (IOException exc) {
            String msg = "Classed object serialization failed: " + dataObject;
            Logger.getLogger().warn(exc, msg);
            throw new IOException(msg);
        }
    }

    /**
     * Transfers an agent from given stream.
     * <p>
     * Note: The stream remains open after the agent has been transferred.
     * 
     * @param dataIn The inputstream from where the agent is to be retrieved.
     * @return The freshly loaded agent.
     * @throws IOException If transfer failed.
     */
    public Object transferAgentFromStream(DataInputStream dataIn) throws IOException {
        int byteCount = dataIn.readInt();
        byte[] agentBytes = new byte[byteCount];
        dataIn.readFully(agentBytes);
        return turnClassedBytesToObject(agentBytes, null);
    }

    /**
     * Transfers an agent to another host on the network.
     * <p>
     * Note: The stream remains open after the agent has been transferred.
     * 
     * @param agent The agent to be transported.
     * @param dataOut The outputstream used for the transfer.
     * @throws IOException If transfer failed.
     */
    public void transferAgentToStream(Object agent, DataOutputStream dataOut) throws IOException {
        byte[] agentBytes = turnClassedObjectToBytes(agent, null);
        dataOut.writeInt(agentBytes.length);
        dataOut.write(agentBytes);
        dataOut.flush();
    }

    /**
     * Indicates whether network access is allowed for new agents.
     * 
     * @return Whether network access is allowed for new agents.
     */
    public boolean isAllowNet() {
        return isAllowNet;
    }

    /**
     * Sets whether network access is allowed for new agents.
     * 
     * @param isAllowNet Whether network access is allowed for new agents.
     */
    public void setAllowNet(boolean isAllowNet) {
        this.isAllowNet = isAllowNet;
    }

    /**
     * Indicates whether file access is allowed for new agents.
     * 
     * @return Whether file access is allowed for new agents.
     */
    public boolean isAllowFile() {
        return isAllowFile;
    }

    /**
     * Sets whether file access is allowed for new agents.
     * 
     * @param isAllowFile Whether file access is allowed for new agents.
     */
    public void setAllowFile(boolean isAllowFile) {
        this.isAllowFile = isAllowFile;
    }

    /**
     * Creates and returns a protection domain for class loading.
     * <p>
     * By default, the protection domain gets no permissions (agents shall use
     * the privileged methods provided in <code>AbstractAgent</code> instead).
     * 
     * @param isAllowNet Whether network I/O is allowed for new agents.
     * @param isAllowFile Whether file I/O is allowed for new agents.
     * @return A nothing-allowed protection domain.
     */
    private static ProtectionDomain createProtectionDomain(boolean isAllowNet, boolean isAllowFile) {
        CodeSource codeSrc = null;
        PermissionCollection permissionCollection = new Permissions();
        if (isAllowNet) {
            permissionCollection.add(new java.net.SocketPermission("*:1-", "accept,connect,listen,resolve"));
        }
        if (isAllowFile) {
            permissionCollection.add(new java.io.FilePermission("<<ALL FILES>>", "read,write,delete"));
        }
        ProtectionDomain protectionDomain = new ProtectionDomain(codeSrc, permissionCollection);
        return protectionDomain;
    }

    /**
     * Determines whether given class is an agent class.
     * 
     * @param objectClass The class of an object.
     * @return Whether the object can be an agent.
     */
    public static boolean isAgentClass(Class<?> objectClass) {
        if (!isSerializable(objectClass)) {
            return false;
        }
        final boolean isAgentAnnotation = objectClass.isAnnotationPresent(AgentopiaAgentMarker.class);
        return isAgentAnnotation;
    }

    /**
     * Determines whether given class is serializable.
     * 
     * @param objectClass The class to be checked.
     * @return Whether it is serializable.
     */
    private static boolean isSerializable(Class<?> objectClass) {
        for (Class<?> interfaceClass : objectClass.getInterfaces()) {
            if (interfaceClass.equals(Serializable.class)) {
                return true;
            }
        }
        final Class<?> superClass = objectClass.getSuperclass();
        return null == superClass ? false : isSerializable(superClass);
    }

    /**
     * Determines whether a class can be loaded dynamically (not forbidden).
     * 
     * @param streamClass The class to be loaded.
     * @return Whether it is forbidden to load that class dynamically.
     */
    private static boolean isObjectClassLoadingForbidden(ObjectStreamClass streamClass) {
        final String className = streamClass.getName();
        return className.toLowerCase().startsWith(NO_DYNAMIC_READING_PACKAGE_START);
    }

    /**
     * Indicates whether an object has been loaded by the classed object
     * transmitter.
     * <p>
     * This is generally true of the class loader is an instance of
     * <code>ClassedObjectTransmitter.ObjectClassLoader</code>.
     * 
     * @param object The object.
     * @return Whether the object has been loaded in a classed way.
     */
    public static boolean isObjectClassLoaded(Object object) {
        if (null == object) {
            return false;
        }
        ClassLoader classLoader = object.getClass().getClassLoader();
        if (null == classLoader) {
            return false;
        }
        return classLoader instanceof ObjectClassLoader;
    }

    /**
     * If the object has been loaded by the transmitter, then the class loader
     * has access to the class bytes. This method returns them.
     * <p>
     * If this is not possible, the method returns null.
     * 
     * @param object The object to be tested.
     * @return The objects class code, or null if loaded by system class loader.
     */
    public static byte[] getObjectClassBytes(Object object) {
        if (!isObjectClassLoaded(object)) {
            return null;
        }
        Class<?> objectClass = object.getClass();
        byte[] classBytes = ClassedObjectOutputStream.getClassLoaderBytes(objectClass);
        if (null == classBytes) {
            throw new IllegalStateException("Object has been loaded by transmitter, but bytes not available: " + object);
        }
        return classBytes;
    }

    /**
     * Creates an object class loader which is able to load the given class,
     * while retaining the class bytes.
     * 
     * @param objectClass The object class.
     * @return An object class loader able to instantiate the class.
     * @throws IOException If class bytes loading failed.
     */
    public static ObjectClassLoader createObjectClassLoader(Class<?> objectClass) throws IOException {
        final String className = objectClass.getName();
        final byte[] classBytes = FileFinder.findClassFile(objectClass);
        final ClassInfo classInfo = new ClassInfo(className, classBytes);
        final Map<String, ClassInfo> classInfoMap = new HashMap<String, ClassInfo>();
        classInfoMap.put(classInfo.classResource, classInfo);
        final ProtectionDomain protectionDomain = createProtectionDomain(false, false);
        final ObjectClassLoader classLoader = new ObjectClassLoader(classInfoMap, protectionDomain);
        try {
            Class<?> loadedClass = classLoader.loadClass(className);
            if (null == loadedClass) {
                throw new IllegalStateException("Unable to load class (" + objectClass.getSimpleName() + ").");
            }
        } catch (ClassNotFoundException exc) {
            throw new IllegalStateException(exc);
        }
        if (!classLoader.isDynamicallyLoadedClass(objectClass)) {
            throw new IllegalStateException("Object class (" + objectClass.getSimpleName() + ") was not dynamically loaded.");
        }
        return classLoader;
    }

    /**
     * Indicates the status of a transmit.
     * 
     * @author <a href="mailto:kain@land-of-kain.de">Kai Ruhl</a>
     * @since 2008
     */
    public static class ClassedObjectTransmitterStatus {

        /** The transmitted object, if any. */
        private List<Object> transmittedObjectList = new ArrayList<Object>(32);

        /** A list of transmitted classes (e.g. superclasses). */
        private List<Class<?>> transmittedClassList = new ArrayList<Class<?>>(32);

        /** Whether the "implicit contract" methods have been called. */
        public boolean isAdvancedSerialisationCalled = false;

        /**
         * A new, empty status object.
         */
        public ClassedObjectTransmitterStatus() {
        }

        /**
         * @return Whether an object was transmitted.
         */
        public boolean isObjectTransmitted() {
            return !transmittedObjectList.isEmpty();
        }

        /**
         * @return Whether a class was transmitted.
         */
        public boolean isClassTransmitted() {
            return !transmittedClassList.isEmpty();
        }

        /**
         * @return The number of transmitted classes.
         */
        public int getTransmittedObjectCount() {
            return transmittedObjectList.size();
        }

        /**
         * @return The number of transmitted classes.
         */
        public int getTransmittedClassCount() {
            return transmittedClassList.size();
        }

        /**
         * Always called when a serialized object is transmitted.
         * <p>
         * This is done once for the original object, then for all superclass
         * objects, then for all referenced objects.
         * 
         * @param object A class to be transmitted.
         */
        protected void addTransmittedObject(Object object) {
            transmittedObjectList.add(object);
        }

        /**
         * Always called when class bytecode is transmitted alongside the
         * serialized object.
         * <p>
         * This can be called several times e.g. if an agent has another agent
         * as superclass, or if an agent references another agent.
         * 
         * @param objectClass A class to be transmitted.
         */
        protected void addTransmittedClass(Class<?> objectClass) {
            transmittedClassList.add(objectClass);
        }

        /**
         * @return The list of transmitted objects.
         */
        public List<Object> getTransmittedObjects() {
            return Collections.unmodifiableList(transmittedObjectList);
        }

        /**
         * @return The list of transmitted classes.
         */
        public List<Class<?>> getTransmittedClasses() {
            return Collections.unmodifiableList(transmittedClassList);
        }
    }

    /**
     * Writes an object and its class through an output stream.
     * <p>
     * Similar to <code>ObjectOutputStream</code>, but transfers class code too.
     * 
     * @author <a href="mailto:kain@land-of-kain.de">Kai Ruhl</a>
     * @since 2001
     */
    private static class ClassedObjectOutputStream extends ObjectOutputStream {

        /** The status object. May be null. */
        private ClassedObjectTransmitterStatus status;

        /** Classes are sent only once, en block. */
        private boolean isClassesSent = false;

        /**
         * A new classed output stream.
         * 
         * @param out The parent output stream.
         * @param status A status receiver. May be null.
         * @throws IOException If IO access failed.
         */
        public ClassedObjectOutputStream(OutputStream out, ClassedObjectTransmitterStatus status) throws IOException {
            super(out);
            this.status = status;
        }

        /**
         * Retrieves the class bytes from the object class loader, if one was
         * used to load the class.
         * 
         * @param objectClass The agent class.
         * @return The class bytes, or null if not possible.
         */
        public static byte[] getClassLoaderBytes(Class<?> objectClass) {
            ClassLoader classLoader = objectClass.getClassLoader();
            if (null == classLoader || !(classLoader instanceof ObjectClassLoader)) {
                return null;
            }
            ObjectClassLoader objectClassLoader = (ObjectClassLoader) classLoader;
            byte[] classBytes = objectClassLoader.getClassBytes(objectClass);
            if (null == classBytes) {
                throw new IllegalStateException("Object class loader (" + objectClass.getSimpleName() + ") has no class bytes.");
            }
            return classBytes;
        }

        /**
         * Gets the class bytes, in any local way possible (including searching
         * the class path).
         * 
         * @param objectClass The object class.
         * @return The byte array of its class definition, or null if unable to
         *         find.
         */
        public static byte[] getClassBytesLocal(Class<?> objectClass) {
            final String className = objectClass.getName();
            byte[] classBytes = null;
            classBytes = getClassLoaderBytes(objectClass);
            if (null == classBytes) {
                try {
                    classBytes = FileFinder.findClassFile(objectClass);
                } catch (Exception exc) {
                    if (AgentopiaConstants.CLASS_LOADER_DEBUG) {
                        Logger.getLogger().warn("Unable to get input stream for class code (" + objectClass.getSimpleName() + "): " + exc);
                    }
                }
            }
            if (null == classBytes) {
                try {
                    classBytes = FileFinder.findClassFile(className);
                } catch (Exception exc) {
                    if (AgentopiaConstants.CLASS_LOADER_DEBUG) {
                        Logger.getLogger().warn("Unable to get classpath class code (" + objectClass.getSimpleName() + "): " + exc);
                    }
                }
            }
            return classBytes;
        }

        /**
         * Sends the class file into the input stream. The class array needed
         * for this may either be inside the class loader (inserted at the time
         * when it was loaded) or must be read from the file system (if this is
         * at the first host).
         * <p>
         * The corresponding method is
         * <code>ClassedObjectInputStream.resolveClass()</code>.
         * 
         * @param objectClass The class to be annotated.
         * @exception IOException Any exception thrown by the underlying
         *            OutputStream.
         */
        public void annotateClass(Class<?> objectClass) throws IOException {
            if (null != status) {
                status.isAdvancedSerialisationCalled = true;
            }
            if (isClassesSent || !isAgentClass(objectClass)) {
                writeInt(AgentopiaConstants.MESSAGE_CLASS_NOT_COMING);
                return;
            }
            final List<Class<?>> agentClassList = getAgentClassReferences(objectClass);
            List<byte[]> agentBytesList;
            try {
                agentBytesList = getAgentClassBytes(agentClassList);
            } catch (IllegalStateException exc) {
                writeInt(AgentopiaConstants.MESSAGE_CLASS_NOT_FOUND);
                if (AgentopiaConstants.CLASS_LOADER_DEBUG) {
                    Logger.getLogger().warn("Could not load class for agent at serialization.");
                }
                throw new IOException("Could not find class data for agent (" + objectClass.getSimpleName() + ").");
            }
            isClassesSent = true;
            final int agentClassCount = agentClassList.size();
            writeInt(AgentopiaConstants.MESSAGE_CLASS_COMING);
            writeInt(agentClassCount);
            for (int pos = 0; pos < agentClassCount; pos++) {
                final Class<?> agentClass = agentClassList.get(pos);
                final byte[] agentBytes = agentBytesList.get(pos);
                final byte[] classNameBytes = agentClass.getName().getBytes();
                writeInt(classNameBytes.length);
                write(classNameBytes);
                writeInt(agentBytes.length);
                write(agentBytes);
                if (AgentopiaConstants.CLASS_LOADER_DEBUG) {
                    final ClassLoader classLoader = agentClass.getClassLoader();
                    Logger.getLogger().info(Callers.getObjectName(classLoader) + ": Annotated class " + agentClass.getSimpleName() + " (" + agentBytes.length + " bytes).");
                }
                if (null != status) {
                    status.addTransmittedClass(agentClass);
                }
            }
            flush();
        }

        /**
         * Given a list of agent classes, returns all byte arrays for these
         * classes.
         * 
         * @param agentClassList The agent classes.
         * @return The byte arrays of the class definitions.
         */
        private List<byte[]> getAgentClassBytes(List<Class<?>> agentClassList) {
            List<byte[]> agentBytesList = new ArrayList<byte[]>(32);
            for (Class<?> agentClass : agentClassList) {
                byte[] agentBytes = getClassBytesLocal(agentClass);
                if (null == agentBytes) {
                    throw new IllegalStateException("Unable to retrieve class bytes for: " + agentClass.getSimpleName());
                }
                agentBytesList.add(agentBytes);
            }
            return agentBytesList;
        }

        /**
         * Given an agent class, returns all referenced classes that are also
         * agents.
         * 
         * @param objectClass The agent class.
         * @return All referenced classes (including the original agent class).
         */
        private List<Class<?>> getAgentClassReferences(Class<?> objectClass) {
            if (null == objectClass) {
                throw new IllegalArgumentException("Object class may not be null.");
            }
            if (!isAgentClass(objectClass)) {
                throw new IllegalArgumentException("Class is not an agent: " + objectClass.getSimpleName());
            }
            List<Class<?>> agentClasses = new ArrayList<Class<?>>(32);
            List<Class<?>> visitedClasses = new ArrayList<Class<?>>(32);
            traverseAgentClassReferences(objectClass, agentClasses, visitedClasses);
            return agentClasses;
        }

        /**
         * Recursive helper method: Given an agent class, returns all referenced
         * classes that are also agents.
         * 
         * @param objectClass The agent class.
         * @param agentClasses The agent classes found.
         * @param visitedClasses The visited classes.
         */
        private void traverseAgentClassReferences(Class<?> objectClass, List<Class<?>> agentClasses, List<Class<?>> visitedClasses) {
            if (null == objectClass) {
                throw new IllegalArgumentException("Object class may not be null.");
            }
            if (visitedClasses.contains(objectClass)) {
                return;
            }
            if (isAgentClass(objectClass)) {
                agentClasses.add(objectClass);
            }
            visitedClasses.add(objectClass);
            Class<?> superClass = objectClass.getSuperclass();
            if (null != superClass) {
                traverseAgentClassReferences(superClass, agentClasses, visitedClasses);
            }
            for (Class<?> refClass : objectClass.getClasses()) {
                traverseAgentClassReferences(refClass, agentClasses, visitedClasses);
            }
        }

        /**
         * Writes an object plus its class.
         * 
         * @param dataObject The object to be written.
         * @throws IOException If IO access failed.
         */
        public void writeClassedObject(Object dataObject) throws IOException {
            writeObject(dataObject);
            if (null != status) {
                status.addTransmittedObject(dataObject);
            }
            isClassesSent = false;
        }
    }

    /**
     * Reads an object and its class through an input stream.
     * <p>
     * Similar to <code>ObjectInputStream</code>, but transfers class code too.
     * 
     * @author <a href="mailto:kain@land-of-kain.de">Kai Ruhl</a>
     * @since 2001
     */
    private static class ClassedObjectInputStream extends ObjectInputStream {

        /** The transferred classes. */
        private Map<String, ClassInfo> classMap;

        /** Protection domain (security settings) of new agents. */
        private ProtectionDomain protectionDomain;

        /** The status object. May be null. */
        private ClassedObjectTransmitterStatus status;

        /**
         * A new classed object input stream.
         * 
         * @param in The parent input stream.
         * @param protectionDomain The security settings for loaded agents.
         * @param status A status receiver. May be null.
         * @throws IOException If IO access failed.
         * @throws StreamCorruptedException If a stream error occured.
         */
        public ClassedObjectInputStream(InputStream in, ProtectionDomain protectionDomain, ClassedObjectTransmitterStatus status) throws IOException, StreamCorruptedException {
            super(in);
            if (null == protectionDomain) {
                throw new IllegalArgumentException("Protection domain may not be null.");
            }
            this.status = status;
            this.protectionDomain = protectionDomain;
        }

        /**
         * Read an object and its class, so unknown objects can also be
         * deserialized.
         * 
         * @return The deserialized object.
         * @exception IOException If reading failed.
         * @throws ClassNotFoundException If the class could not be found.
         */
        public Object readClassedObject() throws IOException, ClassNotFoundException {
            Object dataObject = readObject();
            Class<?> objectClass = dataObject.getClass();
            final boolean isAgentClass = isAgentClass(objectClass);
            if (isAgentClass && null == objectClass.getClassLoader()) {
                Logger.getLogger().warn("Classed object \"" + dataObject.toString() + "\" has no class loader.");
            }
            if (null != status) {
                status.addTransmittedObject(dataObject);
            }
            classMap = null;
            return dataObject;
        }

        /**
         * Resolves a class from a stream class.
         * <p>
         * Overload to check agent stuff.
         * 
         * @param streamClass The class to be resolved.
         * @return The resolved class.
         * @exception IOException If reading failed.
         * @throws ClassNotFoundException If the class could not be found.
         */
        public Class<?> resolveClass(ObjectStreamClass streamClass) throws IOException, ClassNotFoundException {
            if (null != status) {
                status.isAdvancedSerialisationCalled = true;
            }
            final int firstFlag = readInt();
            if (AgentopiaConstants.MESSAGE_CLASS_NOT_FOUND == firstFlag) {
                throw new IOException("Could not load agent class: Other side could not find class data.");
            } else if (AgentopiaConstants.MESSAGE_CLASS_NOT_COMING == firstFlag) {
                return super.resolveClass(streamClass);
            } else if (AgentopiaConstants.MESSAGE_CLASS_COMING == firstFlag) {
                final String streamClassName = streamClass.getName();
                if (isObjectClassLoadingForbidden(streamClass)) {
                    throw new IOException("No dynamic loading from package agentopia allowed!");
                }
                classMap = new HashMap<String, ClassInfo>();
                final int agentClassCount = readInt();
                for (int pos = 0; pos < agentClassCount; pos++) {
                    final int classNameByteCount = readInt();
                    final byte[] classNameBytes = new byte[classNameByteCount];
                    readFully(classNameBytes, 0, classNameByteCount);
                    final String className = new String(classNameBytes);
                    final int byteCount = readInt();
                    byte[] classBytes = new byte[byteCount];
                    readFully(classBytes, 0, byteCount);
                    ClassInfo classInfo = new ClassInfo(className, classBytes);
                    classMap.put(classInfo.classResource, classInfo);
                }
                ObjectClassLoader classLoader = new ObjectClassLoader(classMap, protectionDomain);
                Class<?> agentClass = classLoader.loadClass(streamClassName);
                if (!(agentClass.getClassLoader() instanceof ObjectClassLoader)) {
                    throw new IllegalStateException("Agent class (" + agentClass.getSimpleName() + ") has no object class loader (" + Callers.getObjectName(agentClass.getClassLoader()) + ").");
                }
                if (null != status) {
                    for (Class<?> dynamicClass : classLoader.getDynamicallyLoadedClasses()) {
                        status.addTransmittedClass(dynamicClass);
                    }
                }
                return agentClass;
            } else {
                throw new IOException("Could not load agent class: Flag (" + firstFlag + ") unknown.");
            }
        }
    }

    /**
     * Capsules information about an agent class.
     * 
     * @author <a href="mailto:kain@land-of-kain.de">Kai Ruhl</a>
     * @since May 22, 2009
     */
    private static class ClassInfo {

        /** The name of the class. */
        public String className;

        /** The class resource name (i.e., the class file). */
        public String classResource;

        /** The bytes defining the class. */
        public byte[] classBytes;

        /** The number of bytes in the class definition array. */
        public int byteCount;

        /** The class object defining the class. */
        public Class<?> classObject;

        /**
         * @param className The name of the class.
         * @param classBytes The bytes defining the class.
         */
        public ClassInfo(String className, byte[] classBytes) {
            this.className = className;
            this.classResource = getClassResourceName(className);
            this.classBytes = classBytes;
            this.byteCount = classBytes.length;
        }

        /**
         * Returns the resource name (with path slashes "/" and ".class") of a
         * given class name.
         * 
         * @param className The class name.
         * @return The resource name of the class.
         */
        public static String getClassResourceName(String className) {
            String classResource = className.replace('.', '/') + ".class";
            return classResource;
        }

        /**
         * @return The class name (simple version without package).
         */
        public String getClassSimpleName() {
            if (null == classObject) {
                return "no class loaded";
            }
            final int dot = className.lastIndexOf('.');
            return -1 == dot ? className : className.substring(dot + 1);
        }
    }

    /**
     * A class loader for agents (or any other class with object state).
     * <p>
     * Used by the ClassedStreams. Each class loader may only load one agent.
     * 
     * @author <a href="mailto:kain@land-of-kain.de">Kai Ruhl</a>
     * @since 2001
     */
    private static class ObjectClassLoader extends ClassLoader {

        /** The class info of the loaded agent (including superclasses). */
        private Map<String, ClassInfo> classInfoMap;

        /** The dynamically loaded classes. */
        private List<Class<?>> dynamicClasses = new ArrayList<Class<?>>(32);

        /** Protection domain (security settings) of new agents. */
        private ProtectionDomain protectionDomain;

        /**
         * Defines an agent and remembers her class so it can be retrieved again
         * when its time to travel to the next host.
         * 
         * @param classInfoMap The class info objects.
         * @param protectionDomain The protection domain (security settings,
         *        including all allowed permissions) for new, dynamically loaded
         *        agents.
         */
        public ObjectClassLoader(Map<String, ClassInfo> classInfoMap, ProtectionDomain protectionDomain) {
            if (null == classInfoMap) {
                throw new IllegalArgumentException("Class info map may not be null.");
            }
            if (null == protectionDomain) {
                throw new IllegalArgumentException("Protection domain may not be null.");
            }
            this.classInfoMap = classInfoMap;
            this.protectionDomain = protectionDomain;
        }

        /**
         * Loads the class. Overridden method.
         * 
         * @see java.lang.ClassLoader#loadClass(java.lang.String)
         */
        @Override
        public Class<?> loadClass(String className) throws ClassNotFoundException {
            final String classResource = ClassInfo.getClassResourceName(className);
            final ClassInfo classInfo = classInfoMap.get(classResource);
            if (null != classInfo) {
                if (null == classInfo.classObject) {
                    classInfo.classObject = super.defineClass(className, classInfo.classBytes, 0, classInfo.classBytes.length, protectionDomain);
                    if (AgentopiaConstants.CLASS_LOADER_DEBUG) {
                        Logger.getLogger().info(Callers.getObjectName(this) + ": Resolving class " + classInfo.getClassSimpleName() + " (" + classInfo.byteCount + " byte).");
                    }
                    dynamicClasses.add(classInfo.classObject);
                }
                return classInfo.classObject;
            } else {
                return super.loadClass(className);
            }
        }

        /**
         * @return A list of classes that were loaded dynamically by this
         *         classloader.
         */
        public List<Class<?>> getDynamicallyLoadedClasses() {
            return Collections.unmodifiableList(dynamicClasses);
        }

        /**
         * @param objectClass The object class (prior to loading).
         * @return Whether another instance of the object class has been
         *         dynamically loaded.
         */
        public boolean isDynamicallyLoadedClass(Class<?> objectClass) {
            final String className = objectClass.getName();
            for (Class<?> dynamicClass : dynamicClasses) {
                if (dynamicClass.getName().equals(className)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns the class bytes of the given class.
         * <p>
         * Note: If an agent has multiple agent classes in its class hierarchy,
         * this method must be called once per class in the hierarchy.
         * 
         * @param objectClass The class.
         * @return The bytes used to load the class.
         */
        public byte[] getClassBytes(Class<?> objectClass) {
            final String classResource = ClassInfo.getClassResourceName(objectClass.getName());
            ClassInfo classInfo = classInfoMap.get(classResource);
            if (null != classInfo) {
                return classInfo.classBytes;
            }
            return null;
        }

        /**
         * Normally used to retrieve the class files, this must be overloaded
         * when using dynamically loaded classes.
         * 
         * @param resourceName The name of the class/resource.
         * @return The class raw bytes as inputstream.
         */
        public InputStream getResourceAsStream(String resourceName) {
            ClassInfo classInfo = classInfoMap.get(resourceName);
            if (null != classInfo) {
                return new ByteArrayInputStream(classInfo.classBytes);
            } else {
                return super.getResourceAsStream(resourceName);
            }
        }
    }
}
