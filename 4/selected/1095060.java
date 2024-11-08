package org.omg.CosTypedEventChannelAdmin;

public abstract class TypedProxyPushConsumerPOA extends org.omg.PortableServer.Servant implements org.omg.CORBA.portable.InvokeHandler, TypedProxyPushConsumerOperations {

    static final String[] _ob_ids_ = { "IDL:omg.org/CosTypedEventChannelAdmin/TypedProxyPushConsumer:1.0", "IDL:omg.org/CosEventChannelAdmin/ProxyPushConsumer:1.0", "IDL:omg.org/CosEventComm/PushConsumer:1.0", "IDL:omg.org/CosTypedEventComm/TypedPushConsumer:1.0" };

    public TypedProxyPushConsumer _this() {
        return TypedProxyPushConsumerHelper.narrow(super._this_object());
    }

    public TypedProxyPushConsumer _this(org.omg.CORBA.ORB orb) {
        return TypedProxyPushConsumerHelper.narrow(super._this_object(orb));
    }

    public String[] _all_interfaces(org.omg.PortableServer.POA poa, byte[] objectId) {
        return _ob_ids_;
    }

    public org.omg.CORBA.portable.OutputStream _invoke(String opName, org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        final String[] _ob_names = { "connect_push_supplier", "disconnect_push_consumer", "get_typed_consumer", "push" };
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
                return _OB_op_connect_push_supplier(in, handler);
            case 1:
                return _OB_op_disconnect_push_consumer(in, handler);
            case 2:
                return _OB_op_get_typed_consumer(in, handler);
            case 3:
                return _OB_op_push(in, handler);
        }
        throw new org.omg.CORBA.BAD_OPERATION();
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_connect_push_supplier(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        try {
            org.omg.CosEventComm.PushSupplier _ob_a0 = org.omg.CosEventComm.PushSupplierHelper.read(in);
            connect_push_supplier(_ob_a0);
            out = handler.createReply();
        } catch (org.omg.CosEventChannelAdmin.AlreadyConnected _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosEventChannelAdmin.AlreadyConnectedHelper.write(out, _ob_ex);
        }
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_disconnect_push_consumer(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        disconnect_push_consumer();
        out = handler.createReply();
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_get_typed_consumer(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        org.omg.CORBA.Object _ob_r = get_typed_consumer();
        out = handler.createReply();
        out.write_Object(_ob_r);
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_push(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        try {
            org.omg.CORBA.Any _ob_a0 = in.read_any();
            push(_ob_a0);
            out = handler.createReply();
        } catch (org.omg.CosEventComm.Disconnected _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosEventComm.DisconnectedHelper.write(out, _ob_ex);
        }
        return out;
    }
}
