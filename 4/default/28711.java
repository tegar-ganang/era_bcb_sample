import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmEncodeParam;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.DcmParser;
import org.dcm4che.data.DcmParserFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.image.PixelDataReader;
import org.dcm4che.image.PixelDataFactory;
import org.dcm4che.image.PixelDataWriter;

/**
 * @author jforaci
 */
public class PixelDataTest {

    private static final PixelDataFactory pdFact = PixelDataFactory.getInstance();

    private static void readAndRewrite(File inFile, File outFile) throws IOException {
        ImageInputStream iis = ImageIO.createImageInputStream(new BufferedInputStream(new FileInputStream(inFile)));
        DcmParser dcmParser = DcmParserFactory.getInstance().newDcmParser(iis);
        Dataset ds = DcmObjectFactory.getInstance().newDataset();
        dcmParser.setDcmHandler(ds.getDcmHandler());
        dcmParser.parseDcmFile(null, Tags.PixelData);
        PixelDataReader pdReader = pdFact.newReader(ds, iis, dcmParser.getDcmDecodeParam().byteOrder, dcmParser.getReadVR());
        System.out.println("reading " + inFile + "...");
        pdReader.readPixelData(false);
        ImageOutputStream out = ImageIO.createImageOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));
        DcmEncodeParam dcmEncParam = DcmEncodeParam.IVR_LE;
        ds.writeDataset(out, dcmEncParam);
        ds.writeHeader(out, dcmEncParam, Tags.PixelData, dcmParser.getReadVR(), dcmParser.getReadLength());
        System.out.println("writing " + outFile + "...");
        PixelDataWriter pdWriter = pdFact.newWriter(pdReader.getPixelDataArray(), false, ds, out, dcmParser.getDcmDecodeParam().byteOrder, dcmParser.getReadVR());
        pdWriter.writePixelData();
        out.flush();
        out.close();
        System.out.println("done!");
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("PixelDataTest: Error: Too few parameters");
            System.out.println("Usage: PixelDataTest <dicom-file>");
            System.exit(1);
        }
        for (int i = 0; i < args.length; i++) {
            try {
                File inFile = new File(args[i]), outFile = new File(inFile.getAbsolutePath() + ".TEST");
                readAndRewrite(inFile, outFile);
            } catch (IOException ioe) {
                System.err.println("FAILED: " + ioe);
            }
        }
    }
}
