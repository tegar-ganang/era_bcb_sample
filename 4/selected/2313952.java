package moxie.rw;

import fuse.FuseFS;
import antiquity.util.XdrUtils;
import ostore.util.ByteArrayInputBuffer;
import ostore.util.ByteUtils;
import ostore.util.QSException;
import ostore.util.SecureHash;
import ostore.util.SHA1Hash;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import seda.sandStorm.api.ConfigDataIF;
import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.api.StagesInitializedSignal;
import org.apache.log4j.Level;
import static bamboo.util.Curry.*;

public class DataCacheManager extends ostore.util.StandardStage implements CacheManager {

    private static final int CACHE_PATH_LENGTH = 12;

    protected static long fhandle_cntr = 0;

    protected boolean stages_initialized = false;

    protected boolean disable_caching;

    protected UpdateEncoder updater;

    protected long mount_time = -1L;

    protected String cache_root;

    protected Map<String, ObjectState> state_map;

    private Map<String, List<Runnable>> blocked_ops;

    public void init(ConfigDataIF config) throws Exception {
        event_types = new Object[] { seda.sandStorm.api.StagesInitializedSignal.class };
        super.init(config);
        String debug_level_st = config.getString("DebugLevel");
        Level debug_level = Level.toLevel(debug_level_st, Level.WARN);
        logger.warn("Setting debug level to " + debug_level + ".");
        logger.setLevel(debug_level);
        cache_root = config_get_string(config, "CacheRoot");
        if (cache_root == null) BUG("CacheRoot not defined.");
        File root = new File(cache_root);
        if (!root.exists()) BUG("CacheRoot does not exist: cache_root=" + cache_root);
        if (!root.isDirectory()) BUG("CacheRoot is not a directory: cache_root=" + cache_root);
        if (!root.canRead()) BUG("CacheRoot is not readable: cache_root=" + cache_root);
        disable_caching = false;
        if (config.contains("DisableCaching")) disable_caching = config.getBoolean("DisableCaching");
        logger.info("DisableCaching = " + disable_caching);
        logger.info("Obtaining references to other stages...");
        updater = (UpdateEncoder) lookup_stage(config, "UpdateEncoder");
        assert (updater != null);
        logger.info("Obtaining references to other stages...done.");
        state_map = new HashMap<String, ObjectState>();
        blocked_ops = new HashMap<String, List<Runnable>>();
        return;
    }

    public void handleEvent(QueueElementIF event) {
        if (event instanceof StagesInitializedSignal) {
            logger.info("Received StagesInitializedSignal.");
            stages_initialized = true;
        } else {
            BUG("Unexpected event: " + event);
        }
        return;
    }

    public void setMountTime() {
        assert (mount_time == -1L);
        mount_time = System.currentTimeMillis();
        logger.warn("Setting mount time: time=" + mount_time);
        return;
    }

    public void blockUntilStable(String path, Runnable cb) {
        block_stable.run(path, cb);
        return;
    }

    private Thunk2<String, Runnable> block_stable = new Thunk2<String, Runnable>() {

        public void run(String path, Runnable cb) {
            ObjectState state = state_map.get(path);
            if ((state != null) && (state.update_in_progress)) {
                List<Runnable> waiters = blocked_ops.get(path);
                if (waiters == null) {
                    waiters = new LinkedList<Runnable>();
                    blocked_ops.put(path, waiters);
                }
                Runnable wait_cb = curry(block_stable, path, cb);
                waiters.add(wait_cb);
                return;
            }
            cb.run();
            return;
        }
    };

    private void signalWaiters(String path) {
        List<Runnable> waiters = null;
        synchronized (blocked_ops) {
            if (blocked_ops.containsKey(path)) waiters = blocked_ops.remove(path);
        }
        if (waiters != null) {
            if (logger.isDebugEnabled()) logger.debug("Signaling waiters: path=" + path);
            for (Runnable w : waiters) {
                w.run();
            }
        }
        return;
    }

