package com.google.code.jouka.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonReadableChannelException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import java.util.HashMap;

/**
 * <PRE>
 * A useful FileChannel utilities.
 *
 * </PRE>
 *
 * @author Str.Isym
 *
 */
public class FileChUtil {

    /**
     * バッファ取得の際、ByteBuffer#allocate()を使う場合に指定。<br>
     * "java.nio.ByteBuffer.allocate(int capacity)" を使う。
     * 引数に指定されたバイト数の作業領域を確保したByteBufferオブジェクトを生成する。
     * このメソッドで生成されたByteBufferオブジェクトは、通常のJavaオブジェクトと
     * 同様に処理を行う。
     */
    public static final int USE_ALLOCATED_BUFFER = 0;

    /**
     * バッファ取得の際、ByteBuffer#allocateDirect()を使う場合に指定。<br>
     * "java.nio.ByteBuffer.allocateDirect(int capacity)"を使う。
     * 引数に指定されたバイト数の作業領域を確保したByteBufferオブジェクトを生成する。
     * このメソッドで生成されたByteBufferオブジェクトは、ヒープ外のメモリを使う。
     * プラットフォームネイティブの入出力操作を直接実行するダイレクトバッファを用いるため、
     * 非ダイレクトバッファよりも高速処理が可能がだ、割り当て/解放コストがやや高い。
     * 一般にダイレクトバッファの割り当ては、プログラムの性能を十分に改善できる
     * 見込みがある場合にのみ、行うべきとされる。
     */
    public static final int USE_DIRECT_ALLOCATED_BUFFER = 1;

    /**
     * バッファ取得の際、MappedByteBuffer(READ_ONLY)を使う場合に指定。<br>
     * "java.nio.channels.FileChannel.MapMode.READ_ONLY" を使う。
     * 読み取り専用のByteBufferオブジェクトが生成される。
     * ファイルのメモリマップ領域を内容とするダイレクトByteBufferを介す。
     * マップされたByteBufferと、これによって表されるファイルマッピングは、
     * バッファ自体がガベージコレクトされるまで有効。
     * ByteBufferオブジェクトに対して修正を加えようとすると、
     * 例外：ReadOnlyBufferExceptionがthrowされる。
     */
    public static final int USE_MAPMODE_READ_ONLY = 2;

    /**
     * バッファ取得の際、MappedByteBuffer(READ_WRITE)を使う場合に指定。<br>
     * "java.nio.channels.FileChannel.MapMode.READ_WRITE" を使う。
     * 書き込み可能なByteBufferオブジェクトが生成される。
     * ファイルのメモリマップ領域を内容とするダイレクトByteBufferを介す。
     * マップされたByteBufferと、これによって表されるファイルマッピングは、
     * バッファ自体がガベージコレクトされるまで有効。
     * 変更した内容は最終的に元のファイルに反映される
     */
    public static final int USE_MAPMODE_READ_WRITE = 3;

    /**
     * バッファ取得の際、ByteBuffer#wrap(byte[] array)を使う場合に指定。<br>
     * バッファに変更を加えると配列も変更され、配列に変更を加えるとバッファも変更される。
     */
    public static final int USE_WRAPPED_BYTE_ARRAY = 4;

    private static final String[] buffTypes = { "FileChUtil.USE_ALLOCATED_BUFFER", "FileChUtil.USE_DIRECT_ALLOCATED_BUFFER", "FileChUtil.USE_MAPMODE_READ_ONLY", "FileChUtil.USE_MAPMODE_READ_WRITE", "FileChUtil.USE_WRAPPED_BYTE_ARRAY" };

    private static final FileChannel.MapMode[] mapmode = { null, null, FileChannel.MapMode.READ_ONLY, FileChannel.MapMode.READ_WRITE, null };

    /** FileChannel取得用の読み書き属性値 */
    private static final String MODE_READ = "r";

    private static final String MODE_WRITE = "w";

    private static final String MODE_READ_WRITE = "rw";

    private static final String MODE_APPEND = "a";

    /**
     * FileChannelの取得元となるFileStream自体を、FileChannelのhashcodeと共に保存するマップ。
     * channelのclose時に、取得元のFileStreamも併せてcloseさせるために用いる。
     * 取得元FileStreamのclose後は、FileStreamもHashMapから削除される。
     */
    private static HashMap<Integer, Object> objectMap = null;

    /**
     * FileChannelの取得元となるFileStreamの種類を、FileChannelのhashcodeと共に保存するマップ。
     * channelのclose時の取得元FileStreamのcloseと併せて、FileStream種類もHashMapから削除される。
     */
    private static HashMap<Integer, Integer> attrCodeMap = null;

    private static final int FIS = 0;

    private static final int FOS_W = 1;

    private static final int FOS_A = 2;

    private static final int RAF = 3;

    private static void setObjectMap(int hashCode, Object object, int attrCode) {
        if (objectMap == null) {
            objectMap = new HashMap<Integer, Object>();
        }
        if (attrCodeMap == null) {
            attrCodeMap = new HashMap<Integer, Integer>();
        }
        objectMap.put(hashCode, object);
        attrCodeMap.put(hashCode, attrCode);
    }

    /**
     * <pre>
     * Fileオブジェクトから入出力のFileChannelを取得する。
     *   rw = "r":
     *     return (new FileInputStream(file).getChannel())
     *
     *   rw = "w":
     *     return (new FileOutputStream(file).getChannel())
     *     ※ ファイルが既存であった場合、同名のゼロバイトの新規ファイルがとして作成される。
     *     * got channel is overwritable if file is exist.
     *
     *   rw = "rw":
     *     return (new RandomAccessFile(file, rw).getChannel)
     *
     *   rw = "a":
     *     return (new FileOutputStream(file, true).getChannel())
     *     ※ ファイルが既存であった場合、追記可能とする。
     *     * got channel is appendable if file is exist.
     *
     *
     * Example:
     *     File file = new File("C:\\WINDOWS\\explorer.exe");
     *     FileChannel ch = FileChUtil.newFileChannel(file, "r", true);
     *     System.out.println("File Size = " + ch.size() + "bytes.");
     *
     *
     * </pre>
     *
     * @param file 対象ファイル
     * @param rw 入出力の区分。入力専用="r"、出力専用="w"、入出力用="rw"、追記用="a"
     * @param enableException ファイルが存在しない場合に、明示的に例外を発生させたい場合はtrue。例外を発生させずNULLを返すだけならfalse。
     * @return 取得したFileChannel
     * @throws IOException
     * @see #newFileChannel(String , String , boolean)
     * @see #closeQuietly(FileChannel, boolean)
     * @see #closeQuietly(FileChannel)
     */
    public static FileChannel newFileChannel(File file, String rw, boolean enableException) throws IOException {
        if (file == null) return null;
        if (rw == null || rw.length() == 0) {
            return null;
        }
        rw = rw.toLowerCase();
        if (rw.equals(MODE_READ)) {
            if (FileUtil.exists(file, enableException)) {
                FileInputStream fis = new FileInputStream(file);
                FileChannel ch = fis.getChannel();
                setObjectMap(ch.hashCode(), fis, FIS);
                return ch;
            }
        } else if (rw.equals(MODE_WRITE)) {
            FileOutputStream fos = new FileOutputStream(file);
            FileChannel ch = fos.getChannel();
            setObjectMap(ch.hashCode(), fos, FOS_W);
            return ch;
        } else if (rw.equals(MODE_APPEND)) {
            if (FileUtil.exists(file, enableException)) {
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                FileChannel ch = raf.getChannel();
                ch.position(ch.size());
                setObjectMap(ch.hashCode(), raf, FOS_A);
                return ch;
            }
        } else if (rw.equals(MODE_READ_WRITE)) {
            if (FileUtil.exists(file, enableException)) {
                RandomAccessFile raf = new RandomAccessFile(file, rw);
                FileChannel ch = raf.getChannel();
                setObjectMap(ch.hashCode(), raf, RAF);
                return ch;
            }
        } else {
            throw new IllegalArgumentException("Illegal read/write type : [" + rw + "]\n" + "You can use following types for: \n" + "  (1) Read Only  = \"r\"\n" + "  (2) Write Only = \"w\"\n" + "  (3) Read/Write = \"rw\"\n" + "  (4) Append     = \"a\"");
        }
        return null;
    }

