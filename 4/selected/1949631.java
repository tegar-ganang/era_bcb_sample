package eu.annocultor.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.lang.StringUtils;
import org.openrdf.model.Literal;
import org.openrdf.model.Namespace;
import org.openrdf.model.Statement;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import eu.annocultor.cli.Cli.CliExecutable;
import eu.annocultor.context.Namespaces;
import eu.annocultor.triple.LiteralValue;
import eu.annocultor.triple.Property;
import eu.annocultor.triple.ResourceValue;
import eu.annocultor.triple.Triple;
import eu.annocultor.utils.SesameWriter;

/**
 * Loads first file and saves, subtracting the second file.
 * 
 * @author Borys Omelayenko
 *
 */
public class OntologySubtractor implements CliExecutable {

    private static final String DELETED_RDF = ".deleted.rdf";

    private static class FilesWhereDeletedFromFilter implements FilenameFilter {

        String pattern;

        public FilesWhereDeletedFromFilter(String stam) {
            pattern = (stam + ".(\\d+).rdf").replaceAll("\\.", "\\.");
        }

        @Override
        public boolean accept(File dir, String name) {
            return name.matches(pattern);
        }
    }

    private static class FilesToDeleteFilter implements FilenameFilter {

        String stam;

        public FilesToDeleteFilter(String stam) {
            this.stam = stam;
        }

        @Override
        public boolean accept(File dir, String name) {
            return name.matches((stam + ".(\\w+)" + DELETED_RDF).replaceAll("\\.", "\\."));
        }
    }

    @Override
    public void mainMethod(String... args) throws Exception {
        OntologySubtractor.main(args);
    }

    public static void main(String[] args) throws Exception {
        boolean copy = checkNoCopyOption(args);
        if (args.length == 2 || args.length == 3) {
            File sourceDir = new File(args[0]);
            File destinationDir = new File(args[1]);
            checkSrcAndDstDirs(sourceDir, destinationDir);
            Collection<String> filesWithDeletedStatements = listNameStamsForFilesWithDeletedStatements(sourceDir);
            if (filesWithDeletedStatements.isEmpty()) {
                System.out.println("Did not found any file *.*.*.deleted.rdf with statements to be deleted. Do nothing and exit.");
            } else {
                System.out.println("Found " + filesWithDeletedStatements.size() + " files with statements to be deleted");
                System.out.println("Copying all RDF files from " + sourceDir.getName() + " to " + destinationDir.getName());
                if (copy) {
                    copyRdfFiles(sourceDir, destinationDir);
                }
                sutractAll(sourceDir, destinationDir, filesWithDeletedStatements);
            }
        } else {
            for (Object string : IOUtils.readLines(new AutoCloseInputStream(OntologySubtractor.class.getResourceAsStream("/subtractor/readme.txt")))) {
                System.out.println(string.toString());
            }
        }
    }

    private static boolean checkNoCopyOption(String[] args) {
        if (args.length == 3 && args[2].equals("-nocopy")) {
            return false;
        }
        return true;
    }

    private static Collection<String> listNameStamsForFilesWithDeletedStatements(File sourceDir) {
        Set<String> stamms = new HashSet<String>();
        for (String file : sourceDir.list(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(DELETED_RDF);
            }
        })) {
            String stam = StringUtils.substringBeforeLast(file, DELETED_RDF);
            stam = StringUtils.substringBeforeLast(stam, ".");
            stamms.add(stam);
        }
        return stamms;
    }

    private static void copyRdfFiles(File sourceDir, File destinationDir) throws IOException {
        File[] allRdfFiles = sourceDir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".rdf") && !name.endsWith(DELETED_RDF);
            }
        });
        for (File file : allRdfFiles) {
            FileUtils.copyFileToDirectory(file, destinationDir);
        }
    }

    private static void checkSrcAndDstDirs(File sourceDir, File destinationDir) throws Exception, IOException {
        if (!sourceDir.isDirectory()) {
            throw new Exception("Directory expected but this found: " + sourceDir.getCanonicalPath());
        }
        if (!destinationDir.isDirectory()) {
            throw new Exception("Directory expected but this found: " + destinationDir.getCanonicalPath());
        }
        if (!destinationDir.canWrite()) {
            throw new Exception("Directory is not writeable: " + destinationDir.getCanonicalPath());
        }
    }

    private static void sutractAll(File sourceDir, File destinationDir, Collection<String> nameStamsOfFilesWithDeletedStatements) throws Exception {
        for (String stam : nameStamsOfFilesWithDeletedStatements) {
            File[] filesWhereDeletedFrom = sourceDir.listFiles(new FilesWhereDeletedFromFilter(stam));
            File[] filesToDelete = sourceDir.listFiles(new FilesToDeleteFilter(stam));
            System.out.println("Subtracting files " + StringUtils.join(filesToDelete, ",") + " from " + StringUtils.join(filesWhereDeletedFrom, ","));
            for (File fileFrom : filesWhereDeletedFrom) {
                File fileTo = new File(destinationDir, fileFrom.getName());
                subtractStam(fileFrom, filesToDelete, fileTo);
            }
        }
    }

    static void subtractStam(File fileFrom, File[] filesToDelete, File fileTo) throws Exception {
        Repository fromRdf = Helper.createLocalRepository();
        Helper.importRDFXMLFile(fromRdf, Namespaces.ANNOCULTOR_CONVERTER.getUri(), fileFrom);
        RepositoryConnection fromConnection = fromRdf.getConnection();
        System.out.println("Loaded " + fromConnection.size() + " statements");
        Repository whatRdf = Helper.createLocalRepository();
        Helper.importRDFXMLFile(whatRdf, Namespaces.ANNOCULTOR_CONVERTER.getUri(), filesToDelete);
        RepositoryConnection whatConnection = whatRdf.getConnection();
        fromConnection.remove(whatConnection.getStatements(null, null, null, false));
        fromConnection.commit();
        Namespaces namespaces = new Namespaces();
        for (Namespace ns : whatConnection.getNamespaces().asList()) {
            namespaces.addNamespace(ns.getName(), ns.getPrefix());
        }
        SesameWriter destination = SesameWriter.createRDFXMLWriter(fileTo, namespaces, "deleted " + StringUtils.join(filesToDelete, ",") + " from " + fileFrom.getName(), "More info at http:// annocultor.eu", 1024, 1024);
        destination.startRDF();
        List<Statement> destinationStatemenets = fromConnection.getStatements(null, null, null, false).asList();
        for (Statement statement : destinationStatemenets) {
            org.openrdf.model.Value rdfValue = statement.getObject();
            eu.annocultor.triple.Value tripleValue = null;
            if (rdfValue instanceof Literal) {
                Literal literal = (Literal) rdfValue;
                tripleValue = new LiteralValue(literal.getLabel(), literal.getLanguage());
            } else {
                tripleValue = new ResourceValue(rdfValue.stringValue());
            }
            destination.handleTriple(new Triple(statement.getSubject().stringValue(), new Property(statement.getPredicate().stringValue()), tripleValue, null));
        }
        destination.endRDF();
        System.out.println("Saved file " + fileTo + " with deletions.");
        fromConnection.close();
        fromRdf.shutDown();
        whatConnection.close();
        whatRdf.shutDown();
    }
}
