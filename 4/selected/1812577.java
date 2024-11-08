package entagged.audioformats.mpc.util;

import entagged.audioformats.EncodingInfo;
import entagged.audioformats.exceptions.*;
import java.io.*;

public class MpcInfoReader {

    public EncodingInfo read(RandomAccessFile raf) throws CannotReadException, IOException {
        EncodingInfo info = new EncodingInfo();
        if (raf.length() == 0) {
            System.err.println("Error: File empty");
            throw new CannotReadException("File is empty");
        }
        raf.seek(0);
        byte[] b = new byte[3];
        raf.read(b);
        String mpc = new String(b);
        if (!mpc.equals("MP+") && mpc.equals("ID3")) {
            raf.seek(6);
            int tagSize = read_syncsafe_integer(raf);
            raf.seek(tagSize + 10);
            b = new byte[3];
            raf.read(b);
            mpc = new String(b);
            if (!mpc.equals("MP+")) {
                throw new CannotReadException("MP+ Header not found");
            }
        } else if (!mpc.equals("MP+")) {
            throw new CannotReadException("MP+ Header not found");
        }
        b = new byte[25];
        raf.read(b);
        MpcHeader mpcH = new MpcHeader(b);
        double pcm = mpcH.getSamplesNumber();
        info.setPreciseLength((float) (pcm * 1152 / mpcH.getSamplingRate()));
        info.setChannelNumber(mpcH.getChannelNumber());
        info.setSamplingRate(mpcH.getSamplingRate());
        info.setEncodingType(mpcH.getEncodingType());
        info.setExtraEncodingInfos(mpcH.getEncoderInfo());
        info.setBitrate(computeBitrate(info.getLength(), raf.length()));
        return info;
    }

    private int read_syncsafe_integer(RandomAccessFile raf) throws IOException {
        int value = 0;
        value += (raf.read() & 0xFF) << 21;
        value += (raf.read() & 0xFF) << 14;
        value += (raf.read() & 0xFF) << 7;
        value += raf.read() & 0xFF;
        return value;
    }

    private int computeBitrate(int length, long size) {
        return (int) ((size / 1000) * 8 / length);
    }
}
