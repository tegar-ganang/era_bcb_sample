package org.privale.node;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import org.privale.clients.EncodeFailedException;
import org.privale.clients.EncodePacketGenFailedException;
import org.privale.clients.UserClient;
import org.privale.utils.*;

public class DownloadLoop extends StandardLoop implements Serializable {

    private static final long serialVersionUID = 1L;

    protected PiggybackData PigData;

    protected PiggybackData ReturnPigData;

    protected File PiggybackRouteFile;

    protected File PostData;

    private boolean pigdatadisp;

    public DownloadLoop() {
    }

    protected DownloadLoop(NodeAbstract n, RemoteDownloadConfig config) {
        super(n);
        Config = config;
        PigData = new PiggybackData(getNode());
        ReturnPigData = new PiggybackData(getNode());
        pigdatadisp = false;
        PostData = config.PostData;
    }

    public void Init(NodeAbstract n) {
        super.Init(n);
        PigData.Init(n);
        ReturnPigData.Init(n);
    }

    protected void ReadDownloadFile() throws IOException, BadPiggybackFileException {
        RemoteDownloadConfig conf = (RemoteDownloadConfig) Config;
        ChannelReader cr = new ChannelReader(conf.DownloadFile);
        PiggybackRouteFile = cr.getLongFile(getNode().getTempFM());
        Config.DepotClient = cr.getLong();
        Config.CryptoClient = cr.getLong();
        PigData.ReadPiggybackFile(cr, Config.CryptoClient);
        cr.close();
    }

    protected void BuildRoute() throws RouteBuildFailedException {
        super.BuildRoute();
        DownRoute.MainRoute.UserConfig.AllowedCryptoClients = new LinkedList<Long>();
        DownRoute.MainRoute.UserConfig.AllowedCryptoClients.add(Config.CryptoClient);
        DownRoute.MainRoute.UserConfig.AllowedDepotClients = new LinkedList<Long>();
        DownRoute.MainRoute.UserConfig.AllowedDepotClients.add(Config.DepotClient);
        try {
            UpRoute.MainRoute.EndNodes = getNode().getCodec().getFirstNodes(PiggybackRouteFile);
            LinkedList<Long> l = UpRoute.MainRoute.EndNodes.CryptoMatch(null, UpRoute.MainRoute.UserConfig.AllowedCryptoClients);
            int idx = (int) ((float) l.size() * getNode().getRandom().nextFloat());
            UpRoute.MainRoute.EndNodes.EncodeClient = l.get(idx);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RouteBuildFailedException("Could not get the first nodes of the piggyback route!");
        }
        UpRoute.MainRoute.ForwardToEndNodes = true;
        DownRoute.BuildRoute();
        UpRoute.BuildRoute();
    }

    protected void DispatchEncodePacketGen() throws EncodePacketGenFailedException {
        super.DispatchEncodePacketGen();
        PigData.DispatchEncodePacketGen();
    }

    protected boolean isEncodeGenDone() {
        boolean done = super.isEncodeGenDone();
        if (done) {
            done = PigData.isEncodeGenDone();
        }
        if (done) {
            if (!pigdatadisp) {
                pigdatadisp = true;
                for (int cnt = 0; cnt < Config.NumberPiggybackEncodes; cnt++) {
                    PiggybackEncodePackets.get(cnt).getDataLayer(ReturnPigData.Layers);
                }
                try {
                    ReturnPigData.DispatchEncodePacketGen();
                } catch (EncodePacketGenFailedException e) {
                    e.printStackTrace();
                }
            }
            done = ReturnPigData.isEncodeGenDone();
        }
        return done;
    }

    protected void EncodeRoute() throws EncodeFailedException, IOException {
        File storefile = getNode().getCodec().PackStore(null, ReturnStore);
        DownRoute.EncodeData(storefile);
        File data = getNode().getCodec().PackRemoteDownload(this);
        data = PigData.EncodeData(data);
        FileOutputStream fos = new FileOutputStream(PiggybackRouteFile, true);
        FileChannel foc = fos.getChannel();
        FileInputStream fis = new FileInputStream(data);
        FileChannel fic = fis.getChannel();
        fic.transferTo(0, fic.size(), foc);
        foc.close();
        fic.close();
        UpRoute.EncodeData(PiggybackRouteFile);
    }

    protected File ProcessStore(StorePair pair) throws IOException, DataDecodeFailedException, EncodeFailedException {
        File returnfile = null;
        returnfile = super.ProcessStore(pair);
        if (returnfile != null) {
            returnfile = DecodeRaw(returnfile);
            ChannelReader cr = new ChannelReader(returnfile);
            returnfile = cr.getLongFile(getNode().getTempFM());
            cr.close();
            UserClient c = getNode().getUserClientByID(Config.RequestingClient);
            if (c != null) {
                if (returnfile.length() > 0) {
                    c.DownloadComplete((RemoteDownloadConfig) Config, returnfile);
                } else {
                    c.DownloadFailed((RemoteDownloadConfig) Config, "Failure returned from the remote node!");
                }
            }
        }
        return returnfile;
    }

    protected void Failed(String msg) {
        UserClient c = getNode().getUserClientByID(Config.RequestingClient);
        if (c != null) {
            c.DownloadFailed((RemoteDownloadConfig) Config, msg);
        }
    }
}
