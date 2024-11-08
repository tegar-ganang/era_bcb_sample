package jmp123.decoder;

import jmp123.output.Audio;

public final class Decoder {

    public static final int CH_BOTH = 0;

    public static final int CH_LEFT = 1;

    public static final int CH_RIGHT = 2;

    private Synthesis objSynt;

    private ILayer123 layer123;

    public Decoder(BitStream objBS, Header objFrameHeader, int wch) {
        objSynt = new Synthesis(objFrameHeader.getChannels());
        switch(objFrameHeader.getLayer()) {
            case 1:
                layer123 = new Layer1(objBS, objFrameHeader, objSynt, wch);
                break;
            case 2:
                layer123 = new Layer2(objBS, objFrameHeader, objSynt, wch);
                break;
            case 3:
                layer123 = new Layer3(objBS, objFrameHeader, objSynt, wch);
                break;
        }
    }

    public boolean decodeFrame() throws Exception {
        int iLen;
        objSynt.reset();
        layer123.decodeFrame();
        iLen = objSynt.getSize();
        Audio.write(Synthesis.bytePCMBuf, iLen);
        return (iLen == Synthesis.PCM_LENGTH);
    }
}
