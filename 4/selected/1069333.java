package nz.ac.waikato.mcennis.rat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import nz.ac.waikato.mcennis.rat.dataAquisition.DataAquisitionFactory;
import nz.ac.waikato.mcennis.rat.dataAquisition.DataAquisition;
import nz.ac.waikato.mcennis.rat.graph.Graph;
import nz.ac.waikato.mcennis.rat.graph.GraphFactory;
import nz.ac.waikato.mcennis.rat.graph.actor.Actor;
import nz.ac.waikato.mcennis.rat.graph.actor.ActorFactory;
import nz.ac.waikato.mcennis.rat.graph.algorithm.AlgorithmFactory;
import nz.ac.waikato.mcennis.rat.graph.algorithm.Algorithm;
import nz.ac.waikato.mcennis.rat.graph.descriptors.IODescriptorFactory;
import nz.ac.waikato.mcennis.rat.graph.descriptors.IODescriptorInternal;
import nz.ac.waikato.mcennis.rat.graph.descriptors.ParameterFactory;
import nz.ac.waikato.mcennis.rat.graph.descriptors.ParameterInternal;
import nz.ac.waikato.mcennis.rat.graph.descriptors.PropertiesFactory;
import nz.ac.waikato.mcennis.rat.graph.descriptors.PropertiesInternal;
import nz.ac.waikato.mcennis.rat.graph.descriptors.SyntaxCheckerFactory;
import nz.ac.waikato.mcennis.rat.graph.descriptors.SyntaxObject;
import nz.ac.waikato.mcennis.rat.graph.link.Link;
import nz.ac.waikato.mcennis.rat.graph.link.LinkFactory;
import nz.ac.waikato.mcennis.rat.graph.property.Property;
import nz.ac.waikato.mcennis.rat.graph.property.PropertyFactory;
import nz.ac.waikato.mcennis.rat.graph.property.PropertyValueXMLFactory;
import nz.ac.waikato.mcennis.rat.graph.property.xml.PropertyValueXML;
import nz.ac.waikato.mcennis.rat.graph.query.ActorQuery;
import nz.ac.waikato.mcennis.rat.graph.query.ActorQueryFactory;
import nz.ac.waikato.mcennis.rat.graph.query.ActorQueryXML;
import nz.ac.waikato.mcennis.rat.graph.query.GraphQuery;
import nz.ac.waikato.mcennis.rat.graph.query.GraphQueryFactory;
import nz.ac.waikato.mcennis.rat.graph.query.GraphQueryXML;
import nz.ac.waikato.mcennis.rat.graph.query.LinkQuery;
import nz.ac.waikato.mcennis.rat.graph.query.LinkQueryFactory;
import nz.ac.waikato.mcennis.rat.graph.query.LinkQueryXML;
import nz.ac.waikato.mcennis.rat.graph.query.PropertyQueryFactory;
import nz.ac.waikato.mcennis.rat.graph.query.actor.xml.ActorQueryXMLFactory;
import nz.ac.waikato.mcennis.rat.graph.query.graph.xml.GraphQueryXMLFactory;
import nz.ac.waikato.mcennis.rat.graph.query.link.xml.LinkQueryXMLFactory;
import nz.ac.waikato.mcennis.rat.graph.query.property.PropertyQuery;
import nz.ac.waikato.mcennis.rat.graph.query.property.xml.PropertyQueryXML;
import nz.ac.waikato.mcennis.rat.graph.query.property.xml.PropertyQueryXMLFactory;
import nz.ac.waikato.mcennis.rat.parser.Parser;
import nz.ac.waikato.mcennis.rat.parser.ParserFactory;
import nz.ac.waikato.mcennis.rat.parser.xmlHandler.Handler;
import nz.ac.waikato.mcennis.rat.parser.xmlHandler.HandlerFactory;
import nz.ac.waikato.mcennis.rat.reusablecores.aggregator.AggregatorFunction;
import nz.ac.waikato.mcennis.rat.reusablecores.aggregator.AggregatorFunctionFactory;
import nz.ac.waikato.mcennis.rat.reusablecores.aggregator.xml.AggregatorXML;
import nz.ac.waikato.mcennis.rat.reusablecores.aggregator.xml.AggregatorXMLFactory;
import nz.ac.waikato.mcennis.rat.reusablecores.datavector.DataVector;
import nz.ac.waikato.mcennis.rat.reusablecores.datavector.xml.DataVectorXML;
import nz.ac.waikato.mcennis.rat.reusablecores.datavector.xml.DataVectorXMLFactory;
import nz.ac.waikato.mcennis.rat.reusablecores.distance.DistanceFactory;
import nz.ac.waikato.mcennis.rat.reusablecores.distance.DistanceFunction;
import nz.ac.waikato.mcennis.rat.reusablecores.distance.xml.DistanceXML;
import nz.ac.waikato.mcennis.rat.reusablecores.distance.xml.DistanceXMLFactory;
import nz.ac.waikato.mcennis.rat.reusablecores.instancefactory.InstanceFactory;
import nz.ac.waikato.mcennis.rat.reusablecores.instancefactory.InstanceFactoryFactory;

