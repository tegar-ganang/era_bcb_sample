package ipodnano;

import interfaces.IWorkout;
import io.ImportWorkouts;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Vector;

/**
 * @author kreed
 *
 */
public class IpodNanoImporter {

    public static Vector<File> getDifference(File[] intern, File[] extern) {
        Vector<File> keep = new Vector<File>();
        for (int i = 0; i < extern.length; i++) {
            keep.add(extern[i]);
        }
        for (int i = 0; i < intern.length; i++) {
            String si = intern[i].getName();
            for (int j = 0; j < keep.size(); j++) {
                String se = keep.get(j).getName();
                if (se.equals(si)) {
                    keep.remove(j);
                }
            }
        }
        return keep;
    }

    /**
	 * Copies given Files to a destination
	 *
	 */
    public static void importHeats(Vector<File> nanoFiles, String destAbsPath) {
        int cnt = 0;
        for (File f : nanoFiles) {
            System.out.println(f.getAbsolutePath());
            String destFile = destAbsPath + File.separatorChar + f.getName();
            copyFile(f.getAbsolutePath(), destFile);
            cnt++;
        }
        if (cnt > 0) {
            System.out.println(cnt + " Heats successfully imported.");
        }
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[0xFFFF];
        for (int len; (len = in.read(buffer)) != -1; ) out.write(buffer, 0, len);
    }

    private static void copyFile(String src, String dest) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(src);
            fos = new FileOutputStream(dest);
            copy(fis, fos);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) try {
                fis.close();
            } catch (IOException e) {
            }
            if (fos != null) try {
                fos.close();
            } catch (IOException e) {
            }
        }
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        File fdir = new File("");
        System.out.println(fdir.getAbsolutePath());
        File hfolder = new File(fdir.getAbsolutePath() + "/examples");
        System.out.println("dir ? " + hfolder.isDirectory());
        File[] fa = hfolder.listFiles();
        try {
            Vector<IWorkout> v = ImportWorkouts.read(fa);
            ArrayList<IWorkout> heatList = new ArrayList<IWorkout>();
            for (IWorkout h : v) heatList.add(h);
            System.out.println(heatList.size() + "Heats loaded...");
        } catch (IOException e) {
            e.printStackTrace();
        }
        IpodNano d = new IpodNano();
        d.deviceScan();
        d.findPersonalDataFolders();
        Vector<File> pid = d.getPersonalPaths();
        System.out.println("Personal Folders :");
        for (File f : pid) {
            System.out.println(f.getName());
            d.setPersonalPath(f);
        }
        System.out.println("Nano Heats :");
        File[] extern = d.getPersonalHeatFileArray();
        Vector<File> diff = getDifference(fa, extern);
        System.out.println("New Heats :");
        for (File f : diff) {
            System.out.println(f.getName());
        }
        String destAbs = fdir.getAbsolutePath() + File.separatorChar + "imptest";
        importHeats(diff, destAbs);
    }
}
