package de.unibi.techfak.bibiserv.biodom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import de.unibi.techfak.bibiserv.biodom.ebiInterfaces.EBIAlignmentInterface;
import de.unibi.techfak.bibiserv.biodom.ebiInterfaces.EBIApplicationResultInterface;
import de.unibi.techfak.bibiserv.biodom.ebiInterfaces.EBIDatabaseInterface;
import de.unibi.techfak.bibiserv.biodom.ebiInterfaces.EBIHitInterface;
import de.unibi.techfak.bibiserv.biodom.ebiInterfaces.EBIIterationInterface;
import de.unibi.techfak.bibiserv.biodom.ebiInterfaces.EBIParametersInterface;
import de.unibi.techfak.bibiserv.biodom.ebiInterfaces.EBISequenceInterface;
import de.unibi.techfak.bibiserv.biodom.exception.BioDOMException;

/**
 * ApplicationResult -- A java _interface_ to ApplicationResult.xsd compliant Documents
 * 
 * http://www.ebi.ac.uk/schema
 * http://www.ebi.ac.uk/schema/ApplicationResult.xsd
 *
 * @author Alexander Kaiser <akaiser@techfak.uni-bielefeld.de>
 * @version $Id: EBIApplicationResult.java,v 1.16 2006/08/07 13:39:20 spindle_dev Exp $
 *
 */
public class EBIApplicationResult extends AbstractBioDOM implements EBIApplicationResultInterface {

    private static Logger log = Logger.getLogger(EBIApplicationResult.class.toString());

    /**
	 * namespace of ApplicationResult
	 */
    protected static final String NS = "http://www.ebi.ac.uk/schema";

    /**
	 * location of the DTDs
	 */
    private static final String DTDLocation = "http://www.ncbi.nlm.nih.gov/dtd/";

    /**
	 * empty default constructor
	 * creates a new ApplicationResult object
	 *
	 * @exception BioDOMException
	 *   on failure
	 */
    public EBIApplicationResult() throws BioDOMException {
        this("", null);
    }

    /**
	 * creates a new ApplicationResult object
	 *
	 * @param catalogproperties
	 *   path of Catalog.properties file
	 * @exception BioDOMException
	 *   on failure
	 */
    public EBIApplicationResult(String catalogproperties) throws BioDOMException {
        this(catalogproperties, null);
    }

    /**
	 * creates a new ApplicationResult object for processing a Document
	 *
	 * @param submitted_dom
	 *   Document for processing
	 * @exception BioDOMException
	 *   on failure
	 */
    public EBIApplicationResult(Document submitted_dom) throws BioDOMException {
        this("", submitted_dom);
    }

    /**
	 * creates a new ApplicationResult object for processing a Document
	 *
	 * @param catalogproperties
	 *   path of Catalog.properties file
	 * @param submitted_dom
	 *   Document for processing
	 * @exception BioDOMException
	 *   on failure
	 */
    public EBIApplicationResult(String catalogproperties, Document submitted_dom) throws BioDOMException {
        super(catalogproperties);
        NSLOCATION = "http://bibiserv.techfak.uni-bielefeld.de/xsd/uk/ac/ebi/schema/ApplicationResult.xsd";
        isNillable = false;
        if (submitted_dom != null) {
            this.setDom(submitted_dom);
        } else {
            initDOM();
        }
    }

    /**
	 * set a Document for processing
	 *
	 * @param dom
	 *   Document to be set
	 * @exception BioDOMException
	 *   on failure
	 * @see de.unibi.techfak.bibiserv.biodom.AbstractBioDOM#setDom()
	 */
    public void setDom(final Document dom) throws BioDOMException {
        super.setDom(dom);
        if (!this.validate()) {
            log.severe("couldnt init, XML validation failed");
            throw new BioDOMException("couldnt init, XML validation failed");
        }
    }

    /**
	 * get the content of ApplicationResult object as Document
	 *
	 * @return
	 *   returns the current Document
	 * @throws BioDOMException
	 *   on failure
	 * @see de.unibi.techfak.bibiserv.biodom.AbstractBioDOM#getDom()
	 */
    public Document getDom() throws BioDOMException {
        if (!validate()) {
            throw new BioDOMException("DOM validation failed");
        }
        return getDom();
    }

    /**
	 * access the parameters section of this ApplicationResult
	 *
	 * @return
	 *   a reference to the parameters or null if no such section exsits
	 */
    public EBIParametersInterface getParameters() {
        final NodeList nl = dom.getElementsByTagNameNS(NAMESPACE, "parameters");
        return nl.getLength() != 1 ? null : new EBIParametersImpl((Element) nl.item(0));
    }

    /**
	 * access an Iteration by its number
	 *
	 * @param number
	 *   number of Iterarion to be selected
	 *
	 * @return
	 *   a reference to an Iteration or null if Iteration does not exsist
	 */
    public EBIIterationInterface getIteration(final int number) {
        final Element iteration = getElementByAttrib(dom.getDocumentElement(), "iteration", "number", String.valueOf(number));
        return iteration == null ? null : new EBIIterationImpl(iteration);
    }

