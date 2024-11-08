package apollo.dataadapter.chadoxml;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.JOptionPane;
import apollo.config.Config;
import apollo.config.ApolloNameAdapterI;
import apollo.dataadapter.chado.ChadoTransactionTransformer;
import apollo.dataadapter.TransactionOutputAdapter;
import apollo.dataadapter.gamexml.TransactionXMLAdapter;
import apollo.datamodel.*;
import apollo.datamodel.seq.GAMESequence;
import apollo.editor.Transaction;
import apollo.editor.UserName;
import apollo.util.DateUtil;
import org.apache.log4j.*;
import org.bdgp.xml.XML_util;

/** ChadoXmlWrite: writes Chado XML to a file.
 *  Main method is writeXML. 
 *  Please note that this is still somewhat FlyBase-specific, though some
 *  of the constants are now configurable in the style file. */
public class ChadoXmlWrite {

    protected static final Logger logger = LogManager.getLogger(ChadoXmlWrite.class);

    static String typeIDOntology = "SO";

    static String IDForDbxref = "SO:0000105";

    static String defaultChadoDatabase = "FlyBase";

    static String database = defaultChadoDatabase;

    static String defaultSynonymAuthor = "gadfly3";

    static String defaultSynonymPubType = "computer file";

    static String featurepropCV = "annotation property type";

    static String TAB = "  ";

    static String CVTermForResults = "match";

    static String chromosome = "";

    static ApolloNameAdapterI nameAdapter = null;

    static String CHADO_XML_VERSION = "FlyBase v1.0, no macros";

    /** Mention whether an annotation may have been edited, but do so only once. */
    static String mayHaveBeenEditedMessage = "";

    /** Main method for writing out Chado XML */
    public static boolean writeXML(CurationSet curation, String file_str, String preamble, boolean saveAnnots, boolean saveResults, ApolloNameAdapterI localNameAdapter, String version) {
        nameAdapter = localNameAdapter;
        String filename = apollo.util.IOUtil.findFile(file_str, true);
        if (filename == null) {
            String message = "Failed to open file for writing: " + file_str + "\nThe directory may not exist, or may be unwriteable.";
            logger.warn(message);
            JOptionPane.showMessageDialog(null, message, "Warning", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file_str);
        } catch (Exception ex) {
            logger.error("writeXML: caught exception opening " + file_str + " (" + filename + ")", ex);
            return false;
        }
        if (fileWriter == null) return false;
        String what = "annotations and evidence";
        if (!saveAnnots) what = "evidence (results) only";
        if (!saveResults) what = "annotations only (no results)";
        String msg = "Saving " + what + " to file " + filename;
        logger.info(msg);
        boolean success = writeXML(curation, fileWriter, preamble, saveAnnots, saveResults, nameAdapter, version);
        try {
            fileWriter.close();
        } catch (Exception ex) {
            logger.error("writeXML: caught exception closing " + filename, ex);
            return false;
        }
        return success;
    }

    /** This currently assumes CurationSet is the root object,  and
   *  that all its children do not contain references to other
   *  AnnotatedSeqs that would need to be included in the XML doc.
   *  Returns false if something went wrong. 
   *  Preamble is the lines before <chado> that were retrieved from
   *  the original input file by ChadoXmlAdapter.getPreamble. */
    public static boolean writeXML(CurationSet curation, FileWriter fileWriter, String preamble, boolean saveAnnots, boolean saveResults, ApolloNameAdapterI nameAdapter, String version) {
        initConstants();
        String startingIndent = TAB;
        BufferedWriter bw = new BufferedWriter(fileWriter);
        PrintWriter pw = new PrintWriter(bw);
        try {
            pw.print(writeBegin(version));
            pw.print("\n" + preamble);
            pw.print("<chado date=\"" + DateUtil.toGMT(new Date()) + "\">\n");
            writeGenomePosition(curation, startingIndent, pw);
            if (saveAnnots) writeAnnotations(curation, startingIndent, pw);
            if (saveResults) writeResults(curation, startingIndent, pw);
            pw.print(writeEnd());
            pw.close();
        } catch (Exception ex) {
            logger.error("Caught exception committing XML", ex);
            return false;
        }
        return true;
    }

    /** Initialize some of the constants used when writing out ChadoXML for FlyBase.
   *  These are defined in fly.style (or whichever style is used for the ChadoXML
   *  adapter) and read in as generic parameters by Style. */
    private static void initConstants() {
        if (!(Config.getStyle().getParameter("typeIDOntology")).equals("")) typeIDOntology = Config.getStyle().getParameter("typeIDOntology");
        if (!(Config.getStyle().getParameter("IDForDbxref")).equals("")) IDForDbxref = Config.getStyle().getParameter("IDForDbxref");
        if (!Config.getStyle().getParameter("defaultChadoDatabase").equals("")) {
            defaultChadoDatabase = Config.getStyle().getParameter("defaultChadoDatabase");
            logger.debug("Got new setting for defaultDatabase: " + defaultChadoDatabase);
            database = defaultChadoDatabase;
        }
        if (!Config.getStyle().getParameter("defaultSynonymAuthor").equals("")) defaultSynonymAuthor = Config.getStyle().getParameter("defaultSynonymAuthor");
        if (!Config.getStyle().getParameter("defaultSynonymPubType").equals("")) defaultSynonymPubType = Config.getStyle().getParameter("defaultSynonymPubType");
        if (!Config.getStyle().getParameter("featurepropCV").equals("")) featurepropCV = Config.getStyle().getParameter("featurepropCV");
    }

    /** Write the ChadoXML header */
    private static String writeBegin(String ApolloVersion) {
        StringBuffer buf = new StringBuffer();
        String XML_VERSION = "1.0";
        buf.append("<?xml version=\"" + XML_VERSION + "\" encoding=\"ISO-8859-1\"?>\n");
        buf.append("<!-- ChadoXML file (" + CHADO_XML_VERSION + ") created by " + UserName.getUserName() + " on " + DateUtil.toGMT(new Date()) + " -->\n");
        buf.append("<!-- " + ApolloVersion + " -->\n");
        return buf.toString();
    }

