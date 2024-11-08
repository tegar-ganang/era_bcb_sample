package c.tools;

import c.Main;
import c.programming_tools.BadFileDataException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sam
 */
public class MapFormatFix {

    private static String getNewEntryString(String old) {
        String parts[] = old.split(",");
        if (parts.length != 4) throw new BadFileDataException("Unexpected number of parts in map file entry (Should be 4 instead of " + parts.length);
        return parts[0] + "," + parts[1] + "," + parts[2] + ",";
    }

    public static void fixMapFile(String filestr) throws IOException {
        File mapfile = new File(filestr);
        File tempmapfile = new File(filestr + "temp");
        if (!mapfile.isAbsolute()) throw new IllegalArgumentException(filestr + " is not a valid file");
        tempmapfile.createNewFile();
        Scanner scan = new Scanner(mapfile);
        PrintStream ps = new PrintStream(tempmapfile);
        ps.println(scan.nextLine());
        String line;
        String parts[];
        while (scan.hasNextLine()) {
            line = scan.nextLine();
            parts = line.split(";");
            String newent;
            for (int i = 0; i < parts.length; i++) {
                newent = getNewEntryString(parts[i]);
                ps.print(newent + ";");
            }
            ps.append("\n");
        }
        mapfile.delete();
        tempmapfile.renameTo(mapfile);
    }

    public static void fixMapFiles(String dir) {
        File[] maps = new File(dir).listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return (file.getName().endsWith(".map"));
            }
        });
        for (File f : maps) {
            try {
                fixMapFile(f.getPath());
            } catch (IOException ex) {
                System.out.println("Failed to fix map file " + f.getPath());
            }
        }
    }

    public static void insertVersionNumber(File mapfile) throws IOException {
        Scanner scan = new Scanner(mapfile);
        String line = scan.nextLine();
        if (line.startsWith("version")) {
            String ver;
            if (line.startsWith("version;") && ((ver = line.split(";")[1]).compareTo(Main.GAME_VERSION) == 0)) return; else line = scan.nextLine();
        }
        File tempmapfile = new File(mapfile.getPath() + "temp");
        tempmapfile.createNewFile();
        PrintStream ps = new PrintStream(tempmapfile);
        ps.print("version;" + Main.GAME_VERSION + ";\n");
        ps.println(line);
        while (scan.hasNextLine()) {
            line = scan.nextLine();
            ps.println(line);
        }
        ps.flush();
        ps.close();
        mapfile.delete();
        tempmapfile.renameTo(mapfile);
    }

    public static void insertVersionNumberToWorld(File worlddir) {
        File mapfiles[] = worlddir.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                return (pathname.getName().endsWith(".map"));
            }
        });
        for (File f : mapfiles) {
            try {
                insertVersionNumber(f);
            } catch (IOException ex) {
                System.out.println("Failed to version map file " + f.getPath());
            }
        }
    }

    public static void main(String[] args) throws IOException {
        insertVersionNumberToWorld(new File("/Volumes/UNTITLED/Programming/code/java/NetBeansProjects/Organics7/files/worlds/TestWorld"));
    }
}
