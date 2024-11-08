package com.sun.corba.se.PortableActivationIDL;

/** Interface used to support binding references in the bootstrap name
    * service.
    */
public abstract class _InitialNameServiceImplBase extends org.omg.CORBA.portable.ObjectImpl implements com.sun.corba.se.PortableActivationIDL.InitialNameService, org.omg.CORBA.portable.InvokeHandler {

    public _InitialNameServiceImplBase() {
    }

    private static java.util.Hashtable _methods = new java.util.Hashtable();

    static {
        _methods.put("bind", new java.lang.Integer(0));
    }

    public org.omg.CORBA.portable.OutputStream _invoke(String $method, org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler $rh) {
        org.omg.CORBA.portable.OutputStream out = null;
        java.lang.Integer __method = (java.lang.Integer) _methods.get($method);
        if (__method == null) throw new org.omg.CORBA.BAD_OPERATION(0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);
        switch(__method.intValue()) {
            case 0:
                {
                    try {
                        String name = in.read_string();
                        org.omg.CORBA.Object obj = org.omg.CORBA.ObjectHelper.read(in);
                        boolean isPersistant = in.read_boolean();
                        this.bind(name, obj, isPersistant);
                        out = $rh.createReply();
                    } catch (com.sun.corba.se.PortableActivationIDL.InitialNameServicePackage.NameAlreadyBound $ex) {
                        out = $rh.createExceptionReply();
                        com.sun.corba.se.PortableActivationIDL.InitialNameServicePackage.NameAlreadyBoundHelper.write(out, $ex);
                    }
                    break;
                }
            default:
                throw new org.omg.CORBA.BAD_OPERATION(0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);
        }
        return out;
    }

    private static String[] __ids = { "IDL:PortableActivationIDL/InitialNameService:1.0" };

    public String[] _ids() {
        return (String[]) __ids.clone();
    }
}
