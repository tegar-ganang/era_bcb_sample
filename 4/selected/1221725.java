package org.omg.CORBA.FT.CosNaming;

/**
 *	Generated from IDL definition of interface "NamingContextFT"
 *	@author JacORB IDL compiler 
 */
public abstract class NamingContextFTPOA extends org.omg.PortableServer.Servant implements org.omg.CORBA.portable.InvokeHandler, org.omg.CORBA.FT.CosNaming.NamingContextFTOperations {

    private static final java.util.Hashtable m_opsHash = new java.util.Hashtable();

    static {
        m_opsHash.put("set_update", new java.lang.Integer(0));
        m_opsHash.put("to_url", new java.lang.Integer(1));
        m_opsHash.put("list", new java.lang.Integer(2));
        m_opsHash.put("resolve", new java.lang.Integer(3));
        m_opsHash.put("rebind_context", new java.lang.Integer(4));
        m_opsHash.put("set_state", new java.lang.Integer(5));
        m_opsHash.put("is_alive", new java.lang.Integer(6));
        m_opsHash.put("new_context", new java.lang.Integer(7));
        m_opsHash.put("bind_context", new java.lang.Integer(8));
        m_opsHash.put("unbind", new java.lang.Integer(9));
        m_opsHash.put("bind", new java.lang.Integer(10));
        m_opsHash.put("get_update", new java.lang.Integer(11));
        m_opsHash.put("to_string", new java.lang.Integer(12));
        m_opsHash.put("resolve_str", new java.lang.Integer(13));
        m_opsHash.put("to_name", new java.lang.Integer(14));
        m_opsHash.put("bind_new_context", new java.lang.Integer(15));
        m_opsHash.put("destroy", new java.lang.Integer(16));
        m_opsHash.put("get_state", new java.lang.Integer(17));
        m_opsHash.put("rebind", new java.lang.Integer(18));
    }

    private String[] ids = { "IDL:omg.org/CORBA/FT/CosNaming/NamingContextFT:1.0", "IDL:omg.org/CORBA/FT/Checkpointable:1.0", "IDL:omg.org/CosNaming/NamingContext:1.0", "IDL:omg.org/CORBA/FT/PullMonitorable:1.0", "IDL:omg.org/CORBA/FT/Monitorable:1.0", "IDL:omg.org/CORBA/Object:1.0", "IDL:omg.org/CosNaming/NamingContextExt:1.0", "IDL:omg.org/CORBA/FT/Updateable:1.0" };

    public org.omg.CORBA.FT.CosNaming.NamingContextFT _this() {
        return org.omg.CORBA.FT.CosNaming.NamingContextFTHelper.narrow(_this_object());
    }

    public org.omg.CORBA.FT.CosNaming.NamingContextFT _this(org.omg.CORBA.ORB orb) {
        return org.omg.CORBA.FT.CosNaming.NamingContextFTHelper.narrow(_this_object(orb));
    }

