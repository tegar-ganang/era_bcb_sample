package com.mindtree.techworks.insight.preferences.xmlpersistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import com.mindtree.techworks.insight.InsightConstants;
import com.mindtree.techworks.insight.preferences.PreferenceDataHandler;
import com.mindtree.techworks.insight.preferences.PreferenceHandlerInstantiationException;
import com.mindtree.techworks.insight.preferences.PreferenceHandlerStoreException;
import com.mindtree.techworks.insight.preferences.model.Preference;

/**
 * <p>
 * This is an implementation of the PreferenceDataHandler. It populates the
 * preferences stored in an XML file following a schema. The schema is located
 * within the classpath at the location
 * <code>com.mindtree.techworks.insight.preferences.xmlpersistence.res.InsightPreferences.xsd</code>.
 * <br>
 * For more details on the schema see
 * {@link com.mindtree.techworks.insight.preferences.xmlpersistence.XMLPreferenceDataReader XMLPreferenceDataReader}.
 * </p>
 * 
 * @see com.mindtree.techworks.insight.preferences.PreferenceDataHandler
 *      PreferenceDataHandler
 * @author Bindul Bhowmik
 * @version $Revision: 27 $ $Date: 2007-12-16 06:58:03 -0500 (Sun, 16 Dec 2007) $
 */
public class XMLPreferenceDataHandler implements PreferenceDataHandler {

    /**
	 * The location of the schema for preferences.
	 */
    private static final String SCHEMA_LOCATION = "com/mindtree/techworks/insight/preferences/xmlpersistence/res/InsightPreferences.xsd";

    /**
	 * The target namespace for the Schema
	 */
    private static final String TARGET_NAMESPACE = "http://mindtree.com/techworks/insight/Preferences";

    /**
	 * <b>From Apache Xerces Documentation </b>
	 * 
	 * <pre>
	 * 
	 *  
	 *   
	 *    
	 *     True:  	Validate the document and report validity errors.
	 *     False:  	Do not report validity errors.
	 *     Default:	false
	 *     Access:	(parsing) read-only; (not parsing) read-write;
	 *     Note:  	If this feature is set to true, the document must specify a 
	 *     			grammar. By default, validation will occur against DTD. For 
	 *     			more information, please, refer to the FAQ. If this feature is 
	 *     			set to false, and document specifies a grammar that grammar 
	 *     			might be parsed but no validation of the document contents will 
	 *     			be performed.
	 *     See:  	http://apache.org/xml/features/validation/dynamic
	 *     See:  	http://xml.org/sax/features/namespaces
	 *     See:  	http://apache.org/xml/features/nonvalidating/load-external-dtd
	 *     
	 *    
	 *   
	 *  
	 * </pre>
	 */
    private static final String FEATURE_VALIDATION = "http://xml.org/sax/features/validation";

    /**
	 * <b>From Apache Xerces Documentation </b>
	 * 
	 * <pre>
	 * 
	 *  
	 *   
	 *    
	 *     True:  	Turn on XML Schema validation by inserting XML Schema validator 
	 *     			in the pipeline.
	 *     False:  	Do not report validation errors against XML Schema.
	 *     Default:	false
	 *     Access:	(parsing) read-only; (not parsing) read-write;
	 *     Note:  	Validation errors will only be reported if validation feature is 
	 *     			set to true. For more information, please, refer to the FAQ
	 *     See:  	http://xml.org/sax/features/validation
	 *     See:  	http://apache.org/xml/features/validation/dynamic
	 *     See:  	http://xml.org/sax/features/namespaces
	 *     
	 *    
	 *   
	 *  
	 * </pre>
	 * 
	 * @see XMLPreferenceDataHandler#FEATURE_VALIDATION
	 *      http://xml.org/sax/features/validation
	 */
    private static final String FEATURE_VALIDATION_SCHEMA = "http://apache.org/xml/features/validation/schema";

