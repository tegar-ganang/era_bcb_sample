package no.eirikb.sfs.event.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import no.eirikb.sfs.client.Client;
import no.eirikb.sfs.client.LocalShare;
import no.eirikb.sfs.client.SFSClient;
import no.eirikb.sfs.client.SFSClientListener;
import no.eirikb.sfs.event.Event;
import no.eirikb.sfs.event.server.DownloadCompleteEvent;
import no.eirikb.sfs.server.Server;
import no.eirikb.sfs.sfsserver.SFSServer;
import no.eirikb.sfs.sfsserver.SFSServerListener;
import no.eirikb.sfs.share.ShareFileReader;
import no.eirikb.sfs.share.ShareFileWriter;
import no.eirikb.sfs.share.ShareFolder;

/**
 *
 * @author eirikb
 * @author Eirik Brandtzæg <a href="mailto:eirikdb@gmail.com">eirikdb@gmail.com</a>
 */
public class TransferShareEvent extends Event {

    private Integer hash;

    private ShareFolder part;

    private int partNumber;

    public TransferShareEvent(Integer hash, ShareFolder part, int partNumber) {
        this.hash = hash;
        this.part = part;
        this.partNumber = partNumber;
    }

    public void execute(SFSServerListener listener, Server client, SFSServer server) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void execute(SFSClientListener listener, SFSClient client) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void execute(SFSClientListener listener, SFSClient client, Server server) {
        server.sendObject(new TransferShareEvent(hash, part, partNumber));
        try {
            LocalShare ls = client.getLocalShares().get(hash);
            ShareFileReader reader = new ShareFileReader(part, ls.getFile());
            byte[] buf = new byte[server.getSocket().getSendBufferSize()];
            long tot = 0;
            OutputStream out = server.getSocket().getOutputStream();
            while (tot < part.getSize()) {
                reader.read(buf);
                out.write(buf);
                out.flush();
                tot += buf.length;
                listener.sendStatus(ls, part, partNumber, tot);
            }
            listener.sendDone(ls);
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                server.getSocket().close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void execute(SFSClientListener listener, SFSClient sfsClient, Client client) {
        try {
            ShareFileWriter writer = new ShareFileWriter(part, new File(sfsClient.getShareFolder() + part.getName()));
            InputStream in = client.getSocket().getInputStream();
            LocalShare ls = sfsClient.getLocalShares().get(hash);
            byte[] buf = new byte[client.getSocket().getReceiveBufferSize()];
            int read;
            long tot = 0;
            while (tot < part.getSize()) {
                read = in.read(buf);
                writer.write(buf, read);
                tot += read;
                listener.receiveStatus(ls, part, partNumber, read);
            }
            ls.incShares();
            if (ls.getShares() == ls.getTotalShares()) {
                listener.receiveDone(ls);
                sfsClient.getClient().sendObject(new DownloadCompleteEvent(hash));
            }
        } catch (IOException ex) {
            Logger.getLogger(TransferShareEvent.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (client != null && client.getSocket() != null) {
                    client.getSocket().close();
                }
            } catch (IOException ex) {
                Logger.getLogger(TransferShareEvent.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
