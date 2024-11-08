package moxie.ro;

import static antiquity.client.ClientUtils.*;
import moxie.log.BlockAddress;
import moxie.log.DataBlock;
import moxie.log.DataBlockImpl;
import moxie.log.LogInterface;
import moxie.log.LogUtils;
import ostore.db.api.DbGetByGuidReq;
import ostore.db.api.DbGetByGuidResp;
import ostore.db.api.DbKey;
import ostore.db.api.DbPutReq;
import ostore.db.api.DbPutResp;
import ostore.db.api.DbReq;
import ostore.db.api.DbResp;
import ostore.db.impl.GenericStorageManager;
import ostore.util.ByteArrayInputBuffer;
import ostore.util.ByteArrayOutputBuffer;
import ostore.util.CountBuffer;
import ostore.util.InputBuffer;
import ostore.util.OutputBuffer;
import ostore.util.QuickSerializable;
import ostore.util.SHA1Hash;
import seda.sandStorm.api.ConfigDataIF;
import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.api.SinkIF;
import seda.sandStorm.api.StagesInitializedSignal;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import bamboo.lss.ASyncCore;
import bamboo.lss.DustDevil;
import static bamboo.util.Curry.curry;
import static bamboo.util.Curry.Thunk1;
import static bamboo.util.Curry.Thunk2;
import static bamboo.util.Curry.Thunk3;
import static bamboo.util.Curry.Thunk4;
import static bamboo.util.Curry.Thunk5;

public class Publisher extends ostore.util.StandardStage {

    protected PrivateKey skey;

    protected PublicKey pkey;

    protected ASyncCore acore;

    protected LogInterface log_int;

    private static final long HASH_BLOCK_ADDR_MAP_TYPE = 0;

    protected GenericStorageManager db_stage;

    protected File fs_root;

    protected List<String> ignore_paths;

    private AppState prev_app_state = null;

    private VersionInode prev_version_inode = null;

    private int EXTENT_CAPACITY = 4 * 1024 * 1024;

    private int BLOCK_SIZE = 4096;

    private int PUBLISH_TTL_DEFAULT_SEC = 60 * 60;

    private int PUBLISH_TTL_DAY = -1;

    private int PUBLISH_TTL_HOUR = -1;

    private int PUBLISH_TTL_MIN = -1;

    private int PUBLISH_TTL_SEC;

    private static final int DIR_SIZE = 4096;

    public void init(ConfigDataIF config) throws Exception {
        event_types = new Object[] { seda.sandStorm.api.StagesInitializedSignal.class };
        serializable_types = new Object[] { moxie.ro.DirEntry.class, moxie.ro.Inode.class, moxie.ro.IndirectInode.class, moxie.ro.VersionInode.class };
        super.init(config);
        String debug_level_st = config_get_string(config, "DebugLevel");
        Level debug_level = Level.toLevel(debug_level_st, Level.WARN);
        logger.warn("Setting debug level to " + debug_level + ".");
        logger.setLevel(debug_level);
        logger.info("Obtaining reference to external objects...");
        acore = DustDevil.acore_instance();
        log_int = LogInterface.getInstance(my_node_id);
        db_stage = (GenericStorageManager) lookup_stage(config, "StorageManager");
        logger.info("Obtaining reference to external objects...done.");
        logger.info("Reading keys from disk...");
        String pkey_filename = config_get_string(config, "PkeyFilename");
        String skey_filename = config_get_string(config, "SkeyFilename");
        pkey = readRsaPublicKey(pkey_filename);
        skey = readRsaPrivateKey(skey_filename);
        if ((pkey == null) || (skey == null)) throw new Exception("Failed to read key pair from disk: " + "pkey=" + pkey_filename + ", skey=" + skey_filename);
        logger.info("Reading keys from disk...done.");
        logger.info("Checking back-up target...");
        String fs_root_name = config_get_string(config, "FsRoot");
        logger.info("Back-up Target = " + fs_root_name);
        fs_root = new File(fs_root_name);
        if (!fs_root.exists()) {
            logger.fatal("File system root, " + fs_root + ", does not exist.");
            System.exit(1);
        }
        if (!fs_root.isDirectory()) {
            logger.fatal("File system root, " + fs_root + ", is not a directory.");
            System.exit(1);
        }
        ignore_paths = new ArrayList<String>();
        if (config.contains("IgnorePaths")) {
            String config_paths = config_get_string(config, "IgnorePaths");
            StringTokenizer t = new StringTokenizer(config_paths, ",");
            while (t.hasMoreTokens()) {
                File f = new File(t.nextToken());
                String p = f.getCanonicalPath();
                logger.info("Ignore Path = " + p);
                ignore_paths.add(p);
            }
        }
        logger.info("Checking back-up target...done.");
        logger.info("Configuring back-up variables...");
        if (config.contains("ExtentCapacity")) EXTENT_CAPACITY = config_get_int(config, "ExtentCapacity");
        if (config.contains("BlockSize")) BLOCK_SIZE = config_get_int(config, "BlockSize");
        if (config.contains("PublishTTLDays")) {
            PUBLISH_TTL_DAY = config_get_int(config, "PublishTTLDays");
            PUBLISH_TTL_SEC = PUBLISH_TTL_DAY * 24 * 60 * 60;
        } else if (config.contains("PublishTTLHours")) {
            PUBLISH_TTL_HOUR = config_get_int(config, "PublishTTLHours");
            PUBLISH_TTL_SEC = PUBLISH_TTL_HOUR * 60 * 60;
        } else if (config.contains("PublishTTLMins")) {
            PUBLISH_TTL_MIN = config_get_int(config, "PublishTTLMins");
            PUBLISH_TTL_SEC = PUBLISH_TTL_MIN * 60;
        } else {
            PUBLISH_TTL_SEC = PUBLISH_TTL_DEFAULT_SEC;
        }
        logger.warn("EXTENT_CAPACITY  = " + EXTENT_CAPACITY);
        logger.warn("BLOCK_SIZE       = " + BLOCK_SIZE);
        logger.warn("PUBLISH_TTL_DAY  = " + PUBLISH_TTL_DAY);
        logger.warn("PUBLISH_TTL_HOUR = " + PUBLISH_TTL_HOUR);
        logger.warn("PUBLISH_TTL_MIN  = " + PUBLISH_TTL_MIN);
        logger.info("Configuring back-up variables...done.");
        return;
    }

