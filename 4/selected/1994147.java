package org.magnesia.client.gui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.prefs.Preferences;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.SocketFactory;
import org.magnesia.Constants;
import org.magnesia.OPCodes;
import org.magnesia.Pair;
import org.magnesia.client.gui.data.NotAuthenticatedException;
import org.magnesia.client.gui.data.Properties;
import org.magnesia.client.gui.tasks.Committer;
import org.magnesia.client.gui.tasks.Downloader;
import org.magnesia.client.gui.tasks.LongTermManager;
import org.magnesia.client.gui.tasks.LongTermTask;
import org.magnesia.client.gui.tasks.ThumbnailLoader;
import org.magnesia.client.gui.tasks.ThumbnailWidth;
import org.magnesia.misc.StreamWrapper;
import org.magnesia.misc.Utils;
import org.magnesia.misc.WrapperException;

public class ClientConnection {

    private Socket shortSocket, longSocket;

    private StreamWrapper swrap, lwrap;

    private Preferences p;

    private boolean authenticated = false;

    private boolean ro = true;

    private Lock lock;

    private Queue<LongTermTask> longTermTasks;

    private LongTermRunner ltr;

    private static ClientConnection instance;

    public ClientConnection() {
        longTermTasks = new LinkedList<LongTermTask>();
        lock = new ReentrantLock();
        p = Preferences.userNodeForPackage(getClass());
        new Thread(ltr = new LongTermRunner()).start();
    }

    public void connect() throws UnknownHostException, IOException {
        if (shortSocket != null) shortSocket.close();
        if (longSocket != null) longSocket.close();
        shortSocket = SocketFactory.getDefault().createSocket(p.get("HOST", "localhost"), p.getInt("PORT", 12345));
        longSocket = SocketFactory.getDefault().createSocket(p.get("HOST", "localhost"), p.getInt("PORT", 12345));
        swrap = new StreamWrapper(shortSocket);
        lwrap = new StreamWrapper(longSocket);
    }

