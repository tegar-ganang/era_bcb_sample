package obol.format;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.util.Iterator;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;
import java.security.PrivateKey;
import javax.crypto.SecretKey;
import obol.format.Format;
import obol.lang.Symbol;
import obol.lang.SymbolList;
import obol.lang.SymbolProperties;
import obol.lang.ObolException;
import obol.lang.ObolTypes;
import obol.tools.Debug;

public class PeerHandle {

    public static final String __me = "obol.format.PeerHandle";

    protected boolean inPoolMode = false;

    private static Debug log = Debug.getInstance(__me);

    protected Symbol symbol = null;

    protected Format currentFormat = null;

    protected String name = "<no name>";

    protected InputStream input = null;

    protected OutputStream output = null;

    protected InetAddress inetAddr = null;

    protected boolean isConnected = false;

    protected boolean isAccepting = false;

    protected int timeout = -1;

    private PeerHandle() {
    }

    private PeerHandle(Symbol sym, Format format) {
    }

    /** List of properties that are related to [self], and should be
     * dereferenced.  Only used when parsing [self].
     */
    public static final String[] DereferencableProperties = { SymbolProperties.DatagramBroadcast, SymbolProperties.DatagramTrafficClass, SymbolProperties.MaxDatagramSize, SymbolProperties.MulticastGroup, SymbolProperties.MulticastTimeToLive, SymbolProperties.PeerChannel, SymbolProperties.PeerChannelDeliverable, SymbolProperties.PeerPort };

    protected PeerHandle(Symbol sym, Format format, InputStream in, OutputStream out) {
        this.symbol = sym;
        this.currentFormat = format;
        if (null != sym) {
            this.name = sym.getName();
        }
        this.setInputStream(in);
        this.setOutputStream(out);
    }

    protected void setInputStream(InputStream in) {
        this.input = FormatBase.makeBuffered(in);
        this.isAccepting = (null != in);
    }

    protected void setOutputStream(OutputStream out) {
        this.output = FormatBase.makeBuffered(out);
        this.isConnected = (null != out);
    }

    public Symbol getSymbol() {
        return this.symbol;
    }

    /** Examines a symbol containing a Stream input/output type, and returns
     * an array of length two, consisting of input and output streams, as
     * appropriate.
     * @param sym Symbol of Stream type.
     * @return array of Input/Output streams Index 0 is always the
     * InputStream, or null if not provided.  Index 1 is always the
     * OutputStream, or null if not provided.
     * @throws ObolException if stream subtype couldn't be recognized, or
     * some other error.
     */
    protected static Object[] extractStreams(Symbol sym) throws ObolException {
        Object _value = sym.getValue();
        String _type = sym.getType();
        Object _in = null;
        Object _out = null;
        if (_value.getClass().isArray()) {
            Object[] _streams = (Object[]) _value;
            if (_type.equals(ObolTypes.StreamInOut)) {
                if (2 != _streams.length) {
                    throw new FormatException(__me + ".extractStreams(): Symbol \"" + sym.getName() + "\" has illegal stream \"" + _type + "\" value object array size (must be 2, not " + _streams.length + ")!");
                }
                _in = _streams[0];
                _out = _streams[1];
            } else if (_type.equals(ObolTypes.StreamOut) || _type.equals(ObolTypes.StreamOut)) {
                if (1 != _streams.length) {
                    throw new FormatException(__me + ".extractStreams(): Symbol \"" + sym.getName() + "\" has illegal stream \"" + _type + "\" value object array size (must be 1, not " + _streams.length + ")!");
                }
                if (_type.equals(ObolTypes.StreamOut)) {
                    _out = _streams[0];
                } else if (_type.equals(ObolTypes.StreamIn)) {
                    _in = _streams[0];
                }
            } else {
                throw new FormatException(__me + ".extractStreams(): Unknown stream subtype \"" + _type + "\" of symbol \"" + sym.getName() + "\"!");
            }
        } else {
            if (_type.equals(ObolTypes.StreamOut)) {
                _out = _value;
            } else if (_type.equals(ObolTypes.StreamIn)) {
                _in = _value;
            } else {
                throw new FormatException(__me + ".extractStreams(): Symbol \"" + sym.getName() + "\" has illegal stream type \"" + _type + "\" for non-array value object of class " + _value.getClass() + " - must be of either " + "InputStream or OutputStream subclass!!");
            }
        }
        if (null != _in) {
            if (false == (InputStream.class.isInstance(_in))) {
                throw new FormatException(__me + ".extractStreams(): Symbol \"" + sym.getName() + "\" of stream type \"" + _type + "\" should " + "have input stream of InputStream or subclass, " + "but got " + _in.getClass().getName());
            }
        }
        if (null != _out) {
            if (false == (OutputStream.class.isInstance(_out))) {
                throw new FormatException(__me + ".extractStreams(): Symbol \"" + sym.getName() + "\" of stream type \"" + _type + "\" should " + "have output stream of OutputStream or subclass, " + "but got " + _out.getClass().getName());
            }
        }
        Object[] _retval = new Object[2];
        _retval[0] = _in;
        _retval[1] = _out;
        return _retval;
    }

