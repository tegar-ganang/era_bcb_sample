package Modelo;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import java.util.*;
import java.io.*;
import java.net.URLEncoder;
import javax.swing.JOptionPane;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.NodeList;

/**
 *
 * @author Pellizard
 */
public class Ingredient {

    private String title;

    private int id;

    private int revision;

    private Vector authors;

    private Vector revisions;

    private String description;

    private Vector tags;

    private Vector steps;

    /**
     * Creates a new instance of Ingredient
     */
    public Ingredient(String titulo, int id, String introduccion, Vector tags, Vector steps, Vector authors, Vector revisions, int revision) {
        this.setTitle(titulo);
        this.setId(id);
        this.setAuthors(authors);
        this.setRevisions(revisions);
        this.setDescription(introduccion);
        this.setTags(tags);
        this.setSteps(steps);
        this.setRevision(revision);
    }

    public void save() {
        try {
            File f = new File(getFileName());
            boolean saveFile = true;
            if (f.exists()) {
                int res = Util.confirmDialog("File already exists", "You have already a local file with the same name.\nWould you like to overwrite it?");
                if (res == JOptionPane.CANCEL_OPTION) {
                    saveFile = false;
                }
            }
            if (saveFile) {
                OutputStream fout = new FileOutputStream(getFileName());
                TransformerFactory transFactory = TransformerFactory.newInstance();
                Transformer transformer = transFactory.newTransformer();
                DOMSource source = new DOMSource(this.toXml());
                StreamResult result = new StreamResult(fout);
                transformer.transform(source, result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String titulo) {
        this.title = titulo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String introduccion) {
        this.description = introduccion;
    }

    public Vector getTags() {
        return tags;
    }

    public void setTags(Vector tags) {
        this.tags = tags;
    }

    public Node toXml() {
        try {
            DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = fact.newDocumentBuilder();
            Document doc = parser.newDocument();
            return toXml(doc);
        } catch (Exception ex) {
            return null;
        }
    }

    public Node toXml(Document doc) {
        try {
            Node root = doc.createElement("ingredient");
            Node id = doc.createElement("id");
            id.appendChild(doc.createTextNode(String.valueOf(this.id)));
            root.appendChild(id);
            Node revision = doc.createElement("ingredientrevision");
            revision.appendChild(doc.createTextNode("" + this.revision));
            root.appendChild(revision);
            Node titulo = doc.createElement("title");
            titulo.appendChild(doc.createTextNode(this.title));
            root.appendChild(titulo);
            Node description = doc.createElement("description");
            description.appendChild(doc.createTextNode(this.description));
            root.appendChild(description);
            Node autores = doc.createElement("authors");
            for (int i = 0; i < this.getAuthors().size(); i++) {
                Author a = (Author) this.getAuthors().get(i);
                autores.appendChild(a.toXml(doc));
            }
            root.appendChild(autores);
            Node revisiones = doc.createElement("revisions");
            for (int j = 0; j < this.revisions.size(); j++) {
                Revision r = (Revision) this.revisions.get(j);
                revisiones.appendChild(r.toXml(doc));
            }
            root.appendChild(revisiones);
            Node steps = doc.createElement("steps");
            for (int j = 0; j < this.steps.size(); j++) {
                Step step = (Step) (this.steps.get(j));
                steps.appendChild(step.toXml(doc));
            }
            root.appendChild(steps);
            Node tags = doc.createElement("tags");
            for (int i = 0; i < this.tags.size(); i++) {
                String s = (String) this.tags.get(i);
                Node tag = doc.createElement("tag");
                tag.appendChild(doc.createTextNode(s));
                tags.appendChild(tag);
            }
            root.appendChild(tags);
            return root;
        } catch (Exception ex) {
            System.err.println("+============================+");
            System.err.println("|        XML Error           |");
            System.err.println("+============================+");
            System.err.println(ex.getClass());
            System.err.println(ex.getMessage());
            System.err.println("+============================+");
            ex.printStackTrace();
            return null;
        }
    }

    public static Ingredient fromXml(String str) {
        InputStream is;
        try {
            is = new ByteArrayInputStream(str.getBytes("UTF8"));
        } catch (Exception e) {
            return null;
        }
        try {
            DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = fact.newDocumentBuilder();
            Document doc = parser.parse(is);
            return fromXml(doc);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Ingredient fromXml(Document doc) {
        String title = "";
        int id = 0;
        int revision = 0;
        String description = "";
        Vector tags = new Vector();
        Vector steps = new Vector();
        Vector authors = new Vector();
        try {
            NodeList nodes = doc.getElementsByTagName("step");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                Step step = Step.fromXml(Util.node2String(node));
                if (step != null) steps.add(step);
            }
            nodes = doc.getElementsByTagName("tag");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                String t;
                try {
                    t = (String) node.getFirstChild().getNodeValue();
                } catch (Exception e) {
                    t = null;
                }
                if (t != null) {
                    tags.add(t);
                }
            }
            nodes = doc.getElementsByTagName("title");
            if (true) {
                Node node = nodes.item(0);
                try {
                    title = (String) node.getFirstChild().getNodeValue();
                } catch (Exception e) {
                    title = "";
                }
            }
            nodes = doc.getElementsByTagName("id");
            if (nodes.getLength() == 1) {
                Node node = nodes.item(0);
                try {
                    id = Integer.parseInt(node.getFirstChild().getNodeValue());
                } catch (Exception e) {
                    id = 0;
                    e.printStackTrace();
                }
            }
            nodes = doc.getElementsByTagName("ingredientrevision");
            if (nodes.getLength() == 1) {
                Node node = nodes.item(0);
                try {
                    revision = Integer.parseInt(node.getFirstChild().getNodeValue());
                } catch (Exception e) {
                    revision = 0;
                    e.printStackTrace();
                }
            }
            nodes = doc.getElementsByTagName("description");
            if (nodes.getLength() == 1) {
                Node node = nodes.item(0);
                description = node.getFirstChild().getNodeValue();
            }
            nodes = doc.getElementsByTagName("author");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node nodeAuthor = nodes.item(i);
                Author author = Author.fromXml(Util.node2String(nodeAuthor));
                if (author != null) {
                    authors.addElement(author);
                }
            }
            nodes = doc.getElementsByTagName("revision");
            Vector revisions = new Vector();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node nodeRevision = nodes.item(i);
                Revision rev = Revision.fromXml(Util.node2String(nodeRevision));
                if (rev != null) {
                    revisions.addElement(rev);
                }
            }
            return new Ingredient(title, id, description, tags, steps, authors, revisions, revision);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Vector getIngredientsByFileName(Vector filePaths) {
        Vector ingredients = new Vector();
        for (int i = 0; i < filePaths.size(); i++) {
            String ingredientXML = Util.readTextFile((String) filePaths.get(i));
            Ingredient ing = Ingredient.fromXml(ingredientXML);
            ingredients.add(ing);
        }
        return ingredients;
    }

    public Vector getSteps() {
        return steps;
    }

    public void setSteps(Vector steps) {
        this.steps = steps;
    }

    public String getFileName() {
        String dir = Util.getIngredientsDirectory();
        String file = this.getTitle() + "_" + ((Author) this.getAuthors().get(0)).getCompleteName() + "_" + this.revision + ".xml";
        try {
            String path = dir + URLEncoder.encode(file, "UTF8");
            return path;
        } catch (UnsupportedEncodingException e) {
            return "error";
        }
    }

    public Vector getAuthors() {
        return authors;
    }

    public void setAuthors(Vector authors) {
        this.authors = authors;
    }

    public Vector getRevisions() {
        return revisions;
    }

    public void addRevision(Revision revision) {
        revisions.add(revision);
    }

    public void setRevisions(Vector revisions) {
        this.revisions = revisions;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }
}
