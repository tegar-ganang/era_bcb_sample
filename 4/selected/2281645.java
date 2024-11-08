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

public class SoundEffect {

    private static final Pattern DIRECTORY_PATTERN = Pattern.compile("se(\\d{3})");

    private static final Pattern FILE_PATTERN = Pattern.compile("se(\\d{6}).spw");

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
            if (expansion == 1) filename = String.format("sound/win/se/"); else filename = String.format("sound%d/win/se/", expansion);
            File directory = new File(region.getFFXILocation(), filename);
            if (directory.exists() && directory.isDirectory() && directory.canRead()) {
                File maxDirecory = null;
                for (File f : directory.listFiles()) {
                    if (f.isDirectory()) {
                        Matcher m = DIRECTORY_PATTERN.matcher(f.getName());
                        if (m.matches()) {
                            int id = Integer.parseInt(m.group(1), 10);
                            if (id > max / 1000) maxDirecory = f;
                        }
                    }
                }
                if (maxDirecory != null) {
                    for (String f : maxDirecory.list()) {
                        Matcher m = FILE_PATTERN.matcher(f);
                        if (m.matches()) {
                            int id = Integer.parseInt(m.group(1), 10);
                            if (id > max / 1000) max = id;
                        }
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

    public SoundEffect(int id) {
        this(Region.getDefaultFFXIRegion(), id);
    }

    public SoundEffect(int expansion, int id) {
        this(Region.getDefaultFFXIRegion(), expansion, id);
    }

    public SoundEffect(Region region, int id) {
        if (id < 0) throw new IllegalArgumentException("ID cannot be less than zero: " + id);
        this.region = region;
        this.id = id;
        ffxiPath = region.getFFXILocation().getAbsolutePath();
        for (int expansion = 9; expansion > 0; expansion--) {
            File temp = getFile(region, expansion, id);
            if (temp.exists() && temp.isFile() && temp.canRead()) file = temp;
        }
    }

    public SoundEffect(Region region, int expansion, int id) {
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
        SoundEffect other = (SoundEffect) obj;
        if (region == other.region) return false;
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

    private File getFile(Region region, int expansion, int id) {
        int id2 = id / 1000;
        String filename;
        if (expansion == 1) filename = String.format("sound/win/se/se%03d/se%06d.spw", id2, id); else filename = String.format("sound%d/win/se/se%03d/se%06d.spw", expansion, id2, id);
        return new File(region.getFFXILocation(), filename);
    }
}