    /** Takes a filename derived from the symbol and verifies that the
     * given operations on the file are legal.  Examines the symbol for any
     * file-related properties (á la CommonLisp open).
     * <B>NB:</B> Only call with sym being one of the file types.
     * @param filename filename derived from symbol.
     * @param sym Symbol symbol representing file
     * @param write <tt>true</tt> if write-operations are to be performed on
     * the file, <tt>false</tt> if only read-operations will be done.
     * @return File object representing the file.
     */
    protected static File verifyFile(String filename, Symbol sym, boolean write) throws ObolException, IOException {
        File _f = new File(filename);
        boolean _exists = _f.exists();
        if (_exists) {
            if (_f.isDirectory()) {
                throw new ObolException(__me + ".verifyFile(): \"" + filename + "\" is a directory, not a file!");
            }
            if (false == _f.isFile()) {
                throw new ObolException(__me + ".verifyFile(): \"" + filename + "\" is not a file!");
            }
        }
        boolean _mustExist = sym.existProperty(SymbolProperties.FileMustExistKeyword);
        boolean _mustNotExist = sym.existProperty(SymbolProperties.FileMustNotExistKeyword);
        if (_mustExist && _mustNotExist) {
            throw new ObolException(__me + ".verifyFile(): logic error - " + "file \"" + filename + "\" cannot be expected to both " + "exist and not exist!");
        }
        if (_mustExist && (false == _exists)) {
            throw new ObolException(__me + ".verifyFile(): file \"" + filename + "\" does not exist but is expected to!");
        }
        if (_mustNotExist && _exists) {
            throw new ObolException(__me + ".verifyFile(): " + "file \"" + filename + "\" does exist when expected not to!");
        }
        boolean _create = sym.existProperty(SymbolProperties.FileCreateKeyword);
        boolean _overwrite = sym.existProperty(SymbolProperties.FileOverwriteKeyword);
        boolean _append = sym.existProperty(SymbolProperties.FileAppendKeyword);
        boolean _rename = sym.existProperty(SymbolProperties.FileRenameKeyword);
        if (_mustExist && _create) {
            throw new ObolException(__me + ".verifyFile(): logic error - " + "cannot simultaneously create and require existence " + "of file \"" + filename + "\"!");
        }
        if (_append && _overwrite) {
            throw new ObolException(__me + ".verifyFile(): logic error - " + "cannot both append to and overwrite file \"" + filename + "\"!");
        }
        if (_rename && (_append || _overwrite || _create)) {
            throw new ObolException(__me + ".verifyFile(): logic error - " + "cannot combine rename with append, overwrite or " + "create for file \"" + filename + "\"!");
        }
        if (_create && (_append || _overwrite || _rename)) {
            throw new ObolException(__me + ".verifyFile(): logic error - " + "cannot combine create with append, overwrite or " + "rename for file \"" + filename + "\"!");
        }
        if (write) {
            if (_exists) {
                if (_rename) {
                    File _renamedFile = null;
                    for (long _i = 0; ; _i++) {
                        if (_i > 1000) {
                            log.error("[NONFATAL] verifyFile(): number of " + "file-rename versions exceeds 1000 for " + "file \"" + filename + "\" -- " + "filesystem housekeeping seriously " + "recommended!");
                        }
                        _renamedFile = new File(filename + "." + _i);
                        if (false == _renamedFile.exists()) {
                            if (_f.renameTo(_renamedFile)) {
                                log.info("verifyFile() [write rename]: renamed " + "old file to \"" + _renamedFile + "\"");
                                File _newFile = new File(filename);
                                if (_newFile.createNewFile()) {
                                    log.info("verifyFile() [write rename]: " + "created new file \"" + filename + "\"");
                                    _f = _newFile;
                                } else {
                                    log.error("verifyFile() [write rename]: " + "failed created new file \"" + filename + "\", attempting rename rollback");
                                    if (_f.renameTo(new File(filename))) {
                                        log.error("verifyFile() [write rename]: " + "sucessful rename rollback to " + "old filename \"" + filename + "\"");
                                    }
                                    throw new ObolException(__me + "verifyFile() " + "[write, rename]: FAILED to rename-create " + "file \"" + filename + "\"");
                                }
                            }
                        }
                    }
                } else {
                    if (false == (_overwrite || _append)) {
                        throw new ObolException(__me + ".verifyFile() \"" + filename + "\" already exists, and no overwrite " + "or append keyword. ");
                    }
                }
            } else {
                if (_f.createNewFile()) {
                    log.info("verifyFile() [write]: " + "created new file \"" + filename + "\"");
                } else {
                    throw new ObolException(__me + "verifyFile() " + "[write]: FAILED to create new file \"" + filename + "\"");
                }
            }
            if (false == _f.canWrite()) {
                throw new ObolException(__me + ".verifyFile() [write mode]: " + "no write-permission to file \"" + filename + "\"!");
            }
        } else {
            if (false == _exists) {
                if (_create) {
                    if (_f.createNewFile()) {
                        log.info("verifyFile()[read, create]: created file " + "\"" + filename + "\"");
                    } else {
                        throw new ObolException(__me + "verifyFile() " + "[read, create]: FAILED to created file " + "\"" + filename + "\"");
                    }
                }
            }
            if (false == _f.canRead()) {
                throw new ObolException(__me + ".verifyFile() [read mode]: " + "no read-permission to file \"" + filename + "\"!");
            }
        }
        return _f;
    }

    /** Build a PeerHandle instance from the information in the provided symbol.
     * The symbol is not modifed by this method.
     * @param sym Symbol to use to build PeerHandle instance from.
     * @param format format associated with peerhandle
     */
    public static PeerHandle getInstance(Symbol sym, Format format) throws ObolException, IOException {
        Object _value = sym.getValue();
        String _type = sym.getType();
        log.debug("getInstance(): sym=" + sym.getName() + ", type=" + _type + ", value=" + _value);
        PeerHandle _peer = null;
        if (_type.startsWith(ObolTypes.File)) {
            if (null == _value) {
                throw new ObolException(__me + ".getInstance(): symbol \"" + sym.getName() + "\" of type " + _type + " cannot have null value!");
            }
            InputStream _in = null;
            OutputStream _out = null;
            String _fnamebase = (String) _value;
            File _outFile = null;
            File _inFile = null;
            if (_type.equals(ObolTypes.FileOut)) {
                _outFile = verifyFile(_fnamebase, sym, true);
            } else if (_type.equals(ObolTypes.FileIn)) {
                _inFile = verifyFile(_fnamebase, sym, false);
            } else if (_type.equals(ObolTypes.FileInOut)) {
                _outFile = verifyFile(_fnamebase + ".out", sym, true);
                _inFile = verifyFile(_fnamebase + ".in", sym, false);
            } else {
                throw (FormatException) new FormatException(__me + ".getInstance(): Unrecognized file-subtype \"" + _type + "\"!");
            }
            if (null != _outFile) {
                _out = new FileOutputStream(_outFile, sym.existProperty(SymbolProperties.FileAppendKeyword));
                log.info("verifyFile() [write, overwrite]: deleted/created " + "overwritten file \"" + _fnamebase + "\"");
            }
            if (null != _inFile) {
                _in = new FileInputStream(_inFile);
            }
            _peer = PeerHandle.getInstance(sym, format, _in, _out);
        } else if (_type.startsWith(ObolTypes.Stream)) {
            Object[] _streams = extractStreams(sym);
            _peer = PeerHandle.getInstance(sym, format, (InputStream) _streams[0], (OutputStream) _streams[1]);
        } else if (ObolTypes.URI.equals(_type)) {
            if (null == _value) {
                throw new ObolException(__me + ".getInstance(): symbol \"" + sym.getName() + "\" of type " + _type + " cannot have null value!");
            }
            try {
                _peer = PeerHandle.getInstance(sym, format, new URI((String) _value));
            } catch (URISyntaxException e) {
                throw (FormatException) new FormatException(__me + ".getInstance(): Syntax error in URI \"" + _value + "\"!").initCause(e);
            }
        } else if (ObolTypes.HostPort.equals(_type)) {
            if (sym.existProperty(SymbolProperties.PeerChannel)) {
                String _channel = (String) sym.getProperty(SymbolProperties.PeerChannel);
                if (_channel.equals(SymbolProperties.PeerChannelTCP)) {
                    _peer = null;
                } else if (_channel.equals(SymbolProperties.PeerChannelUDP) || _channel.equals(SymbolProperties.PeerChannelMultiCast)) {
                    _peer = new DatagramPeerHandle(sym, format);
                } else if (_channel.equals(SymbolProperties.PeerChannelDeliverable)) {
                    _peer = new DeliverablePeerHandle(sym, format);
                } else {
                    throw new RuntimeException(__me + ".getInstance(): symbol \"" + sym.getName() + "\" has unsupported channel type \"" + _channel + "\"");
                }
            }
            if (null == _peer) {
                _peer = new SocketPeerHandle(sym, format);
            }
        } else {
            String _s = ".getInstance: symbol has unsupported type \"" + _type + "\"";
            throw new ObolException(_s);
        }
        log.debug("getInstance(): returns " + _peer.getClass().getName());
        return _peer;
    }

