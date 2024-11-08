package org.omg.CORBA.FT;

/**
 *	Generated from IDL definition of exception "MemberAlreadyPresent"
 *	@author JacORB IDL compiler 
 */
public final class MemberAlreadyPresentHelper {

    private static org.omg.CORBA.TypeCode _type = org.omg.CORBA.ORB.init().create_exception_tc(org.omg.CORBA.FT.MemberAlreadyPresentHelper.id(), "MemberAlreadyPresent", new org.omg.CORBA.StructMember[0]);

    public static void insert(final org.omg.CORBA.Any any, final org.omg.CORBA.FT.MemberAlreadyPresent s) {
        any.type(type());
        write(any.create_output_stream(), s);
    }

    public static org.omg.CORBA.FT.MemberAlreadyPresent extract(final org.omg.CORBA.Any any) {
        return read(any.create_input_stream());
    }

    public static org.omg.CORBA.TypeCode type() {
        return _type;
    }

    public static String id() {
        return "IDL:omg.org/CORBA/FT/MemberAlreadyPresent:1.0";
    }

    public static org.omg.CORBA.FT.MemberAlreadyPresent read(final org.omg.CORBA.portable.InputStream in) {
        org.omg.CORBA.FT.MemberAlreadyPresent result = new org.omg.CORBA.FT.MemberAlreadyPresent();
        if (!in.read_string().equals(id())) throw new org.omg.CORBA.MARSHAL("wrong id");
        return result;
    }

    public static void write(final org.omg.CORBA.portable.OutputStream out, final org.omg.CORBA.FT.MemberAlreadyPresent s) {
        out.write_string(id());
    }
}
