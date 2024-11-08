package net.sf.xtvdclient.xtvd.parser;

import net.sf.xtvdclient.xtvd.DataDirectException;
import net.sf.xtvdclient.xtvd.datatypes.Crew;
import net.sf.xtvdclient.xtvd.datatypes.CrewMember;
import net.sf.xtvdclient.xtvd.datatypes.DateTime;
import net.sf.xtvdclient.xtvd.datatypes.Genre;
import net.sf.xtvdclient.xtvd.datatypes.Lineup;
import net.sf.xtvdclient.xtvd.datatypes.Map;
import net.sf.xtvdclient.xtvd.datatypes.MovieAdvisories;
import net.sf.xtvdclient.xtvd.datatypes.Part;
import net.sf.xtvdclient.xtvd.datatypes.Program;
import net.sf.xtvdclient.xtvd.datatypes.ProgramGenre;
import net.sf.xtvdclient.xtvd.datatypes.Schedule;
import net.sf.xtvdclient.xtvd.datatypes.Station;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;

/**
 * A <code>parser</code> implementation that is used to parse the
 * incoming XML stream, and then write the parsed data back to an
 * <code>Writer</code>.  This parser is most useful for extracting
 * the <code>XTVD document</code> from the <code>SOAP message</code>
 * and storing to a <code>file</code>.
 *
 * @author Rakesh Vidyadharan 26<sup><small>th</small></sup> February, 2004
 *         <p/>
 *         <p>Copyright 2004, Tribune Media Services</p>
 *         <p/>
 *         $Id: WriterParser.java,v 1.11 2004/04/06 18:36:43 rakesh Exp $
 */
public class WriterParser extends AbstractParser {

    /**
   * The <code>XMLStreamWriter</code> to which the parsed <code>XTVD
   * document</code> is to be written.
   */
    private XMLStreamWriter writer;

    /**
   * Create a new instance of the parser that reads the data from the
   * specified <code>Reader</code>.
   *
   * @param Reader in - The Reader from which the XML data
   *               is to be parsed.  Make sure that the reader object has the
   *               <code>character encoding</code> properly set to
   *               <code>UTF-8</code>.
   * @param Writer out - The writer to which the parsed XTVD XML is
   *               to be written.  Make sure that the writer object has the
   *               <code>character encoding</code> properly set to
   *               <code>UTF-8</code>.
   * @throws DataDirectException - If errors are encountered while
   *                             initialising the parser or reading the data.
   */
    protected WriterParser(Reader in, Writer out) throws DataDirectException {
        super();
        try {
            reader = XMLInputFactory.newInstance().createXMLStreamReader(in);
            writer = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
        } catch (XMLStreamException xsex) {
            throw new DataDirectException(xsex.getMessage(), xsex);
        }
    }

    /**
   * Read the <code>SOAP response</code> from the {@link #reader},
   * parse the <code>XTVD document</code> from the response and
   * write the document to the {@link #writer}.
   *
   * @throws DataDirectException - If errors are encountered while
   *                             parsing the XML data stream or while writing to the
   *                             {@link #writer}.
   * @see #getMessages()
   * @see #parseRootElement()
   * @see #parseStations()
   * @see #parseLineups()
   * @see #parseSchedules()
   * @see #parsePrograms()
   * @see #parseProductionCrew()
   * @see #parseGenres()
   */
    public void parseXTVD() throws DataDirectException {
        try {
            toStartTag();
            Collection collection = getMessages();
            if (!collection.isEmpty()) {
                try {
                    log.write(sdf.format(new java.util.Date()));
                    log.write("\tServer returned the following message(s).");
                    log.write(Parser.END_OF_LINE);
                    log.write("<messages>");
                    log.write(Parser.END_OF_LINE);
                    for (Iterator iterator = collection.iterator(); iterator.hasNext(); ) {
                        log.write("<message>");
                        log.write((String) iterator.next());
                        log.write("</message>");
                        log.write(Parser.END_OF_LINE);
                    }
                    log.write("</messages>");
                    log.write(Parser.END_OF_LINE);
                } catch (IOException ioex) {
                    ioex.printStackTrace();
                }
            }
            while (!reader.getLocalName().equals("xtvd")) {
                reader.next();
                toStartTag();
            }
            parseRootElement();
            while (!reader.getLocalName().equals("stations")) {
                reader.next();
                toStartTag();
            }
            parseStations();
            while (!reader.getLocalName().equals("lineups")) {
                reader.next();
                toStartTag();
            }
            parseLineups();
            while (!reader.getLocalName().equals("schedules")) {
                reader.next();
                toStartTag();
            }
            parseSchedules();
            while (!reader.getLocalName().equals("programs")) {
                reader.next();
                toStartTag();
            }
            parsePrograms();
            while (!reader.getLocalName().equals("productionCrew")) {
                reader.next();
                toStartTag();
            }
            parseProductionCrew();
            while (!reader.getLocalName().equals("genres")) {
                reader.next();
                toStartTag();
            }
            parseGenres();
            while (reader.hasNext()) reader.next();
            reader.close();
            writer.writeEndDocument();
            writer.writeCharacters(Parser.END_OF_LINE);
            try {
                log.write(sdf.format(new java.util.Date()));
                log.write("\tFinished parsing XTVD document");
                log.write(Parser.END_OF_LINE);
            } catch (IOException ioex) {
                ioex.printStackTrace();
            }
        } catch (XMLStreamException xsex) {
            throw new DataDirectException(xsex.getMessage(), xsex);
        }
    }

