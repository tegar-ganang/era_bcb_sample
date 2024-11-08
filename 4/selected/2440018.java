package moxie.log;

import antiquity.util.XdrUtils;
import static antiquity.util.XdrUtils.serialize;
import static antiquity.util.XdrUtils.deserialize;
import static antiquity.client.ClientUtils.printPkey;
import static antiquity.client.ClientUtils.guidToString;
import org.acplt.oncrpc.XdrInt;
import org.acplt.oncrpc.XdrAble;
import org.acplt.oncrpc.OncRpcException;
import org.acplt.oncrpc.XdrEncodingStream;
import org.acplt.oncrpc.XdrDecodingStream;
import ostore.util.QuickSerializable;
import ostore.util.CountBuffer;
import ostore.util.ByteUtils;
import ostore.util.LruMap;
import ostore.util.Pair;
import seda.sandStorm.api.ConfigDataIF;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.LinkedHashMap;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.lang.reflect.Array;
import static bamboo.util.Curry.curry;
import static bamboo.util.Curry.Thunk1;
import static bamboo.util.Curry.Thunk4;
import static bamboo.util.Curry.Thunk5;
import static bamboo.util.Curry.Thunk6;

/**
 * @author Hakim Weatherspoon
 * @version $Id: FileLog.java,v 1.3 2007/09/12 19:18:43 hweather Exp $ 
 */
public class FileLog extends LogInterface {

    private static final int FLUSH_TIMEOUT_MS_DEFAULT = 30 * 1000;

    private static final int FLUSH_THRESHOLD_BYTES_DEFAULT = 1024 * 1024;

    private static final boolean FLUSH_ON_WRITE_DEFAULT = false;

    private Map<PublicKey, LogState> state_map;

    private Map<String, FileChannel> open_writeable_files;

    private LruMap<String, FileChannel> open_readonly_files;

    private Map<BlockAddress, Thunk1<DataBlock>> pending_reads;

    private Map<BlockAddress, DataBlock> pending_writes;

    private Set<BlockAddress> pending_reservations;

    private int MAX_EXTENT_CAPACITY = 2 * 1024 * 1024 * 1024;

    private int BUFFER_SIZE = 2 * 1024 * 1024;

    private String root_dir;

    private boolean disable_caching;

    private Thread _shutdownHook;

    private boolean _destroyed;

    private boolean _closed;

    private boolean _compute_block_hash;

    private boolean _verify_block_hash;

    private CountBuffer _count_buffer;

    private ByteBuffer _byte_buffer;

    /** Constructor: Creates a new <code>FileLog</code> stage. */
    public FileLog() {
        super();
        try {
            ostore.util.TypeTable.register_type(LogState.class);
        } catch (Exception e) {
            BUG(e);
        }
        state_map = new HashMap<PublicKey, LogState>();
        open_writeable_files = new HashMap<String, FileChannel>();
        open_readonly_files = new LruMap<String, FileChannel>(256);
        pending_reads = new HashMap<BlockAddress, Thunk1<DataBlock>>();
        pending_writes = new HashMap<BlockAddress, DataBlock>();
        pending_reservations = new HashSet<BlockAddress>();
        _destroyed = false;
        _closed = false;
        _count_buffer = new CountBuffer();
        _byte_buffer = ByteBuffer.allocate(BUFFER_SIZE);
        return;
    }

    /** Specified by seda.sandStorm.api.EventHandlerIF */
    public void init(ConfigDataIF config) throws Exception {
        super.init(config);
        assert config.contains("FileLogRootDir") : "Need to define FileLogRootDir in cfg file\n";
        root_dir = config_get_string(config, "FileLogRootDir");
        File root = new File(root_dir);
        if (!root.exists() && !root.mkdirs()) BUG("FileLogRootDir does not exist: root_dir=" + root_dir);
        if (!root.isDirectory()) BUG("FileLogRootDir is not a directory: root_dir=" + root_dir);
        if (!root.canRead()) BUG("FileLogRootDir is not readable: root_dir=" + root_dir);
        disable_caching = false;
        if (config.contains("DisableCaching")) disable_caching = config_get_boolean(config, "DisableCaching");
        logger.info("DisableCaching = " + disable_caching);
        if (config.contains("ComputeBlockHash")) _compute_block_hash = config_get_boolean(config, "ComputeBlockHash");
        _verify_block_hash = _compute_block_hash;
        if (config.contains("VerifyBlockHash")) _verify_block_hash = config_get_boolean(config, "VerifyBlockHash");
        logger.warn("Setting flush configuration...");
        flush_timeout_ms = FLUSH_TIMEOUT_MS_DEFAULT;
        if (config.contains("FlushTimeoutMs")) flush_timeout_ms = config_get_int(config, "FlushTimeoutMs");
        flush_threshold_bytes = FLUSH_THRESHOLD_BYTES_DEFAULT;
        if (config.contains("FlushThresholdBytes")) flush_threshold_bytes = config_get_int(config, "FlushThresholdBytes");
        flush_on_write = FLUSH_ON_WRITE_DEFAULT;
        if (config.contains("FlushOnWrite")) flush_on_write = config_get_boolean(config, "FlushOnWrite");
        logger.warn("flush_timeout_ms=" + flush_timeout_ms);
        logger.warn("flush_threshold_bytes=" + flush_threshold_bytes);
        logger.warn("flush_on_write=" + flush_on_write);
        logger.warn("Setting flush configuration...done.");
        _shutdownHook = new Thread() {

            public void run() {
                destroyIndirection();
            }
        };
        Runtime.getRuntime().addShutdownHook(_shutdownHook);
        return;
    }

