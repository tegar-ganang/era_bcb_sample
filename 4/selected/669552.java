package client;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class PacketTransportManager implements ControllableTransportManager {

    private static final int BLOCK_SIZE = 1024 * 1024;

    protected Transport transport;

    private TransactionSemapore semaphore = new VoidSemaphore();

    public PacketTransportManager(Transport transport) {
        this.transport = transport;
    }

    public PacketTransportManager(Socket client) {
        this.transport = new SocketTransport(client);
    }

    public void send(InputStream data) throws IOException {
        DataOutputStream dataOutput = new DataOutputStream(this.transport.getOutputStream());
        DataInputStream dataInput = new DataInputStream(this.transport.getInputStream());
        BufferedInputStream bufferedData = new BufferedInputStream(data);
        byte[] b = new byte[BLOCK_SIZE];
        int readedLength;
        while ((readedLength = bufferedData.read(b, 0, BLOCK_SIZE)) != -1) {
            semaphore.startTransaction();
            synchronized (semaphore.getTransactionKey()) {
                dataOutput.writeInt(readedLength);
                dataOutput.write(b, 0, readedLength);
                dataOutput.flush();
                dataInput.readInt();
            }
            semaphore.endTransaction();
        }
        semaphore.startTransaction();
        synchronized (semaphore.getTransactionKey()) {
            dataOutput.writeInt(-1);
            dataOutput.flush();
        }
        semaphore.lastTransaction();
    }

    public InputStream recieve() throws IOException {
        DataOutputStream dataOutput = new DataOutputStream(this.transport.getOutputStream());
        DataInputStream dataInput = new DataInputStream(this.transport.getInputStream());
        int transferSize = 1;
        ByteArrayOutputStream pout = new ByteArrayOutputStream();
        while (transferSize > 0 || transferSize < BLOCK_SIZE) {
            semaphore.startTransaction();
            synchronized (semaphore.getTransactionKey()) {
                transferSize = dataInput.readInt();
                if (transferSize == -1) {
                    semaphore.lastTransaction();
                    break;
                }
                byte[] b = new byte[transferSize];
                dataInput.readFully(b);
                pout.write(b);
                dataOutput.writeInt(transferSize);
            }
            semaphore.endTransaction();
        }
        return new ByteArrayInputStream(pout.toByteArray());
    }

    public void open() throws IOException {
        this.transport.open();
    }

    public void close() throws IOException {
        this.transport.close();
    }

    public PacketTransportManager setSymaphore(TransactionSemapore symaphore) {
        this.semaphore = symaphore;
        return this;
    }

    public TransactionSemapore getSymaphore() {
        return semaphore;
    }
}