    /**
   * Parse an root <code>xtvd</code> record from the
   * {@link #reader}, and write the parsed data to the {@link #writer}.
   * Also add the XML declaration at the top of the output.
   *
   * @throws DataDirectException - If errors are encountered while
   *                             reading or writing the XML data.
   */
    protected void parseRootElement() throws DataDirectException {
        try {
            writer.writeStartDocument();
            writer.writeCharacters(Parser.END_OF_LINE);
            writer.writeStartElement("xtvd");
            try {
                writer.writeAttribute("from", (new DateTime(reader.getAttributeValue(null, "from"))).toString());
                writer.writeAttribute("to", (new DateTime(reader.getAttributeValue(null, "to"))).toString());
                writer.writeAttribute("xmlns", reader.getNamespaceURI(0));
                writer.writeAttribute("xmlns", reader.getNamespaceURI(0), "xsi", reader.getNamespaceURI(1));
                if (reader.getAttributeValue(null, "schemaLocation") != null) {
                    writer.writeAttribute("xsi", reader.getNamespaceURI(1), "schemaLocation", reader.getAttributeValue(null, "schemaLocation"));
                }
            } catch (java.text.ParseException pex) {
                pex.printStackTrace();
            }
            writer.writeAttribute("schemaVersion", reader.getAttributeValue(null, "schemaVersion"));
            writer.writeCharacters(Parser.END_OF_LINE);
        } catch (XMLStreamException xsex) {
            throw new DataDirectException(xsex.getMessage(), xsex);
        }
    }