    public void handleEvent(QueueElementIF event) {
        if (event instanceof StagesInitializedSignal) {
            if (log_int == null) log_int = LogInterface.getInstance(my_node_id);
            assert log_int != null : "LogInterface is null for " + my_node_id;
            publish();
        } else {
            BUG("Unexpected event: " + event);
        }
        return;
    }

    public void publish() {
        logger.warn("Archiving filesystem: fs_root=" + fs_root);
        logger.warn("Step 1: open log...");
        Map<String, String> options = new HashMap<String, String>();
        options.put("DataTtlSec", Integer.toString(PUBLISH_TTL_SEC));
        log_int.openLog(pkey, skey, options, publish_open_log_cb);
        return;
    }

    private Thunk1<Boolean> publish_open_log_cb = new Thunk1<Boolean>() {

        public void run(Boolean success) {
            if (success) {
                logger.warn("Step 1: open log done...succeeded.");
                logger.warn("Step 2.1: read previous application state...");
                DurableState.readAppState(db_stage, logger, publish_read_app_state_cb, pkey);
            } else {
                logger.warn("Step 1: open log done...failed.");
                logger.warn("Step 2.1: create log...");
                Map<String, String> options = new HashMap<String, String>();
                options.put("DataTtlSec", Integer.toString(PUBLISH_TTL_SEC));
                options.put("ExtentCapacity", Integer.toString(EXTENT_CAPACITY));
                log_int.createLog(pkey, skey, options, publish_create_log_cb);
            }
            return;
        }
    };

    private Thunk1<AppState> publish_read_app_state_cb = new Thunk1<AppState>() {

        public void run(AppState app_state) {
            prev_app_state = app_state;
            logger.info("Recovered application state: " + app_state);
            logger.warn("Step 2.1: read previous application state...done.");
            if (app_state == null) {
                logger.fatal("Log exists, but no application state found.");
                startPublish();
            } else {
                logger.warn("Step 2.2: read previous version inode...");
                readVersionInode(publish_read_vinode_cb, app_state.vinode_addr);
            }
            return;
        }
    };

    private Thunk1<VersionInode> publish_read_vinode_cb = new Thunk1<VersionInode>() {

        public void run(VersionInode vinode) {
            prev_version_inode = vinode;
            if (vinode == null) BUG("Log and AppState found, but no version inode.");
            logger.warn("Step 2.2: read previous version inode...done.");
            startPublish();
            return;
        }
    };

    private Thunk1<Boolean> publish_create_log_cb = new Thunk1<Boolean>() {

        public void run(Boolean success) {
            logger.warn("Step 2.1: create log...done.");
            startPublish();
            return;
        }
    };

    private void startPublish() {
        logger.warn("Step 3: archive filesystem...");
        try {
            writeDir(publish_write_cb, fs_root);
        } catch (java.io.IOException e) {
            BUG("Unexpected I/O exception: " + e);
        }
        return;
    }

    private Thunk2<BlockAddress, Inode> publish_write_cb = new Thunk2<BlockAddress, Inode>() {

        public void run(BlockAddress dir_inode_addr, Inode dir_inode) {
            logger.warn("Step 3: archive filesystem...done.");
            logger.warn("Step 4: write version index block...");
            writeVersionInode(publish_version_cb, prev_version_inode, dir_inode_addr, dir_inode);
            return;
        }
    };

    private Thunk1<BlockAddress> publish_version_cb = new Thunk1<BlockAddress>() {

        public void run(BlockAddress vinode_addr) {
            logger.warn("Step 4: write version index block...done.");
            logger.warn("Step 5: flush data to infrastructure...");
            flush(curry(publish_flush_cb, vinode_addr));
            return;
        }
    };

