package alesis.fusion.objects;

import alesis.fusion.Constant;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Vector;

/**
 * Class to represent a 'Mix' Fusion file
 * 
 * @author jam
 */
public class Mix extends FusionCommonObject {

    /** The standard header for Mix objects */
    private MixHdr header;

    private class MixHdr extends Header {

        public MixHdr() {
            buffer = ByteBuffer.allocate(length);
            setSize(0);
            setSignature(Constant.MIX_SIGNATURE);
        }
    }

    /**
     *
     */
    public Mix() {
        this("New Mix", "", "");
    }

    /**
     *
     * @param fileName
     */
    public Mix(String fileName) {
        this(fileName, "", "");
    }

    /**
     *
     * @param fileName
     * @param bankName
     */
    public Mix(String fileName, String bankName) {
        this(fileName, bankName, "");
    }

    /** Constructor
     * @param fileName
     * @param volumeName
     * @param bankName
     */
    public Mix(String fileName, String bankName, String volumeName) {
        super(Constant.MAX_ARPS);
        setFileName(fileName);
        setBankName(bankName);
        setVolumeName(volumeName);
        header = new MixHdr();
        header.setSize(0);
        trackCommonParameters = new Vector();
    }

    /**
     *
     * @param pathFile
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static Mix createFromFile(String pathFile) throws FileNotFoundException, IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        Mix mix = new Mix();
        FileInputStream fis = new FileInputStream(pathFile);
        FileChannel fc = fis.getChannel();
        mix.header.readFromFileChannel(fc);
        mix.readCommonParameters(fc);
        return mix;
    }

    @Override
    protected void parseCommonParameters() {
    }

    /**
     *
     * @param pathfile
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void writeToFile(String pathfile) throws FileNotFoundException, IOException {
        FileOutputStream fos = new FileOutputStream(pathfile);
        FileChannel fc = fos.getChannel();
        header.writeToFileChannel(fc);
        this.writeCommonParameters(fc);
        fc.close();
        fos.close();
    }
}