    public void getCacheStatus(String path, Thunk2<CacheStatus, VersionId> cb) {
        if (logger.isInfoEnabled()) logger.info("Computing cache status: file=" + path);
        get_cache_status.run(path, cb);
        return;
    }

    private Thunk2<String, Thunk2<CacheStatus, VersionId>> get_cache_status = new Thunk2<String, Thunk2<CacheStatus, VersionId>>() {

        public void run(String path, Thunk2<CacheStatus, VersionId> cb) {
            CacheStatus status = getCacheStatus(path);
            VersionId version_id = null;
            if (status == CacheStatus.BLOCKED) {
                logger.warn("Cache check blocked: path=" + path);
                List<Runnable> waiters = blocked_ops.get(path);
                if (waiters == null) {
                    waiters = new LinkedList<Runnable>();
                    blocked_ops.put(path, waiters);
                }
                Runnable get_status_cb = curry(get_cache_status, path, cb);
                waiters.add(get_status_cb);
                return;
            }
            if (status == CacheStatus.STALE) {
                ObjectState state = state_map.get(path);
                assert (state != null) : ("Cannot find object state: path=" + path);
                String cache_path = state.getCachePath();
                version_id = getCachedVersionId(cache_path);
            }
            cb.run(status, version_id);
            return;
        }
    };

    private CacheStatus getCacheStatus(String path) {
        ObjectState state = state_map.get(path);
        assert (state != null) : ("Attempt to open object with no attributes: path=" + path);
        String cache_path = state.getCachePath();
        VersionId version_id = getCachedVersionId(cache_path);
        CacheStatus status = CacheStatus.UNCACHED;
        if (XdrUtils.equals(version_id, state.ver)) status = CacheStatus.CACHED; else if (!XdrUtils.equals(version_id, MoxieUtils.MOXIE_VERSION_ID_NULL)) status = CacheStatus.STALE;
        if (disable_caching) {
            status = CacheStatus.UNCACHED;
        }
        synchronized (state) {
            if (state.update_in_progress) status = CacheStatus.BLOCKED;
        }
        return status;
    }

    public void getAttributes(String path, Thunk1<Metadata> cb) {
        if (logger.isInfoEnabled()) logger.info("Fetching attributes: path=" + path);
        Metadata md = null;
        ObjectState state = state_map.get(path);
        if (state != null) {
            md = state.md;
            if (state.ts < mount_time) {
                state_map.remove(path);
                md = null;
            }
        }
        if (disable_caching) {
            md = null;
        }
        cb.run(md);
        return;
    }

    public void recordAttributes(String path, Metadata md, VersionId ver, Runnable cb) {
        if (logger.isInfoEnabled()) logger.info("Recording attributes: path=" + path + " metadata=" + XdrUtils.toString(md) + " version=" + XdrUtils.toString(ver));
        ObjectState state = new ObjectState(md, ver);
        state_map.put(path, state);
        if (cb != null) {
            cb.run();
        }
        return;
    }

    public void updateDataVersionId(String path, VersionId ver) {
        if (logger.isInfoEnabled()) logger.info("Updating version id for cached data: path=" + path + " version=" + XdrUtils.toString(ver));
        ObjectState state = state_map.get(path);
        assert (state != null);
        String cache_path = state.getCachePath();
        writeCachedVersionId(cache_path, ver);
        return;
    }

    public void recordUpdateResult(String path, VersionId ver, Runnable cb) {
        if (logger.isInfoEnabled()) logger.info("Recording update version: path=" + path + " version=" + XdrUtils.toString(ver));
        ObjectState state = state_map.get(path);
        assert (state != null);
        String cache_path = state.getCachePath();
        writeCachedVersionId(cache_path, ver);
        synchronized (state) {
            assert (state.update_in_progress);
            state.update_in_progress = false;
            state.ver = ver;
        }
        signalWaiters(path);
        if (cb != null) {
            cb.run();
        }
        return;
    }

