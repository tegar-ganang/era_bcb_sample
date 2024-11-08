package net.sf.jvibes.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import net.sf.jvibes.JVibes;
import net.sf.jvibes.kernel.elements.Element;
import net.sf.jvibes.kernel.elements.Model;
import net.sf.jvibes.kernel.elements.Property;
import net.sf.jvibes.kernel.elements.Element.Category;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class Save {

    private static final Logger __logger = Logger.getLogger(Save.class);

    public static void save(Model system, Writer writer) throws IOException {
        org.jdom.Element root = createTag(system);
        List<Element> bodies = system.getElements(Category.Body);
        List<Element> links = system.getElements(Category.Link);
        org.jdom.Element elementsTag = new org.jdom.Element("elements");
        for (Element e : bodies) {
            org.jdom.Element elementTag = createTag(e);
            elementTag.setAttribute("id", String.valueOf(bodies.indexOf(e)));
            elementsTag.addContent(elementTag);
        }
        for (Element e : links) {
            org.jdom.Element elementTag = createTag(e);
            elementTag.setAttribute("id", String.valueOf(links.indexOf(e)));
            elementsTag.addContent(elementTag);
        }
        root.addContent(elementsTag);
        Document d = new Document(root);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(d, writer);
    }

    private static org.jdom.Element createTag(Element e) {
        org.jdom.Element tag = new org.jdom.Element("element");
        tag.setAttribute("category", e.getCategory().toString());
        List<String> properties = e.getProperties();
        for (String pName : properties) {
            Property property = e.getProperty(pName);
            Object value = property.getValue();
            if (value == null) continue;
            PropertyHandler handler = PropertyHandlers.getHandler(value.getClass().getName());
            if (handler == null) {
                __logger.warn("Couldn't get handler for property '" + pName + "' (class: " + value.getClass() + ")");
                continue;
            }
            org.jdom.Element pElement = new org.jdom.Element("property");
            pElement.setAttribute("name", pName);
            pElement.setAttribute("class", value.getClass().getName());
            handler.write(value, pElement);
            tag.addContent(pElement);
        }
        return tag;
    }

    public static File saveAs(Model model) {
        File modelsFile = (File) model.getPropertyValue(Model.PROPERTY_FILE);
        FileChooser fc = FileChooser.getDataInstance();
        int result = fc.showSaveDialog(JVibes.getFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!f.getAbsolutePath().toLowerCase().endsWith(".jvm")) f = new File(f.getAbsolutePath() + ".jvm");
            if (f.exists() && !f.equals(modelsFile)) {
                result = JOptionPane.showConfirmDialog(JVibes.getFrame(), "File already exists. Overwrite?");
                if (result != JOptionPane.OK_OPTION) return null;
            }
            save(model, f);
            return f;
        }
        return null;
    }

    public static void save(Model model, File f) {
        if (!f.canWrite()) __logger.error("Cannot write to '" + f.getAbsolutePath() + " '.");
        FileWriter writer = null;
        try {
            writer = new FileWriter(f);
            Save.save(model, writer);
            model.setProperty(Model.PROPERTY_FILE, f);
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