    /**
     * <pre>
     * ファイル名文字列から入出力のFileChannelを取得する。
     * return (getNewFileChannel(new File(fileName), rw).getChannel)
     *
     * Example:
     *     FileChannel ch = FileChUtil.newFileChannel("C:\\WINDOWS\\explorer.exe", "r", true);
     *     System.out.println("File Size = " + ch.size() + "bytes.");
     *
     * </pre>
     * @param fileName
     * @param rw
     * @param enableException
     * @return 取得したFileChannel
     * @throws IOException
     * @see #newFileChannel(File , String , boolean)
     * @see #closeQuietly(FileChannel, boolean)
     * @see #closeQuietly(FileChannel)
     */
    public static FileChannel newFileChannel(String fileName, String rw, boolean enableException) throws IOException {
        return newFileChannel(new File(fileName), rw, enableException);
    }

    /**
     * <pre>
     *バッファサイズ、バッファタイプを指定して、FileChannel#position()=0から読み込み用のByteBufferを取得する。
     * </pre>
     *
     * @param channel
     * @param bufferType
     * @param bufferSize
     * @return 取得したByteBuffer
     * @throws IOException
     * @see #getReadBuffer(FileChannel , long , int , int )
     */
    public static ByteBuffer getReadBuffer(FileChannel channel, int bufferType, int bufferSize) throws IOException {
        return getReadBuffer(channel, 0, bufferType, bufferSize);
    }

    /**
     * <pre>
     * バッファサイズ、バッファタイプ、チャネルの位置を指定して、FileChannelから読み込み用のByteBufferを取得する。
     *
     * --- バッファタイプにより取得されるバッファ ----
     * bufferType = USE_ALLOCATED_BUFFER        ; return ByteBuffer.allocate((int)channel.size())
     * bufferType = USE_DIRECT_ALLOCATED_BUFFER ; return ByteBuffer.allocateDirect((int)channel.size())
     * bufferType = USE_MAPMODE_READ_ONLY       ; return channel.map(FileChannel.MapMode. READ_ONLY,  chPosition, channel.size())
     * bufferType = USE_MAPMODE_READ_WRITE      ; return channel.map(FileChannel.MapMode. READ_WRITE, chPosition, channel.size())
     *
     *
     * </pre>
     *
     * @param channel FileChannel
     * @param initChPosition バッファ取得時に設定されるFileChannelのposition
     * @param bufferType 取得したいバッファの種類
     * @param bufferSize バッファサイズ
     * @return 取得したBuffer
     * @throws IOException
     */
    public static ByteBuffer getReadBuffer(FileChannel channel, long initChPosition, int bufferType, int bufferSize) throws IOException {
        channel.position(initChPosition);
        switch(bufferType) {
            case USE_ALLOCATED_BUFFER:
                return ByteBuffer.allocate(bufferSize);
            case USE_DIRECT_ALLOCATED_BUFFER:
                return ByteBuffer.allocateDirect(bufferSize);
            case USE_MAPMODE_READ_ONLY:
                if (channel.size() < bufferSize) {
                    bufferSize = (int) channel.size();
                }
                return channel.map(FileChannel.MapMode.READ_ONLY, initChPosition, bufferSize);
            case USE_MAPMODE_READ_WRITE:
                if (channel.size() < bufferSize) {
                    bufferSize = (int) channel.size();
                }
                return channel.map(FileChannel.MapMode.READ_WRITE, initChPosition, bufferSize);
            default:
                throw new IOException(getMessageForReadBufferTypeError(bufferType));
        }
    }

    /**
     * <pre>
     * FileChannelから出力用のByteBufferを取得する。
     * その際、バッファサイズが指定できる。
     * 小さなバッファを用い、FileChannelに対して繰り返し処理でデータ出力をする場合に利用する。
     *
     * --- バッファタイプにより取得されるバッファ ----
     * bufferType = USE_ALLOCATED_BUFFER        ; return ByteBuffer.allocate(bufferSize)
     * bufferType = USE_DIRECT_ALLOCATED_BUFFER ; return ByteBuffer.allocateDirect(bufferSize)
     * bufferType = USE_MAPMODE_READ_WRITE      ; return channel.map(FileChannel.MapMode.READ_WRITE, 0, bufferSize);
     *
     * </pre>
     *
     * @param channel
     * @param bufferType
     * @param bufferSize
     * @return 取得したByteBuffer
     * @throws IOException
     */
    public static ByteBuffer getWriteBuffer(FileChannel channel, int bufferType, int bufferSize) throws IOException {
        return getWriteBuffer(channel, 0, bufferType, bufferSize);
    }

    /**
     * <pre>
     * FileChannelから出力用のByteBufferを取得する。
     * その際、バッファサイズと対象FileChannelのpositionが指定できる。
     * 小さなバッファを用い、FileChannelに対して繰り返し処理でデータ出力をする場合に利用する。
     *
     * --- バッファタイプにより取得されるバッファ ----
     * bufferType = USE_ALLOCATED_BUFFER        ; return ByteBuffer.allocate(bufferSize)
     * bufferType = USE_DIRECT_ALLOCATED_BUFFER ; return ByteBuffer.allocateDirect(bufferSize)
     * bufferType = USE_MAPMODE_READ_WRITE      ; return channel.map(FileChannel.MapMode.READ_WRITE, chPosition, bufferSize);
     *
     * </pre>
     *
     * @param channel
     * @param initChPosition FileChannelのposition
     * @param bufferType
     * @param bufferSize
     * @return 取得したByteBuffer
     * @throws IOException
     */
    public static ByteBuffer getWriteBuffer(FileChannel channel, long initChPosition, int bufferType, int bufferSize) throws IOException {
        checkWritable(channel);
        switch(bufferType) {
            case USE_ALLOCATED_BUFFER:
                channel.position(initChPosition);
                return ByteBuffer.allocate(bufferSize);
            case USE_DIRECT_ALLOCATED_BUFFER:
                channel.position(initChPosition);
                return ByteBuffer.allocateDirect(bufferSize);
            case USE_MAPMODE_READ_WRITE:
                MappedByteBuffer buff = null;
                try {
                    buff = channel.map(FileChannel.MapMode.READ_WRITE, initChPosition, bufferSize);
                } catch (NonReadableChannelException e) {
                    throw new IOException("[java.nio.channels.NonReadableChannelException] occured.\n" + "If you call getWriteBuffer() with bufferType=USE_MAPMODE_READ_WRITE ,\n" + "you must to use 'FileChUtil.newFileChannel(arg1, \"RW\", arg3)'.\n" + "--------");
                }
                channel.position(initChPosition);
                return buff;
            case USE_MAPMODE_READ_ONLY:
                break;
            case USE_WRAPPED_BYTE_ARRAY:
                break;
            default:
                break;
        }
        throw new IOException(getMessageForWriteBufferTypeError(bufferType));
    }

    /**
     * <pre>
     * getWriteBuffer()用のFileChannelが書き出し用かどうかをチェックする
     * </pre>
     *
     * @param channel
     * @throws IOException
     */
    private static void checkWritable(FileChannel channel) throws IOException {
        if (channel == null) {
            throw new NullPointerException("channel is NULL.");
        }
        boolean foundChannelError = false;
        String channelErrMsg = null;
        int chAttrCode = attrCodeMap.get(channel.hashCode());
        switch(chAttrCode) {
            case FIS:
                foundChannelError = true;
                channelErrMsg = "channel is for READ.\n" + "This channel is derived from [FileChUtil.newFileChannel(file, \"R\" , true/false)].\n";
                break;
            case FOS_W:
                break;
            case FOS_A:
                foundChannelError = true;
                channelErrMsg = "channel is for APPEND.\n" + "This channel is derived from [FileChUtil.newFileChannel(file, \"A\" , true/false)].\n";
                channelErrMsg = channelErrMsg + "----";
                break;
            default:
                break;
        }
        if (foundChannelError) {
            throw new IOException(channelErrMsg + "If you get write-buffer, you need to use writable channel.\n" + " 1) FileChUtil.newFileChannel(file, \"W\" , true/false)\n" + " 2) FileChUtil.newFileChannel(file, \"RW\", true/false)\n" + "--------");
        }
    }

    /**
     * <pre>
     * 追記用のByteBufferを取得する。
     *
     * </pre>
     *
     * @param channel
     * @param bufferType
     * @param bufferSize
     * @return 取得したByteBuffer
     * @throws IOException
     */
    public static ByteBuffer getAppendBuffer(FileChannel channel, int bufferType, int bufferSize) throws IOException {
        checkAppendable(channel, bufferType);
        long chPosition = channel.size();
        switch(bufferType) {
            case USE_ALLOCATED_BUFFER:
                channel.position(chPosition);
                return ByteBuffer.allocate(bufferSize);
            case USE_DIRECT_ALLOCATED_BUFFER:
                channel.position(chPosition);
                return ByteBuffer.allocateDirect(bufferSize);
            case USE_MAPMODE_READ_WRITE:
                MappedByteBuffer buff = channel.map(FileChannel.MapMode.READ_WRITE, chPosition, bufferSize);
                channel.position(chPosition);
                return buff;
            case USE_MAPMODE_READ_ONLY:
                break;
            case USE_WRAPPED_BYTE_ARRAY:
                break;
            default:
                break;
        }
        throw new IOException(getMessageForAppendBufferTypeError(bufferType));
    }

