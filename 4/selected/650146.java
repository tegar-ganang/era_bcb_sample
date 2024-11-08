package net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers;

import net.kano.joustsim.oscar.oscar.service.icbm.ft.ChecksummerImpl;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.OutgoingFileTransfer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.RvConnection;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.Checksummer;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.ChecksummingEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.ComputedChecksumsInfo;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChecksumController extends AbstractStateController {

    public void start(RvConnection transfer, StateController last) {
        try {
            Map<TransferredFile, Long> checksums = new HashMap<TransferredFile, Long>();
            if (transfer instanceof OutgoingFileTransfer) {
                OutgoingFileTransfer otransfer = (OutgoingFileTransfer) transfer;
                List<TransferredFile> files = otransfer.getFiles();
                for (TransferredFile tfile : files) {
                    File file = tfile.getRealFile();
                    RandomAccessFile raf = new RandomAccessFile(file, "r");
                    Checksummer summer = new ChecksummerImpl(raf.getChannel(), raf.length());
                    otransfer.getEventPost().fireEvent(new ChecksummingEvent(tfile, summer));
                    checksums.put(tfile, summer.compute());
                }
            }
            fireSucceeded(new ComputedChecksumsInfo(checksums));
        } catch (IOException e) {
            fireFailed(e);
        }
    }

    public void stop() {
    }
}
