package org.omg.CosTradingRepos;

public abstract class ServiceTypeRepositoryPOA extends org.omg.PortableServer.Servant implements org.omg.CORBA.portable.InvokeHandler, ServiceTypeRepositoryOperations {

    static final String[] _ob_ids_ = { "IDL:omg.org/CosTradingRepos/ServiceTypeRepository:1.0" };

    public ServiceTypeRepository _this() {
        return ServiceTypeRepositoryHelper.narrow(super._this_object());
    }

    public ServiceTypeRepository _this(org.omg.CORBA.ORB orb) {
        return ServiceTypeRepositoryHelper.narrow(super._this_object(orb));
    }

    public String[] _all_interfaces(org.omg.PortableServer.POA poa, byte[] objectId) {
        return _ob_ids_;
    }

    public org.omg.CORBA.portable.OutputStream _invoke(String opName, org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        final String[] _ob_names = { "_get_incarnation", "add_type", "describe_type", "fully_describe_type", "list_types", "mask_type", "remove_type", "unmask_type" };
        int _ob_left = 0;
        int _ob_right = _ob_names.length;
        int _ob_index = -1;
        while (_ob_left < _ob_right) {
            int _ob_m = (_ob_left + _ob_right) / 2;
            int _ob_res = _ob_names[_ob_m].compareTo(opName);
            if (_ob_res == 0) {
                _ob_index = _ob_m;
                break;
            } else if (_ob_res > 0) _ob_right = _ob_m; else _ob_left = _ob_m + 1;
        }
        switch(_ob_index) {
            case 0:
                return _OB_att_get_incarnation(in, handler);
            case 1:
                return _OB_op_add_type(in, handler);
            case 2:
                return _OB_op_describe_type(in, handler);
            case 3:
                return _OB_op_fully_describe_type(in, handler);
            case 4:
                return _OB_op_list_types(in, handler);
            case 5:
                return _OB_op_mask_type(in, handler);
            case 6:
                return _OB_op_remove_type(in, handler);
            case 7:
                return _OB_op_unmask_type(in, handler);
        }
        throw new org.omg.CORBA.BAD_OPERATION();
    }

    private org.omg.CORBA.portable.OutputStream _OB_att_get_incarnation(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.IncarnationNumber _ob_r = incarnation();
        org.omg.CORBA.portable.OutputStream out = handler.createReply();
        org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.IncarnationNumberHelper.write(out, _ob_r);
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_add_type(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        try {
            String _ob_a0 = org.omg.CosTrading.ServiceTypeNameHelper.read(in);
            String _ob_a1 = org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.IdentifierHelper.read(in);
            org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.PropStruct[] _ob_a2 = org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.PropStructSeqHelper.read(in);
            String[] _ob_a3 = org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.ServiceTypeNameSeqHelper.read(in);
            org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.IncarnationNumber _ob_r = add_type(_ob_a0, _ob_a1, _ob_a2, _ob_a3);
            out = handler.createReply();
            org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.IncarnationNumberHelper.write(out, _ob_r);
        } catch (org.omg.CosTrading.IllegalServiceType _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTrading.IllegalServiceTypeHelper.write(out, _ob_ex);
        } catch (org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.ServiceTypeExists _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.ServiceTypeExistsHelper.write(out, _ob_ex);
        } catch (org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.InterfaceTypeMismatch _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.InterfaceTypeMismatchHelper.write(out, _ob_ex);
        } catch (org.omg.CosTrading.IllegalPropertyName _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTrading.IllegalPropertyNameHelper.write(out, _ob_ex);
        } catch (org.omg.CosTrading.DuplicatePropertyName _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTrading.DuplicatePropertyNameHelper.write(out, _ob_ex);
        } catch (org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.ValueTypeRedefinition _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.ValueTypeRedefinitionHelper.write(out, _ob_ex);
        } catch (org.omg.CosTrading.UnknownServiceType _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTrading.UnknownServiceTypeHelper.write(out, _ob_ex);
        } catch (org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.DuplicateServiceTypeName _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.DuplicateServiceTypeNameHelper.write(out, _ob_ex);
        }
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_describe_type(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        try {
            String _ob_a0 = org.omg.CosTrading.ServiceTypeNameHelper.read(in);
            org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.TypeStruct _ob_r = describe_type(_ob_a0);
            out = handler.createReply();
            org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.TypeStructHelper.write(out, _ob_r);
        } catch (org.omg.CosTrading.IllegalServiceType _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTrading.IllegalServiceTypeHelper.write(out, _ob_ex);
        } catch (org.omg.CosTrading.UnknownServiceType _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTrading.UnknownServiceTypeHelper.write(out, _ob_ex);
        }
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_fully_describe_type(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        try {
            String _ob_a0 = org.omg.CosTrading.ServiceTypeNameHelper.read(in);
            org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.TypeStruct _ob_r = fully_describe_type(_ob_a0);
            out = handler.createReply();
            org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.TypeStructHelper.write(out, _ob_r);
        } catch (org.omg.CosTrading.IllegalServiceType _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTrading.IllegalServiceTypeHelper.write(out, _ob_ex);
        } catch (org.omg.CosTrading.UnknownServiceType _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTrading.UnknownServiceTypeHelper.write(out, _ob_ex);
        }
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_list_types(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.SpecifiedServiceTypes _ob_a0 = org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.SpecifiedServiceTypesHelper.read(in);
        String[] _ob_r = list_types(_ob_a0);
        out = handler.createReply();
        org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.ServiceTypeNameSeqHelper.write(out, _ob_r);
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_mask_type(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        try {
            String _ob_a0 = org.omg.CosTrading.ServiceTypeNameHelper.read(in);
            mask_type(_ob_a0);
            out = handler.createReply();
        } catch (org.omg.CosTrading.IllegalServiceType _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTrading.IllegalServiceTypeHelper.write(out, _ob_ex);
        } catch (org.omg.CosTrading.UnknownServiceType _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTrading.UnknownServiceTypeHelper.write(out, _ob_ex);
        } catch (org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.AlreadyMasked _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.AlreadyMaskedHelper.write(out, _ob_ex);
        }
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_remove_type(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        try {
            String _ob_a0 = org.omg.CosTrading.ServiceTypeNameHelper.read(in);
            remove_type(_ob_a0);
            out = handler.createReply();
        } catch (org.omg.CosTrading.IllegalServiceType _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTrading.IllegalServiceTypeHelper.write(out, _ob_ex);
        } catch (org.omg.CosTrading.UnknownServiceType _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTrading.UnknownServiceTypeHelper.write(out, _ob_ex);
        } catch (org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.HasSubTypes _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.HasSubTypesHelper.write(out, _ob_ex);
        }
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_unmask_type(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        try {
            String _ob_a0 = org.omg.CosTrading.ServiceTypeNameHelper.read(in);
            unmask_type(_ob_a0);
            out = handler.createReply();
        } catch (org.omg.CosTrading.IllegalServiceType _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTrading.IllegalServiceTypeHelper.write(out, _ob_ex);
        } catch (org.omg.CosTrading.UnknownServiceType _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTrading.UnknownServiceTypeHelper.write(out, _ob_ex);
        } catch (org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.NotMasked _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosTradingRepos.ServiceTypeRepositoryPackage.NotMaskedHelper.write(out, _ob_ex);
        }
        return out;
    }
}
