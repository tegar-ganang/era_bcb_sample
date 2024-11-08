package uk.ac.ebi.intact.psimitab;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.store.Directory;
import psidev.psi.mi.tab.PsimiTabWriter;
import psidev.psi.mi.tab.converter.xml2tab.TabConversionException;
import psidev.psi.mi.tab.converter.xml2tab.Xml2Tab;
import psidev.psi.mi.tab.expansion.BinaryExpansionStrategy;
import psidev.psi.mi.tab.model.BinaryInteraction;
import psidev.psi.mi.tab.model.CrossReferenceImpl;
import psidev.psi.mi.xml.converter.ConverterException;
import uk.ac.ebi.intact.psimitab.processor.IntactClusterInteractorPairProcessor;
import uk.ac.ebi.intact.bridges.ontologies.OntologyIndexSearcher;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.ArrayList;

/**
 * Tool allowing to convert a set of XML file or directories succesptible to contain XML files into PSIMITAB.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id: ConvertXml2Tab.java 12232 2008-10-07 22:28:52Z baranda $
 * @since <pre>02-Jan-2007</pre>
 */
public class ConvertXml2Tab {

    /**
     * Sets up a logger for that class.
     */
    public static final Log log = LogFactory.getLog(ConvertXml2Tab.class);

    private static final String NEW_LINE = System.getProperty("line.separator");

    /**
     * Controls the clustering of interactor pair in the final PSIMITAB file.
     */
    private boolean interactorPairClustering = true;

    /**
     * Strategy defining the behaviour of the binary expansion.
     *
     * @see psidev.psi.mi.tab.expansion.SpokeExpansion
     * @see psidev.psi.mi.tab.expansion.MatrixExpansion
     */
    private BinaryExpansionStrategy expansionStragegy;

    /**
     * Input file/directory to be converted. If directory are given, a recursive search is performed and all file having
     * an extension '.xml' are selected automatically.
     */
    private Collection<File> xmlFilesToConvert;

    /**
     * Output file resulting of the conversion.
     */
    private File outputFile;

    /**
     * Controls whever the output file can be overwriten or not.
     */
    private boolean overwriteOutputFile = false;

    /**
     * Where warning messages are going to be writtet to.
     */
    private Writer logWriter;

    /**
     * Path to the ontology index searcher that we can use to query CV terms.
     */
    private OntologyIndexSearcher ontologyIndexSearcher;

    /**
     * Name of the ontologies we want to support when fetching the name by identifier.
     */
    private Collection<String> ontologyNameToAutocomplete;

    public ConvertXml2Tab() {
        ontologyNameToAutocomplete = new ArrayList<String>();
    }

    public void setInteractorPairClustering(boolean enabled) {
        this.interactorPairClustering = enabled;
    }

    public boolean isInteractorPairClustering() {
        return interactorPairClustering;
    }

    public void setExpansionStrategy(BinaryExpansionStrategy expansionStragegy) {
        this.expansionStragegy = expansionStragegy;
    }

    public BinaryExpansionStrategy getExpansionStragegy() {
        return expansionStragegy;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public Collection<File> getXmlFilesToConvert() {
        return xmlFilesToConvert;
    }

    public void setXmlFilesToConvert(Collection<File> xmlFilesToConvert) {
        this.xmlFilesToConvert = xmlFilesToConvert;
    }

    public boolean isOverwriteOutputFile() {
        return overwriteOutputFile;
    }

    public void setOverwriteOutputFile(boolean overwriteOutputFile) {
        this.overwriteOutputFile = overwriteOutputFile;
    }

    public Writer getLogWriter() {
        return logWriter;
    }

    public void setLogWriter(Writer logWriter) {
        this.logWriter = logWriter;
    }

    public OntologyIndexSearcher getOntologyIndexSearcher() {
        return ontologyIndexSearcher;
    }

    public void setOntologyIndexSearcher(OntologyIndexSearcher ontologyIndexSearcher) {
        this.ontologyIndexSearcher = ontologyIndexSearcher;
    }

    public void addOntologyNameToAutocomplete(String name) {
        ontologyNameToAutocomplete.add(name);
    }

    public void convert() throws ConverterException, IOException, TabConversionException {
        if (xmlFilesToConvert == null) {
            throw new IllegalArgumentException("You must give a non null Collection<File> to convert.");
        }
        if (xmlFilesToConvert.isEmpty()) {
            throw new IllegalArgumentException("You must give a non empty Collection<File> to convert.");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("You must give a non null output file.");
        }
        if (outputFile.exists() && !overwriteOutputFile) {
            throw new IllegalArgumentException(outputFile.getName() + " already exits, overwrite is set to false. abort.");
        }
        if (outputFile.exists() && !outputFile.canWrite()) {
            throw new IllegalArgumentException(outputFile.getName() + " is not writable. abort.");
        }
        OntologyNameFinder finder = new OntologyNameFinder(ontologyIndexSearcher);
        for (String name : ontologyNameToAutocomplete) {
            finder.addOntologyName(name);
        }
        Xml2Tab x2t = new IntactXml2Tab(finder);
        x2t.addOverrideSourceDatabase(new CrossReferenceImpl("psi-mi", "MI:0469", "intact"));
        x2t.setExpansionStrategy(expansionStragegy);
        if (interactorPairClustering) {
            x2t.setPostProcessor(new IntactClusterInteractorPairProcessor());
        } else {
            x2t.setPostProcessor(null);
        }
        Collection<BinaryInteraction> interactions = x2t.convert(xmlFilesToConvert);
        if (interactions.isEmpty()) {
            if (logWriter != null) {
                logWriter.write("The following file(s) didn't yield any binary interactions:" + NEW_LINE);
                for (File file : xmlFilesToConvert) {
                    logWriter.write("  - " + file.getAbsolutePath() + NEW_LINE);
                }
                logWriter.write(outputFile.getName() + " was not generated." + NEW_LINE);
                logWriter.flush();
            } else {
                log.warn("The MITAB file " + outputFile.getName() + " didn't contain any data");
            }
        } else {
            PsimiTabWriter writer = new IntactPsimiTabWriter();
            writer.write(interactions, outputFile);
        }
    }
}
