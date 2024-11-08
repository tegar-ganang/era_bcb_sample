package spec.io;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import spec.lib.MathPlus;
import spec.main.SpecConstants;
import spec.spectrum.SpecChannel;
import spec.spectrum.Spectrum;
import spec.spectrum.SpectrumException;

public class SpecFormat_spc extends SpecFormat implements Cloneable, Serializable {

    public static final long NAME_OFFSET = 0;

    public static final long TIME_OFFSET = 122;

    public static final long DATE_OFFSET = 130;

    public static final long DIM_OFFSET = 142;

    public static final long XLENGTH_OFFSET = 146;

    public static final long YLENGTH_OFFSET = 150;

    public static final long ORIG_LOADFORMAT_OFFSET = 154;

    public static final long LOADFORMAT_OFFSET = 162;

    public static final long NUMNONZERO_OFFSET = 164;

    public static final long FORMATINFO_OFFSET = 224;

    public static final long TOTALCOUNT_OFFSET = 896;

    public static final long BITMASKRECORD_OFFSET = 1024;

    public static final int MUSORT = 2000;

    public static final int SMAUG = 2001;

    public static final String encoding = "ASCII";

    /** This one is supported */
    public SpecFormat_spc() {
        super();
        supported = true;
    }

    public String toString() {
        return "SPCLIB FORMAT";
    }

    public int formatAndDimension(File aFile) throws SpecFormat.IOException {
        return (checkFormatAndDimension(aFile));
    }

    public Spectrum read(File aFile) throws SpecFormat.IOException {
        return (readSpc(aFile));
    }

    public Spectrum read(File aFile, int aSpecDimension) throws SpecFormat.IOException {
        return (readSpc(aFile, aSpecDimension));
    }

    public boolean write(File aFile, Spectrum aSpectrum) throws SpecFormat.IOException {
        return (writeSpc(aFile, aSpectrum));
    }

    /** get the default extension for this spectrum format */
    public String getExtension() {
        return SpecConstants.ORIGINAL_SPC_FORMAT_EXTENSION;
    }

    /**
   * Returns the spectrum dimension if right format ...
   * or throws a "SpecFormat_spc.IOException" if wrong format
   * format is correct if
   * 1) "short" read at DIM_OFFSET: is 1 or 2
   * 2) "short" read at ORIG_LOADFORMAT_OFFSET: is 1 or 2
   */
    public static int checkFormatAndDimension(File aFile) throws SpecFormat_spc.IOException {
        RandomAccessFile theSpecFile;
        short tempDimension = -1;
        short tempOrigLoadFormat = -1;
        try {
            theSpecFile = new RandomAccessFile(aFile, "r");
            theSpecFile.seek(SpecFormat_spc.DIM_OFFSET);
            tempDimension = theSpecFile.readShort();
            theSpecFile.seek(SpecFormat_spc.ORIG_LOADFORMAT_OFFSET);
            tempOrigLoadFormat = theSpecFile.readShort();
            theSpecFile.close();
            tempDimension = MathPlus.byteSwap(tempDimension);
            tempOrigLoadFormat = MathPlus.byteSwap(tempOrigLoadFormat);
            if (!(tempDimension == 1 || tempDimension == 2) && (tempOrigLoadFormat == 1 || tempOrigLoadFormat == 2)) {
                throw new SpecFormat_spc.IOException("This file does not fit the spclib format");
            }
            return ((int) tempDimension);
        } catch (java.io.IOException e) {
            throw new SpecFormat_spc.IOException("Could not determine the dimension when parsing the file\n", e);
        }
    }

    public static Spectrum readSpc(File aFile) throws SpecFormat_spc.IOException {
        int specDimension = checkFormatAndDimension(aFile);
        return (readSpc(aFile, specDimension));
    }

    public static Spectrum readSpc(File aFile, int specDimension) throws SpecFormat_spc.IOException {
        if (specDimension == 1) {
            return (SpecFormat_spc.OneDimFilter.read(aFile));
        } else if (specDimension == 2) {
            return (SpecFormat_spc.TwoDimFilter.read(aFile));
        } else {
            throw new SpecFormat_spc.IOException("Error reading spectrum from file ... the spectrum dimension: " + specDimension + " is not supported");
        }
    }

