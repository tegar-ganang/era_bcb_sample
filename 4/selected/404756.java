package net.sf.mogbox.pol.ffxi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mogbox.pol.Region;

public class DataFile {

    private static final Map<Region, int[]> TABLE = new EnumMap<Region, int[]>(Region.class);

    private static Logger log = Logger.getLogger(DataFile.class.getName());

    public static int getMaximumID() {
        return getMaximumID(Region.getDefaultFFXIRegion());
    }

    public static int getMaximumID(Region region) {
        return getTable(region).length - 1;
    }

    public static int lookupID(Region region, int expansion, int directory, int dat) {
        int[] table = getTable(region);
        int location = (dat & 0x7F) | (directory << 7 & 0xFF80) | expansion << 16;
        for (int i = 0; i < table.length; i++) {
            if (table[i] == location) return i;
        }
        return -1;
    }

    public static int lookupID(int expansion, int directory, int dat) {
        return lookupID(Region.getDefaultFFXIRegion(), expansion, directory, dat);
    }

    public static int lookupFileNumber(Region region, int id) {
        if (id < 0) return -1;
        int[] table = getTable(region);
        if (table == null) return -1;
        if (id >= table.length) return -1;
        return table[id];
    }

    public static int lookupFileNumber(int id) {
        return lookupFileNumber(Region.getDefaultFFXIRegion(), id);
    }

    private Region region;

    private int id, expansion, directory, dat;

    private File file;

    private String ffxiPath;

    public DataFile(int id) {
        this(Region.getDefaultFFXIRegion(), id);
    }

    public DataFile(Region region, int id) {
        this.region = region;
        this.id = id;
        ffxiPath = region.getFFXILocation().getAbsolutePath();
        int[] table = getTable(region);
        if (id >= table.length) return;
        int location = table[id];
        expansion = location >>> 16;
        if (expansion != 0) {
            directory = location >> 7 & 0x1FF;
            dat = location & 0x7F;
            String filename;
            if (expansion == 1) filename = String.format("ROM/%d/%d.DAT", directory, dat); else filename = String.format("ROM%d/%d/%d.DAT", expansion, directory, dat);
            File temp = new File(region.getFFXILocation(), filename);
            if (temp.exists() && temp.isFile() && temp.canRead()) file = temp;
        }
    }

    public boolean exists() {
        return file != null;
    }

    public Region getRegion() {
        return region;
    }

    public int getID() {
        return id;
    }

    public int getExpansion() {
        return expansion;
    }

    public int getDirectory() {
        return directory;
    }

    public int getDat() {
        return dat;
    }

    public File getFile() {
        return file;
    }

    public FileChannel getFileChannel() throws FileNotFoundException {
        return getFileChannel(false);
    }

    public FileChannel getFileChannel(boolean writeable) throws FileNotFoundException {
        if (file != null) return new RandomAccessFile(file, writeable ? "rw" : "r").getChannel();
        throw new FileNotFoundException();
    }

    public long getPatchTime() {
        if (file != null) return file.lastModified();
        return 0L;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((region == null) ? 0 : region.hashCode());
        result = prime * result + id;
        result = prime * result + expansion;
        result = prime * result + directory;
        result = prime * result + dat;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        DataFile other = (DataFile) obj;
        if (region != other.region) return false;
        if (id != other.id) return false;
        if (expansion != other.expansion) return false;
        if (directory != other.directory) return false;
        if (dat != other.dat) return false;
        return true;
    }

    @Override
    public String toString() {
        String path = null;
        if (file != null) {
            path = file.getAbsolutePath();
            if (path.startsWith(ffxiPath)) {
                path = path.substring(ffxiPath.length());
                path = path.replaceAll("\\\\", "/");
                if (path.startsWith("/")) path = path.replaceFirst("/+", "");
            }
        }
        return String.format("DataFile { ID: %08X, Region: %s, File: %s }", id, region, path);
    }

    private static int[] getTable(Region region) {
        int[] table = TABLE.get(region);
        if (table != null) return table;
        if (!region.isFFXIInstalled()) {
            table = new int[0];
            TABLE.put(region, table);
            return table;
        }
        File ffxi = region.getFFXILocation();
        File[] vtables = new File[9];
        File[] ftables = new File[9];
        vtables[0] = new File(ffxi, "VTABLE.DAT");
        ftables[0] = new File(ffxi, "FTABLE.DAT");
        for (int i = 1; i < 9; i++) {
            vtables[i] = new File(ffxi, String.format("ROM%1$d/VTABLE%1$d.DAT", i + 1));
            ftables[i] = new File(ffxi, String.format("ROM%1$d/FTABLE%1$d.DAT", i + 1));
        }
        int size = 0;
        for (int i = 0; i < vtables.length; i++) {
            if (vtables[i].exists() && ftables[i].exists()) {
                int s = (int) vtables[i].length();
                if (s > size) size = s;
            }
        }
        table = new int[size];
        for (int i = 0; i < vtables.length; i++) {
            if (!vtables[i].exists() || !ftables[i].exists()) continue;
            try {
                InputStream v = new BufferedInputStream(new FileInputStream(vtables[i]));
                InputStream f = new BufferedInputStream(new FileInputStream(ftables[i]));
                int rom = (i + 1) << 16;
                int j = 0;
                int used;
                while ((used = v.read()) >= 0) {
                    int id = rom | (f.read() & 0xFF) | (f.read() << 8 & 0xFF00);
                    if (used != 0) table[j] = id;
                    j++;
                }
            } catch (IOException e) {
                log.log(Level.WARNING, String.format("Error loading table: ROM%d", i + 1), e);
            }
        }
        TABLE.put(region, table);
        return table;
    }
}
