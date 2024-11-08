package espider.network.filetransfert.upload;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import espider.contact.ContactList;
import espider.contact.NoContactException;
import espider.libs.file.SpiderFile;
import espider.network.message.InitTransfertMessage;
import espider.utils.Utils;

public class Uploader extends Thread {

    private final SpiderFile fileToUpload;

    private final ByteBuffer buffer;

    private int nbByteSendPerSecond;

    private FileChannel fileChannel;

    private SocketChannel socketChannel;

    private final int port;

    private final int packetSize;

    private final InitTransfertMessage message;

    private int totalByteWritten = 0;

    private volatile boolean alive;

    /**
	 * 
	 * @param message
	 * @param fileToUpload
	 */
    public Uploader(InitTransfertMessage message, SpiderFile fileToUpload) {
        this.port = message.getUploadPort();
        this.fileToUpload = fileToUpload;
        this.packetSize = message.getPacketSize();
        this.message = message;
        buffer = ByteBuffer.allocate(packetSize);
        nbByteSendPerSecond = 1024;
    }

    /**
	 * Throttled Writer
	 */
    public void run() {
        long indexPart;
        int nbByteSendInSecond = 0;
        int nbByteRead = 0;
        int nbByteWritten = 0;
        int bufferLimit = 0;
        long startUpload = 0;
        long nbPart = fileToUpload.getSize() / packetSize;
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(true);
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            fileChannel = new FileInputStream(fileToUpload.getPath()).getChannel();
            socketChannel = serverSocketChannel.accept();
            alive = true;
            while ((indexPart = getIndexPart()) > -1 && alive) {
                System.out.println("\n-------------- New Part " + indexPart + " ----------------");
                nbByteRead = readPart(indexPart);
                System.out.println("nb byte read : " + nbByteRead);
                bufferLimit = 0;
                while (buffer.position() < nbByteRead) {
                    bufferLimit = bufferLimit + (nbByteSendPerSecond - nbByteSendInSecond);
                    if (bufferLimit > nbByteRead) bufferLimit = nbByteRead;
                    System.out.println("limit buffer : " + bufferLimit);
                    buffer.limit(bufferLimit);
                    if (startUpload == 0) startUpload = System.currentTimeMillis();
                    while (buffer.hasRemaining()) {
                        nbByteWritten = socketChannel.write(buffer);
                        nbByteSendInSecond += nbByteWritten;
                        totalByteWritten += nbByteWritten;
                        System.out.println("byte envoye dans la seconde : " + nbByteSendInSecond + "\ntotal : " + totalByteWritten);
                    }
                    if (nbByteSendInSecond >= nbByteSendPerSecond) {
                        long time = System.currentTimeMillis() - startUpload;
                        long sleep = 1000 - time;
                        if (sleep > 0) {
                            try {
                                Thread.sleep(sleep);
                                System.out.println("Je viens de dormir : " + sleep + " ms");
                            } catch (InterruptedException ie) {
                                ie.printStackTrace();
                            }
                        }
                        if (time > 0) nbByteSendPerSecond = (int) ((nbByteSendInSecond * 1000) / time); else nbByteSendPerSecond *= 10;
                        System.out.println("nouveau taux : " + nbByteSendPerSecond);
                        startUpload = 0;
                        nbByteSendInSecond = 0;
                    }
                }
            }
            socketChannel.close();
            serverSocketChannel.close();
            System.out.println("FIN du transfert!!");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int readPart(long indexPart) throws IOException {
        buffer.clear();
        System.out.println("position : " + (indexPart * packetSize));
        fileChannel.position(indexPart * packetSize);
        int nbByte = fileChannel.read(buffer);
        buffer.position(0);
        return nbByte;
    }

    private long getIndexPart() throws IOException {
        buffer.clear();
        socketChannel.read(buffer);
        buffer.flip();
        return buffer.getLong();
    }

    public String[] getSummary() {
        String[] summary = new String[5];
        summary[0] = message.getFilename();
        summary[1] = Utils.formatNumer((totalByteWritten * 100) / (double) fileToUpload.getSize()).concat(" %");
        summary[2] = Utils.formatNumer(fileToUpload.getSizeMo()).concat(" Mo");
        summary[3] = Utils.formatNumer(((double) fileToUpload.getSize() - totalByteWritten) / (1024 * 1024)).concat(" Mo");
        try {
            summary[4] = ContactList.getContact(message.getIdSender()).getName();
        } catch (NoContactException nce) {
            summary[4] = "";
        }
        return summary;
    }

    public void stopUpload() {
        alive = false;
    }
}
