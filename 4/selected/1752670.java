package org.magnesia.chalk;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.magnesia.JImage;
import org.magnesia.OPCodes;
import org.magnesia.Pair;
import org.magnesia.chalk.data.Constants;
import org.magnesia.misc.StreamWrapper;
import org.magnesia.misc.WrapperException;
import static org.magnesia.misc.Utils.log;

class Connection implements Runnable {

    private final Socket s;

    private Handler h;

    private boolean authenticated = false;

    private Map<String, ThumbnailLoader> readers;

    private Lock lock;

    private final Chalk c;

    private String user;

    private StreamWrapper stream;

    public Connection(Socket s, Chalk c, ChalkProperties p) {
        this.s = s;
        this.c = c;
        this.stream = new StreamWrapper(s);
        lock = new ReentrantLock();
        readers = new HashMap<String, ThumbnailLoader>();
        String base = Constants.DEFAULT_PATH;
        try {
            if (p.containsKey("BASEDIR")) base = p.getProperty("BASEDIR");
        } catch (Exception e) {
            log("Couldn't parse setting for BASEDIR - " + p.getProperty("BASEDIR") + ". Using default '" + base + "'");
        }
        h = new Handler(base);
    }

    private String getHash() {
        return "" + s.hashCode() + "|" + System.nanoTime();
    }

