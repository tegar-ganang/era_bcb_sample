package jdc.lib;

import jdc.util.EventDispatcher;
import jdc.util.Queue;
import jdc.util.Semaphore;
import jdc.util.checkTime;
import java.net.Socket;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.Vector;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.log4j.Logger;
import java.io.File;
import java.io.OutputStream;
import java.io.FileInputStream;

public class UserConnection extends TCPConnection {

    /** get the logger for this package */
    protected static Logger libLogger = LoggerContainer.getLogger(UserConnection.class);

    private static final String _MY_NICK_CMD = "$MyNick";

    private static final String _LOCK_CMD = "$Lock";

    private static final String _KEY_CMD = "$Key";

    private static final String _DIRECTION_CMD = "$Direction";

    private static final String _GET_LIST_LEN_CMD = "$GetListLen";

    private static final String _LIST_LEN_CMD = "$ListLen";

    private static final String _MAXED_OUT_CMD = "$MaxedOut";

    private static final String _ERROR_CMD = "$Error";

    private static final String _SEND_CMD = "$Send";

    private static final String _GET_CMD = "$Get";

    private static final String _GETZBLOCK_CMD = "$GetZBlock";

    private static final String _FILE_LENGTH_CMD = "$FileLength";

    private static final String _SUPPORTS_CMD = "$Supports";

    private static final String _SUPPORTS_ARG_BZLIST = "BZList";

    private static final String _SUPPORTS_ARG_CHUNK = "CHUNK";

    private static final String _SUPPORTS_ARG_MINISLOTS = "MiniSlots";

    private static final String _SUPPORTS_ARG_GETZBLOCK = "GetZBlock";

    private static final String _SUPPORTS_ARG_GETTESTZBLOCK = "GetTestZBlock";

    private static final String _REQUEST_LIST_FILE_NAME = "MyList.DcLst";

    private static final String _REQUEST_BZLIST_FILE_NAME = "MyList.bz2";

    private static final String _ERROR_MSG_FILE_NOT_AVAILABLE = "File Not Available";

    /** Nick of the user we are connected to. */
    private String _connected_to;

    /** This is the $Lock we have sent, used for verification. */
    private String _lock;

    private boolean _passive = false;

    private boolean _maxed_out = false;

    /** Have we connected or are we listening for incoming connections. */
    private boolean _listen_mode;

    /** Handles public callbacks. */
    private CallbackHandler _user_cb_handler;

    /** Queue of file requests. */
    private Queue _download_queue = new Queue();

    private int _download_request_counter = 0;

    private Thread _download_handler_thread;

    private Object _download_lock = new Object();

    /** Current download request. */
    private DownloadRequest _dr = null;

    private DownloadMode _download_mode = DownloadMode.NONE;

    private UploadMode _upload_mode = UploadMode.NONE;

    private UserConnectionState _userconnection_state = UserConnectionState.NONE;

    private Object _userconnection_state_mutex = new Object();

    private String _upload_file_path = null;

    private Object _direction_mutex = new Object();

    private ConnectionDirection _direction = ConnectionDirection.NONE;

    private int _direction_rdm_int = 0;

    private String _direction_cmd = null;

    private ConnectionDirection _counterpart_direction = ConnectionDirection.NONE;

    private java.util.Random _random_generator = new java.util.Random();

    /** The lock that should be sent by the counterpart. */
    private String _counterpart_lock = null;

    /** 
   * A lock that  will be released as soon as 
   *  the handshake process has been completed.
   */
    private Semaphore _handshake_lock = new Semaphore(0);

    private Boolean _acquired_slot = Boolean.FALSE;

    private Boolean _acquired_mini_slot = Boolean.FALSE;

    private boolean _counterpart_has_extended_protocol = false;

    private boolean _counterpart_supports_bzlist = false;

    private boolean _counterpart_supports_chunk = false;

    private boolean _counterpart_supports_minislots = false;

    private boolean _counterpart_supports_getzblock = false;

    private boolean _counterpart_supports_gettestzblock = false;

    /**
   * Handles the $MyNick command
   *
   */
    private class MyNickCB extends Callback {

        public MyNickCB() {
            super(_MY_NICK_CMD);
        }

        public void execute(Object data) {
            _connected_to = (String) data;
            _initDirection();
            _debug("Got the nick, waiting for the lock...");
            EventDispatcher.instance().dispatchEvent(new HandshakeEvent(_connected_to + "_HANDSHAKE", HandshakeEvent.INITIATED));
        }
    }

    /**
   * Handles the $Lock command
   *
   */
    private class LockCB extends Callback {

        public LockCB() {
            super(_LOCK_CMD);
        }

        public void execute(Object data) {
            _counterpart_lock = (String) data;
            if (_counterpart_lock.startsWith("EXTENDEDPROTOCOL")) {
                _counterpart_has_extended_protocol = true;
            }
            if (_listen_mode) {
                _debug("Got the lock, firing _sendListenerHandshakePart1()...");
                _sendListenerHandshakePart1(_counterpart_lock);
            } else {
                _debug("Got the lock, waiting for direction...");
            }
        }
    }

    /**
   * Handles the $Direction command
   *
   */
    private class DirectionCB extends Callback {

        private UserConnection _conn = null;

        public DirectionCB(UserConnection conn) {
            super(_DIRECTION_CMD);
            _conn = conn;
        }

        public void execute(Object data) {
            String direction = (String) data;
            _debug("Counterpart sent direction: " + direction);
            _counterpart_direction = (direction.indexOf("Upload") >= 0) ? ConnectionDirection.OUTGOING : ConnectionDirection.INCOMING;
            UserConnectionHandler.instance().add(_conn);
            if (_listen_mode) {
                _debug("Got the direction, waiting for key...");
            } else {
                _debug("Got the direction, waiting for key...");
            }
        }
    }

