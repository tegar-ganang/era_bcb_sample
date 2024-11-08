package moxie.rw;

import moxie.rw.Update;
import moxie.rw.VersionId;
import antiquity.util.XdrUtils;
import ostore.util.ByteUtils;
import ostore.util.InputStreamInputBuffer;
import ostore.util.OutputStreamOutputBuffer;
import ostore.util.SecureHash;
import ostore.util.SHA1Hash;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.LinkedList;
import seda.sandStorm.api.ConfigDataIF;
import seda.sandStorm.api.QueueElementIF;
import seda.sandStorm.api.StagesInitializedSignal;
import bmsi.util.Diff;
import org.apache.log4j.Level;
import static bamboo.util.Curry.*;

public class BlockUpdateEncoder extends UpdateEncoder {

    private static final int BLOCK_SIZE = 4096;

    private boolean USE_DELTA_UPDATES = true;

    public void init(ConfigDataIF config) throws Exception {
        super.init(config);
        if (config.contains("UseDeltaUpdates")) USE_DELTA_UPDATES = config.getBoolean("UseDeltaUpdates");
        logger.warn("UseDeltaUpdates=" + USE_DELTA_UPDATES);
        return;
    }

    public void objectInserted(String fs_path, String cache_path, VersionId version, Runnable cb) {
        FileRecipe recipe = computeRecipe(fs_path, cache_path);
        recipe.setVersion(version);
        boolean backup = true;
        writeRecipe(fs_path, cache_path, recipe, backup);
        cb.run();
        return;
    }

    public void computeUpdate(String fs_path, String cache_path, VersionId prev_ver, FileChannel fchannel, Thunk1<Update> cb) {
        FileRecipe new_recipe = computeRecipe(fs_path, cache_path);
        int new_size = new_recipe.getFileSize();
        FileRecipe old_recipe = readRecipe(fs_path, cache_path);
        if (!XdrUtils.equals(old_recipe.getVersion(), prev_ver)) {
            old_recipe.setVersion(prev_ver);
            boolean backup = false;
            writeRecipe(fs_path, cache_path, old_recipe, backup);
        }
        int old_size = old_recipe.getFileSize();
        if (old_size == 0) {
            FileRecipe old_recipe_bak = readRecipeBak(fs_path, cache_path);
            if (old_recipe_bak != null) {
                old_recipe = old_recipe_bak;
                old_size = old_recipe.getFileSize();
            }
        }
        if (!USE_DELTA_UPDATES) {
            old_recipe = new FileRecipe();
            old_size = 0;
        }
        Update update = null;
        try {
            update = computeDiffUpdate(old_recipe, new_recipe, fchannel);
        } catch (IOException e) {
            logger.warn("Error while computing update: " + e);
        }
        boolean backup = true;
        writeRecipe(fs_path, cache_path, new_recipe, backup);
        cb.run(update);
        return;
    }

    protected Update computeDiffUpdate(FileRecipe old_recipe, FileRecipe new_recipe, FileChannel fchannel) throws IOException {
        String[] old_diff_st = old_recipe.toDiffString();
        String[] new_diff_st = new_recipe.toDiffString();
        Diff diff = new Diff(old_diff_st, new_diff_st);
        Diff.change change = diff.diff(Diff.forwardScript);
        int bytes_common = 0;
        int bytes_different = 0;
        int old_chunk_index = 0;
        int new_chunk_index = 0;
        int new_file_offset = 0;
        List<Action> actions = new LinkedList<Action>();
        while (change != null) {
            if (logger.isDebugEnabled()) logger.debug("processing change:" + " inserted=" + change.inserted + " deleted=" + change.deleted + " line_num_deleted=" + change.line1 + " line_num_inserted=" + change.line0);
            while (old_chunk_index < change.line0) {
                ++old_chunk_index;
            }
            while (new_chunk_index < change.line1) {
                FileRecipe.Chunk chunk = new_recipe.getChunk(new_chunk_index);
                new_file_offset += chunk.length;
                bytes_common += chunk.length;
                ++new_chunk_index;
            }
            int remove_length = 0;
            for (int i = 0; i < change.deleted; ++i) {
                FileRecipe.Chunk chunk = old_recipe.getChunk(old_chunk_index);
                remove_length += chunk.length;
                ++old_chunk_index;
            }
            if (remove_length > 0) {
                Action delete = new Action();
                delete.type = ActionType.DELETE;
                delete.offset = new_file_offset;
                delete.length = remove_length;
                delete.data = new UserData[1];
                delete.data[0] = new UserData();
                delete.data[0].type = UserDataType.DIRECT;
                delete.data[0].block = MoxieUtils.MOXIE_USER_DATA_BLOCK_NULL;
                actions.add(delete);
            }
            for (int i = 0; i < change.inserted; ++i) {
                FileRecipe.Chunk chunk = new_recipe.getChunk(new_chunk_index);
                ByteBuffer buffer = ByteBuffer.allocate(chunk.length);
                fchannel.position(chunk.offset);
                while (buffer.hasRemaining()) {
                    int bytes_read = fchannel.read(buffer);
                    if (bytes_read == -1) {
                        BUG("Failed to read data to write back");
                        return ((Update) null);
                    }
                }
                UserDataBlock block = new UserDataBlock();
                block.iv = new byte[] { 0 };
                block.data = buffer.array();
                block.user_length = chunk.length;
                block.data_length = chunk.length;
                Action insert = new Action();
                insert.type = ActionType.INSERT;
                insert.offset = new_file_offset;
                insert.length = chunk.length;
                insert.data = new UserData[1];
                insert.data[0] = new UserData();
                insert.data[0].type = UserDataType.DIRECT;
                insert.data[0].block = block;
                actions.add(insert);
                new_file_offset += chunk.length;
                bytes_different += chunk.length;
                ++new_chunk_index;
            }
            change = change.link;
        }
        while (new_chunk_index < new_recipe.numChunks()) {
            FileRecipe.Chunk chunk = new_recipe.getChunk(new_chunk_index);
            bytes_common += chunk.length;
            new_chunk_index++;
        }
        double fraction = ((double) bytes_common) / ((double) bytes_common + bytes_different);
        logger.fatal("commonality: " + " bytes_common=" + bytes_common + " bytes_different=" + bytes_different + " fraction=" + fraction);
        Update update = null;
        if (actions.size() > 0) {
            update = new Update();
            update.prev_ver = old_recipe.getVersion();
            update.actions = actions.toArray(new Action[1]);
        }
        return update;
    }

