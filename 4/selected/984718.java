package apollo.dataadapter.gamexml;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.String;
import java.lang.StringBuffer;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Vector;
import java.util.Iterator;
import javax.swing.JOptionPane;
import apollo.datamodel.*;
import apollo.util.DateUtil;
import apollo.dataadapter.chadoxml.ChadoXmlWrite;
import apollo.config.Config;
import apollo.config.FeatureProperty;
import apollo.config.PropertyScheme;
import org.apache.log4j.*;
import org.bdgp.xml.XML_util;

public class GAMESave {

    protected static final Logger logger = LogManager.getLogger(GAMESave.class);

    /** The latest version should be the default (1.1 for 11/05) but this should
      be a configuration - if DO-ONE-LEVEL-ANNOTS is false this should probably
      be set to 1.0? move to game adapter? */
    public static float gameVersion = 1.1f;

    private static HashSet loadedSeqs = new HashSet();

    /** Save to file name (booleans saveAnnots and saveResults default to true; 
      String "changes" defaults to null) */
    public static void writeXML(CurationSet curation, String fileName, String version, boolean game2_format) {
        writeXML(curation, fileName, true, true, version, game2_format);
    }

    /**
   * Writes XML
   * If the input type is not FILE, prompts user for a file to save to.
   */
    public static boolean writeXML(CurationSet curation, String file_str, boolean saveAnnots, boolean saveResults, String version, boolean game2_format) {
        FileWriter fileWriter;
        String filename = apollo.util.IOUtil.findFile(file_str, true);
        if (filename == null) return false;
        try {
            fileWriter = new FileWriter(filename);
        } catch (IOException ex) {
            String message = "Failed to open file for saving: " + filename;
            logger.error(message);
            JOptionPane.showMessageDialog(null, message, "Warning", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if (fileWriter == null) return false;
        String what = "annotations and evidence";
        if (!saveAnnots) what = "evidence (results) only";
        if (!saveResults) what = "annotations only (no results)";
        String msg = "Saving " + what + " to file " + filename;
        logger.info(msg);
        if (writeXML(curation, fileWriter, saveAnnots, saveResults, version, game2_format)) logger.info("Saved GAME XML (v" + gameVersion + ") to " + filename); else {
            String message = "Failed to save GAME XML to " + filename;
            logger.error(message);
        }
        try {
            fileWriter.close();
        } catch (Exception ex) {
            logger.error("GAMESave.writeXML: caught exception closing " + filename, ex);
            return false;
        }
        return true;
    }

    static boolean writeXML(CurationSet curation, FileWriter fileWriter, boolean saveAnnots, boolean saveResults, String version, boolean game2_format) {
        long time1 = System.currentTimeMillis();
        debugPrint("Buffered save");
        BufferedWriter bw = new BufferedWriter(fileWriter);
        PrintWriter pw = new PrintWriter(bw);
        pw.print(writeGameBegin(curation.getName(), curation.getInputFilename(), version));
        debugPrint("writing genome pos");
        pw.print(writeGenomePosition(curation));
        if (saveAnnots) {
            debugPrint("writing annots");
            pw.print(writeAnnotations(curation));
        }
        if (saveResults) {
            debugPrint("writing seqs");
            writeSequencesToWriter(curation, pw);
            debugPrint("writing anlyses");
            writeAnalysesToWriter(curation, game2_format, pw);
        }
        debugPrint("writing transactions");
        if (Config.outputTransactionXML() && Config.getStyle().transactionsAreInGameFile() && curation.hasTransactions()) {
            TransactionXMLAdapter tnAdapter = new TransactionXMLAdapter();
            tnAdapter.setWriter(pw);
            boolean separateFile = false;
            try {
                tnAdapter.save(curation.getTransactionManager().getTransactions(), separateFile);
            } catch (IOException e) {
                logger.error("Failed to save transactions to game file", e);
            }
        }
        pw.print(writeGameEnd());
        pw.flush();
        pw.close();
        long time2 = System.currentTimeMillis();
        logger.debug("Time for saving: " + (time2 - time1) / 1000 + " secs");
        if (pw.checkError()) {
            logger.error("Error committing XML", new Throwable());
            return false;
        }
        return true;
    }

    private static long lastTime = System.currentTimeMillis();

    private static void debugPrint(String m) {
        if (!logger.isDebugEnabled()) return;
        long time = System.currentTimeMillis();
        long secs = (time - lastTime) / 1000;
        logger.debug("Time elapsed for previous write: " + secs + " seconds");
        logger.debug(m);
        lastTime = time;
    }

    private static String writeGameBegin(String seq_name, String originalInput, String version) {
        StringBuffer buf = new StringBuffer();
        buf.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
        buf.append("\n");
        if (!Config.DO_ONE_LEVEL_ANNOTS) gameVersion = 1.0f;
        buf.append("<game version=\"" + gameVersion + "\">\n");
        buf.append("  <!-- Curational annotations from Apollo -->\n");
        buf.append("  <!-- Analysis of: " + makeSafeForXML(seq_name) + " -->\n");
        if (originalInput != null && !originalInput.equals("")) buf.append("  <!-- Original input read from " + originalInput + " -->\n");
        buf.append("  <!-- Saved on " + DateUtil.toGMT(new Date()) + " -->\n");
        buf.append("  <!-- " + version + " -->\n");
        return buf.toString();
    }

    private static String makeSafeForXML(String str) {
        return str.replaceAll("--", "-");
    }

    private static String writeAnnotations(CurationSet curation) {
        StringBuffer buf = new StringBuffer();
        StrandedFeatureSetI annots = curation.getAnnots();
        if (annots != null) {
            for (int i = 0; i < annots.size(); i++) {
                AnnotatedFeatureI gene = (AnnotatedFeatureI) annots.getFeatureAt(i);
                buf.append(writeAnnotation(curation, gene));
            }
        }
        return buf.toString();
    }

    private static String writeAnnotation(CurationSet curation, AnnotatedFeatureI annot) {
        String indent = "  ";
        StringBuffer buf = new StringBuffer();
        buf.append(indent + "<annotation");
        buf.append(writeID(getID(annot), "annotation"));
        if (annot.isProblematic()) buf.append(" problem=\"" + annot.isProblematic() + "\"");
        buf.append(">\n");
        buf.append(writeName(annot.getName(), "annotation", indent));
        buf.append(indent + "  <type>" + annot.getTopLevelType() + "</type>\n");
        if (annot.getOwner() != null) {
            buf.append(indent + "  <author>" + annot.getOwner() + "</author>\n");
        }
        boolean isa_gene = annot.isProteinCodingGene();
        buf.append(writeDescription("    ", annot.getDescription()));
        buf.append(writeProperties(indent + "  ", annot, "property"));
        buf.append(writeSynonyms(indent + "  ", annot.getSynonyms()));
        Vector xrefs = annot.getDbXrefs();
        if (xrefs.size() > 0) {
            buf.append(indent + "  <gene");
            buf.append(" id=\"" + XML_util.transformToPCData(annot.getName()) + "\"");
            buf.append(" association=\"IS\">\n");
            buf.append(indent + "    <name>" + XML_util.transformToPCData(annot.getName()) + "</name>\n");
            buf.append(indent + "  </gene>\n");
        }
        for (int i = 0; i < xrefs.size(); i++) {
            DbXref xref = (DbXref) xrefs.elementAt(i);
            buf.append(writeXref(indent + "  ", xref.getDbName(), xref.getIdValue(), xref.getIdType()));
        }
        Vector comments = annot.getComments();
        for (int i = 0; i < comments.size(); i++) {
            Comment comment = (Comment) comments.elementAt(i);
            buf.append(writeComment("    ", comment));
        }
        Vector transcripts = annot.getFeatures();
        for (int i = 0; i < transcripts.size(); i++) {
            Transcript fs = (Transcript) transcripts.elementAt(i);
            buf.append(writeFeatureSet(indent + "  ", fs, isa_gene, curation));
        }
        if (isOneLevelAnnot(annot)) {
            buf.append(writeSeqRelationship(indent + "  ", annot, "query", "", curation));
        }
        buf.append(indent + "</annotation>\n");
        return buf.toString();
    }

    static boolean isOneLevelAnnot(SeqFeatureI annot) {
        if (!Config.DO_ONE_LEVEL_ANNOTS) return false;
        if (annot.getFeatures().size() > 0) return false;
        PropertyScheme ps = Config.getPropertyScheme();
        FeatureProperty fp = ps.getFeatureProperty(annot.getFeatureType());
        return fp.getNumberOfLevels() == 1;
    }

    private static String getID(SeqFeatureI sf) {
        String annot_id = sf.getId();
        if (annot_id == null) annot_id = sf.getName();
        return annot_id;
    }

    /** At the moment this is solely for annotation transcripts - result feature sets are
   * handled elsewhere - transcripts go in as <feature_set>. called by writeAnnotation
   */
    private static String writeFeatureSet(String indent, Transcript trans, boolean isa_gene, CurationSet curation) {
        StringBuffer buf = new StringBuffer();
        buf.append(indent + "<feature_set");
        if (trans.getId() != null && trans.getId().length() > 0) buf.append(writeID(trans.getId(), "feature_set"));
        AbstractSequence cDNA;
        cDNA = (AbstractSequence) trans.get_cDNASequence();
        if (cDNA != null && cDNA.getResidues() != null && cDNA.getResidues().length() > 0 && cDNA.getAccessionNo() != null) {
            buf.append(" produces_seq=\"" + cDNA.getAccessionNo() + "\"");
        }
        if (trans.isProblematic()) buf.append(" problem=\"" + trans.isProblematic() + "\"");
        buf.append(">\n");
        buf.append(writeName(trans.getName(), "", indent));
        buf.append(indent + "  <type>" + trans.getFeatureType() + "</type>\n");
        buf.append(writeDescription(indent + "  ", trans.getDescription()));
        if (trans.getOwner() != null) {
            buf.append(indent + "  <author>" + trans.getOwner() + "</author>\n");
        }
        buf.append(writeDate(indent + "  ", trans));
        buf.append(writeSynonyms(indent + "  ", trans.getSynonyms()));
        Vector xrefs = trans.getDbXrefs();
        for (int i = 0; i < xrefs.size(); i++) {
            DbXref xref = (DbXref) xrefs.elementAt(i);
            buf.append(writeXref(indent + indent, xref.getDbName(), xref.getIdValue(), xref.getIdType()));
        }
        Vector comments = trans.getComments();
        for (int i = 0; i < comments.size(); i++) {
            Comment comment = (Comment) comments.elementAt(i);
            buf.append(writeComment("      ", comment));
        }
        buf.append(writeProperties(indent + "  ", trans, "property"));
        if (isa_gene) {
            if (trans.hasReadThroughStop()) {
                if (trans.readThroughStopResidue().equals("X")) writeProperty(indent + " ", "readthrough_stop_codon", "true", buf); else writeProperty(indent + " ", "readthrough_stop_codon", trans.readThroughStopResidue(), buf);
            }
            if (trans.plus1FrameShiftPosition() > 0) {
                writeProperty(indent + "  ", "plus_1_translational_frame_shift", trans.plus1FrameShiftPosition() + "", buf);
            }
            if (trans.minus1FrameShiftPosition() > 0) {
                writeProperty(indent + "  ", "minus_1_translational_frame_shift", trans.minus1FrameShiftPosition() + "", buf);
            }
            if (gameVersion >= 1.1) {
                buf.append(writeSpan(indent + "  ", trans.getProteinFeat(), true, curation, true));
            } else {
                buf.append(writeTSS(indent + "  ", trans, "feature_span", curation));
            }
            if (trans.unConventionalStart()) {
                writeProperty(indent + "  ", "non_canonical_start_codon", trans.getStartCodon(), buf);
            }
            if (trans.isMissing5prime()) {
                writeProperty(indent + "  ", "missing_start_codon", "true", buf);
            }
            if (trans.isMissing3prime()) {
                writeProperty(indent + "  ", "missing_stop_codon", "true", buf);
            }
            if ((trans.getNonConsensusAcceptorNum() >= 0 || trans.getNonConsensusDonorNum() >= 0)) {
                writeProperty(indent + "  ", "non_canonical_splice_site", (trans.nonConsensusSplicingOkay() ? "approved" : "unapproved"), buf);
            }
        }
        Vector exons = trans.getFeatures();
        for (int i = 0; i < exons.size(); i++) {
            Exon exon = (Exon) exons.elementAt(i);
            buf.append(writeSpan(indent + "  ", exon, isa_gene, curation));
        }
        if ((cDNA != null) && cDNA.getAccessionNo() != null) {
            loadedSeqs.add(cDNA.getAccessionNo());
            buf.append(writeSequence(cDNA, indent + "  ", "", "cdna", true));
        }
        if (trans.isProteinCodingGene()) {
            AbstractSequence peptide = (AbstractSequence) trans.getPeptideSequence();
            if (peptide != null) {
                if (peptide.getAccessionNo() != null) {
                    loadedSeqs.add(peptide.getAccessionNo());
                }
            }
        }
        buf.append(indent + "</feature_set>\n");
        return buf.toString();
    }

    private static String writeGenomePosition(CurationSet curation) {
        StringBuffer buf = new StringBuffer();
        SequenceI genomic_seq = curation.getRefSequence();
        buf.append(writeSequence((AbstractSequence) genomic_seq, "  ", " focus=\"true\"", "", true));
        String type = (curation.getFeatureType().equals(RangeI.NO_TYPE) ? "" : "type=\"" + curation.getFeatureType() + "\" ");
        String arm = (curation.getChromosome() == null ? "" : "    <arm>" + curation.getChromosome() + "</arm>\n");
        String chromosome = (curation.getChromosome() == null ? "" : "    <chromosome>" + curation.getChromosome() + "</chromosome>\n");
        String organism = (curation.getOrganism() == null ? "" : "    <organism>" + curation.getOrganism() + "</organism>\n");
        buf.append("  <map_position " + type + "seq=\"" + curation.getName() + "\">\n");
        buf.append(chromosome);
        buf.append(organism);
        buf.append(arm);
        buf.append("    <span>\n");
        buf.append("      <start>" + curation.getStart() + "</start>\n");
        buf.append("      <end>" + curation.getEnd() + "</end>\n");
        buf.append("    </span>\n");
        buf.append("  </map_position>\n");
        return buf.toString();
    }

    /** Write sequences directly to print writer. Previously returned a string.
      Turns out its far far more efficient to write straight to the writer
      than return a string */
    private static void writeSequencesToWriter(CurationSet curation, PrintWriter pw) {
        String name = curation.getName();
        Vector seqs = curation.getSequences();
        for (int i = 0; i < seqs.size(); i++) {
            SequenceI seq = (SequenceI) seqs.elementAt(i);
            String id = seq.getAccessionNo();
            if (id == null || id.equals("")) id = seq.getName();
            if (!id.equals(name)) {
                if (!loadedSeqs.contains(seq.getAccessionNo())) {
                    pw.print(writeSequence(seq, "  ", "", "", true));
                }
            }
        }
    }

    /** ids are attributes not elements */
    private static String writeID(String id, String element_name) {
        if (id == null || id.length() == 0) return "";
        StringBuffer buf = new StringBuffer();
        String quoter = "\"";
        id = id.replaceAll("\"", "&quot;");
        if (id != null && !id.equals("") && !id.equals("?") && !id.startsWith(element_name)) buf.append(" id=" + quoter + id + quoter);
        return buf.toString();
    }

    private static String writeSequence(SequenceI seq, String indent, String focus, String type, boolean include_residues) {
        String quoter = "\"";
        StringBuffer buf = new StringBuffer();
        if (seq == null) return "";
        buf.append(indent + "<seq");
        String id = seq.getAccessionNo();
        if (id == null || id.equals("")) id = seq.getName();
        buf.append(writeID(id, "seq"));
        buf.append(" length=" + quoter + seq.getLength() + quoter);
        if (type != null && !type.equals("")) buf.append(" type=" + quoter + type + quoter);
        String checksum = seq.getChecksum();
        if (checksum != null && !checksum.equals("")) buf.append(" md5checksum=" + quoter + checksum + quoter);
        buf.append(focus);
        buf.append(">\n");
        buf.append(indent + "  <name>" + XML_util.transformToPCData(id, true) + "</name>\n");
        buf.append(writeDescription(indent + "  ", seq.getDescription()));
        if (seq.getOrganism() != null && !seq.getOrganism().equals("")) {
            buf.append(indent + "  <organism>" + seq.getOrganism() + "</organism>\n");
        }
        Vector xrefs = seq.getDbXrefs();
        for (int i = 0; i < xrefs.size(); i++) {
            DbXref xref = (DbXref) xrefs.elementAt(i);
            buf.append(writeXref(indent + "  ", xref.getDbName(), xref.getIdValue(), ""));
        }
        HashMap seq_errors = seq.getGenomicErrors();
        if (seq_errors != null) {
            Iterator positions = seq_errors.keySet().iterator();
            while (positions.hasNext()) {
                String position = (String) positions.next();
                SequenceEdit seq_edit = (SequenceEdit) seq_errors.get(position);
                buf.append(indent + "  <potential_sequencing_error>\n");
                buf.append(indent + "    <type>" + seq_edit.getEditType() + "</type>\n");
                buf.append(indent + "    <position>" + seq_edit.getPosition() + "</position>\n");
                if (seq_edit.getResidue() != null) buf.append(indent + "    <base>" + seq_edit.getResidue() + "</base>\n");
                buf.append(indent + "  </potential_sequencing_error>\n");
            }
        }
        if (include_residues && seq.getResidues() != null && !seq.getResidues().equals("")) {
            buf.append(indent + "  <residues>\n");
            buf.append(writeResidues(indent + "    ", seq.getResidues()));
            buf.append(indent + "  </residues>\n");
        }
        buf.append(indent + "</seq>\n");
        return buf.toString();
    }

    private static String writeResidues(String indent, String residues) {
        StringBuffer buf = new StringBuffer();
        int residues_per_line = 50;
        int last_full_line = (residues.length() / residues_per_line) - 1;
        try {
            int start = 0;
            for (int i = 0; i <= last_full_line; i++) {
                buf.append(indent);
                buf.append(residues.substring(start, start + residues_per_line));
                buf.append("\n");
                start += residues_per_line;
            }
            if (start < residues.length()) {
                buf.append(indent);
                buf.append(residues.substring(start));
                buf.append("\n");
            }
        } catch (Exception ex) {
            logger.error("length=" + residues.length() + " " + "last_full_line=" + last_full_line, ex);
        }
        return buf.toString();
    }

    private static String writeXref(String indent, String db, String id, String key) {
        StringBuffer buf = new StringBuffer();
        if (db != null && !db.equals("")) {
            buf.append(indent + "<dbxref>\n");
            buf.append(indent + "  <xref_db>" + db + "</xref_db>\n");
            buf.append(indent + "  <db_xref_id>" + id + "</db_xref_id>\n");
            buf.append(indent + "</dbxref>\n");
        }
        return buf.toString();
    }

    private static String writeDescription(String indent, String desc) {
        StringBuffer buf = new StringBuffer();
        if (desc != null && !desc.equals("")) {
            buf.append(indent + "<description>\n");
            buf.append("  " + XML_util.transformToPCData(desc.trim()) + "\n");
            buf.append(indent + "</description>\n");
        }
        return buf.toString();
    }

    private static String writeComment(String indent, Comment comment) {
        StringBuffer buf = new StringBuffer();
        buf.append(indent + "<comment");
        buf.append(writeID(comment.getId(), "comment"));
        buf.append(" internal=\"" + comment.isInternal() + "\"");
        buf.append(">\n");
        buf.append(indent + "  <text>\n");
        buf.append(XML_util.transformToPCData(comment.getText()) + "\n");
        buf.append(indent + "  </text>\n");
        buf.append(indent + "  <person>" + comment.getPerson() + "</person>\n");
        buf.append(writeDate(indent + "  ", new Date(comment.getTimeStamp()), ""));
        buf.append(indent + "</comment>\n");
        return buf.toString();
    }

    private static String writeDate(String indent, SeqFeatureI sf) {
        StringBuffer buf = new StringBuffer();
        String value = sf.getProperty("date");
        if (value != null && !value.equals("")) {
            buf.append(writeDate(indent, DateUtil.makeADate(value), value));
        }
        return buf.toString();
    }

    /** This now converts all dates to GMT, because otherwise, it saves local timezones
   *  that it (Java.util.Date) is then unable to parse! */
    public static String writeDate(String indent, Date date, String value) {
        StringBuffer buf = new StringBuffer();
        if (date != null) buf.append(indent + "<date timestamp=\"" + date.getTime() + "\">" + DateUtil.toGMT(date) + "</date>\n"); else if (value != null && !value.equals("")) buf.append(indent + "<date>" + value + "</date>\n");
        return buf.toString();
    }

    private static String writeSynonyms(String indent, Vector syns) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < syns.size(); i++) {
            Synonym syn = (Synonym) syns.elementAt(i);
            if (!(syn == null) && !syn.getName().equals("")) {
                String fixed = XML_util.transformToPCData(syn.getName(), true);
                if (syn.getProperty("is_internal").equals("1")) writeProperty(indent, "internal_synonym", fixed, buf); else {
                    String synAtts = getSynonymAttributes(syn);
                    buf.append(indent + "<synonym" + synAtts + ">" + fixed + "</synonym>\n");
                }
            }
        }
        return buf.toString();
    }

