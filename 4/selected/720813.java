package cz.zcu.fav.hofhans.packer.bdo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import cz.zcu.fav.hofhans.packer.dao.CompressionDao;
import cz.zcu.fav.hofhans.packer.exception.PackerRuntimeException;
import cz.zcu.fav.hofhans.packer.exception.ValidationException;
import cz.zcu.fav.hofhans.packer.exception.ValidationException.ValidationExceptionCode;

/**
 * File representation.
 * @author Tomáš Hofhans
 * @since 6.12.2009
 */
public class PackerFile extends PackerItem {

    /** LOG. */
    private static final Logger LOG = Logger.getLogger(PackerFile.class.getName());

    /** Buffer size. */
    private static final int BUFFER_SIZE = 1024;

    /** Compression type. */
    private CompressionType compression;

    /** File parameters. */
    private CompressionParams params;

    /** Data. */
    private byte[] data;

    /**
   * Constructor for new file.
   * @param id file id
   * @param name file name
   * @param parent parent folder
   */
    public PackerFile(int id, String name, PackerFolder parent) {
        super(id, name, parent);
    }

    /**
   * Constructor for new file.
   * @param name file name
   * @param parent parent folder
   */
    public PackerFile(String name, PackerFolder parent) {
        super(name, parent);
    }

    /**
   * Get compression type.
   * @return the compression type
   */
    public CompressionType getCompression() {
        return compression;
    }

    /**
   * Set compression type.
   * @param compression the compression type to set
   */
    public void setCompression(CompressionType compression) {
        this.compression = compression;
    }

    /**
   * Get parameters for compression. For new file, must be set data first.
   * @return file parameters for compression
   */
    public CompressionParams getParams() {
        if (params == null) {
            if (data != null) {
                params = new CompressionParams(this);
            } else {
                throw new PackerRuntimeException("No data for parameters counting.");
            }
        }
        return params;
    }

    /**
   * Set parameters for compression.
   * @param params loaded parameters
   */
    public void setParams(CompressionParams params) {
        this.params = params;
    }

    /**
   * Get file data. Before calling of this method data must be set or file must be stored in database.
   * @return the data
   */
    public byte[] getData() {
        if (data == null) {
            CompressionDao cd = compression.getDao();
            data = cd.loadData(this);
        }
        assert data != null : "File not store in database and have no data.";
        return data;
    }

    /**
   * Set file data.
   * @param data the data to set
   */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
   * Load data from given file. Maximum file size is 2GiB.
   * @param file file with data
   * @throws ValidationException problem with loading file
   */
    public void setData(File file) throws ValidationException {
        if (file.length() >= Integer.MAX_VALUE) {
            throw new PackerRuntimeException("File is too large.");
        }
        data = new byte[(int) file.length()];
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            int offset = 0;
            int numRead = 0;
            while (offset < data.length && (numRead = is.read(data, offset, data.length - offset)) >= 0) {
                offset += numRead;
            }
            if (offset < data.length) {
                throw new IOException("Could not completely read file " + file.getName());
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Problem with loading data.", e);
            throw new ValidationException(ValidationExceptionCode.PROBLEM_WITH_LOADING_FILE, e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Problem with closing input stream.", e);
                throw new ValidationException(ValidationExceptionCode.PROBLEM_WITH_LOADING_FILE, e);
            }
        }
    }

    /**
   * Extract file to given folder. If given folder doesn't exists try to create it.
   * @param folder folder to extract
   * @return created file
   */
    public File extract(File folder) throws ValidationException {
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                LOG.warning("Problem with creating extracting folder " + folder.toString());
                throw new ValidationException(ValidationExceptionCode.EXTRACTING_PROBLEM);
            }
        }
        File file = new File(folder, getName());
        FileOutputStream output = null;
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(getData());
            output = new FileOutputStream(file);
            byte[] buf = new byte[BUFFER_SIZE];
            int readed = 0;
            while ((readed = input.read(buf)) != -1) {
                output.write(buf, 0, readed);
            }
            output.flush();
        } catch (IOException e) {
            LOG.warning("Problem with extracting file: " + toDetailedString());
            throw new ValidationException(ValidationExceptionCode.EXTRACTING_PROBLEM);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Problem with closing output stream.", e);
                }
            }
        }
        data = null;
        return file;
    }

    /**
   * {@inheritDoc}
   * @see java.lang.Object#equals(java.lang.Object)
   */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (getId() == -1) {
            return false;
        }
        if (obj instanceof PackerFile) {
            PackerFile other = (PackerFile) obj;
            if (getId() == other.getId()) {
                return true;
            }
        }
        return false;
    }
}
