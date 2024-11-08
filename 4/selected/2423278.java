package org.omg.CosTradingRepos.ServiceTypeRepositoryPackage;

public final class AlreadyMaskedHelper {

    public static void insert(org.omg.CORBA.Any any, AlreadyMasked _ob_v) {
        org.omg.CORBA.portable.OutputStream out = any.create_output_stream();
        write(out, _ob_v);
        any.read_value(out.create_input_stream(), type());
    }

    public static AlreadyMasked extract(org.omg.CORBA.Any any) {
        if (any.type().equal(type())) return read(any.create_input_stream()); else throw new org.omg.CORBA.BAD_OPERATION();
    }

    private static org.omg.CORBA.TypeCode typeCode_;

    public static org.omg.CORBA.TypeCode type() {
        if (typeCode_ == null) {
            org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init();
            org.omg.CORBA.StructMember[] members = new org.omg.CORBA.StructMember[1];
            members[0] = new org.omg.CORBA.StructMember();
            members[0].name = "name";
            members[0].type = org.omg.CosTrading.ServiceTypeNameHelper.type();
            typeCode_ = orb.create_exception_tc(id(), "AlreadyMasked", members);
        }
        return typeCode_;
    }

    public static String id() {
        return "IDL:omg.org/CosTradingRepos/ServiceTypeRepository/AlreadyMasked:1.0";
    }

    public static AlreadyMasked read(org.omg.CORBA.portable.InputStream in) {
        if (!id().equals(in.read_string())) throw new org.omg.CORBA.MARSHAL();
        AlreadyMasked _ob_v = new AlreadyMasked();
        _ob_v.name = org.omg.CosTrading.ServiceTypeNameHelper.read(in);
        return _ob_v;
    }

    public static void write(org.omg.CORBA.portable.OutputStream out, AlreadyMasked _ob_v) {
        out.write_string(id());
        org.omg.CosTrading.ServiceTypeNameHelper.write(out, _ob_v.name);
    }
}
