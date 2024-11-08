package org.javaseis.seiszip;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.FloatBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.zip.DataFormatException;

/**
 * This class uncompresses SEGY files that were compressed using 2D SeisPEG.
 */
public class SegyUnzip {

    private static final Logger LOG = Logger.getLogger("org.javaseis.seiszip");

    private FileInputStream _inputStream;

    private FileOutputStream _outputStream;

    private FileChannel _inputChannel;

    private FileChannel _outputChannel;

    private long _nbytesRead = 0L;

    private long _nbytesWritten = 0L;

    private SeisPEG _seisPEG;

    private int _nbytesPerTrace;

    private float[][] _traces;

    private int[][] _hdrs;

    private byte[] _compressedHdrBytes;

    private byte[] _compressedTrcBytes;

    private ByteBuffer _compressedHdrByteBuffer;

    private ByteBuffer _compressedTrcByteBuffer;

    private ByteBuffer _workBuffer;

    private long _frameWrittenCount = 0L;

    /**
   * Constructor for uncompression a file.  Writes the reel header and binary header to the output file.
   *
   * @param  inFile  input compressed file.
   * @param  outFile  output SEGY file.  Any existing file is written over.
   */
    public SegyUnzip(String inFile, String outFile) throws IOException {
        _inputStream = new FileInputStream(inFile);
        _inputChannel = _inputStream.getChannel();
        _outputStream = new FileOutputStream(outFile);
        _outputChannel = _outputStream.getChannel();
        _workBuffer = ByteBuffer.allocate(8);
        _workBuffer.order(ByteOrder.BIG_ENDIAN);
        _workBuffer.position(0);
        int nRead = _inputChannel.read(_workBuffer);
        _nbytesRead += nRead;
        if (nRead != 8) throw new IOException("Error reading file header: " + nRead + "!=8");
        int cookie = _workBuffer.getInt(0);
        if (cookie != SegyZip.COOKIE_V1) throw new RuntimeException("Sorry - you are trying to unzip data from an invalid or corrupted file " + "(contact ddiller@tierrageo.com if this is unacceptable) " + cookie + " " + SegyZip.COOKIE_V1);
        ByteBuffer reelHdrBuffer = ByteBuffer.allocate(SegyZip.LEN_REEL_HDR);
        nRead = _inputChannel.read(reelHdrBuffer);
        _nbytesRead += nRead;
        if (nRead != SegyZip.LEN_REEL_HDR) throw new IOException("Error reading SEG-Y reel header: " + nRead + "!=" + SegyZip.LEN_REEL_HDR);
        reelHdrBuffer.position(0);
        int nWritten = _outputChannel.write(reelHdrBuffer);
        _nbytesWritten += nWritten;
        if (nWritten != SegyZip.LEN_REEL_HDR) throw new IOException("Error writing SEG-Y reel header: " + nWritten + "!=" + SegyZip.LEN_REEL_HDR);
        ByteBuffer binaryHdrBuffer = ByteBuffer.allocate(SegyZip.LEN_BINARY_HDR);
        nRead = _inputChannel.read(binaryHdrBuffer);
        _nbytesRead += nRead;
        if (nRead != SegyZip.LEN_BINARY_HDR) throw new IOException("Error reading SEG-Y binary header: " + nRead + "!=" + SegyZip.LEN_BINARY_HDR);
        binaryHdrBuffer.position(0);
        nWritten = _outputChannel.write(binaryHdrBuffer);
        _nbytesWritten += nWritten;
        if (nWritten != SegyZip.LEN_BINARY_HDR) throw new IOException("Error writing SEG-Y binary header: " + nWritten + "!=" + SegyZip.LEN_BINARY_HDR);
        binaryHdrBuffer.order(ByteOrder.BIG_ENDIAN);
        int tracesPerFrame = binaryHdrBuffer.getShort(12);
        System.out.println("tracesPerFrame= " + tracesPerFrame);
        float sampleInterval = (float) binaryHdrBuffer.getShort(16) / 1000.0F;
        System.out.println("sampleInterval= " + sampleInterval);
        int samplesPerTrace = (int) binaryHdrBuffer.getShort(20);
        System.out.println("samplesPerTrace= " + samplesPerTrace);
        _nbytesPerTrace = samplesPerTrace * 4;
        _traces = new float[tracesPerFrame][samplesPerTrace];
        _hdrs = new int[tracesPerFrame][SegyZip.NINTS_PER_HDR];
        _compressedHdrBytes = new byte[HdrCompressor.getOutputBufferSize(SegyZip.NINTS_PER_HDR, tracesPerFrame)];
        _compressedHdrByteBuffer = ByteBuffer.wrap(_compressedHdrBytes);
        _compressedTrcBytes = new byte[samplesPerTrace * tracesPerFrame * 4];
        _compressedTrcByteBuffer = ByteBuffer.wrap(_compressedTrcBytes);
    }