    private int createNewCacheObject(String path, Metadata md, VersionId ver) {
        Runnable cb = null;
        recordAttributes(path, md, ver, cb);
        ObjectState state = state_map.get(path);
        assert (state != null) : ("Creating object with no previous reference: path=" + path);
        String cache_path = state.getCachePath();
        writeCachedVersionId(cache_path, ver);
        boolean created = false;
        try {
            File f = new File(cache_path);
            File parent = f.getParentFile();
            parent.mkdirs();
            created = f.createNewFile();
        } catch (Exception e) {
            logger.warn("failed to create file: " + e);
            created = false;
        }
        int status = MoxieStatus.MOXIE_STATUS_OK;
        if (!created) {
            logger.warn("failed to create file");
            status = MoxieStatus.MOXIE_STATUS_IO;
        }
        return status;
    }

    private Directory readDir(String path) {
        ObjectState state = state_map.get(path);
        assert (state != null);
        String cache_path = state.getCachePath();
        Directory dir = null;
        try {
            File f = new File(cache_path);
            int dir_size = (int) f.length();
            ByteBuffer buffer = ByteBuffer.allocate(dir_size);
            FileChannel fchannel = state.getChannel();
            fchannel.position(0);
            while (buffer.hasRemaining()) {
                int bytes_read = fchannel.read(buffer);
                if (bytes_read == -1) {
                    break;
                }
            }
            byte[] dir_bytes = buffer.array();
            ByteArrayInputBuffer byte_buffer = new ByteArrayInputBuffer(dir_bytes);
            dir = (Directory) byte_buffer.nextObject();
        } catch (IOException e) {
            logger.warn("Failed to read directory from cache: path=" + path);
            dir = (Directory) null;
        } catch (QSException e) {
            BUG("Failed to reconstruct dir. " + e);
        }
        return dir;
    }

    private int writeDir(String path, Directory dir, VersionId version) {
        byte[] dir_bytes = MoxieUtils.serialize(dir);
        return writeDir(path, dir_bytes, version);
    }

    private int writeDir(String path, byte[] dir_bytes, VersionId version) {
        ObjectState state = state_map.get(path);
        assert (state != null);
        String cache_path = state.getCachePath();
        int status = MoxieStatus.MOXIE_STATUS_OK;
        try {
            writeCachedVersionId(cache_path, version);
            File cache_file = new File(cache_path);
            File parent_dir = cache_file.getParentFile();
            boolean dirs_created = parent_dir.mkdirs();
            boolean file_created = cache_file.createNewFile();
            FileOutputStream f_stream = new FileOutputStream(cache_file);
            f_stream.write(dir_bytes);
            f_stream.close();
        } catch (Exception e) {
            logger.fatal("Failed to insert directory in cache: " + "cache_file=" + cache_path + " " + e);
            status = MoxieStatus.MOXIE_STATUS_IO;
        }
        return status;
    }

    public void create(String path, Metadata md, VersionId ver, Metadata parent_md, VersionId parent_ver, Thunk1<Integer> app_cb) {
        if (logger.isInfoEnabled()) logger.info("Creating file: path=" + path);
        int status = createNewCacheObject(path, md, ver);
        if (status != MoxieStatus.MOXIE_STATUS_OK) {
            logger.warn("Failed to create new cache object: path=" + path);
            app_cb.run(status);
            return;
        }
        ObjectState state = state_map.get(path);
        String cache_path = state.getCachePath();
        Runnable cb = curry(create_cb, path, md, ver, parent_md, parent_ver, app_cb);
        updater.objectInserted(path, cache_path, ver, cb);
        return;
    }

