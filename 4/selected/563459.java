package cn.shining365.webclips.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.io.IOUtils;

public class Utils4Test {

    public static byte[] readFile(String filePath) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        FileInputStream is = new FileInputStream(filePath);
        try {
            IOUtils.copy(is, os);
            return os.toByteArray();
        } finally {
            is.close();
        }
    }

    public static void writeFile(byte[] data, String filePath) throws IOException {
        FileOutputStream is = new FileOutputStream(filePath);
        try {
            is.write(data);
        } finally {
            is.close();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T unmarshall(Class<T> clazz, String xml) throws JAXBException, IOException {
        JAXBContext jc = JAXBContext.newInstance(clazz.getPackage().getName());
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        ByteArrayInputStream xmlStream = new ByteArrayInputStream(xml.getBytes());
        T jaxbObject = (T) unmarshaller.unmarshal(xmlStream);
        return jaxbObject;
    }

    public static <T> String marshall(T jaxbObject) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(jaxbObject.getClass().getPackage().getName());
        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty("jaxb.formatted.output", true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshaller.marshal(jaxbObject, out);
        return new String(out.toByteArray());
    }

    public static <T> T unmarshallFromFile(Class<T> clazz, String filePath) throws JAXBException, IOException {
        return unmarshall(clazz, new String(readFile(filePath)));
    }

    public static <T> void marshallToFile(T jaxbObject, String filePath) throws JAXBException, IOException {
        writeFile(marshall(jaxbObject).getBytes(), filePath);
    }
}