    public boolean writeSpc(File aFile, Spectrum aSpectrum) throws SpecFormat_spc.IOException {
        if (aSpectrum instanceof Spectrum.OneDim) {
            SpecFormat_spc.OneDimFilter aOneDimFilter = new SpecFormat_spc.OneDimFilter();
            return (aOneDimFilter.write(aFile, (Spectrum.OneDim) aSpectrum));
        } else if (aSpectrum instanceof Spectrum.TwoDim) {
            SpecFormat_spc.TwoDimFilter aTwoDimFilter = new SpecFormat_spc.TwoDimFilter();
            return (aTwoDimFilter.write(aFile, (Spectrum.TwoDim) aSpectrum));
        } else return false;
    }

    /**
   * USED WHILE READING SPECTRA
   * Given four bytes which were read in order, returns the intended Total Counts 
   */
    protected static int getTotalCountFromBytes(byte b1, byte b2, byte b3, byte b4) {
        int count = ((b2 & 0xff) << 24) | ((b1 & 0xff) << 16) | ((b4 & 0xff) << 8) | (b3 & 0xff);
        return count;
    }

    /**
   * USED WHILE WRITING SPECTRA
   * Given an integer, return an array of 4 bytes to be written
   */
    protected static byte[] getBytesFromTotalCount(int i) {
        byte[] b = new byte[4];
        b[0] = (byte) ((i >>> 16) & 0xff);
        b[1] = (byte) ((i >>> 24) & 0xff);
        b[2] = (byte) (i & 0xff);
        b[3] = (byte) ((i >>> 8) & 0xff);
        return b;
    }

    /**
   * handles reading and writing the spc header information
   */
    protected static class Header {

        protected String headerInfo;

        protected String specName;

        protected String creationTime;

        protected String creationDate;

        protected short dimension;

        protected short xLength;

        protected short yLength;

        protected short origLoadFormatIndicator;

        protected short loadFormatIndicator;

        protected short numBitmaskRecord;

        protected int count;

        protected int numNonZeroElements;

        protected String fileFormatInfo;

