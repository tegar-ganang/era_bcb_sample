package com.aelitis.azureus.core.diskmanager.file.impl;

import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.DirectByteBufferPool;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SystemTime;
import com.aelitis.azureus.core.diskmanager.file.FMFileManagerException;

public class FMFileAccessPieceReorderer implements FMFileAccess {

    private static final boolean TRACE = false;

    private static final int MIN_PIECES_REORDERABLE = 3;

    private static final byte SS_FILE = DirectByteBuffer.SS_FILE;

    private static final int DIRT_CLEAN = 0;

    private static final int DIRT_DIRTY = 1;

    private static final int DIRT_NEVER_WRITTEN = 2;

    private static final long DIRT_FLUSH_MILLIS = 30 * 1000;

    private FMFileAccess delegate;

    private File control_dir;

    private String control_file;

    private int piece_size;

    private int first_piece_length;

    private int first_piece_number;

    private int last_piece_length;

    private int num_pieces;

    private long current_length;

    private int[] piece_map;

    private int[] piece_reverse_map;

    private int next_piece_index;

    private int dirt_state;

    private long dirt_time = -1;

    protected FMFileAccessPieceReorderer(TOTorrentFile _torrent_file, File _control_dir, String _control_file, FMFileAccess _delegate) throws FMFileManagerException {
        delegate = _delegate;
        control_dir = _control_dir;
        control_file = _control_file;
        try {
            first_piece_number = _torrent_file.getFirstPieceNumber();
            num_pieces = _torrent_file.getLastPieceNumber() - first_piece_number + 1;
            if (num_pieces >= MIN_PIECES_REORDERABLE) {
                piece_size = (int) _torrent_file.getTorrent().getPieceLength();
                TOTorrent torrent = _torrent_file.getTorrent();
                long file_length = _torrent_file.getLength();
                long file_offset_in_torrent = 0;
                TOTorrentFile[] files = torrent.getFiles();
                for (int i = 0; i < files.length; i++) {
                    TOTorrentFile f = files[i];
                    if (f == _torrent_file) {
                        break;
                    }
                    file_offset_in_torrent += f.getLength();
                }
                int first_piece_offset = (int) (file_offset_in_torrent % piece_size);
                first_piece_length = piece_size - first_piece_offset;
                long file_end = file_offset_in_torrent + file_length;
                last_piece_length = (int) (file_end - ((file_end / piece_size) * piece_size));
                if (last_piece_length == 0) {
                    last_piece_length = piece_size;
                }
            }
            dirt_state = new File(control_dir, control_file).exists() ? DIRT_CLEAN : DIRT_NEVER_WRITTEN;
        } catch (Throwable e) {
            throw (new FMFileManagerException("Piece-reorder file init fail", e));
        }
    }

    public void aboutToOpen() throws FMFileManagerException {
        if (dirt_state == DIRT_NEVER_WRITTEN) {
            writeConfig();
        }
    }

    public long getLength(RandomAccessFile raf) throws FMFileManagerException {
        if (num_pieces >= MIN_PIECES_REORDERABLE) {
            if (piece_map == null) {
                readConfig();
            }
            return (current_length);
        } else {
            return (delegate.getLength(raf));
        }
    }

    public void setLength(RandomAccessFile raf, long length) throws FMFileManagerException {
        if (num_pieces >= MIN_PIECES_REORDERABLE) {
            if (piece_map == null) {
                readConfig();
            }
            if (current_length != length) {
                current_length = length;
                setDirty();
            }
        } else {
            delegate.setLength(raf, length);
        }
    }

    protected long getPieceOffset(RandomAccessFile raf, int piece_number, boolean allocate_if_needed) throws FMFileManagerException {
        if (piece_map == null) {
            readConfig();
        }
        int index = getPieceIndex(raf, piece_number, allocate_if_needed);
        if (index < 0) {
            return (index);
        } else if (index == 0) {
            return (0);
        } else if (index == 1) {
            return (first_piece_length);
        } else {
            return (first_piece_length + ((index - 1) * (long) piece_size));
        }
    }

