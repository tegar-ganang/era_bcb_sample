package org.jmule.core.protocol.donkey;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jmule.core.Download;
import org.jmule.core.partialfile.Gap;
import org.jmule.core.partialfile.GapList;
import org.jmule.util.Convert;

/** DonkeyReceiveFileSession cares about Downloading a file from one source conneted by its DonkeyClientConnection.<p>
 * a download goes this way:<br>
 * handshake end (filenameAnswer is optinal!) -&gt;additional information-&gt;last prerequest-&gt;filetransfer<br>
 * fileamerequest may not accepted, or time out in most cases while waiting on queue so we proccess further (only a few client soft send *fileregret* packet if file isn't shared)<p> 
 * handshake end: send Filenamerequest<br>
 * filenameAnswer: receive FilenamerequestAnswer<br>
 * additional information: send Part and HashsetRequest if file is greater than one eDonkenkeyPart (PARTSIZE)<br>
 * last prerequest:if all requests was answerd positive, send StartUpLoadRequest<br>
 * <h5>Filepart-requesting</h5>
 * Allways ask for tree blocks to send in specified order:<br>
 * request = (1,2,3); <br>
 * Where 1 is a valid block a gap or a part of a gap with size less equal BLOCKSIZE. 
 * If the client wants more than one block 2(,3) have to be a valid block like 1.
 * Everytime the client get a block requested it has to rerequest and send a request (2,3,4) if 4 is a valid block, if not rerequest only 2 and 3 will be send by peer, no new request are accepted, if the peer recives the rerequest of this client before send of last byte of last *1* these bytes will not send but the bytes of new *1*.
 * In case of requesting at all less than 3 parts just fill request with empty block(s) [0:0].<br>
 * <p><i>[editor's(emarant) note:]</i> jMule currently doesn't behave exact like this, some (maybe all) versions of emule, too.
 * 
 * wolfc 09-feb-2003: my interpretation of the protocol
 *<pre>
 * >hello
 * <helloans
 * >filereq
 * <fileans
 * ( optional:
 *  >hashreq
 *  <hashans
 * }
 * >statusreq
 * <statusans
 * >startupload
 * <acceptupload
 * >partreq
 * <partans+
 * </pre>
 * <table>
 * <tr colspan="2"><td>small file</td><tr colspan="3"><td>big file</td>
 *</tr>
 * <tr><td>has file</td><td>hasn't file</td><td>has file</td><td>hasn't file parts, but knows name</td><td>hasn't file</td>
 *</tr>
 *
 *</table>
 * @author emarant, casper, wolfc
 * @version $Revision: 1.1.1.1 $
 * <br>Last changed by $Author: jmartinc $ on $Date: 2005/04/22 21:44:31 $
 */
public class DonkeyReceiveFileSession implements DonkeyPacketConstants {

    private Logger log = Logger.getLogger(DonkeyReceiveFileSession.class.getName());

    private static final int WAITING = 0;

    private static final int REQUESTING_FILE = 1;

    private static final int REQUESTING_HASH = 2;

    private static final int REQUESTING_STATUS = 3;

    private static final int REQUESTING_STARTUP = 4;

    private static final int REQUESTING_PARTS = 5;

    private int state = WAITING;

    /** 
	 * Constructor for DonkeyReceiveFileSession.
	 * The session will wait for a HELLO, HELLO_ANSWER or CALLBACK_HINT. So make sure
	 * that a packet has already been send requesting this kind of answer.
	 * 
	 * @param dcc		the DonkeyClientConnection which wants to receive a file
	 */
    private DonkeyReceiveFileSession(DonkeyClientConnection dcc) {
        this(dcc, true);
    }

