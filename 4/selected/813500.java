package org.gudy.azureus2.core3.disk.impl.access.impl;

import java.util.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.disk.impl.DiskManagerFileInfoImpl;
import org.gudy.azureus2.core3.disk.impl.DiskManagerHelper;
import org.gudy.azureus2.core3.disk.impl.access.*;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceList;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceMapEntry;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;
import com.aelitis.azureus.core.diskmanager.access.DiskAccessController;
import com.aelitis.azureus.core.diskmanager.access.DiskAccessRequest;
import com.aelitis.azureus.core.diskmanager.access.DiskAccessRequestListener;
import com.aelitis.azureus.core.diskmanager.cache.*;

/**
 * @author parg
 *
 */
public class DMWriterImpl implements DMWriter {

    private static final LogIDs LOGID = LogIDs.DISK;

    private static final int MIN_ZERO_BLOCK = 1 * 1024 * 1024;

    private DiskManagerHelper disk_manager;

    private DiskAccessController disk_access;

    private int async_writes;

    private Set write_requests = new HashSet();

    private AESemaphore async_write_sem = new AESemaphore("DMWriter::asyncWrite");

    private boolean started;

    private volatile boolean stopped;

    private int pieceLength;

    private long totalLength;

    private boolean complete_recheck_in_progress;

    private AEMonitor this_mon = new AEMonitor("DMWriter");

    public DMWriterImpl(DiskManagerHelper _disk_manager) {
        disk_manager = _disk_manager;
        disk_access = disk_manager.getDiskAccessController();
        pieceLength = disk_manager.getPieceLength();
        totalLength = disk_manager.getTotalLength();
    }

    public void start() {
        try {
            this_mon.enter();
            if (started) {
                throw (new RuntimeException("DMWWriter: start while started"));
            }
            if (stopped) {
                throw (new RuntimeException("DMWWriter: start after stopped"));
            }
            started = true;
        } finally {
            this_mon.exit();
        }
    }

    public void stop() {
        int write_wait;
        try {
            this_mon.enter();
            if (stopped || !started) {
                return;
            }
            stopped = true;
            write_wait = async_writes;
        } finally {
            this_mon.exit();
        }
        long log_time = SystemTime.getCurrentTime();
        for (int i = 0; i < write_wait; i++) {
            long now = SystemTime.getCurrentTime();
            if (now < log_time) {
                log_time = now;
            } else {
                if (now - log_time > 1000) {
                    log_time = now;
                    if (Logger.isEnabled()) {
                        Logger.log(new LogEvent(disk_manager, LOGID, "Waiting for writes to complete - " + (write_wait - i) + " remaining"));
                    }
                }
            }
            async_write_sem.reserve();
        }
    }

    public boolean isChecking() {
        return (complete_recheck_in_progress);
    }

    public boolean zeroFile(DiskManagerFileInfoImpl file, long length) throws DiskManagerException {
        CacheFile cache_file = file.getCacheFile();
        try {
            if (length == 0) {
                cache_file.setLength(0);
            } else {
                int buffer_size = pieceLength < MIN_ZERO_BLOCK ? MIN_ZERO_BLOCK : pieceLength;
                buffer_size = ((buffer_size + 1023) / 1024) * 1024;
                DirectByteBuffer buffer = DirectByteBufferPool.getBuffer(DirectByteBuffer.AL_DM_ZERO, buffer_size);
                long remainder = length;
                long written = 0;
                try {
                    final byte[] blanks = new byte[1024];
                    for (int i = 0; i < buffer_size / 1024; i++) {
                        buffer.put(DirectByteBuffer.SS_DW, blanks);
                    }
                    buffer.position(DirectByteBuffer.SS_DW, 0);
                    while (remainder > 0 && !stopped) {
                        int write_size = buffer_size;
                        if (remainder < write_size) {
                            write_size = (int) remainder;
                            buffer.limit(DirectByteBuffer.SS_DW, write_size);
                        }
                        final AESemaphore sem = new AESemaphore("DMW&C:zeroFile");
                        final Throwable[] op_failed = { null };
                        disk_access.queueWriteRequest(cache_file, written, buffer, false, new DiskAccessRequestListener() {

                            public void requestComplete(DiskAccessRequest request) {
                                sem.release();
                            }

                            public void requestCancelled(DiskAccessRequest request) {
                                op_failed[0] = new Throwable("Request cancelled");
                                sem.release();
                            }

                            public void requestFailed(DiskAccessRequest request, Throwable cause) {
                                op_failed[0] = cause;
                                sem.release();
                            }

                            public int getPriority() {
                                return (-1);
                            }

                            public void requestExecuted(long bytes) {
                            }
                        });
                        sem.reserve();
                        if (op_failed[0] != null) {
                            throw (op_failed[0]);
                        }
                        buffer.position(DirectByteBuffer.SS_DW, 0);
                        written += write_size;
                        remainder -= write_size;
                        disk_manager.setAllocated(disk_manager.getAllocated() + write_size);
                        disk_manager.setPercentDone((int) ((disk_manager.getAllocated() * 1000) / totalLength));
                    }
                } finally {
                    buffer.returnToPool();
                }
                cache_file.flushCache();
            }
            if (stopped) {
                return false;
            }
        } catch (Throwable e) {
            Debug.printStackTrace(e);
            throw new DiskManagerException(e);
        }
        return true;
    }

