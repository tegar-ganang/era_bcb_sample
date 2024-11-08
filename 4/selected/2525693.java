package niskala.sej;

import org.apache.commons.io.IOUtils;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 */
public class Sej {

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            usage(System.out);
            System.exit(1);
        }
        final File tmpFile = File.createTempFile("sej", null);
        tmpFile.deleteOnExit();
        final FileOutputStream destination = new FileOutputStream(tmpFile);
        final String mainClass = args[1];
        final Collection jars = new LinkedList();
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            jars.add(arg);
        }
        JarInterpretted interpretted = new JarInterpretted(destination);
        JarCat rowr = new JarCat(destination, createManifest(mainClass), jars);
        interpretted.write();
        rowr.write();
        destination.close();
        final File finalDestinationFile = new File(args[0]);
        final FileOutputStream finalDestination = new FileOutputStream(finalDestinationFile);
        IOUtils.copy(new FileInputStream(tmpFile), finalDestination);
        finalDestination.close();
        Chmod chmod = new Chmod("a+rx", new File[] { finalDestinationFile });
        chmod.invoke();
    }

    public static void usage(PrintStream out) {
        out.println("USAGE: sej <destinationFile> <mainClass> <jar1> [ <jar2> ... ]");
    }

    protected static Manifest createManifest(String mainClass) {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MAIN_CLASS, mainClass);
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        return manifest;
    }
}
