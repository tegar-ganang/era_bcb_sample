package jaxb.handlers;

import java.io.File;
import jaxb.DataStoreException;
import jaxb.FileReaderWriter;
import jaxb.ReaderWriter;

/**
 * Holds a reference to a ReaderWriter and facilitates the reading and writing
 * of the data to/from the JAXB framework
 * @author em709p
 */
public abstract class JAXBReaderWriterHandler extends AbstractJAXBHandler {

    /**
     * The ReaderWriter to be load/written
     */
    private ReaderWriter readerWriter;

    /**
     * The data object tree loaded from the underlying data source
     */
    private Object data;

    /**
     * The last read date of the data in milliseconds
    */
    private long dataReadAtLastModifiedMillis;

    /**
     * The file exists on disk
     */
    private boolean fileExistsOnDisk;

    /**
     * The schema of the xml file
     */
    private String schema;

    /**
     * Returns the File of the ReaderWriter
     * @return File The File of the ReaderWriter
     */
    public File getFile() {
        return ((FileReaderWriter) this.readerWriter).getFile();
    }

    /**
     * Returns the name of the file
     * @return String The name of the file
     */
    public String getFilename() {
        return ((FileReaderWriter) this.readerWriter).getFile().getName();
    }

    /**
     * Returns if the file of the FileReader is deleted
     * @return boolean If the file is deleted
     */
    public boolean delete() {
        return this.getFile().delete();
    }

    /**
     * Creates a new instance JAXBReaderWriter. Reads the underyling data store.
     * @param fileExistsOnDisk If the file exists on disk
     * @param context The unique context to be passed to the JAXB framework
     * @param readerWriter The ReaderWriter to be load/written
     * @throws es.sgi.spf.jaxb.common.DataStoreException if there is a problem 
     * reading the data
     */
    protected JAXBReaderWriterHandler(String context, ReaderWriter readerWriter, boolean fileExistsOnDisk) throws DataStoreException {
        this(context, readerWriter, null, fileExistsOnDisk);
    }

    /**
     * Creates a new instance JAXBReaderWriter. Reads the underyling data store.
     * @param fileExistsOnDisk If the file exists on disk
     * @param context The unique context to be passed to the JAXB framework
     * @param readerWriter The ReaderWriter to be load/written
     * @param schema The schema for the xml file
     * @throws es.sgi.spf.jaxb.common.DataStoreException if there is a problem 
     * reading the data
     */
    protected JAXBReaderWriterHandler(String context, ReaderWriter readerWriter, String schema, boolean fileExistsOnDisk) throws DataStoreException {
        super(context);
        this.readerWriter = readerWriter;
        this.schema = schema;
        this.fileExistsOnDisk = fileExistsOnDisk;
        if (fileExistsOnDisk) {
            this.load();
        } else {
            this.init();
        }
    }

    /**
     * Reads the ReaderWriter inputstream and stores the data in the data 
     * member variable
     * 
     * @throws es.sgi.spf.jaxb.common.DataStoreException if there is a problem 
     * reading the data
     */
    protected synchronized void load() throws DataStoreException {
        if (fileExistsOnDisk) {
            try {
                this.dataReadAtLastModifiedMillis = readerWriter.getLastModifiedMillis();
                this.data = super.read(readerWriter.getInputStream(), this.schema);
            } catch (DataStoreException d) {
                throw new DataStoreException("Error reading from ReaderWriter:" + readerWriter.toString() + "." + d.getMessage(), d);
            }
        }
    }

    /**
     * Writes the local data object to an ReaderWriter outputstream
     * 
     * @throws es.sgi.spf.jaxb.common.DataStoreException if there is a problem 
     * writing the data
    */
    protected synchronized void store() throws DataStoreException {
        try {
            super.write(this.data, readerWriter.getOutputStream());
            this.fileExistsOnDisk = true;
        } catch (DataStoreException d) {
            throw new DataStoreException("Error writing data to:" + readerWriter.toString(), d);
        }
    }

    /**
     * Refreshes the data object from the underlying data source if the data is
     * deemed to be out of date else no action is performed.
     * @throws es.sgi.spf.jaxb.common.DataStoreException If an error occurs 
     * reading the file. 
     */
    protected synchronized void refresh() throws DataStoreException {
        if (fileExistsOnDisk) {
            if (dataReadAtLastModifiedMillis != readerWriter.getLastModifiedMillis()) {
                this.load();
            } else {
            }
        }
    }

    /**
     * Returns the data object load from the underlying data source. Data is
     * first refreshed if out of date.
     * @throws es.sgi.spf.jaxb.common.DataStoreException If an error occurs 
     * reading the data.
     * @return Object The Data object.
     */
    protected Object getData() throws DataStoreException {
        this.refresh();
        return this.data;
    }

    /**
     * Sets the data object load from the underlying data soure.
     * @param data The data object
     */
    protected void setData(Object data) {
        this.data = data;
    }

    /**
     * Returns the ReaderWriter object
     * @return ReaderWriter The ReaderWriter object
     */
    protected ReaderWriter getReaderWriter() {
        return this.readerWriter;
    }

    /**
     * override in subclasses to populate data if desired in the event 
     * not reading a data file at subclass creat time. See autoload 
     * parameter in constructor.
    */
    protected void init() {
    }
}