    public static PeerHandle getInstance(Symbol sym, Format format, InetAddress address, int port) {
        return new SocketPeerHandle(sym, format, address, port);
    }

    public static PeerHandle getInstance(Symbol sym, Format format, String address, int port) throws UnknownHostException {
        return getInstance(sym, format, InetAddress.getByName(address), port);
    }

    /** Set up a nameless PeerHandle based on known streams.
     * Typically used by cryptographic operations (encrypt/decrypt).
     */
    public static PeerHandle getInstance(Format format, InputStream in, OutputStream out) {
        return getInstance((Symbol) null, format, in, out);
    }

    public static PeerHandle getInstance(Symbol sym, Format format, InputStream in, OutputStream out) {
        return new PeerHandle(sym, format, in, out);
    }

    public static PeerHandle getInstance(Symbol sym, Format format, URI uri) throws MalformedURLException {
        return getInstance(sym, format, uri.toURL());
    }

    public static PeerHandle getInstance(Symbol sym, Format format, URL url) throws MalformedURLException {
        throw new RuntimeException(__me + ".getInstance(... URL): Not Yet Implemented!");
    }

    /** Set up a PeerHandle based on an already-connected socket.
     * Derives streams and peer identity from the socket.
     * This is the only PeerHandle constructor for pool mode reception.
     * When using this constructor, the resulting instance will have
     * disabled receive methods, which will throw exceptions instead.
     * @param name name of associated symbol 
     * @param format Format instance to associate with connection
     * @param incomming socket connection
     */
    public static PeerHandle getInstance(Symbol sym, Format format, Socket incomming) throws IOException {
        return new SocketPeerHandle(sym, format, incomming);
    }

    /** Set up a PeerHandle based on an already-connected socket.
     * Derives streams and peer identity from the socket.
     * This is the only PeerHandle constructor for pool mode reception.
     * When using this constructor, the resulting instance will have
     * disabled receive methods, which will throw exceptions instead.
     * @param name name of associated symbol 
     * @param format Format instance to associate with connection
     * @param incomming UPD or Multicast Socket instance
     */
    public static PeerHandle getInstance(Symbol sym, Format format, DatagramSocket incomming, DatagramPacket packet) throws IOException {
        return new DatagramPeerHandle(sym, format, incomming, packet);
    }

    /** Set up a PeerHandle based on an already-connected socket.
     * Derives streams and peer identity from the socket.
     * This is the only PeerHandle constructor for pool mode reception.
     * When using this constructor, the resulting instance will have
     * disabled receive methods, which will throw exceptions instead.
     * @param name name of associated symbol 
     * @param format Format instance to associate with connection
     * @param incomming Deliverable object
     */
    public static PeerHandle getInstance(Symbol sym, Format format, Deliverable incomming) throws IOException {
        return new DeliverablePeerHandle(sym, format, incomming);
    }

    /** Only used by message pool machinery (MessageFormatHandler and friends).
     */
    public static PeerHandle getInstance(String formatspec, Object accepted) throws FormatException, IOException {
        return getInstance(FormatBase.getInstance(formatspec), accepted);
    }

    /** Only used by message pool machinery (MessageFormatHandler and friends).
     */
    public static PeerHandle getInstance(Format format, Object accepted) throws IOException {
        if (false == accepted instanceof Accepted) {
            throw new IllegalArgumentException(__me + ".getInstance(2): " + "expected 2nd parameter to be instance of PeerHandle." + "Accepted, not " + accepted.getClass().getName() + "!");
        }
        Accepted _ac = (Accepted) accepted;
        Object _obj = _ac.getObject();
        if (_obj instanceof Socket) {
            return new SocketPeerHandle(null, format, (Socket) _obj);
        } else if (_obj instanceof DatagramSocket || _obj instanceof MulticastSocket) {
            return new DatagramPeerHandle(null, format, (DatagramSocket) _obj, (DatagramPacket) _ac.getData());
        } else if (_obj instanceof Deliverable) {
            return new DeliverablePeerHandle(null, format, (Deliverable) _obj);
        } else {
            throw new RuntimeException(__me + ".getInstance(): illegal " + "accepted class " + accepted.getClass().getName());
        }
    }

    /** Only used by message pool machinery (MessageFormatHandler and friends).
     * Returns a peerhandle suitable for server-side accept loop (just
     * accepts, further processing delegated to worker).
     */
    public static PeerHandle getInstancePoolMode(Symbol sym, Format format) throws ObolException, IOException {
        PeerHandle _p = getInstance(sym, format);
        _p.inPoolMode = true;
        return _p;
    }

    /** This attempts to make sure that no connection hangs around after
     * their time is up.
     */
    protected void finalize() throws Throwable {
        this.closeAll();
    }

    /** Return the inputstream associated with this peerhandle.
     * Only legal when the instance is in pool mode.
     * @see format.PeerHandle(String, Format, Socket)
     */
    public InputStream getInputStream() {
        if (false == this.inPoolMode) {
            throw new UnsupportedOperationException(__me + ".getInputStream(): illegal call unless " + "PeerHandle \"" + this.name + "\" is in poolmode!");
        }
        return this.input;
    }

    /** Return the outputstream associated with this peerhandle.
     * Only legal when the instance is in pool mode.
     * @see format.PeerHandle(String, Format, Socket)
     */
    public OutputStream getOutputStream() {
        if (false == this.inPoolMode) {
            throw new UnsupportedOperationException(__me + ".getOutputStream(): illegal call unless " + "PeerHandle \"" + this.name + "\" is in poolmode!");
        }
        return this.output;
    }

    public String getName() {
        return this.name;
    }

    /** Set the name associated with this instance.
     * Only legal when the instance is in pool mode.
     * @see format.PeerHandle(String, Format, Socket)
     */
    public void setName(String name) {
        if (false == this.inPoolMode) {
            throw new UnsupportedOperationException(__me + ".setName(\"" + name + "\"): illegal call unless PeerHandle \"" + this.name + "\" is in poolmode!");
        }
        this.name = name;
    }

    /** Splits a colon-separated hostname and portnumber into two strings,
     * returned in a string-array.
     * Supports IPV6 addresses as per RFC 2732.
     * @param hostport String containing hostname:port
     * @return String array with two elements, the hostname and the port
     */
    public static String[] splitHostAndPort(String hostport) {
        int _cidx = hostport.lastIndexOf(':');
        boolean _ip6 = (-1 != hostport.lastIndexOf("]:"));
        String[] _split = new String[2];
        _split[0] = null;
        if (-1 == _cidx) {
            _split[1] = hostport;
        } else {
            if (0 != _cidx) {
                _split[0] = sanitizeHostName(hostport.substring(0, _cidx));
            }
            _split[1] = hostport.substring(_cidx + 1);
        }
        return _split;
    }

