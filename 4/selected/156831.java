package org.neblipedia.wiki.cache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.neblipedia.wiki.Encabezado;
import org.neblipedia.wiki.mediawiki.Articulo;
import org.xml.sax.helpers.AttributesImpl;

public class CacheXmlGuardar {

    private static final String VALOR = "valor";

    private File path;

    private Articulo str;

    public CacheXmlGuardar(File path, Articulo str) {
        super();
        this.path = path;
        this.str = str;
        guardarXml();
    }

    private void guardarXml() {
        try {
            Encabezado enc = str.getEncabezado();
            FileOutputStream fos = new FileOutputStream(path);
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            XMLStreamWriter writer = factory.createXMLStreamWriter(fos);
            writer.writeStartDocument("utf-8", "1.0");
            writer.writeStartElement("articulo");
            writer.writeStartElement("encabezado");
            tag(writer, "javascriptarchivos", enc.getJavascriptArchivos());
            tag(writer, "domready", enc.getDomReady());
            tag(writer, "cssarchivos", enc.getCssArchivos());
            writer.writeEndElement();
            tag(writer, "titulo", str.getTitulo());
            tag(writer, "wiki", str.getDb().getNombre());
            tag(writer, "id", str.getId() + "");
            tag(writer, "namespace", str.getNamespaceId() + "");
            tag(writer, "fecha", str.getFecha().getTime() + "");
            writer.writeStartElement("contenido");
            String c = str.getContenido().toString();
            writer.writeCharacters(c);
            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeEndDocument();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        } catch (FactoryConfigurationError e) {
            e.printStackTrace();
        }
    }

    private void tag(XMLStreamWriter writer, String nombre, List<String> cont) {
        try {
            writer.writeStartElement(nombre);
            for (String s : cont) {
                tag(writer, "propiedad", s);
            }
            writer.writeEndElement();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }

    private void tag(XMLStreamWriter hd, String nombre, String cont) {
        try {
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, null, VALOR, "string", cont);
            hd.writeEmptyElement(nombre);
            hd.writeAttribute(VALOR, cont);
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }
}
