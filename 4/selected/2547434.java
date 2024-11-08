package barde.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Locale;
import barde.log.Message;

/**
 * This class is intended to bypass limits of {@link HTMLWriter}.
 *
 * Like :
 * <ul>
 * <li>space gained by factorizing the channels and sources.<br>
 *	<i>( Current observed gain is between 35% and 40% over the original decompressed log size. )</i>
 * <li>interactivity like showing or masking channels or sources<br>
 *	<i>( Not currently. )</i>
 * </ul>
 *
 * <p>This class may support or be supported by other classes that also generate javascript.</p>
 *
 * <p>In order for the javascripter to provide advanced and interactive functionalities,
 * he must not be constrained to deal with the Java code,
 * and so, the javascript code must be separated from the Java code.
 * To achieve this, the current solution is for this class to output its results in javascript variables, so that they can be imported by scripts.
 * The javascript functionnalities would then be written in a separate <code>.js</code> file.
 * </p>
 *
 * <p>Beyong those variables lies a huge table which contains all the lines of the log.
 * In spite of the fact that they are compressed, this represents a great amount of data comparing to the size of an average HTML page.
 * The problem that arise in the current implementation is that this table has to be written at the beginning of the output.
 * So the web browser must read all the data before printing the page.
 * This point must be patched up ; for example one may create a list of successive function calls so that they are executed at the time they are received...
 * </p>
 * 
 * <p>A skeleton HTML file is provided here for convenience : <a href="skeleton.html">skeleton.html</a></p>
 *
 * @author cbonar
 * @see HTMLWriter
 */
public class JavascriptWriter extends AbstractLogWriter {

    PrintStream out = null;

    ArrayList channels = null;

    ArrayList sources = null;

    Date startDate = null;

    Date lastDate = null;

    public JavascriptWriter(PrintStream ps) {
        this.out = ps;
    }

    public JavascriptWriter(OutputStream os) {
        this(new PrintStream(os));
    }

    protected void writeHeader() {
        this.channels = new ArrayList();
        this.sources = new ArrayList();
        this.out.println("function printLog() {");
        this.out.println("var log = [");
    }

    protected void writeFooter() {
        this.out.println("];");
        SimpleDateFormat jsdf = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.ENGLISH);
        String dateString = jsdf.format(this.startDate);
        dateString = Character.toUpperCase(dateString.charAt(0)) + dateString.substring(1).replaceAll("\\.", "");
        this.out.println("var startDate = new Date(\"" + dateString + "\");");
        this.out.print("var channels = [");
        int i = 0;
        for (; i < this.channels.size() - 1; i++) this.out.print("\"" + escape((String) this.channels.get(i)) + "\",");
        this.out.println("\"" + escape((String) this.channels.get(i)) + "\"];");
        this.out.print("var sources = [");
        int j = 0;
        for (; j < this.sources.size() - 1; j++) this.out.print("\"" + escape((String) this.sources.get(j)) + "\",");
        this.out.println("\"" + escape((String) this.sources.get(j)) + "\"];");
        this.out.println("document.writeln(\"<table>\");");
        this.out.println("document.writeln(\"<TR><TH class='date'>DATE</TH><TH class='channel'>CHANNEL</TH><TH class='source'>SOURCE</TH><TH class='message'>MESSAGE</TH></TR>\");");
        this.out.println("for ( var m=0 ; m<log.length ; m++ )\n" + "\t{\n" + "\t\tvar date = new Date();\n" + "\t\tdate.setTime( startDate.getTime() + parseInt(log[m][0])*1000 );\n" + "\t\tdocument.write(\"<tr class='\"+channels[log[m][1]].replace(/ /g,'_')+\"'>\");\n" + "\t\tdocument.write(\"<td class='date'>\"+date.toLocaleString()+\"</td>\");\n" + "\t\tdocument.write(\"<td class='channel'>\"+channels[log[m][1]]+\"</td>\");\n" + "\t\tdocument.write(\"<td class='source'>\"+sources[log[m][2]]+\"</td>\");\n" + "\t\tdocument.write(\"<td class='message'>\"+log[m][3]+\"</td>\");\n" + "\t\tdocument.write(\"</tr>\");\n" + "\t}");
        this.out.println("document.writeln(\"</table>\");");
        this.out.println("}");
    }

    /**
	 * Transform a string into a safe Javascript String,
	 * that can be passed to a function as an argument,
	 * between two quotes.
	 */
    protected String escape(String source) {
        return source.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"");
    }

    public void write(Message message) throws IOException {
        if (this.startDate == null) {
            writeHeader();
            this.startDate = message.getDate();
            this.lastDate = this.startDate;
        }
        long date = message.getDate().getTime() / 1000 - this.lastDate.getTime() / 1000;
        this.lastDate = message.getDate();
        String chan = message.getChannel();
        if (!this.channels.contains(chan)) this.channels.add(chan);
        String src = message.getAvatar();
        if (!this.sources.contains(src)) this.sources.add(src);
        this.out.println("\t[" + date + "," + this.channels.indexOf(chan) + "," + this.sources.indexOf(src) + ",\"" + escape(message.getContent()) + "\"],");
    }

    public void close() throws IOException {
        writeFooter();
        this.out.close();
    }

    public boolean commentsAreSupported() {
        return true;
    }

    public void writeComment(String comment) throws IOException, UnsupportedOperationException {
        this.out.println("/*" + comment + "*/");
    }
}
