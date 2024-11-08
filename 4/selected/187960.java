package org.apache.poi.hpsf.examples;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.poi.hpsf.HPSFRuntimeException;
import org.apache.poi.hpsf.MarkUnsupportedException;
import org.apache.poi.hpsf.MutablePropertySet;
import org.apache.poi.hpsf.MutableSection;
import org.apache.poi.hpsf.NoPropertySetStreamException;
import org.apache.poi.hpsf.PropertySet;
import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hpsf.Util;
import org.apache.poi.hpsf.Variant;
import org.apache.poi.hpsf.WritingNotSupportedException;
import org.apache.poi.hpsf.wellknown.PropertyIDMap;
import org.apache.poi.poifs.eventfilesystem.POIFSReader;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.POIFSDocumentPath;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/**
 * <p>This class is a sample application which shows how to write or modify the
 * author and title property of an OLE 2 document. This could be done in two
 * different ways:</p>
 * 
 * <ul>
 * 
 * <li><p>The first approach is to open the OLE 2 file as a POI filesystem
 * (see class {@link POIFSFileSystem}), read the summary information property
 * set (see classes {@link SummaryInformation} and {@link PropertySet}), write
 * the author and title properties into it and write the property set back into
 * the POI filesystem.</p></li>
 * 
 * <li><p>The second approach does not modify the original POI filesystem, but
 * instead creates a new one. All documents from the original POIFS are copied
 * to the destination POIFS, except for the summary information stream. The
 * latter is modified by setting the author and title property before writing
 * it to the destination POIFS. It there are several summary information streams
 * in the original POIFS - e.g. in subordinate directories - they are modified
 * just the same.</p></li>
 * 
 * </ul>
 * 
 * <p>This sample application takes the second approach. It expects the name of
 * the existing POI filesystem's name as its first command-line parameter and
 * the name of the output POIFS as the second command-line argument. The
 * program then works as described above: It copies nearly all documents
 * unmodified from the input POI filesystem to the output POI filesystem. If it
 * encounters a summary information stream it reads its properties. Then it sets
 * the "author" and "title" properties to new values and writes the modified
 * summary information stream into the output file.</p>
 * 
 * <p>Further explanations can be found in the HPSF HOW-TO.</p>
 *
 * @author Rainer Klute <a
 * href="mailto:klute@rainer-klute.de">&lt;klute@rainer-klute.de&gt;</a>
 * @version $Id$
 * @since 2003-09-01
 */
public class WriteAuthorAndTitle {