    public void run() {
        try {
            while (s.isConnected()) try {
                lock.lock();
                short command = stream.readOpCode();
                if (command == 0) break;
                if (authenticated) {
                    String hash = getHash();
                    switch(command) {
                        case OPCodes.SET_TB_WIDTH:
                            int width = stream.readInt();
                            stream.writeBoolean(h.setThumbnailWidth(width));
                            break;
                        case OPCodes.COMMIT:
                            stream.writeString(hash);
                            String path = stream.readString();
                            int count = stream.readInt();
                            log("Read " + count + " images (" + hash + ")");
                            for (int i = 0; i < count; i++) {
                                if (!stream.readBoolean()) break;
                                String name = stream.readString();
                                h.storeImage(path, name, stream.readDataFully());
                            }
                            lock.unlock();
                            break;
                        case OPCodes.GET_INFO:
                            path = stream.readString();
                            log("Get image infos for " + path);
                            Map<String, String> infos = h.getImageInfos(path);
                            stream.writeInt(infos.size());
                            for (String key : infos.keySet()) {
                                stream.writeString(key);
                                stream.writeString(infos.get(key));
                            }
                            break;
                        case OPCodes.GET_COMMENT:
                            path = stream.readString();
                            log("Get comment for " + path);
                            stream.writeString(h.getComment(path));
                            break;
                        case OPCodes.SET_COMMENT:
                            path = stream.readString();
                            String comment = stream.readString();
                            log("Set comment for " + path + " to " + comment);
                            h.setComment(path, comment);
                            break;
                        case OPCodes.LIST_DIRECTORIES:
                            path = stream.readString();
                            log("List subdirectories of " + path);
                            List<String> entries = h.listEntries(path);
                            stream.writeInt(entries.size());
                            for (int i = 0; i < entries.size(); i++) {
                                stream.writeString(entries.get(i));
                            }
                            lock.unlock();
                            break;
                        case OPCodes.LIST_IMAGES:
                            path = stream.readString();
                            log("List images in " + path);
                            entries = h.listImages(path);
                            stream.writeInt(entries.size());
                            for (int i = 0; i < entries.size(); i++) {
                                stream.writeString(entries.get(i));
                            }
                            lock.unlock();
                            break;
                        case OPCodes.GET_IMAGE:
                            path = stream.readString();
                            width = stream.readInt();
                            log("Get image for " + path);
                            Pair<JImage, byte[]> p = h.getImage(path, width, false);
                            long time = System.currentTimeMillis();
                            stream.writeDataFully(p.t);
                            lock.unlock();
                            log("Time to transfer " + path + ": " + (System.currentTimeMillis() - time) + "ms");
                            break;
                        case OPCodes.GET_IMAGES:
                            stream.writeString(hash);
                            count = stream.readInt();
                            log("Read " + count + " images (" + hash + ")");
                            for (int i = 0; i < count; i++) {
                                path = stream.readString();
                                p = h.getImage(path, -1, true);
                                if (!stream.readBoolean()) break;
                                if (p == null) stream.writeDataFully(new byte[] {}); else stream.writeDataFully(p.t);
                            }
                            lock.unlock();
                            break;
                        case OPCodes.GET_THUMBNAIL:
                            path = stream.readString();
                            log("Get thumbnail for " + path);
                            p = h.getThumbnail(path);
                            stream.writeDataFully(p.t);
                            lock.unlock();
                            break;
                        case OPCodes.GET_THUMBNAILS:
                            count = stream.readInt();
                            String[] images = new String[count];
                            for (int i = 0; i < count; i++) {
                                images[i] = stream.readString();
                            }
                            stream.writeString(hash);
                            log("Read " + images.length + " thumbnails (" + hash + ")");
                            ThumbnailLoader tl = new ThumbnailLoader(hash, images);
                            readers.put(hash, tl);
                            tl.run();
                            lock.unlock();
                            break;
                        case OPCodes.STOP_READER:
                            hash = stream.readString();
                            lock.unlock();
                            if (readers.containsKey(hash)) {
                                readers.get(hash).running = false;
                            }
                            break;
                        case OPCodes.CREATE_DIRECTORY:
                            path = stream.readString();
                            lock.unlock();
                            log("Create new directory " + path);
                            stream.writeBoolean(h.createDirectory(path));
                            break;
                        case OPCodes.RENAME:
                            String pathOld = stream.readString();
                            String pathNew = stream.readString();
                            log("Rename " + pathOld + " to " + pathNew);
                            stream.writeBoolean(h.rename(pathOld, pathNew));
                            lock.unlock();
                            break;
                        case OPCodes.ROTATE:
                            path = stream.readString();
                            boolean clockwise = stream.readBoolean();
                            log("Rotate " + path + (clockwise ? " clockwise" : " counter clockwise"));
                            byte[] data = h.rotateOrFlip(path, true, clockwise);
                            stream.writeDataFully(data);
                            lock.unlock();
                            break;
                        case OPCodes.FLIP:
                            path = stream.readString();
                            boolean horizontal = stream.readBoolean();
                            log("Flip " + path + (horizontal ? " horizontal" : " vertical"));
                            data = h.rotateOrFlip(path, false, horizontal);
                            stream.writeDataFully(data);
                            lock.unlock();
                            break;
                        case OPCodes.GET_READ_ONLY:
                            stream.writeBoolean(c.getValidator().getUserCanWrite(user));
                            break;
                    }
                } else if (command == OPCodes.LOGIN) {
                    user = stream.readString();
                    log("User " + user + " tries checkin!");
                    boolean userValid = c.isValidUsername(user);
                    stream.writeBoolean(userValid);
                    if (userValid) {
                        byte[] random = createRString(512).getBytes();
                        stream.writeDataFully(random);
                        byte[] encrypted = stream.readDataFully();
                        authenticated = c.getValidator().handleChallenge(user, random, encrypted);
                        if (authenticated) {
                            h.setAllowedDirectories(c.getValidator().getAllowedRegex(user));
                            boolean ro = c.getValidator().getUserCanWrite(user);
                            h.setReadOnly(ro);
                            log("User " + user + " has " + (ro ? "read-only permissions." : "write permissions."));
                        }
                    }
                    log("User " + user + (authenticated ? " authenticated successfully." : " failed to authenticate."));
                    stream.writeBoolean(authenticated);
                    lock.unlock();
                }
            } catch (WrapperException e) {
                e.printStackTrace();
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                log(s.getInetAddress() + " disconnected");
                stream.close();
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (ChalkListener cl : c.listeners) {
                cl.clientLeft();
            }
        }
    }

    private String createRString(int bit) {
        StringBuilder sb = new StringBuilder();
        Random r = new Random();
        bit = bit / 3;
        bit -= bit % 8;
        for (int i = 0; i < bit; i++) {
            sb.append((char) (48 + r.nextInt(76)));
        }
        return sb.toString();
    }

    private class ThumbnailLoader implements Runnable {

        private String hash;

        private String[] images;

        private int finished = 0;

        private boolean running = true;

        private Queue<Pair<JImage, byte[]>> loaded;

        private ThumbnailLoader(String hash, String[] images) {
            this.hash = hash;
            this.images = images;
            loaded = new ConcurrentLinkedQueue<Pair<JImage, byte[]>>();
            new Thread(new Reader()).start();
        }

        public void run() {
            try {
                while (finished < images.length && running) {
                    if (!loaded.isEmpty()) {
                        Pair<JImage, byte[]> p = loaded.remove();
                        log("Send thumbnail for " + p.e.getPath() + "/" + p.e.getName() + ", hash " + hash);
                        stream.writeString(hash);
                        stream.writeString(p.e.getPath() + "/" + p.e.getName());
                        stream.writeLong(p.e.getFileSize());
                        stream.writeDataFully(p.t);
                        running = stream.readBoolean();
                        finished++;
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    log(s.getInetAddress() + " disconnected");
                    stream.close();
                    s.close();
                } catch (IOException io) {
                    io.printStackTrace();
                }
            }
            running = false;
        }

        private class Reader implements Runnable {

            public void run() {
                try {
                    for (int i = 0; i < images.length && running; i++) {
                        loaded.offer(h.getThumbnail(images[i]));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