    public org.omg.CORBA.portable.OutputStream _invoke(String method, org.omg.CORBA.portable.InputStream _input, org.omg.CORBA.portable.ResponseHandler handler) throws org.omg.CORBA.SystemException {
        org.omg.CORBA.portable.OutputStream _out = null;
        java.lang.Integer opsIndex = (java.lang.Integer) m_opsHash.get(method);
        if (null == opsIndex) throw new org.omg.CORBA.BAD_OPERATION(method + " not found");
        switch(opsIndex.intValue()) {
            case 0:
                {
                    try {
                        byte[] _arg0 = org.omg.CORBA.FT.StateHelper.read(_input);
                        _out = handler.createReply();
                        set_update(_arg0);
                    } catch (org.omg.CORBA.FT.InvalidUpdate _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.InvalidUpdateHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 1:
                {
                    try {
                        java.lang.String _arg0 = org.omg.CosNaming.NamingContextExtPackage.AddressHelper.read(_input);
                        java.lang.String _arg1 = org.omg.CosNaming.NamingContextExtPackage.StringNameHelper.read(_input);
                        _out = handler.createReply();
                        org.omg.CosNaming.NamingContextExtPackage.URLStringHelper.write(_out, to_url(_arg0, _arg1));
                    } catch (org.omg.CosNaming.NamingContextPackage.InvalidName _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.InvalidNameHelper.write(_out, _ex0);
                    } catch (org.omg.CosNaming.NamingContextExtPackage.InvalidAddress _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextExtPackage.InvalidAddressHelper.write(_out, _ex1);
                    }
                    break;
                }
            case 2:
                {
                    int _arg0 = _input.read_ulong();
                    org.omg.CosNaming.BindingListHolder _arg1 = new org.omg.CosNaming.BindingListHolder();
                    org.omg.CosNaming.BindingIteratorHolder _arg2 = new org.omg.CosNaming.BindingIteratorHolder();
                    _out = handler.createReply();
                    list(_arg0, _arg1, _arg2);
                    org.omg.CosNaming.BindingListHelper.write(_out, _arg1.value);
                    org.omg.CosNaming.BindingIteratorHelper.write(_out, _arg2.value);
                    break;
                }
            case 3:
                {
                    try {
                        org.omg.CosNaming.NameComponent[] _arg0 = org.omg.CosNaming.NameHelper.read(_input);
                        _out = handler.createReply();
                        _out.write_Object(resolve(_arg0));
                    } catch (org.omg.CosNaming.NamingContextPackage.NotFound _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.NotFoundHelper.write(_out, _ex0);
                    } catch (org.omg.CosNaming.NamingContextPackage.CannotProceed _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.CannotProceedHelper.write(_out, _ex1);
                    } catch (org.omg.CosNaming.NamingContextPackage.InvalidName _ex2) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.InvalidNameHelper.write(_out, _ex2);
                    }
                    break;
                }
            case 4:
                {
                    try {
                        org.omg.CosNaming.NameComponent[] _arg0 = org.omg.CosNaming.NameHelper.read(_input);
                        org.omg.CosNaming.NamingContext _arg1 = org.omg.CosNaming.NamingContextHelper.read(_input);
                        _out = handler.createReply();
                        rebind_context(_arg0, _arg1);
                    } catch (org.omg.CosNaming.NamingContextPackage.NotFound _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.NotFoundHelper.write(_out, _ex0);
                    } catch (org.omg.CosNaming.NamingContextPackage.CannotProceed _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.CannotProceedHelper.write(_out, _ex1);
                    } catch (org.omg.CosNaming.NamingContextPackage.InvalidName _ex2) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.InvalidNameHelper.write(_out, _ex2);
                    }
                    break;
                }
            case 5:
                {
                    try {
                        byte[] _arg0 = org.omg.CORBA.FT.StateHelper.read(_input);
                        _out = handler.createReply();
                        set_state(_arg0);
                    } catch (org.omg.CORBA.FT.InvalidState _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.InvalidStateHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 6:
                {
                    _out = handler.createReply();
                    _out.write_boolean(is_alive());
                    break;
                }
            case 7:
                {
                    _out = handler.createReply();
                    org.omg.CosNaming.NamingContextHelper.write(_out, new_context());
                    break;
                }
            case 8:
                {
                    try {
                        org.omg.CosNaming.NameComponent[] _arg0 = org.omg.CosNaming.NameHelper.read(_input);
                        org.omg.CosNaming.NamingContext _arg1 = org.omg.CosNaming.NamingContextHelper.read(_input);
                        _out = handler.createReply();
                        bind_context(_arg0, _arg1);
                    } catch (org.omg.CosNaming.NamingContextPackage.NotFound _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.NotFoundHelper.write(_out, _ex0);
                    } catch (org.omg.CosNaming.NamingContextPackage.AlreadyBound _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.AlreadyBoundHelper.write(_out, _ex1);
                    } catch (org.omg.CosNaming.NamingContextPackage.CannotProceed _ex2) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.CannotProceedHelper.write(_out, _ex2);
                    } catch (org.omg.CosNaming.NamingContextPackage.InvalidName _ex3) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.InvalidNameHelper.write(_out, _ex3);
                    }
                    break;
                }
            case 9:
                {
                    try {
                        org.omg.CosNaming.NameComponent[] _arg0 = org.omg.CosNaming.NameHelper.read(_input);
                        _out = handler.createReply();
                        unbind(_arg0);
                    } catch (org.omg.CosNaming.NamingContextPackage.NotFound _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.NotFoundHelper.write(_out, _ex0);
                    } catch (org.omg.CosNaming.NamingContextPackage.CannotProceed _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.CannotProceedHelper.write(_out, _ex1);
                    } catch (org.omg.CosNaming.NamingContextPackage.InvalidName _ex2) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.InvalidNameHelper.write(_out, _ex2);
                    }
                    break;
                }
            case 10:
                {
                    try {
                        org.omg.CosNaming.NameComponent[] _arg0 = org.omg.CosNaming.NameHelper.read(_input);
                        org.omg.CORBA.Object _arg1 = _input.read_Object();
                        _out = handler.createReply();
                        bind(_arg0, _arg1);
                    } catch (org.omg.CosNaming.NamingContextPackage.NotFound _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.NotFoundHelper.write(_out, _ex0);
                    } catch (org.omg.CosNaming.NamingContextPackage.AlreadyBound _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.AlreadyBoundHelper.write(_out, _ex1);
                    } catch (org.omg.CosNaming.NamingContextPackage.CannotProceed _ex2) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.CannotProceedHelper.write(_out, _ex2);
                    } catch (org.omg.CosNaming.NamingContextPackage.InvalidName _ex3) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.InvalidNameHelper.write(_out, _ex3);
                    }
                    break;
                }
            case 11:
                {
                    try {
                        _out = handler.createReply();
                        org.omg.CORBA.FT.StateHelper.write(_out, get_update());
                    } catch (org.omg.CORBA.FT.NoUpdateAvailable _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.NoUpdateAvailableHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 12:
                {
                    try {
                        org.omg.CosNaming.NameComponent[] _arg0 = org.omg.CosNaming.NameHelper.read(_input);
                        _out = handler.createReply();
                        org.omg.CosNaming.NamingContextExtPackage.StringNameHelper.write(_out, to_string(_arg0));
                    } catch (org.omg.CosNaming.NamingContextPackage.InvalidName _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.InvalidNameHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 13:
                {
                    try {
                        java.lang.String _arg0 = org.omg.CosNaming.NamingContextExtPackage.StringNameHelper.read(_input);
                        _out = handler.createReply();
                        _out.write_Object(resolve_str(_arg0));
                    } catch (org.omg.CosNaming.NamingContextPackage.NotFound _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.NotFoundHelper.write(_out, _ex0);
                    } catch (org.omg.CosNaming.NamingContextPackage.CannotProceed _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.CannotProceedHelper.write(_out, _ex1);
                    } catch (org.omg.CosNaming.NamingContextPackage.InvalidName _ex2) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.InvalidNameHelper.write(_out, _ex2);
                    }
                    break;
                }
            case 14:
                {
                    try {
                        java.lang.String _arg0 = org.omg.CosNaming.NamingContextExtPackage.StringNameHelper.read(_input);
                        _out = handler.createReply();
                        org.omg.CosNaming.NameHelper.write(_out, to_name(_arg0));
                    } catch (org.omg.CosNaming.NamingContextPackage.InvalidName _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.InvalidNameHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 15:
                {
                    try {
                        org.omg.CosNaming.NameComponent[] _arg0 = org.omg.CosNaming.NameHelper.read(_input);
                        _out = handler.createReply();
                        org.omg.CosNaming.NamingContextHelper.write(_out, bind_new_context(_arg0));
                    } catch (org.omg.CosNaming.NamingContextPackage.NotFound _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.NotFoundHelper.write(_out, _ex0);
                    } catch (org.omg.CosNaming.NamingContextPackage.CannotProceed _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.CannotProceedHelper.write(_out, _ex1);
                    } catch (org.omg.CosNaming.NamingContextPackage.AlreadyBound _ex2) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.AlreadyBoundHelper.write(_out, _ex2);
                    } catch (org.omg.CosNaming.NamingContextPackage.InvalidName _ex3) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.InvalidNameHelper.write(_out, _ex3);
                    }
                    break;
                }
            case 16:
                {
                    try {
                        _out = handler.createReply();
                        destroy();
                    } catch (org.omg.CosNaming.NamingContextPackage.NotEmpty _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.NotEmptyHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 17:
                {
                    try {
                        _out = handler.createReply();
                        org.omg.CORBA.FT.StateHelper.write(_out, get_state());
                    } catch (org.omg.CORBA.FT.NoStateAvailable _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.NoStateAvailableHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 18:
                {
                    try {
                        org.omg.CosNaming.NameComponent[] _arg0 = org.omg.CosNaming.NameHelper.read(_input);
                        org.omg.CORBA.Object _arg1 = _input.read_Object();
                        _out = handler.createReply();
                        rebind(_arg0, _arg1);
                    } catch (org.omg.CosNaming.NamingContextPackage.NotFound _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.NotFoundHelper.write(_out, _ex0);
                    } catch (org.omg.CosNaming.NamingContextPackage.CannotProceed _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.CannotProceedHelper.write(_out, _ex1);
                    } catch (org.omg.CosNaming.NamingContextPackage.InvalidName _ex2) {
                        _out = handler.createExceptionReply();
                        org.omg.CosNaming.NamingContextPackage.InvalidNameHelper.write(_out, _ex2);
                    }
                    break;
                }
        }
        return _out;
    }

    public String[] _all_interfaces(org.omg.PortableServer.POA poa, byte[] obj_id) {
        return ids;
    }
}
