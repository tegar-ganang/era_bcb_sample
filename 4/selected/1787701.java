package ISS.Technology.JDBC.LoadTest;

import ISS.CodingStandards.*;
import ISS.Technology.JDBC.*;
import ISS.Authentication.Database.*;
import ISS.Util.*;
import java.util.*;
import java.sql.*;

class measurer implements Runnable {

    private LoadTest lt = null;

    public Thread mine;

    public measurer(LoadTest lt) {
        this.lt = lt;
    }

    public void run() {
        java.util.Date now = null;
        java.util.Date then = new java.util.Date();
        try {
            long allreads = 0;
            long allwrites = 0;
            double alldiff = 0;
            while (true) {
                long reads;
                long writes;
                synchronized (lt) {
                    lt.readcount = 0;
                    lt.writecount = 0;
                }
                mine.sleep(10000);
                synchronized (lt) {
                    reads = lt.readcount;
                    writes = lt.writecount;
                }
                now = new java.util.Date();
                allreads += reads;
                allwrites += writes;
                double diff = (double) (now.getTime() - then.getTime());
                alldiff += diff;
                System.out.println("Res:" + (reads / diff) * 1000 + " reads/sec, " + (writes / diff) * 1000 + " writes/sec TOTALS:" + (allreads / alldiff) * 1000 + "r/s " + (allwrites / alldiff) * 1000 + "w/s");
                then = new java.util.Date();
            }
        } catch (Exception huh) {
            Code.fail(huh.toString());
        }
    }
}

public class LoadTest implements Startable, Runnable {

    Hashtable ht = null;

    String acct = null;

    long readcount = 0;

    long writecount = 0;

    int reads = 0;

    int writes = 0;

    public void startupInit(Object arg) throws Exception {
        ht = (Hashtable) arg;
        acct = ht.get("Account").toString();
        reads = ((Number) ht.get("reads")).intValue();
        writes = ((Number) ht.get("writes")).intValue();
        if (writes > reads) Code.fail("wrtes>reads: Each write needs a read!");
        int threads = ((Number) ht.get("threads")).intValue();
        for (int i = 0; i < threads; i++) {
            Thread thr = new Thread(this, "LoadTest" + i);
            thr.start();
        }
        measurer meas = new measurer(this);
        Thread thr = new Thread(meas, "Measurer");
        meas.mine = thr;
        thr.start();
        thr.join();
    }

    public void run() {
        try {
            DataBase db = new DataBase(ht.get("DbDriver").toString(), ht.get("DbUrl").toString());
            AuthInfo table = new AuthInfo(db);
            while (true) {
                for (int i = (reads > writes ? reads : writes); i > 0; i--) {
                    Row row = null;
                    if (i <= reads) {
                        if (i <= writes) {
                            row = table.getRow(acct);
                            int c = ((Number) row.get(1)).intValue();
                            c++;
                            row.set(1, new Integer(c));
                            row.update();
                            synchronized (this) {
                                readcount++;
                                writecount++;
                            }
                        } else {
                            row = table.getRow(acct);
                            synchronized (this) {
                                readcount++;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignore) {
            Code.fail(ignore.toString());
        }
    }
}
