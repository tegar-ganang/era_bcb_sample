package musicbox.backend;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.TimerTask;
import java.util.Vector;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.table.TableRowSorter;
import javazoom.jl.decoder.Bitstream;
import musicbox.backend.comparators.AlbumStringComparator;
import musicbox.backend.comparators.ArtistStringComparator;
import musicbox.backend.comparators.TrackNumberStringComparator;
import musicbox.gui.CustomTableModel;
import musicbox.gui.MusicBoxView;
import noTalent.MusicOutputDesign;
import noTalent.SocketObject;

/**
 *
 * @author Isaac Hammon
 */
public class SocketListener {

    private Socket socket;

    private MusicBoxView mbv;

    private java.util.Timer rec;

    private String pool = "";

    private Vector<byte[]> bufferList;

    private ByteArrayInputStream bais;

    private boolean recievingLibraryData;

    private int songSize = 3290857;

    private int songSizeRead;

    private ByteArrayOutputStream baos;

    private PipedInputStream pis;

    private PipedOutputStream pos;

    private ObjectOutputStream oos;

    private ObjectInputStream ois;

    private boolean playing = false;

    private SocketObject objectToPlay;

    public SocketListener(final Socket socket, MusicBoxView mbv) {
        this.socket = socket;
        this.mbv = mbv;
        try {
            this.socket.setSoTimeout(5000);
        } catch (SocketException ex) {
            mbv.showErrorDialog(ex);
        }
        rec = new java.util.Timer();
        bufferList = new Vector<byte[]>();
        rec.schedule(new recMessage(), 1000);
        recievingLibraryData = false;
        new Thread() {

            public void run() {
                try {
                    System.out.println("initializing input stream");
                    ois = new ObjectInputStream(socket.getInputStream()) {

                        @Override
                        protected void readStreamHeader() throws IOException, StreamCorruptedException {
                        }

                        @Override
                        protected Object readObjectOverride() throws IOException, ClassNotFoundException {
                            Object o = readObject();
                            System.out.println("Object class: " + o.getClass().getCanonicalName());
                            return o;
                        }
                    };
                    System.out.println("initialized input stream");
                } catch (IOException ex) {
                    MusicBoxView.showErrorDialog(ex);
                }
            }
        }.start();
        new Thread() {

            public void run() {
                try {
                    oos = new ObjectOutputStream(socket.getOutputStream()) {

                        @Override
                        protected void writeStreamHeader() throws IOException {
                        }
                    };
                    oos.flush();
                    SocketObject so = new SocketObject("GetAllAddresses");
                    System.out.println("MessageType: " + so.getMessageType());
                    oos.writeObject(so);
                    oos.flush();
                } catch (IOException ex) {
                    MusicBoxView.showErrorDialog(ex);
                }
            }
        }.start();
    }

    public ObjectOutputStream getObjectOutputStream() {
        return oos;
    }

    class recMessage extends TimerTask {

