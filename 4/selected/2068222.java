package edu.psu.its.lionshare.gui.nmd.custom;

import edu.psu.its.lionshare.gui.nmd.MetadataDisplay;
import edu.psu.its.lionshare.gui.nmd.NMDException;
import edu.psu.its.lionshare.metadata.MetadataUtility;
import edu.psu.its.lionshare.metadata.MetadataManager;
import edu.psu.its.lionshare.metadata.MetadataType;
import com.limegroup.gnutella.util.CommonUtils;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.limegroup.gnutella.gui.GUIMediator;
import edu.psu.its.lionshare.gui.nmd.DescriptionEditor;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;
import java.util.Iterator;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

public class CustomizableMetadataDisplay implements MetadataDisplay {

    private static final Log LOG = LogFactory.getLog(CustomizableMetadataDisplay.class);

    private static final String ADD_ELEMENT_LABEL = "Add Element";

    private static final String REMOVE_SELECTED_LABEL = "Remove Selected Elements";

    private List<MetadataElementDisplay> displays = new ArrayList<MetadataElementDisplay>();

    private List<MetadataElementDisplay> filled_displays = null;

    private final Map<String, HashMap> uri_elements = Collections.synchronizedMap(new HashMap());

    private Object document_id = null;

    private String ontology_uri = null;

    private MetadataManager manager = null;

    private JPanel editor_panel = null;

    public CustomizableMetadataDisplay() {
        File nmd_dir = new File(CommonUtils.getUserSettingsDir(), "nmd");
        File available_component_config = new File(nmd_dir, ".components.xml");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new FileInputStream(available_component_config));
            Element root = doc.getDocumentElement();
            NodeList list = root.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node child = list.item(i);
                if (child.getNodeName().equals("component")) {
                    NamedNodeMap map = child.getAttributes();
                    String uri = null;
                    String element = null;
                    String component = null;
                    for (int j = 0; j < map.getLength(); j++) {
                        Node attr = map.item(j);
                        if (attr.getNodeName().equals("schema_uri")) {
                            uri = attr.getNodeValue();
                        } else if (attr.getNodeName().equals("element_name")) {
                            element = attr.getNodeValue();
                        } else if (attr.getNodeName().equals("component")) {
                            component = attr.getNodeValue();
                        }
                    }
                    if (uri != null && element != null && component != null) {
                        HashMap emap = (HashMap) uri_elements.get(uri);
                        if (emap == null) {
                            emap = new HashMap();
                            uri_elements.put(uri, emap);
                        }
                        try {
                            Class clazz = Class.forName(component);
                            Object instance = clazz.newInstance();
                            emap.put(element, instance);
                        } catch (Exception e) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.trace("", e);
        }
    }

    public void setEditable(boolean edit) {
        for (int i = 0; i < displays.size(); i++) {
            displays.get(i).setEditable(edit);
        }
    }

    public boolean isEditable() {
        return false;
    }

    public JComponent getMetadataEditor() throws NMDException {
        if (document_id == null && ontology_uri == null) {
            throw new NMDException("You must Initialize the display by calling" + "setDocumentId() or setOntologyURI() first");
        }
        editor_panel = new JPanel();
        editor_panel.setLayout(new BoxLayout(editor_panel, BoxLayout.Y_AXIS));
        for (int i = 0; i < displays.size(); i++) {
            editor_panel.add(new MetadataElementDisplayDivider(displays.get(i)));
        }
        return new JScrollPane(editor_panel);
    }

    public JComponent getOntologyEditorComponent() {
        editor_panel = new JPanel();
        editor_panel.setLayout(new BoxLayout(editor_panel, BoxLayout.Y_AXIS));
        for (int i = 0; i < displays.size(); i++) {
            editor_panel.add(new MetadataElementDisplayDivider(displays.get(i), true));
        }
        return new JScrollPane(editor_panel);
    }

