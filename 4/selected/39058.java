package org.eoti.mimir;

import org.eoti.spec.mimirdb.v1.DBMapping;
import org.eoti.xml.NamespacePrefixMapper;
import org.eoti.xml.NamespacePrefixMapperFactory;
import javax.xml.bind.*;
import static javax.xml.bind.Marshaller.JAXB_ENCODING;
import static javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import javax.xml.namespace.QName;
import java.io.*;
import java.net.URL;

/**
 * This class represents the underlying context responsible for reading/writing the database entries
 */
public class MimirContext<DATA, ID> {

    protected MimirRegistry registry;

    protected JAXBContext jaxb;

    protected String indentString = "  ";

    public MimirContext(MimirRegistry registry) {
        this.registry = registry;
    }

    public void clear() {
        jaxb = null;
    }

    protected JAXBContext getContext() throws MimirException {
        if (jaxb != null) return jaxb;
        StringBuilder sb = new StringBuilder();
        boolean firstMapping = true;
        for (DBMapping mapping : registry.getPrefixMapper().getMappings()) {
            String specPackage = mapping.getSpecPackage();
            if (specPackage == null) continue;
            if (!firstMapping) sb.append(":");
            sb.append(specPackage);
            firstMapping = false;
        }
        try {
            jaxb = JAXBContext.newInstance(sb.toString(), this.getClass().getClassLoader());
        } catch (JAXBException e) {
            throw new MimirException(registry, e);
        }
        return jaxb;
    }

    public Unmarshaller createUnmarshaller() throws MimirException {
        try {
            return getContext().createUnmarshaller();
        } catch (JAXBException e) {
            throw new MimirException(registry, e);
        }
    }

    public Marshaller createMarshaller() throws MimirException {
        JAXBContext ctx = getContext();
        try {
            Marshaller marshaller = ctx.createMarshaller();
            try {
                NamespacePrefixMapper npm = NamespacePrefixMapperFactory.getInstance(registry.getPrefixMapper());
                try {
                    marshaller.setProperty(npm.getMapperPropertyName(), npm);
                } catch (JAXBException ignorable) {
                }
                try {
                    marshaller.setProperty(npm.getIndentPropertyName(), indentString);
                } catch (JAXBException ignorable) {
                }
            } catch (Exception e) {
            }
            try {
                marshaller.setProperty(JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            } catch (JAXBException ignorable) {
            }
            marshaller.setProperty(JAXB_ENCODING, "UTF-8");
            return marshaller;
        } catch (Exception e) {
            throw new MimirException(registry, e);
        }
    }

    public DATA read(File file) throws MimirException {
        if (file.lastModified() == 0) return null;
        try {
            return (DATA) ((JAXBElement) createUnmarshaller().unmarshal(file)).getValue();
        } catch (JAXBException e) {
            throw new MimirException(registry, e);
        }
    }

    public DATA read(URL url) throws MimirException {
        try {
            return (DATA) ((JAXBElement) createUnmarshaller().unmarshal(url)).getValue();
        } catch (JAXBException e) {
            throw new MimirException(registry, e);
        }
    }

    public DATA read(InputStream stream) throws MimirException {
        try {
            return (DATA) ((JAXBElement) createUnmarshaller().unmarshal(stream)).getValue();
        } catch (JAXBException e) {
            throw new MimirException(registry, e);
        }
    }

    public DATA read(String fullyQualifiedResourceName) throws MimirException {
        URL url = this.getClass().getClassLoader().getResource(fullyQualifiedResourceName);
        if (url == null) throw new MimirException(registry, String.format("Resource not found: %s", fullyQualifiedResourceName));
        return read(url);
    }

    public void write(File file, DATA data, DBMapping mapping) throws MimirException {
        file.getParentFile().mkdirs();
        try {
            write(new FileOutputStream(file), data, mapping);
        } catch (IOException e) {
            throw new MimirException(registry, e);
        }
    }

    public void write(OutputStream out, DATA data, DBMapping mapping) throws MimirException {
        try {
            QName qname = new QName(mapping.getNamespaceURI(), mapping.getNamespacePrefix());
            JAXBElement<DATA> element = new JAXBElement<DATA>(qname, (Class<DATA>) data.getClass(), null, data);
            createMarshaller().marshal(element, out);
        } catch (JAXBException e) {
            throw new MimirException(registry, e);
        }
    }

    public void display(File file, DBMapping mapping) throws MimirException {
        display(file, System.out, mapping);
    }

    public void display(File file, PrintStream out, DBMapping mapping) throws MimirException {
        write(out, read(file), mapping);
    }
}
