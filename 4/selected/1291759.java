package frost;

import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;

/**
 * Downloads messages
 */
public class MessageDownloadThread extends Thread {

    private Frame frameToLock;

    public String board;

    private String downloadHtl;

    private String keypool;

    private int maxMessageDownload;

    private String destination;

    private boolean secure;

    private String publicKey;

    private boolean flagNew;

    private VerifyableMessageObject currentMsg;

    private Identity currentId;

    final String[] block = { "_boardlist", "frost_message_system" };

    public void run() {
        String tofType;
        if (flagNew) tofType = "TOF Download"; else tofType = "TOF Download Back";
        int waitTime = waitTime = (int) (Math.random() * 5000);
        mixed.wait(waitTime);
        System.out.println(tofType + " Thread started for board " + board);
        frame1.tofDownloadThreads++;
        frame1.activeTofThreads.add(board);
        UpdateIdThread uit = null;
        if (flagNew && !mixed.isElementOf(board, block)) {
            uit = new UpdateIdThread(board);
            uit.start();
            mixed.wait(5000);
        }
        String val = new StringBuffer().append(frame1.keypool).append(board).append(".key").toString();
        String state = SettingsFun.getValue(val, "state");
        if (state.equals("writeAccess") || state.equals("readAccess")) {
            publicKey = SettingsFun.getValue(val, "publicKey");
            secure = true;
        } else {
            secure = false;
        }
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        if (this.flagNew) {
            downloadDate(cal);
            if (uit != null) {
                try {
                    uit.join();
                } catch (InterruptedException ex) {
                }
            }
        } else {
            GregorianCalendar firstDate = new GregorianCalendar();
            firstDate.setTimeZone(TimeZone.getTimeZone("GMT"));
            firstDate.set(Calendar.YEAR, 2001);
            firstDate.set(Calendar.MONTH, 5);
            firstDate.set(Calendar.DATE, 11);
            int counter = 0;
            while (cal.after(firstDate) && counter < maxMessageDownload) {
                counter++;
                cal.add(Calendar.DATE, -1);
                downloadDate(cal);
            }
        }
        System.out.println(tofType + " Thread stopped for board " + board);
        frame1.activeTofThreads.removeElement(board);
        frame1.tofDownloadThreads--;
        synchronized (frame1.TOFThreads) {
            frame1.TOFThreads.removeElement(this);
        }
    }

    /**Returns true if message is duplicate*/
    private boolean exists(File file) {
        File[] fileList = (file.getParentFile()).listFiles();
        if (fileList != null) {
            for (int i = 0; i < fileList.length; i++) {
                if (!fileList[i].equals(file) && fileList[i].getName().indexOf(board) != -1 && file.getName().indexOf(board) != -1) {
                    String one = FileAccess.readFile(file);
                    String two = FileAccess.readFile(fileList[i]);
                    if (one.equals(two)) return true;
                }
            }
        }
        return false;
    }

    private void verify() {
        System.out.println("verifying...");
        if ((currentMsg.getKeyAddress() == "none") || (currentMsg.getFrom().indexOf("@") == -1)) {
            currentMsg.setStatus(VerifyableMessageObject.OLD);
        } else {
            if (frame1.getFriends().containsKey(currentMsg.getFrom())) {
                System.out.println("have this person on our list");
                currentId = frame1.getFriends().Get(currentMsg.getFrom());
                if ((currentId.getKeyAddress().compareTo(currentMsg.getKeyAddress()) == 0) && frame1.getCrypto().verify(currentMsg.getContent(), currentId.getKey())) currentMsg.setStatus(VerifyableMessageObject.VERIFIED); else currentMsg.setStatus(VerifyableMessageObject.FAILED);
            } else if (frame1.getEnemies().containsKey(currentMsg.getFrom())) currentMsg.setStatus(VerifyableMessageObject.FAILED); else {
                System.out.println("trying to add");
                try {
                    currentId = new Identity(currentMsg.getFrom(), currentMsg.getKeyAddress());
                } catch (IllegalArgumentException e) {
                    System.out.println("illegal argument exception");
                    currentMsg.setStatus(VerifyableMessageObject.NA);
                    return;
                }
                if (currentId.getKey() == Identity.NA) currentMsg.setStatus(VerifyableMessageObject.NA); else if (frame1.getCrypto().verify(currentMsg.getContent(), currentId.getKey())) {
                    currentMsg.setStatus(VerifyableMessageObject.PENDING);
                } else currentMsg.setStatus(VerifyableMessageObject.FAILED);
            }
        }
    }

