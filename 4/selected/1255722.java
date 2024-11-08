package org.omg.DsObservationAccess;

/**
 * Interface definition: EventConsumer.
 * 
 * @author OpenORB Compiler
 */
public abstract class EventConsumerPOA extends org.omg.PortableServer.Servant implements EventConsumerOperations, org.omg.CORBA.portable.InvokeHandler {

    public EventConsumer _this() {
        return EventConsumerHelper.narrow(_this_object());
    }

    public EventConsumer _this(org.omg.CORBA.ORB orb) {
        return EventConsumerHelper.narrow(_this_object(orb));
    }

    private static String[] _ids_list = { "IDL:omg.org/DsObservationAccess/EventConsumer:1.0", "IDL:omg.org/DsObservationAccess/AbstractManagedObject:1.0", "IDL:omg.org/CosEventComm/PushConsumer:1.0" };

    public String[] _all_interfaces(org.omg.PortableServer.POA poa, byte[] objectId) {
        return _ids_list;
    }

    public final org.omg.CORBA.portable.OutputStream _invoke(final String opName, final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
        if (opName.equals("_get_endpoint_id")) {
            return _invoke__get_endpoint_id(_is, handler);
        } else if (opName.equals("connect_push_supplier")) {
            return _invoke_connect_push_supplier(_is, handler);
        } else if (opName.equals("disconnect_push_consumer")) {
            return _invoke_disconnect_push_consumer(_is, handler);
        } else if (opName.equals("done")) {
            return _invoke_done(_is, handler);
        } else if (opName.equals("get_connected_supplier")) {
            return _invoke_get_connected_supplier(_is, handler);
        } else if (opName.equals("obtain_subscriptions")) {
            return _invoke_obtain_subscriptions(_is, handler);
        } else if (opName.equals("push")) {
            return _invoke_push(_is, handler);
        } else {
            throw new org.omg.CORBA.BAD_OPERATION(opName);
        }
    }

    private org.omg.CORBA.portable.OutputStream _invoke__get_endpoint_id(final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        int arg = endpoint_id();
        _output = handler.createReply();
        org.omg.DsObservationAccess.EndpointIdHelper.write(_output, arg);
        return _output;
    }

    private org.omg.CORBA.portable.OutputStream _invoke_obtain_subscriptions(final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        org.omg.DsObservationAccess.Subscription[] _arg_result = obtain_subscriptions();
        _output = handler.createReply();
        org.omg.DsObservationAccess.SubscriptionSeqHelper.write(_output, _arg_result);
        return _output;
    }

    private org.omg.CORBA.portable.OutputStream _invoke_connect_push_supplier(final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        org.omg.CosEventComm.PushSupplier arg0_in = org.omg.DsObservationAccess.PushSupplierHelper.read(_is);
        try {
            connect_push_supplier(arg0_in);
            _output = handler.createReply();
        } catch (org.omg.CosEventChannelAdmin.AlreadyConnected _exception) {
            _output = handler.createExceptionReply();
            org.omg.CosEventChannelAdmin.AlreadyConnectedHelper.write(_output, _exception);
        }
        return _output;
    }

    private org.omg.CORBA.portable.OutputStream _invoke_get_connected_supplier(final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        try {
            org.omg.CosEventComm.PushSupplier _arg_result = get_connected_supplier();
            _output = handler.createReply();
            org.omg.DsObservationAccess.PushSupplierHelper.write(_output, _arg_result);
        } catch (org.omg.CosEventComm.Disconnected _exception) {
            _output = handler.createExceptionReply();
            org.omg.CosEventComm.DisconnectedHelper.write(_output, _exception);
        }
        return _output;
    }

    private org.omg.CORBA.portable.OutputStream _invoke_done(final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        done();
        _output = handler.createReply();
        return _output;
    }

    private org.omg.CORBA.portable.OutputStream _invoke_push(final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        org.omg.CORBA.Any arg0_in = _is.read_any();
        try {
            push(arg0_in);
            _output = handler.createReply();
        } catch (org.omg.CosEventComm.Disconnected _exception) {
            _output = handler.createExceptionReply();
            org.omg.CosEventComm.DisconnectedHelper.write(_output, _exception);
        }
        return _output;
    }

    private org.omg.CORBA.portable.OutputStream _invoke_disconnect_push_consumer(final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        disconnect_push_consumer();
        _output = handler.createReply();
        return _output;
    }
}