    protected int readWritePiece(RandomAccessFile raf, DirectByteBuffer[] buffers, int piece_number, int piece_offset, boolean is_read) throws FMFileManagerException {
        String str = is_read ? "read" : "write";
        if (piece_number >= num_pieces) {
            throw (new FMFileManagerException("Attempt to " + str + " piece " + piece_number + ": last=" + num_pieces));
        }
        int this_piece_size = piece_number == 0 ? first_piece_length : (piece_number == (num_pieces - 1) ? last_piece_length : piece_size);
        final int piece_space = this_piece_size - piece_offset;
        if (piece_space <= 0) {
            throw (new FMFileManagerException("Attempt to " + str + " piece " + piece_number + ", offset " + piece_offset + " - no space in piece"));
        }
        int rem_space = piece_space;
        int[] limits = new int[buffers.length];
        for (int i = 0; i < buffers.length; i++) {
            DirectByteBuffer buffer = buffers[i];
            limits[i] = buffer.limit(SS_FILE);
            int rem = buffer.remaining(SS_FILE);
            if (rem > rem_space) {
                buffer.limit(SS_FILE, buffer.position(SS_FILE) + rem_space);
                rem_space = 0;
            } else {
                rem_space -= rem;
            }
        }
        try {
            long piece_start = getPieceOffset(raf, piece_number, !is_read);
            if (TRACE) {
                System.out.println(str + " to " + piece_number + "/" + piece_offset + "/" + this_piece_size + "/" + piece_space + "/" + rem_space + "/" + piece_start);
            }
            if (piece_start == -1) {
                return (0);
            }
            long piece_io_position = piece_start + piece_offset;
            if (is_read) {
                delegate.read(raf, buffers, piece_io_position);
            } else {
                delegate.write(raf, buffers, piece_io_position);
            }
            return (piece_space - rem_space);
        } finally {
            for (int i = 0; i < buffers.length; i++) {
                buffers[i].limit(SS_FILE, limits[i]);
            }
        }
    }

    protected void readWrite(RandomAccessFile raf, DirectByteBuffer[] buffers, long position, boolean is_read) throws FMFileManagerException {
        long total_length = 0;
        for (DirectByteBuffer buffer : buffers) {
            total_length += buffer.remaining(SS_FILE);
        }
        if (!is_read && position + total_length > current_length) {
            current_length = position + total_length;
            setDirty();
        }
        long current_position = position;
        while (total_length > 0) {
            int piece_number;
            int piece_offset;
            if (current_position < first_piece_length) {
                piece_number = 0;
                piece_offset = (int) current_position;
            } else {
                long offset = current_position - first_piece_length;
                piece_number = (int) (offset / piece_size) + 1;
                piece_offset = (int) (offset % piece_size);
            }
            int count = readWritePiece(raf, buffers, piece_number, piece_offset, is_read);
            if (count == 0) {
                if (is_read) {
                    for (DirectByteBuffer buffer : buffers) {
                        ByteBuffer bb = buffer.getBuffer(SS_FILE);
                        int rem = bb.remaining();
                        bb.put(new byte[rem]);
                    }
                } else {
                    throw (new FMFileManagerException("partial write operation"));
                }
                return;
            }
            total_length -= count;
            current_position += count;
        }
    }

    public void read(RandomAccessFile raf, DirectByteBuffer[] buffers, long position) throws FMFileManagerException {
        if (num_pieces >= MIN_PIECES_REORDERABLE) {
            readWrite(raf, buffers, position, true);
        } else {
            delegate.read(raf, buffers, position);
        }
    }

    public void write(RandomAccessFile raf, DirectByteBuffer[] buffers, long position) throws FMFileManagerException {
        if (num_pieces >= MIN_PIECES_REORDERABLE) {
            readWrite(raf, buffers, position, false);
        } else {
            delegate.write(raf, buffers, position);
        }
    }

    public void flush() throws FMFileManagerException {
        if (num_pieces >= MIN_PIECES_REORDERABLE) {
            if (dirt_state != DIRT_CLEAN) {
                writeConfig();
            }
        } else {
            delegate.flush();
        }
    }

