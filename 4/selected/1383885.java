package org.omg.CosTradingRepos.ServiceTypeRepositoryPackage;

public final class AlreadyMaskedHolder implements org.omg.CORBA.portable.Streamable {

    public AlreadyMasked value;

    public AlreadyMaskedHolder() {
    }

    public AlreadyMaskedHolder(AlreadyMasked initial) {
        value = initial;
    }

    public void _read(org.omg.CORBA.portable.InputStream in) {
        value = AlreadyMaskedHelper.read(in);
    }

    public void _write(org.omg.CORBA.portable.OutputStream out) {
        AlreadyMaskedHelper.write(out, value);
    }

    public org.omg.CORBA.TypeCode _type() {
        return AlreadyMaskedHelper.type();
    }
}
