package sourceforge.shinigami.maps;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import sourceforge.shinigami.io.FileFormatUnsupportedException;
import sourceforge.shinigami.io.MapFile;
import sourceforge.shinigami.io.SIOException;
import static java.lang.Integer.parseInt;

public class MapScope {

    private static final int VERSION_RELEASE = 1;

    private static final int VERSION_UPDATE = 2;

    private String mapName = "";

    private MapFile file = null;

    private File bg = null;

    private int bgx = 0, bgy = 0;

    private File bgm = null;

    private int width = 0, height = 0;

    private int tw = 0, th = 0;

    private boolean[][][] passage;

    private LinkedList<PortalScope> portals = new LinkedList<PortalScope>();

    public void setMapName(String name) {
        mapName = name;
    }

    public void setBackgroundFile(File f) {
        bg = f;
    }

    public void setBGM(String s) {
        this.bgm = new File(s);
    }

    public void setBGMFile(File f) {
        this.bgm = f;
    }

    public void setBackgroundAdjust(int bgx, int bgy) {
        this.bgx = bgx;
        this.bgy = bgy;
    }

    public void setTileSize(int width, int height) {
        this.tw = width;
        this.th = height;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        passage = new boolean[width][height][Direction.AMOUNT];
    }

    public void setPassage(int tx, int ty, int d, boolean set) {
        passage[tx][ty][d] = set;
    }

    public void addPortal(PortalScope s) {
        portals.add(s);
    }

    public void removePortal(PortalScope s) {
        portals.remove(s);
    }

    public String getMapName() {
        return mapName;
    }

    public File getBackgroundFile() {
        return bg;
    }

    public File getBGM() {
        return this.bgm;
    }

    public int getBackgroundAdjustX() {
        return bgx;
    }

    public int getBackgroundAdjustY() {
        return bgy;
    }

    public int getTileWidth() {
        return tw;
    }