    /**
	 * Initiate a receive file session.
	 * @param dcc		the DonkeyClientConnection which wants to receive a file
	 * @param waitForHello	whether to wait for a HELLO_ANSWER packet to start
	 */
    protected DonkeyReceiveFileSession(DonkeyClientConnection dcc, boolean waitForHello) {
        taggedGaps = new GapList();
        Gap gap = new Gap(0, 0);
        gaps[0] = gap;
        gaps[1] = gap;
        gaps[2] = gap;
        this.dcc = dcc;
        this.dContext = DonkeyProtocol.getInstance();
        currentDownload = dcc.source.getDownload();
        fileHash = ((DonkeyFileHash) currentDownload.getHashes().get("DonkeyHash")).getBytes();
        if (log.isLoggable(Level.FINEST)) log.finest("new DonkeyReceiveFileSession for " + dcc.getConnectionNumber());
        if (!waitForHello) nextStep();
    }

    /**
	 * Called from the connection to indicate closure
	 */
    protected void close() {
        if (state == WAITING && dcc.getSelectionKey() == null) {
        } else if (state == WAITING) {
            log.fine("closing peer " + getPeerAddress() + " (state = " + state + ")");
            log.fine("socket channel = " + dcc.getChannel());
            SelectionKey key = dcc.getSelectionKey();
            log.fine("interestedOps = " + ((key != null && key.isValid()) ? Integer.toHexString(key.interestOps()) : "n/a"));
            checkSendQueue(dcc);
        } else if (state == REQUESTING_PARTS) {
            Exception e = new Exception();
            log.log(Level.FINE, "closing peer " + getPeerAddress() + " (transferring)", e);
        } else if (state != REQUESTING_STARTUP) log.fine("closing peer " + getPeerAddress() + " (state = " + state + ")");
        Collection taggedGapList = taggedGaps.getGaps();
        Iterator it = taggedGapList.iterator();
        ArrayList removedgaps = new ArrayList();
        while (it.hasNext()) {
            Gap gap = (Gap) it.next();
            log.finest(dcc.getConnectionNumber() + " undo tagging for gap: " + gap.getStart() + "-" + gap.getEnd());
            currentDownload.getUntaggedGaps().addGap(gap.getStart(), gap.getEnd());
            removedgaps.add(gap);
        }
        while (!removedgaps.isEmpty()) {
            Gap gap = (Gap) removedgaps.remove(removedgaps.size() - 1);
            taggedGaps.removeGap(gap.getStart(), gap.getEnd());
        }
        closed = true;
        dcc.source.setActive(false);
        if (dcc.isConnected()) {
            dcc.source.setNextRetry();
        } else {
            dcc.source.setNextRetry(System.currentTimeMillis() + dContext.getSourceRetryInterval() * 1000);
        }
    }

    protected void finalize() throws Throwable {
        if ((taggedGaps.byteSize() != 0 || !taggedGaps.getGaps().isEmpty())) {
            if (dcc != null) {
                Collection taggedGapList = taggedGaps.getGaps();
                Iterator it = taggedGapList.iterator();
                while (it.hasNext()) {
                    Gap gap = (Gap) it.next();
                    currentDownload.getUntaggedGaps().addGap(gap.getStart(), gap.getEnd());
                }
            }
            String message = " taggged gaplist error " + closed + " tagged gaps sum " + taggedGaps.byteSize() + " contains gaps " + taggedGaps.getGaps().isEmpty() + (dcc != null ? " source:" + dcc.source + " connection: " + dcc.getConnectionNumber() : " " + Convert.bytesToHexString(fileHash));
            log.severe(message);
            System.out.println((new Date(System.currentTimeMillis())) + " " + this.getClass() + " " + message);
            (new org.jmule.ui.sacli.command.temp.CloseCommand()).execute(new org.jmule.core.internalCommunications.DirectComClient());
        }
    }