    /**
	 * get total number of iterations in this Application Result
	 *   
	 * @return
	 *   the number of iterations as int
	 */
    public int getIterationsTotal() {
        return getAttributeIntByTagName(dom.getDocumentElement(), "iterations", "total");
    }

    /**
	 * append an Iteration
	 *
	 * @return
	 *   a reference to the new Iteration or null if Document
	 *   does not have an "iterations" Element
	 */
    public EBIIterationInterface appendIteration() {
        int total;
        Element iterations, iteration, sssr;
        final NodeList nl = dom.getElementsByTagNameNS(NAMESPACE, "iterations");
        if (nl.getLength() == 0) {
            final NodeList nl2 = dom.getElementsByTagNameNS(NAMESPACE, "SequenceSimilaritySearchResult");
            if (nl2.getLength() == 1) {
                sssr = (Element) nl2.item(0);
            } else {
                return null;
            }
            iterations = dom.createElementNS(NAMESPACE, "iterations");
            iterations.setAttributeNS(null, "total", "0");
            sssr.appendChild(iterations);
            total = 0;
        } else if (nl.getLength() == 1) {
            iterations = (Element) nl.item(0);
            total = getAttribAsInt(iterations, "total");
        } else {
            return null;
        }
        iterations.setAttributeNS(null, "total", String.valueOf(++total));
        iteration = dom.createElementNS(NAMESPACE, "iteration");
        iterations.appendChild(iteration);
        return new EBIIterationImpl(iteration, total);
    }

    /**
	 * set information about the program associated with this ApplicationResult
	 *
	 * @param name
	 *   name of the program (e.g. blastp)
	 * @param version
	 *   version of the used program
	 * @param citation
	 *   the citation
	 */
    public void setProgram(final String name, final String version, final String citation) {
        final NodeList nl = dom.getElementsByTagNameNS(NAMESPACE, "program");
        Element program;
        if (nl.getLength() == 1) {
            program = (Element) nl.item(0);
            program.setAttributeNS(null, "name", name);
            program.setAttributeNS(null, "version", version);
            program.setAttributeNS(null, "citation", citation);
        } else {
            return;
        }
    }

    /**
	 * get the name of the program associated with this ApplicationResult
	 * 
	 * @return
	 *   the name
	 */
    public String getProgramName() {
        return getAttributeValueByTagName(dom.getDocumentElement(), "program", "name");
    }

    /**
	 * get the version of the program associated with this ApplicationResult
	 * 
	 * @return
	 *   the version as String
	 */
    public String getProgramVersion() {
        return getAttributeValueByTagName(dom.getDocumentElement(), "program", "version");
    }

    /**
	 * get the citation for the program associated with this ApplicationResult
	 * 
	 * @return
	 *   the citation
	 */
    public String getProgramCitation() {
        return getAttributeValueByTagName(dom.getDocumentElement(), "program", "citation");
    }

