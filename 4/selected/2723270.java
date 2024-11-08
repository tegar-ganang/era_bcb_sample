package net.sourceforge.ojb2hbm.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Parameter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HibernateConfigFileTask extends Task {

    private List mappingFilesets = new ArrayList();

    private File dest;

    private List properties = new ArrayList();

    private String factoryName;

    private List classCaches = new ArrayList();

    private List collectionCache = new ArrayList();

    private void checkParams() throws BuildException {
        if (dest == null) {
            throw new BuildException("You must specify a destination file for the hibernate configuration file.");
        } else if (mappingFilesets.size() == 0) {
            throw new BuildException("There must be at least one mapping element.");
        } else if (properties.size() > 0) {
            for (int i = 0; i < properties.size(); i++) {
                Parameter parameter = (Parameter) properties.get(i);
                if ((parameter.getName() == null) || (parameter.getValue() == null)) {
                    throw new BuildException("A property must have a name and a value.");
                }
            }
        } else if (classCaches.size() > 0) {
            for (int i = 0; i < classCaches.size(); i++) {
                ClassCache classCache = (ClassCache) this.classCaches.get(i);
                if (classCache.getClassname() == null || classCache.getFilesets().size() == 0) {
                    throw new BuildException("The classcache must have the classname attribute.");
                }
            }
        } else if (collectionCache.size() > 0) {
            for (int i = 0; i < collectionCache.size(); i++) {
                CollectionCache collectionCache = (CollectionCache) this.collectionCache.get(i);
                if (collectionCache.getCollection() == null) {
                    throw new BuildException("The collectioncache must have the collection attribute.");
                }
            }
        }
    }

    public void execute() throws BuildException {
        checkParams();
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = doc.createElement("hibernate-configuration");
            doc.appendChild(root);
            Element sessionRoot = doc.createElement("session-factory");
            if (factoryName != null) {
                sessionRoot.setAttribute("name", factoryName);
            }
            root.appendChild(sessionRoot);
            for (int i = 0; i < properties.size(); i++) {
                Parameter parameter = (Parameter) properties.get(i);
                Element element = doc.createElement("property");
                element.setAttribute("name", parameter.getName());
                Text textNode = doc.createTextNode(parameter.getValue());
                element.appendChild(textNode);
                sessionRoot.appendChild(element);
            }
            for (int i = 0; i < mappingFilesets.size(); i++) {
                FileSet fileSet = (FileSet) mappingFilesets.get(i);
                String files[] = fileSet.getDirectoryScanner(this.getProject()).getIncludedFiles();
                for (int j = 0; j < files.length; j++) {
                    String file = files[j];
                    Element mapping = doc.createElement("mapping");
                    mapping.setAttribute("resource", file.replace('\\', '/'));
                    sessionRoot.appendChild(mapping);
                }
            }
            for (int i = 0; i < classCaches.size(); i++) {
                ClassCache cc = (ClassCache) classCaches.get(i);
                if (cc.getClassname() != null) {
                    sessionRoot.appendChild(createClassCacheElement(doc, cc, cc.getClassname()));
                } else {
                    List fileSets = cc.getFilesets();
                    for (int j = 0; j < fileSets.size(); j++) {
                        FileSet fileSet = (FileSet) fileSets.get(j);
                        String files[] = fileSet.getDirectoryScanner(getProject()).getIncludedFiles();
                        for (int k = 0; k < files.length; k++) {
                            String file = files[k];
                            sessionRoot.appendChild(createClassCacheElement(doc, cc, stripExtension(file.replace('\\', '.'))));
                        }
                    }
                }
            }
            for (int i = 0; i < collectionCache.size(); i++) {
                CollectionCache cc = (CollectionCache) collectionCache.get(i);
                Element element = doc.createElement("collection-cache");
                element.setAttribute("collection", cc.getCollection());
                element.setAttribute("region", cc.getRegion());
                element.setAttribute("usage", cc.getUsage());
                sessionRoot.appendChild(element);
            }
            TransformerFactory tranFactory = TransformerFactory.newInstance();
            Transformer aTransformer = tranFactory.newTransformer();
            aTransformer.setOutputProperty(javax.xml.transform.OutputKeys.DOCTYPE_PUBLIC, "-//Hibernate/Hibernate Configuration DTD//EN");
            aTransformer.setOutputProperty(javax.xml.transform.OutputKeys.DOCTYPE_SYSTEM, "http://hibernate.sourceforge.net/hibernate-configuration-2.0.dtd");
            aTransformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            Source src = new DOMSource(doc);
            Result result = new StreamResult(dest);
            aTransformer.transform(src, result);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (FactoryConfigurationError factoryConfigurationError) {
            factoryConfigurationError.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    private String stripExtension(String name) {
        int extensionIndex;
        if ((extensionIndex = name.indexOf(".hbm.xml")) > 0) {
            name = name.substring(0, extensionIndex);
        } else {
            name = name.substring(0, name.lastIndexOf("."));
        }
        return name;
    }

    protected Element createClassCacheElement(Document doc, ClassCache classCache, String actualClassName) {
        Element element = doc.createElement("class-cache");
        element.setAttribute("class", actualClassName);
        element.setAttribute("region", classCache.getRegion());
        element.setAttribute("usage", classCache.getUsage());
        return element;
    }

    public void setFactoryName(String factoryName) {
        this.factoryName = factoryName;
    }

    public void addSFProperty(Parameter param) {
        properties.add(param);
    }

    public void addMapping(FileSet fileset) {
        mappingFilesets.add(fileset);
    }

    public void addClassCache(ClassCache jcc) {
        classCaches.add(jcc);
    }

    public void addCollectionCache(CollectionCache jcc) {
        collectionCache.add(jcc);
    }

    public void setDest(File dest) {
        this.dest = dest;
    }

    public static class Usage extends EnumeratedAttribute {

        public String[] getValues() {
            return new String[] { "read-only", "read-write", "nonstrict-read-write" };
        }
    }
}