    /** Test string to see if its contents can be a colon-separated
     * hostname/portnumber, suitable for the methods of this class that
     * require a hostname:port parameter.
     * @param hostport String to examine
     * @return <tt>true</tt> if this is a valid host:port string,
     * <tt>false</tt> * if not.
     */
    public static boolean isHostPortString(String hostport) {
        boolean _isHostPort = false;
        String[] _split = splitHostAndPort(hostport);
        try {
            Integer.parseInt(_split[1]);
            _isHostPort = true;
        } catch (NumberFormatException e) {
        }
        return _isHostPort;
    }

    public static String sanitizeHostName(String host) {
        if (null != host) {
            int _first = host.indexOf('[');
            int _last = host.lastIndexOf(']');
            if (-1 != _first && -1 != _last) {
                host = host.substring(_first + 1, _last - 1);
            }
            if ('/' == host.charAt(0)) {
                host = host.substring(1);
            }
        }
        return host;
    }

    public static InetSocketAddress hostportToSocketAddress(String hostport) {
        String[] _split = splitHostAndPort(hostport);
        if (null == _split[0]) {
            return new InetSocketAddress(Integer.parseInt(_split[1]));
        } else {
            return new InetSocketAddress(sanitizeHostName(_split[0]), Integer.parseInt(_split[1]));
        }
    }

    public static void threadTerminationCleanup() {
    }

    public synchronized void closeAll() {
        try {
            if (null != this.input) {
                this.input.close();
                this.input = null;
            }
        } catch (Exception e) {
        }
        try {
            if (null != this.output) {
                this.output.flush();
                this.output.close();
                this.output = null;
            }
        } catch (Exception e) {
        }
    }

    public Format getFormat() {
        return this.currentFormat;
    }

    public String getRemoteID() {
        return null;
    }

    /** Sets the receive timeout value, which may or may not mean something to the
     * underlying peerhandle implementation.
     * If the underlying implementation doesn't support this functionallity,
     * this method has no effect.
     * @param milliseconds timeout in milliseconds, &lt;= 0 to disable.
     */
    public void setReceiveTimeout(int milliseconds) {
        this.timeout = milliseconds;
    }

    protected void ensureConnected() throws ObolException {
        if (false == this.isConnected) {
        }
    }

    protected void ensureAccepting() throws IOException, ObolException {
        if (false == this.isAccepting) {
        }
    }

    public boolean isConnected() {
        return (this.isAccepting && this.isConnected);
    }

    /** Examines the list of message components, and returns a string
     * containing all those that are secrets.  Used to warn about sending
     * secrets on a channel.
     * @return String containing warning about secret components, or null.
     */
    public String checkForSecrets(SymbolList msgparts) {
        StringBuffer _warn = new StringBuffer();
        for (Iterator _it = msgparts.iterator(); _it.hasNext(); ) {
            Symbol _s = (Symbol) _it.next();
            String _n = _s.getName();
            if (ObolTypes.PrivateKey.equals(_s.getType())) {
                _warn.append("private-key symbol ").append(_n).append(", ");
            } else if (ObolTypes.SharedKey.equals(_s.getType())) {
                _warn.append("shared-key symbol").append(_n).append(", ");
            } else if (_s.getValue() instanceof PrivateKey) {
                _warn.append("symbol ").append(_n).append("with PrivateKey value instance, ");
            } else if (_s.getValue() instanceof SecretKey) {
                _warn.append("symbol ").append(_n).append("with SecretKey value  instance, ");
            }
        }
        if (0 != _warn.length()) {
            if (((FormatBase) this.currentFormat).inCrypto()) {
                _warn.insert(0, "(encrypted) ");
            } else {
                _warn.insert(0, "IN CLEAR ");
            }
            return _warn.toString();
        }
        return null;
    }

    public void send(SymbolList msgparts) throws ObolException, IOException {
        this.send(msgparts, true);
    }

    protected void send(SymbolList msgparts, boolean checkForSecrets) throws ObolException, IOException {
        this.ensureConnected();
        if (checkForSecrets) {
            String _warn = this.checkForSecrets(msgparts);
            if (null != _warn) {
                log.warn("[WARNING] sending secrets " + _warn);
            }
        }
        this.currentFormat.convert(msgparts, this.output);
    }

    protected Object accept(Object bindkey) throws ObolException, IOException {
        this.ensureAccepting();
        return null;
    }

    /** Assume that accept() returned something we can use, and receive.
     * Used directly by pool mode infrastructure, all others should use receive().
     * @param accepted Object as returned by accept();
     */
    protected SymbolList receive(Object accepted) throws ObolException, IOException {
        SymbolList _received = this.currentFormat.convert(this.input);
        for (Iterator _i = _received.iterator(); _i.hasNext(); ) {
            Symbol _item = (Symbol) _i.next();
            if (false == _item.existProperty(SymbolProperties.Type)) {
                throw new ObolException(__me + ".receive(): current format \"" + this.currentFormat.getFormatName() + "\" failed to " + "assign type (should be at least binary) to received " + "symbol (" + _item.dumpAll() + ")!");
            }
        }
        return _received;
    }

    /** Assume that this instance was initialized with the address that we
     * should bind to, and receive from this. */
    public SymbolList receive() throws ObolException, IOException {
        assert false == this.inPoolMode : ("PeerHandle \"" + this.name + "\" in poolmode, receive is prohibited!");
        return this.receive(this.accept(null));
    }

    public SymbolList receiveAt(String s) throws ObolException, IOException {
        throw new UnsupportedOperationException(__me + ".receiveAt(): " + "method not supported on PeerHandle base class (must be " + "subclassed!)");
    }

    /** Returns whether this instance represents a stream or not.
     * A stream can contain multiple messages, and should be iterated over.
     * This knowledge is mostly of interest to the message pool machinery.
     * @return <tt>true</tt> if this instance represents a stream, or
     * <tt>false</tt> if it does not.
     */
    public boolean isStream() {
        return true;
    }
}

/**  Peerhandle subclass dealing with TCP communications.
 * @version $Id: PeerHandle.java,v 1.3 2008/05/04 15:52:56 perm Exp $
*/
class SocketPeerHandle extends PeerHandle {

    public static final String __me = "obol.format.SocketPeerHandle";

    protected InetAddress address = null;

    protected int port = 0;

    protected Socket socket = null;

    protected ServerSocket serversocket = null;

    protected InetSocketAddress serverAddress = null;

    protected InetSocketAddress peerAddress = null;

    /** Weak hashmap of all serversockets allocated by the current thread.
     * Handy for cleaning up.
     */
    public static ThreadLocal currentThreadServerSockets = new ThreadLocal() {

        protected synchronized Object initialValue() {
            return new WeakHashMap();
        }
    };

    private Debug log = Debug.getInstance(__me);