    private void destroyIndirection() {
        if (!_destroyed) {
            _destroyed = true;
            if (logger.isInfoEnabled()) logger.info("FileLog: shutdown hook called, closing open files");
            Runnable destroy_cb = new Runnable() {

                public void run() {
                }
            };
            acore.register_timer(0, destroy_cb);
            destroy();
            if (logger.isInfoEnabled()) logger.info("FileLog: shutdown hook checkpoint complete");
        } else {
            logger.warn("FileLog: shutdown hook already called, dropping ");
        }
    }

    public void destroy() {
        if (_closed) {
            logger.warn("destroy: already closed open extents");
            return;
        }
        _closed = true;
        if (logger.isInfoEnabled()) logger.info("destroy: closing all open extents");
        for (Map.Entry<String, FileChannel> entry : open_writeable_files.entrySet()) {
            String extent_key = entry.getKey();
            FileChannel fchannel = entry.getValue();
            try {
                fchannel.close();
            } catch (IOException e) {
                BUG(e);
            }
            if (logger.isInfoEnabled()) logger.info("destroy: closed writeable extent " + extent_key);
        }
        open_writeable_files.clear();
        for (String extent_key : open_readonly_files.keySet()) {
            FileChannel fchannel = open_readonly_files.get(extent_key);
            try {
                fchannel.close();
            } catch (IOException e) {
                BUG(e);
            }
            if (logger.isInfoEnabled()) logger.info("destroy: closed readonly extent " + extent_key);
        }
        open_readonly_files.clear();
        if (logger.isInfoEnabled()) logger.info("destroy: done");
    }

    /** Specified by moxie.log.LogInterface */
    public void getHead(PublicKey pkey, Thunk1<BlockAddress> get_head_cb) {
        PrivateKey skey = null;
        boolean create = false;
        Map<String, String> options = null;
        Thunk1<Boolean> success_cb = null;
        Runnable open_fn = curry(_open, pkey, skey, create, options, get_head_cb, success_cb);
        open_fn.run();
    }

    /** Specified by moxie.log.LogInterface */
    public void openLog(PublicKey pkey, PrivateKey skey, Map<String, String> options, Thunk1<Boolean> success_cb) {
        boolean create = false;
        Thunk1<BlockAddress> get_head_cb = null;
        Runnable open_fn = curry(_open, pkey, skey, create, options, get_head_cb, success_cb);
        open_fn.run();
    }

    /** Specified by moxie.log.LogInterface */
    public void createLog(PublicKey pkey, PrivateKey skey, Map<String, String> options, Thunk1<Boolean> success_cb) {
        boolean create = true;
        Thunk1<BlockAddress> get_head_cb = null;
        Runnable open_fn = curry(_open, pkey, skey, create, options, get_head_cb, success_cb);
        open_fn.run();
    }

    public Thunk6<PublicKey, PrivateKey, Boolean, Map<String, String>, Thunk1<BlockAddress>, Thunk1<Boolean>> _open = new Thunk6<PublicKey, PrivateKey, Boolean, Map<String, String>, Thunk1<BlockAddress>, Thunk1<Boolean>>() {

        public void run(PublicKey pkey, PrivateKey skey, Boolean create, Map<String, String> options, Thunk1<BlockAddress> get_head_cb, Thunk1<Boolean> success_cb) {
            long start_us = now_us();
            if (logger.isDebugEnabled()) {
                logger.debug("open log " + printPkey(pkey) + ". create=" + create + " get_head=" + (get_head_cb != null) + " options=" + options);
                start_us = now_us();
            }
            Runnable cb = null;
            LogState state = readState(pkey);
            if (get_head_cb != null) {
                BlockAddress head = (state != null ? state.getHeadAddr() : (BlockAddress) null);
                cb = curry(get_head_cb, head);
            } else {
                assert success_cb != null;
                if (!create || state != null) {
                    cb = curry(success_cb, state != null);
                } else {
                    int capacity = MAX_EXTENT_CAPACITY;
                    String cap_key = "ExtentCapacity";
                    if (options.containsKey(cap_key)) {
                        String cap_val = options.get(cap_key);
                        capacity = Integer.parseInt(cap_val);
                        MAX_EXTENT_CAPACITY = capacity;
                    }
                    state = new LogState();
                    state.setHeadAddr(_compute_block_hash ? BlockAddressImpl.NULL_BLOCK_ADDR_OFFSET_HASH : BlockAddressImpl.NULL_BLOCK_ADDR_OFFSET);
                    state.setNextOffset(0L);
                    assert state_map.put(pkey, state) == null;
                    String key_prefix = root_dir + "/" + printPkey(pkey);
                    String state_key = key_prefix + ".ext";
                    FileChannel fchannel = createExtent(state_key);
                    assert writeState(fchannel, state) > 0;
                    cb = curry(success_cb, true);
                }
            }
            acore.register_timer(0L, cb);
            if (logger.isInfoEnabled()) logger.info("open: done opening log " + printPkey(pkey) + ". create=" + create + " get_head=" + (get_head_cb != null) + " in " + ((now_us() - start_us) / 1000.0) + "ms");
        }
    };

