package org.dcm4chex.archive.codec;

import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmEncodeParam;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.DcmParser;
import org.dcm4che.data.DcmParserFactory;
import org.dcm4che.data.FileFormat;
import org.dcm4che.data.FileMetaInfo;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.util.BufferedOutputStream;
import org.dcm4cheri.image.ImageReaderFactory;
import org.dcm4cheri.image.ItemParser;
import org.dcm4cheri.imageio.plugins.PatchJpegLSImageInputStream;
import EDU.oswego.cs.dl.util.concurrent.FIFOSemaphore;
import com.sun.media.imageio.stream.SegmentedImageInputStream;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @author Franz Willer <franz.willer@agfa.com>
 * @version $Revision: 16585 $ $Date: 2012-02-13 16:23:27 -0500 (Mon, 13 Feb 2012) $
 * @since 22.05.2004
 * 
 */
public class DecompressCmd extends CodecCmd {

    private static final String J2KIMAGE_READER = "com.sun.media.imageioimpl.plugins.jpeg2000.J2KImageReader";

    private final boolean clampPixelValue;

    private final ItemParser itemParser;

    private final ImageInputStream iis;

    private int[] simpleFrameList;

    private static int maxConcurrentDecompress = 1;

    private static FIFOSemaphore decompressSemaphore = new FIFOSemaphore(maxConcurrentDecompress);

    private static AtomicInteger nrOfConcurrentDecompress = new AtomicInteger();

    public static void setMaxConcurrentDecompression(int maxConcurrentDecompress) {
        decompressSemaphore = new FIFOSemaphore(maxConcurrentDecompress);
        DecompressCmd.maxConcurrentDecompress = maxConcurrentDecompress;
    }

    public static int getMaxConcurrentDecompression() {
        return DecompressCmd.maxConcurrentDecompress;
    }

    public static byte[] decompressFile(File inFile, File outFile, String outTS, int planarConfiguration, int pxdataVR, byte[] buffer) throws Exception {
        log.info("M-READ file:" + inFile);
        FileImageInputStream fiis = new FileImageInputStream(inFile);
        try {
            DcmParser parser = DcmParserFactory.getInstance().newDcmParser(fiis);
            DcmObjectFactory dof = DcmObjectFactory.getInstance();
            Dataset ds = dof.newDataset();
            parser.setDcmHandler(ds.getDcmHandler());
            parser.parseDcmFile(FileFormat.DICOM_FILE, Tags.PixelData);
            log.info("M-WRITE file:" + outFile);
            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestOutputStream dos = new DigestOutputStream(new FileOutputStream(outFile), md);
            BufferedOutputStream bos = new BufferedOutputStream(dos, buffer);
            try {
                DcmEncodeParam encParam = DcmEncodeParam.valueOf(outTS);
                String inTS = getTransferSyntax(ds);
                adjustPhotometricInterpretation(ds, inTS);
                if (planarConfiguration >= 0 && ds.contains(Tags.PlanarConfiguration)) ds.putUS(Tags.PlanarConfiguration, planarConfiguration);
                DecompressCmd cmd = new DecompressCmd(ds, inTS, parser);
                int len = cmd.getPixelDataLength();
                FileMetaInfo fmi = dof.newFileMetaInfo(ds, outTS);
                ds.setFileMetaInfo(fmi);
                ds.writeFile(bos, encParam);
                ds.writeHeader(bos, encParam, Tags.PixelData, pxdataVR, (len + 1) & ~1);
                try {
                    cmd.decompress(encParam.byteOrder, bos);
                } catch (IOException e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException("Decompression failed:", e);
                }
                if ((len & 1) != 0) bos.write(0);
                parser.parseDataset(parser.getDcmDecodeParam(), -1);
                ds.subSet(Tags.PixelData, -1).writeDataset(bos, encParam);
            } finally {
                bos.close();
            }
            return md.digest();
        } finally {
            try {
                fiis.close();
            } catch (IOException ignore) {
            }
        }
    }

    public static String getTransferSyntax(Dataset ds) {
        FileMetaInfo fmi = ds.getFileMetaInfo();
        return fmi != null ? fmi.getTransferSyntaxUID() : UIDs.ImplicitVRLittleEndian;
    }

    public DecompressCmd(Dataset ds, String tsuid, DcmParser parser) throws IOException {
        super(ds, tsuid);
        this.clampPixelValue = pixelRepresentation == 0 && bitsAllocated == 16 && bitsStored < 12 && UIDs.JPEGExtended.equals(tsuid);
        this.iis = parser.getImageInputStream();
        this.itemParser = createItemParser(tsuid, parser);
    }