    protected void nextStep() {
        if (currentDownload.getState() == Download.COMPLETE) {
            if (!cycleDownload()) {
                dcc.source.setRemove(true);
                sendReleaseSlot();
                return;
            }
        }
        if (dcc.source.getPeerFileName() == null && !requestedFileName) {
            log.finest(this + " get filename");
            requestedFileName = true;
            requestFile(0);
        }
        if (needHashSet()) {
            log.finest(this + " get hashset");
            requestHashSet();
        } else if (needFreshPartList()) {
            log.finest(this + " get partlist");
            requestPartList();
        } else if (!needPartList() && (dcc.source.getPartList() == null || dcc.source.getPartList().isEmpty())) {
            if (dcc.source.getPartList() == null) {
                sendReleaseSlot();
                return;
            }
            log.finest(this + " has no parts for download");
            return;
        } else if (needUploadRequest()) {
            log.finest(this + " get remote slot");
            requestUpload();
        } else if (needParts() && !needPartList()) {
            log.finest(this + " get parts");
            try {
                requestParts();
            } catch (NullPointerException npe) {
                log.log(Level.SEVERE, dcc.getConnectionNumber() + " " + currentDownload + " " + dcc.clientDebbugInfo(), npe);
                sendReleaseSlot();
                return;
            }
        }
    }

    private long lastPartListStepTime;

    private long lastUploadRequestStepTime;

    /**
	 * Called from the connection for periodic updates.
     * @param count - ignored
     * @return <code>true</code>
	 */
    protected boolean check(int count) {
        return true;
    }

    /**
     * Called from the connection to see what should happen next.
     * @todo add single source remove on fileregret if multisource
     */
    protected int checkPacket(DonkeyScannedPacket inPacket) throws CorruptPacketException {
        int command = Convert.byteToInt(inPacket.getCommandId());
        switch(command) {
            case OP_HELLOANSWER:
                DonkeyClientInfo dci = inPacket.getClientInfoPacket();
                if (log.isLoggable(Level.FINER)) log.finer(dcc.getConnectionNumber() + " " + (System.currentTimeMillis() - dcc.transferStart) + " Connection established to " + Long.toString(dci.getUserId()) + ", port " + Integer.toString(dci.getPort()) + " Username: " + dci.getUsername());
                dcc.source.setDonkeyClientInfo(dci);
                dcc.source.retrySuccess();
                nextStep();
                break;
            case OP_FILEREQANSWER:
                processFileRequestAnswer(inPacket);
                break;
            case OP_HASHSETANSWER:
                processHashSetAnswer(inPacket);
                nextStep();
                break;
            case OP_FILESTATUS:
                processPartListAnswer(inPacket);
                nextStep();
                break;
            case OP_ACCEPTUPLOADREQ:
                canstartdownload = true;
                nextStep();
                dcc.source.setActive(true);
                break;
            case OP_SENDINGPART:
                processSendingPart(inPacket);
                nextStep();
                break;
            case OP_FILEREQREGRET:
                {
                    log.info(this + ": hasn't file reqested");
                    Download download = currentDownload;
                    if (!cycleDownload()) {
                        dcc.source.setRemove(true);
                        sendReleaseSlot();
                    } else {
                        dcc.source.removeSingleSource(download);
                        nextStep();
                    }
                    break;
                }
            case OP_QUEUEPOSITION:
                {
                    long queueposition = inPacket.getQueuePosition();
                    dcc.source.setQueuePosition(queueposition);
                    log.finer(dcc.getConnectionNumber() + " get on queue position:" + queueposition);
                    break;
                }
            case OP_UPLOADEND:
                {
                    log.fine(dcc.getConnectionNumber() + " " + (System.currentTimeMillis() - dcc.transferStart) + " Received OP_UPLOADEND. remaining chunk(s) requested from this source " + taggedGaps + " \n size in byte: " + taggedGaps.byteSize() + " client: " + dcc.clientDebbugInfo());
                    Collection taggedGapList = taggedGaps.getGaps();
                    ArrayList removedgaps = new ArrayList();
                    Iterator it = taggedGapList.iterator();
                    while (it.hasNext()) {
                        Gap gap = (Gap) it.next();
                        currentDownload.getUntaggedGaps().addGap(gap.getStart(), gap.getEnd());
                        removedgaps.add(gap);
                    }
                    while (!removedgaps.isEmpty()) {
                        Gap gap = (Gap) removedgaps.remove(removedgaps.size() - 1);
                        taggedGaps.removeGap(gap.getStart(), gap.getEnd());
                    }
                    gaps[0] = new Gap(0, 0);
                    gaps[1] = new Gap(0, 0);
                    gaps[2] = new Gap(0, 0);
                    state = REQUESTING_STARTUP;
                    dcc.state = DonkeyConnectionSkeleton.STATE_ENQUEUE;
                    break;
                }
            case OP_HELLO:
            case OP_CALLBACKHINT:
            default:
                {
                    log.warning("unknown/unexpected packettype " + Integer.toHexString(command));
                    return UNKNOWNPACKET;
                }
        }
        return 0;
    }

