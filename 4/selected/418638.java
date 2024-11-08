package org.omg.PortableServer.POAPackage;

import gnu.CORBA.EmptyExceptionHolder;
import gnu.CORBA.Minor;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.StructMember;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

/**
* The helper operations for the exception {@link ServantAlreadyActive}.
*
* @author Audrius Meskauskas, Lithuania (AudriusA@Bioinformatics.org)
*/
public abstract class ServantAlreadyActiveHelper {

    /**
   * The cached typecode value, computed only once.
   */
    private static TypeCode typeCode;

    /**
   * Create the ServantAlreadyActive typecode (structure,
   * named "ServantAlreadyActive").
   */
    public static TypeCode type() {
        if (typeCode == null) {
            ORB orb = ORB.init();
            StructMember[] members = new StructMember[0];
            typeCode = orb.create_exception_tc(id(), "ServantAlreadyActive", members);
        }
        return typeCode;
    }

    /**
   * Insert the ServantAlreadyActive into the given Any.
   *
   * @param any the Any to insert into.
   * @param that the ServantAlreadyActive to insert.
   */
    public static void insert(Any any, ServantAlreadyActive that) {
        any.insert_Streamable(new EmptyExceptionHolder(that, type()));
    }

    /**
   * Extract the ServantAlreadyActive from given Any.
   *
   * @throws BAD_OPERATION if the passed Any does not contain ServantAlreadyActive.
   */
    public static ServantAlreadyActive extract(Any any) {
        try {
            EmptyExceptionHolder h = (EmptyExceptionHolder) any.extract_Streamable();
            return (ServantAlreadyActive) h.value;
        } catch (ClassCastException cex) {
            BAD_OPERATION bad = new BAD_OPERATION("ServantAlreadyActive expected");
            bad.minor = Minor.Any;
            bad.initCause(cex);
            throw bad;
        }
    }

    /**
   * Get the ServantAlreadyActive repository id.
   *
   * @return "IDL:omg.org/PortableServer/POA/ServantAlreadyActive:1.0", always.
   */
    public static String id() {
        return "IDL:omg.org/PortableServer/POA/ServantAlreadyActive:1.0";
    }

    /**
   * Read the exception from the CDR intput stream.
   *
   * @param input a org.omg.CORBA.portable stream to read from.
   */
    public static ServantAlreadyActive read(InputStream input) {
        String id = input.read_string();
        ServantAlreadyActive value = new ServantAlreadyActive(id);
        return value;
    }

    /**
   * Write the exception to the CDR output stream.
   *
   * @param output a org.omg.CORBA.portable stream stream to write into.
   * @param value a value to write.
   */
    public static void write(OutputStream output, ServantAlreadyActive value) {
        output.write_string(id());
    }
}