    private Thunk1<BlockAddress> publish_flush_cb = new Thunk1<BlockAddress>() {

        public void run(BlockAddress vinode_addr) {
            logger.warn("Step 5: flush data to infrastructure...done");
            logger.warn("Step 6: write app state...");
            AppState new_app_state = new AppState();
            new_app_state.last_runtime = System.currentTimeMillis();
            new_app_state.vinode_addr = vinode_addr;
            DurableState.writeAppState(db_stage, logger, publish_write_app_state_cb, pkey, new_app_state);
            return;
        }
    };

    private Thunk1<Boolean> publish_write_app_state_cb = new Thunk1<Boolean>() {

        public void run(Boolean success) {
            if (!success) logger.fatal("Failed to write application state to database.");
            logger.warn("Step 6: write app state...done.");
            logger.warn("Done archiving filesystem: fs_root=" + fs_root);
            System.exit(0);
            return;
        }
    };

    private void readVersionInode(Thunk1<VersionInode> cb, BlockAddress vinode_addr) {
        log_int.getBlock(pkey, vinode_addr, curry(read_vinode_cb, cb, vinode_addr));
    }

    ;

    private Thunk3<Thunk1<VersionInode>, BlockAddress, Map<BlockAddress, DataBlock>> read_vinode_cb = new Thunk3<Thunk1<VersionInode>, BlockAddress, Map<BlockAddress, DataBlock>>() {

        public void run(Thunk1<VersionInode> cb, BlockAddress vinode_addr, Map<BlockAddress, DataBlock> data_map) {
            assert (data_map.size() == 1);
            DataBlock block = data_map.get(vinode_addr);
            ByteArrayInputBuffer bb = new ByteArrayInputBuffer(block.getData());
            VersionInode version_inode = null;
            try {
                version_inode = new VersionInode(bb);
            } catch (Exception e) {
                logger.info("Failed to reconstruct version inode.");
            }
            cb.run(version_inode);
            return;
        }
    };

    private void writeVersionInode(Thunk1<BlockAddress> cb, VersionInode prev_version_inode, BlockAddress root_dir_addr, Inode root_dir_inode) {
        VersionInode version_inode = new VersionInode(prev_version_inode);
        version_inode.add(root_dir_inode.mtime, root_dir_addr);
        logger.warn("Writing archive index: block=" + version_inode);
        CountBuffer cbuff = new CountBuffer();
        version_inode.serialize(cbuff);
        int dir_size = cbuff.size();
        byte[] buffer = new byte[dir_size];
        ByteArrayOutputBuffer bb = new ByteArrayOutputBuffer(buffer);
        version_inode.serialize(bb);
        DataBlock[] blocks = new DataBlock[] { new DataBlockImpl(buffer) };
        log_int.write(pkey, skey, blocks, curry(write_vinode_cb, cb));
        return;
    }

    private Thunk2<Thunk1<BlockAddress>, BlockAddress[]> write_vinode_cb = new Thunk2<Thunk1<BlockAddress>, BlockAddress[]>() {

        public void run(Thunk1<BlockAddress> cb, BlockAddress[] addr) {
            if (logger.isInfoEnabled()) logger.info("Done writing archive version index: " + "addr=" + addr[0]);
            cb.run(addr[0]);
            return;
        }
    };

    public void writeDir(Thunk2<BlockAddress, Inode> parent_cb, File dir) throws java.io.IOException {
        acore.register_timer(0, curry(write_dir_timer, parent_cb, dir));
        return;
    }

    private Thunk2<Thunk2<BlockAddress, Inode>, File> write_dir_timer = new Thunk2<Thunk2<BlockAddress, Inode>, File>() {

        public void run(Thunk2<BlockAddress, Inode> parent_cb, File dir) {
            logger.warn("Entering directory: dir=" + dir);
            DirState dstate = new DirState(dir);
            dstate.children = new ArrayList<File>(Arrays.asList(dir.listFiles()));
            writeDirChild(parent_cb, dstate);
            return;
        }
    };

    public void writeDirChild(Thunk2<BlockAddress, Inode> parent_cb, DirState dstate) {
        File child = null;
        boolean is_dir = false;
        while ((child == null) && (!dstate.children.isEmpty())) {
            File c = dstate.children.remove(0);
            if (logger.isInfoEnabled()) logger.info("Checking directory child: dir=" + dstate.dir + ", child=" + c);
            if (c.isFile()) {
                if (!c.canRead()) {
                    logger.warn("Cannot read file " + c + ".  Skipping...");
                    continue;
                }
                child = c;
            } else if (c.isDirectory()) {
                if (ignoreDir(c)) {
                    logger.warn("Directory " + c + " appears in " + "IgnorePaths.  Skipping...");
                    continue;
                }
                child = c;
                is_dir = true;
            } else {
                logger.warn("Child " + c + " is not a file or directory.  " + "Skipping...");
            }
        }
        try {
            if (child != null) {
                if (is_dir) {
                    Thunk2<BlockAddress, Inode> write_child_cb = curry(write_dir_child_dir_cb, parent_cb, dstate, child);
                    writeDir(write_child_cb, child);
                } else if (child.isFile()) {
                    Thunk1<BlockAddress> write_child_cb = curry(write_dir_child_file_cb, parent_cb, dstate, child);
                    writeFile(write_child_cb, child);
                }
            } else {
                if (logger.isInfoEnabled()) logger.info("No more children to write: dir=" + dstate.dir);
                dstate.finalizeDirectory();
                writeDirData(parent_cb, dstate);
            }
        } catch (java.io.IOException e) {
            BUG("Unexpected IO error: " + e);
        }
        return;
    }

