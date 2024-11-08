import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Random;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.tritonus.lowlevel.ogg.StreamState;
import org.tritonus.lowlevel.ogg.Packet;
import org.tritonus.lowlevel.ogg.Page;
import org.tritonus.lowlevel.vorbis.Block;
import org.tritonus.lowlevel.vorbis.Comment;
import org.tritonus.lowlevel.vorbis.DspState;
import org.tritonus.lowlevel.vorbis.Info;

public class VorbisEncoder {

    private static final int READ = 1024;

    private static byte[] readbuffer = new byte[READ * 4 + 44];

    public static void main(String[] args) throws IOException {
        StreamState os = new StreamState();
        Page og = new Page();
        Packet op = new Packet();
        Info vi = new Info();
        Comment vc = new Comment();
        DspState vd = new DspState();
        Block vb = new Block();
        boolean eos = false;
        int i;
        int founddata;
        AudioInputStream ais = null;
        try {
            ais = AudioSystem.getAudioInputStream(new File(args[0]));
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        }
        AudioFormat format = ais.getFormat();
        if (format.getChannels() != 2 || format.getSampleSizeInBits() != 16) {
            System.out.println("need 16 bit stereo!");
            System.exit(1);
        }
        File outputFile = new File(args[1]);
        OutputStream output = new FileOutputStream(outputFile);
        vi.init();
        vi.encodeInitVBR(format.getChannels(), (int) format.getSampleRate(), 0.1F);
        vc.init();
        vc.addTag("ENCODER", "encoder_example.c");
        vd.initAnalysis(vi);
        vb.init(vd);
        Random random = new Random(System.currentTimeMillis());
        os.init(random.nextInt());
        Packet header = new Packet();
        Packet header_comm = new Packet();
        Packet header_code = new Packet();
        vd.headerOut(vc, header, header_comm, header_code);
        os.packetIn(header);
        os.packetIn(header_comm);
        os.packetIn(header_code);
        while (!eos) {
            int result = os.flush(og);
            if (result == 0) break;
            output.write(og.getHeader());
            output.write(og.getBody());
        }
        while (!eos) {
            int bytes = ais.read(readbuffer, 0, READ * 4);
            if (bytes == 0 || bytes == -1) {
                vd.write(null, 0);
            } else {
                float[][] buffer = new float[format.getChannels()][READ];
                for (i = 0; i < bytes / 4; i++) {
                    int nSample;
                    float fSample;
                    nSample = (readbuffer[i * 4 + 1] << 8) | (0x00ff & readbuffer[i * 4 + 0]);
                    fSample = nSample / 32768.0F;
                    buffer[0][i] = fSample;
                    nSample = (readbuffer[i * 4 + 3] << 8) | (0x00ff & readbuffer[i * 4 + 2]);
                    fSample = nSample / 32768.f;
                    buffer[1][i] = fSample;
                }
                vd.write(buffer, bytes / 4);
            }
            while (vd.blockOut(vb) == 1) {
                vb.analysis(null);
                vb.addBlock();
                while (vd.flushPacket(op) != 0) {
                    os.packetIn(op);
                    while (!eos) {
                        int result = os.pageOut(og);
                        if (result == 0) break;
                        output.write(og.getHeader());
                        output.write(og.getBody());
                        if (og.isEos()) {
                            eos = true;
                        }
                    }
                }
            }
        }
        os.clear();
        vb.clear();
        vd.clear();
        vc.clear();
        vi.clear();
        output.close();
    }
}
