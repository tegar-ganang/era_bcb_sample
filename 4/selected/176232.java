package org.tigr.cloe.model.facade.assemblerFacade.assemblerDataConverter;

import org.jcvi.assembly.tasm.DefaultTigrAssemblerFileContigDataStore;
import org.jcvi.assembly.tasm.TigrAssemblerContig;
import org.jcvi.assembly.tasm.TigrAssemblerPlacedRead;
import org.jcvi.io.IOUtil;
import org.jcvi.glyph.nuc.NucleotideEncodedGlyphs;
import org.jtc.common.util.dataencoding.Hex;
import java.io.File;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * User: aresnick
 * Date: Mar 22, 2010
 * Time: 12:48:36 PM
 * <p/>
 * $HeadURL$
 * $LastChangedRevision$
 * $LastChangedBy$
 * $LastChangedDate$
 * <p/>
 * Description:
 */
public class JavaAssemblyResponseXMLHandler implements Closeable {

    private OutputStream out;

    private Map<String, String> sequenceNameToCloeSeqIdMap;

    public JavaAssemblyResponseXMLHandler(OutputStream out, Map<String, String> sequenceNameToCloeSeqIdMap) {
        this.out = out;
        this.sequenceNameToCloeSeqIdMap = sequenceNameToCloeSeqIdMap;
    }

    @Override
    public void close() throws IOException {
        IOUtil.closeAndIgnoreErrors(out);
    }

    public void convertAssemblyResultToXML(File assemblyResult) throws Exception {
        DefaultTigrAssemblerFileContigDataStore dataStore = new DefaultTigrAssemblerFileContigDataStore(assemblyResult);
        writeToOutputStream("<ContigSet>\n");
        for (TigrAssemblerContig contig : dataStore) {
            writeContig(contig);
        }
        writeToOutputStream("</ContigSet>\n");
        out.flush();
    }

    private void writeContig(TigrAssemblerContig contig) throws Exception {
        writeToOutputStream("<Contig Id=\"RESASM_" + contig.getId() + "\">\n");
        Map<String, String> contigAttributes = contig.getAttributes();
        String consensus = contigAttributes.get("lsequence");
        writeToOutputStream("<Nuc>" + consensus + "</Nuc>\n");
        String qualities = getQualityElementString(contigAttributes.get("quality"));
        writeToOutputStream("<Qualclass>" + qualities + "</Qualclass>\n");
        for (TigrAssemblerPlacedRead read : contig.getPlacedReads()) {
            writeRead(read);
        }
        writeToOutputStream("</Contig>\n");
    }

    private void writeRead(TigrAssemblerPlacedRead read) throws Exception {
        writeToOutputStream("<Seq Id=\"" + getOutputFileReadId(read) + "\">\n");
        Map<String, String> readAttributes = read.getAttributes();
        String gapIndexes = getGapIndexString(read.getEncodedGlyphs());
        writeToOutputStream("<Gaps>" + gapIndexes + "</Gaps>\n");
        String leftSeqrange = readAttributes.get("seq_lend");
        String rightSeqrange = readAttributes.get("seq_rend");
        writeToOutputStream("<Seqrange Left=\"" + leftSeqrange + "\" Right=\"" + rightSeqrange + "\"/>\n");
        String leftAsmrange = readAttributes.get("asm_lend");
        String rightAsmrange = readAttributes.get("asm_rend");
        writeToOutputStream("<Asmrange Left=\"" + leftAsmrange + "\" Right=\"" + rightAsmrange + "\"/>\n");
        String offset = readAttributes.get("offset");
        writeToOutputStream("<Offset>" + offset + "</Offset>\n");
        writeToOutputStream("</Seq>\n");
    }

    private String getQualityElementString(String encodedQualities) throws Exception {
        StringBuilder buf = new StringBuilder();
        for (byte quality : Hex.hexStringToByteArr(encodedQualities)) {
            buf.append(quality);
            buf.append(" ");
        }
        return buf.substring(0, buf.length() - 1);
    }

    private String getGapIndexString(NucleotideEncodedGlyphs glyphs) {
        List<Integer> gapList = glyphs.getGapIndexes();
        if (gapList.isEmpty()) {
            return "";
        }
        SortedSet<Integer> gappedZeroBaseGapLocations = new TreeSet<Integer>(glyphs.getGapIndexes());
        List<Integer> ungappedZeroBaseGapLocations = new ArrayList<Integer>();
        int gapIndexOrdinalValue = 0;
        for (int gappedZeroBaseGapLocation : gappedZeroBaseGapLocations) {
            ungappedZeroBaseGapLocations.add(gappedZeroBaseGapLocation - gapIndexOrdinalValue + 1);
            gapIndexOrdinalValue++;
        }
        StringBuilder buf = new StringBuilder();
        for (int ungappedZeroBaseGapLocation : ungappedZeroBaseGapLocations) {
            buf.append(ungappedZeroBaseGapLocation);
            buf.append(" ");
        }
        return buf.substring(0, buf.length() - 1);
    }

    private String getOutputFileReadId(TigrAssemblerPlacedRead read) throws Exception {
        String sequenceName = read.getId();
        String cloeReadId = sequenceNameToCloeSeqIdMap.get(sequenceName);
        if (cloeReadId == null) {
            throw new Exception("Can't convert sequence name " + sequenceName + " to cloe sequence id; can't find sequence name in sequence name to cloe id map");
        } else {
            return cloeReadId;
        }
    }

    private void writeToOutputStream(String data) throws IOException {
        out.write(data.getBytes());
    }
}