    public SocketPeerHandle(Symbol sym, Format format, InetAddress address, int port) {
        super(sym, format, null, null);
        this.address = address;
        this.port = port;
    }

    protected SocketPeerHandle(Symbol sym, Format format) throws IOException {
        super(sym, format, null, null);
        if (false == ObolTypes.HostPort.equals(sym.getType())) {
            throw new IllegalArgumentException(__me + "(): symbol parameter is not of " + sym.getType() + "type!");
        }
        int _port = 0;
        String _hostname = (String) sym.getValue();
        if (null != _hostname) {
            if (isHostPortString(_hostname)) {
                String[] _sa = splitHostAndPort(_hostname);
                _hostname = _sa[0];
                _port = Integer.parseInt(_sa[1]);
            }
            this.address = InetAddress.getByName(_hostname);
        }
        if (sym.existProperty(SymbolProperties.PeerPort)) {
            _port = Integer.parseInt((String) sym.getProperty(SymbolProperties.PeerPort));
        }
        this.port = _port;
    }

    /** Set up a PeerHandle based on an already-connected socket.
     * Derives streams and peer identity from the socket.
     * This is the only PeerHandle constructor for pool mode reception.
     * When using this constructor, the resulting instance will have
     * disabled receive methods, which will throw exceptions instead.
     * @param name name of associated symbol 
     * @param format Format instance to associate with connection
     * @param incomming socket connection
     */
    protected SocketPeerHandle(Symbol sym, Format format, Socket incomming) throws IOException {
        super(sym, format, incomming.getInputStream(), incomming.getOutputStream());
        this.socket = incomming;
        this.peerAddress = (InetSocketAddress) incomming.getRemoteSocketAddress();
        this.inPoolMode = (null != incomming);
        if (null != incomming) {
            this.setInputStream(incomming.getInputStream());
            this.setOutputStream(incomming.getOutputStream());
        }
    }

    /** This attempts to make sure that no connection hangs around after
     * their time is up.
     */
    protected void finalize() throws Throwable {
        this.closeAll();
        super.finalize();
    }

    public static void threadTerminationCleanup() {
        WeakHashMap _map = (WeakHashMap) currentThreadServerSockets.get();
        Iterator _it = _map.keySet().iterator();
        while (_it.hasNext()) {
            ServerSocket _ss = (ServerSocket) _it.next();
            WeakReference _wr = (WeakReference) _map.get(_ss);
            if (null != _wr) {
                PeerHandle _ph = (PeerHandle) _wr.get();
                if (null != _ph) {
                    _ph.closeAll();
                }
            }
            if (null != _ss) {
                try {
                    if (false == _ss.isClosed()) {
                        _ss.close();
                    }
                } catch (Exception e) {
                }
            }
            try {
                _it.remove();
            } catch (UnsupportedOperationException e) {
            }
        }
    }

    public synchronized void closeAll() {
        try {
            if (null != this.serversocket) {
                this.serversocket.close();
                this.serversocket = null;
            }
        } catch (Exception e) {
        }
        try {
            if (null != this.socket) {
                this.socket.close();
                this.socket = null;
            }
        } catch (Exception e) {
        }
    }

    /** Return the address we're connected to (the remote side),  if
     * that information is available (if not, it returns null).
     */
    public String getRemoteID() {
        if (null != this.peerAddress) {
            return this.peerAddress.toString();
        } else {
            return super.getRemoteID();
        }
    }

    protected void connect() throws ObolException {
        if ((null != this.socket) && (null != this.output)) {
            return;
        }
        try {
            this.socket = new Socket(this.address, this.port);
            this.peerAddress = (InetSocketAddress) this.socket.getRemoteSocketAddress();
            this.setInputStream(this.socket.getInputStream());
            this.setOutputStream(this.socket.getOutputStream());
        } catch (Exception e) {
            throw (ObolException) new ObolException(__me + ".connect(): caught exception whilst connecting to \"" + this.address.toString() + "\" port " + this.port).initCause(e);
        }
    }

    protected Accepted accept(Object bindkey) throws IOException, ObolException {
        if (false == this.inPoolMode) {
            if (null != this.socket && null != this.input) {
                if ((false == this.socket.isClosed()) && (false == this.socket.isInputShutdown()) && (false == this.socket.isOutputShutdown())) {
                    return new Accepted(this.socket);
                }
            }
        }
        try {
            boolean _makenewSS = false;
            if (null == this.serversocket) {
                _makenewSS = true;
            } else if (this.serversocket.isClosed()) {
                _makenewSS = true;
            }
            if (_makenewSS) {
                if (null != bindkey && null == this.serverAddress) {
                    if (bindkey instanceof String) {
                        this.serverAddress = hostportToSocketAddress((String) bindkey);
                    } else if (bindkey instanceof InetSocketAddress) {
                        this.serverAddress = (InetSocketAddress) bindkey;
                    } else {
                        throw new ObolException(__me + ".accept(): bindkey " + "of unknown class: " + bindkey.getClass().getName());
                    }
                }
                InetSocketAddress _bind = this.serverAddress;
                if (null == _bind) {
                    _bind = new InetSocketAddress(this.port);
                }
                this.serversocket = new ServerSocket();
                this.serversocket.bind(_bind);
                log.info("Created and bound serversocket to " + _bind);
                if (0 <= this.timeout) {
                    this.serversocket.setSoTimeout(this.timeout);
                }
                if (false == this.serversocket.getReuseAddress()) {
                    this.serversocket.setReuseAddress(true);
                }
                ((WeakHashMap) currentThreadServerSockets.get()).put(this.serversocket, new WeakReference(this));
            }
            log.info("Accepting on " + this.serversocket);
            Socket _sock;
            while (true) {
                _sock = this.serversocket.accept();
                if (this.inPoolMode) {
                    this.log.info("receive(): connection from " + _sock.getInetAddress());
                    return new Accepted(_sock);
                }
                if (null != this.address) {
                    if (this.address.equals(_sock.getInetAddress())) {
                        this.log.info("receive(): filter accepting connection from " + this.address);
                        break;
                    } else {
                        this.log.info("receive(): filter refusing connection from " + _sock.getInetAddress());
                        try {
                            _sock.shutdownInput();
                        } catch (IOException e) {
                        }
                        try {
                            _sock.shutdownOutput();
                        } catch (IOException e) {
                        }
                        try {
                            _sock.close();
                        } catch (IOException e) {
                        }
                        _sock = null;
                        continue;
                    }
                }
                break;
            }
            this.socket = _sock;
            this.peerAddress = (InetSocketAddress) this.socket.getRemoteSocketAddress();
            log.info("Accepted connection from " + this.socket);
            this.setInputStream(this.socket.getInputStream());
            this.setOutputStream(this.socket.getOutputStream());
        } catch (ObolException e) {
            String _adr = (null == this.address) ? "null" : this.address.toString();
            throw (ObolException) new ObolException(__me + "connect(): caught exception whilst accepting to \"" + _adr + "\" port " + this.port).initCause(e);
        }
        return new Accepted(this.socket);
    }