    /**
	 * append blast hits from blast's native XML output
	 *
	 * @param doc
	 *   the input as blast compliant Document
	 * @param setParameters
	 *   set global parameters of this ApplicationResult as well as the hits
	 * @param setSequences
	 *   use query-description  as sequence name and append sequence information
	 *   to this EBIApplicationResultInterface
	 */
    public void appendHitsFromBlastXML(final Document doc, final boolean setParameters, final boolean setSequences) {
        final String NO_DEF = "No definition line found";
        final Element blastOutput = doc.getDocumentElement();
        final String database = getTextByTagName(blastOutput, "BlastOutput_db");
        final EBIParametersInterface params = this.getParameters();
        if (params != null) {
            if (setSequences) {
                int qlen;
                String qdef;
                qdef = getTextByTagName(blastOutput, "BlastOutput_query-def");
                qlen = getTextAsInt(blastOutput, "BlastOutput_query-len");
                final EBISequenceInterface seq = params.appendSequence("any", qlen);
                if (!qdef.equalsIgnoreCase(NO_DEF)) seq.setName(qdef);
            }
            if (setParameters) {
                final String name = getTextByTagName(blastOutput, "BlastOutput_program");
                final String version = getTextByTagName(blastOutput, "BlastOutput_version");
                final String citation = getTextByTagName(blastOutput, "BlastOutput_reference");
                this.setProgram(name, version, citation);
                final int sequences = getTextAsInt(blastOutput, "Statistics_db-num");
                final int letters = getTextAsInt(blastOutput, "Statistics_db-len");
                params.appendDatabase(database, "any", sequences, letters);
                params.setMatrix(getTextByTagName(blastOutput, "Parameters_matrix"));
                params.setExpectationUpper(getTextByTagName(blastOutput, "Parameters_expect"));
                params.setGapOpen(getTextAsFloat(blastOutput, "Parameters_gap-open"));
                params.setGapExtension(getTextAsFloat(blastOutput, "Parameters_gap-extend"));
                params.setFilter(getTextByTagName(blastOutput, "Parameters_filter"));
            }
        }
        final NodeList blastIterations = blastOutput.getElementsByTagNameNS(NAMESPACE, "Iteration");
        int i = 0;
        while (blastIterations != null && i < blastIterations.getLength()) {
            final Element blastIteration = (Element) blastIterations.item(i++);
            EBIIterationInterface iter = this.getIteration(getTextAsInt(blastIteration, "Iteration_iter-num"));
            if (iter == null) iter = this.appendIteration();
            final NodeList blastHits = blastIteration.getElementsByTagNameNS(NAMESPACE, "Hit");
            int j = 0;
            while (blastHits != null && j < blastHits.getLength()) {
                final Element blastHit = (Element) blastHits.item(j++);
                final String id = getTextByTagName(blastHit, "Hit_id");
                final String hdef = getTextByTagName(blastHit, "Hit_def");
                final EBIHitInterface hit = iter.appendHit(database, id);
                hit.setAc(getTextByTagName(blastHit, "Hit_accession"));
                hit.setLength(getTextAsInt(blastHit, "Hit_len"));
                if (!hdef.equalsIgnoreCase(NO_DEF)) hit.setDescription(hdef);
                final NodeList hsps = blastHit.getElementsByTagNameNS(NAMESPACE, "Hsp");
                int k = 0;
                while (hsps != null && k < hsps.getLength()) {
                    final Element hsp = (Element) hsps.item(k++);
                    final EBIAlignmentInterface ali = hit.appendAlignment();
                    ali.setBits(getTextAsFloat(hsp, "Hsp_bit-score"));
                    ali.setScore(getTextAsInt(hsp, "Hsp_score"));
                    ali.setExpectation(getTextAsFloat(hsp, "Hsp_evalue"));
                    ali.setQuerySeq(getTextAsInt(hsp, "Hsp_query-from"), getTextAsInt(hsp, "Hsp_query-to"), getTextByTagName(hsp, "Hsp_qseq"));
                    ali.setPattern(getTextByTagName(hsp, "Hsp_midline"));
                    ali.setMatchSeq(getTextAsInt(hsp, "Hsp_hit-from"), getTextAsInt(hsp, "Hsp_hit-to"), getTextByTagName(hsp, "Hsp_hseq"));
                    ali.setIdentity(getTextAsFloat(hsp, "Hsp_identity"));
                    ali.setPositives(getTextAsFloat(hsp, "Hsp_positive"));
                }
            }
        }
    }

    /**
	 * use data of the broken xml output from a multiple sequence blast query
	 * for this ApplicationResult
	 * if data cannot be use or is corrupted a BioDOMException will be thrown
	 *fromBrokenBlastXML
	 * @param input
	 *   the xml as an InputStream
	 */
    public void fromBrokenBlastXML(final InputStream input) throws BioDOMException {
        int c;
        final int buffSize = 1024;
        final byte buff[] = new byte[buffSize];
        final OutputStream os = new ByteArrayOutputStream(buffSize);
        try {
            while ((c = input.read(buff)) != -1) os.write(buff, 0, c);
            fromBrokenBlastXML(os.toString());
        } catch (IOException ex) {
            throw new BioDOMException(ex.toString());
        }
    }

    /**
	 * use data of the broken xml output from a multiple sequence blast query
	 * for this ApplicationResult
	 * if data cannot be use or is corrupted a BioDOMException will be thrown
	 *
	 * @param input
	 *   the xml as a String
	 */
    public void fromBrokenBlastXML(final String input) throws BioDOMException {
        try {
            final List<Document> docList = getDocsFromBlastXML(input);
            int i = 0;
            for (Document doc : docList) this.appendHitsFromBlastXML(doc, i++ == 0, true);
            if (!this.validate()) throw new BioDOMException("validation of underlying Document faild");
        } catch (Exception ex) {
            throw new BioDOMException(ex.toString());
        }
    }

    /**
	 * get the value of an text node by its name
	 *
	 * @param elem
	 *   the root element
	 * @param tagname
	 *   the tagname of the element to be considered
	 * @return
	 *   the value of the text content of the element associated with tagname
	 */
    private String getTextByTagName(final Element elem, final String tagname) {
        final NodeList nl = elem.getElementsByTagNameNS(NAMESPACE, tagname);
        if (nl.getLength() == 0) return null;
        return nl.item(0).getTextContent();
    }