    private Thunk5<Thunk2<BlockAddress, Inode>, DirState, File, BlockAddress, Inode> write_dir_child_dir_cb = new Thunk5<Thunk2<BlockAddress, Inode>, DirState, File, BlockAddress, Inode>() {

        public void run(Thunk2<BlockAddress, Inode> parent_cb, DirState dstate, File child, BlockAddress child_addr, Inode child_inode) {
            finishWritingChild(parent_cb, dstate, child, child_addr);
            return;
        }
    };

    private Thunk4<Thunk2<BlockAddress, Inode>, DirState, File, BlockAddress> write_dir_child_file_cb = new Thunk4<Thunk2<BlockAddress, Inode>, DirState, File, BlockAddress>() {

        public void run(Thunk2<BlockAddress, Inode> parent_cb, DirState dstate, File child, BlockAddress child_addr) {
            finishWritingChild(parent_cb, dstate, child, child_addr);
            return;
        }
    };

    private void finishWritingChild(Thunk2<BlockAddress, Inode> parent_cb, DirState dstate, File child, BlockAddress child_addr) {
        if (logger.isInfoEnabled()) logger.info("Done writing directory child: dir=" + dstate.dir + ", child=" + child + ", child_addr=" + child_addr);
        dstate.addChild(child.getName(), child.isDirectory(), child_addr);
        if (logger.isInfoEnabled()) logger.info("Look for another child: dir=" + dstate.dir);
        writeDirChild(parent_cb, dstate);
        return;
    }

    protected void writeDirData(Thunk2<BlockAddress, Inode> parent_cb, DirState dstate) {
        if (dstate.bytes_written < dstate.dir_size) {
            LinkedList<DataBlock> block_list = new LinkedList<DataBlock>();
            LinkedList<Integer> offset_list = new LinkedList<Integer>();
            for (int i = 0; i < Inode.getPointerCapacity() && dstate.bytes_written < dstate.dir_size; i++) {
                int num_bytes_tail = (int) (dstate.dir_size - dstate.bytes_written);
                int block_size;
                if (num_bytes_tail < BLOCK_SIZE) {
                    if (logger.isInfoEnabled()) logger.info("Writing partial block: dir=" + dstate.dir + ", dir_size=" + dstate.dir_size + ", offset=" + dstate.bytes_written + ", block_size=" + num_bytes_tail);
                    block_size = num_bytes_tail;
                } else {
                    if (logger.isInfoEnabled()) logger.info("Writing whole block: dir=" + dstate.dir + ", dir_size=" + dstate.dir_size + ", offset=" + dstate.bytes_written);
                    block_size = BLOCK_SIZE;
                }
                dstate.byte_buffer.position(dstate.bytes_written);
                byte[] dir_data = new byte[block_size];
                dstate.byte_buffer.get(dir_data, 0, block_size);
                DataBlock block = new DataBlockImpl(dir_data);
                block_list.add(block);
                offset_list.add(dstate.bytes_written);
                dstate.bytes_written += block_size;
            }
            DataBlock[] blocks = new DataBlock[block_list.size()];
            block_list.toArray(blocks);
            Integer[] offsets = new Integer[offset_list.size()];
            offset_list.toArray(offsets);
            Thunk1<BlockAddress[]> cb = curry(write_dir_data_cb, parent_cb, dstate, 0, offsets);
            writeBlocks(blocks, cb);
        } else {
            if (logger.isInfoEnabled()) logger.info("Writing dir inode: dir=" + dstate.dir + ", inode=" + dstate.inode);
            Thunk1<BlockAddress> cb = curry(write_dir_inode_cb, parent_cb, dstate);
            writeQSBlock(cb, dstate.inode);
        }
        return;
    }