    protected FileRecipe computeRecipe(String fs_path, String cache_path) {
        FileRecipe recipe = new FileRecipe();
        try {
            File f = new File(cache_path);
            FileInputStream f_in = new FileInputStream(f);
            FileChannel f_channel = f_in.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);
            int num_blocks = 0;
            int fpos = 0;
            boolean eof = false;
            while (!eof) {
                do {
                    int read_length = f_channel.read(buffer);
                    eof = (read_length == -1);
                } while (buffer.hasRemaining() && (!eof));
                if (buffer.remaining() < BLOCK_SIZE) {
                    int bytes_read = buffer.position();
                    SecureHash hash = new SHA1Hash(buffer.array());
                    recipe.addChunk(hash, bytes_read, fpos);
                    fpos += bytes_read;
                    num_blocks++;
                }
                buffer.clear();
            }
            if (logger.isInfoEnabled()) logger.info("computed block recipe: path=" + fs_path + " file_size=" + fpos + " num_blocks=" + num_blocks);
        } catch (Exception e) {
            BUG("Failed to compute file recipe of file in local cache: " + "path=" + fs_path + " exception=" + e);
        }
        return recipe;
    }

    protected void writeRecipe(String fs_path, String cache_path, FileRecipe recipe, boolean backup) {
        String recipe_path = getRecipePath(cache_path);
        if (backup) {
            File recipe_file = new File(recipe_path);
            if (recipe_file.exists()) {
                File recipe_file_bak = new File(recipe_path + ".bak");
                recipe_file.renameTo(recipe_file_bak);
            }
        }
        try {
            FileOutputStream f_out = new FileOutputStream(recipe_path);
            OutputStreamOutputBuffer buffer = new OutputStreamOutputBuffer(f_out);
            buffer.add(recipe);
            f_out.close();
        } catch (Exception e) {
            BUG("Failed to write file recipe to local cache: " + "path=" + fs_path + " exception=" + e);
        }
        return;
    }

    protected FileRecipe readRecipeBak(String fs_path, String cache_path) {
        String recipe_path = getRecipePath(cache_path) + ".bak";
        return readRecipe(fs_path, cache_path, recipe_path);
    }

    protected FileRecipe readRecipe(String fs_path, String cache_path) {
        String recipe_path = getRecipePath(cache_path);
        return readRecipe(fs_path, cache_path, recipe_path);
    }

    private FileRecipe readRecipe(String fs_path, String cache_path, String recipe_path) {
        FileRecipe recipe = null;
        File recipe_file = new File(recipe_path);
        if (!recipe_file.exists()) return recipe;
        try {
            FileInputStream f_in = new FileInputStream(recipe_path);
            InputStreamInputBuffer buffer = new InputStreamInputBuffer(f_in);
            recipe = (FileRecipe) buffer.nextObject();
            f_in.close();
        } catch (Exception e) {
            BUG("Failed to read file recipe from local cache: " + "path=" + fs_path + " exception=" + e);
        }
        return recipe;
    }

    protected String getRecipePath(String cache_path) {
        String path = cache_path + ".recipe";
        return path;
    }
}