        public void run() {
            while (socket.isConnected()) {
                try {
                    if (ois == null) {
                        System.out.println("returning");
                        continue;
                    }
                    while (socket.getInputStream().available() == 0) {
                        System.out.print("");
                    }
                    SocketObject so = null;
                    Object o = null;
                    try {
                        o = ois.readObject();
                    } catch (Exception e) {
                        continue;
                    }
                    if (o == null) {
                        System.out.println("o is null");
                        continue;
                    }
                    so = (SocketObject) o;
                    System.out.println("MessageType: " + so.getMessageType());
                    final SocketObject so1 = so;
                    if (so.getMessageType().equalsIgnoreCase("Size")) {
                        try {
                            String size = so.getMessage();
                            songSize = Integer.parseInt(size);
                            System.out.println("Size of the song: " + size);
                        } catch (Exception e) {
                            System.out.println("Exception: " + e.toString());
                        }
                    }
                    if (so.getMessageType().equalsIgnoreCase("SendLibraryDone")) {
                        new Thread() {

                            public void run() {
                                updateSharedLibraryTable();
                                pool = "";
                            }
                        }.start();
                    }
                    if (so.getMessageType().equalsIgnoreCase("GetLibrary")) {
                        String sendTo = so.getSource();
                        sendLibraryTo(sendTo);
                    }
                    if (so.getMessageType().equalsIgnoreCase("Address")) {
                        addAddressToList(so.getSource());
                    }
                    if (so.getMessageType().equalsIgnoreCase("Library")) {
                        recievingLibraryData = true;
                        new Thread() {

                            public void run() {
                                addToSharedLibrary(so1);
                            }
                        }.start();
                    }
                    if (so.getMessageType().equalsIgnoreCase("MusicOutputDesignDestination")) {
                        recievingLibraryData = true;
                        new Thread() {

                            public void run() {
                                addToSharedLibrary(so1);
                            }
                        }.start();
                    }
                    if (so.getMessageType().equalsIgnoreCase("GetStreamFrom")) {
                        final String address = so.getSource();
                        final String audioFile = so.getMessage();
                        new Thread() {

                            public void run() {
                                sendAudioStream(address, audioFile);
                            }
                        }.start();
                    }
                    if (so.getMessageType().equalsIgnoreCase("SendToAddress")) {
                        setObjectToPlay(so);
                    }
                    if (so.getMessageType().equalsIgnoreCase("Done")) {
                        System.out.println("received done");
                    }
                } catch (IOException ex) {
                    mbv.showErrorDialog(ex);
                    break;
                }
            }
            mbv.sharedLibraryTable.setModel(new CustomTableModel());
            mbv.sharedLibrary.clear();
            ((DefaultListModel) mbv.addressList.getModel()).removeAllElements();
        }
    }