    /** in game 1.1 synonyms have owner and internal as attributes */
    private static String getSynonymAttributes(Synonym syn) {
        StringBuffer sb = new StringBuffer("");
        if (syn.hasOwner()) sb.append(" owner=\"" + syn.getOwner() + "\"");
        return sb.toString();
    }

    private static String writeSpan(String indent, AnnotatedFeatureI sf, boolean isa_gene, CurationSet curation) {
        return writeSpan(indent, sf, isa_gene, curation, false);
    }

    /** Called by writeFeatureSet for transcripts exons - this is for annots not
      results. for game 1.1 this is also called for protein spans */
    private static String writeSpan(String indent, AnnotatedFeatureI sf, boolean isa_gene, CurationSet curation, boolean writeNameAndId) {
        StringBuffer buf = new StringBuffer();
        buf.append(indent + "<feature_span");
        if (writeNameAndId) buf.append(writeID(sf.getId(), "feature_span"));
        buf.append(">\n");
        if (isa_gene && !sf.getFeatureType().equals("")) buf.append(indent + "  <type>" + sf.getFeatureType() + "</type>\n");
        if (writeNameAndId) buf.append(writeName(sf.getName(), "feature_span", indent));
        buf.append(writeSeqRelationship(indent + "  ", sf, "query", "", curation));
        buf.append(indent + "</feature_span>\n");
        return buf.toString();
    }

