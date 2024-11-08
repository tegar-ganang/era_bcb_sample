package org.obolibrary.oboformat.writer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.Frame.FrameType;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.model.QualifierValue;
import org.obolibrary.oboformat.model.Xref;
import org.obolibrary.oboformat.parser.OBOFormatConstants;
import org.obolibrary.oboformat.parser.OBOFormatConstants.OboFormatTag;
import org.obolibrary.oboformat.parser.OBOFormatParser;

/**
 * 
 * @author Shahid Manzoor
 *
 */
public class OBOFormatWriter {

    private static Logger LOG = Logger.getLogger(OBOFormatWriter.class);

    private static HashSet<String> tagsInformative = buildTagsInformative();

    private boolean isCheckStructure = true;

    public boolean isCheckStructure() {
        return isCheckStructure;
    }

    public void setCheckStructure(boolean isCheckStructure) {
        this.isCheckStructure = isCheckStructure;
    }

    private static HashSet<String> buildTagsInformative() {
        HashSet<String> set = new HashSet<String>();
        set.add(OboFormatTag.TAG_IS_A.getTag());
        set.add(OboFormatTag.TAG_RELATIONSHIP.getTag());
        set.add(OboFormatTag.TAG_DISJOINT_FROM.getTag());
        set.add(OboFormatTag.TAG_INTERSECTION_OF.getTag());
        set.add(OboFormatTag.TAG_UNION_OF.getTag());
        set.add(OboFormatTag.TAG_EQUIVALENT_TO.getTag());
        set.add(OboFormatTag.TAG_PROPERTY_VALUE.getTag());
        set.add(OboFormatTag.TAG_DOMAIN.getTag());
        set.add(OboFormatTag.TAG_RANGE.getTag());
        set.add(OboFormatTag.TAG_INVERSE_OF.getTag());
        set.add(OboFormatTag.TAG_TRANSITIVE_OVER.getTag());
        set.add(OboFormatTag.TAG_HOLDS_OVER_CHAIN.getTag());
        set.add(OboFormatTag.TAG_EQUIVALENT_TO_CHAIN.getTag());
        set.add(OboFormatTag.TAG_DISJOINT_OVER.getTag());
        return set;
    }

    public void write(String fn, BufferedWriter writer) throws IOException {
        if (fn.startsWith("http:")) {
            write(new URL(fn), writer);
        } else {
            BufferedReader reader = new BufferedReader(new FileReader(new File(fn)));
            write(reader, writer);
        }
    }