    private Thunk6<String, Metadata, VersionId, Metadata, VersionId, Thunk1<Integer>> create_cb = new Thunk6<String, Metadata, VersionId, Metadata, VersionId, Thunk1<Integer>>() {

        public void run(String path, Metadata md, VersionId ver, Metadata parent_md, VersionId parent_ver, Thunk1<Integer> app_cb) {
            try {
                File f = new File(path);
                String parent_path = f.getParent();
                String child_name = f.getName();
                ObjectState parent_state = state_map.get(parent_path);
                if (parent_state != null) {
                    CacheStatus cstatus = getCacheStatus(parent_path);
                    if (cstatus == CacheStatus.CACHED) {
                        Directory parent_dir = readDir(parent_path);
                        parent_dir.addChild(child_name, md.obj_id);
                        writeDir(parent_path, parent_dir, parent_ver);
                    }
                }
            } catch (Exception e) {
                BUG("TODO:");
            }
            int status = MoxieStatus.MOXIE_STATUS_OK;
            app_cb.run(status);
            return;
        }
    };

    public void open(String path, int flags, Thunk2<Integer, Long> app_cb) {
        if (logger.isInfoEnabled()) logger.info("Opening file: name=" + path + " flags=" + flags);
        ObjectState state = state_map.get(path);
        assert (state != null);
        synchronized (state) {
            state.open_cnt++;
            state.open_write |= (((flags & FuseFS.O_WRONLY) != 0) || ((flags & FuseFS.O_RDWR) != 0));
        }
        long fhandle = fhandle_cntr++;
        app_cb.run(MoxieStatus.MOXIE_STATUS_OK, fhandle);
        return;
    }

    public void close(String path, long fhandle, Thunk1<Integer> app_cb) {
        if (logger.isInfoEnabled()) logger.info("Closing file: name=" + path + " fhandle=" + fhandle);
        ObjectState state = state_map.get(path);
        assert (state != null);
        synchronized (state) {
            state.open_cnt--;
            if (state.open_write) state.update_in_progress = true;
        }
        app_cb.run(MoxieStatus.MOXIE_STATUS_OK);
        return;
    }

    public void release(String path, long fhandle, Thunk2<Integer, Update> app_cb) {
        if (logger.isInfoEnabled()) logger.info("Releasing file: name=" + path + " fhandle=" + fhandle);
        ObjectState state = state_map.get(path);
        assert (state != null);
        synchronized (state) {
            state.busy = true;
        }
        FileChannel fchannel = null;
        try {
            fchannel = state.getChannel();
        } catch (IOException e) {
            int status = MoxieStatus.MOXIE_STATUS_IO;
            app_cb.run(status, (Update) null);
            return;
        }
        if (state.update_in_progress) {
            Thunk1<Update> update_cb = curry(release_update_cb, path, fhandle, app_cb);
            updater.computeUpdate(path, state.getCachePath(), state.ver, fchannel, update_cb);
        } else {
            release_update_cb.run(path, fhandle, app_cb, (Update) null);
        }
        return;
    }

    private Thunk4<String, Long, Thunk2<Integer, Update>, Update> release_update_cb = new Thunk4<String, Long, Thunk2<Integer, Update>, Update>() {

        public void run(String path, Long fhandle, Thunk2<Integer, Update> app_cb, Update update) {
            if (logger.isDebugEnabled()) logger.debug("Computed file update: path=" + path + " update=" + XdrUtils.toString(update));
            ObjectState state = state_map.get(path);
            assert (state != null);
            int status = MoxieStatus.MOXIE_STATUS_OK;
            synchronized (state) {
                if (update == null) state.update_in_progress = false;
                if (state.open_cnt == 0) {
                    try {
                        state.close();
                    } catch (IOException e) {
                        status = MoxieStatus.MOXIE_STATUS_IO;
                    }
                }
                state.busy = false;
            }
            if (update == null) signalWaiters(path);
            app_cb.run(status, update);
            return;
        }
    };

