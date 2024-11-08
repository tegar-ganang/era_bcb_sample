package org.omg.CosEventChannelAdmin;

public abstract class ProxyPushSupplierPOA extends org.omg.PortableServer.Servant implements org.omg.CORBA.portable.InvokeHandler, ProxyPushSupplierOperations {

    static final String[] _ob_ids_ = { "IDL:omg.org/CosEventChannelAdmin/ProxyPushSupplier:1.0", "IDL:omg.org/CosEventComm/PushSupplier:1.0" };

    public ProxyPushSupplier _this() {
        return ProxyPushSupplierHelper.narrow(super._this_object());
    }

    public ProxyPushSupplier _this(org.omg.CORBA.ORB orb) {
        return ProxyPushSupplierHelper.narrow(super._this_object(orb));
    }

    public String[] _all_interfaces(org.omg.PortableServer.POA poa, byte[] objectId) {
        return _ob_ids_;
    }

    public org.omg.CORBA.portable.OutputStream _invoke(String opName, org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        final String[] _ob_names = { "connect_push_consumer", "disconnect_push_supplier" };
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
                return _OB_op_connect_push_consumer(in, handler);
            case 1:
                return _OB_op_disconnect_push_supplier(in, handler);
        }
        throw new org.omg.CORBA.BAD_OPERATION();
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_connect_push_consumer(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        try {
            org.omg.CosEventComm.PushConsumer _ob_a0 = org.omg.CosEventComm.PushConsumerHelper.read(in);
            connect_push_consumer(_ob_a0);
            out = handler.createReply();
        } catch (AlreadyConnected _ob_ex) {
            out = handler.createExceptionReply();
            AlreadyConnectedHelper.write(out, _ob_ex);
        } catch (TypeError _ob_ex) {
            out = handler.createExceptionReply();
            TypeErrorHelper.write(out, _ob_ex);
        }
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_disconnect_push_supplier(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        disconnect_push_supplier();
        out = handler.createReply();
        return out;
    }
}