    /**
   * Handles the $Key command.
   *
   */
    private class KeyCB extends Callback {

        public KeyCB() {
            super(_KEY_CMD);
        }

        public void execute(Object data) {
            boolean key_ok = KeyHandler.instance().validateKey(_lock, (String) data);
            if (!key_ok) {
                String key = (String) data;
                String mkey = KeyHandler.instance().generateKey(_lock);
                for (int i = 0; i < mkey.length(); i++) {
                    if (key.charAt(i) != mkey.charAt(i)) _debug("[" + i + "]" + (byte) key.charAt(i) + "!=" + (int) mkey.charAt(i));
                }
                libLogger.error("KeyCB: Key mismatch!");
                _debug("      Got: '" + (String) data + "'");
                _debug(" Expected: '" + mkey + "'");
                disconnect();
            } else {
                if (_listen_mode) {
                    _debug("Got the key, now the actual downloading should start...");
                } else {
                    _debug("Got the key, firing up _sendConnectorHandshakePart2()...");
                    _sendConnectorHandshakePart2(_counterpart_lock);
                }
                _debug("Releasing the handshake lock");
                _handshake_lock.release();
                EventDispatcher.instance().dispatchEvent(new HandshakeEvent(_connected_to + "_HANDSHAKE", HandshakeEvent.OK));
            }
        }
    }

    /**
   * Handles the $GetListLen command.
   */
    private class GetListLenCB extends Callback {

        public GetListLenCB() {
            super(_GET_LIST_LEN_CMD);
        }

        public void execute(Object data) {
            _setUploadMode(UploadMode.FILE_LIST_UL);
            _upload_file_path = new String("");
            try {
                _setUserConnectionState(UserConnectionState.UL_WAITING_FOR_GET);
            } catch (IllegalStateTransition ex) {
                Configuration.instance().executeExceptionCallback(ex);
                libLogger.debug("Exception GetListLenCB", ex);
                return;
            }
            Long list_length = null;
            if (_counterpart_supports_bzlist) {
                list_length = new Long(Configuration.instance().getUser().getShareStorage().getMyBZipCompressedList().length());
            } else {
                list_length = new Long(Configuration.instance().getUser().getShareStorage().getMyCompressedList().length());
            }
            _sendListLen(list_length.toString());
        }
    }

    /**
   * Handles the $Get command.
   */
    private class GetCB extends Callback {

        public GetCB() {
            super(_GET_CMD);
        }

        public void execute(Object data) {
            String filename = (String) data;
            File upload_file = null;
            boolean acquire_slot = false;
            boolean acquire_mini_slot = false;
            if (filename.equals(_REQUEST_LIST_FILE_NAME + "$1") || filename.equals(_REQUEST_BZLIST_FILE_NAME + "$1")) {
                _setUploadMode(UploadMode.FILE_LIST_UL);
            }
            if (getUploadMode().equals(UploadMode.FILE_LIST_UL)) {
                if (filename.equals(_REQUEST_LIST_FILE_NAME + "$1")) {
                    _debug("Get a minislot for the file list!");
                    acquire_mini_slot = true;
                    upload_file = Configuration.instance().getUser().getShareStorage().getMyCompressedList();
                    _upload_file_path = upload_file.getName();
                } else if (_counterpart_supports_bzlist && filename.equals(_REQUEST_BZLIST_FILE_NAME + "$1")) {
                    acquire_mini_slot = true;
                    upload_file = Configuration.instance().getUser().getShareStorage().getMyBZipCompressedList();
                    _upload_file_path = upload_file.getName();
                } else {
                    libLogger.error("We are in file list upload mode, " + "but the other user requested " + filename);
                    _setUploadMode(UploadMode.NORMAL_FILE_UL);
                }
            } else _setUploadMode(UploadMode.NORMAL_FILE_UL);
            if (getUploadMode().equals(UploadMode.NORMAL_FILE_UL)) {
                int dollarOne = filename.lastIndexOf("$");
                String parsed_filename = filename.substring(0, dollarOne);
                String resolvedFileName = Configuration.instance().getUser().getShareStorage().resolvePath(parsed_filename);
                upload_file = new File(resolvedFileName);
                if (!upload_file.exists()) {
                    _debug("GetCB: File " + upload_file + " does not exist.");
                    _sendCommand(_ERROR_CMD, _ERROR_MSG_FILE_NOT_AVAILABLE);
                    return;
                }
                _upload_file_path = new String(resolvedFileName);
                if (upload_file.length() < (1024 * 16)) {
                    _debug("Get a minislot!");
                    acquire_mini_slot = true;
                } else {
                    acquire_slot = true;
                }
            }
            if (acquire_mini_slot && _acquireMiniSlot()) {
                _debug("Got the mini slot, it's OK to proceed");
            } else if (acquire_slot && _acquireSlot()) {
                _debug("Got the slot, it's OK to proceed");
            } else {
                _debug("Couldn't get a slot, it's NOT OK to proceed");
                _sendMaxedOut();
                disconnect();
                return;
            }
            try {
                _setUserConnectionState(UserConnectionState.UL_WAITING_FOR_SEND);
            } catch (IllegalStateTransition ex) {
                Configuration.instance().executeExceptionCallback(ex);
                libLogger.debug("Exception GetCB", ex);
                return;
            }
            Long file_length = new Long(upload_file.length());
            _sendFileLength(file_length.toString());
        }
    }