    /** Specified by moxie.log.LogInterface */
    public void write(PublicKey pkey, PrivateKey skey, DataBlock[] blocks, Thunk1<BlockAddress[]> app_write_cb) {
        Thunk1<BlockAddress> app_flush_cb = null;
        Runnable write_fn = curry(_write, pkey, skey, blocks, app_write_cb, app_flush_cb);
        write_fn.run();
    }

    /** Specified by moxie.log.LogInterface */
    public void write(PublicKey pkey, PrivateKey skey, DataBlock[] blocks, Thunk1<BlockAddress[]> app_write_cb, Thunk1<BlockAddress> app_flush_cb) {
        Runnable write_fn = curry(_write, pkey, skey, blocks, app_write_cb, app_flush_cb);
        write_fn.run();
    }

    private Thunk5<PublicKey, PrivateKey, DataBlock[], Thunk1<BlockAddress[]>, Thunk1<BlockAddress>> _write = new Thunk5<PublicKey, PrivateKey, DataBlock[], Thunk1<BlockAddress[]>, Thunk1<BlockAddress>>() {

        public void run(PublicKey pkey, PrivateKey skey, DataBlock[] blocks, Thunk1<BlockAddress[]> app_write_cb, Thunk1<BlockAddress> app_flush_cb) {
            long start_us = now_us();
            if (logger.isDebugEnabled()) {
                String s = new String();
                for (DataBlock block : blocks) s += " " + block;
                logger.debug("write to log " + printPkey(pkey) + " " + blocks.length + " blocks: " + s);
            }
            BlockAddress[] addrs = (blocks.length > 0 ? appendFile(pkey, blocks) : new BlockAddress[0]);
            if (app_write_cb != null) {
                acore.register_timer(0L, curry(app_write_cb, addrs));
            }
            if (app_flush_cb != null) {
                for (BlockAddress addr : addrs) {
                    acore.register_timer(0L, curry(app_flush_cb, addr));
                }
            }
            if (logger.isInfoEnabled()) logger.info("write: done writing " + blocks.length + " blocks in " + ((now_us() - start_us) / 1000.0) + "ms");
        }
    };

    public void reserve(PublicKey pkey, PrivateKey skey, DataBlockReservation[] blocks, Thunk1<BlockAddress[]> app_write_cb) {
        throw new NoSuchMethodError();
    }

    public void reserve(PublicKey pkey, PrivateKey skey, DataBlockReservation[] blocks, Thunk1<BlockAddress[]> app_write_cb, Thunk1<BlockAddress> app_flush_cb) {
        throw new NoSuchMethodError();
    }

    public void fill(PublicKey pkey, PrivateKey skey, BlockAddress[] addrs, DataBlock[] blocks, Thunk1<BlockAddress[]> app_write_cb) {
        throw new NoSuchMethodError();
    }

    public void fill(PublicKey pkey, PrivateKey skey, BlockAddress[] addrs, DataBlock[] blocks, Thunk1<BlockAddress[]> app_write_cb, Thunk1<BlockAddress> app_flush_cb) {
        throw new NoSuchMethodError();
    }

    public void flush(PublicKey pkey, PrivateKey skey, Runnable log_cb) {
        acore.register_timer(0L, log_cb);
    }

    public void getBlock(PublicKey pkey, BlockAddress addr, Thunk1<Map<BlockAddress, DataBlock>> log_cb) {
        BlockAddress[] addrs = new BlockAddress[] { addr };
        boolean block = true;
        Runnable read_fn = curry(_read, pkey, addrs, block, log_cb);
        read_fn.run();
    }

    public void getBlocks(PublicKey pkey, BlockAddress[] addrs, Thunk1<Map<BlockAddress, DataBlock>> log_cb) {
        boolean block = true;
        Runnable read_fn = curry(_read, pkey, addrs, block, log_cb);
        read_fn.run();
    }

