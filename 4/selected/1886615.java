package com.kni.etl.ketl.writer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetEncoder;
import java.util.zip.GZIPOutputStream;
import com.kni.etl.dbutils.ResourcePool;

/**
 * The Class OutputFile.
 */
public class OutputFile {

    /** The stream. */
    private FileOutputStream stream;

    public OutputFile(String charSet, boolean zip, int miOutputBufferSize) {
        super();
        mCharSet = charSet;
        mZip = zip;
        this.miOutputBufferSize = miOutputBufferSize;
    }

    /** The channel. */
    private FileChannel channel;

    /** The writer. */
    public Writer writer;

    private OutputStream os;

    private BufferedOutputStream bos;

    private GZIPOutputStream zos;

    private String mCharSet;

    private boolean mZip;

    private int miOutputBufferSize;

    private File mFile;

    public File getFile() {
        return this.mFile;
    }

    /**
	 * Open.
	 * 
	 * @param filePath
	 *            the file path
	 * @throws IOException
	 */
    public String openTemp(File tmpDir) throws IOException {
        return this.open(File.createTempFile("ast", ".tmp", tmpDir));
    }

    public String open(String filePath) throws IOException {
        return this.open(new File(filePath));
    }

    public String open(File file) throws IOException {
        mFile = file;
        this.stream = new FileOutputStream(file);
        this.channel = this.stream.getChannel();
        CharsetEncoder charSet = (mCharSet == null ? java.nio.charset.Charset.defaultCharset().newEncoder() : java.nio.charset.Charset.forName(mCharSet).newEncoder());
        ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.INFO_MESSAGE, "Writing to file " + file.getAbsolutePath() + ", character set " + charSet.charset().displayName());
        os = java.nio.channels.Channels.newOutputStream(this.channel);
        bos = new BufferedOutputStream(os, miOutputBufferSize);
        zos = mZip ? new GZIPOutputStream(bos) : null;
        this.writer = new OutputStreamWriter(mZip ? zos : bos, charSet);
        return file.getAbsolutePath();
    }

    /**
	 * Close.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    public void close() throws IOException {
        this.writer.flush();
        this.writer.close();
        if (mZip) {
            zos.flush();
            zos.close();
        }
        bos.flush();
        bos.close();
        os.flush();
        os.close();
        this.channel.close();
        this.stream.close();
    }

    public Writer getWriter() {
        return this.writer;
    }
}