    private class GetZBlockCB extends Callback {

        public GetZBlockCB() {
            super(_GETZBLOCK_CMD);
        }

        public void execute(Object data) {
            _debug("GetZBlockCB: Command " + _GETZBLOCK_CMD + " not yet supported...");
            _sendCommand(_ERROR_CMD, _GETZBLOCK_CMD + " not yet supported");
        }
    }

    /**
   * Handles the $FileLength command.
   */
    private class FileLengthCB extends Callback {

        public FileLengthCB() {
            super(_FILE_LENGTH_CMD);
        }

        public void execute(Object data) {
            try {
                _setUserConnectionState(UserConnectionState.DL_WAITING_FOR_FILE);
            } catch (IllegalStateTransition ex) {
                Configuration.instance().executeExceptionCallback(ex);
                libLogger.debug("Exception FileLengthCB", ex);
                return;
            }
            String file_length_str = (String) data;
            _downloadFileSize(Long.parseLong(file_length_str));
            _fileDownloadMode(true);
            _sendSend();
        }
    }

    /**
   * Handles the $ListLen command.
   */
    private class ListLengthCB extends Callback {

        public ListLengthCB() {
            super(_LIST_LEN_CMD);
        }

        public void execute(Object data) {
            try {
                _setUserConnectionState(UserConnectionState.DL_WAITING_FOR_FILELENGTH);
            } catch (IllegalStateTransition ex) {
                Configuration.instance().executeExceptionCallback(ex);
                libLogger.debug("Exception ListLengthCB", ex);
                return;
            }
            String list_length_str = (String) data;
            _downloadFileSize(Long.parseLong(list_length_str));
            if (_counterpart_supports_bzlist) _sendGet(_REQUEST_BZLIST_FILE_NAME); else _sendGet(_REQUEST_LIST_FILE_NAME);
        }
    }

    /**
   * Handles the $Send command.
   */
    private class SendCB extends Callback {

        public SendCB() {
            super(_SEND_CMD);
        }

        public void execute(Object data) {
            if (_upload_file_path != null) {
                try {
                    _setUserConnectionState(UserConnectionState.UL_UPLOADING);
                    _uploadFile(_upload_file_path);
                    _setUserConnectionState(UserConnectionState.NONE);
                } catch (IllegalStateTransition ex) {
                    Configuration.instance().executeExceptionCallback(ex);
                    libLogger.debug("Exception SendCB", ex);
                }
            } else _debug("Got a $Send, but there is no " + "info about which file to send...");
            _releaseSlot();
            _setUploadMode(UploadMode.NONE);
        }
    }

    private class MaxedOutCB extends Callback {

        public MaxedOutCB() {
            super(_MAXED_OUT_CMD);
        }

        public void execute(Object data) {
            _debug("Handling MaxedOut!!");
            _maxed_out = true;
            if (_dr != null && _dr.getActive() && (_dr.getStatus() == DownloadRequest._RUNNING || _dr.getStatus() == DownloadRequest._WAITING)) {
                EventDispatcher.instance().dispatchEvent(new DownloadEvent(_dr.getIdStr(), _dr, DownloadEvent.FAILED, DownloadEvent.REASON_USER_MAXED_OUT));
                _dispatchDLFailedEvents(DownloadEvent.REASON_USER_MAXED_OUT);
            }
            EventDispatcher.instance().dispatchEvent(new HandshakeEvent(_connected_to + "_HANDSHAKE", HandshakeEvent.NOT_OK, "MaxedOut"));
            disconnect();
        }
    }

    private class SupportsCB extends Callback {

        public SupportsCB() {
            super(_SUPPORTS_CMD);
        }

        public void execute(Object data) {
            String supports_str = (String) data;
            _debug("Counterpart supports: " + supports_str);
            if (supports_str.indexOf(_SUPPORTS_ARG_BZLIST) > -1) _counterpart_supports_bzlist = true;
            if (supports_str.indexOf(_SUPPORTS_ARG_CHUNK) > -1) _counterpart_supports_chunk = true;
            if (supports_str.indexOf(_SUPPORTS_ARG_MINISLOTS) > -1) _counterpart_supports_minislots = true;
            if (supports_str.indexOf(_SUPPORTS_ARG_GETZBLOCK) > -1) _counterpart_supports_getzblock = true;
            if (supports_str.indexOf(_SUPPORTS_ARG_GETTESTZBLOCK) > -1) _counterpart_supports_gettestzblock = true;
        }
    }

    /**
   * This callback is intended to be run when
   * a file download has been completed.
   */
    private class FileDownloadDoneCB extends Callback {

        public FileDownloadDoneCB() {
            super(FILE_DOWNLOAD_DONE_CB);
        }

        public void execute(Object data) {
            try {
                _debug("File download done, data: [" + data + "] ...");
                _setUserConnectionState(UserConnectionState.NONE);
                _setDownloadMode(DownloadMode.NONE);
                _fileDownloadMode(false);
                _user_cb_handler.execute(FILE_DOWNLOAD_DONE_CB, data);
            } catch (IllegalStateTransition ex) {
                Configuration.instance().executeExceptionCallback(ex);
                libLogger.debug("Exception FileDownloadedDoneCB", ex);
            }
        }
    }

    private class FileDownloadAbortedCB extends Callback {

        public FileDownloadAbortedCB() {
            super(FILE_DOWNLOAD_ABORTED_CB);
        }

