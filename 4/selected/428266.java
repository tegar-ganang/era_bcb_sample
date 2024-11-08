package org.apache.commons.compress.changes;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Performs the operations of a change set
 */
public class ChangeWorker {

    private ChangeWorker() {
    }

    /**
	 * TODO
	 * @param changes
	 * @param in
	 * @param out
	 * @throws IOException 
	 */
    public static void perform(ChangeSet changes, ArchiveInputStream in, ArchiveOutputStream out) throws IOException {
        ArchiveEntry entry = null;
        while ((entry = in.getNextEntry()) != null) {
            System.out.println(entry.getName());
            boolean copy = true;
            for (Iterator it = changes.asSet().iterator(); it.hasNext(); ) {
                Change change = (Change) it.next();
                if (change.type() == ChangeSet.CHANGE_TYPE_DELETE) {
                    DeleteChange delete = ((DeleteChange) change);
                    if (entry.getName() != null && entry.getName().equals(delete.targetFile())) {
                        copy = false;
                    }
                }
            }
            if (copy) {
                System.out.println("Copy: " + entry.getName());
                long size = entry.getSize();
                out.putArchiveEntry(entry);
                IOUtils.copy((InputStream) in, out, (int) size);
                out.closeArchiveEntry();
            }
            System.out.println("---");
        }
        out.close();
    }
}
