package org.omg.DynamicAny;

import gnu.CORBA.Minor;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Any;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.portable.OutputStream;

/**
 * The helper operations for {@link DynAnyFactory}. Following the 1.5 JDK
 * specifications, DynAnyFactory is always a local object, so the two methods of
 * this helper ({@link #read} and {@link #write} are not in use, always
 * throwing {@link MARSHAL}.
 * 
 * @specnote always throwing MARSHAL in read and write ensures compatibility
 * with other popular implementations like Sun's.
 * 
 * @author Audrius Meskauskas, Lithuania (AudriusA@Bioinformatics.org)
 */
public abstract class DynAnyFactoryHelper {

    /**
   * Cast the passed object into the DynAnyFactory. As DynAnyFactory is a local
   * object, the method just uses java final_type cast.
   * 
   * @param obj the object to narrow.
   * @return narrowed instance.
   * @throws BAD_PARAM if the passed object is not a DynAnyFactory.
   */
    public static DynAnyFactory narrow(org.omg.CORBA.Object obj) {
        try {
            return (DynAnyFactory) obj;
        } catch (ClassCastException cex) {
            throw new BAD_PARAM(obj.getClass().getName() + " is not a DynAnyFactory");
        }
    }

    /**
   * Narrow the given object to the DynAnyFactory. For the objects that are
   * always local, this operation does not differ from the ordinary
   * {@link #narrow} (ClassCastException will be thrown if narrowing something
   * different).
   * 
   * @param obj the object to cast.
   * 
   * @return the casted DynAnyFactory.
   * 
   * @since 1.5 
   * 
   * @see OMG issue 4158.
   */
    public static DynAnyFactory unchecked_narrow(org.omg.CORBA.Object obj) {
        return narrow(obj);
    }

    /**
   * Get the final_type code of the {@link DynAnyFactory}.
   */
    public static TypeCode type() {
        return ORB.init().create_interface_tc(id(), "DynAnyFactory");
    }

    /**
   * Insert the DynAnyFactory into the given Any.
   * 
   * @param any the Any to insert into.
   * 
   * @param that the DynAnyFactory to insert.
   */
    public static void insert(Any any, DynAnyFactory that) {
        any.insert_Object(that);
    }

    /**
   * Extract the DynAnyFactory from given Any.
   * 
   * @throws BAD_OPERATION if the passed Any does not contain DynAnyFactory.
   */
    public static DynAnyFactory extract(Any any) {
        return narrow(any.extract_Object());
    }

    /**
   * Get the DynAnyFactory repository id.
   * 
   * @return "IDL:omg.org/DynamicAny/DynAnyFactory:1.0", always.
   */
    public static String id() {
        return "IDL:omg.org/DynamicAny/DynAnyFactory:1.0";
    }

    /**
   * This should read DynAnyFactory from the CDR input stream, but (following
   * the JDK 1.5 API) it does not. The factory can only be obtained from the
   * ORB.
   * 
   * @param input a org.omg.CORBA.portable stream to read from.
   * 
   * @specenote Sun throws the same exception.
   * 
   * @throws MARSHAL always.
   */
    public static DynAnyFactory read(InputStream input) {
        throw new MARSHAL(not_applicable(id()));
    }

    /**
   * This should read DynAnyFactory from the CDR input stream, but (following
   * the JDK 1.5 API) it does not.
   * 
   * @param input a org.omg.CORBA.portable stream to read from.
   * 
   * @specenote Sun throws the same exception.
   * 
   * @throws MARSHAL always.
   */
    public static void write(OutputStream output, DynAnyFactory value) {
        throw new MARSHAL(not_applicable(id()));
    }

    /**
   * The package level method for throwing exception, explaining that the
   * operation is not applicable.
   * 
   * @param Id the Id for the typecode for that the operations was attempted to
   * perform.
   */
    static String not_applicable(String Id) {
        MARSHAL m = new MARSHAL("The read/write are not applicable for " + Id);
        m.minor = Minor.Inappropriate;
        throw m;
    }
}