    public void write(String path, long fhandle, ByteBuffer buffer, long offset, Thunk1<Integer> app_cb) {
        int length = buffer.capacity();
        if (logger.isInfoEnabled()) logger.info("Writing to cached file: file=" + path + " offset=" + offset + " length=" + length);
        ObjectState state = state_map.get(path);
        assert (state != null);
        int status = MoxieStatus.MOXIE_STATUS_OK;
        try {
            int bytes_written = state.getChannel().write(buffer, offset);
            assert (bytes_written == length);
            if (offset + buffer.limit() > state.md.size) state.md.size = (int) (offset + buffer.limit());
        } catch (Exception e) {
            status = MoxieStatus.MOXIE_STATUS_IO;
            logger.fatal("except: e=" + e);
        }
        app_cb.run(status);
        return;
    }

    public void read(String path, long fhandle, ByteBuffer buffer, long offset, Thunk1<Integer> app_cb) {
        int length = buffer.capacity();
        if (logger.isInfoEnabled()) logger.info("Reading cached file: file=" + path + " offset=" + offset + " length=" + length);
        ObjectState state = state_map.get(path);
        assert (state != null);
        int status = MoxieStatus.MOXIE_STATUS_OK;
        try {
            state.getChannel().read(buffer, offset);
        } catch (Exception e) {
            status = MoxieStatus.MOXIE_STATUS_IO;
        }
        app_cb.run(status);
        return;
    }

    public void fsync(String path, long fhandle, Thunk2<Integer, Update> app_cb) {
        if (logger.isInfoEnabled()) logger.info("fsync - start: path=" + path + " fhandle=" + fhandle);
        ObjectState state = state_map.get(path);
        assert (state != null);
        synchronized (state) {
            state.busy = true;
        }
        FileChannel fchannel = null;
        try {
            fchannel = state.getChannel();
        } catch (IOException e) {
            int status = MoxieStatus.MOXIE_STATUS_IO;
            app_cb.run(status, (Update) null);
            return;
        }
        Thunk1<Update> update_cb = curry(fsync_update_cb, path, fhandle, app_cb);
        updater.computeUpdate(path, state.getCachePath(), state.ver, fchannel, update_cb);
        return;
    }

    private Thunk4<String, Long, Thunk2<Integer, Update>, Update> fsync_update_cb = new Thunk4<String, Long, Thunk2<Integer, Update>, Update>() {

        public void run(String path, Long fhandle, Thunk2<Integer, Update> app_cb, Update update) {
            if (logger.isInfoEnabled()) logger.info("fsync - computed update: update=" + XdrUtils.toString(update));
            ObjectState state = state_map.get(path);
            assert (state != null);
            synchronized (state) {
                if (update != null) state.update_in_progress = true;
                state.busy = false;
            }
            int status = MoxieStatus.MOXIE_STATUS_OK;
            app_cb.run(status, update);
            return;
        }
    };

    public void truncate(String path, long truncate_length, Thunk2<Integer, Update> app_cb) {
        if (logger.isInfoEnabled()) logger.info("truncate - start: file=" + path + " length=" + truncate_length);
        ObjectState state = state_map.get(path);
        if (state == null) {
            logger.warn("truncate - done, file not cached: path=" + path);
            int status = MoxieStatus.MOXIE_STATUS_IO;
            app_cb.run(status, (Update) null);
            return;
        }
        synchronized (state) {
            state.busy = true;
        }
        boolean requires_server_update = false;
        try {
            long file_length = state.getChannel().size();
            if (file_length > truncate_length) {
                state.getChannel().truncate(truncate_length);
                requires_server_update = true;
                state.md.size = (int) truncate_length;
            } else if (file_length < truncate_length) {
                BUG("TODO");
                requires_server_update = true;
                state.md.size = (int) truncate_length;
            } else {
                assert (file_length == truncate_length);
            }
        } catch (Exception e) {
            logger.fatal("Exception while truncating file: e=" + e);
            synchronized (state) {
                state.busy = false;
            }
            int status = MoxieStatus.MOXIE_STATUS_IO;
            app_cb.run(status, (Update) null);
            return;
        }
        if (state.open_write) {
            logger.warn("truncate - done, cannot write back changes while" + " other processes have file open: path=" + path);
            truncate_update_cb.run(path, truncate_length, app_cb, (Update) null);
        } else if (!requires_server_update) {
            truncate_update_cb.run(path, truncate_length, app_cb, (Update) null);
        } else {
            assert (requires_server_update);
            FileChannel fchannel = null;
            try {
                fchannel = state.getChannel();
            } catch (IOException e) {
                synchronized (state) {
                    state.busy = false;
                }
                int status = MoxieStatus.MOXIE_STATUS_IO;
                app_cb.run(status, (Update) null);
                return;
            }
            Thunk1<Update> update_cb = curry(truncate_update_cb, path, truncate_length, app_cb);
            updater.computeUpdate(path, state.getCachePath(), state.ver, fchannel, update_cb);
        }
        return;
    }