    private static String writeName(String name, String prefix, String indent) {
        if (name != null && !name.equals("") && !name.equals("?") && !name.equals("no_name") && !(!prefix.equals("") && name.startsWith(prefix))) return (indent + "  " + "<name>" + XML_util.transformToPCData(name) + "</name>\n"); else return "";
    }

    private static String writeTSS(String indent, SeqFeatureI sf, String element, CurationSet curation) {
        StringBuffer buf = new StringBuffer();
        TranslationI translation = sf.getTranslation();
        if (translation.hasTranslationStart()) {
            buf.append(indent + "<" + element);
            if (sf.isTranscript()) {
                SeqFeatureI protein = sf.getProteinFeat();
                if (protein != null && protein.getRefSequence().hasResidues() && protein.hasId()) {
                    buf.append(" produces_seq=\"" + protein.getId() + "\"");
                }
            }
            buf.append(">\n");
            buf.append(indent + "  <type>start_codon</type>\n");
            buf.append(indent + "  <seq_relationship");
            buf.append(" type=\"query\"");
            if (sf.getRefSequence() != null) buf.append(" seq=\"" + sf.getRefSequence().getAccessionNo() + "\">\n");
            int offset = (curation == null ? 0 : curation.getStart() - 1);
            int tss = translation.getTranslationStart() - offset;
            buf.append(indent + "    <span>\n");
            buf.append(indent + "      <start>" + tss + "</start>\n");
            buf.append(indent + "      <end>" + (tss + (2 * sf.getStrand())) + "</end>\n");
            buf.append(indent + "    </span>\n");
            buf.append(indent + "  </seq_relationship>\n");
            buf.append(indent + "</" + element + ">\n");
        }
        return buf.toString();
    }