    public boolean login(String username, String password) {
        try {
            lock.lock();
            boolean b = login(username, password, swrap);
            lock.unlock();
            if (b) authenticated = login(username, password, lwrap); else authenticated = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (authenticated) {
            if (!setThumbnailWidth(Properties.getThumbnailWidth())) {
                Properties.setThumbnailWidth(Constants.THUMBNAIL_WIDTH);
            }
            try {
                swrap.writeOpCode(OPCodes.GET_READ_ONLY);
                ro = swrap.readBoolean();
            } catch (WrapperException e) {
                e.printStackTrace();
            }
        }
        return authenticated;
    }

    public boolean isReadOnly() {
        return ro;
    }

    private boolean login(String username, String password, StreamWrapper stream) throws WrapperException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        stream.writeOpCode(OPCodes.LOGIN);
        stream.writeString(username);
        if (stream.readBoolean()) {
            byte[] rand = stream.readDataFully();
            byte[] keyBytes = Utils.prepareKey(password);
            Cipher c = Cipher.getInstance("DES/CBC/NoPadding");
            SecretKeySpec key = new SecretKeySpec(keyBytes, "DES");
            c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(new byte[] { 1, 1, 1, 1, 1, 1, 1, 1 }));
            byte[] cipherText = new byte[rand.length];
            int ptLength = c.update(rand, 0, rand.length, cipherText, 0);
            ptLength += c.doFinal(cipherText, ptLength);
            stream.writeDataFully(cipherText);
        }
        return stream.readBoolean();
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public boolean setThumbnailWidth(int width) {
        check();
        boolean ok = false;
        lock.lock();
        try {
            swrap.writeOpCode(OPCodes.SET_TB_WIDTH);
            swrap.writeInt(width);
            ok = swrap.readBoolean();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        if (ok) longTermTasks.add(new ThumbnailWidth(ltr, width));
        return ok;
    }

    public void commitImages(Collection<String> images, String path, ProgressListener pl) {
        check();
        longTermTasks.add(new Committer(ltr, path, images, pl));
    }

    private void check() {
        if (!authenticated) throw new NotAuthenticatedException();
    }

    public List<String> getEntries(String path) {
        check();
        ArrayList<String> entries = new ArrayList<String>();
        try {
            lock.lock();
            swrap.writeOpCode(OPCodes.LIST_DIRECTORIES);
            swrap.writeString(path);
            int size = swrap.readInt();
            entries.ensureCapacity(size);
            for (int i = 0; i < size; i++) {
                entries.add(swrap.readString());
            }
            lock.unlock();
        } catch (WrapperException e) {
            e.printStackTrace();
        }
        return entries;
    }

    public List<String> getImages(String path) {
        check();
        ArrayList<String> entries = new ArrayList<String>();
        try {
            lock.lock();
            swrap.writeOpCode(OPCodes.LIST_IMAGES);
            swrap.writeString(path);
            int size = swrap.readInt();
            entries.ensureCapacity(size);
            for (int i = 0; i < size; i++) {
                entries.add(swrap.readString());
            }
            List<String> tmp = new ArrayList<String>();
            for (String s : entries) {
                tmp.add(path + "/" + s);
            }
            lock.unlock();
        } catch (WrapperException e) {
            e.printStackTrace();
        }
        return entries;
    }

    public void getImages(Collection<String> images, String path, ProgressListener pl) {
        check();
        longTermTasks.add(new Downloader(ltr, path, images, pl));
    }

    public Map<String, String> getImageInformation(String path) {
        Map<String, String> infos = new HashMap<String, String>();
        try {
            swrap.writeOpCode(OPCodes.GET_INFO);
            swrap.writeString(path);
            int count = swrap.readInt();
            for (int i = 0; i < count; i++) {
                String key = swrap.readString();
                String value = swrap.readString();
                infos.put(key, value);
            }
        } catch (WrapperException e) {
            e.printStackTrace();
        }
        return infos;
    }

    public String getImageComment(String path) {
        String comment = null;
        try {
            swrap.writeOpCode(OPCodes.GET_COMMENT);
            swrap.writeString(path);
            comment = swrap.readString();
        } catch (WrapperException e) {
            e.printStackTrace();
        }
        return comment;
    }

    public void setImageComment(String path, String comment) {
        try {
            swrap.writeOpCode(OPCodes.SET_COMMENT);
            swrap.writeString(path);
            swrap.writeString(comment);
        } catch (WrapperException e) {
            e.printStackTrace();
        }
    }

    public byte[] getImage(String image, int width, ProgressListener pl) {
        check();
        try {
            lock.lock();
            swrap.writeOpCode(OPCodes.GET_IMAGE);
            swrap.writeString(image);
            swrap.writeInt(width);
            int toRead = swrap.readInt();
            if (pl != null) pl.toRead(toRead);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte buf[] = new byte[org.magnesia.Constants.CHUNK_SIZE];
            int read = 0;
            if (pl != null) pl.currentRead(0);
            int size = toRead;
            while (read >= 0 && toRead > 0) {
                read = swrap.readData(buf, ((toRead >= buf.length) ? buf.length : toRead));
                toRead -= read;
                bos.write(buf, 0, read);
                if (pl != null) pl.currentRead(size - toRead);
            }
            lock.unlock();
            return bos.toByteArray();
        } catch (WrapperException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Pair<Long, byte[]> getThumbnail(String image) {
        check();
        try {
            lock.lock();
            swrap.writeOpCode(OPCodes.GET_THUMBNAIL);
            swrap.writeString(image);
            long filesize = swrap.readInt();
            lock.unlock();
            return new Pair<Long, byte[]>(filesize, swrap.readDataFully());
        } catch (WrapperException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ThumbnailLoader getThumbnails(Collection<String> images, ThumbnailListener tl) {
        check();
        if (tl != null) {
            ThumbnailLoader loader = new ThumbnailLoader(ltr, images, tl);
            longTermTasks.add(loader);
            return loader;
        }
        return null;
    }

    public boolean createNewDirectory(String path) {
        check();
        try {
            lock.lock();
            swrap.writeOpCode(OPCodes.CREATE_DIRECTORY);
            swrap.writeString(path);
            boolean success = swrap.readBoolean();
            lock.unlock();
            return success;
        } catch (WrapperException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean rename(String old, String newPath) {
        check();
        try {
            lock.lock();
            swrap.writeOpCode(OPCodes.RENAME);
            swrap.writeString(old);
            swrap.writeString(newPath);
            boolean ret = swrap.readBoolean();
            lock.unlock();
            return ret;
        } catch (WrapperException e) {
            e.printStackTrace();
        }
        return false;
    }

    public byte[] rotate(String path, boolean clockwise) {
        check();
        try {
            lock.lock();
            swrap.writeOpCode(OPCodes.ROTATE);
            swrap.writeString(path);
            swrap.writeBoolean(clockwise);
            byte[] data = swrap.readDataFully();
            lock.unlock();
            return data;
        } catch (WrapperException e) {
            e.printStackTrace();
        }
        return new byte[] {};
    }

    public byte[] flip(String path, boolean horizontal) {
        check();
        try {
            lock.lock();
            swrap.writeOpCode(OPCodes.FLIP);
            swrap.writeString(path);
            swrap.writeBoolean(horizontal);
            byte[] data = swrap.readDataFully();
            lock.unlock();
            return data;
        } catch (WrapperException e) {
            e.printStackTrace();
        }
        return new byte[] {};
    }

    public static ClientConnection getConnection() {
        if (instance == null) instance = new ClientConnection();
        return instance;
    }

    public void stopActiveTask() {
        ltr.stopActiveTask();
    }

    private class LongTermRunner implements Runnable, LongTermManager {

        private LongTermTask active;

        public void run() {
            while (true) {
                if (!longTermTasks.isEmpty() && (active == null || active.finished())) {
                    active = longTermTasks.poll();
                    Progressor pr = Magnesia.getProgressor();
                    if (pr != null) {
                        pr.reset();
                        pr.setText(active.getDescription());
                    }
                    new Thread(active).start();
                } else if (active != null && active.finished()) {
                    if (Magnesia.getProgressor() != null) Magnesia.getProgressor().reset();
                    active = null;
                } else {
                    if (active != null) {
                        Progressor pr = Magnesia.getProgressor();
                        if (pr != null) {
                            pr.toLoad(active.getLength());
                            pr.setValue(active.getStatus());
                            pr.setTooltip(active.getActiveText());
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                    }
                }
            }
        }

        public StreamWrapper getStream() {
            return lwrap;
        }

        public void lockConnection() {
        }

        public void unlockConnection() {
        }

        public void stopActiveTask() {
            if (active != null) {
                active.stop();
            }
        }
    }

    public interface ProgressListener {

        public void currentRead(long bytes);

        public boolean toRead(long bytes);
    }

    public interface ThumbnailListener {

        public boolean received(String hash, String img, byte[] data, long filesize);
    }
}
