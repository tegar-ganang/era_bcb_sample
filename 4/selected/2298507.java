package moxie.rw;

import moxie.log.BlockAddress;
import moxie.log.BlockAddressImpl;
import antiquity.client.ClientUtils;
import antiquity.util.XdrUtils;
import ostore.util.ByteArrayInputBuffer;
import ostore.util.ByteArrayOutputBuffer;
import ostore.util.CountBuffer;
import ostore.util.QSException;
import ostore.util.SecureHash;
import ostore.util.SHA1Hash;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import seda.sandStorm.api.ConfigDataIF;
import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.api.StagesInitializedSignal;
import static bamboo.util.Curry.*;

public class DataFormatterTester extends ostore.util.StandardStage {

    protected LogFormatter log_formatter;

    protected DataFormatter data_formatter;

    protected Random rnd;

    private Map<String, Long> resolver;

    private int num_requests = 0;

    private int MAX_REQUESTS = 100;

    private int MAX_FILES = 3;

    private int MAX_ACTIONS = 5;

    private int BLOCK_SIZE = 4096;

    private double READ_RATIO = 0.5;

    private double APPEND_RATIO = 0.33;

    private double INSERT_RATIO = 0.33;

    private Map<String, FileState> state_cache;

    public void init(ConfigDataIF config) throws Exception {
        event_types = new Object[] { seda.sandStorm.api.StagesInitializedSignal.class };
        serializable_types = new Object[] { Inode.class, BlockAddressImpl.class };
        super.init(config);
        String debug_level_st = config.getString("DebugLevel");
        Level debug_level = Level.toLevel(debug_level_st, Level.WARN);
        logger.warn("Setting debug level to " + debug_level + ".");
        logger.setLevel(debug_level);
        logger.info("Obtaining reference to external objects...");
        String log_formatter_name = config.getString("LogFormatter");
        log_formatter = (LogFormatter) lookup_stage(config, "LogFormatter");
        assert (log_formatter != null);
        data_formatter = (DataFormatter) lookup_stage(config, "DataFormatter");
        assert (data_formatter != null);
        logger.info("Obtaining reference to external objects...done.");
        int seed = 10;
        rnd = new Random(seed);
        state_cache = new HashMap<String, FileState>();
        resolver = new HashMap<String, Long>();
        return;
    }

    public void handleEvent(QueueElementIF event) {
        if (event instanceof StagesInitializedSignal) createLog(); else BUG("Unexpected event: " + event);
        return;
    }

    private void createLog() {
        logger.info("Creating log");
        log_formatter.createLog(create_log_cb);
        return;
    }

    private Thunk1<Boolean> create_log_cb = new Thunk1<Boolean>() {

        public void run(Boolean success) {
            logger.info("Done creating log: success=" + success);
            assert (success);
            sendRequest();
            return;
        }
    };

    private void sendRequest() {
        num_requests++;
        if (num_requests >= MAX_REQUESTS) {
            endTest();
        } else {
            logger.info("Submitting request #" + num_requests);
            String target = "file_" + rnd.nextInt(MAX_FILES);
            FileState fstate = state_cache.get(target);
            if (fstate == null) {
                fstate = new FileState(target);
                state_cache.put(target, fstate);
                submitUpdate(fstate);
            } else {
                double rw_dist = rnd.nextDouble();
                if (rw_dist < READ_RATIO) submitRead(fstate); else submitUpdate(fstate);
            }
        }
        return;
    }

    private void endTest() {
        logger.info("Test complete.");
        System.exit(0);
        return;
    }

    private void submitUpdate(FileState fstate) {
        int num_actions = rnd.nextInt(MAX_ACTIONS) + 1;
        Action[] actions = new Action[num_actions];
        for (int i = 0; i < num_actions; ++i) {
            UserData data = new UserData();
            data.type = UserDataType.DIRECT;
            data.block = new UserDataBlock();
            data.block.iv = new byte[moxie.CRYPT_IV_SIZE];
            Arrays.fill(data.block.iv, (byte) 0x01);
            data.block.data = new byte[BLOCK_SIZE];
            data.block.user_length = BLOCK_SIZE;
            data.block.data_length = BLOCK_SIZE;
            rnd.nextBytes(data.block.data);
            actions[i] = new Action();
            Action action = actions[i];
            if (fstate.size == 0) {
                action.type = ActionType.APPEND;
                action.data = new UserData[] { data };
                fstate.size += BLOCK_SIZE;
                fstate.blocks.add(0, new SHA1Hash(data.block.data));
            } else {
                double op_dist = rnd.nextDouble();
                if (op_dist < APPEND_RATIO) {
                    action.type = ActionType.APPEND;
                    action.data = new UserData[] { data };
                    fstate.size += BLOCK_SIZE;
                    fstate.blocks.add(new SHA1Hash(data.block.data));
                } else if (op_dist < APPEND_RATIO + INSERT_RATIO) {
                    int block_num = rnd.nextInt(fstate.size / BLOCK_SIZE);
                    action.type = ActionType.INSERT;
                    action.data = new UserData[] { data };
                    action.offset = block_num * BLOCK_SIZE;
                    fstate.size += BLOCK_SIZE;
                    fstate.blocks.add(block_num, new SHA1Hash(data.block.data));
                } else {
                    int block_num = rnd.nextInt(fstate.size / BLOCK_SIZE);
                    action.type = ActionType.DELETE;
                    action.offset = block_num * BLOCK_SIZE;
                    action.length = BLOCK_SIZE;
                    fstate.size -= BLOCK_SIZE;
                    fstate.blocks.remove(block_num);
                }
            }
        }
        Update update = new Update();
        update.actions = actions;
        logger.info("Update - handling new update: update=" + XdrUtils.toString(update));
        Long inum = resolver.get(fstate.name);
        if (inum == null) {
            logger.info("Update - object does not exist, allocating " + "new inode: name=" + fstate.name);
            Thunk2<Boolean, Inode> cb = curry(update_alloc_inode_cb, fstate, update);
            log_formatter.allocInode(cb);
        } else {
            logger.info("Update - reading inode: name=" + fstate.name);
            Thunk2<BlockAddress, Inode> cb = curry(update_read_inode_cb, fstate, update);
            log_formatter.readInode(fstate.name, inum, cb);
        }
        return;
    }