    /** subtracts curation start (and adds 1) to start and end for wrting to game
   * as game coords are relative to cur/map where apollo coords are absolute
   */
    private static String writeSeqRelationship(String indent, SeqFeatureI sf, String rel_type, String alignment, CurationSet curation) {
        StringBuffer buf = new StringBuffer();
        buf.append(indent + "<seq_relationship");
        if (rel_type != null && !rel_type.equals("")) buf.append(" type=\"" + rel_type + "\"");
        String seq_id = (sf.getRefSequence() != null ? (sf.getRefSequence().getAccessionNo() != null ? sf.getRefSequence().getAccessionNo() : sf.getRefSequence().getName()) : null);
        if (seq_id != null) buf.append(" seq=\"" + seq_id + "\"");
        buf.append(">\n");
        int offset = (curation == null || curation.getStart() < 0 ? 0 : curation.getStart() - 1);
        buf.append(indent + "  <span>\n");
        buf.append(indent + "    <start>" + (sf.getStart() - offset) + "</start>\n");
        buf.append(indent + "    <end>" + (sf.getEnd() - offset) + "</end>\n");
        buf.append(indent + "  </span>\n");
        if (alignment != null && !alignment.equals("")) {
            buf.append(indent + "  <alignment>\n");
            buf.append(indent + "    " + alignment + "\n");
            buf.append(indent + "  </alignment>\n");
        }
        buf.append(indent + "</seq_relationship>\n");
        return buf.toString();
    }