        /**
     * Reads the spectrum header information from the file into the variables
     */
        protected void read(RandomAccessFile theSpecFile) throws java.io.IOException {
            byte[] byteHeaderInfo = new byte[1024];
            theSpecFile.seek(SpecFormat_spc.NAME_OFFSET);
            theSpecFile.readFully(byteHeaderInfo, (int) SpecFormat_spc.NAME_OFFSET, 120);
            headerInfo = new String(byteHeaderInfo, 0, 120, encoding);
            theSpecFile.seek(SpecFormat_spc.NAME_OFFSET);
            theSpecFile.readFully(byteHeaderInfo, (int) SpecFormat_spc.NAME_OFFSET, 80);
            specName = new String(byteHeaderInfo, (int) SpecFormat_spc.NAME_OFFSET, 80, encoding);
            theSpecFile.seek(SpecFormat_spc.TIME_OFFSET);
            theSpecFile.readFully(byteHeaderInfo, (int) SpecFormat_spc.TIME_OFFSET, 8);
            creationTime = new String(byteHeaderInfo, (int) SpecFormat_spc.TIME_OFFSET, 8, encoding);
            theSpecFile.seek(SpecFormat_spc.DATE_OFFSET);
            theSpecFile.readFully(byteHeaderInfo, (int) SpecFormat_spc.DATE_OFFSET, 9);
            creationDate = new String(byteHeaderInfo, (int) SpecFormat_spc.DATE_OFFSET, 9, encoding);
            theSpecFile.seek(SpecFormat_spc.DIM_OFFSET);
            dimension = theSpecFile.readShort();
            dimension = MathPlus.byteSwap(dimension);
            theSpecFile.seek(SpecFormat_spc.XLENGTH_OFFSET);
            xLength = theSpecFile.readShort();
            xLength = MathPlus.byteSwap(xLength);
            theSpecFile.seek(SpecFormat_spc.YLENGTH_OFFSET);
            yLength = theSpecFile.readShort();
            yLength = MathPlus.byteSwap(yLength);
            yLength = (short) Math.max((int) yLength, 1);
            theSpecFile.seek(SpecFormat_spc.ORIG_LOADFORMAT_OFFSET);
            origLoadFormatIndicator = theSpecFile.readShort();
            origLoadFormatIndicator = MathPlus.byteSwap(origLoadFormatIndicator);
            theSpecFile.seek(SpecFormat_spc.LOADFORMAT_OFFSET);
            loadFormatIndicator = theSpecFile.readShort();
            loadFormatIndicator = MathPlus.byteSwap(loadFormatIndicator);
            theSpecFile.seek(SpecFormat_spc.NUMNONZERO_OFFSET);
            numNonZeroElements = theSpecFile.readInt();
            numNonZeroElements = MathPlus.byteSwap(numNonZeroElements);
            theSpecFile.seek(SpecFormat_spc.FORMATINFO_OFFSET);
            theSpecFile.readFully(byteHeaderInfo, (int) SpecFormat_spc.FORMATINFO_OFFSET, 130);
            fileFormatInfo = new String(byteHeaderInfo, (int) SpecFormat_spc.FORMATINFO_OFFSET, 130, encoding);
            theSpecFile.seek(SpecFormat_spc.TOTALCOUNT_OFFSET);
            byte b1, b2, b3, b4;
            b1 = theSpecFile.readByte();
            theSpecFile.seek(SpecFormat_spc.TOTALCOUNT_OFFSET + 1);
            b2 = theSpecFile.readByte();
            theSpecFile.seek(SpecFormat_spc.TOTALCOUNT_OFFSET + 2);
            b3 = theSpecFile.readByte();
            theSpecFile.seek(SpecFormat_spc.TOTALCOUNT_OFFSET + 3);
            b4 = theSpecFile.readByte();
            count = SpecFormat_spc.getTotalCountFromBytes(b1, b2, b3, b4);
            numBitmaskRecord = (short) ((xLength * yLength - 1) / 8192 + 1);
        }

        /**
     * Called by the other, more specific write methods to write the init beginning
     * header information for a spectrum
     */
        private void write(RandomAccessFile theSpecFile, Spectrum aSpectrum) throws java.io.IOException {
            theSpecFile.seek(SpecFormat_spc.NAME_OFFSET);
            specName = aSpectrum.specName;
            theSpecFile.write((specName + "\n").getBytes(encoding));
            SimpleDateFormat aDateFormat = new SimpleDateFormat("dd-MMM-yy hh:mm:ss");
            String dateTimeString = aDateFormat.format(aSpectrum.getSpecDate());
            theSpecFile.seek(SpecFormat_spc.TIME_OFFSET);
            creationTime = dateTimeString.substring(10);
            theSpecFile.write(creationTime.getBytes(encoding));
            theSpecFile.seek(SpecFormat_spc.DATE_OFFSET);
            creationDate = dateTimeString.substring(0, 9);
            theSpecFile.write(creationDate.getBytes(encoding));
            theSpecFile.seek(SpecFormat_spc.DIM_OFFSET);
            dimension = (short) aSpectrum.getSpecDimension();
            dimension = MathPlus.byteSwap(dimension);
            theSpecFile.writeShort(dimension);
        }

