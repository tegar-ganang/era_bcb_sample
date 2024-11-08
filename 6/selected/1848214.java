package org.apache.commons.net.ftp;

import java.io.IOException;
import java.net.SocketException;
import java.util.Calendar;
import java.util.Comparator;
import java.util.TreeSet;
import junit.framework.TestCase;

public class FTPClientConfigFunctionalTest extends TestCase {

    private FTPClient FTP = new FTPClient();

    private FTPClientConfig FTPConf;

    /**
     * 
     */
    public FTPClientConfigFunctionalTest() {
        super();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        FTPConf = new FTPClientConfig(FTPClientConfig.SYST_UNIX);
        FTPConf.setServerTimeZoneId("GMT");
        FTP.configure(FTPConf);
        try {
            FTP.connect("tgftp.nws.noaa.gov");
            FTP.login("anonymous", "testing@apache.org");
            FTP.changeWorkingDirectory("SL.us008001/DF.an/DC.sflnd/DS.metar");
            FTP.enterLocalPassiveMode();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        FTP.disconnect();
        super.tearDown();
    }

    /**
     * @param arg0
     */
    public FTPClientConfigFunctionalTest(String arg0) {
        super(arg0);
    }

    private TreeSet<FTPFile> getSortedList(FTPFile[] files) {
        TreeSet<FTPFile> sorted = new TreeSet<FTPFile>(new Comparator<Object>() {

            public int compare(Object o1, Object o2) {
                FTPFile f1 = (FTPFile) o1;
                FTPFile f2 = (FTPFile) o2;
                return f1.getTimestamp().getTime().compareTo(f2.getTimestamp().getTime());
            }
        });
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().startsWith("sn")) {
                sorted.add(files[i]);
            }
        }
        return sorted;
    }

    public void testTimeZoneFunctionality() throws Exception {
        java.util.Date now = new java.util.Date();
        FTPFile[] files = FTP.listFiles();
        TreeSet<FTPFile> sorted = getSortedList(files);
        FTPFile lastfile = null;
        FTPFile firstfile = null;
        for (FTPFile thisfile : sorted) {
            if (firstfile == null) {
                firstfile = thisfile;
            }
            if (lastfile != null) {
                assertTrue(lastfile.getTimestamp().before(thisfile.getTimestamp()));
            }
            lastfile = thisfile;
        }
        assertTrue(lastfile.getTimestamp().getTime().before(now));
        Calendar first = firstfile.getTimestamp();
        first.add(Calendar.DATE, 2);
        assertTrue(lastfile.getTimestamp().before(first));
    }
}