    /**
   * Performs the uncompression and closes the files.
   */
    public void unzip() throws IOException {
        ByteBuffer hdrByteBuffer = ByteBuffer.allocateDirect(SegyZip.NBYTES_PER_HDR);
        hdrByteBuffer.order(ByteOrder.BIG_ENDIAN);
        ByteBuffer trcByteBuffer = ByteBuffer.allocateDirect(_nbytesPerTrace);
        trcByteBuffer.order(ByteOrder.BIG_ENDIAN);
        IntBuffer hdrBuffer = hdrByteBuffer.asIntBuffer();
        FloatBuffer trcBuffer = trcByteBuffer.asFloatBuffer();
        boolean finished = false;
        long traceInFileCount = 1L;
        while (!finished) {
            _workBuffer.position(0);
            int nRead = _inputChannel.read(_workBuffer);
            _nbytesRead += nRead;
            if (nRead == -1) {
                finished = true;
            } else {
                if (nRead != 8) throw new IOException("Error reading mini header near trace " + traceInFileCount + ": " + nRead + "!=8");
                int nbytesHdrs = _workBuffer.getInt(0);
                int nbytesTraces = _workBuffer.getInt(4);
                _compressedHdrByteBuffer.position(0);
                _compressedHdrByteBuffer.limit(nbytesHdrs);
                nRead = _inputChannel.read(_compressedHdrByteBuffer);
                _nbytesRead += nRead;
                if (nRead != nbytesHdrs) throw new IOException("Error reading compressed headers near trace " + traceInFileCount + ": " + nRead + "!=" + nbytesHdrs);
                _compressedTrcByteBuffer.position(0);
                _compressedTrcByteBuffer.limit(nbytesTraces);
                nRead = _inputChannel.read(_compressedTrcByteBuffer);
                _nbytesRead += nRead;
                if (nRead != nbytesTraces) throw new IOException("Error reading compressed data near trace " + traceInFileCount + ": " + nRead + "!=" + nbytesTraces);
                if (_seisPEG == null) _seisPEG = new SeisPEG(_compressedTrcBytes);
                int tracesInFrame = 0;
                try {
                    tracesInFrame = _seisPEG.uncompressHdrs(_compressedHdrBytes, nbytesHdrs, _hdrs);
                } catch (DataFormatException e) {
                    throw new IOException(e.toString());
                }
                _seisPEG.uncompress(_compressedTrcBytes, nbytesTraces, _traces);
                for (int j = 0; j < tracesInFrame; j++) {
                    hdrBuffer.position(0);
                    hdrBuffer.put(_hdrs[j]);
                    trcBuffer.position(0);
                    trcBuffer.put(_traces[j]);
                    hdrByteBuffer.position(0);
                    int nWritten = _outputChannel.write(hdrByteBuffer);
                    _nbytesWritten += nWritten;
                    if (nWritten != SegyZip.NBYTES_PER_HDR) throw new IOException("Error writing SEG-Y trace header " + traceInFileCount + ": " + nWritten + "!=" + SegyZip.NBYTES_PER_HDR);
                    trcByteBuffer.position(0);
                    nWritten = _outputChannel.write(trcByteBuffer);
                    _nbytesWritten += nWritten;
                    if (nWritten != _nbytesPerTrace) throw new IOException("Error writing SEG-Y trace " + traceInFileCount + ": " + nWritten + "!=" + _nbytesPerTrace);
                    traceInFileCount++;
                }
                _frameWrittenCount++;
                if (_frameWrittenCount % 1000 == 0) System.out.println("Finished uncompressing and writing frame " + _frameWrittenCount + " ...");
            }
        }
        _inputStream.close();
        _outputStream.close();
    }

    /**
   * Returns the uncompression ratio.
   *
   * @return  the uncompression ratio.
   */
    public double getUncompressionRatio() {
        return (double) _nbytesWritten / (double) _nbytesRead;
    }

    /**
   * Command line interface.  Call with zero-length args to see usage.
   */
    public static void main(String[] args) {
        if (args.length != 2) throw new IllegalArgumentException("Usage: java SegyUnzip zippedInputFile segyOutputFile");
        try {
            String inFile = args[0];
            String outFile = args[1];
            SegyUnzip segyUnzip = new SegyUnzip(inFile, outFile);
            segyUnzip.unzip();
            System.out.println("Data expansion ratio= " + segyUnzip.getUncompressionRatio());
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