    private Thunk4<String, Long, Thunk2<Integer, Update>, Update> truncate_update_cb = new Thunk4<String, Long, Thunk2<Integer, Update>, Update>() {

        public void run(String path, Long truncate_length, Thunk2<Integer, Update> app_cb, Update update) {
            if (logger.isDebugEnabled()) logger.debug("truncate - computed update: " + XdrUtils.toString(update));
            ObjectState state = state_map.get(path);
            assert (state != null);
            synchronized (state) {
                if (update != null) state.update_in_progress = true;
                state.busy = false;
            }
            int status = MoxieStatus.MOXIE_STATUS_OK;
            app_cb.run(status, update);
            return;
        }
    };

    public void remove(String path, Metadata parent_md, VersionId parent_ver, Thunk1<Integer> app_cb) {
        if (logger.isInfoEnabled()) logger.info("Removing object: path=" + path);
        ObjectState state = state_map.remove(path);
        assert (state != null);
        String cache_path = state.getCachePath();
        File f = new File(cache_path);
        if (f.exists()) {
            boolean deleted = f.delete();
            if (!deleted) {
                app_cb.run(MoxieStatus.MOXIE_STATUS_IO);
                return;
            }
        }
        try {
            String parent_path = f.getParent();
            String child_name = f.getName();
            ObjectState parent_state = state_map.get(parent_path);
            if (parent_state != null) {
                CacheStatus cstatus = getCacheStatus(parent_path);
                if (cstatus == CacheStatus.CACHED) {
                    Directory parent_dir = readDir(parent_path);
                    parent_dir.removeChild(child_name);
                    writeDir(parent_path, parent_dir, parent_ver);
                }
            }
        } catch (Exception e) {
            BUG("TODO:");
        }
        app_cb.run(MoxieStatus.MOXIE_STATUS_OK);
        return;
    }

    public void rename(String from_path, String to_path, long to_obj_id, Thunk1<Integer> app_cb) {
        if (logger.isInfoEnabled()) logger.info("Renaming object: from_path=" + from_path + " to_path=" + to_path + " to_obj_id=" + to_obj_id);
        ObjectState state = state_map.remove(from_path);
        assert (state != null);
        String from_cache_path = state.getCachePath();
        state_map.put(to_path, state);
        if (state.md.obj_id != to_obj_id) {
            state.md.obj_id = to_obj_id;
            String to_cache_path = state.getCachePath();
            assert (!from_cache_path.equals(to_cache_path));
            try {
                File from_data = new File(from_cache_path);
                File to_data = new File(to_cache_path);
                from_data.renameTo(to_data);
                VersionId version = getCachedVersionId(from_cache_path);
                writeCachedVersionId(to_cache_path, version);
            } catch (Exception e) {
                BUG("Failed to rename file in cache: " + e);
            }
        }
        app_cb.run(MoxieStatus.MOXIE_STATUS_OK);
        return;
    }