    final int UNKNOWNPACKET = -1;

    protected boolean needUploadRequest() {
        return !canstartdownload;
    }

    /**
     * @deprecated
     */
    protected void requestFile() {
        log.finer("calling deprecated requestFile (state = " + state + ")");
        return;
    }

    private Gap getWholeGap() {
        Gap wholeGap = new Gap(0, 0);
        int transferedChunk = dcc.source.getMostWantedChunkNum();
        long fileSize = currentDownload.getFileSize();
        long chunkStart = (transferedChunk - 1) * PARTSIZE;
        long chunkEnd = transferedChunk * PARTSIZE;
        if (transferedChunk != 0) {
            wholeGap = currentDownload.getTaggedGap(chunkStart, chunkEnd, BLOCKSIZE);
            if (wholeGap.size() > 0) {
                long gapStart = wholeGap.getStart();
                long gapEnd = wholeGap.getEnd();
                if (gapStart % 1024 != 0) {
                    gapStart = 1024 * (gapStart / 1024);
                    if (gapEnd - gapStart > BLOCKSIZE) {
                        long oldGapEnd = gapEnd;
                        gapEnd = gapStart + BLOCKSIZE;
                        currentDownload.getUntaggedGaps().addGap(oldGapEnd, gapEnd);
                    }
                    log.finest(dcc.getConnectionNumber() + " adjust gap " + gapStart + "-" + gapEnd);
                }
                if ((gapEnd % 1024 != 0) && gapEnd != fileSize) {
                    long oldGapEnd = gapEnd;
                    gapEnd = Math.min(1024 * ((gapEnd / 1024) + 1), fileSize);
                    currentDownload.tagGap(new Gap(oldGapEnd, gapEnd));
                    log.finest(dcc.getConnectionNumber() + " extra gap tag " + oldGapEnd + "-" + gapEnd);
                }
                wholeGap = new Gap(gapStart, gapEnd);
                taggedGaps.addGap(gapStart, gapEnd);
            }
        }
        return wholeGap;
    }

    protected boolean isFinished() {
        return finished;
    }

    private DonkeyClientConnection dcc;

    private DonkeyScannedPacket inPacket;

    private DonkeyProtocol dContext;

    /** this holds the number of already transfered bytes of the actual requested block */
    private int blockBytes;

    private int newBlockSize = 0;

    private long lastBlockRequest = 0;

    private long requestedBytes = 0;

    private boolean finished = false;

    private boolean nextBlockRequested = false;

    private boolean gotPartRequest = false;

    private boolean requestedFileName = false;

    private boolean transfering = false;

    private boolean noNeededParts = false;

    private boolean change = false;

    private boolean enqueue = false;

    private boolean bigfile = false;

    private boolean canstartdownload = false;

    private boolean requestedPartList = false;

    private Gap[] gaps = new Gap[3];

    private long lastRequestedBlockSize = 0;

    private boolean endConnection = false;

    private GapList taggedGaps;

    private long fileBytesReceived = 0;