    public void setPieceComplete(RandomAccessFile raf, int piece_number, DirectByteBuffer piece_data) throws FMFileManagerException {
        if (num_pieces >= MIN_PIECES_REORDERABLE) {
            piece_number = piece_number - first_piece_number;
            if (TRACE) {
                System.out.println("pieceComplete: " + piece_number);
            }
            if (piece_number >= next_piece_index) {
                return;
            }
            int store_index = getPieceIndex(raf, piece_number, false);
            if (store_index == -1) {
                throw (new FMFileManagerException("piece marked as complete but not yet allocated"));
            }
            if (piece_number == store_index) {
                if (TRACE) {
                    System.out.println("    already in right place");
                }
                return;
            }
            int swap_piece_number = piece_reverse_map[piece_number];
            if (swap_piece_number < 1) {
                throw (new FMFileManagerException("Inconsistent: failed to find piece to swap"));
            }
            if (TRACE) {
                System.out.println("    swapping " + piece_number + " and " + swap_piece_number + ": " + piece_number + " <-> " + store_index);
            }
            DirectByteBuffer temp_buffer = DirectByteBufferPool.getBuffer(SS_FILE, piece_size);
            DirectByteBuffer[] temp_buffers = new DirectByteBuffer[] { temp_buffer };
            try {
                long store_offset = first_piece_length + ((store_index - 1) * (long) piece_size);
                long swap_offset = first_piece_length + ((piece_number - 1) * (long) piece_size);
                delegate.read(raf, temp_buffers, swap_offset);
                piece_data.position(SS_FILE, 0);
                delegate.write(raf, new DirectByteBuffer[] { piece_data }, swap_offset);
                temp_buffer.position(SS_FILE, 0);
                delegate.write(raf, temp_buffers, store_offset);
                piece_map[piece_number] = piece_number;
                piece_reverse_map[piece_number] = piece_number;
                piece_map[swap_piece_number] = store_index;
                piece_reverse_map[store_index] = swap_piece_number;
                setDirty();
                if (piece_number == num_pieces - 1) {
                    long file_length = swap_offset + last_piece_length;
                    if (delegate.getLength(raf) > file_length) {
                        if (TRACE) {
                            System.out.println("    truncating file to correct length of " + file_length);
                        }
                        delegate.setLength(raf, file_length);
                    }
                }
            } finally {
                temp_buffer.returnToPool();
            }
        } else {
            delegate.setPieceComplete(raf, piece_number, piece_data);
        }
    }

    protected int getPieceIndex(RandomAccessFile raf, int piece_number, boolean allocate_if_needed) throws FMFileManagerException {
        int store_index = piece_map[piece_number];
        if (store_index == -1 && allocate_if_needed) {
            store_index = next_piece_index++;
            if (TRACE) {
                System.out.println("getPiece(" + piece_number + "): allocated " + store_index);
            }
            piece_map[piece_number] = store_index;
            piece_reverse_map[store_index] = piece_number;
            if (piece_number != store_index) {
                int swap_index = piece_map[store_index];
                if (swap_index > 0) {
                    if (TRACE) {
                        System.out.println("    piece number " + store_index + " already allocated at " + swap_index + ": moving piece ");
                    }
                    DirectByteBuffer temp_buffer = DirectByteBufferPool.getBuffer(SS_FILE, piece_size);
                    DirectByteBuffer[] temp_buffers = new DirectByteBuffer[] { temp_buffer };
                    try {
                        long store_offset = first_piece_length + ((store_index - 1) * (long) piece_size);
                        long swap_offset = first_piece_length + ((swap_index - 1) * (long) piece_size);
                        delegate.read(raf, temp_buffers, swap_offset);
                        temp_buffer.position(SS_FILE, 0);
                        delegate.write(raf, temp_buffers, store_offset);
                        piece_map[store_index] = store_index;
                        piece_reverse_map[store_index] = store_index;
                        piece_map[piece_number] = swap_index;
                        piece_reverse_map[swap_index] = piece_number;
                        if (store_index == num_pieces - 1) {
                            long file_length = store_offset + last_piece_length;
                            if (delegate.getLength(raf) > file_length) {
                                if (TRACE) {
                                    System.out.println("    truncating file to correct length of " + file_length);
                                }
                                delegate.setLength(raf, file_length);
                            }
                        }
                        store_index = swap_index;
                    } finally {
                        temp_buffer.returnToPool();
                    }
                }
            }
            setDirty();
        }
        return (store_index);
    }

    private void readConfig() throws FMFileManagerException {
        piece_map = new int[num_pieces];
        piece_reverse_map = new int[num_pieces];
        if (dirt_state == DIRT_NEVER_WRITTEN) {
            Arrays.fill(piece_map, -1);
            piece_map[0] = 0;
            piece_reverse_map[0] = 0;
            next_piece_index = 1;
            current_length = 0;
        } else {
            Map map = FileUtil.readResilientFile(control_dir, control_file, false);
            Long l_len = (Long) map.get("len");
            Long l_next = (Long) map.get("next");
            byte[] piece_bytes = (byte[]) map.get("pieces");
            if (l_len == null || l_next == null || piece_bytes == null) {
                configBorked("Failed to read control file " + new File(control_dir, control_file).getAbsolutePath() + ": map invalid - " + map);
                return;
            }
            current_length = l_len.longValue();
            next_piece_index = l_next.intValue();
            if (piece_bytes.length != num_pieces * 4) {
                configBorked("Failed to read control file " + new File(control_dir, control_file).getAbsolutePath() + ": piece bytes invalid");
                return;
            }
            int pos = 0;
            for (int i = 0; i < num_pieces; i++) {
                int index = (piece_bytes[pos++] << 24) + ((piece_bytes[pos++] & 0xff) << 16) + ((piece_bytes[pos++] & 0xff) << 8) + ((piece_bytes[pos++] & 0xff));
                piece_map[i] = index;
                if (index != -1) {
                    piece_reverse_map[index] = i;
                }
            }
        }
        if (TRACE) {
            System.out.println("ReadConfig: length=" + current_length + ", next=" + next_piece_index);
        }
    }

