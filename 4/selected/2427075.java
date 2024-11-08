package eu.planets_project.ifr.core.services.characterisation.metadata.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.ejb.Stateless;
import javax.jws.WebService;
import javax.xml.ws.BindingType;
import javax.xml.ws.soap.MTOM;
import nz.govt.natlib.meta.FileHarvestSource;
import nz.govt.natlib.meta.config.Config;
import nz.govt.natlib.meta.config.Configuration;
import nz.govt.natlib.meta.config.ConfigurationException;
import nz.govt.natlib.meta.ui.PropsManager;
import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import com.sun.xml.ws.developer.StreamingAttachment;
import eu.planets_project.ifr.core.techreg.formats.FormatRegistry;
import eu.planets_project.ifr.core.techreg.formats.FormatRegistryFactory;
import eu.planets_project.services.PlanetsServices;
import eu.planets_project.services.characterise.Characterise;
import eu.planets_project.services.characterise.CharacteriseResult;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.services.datatypes.Parameter;
import eu.planets_project.services.datatypes.Property;
import eu.planets_project.services.datatypes.ServiceDescription;
import eu.planets_project.services.datatypes.ServiceReport;
import eu.planets_project.services.datatypes.Tool;
import eu.planets_project.services.datatypes.ServiceDescription.Builder;
import eu.planets_project.services.datatypes.ServiceReport.Status;
import eu.planets_project.services.datatypes.ServiceReport.Type;
import eu.planets_project.services.utils.DigitalObjectUtils;
import eu.planets_project.services.utils.ServiceUtils;

/**
 * Service wrapping the Metadata Extraction Tool from the National Archive of New Zealand
 * (http://meta-extractor.sourceforge.net/).
 * @author Fabian Steeg (fabian.steeg@uni-koeln.de)
 */
@Stateless
@MTOM
@StreamingAttachment(parseEagerly = true, memoryThreshold = ServiceUtils.JAXWS_SIZE_THRESHOLD)
@WebService(name = MetadataExtractor.NAME, serviceName = Characterise.NAME, targetNamespace = PlanetsServices.NS, endpointInterface = "eu.planets_project.services.characterise.Characterise")
public final class MetadataExtractor implements Characterise {

    static final String NAME = "MetadataExtractor";

    static final String NZME_PROPERTY_ROOT = "planets:pc/nzme/";

    /**
     * @param name The property name
     * @return A property URI for the given name
     */
    static URI makePropertyURI(final String name) {
        return URI.create(NZME_PROPERTY_ROOT + name);
    }

    /**
     * The optional format XCEL and parameters are ignored in this implementation (you may pass
     * null). {@inheritDoc}
     * @see eu.planets_project.services.characterise.Characterise#characterise(eu.planets_project.services.datatypes.DigitalObject,
     *      java.lang.String, eu.planets_project.services.datatypes.Parameter)
     */
    public CharacteriseResult characterise(final DigitalObject digitalObject, final List<Parameter> parameters) {
        String resultString = basicCharacteriseOneBinary(digitalObject);
        List<Property> props = readProperties(resultString);
        return new CharacteriseResult(props, new ServiceReport(Type.INFO, Status.SUCCESS, "OK"));
    }