    public void getdir(String path, Thunk2<Integer, Directory> app_cb) {
        if (logger.isInfoEnabled()) logger.info("Reading cached directory: dir=" + path);
        Directory dir = readDir(path);
        int status = MoxieStatus.MOXIE_STATUS_OK;
        if (dir == null) status = MoxieStatus.MOXIE_STATUS_IO;
        app_cb.run(status, dir);
        return;
    }

    public void mkdir(String path, Metadata md, VersionId ver, Metadata parent_md, VersionId parent_ver, Thunk1<Integer> app_cb) {
        if (logger.isInfoEnabled()) logger.info("making directory: path=" + path);
        File f = new File(path);
        String child_name = f.getName();
        Integer status = createNewCacheObject(path, md, ver);
        if (status != MoxieStatus.MOXIE_STATUS_OK) BUG("TODO: unhandle error: status=" + status);
        Directory dir = new Directory(child_name);
        writeDir(path, dir, ver);
        try {
            String parent_path = f.getParent();
            ObjectState parent_state = state_map.get(parent_path);
            if (parent_state != null) {
                CacheStatus cstatus = getCacheStatus(parent_path);
                if (cstatus == CacheStatus.CACHED) {
                    Directory parent_dir = readDir(parent_path);
                    parent_dir.addChild(child_name, md.obj_id);
                    writeDir(parent_path, parent_dir, parent_ver);
                }
            }
        } catch (Exception e) {
            BUG("TODO:");
        }
        app_cb.run(MoxieStatus.MOXIE_STATUS_OK);
        return;
    }

    public void link(String path, Metadata md, VersionId ver, Metadata parent_md, VersionId parent_ver, Thunk1<Integer> app_cb) {
        if (logger.isInfoEnabled()) logger.info("creating link: path=" + path);
        File f = new File(path);
        String child_name = f.getName();
        Runnable cb = null;
        recordAttributes(path, md, ver, cb);
        try {
            String parent_path = f.getParent();
            ObjectState parent_state = state_map.get(parent_path);
            if (parent_state != null) {
                CacheStatus cstatus = getCacheStatus(parent_path);
                if (cstatus == CacheStatus.CACHED) {
                    Directory parent_dir = readDir(parent_path);
                    parent_dir.addChild(child_name, md.obj_id);
                    writeDir(parent_path, parent_dir, parent_ver);
                }
            }
        } catch (Exception e) {
            BUG("TODO:");
        }
        app_cb.run(MoxieStatus.MOXIE_STATUS_OK);
        return;
    }

    public void insertStart(String path, VersionId version, Thunk1<Integer> app_cb) {
        ObjectState state = state_map.get(path);
        assert (state != null);
        String cache_path = state.getCachePath();
        if (logger.isDebugEnabled()) logger.debug("Preparing to insert data in cache: " + "cache_path=" + cache_path);
        int status = MoxieStatus.MOXIE_STATUS_OK;
        try {
            writeCachedVersionId(cache_path, version);
            File cache_file = new File(cache_path);
            File parent_dir = cache_file.getParentFile();
            boolean dirs_created = parent_dir.mkdirs();
            boolean file_created = cache_file.createNewFile();
        } catch (Exception e) {
            logger.fatal("Failed to prepare cache for insertion: " + "cache_file=" + cache_path + " " + e);
            status = MoxieStatus.MOXIE_STATUS_IO;
        }
        app_cb.run(status);
        return;
    }