    /** Write analysis directly to print writer. Previously returned a string.
      Turns out it's far far more efficient to write straight to the writer
      than return a string */
    private static void writeAnalysesToWriter(CurationSet curation, boolean game2_format, PrintWriter pw) {
        StrandedFeatureSetI analyses = curation.getResults();
        if (analyses != null) {
            for (int i = 0; i < analyses.size(); i++) {
                FeatureSetI analysis = (FeatureSetI) analyses.getFeatureAt(i);
                if (analysis.getFeatureType() != null && !analysis.getFeatureType().equals("codons") && !analysis.getFeatureType().equals("Gene")) {
                    pw.print(writeAnalysis("  ", analysis, curation, game2_format));
                }
            }
        }
    }

    private static String writeAnalysis(String indent, FeatureSetI sf, CurationSet curation, boolean game2_format) {
        StringBuffer buf = new StringBuffer();
        buf.append(indent + "<computational_analysis>\n");
        String program = sf.getProgramName();
        if (program == null || program.equals("") || program.equals(RangeI.NO_TYPE)) program = sf.getProperty("type");
        if (program == null || program.equals("") || program.equals(RangeI.NO_TYPE)) program = sf.getTopLevelType();
        if (program != null && !program.equals("") && !program.equals(RangeI.NO_TYPE)) buf.append(indent + "  <program>" + program + "</program>\n");
        if (sf.getDatabase() != null && !sf.getDatabase().equals("")) buf.append(indent + "  <database>" + sf.getDatabase() + "</database>\n"); else {
            String type = sf.getTopLevelType();
            if (type != null && !type.equals("") && !type.equals(RangeI.NO_TYPE)) buf.append(indent + "  <type>" + type + "</type>\n");
        }
        String value = sf.getProperty("version");
        if (value != null && !value.equals("")) {
            buf.append(indent + "  <version>" + value + "</version>\n");
        }
        buf.append(writeProperties(indent + "  ", sf, "property"));
        value = sf.getProperty("type");
        if (value != null && !value.equals("") && !value.equals(RangeI.NO_TYPE)) {
            writeProperty(indent + "  ", "type", value, buf);
        }
        buf.append(writeDate(indent + "  ", sf));
        Vector results = sf.getFeatures();
        for (int i = 0; i < results.size(); i++) {
            if (results.elementAt(i) instanceof FeatureSetI) {
                FeatureSetI result = (FeatureSetI) results.elementAt(i);
                int levels = result.numberOfGenerations();
                if (game2_format || (!game2_format && levels == 2)) buf.append(writeResult(indent + "  ", result, curation)); else buf.append(skipLevel(indent, result, curation));
            } else if (results.elementAt(i) instanceof FeaturePairI) {
                FeaturePairI result = (FeaturePairI) results.elementAt(i);
                if (game2_format) {
                    buf.append(writeHSP(indent + "  ", result, curation));
                } else {
                    FeatureSetI fake = new FeatureSet(result.getLow(), result.getHigh(), result.getFeatureType(), result.getStrand());
                    fake.addFeature(result);
                    buf.append(writeResult(indent + "  ", fake, curation));
                }
            } else if (results.elementAt(i) instanceof SeqFeatureI) {
                SeqFeatureI result = (SeqFeatureI) results.elementAt(i);
                if (game2_format) {
                    buf.append(writeFeature(indent + "  ", result, curation));
                } else {
                    FeatureSetI fake = new FeatureSet(result.getLow(), result.getHigh(), result.getFeatureType(), result.getStrand());
                    fake.addFeature(result);
                    buf.append(writeResult(indent + "  ", fake, curation));
                }
            } else {
                logger.warn("Don't know what to do to save " + results.elementAt(i).getClass().getName());
            }
        }
        buf.append(indent + "</computational_analysis>\n");
        return buf.toString();
    }