    /**
     * <pre>
     * getAppendBuffer()用のFileChannelが追記用かどうかをチェックする
     * </pre>
     *
     * @param channel
     * @param bufferType
     * @throws IOException
     */
    private static void checkAppendable(FileChannel channel, int bufferType) throws IOException {
        if (channel == null) {
            throw new NullPointerException("channel is NULL.");
        }
        boolean foundChannelError = false;
        String channelErrMsg = null;
        int chAttrCode = attrCodeMap.get(channel.hashCode());
        switch(chAttrCode) {
            case FIS:
                foundChannelError = true;
                channelErrMsg = "channel is for READ.\n" + "This channel is derived from [FileChUtil.newFileChannel(file, \"R\" , true/false)].\n";
                break;
            case FOS_W:
                foundChannelError = true;
                channelErrMsg = "channel is for (OVER)WRITE.\n" + "This channel is derived from [FileChUtil.newFileChannel(file, \"W\" , true/false)].\n";
                break;
            case FOS_A:
                if (bufferType == USE_MAPMODE_READ_WRITE) {
                    throw new IOException("Miss match of channel and bufferType.\n" + "if you use channel :\n" + "    [FileChUtil.newFileChannel(file, \"A\" , true/false)].\n" + "    you must to choice bufferType is :\n" + "        [USE_ALLOCATED_BUFFER] or [USE_DIRECT_ALLOCATED_BUFFER].\n" + "OR if you use bufferType :\n" + "    [USE_MAPMODE_READ_WRITE]\n" + "    you must to use channel :\n" + "        [FileChUtil.newFileChannel(file, \"RW\" , true/false)].\n" + "--------");
                }
                break;
            default:
                break;
        }
        if (foundChannelError) {
            throw new IOException(channelErrMsg + "If you get append-buffer, you need to use appendable channel.\n" + " 1) FileChUtil.newFileChannel(file, \"A\" , true/false)\n" + " 2) FileChUtil.newFileChannel(file, \"RW\", true/false)\n" + "--------");
        }
    }

    /**
     * <pre>
     * FileChannelにロックをおこなう。
     *
     * </pre>
     * @param channel
     * @return FileLockを返す
     * @throws IOException
     * @see #releaseQuietly(FileLock)
     */
    public static FileLock lock(FileChannel channel) throws IOException {
        FileLock lock = channel.tryLock();
        return lock;
    }

    /**
     * <pre>
     * FileLockを解除する。
     *
     * </pre>
     * @param lock lock()で取得したFileLockオブジェクト。HostOS側の排他制御は関与しない。
     * @see #lock(FileChannel)
     */
    public static void releaseQuietly(FileLock lock) {
        try {
            lock.release();
        } catch (Exception e) {
            lock = null;
        }
        lock = null;
    }

    /**
     * <pre>
     * FileChannelを例外を発生させずにcloseする。
     * closeまでのステップは、
     *  1) channel.force(false);
     *  2) channel.close();
     * となる。
     *
     * </pre>
     * @param channel
     * @see #closeQuietly(FileChannel, boolean)
     * @see #newFileChannel(File , String , boolean)
     * @see #newFileChannel(String , String , boolean)
     */
    public static void closeQuietly(FileChannel channel) {
        closeQuietly(channel, false);
    }