    /**
   * Parse an individual <code>station</code> record from the
   * {@link #reader}, and write the parsed data to the {@link #writer}.
   *
   * @throws DataDirectException - If errors are encountered while
   *                             reading or writing the XML data.
   * @see #getStation(Station)
   */
    protected void parseStations() throws DataDirectException {
        try {
            log.write(sdf.format(new java.util.Date()));
            log.write("\tParsing stations top-level element");
            log.write(Parser.END_OF_LINE);
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
        try {
            writer.writeStartElement("stations");
            writer.writeCharacters(Parser.END_OF_LINE);
            reader.next();
            toStartTag();
            Station station = new Station();
            while (reader.getLocalName().equals("station")) {
                getStation(station);
                writer.writeStartElement("station");
                writer.writeAttribute("id", String.valueOf(station.getId()));
                writer.writeCharacters(Parser.END_OF_LINE);
                writer.writeStartElement("callSign");
                writer.writeCharacters(station.getCallSign());
                writer.writeEndElement();
                writer.writeCharacters(Parser.END_OF_LINE);
                if (station.getName() != null && station.getName().length() > 0) {
                    writer.writeStartElement("name");
                    writer.writeCharacters(station.getName());
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                }
                if (station.getFccChannelNumber() > 0) {
                    writer.writeStartElement("fccChannelNumber");
                    writer.writeCharacters(String.valueOf(station.getFccChannelNumber()));
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                }
                if (station.getAffiliate() != null && station.getAffiliate().length() > 0) {
                    writer.writeStartElement("affiliate");
                    writer.writeCharacters(station.getAffiliate());
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                }
                writer.writeEndElement();
                writer.writeCharacters(Parser.END_OF_LINE);
                station.reset();
            }
            writer.writeEndElement();
            writer.writeCharacters(Parser.END_OF_LINE);
        } catch (XMLStreamException xsex) {
            throw new DataDirectException(xsex.getMessage(), xsex);
        }
    }

    /**
   * Parse the <code>lineups</code> top-level element from the
   * {@link #reader} and write the parsed data to the {@link #writer}.
   *
   * @throws DataDirectException - If errors are encountered while
   *                             reading or writing the XML data.
   * @see #getLineup(net.sf.xtvdclient.xtvd.Lineup)
   */
    protected void parseLineups() throws DataDirectException {
        try {
            log.write(sdf.format(new java.util.Date()));
            log.write("\tParsing lineups top-level element");
            log.write(Parser.END_OF_LINE);
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
        try {
            writer.writeStartElement("lineups");
            writer.writeCharacters(Parser.END_OF_LINE);
            reader.next();
            toStartTag();
            Lineup lineup = new Lineup();
            while (reader.getLocalName().equals("lineup")) {
                getLineup(lineup);
                writer.writeStartElement("lineup");
                writer.writeAttribute("id", lineup.getId());
                writer.writeAttribute("name", lineup.getName());
                writer.writeAttribute("location", lineup.getLocation());
                writer.writeAttribute("type", lineup.getType().toString());
                if (lineup.getDevice() != null && lineup.getDevice().length() > 0) {
                    writer.writeAttribute("device", lineup.getDevice());
                }
                if (lineup.getPostalCode() != null && lineup.getPostalCode().length() > 0) {
                    writer.writeAttribute("postalCode", lineup.getPostalCode());
                }
                writer.writeCharacters(Parser.END_OF_LINE);
                for (Iterator iterator = lineup.getMaps().iterator(); iterator.hasNext(); ) {
                    Map map = (Map) iterator.next();
                    writer.writeStartElement("map");
                    writer.writeAttribute("station", String.valueOf(map.getStation()));
                    writer.writeAttribute("channel", String.valueOf(map.getChannel()));
                    if (map.getChannelMinor() != 0) {
                        writer.writeAttribute("channelMinor", String.valueOf(map.getChannelMinor()));
                    }
                    if (map.getFrom() != null) {
                        writer.writeAttribute("from", map.getFrom().toString());
                    }
                    if (map.getTo() != null) {
                        writer.writeAttribute("to", map.getTo().toString());
                    }
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                }
                writer.writeEndElement();
                writer.writeCharacters(Parser.END_OF_LINE);
                lineup.reset();
            }
            writer.writeEndElement();
            writer.writeCharacters(Parser.END_OF_LINE);
        } catch (XMLStreamException xsex) {
            throw new DataDirectException(xsex.getMessage(), xsex);
        }
    }

    /**
   * Parse the <code>schedules</code> top-level element from the
   * {@link #reader} and write the parsed data to the {@link #writer}.
   *
   * @throws DataDirectException - If errors are encountered while
   *                             reading or writing the XML data.
   * @see #getSchedule(net.sf.xtvdclient.xtvd.datatypes.Schedule)
   */
    protected void parseSchedules() throws DataDirectException {
        try {
            log.write(sdf.format(new java.util.Date()));
            log.write("\tParsing schedules top-level element");
            log.write(Parser.END_OF_LINE);
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
        try {
            writer.writeStartElement("schedules");
            writer.writeCharacters(Parser.END_OF_LINE);
            reader.next();
            toStartTag();
            Schedule schedule = new Schedule();
            while (reader.getLocalName().equals("schedule")) {
                getSchedule(schedule);
                writer.writeStartElement("schedule");
                writer.writeAttribute("program", schedule.getProgram());
                writer.writeAttribute("station", String.valueOf(schedule.getStation()));
                writer.writeAttribute("time", schedule.getTime().toString());
                writer.writeAttribute("duration", schedule.getDuration().toString());
                if (schedule.getRepeat()) writer.writeAttribute("repeat", String.valueOf(schedule.getRepeat()));
                if (schedule.getTvRating() != null) writer.writeAttribute("tvRating", schedule.getTvRating().toString());
                if (schedule.getStereo()) writer.writeAttribute("stereo", String.valueOf(schedule.getStereo()));
                if (schedule.getSubtitled()) writer.writeAttribute("subtitled", String.valueOf(schedule.getSubtitled()));
                if (schedule.getHdtv()) writer.writeAttribute("hdtv", String.valueOf(schedule.getHdtv()));
                if (schedule.getCloseCaptioned()) writer.writeAttribute("closeCaptioned", String.valueOf(schedule.getCloseCaptioned()));
                Part part = schedule.getPart();
                if (part != null) {
                    writer.writeCharacters(Parser.END_OF_LINE);
                    writer.writeStartElement("part");
                    writer.writeAttribute("number", String.valueOf(part.getNumber()));
                    writer.writeAttribute("total", String.valueOf(part.getTotal()));
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                }
                writer.writeEndElement();
                writer.writeCharacters(Parser.END_OF_LINE);
                schedule.reset();
            }
            writer.writeEndElement();
            writer.writeCharacters(Parser.END_OF_LINE);
        } catch (XMLStreamException xsex) {
            throw new DataDirectException(xsex.getMessage(), xsex);
        }
    }

    /**
   * Parse the <code>programs</code> top-level element from the
   * {@link #reader} and write the parsed data to the {@link #writer}.
   *
   * @throws DataDirectException - If errors are encountered while
   *                             reading or writing the XML data.
   * @see #getProgram(net.sf.xtvdclient.xtvd.datatypes.Program)
   */
    protected void parsePrograms() throws DataDirectException {
        try {
            log.write(sdf.format(new java.util.Date()));
            log.write("\tParsing programs top-level element");
            log.write(Parser.END_OF_LINE);
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
        try {
            writer.writeStartElement("programs");
            writer.writeCharacters(Parser.END_OF_LINE);
            reader.next();
            toStartTag();
            Program program = new Program();
            while (reader.getLocalName().equals("program")) {
                getProgram(program);
                writer.writeStartElement("program");
                writer.writeAttribute("id", program.getId());
                writer.writeCharacters(Parser.END_OF_LINE);
                writer.writeStartElement("title");
                writer.writeCharacters(program.getTitle());
                writer.writeEndElement();
                writer.writeCharacters(Parser.END_OF_LINE);
                if (program.getSubtitle() != null && program.getSubtitle().length() > 0) {
                    writer.writeStartElement("subtitle");
                    writer.writeCharacters(program.getSubtitle());
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                }
                if (program.getDescription() != null && program.getDescription().length() > 0) {
                    writer.writeStartElement("description");
                    writer.writeCharacters(program.getDescription());
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                }
                if (program.getMpaaRating() != null) {
                    writer.writeStartElement("mpaaRating");
                    writer.writeCharacters(program.getMpaaRating().toString());
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                }
                if (program.getStarRating() != null) {
                    writer.writeStartElement("starRating");
                    writer.writeCharacters(program.getStarRating().toString());
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                }
                if (program.getRunTime() != null) {
                    writer.writeStartElement("runTime");
                    writer.writeCharacters(program.getRunTime().toString());
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                }
                if (program.getYear() != null && program.getYear().length() > 0) {
                    writer.writeStartElement("year");
                    writer.writeCharacters(program.getYear());
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                }
                if (program.getShowType() != null && program.getShowType().length() > 0) {
                    writer.writeStartElement("showType");
                    writer.writeCharacters(program.getShowType());
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                }
                if (program.getSeries() != null && program.getSeries().length() > 0) {
                    writer.writeStartElement("series");
                    writer.writeCharacters(program.getSeries());
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                }
                if (program.getColorCode() != null) {
                    writer.writeStartElement("colorCode");
                    writer.writeCharacters(program.getColorCode().toString());
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                }
                Collection advisories = program.getAdvisories();
                if (!advisories.isEmpty()) {
                    writer.writeStartElement("advisories");
                    writer.writeCharacters(Parser.END_OF_LINE);
                    for (Iterator iterator = advisories.iterator(); iterator.hasNext(); ) {
                        writer.writeStartElement("advisory");
                        writer.writeCharacters(((MovieAdvisories) iterator.next()).toString());
                        writer.writeEndElement();
                        writer.writeCharacters(Parser.END_OF_LINE);
                    }
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                }
                if (program.getSyndicatedEpisodeNumber() != null) {
                    writer.writeStartElement("syndicatedEpisodeNumber");
                    writer.writeCharacters(program.getSyndicatedEpisodeNumber());
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                }
                if (program.getOriginalAirDate() != null) {
                    writer.writeStartElement("originalAirDate");
                    writer.writeCharacters(program.getOriginalAirDate().toString());
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                }
                writer.writeEndElement();
                writer.writeCharacters(Parser.END_OF_LINE);
                program.reset();
            }
            writer.writeEndElement();
            writer.writeCharacters(Parser.END_OF_LINE);
        } catch (XMLStreamException xsex) {
            throw new DataDirectException(xsex.getMessage(), xsex);
        }
    }

    /**
   * Parse the <code>productionCrew</code> top-level element from the
   * {@link #reader} and write the parsed data to the {@link #writer}.
   *
   * @throws DataDirectException - If errors are encountered while
   *                             reading or writing the XML data.
   * @see #getCrew(net.sf.xtvdclient.xtvd.Crew)
   */
    protected void parseProductionCrew() throws DataDirectException {
        try {
            log.write(sdf.format(new java.util.Date()));
            log.write("\tParsing productionCrew top-level element");
            log.write(Parser.END_OF_LINE);
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
        try {
            writer.writeStartElement("productionCrew");
            writer.writeCharacters(Parser.END_OF_LINE);
            reader.next();
            toStartTag();
            Crew crew = new Crew();
            while (reader.getLocalName().equals("crew")) {
                getCrew(crew);
                writer.writeStartElement("crew");
                writer.writeAttribute("program", crew.getProgram());
                writer.writeCharacters(Parser.END_OF_LINE);
                Collection collection = crew.getMember();
                if (collection != null) {
                    for (Iterator iterator = collection.iterator(); iterator.hasNext(); ) {
                        writer.writeStartElement("member");
                        writer.writeCharacters(Parser.END_OF_LINE);
                        CrewMember member = (CrewMember) iterator.next();
                        writer.writeStartElement("role");
                        writer.writeCharacters(member.getRole());
                        writer.writeEndElement();
                        writer.writeCharacters(Parser.END_OF_LINE);
                        writer.writeStartElement("givenname");
                        writer.writeCharacters(member.getGivenname());
                        writer.writeEndElement();
                        writer.writeCharacters(Parser.END_OF_LINE);
                        writer.writeStartElement("surname");
                        writer.writeCharacters(member.getSurname());
                        writer.writeEndElement();
                        writer.writeCharacters(Parser.END_OF_LINE);
                        writer.writeEndElement();
                        writer.writeCharacters(Parser.END_OF_LINE);
                    }
                }
                writer.writeEndElement();
                writer.writeCharacters(Parser.END_OF_LINE);
                crew.reset();
            }
            writer.writeEndElement();
            writer.writeCharacters(Parser.END_OF_LINE);
        } catch (XMLStreamException xsex) {
            throw new DataDirectException(xsex.getMessage(), xsex);
        }
    }

    /**
   * Parse the <code>genres</code> top-level element from the
   * {@link #reader} and write the parsed data to the {@link #writer}.
   *
   * @throws DataDirectException - If errors are encountered while
   *                             reading or writing the XML data.
   * @see #getProgramGenre(ProgramGenre)
   */
    protected void parseGenres() throws DataDirectException {
        try {
            log.write(sdf.format(new java.util.Date()));
            log.write("\tParsing genres top-level element");
            log.write(Parser.END_OF_LINE);
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
        try {
            writer.writeStartElement("genres");
            writer.writeCharacters(Parser.END_OF_LINE);
            reader.next();
            toStartTag();
            ProgramGenre programGenre = new ProgramGenre();
            while (reader.hasNext() && reader.getLocalName().equals("programGenre")) {
                getProgramGenre(programGenre);
                writer.writeStartElement("programGenre");
                writer.writeAttribute("program", programGenre.getProgram());
                writer.writeCharacters(Parser.END_OF_LINE);
                for (Iterator iterator = programGenre.getGenres().iterator(); iterator.hasNext(); ) {
                    Genre genre = (Genre) iterator.next();
                    writer.writeStartElement("genre");
                    writer.writeCharacters(Parser.END_OF_LINE);
                    writer.writeStartElement("class");
                    writer.writeCharacters(genre.getClassValue());
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                    writer.writeStartElement("relevance");
                    writer.writeCharacters(String.valueOf(genre.getRelevance()));
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                    writer.writeEndElement();
                    writer.writeCharacters(Parser.END_OF_LINE);
                }
                writer.writeEndElement();
                writer.writeCharacters(Parser.END_OF_LINE);
                programGenre.reset();
            }
            writer.writeEndElement();
            writer.writeCharacters(Parser.END_OF_LINE);
        } catch (XMLStreamException xsex) {
            throw new DataDirectException(xsex.getMessage(), xsex);
        }
    }
}
