package wjhk.jupload2.upload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import wjhk.jupload2.exception.JUploadException;
import wjhk.jupload2.exception.JUploadIOException;
import wjhk.jupload2.exception.JUploadInterrupted;
import wjhk.jupload2.filedata.FileData;
import wjhk.jupload2.policies.UploadPolicy;
import wjhk.jupload2.upload.helper.ByteArrayEncoder;

/**
 * This class implements the FileData interface, and is responsible to do the
 * actual upload of the files.
 * 
 * @author etienne_sf
 */
public class UploadFileData implements FileData {

    /**
	 * The {@link FileData} instance that contains all information about the
	 * file to upload.
	 */
    private FileData fileData = null;

    /**
	 * The value of the fileData InputStream. It's main use is to allow chunk
	 * upload, to reuse the previous InputStream, that is: each chunk will start
	 * reading the stream where the previous one stopped.
	 */
    InputStream uploadInputStream = null;

    /**
	 * Indicates the position of the file in the current upload (from 0 to
	 * max-1). It is mainly used by the
	 * ProgressBarManager.updateUploadProgressBarText() method, to display the
	 * upload status to the user.
	 */
    int numOfFileInCurrentUpload = -1;

    /**
	 * Instance of the fileUploadManagerThread. This allow this class to send
	 * feedback to the thread.
	 * 
	 * @see FileUploadManagerThread#nbBytesUploaded(long)
	 */
    private FileUploadManagerThread fileUploadManagerThread = null;

    /**
	 * The number of bytes to upload, for this file (without the head and tail
	 * defined for the HTTP multipart body).
	 */
    private long uploadRemainingLength = -1;

    /**
	 * The current {@link UploadPolicy}
	 */
    private UploadPolicy uploadPolicy = null;

    private static final int BUFLEN = 4096;

    /**
	 * This field is no more static, as we could decide to upload two files
	 * simultaneously.
	 */
    private final byte readBuffer[] = new byte[BUFLEN];

    /**
	 * Standard constructor for the UploadFileData class.
	 * 
	 * @param fileDataParam
	 *            The file data the this instance must transmist.
	 * @param numOfFileInCurrentUpload
	 * @param fileUploadManagerThreadParam
	 *            The current instance of {@link FileUploadThread}
	 * @param uploadPolicyParam
	 *            The current upload policy, instance of {@link UploadPolicy}
	 */
    public UploadFileData(FileData fileDataParam, int numOfFileInCurrentUpload, FileUploadManagerThread fileUploadManagerThreadParam, UploadPolicy uploadPolicyParam) {
        if (fileDataParam == null && !(this instanceof UploadFileDataPoisonned)) {
            throw new NullPointerException("fileData is null in UploadFileData(FileData, FileUploadManagerThread, UploadPolicy) constructor");
        }
        this.fileData = fileDataParam;
        this.numOfFileInCurrentUpload = numOfFileInCurrentUpload;
        this.fileUploadManagerThread = fileUploadManagerThreadParam;
        this.uploadPolicy = uploadPolicyParam;
    }

    /**
	 * This particular constructor is posted by the
	 * {@link FilePreparationThread} in the preparedFileQueue to indicate that
	 * the last file has been prepared.
	 * 
	 * @param poisonned
	 *            This parameter is here to avoid this constructor to be the
	 *            default constructor. Its value must be 'true'.
	 */
    public UploadFileData(boolean poisonned) {
        if (!poisonned) {
            throw new IllegalArgumentException("poisonned must be true in UploadFileData(boolean) constructor");
        }
    }

    /**
	 * Get the number of files that are still to upload. It is initialized at
	 * the creation of the file, by a call to the
	 * {@link FileData#getUploadLength()}. <BR>
	 * <B>Note:</B> When the upload for this file is finish and you want to send
	 * it again (for instance the upload failed, and you want to do a retry),
	 * you should not reuse this instance, but, instead, create a new
	 * UploadFileData instance.
	 * 
	 * @return Number of bytes still to upload.
	 * @see #getInputStream()
	 */
    long getRemainingLength() {
        return this.uploadRemainingLength;
    }

