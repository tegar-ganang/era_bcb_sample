package entagged.audioformats.flac.util;

import entagged.audioformats.*;
import entagged.audioformats.exceptions.*;
import entagged.audioformats.ogg.OggTag;
import java.io.*;
import java.nio.channels.*;
import java.util.*;

public class FlacTagWriter {

    private Vector metadataBlockPadding = new Vector(1);

    private Vector metadataBlockApplication = new Vector(1);

    private Vector metadataBlockSeekTable = new Vector(1);

    private Vector metadataBlockCueSheet = new Vector(1);

    private FlacTagCreator tc = new FlacTagCreator();

    private FlacTagReader reader = new FlacTagReader();

    public void delete(RandomAccessFile raf, RandomAccessFile tempRaf) throws IOException, CannotWriteException {
        OggTag tag = null;
        try {
            tag = reader.read(raf);
        } catch (CannotReadException e) {
            write(new OggTag(), raf, tempRaf);
            return;
        }
        OggTag emptyTag = new OggTag();
        emptyTag.setVendor(tag.getVendor());
        raf.seek(0);
        tempRaf.seek(0);
        write(emptyTag, raf, tempRaf);
    }

    public void write(Tag tag, RandomAccessFile raf, RandomAccessFile rafTemp) throws CannotWriteException, IOException {
        metadataBlockPadding.removeAllElements();
        metadataBlockApplication.removeAllElements();
        metadataBlockSeekTable.removeAllElements();
        metadataBlockCueSheet.removeAllElements();
        byte[] b = new byte[4];
        raf.readFully(b);
        String flac = new String(b);
        if (!flac.equals("fLaC")) throw new CannotWriteException("This is not a FLAC file");
        boolean isLastBlock = false;
        while (!isLastBlock) {
            b = new byte[4];
            raf.readFully(b);
            MetadataBlockHeader mbh = new MetadataBlockHeader(b);
            switch(mbh.getBlockType()) {
                case MetadataBlockHeader.VORBIS_COMMENT:
                    handlePadding(mbh, raf);
                    break;
                case MetadataBlockHeader.PADDING:
                    handlePadding(mbh, raf);
                    break;
                case MetadataBlockHeader.APPLICATION:
                    handleApplication(mbh, raf);
                    break;
                case MetadataBlockHeader.SEEKTABLE:
                    handleSeekTable(mbh, raf);
                    break;
                case MetadataBlockHeader.CUESHEET:
                    handleCueSheet(mbh, raf);
                    break;
                default:
                    skipBlock(mbh, raf);
                    break;
            }
            isLastBlock = mbh.isLastBlock();
        }
        int availableRoom = computeAvailableRoom();
        int newTagSize = tc.getTagLength(tag);
        int neededRoom = newTagSize + computeNeededRoom();
        raf.seek(0);
        if (availableRoom >= neededRoom) {
            raf.seek(42);
            for (int i = 0; i < metadataBlockApplication.size(); i++) {
                raf.write(((MetadataBlock) metadataBlockApplication.elementAt(i)).getHeader().getBytes());
                raf.write(((MetadataBlock) metadataBlockApplication.elementAt(i)).getData().getBytes());
            }
            for (int i = 0; i < metadataBlockSeekTable.size(); i++) {
                raf.write(((MetadataBlock) metadataBlockSeekTable.elementAt(i)).getHeader().getBytes());
                raf.write(((MetadataBlock) metadataBlockSeekTable.elementAt(i)).getData().getBytes());
            }
            for (int i = 0; i < metadataBlockCueSheet.size(); i++) {
                raf.write(((MetadataBlock) metadataBlockCueSheet.elementAt(i)).getHeader().getBytes());
                raf.write(((MetadataBlock) metadataBlockCueSheet.elementAt(i)).getData().getBytes());
            }
            raf.getChannel().write(tc.convert(tag, availableRoom - neededRoom));
        } else {
            FileChannel fc = raf.getChannel();
            b = new byte[42];
            raf.readFully(b);
            raf.seek(availableRoom + 42);
            FileChannel tempFC = rafTemp.getChannel();
            rafTemp.write(b);
            for (int i = 0; i < metadataBlockApplication.size(); i++) {
                rafTemp.write(((MetadataBlock) metadataBlockApplication.elementAt(i)).getHeader().getBytes());
                rafTemp.write(((MetadataBlock) metadataBlockApplication.elementAt(i)).getData().getBytes());
            }
            for (int i = 0; i < metadataBlockSeekTable.size(); i++) {
                rafTemp.write(((MetadataBlock) metadataBlockSeekTable.elementAt(i)).getHeader().getBytes());
                rafTemp.write(((MetadataBlock) metadataBlockSeekTable.elementAt(i)).getData().getBytes());
            }
            for (int i = 0; i < metadataBlockCueSheet.size(); i++) {
                rafTemp.write(((MetadataBlock) metadataBlockCueSheet.elementAt(i)).getHeader().getBytes());
                rafTemp.write(((MetadataBlock) metadataBlockCueSheet.elementAt(i)).getData().getBytes());
            }
            rafTemp.write(tc.convert(tag, FlacTagCreator.DEFAULT_PADDING).array());
            tempFC.transferFrom(fc, tempFC.position(), fc.size());
        }
    }