    public void getBlock(PublicKey pkey, BlockAddress addr, boolean block, Thunk1<Map<BlockAddress, DataBlock>> log_cb) {
        BlockAddress[] addrs = new BlockAddress[] { addr };
        Runnable read_fn = curry(_read, pkey, addrs, block, log_cb);
        read_fn.run();
    }

    public void getBlocks(PublicKey pkey, BlockAddress[] addrs, boolean block, Thunk1<Map<BlockAddress, DataBlock>> log_cb) {
        Runnable read_fn = curry(_read, pkey, addrs, block, log_cb);
        read_fn.run();
    }

    private Thunk4<PublicKey, BlockAddress[], Boolean, Thunk1<Map<BlockAddress, DataBlock>>> _read = new Thunk4<PublicKey, BlockAddress[], Boolean, Thunk1<Map<BlockAddress, DataBlock>>>() {

        public void run(PublicKey pkey, BlockAddress[] addrs, Boolean block, Thunk1<Map<BlockAddress, DataBlock>> log_cb) {
            long start_us = now_us();
            if (logger.isDebugEnabled()) {
                String s = new String();
                for (BlockAddress addr : addrs) s += addr + " ";
                logger.debug("read from log " + printPkey(pkey) + " blocks " + s);
            }
            Map<BlockAddress, DataBlock> map = null;
            if (addrs != null && addrs.length > 0) map = readData(pkey, addrs); else map = new HashMap<BlockAddress, DataBlock>();
            acore.register_timer(0L, curry(log_cb, map));
            if (logger.isInfoEnabled()) logger.info("read: done reading " + addrs.length + " blocks in " + ((now_us() - start_us) / 1000.0) + "ms");
        }
    };

