package russotto.zplet;

import java.util.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import russotto.zplet.screenmodel.ZStatus;
import russotto.zplet.screenmodel.ZScreen;
import russotto.zplet.zmachine.ZMachine;
import russotto.zplet.zmachine.zmachine3.ZMachine3;
import russotto.zplet.zmachine.zmachine5.ZMachine5;
import russotto.zplet.zmachine.zmachine5.ZMachine8;

public class ZJApp extends Frame {

    ZScreen screen;

    ZStatus status_line;

    ZMachine zm;

    static String pstatusfg, pstatusbg, pmainfg, pmainbg;

    static String pzcodefile = null;

    String statusfg, statusbg, mainfg, mainbg;

    String zcodefile = null;

    boolean failed = false;

    public static synchronized void main(String argv[]) {
        int i;
        ZJApp myz;
        for (i = 0; i < argv.length; i++) {
            if (argv[i].charAt(0) == '-') {
                switch(argv[i].charAt(1)) {
                    case 'f':
                        pmainfg = pstatusbg = argv[++i];
                        break;
                    case 'b':
                        pmainbg = pstatusfg = argv[++i];
                        break;
                    default:
                        break;
                }
            } else pzcodefile = argv[i];
        }
        myz = new ZJApp(pzcodefile, pstatusfg, pstatusbg, pmainfg, pmainbg);
        System.err.println("Location = " + myz.bounds().x + " " + myz.bounds().y);
        System.err.println("Parent = " + myz.getParent());
        myz.setTitle("ZJApp");
        myz.pack();
        myz.show();
        myz.start();
    }

    ZJApp() {
        this(null, null, null, null, null);
    }

    ZJApp(String pzcodefile, String pstatusfg, String pstatusbg, String pmainfg, String pmainbg) {
        mainfg = pmainfg;
        mainbg = pmainbg;
        statusfg = pstatusfg;
        statusbg = pstatusbg;
        zcodefile = pzcodefile;
        setLayout(new BorderLayout());
        screen = new ZScreen();
        status_line = new ZStatus();
        status_line.setForeground(Color.black);
        status_line.setBackground(Color.white);
        screen.setZForeground(ZColor.Z_WHITE);
        screen.setZBackground(ZColor.Z_BLACK);
        if (statusfg != null) status_line.setForeground(ZColor.getcolor(statusfg));
        if (statusbg != null) status_line.setBackground(ZColor.getcolor(statusbg));
        if (mainfg != null) screen.setZForeground(ZColor.getcolornumber(mainfg));
        if (mainbg != null) screen.setZBackground(ZColor.getcolornumber(mainbg));
        add("North", status_line);
        add("Center", screen);
    }

    void startzm() {
        URL myzzurl;
        InputStream myzstream;
        byte zmemimage[];
        boolean joined;
        zmemimage = null;
        try {
            System.err.println(zcodefile);
            myzzurl = new URL(zcodefile);
            myzstream = myzzurl.openStream();
            zmemimage = suckstream(myzstream);
        } catch (MalformedURLException booga) {
            try {
                myzstream = new FileInputStream(zcodefile);
                zmemimage = suckstream(myzstream);
            } catch (IOException booga2) {
                add("North", new Label("Malformed URL"));
                failed = true;
            }
        } catch (IOException booga) {
            add("North", new Label("I/O Error"));
        }
        if (zmemimage != null) {
            switch(zmemimage[0]) {
                case 3:
                    zm = new ZMachine3(screen, status_line, zmemimage);
                    break;
                case 5:
                    remove(status_line);
                    zm = new ZMachine5(screen, zmemimage);
                    break;
                case 8:
                    remove(status_line);
                    zm = new ZMachine8(screen, zmemimage);
                    break;
                default:
                    add("North", new Label("Not a valid V3,V5, or V8 story file"));
            }
            if (zm != null) zm.start();
        }
        joined = false;
        if (zmemimage != null) {
            while (!joined) {
                try {
                    zm.join();
                    joined = true;
                } catch (InterruptedException booga) {
                }
            }
        }
        System.exit(0);
    }

    byte[] suckstream(InputStream mystream) throws IOException {
        byte buffer[];
        byte oldbuffer[];
        int currentbytes = 0;
        int bytesleft;
        int got;
        int buffersize = 2048;
        buffer = new byte[buffersize];
        bytesleft = buffersize;
        got = 0;
        while (got != -1) {
            bytesleft -= got;
            currentbytes += got;
            if (bytesleft == 0) {
                oldbuffer = buffer;
                buffer = new byte[buffersize + currentbytes];
                System.arraycopy(oldbuffer, 0, buffer, 0, currentbytes);
                oldbuffer = null;
                bytesleft = buffersize;
            }
            got = mystream.read(buffer, currentbytes, bytesleft);
        }
        if (buffer.length != currentbytes) {
            oldbuffer = buffer;
            buffer = new byte[currentbytes];
            System.arraycopy(oldbuffer, 0, buffer, 0, currentbytes);
        }
        return buffer;
    }

    public void start() {
        if (zcodefile == null) {
            FileDialog fd = new FileDialog(this, "Select game file", FileDialog.LOAD);
            fd.show();
            zcodefile = (new File(fd.getDirectory(), fd.getFile())).getAbsolutePath();
        }
        if (!failed && ((zm == null) || !zm.isAlive())) {
            startzm();
        }
    }

    public void destroy() {
        zm.stop();
        zm = null;
        remove(screen);
        screen = null;
    }
}