    /** Default element_name for properties is "property" (sometimes, for unknown
      reasons, it's "output") */
    private static void writeProperty(String indent, String type, String value, StringBuffer buf) {
        writeProperty(indent, "property", type, value, buf);
    }

    private static void writeProperty(String indent, String element_name, String type, String value, StringBuffer buf) {
        buf.append(indent + "<" + element_name + ">\n");
        buf.append(indent + "  <type>" + type + "</type>\n");
        buf.append(indent + "  <value>" + value + "</value>\n");
        buf.append(indent + "</" + element_name + ">\n");
    }

    private static String skipLevel(String indent, FeatureSetI fs, CurationSet curation) {
        StringBuffer buf = new StringBuffer();
        int kid_count = fs.size();
        for (int i = 0; i < kid_count; i++) {
            FeatureSetI result = (FeatureSetI) fs.getFeatureAt(i);
            int levels = result.numberOfGenerations();
            if (levels == 2) buf.append(writeResult(indent + "  ", result, curation)); else {
                buf.append(skipLevel(indent, result, curation));
            }
        }
        return buf.toString();
    }

    public static String debugName(SeqFeatureI sf) {
        String name = null;
        if (sf instanceof FeaturePairI) name = ((FeaturePairI) sf).getHitSequence().getName(); else if (sf instanceof FeatureSetI) name = ((FeatureSetI) sf).getHitSequence().getName(); else name = sf.getRefSequence().getName();
        if (name == null) {
            logger.warn("Something seriously wrong with feature " + sf.toString());
        }
        return name;
    }