    public DiskManagerWriteRequest createWriteRequest(int pieceNumber, int offset, DirectByteBuffer buffer, Object user_data) {
        return (new DiskManagerWriteRequestImpl(pieceNumber, offset, buffer, user_data));
    }

    public boolean hasOutstandingWriteRequestForPiece(int piece_number) {
        try {
            this_mon.enter();
            Iterator it = write_requests.iterator();
            while (it.hasNext()) {
                DiskManagerWriteRequest request = (DiskManagerWriteRequest) it.next();
                if (request.getPieceNumber() == piece_number) {
                    return (true);
                }
            }
            return (false);
        } finally {
            this_mon.exit();
        }
    }

    public void writeBlock(final DiskManagerWriteRequest request, final DiskManagerWriteRequestListener _listener) {
        request.requestStarts();
        final DiskManagerWriteRequestListener listener = new DiskManagerWriteRequestListener() {

            public void writeCompleted(DiskManagerWriteRequest request) {
                request.requestEnds(true);
                _listener.writeCompleted(request);
            }

            public void writeFailed(DiskManagerWriteRequest request, Throwable cause) {
                request.requestEnds(false);
                _listener.writeFailed(request, cause);
            }
        };
        try {
            int pieceNumber = request.getPieceNumber();
            DirectByteBuffer buffer = request.getBuffer();
            int offset = request.getOffset();
            final DiskManagerPiece dmPiece = disk_manager.getPieces()[pieceNumber];
            if (dmPiece.isDone()) {
                buffer.returnToPool();
                listener.writeCompleted(request);
            } else {
                int buffer_position = buffer.position(DirectByteBuffer.SS_DW);
                int buffer_limit = buffer.limit(DirectByteBuffer.SS_DW);
                int previousFilesLength = 0;
                int currentFile = 0;
                DMPieceList pieceList = disk_manager.getPieceList(pieceNumber);
                DMPieceMapEntry current_piece = pieceList.get(currentFile);
                long fileOffset = current_piece.getOffset();
                while ((previousFilesLength + current_piece.getLength()) < offset) {
                    previousFilesLength += current_piece.getLength();
                    currentFile++;
                    fileOffset = 0;
                    current_piece = pieceList.get(currentFile);
                }
                List chunks = new ArrayList();
                while (buffer_position < buffer_limit) {
                    current_piece = pieceList.get(currentFile);
                    long file_limit = buffer_position + ((current_piece.getFile().getLength() - current_piece.getOffset()) - (offset - previousFilesLength));
                    if (file_limit > buffer_limit) {
                        file_limit = buffer_limit;
                    }
                    if (file_limit > buffer_position) {
                        long file_pos = fileOffset + (offset - previousFilesLength);
                        chunks.add(new Object[] { current_piece.getFile(), new Long(file_pos), new Integer((int) file_limit) });
                        buffer_position = (int) file_limit;
                    }
                    currentFile++;
                    fileOffset = 0;
                    previousFilesLength = offset;
                }
                DiskManagerWriteRequestListener l = new DiskManagerWriteRequestListener() {

                    public void writeCompleted(DiskManagerWriteRequest request) {
                        complete();
                        listener.writeCompleted(request);
                    }

                    public void writeFailed(DiskManagerWriteRequest request, Throwable cause) {
                        complete();
                        if (dmPiece.isDone()) {
                            if (Logger.isEnabled()) {
                                Logger.log(new LogEvent(disk_manager, LOGID, "Piece " + dmPiece.getPieceNumber() + " write failed but already marked as done"));
                            }
                            listener.writeCompleted(request);
                        } else {
                            disk_manager.setFailed("Disk write error - " + Debug.getNestedExceptionMessage(cause));
                            Debug.printStackTrace(cause);
                            listener.writeFailed(request, cause);
                        }
                    }

                    protected void complete() {
                        try {
                            this_mon.enter();
                            async_writes--;
                            if (!write_requests.remove(request)) {
                                Debug.out("request not found");
                            }
                            if (stopped) {
                                async_write_sem.release();
                            }
                        } finally {
                            this_mon.exit();
                        }
                    }
                };
                try {
                    this_mon.enter();
                    if (stopped) {
                        buffer.returnToPool();
                        listener.writeFailed(request, new Exception("Disk writer has been stopped"));
                        return;
                    } else {
                        async_writes++;
                        write_requests.add(request);
                    }
                } finally {
                    this_mon.exit();
                }
                new requestDispatcher(request, l, buffer, chunks);
            }
        } catch (Throwable e) {
            request.getBuffer().returnToPool();
            disk_manager.setFailed("Disk write error - " + Debug.getNestedExceptionMessage(e));
            Debug.printStackTrace(e);
            listener.writeFailed(request, e);
        }
    }

