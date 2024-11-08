package de.fraunhofer.iitb.owldb.test;

import de.fraunhofer.iitb.owldb.*;
import java.io.*;
import java.nio.channels.*;
import java.util.*;
import org.junit.*;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.*;

/**
 * Test OWLDB: load ontology, add axioms and remove them later.
 * 
 * @author Manfred Schenk (Fraunhofer IOSB)
 */
public class LUBMOntologyTestOWLDB {

    private static OWLDBOntologyManager dbManager;

    private static OWLMutableOntology onto;

    private static String ontoUri = "jdbc:h2:./resources/LUBM10-DB-forUpdate/LUBM10";

    private static IRI dbIRI;

    private static OWLDataFactory factory;

    /**
	 * Setup the ontology database.
	 * 
	 * @throws OWLOntologyCreationException Could not create the ontology
	 */
    @BeforeClass
    public static void setUpOnce() throws OWLOntologyCreationException {
        dbManager = (OWLDBOntologyManager) OWLDBManager.createOWLOntologyManager(OWLDataFactoryImpl.getInstance());
        dbIRI = IRI.create(ontoUri);
        System.out.println("copying ontology to work folder...");
        try {
            final File directory = new File("./resources/LUBM10-DB-forUpdate/");
            final File[] filesToDelete = directory.listFiles();
            if (filesToDelete != null && filesToDelete.length > 0) {
                for (final File file : filesToDelete) {
                    if (!file.getName().endsWith(".svn")) Assert.assertTrue(file.delete());
                }
            }
            final File original = new File("./resources/LUBM10-DB/LUBM10.h2.db");
            final File copy = new File("./resources/LUBM10-DB-forUpdate/LUBM10.h2.db");
            final FileChannel inChannel = new FileInputStream(original).getChannel();
            final FileChannel outChannel = new FileOutputStream(copy).getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (final IOException ioe) {
            System.err.println(ioe.getMessage());
            Assert.fail();
        }
        onto = (OWLMutableOntology) dbManager.loadOntology(dbIRI);
        factory = dbManager.getOWLDataFactory();
    }

    /**
	 * A complex test to trigger a hibernate exception.
	 * 
	 * @throws OWLOntologyCreationException Could not create the ontology
	 */
    @Test
    public void testComplex() throws OWLOntologyCreationException {
        final IRI owlClassIRI = IRI.create("http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#FullProfessor");
        final OWLClass clazz1 = LUBMOntologyTestOWLDB.factory.getOWLClass(owlClassIRI);
        final IRI owlIndividualIRI = IRI.create("http://www.Department0.University0.edu/FullProfessor666");
        final OWLNamedIndividual namedIndi = LUBMOntologyTestOWLDB.factory.getOWLNamedIndividual(owlIndividualIRI);
        final OWLClassAssertionAxiom classAss1 = LUBMOntologyTestOWLDB.factory.getOWLClassAssertionAxiom(clazz1, namedIndi);
        LUBMOntologyTestOWLDB.onto.applyChange(new AddAxiom(onto, classAss1));
        final OWLLiteral literal = LUBMOntologyTestOWLDB.factory.getOWLLiteral("FullProfessor666");
        final IRI owlDataPropIRI = IRI.create("http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#name");
        final OWLDataPropertyExpression dpe = LUBMOntologyTestOWLDB.factory.getOWLDataProperty(owlDataPropIRI);
        final OWLDataPropertyAssertionAxiom dpaa = LUBMOntologyTestOWLDB.factory.getOWLDataPropertyAssertionAxiom(dpe, namedIndi, literal);
        LUBMOntologyTestOWLDB.onto.applyChange(new AddAxiom(onto, dpaa));
        final List<OWLOntologyChange> changes = new ArrayList<OWLOntologyChange>();
        changes.add(new RemoveAxiom(onto, dpaa));
        changes.add(new RemoveAxiom(onto, classAss1));
        LUBMOntologyTestOWLDB.onto.applyChanges(changes);
    }
}