/**
 *
 * @author Daniel McEnnis
 */
public class DynamicLoader extends ClassLoader {

    static DynamicLoader instance = null;

    static DynamicLoader newInstance() {
        if (instance == null) {
            instance = new DynamicLoader();
        }
        return instance;
    }

    ProtectionDomain domain = null;

    private DynamicLoader() {
    }

    public void loadClasses() {
        File libDirectory = new File(java.lang.System.getenv("GRAPH_RAT") + File.pathSeparator + "lib");
        LinkedList<String> classNames = new LinkedList<String>();
        LinkedList<File> fileNames = new LinkedList<File>();
        findClass("", libDirectory, classNames, fileNames);
        File jarDirectory = new File(java.lang.System.getenv("GRAPH_RAT") + File.pathSeparator + "jar");
        File[] jarList = jarDirectory.listFiles();
        for (int i = 0; i < jarList.length; ++i) {
            if ((jarList[i].isFile()) && (jarList[i].getName().matches(".+\\.jar"))) {
                try {
                    JarFile jar = new JarFile(jarList[i]);
                    Enumeration<JarEntry> enumeration = jar.entries();
                    while (enumeration.hasMoreElements()) {
                        JarEntry entry = enumeration.nextElement();
                        String fileName = entry.getName();
                        if (fileName.matches(".+\\.class")) {
                            String className = fileName.replace('/', '.');
                            className = className.replace('\\', '.');
                            className = className.replace(':', '.');
                            className.substring(0, className.lastIndexOf('.'));
                            InputStream stream = jar.getInputStream(entry);
                            try {
                                loadClass(className, stream);
                            } catch (ClassNotFoundException ex) {
                                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    protected void findClass(String prefix, File currentDirectory, LinkedList<String> names, LinkedList<File> files) {
        File[] children = currentDirectory.listFiles();
        if (children != null) {
            for (int i = 0; i < children.length; ++i) {
                if (children[i].isDirectory()) {
                    findClass(prefix + children[i].getName() + ".", children[i], names, files);
                } else if (children[i].getName().matches(".+\\.class")) {
                    FileInputStream fileStream = null;
                    try {
                        String name = children[i].getName().substring(0, children[i].getName().lastIndexOf('.'));
                        fileStream = new FileInputStream(children[i]);
                        loadClass(name, fileStream);
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        try {
                            fileStream.close();
                        } catch (IOException ex) {
                            Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
    }

    protected void loadClass(String name, InputStream fileStream) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream dataSink = new ByteArrayOutputStream();
        int read = -1;
        byte[] array = new byte[10240];
        read = fileStream.read(array);
        while (read != -1) {
            dataSink.write(array, 0, read);
            read = fileStream.read(array);
        }
        array = dataSink.toByteArray();
        Class classObject = null;
        if (domain == null) {
            classObject = this.defineClass(name, array, 0, read);
        } else {
            classObject = this.defineClass(name, array, 0, read, domain);
        }
        this.loadClass(name, true);
        loadMaps(classObject);
    }

    protected void loadMaps(Class name) {
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Loading " + name.getName());
        if (ParameterInternal.class.isAssignableFrom(name)) {
            try {
                ParameterFactory.newInstance().addType(name.getSimpleName(), (ParameterInternal) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (Property.class.isAssignableFrom(name)) {
            try {
                PropertyFactory.newInstance().addType(name.getSimpleName(), (Property) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (Actor.class.isAssignableFrom(name)) {
            try {
                ActorFactory.newInstance().addType(name.getSimpleName(), (Actor) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (Link.class.isAssignableFrom(name)) {
            try {
                LinkFactory.newInstance().addType(name.getSimpleName(), (Link) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (Graph.class.isAssignableFrom(name)) {
            try {
                GraphFactory.newInstance().addType(name.getSimpleName(), (Graph) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (PropertyValueXML.class.isAssignableFrom(name)) {
            try {
                PropertyValueXMLFactory.newInstance().addType(name.getSimpleName(), (PropertyValueXML) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (PropertyQuery.class.isAssignableFrom(name)) {
            try {
                PropertyQueryFactory.newInstance().addType(name.getSimpleName(), (PropertyQuery) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (Algorithm.class.isAssignableFrom(name)) {
            try {
                AlgorithmFactory.newInstance().addType(name.getSimpleName(), (Algorithm) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (DataAquisition.class.isAssignableFrom(name)) {
            try {
                DataAquisitionFactory.newInstance().addType(name.getSimpleName(), (DataAquisition) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (Parser.class.isAssignableFrom(name)) {
            try {
                ParserFactory.newInstance().addType(name.getSimpleName(), (Parser) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (Handler.class.isAssignableFrom(name)) {
            try {
                HandlerFactory.newInstance().addType(name.getSimpleName(), (Handler) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (PropertiesInternal.class.isAssignableFrom(name)) {
            try {
                PropertiesFactory.newInstance().addType(name.getSimpleName(), (PropertiesInternal) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (SyntaxObject.class.isAssignableFrom(name)) {
            try {
                SyntaxCheckerFactory.newInstance().addType(name.getSimpleName(), (SyntaxObject) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (ActorQuery.class.isAssignableFrom(name)) {
            try {
                ActorQueryFactory.newInstance().addType(name.getSimpleName(), (ActorQuery) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (LinkQuery.class.isAssignableFrom(name)) {
            try {
                LinkQueryFactory.newInstance().addType(name.getSimpleName(), (LinkQuery) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (GraphQuery.class.isAssignableFrom(name)) {
            try {
                GraphQueryFactory.newInstance().addType(name.getSimpleName(), (GraphQuery) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (ActorQueryXML.class.isAssignableFrom(name)) {
            try {
                ActorQueryXMLFactory.newInstance().addType(name.getSimpleName(), (ActorQueryXML) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (LinkQueryXML.class.isAssignableFrom(name)) {
            try {
                LinkQueryXMLFactory.newInstance().addType(name.getSimpleName(), (LinkQueryXML) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (GraphQueryXML.class.isAssignableFrom(name)) {
            try {
                GraphQueryXMLFactory.newInstance().addType(name.getSimpleName(), (GraphQueryXML) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (IODescriptorInternal.class.isAssignableFrom(name)) {
            try {
                IODescriptorFactory.newInstance().addType(name.getSimpleName(), (IODescriptorInternal) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (PropertyQueryXML.class.isAssignableFrom(name)) {
            try {
                PropertyQueryXMLFactory.newInstance().addType(name.getSimpleName(), (PropertyQueryXML) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (DataVectorXML.class.isAssignableFrom(name)) {
            try {
                DataVectorXMLFactory.newInstance().addType(name.getSimpleName(), (DataVectorXML) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (DistanceFunction.class.isAssignableFrom(name)) {
            try {
                DistanceFactory.newInstance().addType(name.getSimpleName(), (DistanceFunction) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (DistanceXML.class.isAssignableFrom(name)) {
            try {
                DistanceXMLFactory.newInstance().addType(name.getSimpleName(), (DistanceXML) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (AggregatorFunction.class.isAssignableFrom(name)) {
            try {
                AggregatorFunctionFactory.newInstance().addType(name.getSimpleName(), (AggregatorFunction) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (AggregatorXML.class.isAssignableFrom(name)) {
            try {
                AggregatorXMLFactory.newInstance().addType(name.getSimpleName(), (AggregatorXML) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
        if (InstanceFactory.class.isAssignableFrom(name)) {
            try {
                InstanceFactoryFactory.newInstance().addType(name.getSimpleName(), (InstanceFactory) name.newInstance());
            } catch (InstantiationException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(DynamicLoader.class.getName()).log(Level.SEVERE, "Failed to create a '" + name.getSimpleName() + "' object", ex);
            }
        }
    }
}
