package hci.gnomex.useq;

import hci.gnomex.useq.data.*;
import java.io.*;
import java.util.zip.*;
import java.util.*;

/**Class for parsing USeq binary files for DAS2 requests and writing the data to stream. A USeqArchive is created upon request for a USeq data file
 * this should be cached to speed up subsequent retrieval.
 * 
 * @author david.nix@hci.utah.edu*/
public class USeqArchive {

    private File zipFile;

    private ZipFile zipArchive;

    private ArchiveInfo archiveInfo;

    private ZipEntry archiveReadMeEntry;

    private String binaryDataType;

    private HashMap<String, DataRange[]> chromStrandRegions = new HashMap<String, DataRange[]>();

    private boolean maintainStrandedness = false;

    private boolean stranded = false;

    public USeqArchive(File zipFile) throws Exception {
        this.zipFile = zipFile;
        parseZipFile();
    }

    /**Fetches and builds a merged USeqData[] object for the data that intersects the region.  Returns null if no data found.
	 * @return USeqData[0] = "+", USeqData[1] = "-"; USeqData[2] = ".", one or more may be null.*/
    public USeqData[] fetch(String chromosome, int beginningBP, int endingBP) {
        ArrayList<ZipEntry> entries = fetchZipEntries(chromosome, beginningBP, endingBP);
        if (entries == null) return null;
        ArrayList<USeqData> useqDataALPlus = new ArrayList<USeqData>();
        ArrayList<USeqData> useqDataALMinus = new ArrayList<USeqData>();
        ArrayList<USeqData> useqDataALNone = new ArrayList<USeqData>();
        BufferedInputStream bis = null;
        try {
            int numEntries = entries.size();
            for (int i = 0; i < numEntries; i++) {
                ZipEntry entry = entries.get(i);
                bis = new BufferedInputStream(zipArchive.getInputStream(entry));
                SliceInfo sliceInfo = new SliceInfo(entry.getName());
                USeqData d = loadSlice(beginningBP, endingBP, sliceInfo, bis);
                if (d != null) {
                    if (sliceInfo.getStrand().equals("+")) useqDataALPlus.add(d); else if (sliceInfo.getStrand().equals("+")) useqDataALMinus.add(d); else useqDataALNone.add(d);
                }
                bis.close();
            }
            USeqData plus = null;
            USeqData minus = null;
            USeqData non = null;
            if (useqDataALPlus.size() != 0) plus = mergeUSeqData(useqDataALPlus);
            if (useqDataALMinus.size() != 0) minus = mergeUSeqData(useqDataALMinus);
            if (useqDataALNone.size() != 0) minus = mergeUSeqData(useqDataALNone);
            if (plus != null || minus != null || non != null) return new USeqData[] { plus, minus, non }; else return null;
        } catch (IOException e) {
            e.printStackTrace();
            USeqUtilities.safeClose(bis);
            return null;
        }
    }

    /**Merges an ArrayList of the same dataType.*/
    public USeqData mergeUSeqData(ArrayList<USeqData> useqDataAL) {
        if (USeqUtilities.POSITION.matcher(binaryDataType).matches()) return PositionData.mergeUSeqData(useqDataAL);
        if (USeqUtilities.POSITION_SCORE.matcher(binaryDataType).matches()) return PositionScoreData.mergeUSeqData(useqDataAL);
        if (USeqUtilities.POSITION_TEXT.matcher(binaryDataType).matches()) return PositionTextData.mergeUSeqData(useqDataAL);
        if (USeqUtilities.POSITION_SCORE_TEXT.matcher(binaryDataType).matches()) return PositionScoreTextData.mergeUSeqData(useqDataAL);
        if (USeqUtilities.REGION.matcher(binaryDataType).matches()) return RegionData.mergeUSeqData(useqDataAL);
        if (USeqUtilities.REGION_SCORE.matcher(binaryDataType).matches()) return RegionScoreData.mergeUSeqData(useqDataAL);
        if (USeqUtilities.REGION_TEXT.matcher(binaryDataType).matches()) return RegionTextData.mergeUSeqData(useqDataAL);
        if (USeqUtilities.REGION_SCORE_TEXT.matcher(binaryDataType).matches()) return RegionScoreTextData.mergeUSeqData(useqDataAL);
        return null;
    }

