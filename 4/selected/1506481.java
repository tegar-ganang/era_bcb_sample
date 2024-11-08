package edu.indiana.cs.b534.torrent.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;
import edu.indiana.cs.b534.torrent.Constants;
import edu.indiana.cs.b534.torrent.StorageManager;
import edu.indiana.cs.b534.torrent.TorrentException;
import edu.indiana.cs.b534.torrent.TorrentMetainfo;
import edu.indiana.cs.b534.torrent.context.TorrentContext;

public class MemoryBasedStorageManager implements StorageManager {

    private PieceInfo[] piecesInformation;

    private static Logger log = Logger.getLogger(TorrentManager.TORRENT_MANAGER_NAME);

    private File storeFoler;

    private TorrentContext torrentContext;

    private int fileLength;

    private int totalDownloadedBytes = 0;

    private int totalUploadedBytes = 0;

    private int bytesLeftToBeDownloaded = 0;

    public MemoryBasedStorageManager(TorrentContext torrentContext) {
        this.torrentContext = torrentContext;
    }

    /**
     * This method will help to initialize the storage for the expected number of pieces
     *
     * @param pieceInfos
     * @param storeFoler - this is the folder the completed file will be saved
     * @throws TorrentException - this will be thrown when a piece is marked as completed and
     *                          the hash check for that piece failed
     */
    public synchronized void initialize(PieceInfo[] pieceInfos, File storeFoler, int fileLength) throws TorrentException {
        this.storeFoler = storeFoler;
        this.piecesInformation = pieceInfos;
        this.fileLength = fileLength;
        bytesLeftToBeDownloaded = fileLength;
    }

    public synchronized void initAsSeed(File fileToSeed, PieceInfo[] pieceInfo, int fileLength) throws TorrentException {
        byte[] bytes;
        try {
            InputStream is = new FileInputStream(fileToSeed);
            int offset = 0;
            int numRead = 0;
            for (int i = 0; i < pieceInfo.length; i++) {
                int pieceLength = pieceInfo[i].getPieceLength();
                bytes = new byte[pieceLength];
                int read = 0;
                while (read < pieceLength) {
                    int lastReadcount = is.read(bytes, 0, pieceLength);
                    if (lastReadcount == -1) {
                        break;
                    } else {
                        read = read + lastReadcount;
                    }
                }
                pieceInfo[i].setValue(bytes);
                pieceInfo[i].setCompleted(true);
            }
            is.close();
        } catch (IOException e) {
            throw new TorrentException(e);
        }
        initialize(pieceInfo, null, fileLength);
        totalDownloadedBytes = 0;
        totalDownloadedBytes = 0;
        bytesLeftToBeDownloaded = 0;
    }

    public synchronized boolean[] getPieceAvailability() {
        boolean[] pieceAvailabilty = new boolean[piecesInformation.length];
        boolean isAtLeastOnePiecePresent = false;
        for (int i = 0; i < pieceAvailabilty.length; i++) {
            boolean isCompleted = pieceAvailabilty[i] = piecesInformation[i].isCompleted();
            if (isCompleted) {
                isAtLeastOnePiecePresent = isCompleted;
            }
        }
        return isAtLeastOnePiecePresent ? pieceAvailabilty : null;
    }

    /**
     * This will store a downloaded block in to the torrent
     *
     * @param pieceIndex   - index of the piece which this block belongs to
     * @param blockOffset
     * @param bytes        - buffered data
     * @param sourceOffset - this is the offset of which we should look in the bytes buffer passed in
     */
    public synchronized void storeBlock(int pieceIndex, int blockOffset, byte[] bytes, int sourceOffset) throws TorrentException {
        PieceInfo pieceInfo = piecesInformation[pieceIndex];
        byte[] dest = pieceInfo.getValue();
        int downloadedByteCount = (bytes.length - sourceOffset);
        if ((downloadedByteCount + blockOffset) <= pieceInfo.getPieceLength()) {
            int amountOfDataToCopy = downloadedByteCount;
            System.arraycopy(bytes, sourceOffset, dest, blockOffset, amountOfDataToCopy);
            int blockIndex = blockOffset / Constants.BLOCK_SIZE;
            pieceInfo.setBlockAvailability(true, blockIndex);
        } else {
            throw new TorrentException("I got more data to be stored in the piece than I want. " + "Piece Offset = " + blockOffset + " length of data to be copied " + downloadedByteCount);
        }
        totalDownloadedBytes = totalDownloadedBytes + downloadedByteCount;
        bytesLeftToBeDownloaded = bytesLeftToBeDownloaded - downloadedByteCount;
    }

    /**
     * This will check whether the given piece is completed or not
     *
     * @param pieceIndex
     * @return
     */
    public synchronized boolean isPieceCompleted(int pieceIndex) {
        return piecesInformation[pieceIndex].isCompleted();
    }

    /**
     * will return the piece of the given index and null if the index can not be served from this
     *
     * @param pieceIndex
     * @return
     */
    public synchronized byte[] getPiece(int pieceIndex) {
        if (!(pieceIndex > piecesInformation.length)) {
            byte[] data = piecesInformation[pieceIndex].getValue();
            totalUploadedBytes += data.length;
            return data;
        }
        return null;
    }

    /**
     * This enables us to check whether we have completed this
     *
     * @return
     */
    public synchronized boolean isDownloadCompleted() {
        for (PieceInfo pieceInfo : piecesInformation) {
            if (!pieceInfo.isCompleted()) {
                return false;
            }
        }
        return true;
    }

    public synchronized void setDownloadCompleted() throws TorrentException {
        if (isDownloadCompleted()) {
            TorrentMetainfo torrentMetaInfo = torrentContext.getTorrentMetainfo();
            String fileName = torrentMetaInfo.getInfo().getName().getValue();
            try {
                File destinationFile = new File(storeFoler, fileName);
                if (!destinationFile.isFile()) {
                    destinationFile.createNewFile();
                }
                FileChannel wChannel = new FileOutputStream(destinationFile, true).getChannel();
                int writtenCount = 0;
                for (PieceInfo pieceInfo : piecesInformation) {
                    byte[] data = pieceInfo.getValue();
                    writtenCount = writtenCount + data.length;
                    wChannel.write(ByteBuffer.wrap(data));
                }
                wChannel.close();
                if (writtenCount != fileLength) {
                    throw new TorrentException("File is of length " + fileLength + " but only " + writtenCount + " is written ");
                }
                log.info(fileName + " saved successfully to " + storeFoler);
            } catch (IOException e) {
                throw new TorrentException("Can not save the downloaded the torrent file to " + storeFoler + File.pathSeparator + fileName + "\n Reason " + e);
            }
        } else {
            throw new TorrentException("Storage Manager is notified as completed, but some of the pieces are yet to complete");
        }
    }

    public synchronized PieceInfo getPieceInfo(int pieceIndex) {
        return this.piecesInformation[pieceIndex];
    }

    private static PieceInfo[] getBytesFromFile(File file, TorrentMetainfo metainfo) throws TorrentException {
        return new PieceInfo[0];
    }

    public static void main(String[] args) {
        File tempFile = new File("/home/chinthaka/Desktop/temp", "testFile.txt");
        try {
            tempFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getFileSize() {
        return fileLength;
    }

    public int getTotalDownloadedBytes() {
        return totalDownloadedBytes;
    }

    public int getTotalUploadedBytes() {
        return totalUploadedBytes;
    }

    public int getBytesLeftToBeDownloaded() {
        return bytesLeftToBeDownloaded;
    }
}
