package org.ttalbott.mytelly;

import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;

/**
 *
 * @author  Tom Talbott
 * @version
 */
public class Schedule extends java.lang.Object {

    private static Schedule m_instance = null;

    private static final String SCHEDULE = "schedule";

    private static final String PROGRAM = "programme";

    private static final String CHANNEL = "channel";

    private static final String START = "start";

    private static final String DEFAULTFILE = "schedule.xml";

    private TreeSet m_schedule = new TreeSet(new ResultComparitor());

    private String m_filename = DEFAULTFILE;

    /** Creates new Schedule */
    private Schedule() {
    }

    private void setFilename(String filename) {
        m_filename = filename;
    }

    public static Schedule getInstance() {
        if (m_instance == null) m_instance = new Schedule();
        return m_instance;
    }

    public static void release() {
        m_instance = null;
    }

    public static Schedule readSchedule() throws IOException {
        return readSchedule(DEFAULTFILE);
    }

    public static Schedule readSchedule(String file) throws IOException {
        boolean validation = false;
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setValidating(validation);
        XMLReader xmlReader = null;
        try {
            SAXParser saxParser = spf.newSAXParser();
            xmlReader = saxParser.getXMLReader();
        } catch (Exception ex) {
            System.err.println(ex);
            return null;
        }
        xmlReader.setErrorHandler(new MyErrorHandler(System.err));
        Schedule schedule = Schedule.getInstance();
        schedule.setFilename(file);
        schedule.clear();
        xmlReader.setContentHandler(new ScheduleHandler(schedule));
        try {
            xmlReader.parse(convertToFileURL(file));
        } catch (SAXException se) {
            System.err.println(se.getMessage());
        } catch (IOException ioe) {
            System.err.println(ioe);
        }
        return schedule;
    }

    /**
     * Convert from a filename to a file URL.
     */
    private static String convertToFileURL(String filename) {
        String path = null;
        try {
            path = new File(filename).toURL().toString();
        } catch (java.net.MalformedURLException mue) {
            System.err.println(mue.getMessage());
        }
        return path;
    }

    private static class MyErrorHandler implements ErrorHandler {

        /** Error handler output goes here */
        private PrintStream out;

        MyErrorHandler(PrintStream out) {
            this.out = out;
        }

        /**
         * Returns a string describing parse exception details
         */
        private String getParseExceptionInfo(SAXParseException spe) {
            String systemId = spe.getSystemId();
            if (systemId == null) {
                systemId = "null";
            }
            String info = "URI=" + systemId + " Line=" + spe.getLineNumber() + ": " + spe.getMessage();
            return info;
        }

        public void warning(SAXParseException spe) throws SAXException {
            out.println("Warning: " + getParseExceptionInfo(spe));
        }

        public void error(SAXParseException spe) throws SAXException {
            String message = "Error: " + getParseExceptionInfo(spe);
            throw new SAXException(message);
        }

        public void fatalError(SAXParseException spe) throws SAXException {
            String message = "Fatal Error: " + getParseExceptionInfo(spe);
            throw new SAXException(message);
        }
    }

    private static class ScheduleHandler extends DefaultHandler {

        Schedule m_schedule;

        StringBuffer m_buf = new StringBuffer();

        TreeSet m_set = null;

        public ScheduleHandler(Schedule schedule) {
            m_schedule = schedule;
        }

        public void startDocument() throws SAXException {
            System.out.println("Parsing schedule file.");
        }

        public void startElement(String namespaceURI, String localName, String rawName, Attributes atts) throws SAXException {
            if (m_buf.length() > 0) m_buf.delete(0, m_buf.length());
            if (rawName.equals(Schedule.SCHEDULE) || localName.equals(Schedule.SCHEDULE)) {
                m_set = new TreeSet(new ResultComparitor());
            }
            if (rawName.equals(Schedule.PROGRAM) || localName.equals(Schedule.PROGRAM)) {
                String[] program = new String[2];
                program[0] = atts.getValue(Schedule.CHANNEL);
                program[1] = atts.getValue(Schedule.START);
                if (m_set != null) {
                    m_set.add(program);
                }
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals(Schedule.PROGRAM) || localName.equals(Schedule.PROGRAM)) {
                if (m_set != null) {
                    m_schedule.setSchedule(m_set);
                }
            }
        }