    /**
	 * <b>From Apache Xerces Documentation </b>
	 * 
	 * <pre>
	 * 
	 *  
	 *   
	 *    
	 *     Desc:	The XML Schema Recommendation explicitly states that the 
	 *     			inclusion of schemaLocation/noNamespaceSchemaLocation attributes 
	 *     			is only a hint; it does not mandate that these attributes must 
	 *     			be used to locate schemas. Similar situation happens to 
	 *     			&lt;import&gt; element in schema documents. This property allows 
	 *     			the user to specify a list of schemas to use. If the 
	 *     			targetNamespace of a schema (specified using this property) 
	 *     			matches the targetNamespace of a schema occurring in the 
	 *     			instance document in schemaLocation attribute, or if the 
	 *     			targetNamespace matches the namespace attribute of &lt;import&gt; 
	 *     			element, the schema specified by the user using this property 
	 *     			will be used (i.e., the schemaLocation attribute in the instance 
	 *     			document or on the &lt;import&gt; element will be effectively 
	 *     			ignored).
	 *     Type:  	java.lang.String
	 *     Access:  read-write
	 *     Note:  	The syntax is the same as for schemaLocation attributes in 
	 *     			instance documents: e.g, &quot;http://www.example.com file_name.xsd&quot;. 
	 *     			The user can specify more than one XML Schema in the list.
	 *     
	 *    
	 *   
	 *  
	 * </pre>
	 */
    private static final String PROPERTY_SCHEMA_EXTLOC = "http://apache.org/xml/properties/schema/external-schemaLocation";

    /**
	 * Lexical handler property id
	 * (http://xml.org/sax/properties/lexical-handler).
	 */
    protected static final String LEXICAL_HANDLER_PROPERTY_ID = "http://xml.org/sax/properties/lexical-handler";

    /**
	 * Relative path of the config file
	 */
    public static final String XML_PROP_FILE_URI = "/config/insight-preferences.xml";

    /**
	 * Location of the preferences file
	 */
    private String preferenceFileLocation;

    /**
	 * Stores the location of the schema.
	 * <p>
	 * This variable should have been a local variable of the validateXMLFile()
	 * method, but it needs to be accessed by the inner class, hench it is made
	 * a class field.
	 * </p>
	 */
    protected String schemaFilePath;

    /**
	 * Default constructor.
	 * 
	 * @throws PreferenceHandlerInstantiationException When the Handler cannot
	 *             be instantiated because the XML is not valid or is not found.
	 */
    public XMLPreferenceDataHandler() throws PreferenceHandlerInstantiationException {
        preferenceFileLocation = System.getProperty(InsightConstants.INSIGHT_HOME) + XML_PROP_FILE_URI;
        File prefFile = new File(preferenceFileLocation);
        if (!prefFile.isFile() || !prefFile.canRead() || !prefFile.canWrite()) {
            throw new PreferenceHandlerInstantiationException("Preference File Cannot be read, or is not writable");
        }
        XMLPreferenceDataCache dataCache = XMLPreferenceDataCache.getInstance();
        if (!dataCache.isXMLValidated()) {
            boolean fileValidity = validateXMLFile();
            if (false == fileValidity) {
                throw new PreferenceHandlerInstantiationException("Cannot get valid xml");
            }
            dataCache.setXMLValidated(true);
        }
    }

    /**
	 * <p>
	 * Returns a list of PreferenceInfo objects.
	 * </p>
	 * <p>
	 * It first checks if the preferences have been loaded onto the cache or
	 * not. If not then loads the preferences on the cache and then returns the
	 * preference info.
	 * </p>
	 * 
	 * @see com.mindtree.techworks.insight.preferences.PreferenceDataHandler#getPreferenceNameList()
	 */
    public Collection getPreferenceNameList() {
        XMLPreferenceDataCache dataCache = XMLPreferenceDataCache.getInstance();
        if (!dataCache.isXMLLoaded()) {
            loadPreferencesIntoCache();
        }
        ArrayList preferenceInfo = new ArrayList();
        for (Iterator preferencesIterator = dataCache.iteratePreferences(); preferencesIterator.hasNext(); ) {
            Preference preference = (Preference) preferencesIterator.next();
            preferenceInfo.add(preference.getPreferenceInfo());
        }
        return preferenceInfo;
    }

    /**
	 * <p>
	 * Returns a preference for the id passed, or null if no preference is found
	 * for the id.
	 * </p>
	 * <p>
	 * It first checks if the preferences have been loaded onto the cache or
	 * not. If not then loads the preferences on the cache and then returns the
	 * preference.
	 * </p>
	 * 
	 * @see com.mindtree.techworks.insight.preferences.PreferenceDataHandler#getPreference(java.lang.String)
	 */
    public Preference getPreference(String preferenceId) {
        XMLPreferenceDataCache dataCache = XMLPreferenceDataCache.getInstance();
        if (!dataCache.isXMLLoaded()) {
            loadPreferencesIntoCache();
        }
        return dataCache.getPreference(preferenceId);
    }

