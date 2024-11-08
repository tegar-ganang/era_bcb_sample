package com.bonkey.filesystem.writable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import nu.xom.Element;
import org.eclipse.core.runtime.IProgressMonitor;
import com.bonkey.filesystem.Encryptor;
import com.bonkey.filesystem.Messages;
import com.bonkey.filesystem.PasswordUserAuthenticator;
import com.bonkey.filesystem.browsable.BrowsableFile;
import com.bonkey.filesystem.browsable.BrowsableFileSystem;
import com.bonkey.filesystem.browsable.BrowsableFolder;
import com.bonkey.filesystem.browsable.BrowsableItem;

/**
 * 
 * Base class for filesystems which can be written to; adds functionality
 * to BrowsableFileSystems, including encryption
 * 
 * @author marcel
 */
public abstract class WritableFileSystem extends BrowsableFileSystem {

    private static final String A_ENCRYPTOR = "encryptor";

    /**
	 * The encryptor used with this filesystem
	 */
    private Encryptor encryptor;

    /**
	 * The root of this filesystem
	 */
    protected transient BrowsableFolder root;

    /**
	 * Authentication for this filesystem. May be null.
	 */
    private PasswordUserAuthenticator authentication;

    /**
	 * Construct the filesystem from XML
	 * @param e the XML element representing the filesystem
	 */
    public WritableFileSystem(Element e) {
        super(e);
        Element encrypted = e.getFirstChildElement(A_ENCRYPTOR);
        if (encrypted != null) {
            this.encryptor = new Encryptor(encrypted);
        }
        Element auth = e.getFirstChildElement(PasswordUserAuthenticator.A_AUTH);
        if (auth != null) {
            authentication = new PasswordUserAuthenticator(auth);
        }
    }

    /**
	 * Construct a new filesystem from name and URI
	 * @param name the name of the filesystem
	 * @param uri the URI of the filesystem
	 */
    public WritableFileSystem(String name, String uri) {
        super(name, uri);
    }

    /**
	 * Construct a new encrypted filesystem from name, URI and key
	 * @param name name of the filesystem
	 * @param uri uri of the filesystem
	 * @param key encryption key
	 * @param saveKey whether to save the encryption key between executions of the program
	 */
    public WritableFileSystem(String name, String uri, String key, boolean saveKey) {
        super(name, uri);
        if (key != null) {
            this.encryptor = new Encryptor(key, saveKey);
        }
    }

