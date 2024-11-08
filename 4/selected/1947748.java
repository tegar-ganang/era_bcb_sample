package gnu.classpath.jdwp.processor;

import gnu.classpath.jdwp.JdwpConstants;
import gnu.classpath.jdwp.exception.InvalidObjectException;
import gnu.classpath.jdwp.exception.JdwpException;
import gnu.classpath.jdwp.exception.JdwpInternalErrorException;
import gnu.classpath.jdwp.exception.NotImplementedException;
import gnu.classpath.jdwp.id.ObjectId;
import gnu.classpath.jdwp.util.Value;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;

/**
 * A class representing the ArrayReference Command Set.
 * 
 * @author Aaron Luchko <aluchko@redhat.com>
 */
public class ArrayReferenceCommandSet extends CommandSet {

    public boolean runCommand(ByteBuffer bb, DataOutputStream os, byte command) throws JdwpException {
        try {
            switch(command) {
                case JdwpConstants.CommandSet.ArrayReference.LENGTH:
                    executeLength(bb, os);
                    break;
                case JdwpConstants.CommandSet.ArrayReference.GET_VALUES:
                    executeGetValues(bb, os);
                    break;
                case JdwpConstants.CommandSet.ArrayReference.SET_VALUES:
                    executeSetValues(bb, os);
                    break;
                default:
                    throw new NotImplementedException("Command " + command + " not found in Array Reference Command Set.");
            }
        } catch (IOException ex) {
            throw new JdwpInternalErrorException(ex);
        }
        return false;
    }

    private void executeLength(ByteBuffer bb, DataOutputStream os) throws InvalidObjectException, IOException {
        ObjectId oid = idMan.readObjectId(bb);
        Object array = oid.getObject();
        os.writeInt(Array.getLength(array));
    }

    private void executeGetValues(ByteBuffer bb, DataOutputStream os) throws JdwpException, IOException {
        ObjectId oid = idMan.readObjectId(bb);
        Object array = oid.getObject();
        int first = bb.getInt();
        int length = bb.getInt();
        Class clazz = array.getClass().getComponentType();
        if (clazz == byte.class) os.writeByte(JdwpConstants.Tag.BYTE); else if (clazz == char.class) os.writeByte(JdwpConstants.Tag.CHAR); else if (clazz == float.class) os.writeByte(JdwpConstants.Tag.FLOAT); else if (clazz == double.class) os.writeByte(JdwpConstants.Tag.DOUBLE); else if (clazz == int.class) os.writeByte(JdwpConstants.Tag.BYTE); else if (clazz == long.class) os.writeByte(JdwpConstants.Tag.LONG); else if (clazz == short.class) os.writeByte(JdwpConstants.Tag.SHORT); else if (clazz == void.class) os.writeByte(JdwpConstants.Tag.VOID); else if (clazz == boolean.class) os.writeByte(JdwpConstants.Tag.BOOLEAN); else if (clazz.isArray()) os.writeByte(JdwpConstants.Tag.ARRAY); else if (String.class.isAssignableFrom(clazz)) os.writeByte(JdwpConstants.Tag.STRING); else if (Thread.class.isAssignableFrom(clazz)) os.writeByte(JdwpConstants.Tag.THREAD); else if (ThreadGroup.class.isAssignableFrom(clazz)) os.writeByte(JdwpConstants.Tag.THREAD_GROUP); else if (ClassLoader.class.isAssignableFrom(clazz)) os.writeByte(JdwpConstants.Tag.CLASS_LOADER); else if (Class.class.isAssignableFrom(clazz)) os.writeByte(JdwpConstants.Tag.CLASS_OBJECT); else os.writeByte(JdwpConstants.Tag.OBJECT);
        for (int i = first; i < first + length; i++) {
            Object value = Array.get(array, i);
            if (clazz.isPrimitive()) Value.writeUntaggedValue(os, value); else Value.writeTaggedValue(os, value);
        }
    }

    private void executeSetValues(ByteBuffer bb, DataOutputStream os) throws IOException, JdwpException {
        ObjectId oid = idMan.readObjectId(bb);
        Object array = oid.getObject();
        int first = bb.getInt();
        int length = bb.getInt();
        Class type = array.getClass().getComponentType();
        for (int i = first; i < first + length; i++) {
            Object value = Value.getUntaggedObj(bb, type);
            Array.set(array, i, value);
        }
    }
}