    /**
	 * Saves the given preferences
	 * 
	 * @throws PreferenceHandlerStoreException
	 * @see com.mindtree.techworks.insight.preferences.PreferenceDataHandler#savePreferences(java.util.List)
	 */
    public void savePreferences(List preferences) throws PreferenceHandlerStoreException {
        String tempFilePath;
        try {
            File tempFile = File.createTempFile("insight-preference", ".xml");
            tempFile.deleteOnExit();
            tempFilePath = tempFile.getAbsolutePath();
        } catch (IOException e) {
            throw new PreferenceHandlerStoreException(e);
        }
        XMLDOMPreferenceDataWriter writer = new XMLDOMPreferenceDataWriter(preferenceFileLocation);
        writer.writePreferences(preferences, tempFilePath);
        File origFile = new File(preferenceFileLocation);
        origFile.delete();
        origFile = new File(preferenceFileLocation);
        File sourceFile = new File(tempFilePath);
        try {
            copyFile(sourceFile, origFile);
        } catch (IOException e) {
            throw new PreferenceHandlerStoreException(e);
        }
        sourceFile.delete();
    }

    /**
	 * Returns all the preferences.
	 * 
	 * @see com.mindtree.techworks.insight.preferences.PreferenceDataHandler#getAllPreferences()
	 */
    public Collection getAllPreferences() {
        XMLPreferenceDataCache dataCache = XMLPreferenceDataCache.getInstance();
        if (!dataCache.isXMLLoaded()) {
            loadPreferencesIntoCache();
        }
        return dataCache.getAllPreferences();
    }

    /**
	 * Validates the preferences XML file against the Schema. This is done only
	 * once at the init of the instance.
	 * 
	 * @return <code>true</code> if the xml is valid or <code>false</code>.
	 */
    private boolean validateXMLFile() {
        URL schemaLocation = Thread.currentThread().getContextClassLoader().getResource(SCHEMA_LOCATION);
        schemaFilePath = schemaLocation.getFile();
        String schemaExtLoc = TARGET_NAMESPACE + " " + schemaFilePath;
        FileInputStream fis = null;
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
        SAXParser parser = null;
        try {
            parser = saxParserFactory.newSAXParser();
            XMLReader xmlReader = parser.getXMLReader();
            xmlReader.setFeature(FEATURE_VALIDATION, true);
            xmlReader.setFeature(FEATURE_VALIDATION_SCHEMA, true);
            xmlReader.setProperty(PROPERTY_SCHEMA_EXTLOC, schemaExtLoc);
            DefaultHandler validationHandler = (new DefaultHandler() {

                public void warning(SAXParseException e) throws SAXException {
                    throw e;
                }

                public void error(SAXParseException e) throws SAXException {
                    throw e;
                }

                public void fatalError(SAXParseException e) throws SAXException {
                    throw e;
                }

                public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
                    if (systemId.endsWith(schemaFilePath)) {
                        InputSource schemaInputSource = new InputSource(this.getClass().getClassLoader().getResourceAsStream(SCHEMA_LOCATION));
                        return schemaInputSource;
                    }
                    try {
                        return super.resolveEntity(publicId, systemId);
                    } catch (Exception e) {
                        throw new SAXException("Exception occured resolving entity", e);
                    }
                }
            });
            fis = new FileInputStream(preferenceFileLocation);
            parser.parse(new InputSource(fis), validationHandler);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return false;
        } catch (SAXException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
	 * Loads the Preferences from the XML file into the cache.
	 */
    private void loadPreferencesIntoCache() {
        FileInputStream fis = null;
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
        SAXParser parser = null;
        try {
            parser = saxParserFactory.newSAXParser();
            DefaultHandler validationHandler = new XMLPreferenceDataReader();
            fis = new FileInputStream(preferenceFileLocation);
            parser.parse(new InputSource(fis), validationHandler);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        XMLPreferenceDataCache cache = XMLPreferenceDataCache.getInstance();
        cache.setXMLLoaded(true);
    }

    /**
	 * Copies the in file to out file
	 * 
	 * @param in Location of Source File
	 * @param out Location of Destination File
	 * @throws IOException
	 * @throws IOException Exception if Copy Fails
	 */
    private void copyFile(File in, File out) throws IOException {
        FileChannel sourceChannel = new FileInputStream(in).getChannel();
        FileChannel destinationChannel = new FileOutputStream(out).getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        sourceChannel.close();
        destinationChannel.close();
    }
}
