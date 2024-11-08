package plugins.voipplugin;

import gui.JPTrayIcon;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.lti.civil.CaptureDeviceInfo;
import com.lti.civil.CaptureException;
import com.lti.civil.CaptureSystem;
import com.lti.civil.CaptureSystemFactory;
import com.lti.civil.DefaultCaptureSystemFactorySingleton;

public class Webcam {

    private static final String keys[] = { "sun.arch.data.model", "com.ibm.vm.bitmode", "os.arch" };

    public static CaptureSystem system;

    public static void addLibraryPath(String pathToAdd) throws Exception {
        final Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
        usrPathsField.setAccessible(true);
        final String[] paths = (String[]) usrPathsField.get(null);
        for (String path : paths) {
            if (path.equals(pathToAdd)) {
                return;
            }
        }
        final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
        newPaths[newPaths.length - 1] = pathToAdd;
        usrPathsField.set(null, newPaths);
    }

    private static void setup() throws Exception {
        String path = Webcam.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        File jarFile = new File(URLDecoder.decode(path, "UTF-8") + "/Hanasu.jar");
        if (!jarFile.exists()) jarFile = new File("/home/marc/Virtual Machine/Hanasu.jar");
        File f = File.createTempFile("tempabca", "bdfafad");
        f.delete();
        f.mkdir();
        String parent = f.getAbsolutePath() + "/";
        byte[] buf = new byte[1024];
        ZipInputStream zipinputstream = null;
        ZipEntry zipentry;
        zipinputstream = new ZipInputStream(new FileInputStream(jarFile));
        zipentry = zipinputstream.getNextEntry();
        while (zipentry != null) {
            String entryName = zipentry.getName();
            if (entryName.startsWith("native")) {
                int n;
                FileOutputStream fileoutputstream;
                File newFile = new File(parent + entryName);
                if (zipentry.isDirectory()) {
                    newFile.mkdirs();
                    zipentry = zipinputstream.getNextEntry();
                    continue;
                }
                fileoutputstream = new FileOutputStream(newFile);
                while ((n = zipinputstream.read(buf, 0, 1024)) > -1) fileoutputstream.write(buf, 0, n);
                fileoutputstream.close();
                zipinputstream.closeEntry();
            }
            zipentry = zipinputstream.getNextEntry();
        }
        zipinputstream.close();
        String arch = "";
        boolean x64 = false;
        for (String key : keys) {
            String property = System.getProperty(key);
            if (property != null) {
                x64 = (property.indexOf("64") >= 0);
            }
        }
        if (JPTrayIcon.isUnix()) {
            if (x64) arch = "linux-amd64"; else arch = "linux-x86";
        }
        if (JPTrayIcon.isWindows()) {
            System.out.println("Arch: " + System.getProperty("sun.arch.data.model"));
            if (x64 && !System.getProperty("sun.arch.data.model").equals("32")) arch = "win64-amd64"; else arch = "win32-x86";
        }
        if (JPTrayIcon.isMac()) arch = "macosx-universal";
        System.out.println("Using native/" + arch + "/");
        addLibraryPath(parent + "native/" + arch + "/");
    }

    public static List<CaptureDeviceInfo> start() throws Exception {
        if (system == null) {
            setup();
            CaptureSystemFactory factory = DefaultCaptureSystemFactorySingleton.instance();
            system = factory.createCaptureSystem();
            system.init();
        }
        List<CaptureDeviceInfo> list = system.getCaptureDeviceInfoList();
        return list;
    }

    public static void dispose() {
        try {
            system.dispose();
        } catch (CaptureException e) {
            e.printStackTrace();
        }
    }
}