    private Thunk5<Thunk2<BlockAddress, Inode>, DirState, Integer, Integer[], BlockAddress[]> write_dir_data_cb = new Thunk5<Thunk2<BlockAddress, Inode>, DirState, Integer, Integer[], BlockAddress[]>() {

        public void run(Thunk2<BlockAddress, Inode> parent_cb, DirState dstate, Integer index, Integer[] offsets, BlockAddress[] addrs) {
            assert (offsets != null && addrs != null && offsets.length == addrs.length) : " wrong lengths. " + " offsets.len=" + (offsets == null ? -1 : offsets.length) + " addrs.len=" + (addrs == null ? -1 : addrs.length);
            if (logger.isInfoEnabled()) logger.info("Done writing directory block: " + offsets.length + " blocks to dir " + dstate.dir + (index > 0 ? " continue recording address at " + " block num " + index : ""));
            for (int i = index; i < offsets.length; i++) {
                if (logger.isDebugEnabled()) logger.debug("Done writing directory block: " + " dir=" + dstate.dir + ", offsets[" + i + "]=" + offsets[i] + ", addrs[" + i + "]=" + addrs[i]);
                boolean full = dstate.recordBlockAddress(offsets[i], addrs[i]);
                if (full && i < offsets.length - 1) {
                    if (logger.isInfoEnabled()) logger.info("write_dir_data_cb: full=" + full + " begin index=" + index + ", offsets.len=" + offsets.length + ", offsets[" + i + "]=" + offsets[i] + ", writing inode");
                    Runnable cb = curry(write_dir_data_cb, parent_cb, dstate, i + 1, offsets, addrs);
                    writeIndirectInodes(cb, dstate, offsets[i], dstate.dir_size);
                    return;
                }
            }
            Runnable cb = curry(write_dir_iinode_cb, parent_cb, dstate);
            writeIndirectInodes(cb, dstate, offsets[offsets.length - 1], dstate.dir_size);
            return;
        }
    };

    private Thunk2<Thunk2<BlockAddress, Inode>, DirState> write_dir_iinode_cb = new Thunk2<Thunk2<BlockAddress, Inode>, DirState>() {

        public void run(Thunk2<BlockAddress, Inode> parent_cb, DirState dstate) {
            writeDirData(parent_cb, dstate);
            return;
        }
    };

    private Thunk3<Thunk2<BlockAddress, Inode>, DirState, BlockAddress> write_dir_inode_cb = new Thunk3<Thunk2<BlockAddress, Inode>, DirState, BlockAddress>() {

        public void run(Thunk2<BlockAddress, Inode> parent_cb, DirState dstate, BlockAddress addr) {
            if (logger.isInfoEnabled()) logger.info("Done writing directory inode: dir=" + dstate.dir + ", dir_addr=" + addr);
            parent_cb.run(addr, dstate.inode);
            return;
        }
    };

    public void writeFile(Thunk1<BlockAddress> parent_cb, File f) throws java.io.IOException {
        logger.warn("Writing file: file=" + f);
        FileState fstate = new FileState(f);
        writeFileData(parent_cb, fstate);
        return;
    }

    public void writeFileData(Thunk1<BlockAddress> parent_cb, FileState fstate) {
        File f = fstate.file;
        if (fstate.bytes_written < fstate.file_size) {
            LinkedList<DataBlock> block_list = new LinkedList<DataBlock>();
            LinkedList<Integer> offset_list = new LinkedList<Integer>();
            for (int i = 0; i < Inode.getPointerCapacity() && fstate.bytes_written < fstate.file_size; i++) {
                int num_tail_bytes = (int) fstate.file_size - fstate.bytes_written;
                int block_size;
                if (num_tail_bytes < BLOCK_SIZE) {
                    if (logger.isInfoEnabled()) logger.info("Writing partial file block: file=" + f + ", offset=" + fstate.bytes_written + ", block_size=" + num_tail_bytes);
                    block_size = num_tail_bytes;
                } else {
                    if (logger.isInfoEnabled()) logger.info("Writing whole file block: file=" + f + ", offset=" + fstate.bytes_written);
                    block_size = BLOCK_SIZE;
                }
                byte[] block_buffer = readNextBlock(fstate.reader, block_size);
                DataBlock block = new DataBlockImpl(block_buffer);
                block_list.add(block);
                offset_list.add(fstate.bytes_written);
                fstate.bytes_written += block_size;
            }
            DataBlock[] blocks = new DataBlock[block_list.size()];
            block_list.toArray(blocks);
            Integer[] offsets = new Integer[offset_list.size()];
            offset_list.toArray(offsets);
            Thunk1<BlockAddress[]> cb = curry(write_file_data_cb, parent_cb, fstate, 0, offsets);
            writeBlocks(blocks, cb);
        } else {
            try {
                fstate.reader.close();
            } catch (java.io.IOException e) {
                logger.fatal("Failed to close file: file=" + fstate.file);
            }
            if (logger.isInfoEnabled()) logger.info("Writing file inode: file=" + fstate.file + ", inode=" + fstate.inode);
            writeQSBlock(parent_cb, fstate.inode);
        }
        return;
    }

