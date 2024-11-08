package org.omg.CORBA.FT;

/**
 *	Generated from IDL definition of exception "MemberAlreadyPresent"
 *	@author JacORB IDL compiler 
 */
public final class MemberAlreadyPresentHolder implements org.omg.CORBA.portable.Streamable {

    public org.omg.CORBA.FT.MemberAlreadyPresent value;

    public MemberAlreadyPresentHolder() {
    }

    public MemberAlreadyPresentHolder(final org.omg.CORBA.FT.MemberAlreadyPresent initial) {
        value = initial;
    }

    public org.omg.CORBA.TypeCode _type() {
        return org.omg.CORBA.FT.MemberAlreadyPresentHelper.type();
    }

    public void _read(final org.omg.CORBA.portable.InputStream _in) {
        value = org.omg.CORBA.FT.MemberAlreadyPresentHelper.read(_in);
    }

    public void _write(final org.omg.CORBA.portable.OutputStream _out) {
        org.omg.CORBA.FT.MemberAlreadyPresentHelper.write(_out, value);
    }
}