    public void saveDocument() throws NMDException {
        try {
            Model model = ModelFactory.createDefaultModel();
            String uri = "http://localhost/" + document_id;
            Resource root = model.createResource(uri);
            Iterator<HashMap> iterator = uri_elements.values().iterator();
            while (iterator.hasNext()) {
                HashMap emap = iterator.next();
                Iterator iter = emap.values().iterator();
                while (iter.hasNext()) {
                    MetadataElementDisplay display = (MetadataElementDisplay) iter.next();
                    Object value = display.getValue(model);
                    if (value != null) {
                        root.addProperty(display.getProperty(), value);
                    }
                }
            }
            MetadataType meta = manager.getMetadata(document_id);
            if (meta != null) {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                model.write(out, "RDF/XML-ABBREV");
                meta.setMetadata(new String(out.toByteArray()));
            }
            manager.updateMetadata(meta, document_id);
        } catch (Exception e) {
            LOG.trace("", e);
            throw new NMDException("Unable to save metadata");
        }
    }

    public void setMetadataManager(MetadataManager manager) {
        this.manager = manager;
    }

    public void removeSelectedOntologyElements() {
        Component[] childs = editor_panel.getComponents();
        for (int i = 0; i < childs.length; i++) {
            if (childs[i] instanceof MetadataElementDisplayDivider) {
                if (((MetadataElementDisplayDivider) childs[i]).isSelected()) {
                    MetadataElementDisplay disp = getDisplayForName(((MetadataElementDisplayDivider) childs[i]).getSchemaUri(), ((MetadataElementDisplayDivider) childs[i]).getElementName());
                    displays.remove(disp);
                }
            }
        }
    }

    public JDialog createOntologyElementSelectionDialog(JDialog parent) {
        final JDialog dialog = new JDialog(parent, "Element Chooser", true);
        dialog.setSize(350, 150);
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setLocation((dim.width - 350) / 2, (dim.height - 150) / 2);
        dialog.getContentPane().setLayout(new BorderLayout());
        ArrayList<MetadataElementDisplay> selections = new ArrayList<MetadataElementDisplay>();
        Iterator<HashMap> iter = uri_elements.values().iterator();
        while (iter.hasNext()) {
            selections.addAll(iter.next().values());
        }
        final JComboBox box = new JComboBox(selections.toArray(new MetadataElementDisplay[0]));
        JPanel panel = new JPanel();
        panel.add(box);
        dialog.getContentPane().add(panel, BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        JButton select = new JButton("Select");
        select.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                displays.add((MetadataElementDisplay) box.getSelectedItem());
                dialog.dispose();
            }
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        buttons.add(select);
        buttons.add(cancel);
        dialog.getContentPane().add(buttons, BorderLayout.SOUTH);
        return dialog;
    }

    public void addOntologyElement(String schema_uri, String name) {
        MetadataElementDisplay disp = getDisplayForName(schema_uri, name);
        if (disp != null) {
            displays.add(disp);
        }
    }

    public void setOntologyURI(String ontology_uri) {
        this.ontology_uri = ontology_uri;
        if (ontology_uri == null || ontology_uri.equals("")) {
            return;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            displays = new ArrayList<MetadataElementDisplay>();
            if (manager.getOntologyType(ontology_uri) == null) {
                if (this.document_id != null) setDocumentId(this.document_id);
                return;
            }
            String ontology = manager.getOntologyType(ontology_uri).getOntology();
            Document doc = builder.parse(new ByteArrayInputStream(ontology.getBytes()));
            Element root = doc.getDocumentElement();
            NodeList list = root.getChildNodes();
            for (int e = 0; e < list.getLength(); e++) {
                Node child = list.item(e);
                if (child.getNodeName().equals("component")) {
                    NamedNodeMap map = child.getAttributes();
                    String uri = null;
                    String element = null;
                    for (int j = 0; j < map.getLength(); j++) {
                        Node attr = map.item(j);
                        if (attr.getNodeName().equals("schema_uri")) {
                            uri = attr.getNodeValue();
                        } else if (attr.getNodeName().equals("element")) {
                            element = attr.getNodeValue();
                        }
                    }
                    if (uri != null && element != null) {
                        MetadataElementDisplay disp = getDisplayForName(uri, element);
                        if (disp != null) {
                            displays.add(disp);
                        }
                    }
                }
            }
        } catch (Exception error) {
            LOG.trace("", error);
        }
    }

    public Object getDocumentId() {
        return document_id;
    }

