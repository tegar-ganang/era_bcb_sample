package repast.simphony.agents.base;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import repast.simphony.agents.flows.tasks.AgentBuilderRuntimeException;

/**
 * @author greifa (Adapted by Michael J. North for Use in Repast Simphony from
 *         Alexander Greif�s Flow4J-Eclipse
 *         (http://flow4jeclipse.sourceforge.net/docs/index.html), with Thanks
 *         to the Original Author)
 * 
 * 
 * 
 */
public class IOUtils {

    /**
	 * TODO
	 * 
	 * @param in
	 *            TODO
	 * @param out
	 *            TODO
	 * @exception IOException
	 *                TODO
	 */
    public static void copyBufferedStream(InputStream in, OutputStream out) throws IOException {
        synchronized (in) {
            synchronized (out) {
                BufferedInputStream bin = new BufferedInputStream(in);
                BufferedOutputStream bout = new BufferedOutputStream(out);
                while (true) {
                    int data = bin.read();
                    if (data == -1) break;
                    bout.write(data);
                }
                bout.flush();
            }
        }
    }

    /**
	 * TODO
	 * 
	 * @param in
	 *            TODO
	 * @param out
	 *            TODO
	 * @exception IOException
	 *                TODO
	 */
    public static void copyChars(Reader in, Writer out) throws IOException {
        synchronized (in) {
            synchronized (out) {
                BufferedReader bin = new BufferedReader(in);
                BufferedWriter bout = new BufferedWriter(out);
                String line;
                while ((line = bin.readLine()) != null) {
                    bout.write(line);
                    bout.newLine();
                }
                bout.flush();
            }
        }
    }

    public static String getResourceAsString(ClassLoader loader, String path) throws AgentBuilderRuntimeException {
        java.io.InputStream in = null;
        java.io.Reader reader = null;
        java.io.StringWriter writer = null;
        try {
            in = loader.getResourceAsStream(path);
            reader = new java.io.InputStreamReader(in);
            writer = new java.io.StringWriter();
            repast.simphony.agents.base.IOUtils.copyChars(reader, writer);
            reader.close();
            writer.close();
        } catch (java.io.IOException e) {
            throw new AgentBuilderRuntimeException("Error while loading resource (" + path + ")", e);
        } finally {
            if (in != null) try {
                reader.close();
            } catch (java.io.IOException e) {
            }
            if (reader != null) try {
                reader.close();
            } catch (java.io.IOException e) {
            }
            if (writer != null) try {
                reader.close();
            } catch (java.io.IOException e) {
            }
        }
        return writer.toString();
    }
}