    /**
	 * set the text content of an element specified by tagname to value
	 * if elem has no direct or indirect children that match tagname a direct
	 * child to elem will be created and its text content will be set to value
	 *
	 * @param doc
	 *   the source Document (for Element creation)
	 * @param elem
	 *   the root element
	 * @param tagname
	 *   the tagname of the Element to be considered
	 * @param value
	 *   the value 
	 * @return
	 *   the node affected by this method
	 */
    private Element setText(final Document doc, final Element elem, final String tagname, final String value) {
        Element tmp = null;
        final NodeList nl = elem.getElementsByTagNameNS(NAMESPACE, tagname);
        if (nl.getLength() == 0) {
            tmp = doc.createElementNS(NAMESPACE, tagname);
            tmp.setTextContent(value);
            elem.appendChild(tmp);
        } else {
            nl.item(0).setTextContent(value);
        }
        return tmp;
    }

    /**
	 * get the value of an text node as int
	 *
	 * @param elem
	 *   the root element
	 * @param tagname
	 *   the tagname of the element to be considered
	 * @return
	 *   the value of the text content of the element associated with tagname
	 */
    private int getTextAsInt(final Element elem, final String tagname) {
        int value;
        String text;
        if ((text = getTextByTagName(elem, tagname)) == null) return 0;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            log.warning("NumberFormatException while parsing int value of Text Node");
            value = 0;
        }
        return value;
    }

    /**
	 * set the text content of an element specified by tagname to value
	 * if elem has no direct or indirect children that match tagname a direct
	 * child to elem will be created and its text content will be set to value
	 *
	 * @param doc
	 *   the source Document (for Element generation)
	 * @param elem
	 *   the root element
	 * @param tagname
	 *   the tagname of the Element to be considered
	 * @param value
	 *   the value 
	 */
    private void setTextFromInt(final Document doc, final Element elem, final String tagname, final int value) {
        setText(doc, elem, tagname, String.valueOf(value));
    }

    /**
	 * get the value of an text node as float
	 *
	 * @param elem
	 *   the root element
	 * @param tagname
	 *   the tagname of the element to be considered
	 * @return
	 *   the value of the text content of the element associated with tagname
	 */
    private float getTextAsFloat(final Element elem, final String tagname) {
        float value;
        String text;
        if ((text = getTextByTagName(elem, tagname)) == null) return 0;
        try {
            value = Float.parseFloat(text);
        } catch (NumberFormatException ex) {
            log.warning("NumberFormatException while parsing float value of Text Node");
            value = 0;
        }
        return value;
    }

    /**
	 * set the text content of an element specified by tagname to value
	 * if elem has no direct or indirect children that match tagname a direct
	 * child to elem will be created and its text content will be set to value
	 *
	 * @param doc
	 *   the source Document (for Element generation)
	 * @param elem
	 *   the root element
	 * @param tagname
	 *   the tagname of the Element to be considered
	 * @param value
	 *   the value 
	 */
    private void setTextFromFloat(final Document doc, final Element elem, final String tagname, final float value) {
        setText(doc, elem, tagname, String.valueOf(value));
    }

    /**
	 * get the value of an text node as boolean 
	 *
	 * @param elem
	 *   the root element
	 * @param tagname
	 *   the tagname of the element to be considered
	 * @return
	 *   the value of the text content of the element associated with tagname
	 */
    private boolean getTextAsBoolean(final Element elem, final String tagname) {
        String text;
        if ((text = getTextByTagName(elem, tagname)) == null) return false;
        return Boolean.parseBoolean(text);
    }

    /**
	 * set the text content of an element specified by tagname to value
	 * if elem has no direct or indirect children that match tagname a direct
	 * child to elem will be created and its text content will be set to value
	 *
	 * @param doc
	 *   the source Document (for Element generation)
	 * @param elem
	 *   the root element
	 * @param tagname
	 *   the tagname of the Element to be considered
	 * @param value
	 *   the value 
	 */
    private void setTextFromBoolean(final Document doc, final Element elem, final String tagname, final boolean value) {
        setText(doc, elem, tagname, String.valueOf(value));
    }

    /**
	 * get the value of an Attribute specified by attname of an Element specified
	 * by tagname that is direct or indirect child of elem
	 *
	 * @param elem
	 *   the root Element
	 * @param tagname
	 *   the name of the Element to be considered
	 * @param attname
	 *   the name of the Attr
	 * @return
	 *   the value of the Attr or null if no such Element/Attr exists
	 */
    private String getAttributeValueByTagName(final Element elem, final String tagname, final String attname) {
        final NodeList nl = elem.getElementsByTagNameNS(NAMESPACE, tagname);
        if (nl.getLength() == 0) return "";
        return ((Element) nl.item(0)).getAttribute(attname);
    }

    /**
	 * get the value of an Attribute specified by attname of an Element specified
	 * by tagname that is direct or indirect child of elem
	 *
	 * @param elem
	 *   the root Element
	 * @param tagname
	 *   the name of the Element to be considered
	 * @param attname
	 *   the name of the Attr
	 * @return
	 *   the value of the Attr or null if no such Element/Attr exists
	 */
    private int getAttributeIntByTagName(final Element elem, final String tagname, final String attname) {
        String text;
        int value;
        text = getAttributeValueByTagName(elem, tagname, attname);
        if (text == null) return 0;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            log.warning("NumberFormatException while parsing int value of Attribute");
            value = 0;
        }
        return value;
    }

    /**
	 * get the value of an attribute node as int
	 *
	 * @param elem
	 *   the source Element
	 * @param attname
	 *   name of the Attr
	 * @return
	 *   the value of the Attr associated with attname as int
	 */
    private static int getAttribAsInt(final Element elem, final String attname) {
        int value;
        try {
            value = Integer.parseInt(elem.getAttribute(attname));
        } catch (NumberFormatException ex) {
            log.warning("NumberFormatException while parsing int value of Attribute");
            value = 0;
        }
        return value;
    }

    /**
	 * get an Element by a specific value of one of its Attrs
	 *
	 * @param elem
	 *   the source Element
	 * @param name
	 *   name of the Elements to be considered
	 * @param attname
	 *   name of the Attrs to be considered
	 * @param value
	 *   value to look for
	 * @return
	 *   an Element that is direct or indirect child of elem which has an Attr named attname with the specified value
	 */
    private Element getElementByAttrib(final Element elem, final String name, final String attname, final String value) {
        Node n;
        final NodeList nl = elem.getElementsByTagNameNS(NAMESPACE, name);
        for (int i = 0; i < nl.getLength(); i++) {
            n = nl.item(i);
            if ((n.getNodeType() == Node.ELEMENT_NODE) && (((Element) n).getAttribute(attname).equals(value))) {
                return (Element) n;
            }
        }
        return null;
    }

    /**
	 * extracts all Documents from a String that contains more than one
	 *
	 * @param input
	 *   the String that should be searched for Documents
	 * @return
	 *   a List of Documents
	 */
    private static List<Document> getDocsFromBlastXML(final String input) throws IOException, ParserConfigurationException, SAXException {
        final LinkedList<Document> docList = new LinkedList<Document>();
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final int[] shifts = shiftsOf(input, "<?xml");
        final int lastIndex = input.length() - 1;
        int beginIndex, endIndex;
        for (int i = 0; i < shifts.length; i++) {
            beginIndex = shifts[i];
            endIndex = (i == shifts.length - 1) ? lastIndex : shifts[i + 1];
            final String xml = input.substring(beginIndex, endIndex);
            final InputSource is = new InputSource(new StringReader(xml));
            is.setSystemId(DTDLocation);
            final Document doc = builder.parse(is);
            docList.addLast(doc);
        }
        return docList;
    }

    /**
	 * An exact pattern matching algorithm using Java String.indexOf()
	 *
	 * @param str
	 *   the source String
	 * @param pat
	 *   the pattern
	 * @return
	 *   an array containing the inices of all occurences of pat in str
	 */
    private static int[] shiftsOf(final String str, final String pat) {
        int shift = 0;
        final LinkedList<Integer> ll = new LinkedList<Integer>();
        while ((shift = str.indexOf(pat, ll.isEmpty() ? 0 : ll.getLast() + 1)) != -1) ll.addLast(new Integer(shift));
        int[] outArr = new int[ll.size()];
        int i = 0;
        for (Integer tmp : ll) outArr[i++] = tmp;
        return outArr;
    }

    /**
	 * generates an empty DOM Tree
	 */
    private void initDOM() {
        Element header, sssr, program, parameters;
        dom = DB.newDocument();
        final Element root = dom.createElementNS(NAMESPACE, "EBIApplicationResult");
        root.setAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "schemaLocation", NAMESPACE + " " + NSLOCATION);
        dom.appendChild(root);
        dom.normalizeDocument();
        log.config("DOM ready...");
        header = dom.createElementNS(NAMESPACE, "Header");
        program = dom.createElementNS(NAMESPACE, "program");
        header.appendChild(program);
        parameters = dom.createElementNS(NAMESPACE, "parameters");
        header.appendChild(parameters);
        root.appendChild(header);
        sssr = dom.createElementNS(NAMESPACE, "SequenceSimilaritySearchResult");
        root.appendChild(sssr);
    }

    /**
	 * private implementation of EBIParametersInterface
	 */
    private class EBIParametersImpl implements EBIParametersInterface {

        private Element parameters;

        public EBIParametersImpl(Element parameters) {
            this.parameters = parameters;
        }

        public EBISequenceInterface appendSequence(final String type, final int length) {
            int total;
            Element sequences, sequence;
            final NodeList nl = parameters.getElementsByTagNameNS(NAMESPACE, "sequences");
            if (nl.getLength() == 0) {
                sequences = dom.createElementNS(NAMESPACE, "sequences");
                sequences.setAttributeNS(null, "total", "0");
                parameters.appendChild(sequences);
                total = 0;
            } else if (nl.getLength() == 1) {
                sequences = (Element) nl.item(0);
                total = getAttribAsInt(sequences, "total");
            } else {
                return null;
            }
            sequences.setAttributeNS(null, "total", String.valueOf(++total));
            sequence = dom.createElementNS(NAMESPACE, "sequence");
            sequences.appendChild(sequence);
            return new EBISequenceImpl(sequence, total, type, length);
        }

        public EBISequenceInterface getSequence(final int number) {
            final Element sequence = getElementByAttrib(parameters, "sequence", "number", String.valueOf(number));
            return sequence == null ? null : new EBISequenceImpl(sequence);
        }

        public int getSequencesTotal() {
            return getAttributeIntByTagName(parameters, "sequences", "total");
        }

        public EBIDatabaseInterface appendDatabase(final String name, final String type, final int sequences, final int letters) {
            int total, olds, oldl;
            Element databases, database;
            final NodeList nl = parameters.getElementsByTagNameNS(NAMESPACE, "databases");
            if (nl.getLength() == 0) {
                databases = dom.createElementNS(NAMESPACE, "databases");
                databases.setAttributeNS(null, "total", "0");
                databases.setAttributeNS(null, "sequences", String.valueOf(sequences));
                databases.setAttributeNS(null, "letters", String.valueOf(letters));
                parameters.appendChild(databases);
                total = 0;
            } else if (nl.getLength() == 1) {
                databases = (Element) nl.item(0);
                total = getAttribAsInt(databases, "total");
                olds = getAttribAsInt(databases, "sequences");
                oldl = getAttribAsInt(databases, "letters");
                databases.setAttributeNS(null, "sequences", String.valueOf(sequences + olds));
                databases.setAttributeNS(null, "letters", String.valueOf(letters + oldl));
            } else {
                return null;
            }
            databases.setAttributeNS(null, "total", String.valueOf(++total));
            database = dom.createElementNS(NAMESPACE, "database");
            databases.appendChild(database);
            return new EBIDatabaseImpl(database, total, name, type);
        }

        public EBIDatabaseInterface getDatabase(final int number) {
            final Element database = getElementByAttrib(parameters, "database", "number", String.valueOf(number));
            return database == null ? null : new EBIDatabaseImpl(database);
        }

        public int getDatabasesTotal() {
            return getAttributeIntByTagName(parameters, "databases", "total");
        }

        public int getDatabasesSequences() {
            return getAttributeIntByTagName(parameters, "databases", "sequences");
        }

        public int getDatabasesLetters() {
            return getAttributeIntByTagName(parameters, "databases", "letters");
        }

        public void setScores(final int scores) {
            setTextFromInt(dom, parameters, "scores", scores);
        }

        public int getScores() {
            return getTextAsInt(parameters, "scores");
        }

        public void setAlignments(final int alignments) {
            setTextFromInt(dom, parameters, "alignments", alignments);
        }

        public int getAlignments() {
            return getTextAsInt(parameters, "alignments");
        }

        public void setMatrix(final String matrix) {
            setText(dom, parameters, "matrix", matrix);
        }

        public String getMatrix() {
            return getTextByTagName(parameters, "matrix");
        }

        public void setExpectationLower(final String expectationLower) {
            setText(dom, parameters, "expectationLower", expectationLower);
        }

        public String getExpectationLower() {
            return getTextByTagName(parameters, "expectationLower");
        }

        public void setExpectationUpper(final String expectationUpper) {
            setText(dom, parameters, "expectationUpper", expectationUpper);
        }

        public String getExpectationUpper() {
            return getTextByTagName(parameters, "expectationUpper");
        }

        public void setStatistics(final String statistics) {
            setText(dom, parameters, "statistics", statistics);
        }

        public String getStatistics() {
            return getTextByTagName(parameters, "statistics");
        }

        public void setKtup(final int ktup) {
            setTextFromInt(dom, parameters, "ktup", ktup);
        }

        public int getKtup() {
            return getTextAsInt(parameters, "ktup");
        }

        public void setSort(final String sort) {
            setText(dom, parameters, "sort", sort);
        }

        public String getSort() {
            return getTextByTagName(parameters, "sort");
        }

        public void setStrand(final String strand) {
            setText(dom, parameters, "strand", strand);
        }

        public String getStrand() {
            return getTextByTagName(parameters, "strand");
        }

        public void setFilter(final String filter) {
            setText(dom, parameters, "filter", filter);
        }

        public String getFilter() {
            return getTextByTagName(parameters, "filter");
        }

        public void setHistogram(final boolean histogram) {
            setTextFromBoolean(dom, parameters, "histogram", histogram);
        }

        public boolean getHistogram() {
            return getTextAsBoolean(parameters, "histogram");
        }

        public void setGapExtension(final float gapExtension) {
            setTextFromFloat(dom, parameters, "gapExtension", gapExtension);
        }

        public float getGapExtension() {
            return getTextAsFloat(parameters, "gapExtension");
        }

        public void setGapOpen(final float gapOpen) {
            setTextFromFloat(dom, parameters, "gapOpen", gapOpen);
        }

        public float getGapOpen() {
            return getTextAsFloat(parameters, "gapOpen");
        }

        public void setSequenceRange(final String sequenceRange) {
            setText(dom, parameters, "sequenceRange", sequenceRange);
        }

        public String getSequenceRange() {
            return getTextByTagName(parameters, "sequenceRange");
        }

        public void setDatabaseRange(final String databaseRange) {
            setText(dom, parameters, "databaseRange", databaseRange);
        }

        public String getDatabaseRange() {
            return getTextByTagName(parameters, "databaseRange");
        }

        public void setMoleculeType(final String moleculeType) {
            setText(dom, parameters, "moleculeType", moleculeType);
        }

        public String getMoleculeType() {
            return getTextByTagName(parameters, "moleculeType");
        }
    }

    /**
	 * private implementation of EBISequenceInterface
	 */
    private class EBISequenceImpl implements EBISequenceInterface {

        Element sequence;

        EBISequenceImpl(Element sequence) {
            this.sequence = sequence;
        }

        EBISequenceImpl(Element sequence, int number, String type, int length) {
            this.sequence = sequence;
            this.sequence.setAttributeNS(null, "number", String.valueOf(number));
            this.sequence.setAttributeNS(null, "type", type);
            this.sequence.setAttributeNS(null, "length", String.valueOf(length));
        }

        public int getNumber() {
            return getAttribAsInt(sequence, "number");
        }

        public void setName(final String name) {
            sequence.setAttributeNS(null, "name", name);
        }

        public String getName() {
            return sequence.getAttribute("name");
        }

        public void setType(final String type) {
            sequence.setAttributeNS(null, "type", type);
        }

        public String getType() {
            return sequence.getAttribute("type");
        }

        public void setLength(final int length) {
            sequence.setAttributeNS(null, "length", String.valueOf(length));
        }

        public int getLength() {
            return getAttribAsInt(sequence, "length");
        }
    }

    /**
	 * private implementation of EBIDatabaseInterface
	 */
    private class EBIDatabaseImpl implements EBIDatabaseInterface {

        private Element database;

        public EBIDatabaseImpl(Element database) {
            this.database = database;
        }

        EBIDatabaseImpl(Element database, int number, String name, String type) {
            this.database = database;
            this.database.setAttributeNS(null, "number", String.valueOf(number));
            this.database.setAttributeNS(null, "name", name);
            this.database.setAttributeNS(null, "type", type);
        }

        public int getNumber() {
            return getAttribAsInt(database, "number");
        }

        public void setName(final String name) {
            database.setAttributeNS(null, "name", name);
        }

        public String getName() {
            return database.getAttribute("name");
        }

        public void setType(final String type) {
            database.setAttributeNS(null, "type", type);
        }

        public String getType() {
            return database.getAttribute("type");
        }

        public void setCreated(final String created) {
            database.setAttributeNS(null, "created", created);
        }

        public String getCreated() {
            return database.getAttribute("created");
        }
    }

    /**
	 * private implementation of EBIIterationInterface
	 */
    private class EBIIterationImpl implements EBIIterationInterface {

        private Element iteration;

        public EBIIterationImpl(Element iteration) {
            this.iteration = iteration;
        }

        public EBIIterationImpl(Element iteration, int number) {
            this.iteration = iteration;
            this.iteration.setAttributeNS(null, "number", String.valueOf(number));
        }

        public EBIHitInterface appendHit(final String database, final String id) {
            int total;
            Element hits, hit;
            final NodeList nl = iteration.getElementsByTagNameNS(NAMESPACE, "hits");
            if (nl.getLength() == 0) {
                hits = dom.createElementNS(NAMESPACE, "hits");
                hits.setAttributeNS(null, "total", "0");
                iteration.appendChild(hits);
                total = 0;
            } else if (nl.getLength() == 1) {
                hits = (Element) nl.item(0);
                total = getAttribAsInt(hits, "total");
            } else {
                return null;
            }
            hits.setAttributeNS(null, "total", String.valueOf(++total));
            hit = dom.createElementNS(NAMESPACE, "hit");
            hits.appendChild(hit);
            return new EBIHitImpl(hit, total, database, id);
        }

        public EBIHitInterface getHit(final int number) {
            final Element hit = getElementByAttrib(iteration, "hit", "number", String.valueOf(number));
            return hit == null ? null : new EBIHitImpl(hit);
        }

        public int getHitsTotal() {
            return getAttributeIntByTagName(iteration, "hits", "total");
        }

        public int getNumber() {
            return getAttribAsInt(iteration, "number");
        }
    }

    /**
	 * private implementation of EBIHitInterface
	 */
    private class EBIHitImpl implements EBIHitInterface {

        Element hit;

        public EBIHitImpl(Element hit, int number, String database, String id) {
            this.hit = hit;
            this.hit.setAttributeNS(null, "number", String.valueOf(number));
            this.hit.setAttributeNS(null, "database", database);
            this.hit.setAttributeNS(null, "id", id);
        }

        public EBIHitImpl(Element hit) {
            this.hit = hit;
        }

        public EBIAlignmentInterface appendAlignment() {
            int total;
            Element alignments, alignment;
            final NodeList nl = hit.getElementsByTagNameNS(NAMESPACE, "alignments");
            if (nl.getLength() == 0) {
                alignments = dom.createElementNS(NAMESPACE, "alignments");
                alignments.setAttributeNS(null, "total", "0");
                hit.appendChild(alignments);
                total = 0;
            } else if (nl.getLength() == 1) {
                alignments = (Element) nl.item(0);
                total = getAttribAsInt(alignments, "total");
            } else {
                return null;
            }
            alignments.setAttributeNS(null, "total", String.valueOf(++total));
            alignment = dom.createElementNS(NAMESPACE, "alignment");
            alignments.appendChild(alignment);
            return new EBIAlignmentImpl(alignment, total);
        }

        public EBIAlignmentInterface getAlignment(final int number) {
            final Element alignment = getElementByAttrib(hit, "alignment", "number", String.valueOf(number));
            return alignment == null ? null : new EBIAlignmentImpl(alignment);
        }

        public int getAlignmentsTotal() {
            return getAttributeIntByTagName(hit, "alignments", "total");
        }

        public int getNumber() {
            return getAttribAsInt(hit, "number");
        }

        public void setDatabse(final String database) {
            hit.setAttributeNS(null, "database", database);
        }

        public String getDatabse() {
            return hit.getAttribute("database");
        }

        public void setId(final String id) {
            hit.setAttributeNS(null, "id", id);
        }

        public String getId() {
            return hit.getAttribute("id");
        }

        public void setAc(final String ac) {
            hit.setAttributeNS(null, "ac", ac);
        }

        public String getAc() {
            return hit.getAttribute("ac");
        }

        public void setLength(final int length) {
            hit.setAttributeNS(null, "length", String.valueOf(length));
        }

        public int getLength() {
            return getAttribAsInt(hit, "length");
        }

        public void setDescription(final String description) {
            hit.setAttributeNS(null, "description", description);
        }

        public String getDescription() {
            return hit.getAttribute("description");
        }
    }

    /**
	 * private implementation of EBIAlignmentInterface
	 */
    private class EBIAlignmentImpl implements EBIAlignmentInterface {

        private Element alignment;

        public EBIAlignmentImpl(Element alignment) {
            this.alignment = alignment;
        }

        public EBIAlignmentImpl(Element alignment, int number) {
            this.alignment = alignment;
            this.alignment.setAttributeNS(null, "number", String.valueOf(number));
        }

        public void setBits(final float bits) {
            setTextFromFloat(dom, alignment, "bits", bits);
        }

        public float getBits() {
            return getTextAsFloat(alignment, "bits");
        }

        public void setExpectation(final float expectation) {
            setTextFromFloat(dom, alignment, "expectation", expectation);
        }

        public float getExpectation() {
            return getTextAsFloat(alignment, "expectation");
        }

        public void setIdentity(final float identity) {
            setTextFromFloat(dom, alignment, "identity", identity);
        }

        public float getIdentity() {
            return getTextAsFloat(alignment, "identity");
        }

        public void setQuerySeq(final int start, final int end, final String querySeq) {
            final Element query = setText(dom, alignment, "querySeq", querySeq);
            if (query != null) {
                query.setAttributeNS(null, "start", String.valueOf(start));
                query.setAttributeNS(null, "end", String.valueOf(end));
            }
        }

        public String getQuerySeq() {
            return getTextByTagName(alignment, "querySeq");
        }

        public int getQuerySeqStart() {
            return getAttributeIntByTagName(alignment, "querySeq", "start");
        }

        public int getQuerySeqEnd() {
            return getAttributeIntByTagName(alignment, "querySeq", "end");
        }

        public void setPattern(final String pattern) {
            setText(dom, alignment, "pattern", pattern);
        }

        public String getPattern() {
            return getTextByTagName(alignment, "pattern");
        }

        public void setMatchSeq(final int start, final int end, final String matchSeq) {
            final Element query = setText(dom, alignment, "matchSeq", matchSeq);
            if (query != null) {
                query.setAttributeNS(null, "start", String.valueOf(start));
                query.setAttributeNS(null, "end", String.valueOf(end));
            }
        }

        public String getMatchSeq() {
            return getTextByTagName(alignment, "matchSeq");
        }

        public int getMatchSeqStart() {
            return getAttributeIntByTagName(alignment, "matchSeq", "start");
        }

        public int getMatchSeqEnd() {
            return getAttributeIntByTagName(alignment, "matchSeq", "end");
        }

        public void setScore(final int score) {
            setTextFromInt(dom, alignment, "score", score);
        }

        public int getScore() {
            return getTextAsInt(alignment, "score");
        }

        public void setPositives(final float positives) {
            setTextFromFloat(dom, alignment, "positives", positives);
        }

        public float getPositives() {
            return getTextAsFloat(alignment, "positives");
        }
    }
}
