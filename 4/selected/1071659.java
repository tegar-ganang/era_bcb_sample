package buttress.torrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import buttress.gui.ConsoleDialog;

public class TorrentInfo implements Serializable {

    public String announcetag;

    public String filetag;

    public String nametag;

    public String piecelengthtag;

    public String piecestag;

    public String trackerURL;

    public String[] comments;

    public long singlefilelength = 0;

    public TorrentSubFile[] fileAttribs;

    public String suggestedname;

    public long piecelength;

    public String[] piecehash;

    public long date;

    public static void main(String[] args) {
        try {
            TorrentInfo TI = new TorrentInfo();
            TI.read(new URL("http://btefnet.no-ip.org/torrents/The.O.C.S01E01.Pilot.DVDRip.XviD-TVEP.%5BBT%5D.torrent"));
            System.out.println("date is: " + TI.date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public boolean read(URL downloadUrl) {
        try {
            InputStreamReader ISR = new InputStreamReader(downloadUrl.openStream());
            if (!read(ISR)) {
                return false;
            }
            ISR.close();
        } catch (IOException e) {
            ConsoleDialog.writeError("An error occurred while trying to read " + "the torrent file", e);
            return false;
        }
        return true;
    }

    public boolean read(File file) {
        try {
            InputStreamReader ISR = new InputStreamReader(new FileInputStream(file));
            if (!read(ISR)) {
                return false;
            }
            ISR.close();
        } catch (Exception e) {
            ConsoleDialog.writeError("An error occurred while trying to read " + "the torrent file", e);
            return false;
        }
        return true;
    }

    public boolean read(InputStreamReader ISR) {
        comments = new String[0];
        if (Bencoding.readChar(ISR) != 'd') return false;
        announcetag = Bencoding.readString(ISR);
        trackerURL = Bencoding.readString(ISR);
        String maybeinfo = Bencoding.readString(ISR);
        int i = 0;
        while ((maybeinfo.equals("creation date") == false) && (i < 8)) {
            i++;
            addToComments(maybeinfo);
            char tempchar = Bencoding.readChar(ISR);
            if (tempchar == 'i') {
                maybeinfo = String.valueOf(Bencoding.readLong(ISR, tempchar));
            } else {
                maybeinfo = Bencoding.readString(ISR, tempchar);
            }
        }
        date = Bencoding.readLong(ISR);
        char[] blah = new char[6];
        try {
            ISR.read(blah);
        } catch (IOException exc) {
        }
        if (new String(blah).equals("4:info")) {
            readInfoDictionary(ISR);
        }
        Bencoding.readChar(ISR);
        return true;
    }

    public void readInfoDictionary(InputStreamReader ISR) {
        readInfoDictionary(ISR, (char) 0);
    }

    public void readInfoDictionary(InputStreamReader ISR, char firstchar) {
        if (firstchar == (char) 0) firstchar = Bencoding.readChar(ISR);
        if (firstchar != 'd') {
            System.out.println("Error firstchar != d, firstchar: " + firstchar);
            return;
        }
        filetag = Bencoding.readString(ISR);
        if (filetag.equals("files")) {
            readFilesDictionary(ISR);
        } else if (filetag.equals("length")) {
            singlefilelength = Bencoding.readLong(ISR);
            fileAttribs = null;
        } else {
            System.out.println("Unknown filetag detected: " + filetag);
            return;
        }
        nametag = Bencoding.readString(ISR);
        suggestedname = Bencoding.readString(ISR);
        piecelengthtag = Bencoding.readString(ISR);
        piecelength = Bencoding.readLong(ISR);
        piecestag = Bencoding.readString(ISR);
        readFileHash(ISR);
        Bencoding.readChar(ISR);
    }

    private void readFilesDictionary(InputStreamReader ISR) {
        fileAttribs = new TorrentSubFile[0];
        char liststart = Bencoding.readChar(ISR);
        if (liststart != 'l') {
            System.out.println("Error liststart != l, liststart: " + liststart);
            return;
        }
        char nextchar = Bencoding.readChar(ISR);
        while (nextchar != 'e') {
            TorrentSubFile toadd = new TorrentSubFile();
            toadd.read(ISR, nextchar);
            addToFileAttribs(toadd);
            nextchar = Bencoding.readChar(ISR);
        }
    }

    private void readFileHash(InputStreamReader ISR) {
        try {
            String lengthstring = "";
            int inputchar = 0;
            String inputstring = "";
            while (inputchar != ':') {
                if ((inputchar = ISR.read()) != -1) {
                    char[] charitem = new char[1];
                    charitem[0] = (char) inputchar;
                    inputstring = new String(charitem);
                    if (inputstring.equals(":") == false) lengthstring = lengthstring + inputstring;
                }
            }
            Integer tempINT = new Integer(lengthstring);
            long stringlength = tempINT.longValue();
            int numhashes = (int) stringlength / 20;
            piecehash = new String[numhashes];
            for (int i = 0; i < numhashes; i++) {
                char[] charitem = new char[20];
                for (int j = 0; j < 20; j++) {
                    charitem[j] = Bencoding.readChar(ISR);
                    ;
                }
                piecehash[i] = new String(charitem);
            }
        } catch (Exception e) {
            e.printStackTrace();
            piecehash = new String[0];
        }
    }

    private void addToComments(String toadd) {
        String[] temp = new String[comments.length + 1];
        for (int i = 0; i < comments.length; i++) temp[i] = comments[i];
        temp[temp.length - 1] = toadd;
        comments = temp;
    }

    private void addToFileAttribs(TorrentSubFile toadd) {
        TorrentSubFile[] temp = new TorrentSubFile[fileAttribs.length + 1];
        for (int i = 0; i < fileAttribs.length; i++) temp[i] = fileAttribs[i];
        temp[temp.length - 1] = toadd;
        fileAttribs = temp;
    }

    public void print() {
        System.out.println(nametag + ": " + suggestedname);
        System.out.println(announcetag + ": " + trackerURL);
        for (int i = 0; i < comments.length; i = i + 2) {
            System.out.println(comments[i] + ": " + comments[i + 1]);
        }
        if (fileAttribs == null) {
            String MBsize = Double.toString(((double) getFileSize()) / (1024 * 1024));
            if (MBsize.endsWith(".0")) MBsize = MBsize.concat("000000");
            MBsize = MBsize.substring(0, MBsize.lastIndexOf('.') + 3);
            System.out.println(filetag + ": " + singlefilelength + " bytes (" + MBsize + "MB)");
        } else {
            for (int i = 0; i < fileAttribs.length; i++) {
                fileAttribs[i].print();
            }
        }
        String segMB = Double.toString(((double) piecelength) / (1024 * 1024));
        if (segMB.indexOf(".") > 0) segMB = segMB.concat("000000");
        segMB = segMB.substring(0, segMB.lastIndexOf('.') + 3);
        System.out.println(piecehash.length + " pieces of " + piecelength + " bytes (" + segMB + "MB)");
    }

    public long getTotalSzie() {
        if (fileAttribs == null) return getFileSize();
        long totalsize = 0;
        for (int i = 0; i < fileAttribs.length; i++) {
            totalsize += fileAttribs[i].getSize();
        }
        return totalsize;
    }

    public long getFileSize() {
        long toreturn = 0;
        if (fileAttribs == null) {
            toreturn = singlefilelength;
        } else {
            for (int i = 0; i < fileAttribs.length; i++) {
                toreturn = toreturn + fileAttribs[i].size;
            }
        }
        return toreturn;
    }
}
