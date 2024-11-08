package org.omg.CosNaming;

import gnu.CORBA.Minor;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.ObjectHelper;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.InvokeHandler;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.ResponseHandler;
import org.omg.CosNaming.NamingContextPackage.AlreadyBound;
import org.omg.CosNaming.NamingContextPackage.AlreadyBoundHelper;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.CannotProceedHelper;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.InvalidNameHelper;
import org.omg.CosNaming.NamingContextPackage.NotEmpty;
import org.omg.CosNaming.NamingContextPackage.NotEmptyHelper;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.CosNaming.NamingContextPackage.NotFoundHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;

/**
 * The naming service servant. After implementing the abstract methods the
 * instance of this class can be connected to an ORB using POA.
 * 
 * @since 1.4 
 *
 * @author Audrius Meskauskas, Lithuania (AudriusA@Bioinformatics.org)
 */
public abstract class NamingContextPOA extends Servant implements NamingContextOperations, InvokeHandler {

    /** @inheritDoc */
    public String[] _all_interfaces(POA poa, byte[] object_ID) {
        return new String[] { NamingContextHelper.id() };
    }

    /**
   * The server calls this method after receiving the request message from
   * client. The implementation base calls one of its abstract methods to
   * perform the requested operation.
   *
   * @param method the method being invoked.
   * @param in the stream to read parameters from.
   * @param rh the handler to get a stream for writing a response.
   *
   * @return the stream, returned by the handler.
   */
    public OutputStream _invoke(String method, InputStream in, ResponseHandler rh) {
        OutputStream out = null;
        Integer call_method = (Integer) _NamingContextImplBase.methods.get(method);
        if (call_method == null) throw new BAD_OPERATION(Minor.Method, CompletionStatus.COMPLETED_MAYBE);
        switch(call_method.intValue()) {
            case 0:
                {
                    try {
                        NameComponent[] a_name = NameHelper.read(in);
                        org.omg.CORBA.Object an_object = ObjectHelper.read(in);
                        bind(a_name, an_object);
                        out = rh.createReply();
                    } catch (NotFound ex) {
                        out = rh.createExceptionReply();
                        NotFoundHelper.write(out, ex);
                    } catch (CannotProceed ex) {
                        out = rh.createExceptionReply();
                        CannotProceedHelper.write(out, ex);
                    } catch (InvalidName ex) {
                        out = rh.createExceptionReply();
                        InvalidNameHelper.write(out, ex);
                    } catch (AlreadyBound ex) {
                        out = rh.createExceptionReply();
                        AlreadyBoundHelper.write(out, ex);
                    }
                    break;
                }
            case 1:
                {
                    try {
                        NameComponent[] a_name = NameHelper.read(in);
                        org.omg.CORBA.Object an_object = ObjectHelper.read(in);
                        rebind(a_name, an_object);
                        out = rh.createReply();
                    } catch (NotFound ex) {
                        out = rh.createExceptionReply();
                        NotFoundHelper.write(out, ex);
                    } catch (CannotProceed ex) {
                        out = rh.createExceptionReply();
                        CannotProceedHelper.write(out, ex);
                    } catch (InvalidName ex) {
                        out = rh.createExceptionReply();
                        InvalidNameHelper.write(out, ex);
                    }
                    break;
                }
            case 2:
                {
                    try {
                        NameComponent[] a_name = NameHelper.read(in);
                        NamingContext a_context = NamingContextHelper.read(in);
                        bind_context(a_name, a_context);
                        out = rh.createReply();
                    } catch (NotFound ex) {
                        out = rh.createExceptionReply();
                        NotFoundHelper.write(out, ex);
                    } catch (CannotProceed ex) {
                        out = rh.createExceptionReply();
                        CannotProceedHelper.write(out, ex);
                    } catch (InvalidName ex) {
                        out = rh.createExceptionReply();
                        InvalidNameHelper.write(out, ex);
                    } catch (AlreadyBound ex) {
                        out = rh.createExceptionReply();
                        AlreadyBoundHelper.write(out, ex);
                    }
                    break;
                }
            case 3:
                {
                    try {
                        NameComponent[] a_name = NameHelper.read(in);
                        NamingContext a_context = NamingContextHelper.read(in);
                        rebind_context(a_name, a_context);
                        out = rh.createReply();
                    } catch (NotFound ex) {
                        out = rh.createExceptionReply();
                        NotFoundHelper.write(out, ex);
                    } catch (CannotProceed ex) {
                        out = rh.createExceptionReply();
                        CannotProceedHelper.write(out, ex);
                    } catch (InvalidName ex) {
                        out = rh.createExceptionReply();
                        InvalidNameHelper.write(out, ex);
                    }
                    break;
                }
            case 4:
                {
                    try {
                        NameComponent[] a_name = NameHelper.read(in);
                        org.omg.CORBA.Object __result = null;
                        __result = resolve(a_name);
                        out = rh.createReply();
                        ObjectHelper.write(out, __result);
                    } catch (NotFound ex) {
                        out = rh.createExceptionReply();
                        NotFoundHelper.write(out, ex);
                    } catch (CannotProceed ex) {
                        out = rh.createExceptionReply();
                        CannotProceedHelper.write(out, ex);
                    } catch (InvalidName ex) {
                        out = rh.createExceptionReply();
                        InvalidNameHelper.write(out, ex);
                    }
                    break;
                }
            case 5:
                {
                    try {
                        NameComponent[] a_name = NameHelper.read(in);
                        unbind(a_name);
                        out = rh.createReply();
                    } catch (NotFound ex) {
                        out = rh.createExceptionReply();
                        NotFoundHelper.write(out, ex);
                    } catch (CannotProceed ex) {
                        out = rh.createExceptionReply();
                        CannotProceedHelper.write(out, ex);
                    } catch (InvalidName ex) {
                        out = rh.createExceptionReply();
                        InvalidNameHelper.write(out, ex);
                    }
                    break;
                }
            case 6:
                {
                    NamingContext __result = null;
                    __result = new_context();
                    out = rh.createReply();
                    NamingContextHelper.write(out, __result);
                    break;
                }
            case 7:
                {
                    try {
                        NameComponent[] a_name = NameHelper.read(in);
                        NamingContext __result = null;
                        __result = bind_new_context(a_name);
                        out = rh.createReply();
                        NamingContextHelper.write(out, __result);
                    } catch (NotFound ex) {
                        out = rh.createExceptionReply();
                        NotFoundHelper.write(out, ex);
                    } catch (AlreadyBound ex) {
                        out = rh.createExceptionReply();
                        AlreadyBoundHelper.write(out, ex);
                    } catch (CannotProceed ex) {
                        out = rh.createExceptionReply();
                        CannotProceedHelper.write(out, ex);
                    } catch (InvalidName ex) {
                        out = rh.createExceptionReply();
                        InvalidNameHelper.write(out, ex);
                    }
                    break;
                }
            case 8:
                {
                    try {
                        destroy();
                        out = rh.createReply();
                    } catch (NotEmpty ex) {
                        out = rh.createExceptionReply();
                        NotEmptyHelper.write(out, ex);
                    }
                    break;
                }
            case 9:
                {
                    int amount = in.read_ulong();
                    BindingListHolder a_list = new BindingListHolder();
                    BindingIteratorHolder an_iter = new BindingIteratorHolder();
                    list(amount, a_list, an_iter);
                    out = rh.createReply();
                    BindingListHelper.write(out, a_list.value);
                    BindingIteratorHelper.write(out, an_iter.value);
                    break;
                }
            default:
                throw new BAD_OPERATION(0, CompletionStatus.COMPLETED_MAYBE);
        }
        return out;
    }

    /**
   * Get the CORBA object that delegates calls to this servant. The servant must
   * be already connected to an ORB.
   */
    public NamingContext _this() {
        return NamingContextHelper.narrow(super._this_object());
    }

    /**
   * Get the CORBA object that delegates calls to this servant. Connect to the
   * given ORB, if needed.
   */
    public NamingContext _this(org.omg.CORBA.ORB orb) {
        return NamingContextHelper.narrow(super._this_object(orb));
    }
}