    /**
	 * Put a file into this filesystem
	 * @param file the file to be put into the filesystem
	 * @param relativePath the relative URI on the filesystem where the file is to be stored
	 * @param monitor the progress monitor to report progress on; can be null for no reporting
	 * @param incrementSize the size in bytes of each unit of work on the progress monitor 
	 * @throws IOException when the operation fails (eg disk error)
	 */
    public void putFile(BrowsableItem file, String relativePath, IProgressMonitor monitor, float incrementSize) throws IOException {
        if (!(file instanceof BrowsableFile)) {
            throw new IOException(Messages.getString("WritableFileSystem.ErrorPutFiles"));
        }
        InputStream sourceStream = null;
        try {
            sourceStream = ((BrowsableFile) file).getInputStream();
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
        putFileContents(sourceStream, file.getSize(), relativePath, monitor, incrementSize);
    }

    /**
	 * Create a file in this filesystem with the given contents, to be read from the input stream.
	 * Closes the input stream after the operation.
	 * If the monitor is not null, progress should be reported on it and it should periodically be
	 * checked to see if the operation has been cancelled.
	 * Handles encryption if destination filesystem is encrypted.
	 * @param input the input stream from which to read the contents of the file
	 * @param size the size of the file being read
	 * @param relativeURI the relative uri at which the file is to be stored on this filesystem
	 * @param monitor the progress monitor to report progress on; can be null for no reporting
	 * @param incrementSize the size in bytes of each unit of work on the progress monitor
	 * @throws IOException where the operation fails (eg disk error)
	 */
    public abstract void putFileContents(InputStream input, long size, String relativeURI, IProgressMonitor monitor, float incrementSize) throws IOException;

    /**
	 * Create a folder on this filesystem
	 * @param relativePath the path of the folder to be created on the filesystem
	 * @throws IOException if creation fails (eg disk error)
	 */
    public abstract void createFolder(String relativePath) throws IOException;

    /**
	 * Get the input stream for a file in this filesystem.
	 * @param relativeURI the location of the file.
	 * @return the input stream, decrypted if necessary.
	 * @throws IOException when IO or decryption problems.
	 */
    public abstract InputStream getInputStream(String relativeURI) throws IOException;

    /**
	 * Delete a file (or other BrowsableItem) from this filesystem
	 * @param item the file to be deleted
	 * @throws IOException where deletion fails (eg disk error)
	 */
    public abstract void deleteFile(BrowsableItem item) throws IOException;

    public Element toXML() {
        Element e = super.toXML();
        if (encryptor != null) {
            e.appendChild(encryptor.toXML());
        }
        if (getAuthentication() != null) {
            e.appendChild(getAuthentication().toXML());
        }
        return e;
    }

    public Element toXMLSecure() {
        Element e = super.toXMLSecure();
        if (getAuthentication() != null) {
            e.appendChild(getAuthentication().toXMLSecure());
        }
        return e;
    }

    /**
	 * Set authentication for this filesystem
	 * @param username never null
	 * @param password can be null
	 * @param savePassword whether to save the password between executions
	 */
    public void setAuthentication(String username, String password, boolean savePassword) {
        if (username != null) {
            authentication = new PasswordUserAuthenticator(username, password, savePassword);
        }
    }

    public PasswordUserAuthenticator getAuthentication() {
        return authentication;
    }

    public boolean isEncrypted() {
        return (encryptor != null);
    }

    /**
	 * Get the encryptor used to encrypt this filesystem
	 * @return the encryptor, or null if the filesystem isn't encrypted
	 */
    public Encryptor getEncryptor() throws IOException {
        if (encryptor == null) {
            throw new IOException(Messages.getString("WritableFileSystem.ErrorNoEncryptor"));
        }
        return encryptor;
    }

    /**
	 * Set the encryptor for this filesystem. Set to null for no encryption
	 * @param encryptor the encryptor, or null for no encryption
	 */
    public void setEncryptor(Encryptor encryptor) {
        this.encryptor = encryptor;
    }

    protected BrowsableFolder getRoot() throws IOException {
        return root;
    }

    public long getSize() throws IOException {
        return getRoot().getSize();
    }

    public void freeMemory() {
        root = null;
    }

    /**
	 * Copy the whole of a file from an input stream to an output stream, in chunks of the given buffer size
	 * @param sourceStream the input stream
	 * @param destStream the output stream
	 * @param bufferSize the size of chunks to copy in
	 * @param monitor progress monitor to report on; can be null
	 * @param incrementSize the amount of work to report on the progress monitor per byte
	 * @throws IOException where the operation fails
	 */
    protected long copyFile(InputStream sourceStream, OutputStream destStream, int bufferSize, IProgressMonitor monitor, float incrementSize) throws IOException {
        byte[] data = new byte[bufferSize];
        long totalRead = 0;
        int read = 0;
        float stackedWork = 0;
        boolean cancelled = false;
        while (((read = sourceStream.read(data, 0, bufferSize)) > 0) && !cancelled) {
            destStream.write(data, 0, read);
            destStream.flush();
            totalRead += read;
            if (monitor != null) {
                stackedWork += (read * incrementSize);
                if (stackedWork >= 1) {
                    monitor.worked((int) stackedWork);
                    stackedWork = stackedWork - ((int) stackedWork);
                }
                if (monitor.isCanceled()) {
                    cancelled = true;
                }
            }
        }
        return totalRead;
    }

    /**
	 * Encrypt the input stream if this filesystem is encrypted.
	 * @param input the input stream to encrypt
	 * @return the input stream encrypted if necessary
	 * @throws IOException where encryption fails
	 */
    protected InputStream encryptStreamIfNeeded(InputStream input) throws IOException {
        if (isEncrypted()) {
            try {
                input = getEncryptor().encryptStream(input);
            } catch (Exception e) {
                throw new IOException(e.getMessage());
            }
        }
        return input;
    }

    /**
	 * Decrypt the input stream if this filesystem is encrypted.
	 * @param input the input stream to encrypt
	 * @return the input stream encrypted if necessary
	 * @throws IOException where encryption fails
	 */
    protected InputStream decryptStreamIfNeeded(InputStream input) throws IOException {
        if (isEncrypted()) {
            try {
                input = getEncryptor().decryptStream(input);
            } catch (Exception e) {
                throw new IOException(e.getMessage());
            }
        }
        return input;
    }
}