    /**
     * <p>Runs the example program.</p>
     *
     * @param args Command-line arguments. The first command-line argument must
     * be the name of a POI filesystem to read.
     * @throws IOException if any I/O exception occurs.
     */
    public static void main(final String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: " + WriteAuthorAndTitle.class.getName() + " originPOIFS destinationPOIFS");
            System.exit(1);
        }
        final String srcName = args[0];
        final String dstName = args[1];
        final POIFSReader r = new POIFSReader();
        final ModifySICopyTheRest msrl = new ModifySICopyTheRest(dstName);
        r.registerListener(msrl);
        r.read(new FileInputStream(srcName));
        msrl.close();
    }

    /**
     * <p>This class does all the work. As its name implies it modifies a
     * summary information property set and copies everything else unmodified
     * to the destination POI filesystem. Since an instance of it is registered
     * as a {@link POIFSReader} its method {@link 
     * #processPOIFSReaderEvent(POIFSReaderEvent)} is called for each document
     * in the origin POIFS.</p>
     */
    static class ModifySICopyTheRest implements POIFSReaderListener {

        String dstName;

        OutputStream out;

        POIFSFileSystem poiFs;

        /**
         * <p>The constructor of a {@link ModifySICopyTheRest} instance creates
         * the target POIFS. It also stores the name of the file the POIFS will
         * be written to once it is complete.</p>
         * 
         * @param dstName The name of the disk file the destination POIFS is to
         * be written to.
         */
        public ModifySICopyTheRest(final String dstName) {
            this.dstName = dstName;
            poiFs = new POIFSFileSystem();
        }

        /**
         * <p>The method is called by POI's eventing API for each file in the
         * origin POIFS.</p>
         */
        public void processPOIFSReaderEvent(final POIFSReaderEvent event) {
            final POIFSDocumentPath path = event.getPath();
            final String name = event.getName();
            final DocumentInputStream stream = event.getStream();
            Throwable t = null;
            try {
                if (PropertySet.isPropertySetStream(stream)) {
                    PropertySet ps = null;
                    try {
                        ps = PropertySetFactory.create(stream);
                    } catch (NoPropertySetStreamException ex) {
                    }
                    if (ps.isSummaryInformation()) editSI(poiFs, path, name, ps); else copy(poiFs, path, name, ps);
                } else copy(poiFs, event.getPath(), event.getName(), stream);
            } catch (MarkUnsupportedException ex) {
                t = ex;
            } catch (IOException ex) {
                t = ex;
            } catch (WritingNotSupportedException ex) {
                t = ex;
            }
            if (t != null) {
                throw new HPSFRuntimeException("Could not read file \"" + path + "/" + name + "\". Reason: " + Util.toString(t));
            }
        }

        /**
         * <p>Receives a summary information property set modifies (or creates)
         * its "author" and "title" properties and writes the result under the
         * same path and name as the origin to a destination POI filesystem.</p>
         *
         * @param poiFs The POI filesystem to write to.
         * @param path The original (and destination) stream's path.
         * @param name The original (and destination) stream's name.
         * @param si The property set. It should be a summary information
         * property set.
         * @throws IOException 
         * @throws WritingNotSupportedException 
         */
        public void editSI(final POIFSFileSystem poiFs, final POIFSDocumentPath path, final String name, final PropertySet si) throws WritingNotSupportedException, IOException {
            final DirectoryEntry de = getPath(poiFs, path);
            final MutablePropertySet mps = new MutablePropertySet(si);
            final MutableSection s = (MutableSection) mps.getSections().get(0);
            s.setProperty(PropertyIDMap.PID_AUTHOR, Variant.VT_LPSTR, "Rainer Klute");
            s.setProperty(PropertyIDMap.PID_TITLE, Variant.VT_LPWSTR, "Test");
            final InputStream pss = mps.toInputStream();
            de.createDocument(name, pss);
        }

        /**
         * <p>Writes a {@link PropertySet} to a POI filesystem. This method is
         * simpler than {@link #editSI} because the origin property set has just
         * to be copied.</p>
         *
         * @param poiFs The POI filesystem to write to.
         * @param path The file's path in the POI filesystem.
         * @param name The file's name in the POI filesystem.
         * @param ps The property set to write.
         * @throws WritingNotSupportedException 
         * @throws IOException 
         */
        public void copy(final POIFSFileSystem poiFs, final POIFSDocumentPath path, final String name, final PropertySet ps) throws WritingNotSupportedException, IOException {
            final DirectoryEntry de = getPath(poiFs, path);
            final MutablePropertySet mps = new MutablePropertySet(ps);
            de.createDocument(name, mps.toInputStream());
        }

        /**
         * <p>Copies the bytes from a {@link DocumentInputStream} to a new
         * stream in a POI filesystem.</p>
         *
         * @param poiFs The POI filesystem to write to.
         * @param path The source document's path.
         * @param name The source document's name.
         * @param stream The stream containing the source document.
         * @throws IOException 
         */
        public void copy(final POIFSFileSystem poiFs, final POIFSDocumentPath path, final String name, final DocumentInputStream stream) throws IOException {
            final DirectoryEntry de = getPath(poiFs, path);
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            int c;
            while ((c = stream.read()) != -1) out.write(c);
            stream.close();
            out.close();
            final InputStream in = new ByteArrayInputStream(out.toByteArray());
            de.createDocument(name, in);
        }

        /**
         * <p>Writes the POI file system to a disk file.</p>
         *
         * @throws FileNotFoundException
         * @throws IOException
         */
        public void close() throws FileNotFoundException, IOException {
            out = new FileOutputStream(dstName);
            poiFs.writeFilesystem(out);
            out.close();
        }

        /** Contains the directory paths that have already been created in the
         * output POI filesystem and maps them to their corresponding
         * {@link org.apache.poi.poifs.filesystem.DirectoryNode}s. */
        private final Map paths = new HashMap();

        /**
         * <p>Ensures that the directory hierarchy for a document in a POI
         * fileystem is in place. When a document is to be created somewhere in
         * a POI filesystem its directory must be created first. This method
         * creates all directories between the POI filesystem root and the
         * directory the document should belong to which do not yet exist.</p>
         * 
         * <p>Unfortunately POI does not offer a simple method to interrogate
         * the POIFS whether a certain child node (file or directory) exists in
         * a directory. However, since we always start with an empty POIFS which
         * contains the root directory only and since each directory in the
         * POIFS is created by this method we can maintain the POIFS's directory
         * hierarchy ourselves: The {@link DirectoryEntry} of each directory
         * created is stored in a {@link Map}. The directories' path names map
         * to the corresponding {@link DirectoryEntry} instances.</p>
         *
         * @param poiFs The POI filesystem the directory hierarchy is created
         * in, if needed.
         * @param path The document's path. This method creates those directory
         * components of this hierarchy which do not yet exist.
         * @return The directory entry of the document path's parent. The caller
         * should use this {@link DirectoryEntry} to create documents in it.
         */
        public DirectoryEntry getPath(final POIFSFileSystem poiFs, final POIFSDocumentPath path) {
            try {
                final String s = path.toString();
                DirectoryEntry de = (DirectoryEntry) paths.get(s);
                if (de != null) return de;
                int l = path.length();
                if (l == 0) de = poiFs.getRoot(); else {
                    de = getPath(poiFs, path.getParent());
                    de = de.createDirectory(path.getComponent(path.length() - 1));
                }
                paths.put(s, de);
                return de;
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
                throw new RuntimeException(ex.toString());
            }
        }
    }
}
