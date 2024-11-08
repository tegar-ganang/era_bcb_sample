package org.omg.CosNaming.NamingContextPackage;

import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.Streamable;

/**
 * The holder for class {@link AlreadyBound} exception.
 *
 * @author Audrius Meskauskas, Lithuania (AudriusA@Bioinformatics.org)
 */
public final class AlreadyBoundHolder implements Streamable {

    /**
   * The stored value.
   */
    public AlreadyBound value;

    /**
   * Create the holder with unitialised value.
   */
    public AlreadyBoundHolder() {
    }

    /**
   * Create the holder, storing the given value.
   */
    public AlreadyBoundHolder(AlreadyBound initialValue) {
        value = initialValue;
    }

    /**
   * Fill in the stored value, reading it from the given CDR stream.
   */
    public void _read(InputStream in) {
        value = AlreadyBoundHelper.read(in);
    }

    /**
   * Get the type code of the {@link NotEmpty} exception.
   */
    public TypeCode _type() {
        return AlreadyBoundHelper.type();
    }

    /**
   * Write the stored value to the given CDR stream.
   */
    public void _write(OutputStream out) {
        AlreadyBoundHelper.write(out, value);
    }
}