    private Thunk4<FileState, Update, Boolean, Inode> update_alloc_inode_cb = new Thunk4<FileState, Update, Boolean, Inode>() {

        public void run(FileState fstate, Update update, Boolean success, Inode inode) {
            assert (success);
            logger.info("Update - allocated inode: name=" + fstate.name + " inumber=" + inode.inumber);
            resolver.put(fstate.name, inode.inumber);
            logger.info("Update - creating new object: name=" + fstate.name);
            Thunk1<Etree> cb = curry(update_create_cb, fstate, update, inode);
            data_formatter.create(cb);
            return;
        }
    };

    private Thunk4<FileState, Update, Inode, Etree> update_create_cb = new Thunk4<FileState, Update, Inode, Etree>() {

        public void run(FileState fstate, Update update, Inode inode, Etree etree) {
            logger.info("Update - created new object: name=" + fstate.name);
            executeUpdate(fstate, update, inode, etree);
            return;
        }
    };

    private Thunk4<FileState, Update, BlockAddress, Inode> update_read_inode_cb = new Thunk4<FileState, Update, BlockAddress, Inode>() {

        public void run(FileState fstate, Update update, BlockAddress inode_addr, Inode inode) {
            logger.info("Update - read inode: name=" + fstate.name + " inumber=" + inode.inumber);
            logger.info("Update - reading etree root: name=" + fstate.name + " addr=" + inode.data_ptr);
            Thunk1<byte[]> cb = curry(update_read_etree_cb, fstate, update, inode);
            log_formatter.readBlock(inode.data_ptr, cb);
            return;
        }
    };

    private Thunk4<FileState, Update, Inode, byte[]> update_read_etree_cb = new Thunk4<FileState, Update, Inode, byte[]>() {

        public void run(FileState fstate, Update update, Inode inode, byte[] etree_bytes) {
            logger.info("Update - read etree root: name=" + fstate.name);
            Etree etree = null;
            try {
                ByteArrayInputBuffer buffer = new ByteArrayInputBuffer(etree_bytes);
                etree = (Etree) buffer.nextObject();
            } catch (QSException e) {
                BUG("Failed to recover etree: " + e);
            }
            executeUpdate(fstate, update, inode, etree);
            return;
        }
    };

    private void executeUpdate(FileState fstate, Update update, Inode inode, Etree etree) {
        logger.info("Update - applying update: update=" + XdrUtils.toString(update));
        Thunk1<Boolean> cb = curry(update_cb, fstate, update, inode, etree);
        data_formatter.update(etree, update, log_formatter, cb);
    }

    private Thunk5<FileState, Update, Inode, Etree, Boolean> update_cb = new Thunk5<FileState, Update, Inode, Etree, Boolean>() {

        public void run(FileState fstate, Update update, Inode inode, Etree etree, Boolean success) {
            logger.info("Update - done applying update: update=" + XdrUtils.toString(update) + " success=" + success);
            Thunk1<BlockAddress> cb = curry(commit_data_cb, fstate, update, inode);
            data_formatter.commit(etree, log_formatter, cb);
            return;
        }
    };

    private Thunk4<FileState, Update, Inode, BlockAddress> commit_data_cb = new Thunk4<FileState, Update, Inode, BlockAddress>() {

        public void run(FileState fstate, Update update, Inode inode, BlockAddress root_addr) {
            logger.info("Update - committed etree: name=" + fstate.name);
            inode.size = fstate.size;
            inode.type = Inode.InodeType.TYPE_FILE;
            inode.data_ptr = root_addr;
            Thunk1<BlockAddress> cb = curry(commit_inode_cb, fstate);
            log_formatter.writeInode(fstate.name, inode, cb);
            return;
        }
    };