    public int getTileHeight() {
        return th;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean[][][] getPassage() {
        return passage.clone();
    }

    public boolean getPassage(int tx, int ty, int d) {
        return passage[tx][ty][d];
    }

    public LinkedList<PortalScope> getPortals() {
        LinkedList<PortalScope> c = new LinkedList<PortalScope>();
        for (PortalScope ps : portals) c.add(ps);
        return c;
    }

    public static MapScope createScope(MapFile file) {
        MapFile.Handler h = file.getHandler();
        MapScope scope = new MapScope();
        scope.file = file;
        h.resetCaret();
        String line = h.getNextLine();
        while (line != null) {
            scope.loadCommand(line);
            line = h.getNextLine();
        }
        return scope;
    }

    private void loadCommand(String s) {
        String[] line = s.split(" ");
        if (line.length == 0) return;
        for (int i = 0; i < line.length; i++) line[i] = line[i].trim();
        if (line[0].startsWith("#")) return; else if (line[0].equals("VERSION")) {
            if (parseInt(line[1]) > MapScope.VERSION_RELEASE) throw new FileFormatUnsupportedException("This program version" + " is inferior to the one in the file " + file + ". The" + " file could not be loaded.");
            if (parseInt(line[2]) > MapScope.VERSION_UPDATE) System.err.println("This program version is inferior to the one" + " in the file " + file + ". The file can still be loaded," + " but some of it may not work.");
        } else if (line[0].equals("INFO")) {
            this.mapName = line[1];
            this.width = parseInt(line[2]);
            this.height = parseInt(line[3]);
            this.tw = Map.DEFAULT_TILE_WIDTH;
            this.th = Map.DEFAULT_TILE_HEIGHT;
            if (line.length > 4) {
                if (!line[4].startsWith("#")) {
                    this.tw = parseInt(line[4]);
                    this.th = parseInt(line[5]);
                }
            }
            this.passage = new boolean[width][height][Direction.AMOUNT];
            for (int i = 0; i < width; i++) for (int k = 0; k < height; k++) for (int d = 0; d < Direction.AMOUNT; d++) passage[i][k][d] = true;
        } else if (line[0].equals("BGIMAGE")) {
            this.bgx = parseInt(line[1]);
            this.bgy = parseInt(line[2]);
            this.bg = new File(this.file.getParent() + File.separator + line[3]);
        } else if (line[0].equals("ACCESS")) {
            boolean set = false;
            if (line[4].equalsIgnoreCase("true")) set = true; else if (line[4].equalsIgnoreCase("false")) set = false; else return;
            Direction dir = null;
            if (line[3].equals("NORTH")) dir = Direction.NORTH; else if (line[3].equals("SOUTH")) dir = Direction.SOUTH; else if (line[3].equals("EAST")) dir = Direction.EAST; else if (line[3].equals("WEST")) dir = Direction.WEST;
            this.passage[parseInt(line[1])][parseInt(line[2])][dir.getID()] = set;
        } else if (line[0].equals("PORTAL")) {
            this.portals.add(new PortalScope(line[1], line[4], parseInt(line[2]), parseInt(line[3]), parseInt(line[5]), parseInt(line[6])));
        } else if (line[0].equals("BGM")) {
            this.bgm = new File(this.file.getParent() + File.separator + line[1]);
        }
    }

    public synchronized void createFile(MapFile f) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(f));
            String[] parts = bg.getName().split("\\.");
            String bgImage = mapName + ".shmap." + parts[parts.length - 1];
            out.write("VERSION " + MapScope.VERSION_RELEASE + " " + MapScope.VERSION_UPDATE + "\n\n");
            out.write("# Creates basic map definitions\n");
            out.write("INFO " + this.mapName + " " + this.width + " " + this.height + " " + this.tw + " " + this.th + "\n");
            out.write("BGIMAGE " + this.bgx + " " + this.bgy + " " + bgImage + "\n");
            out.write("\n");
            String[] mparts = null;
            String bgMusic = null;
            if (this.bgm != null) {
                mparts = bgm.getName().split("\\.");
                bgMusic = mapName + ".shmap." + mparts[mparts.length - 1];
                out.write("# Creates Background Music\n");
                out.write("BGM " + bgMusic + "\n");
                out.write("\n");
            }
            int pn = 0;
            out.write("# Creates portals\n");
            for (PortalScope p : portals) out.write("PORTAL p" + (pn++) + " " + p.TX + " " + p.TY + " " + p.TARGET + " " + p.TTX + " " + p.TTY + "\n");
            out.write("\n");
            out.write("# Creates accessibility settings\n");
            for (int i = 0; i < passage.length; i++) for (int k = 0; k < passage[0].length; k++) {
                if (!passage[i][k][Direction.NORTH.getID()]) out.write("ACCESS " + i + " " + k + " NORTH " + passage[i][k][Direction.NORTH.getID()] + "\n");
                if (!passage[i][k][Direction.SOUTH.getID()]) out.write("ACCESS " + i + " " + k + " SOUTH " + passage[i][k][Direction.SOUTH.getID()] + "\n");
                if (!passage[i][k][Direction.EAST.getID()]) out.write("ACCESS " + i + " " + k + " EAST " + passage[i][k][Direction.EAST.getID()] + "\n");
                if (!passage[i][k][Direction.WEST.getID()]) out.write("ACCESS " + i + " " + k + " WEST " + passage[i][k][Direction.WEST.getID()] + "\n");
            }
            out.close();
            File temp = File.createTempFile("shmapBG", "tempFile");
            MapScope.cloneFile(bg, temp);
            File output = new File(f.getParent() + File.separator + bgImage);
            MapScope.cloneFile(temp, output);
            if (bgm != null) {
                temp = File.createTempFile("shmapBGM", "tempFile");
                MapScope.cloneFile(bgm, temp);
                output = new File(f.getParent() + File.separator + bgMusic);
                MapScope.cloneFile(temp, output);
            }
        } catch (IOException e) {
            throw new SIOException(e);
        }
    }

    private static final void cloneFile(File origin, File target) throws IOException {
        FileChannel srcChannel = null;
        FileChannel destChannel = null;
        try {
            srcChannel = new FileInputStream(origin).getChannel();
            destChannel = new FileOutputStream(target).getChannel();
            destChannel.transferFrom(srcChannel, 0, srcChannel.size());
        } finally {
            if (srcChannel != null) srcChannel.close();
            if (destChannel != null) destChannel.close();
        }
    }

    public static class PortalScope {

        public final String ID, TARGET;

        public final int TX, TY;

        public final int TTX, TTY;

        public PortalScope(String id, String target, int tx, int ty, int ttx, int tty) {
            this.ID = id;
            this.TARGET = target;
            this.TX = tx;
            this.TY = ty;
            this.TTX = ttx;
            this.TTY = tty;
        }
    }
}
