package net.sf.mogbox.pol.ffxi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.mogbox.pol.Region;
import net.sf.mogbox.renderer.engine.Sound;

public class Music {

    private static final Pattern FILE_PATTERN = Pattern.compile("music(\\d{3}).bgw");

    private static final Map<Region, Integer> MAX = new EnumMap<Region, Integer>(Region.class);

    public static int getMaximumID() {
        return getMaximumID(Region.getDefaultFFXIRegion());
    }

    public static int getMaximumID(Region region) {
        Integer maxInteger = MAX.get(region);
        if (maxInteger != null) return maxInteger;
        int max = 0;
        for (int expansion = 1; expansion <= 9; expansion++) {
            String filename;
            if (expansion == 1) filename = String.format("sound/win/music/data/"); else filename = String.format("sound%d/win/music/data/", expansion);
            File directory = new File(region.getFFXILocation(), filename);
            if (directory.exists() && directory.isDirectory() && directory.canRead()) {
                for (String f : directory.list()) {
                    Matcher m = FILE_PATTERN.matcher(f);
                    if (m.matches()) {
                        int id = Integer.parseInt(m.group(1), 10);
                        if (id > max) max = id;
                    }
                }
            }
        }
        MAX.put(region, max);
        return max;
    }

    private Region region;

    private int id, expansion;

    private File file;

    private String ffxiPath;

    public Music(int id) {
        this(Region.getDefaultFFXIRegion(), id);
    }

    public Music(int expansion, int id) {
        this(Region.getDefaultFFXIRegion(), expansion, id);
    }

    public Music(Region region, int id) {
        if (id < 0) throw new IllegalArgumentException("ID cannot be less than zero: " + id);
        this.region = region;
        this.id = id;
        ffxiPath = region.getFFXILocation().getAbsolutePath();
        for (int expansion = 9; expansion > 0; expansion--) {
            File temp = getFile(region, expansion, id);
            if (temp.exists() && temp.isFile() && temp.canRead()) file = temp;
        }
    }

    public Music(Region region, int expansion, int id) {
        if (expansion <= 0) throw new IllegalArgumentException("Expansion must be greater than zero: " + expansion);
        if (id < 0) throw new IllegalArgumentException("ID cannot be less than zero: " + id);
        this.region = region;
        this.id = id;
        this.expansion = expansion;
        ffxiPath = region.getFFXILocation().getAbsolutePath();
        File temp = getFile(region, expansion, id);
        if (temp.exists() && temp.isFile() && temp.canRead()) file = temp;
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

    public Sound getSound() throws IOException {
        return new FFXISound(getFileChannel());
    }

    public long getPatchTime() {
        if (file != null) return file.lastModified();
        return 0L;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + expansion;
        result = prime * result + id;
        result = prime * result + ((region == null) ? 0 : region.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Music other = (Music) obj;
        if (region != other.region) return false;
        if (expansion != other.expansion) return false;
        if (id != other.id) return false;
        return true;
    }

    @Override
    public String toString() {
        String path = file.getAbsolutePath();
        if (path.startsWith(ffxiPath)) {
            path = path.substring(ffxiPath.length());
            path = path.replaceAll("\\\\", "/");
            if (path.startsWith("/")) path = path.replaceFirst("/+", "");
        }
        return String.format("DataFile { ID: %08X, Region: %s, File: %s }", id, region, path);
    }

    private static File getFile(Region region, int expansion, int id) {
        String filename;
        if (expansion == 1) filename = String.format("sound/win/music/data/music%03d.bgw", id); else filename = String.format("sound%d/win/music/data/music%03d.bgw", expansion, id);
        return new File(region.getFFXILocation(), filename);
    }
}