    private static void writeGenomePosition(CurationSet curation, String indent, PrintWriter pw) {
        SequenceI genomic_seq = curation.getRefSequence();
        pw.print(indent + "<_appdata  name=\"title\">" + curation.getName() + "</_appdata>\n");
        chromosome = curation.getChromosome();
        if (chromosome == null || chromosome.equals("")) {
            logger.warn("couldn't find chromosome for curation " + curation.getName());
            chromosome = "";
        }
        pw.print(indent + "<_appdata  name=\"arm\">" + chromosome + "</_appdata>\n");
        pw.print(indent + "<_appdata  name=\"fmin\">" + (curation.getStart() - 1) + "</_appdata>\n");
        pw.print(indent + "<_appdata  name=\"fmax\">" + curation.getEnd() + "</_appdata>\n");
        writeGenomicSequence((AbstractSequence) genomic_seq, indent + "", true, pw);
        if (genomic_seq != null && genomic_seq.getGenomicErrors() != null) {
            String message = "there are genomic sequencing errors annotated, but I don't know how to save them in ChadoXML!";
            logger.warn(message);
            JOptionPane.showMessageDialog(null, message, "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static void writeGenomicSequence(SequenceI seq, String indent, boolean include_residues, PrintWriter pw) {
        if (seq == null) return;
        if (include_residues && seq.getResidues() != null && !seq.getResidues().equals("")) {
            pw.print(indent + "<_appdata  name=\"residues\">");
            pw.print(seq.getResidues());
            pw.print("</_appdata>\n");
        }
    }

    private static void writeAnnotations(CurationSet curation, String indent, PrintWriter pw) {
        StrandedFeatureSetI annots = curation.getAnnots();
        if (annots != null) {
            AnnotatedFeatureI prevAnnot = null;
            AnnotatedFeatureI annot = null;
            for (int i = 0; i < annots.size(); i++) {
                prevAnnot = annot;
                annot = (AnnotatedFeatureI) annots.getFeatureAt(i);
                if (prevAnnot != null && (annot.getId() == null || (annot.getId().equals(prevAnnot.getId())))) logger.info("Annotation " + annot.getId() + "has strand=0 so it got added to both strands.  Saving single copy."); else writeAnnotation(curation, annot, indent, pw);
            }
        }
    }

    private static void writeAnnotation(CurationSet curation, AnnotatedFeatureI annot, String startingIndent, PrintWriter pw) {
        pw.print(startingIndent + "<feature>\n");
        String indent = startingIndent + TAB;
        DbXref xref = ((SeqFeature) annot).getPrimaryDbXref();
        if (xref != null) writeXref(indent, xref.getDbName(), xref.getIdValue(), xref.getVersion(), false, -1, pw); else logger.warn("couldn't find primary xref for annotation with uniquename " + annot.getId());
        pw.print(indent + "<is_analysis>0</is_analysis>\n");
        if (!(annot.getProperty("is_obsolete")).equals("")) pw.print(writeField(indent, "is_obsolete", annot.getProperty("is_obsolete")));
        pw.print(indent + "<name>" + XML_util.transformToPCData(annot.getName()) + "</name>\n");
        writeFields(indent, annot, pw);
        writeOrganismFromFeature(indent, annot, curation, pw);
        if (annot.getFeatures().size() > 0) {
            SequenceI seq = annot.getRefSequence();
            if (seq != null && seq.getResidues() != null && !seq.getResidues().equals("")) {
                int seqlen;
                if (annot.getStart() > annot.getEnd()) seqlen = annot.getStart() - annot.getEnd() + 1; else seqlen = annot.getEnd() - annot.getStart() + 1;
                pw.print(writeField(indent, "seqlen", seqlen + ""));
            }
        }
        writeTimes(indent, annot, pw);
        writeCVtype(indent, typeIDOntology, annotTypeForChado(annot), pw);
        pw.print(writeField(indent, "uniquename", XML_util.transformToPCData(annot.getId())));
        boolean isa_gene = annot.getTopLevelType().equalsIgnoreCase("gene");
        Vector transcripts = annot.getFeatures();
        for (int i = 0; i < transcripts.size(); i++) {
            Transcript fs = (Transcript) transcripts.elementAt(i);
            writeTranscript(indent, fs, isa_gene, curation, pw);
        }
        writeProperty(indent, "owner", annot.getOwner(), 0, pw);
        writeComments(indent, annot, pw);
        Vector xrefs = annot.getDbXrefs();
        for (int i = 0; i < xrefs.size(); i++) {
            xref = (DbXref) xrefs.elementAt(i);
            if (xref.isSecondary()) writeXref(indent, xref.getDbName(), xref.getIdValue(), xref.getVersion(), true, xref.getCurrent(), pw); else logger.debug("Not writing primary dbxref " + xref.getIdValue() + " as feature_dbxref for " + annot.getId());
        }
        writeSynonyms(indent, annot, pw);
        writeFeatureloc(indent, annot, pw);
        writeProperties(indent, annot, pw);
        if (annot.isProblematic()) writeProperty(indent, "problem", "true", 0, pw);
        pw.print(startingIndent + "</feature>\n");
    }

    /** Write either a plain dbxref_id (if feature_dbxref is false) or a feature_dbxref,
   *  which has a <feature_dbxref> around the dbxref_id. 
   *  is_current is only used for feature_dbxref. */
    private static void writeXref(String indent, String db, String id, String ver, boolean feature_dbxref, int isCurrent, PrintWriter pw) {
        if (id == null || id.equals("")) return;
        String original_indent = indent;
        if (feature_dbxref) {
            pw.print(indent + "<feature_dbxref>\n");
            indent += TAB;
        }
        pw.print(indent + "<dbxref_id>\n");
        pw.print(indent + TAB + "<dbxref>\n");
        pw.print(writeField(indent + TAB + TAB, "accession", id));
        pw.print(indent + TAB + TAB + "<db_id>\n");
        pw.print(indent + TAB + TAB + TAB + "<db>\n");
        pw.print(writeField(indent + TAB + TAB + TAB + TAB, "name", db));
        pw.print(indent + TAB + TAB + TAB + "</db>\n");
        pw.print(indent + TAB + TAB + "</db_id>\n");
        if (ver != null) pw.print(writeField(indent + TAB + TAB, "version", ver));
        pw.print(indent + TAB + "</dbxref>\n");
        pw.print(indent + "</dbxref_id>\n");
        if (feature_dbxref) {
            pw.print(indent + "<is_current>" + isCurrent + "</is_current>\n");
            pw.print(original_indent + "</feature_dbxref>\n");
        }
    }

    private static void writeTranscript(String indent, Transcript transcript, boolean isa_gene, CurationSet curation, PrintWriter pw) {
        pw.print(indent + "<feature_relationship>\n");
        pw.print(indent + TAB + "<subject_id>\n");
        pw.print(indent + TAB + TAB + "<feature>\n");
        DbXref xref = transcript.getPrimaryDbXref();
        if (xref != null) writeXref(indent + TAB + TAB + TAB, xref.getDbName(), xref.getIdValue(), xref.getVersion(), false, -1, pw); else logger.warn("couldn't find primary xref for transcript with uniquename " + transcript.getId());
        pw.print(indent + TAB + TAB + TAB);
        pw.print("<is_analysis>0</is_analysis>\n");
        if (!(transcript.getProperty("is_obsolete")).equals("")) pw.print(writeField(indent + TAB + TAB + TAB, "is_obsolete", transcript.getProperty("is_obsolete")));
        SequenceI seq = transcript.get_cDNASequence();
        String checksum = seq.getChecksum();
        if (checksum != null && !checksum.equals("")) {
            pw.print(writeField(indent + TAB + TAB + TAB, "md5checksum", checksum));
        }
        pw.print(writeField(indent + TAB + TAB + TAB, "name", XML_util.transformToPCData(transcript.getName())));
        writeFields(indent + TAB + TAB + TAB, transcript, pw);
        writeOrganismFromFeature(indent + TAB + TAB + TAB, transcript, curation, pw);
        if (seq != null && seq.getResidues() != null && !seq.getResidues().equals("")) {
            pw.print(writeField(indent + TAB + TAB + TAB, "residues", seq.getResidues()));
            pw.print(writeField(indent + TAB + TAB + TAB, "seqlen", seq.getResidues().length() + ""));
        }
        writeTimes(indent + TAB + TAB + TAB, transcript, pw);
        writeCVtype(indent + TAB + TAB + TAB, typeIDOntology, transcriptTypeForChado(transcript), pw);
        pw.print(writeField(indent + TAB + TAB + TAB, "uniquename", XML_util.transformToPCData(transcript.getId())));
        boolean edited = mayHaveBeenEdited(transcript, curation);
        if (edited) {
            String message = transcript.getRefFeature().getName() + " may have been edited--updaing exon names and ids";
            if (!mayHaveBeenEditedMessage.equals(message)) {
                logger.info(message);
                mayHaveBeenEditedMessage = message;
            }
        }
        Vector exons = transcript.getFeatures();
        logger.debug("Writing transcript " + transcript.getName() + " (" + exons.size() + " exons)");
        for (int i = 0; i < exons.size(); i++) {
            Exon exon = (Exon) exons.elementAt(i);
            if (edited || exon.getName().equals("no_name")) renameExon(exon, i, curation);
            writeExon(indent + TAB + TAB + TAB, exon, i, pw);
        }
        if (isa_gene) {
            writePeptide(indent + TAB + TAB + TAB, transcript, curation, pw);
        }
        writeFeatureloc(indent + TAB + TAB + TAB, transcript, pw);
        Vector xrefs = transcript.getDbXrefs();
        for (int i = 0; i < xrefs.size(); i++) {
            xref = (DbXref) xrefs.elementAt(i);
            if (xref.isSecondary()) writeXref(indent + TAB + TAB + TAB, xref.getDbName(), xref.getIdValue(), xref.getVersion(), true, xref.getCurrent(), pw);
        }
        writeSynonyms(indent + TAB + TAB + TAB, transcript, pw);
        writeProperty(indent + TAB + TAB + TAB, "owner", transcript.getOwner(), 0, pw);
        writeComments(indent + TAB + TAB + TAB, transcript, pw);
        writeProperties(indent + TAB + TAB + TAB, transcript, pw);
        if (isa_gene) writeWeirdTranscriptProperties(indent + TAB + TAB + TAB, transcript, pw);
        pw.print(indent + TAB + TAB + "</feature>\n");
        pw.print(indent + TAB + "</subject_id>\n");
        writeCVtype(indent + TAB, "relationship type", "partof", pw);
        pw.print(indent + "</feature_relationship>\n");
    }

    /** Write the peptide associated with transcript */
    private static void writePeptide(String indent, Transcript transcript, CurationSet curation, PrintWriter pw) {
        SequenceI seq = transcript.getPeptideSequence();
        Protein protFeat = transcript.getProteinFeat();
        if (seq == null) {
            logger.warn("peptide seq is null for transcript " + transcript.getName() + "--parent annot type is " + transcript.getRefFeature().getTopLevelType());
            return;
        }
        pw.print(indent + "<feature_relationship>\n");
        pw.print(indent + TAB + "<subject_id>\n");
        pw.print(indent + TAB + TAB + "<feature>\n");
        DbXref xref = protFeat.getPrimaryDbXref();
        if (xref != null) writeXref(indent + TAB + TAB + TAB, xref.getDbName(), xref.getIdValue(), xref.getVersion(), false, -1, pw);
        pw.print(indent + TAB + TAB + TAB);
        pw.print("<is_analysis>0</is_analysis>\n");
        if (!(protFeat.getProperty("is_obsolete")).equals("")) pw.print(writeField(indent + TAB + TAB + TAB, "is_obsolete", protFeat.getProperty("is_obsolete")));
        String checksum = seq.getChecksum();
        if (checksum != null && !checksum.equals("")) {
            pw.print(writeField(indent + TAB + TAB + TAB, "md5checksum", checksum));
        }
        pw.print(writeField(indent + TAB + TAB + TAB, "name", XML_util.transformToPCData(seq.getName())));
        writeFields(indent + TAB + TAB + TAB, protFeat, pw);
        writeOrganismFromFeature(indent + TAB + TAB + TAB, protFeat, curation, pw);
        try {
            logger.debug("About to save peptide " + seq.getName());
            if (seq.getResidues() != null && !(seq.getResidues().equals(""))) {
                pw.print(writeField(indent + TAB + TAB + TAB, "residues", seq.getResidues()));
                pw.print(writeField(indent + TAB + TAB + TAB, "seqlen", seq.getResidues().length() + ""));
            }
        } catch (Exception e) {
            logger.error("Can't save residues for peptide " + seq.getName() + ": ", e);
        }
        writeTimes(indent + TAB + TAB + TAB, protFeat, pw);
        writeCVtype(indent + TAB + TAB + TAB, typeIDOntology, "protein", pw);
        String acc = seq.getAccessionNo();
        if (acc == null || acc.equals("")) acc = seq.getName();
        pw.print(writeField(indent + TAB + TAB + TAB, "uniquename", XML_util.transformToPCData(acc)));
        writePepFeatloc(indent + TAB + TAB + TAB, protFeat, transcript, pw);
        for (int i = 0; i < protFeat.getDbXrefs().size(); i++) {
            xref = protFeat.getDbXref(i);
            if (xref.isSecondary()) writeXref(indent + TAB + TAB + TAB, xref.getDbName(), xref.getIdValue(), xref.getVersion(), true, xref.getCurrent(), pw);
        }
        writeProperties(indent + TAB + TAB + TAB, protFeat, pw);
        writeSynonyms(indent + TAB + TAB + TAB, protFeat, pw);
        pw.print(indent + TAB + TAB + "</feature>\n");
        pw.print(indent + TAB + "</subject_id>\n");
        writeCVtype(indent + TAB, "relationship type", "producedby", pw);
        pw.print(indent + "</feature_relationship>\n");
    }

    private static void writeExon(String indent, Exon exon, int exonRank, PrintWriter pw) {
        pw.print(indent + "<feature_relationship>\n");
        writeExonRank(indent + TAB, exon, exonRank, pw);
        pw.print(indent + TAB + "<subject_id>\n");
        pw.print(indent + TAB + TAB + "<feature>\n");
        pw.print(indent + TAB + TAB + TAB);
        pw.print("<is_analysis>0</is_analysis>\n");
        if (!(exon.getProperty("is_obsolete")).equals("")) pw.print(writeField(indent + TAB + TAB + TAB, "is_obsolete", exon.getProperty("is_obsolete")));
        pw.print(writeField(indent + TAB + TAB + TAB, "name", XML_util.transformToPCData(exon.getName())));
        writeFields(indent + TAB + TAB + TAB, exon, pw);
        writeOrganismFromFeature(indent + TAB + TAB + TAB, exon, pw);
        int seqlen;
        if (exon.getStart() > exon.getEnd()) seqlen = exon.getStart() - exon.getEnd() + 1; else seqlen = exon.getEnd() - exon.getStart() + 1;
        pw.print(writeField(indent + TAB + TAB + TAB, "seqlen", seqlen + ""));
        writeTimes(indent + TAB + TAB + TAB, exon, pw);
        writeCVtype(indent + TAB + TAB + TAB, typeIDOntology, "exon", pw);
        logger.debug("Exon " + exon.getName() + ": " + exon.getLow() + "-" + exon.getHigh());
        pw.print(writeField(indent + TAB + TAB + TAB, "uniquename", exon.getId()));
        writeFeatureloc(indent + TAB + TAB + TAB, exon, pw);
        pw.print(indent + TAB + TAB + "</feature>\n");
        pw.print(indent + TAB + "</subject_id>\n");
        writeCVtype(indent + TAB, "relationship type", "partof", pw);
        pw.print(indent + "</feature_relationship>\n");
    }

    /**  3/2005: it's hard to make sure exon names and ids are correct, so for now,
       if ANY edits were done on ANY transcript belonging to the same annot as this
       one, then we'll invalidate ALL exon names and ids.  This method returns true
       if that's the case. */
    private static boolean mayHaveBeenEdited(Transcript transcript, CurationSet curation) {
        AnnotatedFeatureI annot = (AnnotatedFeatureI) transcript.getRefFeature();
        return someTransactionIncludesThisAnnot(annot, curation);
    }

    /** Returns true is there is a transaction that includes annotInQuestion */
    private static boolean someTransactionIncludesThisAnnot(AnnotatedFeatureI annotInQuestion, CurationSet curation) {
        if (curation.getTransactionManager() == null) return false;
        boolean edited = false;
        try {
            edited = curation.getTransactionManager().featureHasBeenEdited(annotInQuestion);
        } catch (Exception e) {
            logger.error("Error checking transactions: ", e);
        }
        return edited;
    }

    /** Exon (or transcript or annot it belongs to) may have been changed--assign it
   *  a new name, ID, and rank. */
    private static void renameExon(Exon exon, int exonNum, CurationSet curation) {
        exon.setName(nameAdapter.generateName(curation.getAnnots(), curation.getName(), exon));
        exon.setId(nameAdapter.generateId(curation.getAnnots(), curation.getName(), exon));
        assignExonRank(exon, exonNum);
    }

    /** Frank wrote (on 17 Mar 2005):
   *   The <rank> should be assigned sequentially and contiguously on a per
   *   transcript basis.  If CG12345-RA has 5 exons, they should have rank
   *   numbers 1,2,3,4,5.  (regardless of the rank numbers of those same
   *   exons in other transcripts of the same gene.)

   *   (Should this go in the name adapter?)
  */
    private static void assignExonRank(Exon exon, int rank) {
        exon.replaceProperty("rank", Integer.toString(rank));
    }

    /** If feature is a pair (e.g. a blast hit), it will have TWO featurelocs,
   *  for subject and query.
   *  If it is not a pair (e.g. an exon), it will only have a query featureloc. */
    private static void writeFeatureloc(String indent, SeqFeatureI sf, PrintWriter pw) {
        int strand = sf.getStrand();
        if (sf.getProperty("unstranded").equals("true")) strand = 0;
        int start, end;
        if (sf instanceof FeaturePair) {
            FeaturePair fp = (FeaturePair) sf;
            SeqFeatureI query = fp.getQueryFeature();
            SeqFeatureI subject = fp.getHitFeature();
            start = subject.getLow() - 1;
            end = subject.getHigh();
            writeFeatureloc(indent, start, end, strand, false, false, subject, true, pw);
            start = query.getLow() - 1;
            end = query.getHigh();
            writeFeatureloc(indent, start, end, strand, false, false, query, false, pw);
        } else {
            start = sf.getLow() - 1;
            end = sf.getHigh();
            if (sf.getProperty("unstranded").equals("true") && sf.getStrand() == -1) {
                logger.warn("Unstranded feature " + sf.getName() + " had its fmax/fmin swapped when it was forced\nonto the minus strand--swapping back for writing.");
                int temp = start;
                start = end - 1;
                end = temp + 1;
            }
            boolean isFminPartial = false;
            boolean isFmaxPartial = false;
            if (sf instanceof Transcript) {
                isFminPartial = isPartial((Transcript) sf, "fmin");
                isFmaxPartial = isPartial((Transcript) sf, "fmax");
            }
            writeFeatureloc(indent, start, end, strand, isFminPartial, isFmaxPartial, sf, false, pw);
        }
    }

    /** Method for writing peptide feat locs */
    private static void writePepFeatloc(String indent, Protein prot, Transcript trans, PrintWriter pw) {
        RangeI baseOrientedRange = new Range(-1, -1);
        baseOrientedRange.setStrand(trans.getStrand());
        if (prot.hasTranslationStart()) {
            baseOrientedRange.setStart(prot.getStart());
        } else {
            logger.warn("no translation start for " + prot.getName() + "--using start of " + trans.getName());
            baseOrientedRange.setStart(trans.getStart());
        }
        if (prot.hasTranslationEnd()) {
            baseOrientedRange.setEnd(prot.getEnd());
        } else {
            logger.warn("no translation end for " + prot.getName() + "--using end of " + trans.getName());
            baseOrientedRange.setEnd(trans.getEnd());
        }
        RangeI interbaseRange = convertToInterbase(baseOrientedRange);
        writeFeatureloc(indent, interbaseRange, isPartial(trans, "fmin"), isPartial(trans, "fmax"), prot, false, pw);
    }

    private static RangeI convertToInterbase(RangeI baseOriented) {
        RangeI interbase = baseOriented.getRangeClone();
        interbase.convertFromBaseOrientedToInterbase();
        return interbase;
    }

    private static void writeFeatureloc(String in, RangeI range, boolean minPart, boolean maxPart, SeqFeatureI feat, boolean isSub, PrintWriter pw) {
        writeFeatureloc(in, range.getLow(), range.getHigh(), range.getStrand(), minPart, maxPart, feat, isSub, pw);
    }

    /** thing can be a SeqFeatureI or a SequenceI.
    * If feature is a pair (e.g. a blast hit), it will have TWO featurelocs,
    * for subject and query.  
    * I think start & end are actually low/fmin & high/fmax here */
    private static void writeFeatureloc(String indent, int start, int end, int strand, boolean is_fmin_partial, boolean is_fmax_partial, SeqFeatureI feat, boolean isSubject, PrintWriter pw) {
        if (start == -1 && end == 0) {
            logger.debug("Not saving bogus featureloc for " + feat.getName());
            return;
        }
        pw.print(indent + "<featureloc>\n");
        pw.print(writeField(indent + TAB, "fmax", end + ""));
        pw.print(writeField(indent + TAB, "fmin", start + ""));
        pw.print(writeField(indent + TAB, "is_fmax_partial", is_fmax_partial ? "1" : "0"));
        pw.print(writeField(indent + TAB, "is_fmin_partial", is_fmin_partial ? "1" : "0"));
        pw.print(writeField(indent + TAB, "locgroup", "0"));
        if (isSubject) pw.print(writeField(indent + TAB, "rank", "1")); else pw.print(writeField(indent + TAB, "rank", "0"));
        if (feat.haveExplicitAlignment()) pw.print(writeField(indent + TAB, "residue_info", feat.getExplicitAlignment()));
        if (feat != null) writeSrcfeatureId(indent + TAB, feat, isSubject, pw);
        if (isSubject) {
            pw.print(writeField(indent + TAB, "strand", "1"));
        } else pw.print(writeField(indent + TAB, "strand", strand + ""));
        pw.print(indent + "</featureloc>\n");
    }

    /** isSubject -> hit seq(FeatPair?) */
    private static void writeSrcfeatureId(String indent, SeqFeatureI sf, boolean isSubject, PrintWriter pw) {
        pw.print(indent + "<srcfeature_id>\n");
        pw.print(indent + TAB + "<feature>\n");
        if (isResult(sf)) {
            if (isSubject) {
                pw.print(indent + TAB + TAB + "<is_analysis>1</is_analysis>\n");
                String name = sf.getProperty("ref_name");
                if (name.equals("")) name = sf.getName();
                pw.print(indent + TAB + TAB + "<name>" + name + "</name>\n");
                logger.debug("Printing result with name=" + name + ": " + sf);
            } else {
                writeXref(indent + TAB + TAB, database, chromosome, null, false, -1, pw);
            }
        }
        if (isSubject) {
            writeOrganism(indent + TAB + TAB, "Computational", "Result", pw);
        } else writeOrganismFromFeature(indent + TAB + TAB, sf, pw);
        if (isSubject) {
            SequenceI seq = sf.getRefSequence();
            if (seq != null) {
                logger.debug("Ref seq for subject " + sf.getName() + " is called " + seq.getName());
                pw.print(writeField(indent + TAB + TAB, "residues", seq.getResidues()));
            }
            writeTimes(indent + TAB + TAB, sf, pw);
        }
        if (isResult(sf)) {
            if (isSubject) writeCVtype(indent + TAB + TAB, typeIDOntology, typeForResult(sf), pw); else writeCVtype(indent + TAB + TAB, typeIDOntology, "chromosome_arm", pw);
            pw.print(indent + TAB + TAB + "<uniquename>" + idForResult(sf, true) + "</uniquename>\n");
            if (isSubject) {
                writeProperties(indent + TAB + TAB, sf, pw);
                logger.debug("property desc = " + sf.getProperty("description") + " for sf " + sf.getName());
                if (sf.getProperty("description").equals("")) {
                    SequenceI seq = sf.getRefSequence();
                    if (seq != null) {
                        logger.debug("seq desc = " + seq.getDescription() + " for seq " + seq.getName());
                        writeProperty(indent + TAB + TAB, "description", seq.getDescription(), 0, pw);
                    }
                }
            }
        } else {
            writeChromArmTypeId(indent + TAB + TAB, sf, pw);
        }
        pw.print(indent + TAB + "</feature>\n");
        pw.print(indent + "</srcfeature_id>\n");
    }

    private static boolean isResult(SeqFeatureI sf) {
        if (sf instanceof AnnotatedFeature) return false; else return true;
    }

    /** If we read the data from chadoxml, type should be stored as ref_type property.
   *  If that's missing, try to get it from parent. */
    private static String typeForResult(SeqFeatureI result) {
        if (!result.getProperty("ref_type").equals("")) return result.getProperty("ref_type"); else {
            String parent_type = "";
            if (result.getRefFeature() != null) parent_type = result.getRefFeature().getFeatureType();
            return parent_type;
        }
    }

    /** result arg could be a result span or a parent result (singleton or pair).
   *  If this is a span of a feature pair and we read the data from chadoxml, 
   *  the result ID should be stored as ref_id property.
   *  If that's missing (or boring), try the id or name. */
    private static String idForResult(SeqFeatureI result, boolean useRefId) {
        logger.debug("idForResult: result = " + result + "; ref_id = " + result.getProperty("ref_id"));
        if (useRefId && !result.getProperty("ref_id").equals("")) return result.getProperty("ref_id"); else if (result.getId() != null && !result.getId().equals("")) {
            if ((result.getId().startsWith("result_span") || result.getId().equals("")) && !(result.getName().equals(""))) return result.getName(); else return result.getId();
        } else return "";
    }

    /** is_fmax_partial is true if the property is set to 1 *or* if missing_stop_codon is true.
   *  is_fmin_partial is true if the property is set to 1 *or* if missing_start_codon is true.
   *  1/2006: Changed to look at transcript for missing start/stop codon, rather than
   *  using is_fmin/fmax_partial property, because user might have changed transcript
   *  so that it now is ok, but Apollo didn't change the ChadoXML-specific property
   *  is_fmin/fmax_partial.
   *  (The reason this method returns a boolean instead of a String is so we can combine
   *  calls with ||.) */
    private static boolean isPartial(Transcript trans, String which) {
        if (which.equals("fmin")) {
            if (trans.getStrand() == 1 && trans.isMissing5prime()) return true; else if (trans.getStrand() == -1 && trans.isMissing3prime()) return true;
        } else {
            if (trans.getStrand() == -1 && trans.isMissing5prime()) return true; else if (trans.getStrand() == 1 && trans.isMissing3prime()) return true;
        }
        return false;
    }

    private static boolean isPartial(Protein prot, String which) {
        if (which.equals("fmin")) {
            if (prot.getProperty("is_fmin_partial").equals("1")) return true; else if (prot.getProperty("is_fmax_partial").equals("1")) return true;
        }
        return false;
    }

    /** Exon rank was stored as a property on the exon */
    private static void writeExonRank(String indent, Exon exon, int exonRank, PrintWriter pw) {
        if (!(exon.getProperty("rank").equals(""))) pw.print(indent + "<rank>" + exon.getProperty("rank") + "</rank>\n"); else {
            pw.print(indent + "<rank>" + exonRank + "</rank>\n");
        }
    }

    /** Write type ID (including dbxref for NON-top-level annots) for chromosome arm */
    private static void writeChromArmTypeId(String indent, SeqFeatureI sf, PrintWriter pw) {
        pw.print(indent + "<type_id>\n");
        pw.print(indent + TAB + "<cvterm>\n");
        pw.print(indent + TAB + TAB + "<cv_id>\n");
        pw.print(indent + TAB + TAB + TAB + "<cv>\n");
        pw.print(indent + TAB + TAB + TAB + TAB + "<name>" + typeIDOntology + "</name>\n");
        pw.print(indent + TAB + TAB + TAB + "</cv>\n");
        pw.print(indent + TAB + TAB + "</cv_id>\n");
        if (sf.isAnnot()) {
            if (!(sf.isExon()) && !(sf.isTranscript()) && !(sf.isProtein()) && !(sf.getFeatureType().equalsIgnoreCase("mRNA"))) ; else writeXref(indent + TAB + TAB, typeIDOntology, IDForDbxref, null, false, -1, pw);
        } else {
            writeXref(indent + TAB + TAB, typeIDOntology, IDForDbxref, null, false, -1, pw);
        }
        pw.print(writeField(indent + TAB + TAB, "name", "chromosome_arm"));
        pw.print(indent + TAB + "</cvterm>\n");
        pw.print(indent + "</type_id>\n");
        pw.print(writeField(indent, "uniquename", chromosome));
    }

    private static void writeSynonyms(String indent, AnnotatedFeatureI feat, PrintWriter pw) {
        Vector syns = feat.getSynonyms();
        for (int i = 0; i < syns.size(); i++) {
            Synonym syn = (Synonym) syns.elementAt(i);
            writeSynonym(indent, feat, feat.getName(), syn, pw);
        }
    }

    private static void writeSynonym(String indent, AnnotatedFeatureI feat, String feature_name, Synonym syn, PrintWriter pw) {
        if (syn == null) return;
        pw.print(indent + "<feature_synonym>\n");
        syn.setName(XML_util.transformToPCData(syn.getName(), true));
        pw.print(writeField(indent + TAB, "is_current", getIsCurrent(syn, feature_name)));
        pw.print(writeField(indent + TAB, "is_internal", getIsInternal(syn)));
        String pub_id = syn.getOwner();
        if (syn == null || syn.equals("")) pub_id = syn.getProperty("author");
        if (syn.equals("")) {
            if (feat.getOwner() != null && !feat.getOwner().equals("") && !feat.getOwner().equals("null")) pub_id = feat.getOwner(); else pub_id = defaultSynonymAuthor;
        }
        String pubType = syn.getProperty("pub_type");
        if (pubType.equals("")) pubType = defaultSynonymPubType;
        writePubId(indent + TAB, pub_id, pubType, pw);
        pw.print(indent + TAB + "<synonym_id>\n");
        pw.print(indent + TAB + TAB + "<synonym>\n");
        pw.print(writeField(indent + TAB + TAB + TAB, "name", syn.getName()));
        if (syn.getProperty("synonym_sgml").equals("")) pw.print(writeField(indent + TAB + TAB + TAB, "synonym_sgml", syn.getName())); else pw.print(writeField(indent + TAB + TAB + TAB, "synonym_sgml", syn.getProperty("synonym_sgml")));
        writeCVtype(indent + TAB + TAB + TAB, "synonym type", "synonym", pw);
        pw.print(indent + TAB + TAB + "</synonym>\n");
        pw.print(indent + TAB + "</synonym_id>\n");
        pw.print(indent + "</feature_synonym>\n");
    }

    /** Write a pub_id, filling in pubType and id */
    private static void writePubId(String indent, String id, String pubType, PrintWriter pw) {
        pw.print(indent + "<pub_id>\n");
        pw.print(indent + TAB + "<pub>\n");
        writeCVtype(indent + TAB + TAB, "pub type", pubType, pw);
        pw.print(indent + TAB + TAB + "<uniquename>" + id + "</uniquename>\n");
        pw.print(indent + TAB + "</pub>\n");
        pw.print(indent + "</pub_id>\n");
    }

    private static String getIsCurrent(Synonym syn, String feature_name) {
        String value;
        value = syn.getProperty("is_current");
        if (value != null && !(value.equals(""))) return value;
        return (feature_name.equals(syn.getName())) ? "1" : "0";
    }

    private static String getIsInternal(Synonym syn) {
        String value;
        value = syn.getProperty("is_internal");
        if (value != null && !(value.equals(""))) return value;
        return "0";
    }

    /** Writes results directly to buffered print writer, which is much faster than
   *  writing to a temporary string buffer and passing back a string */
    private static void writeResults(CurationSet curation, String indent, PrintWriter pw) {
        StrandedFeatureSetI analyses = curation.getResults();
        if (analyses == null) {
            logger.debug("No results to save.");
            return;
        }
        logger.debug("Saving " + analyses.size() + " types of results");
        if (analyses.size() > 0) {
            Vector genomic_xrefs = curation.getRefSequence().getDbXrefs();
            if (genomic_xrefs != null && genomic_xrefs.size() > 0) {
                DbXref xref = (DbXref) genomic_xrefs.firstElement();
                database = xref.getDbName();
                logger.debug("Set genomic database to " + database + " based on xref for curation's ref seq");
            } else logger.info("No dbxref for genomic sequence--using default database definition " + database);
            for (int i = 0; i < analyses.size(); i++) {
                FeatureSetI analysis = (FeatureSetI) analyses.getFeatureAt(i);
                if (analysis.getFeatureType() != null && !analysis.getFeatureType().equals("codons") && !analysis.getFeatureType().equals("Gene")) {
                    writeAnalysisResults(analysis, indent, pw);
                }
            }
        }
    }

    private static String getProgramName(SeqFeatureI sf) {
        String program = sf.getProgramName();
        if (program == null || program.length() == 0 || program.equals(RangeI.NO_TYPE)) program = sf.getProperty("type");
        if (program.length() == 0 || program.equals(RangeI.NO_TYPE)) program = sf.getTopLevelType();
        return program;
    }

    /** Write out all the results of this particular analysis type */
    private static void writeAnalysisResults(FeatureSetI analysis, String indent, PrintWriter pw) {
        String program = getProgramName(analysis);
        Vector results = analysis.getFeatures();
        if (logger.isDebugEnabled()) {
            logger.debug("Writing " + (results.size() + 1) + " results (program = " + program + ", db = " + database + ")");
        }
        for (int i = 0; i < results.size(); i++) {
            if (results.elementAt(i) instanceof FeatureSetI) {
                FeatureSetI result = (FeatureSetI) results.elementAt(i);
                writeResult(result, indent, pw);
            } else {
                logger.error("writeAnalysisResults: don't know what to do to save non-FeatureSet class " + results.elementAt(i).getClass().getName());
            }
        }
    }

    /** Note that result spans can be FeaturePairs or just SeqFeatures */
    private static void writeResult(FeatureSetI result, String indent, PrintWriter pw) {
        pw.print(indent + "<feature>\n");
        pw.print(writeField(indent + TAB, "is_analysis", "1"));
        if (!(result.getProperty("is_obsolete")).equals("")) pw.print(writeField(indent + TAB + TAB + TAB, "is_obsolete", result.getProperty("is_obsolete")));
        pw.print(writeField(indent + TAB, "name", XML_util.transformToPCData(result.getName())));
        writeFields(indent + TAB + TAB + TAB, result, pw);
        writeOrganism(indent + TAB, "Computational", "Result", pw);
        pw.print(writeField(indent + TAB, "seqlen", "0"));
        writeTimes(indent + TAB, result, pw);
        writeCVtype(indent + TAB, typeIDOntology, CVTermForResults, pw);
        pw.print(writeField(indent + TAB, "uniquename", XML_util.transformToPCData(result.getId())));
        writeAnalysisFeature(indent + TAB, result, pw);
        Vector spans = result.getFeatures();
        for (int i = 0; i < spans.size(); i++) {
            SeqFeatureI span = (SeqFeatureI) spans.elementAt(i);
            writeSpan(indent + TAB, span, pw);
        }
        pw.print(indent + "</feature>\n");
    }

    /** Write a result span */
    private static void writeSpan(String indent, SeqFeatureI span, PrintWriter pw) {
        pw.print(indent + "<feature_relationship>\n");
        pw.print(indent + TAB + "<subject_id>\n");
        pw.print(indent + TAB + TAB + "<feature>\n");
        writeOrganism(indent + TAB + TAB + TAB, "Computational", "Result", pw);
        writeCVtype(indent + TAB + TAB + TAB, typeIDOntology, "match", pw);
        pw.print(writeField(indent + TAB + TAB + TAB, "uniquename", XML_util.transformToPCData(idForResult(span, false))));
        writeAnalysisFeature(indent + TAB + TAB + TAB, span, pw);
        writeFeatureloc(indent + TAB + TAB + TAB, span, pw);
        pw.print(indent + TAB + TAB + "</feature>\n");
        pw.print(indent + TAB + "</subject_id>\n");
        writeCVtype(indent + TAB, "relationship type", "partof", pw);
        pw.print(indent + "</feature_relationship>\n");
    }

    private static void writeOrganismFromFeature(String indent, SeqFeatureI feat, PrintWriter pw) {
        writeOrganismFromFeature(indent, feat, null, pw);
    }

    /** In SeqFeatures, organism is stored as a property.
   *  If feature doesn't have organism, try to figure it out. */
    private static void writeOrganismFromFeature(String indent, SeqFeatureI feat, CurationSet curation, PrintWriter pw) {
        String organism = getOrganismFromFeature(feat, curation);
        if (organism != null) {
            String genus = organism;
            String species = "";
            if (organism.indexOf(" ") > 0) {
                genus = organism.substring(0, organism.indexOf(" "));
                species = organism.substring(organism.indexOf(" ") + 1);
            }
            writeOrganism(indent, genus, species, pw);
        } else {
            logger.warn("no organism for " + feat.getName() + " (" + feat.getId() + ")");
            organism = Config.getStyle().getParameter("organism");
            if (organism != null && !(organism.equals(""))) {
                logger.info("Using default organism for this style: " + organism);
                String genus = organism;
                String species = "";
                if (organism.indexOf(" ") > 0) {
                    genus = organism.substring(0, organism.indexOf(" "));
                    species = organism.substring(organism.indexOf(" ") + 1);
                }
                writeOrganism(indent, genus, species, pw);
            }
        }
    }

    /** Try to get organism from feat; failing that, look at its parent, refseq, etc. */
    private static String getOrganismFromFeature(SeqFeatureI feat, CurationSet curation) {
        String organism = feat.getProperty("organism");
        if (organism != null && !(organism.equals(""))) return organism;
        SeqFeatureI parent = feat.getRefFeature();
        if (parent != null) {
            organism = parent.getProperty("organism");
            if (organism != null && !(organism.equals(""))) {
                if (organism.indexOf(" ") > 0) {
                    logger.debug("organism for " + feat.getName() + " is " + organism);
                    String genus = organism.substring(0, organism.indexOf(" "));
                    String species = organism.substring(organism.indexOf(" ") + 1);
                    feat.addProperty("organism", genus + " " + species);
                }
                return organism;
            }
        }
        if (feat.isAnnot()) {
            SequenceI seq = feat.getRefSequence();
            organism = seq.getOrganism();
            if (organism != null && !(organism.equals(""))) {
                if (organism.indexOf(" ") > 0) {
                    logger.debug("organism for " + feat.getName() + " is " + organism);
                    String genus = organism.substring(0, organism.indexOf(" "));
                    String species = organism.substring(organism.indexOf(" ") + 1);
                    feat.addProperty("organism", genus + " " + species);
                }
                return organism;
            }
        }
        if (parent != null) {
            SequenceI seq = parent.getRefSequence();
            organism = seq.getOrganism();
            if (organism != null && !(organism.equals(""))) {
                if (organism.indexOf(" ") > 0) {
                    String genus = organism.substring(0, organism.indexOf(" "));
                    String species = organism.substring(organism.indexOf(" ") + 1);
                    feat.addProperty("organism", genus + " " + species);
                    parent.addProperty("organism", genus + " " + species);
                }
                return organism;
            }
        }
        if (curation != null && feat.isAnnot()) {
            organism = curation.getOrganism();
            if (organism != null && !(organism.equals(""))) {
                if (organism.indexOf(" ") > 0) {
                    String genus = organism.substring(0, organism.indexOf(" "));
                    String species = organism.substring(organism.indexOf(" ") + 1);
                    feat.addProperty("organism", genus + " " + species);
                    if (parent != null) parent.addProperty("organism", genus + " " + species);
                }
                return organism;
            }
        }
        return null;
    }

    private static void writeOrganism(String indent, String genus, String species, PrintWriter pw) {
        pw.print(indent + "<organism_id>\n");
        pw.print(indent + TAB + "<organism>\n");
        pw.print(indent + TAB + TAB + "<genus>" + genus + "</genus>\n");
        pw.print(indent + TAB + TAB + "<species>" + species + "</species>\n");
        pw.print(indent + TAB + "</organism>\n");
        pw.print(indent + "</organism_id>\n");
    }

    private static void writeCVtype(String indent, String type, String term, PrintWriter pw) {
        if (term == null || term.equals("")) return;
        pw.print(indent + "<type_id>\n");
        pw.print(indent + TAB + "<cvterm>\n");
        pw.print(indent + TAB + TAB + "<cv_id>\n");
        pw.print(indent + TAB + TAB + TAB + "<cv>\n");
        pw.print(indent + TAB + TAB + TAB + TAB + "<name>" + convertToChado(type) + "</name>\n");
        pw.print(indent + TAB + TAB + TAB + "</cv>\n");
        pw.print(indent + TAB + TAB + "</cv_id>\n");
        pw.print(indent + TAB + TAB + "<name>" + term + "</name>\n");
        pw.print(indent + TAB + "</cvterm>\n");
        pw.print(indent + "</type_id>\n");
    }

    /** There are certain type names in GAME that are different in Chado, so in
   *  case the data came in from GAME, we may need to convert the type.
   *   from:                                   to:
   *    cdna                                    cDNA
   *    transcript                              mRNA
   *    pseudotranscript                        mRNA
   *    transposon                              transposable_element
   *    aa                                      protein
   *    misc. non-coding RNA                    ncRNA
   *    microRNA                                nuclear_micro_RNA_coding_gene
   *    miscellaneous curator's observation     remark
  */
    private static String convertToChado(String type) {
        if (type.equalsIgnoreCase("cdna")) return "cDNA"; else if (type.equalsIgnoreCase("transcript")) return "mRNA"; else if (type.equalsIgnoreCase("pseudotranscript")) return "mRNA"; else if (type.equalsIgnoreCase("transposon")) return "transposable_element"; else if (type.equalsIgnoreCase("aa")) return "protein"; else if (type.equalsIgnoreCase("misc. non-coding RNA")) return "ncRNA"; else if (type.equalsIgnoreCase("microRNA")) return "nuclear_micro_RNA_coding_gene"; else if (type.equalsIgnoreCase("miscellaneous curator's observation")) return "remark"; else return type;
    }

    /** Note that Apollo's datamodels (which mostly match GAME XML) encode pseudogenes (etc.)
      as annot type = pseudogene, whereas in Chado, pseudogenes are encoded as annot type = gene,
      transcript type = pseudogene.  So we need to adjust for this when writing. */
    private static String annotTypeForChado(AnnotatedFeatureI annot) {
        String type = annot.getTopLevelType();
        if (type.equalsIgnoreCase("pseudogene") || type.equalsIgnoreCase("tRNA") || type.equalsIgnoreCase("snoRNA") || type.equalsIgnoreCase("ncRNA") || type.equalsIgnoreCase("rRNA") || type.equalsIgnoreCase("miRNA") || type.equalsIgnoreCase("snRNA")) {
            return "gene";
        } else return type;
    }

    /** If transcript's bioType is gene, we print "mRNA"--otherwise, we use the bioType. */
    private static String transcriptTypeForChado(Transcript transcript) {
        String type = transcript.getTopLevelType();
        if (type.equalsIgnoreCase("gene")) type = "mRNA";
        return type;
    }

    private static void writeAnalysisFeature(String indent, SeqFeatureI sf, PrintWriter pw) {
        double score = sf.getScore();
        pw.print(indent + "<analysisfeature>\n");
        pw.print(indent + TAB + "<analysis_id>\n");
        pw.print(indent + TAB + TAB + "<analysis>\n");
        if (sf.getProgramName() == null || sf.getProgramName().equals("")) sf = sf.getRefFeature();
        String program = getProgramName(sf);
        pw.print(writeField(indent + TAB + TAB + TAB, "program", program));
        pw.print(writeField(indent + TAB + TAB + TAB, "programversion", sf.getProperty("programversion")));
        pw.print(writeField(indent + TAB + TAB + TAB, "sourcename", sf.getDatabase()));
        pw.print(writeField(indent + TAB + TAB + TAB, "sourceversion", sf.getProperty("sourceversion")));
        pw.print(writeField(indent + TAB + TAB + TAB, "timeexecuted", sf.getProperty("timeexecuted")));
        pw.print(indent + TAB + TAB + "</analysis>\n");
        pw.print(indent + TAB + "</analysis_id>\n");
        pw.print(writeField(indent + TAB, "rawscore", score + ""));
        pw.print(indent + "</analysisfeature>\n");
    }

    /** Write timeaccessioned and timelastmodified (which were stored as properties on a SeqFeature). */
    private static void writeTimes(String indent, SeqFeatureI sf, PrintWriter pw) {
        pw.print(writeField(indent, "timeaccessioned", sf.getProperty("timeaccessioned")));
        pw.print(writeField(indent, "timelastmodified", sf.getProperty("timelastmodified")));
    }

    /** Writes all properties for this feature or sequence (except for those that are
   *  explicitly written out elsewhere). */
    private static void writeProperties(String indent, SeqFeatureI feat, PrintWriter pw) {
        Hashtable props = feat.getPropertiesMulti();
        Enumeration e = props.keys();
        while (e.hasMoreElements()) {
            String type = (String) e.nextElement();
            if (!isSpecialProperty(type) && !type.equals("unstranded")) {
                Vector values = feat.getPropertyMulti(type);
                if (values == null) continue;
                for (int i = 0; i < values.size(); i++) {
                    String value = (String) values.elementAt(i);
                    if (type.equals("dicistronic") && value.equals("false")) continue;
                    if (!value.startsWith(ChadoXmlAdapter.FIELD_LABEL)) writeProperty(indent, type, value, i, pw);
                }
            }
        }
    }

    /** Writes out flat fields for this feature or sequence (except for those that are
   *  explicitly written out elsewhere).
   *  Unfamiliar fields in the input were stored as properties, but with
   *  the FIELD_LABEL prepended to the value (e.g. color=field:blue).
  */
    private static void writeFields(String indent, SeqFeatureI feat, PrintWriter pw) {
        Hashtable props = feat.getPropertiesMulti();
        Enumeration e = props.keys();
        while (e.hasMoreElements()) {
            String type = (String) e.nextElement();
            if (!isSpecialProperty(type)) {
                Vector values = feat.getPropertyMulti(type);
                if (values == null) continue;
                for (int i = 0; i < values.size(); i++) {
                    String value = (String) values.elementAt(i);
                    if (value.startsWith(ChadoXmlAdapter.FIELD_LABEL)) pw.print(writeField(indent, type, value));
                }
            }
        }
    }

    private static void writeWeirdTranscriptProperties(String indent, Transcript trans, PrintWriter pw) {
        if (trans.readThroughStopResidue() != null) {
            if (trans.readThroughStopResidue().equals("U")) writeProperty(indent, "stop_codon_redefinition_as_selenocysteine", "true", 0, pw); else if (trans.readThroughStopResidue().equals("X")) writeProperty(indent, "stop_codon_readthrough", "true", 0, pw); else writeProperty(indent, "stop_codon_readthrough", trans.readThroughStopResidue(), 0, pw);
        }
        if (trans.plus1FrameShiftPosition() > 0) writeProperty(indent, "plus_1_translational_frameshift", trans.plus1FrameShiftPosition() + "", 0, pw);
        if (trans.minus1FrameShiftPosition() > 0) writeProperty(indent, "minus_1_translational_frameshift", trans.minus1FrameShiftPosition() + "", 0, pw);
        if (trans.unConventionalStart()) writeProperty(indent, "non_canonical_start_codon", trans.getStartCodon(), 0, pw);
        if (trans.isMissing5prime()) writeProperty(indent, "missing_start_codon", "true", 0, pw);
        if (trans.isMissing3prime()) writeProperty(indent, "missing_stop_codon", "true", 0, pw);
        if ((trans.getNonConsensusAcceptorNum() >= 0 || trans.getNonConsensusDonorNum() >= 0)) {
            if (trans.getProperty("non_canonical_splice_site").equals("")) writeProperty(indent, "non_canonical_splice_site", (trans.nonConsensusSplicingOkay() ? "approved" : "unapproved"), 0, pw);
        }
        if (trans.isProblematic()) writeProperty(indent, "problem", "true", 0, pw);
    }

    /** Identify "special" properties that are explicitly written out elsewhere
   *  so we don't also write them out as generic properties. */
    public static boolean isSpecialProperty(String prop) {
        if (prop.equalsIgnoreCase("timeaccessioned")) return true;
        if (prop.equalsIgnoreCase("timelastmodified")) return true;
        if (prop.equalsIgnoreCase("seqlen")) return true;
        if (prop.equalsIgnoreCase("is_analysis")) return true;
        if (prop.equalsIgnoreCase("is_obsolete")) return true;
        if (prop.equalsIgnoreCase("uniquename")) return true;
        if (prop.equalsIgnoreCase("name")) return true;
        if (prop.equalsIgnoreCase("organism")) return true;
        if (prop.equalsIgnoreCase("rank")) return true;
        if (prop.equalsIgnoreCase("is_fmin_partial")) return true;
        if (prop.equalsIgnoreCase("is_fmax_partial")) return true;
        if (prop.equalsIgnoreCase("ref_name")) return true;
        if (prop.equalsIgnoreCase("ref_id")) return true;
        if (prop.equalsIgnoreCase("ref_type")) return true;
        if (prop.equalsIgnoreCase("type_id")) return true;
        if (prop.equalsIgnoreCase("programversion")) return true;
        if (prop.equalsIgnoreCase("sourceversion")) return true;
        if (prop.equalsIgnoreCase("timeexecuted")) return true;
        if (prop.equalsIgnoreCase("internal_synonym")) return true;
        if (prop.equalsIgnoreCase("readthrough_stop_codon")) return true;
        return false;
    }

    private static void writeProperty(String indent, String prop, String value, int rank, PrintWriter pw) {
        if (value == null || value.equals("")) return;
        if (prop.equals("tag")) prop = "problem";
        pw.print(indent + "<featureprop>\n");
        pw.print(indent + TAB + "<rank>" + rank + "</rank>\n");
        writeCVtype(indent + TAB, featurepropCV, prop, pw);
        value = XML_util.transformToPCData(value, true);
        pw.print(indent + TAB + "<value>" + value + "</value>\n");
        pw.print(indent + "</featureprop>\n");
    }

    /** Comment rank, as for other featureprops, is generated as comments are written out,
   *  not roundtripped. */
    private static void writeComments(String indent, AnnotatedFeatureI feat, PrintWriter pw) {
        Vector comments = feat.getComments();
        for (int i = 0; i < comments.size(); i++) {
            Comment comment = (Comment) comments.elementAt(i);
            pw.print(indent + "<featureprop>\n");
            pw.print(indent + TAB + "<rank>" + i + "</rank>\n");
            writeCVtype(indent + TAB, featurepropCV, "comment", pw);
            String commentText = XML_util.transformToPCData(comment.getText(), true);
            pw.print(indent + TAB + "<value>" + commentText + "</value>\n");
            writeFeaturepropPub(indent + TAB, comment.getPerson(), pw);
            pw.print(indent + "</featureprop>\n");
        }
    }

    private static void writeFeaturepropPub(String indent, String id, PrintWriter pw) {
        if (id == null) return;
        pw.print(indent + "<featureprop_pub>\n");
        pw.print(indent + TAB + "<pub_id>\n");
        pw.print(indent + TAB + TAB + "<pub>\n");
        writeCVtype(indent + TAB + TAB + TAB, "pub type", defaultSynonymPubType, pw);
        pw.print(indent + TAB + TAB + TAB + "<uniquename>" + id + "</uniquename>\n");
        pw.print(indent + TAB + TAB + "</pub>\n");
        pw.print(indent + TAB + "</pub_id>\n");
        pw.print(indent + "</featureprop_pub>\n");
    }

    /** Writes a field on one or more lines:
   *  <name>value</name> or
   *  <name>
   *    value
   *  </name> */
    private static String writeField(String startingIndent, String name, String value, boolean separateLines) {
        if (name == null || name.equals("")) return "";
        if (value == null || value.equals("") || value.equals(ChadoXmlAdapter.FIELD_LABEL)) return "";
        String field = startingIndent + "<" + name + ">";
        if (separateLines) field += "\n" + startingIndent + TAB;
        if (value.startsWith(ChadoXmlAdapter.FIELD_LABEL)) value = value.substring((ChadoXmlAdapter.FIELD_LABEL).length());
        field += value;
        if (separateLines) field += "\n" + startingIndent;
        field += "</" + name + ">" + "\n";
        return field;
    }

    private static String writeField(String startingIndent, String name, String value) {
        return writeField(startingIndent, name, value, false);
    }

    private static String writeEnd() {
        return "</chado>\n";
    }

    /** The transactions are saved in a separate file.  The transaction
   *  filename is actually generated by the ChadoTransactionXMLWriter
   *  from the mainFileName. */
    protected static void saveTransactions(CurationSet curation, String mainFileName) {
        if (curation.getTransactionManager() == null) {
            return;
        }
        try {
            curation.getTransactionManager().coalesce();
            if (curation.getTransactionManager().getTransactions().size() == 0) {
                logger.info("No transactions to save");
                return;
            }
            if (Config.outputTransactionXML()) {
                saveTransactionsInGAME(curation, mainFileName);
            }
            if (Config.isChadoTnOutputNeeded()) {
                TransactionOutputAdapter output = new ChadoTransactionXMLWriter();
                if (curation.isChromosomeArmUsed()) {
                    output.setMapID(curation.getChromosome());
                    output.setMapType("chromosome_arm");
                } else {
                    output.setMapID(curation.getChromosome());
                    output.setMapType("chromosome");
                }
                output.setTransformer(new ChadoTransactionTransformer());
                output.setTarget(mainFileName);
                output.commitTransactions(curation.getTransactionManager());
                logger.debug("Saved transactions to " + output.getTarget());
            }
        } catch (Exception e) {
            logger.error("ChadoXmlWrite.saveTransactions(): " + e, e);
            JOptionPane.showMessageDialog(null, "Transactions cannot be saved.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Borrowed from GAMEAdapter */
    private static void saveTransactionsInGAME(CurationSet curationSet, String fileName) {
        try {
            TransactionXMLAdapter tnAdapter = new TransactionXMLAdapter();
            tnAdapter.setFileName(fileName);
            tnAdapter.save(curationSet.getTransactionManager().getTransactions());
        } catch (IOException e) {
            logger.error("saveTransactionsInGAME(): " + e, e);
        }
    }
}
