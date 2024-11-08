package net.sourceforge.plantuml.eps;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import net.sourceforge.plantuml.FileUtils;
import net.sourceforge.plantuml.OptionFlags;

public class SvgToEpsConverter {

    private final Inkscape inkscape;

    private final File svgFile;

    public SvgToEpsConverter(String svg) throws IOException {
        if (svg == null) {
            throw new IllegalArgumentException();
        }
        this.inkscape = InkscapeUtils.create();
        this.svgFile = FileUtils.createTempFile("convert", ".svg");
        final PrintWriter pw = new PrintWriter(svgFile);
        pw.println(svg);
        pw.close();
    }

    public SvgToEpsConverter(File svgFile) {
        if (svgFile == null) {
            throw new IllegalArgumentException();
        }
        this.inkscape = InkscapeUtils.create();
        this.svgFile = svgFile;
    }

    public void createEps(File epsFile) throws IOException, InterruptedException {
        inkscape.createEps(svgFile, epsFile);
    }

    public void createEps(OutputStream os) throws IOException, InterruptedException {
        final File epsFile = FileUtils.createTempFile("eps", ".eps");
        createEps(epsFile);
        int read;
        final InputStream is = new FileInputStream(epsFile);
        while ((read = is.read()) != -1) {
            os.write(read);
        }
        is.close();
        if (OptionFlags.getInstance().isKeepTmpFiles() == false) {
            epsFile.delete();
        }
    }
}