    protected void downloadDate(GregorianCalendar calDL) {
        String dirdate = DateFun.getDateOfCalendar(calDL);
        String fileSeparator = System.getProperty("file.separator");
        destination = new StringBuffer().append(keypool).append(board).append(fileSeparator).append(dirdate).append(fileSeparator).toString();
        File makedir = new File(destination);
        if (!makedir.exists()) {
            System.out.println("creating directory: " + destination);
            makedir.mkdirs();
        }
        File checkLockfile = new File(destination + "locked.lck");
        int index = 0;
        int failures = 0;
        int maxFailures = 2;
        while (failures < maxFailures && (flagNew || !checkLockfile.exists())) {
            String val = new StringBuffer().append(destination).append(System.currentTimeMillis()).append(".txt.msg").toString();
            File testMe = new File(val);
            val = new StringBuffer().append(destination).append(dirdate).append("-").append(board).append("-").append(index).append(".txt").toString();
            File testMe2 = new File(val);
            if (testMe2.length() > 0) {
                index++;
                failures = 0;
            } else {
                if (secure) {
                    val = new StringBuffer().append(publicKey).append("/").append(board).append("/").append(dirdate).append("-").append(index).append(".txt").toString();
                    String downKey = val;
                    FcpRequest.getFile(downKey, "Unknown", testMe, downloadHtl, false);
                } else {
                    val = new StringBuffer().append(frame1.frostSettings.getValue("messageBase")).append("/").append(dirdate).append("-").append(board).append("-").append(index).append(".txt").toString();
                    FcpRequest.getFile("KSK@sftmeage/" + val, "Unknown", testMe, downloadHtl, false);
                    FcpRequest.getFile("KSK@frost/message/" + val, "Unknown", testMe, downloadHtl, false);
                }
                if (testMe.length() > 0) {
                    testMe.renameTo(testMe2);
                    testMe = testMe2;
                    if (!exists(testMe)) {
                        String contents = FileAccess.readFile(testMe);
                        String plaintext;
                        int encstart = contents.indexOf("==== Frost Signed+Encrypted Message ====");
                        if (encstart != -1) {
                            System.out.println("decrypting...");
                            plaintext = frame1.getCrypto().decrypt(contents.substring(encstart, contents.length()), frame1.getMyId().getPrivKey());
                            contents = contents.substring(0, encstart) + plaintext;
                            FileAccess.writeFile(contents, testMe);
                        }
                        currentMsg = new VerifyableMessageObject(testMe);
                        if (currentMsg.getSubject().trim().indexOf("ENCRYPTED MSG FOR") != -1 && currentMsg.getSubject().indexOf(frame1.getMyId().getName()) == -1) {
                            System.out.println("encrypted for someone else");
                            testMe.delete();
                            index++;
                            continue;
                        }
                        verify();
                        File sig = new File(testMe.getPath() + ".sig");
                        frame1.incSuccess(board);
                        if (!mixed.isElementOf(board, block)) {
                            if (currentMsg.isValid()) {
                                if (TOF.blocked(currentMsg) && testMe.length() > 0) {
                                    ((BoardStat) frame1.boardStats.get(currentMsg.getBoard())).incBlocked();
                                    System.out.println("\n########### blocked message #########\n");
                                } else {
                                    frame1.displayNewMessageIcon(true);
                                    String[] header = { SettingsFun.getValue(testMe, "board"), SettingsFun.getValue(testMe, "from"), SettingsFun.getValue(testMe, "subject"), SettingsFun.getValue(testMe, "date") + " " + SettingsFun.getValue(testMe, "time") };
                                    if (header.length == 4) frame1.newMessageHeader = new StringBuffer().append("   ").append(header[0]).append(" : ").append(header[1]).append(" - ").append(header[2]).append(" (").append(header[3]).append(")").toString();
                                    FileAccess.writeFile("This message is new!", testMe.getPath() + ".lck");
                                    TOF.addNewMessageToTable(testMe, board);
                                }
                            } else {
                                FileAccess.writeFile("Empty", testMe);
                            }
                        }
                    } else {
                        System.out.println("****** Duplicate Message : " + testMe.getName() + " *****");
                        FileAccess.writeFile("Empty", testMe);
                    }
                    index++;
                    failures = 0;
                } else {
                    if (!flagNew) System.out.println("***** Increased TOF index *****");
                    failures++;
                    index++;
                }
            }
        }
    }

    public MessageDownloadThread(boolean fn, String boa, String dlHtl, String kpool, String maxmsg, Frame frame) {
        super();
        this.frameToLock = frame;
        this.flagNew = fn;
        this.board = boa;
        this.downloadHtl = dlHtl;
        this.keypool = kpool;
        this.maxMessageDownload = Integer.parseInt(maxmsg);
    }
}