    private Thunk5<Thunk1<BlockAddress>, FileState, Integer, Integer[], BlockAddress[]> write_file_data_cb = new Thunk5<Thunk1<BlockAddress>, FileState, Integer, Integer[], BlockAddress[]>() {

        public void run(Thunk1<BlockAddress> parent_cb, FileState fstate, Integer index, Integer[] offsets, BlockAddress[] addrs) {
            assert (addrs.length == offsets.length) : ("Wrote more blocks than requested.");
            if (logger.isInfoEnabled()) logger.info("Done writing data block: " + offsets.length + " blocks to file " + fstate.file + (index > 0 ? " continue recording address at " + " block num " + index : ""));
            for (int i = index; i < offsets.length; i++) {
                if (logger.isDebugEnabled()) logger.debug("Done writing data block: file " + fstate.file + ", offsets[" + i + "]=" + offsets[i] + ", addrs[" + i + "]=" + addrs[i]);
                boolean full = fstate.recordBlockAddress(offsets[i], addrs[i]);
                if (full && i < offsets.length - 1) {
                    if (logger.isInfoEnabled()) logger.info("write_file_data_cb: full=" + full + " begin index=" + index + ", offsets.len=" + offsets.length + ", offsets[" + i + "]=" + offsets[i] + ", writing inode");
                    Runnable cb = curry(write_file_data_cb, parent_cb, fstate, i + 1, offsets, addrs);
                    writeIndirectInodes(cb, fstate, offsets[i], fstate.file_size);
                    return;
                }
            }
            Runnable cb = curry(write_file_iinode_cb, parent_cb, fstate);
            writeIndirectInodes(cb, fstate, offsets[offsets.length - 1], fstate.file_size);
            return;
        }
    };

    private Thunk2<Thunk1<BlockAddress>, FileState> write_file_iinode_cb = new Thunk2<Thunk1<BlockAddress>, FileState>() {

        public void run(Thunk1<BlockAddress> parent_cb, FileState fstate) {
            writeFileData(parent_cb, fstate);
            return;
        }
    };

    public void writeIndirectInodes(Runnable parent_cb, PublishState state, int offset, long size) {
        boolean done_writing = (offset + BLOCK_SIZE >= size);
        if (logger.isInfoEnabled()) logger.info("Scanning for indirect inodes to write: " + "offset=" + offset + ", done=" + done_writing);
        Thunk1<BlockAddress> cb = curry(write_indirect_inode_cb, parent_cb, state, offset, size);
        if ((state.iinode3 != null) && (state.iinode3.isFull() || done_writing)) {
            if (logger.isDebugEnabled()) logger.debug("Writing 3rd-level indirect inode: inode=" + state.iinode3);
            writeQSBlock(cb, state.iinode3);
        } else if ((state.iinode2 != null) && (state.iinode2.isFull() || done_writing)) {
            if (logger.isDebugEnabled()) logger.debug("Writing 2nd-level indirect inode: inode=" + state.iinode2);
            writeQSBlock(cb, state.iinode2);
        } else if ((state.iinode1 != null) && (state.iinode1.isFull() || done_writing)) {
            if (logger.isDebugEnabled()) logger.debug("Writing 1st-level indirect inode: inode=" + state.iinode1);
            writeQSBlock(cb, state.iinode1);
        } else {
            if (logger.isDebugEnabled()) logger.debug("Found no indirect inodes to write");
            parent_cb.run();
        }
        return;
    }

    private Thunk5<Runnable, PublishState, Integer, Long, BlockAddress> write_indirect_inode_cb = new Thunk5<Runnable, PublishState, Integer, Long, BlockAddress>() {

        public void run(Runnable parent_cb, PublishState state, Integer offset, Long size, BlockAddress addr) {
            if (state.iinode3 != null) {
                state.iinode3 = null;
                state.iinode2.setNextDataPointer(addr);
                if (logger.isDebugEnabled()) logger.debug("write_indirect_inode_cb: addr=" + addr + " to iinode2=" + state.iinode2);
            } else if (state.iinode2 != null) {
                state.iinode2 = null;
                state.iinode1.setNextDataPointer(addr);
                if (logger.isDebugEnabled()) logger.debug("write_indirect_inode_cb: addr=" + addr + " to iinode1=" + state.iinode1);
            } else if (state.iinode1 != null) {
                state.iinode1 = null;
                int inode_ptrs = Inode.getPointerCapacity();
                int iinode_ptrs = IndirectInode.getPointerCapacity();
                int block_num = offset / BLOCK_SIZE;
                if (block_num < inode_ptrs + iinode_ptrs) state.inode.si_inode = addr; else if (block_num < inode_ptrs + iinode_ptrs + (iinode_ptrs * iinode_ptrs)) state.inode.di_inode = addr; else state.inode.ti_inode = addr;
                if (logger.isDebugEnabled()) logger.debug("write_indirect_inode_cb: addr=" + addr + " offset=" + offset + " to inode=" + state.inode);
            }
            writeIndirectInodes(parent_cb, state, offset, size);
            return;
        }
    };

    private void flush(Runnable cb) {
        logger.info("Flushing data to infrastructure.");
        log_int.flush(pkey, skey, curry(flush_cb, cb));
        return;
    }

    private Thunk1<Runnable> flush_cb = new Thunk1<Runnable>() {

        public void run(Runnable cb) {
            logger.warn("Done flushing data to infrastructure.");
            cb.run();
            return;
        }
    };