    /**
	 * This methods writes the file data (see {@link FileData#getInputStream()}
	 * to the given outputStream (the output toward the HTTP server).
	 * 
	 * @param outputStream
	 *            The stream on which the data is to be written.
	 * @param amount
	 *            The number of bytes to write.
	 * @throws JUploadException
	 *             if an I/O error occurs.
	 * @throws JUploadInterrupted
	 *             Thrown when an interruption of the thread is detected.
	 */
    void uploadFile(OutputStream outputStream, long amount) throws JUploadException, JUploadInterrupted {
        this.uploadPolicy.displayDebug("in UploadFileData.uploadFile (amount:" + amount + ", getUploadLength(): " + getUploadLength() + ")", 30);
        InputStream inputStream = getInputStream();
        while (amount > 0 && !this.fileUploadManagerThread.isUploadFinished()) {
            if (Thread.interrupted()) {
                throw new JUploadInterrupted(getClass().getName() + ".uploadFile [" + this.getFileName() + "]", this.uploadPolicy);
            }
            int toread = (amount > BUFLEN) ? BUFLEN : (int) amount;
            int towrite = 0;
            try {
                towrite = inputStream.read(this.readBuffer, 0, toread);
            } catch (IOException e) {
                throw new JUploadIOException(e);
            }
            if (towrite > 0) {
                try {
                    outputStream.write(this.readBuffer, 0, towrite);
                    this.fileUploadManagerThread.nbBytesUploaded(towrite, this);
                    amount -= towrite;
                    this.uploadRemainingLength -= towrite;
                    if (this.uploadPolicy.getDebugLevel() > 100) {
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                        }
                    }
                } catch (IOException ioe) {
                    throw new JUploadIOException(this.getClass().getName() + ".uploadFile()", ioe);
                } catch (Exception e) {
                    throw new JUploadException(this.getClass().getName() + ".uploadFile()  (check the user permission on the server)", e);
                }
            }
        }
    }

    /**
	 * Just transmit to {@link FileData#afterUpload()}.
	 * 
	 * @see FileData#afterUpload()
	 */
    public void afterUpload() {
        this.fileData.afterUpload();
    }

    /** {@inheritDoc} */
    public void appendFileProperties(ByteArrayEncoder bae, int index) throws JUploadIOException {
        this.fileData.appendFileProperties(bae, index);
    }

    /** {@inheritDoc} */
    public void beforeUpload() throws JUploadException {
        this.fileData.beforeUpload();
        this.uploadRemainingLength = this.fileData.getUploadLength();
    }

    /** {@inheritDoc} */
    public boolean canRead() {
        return this.fileData.canRead();
    }

    /** {@inheritDoc} */
    public String getDirectory() {
        return this.fileData.getDirectory();
    }

    /** {@inheritDoc} */
    public File getFile() {
        return this.fileData.getFile();
    }

    /** {@inheritDoc} */
    public String getFileExtension() {
        return this.fileData.getFileExtension();
    }

    /** {@inheritDoc} */
    public long getFileLength() {
        return this.fileData.getFileLength();
    }

    /** {@inheritDoc} */
    public String getFileName() {
        return this.fileData.getFileName();
    }

    /** {@inheritDoc} */
    public InputStream getInputStream() throws JUploadException {
        if (this.uploadInputStream == null) {
            this.uploadInputStream = this.fileData.getInputStream();
        }
        return this.uploadInputStream;
    }

    /** {@inheritDoc} */
    public Date getLastModified() {
        return this.fileData.getLastModified();
    }

    /** {@inheritDoc} */
    public String getMD5() throws JUploadException {
        return this.fileData.getMD5();
    }

    /** {@inheritDoc} */
    public String getMimeType() {
        return this.fileData.getMimeType();
    }

    /** {@inheritDoc} */
    public String getRelativeDir() {
        return this.fileData.getRelativeDir();
    }

    /**
	 * Retrieves the file name, that should be used in the server application.
	 * Default is to send the original filename.
	 * 
	 * @param index
	 *            The index of this file in the current request to the server.
	 * @return The real file name. Not used in FTP upload.
	 * @throws JUploadException
	 *             Thrown when an error occurs.
	 * @see UploadPolicy#getUploadFilename(FileData, int)
	 */
    public String getUploadFilename(int index) throws JUploadException {
        return this.uploadPolicy.getUploadFilename(this.fileData, index);
    }

    /**
	 * Retrieves the upload file name, that should be sent to the server. It's
	 * the technical name used to retrieve the file content. Default is File0,
	 * File1... This method just calls the
	 * {@link UploadPolicy#getUploadFilename(FileData, int)} method.
	 * 
	 * @param index
	 *            The index of this file in the current request to the server.
	 * @return The technical upload file name. Not used in FTP upload.
	 * @throws JUploadException
	 * @see UploadPolicy#getUploadName(FileData, int)
	 */
    public String getUploadName(int index) throws JUploadException {
        return this.uploadPolicy.getUploadName(this.fileData, index);
    }

    /**
	 * This methods stores locally the upload length. So, on the contrary of the
	 * {@link FileData} interface, this method may be called after
	 * {@link #afterUpload()}, at one condition: that it has been called once
	 * before {@link #afterUpload()} is called.
	 * 
	 * @see FileData#getUploadLength()
	 */
    public long getUploadLength() {
        return this.fileData.getUploadLength();
    }

    /** {@inheritDoc} */
    public boolean isPreparedForUpload() {
        return this.fileData.isPreparedForUpload();
    }

    /**
	 * @return the poisonned status. Returns always false, as this instance is a
	 *         true one. false indicates the 'End Of Queue' marker in the
	 *         preparedFileQueue, which is not the case here
	 * @see UploadFileDataPoisonned
	 */
    public boolean isPoisonned() {
        return false;
    }

    /**
	 * @return the numOfFileInCurrentUpload
	 */
    public int getNumOfFileInCurrentUpload() {
        return numOfFileInCurrentUpload;
    }
}