        /**
     * Writes the Spectrum (One Dimensional) to the spectrum file
     */
        protected void write(RandomAccessFile theSpecFile, Spectrum.OneDim aOneDSpectrum) throws java.io.IOException {
            write(theSpecFile, (Spectrum) aOneDSpectrum);
            xLength = (short) aOneDSpectrum.getSpecChannel().getSpecShape();
            theSpecFile.seek(SpecFormat_spc.XLENGTH_OFFSET);
            theSpecFile.writeShort(MathPlus.byteSwap(xLength));
            yLength = 0;
            theSpecFile.seek(SpecFormat_spc.YLENGTH_OFFSET);
            theSpecFile.writeShort(MathPlus.byteSwap(yLength));
            numBitmaskRecord = (short) ((xLength * 1 - 1) / 8192 + 1);
            StringBuffer sb = new StringBuffer();
            sb.append("CANPS FORMAT 1.0.1986 NSCL-MSU; SPCLIB; PLATFORM-INDEPENDENT; ");
            sb.append("MAX REC LENGTH=1KB FOLLOWING RECORDS ARE OF TYPE: ");
            for (short i = 0; i < numBitmaskRecord; i++) {
                sb.append(" U1");
            }
            sb.append(" H2");
            fileFormatInfo = sb.toString();
            theSpecFile.seek(SpecFormat_spc.FORMATINFO_OFFSET);
            theSpecFile.write(fileFormatInfo.getBytes(encoding));
            int maxChannelCount = aOneDSpectrum.getMaxCount();
            if (maxChannelCount < Short.MAX_VALUE) loadFormatIndicator = origLoadFormatIndicator = 2; else loadFormatIndicator = origLoadFormatIndicator = 4;
            theSpecFile.seek(SpecFormat_spc.ORIG_LOADFORMAT_OFFSET);
            theSpecFile.writeShort(MathPlus.byteSwap(origLoadFormatIndicator));
            theSpecFile.seek(SpecFormat_spc.LOADFORMAT_OFFSET);
            theSpecFile.writeShort(MathPlus.byteSwap(loadFormatIndicator));
        }

        /**
     * Writes the Spectrum (Two Dimensional) to the spectrum file
     */
        protected void write(RandomAccessFile theSpecFile, Spectrum.TwoDim aTwoDSpectrum) throws java.io.IOException {
            write(theSpecFile, (Spectrum) aTwoDSpectrum);
            xLength = (short) aTwoDSpectrum.getSpecChannel().getSpecShape1();
            theSpecFile.seek(SpecFormat_spc.XLENGTH_OFFSET);
            theSpecFile.writeShort(MathPlus.byteSwap(xLength));
            yLength = (short) aTwoDSpectrum.getSpecChannel().getSpecShape0();
            theSpecFile.seek(SpecFormat_spc.YLENGTH_OFFSET);
            theSpecFile.writeShort(MathPlus.byteSwap(yLength));
            numBitmaskRecord = (short) ((xLength * yLength - 1) / 8192 + 1);
            StringBuffer sb = new StringBuffer();
            sb.append("CANPS FORMAT 1.0.1986 NSCL-MSU; SPCLIB; PLATFORM-INDEPENDENT; ");
            sb.append("MAX REC LENGTH=1KB FOLLOWING RECORDS ARE OF TYPE: ");
            for (short i = 0; i < numBitmaskRecord; i++) {
                sb.append(" U1");
            }
            sb.append(" H2");
            fileFormatInfo = sb.toString();
            theSpecFile.seek(SpecFormat_spc.FORMATINFO_OFFSET);
            theSpecFile.write(fileFormatInfo.getBytes(encoding));
            int maxChannelCount = aTwoDSpectrum.getMaxCount();
            if (maxChannelCount < Short.MAX_VALUE) loadFormatIndicator = origLoadFormatIndicator = 2; else loadFormatIndicator = origLoadFormatIndicator = 4;
            theSpecFile.seek(SpecFormat_spc.ORIG_LOADFORMAT_OFFSET);
            theSpecFile.writeShort(MathPlus.byteSwap(origLoadFormatIndicator));
            theSpecFile.seek(SpecFormat_spc.LOADFORMAT_OFFSET);
            theSpecFile.writeShort(MathPlus.byteSwap(loadFormatIndicator));
        }
    }

    /**
   * Handles reading and writing 2-d spectrum files in spc format
   */
    public static class OneDimFilter {

        public OneDimFilter() {
            super();
        }

