package fr.x9c.cadmium.primitives.unix;

import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;
import fr.x9c.cadmium.kernel.Block;
import fr.x9c.cadmium.kernel.Channel;
import fr.x9c.cadmium.kernel.CodeRunner;
import fr.x9c.cadmium.kernel.Fail;
import fr.x9c.cadmium.kernel.Primitive;
import fr.x9c.cadmium.kernel.PrimitiveProvider;
import fr.x9c.cadmium.kernel.Value;

/**
 * This class provides implementation for socket option-related primitives.
 *
 * @author <a href="mailto:cadmium@x9c.fr">Xavier Clerc</a>
 * @version 1.2
 * @since 1.0
 */
@PrimitiveProvider
public final class Sockopt {

    /** Identifier for boolean option 'SO_DEBUG'. */
    private static final int SO_DEBUG = 0;

    /** Identifier for boolean option 'SO_BROADCAST'. */
    private static final int SO_BROADCAST = 1;

    /** Identifier for boolean option 'SO_REUSEADDR'. */
    private static final int SO_REUSEADDR = 2;

    /** Identifier for boolean option 'SO_KEEPALIVE'. */
    private static final int SO_KEEPALIVE = 3;

    /** Identifier for boolean option 'SO_DONTROUTE'. */
    private static final int SO_DONTROUTE = 4;

    /** Identifier for boolean option 'SO_OOBINLINE'. */
    private static final int SO_OOBINLINE = 5;

    /** Identifier for boolean option 'SO_ACCEPTCONN'. */
    private static final int SO_ACCEPTCONN = 6;

    /** Identifier for boolean option 'TCP_NODELAY'. */
    private static final int TCP_NODELAY = 7;

    /** Identifier for boolean option 'IPV6_V6ONLY'. */
    private static final int IPV6_V6ONLY = 8;

    /** Identifier for integer option 'SO_SNDBUF'. */
    private static final int SO_SNDBUF = 0;

    /** Identifier for integer option 'SO_RCVBUF'. */
    private static final int SO_RCVBUF = 1;

    /** Identifier for integer option 'SO_ERROR'. */
    private static final int SO_ERROR = 2;

    /** Identifier for integer option 'SO_TYPE'. */
    private static final int SO_TYPE = 3;

    /** Identifier for integer option 'SO_RCVLOWAT'. */
    private static final int SO_RCVLOWAT = 4;

    /** Identifier for integer option 'SO_SNDLOWAT'. */
    private static final int SO_SNDLOWAT = 5;

    /** Identifier for option type 'TYPE_BOOL'. */
    private static final int OPTION_TYPE_BOOL = 0;

    /** Identifier for option type 'TYPE_INT'. */
    private static final int OPTION_TYPE_INT = 1;

    /** Identifier for option type 'TYPE_LINGER'. */
    private static final int OPTION_TYPE_LINGER = 2;

    /** Identifier for option type 'TYPE_TIMEVAL'. */
    private static final int OPTION_TYPE_TIMEVAL = 3;

    /** Identifier for option type 'TYPE_UNIX_ERROR'. */
    private static final int OPTION_TYPE_UNIX_ERROR = 4;

    /** Map from options types to names of related 'get' functions. */
    private static final String[] GET_FUNC_NAMES = { "getsockopt", "getsockopt_int", "getsockopt_optint", "getsockopt_float", "getsockopt_error" };

    /** Map from options types to names of related 'set' functions. */
    private static final String[] SET_FUNC_NAMES = { "setsockopt", "setsockopt_int", "setsockopt_optint", "setsockopt_float", "setsockopt_error" };

    /**
     * No instance of this class.
     */
    private Sockopt() {
    }