    /**
     * Property listing is not yet implemented for this class, the resulting list will always be
     * empty. {@inheritDoc}
     * @see eu.planets_project.services.characterise.Characterise#listProperties(java.net.URI)
     */
    public List<Property> listProperties(final URI formatURI) {
        ArrayList<Property> result = new ArrayList<Property>();
        FormatRegistry registry = FormatRegistryFactory.getFormatRegistry();
        Set<String> extensions = registry.getExtensions(formatURI);
        MetadataType[] types = MetadataType.values();
        for (MetadataType metadataType : types) {
            String[] split = metadataType.sample.split("\\.");
            String suffix = split[split.length - 1];
            if (extensions.contains(suffix.toLowerCase())) {
                List<String> listProperties = listProperties(metadataType);
                for (String string : listProperties) {
                    result.add(new Property(makePropertyURI(string), string, null));
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * @see eu.planets_project.services.PlanetsService#describe()
     */
    public ServiceDescription describe() {
        FormatRegistry formatRegistry = FormatRegistryFactory.getFormatRegistry();
        List<URI> inputFormats = new ArrayList<URI>();
        MetadataType[] metadataTypes = MetadataType.values();
        for (MetadataType metadataType : metadataTypes) {
            String[] split = metadataType.sample.split("\\.");
            String extension = split[split.length - 1];
            inputFormats.addAll(formatRegistry.getUrisForExtension(extension));
        }
        Builder builder = new ServiceDescription.Builder("New Zealand Metadata Extractor Service", Characterise.class.getName());
        builder.author("Fabian Steeg");
        builder.classname(this.getClass().getName());
        builder.description("Metadata extraction service based on the Metadata Extraction Tool of the National " + "Library of New Zealand (patched 3.4GA).");
        builder.serviceProvider("The Planets Consortium");
        builder.tool(Tool.create(null, "New Zealand Metadata Extractor", "3.4GA (patched)", null, "http://meta-extractor.sourceforge.net/"));
        builder.furtherInfo(URI.create("http://sourceforge.net/tracker/index.php?func=detail&aid=2027729&group_id=189407" + "&atid=929202"));
        builder.inputFormats(inputFormats.toArray(new URI[] {}));
        return builder.build();
    }

    /**
     * @param metadataXml The XML string resulting from harvesting, the output of the NZ metadata
     *        extractor
     * @return A list of properties
     */
    static List<Property> readProperties(final String metadataXml) {
        List<Property> properties = new ArrayList<Property>();
        SAXBuilder builder = new SAXBuilder();
        try {
            Document doc = builder.build(new StringReader(metadataXml));
            Element meta = doc.getRootElement().getChild("METADATA");
            for (Object propElem : meta.getChildren()) {
                Element e = (Element) propElem;
                Property p = new Property(makePropertyURI(e.getName()), e.getName(), e.getText());
                properties.add(p);
            }
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    /**
     * @param type The file type
     * @return A list of attributes extractable for the given type, as defined in the adapters DTD
     *         file
     */
    static List<String> listProperties(final MetadataType type) {
        List<String> props = new ArrayList<String>();
        try {
            File adapter = File.createTempFile("adapter", null);
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(type.adapter);
            if (stream == null) {
                throw new IllegalStateException("Could not load adapter Jar: " + type.adapter);
            }
            FileOutputStream out = new FileOutputStream(adapter);
            IOUtils.copyLarge(stream, out);
            out.close();
            JarFile jar = new JarFile(adapter);
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith("dtd")) {
                    InputStream inputStream = jar.getInputStream(entry);
                    Scanner s = new Scanner(inputStream);
                    while (s.hasNextLine()) {
                        String nextLine = s.nextLine();
                        if (nextLine.startsWith("<!ELEMENT")) {
                            String prop = nextLine.split(" ")[1];
                            props.add(prop);
                        }
                    }
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }

    /**
     * @param digitalObject The binary file to characterize
     * @return Returns the proprietary XML result string returned by the extractor tool
     * @see eu.planets_project.services.characterise.BasicCharacteriseOneBinary#basicCharacteriseOneBinary(byte[])
     */
    private String basicCharacteriseOneBinary(final DigitalObject digitalObject) {
        try {
            File file = DigitalObjectUtils.toFile(digitalObject);
            FileHarvestSource source = new FileHarvestSource(file);
            Configuration c = Config.getInstance().getConfiguration("Extract in Native form");
            String tempFolder = file.getParent();
            c.setOutputDirectory(tempFolder);
            c.getHarvester().harvest(c, source, new PropsManager());
            File result = new File(c.getOutputDirectory() + File.separator + file.getName() + ".xml");
            result.deleteOnExit();
            return read(result.getAbsolutePath());
        } catch (ConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param location The location of the text file to read
     * @return Return the content of the file at the specified location
     */
    private static String read(final String location) {
        StringBuilder builder = new StringBuilder();
        Scanner s;
        try {
            s = new Scanner(new File(location));
            while (s.hasNextLine()) {
                builder.append(s.nextLine()).append("\n");
            }
            return builder.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