    public void playStream() {
        AudioInputStream in = null;
        System.out.println("Got audio stream");
        ByteArrayInputStream bais = null;
        long fileLength = 0;
        playing = true;
        try {
            fileLength = pis.available();
            in = AudioSystem.getAudioInputStream(pis);
        } catch (Exception ex1) {
            MusicBoxView.showErrorDialog(ex1);
            System.err.println("First AudioSystem.getAudioInputStream method didn't work");
            Bitstream m = new Bitstream(pis);
            long start = m.header_pos();
            fileLength = fileLength - start;
            try {
                m.close();
            } catch (Exception ex) {
                MusicBoxView.showErrorDialog(ex);
            }
            try {
                pis.skip(start);
            } catch (IOException ex) {
                System.out.println("IOException: " + ex.toString());
            }
            try {
                in = AudioSystem.getAudioInputStream(pis);
            } catch (UnsupportedAudioFileException ex) {
                System.out.println("UnsupportedAudioFileException");
                ex.printStackTrace();
            } catch (IOException ex) {
                System.out.append("IOException");
                ex.printStackTrace();
            }
        }
        AudioInputStream din = null;
        if (in != null) {
            try {
                AudioFormat baseFormat = in.getFormat();
                AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                System.out.println("decodedFormat.toString(): " + decodedFormat.toString());
                din = AudioSystem.getAudioInputStream(decodedFormat, in);
                System.out.println("rawplay called");
                mbv.back.rawplay(decodedFormat, din);
                playing = false;
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (LineUnavailableException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void playStream(byte[] buf) {
        System.out.println("buf.length: " + buf.length);
        AudioInputStream in = null;
        System.out.println("Got audio stream");
        ByteArrayInputStream bais = null;
        long fileLength = buf.length;
        playing = true;
        try {
            bais = new ByteArrayInputStream(buf);
            in = AudioSystem.getAudioInputStream(bais);
        } catch (Exception ex1) {
            MusicBoxView.showErrorDialog(ex1);
            Bitstream m = new Bitstream(bais);
            long start = m.header_pos();
            fileLength = fileLength - start;
            try {
                m.close();
            } catch (Exception ex) {
                MusicBoxView.showErrorDialog(ex);
            }
            bais = new ByteArrayInputStream(buf);
            bais.skip(start);
            try {
                in = AudioSystem.getAudioInputStream(bais);
            } catch (UnsupportedAudioFileException ex) {
                System.out.println("UnsupportedAudioFileException");
                ex.printStackTrace();
            } catch (IOException ex) {
                System.out.append("IOException");
                ex.printStackTrace();
            }
        }
        AudioInputStream din = null;
        if (in != null) {
            try {
                AudioFormat baseFormat = in.getFormat();
                AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                din = AudioSystem.getAudioInputStream(decodedFormat, in);
                System.out.println("rawplay called");
                mbv.back.rawplay(decodedFormat, din);
                playing = false;
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (LineUnavailableException ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("Done playing audio stream");
    }

    public void getAudioStream(String address, String audioFile) {
        SocketObject so = null;
        try {
            so = new SocketObject("GetStreamFrom", audioFile, address);
        } catch (UnknownHostException ex) {
            mbv.showErrorDialog(ex);
        }
        try {
            oos.writeObject(so);
            oos.flush();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            mbv.showErrorDialog(ex);
        }
    }

    public void sendAudioStream(final String address, final String audioFile) {
        new Thread() {

            public void run() {
                File file1 = new File(audioFile);
                byte[] buf = null;
                try {
                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file1));
                    while (bis.available() < file1.length()) System.out.print("");
                    buf = new byte[bis.available()];
                    bis.read(buf, 0, buf.length);
                    bis.close();
                } catch (Exception ex1) {
                    mbv.showErrorDialog(ex1);
                }
                try {
                    SocketObject so;
                    if (buf != null) {
                        so = new SocketObject(null, "SendToAddress", "", socket.getLocalAddress().getHostAddress(), address, buf);
                    } else {
                        so = new SocketObject(null, "FileNotFound", "", socket.getLocalAddress().getHostAddress(), address, null);
                    }
                    System.out.println("destination: " + so.getDestination());
                    oos.writeObject(so);
                    oos.flush();
                } catch (IOException ex) {
                    mbv.showErrorDialog(ex);
                }
            }
        }.start();
    }

    private void addToSharedLibrary(SocketObject so) {
        if (mbv.sharedLibrary == null) {
            mbv.sharedLibrary = (Vector<MusicOutputDesign>) so.getList().clone();
        }
        for (int i = 0; i < mbv.sharedLibrary.size(); i++) {
            MusicOutputDesign mod = mbv.sharedLibrary.elementAt(i);
            String rating = mod.getRating();
            ImageIcon ico = mbv.smileys[0];
            try {
                ico = mbv.smileys[Integer.parseInt(rating)];
            } catch (NumberFormatException ex) {
                ico = mbv.smileys[0];
            }
            final Object[] o = new Object[] { mod.getTrackNumber(), mod.getTrackTitle(), mod.getArtist(), mod.getAlbum(), mod.getTrackLength(), mod.getPlayCount(), ico };
            if (((CustomTableModel) MusicBoxView.sharedLibraryTable.getModel()).getColumnCount() == 0) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        public void run() {
                            CustomTableModel ctm = (CustomTableModel) MusicBoxView.sharedLibraryTable.getModel();
                            TableRowSorter<CustomTableModel> sorter3 = new TableRowSorter<CustomTableModel>(ctm);
                            if (ctm.getColumnCount() == 0) {
                                for (int i = 0; i < mbv.columnNames.length; i++) {
                                    ctm.addColumn(mbv.columnNames[i]);
                                }
                                sorter3.setComparator(ctm.findColumn("Track Number"), new TrackNumberStringComparator());
                                sorter3.setComparator(ctm.findColumn("Artist"), new ArtistStringComparator());
                                sorter3.setComparator(ctm.findColumn("Album"), new AlbumStringComparator());
                                MusicBoxView.sharedLibraryTable.setRowSorter(sorter3);
                            }
                            ctm.addRow(o);
                        }
                    });
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } catch (InvocationTargetException ex) {
                    ex.printStackTrace();
                }
            } else {
                CustomTableModel ctm = (CustomTableModel) MusicBoxView.sharedLibraryTable.getModel();
                ctm.addRow(o);
            }
        }
    }

    private void addToSharedLibrary(String s, InputStream is) {
        if (mbv.sharedLibrary == null) {
            mbv.sharedLibrary = new Vector<MusicOutputDesign>();
        }
        System.out.println("Parsing");
        StringTokenizer st = new StringTokenizer(s, "\n");
        while (st.hasMoreTokens()) {
            String info = st.nextToken();
            if (info.indexOf("<MusicOutputDesign>") > info.indexOf("</MusicOutputDesign>")) {
                continue;
            }
            String trackNumber = "";
            String trackTitle = "";
            String artist = "";
            String album = "";
            String trackLength = "";
            String playCount = "";
            String rating = "";
            String ImagePath = "NoImage";
            String file = "";
            String filePath = "";
            String id = "";
            MusicOutputDesign mod = new MusicOutputDesign();
            ImageIcon ico = mbv.smileys[0];
            if (info.indexOf("<TrackNumber>") < info.indexOf("</TrackNumber>") && info.indexOf("<TrackNumber>") != -1) {
                trackNumber = info.substring(info.indexOf("<TrackNumber>") + 13, info.indexOf("</TrackNumber>"));
            }
            if (info.indexOf("<TrackTitle>") < info.indexOf("</TrackTitle>") && info.indexOf("<TrackTitle>") != -1) {
                trackTitle = info.substring(info.indexOf("<TrackTitle>") + 12, info.indexOf("</TrackTitle>"));
            }
            if (info.indexOf("<Artist>") < info.indexOf("</Artist>") && info.indexOf("<Artist>") != -1) {
                artist = info.substring(info.indexOf("<Artist>") + 8, info.indexOf("</Artist>"));
            }
            if (info.indexOf("<Album>") < info.indexOf("</Album>") && info.indexOf("<Album>") != -1) {
                album = info.substring(info.indexOf("<Album>") + 7, info.indexOf("</Album>"));
            }
            if (info.indexOf("<TrackLength>") < info.indexOf("</TrackLength>") && info.indexOf("<TrackLength>") != -1) {
                trackLength = info.substring(info.indexOf("<TrackLength>") + 13, info.indexOf("</TrackLength>"));
            }
            if (info.indexOf("<PlayCount>") < info.indexOf("</PlayCount>") && info.indexOf("<PlayCount>") != -1) {
                playCount = info.substring(info.indexOf("<PlayCount>") + 11, info.indexOf("</PlayCount>"));
            }
            if (info.indexOf("<Rating>") < info.indexOf("</Rating>") && info.indexOf("<Rating>") != -1) {
                rating = info.substring(info.indexOf("<Rating>") + 8, info.indexOf("</Rating>"));
                try {
                    ico = mbv.smileys[Integer.parseInt(rating)];
                } catch (NumberFormatException ex) {
                    ico = mbv.smileys[0];
                }
            }
            if (info.indexOf("<File>") < info.indexOf("</File>") && info.indexOf("<File>") != -1) {
                file = info.substring(info.indexOf("<File>") + 6, info.indexOf("</File>"));
            }
            if (info.indexOf("<FilePath>") < info.indexOf("</FilePath>") && info.indexOf("<FilePath>") != -1) {
                filePath = info.substring(info.indexOf("<FilePath>") + 10, info.indexOf("</FilePath>"));
            }
            if (info.indexOf("<Id>") < info.indexOf("</Id>") && info.indexOf("<Id>") != -1) {
                id = info.substring(info.indexOf("<Id>") + 4, info.indexOf("</Id>"));
            }
            mod.setAlbum(album);
            mod.setArtist(artist);
            mod.setFile(file);
            mod.setFilePath(filePath);
            mod.setId(id);
            mod.setImagePath(ImagePath);
            mod.setPlayCount(playCount);
            mod.setRating(rating);
            mod.setTrackLength(trackLength);
            mod.setTrackNumber(trackNumber);
            mod.setTrackTitle(trackTitle);
            mbv.sharedLibrary.add(mod);
            final Object[] o = new Object[] { trackNumber, trackTitle, artist, album, trackLength, playCount, ico };
            if (((CustomTableModel) MusicBoxView.sharedLibraryTable.getModel()).getColumnCount() == 0) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        public void run() {
                            CustomTableModel ctm = (CustomTableModel) MusicBoxView.sharedLibraryTable.getModel();
                            if (ctm.getColumnCount() == 0) {
                                for (int i = 0; i < mbv.columnNames.length; i++) {
                                    ctm.addColumn(mbv.columnNames[i]);
                                }
                            }
                            ctm.addRow(o);
                        }
                    });
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } catch (InvocationTargetException ex) {
                    ex.printStackTrace();
                }
            } else {
                CustomTableModel ctm = (CustomTableModel) MusicBoxView.sharedLibraryTable.getModel();
                ctm.addRow(o);
            }
        }
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        CustomTableModel ctm = (CustomTableModel) MusicBoxView.sharedLibraryTable.getModel();
                        if (ctm.getColumnCount() == 0) {
                            for (int i = 0; i < mbv.columnNames.length; i++) {
                                ctm.addColumn(mbv.columnNames[i]);
                            }
                        }
                        ctm.fireTableDataChanged();
                    }
                });
            } catch (InterruptedException ex) {
                mbv.showErrorDialog(ex);
            } catch (InvocationTargetException ex) {
                mbv.showErrorDialog(ex);
            }
        } else {
            ((CustomTableModel) mbv.sharedLibraryTable.getModel()).fireTableDataChanged();
        }
        recievingLibraryData = false;
        pool = "";
        System.out.println("Finished parsing");
    }

    private void updateSharedLibraryTable() {
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        CustomTableModel ctm = (CustomTableModel) MusicBoxView.sharedLibraryTable.getModel();
                        if (ctm.getColumnCount() == 0) {
                            for (int i = 0; i < mbv.columnNames.length; i++) {
                                ctm.addColumn(mbv.columnNames[i]);
                            }
                        }
                        ctm.fireTableDataChanged();
                    }
                });
            } catch (InterruptedException ex) {
                mbv.showErrorDialog(ex);
            } catch (InvocationTargetException ex) {
                mbv.showErrorDialog(ex);
            }
        } else {
            System.out.println("Already in EDT");
            ((CustomTableModel) mbv.sharedLibraryTable.getModel()).fireTableDataChanged();
        }
    }

    private void sendLibraryTo(final String sendTo) {
        new Thread() {

            public void run() {
                try {
                    OutputStream os = socket.getOutputStream();
                    SocketObject so = new SocketObject(new MusicOutputDesign(), "Library", "MusicOutputDesigns", socket.getLocalAddress().getHostAddress(), sendTo, new byte[0]);
                    so.setList(mbv.allMusic);
                    oos.writeObject(so);
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (IOException ex) {
                    mbv.showErrorDialog(ex);
                }
            }
        }.start();
    }

    private void addAddressToList(String address) {
        try {
            InetAddress addr = InetAddress.getByName(address);
            System.out.println(addr.getHostName());
            String hostName = addr.getHostName();
            System.out.println(!((DefaultListModel) mbv.addressList.getModel()).contains(hostName));
            System.out.println("Host name: " + hostName);
            if (!((DefaultListModel) mbv.addressList.getModel()).contains(hostName)) {
                ((DefaultListModel) mbv.addressList.getModel()).addElement(hostName);
            }
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        }
    }

    public void setObjectToPlay(SocketObject objectToPlay) {
        this.objectToPlay = objectToPlay;
    }

    public SocketObject getObjectToPlay() {
        return objectToPlay;
    }

    public void disconnect() {
        rec.cancel();
        rec.purge();
        try {
            socket.close();
        } catch (IOException ex) {
            mbv.showErrorDialog(ex);
        }
    }
}
