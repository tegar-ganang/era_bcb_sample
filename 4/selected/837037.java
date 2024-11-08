package tcg.syscontrol.cos;

/**
 * Generated from IDL interface "ICosMonitoredThread".
 *
 * @author JacORB IDL compiler V 2.3-beta-2, 14-Oct-2006
 * @version generated at 27-Nov-2009 18:09:27
 */
public final class ICosMonitoredThreadHolder implements org.omg.CORBA.portable.Streamable {

    public ICosMonitoredThread value;

    public ICosMonitoredThreadHolder() {
    }

    public ICosMonitoredThreadHolder(final ICosMonitoredThread initial) {
        value = initial;
    }

    public org.omg.CORBA.TypeCode _type() {
        return ICosMonitoredThreadHelper.type();
    }

    public void _read(final org.omg.CORBA.portable.InputStream in) {
        value = ICosMonitoredThreadHelper.read(in);
    }

    public void _write(final org.omg.CORBA.portable.OutputStream _out) {
        ICosMonitoredThreadHelper.write(_out, value);
    }
}
