package icat3wstestdata;

import client.ICAT;
import icat3wstest.Constants;
import icat3wstest.ICATSingleton;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.SwingWorker;

/**
 *
 * @author gjd37
 */
public class DownloadDatasetSwingworker {

    /** Creates a new instance of DownloadDatafile */
    public DownloadDatasetSwingworker() {
    }

    static final ICAT icat = ICATSingleton.getInstance();

    static java.lang.String sessionId = Constants.SID;

    static java.lang.Long datafileId = 106L;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        final int[] finished = { 0 };
        try {
            Collection<SwingWorker> sws = new ArrayList<SwingWorker>();
            for (int i = 0; i < 20; i++) {
                SwingWorker sw = new SwingWorker<String, String>() {

                    int total = 0;

                    long timeTotal = 0;

                    float totalTime = 0;

                    @Override
                    public String doInBackground() {
                        long time = System.currentTimeMillis();
                        try {
                            String result = icat.downloadDataset(sessionId, datafileId);
                            java.lang.System.out.println("Result = " + result);
                            totalTime = (System.currentTimeMillis() - time) / 1000f;
                            System.out.println("\nTime taken to get URL back: " + totalTime + " seconds");
                            time = System.currentTimeMillis();
                            URL url = new URL(result);
                            InputStream is = null;
                            DataInputStream dis;
                            int s;
                            try {
                                is = url.openStream();
                                byte[] buff = new byte[4 * 1024];
                                while ((s = is.read(buff)) != -1) {
                                    total += s;
                                }
                                totalTime = (System.currentTimeMillis() - time) / 1000f;
                                System.out.println("\nTime taken to download: " + totalTime + " seconds");
                            } finally {
                                try {
                                    if (is != null) {
                                        is.close();
                                    }
                                } catch (IOException ioe) {
                                }
                            }
                        } catch (Exception ex) {
                            System.out.println("Error with: " + ex);
                            ex.printStackTrace();
                        }
                        return "finished";
                    }

                    @Override
                    public void done() {
                        try {
                            float totalTime = (System.currentTimeMillis() - timeTotal) / 1000f;
                            System.out.println("finished ok " + finished[0]++ + " : total " + total / 1024f / 1024f + " Mb, Total time: " + totalTime + " seconds");
                        } catch (Exception ex) {
                            finished[0]++;
                            System.out.println(ex.getMessage());
                        }
                    }
                };
                sws.add(sw);
            }
            int i = 0;
            for (SwingWorker swingWorker : sws) {
                System.out.println("Starting" + i++);
                swingWorker.execute();
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }
}
