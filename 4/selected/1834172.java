package org.drftpd.tests;

import net.sf.drftpd.SlaveUnavailableException;
import org.drftpd.GlobalContext;
import org.drftpd.LightSFVFile;
import org.drftpd.SFVFile;
import org.drftpd.id3.ID3Tag;
import org.drftpd.master.RemoteSlave;
import org.drftpd.master.RemoteTransfer;
import org.drftpd.remotefile.LinkedRemoteFile;
import org.drftpd.slave.RemoteIOException;
import org.drftpd.slave.SlaveStatus;
import org.drftpd.slave.TransferIndex;
import org.drftpd.slave.async.AsyncResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author zubov
 * @version $Id: DummyRemoteSlave.java 1764 2007-08-04 02:01:21Z tdsoul $
 */
public class DummyRemoteSlave extends RemoteSlave {

    public DummyRemoteSlave(String name, GlobalContext gctx) {
        super(name, gctx);
    }

    public int getPort() {
        return 10;
    }

    public void fakeConnect() {
        _errors = 0;
        _lastNetworkError = System.currentTimeMillis();
    }

    public boolean isOnline() {
        return false;
    }

    public LinkedRemoteFile getSlaveRoot() throws SlaveUnavailableException, IOException {
        return null;
    }

    public void listenForInfoFromSlave() {
    }

    public void connect(Socket socket, BufferedReader in) throws IOException {
    }

    public void disconnect() {
    }

    public int fetchMaxPathFromIndex(String maxPathIndex) throws SlaveUnavailableException {
        return 0;
    }

    public AsyncResponse fetchResponse(String index) throws SlaveUnavailableException {
        return null;
    }

    public SlaveStatus fetchStatusFromIndex(String statusIndex) throws SlaveUnavailableException {
        return null;
    }

    public void fetchAbortFromIndex(String abortIndex) throws IOException, SlaveUnavailableException {
    }

    public long fetchChecksumFromIndex(String index) throws RemoteIOException, SlaveUnavailableException {
        return 0;
    }

    public ID3Tag fetchID3TagFromIndex(String index) throws RemoteIOException, SlaveUnavailableException {
        return null;
    }

    public LightSFVFile fetchSFVFileFromIndex(String index) throws RemoteIOException, SlaveUnavailableException {
        return null;
    }

    public String issueChecksumToSlave(String string) throws SlaveUnavailableException {
        return null;
    }

    public String issueConnectToSlave(InetSocketAddress address, boolean encryptedDataChannel) throws SlaveUnavailableException {
        return null;
    }

    public String issueID3TagToSlave(String string) throws SlaveUnavailableException {
        return null;
    }

    public String issueSFVFileToSlave(String string) throws SlaveUnavailableException {
        return null;
    }

    public String issueListenToSlave(boolean encryptedDataChannel) throws SlaveUnavailableException {
        return null;
    }

    public String issueMaxPathToSlave() throws SlaveUnavailableException {
        return null;
    }

    public String issuePingToSlave() throws SlaveUnavailableException {
        return null;
    }

    public String issueReceiveToSlave(String name) throws SlaveUnavailableException {
        return null;
    }

    public String issueTransferStatusToSlave(String transferIndex) throws SlaveUnavailableException {
        return null;
    }

    public String issueAbortToSlave(RemoteTransfer transferIndex) throws SlaveUnavailableException {
        return null;
    }

    public String issueSendToSlave(String name) throws SlaveUnavailableException {
        return null;
    }

    public String issueStatusToSlave() throws SlaveUnavailableException {
        return null;
    }

    public RemoteTransfer fetchTransferIndexFromIndex(String index) throws IOException, SlaveUnavailableException {
        return null;
    }

    public boolean transferIsUpdated(RemoteTransfer transferIndex) {
        return false;
    }

    public void run() {
    }

    public String issueDeleteToSlave(String sourceFile) throws SlaveUnavailableException {
        return null;
    }

    public String issueRenameToSlave(String from, String toDirPath, String toName) throws SlaveUnavailableException {
        return null;
    }

    public String issueTransferStatusToSlave(RemoteTransfer transferIndex) throws SlaveUnavailableException {
        return null;
    }

    public void issueAbortToSlave(TransferIndex transferIndex) throws SlaveUnavailableException {
    }

    public String issueReceiveToSlave(String name, char c, long position, TransferIndex index) throws SlaveUnavailableException {
        return null;
    }

    public String issueSendToSlave(String name, char c, long position, TransferIndex index) throws SlaveUnavailableException {
        return null;
    }

    public String issueTransferStatusToSlave(TransferIndex transferIndex) throws SlaveUnavailableException {
        return null;
    }

    public boolean transferIsUpdated(TransferIndex transferIndex) {
        return false;
    }

    public void connect(Socket socket, BufferedReader reader, PrintWriter writer) throws IOException {
    }

    public String issueRemergeToSlave() throws SlaveUnavailableException {
        return null;
    }

    public void fetchRemergeResponseFromIndex(String index) throws IOException, SlaveUnavailableException {
    }

    public void commit() {
    }
}