    private static void debugFeature(SeqFeatureI sf, String prefix) {
        String name = debugName(sf);
        if (name.startsWith("AT31875")) {
            logger.debug(prefix + "\n\t" + name + " " + " strand=" + sf.getStrand() + " start=" + sf.getStart() + " end =" + sf.getEnd() + "\n\tlength=" + sf.length() + " expect=" + sf.getScore("expect") + " score=" + sf.getScore() + " type=" + sf.getFeatureType());
            if (sf instanceof FeatureSetI) {
                FeatureSetI fs = (FeatureSetI) sf;
                for (int i = 0; i < fs.size(); i++) {
                    FeaturePairI fp = (FeaturePairI) fs.getFeatureAt(i);
                    logger.debug("\tSpan " + (i + 1) + " genomic start=" + fp.getStart() + " genomic end =" + fp.getEnd() + " EST start=" + fp.getHstart() + " EST end =" + fp.getHend());
                }
            }
        }
    }

    private static String writeResult(String indent, FeatureSetI sf, CurationSet curation) {
        StringBuffer buf = new StringBuffer();
        buf.append(indent + "<result_set");
        buf.append(writeID(sf.getId(), "result_set"));
        buf.append(">\n");
        buf.append(writeName(sf.getName(), "result_set", indent));
        if (sf.hasTranslationStart()) {
            sf.addFeature(new SeqFeature(sf.getTranslationStart(), sf.getTranslationStart() + 2, "start codon"));
        }
        Vector spans = sf.getFeatures();
        if (spans.size() > 1) {
            buf.append(writeSeqRelationship(indent + "  ", sf, "query", "", curation));
        }
        buf.append(writeOutputs(indent + "  ", sf));
        buf.append(writeProperties(indent + "  ", sf, "output"));
        for (int i = 0; i < spans.size(); i++) {
            if (spans.elementAt(i) instanceof FeaturePair) {
                FeaturePair span = (FeaturePair) spans.elementAt(i);
                buf.append(writeHSP(indent + "  ", span, curation));
            } else if (spans.elementAt(i) instanceof FeatureSetI) {
                buf.append(writeResult(indent + "  ", (FeatureSetI) spans.elementAt(i), curation));
            } else {
                SeqFeatureI span = (SeqFeatureI) (spans.elementAt(i));
                buf.append(writeFeature(indent + " ", span, curation));
            }
        }
        buf.append(indent + "</result_set>\n");
        return buf.toString();
    }