    protected void ensureConnected() throws ObolException {
        if (false == this.isConnected) {
            this.connect();
        }
    }

    protected void ensureAccepting() throws IOException, ObolException {
        if (false == this.isAccepting) {
            this.accept(null);
        }
    }

    public boolean isConnected() {
        return (this.isAccepting && this.isConnected);
    }

    public boolean isStream() {
        return true;
    }

    public void send(SymbolList msgparts) throws ObolException, IOException {
        this.ensureConnected();
        String _warn = this.checkForSecrets(msgparts);
        if (null != _warn) {
            String _dest;
            if (null != this.socket) {
                _dest = this.socket.getRemoteSocketAddress().toString();
            } else {
                _dest = "outputstream(file?)";
            }
            log.warn("[WARNING] sending secrets " + _warn + " to " + _dest);
        }
        super.send(msgparts, false);
    }

    protected SymbolList receive(Object accepted) throws ObolException, IOException {
        if (false == (accepted instanceof Accepted)) {
            throw new ObolException(__me + ".receive(1): got non-accepted parameter!");
        }
        if (false == this.inPoolMode) {
            Accepted _ac = (Accepted) accepted;
            this.socket = (Socket) _ac.getObject();
            this.peerAddress = (InetSocketAddress) this.socket.getRemoteSocketAddress();
            log.info("Accepted connection from " + this.socket);
            this.setInputStream(this.socket.getInputStream());
            this.setOutputStream(this.socket.getOutputStream());
        }
        return super.receive(null);
    }

    /** Assume that this instance was initialized with the address that we
     * should bind to, and receive from this. */
    public SymbolList receive() throws ObolException, IOException {
        return super.receive();
    }

    /** Receive from the given address, and assume that any address this
     * instance was initialized with is a connection constraint. */
    public SymbolList receiveAt(InetSocketAddress bind) throws ObolException, IOException {
        if (null == this.serverAddress) {
            this.serverAddress = bind;
        }
        return this.receive();
    }

    /** Receive from the given address, and assume that any address this
     * instance was initialized with is a connection constraint. */
    public SymbolList receiveAt(String host, int port) throws ObolException, IOException {
        InetSocketAddress _bind = this.serverAddress;
        if (null == this.serverAddress) {
            if (null == host) {
                _bind = new InetSocketAddress(port);
            } else {
                _bind = new InetSocketAddress(sanitizeHostName(host), port);
            }
        }
        return this.receiveAt(_bind);
    }

    /** Receive from the given address, and assume that any address this
     * instance was initialized with is a connection constraint.
     * @param hostport the hostname and portname as a string, separated by a
     * colon (:).
     */
    public SymbolList receiveAt(String hostport) throws ObolException, IOException {
        String[] _sa = splitHostAndPort(hostport);
        return this.receiveAt(_sa[0], Integer.parseInt(_sa[1]));
    }
}

/** Peerhandle subclass dealing with UDP and Multicast communications.
 * @version $Id: PeerHandle.java,v 1.3 2008/05/04 15:52:56 perm Exp $
*/
class DatagramPeerHandle extends PeerHandle {

    public static final String __me = "obol.format.DatagramPeerHandle";

    protected InetAddress address = null;

    protected int port = 0;

    protected DatagramSocket socket = null;

    protected InetSocketAddress serverAddress = null;

    protected InetSocketAddress peerAddress = null;

    protected int MaxDatagramSize = 65536;

    protected boolean multicast = false;

    protected boolean groupJoined = false;

    protected InetAddress multicastGroup = null;

    protected int multicastTTL = -1;

    protected int broadcast = -1;

    protected int trafficClass = -1;

    private Debug log = Debug.getInstance(__me);

    protected DatagramPeerHandle(Symbol sym, Format format) throws IOException {
        this(sym, format, null, null);
        if (false == ObolTypes.HostPort.equals(sym.getType())) {
            throw new IllegalArgumentException(__me + "(): symbol parameter is not of " + sym.getType() + "type!");
        }
        int _port = 0;
        String _hostname = (String) sym.getValue();
        if (null != _hostname) {
            if (isHostPortString(_hostname)) {
                String[] _sa = splitHostAndPort(_hostname);
                _hostname = _sa[0];
                _port = Integer.parseInt(_sa[1]);
            }
            this.address = InetAddress.getByName(_hostname);
        }
        if (this.multicast && null == this.multicastGroup) {
            this.multicastGroup = this.address;
        }
        if (null != sym) {
            if (sym.existProperty(SymbolProperties.PeerPort)) {
                _port = Integer.parseInt((String) sym.getProperty(SymbolProperties.PeerPort));
            }
        }
        this.port = _port;
    }

    public DatagramPeerHandle(Symbol sym, Format format, DatagramSocket incomming, DatagramPacket packet) throws IOException {
        super(sym, format, null, null);
        if (null != sym) {
            if (sym.existProperty(SymbolProperties.MaxDatagramSize)) {
                this.MaxDatagramSize = Integer.parseInt((String) sym.getProperty(SymbolProperties.MaxDatagramSize));
            }
            if (sym.existProperty(SymbolProperties.PeerChannel)) {
                this.multicast = SymbolProperties.PeerChannelMultiCast.equals((String) sym.getProperty(SymbolProperties.PeerChannel));
                if (this.multicast) {
                    if (sym.existProperty(SymbolProperties.MulticastGroup)) {
                        String _group = (String) sym.getProperty(SymbolProperties.MulticastGroup);
                        this.multicastGroup = InetAddress.getByName(_group);
                        this.log.info("(): multicast, group is " + _group);
                    } else {
                        this.log.warn("(): channel type is \"" + SymbolProperties.PeerChannelMultiCast + "\", but required multicast group property \"" + SymbolProperties.MulticastGroup + "\" was not " + "found (will cause send/receive failure)!");
                    }
                }
            }
            if (sym.existProperty(SymbolProperties.DatagramBroadcast)) {
                if (this.multicast) {
                    this.log.warn("(): symbol property \"" + SymbolProperties.DatagramBroadcast + "\" is not only meaningful on (udp) multicast " + "channels (ignored)!");
                } else {
                    String _b = (String) sym.getProperty(SymbolProperties.DatagramTrafficClass);
                    if ("0".equals(_b)) {
                        this.broadcast = 0;
                    } else if ("1".equals(_b)) {
                        this.broadcast = 1;
                    } else {
                        this.log.warn("(): symbol property \"" + SymbolProperties.DatagramBroadcast + "\" accepts boolean values 0 or 1 only: " + "ignoring \"" + _b + "\"!");
                    }
                }
            }
            if (sym.existProperty(SymbolProperties.DatagramTrafficClass)) {
                int _i = Integer.parseInt((String) sym.getProperty(SymbolProperties.DatagramTrafficClass));
                if (0 <= _i && _i <= 255) {
                    this.trafficClass = _i;
                } else {
                    this.log.warn("(): symbol property \"" + SymbolProperties.DatagramTrafficClass + "\" value " + _i + "exceeds valid range 0..255: ignored!");
                }
            }
            if (sym.existProperty(SymbolProperties.MulticastTimeToLive)) {
                if (this.multicast) {
                    int _i = Integer.parseInt((String) sym.getProperty(SymbolProperties.MulticastTimeToLive));
                    if (0 <= _i && _i <= 255) {
                        this.multicastTTL = _i;
                    } else {
                        this.log.warn("(): symbol property \"" + SymbolProperties.MulticastTimeToLive + "\" value " + _i + "exceeds valid range 0..255: ignoring!");
                    }
                } else {
                    this.log.warn("(): symbol property \"" + SymbolProperties.MulticastTimeToLive + "\" is only meaningful on (udp) multicast " + "channels (ignored)!");
                }
            }
        }
        this.socket = incomming;
        this.inPoolMode = (null != incomming);
        if (this.inPoolMode) {
            this.serverAddress = (InetSocketAddress) incomming.getLocalSocketAddress();
            if (null == this.serverAddress) {
                this.log.error("(poolmode constuctor): unbound incomming socket?");
            }
            if (null != packet) {
                this.peerAddress = (InetSocketAddress) packet.getSocketAddress();
            }
        }
    }

