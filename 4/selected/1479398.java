package org.omg.DsObservationAccess;

/**
 * Interface definition: EventSupplier.
 * 
 * @author OpenORB Compiler
 */
public abstract class EventSupplierPOA extends org.omg.PortableServer.Servant implements EventSupplierOperations, org.omg.CORBA.portable.InvokeHandler {

    public EventSupplier _this() {
        return EventSupplierHelper.narrow(_this_object());
    }

    public EventSupplier _this(org.omg.CORBA.ORB orb) {
        return EventSupplierHelper.narrow(_this_object(orb));
    }

    private static String[] _ids_list = { "IDL:omg.org/DsObservationAccess/EventSupplier:1.0", "IDL:omg.org/DsObservationAccess/AbstractManagedObject:1.0", "IDL:omg.org/CosEventComm/PushSupplier:1.0" };

    public String[] _all_interfaces(org.omg.PortableServer.POA poa, byte[] objectId) {
        return _ids_list;
    }

    private static final java.util.Map operationMap = new java.util.HashMap();

    static {
        operationMap.put("_get_endpoint_id", new Operation__get_endpoint_id());
        operationMap.put("connect_push_consumer", new Operation_connect_push_consumer());
        operationMap.put("describe_subscriptions", new Operation_describe_subscriptions());
        operationMap.put("disconnect_push_supplier", new Operation_disconnect_push_supplier());
        operationMap.put("done", new Operation_done());
        operationMap.put("generate_test_event", new Operation_generate_test_event());
        operationMap.put("get_connected_consumer", new Operation_get_connected_consumer());
        operationMap.put("obtain_offered_codes", new Operation_obtain_offered_codes());
        operationMap.put("subscribe", new Operation_subscribe());
    }

    public final org.omg.CORBA.portable.OutputStream _invoke(final String opName, final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
        final AbstractOperation operation = (AbstractOperation) operationMap.get(opName);
        if (null == operation) {
            throw new org.omg.CORBA.BAD_OPERATION(opName);
        }
        return operation.invoke(this, _is, handler);
    }

    private org.omg.CORBA.portable.OutputStream _invoke__get_endpoint_id(final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        int arg = endpoint_id();
        _output = handler.createReply();
        org.omg.DsObservationAccess.EndpointIdHelper.write(_output, arg);
        return _output;
    }

    private org.omg.CORBA.portable.OutputStream _invoke_obtain_offered_codes(final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        String[] _arg_result = obtain_offered_codes();
        _output = handler.createReply();
        org.omg.DsObservationAccess.QualifiedCodeStrSeqHelper.write(_output, _arg_result);
        return _output;
    }

    private org.omg.CORBA.portable.OutputStream _invoke_connect_push_consumer(final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        org.omg.CosEventComm.PushConsumer arg0_in = org.omg.DsObservationAccess.PushConsumerHelper.read(_is);
        try {
            connect_push_consumer(arg0_in);
            _output = handler.createReply();
        } catch (org.omg.CosEventChannelAdmin.AlreadyConnected _exception) {
            _output = handler.createExceptionReply();
            org.omg.CosEventChannelAdmin.AlreadyConnectedHelper.write(_output, _exception);
        }
        return _output;
    }

    private org.omg.CORBA.portable.OutputStream _invoke_get_connected_consumer(final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        try {
            org.omg.CosEventComm.PushConsumer _arg_result = get_connected_consumer();
            _output = handler.createReply();
            org.omg.DsObservationAccess.PushConsumerHelper.write(_output, _arg_result);
        } catch (org.omg.CosEventComm.Disconnected _exception) {
            _output = handler.createExceptionReply();
            org.omg.CosEventComm.DisconnectedHelper.write(_output, _exception);
        }
        return _output;
    }

