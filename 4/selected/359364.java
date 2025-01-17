package org.omg.CosNaming.NamingContextPackage;

import gnu.CORBA.Minor;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.StructMember;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

/**
 * The helper operations for the {@link AlreadyBound} user exception.
 *
 * @author Audrius Meskauskas, Lithuania (AudriusA@Bioinformatics.org)
 */
public abstract class AlreadyBoundHelper {

    /**
   * The {@link AlreadyBound} repository id.
   */
    private static String _id = "IDL:omg.org/CosNaming/NamingContext/AlreadyBound:1.0";

    /**
   * The cached type code value.
   */
    private static TypeCode typeCode;

    /**
   * Extract the exception from the given {@link Any}.
   */
    public static AlreadyBound extract(Any a) {
        try {
            return ((AlreadyBoundHolder) a.extract_Streamable()).value;
        } catch (ClassCastException ex) {
            BAD_OPERATION bad = new BAD_OPERATION();
            bad.initCause(ex);
            bad.minor = Minor.Any;
            throw bad;
        }
    }

    /**
   * Return the exception repository id.
   */
    public static String id() {
        return _id;
    }

    /**
   * Insert the exception into the given {@link Any}.
   */
    public static void insert(Any a, AlreadyBound that) {
        a.insert_Streamable(new AlreadyBoundHolder(that));
    }

    /**
   * Read the exception from the given CDR stream.
   */
    public static AlreadyBound read(InputStream istream) {
        AlreadyBound value = new AlreadyBound();
        istream.read_string();
        return value;
    }

    /**
   * Create the type code for this exception.
   */
    public static TypeCode type() {
        if (typeCode == null) typeCode = ORB.init().create_struct_tc(id(), "AlreadyBound", new StructMember[0]);
        return typeCode;
    }

    /**
   * Write the exception to the CDR output stream.
   */
    public static void write(OutputStream ostream, AlreadyBound value) {
        ostream.write_string(id());
    }
}
