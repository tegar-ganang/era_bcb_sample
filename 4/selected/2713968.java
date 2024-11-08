package org.bookshare.document.packager;

import java.io.File;
import java.io.IOException;
import org.benetech.util.FileUtils;
import org.benetech.util.ZipUtils;
import org.bookshare.document.beans.DocumentComponent;
import org.bookshare.document.beans.DocumentSet;
import org.bookshare.document.beans.DocumentType;

/**
 * Zip a DocumentSet up.
 * @author Reuben Firmin
 *
 * @param <T> The type of documentset.
 */
public final class ZipDocumentPackager<T extends DocumentType> implements Packager<T> {

    /**
	 * Default constructor.
	 */
    public ZipDocumentPackager() {
    }

    /**
     * {@inheritDoc}
     */
    public File packageDocuments(final String fileName, final DocumentSet<T> documentSet, final File outDir) throws IOException {
        return packageDocuments(fileName, documentSet, outDir, null);
    }

    /**
     * {@inheritDoc}
     */
    public File packageDocuments(final String fileName, final DocumentSet<T> documentSet, final File outDir, final String topDir) throws IOException {
        final File dir = new File(outDir.getAbsolutePath() + File.separator + fileName + "_pkg");
        dir.mkdirs();
        File zipStruct;
        if (topDir == null) {
            zipStruct = dir;
        } else {
            zipStruct = new File(dir.getAbsolutePath() + File.separator + topDir);
            zipStruct.mkdirs();
        }
        for (DocumentComponent element : documentSet.getType().getComponents()) {
            for (File file : documentSet.getFiles(element)) {
                FileUtils.copyFileToDirectory(file, documentSet.getFileBasePath(), zipStruct);
            }
        }
        final File out = ZipUtils.createZip(fileName + ".zip", dir, outDir);
        FileUtils.deleteDirectory(dir);
        return out;
    }
}