    private Thunk2<FileState, BlockAddress> commit_inode_cb = new Thunk2<FileState, BlockAddress>() {

        public void run(FileState fstate, BlockAddress inode_addr) {
            logger.info("Update - wrote inode: name=" + fstate.name);
            sendRequest();
            return;
        }
    };

    private void submitRead(FileState fstate) {
        logger.info("Read - handling new read: name=" + fstate.name);
        Long inum = resolver.get(fstate.name);
        assert (inum != null);
        logger.info("Read - reading inode: name=" + fstate.name + " inumber=" + inum);
        Thunk2<BlockAddress, Inode> cb = curry(read_inode_cb, fstate);
        log_formatter.readInode(fstate.name, inum, cb);
        return;
    }

    private Thunk3<FileState, BlockAddress, Inode> read_inode_cb = new Thunk3<FileState, BlockAddress, Inode>() {

        public void run(FileState fstate, BlockAddress inode_addr, Inode inode) {
            logger.info("Read - read inode: name=" + fstate.name + " inumber=" + inode.inumber);
            logger.info("Read - reading etree root: name=" + fstate.name + " addr=" + inode.data_ptr);
            Thunk1<byte[]> cb = curry(read_etree_cb, fstate, inode);
            log_formatter.readBlock(inode.data_ptr, cb);
            return;
        }
    };

    private Thunk3<FileState, Inode, byte[]> read_etree_cb = new Thunk3<FileState, Inode, byte[]>() {

        public void run(FileState fstate, Inode inode, byte[] etree_bytes) {
            logger.info("Read - read etree root: name=" + fstate.name);
            Etree etree = null;
            try {
                ByteArrayInputBuffer buffer = new ByteArrayInputBuffer(etree_bytes);
                etree = (Etree) buffer.nextObject();
            } catch (QSException e) {
                BUG("Failed to recover etree: " + e);
            }
            int local_size = fstate.blocks.size() * BLOCK_SIZE;
            int remote_size = etree.getSize();
            if (local_size != remote_size) logger.fatal("Read - File has wrong size: remote_size=" + remote_size + " local_size=" + local_size);
            logger.info("Read - submitting read: name=" + fstate.name);
            Thunk2<Map<BlockAddress, Object>, Set<BlockAddress>> cb = curry(read_blocks_cb, fstate, inode, etree);
            data_formatter.readBlocks(etree, log_formatter, cb);
            return;
        }
    };

    private Thunk5<FileState, Inode, Etree, Map<BlockAddress, Object>, Set<BlockAddress>> read_blocks_cb = new Thunk5<FileState, Inode, Etree, Map<BlockAddress, Object>, Set<BlockAddress>>() {

        public void run(FileState fstate, Inode inode, Etree etree, Map<BlockAddress, Object> data_map, Set<BlockAddress> missing_blocks) {
            logger.info("Done - read blocks: name=" + fstate.name + " num_blocks=" + data_map.size());
            int block_num = 0;
            read_bytes.run(fstate, block_num, etree, data_map);
            return;
        }
    };

    private Thunk4<FileState, Integer, Etree, Map<BlockAddress, Object>> read_bytes = new Thunk4<FileState, Integer, Etree, Map<BlockAddress, Object>>() {

        public void run(FileState fstate, Integer block_num, Etree etree, Map<BlockAddress, Object> data_map) {
            logger.info("Reading bytes: target=" + fstate.name + " block_num=" + block_num);
            Thunk2<UserData, Integer> cb = curry(read_bytes_cb, fstate, block_num, etree, data_map);
            int offset = block_num * BLOCK_SIZE;
            data_formatter.readBytes(etree, offset, data_map, cb);
            return;
        }
    };

    private Thunk6<FileState, Integer, Etree, Map<BlockAddress, Object>, UserData, Integer> read_bytes_cb = new Thunk6<FileState, Integer, Etree, Map<BlockAddress, Object>, UserData, Integer>() {

        public void run(FileState fstate, Integer block_num, Etree etree, Map<BlockAddress, Object> data_map, UserData user_data, Integer data_offset) {
            assert (user_data != null);
            assert (data_offset == 0);
            byte[] data = user_data.block.data;
            SecureHash read_hash = new SHA1Hash(data);
            SecureHash write_hash = fstate.blocks.get(block_num);
            if (!write_hash.equals(read_hash)) logger.fatal("Bytes read do not match bytes written: " + "write_hash=" + write_hash + " read_hash=" + read_hash); else logger.info("Done reading bytes: target=" + fstate.name + " block_num=" + block_num);
            block_num++;
            if ((block_num * BLOCK_SIZE) < fstate.size) read_bytes.run(fstate, block_num, etree, data_map); else {
                logger.info("Done processing read request: " + "target=" + fstate.name);
                sendRequest();
            }
            return;
        }
    };

    private static class FileState {

        public String name;

        public int size;

        public List<SecureHash> blocks;

        public FileState(String name) {
            this.name = name;
            this.size = 0;
            this.blocks = new LinkedList<SecureHash>();
            return;
        }
    }
}
