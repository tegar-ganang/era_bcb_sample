package fr.ens.transcriptome.teolenn.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import fr.ens.transcriptome.teolenn.Globals;

public class BinariesInstaller {

    private static Logger logger = Logger.getLogger(Globals.APP_NAME);

    private static final int BUFFER_SIZE = 32 * 1024;

    private static void install(final String inputPath, final String file, final String outputPath) throws FileNotFoundException, IOException {
        if (new File(outputPath, file).isFile()) {
            logger.fine(file + " is allready installed.");
            return;
        }
        final String resourcePath = inputPath.toLowerCase() + "/" + file;
        final InputStream is = BinariesInstaller.class.getResourceAsStream(resourcePath);
        if (is == null) throw new FileNotFoundException("Unable to find the correct resource (" + resourcePath + ")");
        final File outputDir = new File(outputPath);
        if (!outputDir.isDirectory()) if (!outputDir.mkdirs()) throw new IOException("Can't create directory for binaries installation: " + outputDir.getAbsolutePath());
        final File outputFile = new File(outputDir, file);
        OutputStream fos = new FileOutputStream(outputFile);
        byte[] buf = new byte[BUFFER_SIZE];
        int i = 0;
        while ((i = is.read(buf)) != -1) fos.write(buf, 0, i);
        is.close();
        fos.close();
        FileUtils.setExecutable(outputFile, false);
        FileUtils.setReadable(outputFile, false);
    }

    public static String install(final String file) throws IOException {
        final String os = System.getProperty("os.name");
        final String arch = System.getProperty("os.arch");
        logger.fine("Try to install \"" + file + "\" for " + os + " (" + arch + ")");
        final boolean linux = SystemUtils.isLinux();
        final boolean macos = SystemUtils.isMacOsX();
        if (!(linux || macos)) throw new FileNotFoundException("There is no executable for your plateform (" + os + ") included in " + Globals.APP_NAME);
        if (linux && !("i386".equals(arch) || "amd64".equals(arch))) throw new FileNotFoundException("There is no executable for your architecture (" + arch + ") included in " + Globals.APP_NAME);
        final String inputPath = "/" + os.replace(" ", "") + "/" + arch;
        final String outputPath = "/tmp/" + Globals.APP_NAME_LOWER_CASE + "/" + Globals.APP_VERSION;
        if (new File(outputPath, file).isFile()) {
            logger.info(file + " is allready installed.");
            return outputPath + "/" + file;
        }
        install(inputPath, file, outputPath);
        logger.fine("Successful installation of " + file + " in " + outputPath);
        return outputPath + "/" + file;
    }

    public static void main(String[] args) throws IOException {
        install("gt");
        install("soap");
    }
}