    private long queueSince = 0;

    private byte[] fileHash;

    private Download currentDownload;

    private String peerFilename = "";

    private boolean closed = false;

    private void checkSendQueue(DonkeyClientConnection dcc) {
        DonkeyPacket packet[] = new DonkeyPacket[0];
        packet = (DonkeyPacket[]) dcc.dSendQueue.toArray(packet);
        log.fine("send queue size = " + packet.length);
        for (int i = 0; i < packet.length; i++) {
            log.fine(i + ": " + packet[i]);
        }
    }

    private void enqueue() {
        if (log.isLoggable(Level.FINER)) log.finer(this + ": enqueuing");
        long now = System.currentTimeMillis();
        state = REQUESTING_STARTUP;
        dcc.addOutPacket(DonkeyPacketFactory.command(OP_STARTUPLOADREQ, fileHash));
        dcc.state = DonkeyConnectionSkeleton.STATE_ENQUEUE;
        queueSince = now;
        DonkeyClientInfo clientinfo = dcc.source.getDonkeyClientInfo();
        enqueue = true;
    }

    private InetSocketAddress getPeerAddress() {
        return dcc.getPeerAddress();
    }

    public long getQueueSince() {
        return queueSince;
    }

    /**
	 * Is the file larger than one part?
	 */
    private boolean isBigFile() {
        return currentDownload.getFileSize() > PARTSIZE;
    }

    /**
	 * Specifies whether we want a new part list or we're gonna
	 * use an old one.
	 */
    private boolean needFreshPartList() {
        long now = System.currentTimeMillis();
        if (now > lastPartListStepTime + 600000) {
            lastPartListStepTime = now;
            if (isBigFile() && dcc.source.getPartList() != null && !dcc.source.hasAllParts()) return true;
            return needPartList();
        } else if (now > lastPartListStepTime + 60000) {
            return dcc.source.getPartList() == null ? needPartList() : false;
        }
        return false;
    }

    private boolean needHashSet() {
        if (isBigFile()) return !currentDownload.getHashes().containsKey("DonkeyHashes");
        return false;
    }

    /**
	 * We only need a partlist for big files (> PARTSIZE).
	 * If the file is smaller we'll assume the source has all(/one) parts.
	 */
    private boolean needPartList() {
        if (dcc.source.getPartList() != null) {
            return false;
        }
        if (isBigFile()) {
            return true;
        }
        BitSet parts = new BitSet(1);
        parts.set(0);
        dcc.source.setPartList(parts, currentDownload);
        return false;
    }

    private boolean needParts() {
        return (blockBytes >= gaps[0].size()) && !noNeededParts;
    }

    private void processFileRequestAnswer(DonkeyScannedPacket inPacket) throws CorruptPacketException {
        if (!inPacket.isExpectedFileID(fileHash)) {
            return;
        }
        peerFilename = inPacket.getFileName();
        String tmpOldFileName = dcc.source.setPeerFileName(peerFilename);
        if (log.isLoggable(Level.FINEST)) log.finest(dcc.getConnectionNumber() + " " + tmpOldFileName);
        if (log.isLoggable(Level.FINEST)) log.finest(this + ": Peer knows file as " + peerFilename);
    }

    private void processHashSetAnswer(DonkeyScannedPacket inPacket) throws CorruptPacketException {
        if (!needHashSet()) {
            log.fine("already got a hashset!?");
            return;
        }
        log.fine(this + ": received hash set.");
        byte[][] hashes = inPacket.getHashSet();
        if (hashes == null) {
            log.info(this + ": invalid hash set packet received.");
            return;
        }
        boolean matches = true;
        for (int i = 0; i < 16; i++) {
            if (hashes[0][i] != fileHash[i]) {
                matches = false;
                break;
            }
        }
        if (matches) {
            int numChunks = DonkeyProtocol.calcNumChunks(currentDownload.getFileSize());
            if ((hashes.length < 2 ? hashes.length : hashes.length - 1) != numChunks) {
                log.warning(this + ": hashset length wrong " + (hashes.length < 2 ? hashes.length : hashes.length - 1) + " != " + numChunks);
                return;
            }
            if (!DonkeyHashFile.isValidHashset(hashes)) {
                log.warning(this + ": hashset invalid");
                return;
            }
            currentDownload.getHashes().put("DonkeyHashes", new DonkeyFileHashSet(hashes));
        } else log.warning(this + ": wrong hashset");
    }