    public void writeQSBlock(Thunk1<BlockAddress> write_cb, QuickSerializable qs) {
        if (logger.isDebugEnabled()) logger.debug("Writing QS block: block=" + qs);
        CountBuffer cb = new CountBuffer();
        qs.serialize(cb);
        int block_size = cb.size();
        byte[] data = new byte[block_size];
        ByteArrayOutputBuffer bb = new ByteArrayOutputBuffer(data);
        qs.serialize(bb);
        DataBlock[] blocks = new DataBlock[] { new DataBlockImpl(data) };
        writeBlocks(blocks, curry(write_qs_cb, write_cb, qs));
    }

    private Thunk3<Thunk1<BlockAddress>, QuickSerializable, BlockAddress[]> write_qs_cb = new Thunk3<Thunk1<BlockAddress>, QuickSerializable, BlockAddress[]>() {

        public void run(Thunk1<BlockAddress> cb, QuickSerializable qs, BlockAddress[] addrs) {
            assert (addrs.length == 1);
            BlockAddress addr = addrs[0];
            if (logger.isDebugEnabled()) logger.debug("Done writing QS block: block=" + qs + ", addr=" + addr);
            cb.run(addr);
            return;
        }
    };

    private byte[] readNextBlock(FileInputStream reader, int block_size) {
        byte[] block_buffer = new byte[block_size];
        try {
            int block_offset = 0;
            while (block_offset < block_size) block_offset += reader.read(block_buffer, block_offset, block_buffer.length - block_offset);
        } catch (java.io.IOException e) {
            BUG("Failed to read block.");
        }
        return block_buffer;
    }

    private boolean ignoreDir(File dir) {
        String path = "";
        try {
            path = dir.getCanonicalPath();
        } catch (java.io.IOException e) {
            logger.fatal("Could not resolve path " + dir);
            return true;
        }
        for (String s : ignore_paths) if (path.equals(s)) {
            return true;
        }
        return false;
    }

    private void writeBlocks(DataBlock[] blocks, Thunk1<BlockAddress[]> cb) {
        if (logger.isInfoEnabled()) logger.info("Writing blocks to log: num_blocks=" + blocks.length);
        BigInteger[] bnames = new BigInteger[blocks.length];
        Map<Integer, BigInteger> offset_name_map = new HashMap<Integer, BigInteger>();
        for (int i = 0; i < blocks.length; ++i) {
            bnames[i] = LogUtils.computeBlockName(blocks[i]);
            offset_name_map.put(i, bnames[i]);
        }
        if (logger.isInfoEnabled()) logger.info("Checking local db for blocks that have " + "been written previously.");
        Thunk1<BlockAddress[]> db_query_cb = curry(write_blocks_query_db_cb, cb, offset_name_map, blocks, bnames);
        DurableState.query(db_stage, logger, db_query_cb, bnames);
        return;
    }

    private Thunk5<Thunk1<BlockAddress[]>, Map<Integer, BigInteger>, DataBlock[], BigInteger[], BlockAddress[]> write_blocks_query_db_cb = new Thunk5<Thunk1<BlockAddress[]>, Map<Integer, BigInteger>, DataBlock[], BigInteger[], BlockAddress[]>() {

        public void run(Thunk1<BlockAddress[]> cb, Map<Integer, BigInteger> offset_name_map, DataBlock[] blocks, BigInteger[] bnames, BlockAddress[] db_baddrs) {
            Map<BigInteger, BlockAddress> name_addr_map = new HashMap<BigInteger, BlockAddress>();
            Set<DataBlock> to_write_set = new HashSet<DataBlock>();
            for (int i = 0; i < blocks.length; ++i) {
                if (db_baddrs[i] != null) {
                    name_addr_map.put(bnames[i], db_baddrs[i]);
                } else {
                    to_write_set.add(blocks[i]);
                }
            }
            if (logger.isDebugEnabled()) logger.debug("Done checking local db: " + "blocks_already_written=" + name_addr_map.size() + ", blocks_to_write=" + to_write_set.size());
            if (to_write_set.size() > 0) {
                DataBlock[] to_write = new DataBlock[to_write_set.size()];
                to_write = to_write_set.toArray(to_write);
                Thunk1<BlockAddress[]> write_cb = curry(write_blocks_cb, cb, offset_name_map, name_addr_map);
                log_int.write(pkey, skey, to_write, write_cb, flush_blocks_cb);
            } else {
                BlockAddress[] written = new BlockAddress[0];
                write_blocks_cb.run(cb, offset_name_map, name_addr_map, written);
            }
            return;
        }
    };

