package ejmf.toolkit.gui.multiimage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.ImageIcon;

/**
 * A conveniece class for creating a multi-image (.mti) file.
 * <p>
 * Usage:
 * <blockquote><pre>
 * java MakeMultiImage <out-file> <frame-delay> <in-files...>
 * </pre></blockquote>
 * The frame-delay is in nanoseconds.
 *
 * @see        MultiImageComponent
 * @see        MultiImageFrame
 * @see        ShowMultiImage
 *
 * @author     Steve Talley
 */
public class MakeMultiImage {

    /**
     * The usage message when run from the command line
     */
    public static String usage = "Usage: java MakeMultiImage <out-file> <frame-delay-nanos> <in-files...>";

    /**
     * Creates a multi-image (.mit) file based on the given frame
     * delay and input files.  The input files may be of any
     * format which the JDK can use to create an Image.
     *
     * @param      args[]
     *             See the above usage
     *
     */
    public static void main(String args[]) {
        if (args.length < 3) {
            System.out.println(usage);
            System.exit(0);
        }
        long delay = Long.valueOf(args[1]).longValue();
        DataOutputStream bufferOut = null;
        DataOutputStream fileOut = null;
        try {
            ByteArrayOutputStream bout;
            bufferOut = new DataOutputStream(bout = new ByteArrayOutputStream());
            fileOut = new DataOutputStream(new FileOutputStream(new File(args[0])));
            int maxw = 0;
            int maxh = 0;
            for (int i = 2; i < args.length; i++) {
                File f = new File(args[i]);
                byte[] b = fileToByteArray(f);
                ImageIcon icon = new ImageIcon(b);
                int w = icon.getIconWidth();
                int h = icon.getIconHeight();
                if (w > maxw) maxw = w;
                if (h > maxh) maxh = h;
                bufferOut.writeLong(b.length);
                bufferOut.writeLong(delay);
                bufferOut.write(b, 0, b.length);
            }
            fileOut.writeInt(maxw);
            fileOut.writeInt(maxh);
            fileOut.writeLong((long) (delay * args.length));
            fileOut.write(bout.toByteArray());
            try {
                bout.close();
            } catch (IOException e) {
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not read/write data");
            System.exit(1);
        } finally {
            try {
                bufferOut.close();
            } catch (IOException e) {
            }
            try {
                fileOut.close();
            } catch (IOException e) {
            }
        }
        System.exit(0);
    }

    /**
     * Opens a file and loads the contents into a byte array.
     *
     * @param      f
     *             The file to open
     *
     * @return     A byte array containing the contents of the file.
     *
     * @exception  IOException
     *             If the file could not be read.
     */
    public static byte[] fileToByteArray(File f) throws IOException {
        byte[] b = new byte[(int) f.length()];
        FileInputStream is = new FileInputStream(f);
        is.read(b);
        is.close();
        return b;
    }
}
