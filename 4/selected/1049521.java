package trb.trials4k;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Compressor2 {

    static final String PATH_TO_JDK = "C:\\Program Files (x86)\\Java\\jdk1.6.0_19\\bin\\";

    static final String SOURCE_FILE = "C:\\vrdev\\trb\\trials4k\\src\\A.java";

    static final String CLASS_NAME = "A";

    private static String BASE_DIR = new File("C:\\vrdev\\trb\\trials4k\\").getAbsolutePath() + File.separator;

    static final boolean SLOWER_BUT_BETTER____MAYBE = false;

    static final File GENERATED_FILE_BASE = new File("C:\\vrdev\\trb\\trials4k\\tmp");

    static final File DATA_FILE = new File("C:\\vrdev\\trb\\trials4k\\src\\data.bin");

    public static class Text {

        private static final Charset UTF_8 = Charset.forName("UTF-8");

        public static String after(String string, String token) {
            int index = string.indexOf(token);
            return string.substring(index + token.length());
        }

        public static String before(String string, String token) {
            int index = string.indexOf(token);
            return string.substring(0, index);
        }

        public static byte[] utf8(String sourcecode) {
            return sourcecode.getBytes(UTF_8);
        }

        public static String utf8(byte[] byteArray) {
            return new String(byteArray, UTF_8);
        }
    }

    public static class Streams {

        public static void asynchronousTransfer(final InputStream in, final OutputStream out) {
            new Thread() {

                public void run() {
                    transfer(in, out);
                    try {
                        out.close();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }.start();
        }

        public static void transfer(InputStream in, OutputStream out) {
            try {
                byte[] buffer = new byte[1024 * 16];
                int read = 0;
                while ((read = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, read);
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class FileUtil {

        public static void writeFile(File file, byte[] data) {
            try {
                DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
                dos.write(data);
                dos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public static void copyFile(File from, File to) {
            try {
                FileInputStream in = new FileInputStream(from);
                FileOutputStream out = new FileOutputStream(to);
                byte[] buffer = new byte[1024 * 16];
                int read = 0;
                while ((read = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, read);
                }
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public static void deleteDirectory(File dir, boolean recurse) {
            File[] files = dir.listFiles();
            if (recurse) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file, true);
                    }
                }
            }
            for (File file : files) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        }

        public static String read(File from) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(from)));
                StringWriter sw = new StringWriter();
                String line = "";
                while ((line = br.readLine()) != null) {
                    sw.append(line).append("\n");
                }
                br.close();
                sw.close();
                return sw.toString();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public static class ArchiveEntry {

        public final File file;

        public final String name;

        public ArchiveEntry(File file, String name) {
            this.file = file;
            this.name = name;
        }
    }

    public static class Compression {

        public static void zip(List<ArchiveEntry> entries, File file, boolean b) {
            try {
                ZipOutputStream jos = new ZipOutputStream(new FileOutputStream(file));
                jos.setLevel(9);
                for (ArchiveEntry entry : entries) {
                    jos.putNextEntry(new ZipEntry(entry.name));
                    Streams.transfer(new FileInputStream(entry.file), jos);
                    jos.closeEntry();
                }
                jos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void manage(String sourcecode, String className) {
        FileUtil.deleteDirectory(GENERATED_FILE_BASE, true);
        String downloadprefix = className;
        if (!GENERATED_FILE_BASE.exists()) {
            GENERATED_FILE_BASE.mkdir();
        }
        try {
            int reruns = SLOWER_BUT_BETTER____MAYBE ? 8 : 1;
            int maxSplit = SLOWER_BUT_BETTER____MAYBE ? 256 : 4;
            File javaFile = new File(GENERATED_FILE_BASE, className + ".java");
            File classFile = new File(GENERATED_FILE_BASE, className + ".class");
            if (className.equals("download")) {
                throw new IllegalStateException();
            }
            int[] javaVersions = new int[] { 6 };
            String[] jarTypes = new String[] { "normal", "progrd" };
            String[] compressors = new String[] { "kz" };
            try {
                outer: if (true) {
                    if (!source(sourcecode, javaFile)) {
                        break outer;
                    }
                    for (int javaVersion : javaVersions) {
                        if (!javac("javac (java" + javaVersion + ")", GENERATED_FILE_BASE, javaFile, null)) {
                            break outer;
                        }
                        if (!jar("jar (java" + javaVersion + ")", classFile, getTempResourceFilename(className, jarTypes[0], javaVersion, null, "jar"))) {
                            break outer;
                        }
                    }
                    for (int javaVersion : javaVersions) {
                        File normalJar = getTempResourceFilename(className, jarTypes[0], javaVersion, null, "jar");
                        File progrdJar = getTempResourceFilename(className, jarTypes[1], javaVersion, null, "jar");
                        if (!proguard("proguard (java" + javaVersion + ")", normalJar, progrdJar)) {
                            break outer;
                        }
                    }
                    for (int javaVersion : javaVersions) {
                        for (String jarType : jarTypes) {
                            File xJarFile = getTempResourceFilename(className, jarType, javaVersion, null, "jar");
                            File xPackFile = getTempResourceFilename(className, jarType, javaVersion, null, "pack");
                            if (!pack200("pack200 (java" + javaVersion + ")", xJarFile, xPackFile)) {
                                continue;
                            }
                            for (String compressor : compressors) {
                                File file = xPackFile;
                                File toDownload = getDownloadPackGzFilename(downloadprefix, jarType, javaVersion, compressor);
                                boolean result;
                                if (compressor.equals("7z")) {
                                    result = gz_7z("compress:7z (java" + javaVersion + ")", file, toDownload);
                                } else if (compressor.equals("kz")) {
                                    result = fastKzip("compress:kzip (java" + javaVersion + ")", file, toDownload, 0, 4, 64, reruns) != -1;
                                } else if (compressor.equals("bj")) {
                                    result = bruteforceBjwflate("compress:bjwflate (java" + javaVersion + ")", file, toDownload, maxSplit) != -1;
                                } else {
                                    throw new IllegalStateException("unexpected compressor: " + compressor);
                                }
                                if (!result) {
                                    toDownload.delete();
                                }
                            }
                        }
                    }
                    System.out.println("RESULT");
                    System.out.println(className + ".java => " + javaFile.length() + " bytes");
                    System.out.println(className + ".class => " + classFile.length() + " bytes");
                    for (int javaVersion : javaVersions) {
                        for (String jarType : jarTypes) {
                            File xJarFile = getTempResourceFilename(className, jarType, javaVersion, null, "jar");
                            File xPackFile = getTempResourceFilename(className, jarType, javaVersion, null, "pack");
                            System.out.println(xJarFile.getName() + " => " + xJarFile.length() + " bytes");
                            System.out.println(xPackFile.getName() + " => " + xPackFile.length() + " bytes");
                        }
                    }
                    class ResultEntry {

                        File file;

                        String html;
                    }
                    TreeSet<ResultEntry> entries = new TreeSet<ResultEntry>(new Comparator<ResultEntry>() {

                        public int compare(ResultEntry a, ResultEntry b) {
                            if (a.file.length() < b.file.length()) {
                                return -1;
                            }
                            if (a.file.length() > b.file.length()) {
                                return +1;
                            }
                            if (a.file.lastModified() < b.file.lastModified()) {
                                return -1;
                            }
                            if (a.file.lastModified() > b.file.lastModified()) {
                                return +1;
                            }
                            if (a.html.hashCode() < b.html.hashCode()) {
                                return -1;
                            }
                            return 1;
                        }
                    });
                    for (int javaVersion : javaVersions) {
                        for (String jarType : jarTypes) {
                            for (String compressor : compressors) {
                                File packGzFile = getDownloadPackGzFilename(downloadprefix, jarType, javaVersion, compressor);
                                String htmlEntry = "";
                                htmlEntry += packGzFile.getName() + " -> " + packGzFile.length() + " bytes";
                                ResultEntry entry = new ResultEntry();
                                entry.file = packGzFile;
                                entry.html = htmlEntry;
                                entries.add(entry);
                            }
                        }
                    }
                    for (ResultEntry entry : entries) {
                        System.out.println(entry.html);
                    }
                }
            } finally {
                javaFile.delete();
                classFile.delete();
                for (int javaVersion : javaVersions) {
                    for (String jarType : jarTypes) {
                        File xJarFile = getTempResourceFilename(className, jarType, javaVersion, null, "jar");
                        File xPackFile = getTempResourceFilename(className, jarType, javaVersion, null, "pack");
                        xPackFile.delete();
                    }
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    private static File getTempResourceFilename(String className, String jarType, int javaVersion, String compressorIfAny, String extension) {
        String filename = className + "." + jarType + ".java" + javaVersion + (compressorIfAny == null ? "" : ("." + compressorIfAny)) + "." + extension;
        return new File(GENERATED_FILE_BASE, filename);
    }

    private static File getDownloadPackGzFilename(String downloadprefix, String jarType, int javaVersion, String compressor) {
        String filename = "play." + downloadprefix + "." + jarType + "." + compressor + ".pack.gz";
        return new File(GENERATED_FILE_BASE, filename);
    }

    private static boolean source(String sourcecode, File javaFile) {
        try {
            FileUtil.writeFile(javaFile, Text.utf8(sourcecode));
            return true;
        } catch (Exception exc) {
            System.out.println("EXCEPTION: " + exc.getClass().getName() + "");
            exc.printStackTrace();
            return false;
        }
    }

    private static boolean javac(String caption, File base, File javaFile, String alternativeBootClasspath) {
        try {
            List<String> cmds = new ArrayList<String>();
            cmds.add(PATH_TO_JDK + "javac");
            cmds.add("-nowarn");
            cmds.add("-target");
            cmds.add("1.5");
            if (alternativeBootClasspath != null) {
                cmds.add("-bootclasspath");
                cmds.add(alternativeBootClasspath);
            }
            cmds.add("-g:none");
            cmds.add("-sourcepath");
            cmds.add(base.getAbsolutePath());
            cmds.add("-d");
            cmds.add(base.getAbsolutePath());
            cmds.add(javaFile.getAbsolutePath());
            Process proc = Runtime.getRuntime().exec(cmds.toArray(new String[cmds.size()]));
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            Streams.asynchronousTransfer(proc.getInputStream(), stdout);
            Streams.asynchronousTransfer(proc.getErrorStream(), stderr);
            int exit = proc.waitFor();
            System.out.println("" + caption + "");
            if (stderr.size() != 0) {
                System.out.println("" + Text.utf8(stdout.toByteArray()) + "");
                System.out.println("" + Text.utf8(stderr.toByteArray()) + "");
            }
            if (exit != 0) {
                System.out.println("Exit value: " + exit + "");
            }
            if (exit != 0) {
                return false;
            }
            return true;
        } catch (Exception exc) {
            System.out.println("EXCEPTION: " + exc.getClass().getName() + "");
            exc.printStackTrace();
            return false;
        }
    }

    private static boolean jar(String caption, File classFile, File jarFile) {
        try {
            System.out.println("" + caption + "");
            List<ArchiveEntry> entries = new ArrayList<ArchiveEntry>();
            entries.add(new ArchiveEntry(classFile, classFile.getName()));
            entries.add(new ArchiveEntry(DATA_FILE, DATA_FILE.getName()));
            Compression.zip(entries, jarFile, true);
            return true;
        } catch (Exception exc) {
            System.out.println("EXCEPTION: " + exc.getClass().getName() + "");
            exc.printStackTrace();
            return false;
        }
    }

    private static boolean proguard(String caption, File normalJarFile, File progrdJarFile) {
        System.out.println("proguard");
        try {
            String[] cmds = new String[9];
            cmds[0] = PATH_TO_JDK + "java";
            cmds[1] = "-jar";
            cmds[2] = BASE_DIR + "tools/proguard.jar";
            cmds[3] = "-include";
            cmds[4] = BASE_DIR + "tools/proguard.conf";
            cmds[5] = "-injars";
            cmds[6] = normalJarFile.getAbsolutePath();
            cmds[7] = "-outjars";
            cmds[8] = progrdJarFile.getAbsolutePath();
            Process proc = Runtime.getRuntime().exec(cmds);
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            Streams.asynchronousTransfer(proc.getInputStream(), stdout);
            Streams.asynchronousTransfer(proc.getErrorStream(), stderr);
            int exit = proc.waitFor();
            System.out.println("" + caption + "");
            if (stderr.size() != 0) {
                System.out.println("" + Text.utf8(stdout.toByteArray()) + "");
                System.out.println("" + Text.utf8(stderr.toByteArray()) + "");
            }
            if (exit != 0) {
                System.out.println("Exit value: " + exit + "");
            }
            if (exit != 0) {
                return false;
            }
            return true;
        } catch (Exception exc) {
            System.out.println("EXCEPTION: " + exc.getClass().getName() + "");
            exc.printStackTrace();
            return false;
        }
    }

    private static boolean pack200(String caption, File jarFile, File packFile) {
        System.out.println("pack200");
        try {
            String[] cmds = new String[7];
            cmds[0] = PATH_TO_JDK + "pack200";
            cmds[1] = "--no-gzip";
            cmds[2] = "--strip-debug";
            cmds[3] = "--no-keep-file-order";
            cmds[4] = "--effort=9";
            cmds[5] = packFile.getAbsolutePath();
            cmds[6] = jarFile.getAbsolutePath();
            Process proc = Runtime.getRuntime().exec(cmds);
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            Streams.asynchronousTransfer(proc.getInputStream(), stdout);
            Streams.asynchronousTransfer(proc.getErrorStream(), stderr);
            int exit = proc.waitFor();
            System.out.println("" + caption + "");
            if (stderr.size() != 0) {
                System.out.println("" + Text.utf8(stdout.toByteArray()) + "");
                System.out.println("" + Text.utf8(stderr.toByteArray()) + "");
            }
            if (exit != 0) {
                System.out.println("Exit value: " + exit + "");
            }
            if (exit != 0) {
                return false;
            }
            return true;
        } catch (Exception exc) {
            System.out.println("EXCEPTION: " + exc.getClass().getName() + "");
            exc.printStackTrace();
            return false;
        }
    }

    private static int fastKzip(String caption, File packFile, File packGzFile, int splitOffset, int splitStep, int steps, int reruns) throws IOException {
        File min = packGzFile;
        File dst = new File(packGzFile.getParentFile(), 0 + "_" + packGzFile.getName());
        int minSplit = -1;
        int split = 116;
        if (kzip_deflopt_gz(caption, packFile, dst, split, true)) {
            if (dst.length() != 0 && ((min == null || min.length() == 0) || dst.length() < min.length())) {
                min = dst;
                minSplit = split;
            }
        }
        if (minSplit != -1) {
            FileUtil.copyFile(min, packGzFile);
        }
        dst.delete();
        return minSplit;
    }

    private static int bruteforceKzip(String caption, File packFile, File packGzFile, int splitOffset, int splitStep, int steps, int reruns) throws IOException {
        File min = packGzFile;
        File[] dst = new File[steps];
        int minSplit = -1;
        for (int i = 0; i < dst.length; i++) {
            dst[i] = new File(packGzFile.getParentFile(), i + "_" + packGzFile.getName());
            int split = splitOffset + i * splitStep;
            for (int k = 0; k < reruns; k++) {
                if (kzip_deflopt_gz(caption, packFile, dst[i], split, k != 0)) {
                    if (dst[i].length() != 0 && ((min == null || min.length() == 0) || dst[i].length() < min.length())) {
                        min = dst[i];
                        minSplit = split;
                    }
                }
            }
        }
        if (minSplit != -1) {
            FileUtil.copyFile(min, packGzFile);
            System.out.println("minsplit: " + minSplit);
        }
        for (int i = 0; i < dst.length; i++) {
            dst[i].delete();
        }
        return minSplit;
    }

    private static int bruteforceBjwflate(String caption, File packFile, File packGzFile, int maxSplit) throws IOException {
        File min = packGzFile;
        int minSplit = -1;
        List<File> dsts = new ArrayList<File>();
        for (int splitSize = 4, i = 0; splitSize <= maxSplit; splitSize += (splitSize <= 12 ? 4 : (splitSize <= 32 ? 8 : (splitSize <= 128 ? 16 : 32))), i++) {
            File dst = new File(packGzFile.getParentFile(), splitSize + "_" + packGzFile.getName());
            if (bjwflate_deflopt_gz(caption, packFile, dst, splitSize, false)) {
                if (dst.length() != 0 && ((min == null || min.length() == 0) || dst.length() < min.length())) {
                    min = dst;
                    minSplit = splitSize;
                }
            }
        }
        if (minSplit != -1) {
            FileUtil.copyFile(min, packGzFile);
        }
        for (File dst : dsts) {
            dst.delete();
        }
        return minSplit;
    }

    public static boolean kzip_deflopt_gz(String caption, File src, File dst, int splitSize, boolean rn) {
        System.out.println("kzip:" + splitSize + " @ " + System.currentTimeMillis());
        File zip = new File(src.getParentFile(), src.getName() + ".zip");
        File dir = new File(src.getParentFile(), "tmp_" + System.nanoTime());
        dir.mkdir();
        try {
            try {
                FileUtil.copyFile(src, new File(dir, src.getName()));
                String[] cmds = new String[rn ? 5 : 4];
                cmds[0] = BASE_DIR + "tools/kzip.exe";
                cmds[1] = "-y";
                cmds[2] = "-b" + splitSize;
                if (rn) {
                    cmds[3] = "-rn";
                }
                cmds[rn ? 4 : 3] = zip.getAbsolutePath();
                final Process proc = Runtime.getRuntime().exec(cmds, null, dir);
                ByteArrayOutputStream stdout = new ByteArrayOutputStream();
                ByteArrayOutputStream stderr = new ByteArrayOutputStream();
                Streams.asynchronousTransfer(proc.getInputStream(), stdout);
                Streams.asynchronousTransfer(proc.getErrorStream(), stderr);
                int exit = proc.waitFor();
                if (splitSize == 0 && !rn) {
                    System.out.println("" + caption + "");
                }
                if (stderr.size() != 0) {
                    System.out.println("" + Text.utf8(stdout.toByteArray()) + "");
                    System.out.println("" + Text.utf8(stderr.toByteArray()) + "");
                }
                if (exit != 0) {
                    System.out.println("Exit value: " + exit + "");
                }
                if (exit != 0) {
                    return false;
                }
            } catch (Exception exc) {
                System.out.println("EXCEPTION: " + exc.getClass().getName() + "");
                exc.printStackTrace();
                return false;
            }
            try {
                zip2gz(new FileInputStream(zip), new FileOutputStream(dst));
            } catch (IOException exc) {
                System.out.println("EXCEPTION: " + exc.getClass().getName() + "");
                exc.printStackTrace();
                return false;
            }
            return true;
        } finally {
            zip.delete();
            FileUtil.deleteDirectory(dir, true);
        }
    }

    public static boolean bjwflate_deflopt_gz(String caption, File src, File dst, int splitSize, boolean noprep) {
        System.out.println("bjwflate:" + splitSize + " @ " + System.currentTimeMillis());
        File zip = new File(src.getParentFile(), src.getName() + ".zip");
        try {
            try {
                String[] cmds = new String[noprep ? 5 : 4];
                cmds[0] = BASE_DIR + "tools/BJWFlate.exe";
                cmds[1] = "-y";
                if (noprep) {
                    cmds[2] = "-n";
                }
                cmds[noprep ? 3 : 2] = zip.getAbsolutePath();
                cmds[noprep ? 4 : 3] = src.getAbsolutePath();
                final Process proc = Runtime.getRuntime().exec(cmds, null);
                ByteArrayOutputStream stdout = new ByteArrayOutputStream();
                ByteArrayOutputStream stderr = new ByteArrayOutputStream();
                Streams.asynchronousTransfer(proc.getInputStream(), stdout);
                Streams.asynchronousTransfer(proc.getErrorStream(), stderr);
                int exit = proc.waitFor();
                if (splitSize == 4) {
                    System.out.println("" + caption + "");
                }
                if (stderr.size() != 0) {
                    System.out.println("" + Text.utf8(stdout.toByteArray()) + "");
                    System.out.println("" + Text.utf8(stderr.toByteArray()) + "");
                }
                if (exit != 0) {
                    System.out.println("Exit value: " + exit + "");
                }
                if (exit != 0) {
                    return false;
                }
            } catch (Exception exc) {
                System.out.println("EXCEPTION: " + exc.getClass().getName() + "");
                exc.printStackTrace();
                return false;
            }
            {
            }
            try {
                zip2gz(new FileInputStream(zip), new FileOutputStream(dst));
            } catch (IOException exc) {
                System.out.println("EXCEPTION: " + exc.getClass().getName() + "");
                exc.printStackTrace();
                return false;
            }
            return true;
        } finally {
            zip.delete();
        }
    }

    public static boolean gz_7z(String caption, File src, File dst) {
        System.out.println("7z");
        try {
            String[] cmds = new String[4];
            cmds[0] = BASE_DIR + "tools/7za.exe";
            cmds[1] = "a";
            cmds[2] = dst.getAbsolutePath();
            cmds[3] = src.getAbsolutePath();
            Process proc = Runtime.getRuntime().exec(cmds);
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            Streams.asynchronousTransfer(proc.getInputStream(), stdout);
            Streams.asynchronousTransfer(proc.getErrorStream(), stderr);
            int exit = proc.waitFor();
            System.out.println("" + caption + "");
            if (stderr.size() != 0) {
                System.out.println("" + Text.utf8(stdout.toByteArray()) + "");
                System.out.println("" + Text.utf8(stderr.toByteArray()) + "");
            }
            if (exit != 0) {
                System.out.println("Exit value: " + exit + "");
            }
            if (exit != 0) {
                return false;
            }
            return true;
        } catch (Exception exc) {
            System.out.println("EXCEPTION: " + exc.getClass().getName() + "");
            exc.printStackTrace();
            return false;
        }
    }

    public static void zip2gz(InputStream in, OutputStream out) throws IOException {
        in = new BufferedInputStream(in);
        out = new BufferedOutputStream(out);
        out.write(0x1f);
        out.write(0x8b);
        out.write(0x08);
        out.write(0x00);
        out.write(0x00);
        out.write(0x00);
        out.write(0x00);
        out.write(0x00);
        out.write(0x00);
        out.write(0xff);
        for (int i = 0; i < 14; i++) {
            in.read();
        }
        int crc1 = in.read();
        int crc2 = in.read();
        int crc3 = in.read();
        int crc4 = in.read();
        int cmpSz = (in.read() & 0xff) + ((in.read() & 0xff) << 8) + ((in.read() & 0xff) << 16) + ((in.read() & 0xff) << 24);
        int ucmpSz1 = in.read();
        int ucmpSz2 = in.read();
        int ucmpSz3 = in.read();
        int ucmpSz4 = in.read();
        int nameLen = (in.read() & 0xff) + ((in.read() & 0xff) << 8);
        int xfLen = (in.read() & 0xff) + ((in.read() & 0xff) << 8);
        for (int i = 0; i < nameLen; i++) {
            in.read();
        }
        for (int i = 0; i < xfLen; i++) {
            in.read();
        }
        byte[] buf = new byte[4096];
        while (cmpSz > 0) {
            int desired = cmpSz > buf.length ? buf.length : cmpSz;
            int len = in.read(buf, 0, desired);
            if (len == 0) {
                throw new EOFException();
            }
            out.write(buf, 0, len);
            cmpSz -= len;
        }
        out.write(crc1);
        out.write(crc2);
        out.write(crc3);
        out.write(crc4);
        out.write(ucmpSz1);
        out.write(ucmpSz2);
        out.write(ucmpSz3);
        out.write(ucmpSz4);
        out.close();
        in.close();
    }

    public static void main(String[] args) {
        new Compressor2().manage(FileUtil.read(new File(SOURCE_FILE)), CLASS_NAME);
    }
}
