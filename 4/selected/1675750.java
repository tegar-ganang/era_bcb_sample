package gov.lanl.xmltape.identifier.index.jdbImpl;

import gov.lanl.identifier.Identifier;
import gov.lanl.identifier.IndexException;
import gov.lanl.xmltape.identifier.index.IdentifierIndexInterface;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class IdentifierIndex implements IdentifierIndexInterface {

    private IdentifierManager idx = new IdentifierManager();

    boolean readOnly = false;

    private String indexDir;

    private String indexFile;

    public IdentifierIndex() {
    }

    /**
     * Close current index instance
     * @throws IndexException
     */
    public void close() throws IndexException {
        if (indexFile == null) System.out.println("indexFile already closed.");
        if (!readOnly) writeIndex();
    }

    /**
     * Returns the number of Identifier records
     * @return 
     *        number of records in this database or set
     */
    public long count() throws IndexException {
        return this.idx.size();
    }

    /**
     * Delete Identifier for the specified identifier
     * @param identifier
     *             identifier of Identifier to be deleted from index
     */
    public void delete(String identifier) throws IndexException {
        if (!readOnly) {
            idx.deleteIdentifier(identifier);
        }
    }

    /**
     * Gets the IndexItem for the specified unique id
     * @param identifier
     *            unique record id
     * @return IndexItem
     *            object matching specified id
     */
    public Identifier getIdentifier(String identifier) throws IndexException {
        return this.getIdentifiers(identifier).get(0);
    }

    /**
     * Determine if current identifier is a record identifier
     * @param identifier - identifier to test
     * @return if docId, returns datestamp else returns null
     * @throws IndexException
     */
    public String isDocId(String identifier) throws IndexException {
        for (Iterator<Identifier> i = idx.iterator(); i.hasNext(); ) {
            Identifier id = i.next();
            if (id.getRecordId().equals(identifier)) return id.getDatestamp();
        }
        return null;
    }

    /**
     * Opens index file, optionally in read-only mode
     * @param readonly
     *            Open database as readonly
     */
    public void open(boolean readonly) throws IndexException {
        this.readOnly = readonly;
        if (idx.size() == 0) {
            openIndexFile(readonly);
        }
    }

    /**
     * Adds an Identifier instance to the Identifiers TreeSet
     * @param item
     *            Identifier to be added to current index instance
     */
    public synchronized void putIdentifier(Identifier item) throws IndexException {
        idx.add(item);
    }

    /**
     * Adds an Identifier instance to the Identifier TreeSet
     * @param items
     *            Identifiers to be added to current index instance
     */
    public synchronized void putIdentifiers(ArrayList<Identifier> items) throws IndexException {
        for (Identifier id : items) {
            idx.add(id);
        }
    }

    /**
     * Read from index, list of identifiers
     * @param identifier
     *            content or datastream identifier
     * @return
     *            Vector of Identifier
     */
    public ArrayList<Identifier> getIdentifiers(String identifier) throws IndexException {
        ArrayList<Identifier> v = new ArrayList<Identifier>();
        for (Iterator it = idx.iterator(); it.hasNext(); ) {
            Identifier i = (Identifier) (it.next());
            String id = i.getIdentifier();
            if ((identifier == null) || (id.equals(identifier))) v.add(i);
        }
        Collections.sort(v);
        return v;
    }

    /**
     * Serialize IdentifierManager TreeSet to object's indexFile
     * @throws IndexException
     */
    private void writeIndex() throws IndexException {
        if (this.indexFile != null) {
            File idx = new File(this.indexFile);
            this.writeIndex(idx);
        } else {
            throw new IndexException("indexFile has yet to be initialized");
        }
    }

    /**
     * Serialize IdentifierManager TreeSet to specified idxFile
     * @throws IndexException
     */
    private void writeIndex(File idxFile) throws IndexException {
        if (idxFile.exists()) idxFile.delete();
        if (idx.size() > 0) {
            FileOutputStream out;
            ObjectOutputStream s;
            try {
                out = new FileOutputStream(idxFile);
                s = new ObjectOutputStream(out);
                s.writeObject(idx);
                s.flush();
                s.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets a Byte Array of identifiers
     * Format: id\n
     */
    public byte[] listIdentifiers() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] newline = "\n".getBytes();
        ArrayList docids = new ArrayList();
        Identifier c;
        for (Iterator i = idx.iterator(); i.hasNext(); ) {
            c = (Identifier) i.next();
            try {
                String docid = c.getRecordId();
                if (!docids.contains(docid)) {
                    docids.add(docid);
                    baos.write(docid.getBytes());
                    baos.write(newline);
                }
                baos.write(c.getIdentifier().getBytes());
                baos.write(newline);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (baos != null) baos.close();
                } catch (Exception dbe) {
                }
            }
        }
        return baos.toByteArray();
    }

    /**
     * Sets path to directory containing index files
     * 
     * @param indexDir
     *            Absolute path to dir containing index
     */
    public void setIndexDir(String indexDir) {
        this.indexDir = indexDir;
        String name = new File(indexDir).getName() + ".idi";
        this.indexFile = new File(indexDir, name).getAbsolutePath();
    }

    /**
     * Gets path to directory containing index files
     */
    public String getIndexDir() {
        return indexDir;
    }

    private void openIndexFile(boolean readonly) throws IndexException {
        this.readOnly = readonly;
        if (indexFile == null) throw new IndexException("indexFile has yet to be initialized");
        File tape = new File(indexFile);
        if (tape.exists()) {
            setIndex(tape);
        }
    }

    private void setIndex(File src) {
        InputStream in = null;
        int bufLen = 20 * 1024 * 1024;
        try {
            long s = System.currentTimeMillis();
            in = new BufferedInputStream(new FileInputStream(src), bufLen);
            ObjectInputStream objectInputStream = new ObjectInputStream(in);
            idx = (IdentifierManager) objectInputStream.readObject();
            objectInputStream.close();
            System.out.println("Time to open: " + (System.currentTimeMillis() - s));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (in != null) try {
                in.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Verifies that the active index is valid
     * (i.e. contains necessary indexes and not corrupt)
     */
    public boolean isValid() throws IndexException {
        try {
            if (count() > 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new IndexException("Invalid Index: " + indexFile);
        }
    }

    public void closeDatabases() throws IndexException {
        close();
    }
}