    private void processPartListAnswer(DonkeyScannedPacket inPacket) throws CorruptPacketException {
        if (!inPacket.isExpectedFileID(fileHash)) {
            return;
        }
        int numChunks = DonkeyProtocol.calcNumChunks(currentDownload.getFileSize());
        BitSet parts = inPacket.getPartList(numChunks);
        dcc.source.setPartList(parts, currentDownload);
    }

    private void processSendingPart(DonkeyScannedPacket inPacket) throws CorruptPacketException {
        try {
            if (!inPacket.isExpectedFileID(fileHash)) {
                return;
            }
            long[] receivedWindow = inPacket.getFileBlock(currentDownload.getPartialFile());
            int nbytes = (int) (receivedWindow[1] - receivedWindow[0]);
            blockBytes += nbytes;
            taggedGaps.removeGap(receivedWindow[0], receivedWindow[1]);
            fileBytesReceived += nbytes;
            if (log.isLoggable(Level.FINER)) {
                log.finest(getPeerAddress() + " " + dcc.getConnectionNumber() + " " + (System.currentTimeMillis() - dcc.transferStart) + " Received file block " + receivedWindow[0] + "-" + receivedWindow[1] + "." + " fileBytesReceived:" + currentDownload.getPartialFile().getTransferredBytes());
            }
            if (taggedGaps.byteSize() == 0) {
                log.info(this + ": all requested parts from peer for " + currentDownload + " recieved");
                state = WAITING;
                sendEndTransfer();
            }
        } catch (Exception e) {
            log.log(Level.WARNING, this + ": exception", e);
        }
    }

    /**
	 * Send a file request.
	 * @param dummy	because the protected method is deprecated
	 */
    private void requestFile(int dummy) {
        if (log.isLoggable(Level.FINER)) log.finer(this + ": requesting file id=" + Convert.bytesToHexString(fileHash));
        state = REQUESTING_FILE;
        dcc.addOutPacket(DonkeyPacketFactory.fileRequest(fileHash));
    }

    private void requestHashSet() {
        assert needHashSet();
        if (log.isLoggable(Level.FINER)) log.finer(this + ": requesting hashset");
        state = REQUESTING_HASH;
        dcc.addOutPacket(DonkeyPacketFactory.command(OP_HASHSETREQUEST, fileHash));
    }

    private void requestParts() {
        if (dcc.waiting) {
            dcc.waiting = false;
        }
        if (gaps[0].size() == 0) {
            for (int i = 0; i < 3; i++) {
                gaps[i] = getWholeGap();
            }
        } else {
            gaps[0] = gaps[1];
            gaps[1] = gaps[2];
            if (gaps[1].size() > 0) {
                gaps[2] = getWholeGap();
            }
        }
        if (gaps[0].size() == 0) {
            if (state == REQUESTING_PARTS) {
                sendEndTransfer();
                log.info(this + ": no needed parts (NYI)");
            }
            return;
        }
        if (log.isLoggable(Level.FINE)) {
            log.fine(this + ": " + "Requesting parts: " + gaps[0].getStart() + "-" + gaps[0].getEnd() + " " + gaps[1].getStart() + "-" + gaps[1].getEnd() + " " + gaps[2].getStart() + "-" + gaps[2].getEnd());
        }
        state = REQUESTING_PARTS;
        dcc.addOutPacket(DonkeyPacketFactory.partRequest(fileHash, gaps[0].getStart(), gaps[1].getStart(), gaps[2].getStart(), gaps[0].getEnd(), gaps[1].getEnd(), gaps[2].getEnd()));
        dcc.state = DonkeyConnectionSkeleton.STATE_TRANSFERING;
        blockBytes = 0;
    }

