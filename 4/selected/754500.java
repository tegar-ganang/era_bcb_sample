package uk.ac.ebi.intact.plugin.psigenerator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import uk.ac.ebi.intact.application.dataConversion.ExperimentListGenerator;
import uk.ac.ebi.intact.application.dataConversion.ExperimentListItem;
import uk.ac.ebi.intact.config.impl.CustomCoreDataConfig;
import uk.ac.ebi.intact.context.IntactContext;
import uk.ac.ebi.intact.context.IntactSession;
import uk.ac.ebi.intact.context.impl.StandaloneSession;
import uk.ac.ebi.intact.model.Experiment;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * TODO: comment this!
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id:PsiXmlGeneratorAbstractMojo.java 5772 2006-08-11 16:08:37 +0100 (Fri, 11 Aug 2006) baranda $
 * @since <pre>04/08/2006</pre>
 */
public abstract class PsiXmlGeneratorAbstractMojo extends AbstractMojo {

    protected static final String NEW_LINE = System.getProperty("line.separator");

    /**
    * Project instance
    * @parameter default-value="${project}"
    * @readonly
    */
    protected MavenProject project;

    /**
    * File containing the species
    * @parameter default-value="target/psixml"
    * @required
    */
    protected File targetPath;

    /**
    * File containing the species
    * @parameter default-value="classification_by_species.txt"
    */
    protected String speciesFilename;

    /**
    * File containing the publications
    * @parameter default-value="classification_by_publications.txt"
    */
    protected String publicationsFilename;

    /**
    * File containing the errors
    * @parameter default-value="target/psixml/experiment-error.log"
    */
    protected File experimentErrorFile;

    /**
    * File containing the negative experiments
    * @parameter default-value="target/psixml/negative-experiments.log"
    */
    protected File negativeExperimentsFile;

    /**
    * File containing the publications
    * @parameter default-value="%"
    */
    protected String searchPattern;

    /**
     * If true, all experiment without a PubMed ID (primary-reference) will be filtered out.
     *
     * @parameter default-value="true"
     */
    protected boolean onlyWithPmid;

    /**
     * Whether to update the existing project files or overwrite them.
     *
     * @parameter expression="${overwrite}" default-value="false"
     */
    protected boolean overwrite;

    /**
     * Create zip files, clustering the XMLs
     *
     * @parameter expression="${zipXml}" default-value="true"
     */
    protected boolean zipXml;

    /**
     * @parameter default-value="${project.build.outputDirectory}/hibernate/config/hibernate.cfg.xml"
     * @required
     */
    protected File hibernateConfig;

    private boolean initialized;

    private boolean reportsWritten;

    protected ExperimentListGenerator experimentListGenerator;

    public PsiXmlGeneratorAbstractMojo() {
    }

    protected File getSpeciesFile() {
        return new File(targetPath, speciesFilename);
    }

    protected File getPublicationsFile() {
        return new File(targetPath, publicationsFilename);
    }

    protected void initialize() throws MojoExecutionException {
        if (initialized) {
            return;
        }
        getLog().debug("Using hibernate cfg file: " + hibernateConfig);
        if (!hibernateConfig.exists()) {
            throw new MojoExecutionException("No hibernate config file found: " + hibernateConfig);
        }
        if (!targetPath.exists()) {
            targetPath.mkdirs();
        }
        File speciesFile = getSpeciesFile();
        File publicationsFile = getPublicationsFile();
        if (speciesFile.exists() && !overwrite) {
            throw new MojoExecutionException("Target species file already exist and overwrite is set to false: " + speciesFile);
        }
        if (publicationsFile.exists() && !overwrite) {
            throw new MojoExecutionException("Target publications file already exist and overwrite is set to false: " + speciesFile);
        }
        IntactSession session = new StandaloneSession();
        CustomCoreDataConfig testConfig = new CustomCoreDataConfig("PsiXmlGeneratorMojoTest", hibernateConfig, session);
        testConfig.initialize();
        IntactContext.initContext(testConfig, session);
        experimentListGenerator = new ExperimentListGenerator(searchPattern);
        experimentListGenerator.setOnlyWithPmid(onlyWithPmid);
        initialized = true;
    }

    protected Collection<ExperimentListItem> generateSpeciesListItems() throws MojoExecutionException {
        initialize();
        return experimentListGenerator.generateClassificationBySpecies();
    }

    protected Collection<ExperimentListItem> generatePublicationsListItems() throws MojoExecutionException {
        initialize();
        return experimentListGenerator.generateClassificationByPublications();
    }

    protected Collection<ExperimentListItem> generateAllClassifications() throws MojoExecutionException {
        initialize();
        return experimentListGenerator.generateAllClassifications();
    }

    protected void writeClassificationBySpeciesToFile() throws MojoExecutionException {
        getLog().debug("Species filename: " + getSpeciesFile());
        try {
            writeItems(getSpeciesFile(), generateSpeciesListItems());
        } catch (IOException e) {
            throw new MojoExecutionException("Problem creating the species file", e);
        }
        writeReports();
    }

    protected void writeClassificationByPublicationsToFile() throws MojoExecutionException {
        getLog().debug("Publications filename: " + getPublicationsFile());
        try {
            writeItems(getPublicationsFile(), generatePublicationsListItems());
        } catch (IOException e) {
            throw new MojoExecutionException("Problem creating the species file", e);
        }
        writeReports();
    }

    private void writeReports() throws MojoExecutionException {
        if (reportsWritten) {
            return;
        }
        initialize();
        writeNegativeExperiments();
        writeErrorFile();
        reportsWritten = true;
    }

    private static void writeItems(File itemsFile, Collection<ExperimentListItem> items) throws IOException {
        Writer writer = new FileWriter(itemsFile);
        for (ExperimentListItem item : items) {
            writer.write(item.toString() + NEW_LINE);
        }
        writer.close();
    }

    private void writeNegativeExperiments() throws MojoExecutionException {
        Collection<Experiment> negativeExperiments = experimentListGenerator.getNegativeExperiments();
        getLog().info("Negative experiments: " + negativeExperiments.size());
        try {
            Writer writer = new FileWriter(negativeExperimentsFile);
            writer.write("# Negative experiments " + new Date() + NEW_LINE);
            writer.write("############################################# " + NEW_LINE + NEW_LINE);
            if (negativeExperiments.isEmpty()) {
                writer.write("# No negative experiments found! " + NEW_LINE);
            }
            for (Experiment negExp : negativeExperiments) {
                writer.write(negExp.getAc() + "\t" + negExp.getShortLabel() + NEW_LINE);
            }
            writer.close();
        } catch (IOException e) {
            throw new MojoExecutionException("Problem writing negative experiments file", e);
        }
    }

    private void writeErrorFile() throws MojoExecutionException {
        Map<String, String> experimentsWithErrors = experimentListGenerator.getExperimentWithErrors();
        getLog().info("Experiments with errors: " + experimentsWithErrors.size());
        try {
            Writer writer = new FileWriter(experimentErrorFile);
            writer.write("# Experiments with errors " + new Date() + NEW_LINE);
            writer.write("###################################################### " + NEW_LINE + NEW_LINE);
            if (experimentsWithErrors.isEmpty()) {
                writer.write("# No errors found! " + NEW_LINE);
            }
            for (Map.Entry<String, String> error : experimentsWithErrors.entrySet()) {
                writer.write(error.getKey() + " " + error.getValue() + NEW_LINE);
            }
            writer.close();
        } catch (IOException e) {
            throw new MojoExecutionException("Problem writing error file", e);
        }
    }
}