        /**
     * Method to read a 1-dimensional Spectrum File and extract
     * necessary information to create a Spectrum.OneDim and return
     * RandomAccessFiles are declared to throw an IOException
     */
        public static Spectrum.OneDim read(File aFile) throws SpecFormat_spc.IOException {
            RandomAccessFile theSpecFile;
            try {
                theSpecFile = new RandomAccessFile(aFile, "r");
                Header header = new Header();
                header.read(theSpecFile);
                int[] arrayOfChannelCount;
                switch(header.loadFormatIndicator) {
                    case 0:
                        arrayOfChannelCount = getChannelsMUSORT(theSpecFile, header);
                        break;
                    default:
                        arrayOfChannelCount = getChannelsSMAUG(theSpecFile, header);
                        break;
                }
                theSpecFile.close();
                SimpleDateFormat aDateFormat = new SimpleDateFormat("dd-MMM-yy hh:mm:ss");
                String dateTimeString = header.creationDate + " " + header.creationTime;
                ParsePosition pos = new ParsePosition(0);
                Date specDate = aDateFormat.parse(dateTimeString, pos);
                return (new Spectrum.OneDim(header.specName, specDate, new SpecChannel.OneDim((int) header.xLength, arrayOfChannelCount)));
            } catch (java.io.IOException e) {
                throw new SpecFormat_spc.IOException("Could not read the spectrum from the named file\n\n", e);
            } catch (SpectrumException se) {
                throw new SpecFormat_spc.IOException("Could not read the spectrum from the named file\n\n", se);
            }
        }

        private static int[] getChannelsSMAUG(RandomAccessFile theSpecFile, Header aHeader) throws java.io.IOException {
            theSpecFile.seek(SpecFormat_spc.BITMASKRECORD_OFFSET);
            byte[] arrayOfBitmaskRecord = new byte[aHeader.numBitmaskRecord * 1024];
            theSpecFile.readFully(arrayOfBitmaskRecord);
            int[] arrayOfSetBit = SpecFormat.getArrayOfSetBit(arrayOfBitmaskRecord, aHeader.numNonZeroElements);
            theSpecFile.seek((aHeader.numBitmaskRecord + 1) * 1024);
            int[] arrayOfChannelCount = new int[aHeader.xLength];
            int maxChannelCount = Integer.MIN_VALUE;
            int xptr;
            for (int i = 0; i < aHeader.numNonZeroElements; i++) {
                xptr = arrayOfSetBit[i];
                switch(aHeader.loadFormatIndicator) {
                    case 2:
                        arrayOfChannelCount[xptr] = (int) MathPlus.byteSwap(theSpecFile.readShort());
                        break;
                    case 4:
                        arrayOfChannelCount[xptr] = (int) MathPlus.byteSwap(theSpecFile.readInt());
                        break;
                    case 1:
                        arrayOfChannelCount[xptr] = (int) theSpecFile.readByte();
                        break;
                    default:
                        arrayOfChannelCount[xptr] = (int) MathPlus.byteSwap(theSpecFile.readShort());
                        break;
                }
                maxChannelCount = Math.max(maxChannelCount, arrayOfChannelCount[xptr]);
            }
            return arrayOfChannelCount;
        }

        private static int[] getChannelsMUSORT(RandomAccessFile theSpecFile, Header aHeader) throws java.io.IOException {
            theSpecFile.seek(SpecFormat_spc.BITMASKRECORD_OFFSET);
            int[] arrayOfChannelCount = new int[aHeader.xLength];
            int maxChannelCount = Integer.MIN_VALUE;
            for (int i = 0; i < aHeader.xLength; i++) {
                switch(aHeader.origLoadFormatIndicator) {
                    case 2:
                        arrayOfChannelCount[i] = (int) MathPlus.byteSwap(theSpecFile.readShort());
                        break;
                    case 4:
                        arrayOfChannelCount[i] = (int) MathPlus.byteSwap(theSpecFile.readInt());
                        break;
                    case 1:
                        arrayOfChannelCount[i] = (int) theSpecFile.readByte();
                        break;
                    default:
                        arrayOfChannelCount[i] = (int) MathPlus.byteSwap(theSpecFile.readShort());
                        break;
                }
                maxChannelCount = Math.max(maxChannelCount, arrayOfChannelCount[i]);
            }
            return arrayOfChannelCount;
        }

