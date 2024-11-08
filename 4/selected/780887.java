package uk.ac.manchester.cs.snee.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Utils {

    static Logger logger = Logger.getLogger(Utils.class.getName());

    /**
	 * Given a relative or absolute filename, ensures file exists.
	 * @param Property name used to specify a file location
	 * @return Full file path is returned
	 * @throws UtilsException 
	 */
    public static String validateFileLocation(String filename) throws UtilsException {
        if (logger.isTraceEnabled()) logger.trace("ENTER validateFileLocation() with " + filename);
        File file = new File(filename);
        if (!file.exists()) {
            logger.trace("Absolute file location failed, testing relative path");
            URL fileUrl = Utils.class.getClassLoader().getResource(filename);
            try {
                file = new File(fileUrl.toURI());
            } catch (Exception e) {
                String message = "Problem reading " + filename + " location. Ensure proper path. " + file;
                logger.warn(message, e);
                throw new UtilsException(message, e);
            }
        }
        if (!file.exists()) {
            String message = "File location " + "specified for " + filename + " does not exist. " + "Please provide a valid location.";
            logger.warn(message);
            throw new UtilsException(message);
        }
        String filePath = file.getAbsolutePath();
        if (logger.isTraceEnabled()) logger.trace("RETURN validateFileLocation() with " + filePath);
        return filePath;
    }

    public static void checkDirectory(String name, boolean createIfNonExistent) throws IOException {
        File f = new File(name);
        if ((f.exists()) && (!f.isDirectory())) {
            throw new IOException("Directory " + name + "already exists but is not a directory");
        }
        if ((!f.exists() && (!createIfNonExistent))) {
            throw new IOException("Directory " + name + " does not exist");
        }
        if (!f.exists() && (createIfNonExistent)) {
            boolean success = f.mkdirs();
            if (!success) {
                throw new IOException("Directory " + name + " does not exist and cannot be created");
            }
        }
    }

    /**
	 * Delete all the folders and subdirectories of the given directory
	 * @param path
	 * @return
	 */
    public static boolean deleteDirectoryContents(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectoryContents(files[i]);
                    files[i].delete();
                } else {
                    files[i].delete();
                }
            }
            return true;
        }
        return false;
    }

    /**
	 * Delete the contents of a directory
	 * @param pathName directory name 
	 * @return
	 */
    public static boolean deleteDirectoryContents(String pathName) {
        File path = new File(pathName);
        return deleteDirectoryContents(path);
    }

    public static int divideAndRoundUp(long dividend, int divisor) {
        if ((dividend % (long) divisor) == 0) return (int) (dividend / (long) divisor);
        return (int) (dividend / (long) divisor + 1);
    }

    /**
	 * Pad a string to a specified length
	 * @param s
	 * @param n
	 * @return
	 */
    public static String pad(String s, int n) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < n; i++) {
            result.append(s);
        }
        return result.toString();
    }

    public static String indent(int i) {
        return pad("\t", i);
    }

    public static void validateXMLFile(String filename, String schemaFile) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = parser.parse(new File(filename));
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(new File(schemaFile));
        Validator validator = schema.newValidator();
        validator.validate(new DOMSource(document));
    }

    public static String doXPathStrQuery(String xmlFile, String query) throws XPathExpressionException, FileNotFoundException {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        File xmlDocument = new File(xmlFile);
        InputSource inputSource = new InputSource(new FileInputStream(xmlDocument));
        xPath.setNamespaceContext(new SNEENamespaceContext());
        String result = xPath.evaluate(query, inputSource);
        if (result.equals("")) return null; else return result;
    }

    public static String doXPathStrQuery(Node node, String query) throws XPathExpressionException, FileNotFoundException {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        xPath.setNamespaceContext(new SNEENamespaceContext());
        String result = (String) xPath.evaluate(query, node, XPathConstants.STRING);
        return result;
    }

    public static int doXPathIntQuery(String xmlFile, String query) throws XPathExpressionException, FileNotFoundException {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        File xmlDocument = new File(xmlFile);
        InputSource inputSource = new InputSource(new FileInputStream(xmlDocument));
        xPath.setNamespaceContext(new SNEENamespaceContext());
        String result = xPath.evaluate(query, inputSource);
        if (result.equals("")) return -1; else return Integer.parseInt(result);
    }

    public int doXPathIntQuery(Node node, String query) throws XPathExpressionException, FileNotFoundException {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        xPath.setNamespaceContext(new SNEENamespaceContext());
        int result = (Integer) xPath.evaluate(query, node, XPathConstants.NUMBER);
        return result;
    }

    public static NodeList doXPathQuery(String xmlFile, String query) throws XPathExpressionException, FileNotFoundException {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        File xmlDocument = new File(xmlFile);
        InputSource inputSource = new InputSource(new FileInputStream(xmlDocument));
        xPath.setNamespaceContext(new SNEENamespaceContext());
        NodeList result = (NodeList) xPath.evaluate(query, inputSource, XPathConstants.NODESET);
        return result;
    }

    /**
	 * Capitalise the first letter of a string
	 * @param str
	 * @return
	 */
    public static String capFirstLetter(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1, str.length());
    }

    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    /**
	 * Runs an external program. Waits until it has finished executing.
	 * @param progName
	 * @param params
	 * @param env
	 * @throws IOException
	 */
    public static String runExternalProgram(String progName, String[] params, Map<String, String> extraEnvVars, String workingDir) throws IOException {
        if (logger.isDebugEnabled()) logger.debug("ENTER runExternalProgram()");
        String cmdarray[] = concat(new String[] { progName }, params);
        logger.info("Command array=" + Arrays.toString(cmdarray));
        ProcessBuilder pb = new ProcessBuilder(cmdarray);
        pb.redirectErrorStream(true);
        pb.directory(new File(workingDir));
        Map<String, String> env = pb.environment();
        env.putAll(extraEnvVars);
        Process proc = pb.start();
        final InputStream is = proc.getInputStream();
        final InputStreamReader isr = new InputStreamReader(is);
        final BufferedReader br = new BufferedReader(isr);
        String line;
        StringBuffer output = new StringBuffer();
        while ((line = br.readLine()) != null) {
            logger.trace(line);
            System.out.println(line);
            output.append(line + "\n");
        }
        try {
            proc.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (proc.exitValue() != 0) {
            System.err.println("an error has occurred");
            System.exit(-1);
        }
        if (logger.isDebugEnabled()) logger.debug("RETURN runExternalProgram()");
        return output.toString();
    }

    /**
	 * Given a resource in the class loader search path, returns an absolute path to this resource.
	 * @param relativeResourcePath
	 * @return
	 */
    public static String getResourcePath(String relativeResourcePath) {
        if (logger.isDebugEnabled()) logger.debug("ENTER getResourcePath()");
        URL url = Utils.class.getClassLoader().getResource(relativeResourcePath);
        File file = new File(url.getFile());
        if (logger.isDebugEnabled()) logger.debug("RETURN getResourcePath()");
        return file.toString();
    }

    public static String readFileToString(String path) throws IOException {
        FileInputStream stream = new FileInputStream(new File(path));
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            return Charset.defaultCharset().decode(bb).toString();
        } finally {
            stream.close();
        }
    }

    public static int[] hashset_to_int_array(Set<Integer> hs) {
        int array[] = new int[hs.size()];
        Iterator<Integer> itr = hs.iterator();
        int i = 0;
        while (itr.hasNext()) {
            array[i++] = Integer.parseInt(itr.next().toString());
        }
        return array;
    }
}