    /**
     * <pre>
     * FileChannelを例外を発生させずにcloseする。
     * closeまでのステップは、
     *  1) channel.force(true / false);
     *  2) channel.close();
     * となる。
     * </pre>
     *
     * @param channel
     * @param chForceMeta
     * @see #closeQuietly(FileChannel)
     * @see #newFileChannel(File , String , boolean)
     * @see #newFileChannel(String , String , boolean)
     */
    public static void closeQuietly(FileChannel channel, boolean chForceMeta) {
        if (channel == null) {
            throw new NullPointerException("channel is NULL.");
        }
        Object fileStream = null;
        int key = channel.hashCode();
        try {
            if (!objectMap.containsKey(key)) {
                throw new RuntimeException("\n" + "  channel is not retrieve from FileChUtil#newFileChannel()\n" + "  or channel is closed already.\n" + "  you must to use [FileChUtil#newFileChannel()] for retrieving channel.\n" + "--------");
            }
            if (!channel.isOpen()) {
                throw new RuntimeException("\n" + "  channel is closed already.\n" + "  if channel closed without FileChUtil#closeQuietly(),\n" + "  you must to use [FileChUtil#closeQuietly()] for channel closing.\n" + "--------");
            }
            int chAttrCode = 0;
            channel.force(chForceMeta);
            channel.close();
            fileStream = objectMap.get(key);
            chAttrCode = attrCodeMap.get(key);
            switch(chAttrCode) {
                case FIS:
                    ((FileInputStream) fileStream).close();
                    break;
                case FOS_W:
                    ((FileOutputStream) fileStream).close();
                    break;
                case FOS_A:
                    ((RandomAccessFile) fileStream).close();
                    break;
                case RAF:
                    ((RandomAccessFile) fileStream).close();
                    break;
                default:
                    break;
            }
            removeRefObj(key);
        } catch (Exception e) {
            channel = null;
            fileStream = null;
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * FileChannelの取得元となるFileStreamをhashmapから削除する。
     * FileChannelの取得元となる「FileStreamの種類」をhashmapから削除する。
     *
     * ※FileChUtilTestクラスからも、リフレクションで実行されるので、
     * 　メソッド名変更等をおこなわないこと。
     *
     * @param key FileChannelオブジェクトのhashCode。
     */
    private static void removeRefObj(int key) {
        objectMap.remove(key);
        attrCodeMap.remove(key);
    }

    private static String getMessageForReadBufferTypeError(int bufferType) {
        String message = null;
        if (bufferType < buffTypes.length) {
            message = buffTypes[bufferType];
        } else {
            message = "N/A(" + bufferType + ")";
        }
        return ("Illegal buffer type for READ : [" + message + "]\n" + "--------");
    }

    private static String getMessageForWriteBufferTypeError(int bufferType) {
        String message = null;
        if (bufferType < buffTypes.length) {
            message = buffTypes[bufferType];
        } else {
            message = "N/A(" + bufferType + ")";
        }
        return ("Illegal buffer type for WRITE : [" + message + "]\n" + "--------");
    }

    private static String getMessageForAppendBufferTypeError(int bufferType) {
        String message = null;
        if (bufferType < buffTypes.length) {
            message = buffTypes[bufferType];
        } else {
            message = "N/A(" + bufferType + ")";
        }
        return ("Illegal buffer type for APPEND : [" + message + "]\n" + "--------\n");
    }

    public static FileChannel incPosition(FileChannel channel, int offset) throws IOException {
        channel.position(channel.position() + offset);
        return channel;
    }

    public static FileChannel decPosition(FileChannel channel, long offset) throws IOException {
        channel.position(channel.position() - offset);
        return channel;
    }

    private static String CBDump(FileChannel channel, ByteBuffer buff) throws IOException {
        StringBuilder sb = new StringBuilder();
        int savePos = buff.position();
        int saveLim = buff.limit();
        sb.append("{");
        for (int i = 0; i < saveLim; i++) {
            int val = buff.get(i);
            sb.append("0x");
            sb.append(Integer.toHexString(val));
            sb.append(",");
        }
        sb.append("}").append("\r\n");
        sb.append("# buff.position() =" + savePos).append("\r\n");
        sb.append("# buff.limit()    =" + saveLim).append("\r\n");
        sb.append("# buff.capacity() =" + buff.capacity()).append("\r\n");
        sb.append("# channel.positn()=" + channel.position()).append("\r\n");
        sb.append("# channel.size()  =" + channel.size()).append("\r\n");
        buff.position(savePos);
        return sb.toString();
    }

    private static boolean lastPositionOf(FileChannel channel) throws IOException {
        return (channel.position() == channel.size()) ? true : false;
    }

    private static boolean firstPositionOf(FileChannel channel) throws IOException {
        return (channel.position() == 0) ? true : false;
    }

    private static boolean isEOF(FileChannel channel, ByteBuffer buff) throws IOException {
        return (channel.position() == channel.size() && buff.position() == buff.limit()) ? true : false;
    }

    public static ByteBuilder readLine_CRLF(FileChannel channel, ByteBuffer buff, int buffType, String charset, boolean enableCRLF, byte cr, byte lf) throws IOException {
        if (isEOF(channel, buff)) {
            return null;
        }
        ByteBuilder bb = new ByteBuilder(buff.capacity());
        boolean foundCR = false;
        boolean foundLF = false;
        boolean exit = false;
        while (!exit) {
            if (isEOF(channel, buff)) {
                bb.setReferObject(-1);
                return bb;
            }
            String tmp = CBDump(channel, buff);
            System.out.println("2) " + channel.hashCode());
            System.out.println("2) " + buff.hashCode());
            if (firstPositionOf(channel) || !buff.hasRemaining()) {
                buff = FileChUtil.read(channel, buff, buffType);
                if (buff == null) {
                    break;
                }
            }
            System.out.println("3) " + channel.hashCode());
            System.out.println("3) " + buff.hashCode());
            tmp = CBDump(channel, buff);
            int bufPos = 0;
            int bufLim = buff.limit();
            byte b1 = 0;
            int initBufPos = buff.position();
            if (initBufPos > 0) {
                bufPos = initBufPos;
            }
            while (bufPos < bufLim) {
                System.out.println("A) " + channel.hashCode());
                System.out.println("A) " + buff.hashCode());
                b1 = buff.get(bufPos++);
                System.out.println("B) " + channel.hashCode());
                System.out.println("B) " + buff.hashCode());
                if (b1 == cr) {
                    foundCR = true;
                }
                if (b1 == lf) {
                    foundLF = true;
                }
                if (foundCR && foundLF) {
                    bb.append(buff, initBufPos, bufPos - initBufPos);
                    exit = true;
                    break;
                }
                if (bufPos == bufLim) {
                    System.out.println("SS1) " + buff.hashCode());
                    buff.position(bufPos);
                    System.out.println("SS2) " + buff.hashCode());
                    ByteBuffer tmpBuf = buff.duplicate();
                    System.out.println("SS3) " + buff.hashCode());
                    System.out.println("TT) " + tmpBuf.hashCode());
                    bb.append(tmpBuf, initBufPos, buff.limit() - initBufPos);
                }
            }
        }
        if (buff != null) {
            bb.setReferObject(buff.position());
        }
        return bb;
    }

    /**
     * <PRE>
     * FileChannelの現在のポジションからからByteBufferで定義されたバッファにデータを読み込み、
     * データが読み込まれたByteBufferを返す。
     * FileChannelから読み込まれるデータが無くなった場合、戻り値として null を返す。
     *
     * 大きなサイズのファイルを、小さなバッファを介して断続的・順番に読み込む場合などに利用する。
     * 一回に読み込まれるデータサイズの上限は、バッファサイズに依存する。
     *
     * 読み込んだバイト数を知りたい場合は、ByteBuffer#limit()を用いる。
     *
     * 読み込み元となるFileChannelに対して、事前にFileChannel#position(long newPosition)を
     * 実行することで、読み込み開始位置を指定できる。
     *
     * 利用可能なバッファの種類：
     *   1) FileChUtil.USE_ALLOCATED_BUFFER
     *   2) FileChUtil.USE_DIRECT_ALLOCATED_BUFFER
     *   3) FileChUtil.USE_MAPMODE_READ_ONLY
     *   4) FileChUtil.USE_MAPMODE_READ_WRITE
     *
     * ※ FileChUtil.USE_MAPMODE_READ_* を使う場合、メソッドを呼ぶたびに
     *    FileChannelのpositionを変更し、ByteBuffに再マッピングする。
     *
     * ※ FileChUtil.USE_ALLOCATED_BUFFER、USE_DIRECT_ALLOCATED_BUFFERを使う場合、
     *    返されるByteBuffはflip()されている。
     *
     * </PRE>
     *
     * @param channel 入力元となるFileChannel
     * @param buff データを読み込むバッファ(データ終端の場合はnull))
     * @param buffType バッファの種類
     * @return データが読み込まれたByteBuffer
     * @throws IOException
     */
    public static ByteBuffer read(FileChannel channel, ByteBuffer buff, int buffType) throws IOException {
        if (buffType == USE_ALLOCATED_BUFFER || buffType == USE_DIRECT_ALLOCATED_BUFFER) {
            buff.clear();
            channel.read(buff);
            buff.flip();
            return buff;
        } else if (buffType == USE_MAPMODE_READ_ONLY || buffType == USE_MAPMODE_READ_WRITE) {
            buff.clear();
            long chRemain = channel.size() - channel.position();
            if (chRemain == 0) {
                buff = channel.map(mapmode[buffType], channel.position() - buff.limit(), buff.limit());
            } else if (chRemain > buff.capacity()) {
                buff = channel.map(mapmode[buffType], channel.position(), buff.capacity());
            } else if (chRemain > 0) {
                buff = channel.map(mapmode[buffType], channel.position(), chRemain);
            }
            channel.position(channel.position() + buff.limit());
            return buff;
        } else {
            throw new IOException(getMessageForReadBufferTypeError(buffType));
        }
    }

    /**
     * <pre>
     * FileChannelからbyte配列にデータを読み込む。
     * FileChannelの現在のポジションからByteBufferを経由して、byte配列にデータを読み込み、
     * 読み込んだバイト数を返す。
     * FileChannelから読み込まれるデータが無くなった場合、戻り値として -1 を返す。
     *
     * 大きなサイズのファイルを、小さなバッファを介して断続的・順番に読み込む場合などに利用する。
     * 一回に読み込まれるデータサイズの上限は、バッファサイズに依存する。
     *
     * ByteBufferのバッファサイズと、byte配列のサイズは同じである必要がある。
     * 両者のサイズが異なる場合は、IllegalArgumentExceptionが発生する。
     *
     * FileChannelからByteBufferへ最後に読み込んだデータ群のサイズが
     * 格納先のbyte配列サイズより小さい場合は、byte配列の最終読み込み位置以降は
     * 0x00で埋められる。
     *
     * 読み込み元となるFileChannelに対して、事前にFileChannel#position(long newPosition)を
     * 実行することで、読み込み開始位置を指定できる。
     *
     * 利用可能なバッファの種類：
     *   1) FileChUtil.USE_ALLOCATED_BUFFER
     *   2) FileChUtil.USE_DIRECT_ALLOCATED_BUFFER
     *   3) FileChUtil.USE_MAPMODE_READ_ONLY
     *   4) FileChUtil.USE_MAPMODE_READ_WRITE
     *
     * ※ FileChUtil.USE_MAPMODE_READ_* を使う場合、メソッドを呼ぶたびに
     *    FileChannelのpositionを変更し、ByteBuffに再マッピングする。
     *
     *
     * Example : Read from binary file to byte array and delete file.
     *      read target binary file : "C:\temp\test.bin"
     *      hex content (48 bytes) :
     *      +------------------------------------------------+
     *      |00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F |
     *      |10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F |
     *      |20 21 22 23 24 25 26 27 28 29 2A 2B 2C 2D 2E 2F |
     *      +------------------------------------------------+
     *
     *      #======= Use (direct) allocated buffer  ============================
     *      # - buffer size       : 10 bytes
     *      # - using buffer type : ByteBuffer.allocateDirect(10)
     *      #                       (or ByteBuffer.allocate(10))
     *      # - start offset of FileChannel position : 32
     *      #===================================================================
     *
     *      File        file = new File("C:\\temp\\test.bin");
     *      int buffType     = FileChUtil.USE_DIRECT_ALLOCATED_BUFFER;
     *      //int buffType   = FileChUtil.USE_ALLOCATED_BUFFER;
     *
     *      FileChannel ch   = FileChUtil.newFileChannel(file, "r", true);
     *      ByteBuffer  buf  = FileChUtil.getReadBuffer(ch, 0, buffType, 10);
     *
     *      byte[] data = new byte[buf.capacity()];
     *      ch.position(32);
     *      int readCount = 0;
     *      readCount = FileChUtil.readTo(data,  ch, buf, buffType);
     *      // data = {0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29}, readCount = 10
     *      readCount = FileChUtil.readTo(data,  ch, buf, buffType);
     *      // data = {0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x00, 0x00, 0x00, 0x00}, readCount = 6
     *      readCount = FileChUtil.readTo(data,  ch, buf, buffType);
     *      // data = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}, readCount = -1
     *
     *      FileChUtil.closeQuietly(ch);
     *
     *      // *** if you want to delete source file;
     *      //buf = null;
     *      //file.delete();
     *
     *      #======= Use 'FileChannel.MapMode' =====================================
     *      # - buffer size       : 10 bytes
     *      # - using buffer type : channel.map(FileChannel.MapMode.READ_ONLY)
     *      #                       (or channel.map(FileChannel.MapMode.READ_WRITE) )
     *      # - start offset of FileChannel position : 32
     *      #=======================================================================
     *
     *      File        file = new File("C:\\temp\\test.bin");
     *      int buffType     = FileChUtil.USE_MAPMODE_READ_ONLY;
     *      //int buffType   = FileChUtil.USE_MAPMODE_READ_WRITE;
     *
     *      FileChannel ch   = FileChUtil.newFileChannel(file, "r", true);
     *      // FileChannel ch= FileChUtil.newFileChannel(file, "rw", true);
     *      ByteBuffer  buf  = FileChUtil.getReadBuffer(ch, 0, buffType, 10);
     *
     *      byte[] data = new byte[buf.capacity()];
     *      ch.position(32);
     *      int readCount = 0;
     *      readCount = FileChUtil.readTo(data,  ch, buf, buffType);
     *      // data = {0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29}, readCount = 10
     *      readCount = FileChUtil.readTo(data,  ch, buf, buffType);
     *      // data = {0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x00, 0x00, 0x00, 0x00}, readCount = 6
     *      readCount = FileChUtil.readTo(data,  ch, buf, buffType);
     *      // data = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}, readCount = -1
     *
     *      FileChUtil.closeQuietly(ch);
     *
     *      // *** if you want to delete source file;
     *      //buf = null;
     *      //System.gc();
     *      //file.delete();
     *
     *      // *** Win32上で、FileChannel#MapModeを使った際に、対象fileを
     *      // *** 削除するには、まずバッファにNULLをセットしてからgcを実行する必要がある。
     *      // *** need to set NULL to buffer and call gc
     *      // *** before delete file when using FileChannel#MapMode on Win32.
     *
     * </pre>
     *
     * @param dst ファイルから読み込んだデータを格納するバイト配列
     * @param channel 入力元となるFileChannel
     * @param buff getReadBuffer()で取得したByteBuffer
     * @param buffType バッファの種類。USE_ALLOCATED_BUFFER / USE_DIRECT_ALLOCATED_BUFFER / USE_MAPMODE_READ_ONLY / USE_MAPMODE_READ_WRITE
     * @return 読み込んだバイト数。ファイル終端なら -1 を返す。
     * @throws IOException
     */
    public static int readTo(byte[] dst, FileChannel channel, ByteBuffer buff, int buffType) throws IOException {
        if (buff.capacity() != dst.length) {
            throw new IllegalArgumentException("Size of 'buff' and length of 'dst' should be the same.\n" + "    (ByteBuffer) buff.capacity() = [" + buff.capacity() + "]\n" + "    (byte[]) dst.length          = [" + dst.length + "]\n" + "--------");
        }
        int readBytes = -1;
        if (buffType == USE_ALLOCATED_BUFFER || buffType == USE_DIRECT_ALLOCATED_BUFFER) {
            buff.clear();
            readBytes = channel.read(buff);
            if (readBytes < 0) {
                Arrays.fill(dst, (byte) 0x00);
                return readBytes;
            }
            buff.flip();
            if (readBytes < buff.capacity()) {
                (buff).get(dst, 0, readBytes);
                Arrays.fill(dst, readBytes, dst.length, (byte) 0x00);
            } else {
                (buff).get(dst);
            }
            return readBytes;
        } else if (buffType == USE_MAPMODE_READ_ONLY || buffType == USE_MAPMODE_READ_WRITE) {
            long chRemain = channel.size() - channel.position();
            if (chRemain == 0) {
                readBytes = -1;
                Arrays.fill(dst, (byte) 0x00);
            } else if (chRemain > buff.capacity()) {
                buff = channel.map(mapmode[buffType], channel.position(), buff.capacity());
                (buff).get(dst);
                readBytes = buff.capacity();
            } else {
                buff = channel.map(mapmode[buffType], channel.position(), chRemain);
                (buff).get(dst, 0, (int) chRemain);
                Arrays.fill(dst, (int) chRemain, dst.length, (byte) 0x00);
                readBytes = (int) chRemain;
            }
            channel.position(channel.position() + buff.position());
            return readBytes;
        } else {
            throw new IOException(getMessageForReadBufferTypeError(buffType));
        }
    }

    /**
     * <pre>
     * FileChannelからbyte配列への一括読み込み。
     *
     * 読み込み用の bufferType には、次の３種類を指定できる。
     *   (1) FileChUtil.USE_ALLOCATED_BUFFER
     *   (2) FileChUtil.USE_DIRECT_ALLOCATED_BUFFER
     *   (3) FileChUtil.USE_MAPMODE_READ_ONLY
     *
     * </pre>
     * @param channel 入力元となるFileChannel
     * @param bufferType 読み込みに使うバッファタイプ。USE_ALLOCATED_BUFFER / USE_DIRECT_ALLOCATED_BUFFER / USE_MAPMODE_READ_ONLY
     * @return FileChannelからの読み込みが完了したbyte配列
     * @throws IOException
     */
    public static byte[] readToBytes(FileChannel channel, int bufferType) throws IOException {
        switch(bufferType) {
            case USE_ALLOCATED_BUFFER:
                return readToBytesViaAB(channel);
            case USE_DIRECT_ALLOCATED_BUFFER:
                return readToBytesViaDAB(channel);
            case USE_MAPMODE_READ_ONLY:
                return readToBytesViaMBB(channel);
        }
        throw new IOException(getMessageForReadBufferTypeError(bufferType));
    }

    /**
     * <pre>
     * FileChannelからbyte配列への一括読み込み。
     *
     * "ByteBuffer.allocate(int capacity)" を使う。
     *
     * read (binary) file to byte array.
     * using : ByteBuffer.allocate((int)inputChannel.size())
     * FileChannel -> ByteBuffer -> byte[]
     * </pre>
     *
     * @param channel
     * @return 読み込まれたバイト配列 / byte array as result
     * @throws IOException
     */
    private static byte[] readToBytesViaAB(FileChannel channel) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate((int) channel.size());
        channel.read(buf);
        buf.clear();
        byte[] bytes = new byte[buf.capacity()];
        buf.get(bytes);
        buf = null;
        return bytes;
    }

    /**
     * <pre>
     * FileChannelからbyte配列への一括読み込み。
     *
     * read (binary) file to byte array.
     * using : ByteBuffer.allocateDirect((int)inputChannel.size())
     * FileChannel -> ByteBuffer -> byte[]
     *
     * </pre>
     * @param channel
     * @return 読み込まれたバイト配列 / byte array as result
     * @throws IOException
     */
    private static byte[] readToBytesViaDAB(FileChannel channel) throws IOException {
        ByteBuffer buf = ByteBuffer.allocateDirect((int) channel.size());
        channel.read(buf);
        buf.clear();
        byte[] bytes = new byte[buf.capacity()];
        buf.get(bytes);
        buf = null;
        return bytes;
    }

    /**
     * <pre>
     * FileChannelからbyte配列への一括読み込み。
     *
     * read (binary) file to byte array.
     * using : inputChannel.map(FileChannel.MapMode.READ_ONLY, 0, inputChannel.size())
     * FileChannel -> MappedByteBuffer -> byte[]
     *
     * </pre>
     * @param channel
     * @return 読み込まれたバイト配列 / byte array as result
     * @throws IOException
     */
    private static byte[] readToBytesViaMBB(FileChannel channel) throws IOException {
        MappedByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        byte[] data = new byte[(int) buf.capacity()];
        buf.get(data);
        buf = null;
        return data;
    }

    /**
     * <pre>
     * FileChannelから連続したbyteデータを、int型データの下位8bit値としてint配列へ一括で読み込む。
     *
     * 読み込み用の bufferType には、次の３種類を指定できる。
     *   (1) FileChUtil.USE_ALLOCATED_BUFFER
     *   (2) FileChUtil.USE_DIRECT_ALLOCATED_BUFFER
     *   (3) FileChUtil.USE_MAPMODE_READ_ONLY
     *
     *
     * </pre>
     *
     * @param channel 入力元となるFileChannel
     * @param bufferType 読み込みに使うバッファタイプ。USE_ALLOCATED_BUFFER / USE_DIRECT_ALLOCATED_BUFFER / USE_MAPMODE_READ_ONLY
     * @return FileChannelからの読み込みが完了したint配列
     * @throws IOException
     */
    public static int[] readToInts(FileChannel channel, int bufferType) throws IOException {
        switch(bufferType) {
            case USE_ALLOCATED_BUFFER:
                return readToIntsViaAB(channel);
            case USE_DIRECT_ALLOCATED_BUFFER:
                return readToIntsViaDAB(channel);
            case USE_MAPMODE_READ_ONLY:
                return readToIntsViaMBB(channel);
        }
        throw new IOException(getMessageForReadBufferTypeError(bufferType));
    }

    private static int[] readToIntsViaAB(FileChannel channel) throws IOException {
        byte[] src = readToBytesViaAB(channel);
        int[] data = new int[src.length];
        for (int i = 0; i < src.length; i++) {
            data[i] = src[i] & 0xFF;
        }
        return data;
    }

    private static int[] readToIntsViaDAB(FileChannel channel) throws IOException {
        byte[] src = readToBytesViaDAB(channel);
        int[] data = new int[src.length];
        for (int i = 0; i < src.length; i++) {
            data[i] = src[i] & 0xFF;
        }
        return data;
    }

    private static int[] readToIntsViaMBB(FileChannel channel) throws IOException {
        byte[] src = readToBytesViaMBB(channel);
        int[] data = new int[src.length];
        for (int i = 0; i < src.length; i++) {
            data[i] = src[i] & 0xFF;
        }
        return data;
    }

    /**
     * <pre>
     * FileChannelからString型への一括読み込み。
     *
     * 読み込み用の bufferType には、次の３種類を指定できる。
     *   (1) FileChUtil.USE_ALLOCATED_BUFFER
     *   (2) FileChUtil.USE_DIRECT_ALLOCATED_BUFFER
     *   (3) FileChUtil.USE_MAPMODE_READ_ONLY
     *
     *
     * </pre>
     * @param channel 対象FileChannel
     * @param bufferType バッファタイプ
     * @return FileChannelからの読み込みが完了したStringオブジェクト
     * @throws IOException
     */
    public static String readToStr(FileChannel channel, int bufferType) throws IOException {
        return new String(readToBytes(channel, bufferType));
    }

    /**
     * <pre>
     * FileChannelからエンコードを指定したString型への一括読み込み。
     * 任意の文字エンコードで書かれたファイルを読み取り、UTF8形式のStringオブジェクトに格納する。
     *
     * 読み込み用の bufferType には、次の３種類を指定できる。
     *   (1) FileChUtil.USE_ALLOCATED_BUFFER
     *   (2) FileChUtil.USE_DIRECT_ALLOCATED_BUFFER
     *   (3) FileChUtil.USE_MAPMODE_READ_ONLY
     *
     *
     *
     * </pre>
     * @param channel 対象FileChannel
     * @param bufferType バッファタイプ
     * @param encoding 対象FileChannelが扱っているファイル内データのエンコーディング
     * @return FileChannelからの読み込みが完了したStringオブジェクト
     * @throws IOException
     */
    public static String readToStr(FileChannel channel, int bufferType, String encoding) throws IOException {
        return new String(readToBytes(channel, bufferType), encoding);
    }

    /**
     * <pre>
     * FileChannelに対してbyte配列のデータを出力する。
     * byte配列のデータを、FileChannelの現在のポジション以降に対しByteBufferを経由して出力し、
     * 出力したバイト数を返す。
     * 出力ソースとなるbyte配列データ中の、出力オフセットであるsrcOffsetが配列末尾または
     * それを越えた場合は出力はおこなわれず、戻り値として -1 を返す。
     *
     * byte配列データを、小さなバッファを介しFileChannelに断続的・順番に書き出す場合などに利用する。
     * 一回に書き出されるサイズの上限は、バッファサイズに依存する。
     *
     * 出力ソース中の起点オフセットであるsrcOffsetは、出力毎に更新する必要がある。
     *
     * 対象となるFileChannelに対して、事前にFileChannel#position(long newPosition)を実行することで、
     * 出力先の位置を指定できる。
     *
     *
     * 利用可能なバッファの種類：
     *   1) FileChUtil.USE_ALLOCATED_BUFFER
     *   2) FileChUtil.USE_DIRECT_ALLOCATED_BUFFER
     *   3) FileChUtil.USE_MAPMODE_READ_WRITE
     *
     * Example:
     *      byte[] src = { 0,  1,  2,  3,  4,  5,  6,  7
     *                  ,  8,  9, 10, 11, 12, 13, 14, 15
     *                  , 16, 17, 18, 19, 20, 21, 22     };
     *      int buffType = FileChUtil.USE_ALLOCATED_BUFFER;
     *      File file = new File("C:\\temp\\test.bin");
     *      FileChannel channel = FileChUtil.newFileChannel(file, "W", true);
     *      ByteBuffer buff = FileChUtil.getWriteBuffer(channel, buffType, 10);
     *
     *      int wroteSize = 0;
     *      int srcOffset = 0;
     *
     *      // *** you can set new output position before write.
     *      // channel.position(7);
     *      while(wroteSize != -1){
     *          wroteSize = FileChUtil.writeTo(channel,  src, srcOffset, buff, buffType);
     *          srcOffset += 10;
     *      }
     *      System.out.println("Target Size = " + channel.size());
     *      FileChUtil.closeQuietly(channel);
     *
     *      // *** if you want to delete target file;
     *      // buff = null;
     *      // file.delete();
     *
     *      -------------------------------------------------------
     *      output file : "C:\temp\test.bin"
     *      hex content (23 bytes) :
     *      +------------------------------------------------+
     *      |00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F |
     *      |10 11 12 13 14 15 16                            |
     *      +------------------------------------------------+
     *
     * </pre>
     *
     * @param channel 出力先となるFileChannel
     * @param src 出力したいデータが格納されたバイト配列
     * @param srcOffset src中の出力起点となるオフセット位置。出力毎に指定する必要がある。
     * @param buff getWriteBuffer()で取得したByteBuffer
     * @param buffType バッファの種類。USE_ALLOCATED_BUFFER / USE_DIRECT_ALLOCATED_BUFFER / USE_MAPMODE_READ_WRITE
     * @return 出力したバイト数。ソースデータの終端なら -1 を返す。
     * @throws IOException
     */
    public static int writeTo(FileChannel channel, byte[] src, int srcOffset, ByteBuffer buff, int buffType) throws IOException {
        if (srcOffset >= src.length) {
            return -1;
        }
        int wroteSize = 0;
        int writeLength = buff.capacity();
        if ((src.length - srcOffset) < writeLength) {
            writeLength = src.length - srcOffset;
            buff.limit(writeLength);
        }
        if (buffType == USE_ALLOCATED_BUFFER || buffType == USE_DIRECT_ALLOCATED_BUFFER) {
            (buff).put(src, srcOffset, writeLength);
            buff.position(0);
            wroteSize = channel.write(buff);
            buff.clear();
        } else if (buffType == USE_MAPMODE_READ_WRITE) {
            buff = channel.map(FileChannel.MapMode.READ_WRITE, channel.position(), writeLength);
            (buff).put(src, srcOffset, writeLength);
            if (channel.position() == 0) {
                channel.position(buff.capacity());
            }
            wroteSize = writeLength;
        } else {
            throw new IOException(getMessageForWriteBufferTypeError(buffType));
        }
        return wroteSize;
    }

    /**
     * <pre>
     * FileChannelに対してbyte配列のデータを一括出力する。
     * byte配列をFileChannelの現在のポジションから書き出し、そのFileChannel自身を返す。
     * 書き出し処理は、ポジションを明示的に操作する必要無しに連続しておこなえる。
     * (当該FileChannelのポジションは、書き出したバイト数に応じて増加する)
     *
     * バッファ(ByteBuffer)は、メソッド実行毎に新たに割り当てられる。
     * 単一のバッファを再利用して細かい処理がしたい場合は、
     * #writeTo(FileChannel, byte[], int, ByteBuffer, int)
     * を使うと良い。
     *
     * 対象となるFileChannelに対して、事前にFileChannel#position(long newPosition)を実行することで、
     * 出力先の位置を指定できる。
     *
     * 利用可能なバッファの種類：
     *   1) FileChUtil.USE_ALLOCATED_BUFFER
     *   2) FileChUtil.USE_DIRECT_ALLOCATED_BUFFER
     *   3) FileChUtil.USE_MAPMODE_READ_WRITE
     *   4) FileChUtil.USE_WRAPPED_BYTE_ARRAY
     *
     * Example :
     *      File file = new File("C:\\temp\\test.bin");
     *      file.createNewFile();
     *
     *      // *** retrieve new FileChannel.
     *      FileChannel ch = FileChUtil.newFileChannel(file, "w", true);
     *      int bufType = FileChUtil.USE_ALLOCATED_BUFFER;
     *
     *      // *** source byte datas.
     *      byte[][] src = {
     *           { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 }
     *          ,{ 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18 }
     *          ,{ 0x20, 0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F }
     *          ,{ 0x30, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F }
     *      };
     *
     *      // *** continuative write source.
     *      FileChUtil.writeTo(ch, src[0], bufType);
     *      FileChUtil.writeTo(ch, src[1], bufType);
     *      FileChUtil.writeTo(ch, src[2], bufType);
     *
     *      // *** position skip (8 bytes)
     *      FileChUtil.incPosition(ch, 8);
     *
     *      // *** write final
     *      FileChUtil.writeTo(ch, src[3], bufType);
     *
     *      // *** close FileChannel
     *      FileChUtil.closeQuietly(ch);
     *
     *      -------------------------------------------------------
     *      output file : "C:\temp\test.bin"
     *      hex content (40 bytes) :
     *      +------------------------------------------------+
     *      |01 02 03 04 05 06 07 08 11 12 13 14 15 16 17 18 |
     *      |20 29 2A 2B 2C 2D 2E 2F 00 00 00 00 00 00 00 00 |
     *      |30 39 3A 3B 3C 3D 3E 3F                         |
     *      +------------------------------------------------+
     *
     * </pre>
     *
     * @param channel 出力対象となるFileChannel
     * @param data ソースとなるbyte配列
     * @param bufferType バッファタイプ
     * @return 出力対象のFileChannel
     * @throws IOException
     * @see #writeTo(FileChannel, byte[], int, ByteBuffer, int)
     */
    public static FileChannel writeTo(FileChannel channel, byte[] data, int bufferType) throws IOException {
        switch(bufferType) {
            case USE_ALLOCATED_BUFFER:
                return writeToFileChannelViaAB(channel, data);
            case USE_DIRECT_ALLOCATED_BUFFER:
                return writeToFileChannelViaDAB(channel, data);
            case USE_MAPMODE_READ_WRITE:
                return writeToFileChannelViaMBB(channel, data);
            case USE_WRAPPED_BYTE_ARRAY:
                return writeToFileChannelViaWBB(channel, data);
            case USE_MAPMODE_READ_ONLY:
                break;
            default:
                break;
        }
        throw new IOException(getMessageForWriteBufferTypeError(bufferType));
    }

    private static FileChannel writeToFileChannelViaAB(FileChannel channel, byte[] data) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(data.length);
        buf.put(data);
        buf.flip();
        channel.write(buf);
        return channel;
    }

