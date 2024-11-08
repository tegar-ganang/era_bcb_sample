package de.andreavicentini.mehlsaecke.reader;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import de.andreavicentini.mehlsaecke.domain.SackDomain;
import de.andreavicentini.mehlsaecke.domain.SackImage;

public class SackDetailsExtractor {

    private final SackDomain domain;

    public static void main(String args[]) throws Exception {
        final SackDomain domain = new SackDomain().loadFrom(args[0]);
        new SackDetailsExtractor(domain).listDetails(new File(args[1]), new File(args[2]));
    }

    public SackDetailsExtractor(SackDomain domain) {
        this.domain = domain;
    }

    public void listDetails(File root, File destDir) throws IOException {
        long l1 = System.currentTimeMillis();
        destDir.mkdirs();
        for (SackImage image : this.domain.images().list()) if (image.type().get().isDetail()) FileUtils.copyFileToDirectory(new File(root, image.originalName()), destDir);
        System.out.println("Elapsed time=" + (System.currentTimeMillis() - l1) + " ms");
    }
}
