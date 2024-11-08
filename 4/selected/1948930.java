package org.nmc.pachyderm.foundation;

import com.webobjects.foundation.*;
import com.webobjects.eocontrol.*;
import java.util.*;
import java.util.zip.*;
import java.net.*;
import java.io.*;

public class PXCompileZipArchive extends PXBuildPhase {

    public PXCompileZipArchive(NSDictionary archive) {
        super(archive);
    }

    public void executeInContext(PXBuildContext context) {
        System.out.println("PXCompileZipArchive.executeInContext() executing\n");
        PXBundle bundle = context.bundle();
        URL bundleURL = bundle.bundleURL();
        String bundleDirectoryName = bundleURL.getPath();
        if (bundleURL.getProtocol().equals("file")) {
            try {
                String presentationDirectory = bundleURL.toString();
                presentationDirectory = presentationDirectory.substring(5, presentationDirectory.length() - 1);
                File presentationDirectoryFile = new File(presentationDirectory);
                String zipFilename = presentationDirectory + ".zip";
                String targetDirectory = (String) PXUtility.defaultValueForKey("PublishedPresentationDirectory");
                FileOutputStream dest = new FileOutputStream(zipFilename);
                ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
                zipDir(presentationDirectoryFile, out, presentationDirectoryFile.getPath(), context);
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
                NSDictionary info = new NSDictionary(new Object[] { e.getMessage() }, new String[] { NSError.LocalizedDescriptionKey });
                NSError message = new NSError("Pachyderm Build Domain", -1, info);
                context.appendBuildMessage(message);
            }
        }
    }

    private void zipDir(File zipDir, ZipOutputStream zos, String startingPath, PXBuildContext context) {
        try {
            String[] dirList = zipDir.list();
            byte[] readBuffer = new byte[2156];
            int bytesIn = 0;
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                String entryPath = f.getPath();
                if (entryPath.startsWith(startingPath)) {
                    entryPath = entryPath.substring(startingPath.length(), entryPath.length());
                }
                if (f.isDirectory()) {
                    String filePath = entryPath;
                    zipDir(f, zos, startingPath, context);
                    continue;
                }
                FileInputStream fis = new FileInputStream(f);
                ZipEntry anEntry = new ZipEntry(entryPath);
                zos.putNextEntry(anEntry);
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                fis.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            NSDictionary info = new NSDictionary(new Object[] { e.getMessage() }, new String[] { NSError.LocalizedDescriptionKey });
            NSError message = new NSError("Pachyderm Build Domain", -1, info);
            context.appendBuildMessage(message);
        }
    }
}