    private Thunk4<Thunk1<BlockAddress[]>, Map<Integer, BigInteger>, Map<BigInteger, BlockAddress>, BlockAddress[]> write_blocks_cb = new Thunk4<Thunk1<BlockAddress[]>, Map<Integer, BigInteger>, Map<BigInteger, BlockAddress>, BlockAddress[]>() {

        public void run(Thunk1<BlockAddress[]> cb, Map<Integer, BigInteger> offset_hash_map, Map<BigInteger, BlockAddress> hash_addr_map, BlockAddress[] addrs) {
            if (logger.isDebugEnabled()) logger.debug("Done writing blocks to log.");
            for (BlockAddress addr : addrs) {
                BigInteger hash = addr.getBlockName();
                hash_addr_map.put(hash, addr);
            }
            BlockAddress[] write_addrs = new BlockAddress[offset_hash_map.size()];
            TreeSet<Integer> sorted_offsets = new TreeSet<Integer>(offset_hash_map.keySet());
            for (Integer i : sorted_offsets) {
                BigInteger hash = offset_hash_map.get(i);
                BlockAddress addr = hash_addr_map.get(hash);
                if (logger.isDebugEnabled()) logger.debug("Write block result: offset=" + i + ", hash=" + guidToString(hash) + ", addr=" + addr);
                write_addrs[i] = addr;
            }
            cb.run(write_addrs);
            return;
        }
    };

    private Thunk1<BlockAddress> flush_blocks_cb = new Thunk1<BlockAddress>() {

        public void run(BlockAddress addr) {
            DurableState.recordFlushedBlock(db_stage, logger, addr);
            return;
        }
    };

    private class FileState extends PublishState {

        public File file;

        public long file_size;

        public int bytes_written;

        public FileInputStream reader;

        public Map<Integer, BlockAddress> data_map;

        public FileState(File f) throws java.io.IOException {
            super();
            file = f;
            file_size = f.length();
            bytes_written = 0;
            reader = new FileInputStream(f);
            data_map = new HashMap<Integer, BlockAddress>();
            inode.size = file_size;
            return;
        }
    }

    private class DirState extends PublishState {

        public File dir;

        public List<File> children;

        public Map<String, DirEntry> dir_entries;

        public int dir_size;

        public int bytes_written;

        public ByteBuffer byte_buffer;

        public DirState(File d) {
            super();
            dir = d;
            dir_entries = new HashMap<String, DirEntry>();
            return;
        }

        public void addChild(String name, boolean is_dir, BlockAddress addr) {
            DirEntry de = new DirEntry(name, is_dir, addr);
            dir_entries.put(name, de);
            return;
        }

        public void finalizeDirectory() {
            CountBuffer cb = new CountBuffer();
            cb.add(dir_entries.size());
            for (DirEntry de : dir_entries.values()) cb.add(de);
            dir_size = cb.size();
            byte[] dir_bytes = new byte[dir_size];
            ByteArrayOutputBuffer bb = new ByteArrayOutputBuffer(dir_bytes);
            bb.add(dir_entries.size());
            for (DirEntry de : dir_entries.values()) bb.add(de);
            byte_buffer = ByteBuffer.wrap(dir_bytes);
            inode.size = dir_size;
            return;
        }
    }

    private class PublishState {

        public Inode inode;

        public IndirectInode iinode1;

        public IndirectInode iinode2;

        public IndirectInode iinode3;

        public PublishState() {
            inode = new Inode();
            inode.uid = inode.gid = 0;
            inode.atime = inode.mtime = inode.ctime = 1000;
            inode.size = 0;
            return;
        }

        public boolean recordBlockAddress(int offset, BlockAddress addr) {
            int block_num = offset / BLOCK_SIZE;
            int inode_ptrs = Inode.getPointerCapacity();
            int iinode_ptrs = IndirectInode.getPointerCapacity();
            boolean full = false;
            if (block_num < inode_ptrs) {
                inode.setDataPointer(block_num, addr);
            } else if (block_num < inode_ptrs + iinode_ptrs) {
                if (iinode1 == null) {
                    iinode1 = new IndirectInode(true);
                }
                iinode1.setDataPointer(block_num - inode_ptrs, addr);
                full = iinode1.isFull();
            } else if (block_num < inode_ptrs + iinode_ptrs + (iinode_ptrs * iinode_ptrs)) {
                if (iinode1 == null) {
                    iinode1 = new IndirectInode(false);
                }
                if (iinode2 == null) {
                    iinode2 = new IndirectInode(true);
                }
                block_num -= inode_ptrs;
                block_num %= iinode_ptrs;
                iinode2.setDataPointer(block_num, addr);
                full = iinode2.isFull();
            } else {
                assert (block_num < inode_ptrs + iinode_ptrs + (iinode_ptrs * iinode_ptrs) + (iinode_ptrs * iinode_ptrs * iinode_ptrs)) : ("File too big: offset=" + offset + ", addr=" + addr + ".");
                if (iinode1 == null) {
                    iinode1 = new IndirectInode(false);
                }
                if (iinode2 == null) {
                    iinode2 = new IndirectInode(false);
                }
                if (iinode3 == null) {
                    iinode3 = new IndirectInode(true);
                }
                block_num -= inode_ptrs;
                block_num -= iinode_ptrs;
                block_num -= iinode_ptrs * iinode_ptrs;
                block_num %= iinode_ptrs;
                iinode3.setDataPointer(block_num, addr);
                full = iinode3.isFull();
            }
            return full;
        }
    }
}