    protected ItemParser createItemParser(String tsuid, DcmParser parser) throws IOException {
        return new ItemParser(parser, frames, tsuid);
    }

    public static void adjustPhotometricInterpretation(Dataset ds, String tsOrig) {
        String pmi = ds.getString(Tags.PhotometricInterpretation, "MONOCHROME2");
        if (pmi.startsWith("YBR") && (tsOrig.equals(UIDs.JPEGBaseline) || tsOrig.equals(UIDs.JPEGExtended) || tsOrig.equals(UIDs.JPEG2000Lossless) || tsOrig.equals(UIDs.JPEG2000Lossy))) {
            ds.putCS(Tags.PhotometricInterpretation, "RGB");
        }
    }

    public void decompress(ByteOrder byteOrder, OutputStream out) throws Exception {
        long t1 = System.currentTimeMillis();
        ImageReader reader = null;
        BufferedImage bi = null;
        boolean patchJpegLS = false;
        boolean semaphoreAquired = false;
        try {
            semaphoreAquired = acquireSemaphore();
            if (log.isDebugEnabled()) log.debug("codec semaphore acquired after " + (System.currentTimeMillis() - t1) + "ms!");
            log.info("start decompression of image: " + rows + "x" + columns + "x" + frames + " (current codec tasks: compress&decompress:" + nrOfConcurrentCodec.get() + " decompress:" + nrOfConcurrentDecompress.get() + ")");
            t1 = System.currentTimeMillis();
            ImageReaderFactory f = ImageReaderFactory.getInstance();
            reader = f.getReaderForTransferSyntax(tsuid);
            if (bitsAllocated == 16 && UIDs.JPEGLSLossless.equals(tsuid)) {
                String patchJAIJpegLS = f.patchJAIJpegLS();
                if (patchJAIJpegLS != null) patchJpegLS = patchJAIJpegLS.length() == 0 || patchJAIJpegLS.equals(implClassUID);
            }
            bi = getBufferedImage();
            for (int i = 0, n = getNumberOfFrames(); i < n; ++i) {
                int frame = simpleFrameList != null ? (simpleFrameList[i] - 1) : i;
                log.debug("start decompression of frame #" + (frame + 1));
                SegmentedImageInputStream siis = new SegmentedImageInputStream(iis, itemParser);
                itemParser.seekFrame(siis, frame);
                reader.setInput(patchJpegLS ? new PatchJpegLSImageInputStream(siis) : (ImageInputStream) siis);
                ImageReadParam param = createImageReadParam(reader);
                param.setDestination(bi);
                bi = reader.read(0, param);
                if (reader.getClass().getName().startsWith(J2KIMAGE_READER)) {
                    reader.dispose();
                    reader = i < n - 1 ? f.getReaderForTransferSyntax(tsuid) : null;
                } else {
                    reader.reset();
                }
                write(bi.getRaster(), out, byteOrder);
            }
            itemParser.seekFooter();
        } finally {
            if (reader != null) reader.dispose();
            if (bi != null) biPool.returnBufferedImage(bi);
            long t2 = System.currentTimeMillis();
            finished();
            log.info("finished decompression in " + (t2 - t1) + "ms." + " (remaining codec tasks: compress&decompress:" + nrOfConcurrentCodec + " decompress:" + nrOfConcurrentDecompress + ")");
            if (semaphoreAquired) releaseSemaphore();
        }
    }

    public static boolean acquireSemaphore() throws InterruptedException {
        if (log.isDebugEnabled()) {
            log.debug("acquire codec semaphore");
            log.debug("#####codecSemaphore.permits():" + codecSemaphore.permits());
            log.debug("#####decompressSemaphore.permits():" + decompressSemaphore.permits());
        }
        boolean success = false;
        codecSemaphore.acquire();
        try {
            decompressSemaphore.acquire();
            success = true;
            nrOfConcurrentDecompress.incrementAndGet();
        } finally {
            if (!success) codecSemaphore.release(); else nrOfConcurrentCodec.incrementAndGet();
        }
        return success;
    }

    public static void finished() {
        log.debug("finished: decrement nrOfConcurrentCodec and nrOfConcurrentDecompress");
        nrOfConcurrentCodec.decrementAndGet();
        nrOfConcurrentDecompress.decrementAndGet();
    }

    public static void releaseSemaphore() {
        decompressSemaphore.release();
        codecSemaphore.release();
    }

