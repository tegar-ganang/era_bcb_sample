package net.chipped.monolith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.chipped.utils.Tools;

public class Monolith {

    public static final String VERSION = "$Id: Monolith.java,v 1.3 2006/02/08 08:04:19 gnovos Exp $";

    private static final int HASHABLE_BYTECOUNT = 1024;

    private static final Map<String, Monolith> _easterIsland = new LinkedHashMap<String, Monolith>();

    private boolean _sync = true;

    private boolean _contiguous = false;

    private boolean _useHashes = true;

    public static Monolith get(File file, boolean rewrite) throws IOException {
        synchronized (_easterIsland) {
            Monolith lith = _easterIsland.get(file.getAbsolutePath());
            if (lith == null) {
                lith = new Monolith(file, rewrite);
                _easterIsland.put(file.getAbsolutePath(), lith);
            }
            return lith;
        }
    }

    public static Monolith get(File file) throws IOException {
        return get(file, false);
    }

    private final AllocationTable _index;

    private FileChannel _channel;

    private final File _store;

    public Monolith(File store) throws IOException {
        this(store, false);
    }

    public Monolith(File store, boolean rewrite) throws IOException {
        _store = store;
        boolean newStore = false;
        if (!store.exists() || store.length() == 0) {
            newStore = true;
        }
        _channel = new RandomAccessFile(_store, "rw").getChannel();
        if (rewrite) _channel.truncate(0L);
        _index = new AllocationTable(_channel);
        if (newStore || rewrite) {
            sync();
        }
        _index.loadTable();
        truncateEmptySpace();
        Runtime.getRuntime().addShutdownHook(new Thread() {

            public void run() {
                try {
                    close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static boolean isMonolithStore(File store) {
        if (!store.exists()) return false;
        FileChannel channel = null;
        try {
            channel = new RandomAccessFile(store, "r").getChannel();
            AllocationTable table = new AllocationTable(channel);
            table.loadTable();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (channel != null) channel.close();
            } catch (IOException e) {
            }
        }
    }

    public void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public void store(String name, File file) {
        try {
            String md5 = null;
            if (_useHashes && file.length() > HASHABLE_BYTECOUNT) {
                try {
                    md5 = Tools.MD5File(file);
                    if (_index.containsHash(md5, file.length())) {
                        String hashed = _index.getHashed(md5, file.length());
                        if (!name.equals(hashed)) {
                            _index.alias(hashed, name);
                        }
                        return;
                    }
                } catch (NoSuchAlgorithmException e) {
                    _useHashes = false;
                    System.err.println("Hashes has been disabled! Hash algorithm " + Tools.HASH_ALGORITHM + " is not available here!");
                    e.printStackTrace();
                }
            }
            List<Segment> segments = new ArrayList<Segment>();
            FileChannel source = new FileInputStream(file).getChannel();
            for (final Segment segment : _index.freeSegments((int) source.size(), _contiguous)) {
                xferSegment(source, segment);
                segments.add(segment);
            }
            if (source.position() < source.size()) {
                Segment segment = new Segment(_channel, _index.end(), (int) (source.size() - source.position()));
                xferSegment(source, segment);
                segments.add(segment);
            }
            _index.addSegments(name, segments);
            _index.setHash(name, md5, file.length());
            flush(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void xferSegment(FileChannel source, Segment segment) throws IOException {
        long len = _channel.transferFrom(source, segment.getPosition(), segment.getLength());
        if (len != segment.getLength() && len != source.size()) {
            System.err.println("Did not transfer as many bytes(" + len + ") " + "as expected(" + segment.getLength() + ")!  Trying it the slow way.");
            ByteBuffer buffer = ByteBuffer.allocateDirect(segment.getLength());
            source.read(buffer);
            buffer.flip();
            _channel.write(buffer, segment.getPosition());
            _channel.force(true);
            segment.initBuffers(true);
        }
    }

    public void store(String name, byte[] data) throws IOException {
        String hash = null;
        if (_useHashes && data.length > HASHABLE_BYTECOUNT) {
            try {
                hash = Tools.MD5hash(data);
                if (_index.containsHash(hash, data.length)) {
                    String hashed = _index.getHashed(hash, data.length);
                    if (!name.equals(hashed)) {
                        _index.alias(hashed, name);
                    }
                    return;
                }
            } catch (NoSuchAlgorithmException e) {
                _useHashes = false;
                System.err.println("Hashes has been disabled! Hash algorithm " + Tools.HASH_ALGORITHM + " is not available here!");
                e.printStackTrace();
            }
        }
        List<Segment> segments = new ArrayList<Segment>();
        int offset = 0;
        for (final Segment segment : _index.freeSegments(data.length, _contiguous)) {
            segment.write(data, offset);
            offset += segment.getLength();
            segments.add(segment);
        }
        if (offset < data.length) {
            int remainder = data.length - offset;
            Segment segment = new Segment(_channel, _index.end(), remainder);
            segment.write(data, offset);
            segments.add(segment);
        }
        _index.addSegments(name, segments);
        _index.setHash(name, hash, data.length);
        flush(false);
    }

    public byte[] retrieve(String name) {
        name = _index.unalias(name);
        if (!_index.contains(name)) {
            return null;
        }
        int filesize = _index.getSize(name);
        byte[] data = new byte[filesize];
        int offset = 0;
        for (Segment segment : _index.getSegments(name)) {
            segment.read(data, offset);
            offset += segment.getLength();
        }
        return data;
    }

    public List<String> list() {
        return new ArrayList<String>(_index.entries());
    }

    public List<String> list(final String pattern) {
        ArrayList<String> res = new ArrayList<String>();
        for (String name : list()) {
            if (name.matches(pattern)) {
                res.add(name);
            }
        }
        return res;
    }

    public void remove(String name) {
        _index.unlink(name);
    }

    public File getStore() {
        return _store;
    }

    public void flush(boolean force) throws IOException {
        if (_sync || force) {
            sync();
        }
    }

    public void sync() throws IOException {
        _sync = true;
        _index.flush(false);
        _index.saveTable();
        _channel.force(true);
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean extendedPrint) {
        Set<String> list = _index.entries();
        String out = "[Monolith]\n";
        out += "FILE:    " + _store.getAbsolutePath() + "\n";
        out += "SIZE:    " + _store.length() + " bytes\n";
        out += "INDEX:   (approx) " + (_store.length() - _index.end()) + " bytes\n";
        out += "DATA:    " + _index.end() + " bytes " + _index.frag() + "% fragmented\n";
        out += "FREE:    " + _index.getFreeSize() + " bytes ";
        Set<Segment> empty = _index.unallocated();
        if (!empty.isEmpty()) {
            out += "Segments ";
        }
        for (final Segment segment : empty) {
            out += "[" + segment.getPosition() + '-' + segment.getEnd() + "] ";
        }
        out += '\n';
        if (extendedPrint) {
            out += "ENTRIES: " + list.size() + "\n";
            for (final String entry : list) {
                if (_index.isAlias(entry)) {
                    continue;
                }
                out += entry + " (" + _index.getSize(entry) + " bytes) Segments: ";
                List<Segment> segments = _index.segments(entry);
                for (final Segment segment : segments) {
                    out += "[" + segment.getPosition() + '-' + segment.getEnd() + "] ";
                }
                out += '\n';
                Set<String> aliases = _index.aliasesFor(entry);
                if (!aliases.isEmpty()) {
                    out += "  aka: ";
                }
                for (final String alias : aliases) {
                    out += '(' + alias + ") ";
                }
                if (!aliases.isEmpty()) {
                    out += '\n';
                }
                Map<String, String> p = _index.getFileProperties(entry);
                if (p != null && !p.isEmpty()) {
                    out += "     ";
                    for (final Entry<String, String> ent : p.entrySet()) {
                        out += '<' + ent.getKey() + '=' + ent.getValue() + "> ";
                    }
                    out += '\n';
                }
            }
        }
        return out;
    }

    public InputStream retrieveInputStream(String name) {
        return new ByteArrayInputStream(retrieve(name));
    }

    public OutputStream retrieveOutputStream(final String name) {
        return new ByteArrayOutputStream() {

            public void close() throws IOException {
                store(name, toByteArray());
                super.close();
            }
        };
    }

    public void setProperty(String name, String key, String value) throws IOException {
        _index.setFileProperty(name, key, value);
        flush(false);
    }

    public void setProperty(String key, String value) throws IOException {
        _index.setProperty(key, value);
        flush(false);
    }

    public String getProperty(String key) {
        return _index.getProperty(key);
    }

    public boolean hasProperties(String name) {
        return _index.hasFileProperties(name);
    }

    public String getProperty(String name, String key) {
        return _index.getFileProperty(name, key);
    }

    void unsync() {
        _sync = false;
    }

    public void setContiguousAllocation(boolean contig) {
        _contiguous = true;
    }

    public void close() throws IOException {
        _easterIsland.remove(_store.getAbsolutePath());
        if (!_channel.isOpen()) {
            return;
        }
        sync();
        _index.flush(true);
        truncateEmptySpace();
        try {
            _channel.force(true);
            _channel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void truncateEmptySpace() {
        long pos = 0;
        long size = 0;
        boolean truncated = false;
        int trycount = 0;
        while (!truncated) {
            try {
                pos = _channel.position();
                size = _channel.size();
                if (pos != size) {
                    _channel.truncate(pos);
                }
                truncated = true;
            } catch (Exception e) {
                trycount++;
                if (trycount > 5) {
                    truncated = true;
                    System.out.println("Could not truncate channel this time, file is " + (size - pos) + " bytes larger than it could be.");
                } else {
                    System.out.println("Compacting store... please wait.");
                    try {
                        Thread.sleep(trycount * 500);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("" + "This is basically a library, but you can use it from the command line\n" + "to pack up and view Monoliths.\n" + "\n" + "Usage: java net.chipped.monolith.Monolith lithfile.lith [file] [file] [dir] [etc]\n" + "\n" + "If you include addional files and directories on the command line they will\n" + "be added to the lithfile.  If not it will print out the contents of the file.\n" + "This command line version is not particularly smart, so don't expect much.");
            System.exit(0);
        }
        try {
            Monolith lith = new Monolith(new File(args[0]), false);
            if (args.length > 1) {
                lith.unsync();
                for (int i = 1; i < args.length; i++) {
                    packDir(lith, new File(args[i]));
                }
                lith.sync();
            }
            System.out.println(lith.toString(true));
            lith.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void packDir(Monolith lith, File file) {
        File[] list = file.listFiles();
        for (File pack : list) {
            if (pack.isFile()) {
                System.out.println("add: " + pack.getName());
                lith.store(pack.getName(), pack);
            } else if (pack.isDirectory()) {
                packDir(lith, pack);
            } else {
                System.out.println("don't know how to handle: " + pack);
            }
        }
    }

    public void usemmap(boolean usemmap) {
        Segment.mmap(usemmap);
    }

    public void usehash(boolean usehash) {
        _useHashes = usehash;
    }

    public long sizeOf(String name) {
        return _index.getSize(name);
    }

    public boolean contains(String name, String hash, long len) {
        name = _index.unalias(name);
        return name.equals(_index.getHashed(hash, len));
    }

    public boolean contains(String name) {
        return list().contains(name);
    }

    public void rename(String oldname, String newname) {
        _index.rename(oldname, newname);
    }
}