    /**
     * Returns the current value of socket option.
     * @param ctxt context
     * @param type type of socket option
     * @param socket socket to query
     * @param option option to get
     * @return current value of socket option
     * @throws Fail.Exception if the option is not supported
     */
    @Primitive
    public static Value unix_getsockopt(final CodeRunner ctxt, final Value type, final Value socket, final Value option) throws Fail.Exception {
        final String name = Sockopt.GET_FUNC_NAMES[type.asLong()];
        final Channel ch = ctxt.getContext().getChannel(socket.asLong());
        if (ch == null) {
            Unix.fail(ctxt, name, Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        final java.net.Socket s = ch.asSocket();
        final DatagramSocket ds = ch.asDatagramSocket();
        final ServerSocket srv = ch.asServerSocket();
        if ((s == null) && (ds == null) && (srv == null)) {
            Unix.fail(ctxt, name, Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        try {
            switch(type.asLong()) {
                case Sockopt.OPTION_TYPE_BOOL:
                    switch(option.asLong()) {
                        case Sockopt.SO_BROADCAST:
                            if (ds != null) {
                                return ds.getBroadcast() ? Value.TRUE : Value.FALSE;
                            } else {
                                Unix.fail(ctxt, "getsockopt", Unix.UNSUPPORTED_SOCKOPT_MSG);
                                return Value.UNIT;
                            }
                        case Sockopt.SO_REUSEADDR:
                            if (s != null) {
                                return s.getReuseAddress() ? Value.TRUE : Value.FALSE;
                            }
                            if (ds != null) {
                                return ds.getReuseAddress() ? Value.TRUE : Value.FALSE;
                            }
                            if (srv != null) {
                                return srv.getReuseAddress() ? Value.TRUE : Value.FALSE;
                            }
                            return Value.UNIT;
                        case Sockopt.SO_KEEPALIVE:
                            if (s != null) {
                                return s.getKeepAlive() ? Value.TRUE : Value.FALSE;
                            } else {
                                Unix.fail(ctxt, "getsockopt", Unix.UNSUPPORTED_SOCKOPT_MSG);
                                return Value.UNIT;
                            }
                        case Sockopt.SO_OOBINLINE:
                            if (s != null) {
                                return s.getOOBInline() ? Value.TRUE : Value.FALSE;
                            } else {
                                Unix.fail(ctxt, "getsockopt", Unix.UNSUPPORTED_SOCKOPT_MSG);
                                return Value.UNIT;
                            }
                        case Sockopt.TCP_NODELAY:
                            if (s != null) {
                                return s.getTcpNoDelay() ? Value.TRUE : Value.FALSE;
                            } else {
                                Unix.fail(ctxt, "getsockopt", Unix.UNSUPPORTED_SOCKOPT_MSG);
                                return Value.UNIT;
                            }
                        case Sockopt.SO_ACCEPTCONN:
                        case Sockopt.SO_DEBUG:
                        case Sockopt.SO_DONTROUTE:
                        case Sockopt.IPV6_V6ONLY:
                            Unix.fail(ctxt, "getsockopt", Unix.UNSUPPORTED_SOCKOPT_MSG);
                            return Value.UNIT;
                        default:
                            assert false : "invalid socket option";
                            return Value.UNIT;
                    }
                case Sockopt.OPTION_TYPE_INT:
                    switch(option.asLong()) {
                        case Sockopt.SO_SNDBUF:
                            if (s != null) {
                                return Value.createFromLong(s.getSendBufferSize());
                            } else if (ds != null) {
                                return Value.createFromLong(ds.getSendBufferSize());
                            } else {
                                Unix.fail(ctxt, "getsockopt_int", Unix.UNSUPPORTED_SOCKOPT_MSG);
                                return Value.UNIT;
                            }
                        case Sockopt.SO_RCVBUF:
                            if (s != null) {
                                return Value.createFromLong(s.getReceiveBufferSize());
                            } else if (srv != null) {
                                return Value.createFromLong(srv.getReceiveBufferSize());
                            } else {
                                return Value.createFromLong(ds.getReceiveBufferSize());
                            }
                        case Sockopt.SO_ERROR:
                        case Sockopt.SO_TYPE:
                        case Sockopt.SO_RCVLOWAT:
                        case Sockopt.SO_SNDLOWAT:
                            Unix.fail(ctxt, "getsockopt_int", Unix.UNSUPPORTED_SOCKOPT_MSG);
                            return Value.UNIT;
                        default:
                            assert false : "invalid socket option";
                            return Value.UNIT;
                    }
                case Sockopt.OPTION_TYPE_LINGER:
                    if (option != Value.ZERO) {
                        Unix.fail(ctxt, "getsockopt_optint", Unix.UNSUPPORTED_SOCKOPT_MSG);
                        return Value.UNIT;
                    }
                    if (s != null) {
                        final int val = s.getSoLinger();
                        if (val == -1) {
                            return Value.ZERO;
                        } else {
                            final Block res = Block.createBlock(0, Value.createFromLong(val));
                            return Value.createFromBlock(res);
                        }
                    } else {
                        Unix.fail(ctxt, "getsockopt_optint", Unix.UNSUPPORTED_SOCKOPT_MSG);
                        return Value.UNIT;
                    }
                case Sockopt.OPTION_TYPE_TIMEVAL:
                    Unix.fail(ctxt, "getsockopt_float", Unix.UNSUPPORTED_SOCKOPT_MSG);
                    return Value.UNIT;
                case Sockopt.OPTION_TYPE_UNIX_ERROR:
                    Unix.fail(ctxt, "getsockopt_error", Unix.UNSUPPORTED_SOCKOPT_MSG);
                    return Value.UNIT;
                default:
                    assert false : "invalid socket option";
                    return Value.UNIT;
            }
        } catch (final SocketException se) {
            Unix.fail(ctxt, name, se);
            return Value.UNIT;
        }
    }

    /**
     * Sets a socket option.
     * @param ctxt context
     * @param type type of socket option
     * @param socket socket to configure
     * @param option option to set
     * @param status new option value
     * @return <i>unit</i>
     * @throws Fail.Exception if the option is not supported
     */
    @Primitive
    public static Value unix_setsockopt(final CodeRunner ctxt, final Value type, final Value socket, final Value option, final Value status) throws Fail.Exception {
        final String name = Sockopt.SET_FUNC_NAMES[type.asLong()];
        final Channel ch = ctxt.getContext().getChannel(socket.asLong());
        if (ch == null) {
            Unix.fail(ctxt, "setsockopt_bool", Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        final java.net.Socket s = ch.asSocket();
        final DatagramSocket ds = ch.asDatagramSocket();
        final ServerSocket srv = ch.asServerSocket();
        if ((s == null) && (ds == null) && (srv == null)) {
            Unix.fail(ctxt, name, Unix.INVALID_DESCRIPTOR_MSG);
            return Value.UNIT;
        }
        try {
            switch(type.asLong()) {
                case Sockopt.OPTION_TYPE_BOOL:
                    switch(option.asLong()) {
                        case Sockopt.SO_BROADCAST:
                            if (ds != null) {
                                ds.setBroadcast(status == Value.TRUE);
                            } else {
                                Unix.fail(ctxt, "setsockopt_bool", Unix.UNSUPPORTED_SOCKOPT_MSG);
                            }
                            return Value.UNIT;
                        case Sockopt.SO_REUSEADDR:
                            if (s != null) {
                                s.setReuseAddress(status == Value.TRUE);
                            }
                            if (ds != null) {
                                ds.setReuseAddress(status == Value.TRUE);
                            }
                            if (srv != null) {
                                srv.setReuseAddress(status == Value.TRUE);
                            }
                            return Value.UNIT;
                        case Sockopt.SO_KEEPALIVE:
                            if (s != null) {
                                s.setKeepAlive(status == Value.TRUE);
                            } else {
                                Unix.fail(ctxt, "setsockopt_bool", Unix.UNSUPPORTED_SOCKOPT_MSG);
                            }
                            return Value.UNIT;
                        case Sockopt.SO_OOBINLINE:
                            if (s != null) {
                                s.setOOBInline(status == Value.TRUE);
                            } else {
                                Unix.fail(ctxt, "setsockopt_bool", Unix.UNSUPPORTED_SOCKOPT_MSG);
                            }
                            return Value.UNIT;
                        case Sockopt.TCP_NODELAY:
                            if (s != null) {
                                s.setTcpNoDelay(status == Value.TRUE);
                                return Value.UNIT;
                            } else {
                                Unix.fail(ctxt, "setsockopt_bool", Unix.UNSUPPORTED_SOCKOPT_MSG);
                                return Value.UNIT;
                            }
                        case Sockopt.SO_ACCEPTCONN:
                        case Sockopt.SO_DEBUG:
                        case Sockopt.SO_DONTROUTE:
                        case Sockopt.IPV6_V6ONLY:
                            Unix.fail(ctxt, "setsockopt_bool", Unix.UNSUPPORTED_SOCKOPT_MSG);
                            return Value.UNIT;
                        default:
                            assert false : "invalid socket option";
                            return Value.UNIT;
                    }
                case Sockopt.OPTION_TYPE_INT:
                    switch(option.asLong()) {
                        case Sockopt.SO_SNDBUF:
                            if (s != null) {
                                s.setSendBufferSize(status.asLong());
                            } else if (ds != null) {
                                ds.setSendBufferSize(status.asLong());
                            } else {
                                Unix.fail(ctxt, "setsockopt_int", Unix.UNSUPPORTED_SOCKOPT_MSG);
                                return Value.UNIT;
                            }
                            break;
                        case Sockopt.SO_RCVBUF:
                            if (s != null) {
                                s.setReceiveBufferSize(status.asLong());
                            } else if (srv != null) {
                                srv.setReceiveBufferSize(status.asLong());
                            } else {
                                ds.setReceiveBufferSize(status.asLong());
                            }
                            break;
                        case Sockopt.SO_ERROR:
                        case Sockopt.SO_TYPE:
                        case Sockopt.SO_RCVLOWAT:
                        case Sockopt.SO_SNDLOWAT:
                            Unix.fail(ctxt, "setsockopt_int", Unix.UNSUPPORTED_SOCKOPT_MSG);
                            break;
                        default:
                            assert false : "invalid socket option";
                    }
                    return Value.UNIT;
                case Sockopt.OPTION_TYPE_LINGER:
                    if (option != Value.ZERO) {
                        Unix.fail(ctxt, "setsockopt_optint", Unix.UNSUPPORTED_SOCKOPT_MSG);
                        return Value.UNIT;
                    }
                    if (s != null) {
                        if (status.isBlock()) {
                            s.setSoLinger(true, status.asBlock().get(0).asLong());
                        } else {
                            s.setSoLinger(false, 0);
                        }
                        return Value.UNIT;
                    } else {
                        Unix.fail(ctxt, "setsockopt_optint", Unix.INVALID_DESCRIPTOR_MSG);
                        return Value.UNIT;
                    }
                case Sockopt.OPTION_TYPE_TIMEVAL:
                    Unix.fail(ctxt, "setsockopt_float", Unix.UNSUPPORTED_SOCKOPT_MSG);
                    return Value.UNIT;
                case Sockopt.OPTION_TYPE_UNIX_ERROR:
                    Unix.fail(ctxt, "setsockopt_error", Unix.UNSUPPORTED_SOCKOPT_MSG);
                    return Value.UNIT;
                default:
                    assert false : "invalid socket option";
                    return Value.UNIT;
            }
        } catch (final SocketException se) {
            Unix.fail(ctxt, name, se);
            return Value.UNIT;
        }
    }
}
