package jjsplit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

/**
 * License: Public Domain
 * @author vigneshwaran
 * Email: vigneshwaran2007@gmail.com
 */
class SplitterBySize implements Runnable {

    private int noofparts;

    private long splitsize;

    private long remainingsize;

    private long filesize;

    private File inputfile;

    private JProgressBar pb;

    private JLabel status;

    private File outputfile;

    private boolean deleteOnFinish;

    public SplitterBySize(File inputfile, File outputfile, long sizeinbyte, JProgressBar pb, JLabel status, boolean deleteOnFinish) throws IOException {
        this.inputfile = inputfile;
        this.outputfile = outputfile;
        filesize = remainingsize = inputfile.length();
        this.splitsize = sizeinbyte;
        noofparts = ((filesize % splitsize == 0) ? (int) (filesize / splitsize) : (int) (filesize / splitsize) + 1);
        if (splitsize > filesize) return;
        this.pb = pb;
        this.status = status;
        this.deleteOnFinish = deleteOnFinish;
        splitter();
    }

    private void splitter() {
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        try {
            FileChannel in = new FileInputStream(inputfile).getChannel();
            long pos = 0;
            for (int i = 1; i <= noofparts; i++) {
                FileChannel out = new FileOutputStream(outputfile.getAbsolutePath() + "." + "v" + i).getChannel();
                status.setText("Rozdělovač: Rozděluji část " + i + "..");
                if (remainingsize >= splitsize) {
                    in.transferTo(pos, splitsize, out);
                    pos += splitsize;
                    remainingsize -= splitsize;
                } else {
                    in.transferTo(pos, remainingsize, out);
                }
                pb.setValue(100 * i / noofparts);
                out.close();
            }
            in.close();
            if (deleteOnFinish) new File(inputfile + "").delete();
            status.setText("Rozdělovač: Hotovo..");
            JOptionPane.showMessageDialog(null, "Rozděleno!", "Rozdělovač", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
        }
    }
}
