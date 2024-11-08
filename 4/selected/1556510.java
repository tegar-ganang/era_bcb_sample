package scanThreads;

import headFrame.SuperFrame;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import mainpackage.FileStorage;
import mainpackage.Lyricscatcher;
import filePackage.MP3FileInformation;

public class ExportThread extends ScanThread {

    String desfolder;

    int selection;

    public ExportThread(SuperFrame mframe) {
        super(mframe);
        desfolder = mframe.jTextField2.getText();
        selection = mframe.jComboBox1.getSelectedIndex();
    }

    public Void doInBackground() {
        println("started!");
        doyourthing();
        return null;
    }

    public void println(String str) {
        Lyricscatcher.mframe.jTextArea4.append("\n" + str);
        Lyricscatcher.mframe.jTextArea4.setCaretPosition(Lyricscatcher.mframe.jTextArea4.getText().lastIndexOf("\n") + 1);
        return;
    }

    public static void staticprintln(String str) {
        Lyricscatcher.mframe.jTextArea4.append("\n" + str);
        Lyricscatcher.mframe.jTextArea4.setCaretPosition(Lyricscatcher.mframe.jTextArea4.getText().lastIndexOf("\n") + 1);
        return;
    }

    public void doyourthing() {
        ArrayList<MP3FileInformation> list = FileStorage.getMP3List();
        for (int i = 0; i < list.size(); i++) {
            callInLoop();
            setbar(i, list.size());
            if (list.get(i).hasLyrics()) {
                String newdir = "";
                switch(selection) {
                    case 0:
                        newdir = desfolder + "/" + (list.get(i).getArtist()).replace("/", "").replace("\\", "");
                        break;
                    case 1:
                        newdir = desfolder + "/" + (list.get(i).getArtist().replace("/", "").replace("\\", "") + " - " + list.get(i).getTitle()).replace("/", "").replace("\\", "");
                        break;
                    case 2:
                        if (list.get(i).getAlbum() != null) {
                            newdir = desfolder + "/" + (list.get(i).getAlbum()).replace("/", "").replace("\\", "");
                        } else {
                            newdir = desfolder + "/" + (list.get(i).getArtist()).replace("/", "").replace("\\", "");
                        }
                        break;
                    case 3:
                        newdir = desfolder + "/" + (list.get(i).getArtist()).replace("/", "").replace("\\", "") + "/" + (list.get(i).getTitle()).replace("/", "").replace("\\", "");
                        break;
                    default:
                        return;
                }
                boolean success = (new File(newdir)).mkdirs();
                if (!success) {
                    publish("error making directory (might already exist)");
                }
                publish("***********************************\nCopying:" + list.get(i).getArtist() + " - " + list.get(i).getTitle());
                copyFromTo(list.get(i).getPath(), newdir + "/" + list.get(i).getLyrics().getMP3Title());
                copyFromTo(list.get(i).getLyrics().getPath(), newdir + "/" + list.get(i).getLyrics().getArtist() + " - " + list.get(i).getLyrics().getTitle() + ".txt");
                if (list.get(i).hasCover()) copyFromTo(list.get(i).getImage(), newdir + "/" + list.get(i).getLyrics().getTag("COVER"));
                if (list.get(i).hasBackground()) copyFromTo(list.get(i).getBackground(), newdir + "/" + list.get(i).getLyrics().getTag("BACKGROUND"));
                if (list.get(i).hasVideo()) copyFromTo(list.get(i).getVideo(), newdir + "/" + list.get(i).getLyrics().getTag("VIDEO"));
                publish("***********************************\n");
            }
        }
    }

    public static void export(MP3FileInformation mp3, String destination) {
        boolean success = (new File(destination)).mkdirs();
        if (!success) {
            staticprintln("error making directory (might already exist)");
        }
        staticprintln("***********************************\nCopying:" + mp3.getArtist() + " - " + mp3.getTitle());
        copyFromTo(mp3.getPath(), destination + "/" + mp3.getName());
        if (mp3.hasLyrics()) copyFromTo(mp3.getLyrics().getPath(), destination + "/" + mp3.getLyrics().getArtist() + " - " + mp3.getLyrics().getTitle() + ".txt");
        if (mp3.hasCover()) copyFromTo(mp3.getImage(), destination + "/" + mp3.getLyrics().getTag("COVER"));
        if (mp3.hasBackground()) copyFromTo(mp3.getBackground(), destination + "/" + mp3.getLyrics().getTag("BACKGROUND"));
        if (mp3.hasVideo()) copyFromTo(mp3.getVideo(), destination + "/" + mp3.getLyrics().getTag("VIDEO"));
        staticprintln("***********************************\n");
    }

    protected void done() {
        setbar(1, 0);
    }

    public static void copyFromTo(String src, String des) {
        staticprintln("Copying:\"" + src + "\"\nto:\"" + des + "\"");
        try {
            FileChannel srcChannel = new FileInputStream(src).getChannel();
            FileChannel dstChannel = new FileOutputStream(des).getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            srcChannel.close();
            dstChannel.close();
        } catch (IOException e) {
        }
    }
}
