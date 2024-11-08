package org.omg.CosNotifyChannelAdmin;

public abstract class StructuredProxyPushConsumerPOA extends org.omg.PortableServer.Servant implements org.omg.CORBA.portable.InvokeHandler, StructuredProxyPushConsumerOperations {

    static final String[] _ob_ids_ = { "IDL:omg.org/CosNotifyChannelAdmin/StructuredProxyPushConsumer:1.0", "IDL:omg.org/CosNotifyChannelAdmin/ProxyConsumer:1.0", "IDL:omg.org/CosNotification/QoSAdmin:1.0", "IDL:omg.org/CosNotifyFilter/FilterAdmin:1.0", "IDL:omg.org/CosNotifyComm/StructuredPushConsumer:1.0", "IDL:omg.org/CosNotifyComm/NotifyPublish:1.0" };

    public StructuredProxyPushConsumer _this() {
        return StructuredProxyPushConsumerHelper.narrow(super._this_object());
    }

    public StructuredProxyPushConsumer _this(org.omg.CORBA.ORB orb) {
        return StructuredProxyPushConsumerHelper.narrow(super._this_object(orb));
    }

    public String[] _all_interfaces(org.omg.PortableServer.POA poa, byte[] objectId) {
        return _ob_ids_;
    }

    public org.omg.CORBA.portable.OutputStream _invoke(String opName, org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        final String[] _ob_names = { "_get_MyAdmin", "_get_MyType", "add_filter", "connect_structured_push_supplier", "disconnect_structured_push_consumer", "get_all_filters", "get_filter", "get_qos", "obtain_subscription_types", "offer_change", "push_structured_event", "remove_all_filters", "remove_filter", "set_qos", "validate_event_qos", "validate_qos" };
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
                return _OB_att_get_MyAdmin(in, handler);
            case 1:
                return _OB_att_get_MyType(in, handler);
            case 2:
                return _OB_op_add_filter(in, handler);
            case 3:
                return _OB_op_connect_structured_push_supplier(in, handler);
            case 4:
                return _OB_op_disconnect_structured_push_consumer(in, handler);
            case 5:
                return _OB_op_get_all_filters(in, handler);
            case 6:
                return _OB_op_get_filter(in, handler);
            case 7:
                return _OB_op_get_qos(in, handler);
            case 8:
                return _OB_op_obtain_subscription_types(in, handler);
            case 9:
                return _OB_op_offer_change(in, handler);
            case 10:
                return _OB_op_push_structured_event(in, handler);
            case 11:
                return _OB_op_remove_all_filters(in, handler);
            case 12:
                return _OB_op_remove_filter(in, handler);
            case 13:
                return _OB_op_set_qos(in, handler);
            case 14:
                return _OB_op_validate_event_qos(in, handler);
            case 15:
                return _OB_op_validate_qos(in, handler);
        }
        throw new org.omg.CORBA.BAD_OPERATION();
    }

    private org.omg.CORBA.portable.OutputStream _OB_att_get_MyAdmin(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        SupplierAdmin _ob_r = MyAdmin();
        org.omg.CORBA.portable.OutputStream out = handler.createReply();
        SupplierAdminHelper.write(out, _ob_r);
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_att_get_MyType(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        ProxyType _ob_r = MyType();
        org.omg.CORBA.portable.OutputStream out = handler.createReply();
        ProxyTypeHelper.write(out, _ob_r);
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_add_filter(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        org.omg.CosNotifyFilter.Filter _ob_a0 = org.omg.CosNotifyFilter.FilterHelper.read(in);
        int _ob_r = add_filter(_ob_a0);
        out = handler.createReply();
        org.omg.CosNotifyFilter.FilterIDHelper.write(out, _ob_r);
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_connect_structured_push_supplier(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        try {
            org.omg.CosNotifyComm.StructuredPushSupplier _ob_a0 = org.omg.CosNotifyComm.StructuredPushSupplierHelper.read(in);
            connect_structured_push_supplier(_ob_a0);
            out = handler.createReply();
        } catch (org.omg.CosEventChannelAdmin.AlreadyConnected _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosEventChannelAdmin.AlreadyConnectedHelper.write(out, _ob_ex);
        }
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_disconnect_structured_push_consumer(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        disconnect_structured_push_consumer();
        out = handler.createReply();
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_get_all_filters(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        int[] _ob_r = get_all_filters();
        out = handler.createReply();
        org.omg.CosNotifyFilter.FilterIDSeqHelper.write(out, _ob_r);
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_get_filter(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        try {
            int _ob_a0 = org.omg.CosNotifyFilter.FilterIDHelper.read(in);
            org.omg.CosNotifyFilter.Filter _ob_r = get_filter(_ob_a0);
            out = handler.createReply();
            org.omg.CosNotifyFilter.FilterHelper.write(out, _ob_r);
        } catch (org.omg.CosNotifyFilter.FilterNotFound _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosNotifyFilter.FilterNotFoundHelper.write(out, _ob_ex);
        }
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_get_qos(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        org.omg.CosNotification.Property[] _ob_r = get_qos();
        out = handler.createReply();
        org.omg.CosNotification.QoSPropertiesHelper.write(out, _ob_r);
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_obtain_subscription_types(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        ObtainInfoMode _ob_a0 = ObtainInfoModeHelper.read(in);
        org.omg.CosNotification.EventType[] _ob_r = obtain_subscription_types(_ob_a0);
        out = handler.createReply();
        org.omg.CosNotification.EventTypeSeqHelper.write(out, _ob_r);
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_offer_change(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        try {
            org.omg.CosNotification.EventType[] _ob_a0 = org.omg.CosNotification.EventTypeSeqHelper.read(in);
            org.omg.CosNotification.EventType[] _ob_a1 = org.omg.CosNotification.EventTypeSeqHelper.read(in);
            offer_change(_ob_a0, _ob_a1);
            out = handler.createReply();
        } catch (org.omg.CosNotifyComm.InvalidEventType _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosNotifyComm.InvalidEventTypeHelper.write(out, _ob_ex);
        }
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_push_structured_event(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        try {
            org.omg.CosNotification.StructuredEvent _ob_a0 = org.omg.CosNotification.StructuredEventHelper.read(in);
            push_structured_event(_ob_a0);
            out = handler.createReply();
        } catch (org.omg.CosEventComm.Disconnected _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosEventComm.DisconnectedHelper.write(out, _ob_ex);
        }
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_remove_all_filters(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        remove_all_filters();
        out = handler.createReply();
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_remove_filter(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        try {
            int _ob_a0 = org.omg.CosNotifyFilter.FilterIDHelper.read(in);
            remove_filter(_ob_a0);
            out = handler.createReply();
        } catch (org.omg.CosNotifyFilter.FilterNotFound _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosNotifyFilter.FilterNotFoundHelper.write(out, _ob_ex);
        }
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_set_qos(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        try {
            org.omg.CosNotification.Property[] _ob_a0 = org.omg.CosNotification.QoSPropertiesHelper.read(in);
            set_qos(_ob_a0);
            out = handler.createReply();
        } catch (org.omg.CosNotification.UnsupportedQoS _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosNotification.UnsupportedQoSHelper.write(out, _ob_ex);
        }
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_validate_event_qos(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        try {
            org.omg.CosNotification.Property[] _ob_a0 = org.omg.CosNotification.QoSPropertiesHelper.read(in);
            org.omg.CosNotification.NamedPropertyRangeSeqHolder _ob_ah1 = new org.omg.CosNotification.NamedPropertyRangeSeqHolder();
            validate_event_qos(_ob_a0, _ob_ah1);
            out = handler.createReply();
            org.omg.CosNotification.NamedPropertyRangeSeqHelper.write(out, _ob_ah1.value);
        } catch (org.omg.CosNotification.UnsupportedQoS _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosNotification.UnsupportedQoSHelper.write(out, _ob_ex);
        }
        return out;
    }

    private org.omg.CORBA.portable.OutputStream _OB_op_validate_qos(org.omg.CORBA.portable.InputStream in, org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream out = null;
        try {
            org.omg.CosNotification.Property[] _ob_a0 = org.omg.CosNotification.QoSPropertiesHelper.read(in);
            org.omg.CosNotification.NamedPropertyRangeSeqHolder _ob_ah1 = new org.omg.CosNotification.NamedPropertyRangeSeqHolder();
            validate_qos(_ob_a0, _ob_ah1);
            out = handler.createReply();
            org.omg.CosNotification.NamedPropertyRangeSeqHelper.write(out, _ob_ah1.value);
        } catch (org.omg.CosNotification.UnsupportedQoS _ob_ex) {
            out = handler.createExceptionReply();
            org.omg.CosNotification.UnsupportedQoSHelper.write(out, _ob_ex);
        }
        return out;
    }
}
