package eu.fbk.hlt.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * @author Milen Kouylekov
 */
public class FileTools {

    public static void checkInput(String filename) throws EDITSException {
        File file = new File(filename);
        if (!file.exists()) throw new EDITSException("The file " + filename + " does not exist!");
        if (!file.canRead()) throw new EDITSException("The system can not read from file " + filename + "!");
    }

    public static void checkOutput(String filename, boolean overwrite) throws EDITSException {
        File file = new File(filename);
        if (!overwrite && file.exists()) throw new EDITSException("The file " + filename + " already exists!");
        if (file.exists() && (file.isDirectory() || !file.canWrite())) throw new EDITSException("The system can not write in " + filename + "!");
    }

    public static Marshaller header(String filename, String classPath, boolean overwrite) throws EDITSException, JAXBException {
        FileTools.checkOutput(filename, overwrite);
        Marshaller marshaller = JAXBContext.newInstance(classPath).createMarshaller();
        marshaller.setProperty("jaxb.formatted.output", true);
        return marshaller;
    }

    @SuppressWarnings("unchecked")
    public static Object loadObject(String filename, String classpath) throws EDITSException {
        try {
            FileTools.checkInput(filename);
            Unmarshaller u = JAXBContext.newInstance(classpath).createUnmarshaller();
            Object o = u.unmarshal(new File(filename));
            if (!(o instanceof JAXBElement)) throw new EDITSException("The file " + filename + " is not in the correct format!");
            JAXBElement<Object> el = (JAXBElement<Object>) o;
            return el.getValue();
        } catch (JAXBException e) {
            e.printStackTrace();
            throw new EDITSException("The file " + filename + " is not in the correct format!");
        }
    }

    public static String loadString(String filename) throws EDITSException {
        return loadString(filename, "UTF8");
    }

    public static String loadString(String filename, String encoding) throws EDITSException {
        try {
            checkInput(filename);
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filename), encoding));
            String line = null;
            StringBuilder vud = new StringBuilder();
            while ((line = in.readLine()) != null) {
                vud.append(line + "\n");
            }
            in.close();
            return vud.toString();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new EDITSException("The file " + filename + " is not in the correct format!");
        } catch (IOException e) {
            throw new EDITSException("The file " + filename + " is not in the correct format!");
        }
    }

    public static void main(String[] args) throws EDITSException {
        saveString("/tcc0/tcc/kouylekov/.edits/txi", "eeeeeeeeeeeeeee", true);
    }

    public static void saveString(String filename, String s, boolean overwrite) throws EDITSException {
        saveString(filename, s, overwrite, "UTF-8");
    }

    public static void saveString(String filename, String s, boolean overwrite, String encoding) throws EDITSException {
        try {
            checkOutput(filename, overwrite);
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), encoding));
            out.write(s);
            out.close();
        } catch (IOException e) {
            throw new EDITSException("The system can not write in " + filename + " because:\n" + e.getMessage());
        }
    }
}