    /**********************************************************************/
    private FileChannel createExtent(String key) {
        long start_us = -1L;
        if (logger.isDebugEnabled()) {
            logger.debug("createExtent: creating " + key);
            start_us = now_us();
        }
        File f = new File(key);
        assert !f.exists() : "createExtent: " + key + " alread exists";
        try {
            assert f.createNewFile() : "createExtent: could not create " + key;
        } catch (IOException e) {
            BUG("createExtent: createNEwFile ERROR " + e);
        }
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(f, "rw");
        } catch (IOException e) {
            BUG("createExtent: could not creat file " + e);
        }
        FileChannel fchannel = file.getChannel();
        assert open_writeable_files.put(key, fchannel) == null;
        if (logger.isDebugEnabled()) logger.debug("createExtent: created " + key + " in " + ((now_us() - start_us) / 1000.0) + "ms");
        return fchannel;
    }

    private long writeState(FileChannel fchannel, LogState state) {
        long start_us = now_us();
        _count_buffer.reset();
        serialize(_count_buffer, state);
        int len = _count_buffer.size();
        _byte_buffer.clear();
        serialize(_byte_buffer, new XdrInt(len));
        serialize(_byte_buffer, state);
        _byte_buffer.flip();
        int state_offset = 0;
        try {
            if (fchannel.position() != state_offset) {
                if (logger.isDebugEnabled()) logger.debug("writeState: fchannel.position=" + fchannel.position() + " != offset=" + state_offset + ", moving position to offset.");
                fchannel.position(state_offset);
            }
        } catch (IOException e) {
            BUG("writeState: could not position  " + fchannel + " to offset=" + state_offset + ". ERROR " + e);
        }
        int bytes_written = 0;
        while (bytes_written < _byte_buffer.limit()) {
            try {
                bytes_written += fchannel.write(_byte_buffer);
            } catch (IOException e) {
                BUG("ERROR " + e + " while writing " + fchannel);
            }
        }
        assert bytes_written == _byte_buffer.limit() : "bytes_written=" + bytes_written + " != buf.len=" + _byte_buffer.limit();
        long position = -1;
        try {
            if (fchannel.position() > bytes_written) fchannel.truncate(bytes_written);
            position = fchannel.position();
        } catch (IOException e) {
            BUG("writeState: could not position or truncate " + fchannel + ", bytes_written " + bytes_written + ". ERROR " + e);
        }
        if (logger.isDebugEnabled()) logger.debug("writeState: wrote state to log " + state + " in " + ((now_us() - start_us) / 1000.0) + "ms");
        return position;
    }

    private BlockAddress[] appendFile(PublicKey pkey, DataBlock[] blocks) {
        LogState state = state_map.get(pkey);
        assert state != null : "State for " + printPkey(pkey) + " is null";
        String key_prefix = root_dir + "/" + printPkey(pkey);
        String state_key = key_prefix + ".ext";
        FileChannel state_fchannel = open_writeable_files.get(state_key);
        assert state_fchannel != null : state_key + " is not open and should be";
        String key = key_prefix + "_" + state.getHeadAddr().getSeqNum() + ".ext";
        FileChannel fchannel = open_writeable_files.get(key);
        long next_offset = state.getNextOffset();
        if ((!_compute_block_hash && state.getHeadAddr().equals(BlockAddressImpl.NULL_BLOCK_ADDR_OFFSET)) || (_compute_block_hash && state.getHeadAddr().equals(BlockAddressImpl.NULL_BLOCK_ADDR_OFFSET_HASH))) {
            assert fchannel == null : "channel for " + key + " is not null. " + fchannel;
            state.setHeadAddr(_compute_block_hash ? BlockAddressImpl.ZERO_BLOCK_ADDR_OFFSET_HASH : BlockAddressImpl.ZERO_BLOCK_ADDR_OFFSET);
            state.setNextOffset(0L);
            key = key_prefix + "_" + state.getHeadAddr().getSeqNum() + ".ext";
            fchannel = createExtent(key);
            next_offset = state.getHeadAddr().getOffset();
            open_writeable_files.put(key, fchannel);
        }
        assert fchannel != null : key + " is not open and should be";
        try {
            assert next_offset == fchannel.size() : "next_offset=" + next_offset + " != fchannel.size=" + fchannel.size() + ". state=" + state + " fchannel=" + fchannel;
            if (fchannel.position() != fchannel.size()) {
                if (logger.isDebugEnabled()) logger.debug("appendFile: fchannel.position=" + fchannel.position() + " != size=" + fchannel.size() + ", moving position to size.");
                fchannel.position(fchannel.size());
            }
        } catch (IOException e) {
            BUG("appendFile: could not execute cmd file.position  " + fchannel + ". ERROR " + e);
        }
        BlockAddress[] addrs = new BlockAddress[blocks.length];
        for (int i = 0; i < blocks.length; i++) {
            _byte_buffer.clear();
            int num_blocks_to_write = 0;
            int num_bytes_to_write = 0;
            long serialize_us = -1L;
            if (logger.isDebugEnabled()) serialize_us = now_us();
            if (logger.isDebugEnabled()) logger.debug("appendFile: (1) i=" + i + " buffer=" + _byte_buffer);
            for (int j = i; j < blocks.length; j++) {
                _count_buffer.reset();
                serialize(_count_buffer, blocks[j]);
                int len = _count_buffer.size() + ByteUtils.SIZE_INT;
                long file_len = -1;
                try {
                    file_len = fchannel.size();
                } catch (IOException e) {
                    BUG("appendFile: " + fchannel + ". ERROR " + e);
                }
                if (num_bytes_to_write == 0 && (len + file_len) > MAX_EXTENT_CAPACITY) {
                    if (logger.isDebugEnabled()) logger.debug("appendFile: after writing " + j + " blocks" + ", creating new extent " + printPkey(pkey) + "_" + (state.getHeadAddr().getSeqNum() + 1L));
                    assert open_writeable_files.remove(key) == fchannel;
                    Pair victim = open_readonly_files.put(key, fchannel);
                    if (victim != null) {
                        String victim_key = (String) victim.first();
                        FileChannel victim_channel = (FileChannel) victim.second();
                        if (logger.isDebugEnabled()) logger.debug("appendFile: evicting from open channel cache," + " closing channel to extent " + victim_key);
                        assert victim_channel.isOpen();
                        try {
                            victim_channel.close();
                        } catch (IOException e) {
                            BUG("appendFile: could not close victim file=" + victim_key + ", channel=" + victim_channel);
                        }
                    }
                    next_offset = 0L;
                    BlockAddress addr = null;
                    BigInteger null_guid = BigInteger.ZERO;
                    if (_compute_block_hash) addr = new BlockAddressImpl(state.getHeadAddr().getSeqNum() + 1, next_offset, null_guid); else addr = new BlockAddressImpl(state.getHeadAddr().getSeqNum() + 1, next_offset);
                    state.setHeadAddr(addr);
                    state.setNextOffset(next_offset);
                    key = key_prefix + "_" + state.getHeadAddr().getSeqNum() + ".ext";
                    fchannel = createExtent(key);
                }
                try {
                    file_len = fchannel.size();
                } catch (IOException e) {
                    BUG("appendFile: " + fchannel + ". ERROR " + e);
                }
                if (((num_bytes_to_write + len) <= _byte_buffer.limit()) && (num_bytes_to_write + len + file_len) <= MAX_EXTENT_CAPACITY) {
                    BlockAddress addr = null;
                    if (_compute_block_hash) {
                        BigInteger hash = LogUtils.computeBlockName(blocks[j]);
                        addr = new BlockAddressImpl(state.getHeadAddr().getSeqNum(), next_offset, hash);
                    } else {
                        addr = new BlockAddressImpl(state.getHeadAddr().getSeqNum(), next_offset);
                    }
                    addrs[j] = addr;
                    next_offset += _count_buffer.size() + ByteUtils.SIZE_INT;
                    num_blocks_to_write++;
                    num_bytes_to_write += _count_buffer.size() + ByteUtils.SIZE_INT;
                    serialize(_byte_buffer, new XdrInt(_count_buffer.size()));
                    _byte_buffer.position(_byte_buffer.position() + _count_buffer.size());
                    if (logger.isDebugEnabled()) logger.debug("appendFile: i=" + i + " j=" + j + " num_bytes_to_write=" + num_bytes_to_write + " buf.position=" + _byte_buffer.position() + " buf.limit=" + _byte_buffer.limit());
                } else {
                    if (logger.isDebugEnabled()) logger.debug("appendFile: extent " + printPkey(pkey) + "_" + state.getHeadAddr().getSeqNum() + " is full. " + " addrs[j=" + j + "-1]=" + addrs[j - 1] + " numb_blocks_to_write=" + num_blocks_to_write + " num_bytes_to_write=" + num_bytes_to_write + " next_offset=" + next_offset + " state=" + state);
                    break;
                }
            }
            assert num_blocks_to_write > 0;
            _byte_buffer.rewind();
            for (int j = i; j < (i + num_blocks_to_write); j++) {
                _byte_buffer.position(_byte_buffer.position() + ByteUtils.SIZE_INT);
                serialize(_byte_buffer, blocks[j]);
                if (logger.isDebugEnabled()) logger.debug("appendFile: i=" + i + " j=" + j + " num_bytes_to_write=" + num_bytes_to_write + " buf.position=" + _byte_buffer.position() + " buf.limit=" + _byte_buffer.limit());
            }
            if (logger.isDebugEnabled()) logger.debug("appendFile: (2) i=" + i + " buffer=" + _byte_buffer + " num_blocks_to_write=" + num_blocks_to_write + " num_bytes_to_write=" + num_bytes_to_write);
            _byte_buffer.flip();
            assert num_bytes_to_write == _byte_buffer.limit() : "num_bytes_to_write=" + num_bytes_to_write + " != buf.len=" + _byte_buffer.limit();
            long write_us = -1L;
            if (logger.isDebugEnabled()) {
                logger.debug("appendFile: writing " + num_bytes_to_write + " to log " + key);
                write_us = now_us();
                serialize_us = write_us - serialize_us;
            }
            int bytes_written = 0;
            while (bytes_written < num_bytes_to_write) {
                try {
                    bytes_written += fchannel.write(_byte_buffer);
                } catch (IOException e) {
                    BUG("ERROR " + e + " while writing " + fchannel);
                }
                if (logger.isDebugEnabled()) logger.debug("appendFile: wrote " + bytes_written + " to log " + key);
            }
            assert bytes_written == num_bytes_to_write : "bytes_written=" + bytes_written + " != buf.len=" + num_bytes_to_write;
            long position = -1;
            try {
                position = fchannel.position();
            } catch (IOException e) {
                BUG("appendFile: " + fchannel + ". ERROR " + e);
            }
            assert (state.getNextOffset() + num_bytes_to_write) == position : "(next_offset=" + state.getNextOffset() + "+num_bytes_to_write=" + num_bytes_to_write + ")=" + (state.getNextOffset() + num_bytes_to_write) + " != fchannel.position=" + position + ", fchannel=" + fchannel;
            if (logger.isDebugEnabled()) logger.debug("appendFile: done writing " + num_bytes_to_write + " bytes to log " + key + ".  serialize=" + (serialize_us / 1000.0) + "ms, write=" + ((now_us() - write_us) / 1000.0) + "ms, new file position/size=" + position);
            state.setNextOffset(state.getNextOffset() + num_bytes_to_write);
            if (flush_on_write) {
                long start_us = now_us();
                try {
                    fchannel.force(false);
                } catch (IOException e) {
                    BUG("appendFile: could not force " + fchannel + " ERROR " + e);
                }
                if (logger.isDebugEnabled()) logger.debug("appendFile: took " + ((now_us() - start_us) / 1000.0) + "ms to force " + num_bytes_to_write + " bytes to disk.");
            }
            i += num_blocks_to_write - 1;
        }
        state.setHeadAddr(addrs[addrs.length - 1]);
        assert state.getHeadAddr().getOffset() >= 0 && state.getHeadAddr().getOffset() < state.getNextOffset();
        writeState(state_fchannel, state);
        return addrs;
    }

    private LogState readState(PublicKey pkey) {
        long start_us = now_us();
        boolean cached = false;
        LogState state = null;
        if (state_map.containsKey(pkey)) {
            state = state_map.get(pkey);
            cached = true;
        } else {
            String key_prefix = root_dir + "/" + printPkey(pkey);
            String key = key_prefix + ".ext";
            File f = new File(key);
            if (f.exists()) {
                RandomAccessFile file = null;
                try {
                    file = new RandomAccessFile(f, "rw");
                } catch (IOException e) {
                    BUG("readState: error opening file " + key + ". " + e);
                }
                FileChannel fchannel = file.getChannel();
                open_writeable_files.put(key, fchannel);
                state = readState(fchannel);
                assert state_map.put(pkey, state) == null;
            }
        }
        if (logger.isInfoEnabled()) logger.debug("readState: read state from " + (cached ? "cache " : "log ") + state + " in " + ((now_us() - start_us) / 1000.0) + "ms");
        return state;
    }

    private LogState readState(FileChannel fchannel) {
        BlockAddress addr = (_compute_block_hash ? BlockAddressImpl.ZERO_BLOCK_ADDR_OFFSET_HASH : BlockAddressImpl.ZERO_BLOCK_ADDR_OFFSET);
        BlockAddress[] addrs = new BlockAddress[] { addr };
        Map<BlockAddress, LogState> map = readFile(fchannel, addrs, LogState.class);
        LogState state = map.get(addrs[0]);
        assert state != null;
        return state;
    }

    private Map<BlockAddress, DataBlock> readData(FileChannel fchannel, BlockAddress[] addrs) {
        Map<BlockAddress, ? extends DataBlock> map = readFile(fchannel, addrs, DataBlockImpl.class);
        Map<BlockAddress, DataBlock> map2 = null;
        if (map != null) map2 = new LinkedHashMap<BlockAddress, DataBlock>(map);
        if (_compute_block_hash && _verify_block_hash) {
            for (Map.Entry<BlockAddress, DataBlock> entry : map2.entrySet()) {
                BlockAddress addr = entry.getKey();
                DataBlock block = entry.getValue();
                BigInteger hash = LogUtils.computeBlockName(block);
                assert hash.equals(addr.getBlockName()) : "hash(block)=0x" + guidToString(hash) + " != addr.hash=0x" + guidToString(addr.getBlockName()) + ". addr=" + addr + " block=" + block;
            }
        }
        return map2;
    }

    private Map<BlockAddress, DataBlock> readData(PublicKey pkey, BlockAddress[] addrs) {
        String key_prefix = root_dir + "/" + printPkey(pkey);
        Map<Long, SortedSet<BlockAddress>> map = new LinkedHashMap<Long, SortedSet<BlockAddress>>();
        Map<BlockAddress, DataBlock> map2 = new LinkedHashMap<BlockAddress, DataBlock>();
        for (BlockAddress addr : addrs) {
            SortedSet<BlockAddress> set = map.get((long) addr.getSeqNum());
            if (set == null) {
                set = new TreeSet<BlockAddress>();
                map.put((long) addr.getSeqNum(), set);
            }
            set.add(addr);
            map2.put(addr, (DataBlock) null);
        }
        for (Map.Entry<Long, SortedSet<BlockAddress>> entry : map.entrySet()) {
            Long seq_num = entry.getKey();
            SortedSet<BlockAddress> set = entry.getValue();
            BlockAddress[] addrs2 = new BlockAddress[set.size()];
            set.toArray(addrs2);
            String key = key_prefix + "_" + seq_num + ".ext";
            FileChannel fchannel = null;
            if (open_writeable_files.containsKey(key)) {
                fchannel = open_writeable_files.get(key);
            } else if (open_readonly_files.containsKey(key)) {
                fchannel = open_readonly_files.get(key);
            } else {
                File f = new File(key);
                assert f.exists() : key + " does not exist";
                RandomAccessFile file = null;
                try {
                    file = new RandomAccessFile(f, "r");
                } catch (IOException e) {
                    BUG("readData: could not creat file " + e);
                }
                fchannel = file.getChannel();
                Pair victim = open_readonly_files.put(key, fchannel);
                if (victim != null) {
                    String victim_key = (String) victim.first();
                    FileChannel victim_channel = (FileChannel) victim.second();
                    assert victim_channel.isOpen();
                    try {
                        victim_channel.close();
                    } catch (IOException e) {
                        BUG("readData: could not close victim file=" + victim_key + ", channel=" + victim_channel);
                    }
                }
            }
            Map<BlockAddress, DataBlockImpl> blocks = readFile(fchannel, addrs2, DataBlockImpl.class);
            map2.putAll(blocks);
            if (_compute_block_hash && _verify_block_hash) {
                for (Map.Entry<BlockAddress, DataBlockImpl> entry2 : blocks.entrySet()) {
                    BlockAddress addr = entry2.getKey();
                    DataBlock block = entry2.getValue();
                    BigInteger hash = LogUtils.computeBlockName(block);
                    assert hash.equals(addr.getBlockName()) : "hash(block)=0x" + guidToString(hash) + " != addr.hash=0x" + guidToString(addr.getBlockName()) + ". addr=" + addr + " block=" + block;
                }
            }
        }
        return map2;
    }

    private <T extends XdrAble> Map<BlockAddress, T> readFile(FileChannel fchannel, BlockAddress[] addrs, Class<T> clazz) {
        Map<BlockAddress, T> blocks = new LinkedHashMap<BlockAddress, T>();
        for (int i = 0; i < addrs.length; i++) {
            try {
                if (fchannel.position() != addrs[i].getOffset()) {
                    if (logger.isDebugEnabled()) logger.debug("readFile: fchannel.position=" + fchannel.position() + " != offset=" + addrs[i].getOffset() + ", moving position to offset.");
                    fchannel.position(addrs[i].getOffset());
                }
            } catch (IOException e) {
                BUG("readFile: could not position  " + fchannel + " to addrs[" + i + "]=" + addrs[i] + ". ERROR " + e);
            }
            _byte_buffer.clear();
            int bytes_read = 0;
            _byte_buffer.limit(ByteUtils.SIZE_INT + 4096);
            while (bytes_read < ByteUtils.SIZE_INT && _byte_buffer.hasRemaining()) {
                int read = -2;
                try {
                    read = fchannel.read(_byte_buffer);
                } catch (IOException e) {
                    BUG("ERROR " + e + " while reading " + fchannel);
                }
                if (read < 0) BUG("readFile: read returned " + read + ", could not read state " + fchannel);
                bytes_read += read;
            }
            assert bytes_read >= ByteUtils.SIZE_INT : "bytes_read=" + bytes_read + " < " + ByteUtils.SIZE_INT + ". addrs[" + i + "]=" + addrs[i];
            _byte_buffer.position(0);
            int len = deserialize(_byte_buffer, XdrInt.class).intValue();
            assert len > 0 : "len=" + len + "<= 0. addr[" + i + "]=" + addrs[i];
            _byte_buffer.clear();
            _byte_buffer.position(bytes_read);
            _byte_buffer.limit(len + ByteUtils.SIZE_INT);
            if (logger.isDebugEnabled() && false) logger.debug("readFile: (1) bytes_read=" + bytes_read + " len=" + len + " addrs[" + i + "]=" + addrs[i] + " buffer=" + _byte_buffer + (!_byte_buffer.hasArray() ? "" : " bytes=" + ByteUtils.print_bytes(_byte_buffer.array(), _byte_buffer.arrayOffset(), len)));
            while (bytes_read < len + ByteUtils.SIZE_INT && _byte_buffer.hasRemaining()) {
                int read = 0;
                try {
                    read = fchannel.read(_byte_buffer);
                } catch (IOException e) {
                    BUG("ERROR " + e + " while reading " + fchannel);
                }
                if (read == -1) BUG("readFile: could not read state " + fchannel);
                bytes_read += read;
            }
            assert bytes_read >= len + ByteUtils.SIZE_INT : "bytes_read=" + bytes_read + " < (len=" + len + " + " + ByteUtils.SIZE_INT + ")=" + (len + ByteUtils.SIZE_INT) + ". addrs[" + i + "]=" + addrs[i];
            if (logger.isDebugEnabled() && false) logger.debug("readFile: (2) bytes_read=" + bytes_read + " len=" + len + " addrs[" + i + "]=" + addrs[i] + " buffer=" + _byte_buffer);
            _byte_buffer.flip();
            _byte_buffer.position(ByteUtils.SIZE_INT);
            blocks.put(addrs[i], deserialize(_byte_buffer, clazz));
        }
        return blocks;
    }

    protected static class LogState implements QuickSerializable, XdrAble {

        private BlockAddress head_addr;

        private long next_offset;

        public LogState() {
            return;
        }

        public LogState(ostore.util.InputBuffer buffer) throws ostore.util.QSException {
            head_addr = (BlockAddress) buffer.nextObject();
            next_offset = buffer.nextLong();
            return;
        }

        public LogState(XdrDecodingStream buffer) throws OncRpcException, IOException {
            xdrDecode(buffer);
        }

        public void serialize(ostore.util.OutputBuffer buffer) {
            buffer.add(head_addr);
            buffer.add(next_offset);
            return;
        }

        public void xdrEncode(XdrEncodingStream buffer) throws OncRpcException, IOException {
            head_addr.xdrEncode(buffer);
            buffer.xdrEncodeLong(next_offset);
            return;
        }

        public void xdrDecode(XdrDecodingStream buffer) throws OncRpcException, IOException {
            head_addr = new BlockAddressImpl(buffer);
            next_offset = buffer.xdrDecodeLong();
        }

        public void setNextOffset(long o) {
            next_offset = o;
        }

        public long getNextOffset() {
            return next_offset;
        }

        public void setHeadAddr(BlockAddress h) {
            head_addr = h;
        }

        public BlockAddress getHeadAddr() {
            return head_addr;
        }

        public String toString() {
            String s = "(LogState" + " head=" + head_addr + " next_offset=" + next_offset + ")";
            return s;
        }
    }
}