    public synchronized void closeAll() {
        super.closeAll();
        try {
            if (null != this.socket) {
                if (this.multicast && this.groupJoined) {
                    try {
                        ((MulticastSocket) this.socket).leaveGroup(this.multicastGroup);
                        this.groupJoined = false;
                    } catch (Exception e) {
                        this.log.warn(".closeAll(): caught (and ignored) while " + "leaving multicast group: ", e);
                    }
                }
                this.socket.close();
                this.socket = null;
            }
        } catch (Exception e) {
        }
    }

    private synchronized void ensureSocketSetup() throws IOException {
        boolean _create = (null == this.socket);
        if (false == _create) {
            _create = this.socket.isClosed();
        }
        if (_create) {
            this.log.debug("ensureSocketSetup(): creating datagramsocket...");
            if (null == this.serverAddress) {
                if (this.multicast) {
                    this.socket = new MulticastSocket(this.port);
                } else {
                    this.socket = new DatagramSocket(new InetSocketAddress(this.port));
                }
            } else {
                if (this.multicast) {
                    this.socket = new MulticastSocket(this.serverAddress);
                    if (-1 != this.multicastTTL) {
                        ((MulticastSocket) this.socket).setTimeToLive(this.multicastTTL);
                        this.log.info(".ensureSocketSetup(): setting multicast TTL to " + this.multicastTTL);
                    }
                } else {
                    this.socket = new DatagramSocket(this.serverAddress);
                    if (-1 != this.broadcast) {
                        this.socket.setBroadcast(1 == this.broadcast);
                        this.log.info(".ensureSocketSetup(): " + ((1 == this.broadcast) ? "en" : "dis") + "abling broadcast");
                    }
                    if (-1 != this.trafficClass) {
                        this.socket.setTrafficClass(this.trafficClass);
                        this.log.info(".ensureSocketSetup(): setting traffic class to " + this.trafficClass);
                    }
                }
            }
            this.socket.setReuseAddress(true);
            this.log.debug("ensureSocketSetup(): created and bound datagram socket");
        }
        if (this.multicast && false == this.groupJoined) {
            ((MulticastSocket) this.socket).joinGroup(this.multicastGroup);
            this.log.debug("ensureSocketSetup(): joined multicast group \"" + this.multicastGroup + "\"");
            this.groupJoined = true;
        }
    }

    public void send(SymbolList msgparts) throws ObolException, IOException {
        ByteArrayOutputStream _bout = new ByteArrayOutputStream();
        this.setOutputStream(_bout);
        super.send(msgparts);
        super.output.flush();
        byte[] _data = _bout.toByteArray();
        _bout.close();
        InetAddress _dest = this.address;
        if (this.multicast) {
            _dest = this.multicastGroup;
        }
        if (null == _dest) {
            if (null != this.peerAddress) {
                _dest = this.peerAddress.getAddress();
            } else {
                this.log.warn("send(): null destination address!");
            }
        }
        this.ensureSocketSetup();
        this.log.debug("send(): sending " + _data.length + " bytes to " + _dest + " port " + this.port);
        DatagramPacket _packet = new DatagramPacket(_data, _data.length, _dest, this.port);
        this.socket.send(_packet);
    }

    protected Accepted accept(Object bindkey) throws ObolException, IOException {
        int _max = this.MaxDatagramSize;
        if (null != this.symbol) {
            if (this.symbol.existProperty(SymbolProperties.MaxDatagramSize)) {
                _max = Integer.parseInt((String) this.symbol.getProperty(SymbolProperties.MaxDatagramSize));
            }
        }
        this.log.debug("receive(): max = " + _max);
        if (null != bindkey && null == this.serverAddress) {
            if (bindkey instanceof String) {
                this.serverAddress = hostportToSocketAddress((String) bindkey);
            } else if (bindkey instanceof InetSocketAddress) {
                this.serverAddress = (InetSocketAddress) bindkey;
            } else {
                throw new ObolException(__me + ".accept(): bindkey " + "of unknown class: " + bindkey.getClass().getName());
            }
        }
        this.ensureSocketSetup();
        DatagramPacket _packet = new DatagramPacket(new byte[_max], _max);
        InetSocketAddress _remotePeer = null;
        while (true) {
            this.log.debug("receive(): calling socket receive");
            if (0 <= this.timeout) {
                this.socket.setSoTimeout(this.timeout);
            }
            this.socket.receive(_packet);
            _remotePeer = (InetSocketAddress) _packet.getSocketAddress();
            this.peerAddress = _remotePeer;
            if (this.inPoolMode) {
                this.log.debug("receive(): socket receive returned, packet from " + _remotePeer);
                return new Accepted(this.socket, _packet);
            }
            if (null != this.address) {
                if (this.address.equals(_remotePeer.getAddress())) {
                    this.log.debug("receive(): filter accepting packet from " + _remotePeer);
                    break;
                } else {
                    this.log.debug("receive(): filter ignoring packet from " + _remotePeer);
                    continue;
                }
            }
            break;
        }
        this.log.debug("receive(): receive() returned");
        if (null == _remotePeer) {
            this.log.warn("receive(): null remote peer address!");
        }
        return new Accepted(this.socket, _packet);
    }

    public SymbolList receive(Object accepted) throws ObolException, IOException {
        if (false == (accepted instanceof Accepted)) {
            throw new ObolException(__me + ".receive(1): got non-accepted parameter!");
        }
        Accepted _ac = (Accepted) accepted;
        if (false == _ac.hasData()) {
            throw new ObolException(__me + ".receive(1): no data received!");
        }
        DatagramPacket _packet = (DatagramPacket) _ac.getData();
        ;
        InetSocketAddress _remotePeer = (InetSocketAddress) _packet.getSocketAddress();
        if (null == _remotePeer) {
            this.log.warn("receive(1): null remote peer address!");
        }
        this.peerAddress = _remotePeer;
        if (null == this.address) {
            this.address = _remotePeer.getAddress();
            this.port = _remotePeer.getPort();
        }
        ByteArrayInputStream _bin = new ByteArrayInputStream(_packet.getData());
        this.setInputStream(_bin);
        SymbolList _retval = super.receive(null);
        _bin.close();
        return _retval;
    }

