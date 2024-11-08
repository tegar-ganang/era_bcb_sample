package gnu.saw.client.session;

import java.io.File;
import gnu.saw.client.SAWClient;
import gnu.saw.client.connection.SAWClientConnection;
import gnu.saw.client.console.SAWClientServerWriter;
import gnu.saw.client.console.SAWClientServerReader;
import gnu.saw.client.filesystem.SAWClientZipFileCreateOperation;
import gnu.saw.client.filesystem.SAWClientZipFileExtractOperation;
import gnu.saw.client.filesystem.SAWClientZipFileStoreOperation;
import gnu.saw.client.filetransfer.SAWFileTransferClient;
import gnu.saw.client.graphicsmode.SAWGraphicsModeClient;
import gnu.saw.graphics.clipboard.SAWClipboardTransferTask;

public class SAWClientSession {

    private File workingDirectory;

    private Thread readerThread;

    private Thread writerThread;

    private Thread fileTransferThread;

    private Thread graphicsThread;

    private Thread clipboardTransferThread;

    private Thread zipFileCreateThread;

    private Thread zipFileStoreThread;

    private Thread zipFileExtractThread;

    private SAWClient client;

    private SAWClientConnection connection;

    private SAWClientServerReader serverReader;

    private SAWClientServerWriter clientWriter;

    private SAWFileTransferClient fileTransferClient;

    private SAWGraphicsModeClient graphicsClient;

    private SAWClipboardTransferTask clipboardTransferTask;

    private SAWClientZipFileCreateOperation zipFileCreateOperation;

    private SAWClientZipFileStoreOperation zipFileStoreOperation;

    private SAWClientZipFileExtractOperation zipFileExtractOperation;

    public SAWClientSession(SAWClient client, SAWClientConnection connection) {
        this.client = client;
        this.connection = connection;
        this.fileTransferThread = new Thread();
        this.graphicsThread = new Thread();
        this.clipboardTransferThread = new Thread();
        this.zipFileCreateThread = new Thread();
        this.zipFileStoreThread = new Thread();
        this.zipFileExtractThread = new Thread();
        this.serverReader = new SAWClientServerReader(this);
        this.clientWriter = new SAWClientServerWriter(this);
        this.fileTransferClient = new SAWFileTransferClient(this);
        this.graphicsClient = new SAWGraphicsModeClient(this);
        this.clipboardTransferTask = new SAWClipboardTransferTask();
        this.zipFileCreateOperation = new SAWClientZipFileCreateOperation(this);
        this.zipFileStoreOperation = new SAWClientZipFileStoreOperation(this);
        this.zipFileExtractOperation = new SAWClientZipFileExtractOperation(this);
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public SAWClient getClient() {
        return client;
    }

    public SAWClientServerWriter getClientWriter() {
        return clientWriter;
    }

    public SAWClientConnection getConnection() {
        return connection;
    }

    public Thread getReaderThread() {
        return readerThread;
    }

    public void setReaderThread(Thread readerThread) {
        this.readerThread = readerThread;
    }

    public Thread getFileTransferThread() {
        return fileTransferThread;
    }

    public void setFileTransferThread(Thread fileTransferThread) {
        this.fileTransferThread = fileTransferThread;
    }

    public Thread getGraphicsThread() {
        return graphicsThread;
    }

    public void setGraphicsThread(Thread graphicsThread) {
        this.graphicsThread = graphicsThread;
    }

    public Thread getClipboardTransferThread() {
        return clipboardTransferThread;
    }

    public void setClipboardTransferThread(Thread clipboardTransferThread) {
        this.clipboardTransferThread = clipboardTransferThread;
    }

    public SAWFileTransferClient getFileTransferClient() {
        return fileTransferClient;
    }

    public Thread getWriterThread() {
        return writerThread;
    }

    public void setWriterThread(Thread writerThread) {
        this.writerThread = writerThread;
    }

    public SAWGraphicsModeClient getGraphicsClient() {
        return graphicsClient;
    }

    public SAWClipboardTransferTask getClipboardTransferTask() {
        return clipboardTransferTask;
    }

    public void setZipFileCreateOperation(SAWClientZipFileCreateOperation zipFileCreateOperation) {
        this.zipFileCreateOperation = zipFileCreateOperation;
    }

    public SAWClientZipFileCreateOperation getZipFileCreateOperation() {
        return zipFileCreateOperation;
    }

    public void setZipFileStoreOperation(SAWClientZipFileStoreOperation zipFileStoreOperation) {
        this.zipFileStoreOperation = zipFileStoreOperation;
    }

    public SAWClientZipFileStoreOperation getZipFileStoreOperation() {
        return zipFileStoreOperation;
    }

    public void setZipFileExtractOperation(SAWClientZipFileExtractOperation zipFileExtractOperation) {
        this.zipFileExtractOperation = zipFileExtractOperation;
    }

    public SAWClientZipFileExtractOperation getZipFileExtractOperation() {
        return zipFileExtractOperation;
    }

    public Thread getZipFileCreateThread() {
        return zipFileCreateThread;
    }

    public void setZipFileCreateThread(Thread zipFileCreateThread) {
        this.zipFileCreateThread = zipFileCreateThread;
    }

    public Thread getZipFileStoreThread() {
        return zipFileStoreThread;
    }

    public void setZipFileStoreThread(Thread zipFileStoreThread) {
        this.zipFileStoreThread = zipFileStoreThread;
    }

    public Thread getZipFileExtractThread() {
        return zipFileExtractThread;
    }

    public void setZipFileExtractThread(Thread zipFileExtractThread) {
        this.zipFileExtractThread = zipFileExtractThread;
    }

    public boolean isStopped() {
        return serverReader.isStopped() || clientWriter.isStopped() || !connection.isConnected();
    }

    public void setStopped(boolean stopped) {
        serverReader.setStopped(stopped);
        clientWriter.setStopped(stopped);
        fileTransferClient.getHandler().getSession().getTransfer().setStopped(stopped);
        graphicsClient.setStopped(true);
    }

    public void startSession() {
        serverReader.setStopped(false);
        clientWriter.setStopped(false);
    }

    public void startSessionThreads() {
        readerThread = new Thread(serverReader, "SAWClientServerReader");
        readerThread.setPriority(Thread.NORM_PRIORITY);
        readerThread.start();
        writerThread = new Thread(clientWriter, "SAWClientServerWriter");
        writerThread.setPriority(Thread.NORM_PRIORITY);
        writerThread.start();
    }

    public void waitSession() {
        synchronized (this) {
            while (!isStopped()) {
                try {
                    wait();
                } catch (Exception e) {
                    return;
                }
            }
        }
    }

    public void tryStopSessionThreads() {
        setStopped(true);
        if (clipboardTransferThread.isAlive()) {
            clipboardTransferThread.interrupt();
        }
        if (zipFileCreateThread.isAlive()) {
            zipFileCreateThread.interrupt();
        }
        if (zipFileStoreThread.isAlive()) {
            zipFileStoreThread.interrupt();
        }
        if (zipFileExtractThread.isAlive()) {
            zipFileExtractThread.interrupt();
        }
    }

    public void waitThreads() {
        try {
            readerThread.join();
            writerThread.join();
            fileTransferThread.join();
            graphicsThread.join();
            clipboardTransferThread.join();
            zipFileCreateThread.join();
            zipFileStoreThread.join();
            zipFileExtractThread.join();
        } catch (Exception e) {
        }
    }
}