        /**
      Method to write a 1-dimensional Spectrum to a File, returning true or false to indicate success
      */
        public static boolean write(File aFile, Spectrum.OneDim aOneDSpectrum) throws SpecFormat_spc.IOException {
            RandomAccessFile theSpecFile;
            try {
                theSpecFile = new RandomAccessFile(aFile, "rw");
                Header header = new Header();
                header.write(theSpecFile, aOneDSpectrum);
                short numBitmaskRecord = (short) ((header.xLength * 1 - 1) / 8192 + 1);
                theSpecFile.seek((numBitmaskRecord + 1) * 1024);
                int bitMaskPtr = 0;
                int[] arrayOfSetBitOrig = new int[header.xLength];
                int[] aSpecCount = aOneDSpectrum.getSpecChannel().getSpecCount();
                for (int i = 0; i < header.xLength; i++) {
                    int iptr = (8 * (i / 8)) + (7 - (i % 8));
                    if (aSpecCount[iptr] != 0) {
                        arrayOfSetBitOrig[bitMaskPtr] = i;
                        bitMaskPtr++;
                        switch(header.loadFormatIndicator) {
                            case 2:
                                short num = (short) aSpecCount[iptr];
                                num = MathPlus.byteSwap(num);
                                theSpecFile.writeShort(num);
                                break;
                            case 4:
                                int num_ = (int) aSpecCount[iptr];
                                num_ = MathPlus.byteSwap(num_);
                                theSpecFile.writeInt(num_);
                                break;
                            case 1:
                                byte num__ = (byte) aSpecCount[iptr];
                                theSpecFile.writeByte(num__);
                                break;
                            default:
                                short num___ = (short) aSpecCount[iptr];
                                num___ = MathPlus.byteSwap(num___);
                                theSpecFile.writeShort(num___);
                                break;
                        }
                    }
                }
                theSpecFile.seek(SpecFormat_spc.NUMNONZERO_OFFSET);
                theSpecFile.writeInt(MathPlus.byteSwap(bitMaskPtr));
                theSpecFile.seek(SpecFormat_spc.TOTALCOUNT_OFFSET);
                theSpecFile.write(SpecFormat_spc.getBytesFromTotalCount(aOneDSpectrum.getMaxCount()));
                int[] arrayOfSetBit = new int[bitMaskPtr];
                System.arraycopy(arrayOfSetBitOrig, 0, arrayOfSetBit, 0, bitMaskPtr);
                byte[] arrayOfBitmaskRecord = new byte[numBitmaskRecord * 1024];
                theSpecFile.seek(SpecFormat_spc.BITMASKRECORD_OFFSET);
                for (int i = 0; i < arrayOfSetBit.length; i++) {
                    arrayOfBitmaskRecord[arrayOfSetBit[i] / 8] |= (0x80 >>> (arrayOfSetBit[i] % 8));
                }
                theSpecFile.write(arrayOfBitmaskRecord);
                theSpecFile.close();
                return true;
            } catch (java.io.IOException e) {
                throw new SpecFormat_spc.IOException("Could not write the spectrum to the named file\n\n", e);
            }
        }
    }

    /**
   * Handles reading and writing 2-d spectrum files in spc format
   */
    public static class TwoDimFilter {

        public TwoDimFilter() {
            super();
        }

