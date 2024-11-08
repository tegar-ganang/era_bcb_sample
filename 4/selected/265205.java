package jjsplit;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

/**
 *
 * @author vigneshwaran
 */
class Joiner implements Runnable {

    private File originalfilename;

    private File outputfile;

    private int noofparts;

    private long splitsize;

    private JProgressBar pb;

    private JLabel status;

    private boolean deleteOnFinish;

    public Joiner(File inputfile, File outputfile, JProgressBar pb, JLabel status, boolean deleteOnFinish) {
        this.splitsize = inputfile.length();
        this.pb = pb;
        this.originalfilename = new File(inputfile.getAbsolutePath().substring(0, inputfile.getAbsolutePath().length() - 3));
        this.outputfile = new File(outputfile.getAbsolutePath().substring(0, outputfile.getAbsolutePath().length() - 3));
        this.status = status;
        this.deleteOnFinish = deleteOnFinish;
        getParts();
        joiner();
    }

    private void getParts() {
        int i;
        File tempfile;
        for (i = 0; ; i++) {
            tempfile = new File(originalfilename + ".v" + (i + 1));
            if (!tempfile.exists()) break;
        }
        noofparts = i;
        status.setText("Slučovač: Nalezený počet částí - " + i);
    }

    private void joiner() {
        Thread t = new Thread(this);
        t.start();
    }

    @Override
    public void run() {
        try {
            FileChannel out = new FileOutputStream(outputfile).getChannel();
            long pos = 0;
            status.setText("Slučovač: Proces Slučování spuštěn.. Prosím čekejte..");
            for (int i = 1; i <= noofparts; i++) {
                FileChannel in = new FileInputStream(originalfilename.getAbsolutePath() + "." + "v" + i).getChannel();
                status.setText("Slučovač: Slučuji část " + i + "..");
                this.splitsize = in.size();
                out.transferFrom(in, pos, splitsize);
                pos += splitsize;
                in.close();
                if (deleteOnFinish) new File(originalfilename + ".v" + i).delete();
                pb.setValue(100 * i / noofparts);
            }
            out.close();
            status.setText("Slučovač: Hotovo..");
            JOptionPane.showMessageDialog(null, "Sloučeno!", "Slučovač", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
        }
    }

    public static String getSize(long bytes) {
        if (bytes > 1048576) {
            double div = bytes / 1048576;
            return div + "MB";
        } else if (bytes > 1024) {
            double div = bytes / 1024;
            return div + "KB";
        } else return bytes + "bajtů";
    }

    public static void main(String args[]) {
        File inputfile, outputfile;
        boolean delete = false;
        if (args.length < 3) {
            JOptionPane.showMessageDialog(null, "Neplatný počet argumentů", "Chyba", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (args[2].equals("1")) delete = true;
        inputfile = new File(args[0]);
        outputfile = new File(args[1]);
        JFrame miniframe = new JFrame("MiniJJSplit :P");
        miniframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        miniframe.setLocationRelativeTo(null);
        JLabel heading = new JLabel("JJSplit MiniSlučovač :P");
        miniframe.setLayout(new BorderLayout());
        miniframe.add(heading, BorderLayout.NORTH);
        JProgressBar jp = new JProgressBar(0, 100);
        jp.setStringPainted(true);
        miniframe.add(jp, BorderLayout.CENTER);
        JLabel label = new JLabel();
        miniframe.add(label, BorderLayout.SOUTH);
        miniframe.setSize(200, 80);
        miniframe.setVisible(true);
        new Joiner(inputfile, outputfile, jp, label, delete);
    }
}