    private void configBorked(String error) throws FMFileManagerException {
        piece_map = new int[num_pieces];
        piece_reverse_map = new int[num_pieces];
        Arrays.fill(piece_map, -1);
        piece_map[0] = 0;
        piece_reverse_map[0] = 0;
        current_length = getFile().getLinkedFile().length();
        int piece_count = (int) ((current_length + piece_size - 1) / piece_size) + 1;
        if (piece_count > num_pieces) {
            piece_count = num_pieces;
        }
        for (int i = 1; i < piece_count; i++) {
            piece_map[i] = i;
            piece_reverse_map[i] = i;
        }
        next_piece_index = piece_count;
        writeConfig();
        FMFileManagerException e = new FMFileManagerException(error);
        e.setRecoverable(false);
        throw (e);
    }

    protected void setDirty() throws FMFileManagerException {
        if (dirt_state == DIRT_NEVER_WRITTEN) {
            Debug.out("shouldn't get here");
            writeConfig();
        } else {
            long now = SystemTime.getMonotonousTime();
            if (dirt_state == DIRT_CLEAN) {
                dirt_state = DIRT_DIRTY;
                dirt_time = now;
            } else {
                if (dirt_time >= 0 && (now - dirt_time >= DIRT_FLUSH_MILLIS)) {
                    writeConfig();
                }
            }
        }
    }

    private static Map encodeConfig(long current_length, long next_piece_index, int[] piece_map) {
        Map map = new HashMap();
        map.put("len", new Long(current_length));
        map.put("next", new Long(next_piece_index));
        byte[] pieces_bytes = new byte[piece_map.length * 4];
        int pos = 0;
        for (int i = 0; i < piece_map.length; i++) {
            int value = piece_map[i];
            if (value == -1) {
                pieces_bytes[pos++] = pieces_bytes[pos++] = pieces_bytes[pos++] = pieces_bytes[pos++] = (byte) 0xff;
            } else {
                pieces_bytes[pos++] = (byte) (value >> 24);
                pieces_bytes[pos++] = (byte) (value >> 16);
                pieces_bytes[pos++] = (byte) (value >> 8);
                pieces_bytes[pos++] = (byte) (value);
            }
        }
        map.put("pieces", pieces_bytes);
        return (map);
    }

    protected static void recoverConfig(TOTorrentFile torrent_file, File data_file, File config_file) throws FMFileManagerException {
        int first_piece_number = torrent_file.getFirstPieceNumber();
        int num_pieces = torrent_file.getLastPieceNumber() - first_piece_number + 1;
        int piece_size = (int) torrent_file.getTorrent().getPieceLength();
        int[] piece_map = new int[num_pieces];
        Arrays.fill(piece_map, -1);
        piece_map[0] = 0;
        long current_length = data_file.length();
        int piece_count = (int) ((current_length + piece_size - 1) / piece_size) + 1;
        if (piece_count > num_pieces) {
            piece_count = num_pieces;
        }
        for (int i = 1; i < piece_count; i++) {
            piece_map[i] = i;
        }
        int next_piece_index = piece_count;
        Map map = encodeConfig(current_length, next_piece_index, piece_map);
        File control_dir = config_file.getParentFile();
        if (!control_dir.exists()) {
            control_dir.mkdirs();
        }
        if (!FileUtil.writeResilientFileWithResult(control_dir, config_file.getName(), map)) {
            throw (new FMFileManagerException("Failed to write control file " + config_file.getAbsolutePath()));
        }
    }

    private void writeConfig() throws FMFileManagerException {
        if (piece_map == null) {
            readConfig();
        }
        Map map = encodeConfig(current_length, next_piece_index, piece_map);
        if (!control_dir.exists()) {
            control_dir.mkdirs();
        }
        if (!FileUtil.writeResilientFileWithResult(control_dir, control_file, map)) {
            throw (new FMFileManagerException("Failed to write control file " + new File(control_dir, control_file).getAbsolutePath()));
        }
        if (TRACE) {
            System.out.println("WriteConfig: length=" + current_length + ", next=" + next_piece_index);
        }
        dirt_state = DIRT_CLEAN;
        dirt_time = -1;
    }

    public FMFileImpl getFile() {
        return (delegate.getFile());
    }

    public String getString() {
        return ("reorderer");
    }
}