    private void requestPartList() {
        if (log.isLoggable(Level.FINER)) log.finer(this + ": requesting status");
        state = REQUESTING_STATUS;
        dcc.addOutPacket(DonkeyPacketFactory.partRequest(fileHash));
    }

    private void requestUpload() {
        long now = System.currentTimeMillis();
        if (now > lastUploadRequestStepTime + 40 * 60 * 1000) {
            lastUploadRequestStepTime = now;
            enqueue();
        }
    }

    private void sendEndTransfer() {
        dcc.addOutPacket(DonkeyPacketFactory.command(OP_ENDTRANSFER, fileHash));
        if (!cycleDownload()) {
            noNeededParts = true;
            dcc.source.setNoNeededParts(true);
            dcc.source.setNextRetry(System.currentTimeMillis() + (dContext.getNoNeededPartsRetry() * 1000));
            if (currentDownload.getState() == Download.COMPLETE) {
                dcc.source.setRemove(true);
                sendReleaseSlot();
            }
        } else {
            blockBytes = 0;
            Gap gap = new Gap(0, 0);
            gaps[0] = gap;
            gaps[1] = gap;
            gaps[2] = gap;
            canstartdownload = false;
            dcc.state = DonkeyConnectionSkeleton.STATE_CONNECTED;
        }
    }

    private void sendReleaseSlot() {
        dcc.addOutPacket(DonkeyPacketFactory.command(OP_CANCELTRANSFER));
    }

    protected void changeDownload() {
        if (taggedGaps.byteSize() > 0) {
            Iterator it = taggedGaps.getGaps().iterator();
            ArrayList removedgaps = new ArrayList();
            while (it.hasNext()) {
                Gap gap = (Gap) it.next();
                log.finest(dcc.getConnectionNumber() + " undo tagging for gap: " + gap.getStart() + "-" + gap.getEnd());
                currentDownload.getUntaggedGaps().addGap(gap.getStart(), gap.getEnd());
                removedgaps.add(gap);
            }
            while (!removedgaps.isEmpty()) {
                Gap gap = (Gap) removedgaps.remove(removedgaps.size() - 1);
                taggedGaps.removeGap(gap.getStart(), gap.getEnd());
            }
        }
        if (canstartdownload && gaps[0].size() > 0) {
            dcc.addOutPacket(DonkeyPacketFactory.command(OP_ENDTRANSFER, fileHash));
            Gap gap = new Gap(0, 0);
            gaps[0] = gap;
            gaps[1] = gap;
            gaps[2] = gap;
        }
        currentDownload = dcc.source.getDownload();
        fileHash = ((DonkeyFileHash) currentDownload.getHashes().get("DonkeyHash")).getBytes();
        lastPartListStepTime = 0;
        requestedFileName = false;
        nextStep();
    }

    private boolean cycleDownload() {
        if (dcc.source.cycleActiveDownload(DonkeyDownloadSource.DownloadSelectStrategyNext)) {
            currentDownload = dcc.source.getDownload();
            log.fine("change download " + currentDownload);
            fileHash = ((DonkeyFileHash) currentDownload.getHashes().get("DonkeyHash")).getBytes();
            lastPartListStepTime = 0;
            lastUploadRequestStepTime = 0;
            requestedFileName = false;
            return true;
        } else {
            log.fine("change not download " + currentDownload);
        }
        return false;
    }

    public String toString() {
        return "DonkeyReceiveFileSession[" + getPeerAddress() + " " + dcc.getConnectionNumber() + " " + (System.currentTimeMillis() - dcc.transferStart) + "]";
    }
}