        public void execute(Object data) {
            try {
                _debug("File download aborted, data: [" + data + "] ...");
                _setUserConnectionState(UserConnectionState.NONE);
                _setDownloadMode(DownloadMode.NONE);
                _fileDownloadMode(false);
                _user_cb_handler.execute(FILE_DOWNLOAD_ABORTED_CB, data);
            } catch (IllegalStateTransition ex) {
                Configuration.instance().executeExceptionCallback(ex);
                libLogger.debug("Exception FileDownloadedDoneCB", ex);
            }
        }
    }

    /**
   * This callback is intended to be run when
   * a file upload has been completed.
   */
    private class FileUploadDoneCB extends Callback {

        public FileUploadDoneCB() {
            super(FILE_UPLOAD_DONE_CB);
        }

        public void execute(Object data) {
            try {
                _setUserConnectionState(UserConnectionState.NONE);
                _setUploadMode(UploadMode.NONE);
                _upload_file_path = null;
                _user_cb_handler.execute(FILE_UPLOAD_DONE_CB, data);
            } catch (IllegalStateTransition ex) {
                Configuration.instance().executeExceptionCallback(ex);
                libLogger.debug("Exception FileUpladedDoneCB", ex);
            }
        }
    }

    private class FileUploadAbortedCB extends Callback {

        public FileUploadAbortedCB() {
            super(FILE_UPLOAD_ABORTED_CB);
        }

        public void execute(Object data) {
            _debug("in FileUploadAbortedCB");
            try {
                _setUserConnectionState(UserConnectionState.NONE);
                _setUploadMode(UploadMode.NONE);
                _upload_file_path = null;
                _user_cb_handler.execute(FILE_UPLOAD_ABORTED_CB, data);
            } catch (IllegalStateTransition ex) {
                Configuration.instance().executeExceptionCallback(ex);
                libLogger.debug("Exception FileUpladedDoneCB", ex);
            }
        }
    }

    /**
   * Takes the registered callback and propagates it as another callback.
   */
    private class PropagateCB extends Callback {

        /** CB that the data should be propagated as. */
        private String _send_cb;

        public PropagateCB(String register_cb, String send_cb) {
            super(register_cb);
            _send_cb = send_cb;
        }

        public void execute(Object data) {
            _user_cb_handler.execute(_send_cb, data);
        }
    }

    /**
   * Starts the handshake by sending data when we get connected if in
   * listen mode.
   */
    private class ConnectedCB extends Callback {

        public ConnectedCB() {
            super(CONNECTED_CB);
        }

        public void execute(Object data) {
            _debug("In ConnectedCB()");
            _debug("listen mode: " + _listen_mode + " passive mode: " + _passive);
            if (_listen_mode) {
                _debug("In listen mode; NOT initiating handshake.");
            } else {
                _debug("In connecting mode; Initiating handshake. Here goes...");
                _sendConnectorHandshakePart1();
            }
            _startDownloadHandler();
            _user_cb_handler.execute(CONNECTED_CB, data);
        }
    }

    private class ConnectionFailedCB extends Callback {

        public ConnectionFailedCB() {
            super(CONNECTION_FAILED_CB);
        }

        public void execute(Object data) {
            _debug("In ConnectionFailedCB()");
            Configuration.instance().executeErrorMessageCallback("Connection to host " + _host + " on port " + _port + " failed.");
            disconnect();
        }
    }

    /**
   * This exception is thrown if messages turn up
   * in an unexpected way.
   */
    private class IllegalStateTransition extends Exception {

        IllegalStateTransition(String msg) {
            super(msg);
        }
    }

    /**
   * Takes care of a download request and makes it into a file download.
   */
    private class DownloadHandler implements Runnable {

        public DownloadHandler() {
        }