        /**
      Method to read a 2-dimensional Spectrum File and extract necessary information to
      create a Spectrum.TwoDim and return

      Originally written in Fortran (using rectangular arrays), the counts for the
      Spectrum.TwoDim in SPCLIB format are stored as {y,x} values ...
      i.e looping through every y value, all the x value subscripts in the rectangular array are stored ...
      */
        public static Spectrum.TwoDim read(File aFile) throws SpecFormat_spc.IOException {
            RandomAccessFile theSpecFile;
            try {
                theSpecFile = new RandomAccessFile(aFile, "r");
                Header header = new Header();
                header.read(theSpecFile);
                int[][] arrayOfChannelCount;
                switch(header.loadFormatIndicator) {
                    case 0:
                        arrayOfChannelCount = getChannelsMUSORT(theSpecFile, header);
                        break;
                    default:
                        arrayOfChannelCount = getChannelsSMAUG(theSpecFile, header);
                        break;
                }
                theSpecFile.close();
                SimpleDateFormat aDateFormat = new SimpleDateFormat("dd-MMM-yy hh:mm:ss");
                String dateTimeString = header.creationDate + " " + header.creationTime;
                ParsePosition pos = new ParsePosition(0);
                Date specDate = aDateFormat.parse(dateTimeString, pos);
                return (new Spectrum.TwoDim(header.specName, specDate, new SpecChannel.TwoDim(header.yLength, header.xLength, arrayOfChannelCount)));
            } catch (java.io.IOException e) {
                throw new SpecFormat_spc.IOException("Could not read the spectrum from the named file\n\n", e);
            } catch (SpectrumException se) {
                throw new SpecFormat_spc.IOException("Could not get the spectrum from the named file\n\n" + se.getMessage());
            }
        }

        private static int[][] getChannelsSMAUG(RandomAccessFile theSpecFile, Header aHeader) throws java.io.IOException {
            theSpecFile.seek(SpecFormat_spc.BITMASKRECORD_OFFSET);
            byte[] arrayOfBitmaskRecord = new byte[aHeader.numBitmaskRecord * 1024];
            theSpecFile.readFully(arrayOfBitmaskRecord);
            int[] arrayOfSetBit = SpecFormat.getArrayOfSetBit(arrayOfBitmaskRecord, aHeader.numNonZeroElements);
            theSpecFile.seek((aHeader.numBitmaskRecord + 1) * 1024);
            int[][] arrayOfChannelCount = new int[aHeader.yLength][aHeader.xLength];
            int maxChannelCount = Integer.MIN_VALUE;
            int yptr;
            int xptr;
            for (int i = 0; i < aHeader.numNonZeroElements; i++) {
                yptr = (int) (arrayOfSetBit[i] / aHeader.xLength);
                xptr = (int) (arrayOfSetBit[i] % aHeader.xLength);
                switch(aHeader.loadFormatIndicator) {
                    case 2:
                        arrayOfChannelCount[yptr][xptr] = (int) MathPlus.byteSwap(theSpecFile.readShort());
                        break;
                    case 4:
                        arrayOfChannelCount[yptr][xptr] = (int) MathPlus.byteSwap(theSpecFile.readInt());
                        break;
                    case 1:
                        arrayOfChannelCount[yptr][xptr] = (int) theSpecFile.readByte();
                        break;
                    default:
                        arrayOfChannelCount[yptr][xptr] = (int) MathPlus.byteSwap(theSpecFile.readShort());
                        break;
                }
                maxChannelCount = Math.max(maxChannelCount, arrayOfChannelCount[yptr][xptr]);
            }
            return arrayOfChannelCount;
        }

        private static int[][] getChannelsMUSORT(RandomAccessFile theSpecFile, Header aHeader) throws java.io.IOException {
            theSpecFile.seek(SpecFormat_spc.BITMASKRECORD_OFFSET);
            int[][] arrayOfChannelCount = new int[aHeader.xLength][aHeader.yLength];
            int maxChannelCount = Integer.MIN_VALUE;
            for (int i = 0; i < aHeader.yLength; i++) {
                for (int j = 0; j < aHeader.xLength; j++) {
                    switch(aHeader.origLoadFormatIndicator) {
                        case 2:
                            arrayOfChannelCount[i][j] = (int) MathPlus.byteSwap(theSpecFile.readShort());
                            break;
                        case 4:
                            arrayOfChannelCount[i][j] = (int) MathPlus.byteSwap(theSpecFile.readInt());
                            break;
                        case 1:
                            arrayOfChannelCount[i][j] = (int) theSpecFile.readByte();
                            break;
                        default:
                            arrayOfChannelCount[i][j] = (int) MathPlus.byteSwap(theSpecFile.readShort());
                            break;
                    }
                    maxChannelCount = Math.max(maxChannelCount, arrayOfChannelCount[i][j]);
                }
            }
            return arrayOfChannelCount;
        }