        public void characters(char[] values, int start, int length) throws org.xml.sax.SAXException {
            m_buf.append(values, start, length);
        }
    }

    private void setSchedule(TreeSet schedule) {
        m_schedule = schedule;
    }

    public void writeSchedule() throws IOException {
        writeSchedule(m_filename);
    }

    public void writeSchedule(String file) throws IOException {
        MyXMLWriter writer = new MyXMLWriter(new FileWriter(file));
        TreeMap atts = new TreeMap();
        writer.setIndent(2);
        writer.startDocument();
        writer.xmlDecl("ISO-8859-1", "1.0");
        writer.startTag(SCHEDULE);
        Iterator it = m_schedule.iterator();
        while (it.hasNext()) {
            String[] prog = (String[]) it.next();
            atts.clear();
            atts.put(CHANNEL, prog[0]);
            atts.put(START, prog[1]);
            writer.dataElement(PROGRAM, "", atts);
        }
        writer.endTag(SCHEDULE);
        writer.endDocument();
    }

    public boolean checkInSchedule(ProgItem prog) {
        String[] program = new String[2];
        Programs programs = Programs.getInstance();
        program[0] = programs.getChannel(prog);
        program[1] = programs.getStartTime(prog);
        return m_schedule.contains(program);
    }

    public void addToSchedule(ProgItem prog) {
        String[] program = new String[2];
        Programs programs = Programs.getInstance();
        program[0] = programs.getChannel(prog);
        program[1] = programs.getStartTime(prog);
        m_schedule.add(program);
    }

    public void removeFromSchedule(ProgItem prog) {
        String[] program = new String[2];
        Programs programs = Programs.getInstance();
        program[0] = programs.getChannel(prog);
        program[1] = programs.getStartTime(prog);
        m_schedule.remove(program);
    }

    public void toggleSchedule(ProgItem prog) {
        String[] program = new String[2];
        Programs programs = Programs.getInstance();
        program[0] = programs.getChannel(prog);
        program[1] = programs.getStartTime(prog);
        if (m_schedule.contains(program)) {
            m_schedule.remove(program);
        } else {
            m_schedule.add(program);
        }
    }

    public void clear() {
        m_schedule.clear();
    }

    public ProgramList getScheduleNodes(ProgramList inputNodes) {
        Programs programs = Programs.getInstance();
        ProgramList fullResults = programs.getEmptyProgramList();
        if (m_schedule != null && inputNodes != null && fullResults != null) {
            TreeSet sortedResults = new TreeSet(new ResultComparitor());
            sortedResults.addAll(m_schedule);
            int count = inputNodes.getLength();
            for (int i = 0; i < count; i++) {
                ProgItem prog = (ProgItem) inputNodes.item(i);
                String[] program = new String[2];
                program[0] = programs.getChannel(prog);
                program[1] = programs.getStartTime(prog);
                if (sortedResults.contains(program)) {
                    fullResults.add(prog);
                }
            }
        }
        return fullResults;
    }

    public void removeOldEntries() {
        TreeSet newSchedule = new TreeSet(new ResultComparitor());
        Iterator it = m_schedule.iterator();
        String[] prog;
        String time;
        String today = Utilities.dateToXMLTVDate(new Date()).substring(0, 8);
        while (it.hasNext()) {
            prog = (String[]) it.next();
            time = prog[1].substring(0, 8);
            if (time.compareTo(today) >= 0) newSchedule.add(prog);
        }
        m_schedule = newSchedule;
    }
}
