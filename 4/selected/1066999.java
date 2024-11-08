package fr.upemlv.transfile.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import fr.upemlv.transfile.reply.impl.DataReply;

/**
 * surement Pool de Threads pour charger les fichiers
 * 
 * @author aleboula
 * 
 */
public class FileReader implements Runnable {

    public static final LinkedBlockingQueue<GetRequest> requests = new LinkedBlockingQueue<GetRequest>();

    @Override
    public void run() {
        Logger.getLogger("fslogger").info("File Reader started...");
        GetRequest request;
        while (true) {
            try {
                request = (GetRequest) requests.take();
                System.out.println("ok");
                File file = new File(request.getFile());
                int fileNumber = request.getFileId();
                MappedByteBuffer bb = new FileInputStream(file).getChannel().map(MapMode.READ_ONLY, 0, file.length());
                int l = 0;
                DataReply dr;
                while (bb.hasRemaining()) {
                    if (bb.remaining() < 512) l = bb.remaining(); else l = 512;
                    byte[] data = new byte[l];
                    bb.get(data, 0, l);
                    dr = new DataReply(data, fileNumber);
                    request.getClientSession().addReply(dr);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
