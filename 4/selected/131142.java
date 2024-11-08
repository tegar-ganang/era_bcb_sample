package org.omg.CORBA.FT;

/**
 *	Generated from IDL definition of interface "ObjectGroupManager"
 *	@author JacORB IDL compiler 
 */
public abstract class ObjectGroupManagerPOA extends org.omg.PortableServer.Servant implements org.omg.CORBA.portable.InvokeHandler, org.omg.CORBA.FT.ObjectGroupManagerOperations {

    private static final java.util.Hashtable m_opsHash = new java.util.Hashtable();

    static {
        m_opsHash.put("set_primary_member", new java.lang.Integer(0));
        m_opsHash.put("locations_of_members", new java.lang.Integer(1));
        m_opsHash.put("get_member_ref", new java.lang.Integer(2));
        m_opsHash.put("create_member", new java.lang.Integer(3));
        m_opsHash.put("get_object_group_id", new java.lang.Integer(4));
        m_opsHash.put("get_object_group_ref", new java.lang.Integer(5));
        m_opsHash.put("remove_member", new java.lang.Integer(6));
        m_opsHash.put("add_member", new java.lang.Integer(7));
    }

    private String[] ids = { "IDL:omg.org/CORBA/FT/ObjectGroupManager:1.0", "IDL:omg.org/CORBA/Object:1.0" };

    public org.omg.CORBA.FT.ObjectGroupManager _this() {
        return org.omg.CORBA.FT.ObjectGroupManagerHelper.narrow(_this_object());
    }

    public org.omg.CORBA.FT.ObjectGroupManager _this(org.omg.CORBA.ORB orb) {
        return org.omg.CORBA.FT.ObjectGroupManagerHelper.narrow(_this_object(orb));
    }

