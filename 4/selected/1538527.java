package freeguide.common.lib.importexport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import freeguide.common.lib.fgspecific.data.TVChannel;
import freeguide.common.lib.fgspecific.data.TVChannelsSet;
import freeguide.common.lib.fgspecific.data.TVProgramme;
import freeguide.common.lib.general.StringHelper;
import freeguide.plugins.importexport.xmltv.ITVDataIterators;

/**
 * Export data to xmltv file.
 *
 * @author Alex Buloichik (alex73 at zaval.org)
 */
public class XMLTVExport {

    protected DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss Z", Locale.ENGLISH);

    /**
     * Creates a new XMLTVExport object.
     */
    public XMLTVExport() {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
     * DOCUMENT_ME!
     *
     * @param outFile DOCUMENT_ME!
     * @param data DOCUMENT_ME!
     *
     * @throws IOException DOCUMENT_ME!
     */
    public void export(final File outFile, final ITVDataIterators data) throws Exception {
        final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"));
        exportToWriter(out, data);
        out.flush();
        out.close();
    }

    /**
     * Write some XMLTV to a file.
     *
     * @param out the writer to write the exprted XMLTV to
     * @param data the object that supplies the channels and programmes to write
     * @throws IOException
     * @throws Exception
     */
    public void exportToWriter(final Writer out, final ITVDataIterators data) throws IOException, Exception {
        writeHeader(out);
        Collection channels = data.getChannels();
        Iterator itCh = channels.iterator();
        while (itCh.hasNext()) {
            TVChannelsSet.Channel chinfo = (TVChannelsSet.Channel) itCh.next();
            TVChannel ch = data.getRealChannel(chinfo);
            writeChannelInfo(out, ch);
        }
        itCh = channels.iterator();
        while (itCh.hasNext()) {
            TVChannelsSet.Channel ch = (TVChannelsSet.Channel) itCh.next();
            Collection progs = data.getProgrammes(ch);
            final Iterator itP = progs.iterator();
            while (itP.hasNext()) {
                TVProgramme programme = (TVProgramme) itP.next();
                writeProgrammeInfo(out, programme);
            }
        }
        writeFooter(out);
    }

    protected void writeHeader(final Writer out) throws IOException {
        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE tv SYSTEM \"xmltv.dtd\">\n\n");
        out.write("<tv source-info-url=\"freeguide-tv\" generator-info-name=\"freeguide-tv\">\n");
    }

    protected void writeFooter(final Writer out) throws IOException {
        out.write("</tv>\n");
    }

    protected void writeChannelInfo(final Writer out, TVChannel ch) throws IOException {
        out.write("  <channel id=\"" + StringHelper.toXML(ch.getID()) + "\">\n");
        out.write("    <display-name>" + StringHelper.toXML(ch.getDisplayName()) + "</display-name>\n");
        if (ch.getIconURL() != null) {
            out.write("    <icon src=\"" + StringHelper.toXML(ch.getIconURL()) + "\"/>\n");
        }
        out.write("  </channel>\n");
    }

    protected void writeProgrammeInfo(final Writer out, TVProgramme prog) throws IOException {
        out.write("  <programme start=\"" + DATE_FORMAT.format(new Date(prog.getStart())) + "\" stop=\"" + DATE_FORMAT.format(new Date(prog.getEnd())) + "\" channel=\"" + StringHelper.toXML(prog.getChannel().getID()) + "\">\n");
        out.write("    <title>" + StringHelper.toXML(prog.getTitle()) + "</title>\n");
        if (prog.getDescription() != null) {
            out.write("    <desc>" + StringHelper.toXML(prog.getDescription()) + "</desc>\n");
        }
        if (prog.getExtraTags() != null) {
            Iterator itExtra = prog.getExtraTags().keySet().iterator();
            while (itExtra.hasNext()) {
                String tag = (String) itExtra.next();
                Map attrs = (Map) prog.getExtraTags().get(tag);
                if (attrs != null) {
                    if ((attrs.size() == 1) && attrs.containsKey("")) {
                        out.write("    <" + tag + ">");
                        out.write(StringHelper.toXML((String) attrs.get("")));
                        out.write("</" + tag + ">\n");
                    } else {
                        out.write("    <" + tag);
                        Iterator itAttr = attrs.keySet().iterator();
                        while (itAttr.hasNext()) {
                            String attr = (String) itAttr.next();
                            String value = (String) attrs.get(attr);
                            out.write(" " + attr + "=\"");
                            out.write(StringHelper.toXML(value));
                            out.write("\"");
                        }
                        out.write("/>\n");
                    }
                }
            }
        }
        out.write("  </programme>\n");
    }
}