    private static FileChannel writeToFileChannelViaDAB(FileChannel channel, byte[] data) throws IOException {
        ByteBuffer buf = ByteBuffer.allocateDirect(data.length);
        buf.put(data);
        buf.flip();
        channel.write(buf);
        return channel;
    }

    private static FileChannel writeToFileChannelViaWBB(FileChannel channel, byte[] data) throws IOException {
        channel.write(ByteBuffer.wrap(data));
        return channel;
    }

    private static FileChannel writeToFileChannelViaMBB(FileChannel channel, byte[] data) throws IOException {
        MappedByteBuffer buf = channel.map(FileChannel.MapMode.READ_WRITE, 0, data.length);
        buf.put(data);
        buf.force();
        buf = null;
        return channel;
    }

    /**
     * <pre>
     * FileChannelに対してint配列のデータを一括出力する。
     * int配列の各要素の下位8bit値を、FileChannelの現在のポジションからbyte単位で書き出す。
     *
     * </pre>
     * @param channel
     * @param data
     * @return FileChannel
     * @throws IOException
     */
    public static FileChannel writeTo(FileChannel channel, int[] data, int bufferType) throws IOException {
        byte[] outData = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            outData[i] = (byte) data[i];
        }
        return writeTo(channel, outData, bufferType);
    }

    /**
     * <pre>
     * FileChannelに対してString型のデータを一括出力する。
     * </pre>
     *
     * @param channel
     * @param data
     * @return 使用したFileChannel
     * @throws IOException
     */
    public static FileChannel writeTo(FileChannel channel, String data, int bufferType) throws IOException {
        return writeTo(channel, data.getBytes(), bufferType);
    }

    /**
     * <pre>
     * FileChannelに対してString型のデータをエンコードを指定して一括出力する。
     * </pre>
     *
     * @param channel
     * @param data
     * @param encoding
     * @return 使用したFileChannel
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public static FileChannel writeTo(FileChannel channel, String data, int bufferType, String encoding) throws UnsupportedEncodingException, IOException {
        return writeTo(channel, data.getBytes(encoding), bufferType);
    }

    /**
     * <pre>
     * FileChannelに対してbyte配列のデータを追記出力する。
     * 出力ソースとなるbyte配列のデータを、対象となるFileChannelの末尾以降に対しByteBufferを経由して
     * 追加出力(append)し、出力したバイト数を返す。
     * 出力ソースとなるbyte配列データ中の、出力オフセットであるsrcOffsetが配列末尾またはそれを越えた場合は
     * 出力はおこなわれず、戻り値として -1 を返す。
     *
     * byte配列データを、小さなバッファを介しFileChannelに断続的・順番に追加出力する場合などに利用する。
     * 一回に書き出されるサイズの上限は、バッファサイズに依存する。
     *
     * 出力ソース中の起点オフセットであるsrcOffsetは、出力毎に更新する必要がある。
     *
     *
     * 利用可能なバッファの種類：
     *   1) FileChUtil.USE_ALLOCATED_BUFFER
     *   2) FileChUtil.USE_DIRECT_ALLOCATED_BUFFER
     *   3) FileChUtil.USE_MAPMODE_READ_WRITE
     *
     *
     * Example:
     *      // *** Create a null file with a length 32 byte.
     *      String target = "C:\\temp\\test.bin";
     *      RandomAccessFile raf = new RandomAccessFile(target, "rw");
     *      raf.setLength(32);
     *      raf.close();
     *
     *      // *** Prepare for append.
     *      byte[] src = { 0,  1,  2,  3,  4,  5,  6,  7
     *                  ,  8,  9, 10, 11, 12, 13, 14, 15
     *                  , 16, 17, 18, 19, 20, 21, 22    };
     *      int buffType = FileChUtil.USE_ALLOCATED_BUFFER;
     *
     *      File file = new File(target);
     *      FileChannel channel = FileChUtil.newFileChannel(file, "A", true);
     *      ByteBuffer buff = FileChUtil.getAppendBuffer(channel, buffType, 10);
     *
     *      int wroteSize = 0;
     *      int srcOffset = 0;
     *
     *      // *** Start append.
     *      while(wroteSize != -1){
     *          wroteSize = FileChUtil.appendTo(channel,    src, srcOffset, buff, buffType);
     *          srcOffset += 10;
     *      }
     *      System.out.println("Target Size = " + channel.size());
     *      FileChUtil.closeQuietly(channel);
     *
     *      // *** if you want to delete target file;
     *      //buff = null;
     *      //file.delete();
     *
     *      -------------------------------------------------------
     *      output file : "C:\temp\test.bin"
     *      hex content (55 bytes) :
     *      +------------------------------------------------+
     *      |00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 |
     *      |00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 |
     *      |00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F |
     *      |10 11 12 13 14 15 16                            |
     *      +------------------------------------------------+
     *
     * </pre>
     *
     * @param channel 出力先となるFileChannel
     * @param src 出力したいデータが格納されたバイト配列
     * @param srcOffset src中の出力起点となるオフセット位置。出力毎に指定する必要がある。
     * @param buff getApendBuffer()で取得したByteBuffer
     * @param buffType バッファの種類。USE_ALLOCATED_BUFFER / USE_DIRECT_ALLOCATED_BUFFER / USE_MAPMODE_READ_WRITE
     * @return 出力したバイト数。ソースデータの終端なら -1 を返す。
     * @throws IOException
     */
    public static int appendTo(FileChannel channel, byte[] src, int srcOffset, ByteBuffer buff, int buffType) throws IOException {
        if (srcOffset >= src.length) {
            return -1;
        }
        int wroteSize = 0;
        int writeLength = buff.capacity();
        if ((src.length - srcOffset) < writeLength) {
            writeLength = src.length - srcOffset;
            buff.limit(writeLength);
        }
        if (buffType == USE_ALLOCATED_BUFFER || buffType == USE_DIRECT_ALLOCATED_BUFFER) {
            (buff).put(src, srcOffset, writeLength);
            buff.position(0);
            wroteSize = channel.write(buff);
            buff.clear();
        } else if (buffType == USE_MAPMODE_READ_WRITE) {
            long chPos = channel.position();
            buff = channel.map(FileChannel.MapMode.READ_WRITE, channel.position(), writeLength);
            channel.position(chPos);
            (buff).put(src, srcOffset, writeLength);
            channel.position(channel.position() + writeLength);
            wroteSize = writeLength;
        } else {
            throw new IOException(getMessageForAppendBufferTypeError(buffType));
        }
        return wroteSize;
    }

    /**
     * <pre>
     * FileChannelに対してbyte配列のデータを一括で追記出力する。
     * byte配列をFileChannelの末尾のポジション以降に追記し、そのFileChannel自身を返す。
     * 書き出し処理は、ポジションを明示的に操作する必要無しに連続しておこなえる。
     * (当該FileChannelのポジションは、書き出したバイト数に応じて増加する)
     *
     * バッファ(ByteBuffer)は、メソッド実行毎に新たに割り当てられる。
     * 単一のバッファを再利用して細かい処理がしたい場合は、
     * #appendTo(FileChannel, byte[], int, ByteBuffer, int)
     * を使うと良い。
     *
     * 対象となるFileChannelに対して、事前にFileChannel#position(long newPosition)を実行することで、
     * 出力先の位置を指定できる。
     *
     * 利用可能なバッファの種類：
     *   1) FileChUtil.USE_ALLOCATED_BUFFER
     *   2) FileChUtil.USE_DIRECT_ALLOCATED_BUFFER
     *   3) FileChUtil.USE_MAPMODE_READ_WRITE
     *   4) FileChUtil.USE_WRAPPED_BYTE_ARRAY
     *
     * Example :
     *
     *
     *
     *
     * </pre>
     *
     * @param channel
     * @param data
     * @return 使用したFileChannel
     * @throws IOException
     */
    public static FileChannel appendTo(FileChannel channel, byte[] data, int bufferType) throws IOException {
        switch(bufferType) {
            case USE_ALLOCATED_BUFFER:
                return appendToFileChannelViaAB(channel, data);
            case USE_DIRECT_ALLOCATED_BUFFER:
                return appendToFileChannelViaDAB(channel, data);
            case USE_MAPMODE_READ_WRITE:
                return appendToFileChannelViaMBB(channel, data);
            case USE_WRAPPED_BYTE_ARRAY:
                return appendToFileChannelViaWBB(channel, data);
            case USE_MAPMODE_READ_ONLY:
                break;
            default:
                break;
        }
        throw new IOException(getMessageForWriteBufferTypeError(bufferType));
    }

    private static FileChannel appendToFileChannelViaAB(FileChannel channel, byte[] data) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(data.length);
        buf.put(data);
        buf.flip();
        if (channel.position() < channel.size()) {
            channel.position(channel.size());
        }
        channel.write(buf);
        return channel;
    }

    private static FileChannel appendToFileChannelViaDAB(FileChannel channel, byte[] data) throws IOException {
        ByteBuffer buf = ByteBuffer.allocateDirect(data.length);
        buf.put(data);
        buf.flip();
        if (channel.position() < channel.size()) {
            channel.position(channel.size());
        }
        channel.write(buf);
        return channel;
    }

    private static FileChannel appendToFileChannelViaWBB(FileChannel channel, byte[] data) throws IOException {
        if (channel.position() < channel.size()) {
            channel.position(channel.size());
        }
        channel.write(ByteBuffer.wrap(data));
        return channel;
    }

    private static FileChannel appendToFileChannelViaMBB(FileChannel channel, byte[] data) throws IOException {
        MappedByteBuffer buf = null;
        if (channel.position() < channel.size()) {
            buf = channel.map(FileChannel.MapMode.READ_WRITE, channel.size(), data.length);
        } else {
            buf = channel.map(FileChannel.MapMode.READ_WRITE, channel.position(), data.length);
        }
        buf.put(data);
        buf.force();
        buf = null;
        return channel;
    }

    /**
     * <pre>
     * FileChannelに対してint配列のデータを一括で追記出力する。
     * int配列の各要素の下位8bit値を、FileChannelの現在のポジションからbyte単位で追記する。
     *
     * </pre>
     *
     * @param channel
     * @param data
     * @return 使用したFileChannel
     * @throws IOException
     */
    public static FileChannel appendTo(FileChannel channel, int[] data, int bufferType) throws IOException {
        byte[] outData = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            outData[i] = (byte) data[i];
        }
        return appendTo(channel, outData, bufferType);
    }

    /**
     * <pre>
     * FileChannelに対してString型のデータを一括で追記出力する。
     * </pre>
     *
     * @param channel
     * @param data
     * @return 使用したFileChannel
     * @throws IOException
     */
    public static FileChannel appendTo(FileChannel channel, String data, int bufferType) throws IOException {
        return appendTo(channel, data.getBytes(), bufferType);
    }

    /**
     * <pre>
     * FileChannelに対してString型のデータをエンコードを指定して一括出力する。
     *
     * </pre>
     *
     * @param channel
     * @param data
     * @param encoding
     * @return 使用したFileChannel
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public static FileChannel appendTo(FileChannel channel, String data, int bufferType, String encoding) throws UnsupportedEncodingException, IOException {
        return appendTo(channel, data.getBytes(encoding), bufferType);
    }

    /**
     * <pre>
     * FileChannelからByteBufferを経由し、文字エンコーディングを指定してCharBufferに文字列を読み込む。
     * その際、エンコーディングに応じてByteBufferのサイズに収まるように、文字列末尾を調整する。
     *
     * 用途としては、マルチバイト文字列の先頭から、指定バイト数以下で収めつつ文字列末尾が
     * 不自然に欠けたりしないようにして順に読み込む場合など。
     *
     * Data-Example:
     *    Shift_JISで "日本国" (93 FA 96 7B 8D 91) を6バイトのByteBufferで読み込む場合、
     *    CharBufferに格納されるデータは6バイトフルで
     *    ・CharBuffer[0] = '日'
     *    ・CharBuffer[1] = '本'
     *    ・CharBuffer[2] = '国'
     *    となる。
     *
     *    Shift_JISで "日本@国" (93 FA 96 7B 40 8D 91) を同様に6バイトのByteBufferで読み込む場合、
     *    ・CharBuffer[0] = '日'
     *    ・CharBuffer[1] = '本'
     *    ・CharBuffer[2] = '@'
     *    となり、6バイトのByteBufferでは '国' の2バイト中の1バイト(8D)だけが対象となるため、
     *    この文字自体を読み込まない。
     *    次の読み込みは、6バイト目の '国' から開始される。
     *
     *
     *
     *
     * </pre>
     *
     * @param channel 入力元となるFileChannel
     * @param buff getReadBuffer()で取得したByteBuffer
     * @param buffType バッファの種類。USE_ALLOCATED_BUFFER / USE_DIRECT_ALLOCATED_BUFFER / USE_MAPMODE_READ_ONLY / USE_MAPMODE_READ_WRITE
     * @param charsetName 文字エンコーディング
     * @return ByteBufferのサイズ以下で収まるように切り出された文字列を格納したCharBuffer
     * @throws IOException
     */
    public static CharBuffer truncTo(FileChannel channel, ByteBuffer buff, int buffType, String charsetName) throws IOException {
        if (channel.position() >= channel.size()) {
            return null;
        }
        CharsetDecoder decoder = Charset.forName(charsetName).newDecoder();
        CharBuffer cBuff = CharBuffer.allocate(buff.capacity());
        if (buffType == USE_ALLOCATED_BUFFER || buffType == USE_DIRECT_ALLOCATED_BUFFER) {
            buff.clear();
            if (channel.read(buff) <= 0) {
                return null;
            }
            buff.flip();
            if (buff.hasRemaining()) {
                decoder.decode(buff, cBuff, true);
                channel.position(channel.position() - buff.remaining());
            }
            return cBuff;
        } else if (buffType == USE_MAPMODE_READ_ONLY || buffType == USE_MAPMODE_READ_WRITE) {
            long chRemain = channel.size() - channel.position();
            if (chRemain == 0) {
                return null;
            } else if (chRemain > buff.capacity()) {
                buff = channel.map(mapmode[buffType], channel.position(), buff.capacity());
            } else {
                buff = channel.map(mapmode[buffType], channel.position(), chRemain);
            }
            if (buff.hasRemaining()) {
                decoder.decode(buff, cBuff, true);
                channel.position(channel.position() + buff.position());
            }
        }
        return cBuff;
    }

    /**
     * <pre>
     *
     * </pre>
     *
     * @param channel 入力元となるFileChannel
     * @param buff getReadBuffer()で取得したByteBuffer
     * @param buffType バッファの種類。USE_ALLOCATED_BUFFER / USE_DIRECT_ALLOCATED_BUFFER / USE_MAPMODE_READ_ONLY / USE_MAPMODE_READ_WRITE
     * @param charsetName 文字エンコーディング
     * @return ByteBufferのサイズ以下で収まるように切り出された文字列
     * @throws IOException
     */
    public static String truncToStr(FileChannel channel, ByteBuffer buff, int buffType, String charsetName) throws IOException {
        CharBuffer cBuff = truncTo(channel, buff, buffType, charsetName);
        if (cBuff != null) {
            return (new String(cBuff.array()));
        } else {
            return null;
        }
    }
}