    public org.omg.CORBA.portable.OutputStream _invoke(String method, org.omg.CORBA.portable.InputStream _input, org.omg.CORBA.portable.ResponseHandler handler) throws org.omg.CORBA.SystemException {
        org.omg.CORBA.portable.OutputStream _out = null;
        java.lang.Integer opsIndex = (java.lang.Integer) m_opsHash.get(method);
        if (null == opsIndex) throw new org.omg.CORBA.BAD_OPERATION(method + " not found");
        switch(opsIndex.intValue()) {
            case 0:
                {
                    try {
                        org.omg.CORBA.Object _arg0 = _input.read_Object();
                        org.omg.CosNaming.NameComponent[] _arg1 = org.omg.CORBA.FT.LocationHelper.read(_input);
                        _out = handler.createReply();
                        _out.write_Object(set_primary_member(_arg0, _arg1));
                    } catch (org.omg.CORBA.FT.PrimaryNotSet _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.PrimaryNotSetHelper.write(_out, _ex0);
                    } catch (org.omg.CORBA.FT.MemberNotFound _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.MemberNotFoundHelper.write(_out, _ex1);
                    } catch (org.omg.CORBA.FT.BadReplicationStyle _ex2) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.BadReplicationStyleHelper.write(_out, _ex2);
                    } catch (org.omg.CORBA.FT.ObjectGroupNotFound _ex3) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.ObjectGroupNotFoundHelper.write(_out, _ex3);
                    }
                    break;
                }
            case 1:
                {
                    try {
                        org.omg.CORBA.Object _arg0 = _input.read_Object();
                        _out = handler.createReply();
                        org.omg.CORBA.FT.LocationsHelper.write(_out, locations_of_members(_arg0));
                    } catch (org.omg.CORBA.FT.ObjectGroupNotFound _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.ObjectGroupNotFoundHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 2:
                {
                    try {
                        org.omg.CORBA.Object _arg0 = _input.read_Object();
                        org.omg.CosNaming.NameComponent[] _arg1 = org.omg.CORBA.FT.LocationHelper.read(_input);
                        _out = handler.createReply();
                        _out.write_Object(get_member_ref(_arg0, _arg1));
                    } catch (org.omg.CORBA.FT.MemberNotFound _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.MemberNotFoundHelper.write(_out, _ex0);
                    } catch (org.omg.CORBA.FT.ObjectGroupNotFound _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.ObjectGroupNotFoundHelper.write(_out, _ex1);
                    }
                    break;
                }
            case 3:
                {
                    try {
                        org.omg.CORBA.Object _arg0 = _input.read_Object();
                        org.omg.CosNaming.NameComponent[] _arg1 = org.omg.CORBA.FT.LocationHelper.read(_input);
                        java.lang.String _arg2 = org.omg.CORBA.FT.TypeIdHelper.read(_input);
                        org.omg.CORBA.FT.Property[] _arg3 = org.omg.CORBA.FT.CriteriaHelper.read(_input);
                        _out = handler.createReply();
                        _out.write_Object(create_member(_arg0, _arg1, _arg2, _arg3));
                    } catch (org.omg.CORBA.FT.ObjectNotCreated _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.ObjectNotCreatedHelper.write(_out, _ex0);
                    } catch (org.omg.CORBA.FT.MemberAlreadyPresent _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.MemberAlreadyPresentHelper.write(_out, _ex1);
                    } catch (org.omg.CORBA.FT.CannotMeetCriteria _ex2) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.CannotMeetCriteriaHelper.write(_out, _ex2);
                    } catch (org.omg.CORBA.FT.ObjectGroupNotFound _ex3) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.ObjectGroupNotFoundHelper.write(_out, _ex3);
                    } catch (org.omg.CORBA.FT.InvalidCriteria _ex4) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.InvalidCriteriaHelper.write(_out, _ex4);
                    } catch (org.omg.CORBA.FT.NoFactory _ex5) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.NoFactoryHelper.write(_out, _ex5);
                    }
                    break;
                }
            case 4:
                {
                    try {
                        org.omg.CORBA.Object _arg0 = _input.read_Object();
                        _out = handler.createReply();
                        _out.write_ulonglong(get_object_group_id(_arg0));
                    } catch (org.omg.CORBA.FT.ObjectGroupNotFound _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.ObjectGroupNotFoundHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 5:
                {
                    try {
                        org.omg.CORBA.Object _arg0 = _input.read_Object();
                        _out = handler.createReply();
                        _out.write_Object(get_object_group_ref(_arg0));
                    } catch (org.omg.CORBA.FT.ObjectGroupNotFound _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.ObjectGroupNotFoundHelper.write(_out, _ex0);
                    }
                    break;
                }
            case 6:
                {
                    try {
                        org.omg.CORBA.Object _arg0 = _input.read_Object();
                        org.omg.CosNaming.NameComponent[] _arg1 = org.omg.CORBA.FT.LocationHelper.read(_input);
                        _out = handler.createReply();
                        _out.write_Object(remove_member(_arg0, _arg1));
                    } catch (org.omg.CORBA.FT.MemberNotFound _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.MemberNotFoundHelper.write(_out, _ex0);
                    } catch (org.omg.CORBA.FT.ObjectGroupNotFound _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.ObjectGroupNotFoundHelper.write(_out, _ex1);
                    }
                    break;
                }
            case 7:
                {
                    try {
                        org.omg.CORBA.Object _arg0 = _input.read_Object();
                        org.omg.CosNaming.NameComponent[] _arg1 = org.omg.CORBA.FT.LocationHelper.read(_input);
                        org.omg.CORBA.Object _arg2 = _input.read_Object();
                        _out = handler.createReply();
                        _out.write_Object(add_member(_arg0, _arg1, _arg2));
                    } catch (org.omg.CORBA.FT.ObjectNotAdded _ex0) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.ObjectNotAddedHelper.write(_out, _ex0);
                    } catch (org.omg.CORBA.INV_OBJREF _ex1) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.INV_OBJREFHelper.write(_out, _ex1);
                    } catch (org.omg.CORBA.FT.MemberAlreadyPresent _ex2) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.MemberAlreadyPresentHelper.write(_out, _ex2);
                    } catch (org.omg.CORBA.FT.ObjectGroupNotFound _ex3) {
                        _out = handler.createExceptionReply();
                        org.omg.CORBA.FT.ObjectGroupNotFoundHelper.write(_out, _ex3);
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