    public void insert(String path, byte[] data, int offset, int length) {
        if (logger.isDebugEnabled()) logger.debug("Inserting in cache: path=" + path + " data.length=" + data.length + " offset=" + offset + " length=" + length);
        ObjectState state = state_map.get(path);
        assert (state != null);
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            state.getChannel().write(buffer, offset);
        } catch (Exception e) {
            BUG("TODO: Return I/O error.  " + e);
        }
        return;
    }

    public void insertFinish(String path, Thunk1<Integer> app_cb) {
        ObjectState state = state_map.get(path);
        assert (state != null);
        String cache_path = state.getCachePath();
        VersionId ver = state.ver;
        Runnable cb = curry(insert_finish_cb, path, app_cb);
        updater.objectInserted(path, cache_path, ver, cb);
        return;
    }

    private Thunk2<String, Thunk1<Integer>> insert_finish_cb = new Thunk2<String, Thunk1<Integer>>() {

        public void run(String path, Thunk1<Integer> app_cb) {
            int status = MoxieStatus.MOXIE_STATUS_OK;
            app_cb.run(status);
            return;
        }
    };

    public void insertDir(String path, byte[] dir_bytes, VersionId version, Thunk1<Integer> app_cb) {
        int status = writeDir(path, dir_bytes, version);
        app_cb.run(status);
        return;
    }

    private void writeCachedVersionId(String cache_path, VersionId version) {
        String ver_path = cache_path + ".version";
        try {
            File f = new File(ver_path);
            if (f.exists()) f.delete();
            File parent = f.getParentFile();
            parent.mkdirs();
            boolean created = f.createNewFile();
            if (!created) BUG("Failed to write version: path=" + cache_path);
            FileOutputStream md_stream = new FileOutputStream(ver_path);
            byte[] version_bytes = version.value;
            md_stream.write(version_bytes, 0, version_bytes.length);
            md_stream.close();
        } catch (Exception e) {
            BUG("Failed to write version: path=" + cache_path + " exception=" + e);
        }
        return;
    }

    private VersionId getCachedVersionId(String cache_path) {
        VersionId version_id = MoxieUtils.MOXIE_VERSION_ID_NULL;
        try {
            String ver_path = cache_path + ".version";
            File md = new File(ver_path);
            if (!md.exists()) return version_id;
            int length = (int) md.length();
            byte[] version_bytes = new byte[length];
            FileInputStream md_stream = new FileInputStream(ver_path);
            int total_bytes_read = 0;
            do {
                int bytes_read = md_stream.read(version_bytes, total_bytes_read, length - total_bytes_read);
                if (bytes_read == -1) {
                    break;
                }
                total_bytes_read += bytes_read;
            } while (total_bytes_read < length);
            if (total_bytes_read == length) {
                version_id = new VersionId();
                version_id.value = version_bytes;
            }
            md_stream.close();
        } catch (Exception e) {
        }
        return version_id;
    }

    protected class ObjectState {

        protected Metadata md;

        protected VersionId ver;

        protected long ts;

        protected boolean busy;

        protected int open_cnt;

        protected boolean open_write;

        protected boolean update_in_progress;

        protected boolean read_in_progress;

        protected FileChannel fchannel;

        protected ObjectState(Metadata md, VersionId ver) {
            this.md = md;
            this.ver = ver;
            this.ts = System.currentTimeMillis();
            this.busy = false;
            this.open_cnt = 0;
            this.open_write = false;
            this.update_in_progress = false;
            this.read_in_progress = false;
            return;
        }

        private String getCachePath() {
            String oname = Long.toHexString(md.obj_id);
            while (oname.length() < CACHE_PATH_LENGTH) oname = "0" + oname;
            String cache_path = cache_root + "/" + oname.substring(oname.length() - 2, oname.length()) + "/" + oname.substring(oname.length() - 4, oname.length() - 2) + "/" + oname;
            return cache_path;
        }

        protected FileChannel getChannel() throws IOException {
            if ((fchannel == null) || (!fchannel.isOpen())) {
                String cache_path = getCachePath();
                RandomAccessFile file = new RandomAccessFile(cache_path, "rw");
                fchannel = file.getChannel();
            }
            return fchannel;
        }

        protected void close() throws IOException {
            assert (open_cnt == 0);
            open_write = false;
            if ((fchannel != null) && (fchannel.isOpen())) {
                fchannel.close();
            }
            return;
        }
    }
}