    private int computeAvailableRoom() {
        int length = 0;
        for (int i = 0; i < metadataBlockApplication.size(); i++) length += ((MetadataBlock) metadataBlockApplication.elementAt(i)).getLength();
        for (int i = 0; i < metadataBlockSeekTable.size(); i++) length += ((MetadataBlock) metadataBlockSeekTable.elementAt(i)).getLength();
        for (int i = 0; i < metadataBlockCueSheet.size(); i++) length += ((MetadataBlock) metadataBlockCueSheet.elementAt(i)).getLength();
        for (int i = 0; i < metadataBlockPadding.size(); i++) length += ((MetadataBlock) metadataBlockPadding.elementAt(i)).getLength();
        return length;
    }

    private int computeNeededRoom() {
        int length = 0;
        for (int i = 0; i < metadataBlockApplication.size(); i++) length += ((MetadataBlock) metadataBlockApplication.elementAt(i)).getLength();
        for (int i = 0; i < metadataBlockSeekTable.size(); i++) length += ((MetadataBlock) metadataBlockSeekTable.elementAt(i)).getLength();
        for (int i = 0; i < metadataBlockCueSheet.size(); i++) length += ((MetadataBlock) metadataBlockCueSheet.elementAt(i)).getLength();
        return length;
    }

    private void skipBlock(MetadataBlockHeader mbh, RandomAccessFile raf) throws IOException {
        raf.seek(raf.getFilePointer() + mbh.getDataLength());
    }

    private void handlePadding(MetadataBlockHeader mbh, RandomAccessFile raf) throws IOException {
        raf.seek(raf.getFilePointer() + mbh.getDataLength());
        MetadataBlockData mbd = new MetadataBlockDataPadding(mbh.getDataLength());
        metadataBlockPadding.add(new MetadataBlock(mbh, mbd));
    }

    private void handleApplication(MetadataBlockHeader mbh, RandomAccessFile raf) throws IOException {
        byte[] b = new byte[mbh.getDataLength()];
        raf.readFully(b);
        MetadataBlockData mbd = new MetadataBlockDataApplication(b);
        metadataBlockApplication.add(new MetadataBlock(mbh, mbd));
    }

    private void handleSeekTable(MetadataBlockHeader mbh, RandomAccessFile raf) throws IOException {
        byte[] b = new byte[mbh.getDataLength()];
        raf.readFully(b);
        MetadataBlockData mbd = new MetadataBlockDataSeekTable(b);
        metadataBlockSeekTable.add(new MetadataBlock(mbh, mbd));
    }

    private void handleCueSheet(MetadataBlockHeader mbh, RandomAccessFile raf) throws IOException {
        byte[] b = new byte[mbh.getDataLength()];
        raf.readFully(b);
        MetadataBlockData mbd = new MetadataBlockDataCueSheet(b);
        metadataBlockCueSheet.add(new MetadataBlock(mbh, mbd));
    }
}