    private org.omg.CORBA.portable.OutputStream _invoke_subscribe(final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        org.omg.DsObservationAccess.Subscription[] arg0_in = org.omg.DsObservationAccess.SubscriptionSeqHelper.read(_is);
        try {
            subscribe(arg0_in);
            _output = handler.createReply();
        } catch (org.omg.CosEventComm.Disconnected _exception) {
            _output = handler.createExceptionReply();
            org.omg.CosEventComm.DisconnectedHelper.write(_output, _exception);
        }
        return _output;
    }

    private org.omg.CORBA.portable.OutputStream _invoke_describe_subscriptions(final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        try {
            org.omg.DsObservationAccess.Subscription[] _arg_result = describe_subscriptions();
            _output = handler.createReply();
            org.omg.DsObservationAccess.SubscriptionSeqHelper.write(_output, _arg_result);
        } catch (org.omg.DsObservationAccess.NoSubscription _exception) {
            _output = handler.createExceptionReply();
            org.omg.DsObservationAccess.NoSubscriptionHelper.write(_output, _exception);
        }
        return _output;
    }

    private org.omg.CORBA.portable.OutputStream _invoke_generate_test_event(final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        int arg0_in = org.omg.DsObservationAccess.ClientCallIdHelper.read(_is);
        try {
            generate_test_event(arg0_in);
            _output = handler.createReply();
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

    private org.omg.CORBA.portable.OutputStream _invoke_disconnect_push_supplier(final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
        org.omg.CORBA.portable.OutputStream _output;
        disconnect_push_supplier();
        _output = handler.createReply();
        return _output;
    }

    private abstract static class AbstractOperation {

        protected abstract org.omg.CORBA.portable.OutputStream invoke(EventSupplierPOA target, org.omg.CORBA.portable.InputStream _is, org.omg.CORBA.portable.ResponseHandler handler);
    }

    private static final class Operation__get_endpoint_id extends AbstractOperation {

        protected org.omg.CORBA.portable.OutputStream invoke(final EventSupplierPOA target, final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
            return target._invoke__get_endpoint_id(_is, handler);
        }
    }

    private static final class Operation_obtain_offered_codes extends AbstractOperation {

        protected org.omg.CORBA.portable.OutputStream invoke(final EventSupplierPOA target, final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
            return target._invoke_obtain_offered_codes(_is, handler);
        }
    }

    private static final class Operation_connect_push_consumer extends AbstractOperation {

        protected org.omg.CORBA.portable.OutputStream invoke(final EventSupplierPOA target, final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
            return target._invoke_connect_push_consumer(_is, handler);
        }
    }

    private static final class Operation_get_connected_consumer extends AbstractOperation {

        protected org.omg.CORBA.portable.OutputStream invoke(final EventSupplierPOA target, final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
            return target._invoke_get_connected_consumer(_is, handler);
        }
    }

    private static final class Operation_subscribe extends AbstractOperation {

        protected org.omg.CORBA.portable.OutputStream invoke(final EventSupplierPOA target, final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
            return target._invoke_subscribe(_is, handler);
        }
    }

    private static final class Operation_describe_subscriptions extends AbstractOperation {

        protected org.omg.CORBA.portable.OutputStream invoke(final EventSupplierPOA target, final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
            return target._invoke_describe_subscriptions(_is, handler);
        }
    }

    private static final class Operation_generate_test_event extends AbstractOperation {

        protected org.omg.CORBA.portable.OutputStream invoke(final EventSupplierPOA target, final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
            return target._invoke_generate_test_event(_is, handler);
        }
    }

    private static final class Operation_done extends AbstractOperation {

        protected org.omg.CORBA.portable.OutputStream invoke(final EventSupplierPOA target, final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
            return target._invoke_done(_is, handler);
        }
    }

    private static final class Operation_disconnect_push_supplier extends AbstractOperation {

        protected org.omg.CORBA.portable.OutputStream invoke(final EventSupplierPOA target, final org.omg.CORBA.portable.InputStream _is, final org.omg.CORBA.portable.ResponseHandler handler) {
            return target._invoke_disconnect_push_supplier(_is, handler);
        }
    }
}