    protected class requestDispatcher implements DiskAccessRequestListener {

        private DiskManagerWriteRequest request;

        private DiskManagerWriteRequestListener listener;

        private DirectByteBuffer buffer;

        private List chunks;

        private int chunk_index;

        protected requestDispatcher(DiskManagerWriteRequest _request, DiskManagerWriteRequestListener _listener, DirectByteBuffer _buffer, List _chunks) {
            request = _request;
            listener = _listener;
            buffer = _buffer;
            chunks = _chunks;
            dispatch();
        }

        protected void dispatch() {
            try {
                if (chunk_index == chunks.size()) {
                    listener.writeCompleted(request);
                } else {
                    if (chunk_index == 1 && chunks.size() > 32) {
                        for (int i = 1; i < chunks.size(); i++) {
                            final AESemaphore sem = new AESemaphore("DMW&C:dispatch:asyncReq");
                            final Throwable[] error = { null };
                            doRequest(new DiskAccessRequestListener() {

                                public void requestComplete(DiskAccessRequest request) {
                                    sem.release();
                                }

                                public void requestCancelled(DiskAccessRequest request) {
                                    Debug.out("shouldn't get here");
                                }

                                public void requestFailed(DiskAccessRequest request, Throwable cause) {
                                    error[0] = cause;
                                    sem.release();
                                }

                                public int getPriority() {
                                    return (-1);
                                }

                                public void requestExecuted(long bytes) {
                                }
                            });
                            sem.reserve();
                            if (error[0] != null) {
                                throw (error[0]);
                            }
                        }
                        listener.writeCompleted(request);
                    } else {
                        doRequest(this);
                    }
                }
            } catch (Throwable e) {
                failed(e);
            }
        }

        protected void doRequest(final DiskAccessRequestListener l) throws CacheFileManagerException {
            Object[] stuff = (Object[]) chunks.get(chunk_index++);
            final DiskManagerFileInfoImpl file = (DiskManagerFileInfoImpl) stuff[0];
            buffer.limit(DirectByteBuffer.SS_DR, ((Integer) stuff[2]).intValue());
            if (file.getAccessMode() == DiskManagerFileInfo.READ) {
                if (Logger.isEnabled()) Logger.log(new LogEvent(disk_manager, LOGID, "Changing " + file.getFile(true).getName() + " to read/write"));
                file.setAccessMode(DiskManagerFileInfo.WRITE);
            }
            boolean handover_buffer = chunk_index == chunks.size();
            DiskAccessRequestListener delegate_listener = new DiskAccessRequestListener() {

                public void requestComplete(DiskAccessRequest request) {
                    l.requestComplete(request);
                    file.dataWritten(request.getOffset(), request.getSize());
                }

                public void requestCancelled(DiskAccessRequest request) {
                    l.requestCancelled(request);
                }

                public void requestFailed(DiskAccessRequest request, Throwable cause) {
                    l.requestFailed(request, cause);
                }

                public int getPriority() {
                    return (-1);
                }

                public void requestExecuted(long bytes) {
                }
            };
            disk_access.queueWriteRequest(file.getCacheFile(), ((Long) stuff[1]).longValue(), buffer, handover_buffer, delegate_listener);
        }

        public void requestComplete(DiskAccessRequest request) {
            dispatch();
        }

        public void requestCancelled(DiskAccessRequest request) {
            Debug.out("shouldn't get here");
        }

        public void requestFailed(DiskAccessRequest request, Throwable cause) {
            failed(cause);
        }

        public int getPriority() {
            return (-1);
        }

        public void requestExecuted(long bytes) {
        }

        protected void failed(Throwable cause) {
            buffer.returnToPool();
            listener.writeFailed(request, cause);
        }
    }
}