    public static int getNrOfConcurrentCodec() {
        return nrOfConcurrentCodec.get();
    }

    public static int getNrOfConcurrentDecompress() {
        return nrOfConcurrentDecompress.get();
    }

    private void write(WritableRaster raster, OutputStream out, ByteOrder byteOrder) throws IOException {
        DataBuffer buffer = raster.getDataBuffer();
        ComponentSampleModel sm = (ComponentSampleModel) raster.getSampleModel();
        checkSampleModel(sm);
        final int stride = getScanlineStride(sm);
        final int h = raster.getHeight();
        final int w = raster.getWidth();
        final int numBands = getNumBands(sm);
        final int numBanks = buffer.getNumBanks();
        final int l = w * numBands / numBanks;
        for (int b = 0; b < numBanks; b++) {
            switch(buffer.getDataType()) {
                case DataBuffer.TYPE_BYTE:
                    for (int i = 0; i < h; ++i) out.write(((DataBufferByte) buffer).getData(b), i * stride, l);
                    break;
                case DataBuffer.TYPE_USHORT:
                    if (byteOrder == ByteOrder.LITTLE_ENDIAN) for (int i = 0; i < h; ++i) writeShortLE(((DataBufferUShort) buffer).getData(b), i * stride, l, out); else for (int i = 0; i < h; ++i) writeShortBE(((DataBufferUShort) buffer).getData(b), i * stride, l, out);
                    break;
                case DataBuffer.TYPE_SHORT:
                    if (byteOrder == ByteOrder.LITTLE_ENDIAN) for (int i = 0; i < h; ++i) writeShortLE(((DataBufferShort) buffer).getData(b), i * stride, l, out); else for (int i = 0; i < h; ++i) writeShortBE(((DataBufferShort) buffer).getData(b), i * stride, l, out);
                    break;
                default:
                    throw new RuntimeException(buffer.getClass().getName() + " not supported");
            }
        }
    }

    protected int getNumBands(ComponentSampleModel sm) {
        return sm.getNumBands();
    }

    protected int getScanlineStride(ComponentSampleModel sm) {
        return sm.getScanlineStride();
    }

    private static int[] OFF_0 = { 0 };

    private static int[] OFF_0_0_0 = { 0, 0, 0 };

    private static int[] OFF_0_1_2 = { 0, 1, 2 };

    private void checkSampleModel(ComponentSampleModel sm) {
        int[] bandOffsets = sm.getBandOffsets();
        int[] bankIndices = sm.getBankIndices();
        if (!(Arrays.equals(OFF_0, bandOffsets) && Arrays.equals(OFF_0, bankIndices)) && !(Arrays.equals(OFF_0_0_0, bandOffsets) && Arrays.equals(OFF_0_1_2, bankIndices)) && !(Arrays.equals(OFF_0_0_0, bankIndices) && Arrays.equals(OFF_0_1_2, bandOffsets))) throw new RuntimeException(sm.getClass().getName() + " with bandOffsets=" + Arrays.asList(bandOffsets) + " with bankIndices=" + Arrays.asList(bankIndices) + " not supported");
    }

    private void writeShortLE(short[] data, int off, int len, OutputStream out) throws IOException {
        if (clampPixelValue) for (int i = off, end = off + len; i < end; i++) {
            final int px = Math.min(data[i] & 0xffff, maxVal);
            out.write(px & 0xff);
            out.write((px >>> 8) & 0xff);
        } else for (int i = off, end = off + len; i < end; i++) {
            final short px = data[i];
            out.write(px & 0xff);
            out.write((px >>> 8) & 0xff);
        }
    }

    private void writeShortBE(short[] data, int off, int len, OutputStream out) throws IOException {
        if (clampPixelValue) for (int i = off, end = off + len; i < end; i++) {
            final int px = Math.min(data[i] & 0xffff, maxVal);
            out.write((px >>> 8) & 0xff);
            out.write(px & 0xff);
        } else for (int i = off, end = off + len; i < end; i++) {
            final short px = data[i];
            out.write((px >>> 8) & 0xff);
            out.write(px & 0xff);
        }
    }

    public void setSimpleFrameList(int[] simpleFrameList) {
        this.simpleFrameList = simpleFrameList;
    }

    public int getPixelDataLength() {
        return getNumberOfFrames() * frameLength;
    }

    private int getNumberOfFrames() {
        return simpleFrameList != null ? simpleFrameList.length : frames;
    }

    protected ImageReadParam createImageReadParam(ImageReader imageReader) {
        return imageReader.getDefaultReadParam();
    }
}