        public void run() {
            boolean done = false;
            while (!done) {
                synchronized (_download_lock) {
                    while (!getUserConnectionState().equals(UserConnectionState.NONE)) {
                        try {
                            _download_lock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                    _dr = (DownloadRequest) _download_queue.dequeue();
                    _debug(this.toString() + "Handling download request: " + _dr);
                    _debug(this.toString() + "Acquiring handshake lock...");
                    try {
                        _handshake_lock.acquire();
                        _debug(this.toString() + "Got handshake lock; now it is to proceed with download");
                    } catch (InterruptedException ex) {
                        libLogger.error("Got interrupted when waiting for handshake lock! " + ex);
                    }
                    _handshake_lock.release();
                    _download_request_counter--;
                    String nick = _connected_to;
                    boolean list_mode = false;
                    if (_dr == null) {
                        list_mode = true;
                    } else {
                        list_mode = (_dr.getFileType() == DownloadRequest.FILE_TYPE_FILE_LIST);
                        nick = _dr.getOwnerNick();
                    }
                    if (_dr.getActive()) {
                        try {
                            if (list_mode) {
                                _setUserConnectionState(UserConnectionState.DL_WAITING_FOR_LISTLEN);
                                _setDownloadMode(DownloadMode.FILE_LIST_DL);
                                _sendGetListLen();
                            } else {
                                _setUserConnectionState(UserConnectionState.DL_WAITING_FOR_FILELENGTH);
                                _setDownloadMode(DownloadMode.NORMAL_FILE_DL);
                                _sendGet(_dr.getSourcePath());
                            }
                        } catch (IllegalStateTransition ex) {
                            Configuration.instance().executeExceptionCallback(ex);
                            libLogger.debug("Exception DownloadHandler", ex);
                            done = true;
                        }
                    } else {
                        _debug("DownloadRequest was inactive when retrieved from " + "queue, will try to proceed to the next item (if " + "any). Cancelled DownloadRequest: " + _dr);
                    }
                }
            }
            disconnect();
        }
    }

    /**
   * You need to call connect to create the actual connection.
   *
   * @param host Host to connect to or null for listening mode.
   * @param port Port to connect to or listen on.
   */
    public UserConnection(String host, int port) {
        super(host, port);
        _listen_mode = (host == null);
        _initMembers();
    }

    /**
   * This constructor is used by the UserConnectionListener when a new
   * connection is accepted.
   *
   * @param the_socket The socket that has the connection.
   */
    public UserConnection(Socket the_socket) {
        super(null, the_socket.getPort());
        _socket = the_socket;
        _listen_mode = true;
        _connected = true;
        _initMembers();
    }

    /**
   * Connects to other user. Only used for $ConnectToMe
   */
    public void connect() {
        super.connect();
    }

    /**
   * Disconnect user or from user.
   */
    public void disconnect() {
        _releaseSlot();
        UserConnectionHandler.instance().remove(this);
        if (!_maxed_out) {
            if (_dr != null && _dr.getActive() && (_dr.getStatus() == DownloadRequest._RUNNING || _dr.getStatus() == DownloadRequest._WAITING)) {
                EventDispatcher.instance().dispatchEvent(new DownloadEvent(_dr.getIdStr(), _dr, DownloadEvent.FAILED));
            }
            _dispatchDLFailedEvents(DownloadEvent.REASON_NONE);
        }
        super.disconnect();
    }

    public void disconnectNoThread() {
        _releaseSlot();
        UserConnectionHandler.instance().remove(this);
        super.disconnectNoThread();
    }

    /**
   * Returns the public callback handler.
   *
   * @return Public callback handler.
   */
    public CallbackHandler getCBHandler() {
        return _user_cb_handler;
    }

    /**
   * Returns the direction of this connection i.e Incoming or outgoing.
   * Incoming is for Uploads and outgoing for downloads.
   *
   * @return The direction.
   */
    public ConnectionDirection getDirection() {
        synchronized (_direction_mutex) {
            return _direction;
        }
    }

    /**
   * Enqueue a download request. The request will be processed in order.
   *
   * @param dr The download request.
   */
    public void download(DownloadRequest dr) {
        _debug("Enqueueing: " + dr);
        _download_request_counter++;
        _download_queue.enqueue(dr);
    }

    public DownloadMode getDownloadMode() {
        return _download_mode;
    }

    public UploadMode getUploadMode() {
        return _upload_mode;
    }

    public UserConnectionState getUserConnectionState() {
        synchronized (_userconnection_state_mutex) {
            return _userconnection_state;
        }
    }

    /**
   * Returns the user we are connected to.
   *
   * @return A nick.
   */
    public String getConnectedTo() {
        return _connected_to;
    }

    /**
   * Executes a file download
   *
   * @param reader
   * @param first_char
   *
   * @throws IOException
   * @throws FileNotFoundException
   */
    protected void _downloadFile(InputStream reader, byte first_char) throws IOException, java.io.FileNotFoundException {
        TransferStatus ts = null;
        long downloaded_so_far = 0;
        synchronized (_download_lock) {
            try {
                _setUserConnectionState(UserConnectionState.DL_DOWNLOADING);
                downloaded_so_far = 0;
                long file_size = _downloadFileSize();
                ts = new TransferStatus(_dr.getOwnerNick(), _dr.getDestinationPath(), _dr.getDRUCMapId(), TransferStatus._WAITING, TransferStatus._DOWNLOAD, file_size, 0);
                _dr.setStatus(DownloadRequest._RUNNING);
                _user_cb_handler.execute(UserConnectionHandler.TRANSFER_STATUS_CREATED_CB, ts);
                OutputStream os = null;
                File save_file = null;
                if (_dr.getFileType() == DownloadRequest.FILE_TYPE_NORMAL) {
                    save_file = new File(_dr.getDestinationPath());
                    save_file.createNewFile();
                    os = new FileOutputStream(save_file);
                } else {
                    os = new ByteArrayOutputStream((int) file_size);
                }
                int receiverBufferSize = (int) Configuration.instance().getReceiveBufferSize();
                byte[] read_arr = new byte[receiverBufferSize];
                read_arr[0] = first_char;
                os.write(read_arr, 0, 1);
                downloaded_so_far = 1;
                ts.setStatus_Progress(TransferStatus._RUNNING, downloaded_so_far);
                _debug("downloaded_so_far: " + downloaded_so_far);
                _debug("file_size: " + file_size);
                while (downloaded_so_far < file_size && _dr.getActive()) {
                    int nr_of_chars_read = reader.read(read_arr, 0, read_arr.length);
                    if (nr_of_chars_read == -1) break;
                    os.write(read_arr, 0, nr_of_chars_read);
                    downloaded_so_far += nr_of_chars_read;
                    ts.setProgress(downloaded_so_far);
                    _user_cb_handler.execute(FILE_DOWNLOAD_PROGRESS_CB, ts);
                }
                os.flush();
                os.close();
                _fileDownloadMode(false);
                _downloadFileSize(0);
                if (_dr.getActive()) {
                    _debug("File download complete...");
                    ts.setStatus_Progress(TransferStatus._FINISHED, downloaded_so_far);
                    Object data = save_file;
                    if (_dr.getFileType() == DownloadRequest.FILE_TYPE_FILE_LIST) {
                        data = _createTree((ByteArrayOutputStream) os);
                    } else {
                    }
                    _cb_handler.execute(FILE_DOWNLOAD_DONE_CB, new DownloadedFile(_dr, data));
                    EventDispatcher.instance().dispatchEvent(new DownloadEvent(_dr.getIdStr(), _dr, DownloadEvent.DONE));
                } else {
                    _debug("File download aborted...");
                    ts.setStatus_Progress(TransferStatus._ABORTED, downloaded_so_far);
                    _cb_handler.execute(FILE_DOWNLOAD_ABORTED_CB, _dr);
                    EventDispatcher.instance().dispatchEvent(new DownloadEvent(_dr.getIdStr(), _dr, DownloadEvent.ABORTED));
                    disconnectNoThread();
                    _dispatchRelocateDLQueueEvent();
                }
            } catch (IOException ioEx) {
                ts.setStatus_Progress(TransferStatus._FAILURE, downloaded_so_far);
                EventDispatcher.instance().dispatchEvent(new DownloadEvent(_dr.getIdStr(), _dr, DownloadEvent.FAILED, DownloadEvent.REASON_CONNECTION_CLOSED));
            } catch (IllegalStateTransition ex) {
                Configuration.instance().executeExceptionCallback(ex);
                ts.setStatus_Progress(TransferStatus._FAILURE, downloaded_so_far);
                EventDispatcher.instance().dispatchEvent(new DownloadEvent(_dr.getIdStr(), _dr, DownloadEvent.FAILED));
                libLogger.debug("Exception _downloadFile", ex);
            } finally {
                checkTime cleaner = new checkTime(10, ts, "ts cleaner");
                try {
                    _setUserConnectionState(UserConnectionState.NONE);
                } catch (IllegalStateTransition ex) {
                    Configuration.instance().executeExceptionCallback(ex);
                    libLogger.debug("Exception _downloadFile", ex);
                }
                _download_lock.notifyAll();
            }
        }
    }

    private TreeHandler.Node _createTree(ByteArrayOutputStream os) throws IOException {
        jdc.util.Decoder dec = new jdc.util.Decoder();
        String raw_tree = "";
        StringBuffer file_contents = new StringBuffer("");
        if (_counterpart_supports_bzlist) {
            ByteArrayInputStream byte_is = new ByteArrayInputStream(os.toByteArray());
            int next_char = byte_is.read();
            if (next_char != 'B') {
                throw new IOException("Invalid bz2 list.");
            }
            next_char = byte_is.read();
            if (next_char != 'Z') {
                throw new IOException("Invalid bz2 list.");
            }
            CBZip2InputStream bz2 = new CBZip2InputStream(byte_is);
            while ((next_char = bz2.read()) != -1) {
                file_contents.append((char) next_char);
            }
            bz2.close();
            raw_tree = file_contents.toString();
        } else raw_tree = dec.decode(os.toByteArray());
        return TreeHandler.instance().parse(raw_tree, _connected_to);
    }

    /**
   * Start the download handler thread.
   */
    private void _startDownloadHandler() {
        _download_handler_thread = new Thread(new DownloadHandler());
        _download_handler_thread.start();
    }

    /**
   * Sends users nickname.
   */
    private void _sendMyNick() {
        _sendCommand(_MY_NICK_CMD, Configuration.instance().getUser().getNick());
    }

    /**
   * Create a lock and send it to the client.
   *
   */
    private void _sendLock() {
        _lock = KeyHandler.instance().generateLock();
        _sendCommand(_LOCK_CMD, _lock);
    }

    /**
   * Takes the supplied lock, calculates a key and sends that.
   *
   * @param lock The supplied lock.
   */
    private void _sendKey(String lock) {
        _sendCommand(_KEY_CMD, KeyHandler.instance().generateKey(lock));
    }

    private void _sendDirection() {
        synchronized (_direction_mutex) {
            _sendCommand(_DIRECTION_CMD, _direction_cmd);
        }
    }

    private void _sendListLen(String list_len) {
        _sendCommand(_LIST_LEN_CMD, list_len);
    }

    /**
   * Sends the GetListLen command.
   */
    private void _sendGetListLen() {
        _sendCommand(_GET_LIST_LEN_CMD, null);
    }

    private void _sendGet(String file_to_request) {
        String get_arg = file_to_request + "$1";
        _sendCommand(_GET_CMD, get_arg);
    }

    private void _sendFileLength(String file_length) {
        _sendCommand(_FILE_LENGTH_CMD, file_length);
    }

    private void _sendSend() {
        _sendCommand(_SEND_CMD, null);
    }

    private void _sendMaxedOut() {
        _sendCommand(_MAXED_OUT_CMD, null);
    }

    private void _sendSupports() {
        if (_counterpart_has_extended_protocol) {
            _sendCommand(_SUPPORTS_CMD, _SUPPORTS_ARG_BZLIST + " " + _SUPPORTS_ARG_MINISLOTS + " ");
        }
    }

    protected synchronized void _setUserConnectionState(UserConnectionState new_state) throws IllegalStateTransition {
        synchronized (_userconnection_state_mutex) {
            boolean legal = UserConnectionStateTransition.instance().legalStateTransition(_userconnection_state, new_state);
            if (legal) {
                _debug("Setting _userconnection_state to " + new_state.toString());
                _userconnection_state = new_state;
            } else {
                String msg = new String("Illegal state transition: ");
                msg += _userconnection_state.toString();
                msg += " -> ";
                msg += new_state.toString();
                throw new IllegalStateTransition(msg);
            }
        }
    }

    protected void _setDownloadMode(DownloadMode new_mode) {
        synchronized (_download_mode) {
            _download_mode = new_mode;
        }
    }

    protected void _setUploadMode(UploadMode new_mode) {
        synchronized (_upload_mode) {
            _upload_mode = new_mode;
        }
    }

    protected void _setupValidCBs() {
        _user_cb_handler = new CallbackHandler();
        _cb_handler.addValidEventType(_MY_NICK_CMD);
        _cb_handler.addValidEventType(_LOCK_CMD);
        _cb_handler.addValidEventType(_DIRECTION_CMD);
        _cb_handler.addValidEventType(_KEY_CMD);
        _cb_handler.addValidEventType(_GET_CMD);
        _cb_handler.addValidEventType(_GET_LIST_LEN_CMD);
        _cb_handler.addValidEventType(_SEND_CMD);
        _cb_handler.addValidEventType(_LIST_LEN_CMD);
        _cb_handler.addValidEventType(_FILE_LENGTH_CMD);
        _cb_handler.addValidEventType(_MAXED_OUT_CMD);
        _cb_handler.addValidEventType(_SUPPORTS_CMD);
        _cb_handler.addValidEventType(FILE_DOWNLOAD_DONE_CB);
        _cb_handler.addValidEventType(FILE_DOWNLOAD_ABORTED_CB);
        _cb_handler.addValidEventType(FILE_DOWNLOAD_PROGRESS_CB);
        _cb_handler.addValidEventType(FILE_UPLOAD_DONE_CB);
        _cb_handler.addValidEventType(FILE_UPLOAD_ABORTED_CB);
        _cb_handler.addValidEventType(FILE_UPLOAD_PROGRESS_CB);
        _user_cb_handler.addValidEventType(UserConnectionHandler.TRANSFER_STATUS_CREATED_CB);
        _user_cb_handler.addValidEventType(UserConnectionHandler.TRANSFER_STATUS_UPDATED_CB);
        _user_cb_handler.addValidEventType(UserConnectionHandler.TRANSFER_STATUS_REMOVED_CB);
        _user_cb_handler.addValidEventType(FILE_DOWNLOAD_DONE_CB);
        _user_cb_handler.addValidEventType(FILE_DOWNLOAD_ABORTED_CB);
        _user_cb_handler.addValidEventType(FILE_DOWNLOAD_PROGRESS_CB);
        _user_cb_handler.addValidEventType(FILE_UPLOAD_DONE_CB);
        _user_cb_handler.addValidEventType(FILE_UPLOAD_ABORTED_CB);
        _user_cb_handler.addValidEventType(FILE_UPLOAD_PROGRESS_CB);
    }

    protected void _registerCallbacks() {
        _cb_handler.add(new MyNickCB());
        _cb_handler.add(new LockCB());
        _cb_handler.add(new DirectionCB(this));
        _cb_handler.add(new KeyCB());
        _cb_handler.add(new GetCB());
        _cb_handler.add(new SendCB());
        _cb_handler.add(new FileLengthCB());
        _cb_handler.add(new MaxedOutCB());
        _cb_handler.add(new SupportsCB());
        _cb_handler.add(new ListLengthCB());
        _cb_handler.add(new FileDownloadDoneCB());
        _cb_handler.add(new FileDownloadAbortedCB());
        _cb_handler.add(new FileUploadDoneCB());
        _cb_handler.add(new FileUploadAbortedCB());
        _cb_handler.add(new GetListLenCB());
    }

    protected synchronized void _uploadFile(String file_path) {
        File upload_file = null;
        TransferStatus ts = null;
        if (getUploadMode().equals(UploadMode.FILE_LIST_UL)) {
            _debug("Uploading my file list...");
            if (file_path.equals("MyList.bz2")) {
                upload_file = Configuration.instance().getUser().getShareStorage().getMyBZipCompressedList();
            } else {
                upload_file = Configuration.instance().getUser().getShareStorage().getMyCompressedList();
            }
        } else {
            _debug("Uploading " + file_path + "...");
            upload_file = new File(file_path);
        }
        if (!upload_file.canRead() || !upload_file.exists() || !upload_file.isFile() || upload_file.length() == 0L) {
            libLogger.error("upload_file.canRead(): " + upload_file.canRead());
            libLogger.error("upload_file.exists(): " + upload_file.exists());
            libLogger.error("upload_file.isFile(): " + upload_file.isFile());
            libLogger.error("upload_file.length(): " + upload_file.length());
            libLogger.error("This file is rotten...");
            disconnect();
            return;
        }
        int sendBufferSize = (int) Configuration.instance().getSendBufferSize();
        synchronized (_socket) {
            byte byte_arr[] = new byte[sendBufferSize];
            long uploaded_so_far = 0;
            long file_size = upload_file.length();
            int nr_of_chars_read = 0;
            UploadListEntry ull_entry = new UploadListEntry(_connected_to, upload_file.getName());
            UploadList.instance().put(ull_entry);
            ts = new TransferStatus(_connected_to, upload_file.getName(), ull_entry.id(), TransferStatus._WAITING, TransferStatus._UPLOAD, file_size, 0);
            _user_cb_handler.execute(UserConnectionHandler.TRANSFER_STATUS_CREATED_CB, ts);
            ts.setStatus_Progress(TransferStatus._RUNNING, uploaded_so_far);
            try {
                OutputStream out = _socket.getOutputStream();
                FileInputStream in = new FileInputStream(upload_file);
                while (ull_entry.active() && uploaded_so_far < file_size) {
                    nr_of_chars_read = in.read(byte_arr, 0, sendBufferSize);
                    if (nr_of_chars_read == -1) break;
                    out.write(byte_arr, 0, nr_of_chars_read);
                    uploaded_so_far += nr_of_chars_read;
                    ts.setProgress(uploaded_so_far);
                    _user_cb_handler.execute(UserConnectionHandler.FILE_UPLOAD_PROGRESS_CB, ts);
                }
                if (ull_entry.active()) {
                    _debug("  ... Done!");
                    ts.setStatus_Progress(TransferStatus._FINISHED, uploaded_so_far);
                    _cb_handler.execute(FILE_UPLOAD_DONE_CB, ull_entry);
                } else {
                    _debug("  ... Aborted!");
                    ts.setStatus_Progress(TransferStatus._ABORTED, uploaded_so_far);
                    _cb_handler.execute(FILE_UPLOAD_ABORTED_CB, ull_entry);
                    disconnect();
                }
            } catch (java.io.IOException e) {
                ts.setStatus_Progress(TransferStatus._FAILURE, uploaded_so_far);
                _user_cb_handler.execute(UserConnectionHandler.FILE_UPLOAD_PROGRESS_CB, ts);
                _debug("  ... Failed.");
                libLogger.error(e);
            } finally {
                checkTime cleaner = new checkTime(5, ts, "ts cleaner");
                UploadList.instance().remove(ull_entry);
                _releaseSlot();
            }
        }
    }

    protected boolean _acquireSlot() {
        synchronized (_acquired_slot) {
            boolean ret_val = false;
            if (_acquired_slot.equals(Boolean.FALSE)) {
                if (Configuration.instance().acquireSlot()) {
                    _acquired_slot = Boolean.TRUE;
                    ret_val = true;
                } else {
                }
            } else _debug("Already have a slot; this should NEVER happen!");
            _debug("Acquired slot: " + ret_val);
            return ret_val;
        }
    }

    protected boolean _releaseSlot() {
        boolean ret_val = false;
        synchronized (_acquired_slot) {
            if (_acquired_slot.equals(Boolean.TRUE)) {
                Configuration.instance().releaseSlot();
                _acquired_slot = Boolean.FALSE;
                ret_val = true;
                _debug("Released slot");
            } else _debug("Haven't acquired a slot, so no release will be done");
        }
        synchronized (_acquired_mini_slot) {
            if (_acquired_mini_slot.equals(Boolean.TRUE)) {
                Configuration.instance().releaseMiniSlot();
                _acquired_mini_slot = Boolean.FALSE;
                ret_val = true;
                _debug("Released mini slot");
            } else _debug("Haven't acquired a mini slot, so no release will be done");
        }
        return ret_val;
    }

    protected boolean _acquireMiniSlot() {
        synchronized (_acquired_mini_slot) {
            boolean ret_val = false;
            if (_acquired_mini_slot.equals(Boolean.FALSE)) {
                if (Configuration.instance().acquireMiniSlot()) {
                    _acquired_mini_slot = Boolean.TRUE;
                    ret_val = true;
                } else {
                }
            } else _debug("Already have a mini slot; this should NEVER happen!");
            _debug("Acquired mini slot: " + ret_val);
            return ret_val;
        }
    }

    protected void _initMembers() {
        _log_communication = true;
        _passive = Configuration.instance().getUser().passiveConnection();
        ConnectionTimer connection_timer = new ConnectionTimer(this);
        checkTime cleaner = new checkTime(1, 1, connection_timer, "uc idle timeout");
        _cb_handler.add(new ConnectedCB());
        _cb_handler.add(new ConnectionFailedCB());
        _cb_handler.owner(this.toString() + "::_cb_handler");
        _user_cb_handler.owner(this.toString() + "::_cb_handler");
    }

    protected void _initDirection() {
        synchronized (_direction_mutex) {
            int no_pending_dls = _download_request_counter;
            if (no_pending_dls == 0) no_pending_dls = DownloadRequestCounter.instance().value(_connected_to);
            String direction_cmd = (no_pending_dls > 0 ? "Download" : "Upload");
            _direction = (direction_cmd.compareTo("Upload") == 0) ? ConnectionDirection.OUTGOING : ConnectionDirection.INCOMING;
            _direction_rdm_int = _random_generator.nextInt(32767);
            _direction_cmd = direction_cmd + " " + Integer.toString(_direction_rdm_int);
        }
    }

    protected String _directionCmd() {
        synchronized (_direction_mutex) {
            return _direction_cmd;
        }
    }

    protected int _directionRdmInt() {
        synchronized (_direction_mutex) {
            return _direction_rdm_int;
        }
    }

    protected void _dispatchDLFailedEvents(int download_event_reason) {
        Iterator dl_reqs = _download_queue.queueContents();
        while (dl_reqs.hasNext()) {
            DownloadRequest dr = (DownloadRequest) dl_reqs.next();
            EventDispatcher.instance().dispatchEvent(new DownloadEvent(dr.getIdStr(), dr, DownloadEvent.FAILED, download_event_reason));
        }
    }

    protected void _dispatchRelocateDLQueueEvent() {
        Vector dl_reqs = _download_queue.queueContentsVector();
        EventDispatcher.instance().dispatchEvent(new UserConnectionEvent(_connected_to + "_USER_CONNECTION_EVENTS", dl_reqs, UserConnectionEvent.RELOCATE_DL_QUEUE));
    }

    protected void _sendConnectorHandshakePart1() {
        _sendMyNick();
        _sendLock();
    }

    protected void _sendConnectorHandshakePart2(String listeners_lock) {
        _sendSupports();
        _sendDirection();
        _sendKey(listeners_lock);
    }

    protected void _sendListenerHandshakePart1(String connectors_lock) {
        _sendMyNick();
        _sendLock();
        _sendSupports();
        _sendDirection();
        _sendKey(connectors_lock);
    }

    protected void _debug(String msg) {
        libLogger.debug(this.getClass().getName() + " [_userconnection_state]: " + _userconnection_state + "  " + msg);
    }
}