    public void write(URL url, BufferedWriter writer) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        write(reader, writer);
    }

    public void write(BufferedReader reader, BufferedWriter writer) throws IOException {
        OBOFormatParser parser = new OBOFormatParser();
        OBODoc doc = parser.parse(reader);
        write(doc, writer);
    }

    public void write(OBODoc doc, String outFile) throws IOException {
        FileOutputStream os = new FileOutputStream(new File(outFile));
        OutputStreamWriter osw = new OutputStreamWriter(os, OBOFormatConstants.DEFAULT_CHARACTER_ENCODING);
        BufferedWriter bw = new BufferedWriter(osw);
        write(doc, bw);
        bw.close();
    }

    public void write(OBODoc doc, BufferedWriter writer) throws IOException {
        NameProvider nameProvider = new OBODocNameProvider(doc);
        write(doc, writer, nameProvider);
    }

    public void write(OBODoc doc, BufferedWriter writer, NameProvider nameProvider) throws IOException {
        if (isCheckStructure) {
            doc.check();
        }
        Frame headerFrame = doc.getHeaderFrame();
        writeHeader(headerFrame, writer, nameProvider);
        List<Frame> termFrames = new ArrayList<Frame>();
        termFrames.addAll(doc.getTermFrames());
        Collections.sort(termFrames, FramesComparator.instance);
        List<Frame> typeDefFrames = new ArrayList<Frame>();
        typeDefFrames.addAll(doc.getTypedefFrames());
        Collections.sort(typeDefFrames, FramesComparator.instance);
        List<Frame> instanceFrames = new ArrayList<Frame>();
        typeDefFrames.addAll(doc.getInstanceFrames());
        Collections.sort(instanceFrames, FramesComparator.instance);
        for (Frame f : termFrames) {
            write(f, writer, nameProvider);
        }
        for (Frame f : typeDefFrames) {
            write(f, writer, nameProvider);
        }
        for (Frame f : instanceFrames) {
            write(f, writer, nameProvider);
        }
    }

    private void writeLine(StringBuilder ln, BufferedWriter writer) throws IOException {
        ln.append('\n');
        writer.write(ln.toString());
    }

    private void writeLine(String ln, BufferedWriter writer) throws IOException {
        writer.write(ln + "\n");
    }

    private void writeEmptyLine(BufferedWriter writer) throws IOException {
        writer.write("\n");
    }

    private List<String> duplicateTags(Set<String> src) {
        List<String> tags = new ArrayList<String>(src.size());
        for (String tag : src) {
            tags.add(tag);
        }
        return tags;
    }

    public void writeHeader(Frame frame, BufferedWriter writer, NameProvider nameProvider) throws IOException {
        List<String> tags = duplicateTags(frame.getTags());
        Collections.sort(tags, HeaderTagsComparator.instance);
        write(new Clause(OboFormatTag.TAG_FORMAT_VERSION.getTag(), "1.2"), writer, nameProvider);
        for (String tag : tags) {
            if (tag.equals(OboFormatTag.TAG_FORMAT_VERSION.getTag())) continue;
            List<Clause> clauses = new ArrayList<Clause>(frame.getClauses(tag));
            Collections.sort(clauses, ClauseComparator.instance);
            for (Clause clause : clauses) {
                if (tag.equals(OboFormatTag.TAG_SUBSETDEF.getTag())) {
                    writeSynonymtypedef(clause, writer);
                } else if (tag.equals(OboFormatTag.TAG_SYNONYMTYPEDEF.getTag())) {
                    writeSynonymtypedef(clause, writer);
                } else if (tag.equals(OboFormatTag.TAG_DATE.getTag())) {
                    writeHeaderDate(clause, writer);
                } else if (tag.equals(OboFormatTag.TAG_PROPERTY_VALUE.getTag())) {
                    writePropertyValue(clause, writer);
                } else write(clause, writer, nameProvider);
            }
        }
        writeEmptyLine(writer);
    }

    public void write(Frame frame, BufferedWriter writer, NameProvider nameProvider) throws IOException {
        Comparator<String> comparator = null;
        if (frame.getType() == FrameType.TERM) {
            writeLine("[Term]", writer);
            comparator = TermsTagsComparator.instance;
        } else if (frame.getType() == FrameType.TYPEDEF) {
            writeLine("[Typedef]", writer);
            comparator = TypeDefTagsComparator.instance;
        } else if (frame.getType() == FrameType.INSTANCE) {
            writeLine("[Instance]", writer);
            comparator = TypeDefTagsComparator.instance;
        }
        if (frame.getId() != null) {
            Object label = frame.getTagValue(OboFormatTag.TAG_NAME);
            String extra = "";
            if (label == null && nameProvider != null) {
                label = nameProvider.getName(frame.getId());
                if (label != null) {
                    extra = " ! " + label;
                }
            }
            writeLine(OboFormatTag.TAG_ID.getTag() + ": " + frame.getId() + extra, writer);
        }
        List<String> tags = duplicateTags(frame.getTags());
        Collections.sort(tags, comparator);
        String defaultOboNamespace = null;
        if (nameProvider != null) {
            defaultOboNamespace = nameProvider.getDefaultOboNamespace();
        }
        for (String tag : tags) {
            List<Clause> clauses = new ArrayList<Clause>(frame.getClauses(tag));
            Collections.sort(clauses, ClauseComparator.instance);
            for (Clause clause : clauses) {
                final String clauseTag = clause.getTag();
                if (OboFormatTag.TAG_ID.getTag().equals(clauseTag)) continue; else if (OboFormatTag.TAG_DEF.getTag().equals(clauseTag)) writeDef(clause, writer); else if (OboFormatTag.TAG_SYNONYM.getTag().equals(clauseTag)) writeSynonym(clause, writer); else if (OboFormatTag.TAG_PROPERTY_VALUE.getTag().equals(clauseTag)) writePropertyValue(clause, writer); else if (OboFormatTag.TAG_EXPAND_EXPRESSION_TO.getTag().equals(clauseTag) || OboFormatTag.TAG_EXPAND_ASSERTION_TO.getTag().equals(clauseTag)) writeClauseWithQuotedString(clause, writer); else if (OboFormatTag.TAG_XREF.getTag().equals(clauseTag)) {
                    writeXRefClause(clause, writer);
                } else if (OboFormatTag.TAG_NAMESPACE.getTag().equals(clauseTag)) {
                    if (defaultOboNamespace == null || clause.getValue().equals(defaultOboNamespace) == false) {
                        write(clause, writer, nameProvider);
                    }
                } else write(clause, writer, nameProvider);
            }
        }
        writeEmptyLine(writer);
    }

    private void writeXRefClause(Clause clause, BufferedWriter writer) throws IOException {
        Xref xref = clause.getValue(Xref.class);
        if (xref != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(clause.getTag());
            sb.append(": ");
            if (xref.getIdref() != null) {
                sb.append(escapeOboString(xref.getIdref(), EscapeMode.xref));
                String annotation = xref.getAnnotation();
                if (annotation != null) {
                    sb.append(" \"");
                    sb.append(escapeOboString(annotation, EscapeMode.quotes));
                    sb.append('"');
                }
            }
            appendQualifiers(sb, clause);
            writeLine(sb, writer);
        }
    }

    private void writeSynonymtypedef(Clause clause, BufferedWriter writer) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(clause.getTag());
        sb.append(": ");
        Iterator<Object> valuesIterator = clause.getValues().iterator();
        Collection<?> values = clause.getValues();
        for (int i = 0; i < values.size(); i++) {
            String value = valuesIterator.next().toString();
            if (i == 1) {
                sb.append('"');
            }
            sb.append(escapeOboString(value, EscapeMode.quotes));
            if (i == 1) {
                sb.append('"');
            }
            if (valuesIterator.hasNext()) {
                sb.append(' ');
            }
        }
        writeLine(sb, writer);
    }

    private void writeHeaderDate(Clause clause, BufferedWriter writer) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(clause.getTag());
        sb.append(": ");
        Object value = clause.getValue();
        if (value instanceof Date) {
            sb.append(OBOFormatConstants.headerDateFormat.get().format((Date) value));
        } else if (value instanceof String) {
            sb.append(value);
        } else {
            LOG.warn("Unknown datatype ('" + value.getClass().getName() + "') for value in clause: " + clause);
            sb.append(value.toString());
        }
        writeLine(sb, writer);
    }

    private void writeClauseWithQuotedString(Clause clause, BufferedWriter writer) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(clause.getTag());
        sb.append(": ");
        boolean first = true;
        Iterator<Object> valuesIterator = clause.getValues().iterator();
        while (valuesIterator.hasNext()) {
            if (first) {
                sb.append('"');
            }
            String value = valuesIterator.next().toString();
            sb.append(escapeOboString(value, EscapeMode.quotes));
            if (first) {
                sb.append('"');
            }
            if (valuesIterator.hasNext()) {
                sb.append(' ');
            }
            first = false;
        }
        Collection<Xref> xrefs = clause.getXrefs();
        if (xrefs != null) {
            appendXrefs(sb, xrefs);
        } else if (OboFormatTag.TAG_DEF.getTag().equals(clause.getTag()) || OboFormatTag.TAG_EXPAND_EXPRESSION_TO.getTag().equals(clause.getTag()) || OboFormatTag.TAG_EXPAND_ASSERTION_TO.getTag().equals(clause.getTag())) {
            sb.append(" []");
        }
        writeLine(sb, writer);
    }

    private void appendXrefs(StringBuilder sb, Collection<Xref> xrefs) {
        List<Xref> sortedXrefs = new ArrayList<Xref>(xrefs);
        Collections.sort(sortedXrefs, XrefComparator.instance);
        sb.append(" [");
        Iterator<Xref> xrefsIterator = sortedXrefs.iterator();
        while (xrefsIterator.hasNext()) {
            sb.append(escapeOboString(xrefsIterator.next().getIdref(), EscapeMode.xrefList));
            if (xrefsIterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("]");
    }

    public void writeDef(Clause clause, BufferedWriter writer) throws IOException {
        writeClauseWithQuotedString(clause, writer);
    }

    public void writePropertyValue(Clause clause, BufferedWriter writer) throws IOException {
        Collection<?> cols = clause.getValues();
        if (cols.size() < 2) {
            LOG.warn("The " + OboFormatTag.TAG_PROPERTY_VALUE.getTag() + " has incorrect number of values: " + clause);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(clause.getTag());
        sb.append(": ");
        for (Iterator<?> it = cols.iterator(); it.hasNext(); ) {
            String val = it.next().toString();
            if (val.contains(" ") || !val.contains(":")) {
                sb.append('"');
                sb.append(escapeOboString(val, EscapeMode.quotes));
                sb.append('"');
            } else {
                sb.append(escapeOboString(val, EscapeMode.simple));
            }
            if (it.hasNext()) {
                sb.append(' ');
            }
        }
        writeLine(sb, writer);
    }

    public void writeSynonym(Clause clause, BufferedWriter writer) throws IOException {
        Collection<Xref> xrefs = clause.getXrefs();
        if (xrefs == null) {
            clause.setXrefs(new ArrayList<Xref>(0));
        }
        writeClauseWithQuotedString(clause, writer);
    }

    public void write(Clause clause, BufferedWriter writer, NameProvider nameProvider) throws IOException {
        if (OboFormatTag.TAG_IS_OBSELETE.getTag().equals(clause.getTag())) {
            Object value = clause.getValue();
            if (value instanceof Boolean) {
                if (Boolean.FALSE.equals(value)) {
                    return;
                }
            } else {
                if (Boolean.TRUE.toString().equals(value) == false) {
                    return;
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(clause.getTag());
        sb.append(": ");
        Iterator<Object> valuesIterator = clause.getValues().iterator();
        StringBuilder idsLabel = null;
        if (nameProvider != null && tagsInformative.contains(clause.getTag())) {
            idsLabel = new StringBuilder();
        }
        while (valuesIterator.hasNext()) {
            String value = valuesIterator.next().toString();
            if (idsLabel != null) {
                if (nameProvider != null) {
                    String label = nameProvider.getName(value);
                    if (label != null && (isOpaqueIdentifier(value) || !valuesIterator.hasNext())) {
                        if (idsLabel.length() > 0) idsLabel.append(" ");
                        idsLabel.append(label);
                    }
                }
            }
            EscapeMode mode = EscapeMode.most;
            if (OboFormatTag.TAG_COMMENT.getTag().equals(clause.getTag())) {
                mode = EscapeMode.simple;
            }
            sb.append(escapeOboString(value, mode));
            if (valuesIterator.hasNext()) {
                sb.append(' ');
            }
        }
        Collection<Xref> xrefs = clause.getXrefs();
        if (xrefs != null) {
            appendXrefs(sb, xrefs);
        }
        appendQualifiers(sb, clause);
        if (idsLabel != null && idsLabel.length() > 0) {
            String trimmed = idsLabel.toString().trim();
            if (trimmed.length() > 0) {
                sb.append(" ! ");
                sb.append(trimmed);
            }
        }
        writeLine(sb, writer);
    }

    /**
	 * Check if the value is an identifier.
	 * 
	 * @param value
	 * @return boolean
	 */
    private boolean isOpaqueIdentifier(String value) {
        boolean result = false;
        if (value != null && value.length() > 0) {
            int colonPos = value.indexOf(':');
            if (colonPos > 0) {
                if (value.length() > (colonPos + 1)) {
                    result = true;
                    for (int i = colonPos; i < value.length(); i++) {
                        char c = value.charAt(i);
                        if (!Character.isDigit(c) && c != ':') {
                            result = false;
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    private void appendQualifiers(StringBuilder sb, Clause clause) {
        Collection<QualifierValue> qvs = clause.getQualifierValues();
        if (qvs != null && qvs.size() > 0) {
            sb.append(" {");
            Iterator<QualifierValue> qvsIterator = qvs.iterator();
            while (qvsIterator.hasNext()) {
                QualifierValue qv = qvsIterator.next();
                sb.append(qv.getQualifier());
                sb.append("=\"");
                sb.append(escapeOboString(qv.getValue().toString(), EscapeMode.quotes));
                sb.append("\"");
                if (qvsIterator.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append("}");
        }
    }

    private enum EscapeMode {

        most, parenthesis, quotes, xref, xrefList, simple
    }

    private CharSequence escapeOboString(String in, EscapeMode mode) {
        boolean modfied = false;
        StringBuilder sb = new StringBuilder();
        int length = in.length();
        for (int i = 0; i < length; i++) {
            char c = in.charAt(i);
            if (c == '\n') {
                modfied = true;
                sb.append("\\n");
            } else if (c == '\\') {
                modfied = true;
                sb.append("\\\\");
            } else if (c == '"' && (mode == EscapeMode.most || mode == EscapeMode.quotes)) {
                modfied = true;
                sb.append("\\\"");
            } else if (c == '{' && (mode == EscapeMode.most || mode == EscapeMode.parenthesis)) {
                modfied = true;
                sb.append("\\{");
            } else if (c == '}' && (mode == EscapeMode.most || mode == EscapeMode.parenthesis)) {
                modfied = true;
                sb.append("\\}");
            } else if (c == ',' && (mode == EscapeMode.xref || mode == EscapeMode.xrefList)) {
                modfied = true;
                sb.append("\\,");
            } else if (c == ']' && mode == EscapeMode.xrefList) {
                modfied = true;
                sb.append("\\]");
            } else {
                sb.append(c);
            }
        }
        if (modfied) {
            return sb;
        }
        return in;
    }

    private static class HeaderTagsComparator implements Comparator<String> {

        static final HeaderTagsComparator instance = new HeaderTagsComparator();

        private static Hashtable<String, Integer> tagsPriorities = buildTagsPriorities();

        private static Hashtable<String, Integer> buildTagsPriorities() {
            Hashtable<String, Integer> table = new Hashtable<String, Integer>();
            table.put(OboFormatTag.TAG_FORMAT_VERSION.getTag(), 0);
            table.put(OboFormatTag.TAG_DATA_VERSION.getTag(), 10);
            table.put(OboFormatTag.TAG_DATE.getTag(), 15);
            table.put(OboFormatTag.TAG_SAVED_BY.getTag(), 20);
            table.put(OboFormatTag.TAG_AUTO_GENERATED_BY.getTag(), 25);
            table.put(OboFormatTag.TAG_IMPORT.getTag(), 30);
            table.put(OboFormatTag.TAG_SUBSETDEF.getTag(), 35);
            table.put(OboFormatTag.TAG_SYNONYMTYPEDEF.getTag(), 40);
            table.put(OboFormatTag.TAG_DEFAULT_NAMESPACE.getTag(), 45);
            table.put(OboFormatTag.TAG_NAMESPACE_ID_RULE.getTag(), 46);
            table.put(OboFormatTag.TAG_IDSPACE.getTag(), 50);
            table.put(OboFormatTag.TAG_TREAT_XREFS_AS_EQUIVALENT.getTag(), 55);
            table.put(OboFormatTag.TAG_TREAT_XREFS_AS_GENUS_DIFFERENTIA.getTag(), 60);
            table.put(OboFormatTag.TAG_TREAT_XREFS_AS_RELATIONSHIP.getTag(), 65);
            table.put(OboFormatTag.TAG_TREAT_XREFS_AS_IS_A.getTag(), 70);
            table.put(OboFormatTag.TAG_REMARK.getTag(), 75);
            table.put(OboFormatTag.TAG_ONTOLOGY.getTag(), 85);
            return table;
        }

        public int compare(String o1, String o2) {
            Integer i1 = tagsPriorities.get(o1);
            Integer i2 = tagsPriorities.get(o2);
            if (i1 == null) i1 = 10000;
            if (i2 == null) i2 = 10000;
            return i1.compareTo(i2);
        }
    }

    private static class TermsTagsComparator implements Comparator<String> {

        static final TermsTagsComparator instance = new TermsTagsComparator();

        private static Hashtable<String, Integer> tagsPriorities = buildTagsPriorities();

        private static Hashtable<String, Integer> buildTagsPriorities() {
            Hashtable<String, Integer> table = new Hashtable<String, Integer>();
            table.put(OboFormatTag.TAG_ID.getTag(), 5);
            table.put(OboFormatTag.TAG_IS_ANONYMOUS.getTag(), 10);
            table.put(OboFormatTag.TAG_NAME.getTag(), 15);
            table.put(OboFormatTag.TAG_NAMESPACE.getTag(), 20);
            table.put(OboFormatTag.TAG_ALT_ID.getTag(), 25);
            table.put(OboFormatTag.TAG_DEF.getTag(), 30);
            table.put(OboFormatTag.TAG_COMMENT.getTag(), 35);
            table.put(OboFormatTag.TAG_SUBSET.getTag(), 40);
            table.put(OboFormatTag.TAG_SYNONYM.getTag(), 45);
            table.put(OboFormatTag.TAG_XREF.getTag(), 50);
            table.put(OboFormatTag.TAG_BUILTIN.getTag(), 55);
            table.put(OboFormatTag.TAG_PROPERTY_VALUE.getTag(), 60);
            table.put(OboFormatTag.TAG_IS_A.getTag(), 65);
            table.put(OboFormatTag.TAG_INTERSECTION_OF.getTag(), 70);
            table.put(OboFormatTag.TAG_UNION_OF.getTag(), 80);
            table.put(OboFormatTag.TAG_EQUIVALENT_TO.getTag(), 85);
            table.put(OboFormatTag.TAG_DISJOINT_FROM.getTag(), 90);
            table.put(OboFormatTag.TAG_RELATIONSHIP.getTag(), 95);
            table.put(OboFormatTag.TAG_CREATED_BY.getTag(), 100);
            table.put(OboFormatTag.TAG_CREATION_DATE.getTag(), 105);
            table.put(OboFormatTag.TAG_IS_OBSELETE.getTag(), 110);
            table.put(OboFormatTag.TAG_REPLACED_BY.getTag(), 115);
            table.put(OboFormatTag.TAG_CONSIDER.getTag(), 120);
            return table;
        }

        public int compare(String o1, String o2) {
            Integer i1 = tagsPriorities.get(o1);
            Integer i2 = tagsPriorities.get(o2);
            if (i1 == null) i1 = 10000;
            if (i2 == null) i2 = 10000;
            return i1.compareTo(i2);
        }
    }

    private static final class ClauseListComparator implements Comparator<Clause> {

        private static final ClauseListComparator instance = new ClauseListComparator();

        public int compare(Clause c1, Clause c2) {
            String t1 = c1.getTag();
            String t2 = c2.getTag();
            int compare = TermsTagsComparator.instance.compare(t1, t2);
            if (compare == 0) {
                compare = ClauseComparator.instance.compare(c1, c2);
            }
            return compare;
        }
    }

    /**
	 * Sort a list of term frame clauses according to in the OBO format 
	 * specified tag and value order. 
	 * 
	 * @param clauses
	 */
    public static void sortTermClauses(List<Clause> clauses) {
        Collections.sort(clauses, ClauseListComparator.instance);
    }

    private static class TypeDefTagsComparator implements Comparator<String> {

        static final TypeDefTagsComparator instance = new TypeDefTagsComparator();

        private static Hashtable<String, Integer> tagsPriorities = buildTagsPriorities();

        private static Hashtable<String, Integer> buildTagsPriorities() {
            Hashtable<String, Integer> table = new Hashtable<String, Integer>();
            table.put(OboFormatTag.TAG_ID.getTag(), 5);
            table.put(OboFormatTag.TAG_IS_ANONYMOUS.getTag(), 10);
            table.put(OboFormatTag.TAG_NAME.getTag(), 15);
            table.put(OboFormatTag.TAG_NAMESPACE.getTag(), 20);
            table.put(OboFormatTag.TAG_ALT_ID.getTag(), 25);
            table.put(OboFormatTag.TAG_DEF.getTag(), 30);
            table.put(OboFormatTag.TAG_COMMENT.getTag(), 35);
            table.put(OboFormatTag.TAG_SUBSET.getTag(), 40);
            table.put(OboFormatTag.TAG_SYNONYM.getTag(), 45);
            table.put(OboFormatTag.TAG_XREF.getTag(), 50);
            table.put(OboFormatTag.TAG_PROPERTY_VALUE.getTag(), 55);
            table.put(OboFormatTag.TAG_DOMAIN.getTag(), 60);
            table.put(OboFormatTag.TAG_RANGE.getTag(), 65);
            table.put(OboFormatTag.TAG_BUILTIN.getTag(), 70);
            table.put(OboFormatTag.TAG_IS_ANTI_SYMMETRIC.getTag(), 75);
            table.put(OboFormatTag.TAG_IS_CYCLIC.getTag(), 80);
            table.put(OboFormatTag.TAG_IS_REFLEXIVE.getTag(), 85);
            table.put(OboFormatTag.TAG_IS_SYMMETRIC.getTag(), 90);
            table.put(OboFormatTag.TAG_IS_TRANSITIVE.getTag(), 100);
            table.put(OboFormatTag.TAG_IS_FUNCTIONAL.getTag(), 105);
            table.put(OboFormatTag.TAG_IS_INVERSE_FUNCTIONAL.getTag(), 110);
            table.put(OboFormatTag.TAG_IS_A.getTag(), 115);
            table.put(OboFormatTag.TAG_INTERSECTION_OF.getTag(), 120);
            table.put(OboFormatTag.TAG_UNION_OF.getTag(), 125);
            table.put(OboFormatTag.TAG_EQUIVALENT_TO.getTag(), 130);
            table.put(OboFormatTag.TAG_DISJOINT_FROM.getTag(), 135);
            table.put(OboFormatTag.TAG_INVERSE_OF.getTag(), 140);
            table.put(OboFormatTag.TAG_TRANSITIVE_OVER.getTag(), 145);
            table.put(OboFormatTag.TAG_HOLDS_OVER_CHAIN.getTag(), 150);
            table.put(OboFormatTag.TAG_EQUIVALENT_TO_CHAIN.getTag(), 155);
            table.put(OboFormatTag.TAG_DISJOINT_OVER.getTag(), 160);
            table.put(OboFormatTag.TAG_RELATIONSHIP.getTag(), 165);
            table.put(OboFormatTag.TAG_CREATED_BY.getTag(), 170);
            table.put(OboFormatTag.TAG_CREATION_DATE.getTag(), 175);
            table.put(OboFormatTag.TAG_IS_OBSELETE.getTag(), 180);
            table.put(OboFormatTag.TAG_REPLACED_BY.getTag(), 185);
            table.put(OboFormatTag.TAG_CONSIDER.getTag(), 190);
            table.put(OboFormatTag.TAG_EXPAND_ASSERTION_TO.getTag(), 195);
            table.put(OboFormatTag.TAG_EXPAND_EXPRESSION_TO.getTag(), 200);
            table.put(OboFormatTag.TAG_IS_METADATA_TAG.getTag(), 205);
            table.put(OboFormatTag.TAG_IS_CLASS_LEVEL_TAG.getTag(), 210);
            return table;
        }

        public int compare(String o1, String o2) {
            Integer i1 = tagsPriorities.get(o1);
            Integer i2 = tagsPriorities.get(o2);
            if (i1 == null) i1 = 10000;
            if (i2 == null) i2 = 10000;
            return i1.compareTo(i2);
        }
    }

    private static class FramesComparator implements Comparator<Frame> {

        static final FramesComparator instance = new FramesComparator();

        public int compare(Frame f1, Frame f2) {
            return f1.getId().compareTo(f2.getId());
        }
    }

    /**
	 * This comparator sorts clauses with the same tag in the specified
	 * write order.
	 */
    private static class ClauseComparator implements Comparator<Clause> {

        static final ClauseComparator instance = new ClauseComparator();

        public int compare(Clause o1, Clause o2) {
            String tag = o1.getTag();
            if (OboFormatTag.TAG_INTERSECTION_OF.getTag().equals(tag)) {
                int s1 = o1.getValues().size();
                int s2 = o2.getValues().size();
                if (s1 < s2) {
                    return -1;
                } else if (s1 > s2) {
                    return 1;
                }
            }
            int comp = compareValues(o1.getValue(), o2.getValue());
            if (comp != 0) {
                return comp;
            }
            return compareValues(o1.getValue2(), o2.getValue2());
        }

        private int compareValues(Object o1, Object o2) {
            String s1 = toStringRepresentation(o1);
            String s2 = toStringRepresentation(o2);
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }
            int comp = s1.compareToIgnoreCase(s2);
            if (comp == 0) {
                comp = s1.compareTo(s2);
            }
            return comp;
        }

        private String toStringRepresentation(Object obj) {
            String s = null;
            if (obj != null) {
                if (obj instanceof Xref) {
                    Xref xref = (Xref) obj;
                    s = xref.getIdref() + " " + xref.getAnnotation();
                } else if (obj instanceof String) {
                    s = (String) obj;
                } else {
                    s = obj.toString();
                }
            }
            return s;
        }
    }

    private static class XrefComparator implements Comparator<Xref> {

        static final XrefComparator instance = new XrefComparator();

        public int compare(Xref x1, Xref x2) {
            String idref1 = x1.getIdref();
            String idref2 = x2.getIdref();
            if (idref1 == null && idref2 == null) {
                return 0;
            }
            if (idref1 == null) {
                return -1;
            }
            if (idref2 == null) {
                return 1;
            }
            return idref1.compareToIgnoreCase(idref2);
        }
    }

    /**
	 * Provide names for given OBO identifiers.
	 * 
	 * This abstraction layer allows to find names from different sources, 
	 * including {@link OBODoc}.
	 */
    public static interface NameProvider {

        /**
		 * Try to retrieve the valid name for the given identifier. 
		 * If not available return null.
		 * 
		 * @param id identifier
		 * @return name or null
		 */
        public String getName(String id);

        /**
		 * Retrieve the default OBO namespace
		 * 
		 * @return default OBO namespace or null
		 */
        public String getDefaultOboNamespace();
    }

    /**
	 * Default implementation of a {@link NameProvider} using an 
	 * underlying {@link OBODoc}.
	 */
    public static class OBODocNameProvider implements NameProvider {

        private final OBODoc oboDoc;

        private final String defaultOboNamespace;

        /**
		 * @param oboDoc
		 */
        public OBODocNameProvider(OBODoc oboDoc) {
            super();
            this.oboDoc = oboDoc;
            Frame headerFrame = oboDoc.getHeaderFrame();
            if (headerFrame != null) {
                defaultOboNamespace = headerFrame.getTagValue(OboFormatTag.TAG_DEFAULT_NAMESPACE, String.class);
            } else {
                defaultOboNamespace = null;
            }
        }

        public String getName(String id) {
            String name = null;
            Frame frame = oboDoc.getTermFrame(id);
            if (frame == null) {
                frame = oboDoc.getTypedefFrame(id);
            }
            if (frame != null) {
                Clause cl = frame.getClause(OboFormatTag.TAG_NAME);
                if (cl != null) {
                    name = cl.getValue(String.class);
                }
            }
            return name;
        }

        public String getDefaultOboNamespace() {
            return defaultOboNamespace;
        }
    }
}