    private static String writeHSP(String indent, FeaturePairI sf, CurationSet curation) {
        StringBuffer buf = new StringBuffer();
        buf.append(indent + "<result_span");
        buf.append(">\n");
        buf.append(writeName(sf.getName(), "result_span", indent));
        buf.append(indent + "  <score>" + sf.getScore() + "</score>\n");
        buf.append(writeOutputs(indent + "  ", sf));
        buf.append(writeProperties(indent + "  ", sf, "output"));
        if (sf.getCigar() != null) {
            buf.append(indent + "  <output>\n");
            buf.append(indent + "    <type>cigar</type>\n");
            buf.append(indent + "    <value>" + sf.getCigar() + "</value>\n");
            buf.append(indent + "  </output>\n");
        }
        String query_alignment = (sf.getExplicitAlignment() == null ? "" : sf.getExplicitAlignment());
        buf.append(writeSeqRelationship(indent + "  ", sf.getQueryFeature(), "query", query_alignment, curation));
        if (sf.getHitFeature().getRefSequence() != null) {
            SeqFeatureI sf_2 = sf.getHitFeature();
            String sbjct_alignment = sf.getHitFeature().getExplicitAlignment();
            if (sbjct_alignment == null) sbjct_alignment = "";
            buf.append(writeSeqRelationship(indent + "  ", sf_2, "subject", sbjct_alignment, null));
        }
        buf.append(indent + "</result_span>\n");
        return buf.toString();
    }

    private static String writeFeature(String indent, SeqFeatureI sf, CurationSet curation) {
        StringBuffer buf = new StringBuffer();
        buf.append(indent + "<result_span");
        buf.append(writeID(sf.getId(), "result_span"));
        buf.append(">\n");
        buf.append(writeName(sf.getName(), "result_span", indent));
        buf.append(indent + "  <type>" + sf.getTopLevelType() + "</type>\n");
        buf.append(indent + "  <score>" + sf.getScore() + "</score>\n");
        buf.append(writeOutputs(indent + "  ", sf));
        buf.append(writeProperties(indent + "  ", sf, "output"));
        buf.append(writeSeqRelationship(indent + "  ", sf, "query", "", curation));
        buf.append(indent + "</result_span>\n");
        return buf.toString();
    }

    private static String writeOutputs(String indent, SeqFeatureI sf) {
        StringBuffer buf = new StringBuffer();
        Hashtable scores = sf.getScores();
        Enumeration e = scores.keys();
        while (e.hasMoreElements()) {
            String type = (String) e.nextElement();
            Score output = (Score) scores.get(type);
            buf.append(indent + "<output>\n");
            buf.append(indent + "  <type>" + type + "</type>\n");
            buf.append(indent + "  <value>" + output.getValue() + "</value>\n");
            buf.append(indent + "</output>\n");
        }
        return buf.toString();
    }

    private static String writeProperties(String indent, SeqFeatureI sf, String element_name) {
        StringBuffer buf = new StringBuffer();
        Hashtable props = sf.getPropertiesMulti();
        Enumeration e = props.keys();
        while (e.hasMoreElements()) {
            String type = (String) e.nextElement();
            if (type.equals("date") || type.equals("version") || type.equals("type") || type.equals("program") || type.equals("database") || type.equals("alignment") || type.equals("sim4_set") || type.equals("description")) {
                continue;
            }
            if (ChadoXmlWrite.isSpecialProperty(type) && !type.equals("organism")) continue;
            Vector values = sf.getPropertyMulti(type);
            if (values == null) continue;
            for (int i = 0; i < values.size(); i++) {
                String value = (String) values.elementAt(i);
                if (type.equals("dicistronic") && value.equals("false")) continue;
                if (value.startsWith(apollo.dataadapter.chadoxml.ChadoXmlAdapter.FIELD_LABEL)) {
                    buf.append(indent + "<" + type + ">");
                    value = value.substring((apollo.dataadapter.chadoxml.ChadoXmlAdapter.FIELD_LABEL).length());
                    buf.append(value);
                    buf.append("</" + type + ">\n");
                } else writeProperty(indent, element_name, type, value, buf);
            }
        }
        return buf.toString();
    }

    private static String writeGameEnd() {
        return ("</game>\n");
    }
}