    /**Fetches from the zip archive the files that intersect the unstranded range request and writes them to the stream.
	 * @return	false if no files found*/
    public boolean writeSlicesToStream(OutputStream outputStream, String chromosome, int beginningBP, int endingBP, boolean closeStream) {
        ArrayList<ZipEntry> entries = fetchZipEntries(chromosome, beginningBP, endingBP);
        if (entries == null) return false;
        entries.add(0, archiveReadMeEntry);
        ZipOutputStream out = new ZipOutputStream(outputStream);
        DataOutputStream dos = new DataOutputStream(out);
        BufferedInputStream bis = null;
        try {
            int count;
            byte data[] = new byte[2048];
            int numEntries = entries.size();
            SliceInfo sliceInfo = null;
            for (int i = 0; i < numEntries; i++) {
                ZipEntry entry = entries.get(i);
                bis = new BufferedInputStream(zipArchive.getInputStream(entry));
                if (i != 0) sliceInfo = new SliceInfo(entry.getName());
                if (i == 0 || sliceInfo.isContainedBy(beginningBP, endingBP)) {
                    out.putNextEntry(entry);
                    while ((count = bis.read(data, 0, 2048)) != -1) out.write(data, 0, count);
                    out.closeEntry();
                } else sliceAndWriteEntry(beginningBP, endingBP, sliceInfo, bis, out, dos);
                bis.close();
            }
            if (closeStream) {
                out.close();
                outputStream.close();
                dos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            USeqUtilities.safeClose(out);
            USeqUtilities.safeClose(outputStream);
            USeqUtilities.safeClose(bis);
            USeqUtilities.safeClose(dos);
            return false;
        }
        return true;
    }

    private USeqData loadSlice(int beginningBP, int endingBP, SliceInfo sliceInfo, BufferedInputStream bis) {
        DataInputStream dis = new DataInputStream(bis);
        USeqData d = null;
        try {
            if (USeqUtilities.POSITION.matcher(binaryDataType).matches()) {
                d = new PositionData(dis, sliceInfo);
                if (sliceInfo.isContainedBy(beginningBP, endingBP) == false) {
                    if (((PositionData) d).trim(beginningBP, endingBP) == false) d = null;
                }
            } else if (USeqUtilities.POSITION_SCORE.matcher(binaryDataType).matches()) {
                d = new PositionScoreData(dis, sliceInfo);
                if (sliceInfo.isContainedBy(beginningBP, endingBP) == false) {
                    if (((PositionScoreData) d).trim(beginningBP, endingBP) == false) d = null;
                }
            } else if (USeqUtilities.POSITION_TEXT.matcher(binaryDataType).matches()) {
                d = new PositionTextData(dis, sliceInfo);
                if (sliceInfo.isContainedBy(beginningBP, endingBP) == false) {
                    if (((PositionTextData) d).trim(beginningBP, endingBP) == false) d = null;
                }
            } else if (USeqUtilities.POSITION_SCORE_TEXT.matcher(binaryDataType).matches()) {
                d = new PositionScoreTextData(dis, sliceInfo);
                if (sliceInfo.isContainedBy(beginningBP, endingBP) == false) {
                    if (((PositionScoreTextData) d).trim(beginningBP, endingBP) == false) d = null;
                }
            } else if (USeqUtilities.REGION.matcher(binaryDataType).matches()) {
                d = new RegionData(dis, sliceInfo);
                if (sliceInfo.isContainedBy(beginningBP, endingBP) == false) {
                    if (((RegionData) d).trim(beginningBP, endingBP) == false) d = null;
                }
            } else if (USeqUtilities.REGION_SCORE.matcher(binaryDataType).matches()) {
                d = new RegionScoreData(dis, sliceInfo);
                if (sliceInfo.isContainedBy(beginningBP, endingBP) == false) {
                    if (((RegionScoreData) d).trim(beginningBP, endingBP) == false) d = null;
                }
            } else if (USeqUtilities.REGION_TEXT.matcher(binaryDataType).matches()) {
                d = new RegionTextData(dis, sliceInfo);
                if (sliceInfo.isContainedBy(beginningBP, endingBP) == false) {
                    if (((RegionTextData) d).trim(beginningBP, endingBP) == false) d = null;
                }
            } else if (USeqUtilities.REGION_SCORE_TEXT.matcher(binaryDataType).matches()) {
                d = new RegionScoreTextData(dis, sliceInfo);
                if (sliceInfo.isContainedBy(beginningBP, endingBP) == false) {
                    if (((RegionScoreTextData) d).trim(beginningBP, endingBP) == false) d = null;
                }
            } else {
                throw new IOException("Unknown USeq data type, '" + binaryDataType + "', for slicing data from  -> '" + sliceInfo.getSliceName() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            USeqUtilities.safeClose(bis);
            return null;
        }
        return d;
    }

    private void sliceAndWriteEntry(int beginningBP, int endingBP, SliceInfo sliceInfo, BufferedInputStream bis, ZipOutputStream out, DataOutputStream dos) {
        String dataType = sliceInfo.getBinaryType();
        DataInputStream dis = new DataInputStream(bis);
        try {
            if (USeqUtilities.POSITION.matcher(dataType).matches()) {
                PositionData d = new PositionData(dis, sliceInfo);
                if (d.trim(beginningBP, endingBP)) d.write(out, dos, true);
            } else if (USeqUtilities.POSITION_SCORE.matcher(dataType).matches()) {
                PositionScoreData d = new PositionScoreData(dis, sliceInfo);
                if (d.trim(beginningBP, endingBP)) d.write(out, dos, true);
            } else if (USeqUtilities.POSITION_TEXT.matcher(dataType).matches()) {
                PositionTextData d = new PositionTextData(dis, sliceInfo);
                if (d.trim(beginningBP, endingBP)) d.write(out, dos, true);
            } else if (USeqUtilities.POSITION_SCORE_TEXT.matcher(dataType).matches()) {
                PositionScoreTextData d = new PositionScoreTextData(dis, sliceInfo);
                if (d.trim(beginningBP, endingBP)) d.write(out, dos, true);
            } else if (USeqUtilities.REGION.matcher(dataType).matches()) {
                RegionData d = new RegionData(dis, sliceInfo);
                if (d.trim(beginningBP, endingBP)) d.write(out, dos, true);
            } else if (USeqUtilities.REGION_SCORE.matcher(dataType).matches()) {
                RegionScoreData d = new RegionScoreData(dis, sliceInfo);
                if (d.trim(beginningBP, endingBP)) d.write(out, dos, true);
            } else if (USeqUtilities.REGION_TEXT.matcher(dataType).matches()) {
                RegionTextData d = new RegionTextData(dis, sliceInfo);
                if (d.trim(beginningBP, endingBP)) d.write(out, dos, true);
            } else if (USeqUtilities.REGION_SCORE_TEXT.matcher(dataType).matches()) {
                RegionScoreTextData d = new RegionScoreTextData(dis, sliceInfo);
                if (d.trim(beginningBP, endingBP)) d.write(out, dos, true);
            } else {
                throw new IOException("Unknown USeq data type, '" + dataType + "', for slicing data from  -> '" + sliceInfo.getSliceName() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            USeqUtilities.safeClose(out);
            USeqUtilities.safeClose(bis);
        } finally {
            USeqUtilities.safeClose(dis);
        }
    }

    /**Fetches from the zip archive the files that intersect the unstranded range request and saves to a new zip archive.
	 * @return	Sliced zip archive or null if no files found*/
    public File writeSlicesToFile(File saveDirectory, String chromosome, int beginningBP, int endingBP) {
        ArrayList<ZipEntry> entries = fetchZipEntries(chromosome, beginningBP, endingBP);
        if (entries == null) return null;
        entries.add(0, archiveReadMeEntry);
        File slicedZipArchive = new File(saveDirectory, "USeqDataSlice_" + createRandowWord(7) + "." + USeqUtilities.USEQ_EXTENSION_NO_PERIOD);
        ZipOutputStream out = null;
        BufferedInputStream is = null;
        try {
            out = new ZipOutputStream(new FileOutputStream(slicedZipArchive));
            int count;
            byte data[] = new byte[2048];
            int numEntries = entries.size();
            for (int i = 0; i < numEntries; i++) {
                ZipEntry entry = entries.get(i);
                out.putNextEntry(entry);
                is = new BufferedInputStream(zipArchive.getInputStream(entry));
                while ((count = is.read(data, 0, 2048)) != -1) out.write(data, 0, count);
                out.closeEntry();
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            USeqUtilities.safeClose(out);
            USeqUtilities.safeClose(is);
        }
        return slicedZipArchive;
    }

    /**Fetches the ZipEntries for a given range.  Returns null if none found or chromStrand not found. 
	 * Remember this list isn't stranded so must search entire set.*/
    public ArrayList<ZipEntry> fetchZipEntries(String chromStrand, int beginningBP, int endingBP) {
        ArrayList<ZipEntry> al = new ArrayList<ZipEntry>();
        DataRange[] dr = chromStrandRegions.get(chromStrand);
        if (dr == null) return null;
        for (int i = 0; i < dr.length; i++) {
            if (dr[i].intersects(beginningBP, endingBP)) {
                al.add(dr[i].zipEntry);
            }
        }
        if (al.size() == 0) return null;
        return al;
    }

    /**Loads the zip entries into the chromosomeStrand DataRange[] HashMap*/
    @SuppressWarnings("unchecked")
    private void parseZipFile() {
        InputStream is = null;
        try {
            if (USeqUtilities.USEQ_ARCHIVE.matcher(zipFile.getName()).matches() == false) throw new IOException("This file does not appear to be a USeq archive! " + zipFile);
            zipArchive = new ZipFile(zipFile);
            Enumeration e = zipArchive.entries();
            archiveReadMeEntry = (ZipEntry) e.nextElement();
            is = zipArchive.getInputStream(archiveReadMeEntry);
            archiveInfo = new ArchiveInfo(is, false);
            HashMap<String, ArrayList<DataRange>> map = new HashMap<String, ArrayList<DataRange>>();
            while (e.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry) e.nextElement();
                SliceInfo sliceInfo = new SliceInfo(zipEntry.getName());
                if (binaryDataType == null) binaryDataType = sliceInfo.getBinaryType();
                String chromName;
                if (maintainStrandedness) chromName = sliceInfo.getChromosome() + sliceInfo.getStrand(); else chromName = sliceInfo.getChromosome();
                if (sliceInfo.getStrand().equals(".") == false) stranded = true;
                ArrayList<DataRange> al = map.get(chromName);
                if (al == null) {
                    al = new ArrayList<DataRange>();
                    map.put(chromName, al);
                }
                al.add(new DataRange(zipEntry, sliceInfo.getFirstStartPosition(), sliceInfo.getLastStartPosition()));
            }
            Iterator<String> it = map.keySet().iterator();
            while (it.hasNext()) {
                String chromName = it.next();
                ArrayList<DataRange> al = map.get(chromName);
                DataRange[] dr = new DataRange[al.size()];
                al.toArray(dr);
                Arrays.sort(dr);
                chromStrandRegions.put(chromName, dr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            USeqUtilities.safeClose(is);
        }
    }

    private class DataRange implements Comparable<DataRange> {

        ZipEntry zipEntry;

        int beginningBP;

        int endingBP;

        public DataRange(ZipEntry zipEntry, int beginningBP, int endingBP) {
            this.zipEntry = zipEntry;
            this.beginningBP = beginningBP;
            this.endingBP = endingBP;
        }

        public boolean intersects(int start, int stop) {
            if (stop <= beginningBP || start >= endingBP) return false;
            return true;
        }

        /**Sorts by beginningBP, smaller to larger.*/
        public int compareTo(DataRange other) {
            if (beginningBP < other.beginningBP) return -1;
            if (beginningBP > other.beginningBP) return 1;
            return 0;
        }
    }

    public static String[] nonAmbiguousLetters = { "A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "N", "P", "Q", "R", "T", "U", "V", "W", "X", "Y", "3", "4", "6", "7", "8", "9" };

    /**Creates pseudorandom Strings derived from an alphabet of String[] using the
	 * java.util.Random class.  Indicate how long you want a particular word and
	 * the number of words.*/
    public static String[] createRandomWords(String[] alphabet, int lengthOfWord, int numberOfWords) {
        ArrayList<String> words = new ArrayList<String>();
        Random r = new Random();
        int len = alphabet.length;
        for (int i = 0; i < numberOfWords; i++) {
            StringBuffer w = new StringBuffer();
            for (int j = 0; j < lengthOfWord; j++) {
                w.append(alphabet[r.nextInt(len)]);
            }
            words.add(w.toString());
        }
        String[] w = new String[words.size()];
        words.toArray(w);
        return w;
    }

    /**Returns a random word using nonambiguous alphabet.  Don't use this method for creating more than one word!*/
    public static String createRandowWord(int lengthOfWord) {
        return createRandomWords(nonAmbiguousLetters, lengthOfWord, 1)[0];
    }

    public ArchiveInfo getArchiveInfo() {
        return archiveInfo;
    }

    public String getBinaryDataType() {
        return binaryDataType;
    }

    /**Returns a HashMap containing chromosomes and the last base covered.*/
    public HashMap<String, Integer> fetchChromosomesAndLastBase() throws IOException {
        HashMap<String, DataRange> map = new HashMap<String, DataRange>();
        for (String chrom : chromStrandRegions.keySet()) {
            DataRange[] dr = chromStrandRegions.get(chrom);
            int lastFirstBase = 0;
            DataRange lastDataRange = null;
            for (DataRange d : dr) {
                if (d.endingBP > lastFirstBase) {
                    lastFirstBase = d.endingBP;
                    lastDataRange = d;
                }
            }
            map.put(chrom, lastDataRange);
        }
        ZipFile zf = new ZipFile(zipFile);
        HashMap<String, Integer> chromBase = new HashMap<String, Integer>();
        for (String chrom : map.keySet()) {
            DataRange dr = map.get(chrom);
            ZipEntry ze = dr.zipEntry;
            SliceInfo si = new SliceInfo(ze.getName());
            DataInputStream dis = new DataInputStream(new BufferedInputStream(zf.getInputStream(ze)));
            String extension = si.getBinaryType();
            int lastBase = -1;
            if (USeqUtilities.POSITION.matcher(extension).matches()) lastBase = new PositionData(dis, si).fetchLastBase(); else if (USeqUtilities.POSITION_SCORE.matcher(extension).matches()) lastBase = new PositionScoreData(dis, si).fetchLastBase(); else if (USeqUtilities.POSITION_TEXT.matcher(extension).matches()) lastBase = new PositionTextData(dis, si).fetchLastBase(); else if (USeqUtilities.POSITION_SCORE_TEXT.matcher(extension).matches()) lastBase = new PositionScoreTextData(dis, si).fetchLastBase(); else if (USeqUtilities.REGION.matcher(extension).matches()) lastBase = new RegionData(dis, si).fetchLastBase(); else if (USeqUtilities.REGION_SCORE.matcher(extension).matches()) lastBase = new RegionScoreData(dis, si).fetchLastBase(); else if (USeqUtilities.REGION_TEXT.matcher(extension).matches()) lastBase = new RegionTextData(dis, si).fetchLastBase(); else if (USeqUtilities.REGION_SCORE_TEXT.matcher(extension).matches()) lastBase = new RegionScoreTextData(dis, si).fetchLastBase(); else throw new IOException("\nFailed to recognize the binary file extension! " + ze.getName());
            chromBase.put(chrom, new Integer(lastBase));
        }
        return chromBase;
    }

    public File getZipFile() {
        return zipFile;
    }

    public boolean isStranded() {
        return stranded;
    }
}