        /**
     * Method to write a 2-dimensional Spectrum to a File,
     * returning true or false to indicate success
     */
        public static boolean write(File aFile, Spectrum.TwoDim aTwoDSpectrum) throws SpecFormat_spc.IOException {
            RandomAccessFile theSpecFile;
            try {
                theSpecFile = new RandomAccessFile(aFile, "rw");
                Header header = new Header();
                header.write(theSpecFile, aTwoDSpectrum);
                theSpecFile.seek((header.numBitmaskRecord + 1) * 1024);
                short bitMaskPtr = 0;
                int[] arrayOfSetBitOrig = new int[header.xLength * header.yLength];
                int[][] aSpecCount = aTwoDSpectrum.getSpecChannel().getSpecCount();
                for (int i = 0; i < header.yLength; i++) {
                    int yOffset = i * header.xLength;
                    int iptr = (8 * (i / 8)) + (7 - (i % 8));
                    for (int j = 0; j < header.xLength; j++) {
                        int jptr = (8 * (j / 8)) + (7 - (j % 8));
                        if (aSpecCount[i][jptr] != 0) {
                            arrayOfSetBitOrig[bitMaskPtr] = (yOffset + j);
                            bitMaskPtr++;
                            switch(header.loadFormatIndicator) {
                                case 2:
                                    short num = (short) aSpecCount[i][jptr];
                                    num = MathPlus.byteSwap(num);
                                    theSpecFile.writeShort(num);
                                    break;
                                case 4:
                                    int num_ = aSpecCount[i][jptr];
                                    num_ = MathPlus.byteSwap(num_);
                                    theSpecFile.writeInt(num_);
                                    break;
                                case 1:
                                    byte num__ = (byte) aSpecCount[i][jptr];
                                    theSpecFile.writeByte(num__);
                                    break;
                                default:
                                    short num___ = (short) aSpecCount[i][jptr];
                                    num___ = MathPlus.byteSwap(num___);
                                    theSpecFile.writeShort(num___);
                                    break;
                            }
                        }
                    }
                }
                theSpecFile.seek(SpecFormat_spc.NUMNONZERO_OFFSET);
                theSpecFile.writeInt(MathPlus.byteSwap((int) bitMaskPtr));
                theSpecFile.seek(SpecFormat_spc.TOTALCOUNT_OFFSET);
                theSpecFile.write(SpecFormat_spc.getBytesFromTotalCount(aTwoDSpectrum.getMaxCount()));
                int[] arrayOfSetBit = new int[bitMaskPtr];
                System.arraycopy(arrayOfSetBitOrig, 0, arrayOfSetBit, 0, bitMaskPtr);
                byte[] arrayOfBitmaskRecord = new byte[header.numBitmaskRecord * 1024];
                theSpecFile.seek(SpecFormat_spc.BITMASKRECORD_OFFSET);
                for (int i = 0; i < arrayOfSetBit.length; i++) {
                    arrayOfBitmaskRecord[arrayOfSetBit[i] / 8] |= (0x80 >>> (arrayOfSetBit[i] % 8));
                }
                theSpecFile.write(arrayOfBitmaskRecord);
                theSpecFile.close();
                return (true);
            } catch (java.io.IOException e) {
                throw new SpecFormat_spc.IOException("Could not write the spectrum to the named file\n\n", e);
            }
        }
    }

    public static class IOException extends SpecFormat.IOException {

        public IOException(String msg) {
            super(msg);
        }

        public IOException(String msg, Throwable thr) {
            super(msg, thr);
        }
    }
}
