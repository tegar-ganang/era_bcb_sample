package org.ambiance.codec;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.CRC32;
import org.apache.commons.codec.BinaryDecoder;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.yenc.MultiPartYEnc;
import org.apache.commons.codec.binary.yenc.YEnc;
import org.apache.commons.codec.binary.yenc.YEncException;
import org.apache.commons.codec.binary.yenc.YEncFile;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

/**
 * @plexus.component role="org.ambiance.codec.Decoder" 
 *                   role-hint="yenc"
 * 
 */
public class YEncDecoder extends AbstractLogEnabled implements Decoder, Initializable {

    /**
	 * @plexus.configuration default-value="/tmp"
	 */
    private String outputDirName;

    private BinaryDecoder decoder;

    /**
	 * Decode a single file
	 */
    public void decode(File input) throws DecoderException {
        try {
            YEncFile yencFile = new YEncFile(input);
            StringBuffer outputName = new StringBuffer(outputDirName);
            outputName.append(File.separator);
            outputName.append(yencFile.getHeader().getName());
            RandomAccessFile output = new RandomAccessFile(outputName.toString(), "rw");
            long pos = yencFile.getDataBegin();
            yencFile.getInput().seek(pos);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (pos < yencFile.getDataEnd()) {
                baos.write(yencFile.getInput().read());
                pos++;
            }
            byte[] buff = decoder.decode(baos.toByteArray());
            output.write(buff);
            output.close();
            CRC32 crc32 = new CRC32();
            crc32.update(buff);
            if (!yencFile.getTrailer().getCrc32().equals(Long.toHexString(crc32.getValue()).toUpperCase())) throw new DecoderException("Error in CRC32 check.");
        } catch (YEncException ye) {
            throw new DecoderException("Input file is not a YEnc file or contains error : " + ye.getMessage());
        } catch (FileNotFoundException fnfe) {
            throw new DecoderException("Enable to create output file : " + fnfe.getMessage());
        } catch (IOException ioe) {
            throw new DecoderException("Enable to read input file : " + ioe.getMessage());
        }
    }

    /**
	 * Decode multiparts archives
	 * 
	 * @see org.ambiance.codec.YEncCodec#decode(java.io.File[])
	 */
    public void decode(File[] inputs) throws DecoderException {
        if (inputs.length <= 0) throw new DecoderException("inputs not be empty");
        try {
            MultiPartYEnc decoder = new MultiPartYEnc(inputs);
            decoder.decode(outputDirName);
        } catch (Exception e) {
            throw new DecoderException(e.getMessage());
        }
    }

    public void initialize() throws InitializationException {
        decoder = new YEnc();
    }
}
