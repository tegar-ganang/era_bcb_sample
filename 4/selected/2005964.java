package jmp123.player;

import jmp123.decoder.BitStream;
import jmp123.decoder.Decoder;
import jmp123.decoder.Header;
import jmp123.instream.IRandomAccess;
import jmp123.instream.BuffRandAcceFile;
import jmp123.instream.BuffRandAcceURL;
import jmp123.output.Audio;

public final class PlayingThread implements Runnable {

    private Decoder objDec123;

    private Header objHeader;

    private IRandomAccess objIRA;

    public PlayingThread(String strFileName) throws Exception {
        if (strFileName.startsWith("http://")) objIRA = new BuffRandAcceURL(strFileName); else objIRA = new BuffRandAcceFile(strFileName);
        objHeader = new Header(objIRA);
    }

    public void run() {
        int frame_count = 0;
        Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);
        try {
            if (objHeader.syncFrame() == false) return;
            objDec123 = new Decoder(new BitStream(objIRA), objHeader, Decoder.CH_BOTH);
            Audio.open(objHeader.getFrequency(), 16, objHeader.getChannels());
            while (true) {
                if (objDec123.decodeFrame() == false) break;
                if (objHeader.syncFrame() == false) break;
                if ((++frame_count & 0x7) == 0x7) objHeader.printState();
            }
            objHeader.printState();
            Audio.close();
        } catch (Exception e) {
        }
        objIRA.close();
    }
}
