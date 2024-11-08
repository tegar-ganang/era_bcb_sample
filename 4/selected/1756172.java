package net.sf.mavensynapseplugin.boundary;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

public class FileUtilsBoundary {

    public void copyFileToDirectory(File file, File outputDirectory) {
        try {
            FileUtils.copyFileToDirectory(file, outputDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