    public SymbolList receive() throws ObolException, IOException {
        return super.receive();
    }

    /** Receive from the given address, and assume that any address this
     * instance was initialized with is a connection constraint. */
    public SymbolList receiveAt(InetSocketAddress bind) throws ObolException, IOException {
        if (null == this.serverAddress) {
            this.serverAddress = bind;
        }
        return this.receive();
    }

    /** Receive from the given address, and assume that any address this
     * instance was initialized with is a connection constraint. */
    public SymbolList receiveAt(String host, int port) throws ObolException, IOException {
        InetSocketAddress _bind = this.serverAddress;
        if (null == this.serverAddress) {
            if (null == host) {
                _bind = new InetSocketAddress(port);
            } else {
                _bind = new InetSocketAddress(sanitizeHostName(host), port);
            }
        }
        return this.receiveAt(_bind);
    }

    /** Receive from the given address, and assume that any address this
     * instance was initialized with is a connection constraint.
     * @param hostport the hostname and portname as a string, separated by a
     * colon (:).
     */
    public SymbolList receiveAt(String hostport) throws ObolException, IOException {
        String[] _sa = splitHostAndPort(hostport);
        return this.receiveAt(_sa[0], Integer.parseInt(_sa[1]));
    }

    /** Return the address we're connected to (the remote side),  if
     * that information is available (if not, it returns null).
     */
    public String getRemoteID() {
        if (null != this.peerAddress) {
            return this.peerAddress.toString();
        } else {
            return super.getRemoteID();
        }
    }

    public boolean isStream() {
        return false;
    }
}

/** Peerhandle subclass dealing with application-provided communication
 * mechanisms.  This behaves similar to UDP.
 * From an application, 
*/
class DeliverablePeerHandle extends PeerHandle {

    public static final String __me = "obol.format.DeliverablePeerHandle";

    protected Object address = null;

    protected Deliverable channel = null;

    protected Object serverID = null;

    protected Object peerID = null;

    private Debug log = Debug.getInstance(__me, Debug.DEBUG);

    public DeliverablePeerHandle(Symbol sym, Format format, Deliverable incomming) throws IOException {
        super(sym, format, null, null);
        this.channel = incomming;
        this.inPoolMode = (null != incomming);
    }

    public DeliverablePeerHandle(Symbol sym, Format format) throws IOException {
        this(sym, format, null);
        if (sym.existProperty(SymbolProperties.PeerChannelDeliverable)) {
            Object _o = sym.getProperty(SymbolProperties.PeerChannelDeliverable);
            System.err.println(__me + "(): _o is " + _o.getClass().getName() + " = " + _o.toString());
            this.channel = (Deliverable) _o;
        } else {
            this.log.warn("(): channel type is \"" + ((String) sym.getProperty(SymbolProperties.PeerChannel)) + "\", but required property \"" + SymbolProperties.PeerChannelDeliverable + "\" containing Deliverable object instance reference " + "was NOT found (will cause send/receive failure)!");
        }
        this.address = sym.getValue();
    }

    public void closeAll() {
        super.closeAll();
        if (null != this.channel) {
            try {
                this.channel.close();
            } catch (Exception e) {
                this.log.info("closeAll(): caught (and ignored) while " + "closing Deliverable channel: ", e);
            }
        }
    }

    public void send(SymbolList msgparts) throws ObolException, IOException {
        ByteArrayOutputStream _bout = new ByteArrayOutputStream();
        this.setOutputStream(_bout);
        super.send(msgparts);
        super.output.flush();
        byte[] _payload = _bout.toByteArray();
        _bout.close();
        Object _dest = this.address;
        if (null == _dest) {
            if (null != this.peerID) {
                _dest = this.peerID;
            } else {
                this.log.warn("send(): null destination address (might fail)!");
            }
        }
        synchronized (this.channel) {
            this.channel.setPeerID(_dest);
            this.channel.setData(_payload);
            this.channel.requestSend();
        }
    }

    protected Accepted accept(Object bindkey) throws ObolException, IOException {
        if (null != bindkey && null == this.serverID) {
            this.serverID = bindkey;
        }
        if (null == this.channel) {
            throw new RuntimeException(__me + ".accept(): null channel! " + "Deliverable channels should be set up by means of " + "input symbols, please ensure that this is the case!");
        }
        byte[] _data = null;
        Object _peerID = null;
        synchronized (this.channel) {
            this.channel.setLocalID(this.serverID);
            this.channel.requestReceive();
            _data = this.channel.getData();
            _peerID = this.channel.getPeerID();
        }
        return new Accepted(_peerID, _data);
    }

    public SymbolList receive(Object accepted) throws ObolException, IOException {
        if (false == (accepted instanceof Accepted)) {
            throw new ObolException(__me + ".receive(1): got non-accepted parameter!");
        }
        Accepted _ac = (Accepted) accepted;
        if (false == _ac.hasData()) {
            throw new ObolException(__me + ".receive(1): no data received!");
        }
        this.channel.setLocalID(this.serverID);
        this.peerID = _ac.getObject();
        if (null == this.peerID) {
            this.log.warn("receive(1): null remote peer address!");
        }
        if (null == this.address) {
            this.address = this.peerID;
        }
        ByteArrayInputStream _bin = new ByteArrayInputStream((byte[]) _ac.getData());
        this.setInputStream(_bin);
        SymbolList _retval = super.receive(null);
        _bin.close();
        return _retval;
    }

    /** Recieve from the given address, and assume that any address this
     * instance was initialized with is a connection constraint.
     * @param self String containing ID.
     */
    public SymbolList receiveAt(String self) throws ObolException, IOException {
        this.serverID = self;
        return this.receive();
    }

    public String getRemoteID() {
        if (null != this.channel) {
            Object _o = this.channel.getPeerID();
            if (_o instanceof String) {
                return (String) _o;
            } else {
                return _o.toString();
            }
        } else {
            return super.getRemoteID();
        }
    }

    public boolean isStream() {
        return false;
    }
}

/** Wrapper class for encapsulating result of accept and passing it to
 * receive.
 */
class Accepted {

    private Object obj = null;

    private Object data = null;

    private boolean hasData = false;

    public Accepted(Object object) {
        this.obj = object;
    }

    public Accepted(Object object, Object data) {
        this(object);
        this.data = data;
        this.hasData = true;
    }

    public Object getObject() {
        return this.obj;
    }

    public Object getData() {
        return this.data;
    }

    public boolean hasData() {
        return this.hasData;
    }
}