    public void setDocumentId(Object id) {
        this.document_id = id;
        try {
            MetadataType meta = manager.getMetadata(document_id);
            if (meta.getMetadata() == null || meta.getMetadata().equals("")) {
                return;
            }
            Model model = ModelFactory.createDefaultModel();
            StringReader reader = new StringReader(meta.getMetadata());
            model.read(reader, "");
            ResIterator iterator = model.listSubjects();
            while (iterator.hasNext()) {
                Resource resource = iterator.nextResource();
                Iterator<HashMap> iter = uri_elements.values().iterator();
                while (iter.hasNext()) {
                    Iterator<MetadataElementDisplay> disps = iter.next().values().iterator();
                    while (disps.hasNext()) {
                        MetadataElementDisplay disp = disps.next();
                        if (resource.getProperty(disp.getProperty()) != null) {
                            if (!resource.getProperty(disp.getProperty()).getObject().toString().equals("")) {
                                disp.setValue(resource.getProperty(disp.getProperty()).getObject());
                                if (ontology_uri == null || ontology_uri.equals("")) displays.add(disp);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.trace("", e);
        }
    }

    public String getOntologyURI() {
        return ontology_uri;
    }

    public boolean saveCurrentOntology(String ontology_name) {
        if (!ontology_name.equals(DescriptionEditor.DUBLIN_CORE_DEFAULT_NAME)) {
            String ont_uri = "http://lionshare.its.psu.edu/" + ontology_name;
            String ontology = "<ontology name=\"" + ontology_name + "\" " + "uri=\"" + ont_uri + "\" " + "display=\"" + this.getClass().getName() + "\">";
            for (int i = 0; i < displays.size(); i++) {
                ontology += "<component schema_uri=\"" + displays.get(i).getSchemaURI() + "\"" + " element=\"" + displays.get(i).getElementName() + "\"/>";
            }
            ontology += "</ontology>";
            try {
                manager.addOntology(ontology_name, ontology, false);
            } catch (IOException ioex) {
                LOG.trace("CustomizableMetadataDisplay.saveCurrentOntology(): " + "Ontology already exists", ioex);
                int nChoice = JOptionPane.showConfirmDialog(GUIMediator.getAppFrame(), "<html>The ontology with that name already exists, would you like to " + "overwrite it?<br>Either click Yes to overwrite the file or No to go " + "back to the editor to<br>enter a different ontology name.</html>", "Ontology Exists", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (nChoice == JOptionPane.YES_OPTION) {
                    try {
                        manager.addOntology(ontology_name, ontology, true);
                    } catch (IOException ioex2) {
                        LOG.trace("CustomizableMetadataDisplay.saveCurrentOntology(): " + "Error overwriting ontology", ioex2);
                    }
                } else {
                    return false;
                }
            }
            try {
                MetadataType meta = manager.getMetadata(document_id);
                meta.setOntologyURI(ont_uri);
                manager.updateMetadata(meta, document_id);
            } catch (Exception ex) {
                LOG.trace("CustomizableMetadataDisplay.saveCurrentOntology(): " + "Error updating metadata.", ex);
            }
            return true;
        } else {
            JOptionPane.showMessageDialog(GUIMediator.getAppFrame(), "<html>The default dublic core ontology cannot be overwritten.</html>", "Cannot Overwrite Default Dublin Core Ontology", JOptionPane.WARNING_MESSAGE);
            return false;
        }
    }

    private void addMetadataElementDisplay(MetadataElementDisplay display) {
    }

    public Document getDocument() {
        return null;
    }

    public MetadataElementDisplay getDisplayForName(String schema_uri, String element_name) {
        HashMap map = (HashMap) uri_elements.get(schema_uri);
        MetadataElementDisplay display = null;
        if (map != null) {
            display = (MetadataElementDisplay) map.get(element_name);
        }
        return display;
    }

    public void setDocument(Document rdf_document) {
    }

    public static void main(String args[]) {
        try {
            String metadata = "<?xml version=\"1.0\"?><!DOCTYPE rdf:RDF SYSTEM \"http://dublincore.org/documents/2002/07/31/dcmes-xml/dcmes-xml-dtd.dtd\"><rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\"> <rdf:Description rdf:about=\"http://lionshare.its.psu.edu\"><dc:title>LionShare: redirecting..</dc:title><dc:publisher></dc:publisher><dc:date>2005-01-31</dc:date><dc:type>Text</dc:type></rdf:Description></rdf:RDF>";
            Document document = MetadataUtility.convertStringToDocument(metadata);
            CustomizableMetadataDisplay display = new CustomizableMetadataDisplay();
        } catch (Exception e) {
            LOG.trace("", e);
        }
    }
}
