package de.fzi.kadmos.cmdutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import de.fzi.kadmos.cmdutils.KADMOSCMDException;

/**
 * @author Juergen Bock
 *
 */
public class EvaluatorTest {

    private static URL referenceURL;

    private static URL alignmentURL1;

    private static URL alignmentURL2;

    private static URL alignmentDirURL;

    /**
     * Loads an alignment that can be used for both reference alignment or alignment for evaluation.
     */
    @BeforeClass
    public static void getAlignmentLocation() {
        referenceURL = EvaluatorTest.class.getResource("/alignments/inria_alignment_1.rdf");
        alignmentURL1 = EvaluatorTest.class.getResource("/alignments/inria_alignment_1.rdf");
        alignmentURL2 = EvaluatorTest.class.getResource("/alignments/inria_alignment_2.rdf");
        alignmentDirURL = EvaluatorTest.class.getResource("/alignments");
        absolutizeURL(alignmentURL2);
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseArguments(String[])}.
     * No argument given.
     * Expected: {@link KADMOSCMDException}
     */
    @Test(expected = IllegalArgumentException.class)
    public final void testParseArgumentsNoArguments() throws Exception {
        String[] args = {};
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseArguments(String[])}.
     * Reference given, but no alignment, no options.
     * Expected: {@link IllegalArgumentException}
     */
    @Test(expected = IllegalArgumentException.class)
    public final void testParseArgumentsNoAlignmentNoOptions() throws Exception {
        String[] args = { referenceURL.getPath() };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseArguments(String[])}.
     * No options, reference and alignment as resolvable URLs
     * Expected: Reference and alignment locations correctly loaded.
     */
    @Test
    public final void testParseArgumentsNoOptionsResolvableURLs() throws Exception {
        String[] args = { referenceURL.toString(), alignmentURL2.toString() };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
        assertNotNull(evaluator.getReferenceLocation());
        assertEquals(referenceURL.toString(), evaluator.getReferenceLocation());
        assertNotNull(evaluator.getAlignmentWrappers().get(0).getAlignmentLocation());
        assertEquals(alignmentURL2.toString(), evaluator.getAlignmentWrappers().get(0).getAlignmentLocation());
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseArguments(String[])}.
     * No options, reference and alignment as resolvable URLs
     * Expected: Reference and alignment locations correctly loaded.
     */
    @Test
    public final void testParseTwoArgumentsNoOptionsResolvableURLs() throws Exception {
        String[] args = { referenceURL.toString(), alignmentURL2.toString() };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
        assertNotNull(evaluator.getReferenceLocation());
        assertEquals(referenceURL.toString(), evaluator.getReferenceLocation());
        assertNotNull(evaluator.getAlignmentWrappers().get(0).getAlignmentLocation());
        assertEquals(alignmentURL2.toString(), evaluator.getAlignmentWrappers().get(0).getAlignmentLocation());
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseArguments(String[])}.
     * No options, reference and alignment as absolute pathnames
     * Expected: Reference and alignment locations correctly loaded.
     */
    @Test
    public final void testParseArgumentsNoOptionsAbsolutePathnames() throws Exception {
        String[] args = { referenceURL.getPath(), alignmentURL2.getPath() };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
        assertNotNull(evaluator.getReferenceLocation());
        assertEquals(referenceURL.getPath(), evaluator.getReferenceLocation());
        assertNotNull(evaluator.getAlignmentWrappers().get(0).getAlignmentLocation());
        assertEquals(alignmentURL2.getPath(), evaluator.getAlignmentWrappers().get(0).getAlignmentLocation());
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseArguments(String[])}.
     * No options, reference and alignment as absolute pathnames
     * Expected: Reference and alignment locations correctly loaded.
     */
    @Test
    public final void testParseTwoArgumentsNoOptionsAbsolutePathnames() throws Exception {
        String[] args = { referenceURL.getPath(), alignmentURL2.getPath() };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
        assertNotNull(evaluator.getReferenceLocation());
        assertEquals(referenceURL.getPath(), evaluator.getReferenceLocation());
        assertNotNull(evaluator.getAlignmentWrappers().get(0).getAlignmentLocation());
        assertEquals(alignmentURL2.getPath(), evaluator.getAlignmentWrappers().get(0).getAlignmentLocation());
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseArguments(String[])}.
     * No options, reference and alignment as relative pathnames
     * Expected: Reference and alignment locations correctly loaded.
     */
    @Test
    public final void testParseArgumentsNoOptionsRelativePathnames() throws Exception {
        String currentDir = System.getProperty("user.dir");
        String relativeReferencePath = "reference.rdf";
        String relativeAlignmentPath = "alignment.rdf";
        copyFile(referenceURL.getPath(), currentDir + File.separator + relativeReferencePath);
        copyFile(alignmentURL2.getPath(), currentDir + File.separator + relativeAlignmentPath);
        String[] args = { relativeReferencePath, relativeAlignmentPath };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
        assertNotNull(evaluator.getReferenceLocation());
        assertEquals(relativeReferencePath, evaluator.getReferenceLocation());
        assertNotNull(evaluator.getAlignmentWrappers().get(0).getAlignmentLocation());
        assertEquals(relativeAlignmentPath, evaluator.getAlignmentWrappers().get(0).getAlignmentLocation());
        new File(currentDir + File.separator + relativeReferencePath).delete();
        new File(currentDir + File.separator + relativeAlignmentPath).delete();
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseArguments(String[])}.
     * No option, reference and alignment as resolvable URLs
     * Expected: computing classical precision and recall is enabled.
     */
    @Test
    public final void testParseArgumentsNoOption() throws Exception {
        String[] args = { referenceURL.toString(), alignmentURL2.toString() };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
        assertTrue(evaluator.computeClassicalPR());
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseArguments(String[])}.
     * -c option, reference and alignment as resolvable URLs
     * Expected: computing classical precision and recall is enabled.
     */
    @Test
    public final void testParseArgumentsCOption() throws Exception {
        String[] args = { "-c", referenceURL.toString(), alignmentURL2.toString() };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
        assertTrue(evaluator.computeClassicalPR());
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseArguments(String[])}.
     * -s option, reference and alignment as resolvable URLs
     * Expected: computing classical precision and recall is disabled,
     *           computing symmetric precision and recall is enabled.
     */
    @Test
    public final void testParseArgumentsSOption() throws Exception {
        String[] args = { "-s", referenceURL.toString(), alignmentURL2.toString() };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
        assertFalse(evaluator.computeClassicalPR());
        assertTrue(evaluator.computeSymmetricPR());
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseArguments(String[])}.
     * -c and -s options, reference and alignment as resolvable URLs
     * Expected: computing classical precision and recall is disabled,
     *           computing symmetric precision and recall is enabled.
     */
    @Test
    public final void testParseArgumentsCandSOption() throws Exception {
        String[] args = { "-c", "-s", referenceURL.toString(), alignmentURL2.toString() };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
        assertTrue(evaluator.computeClassicalPR());
        assertTrue(evaluator.computeSymmetricPR());
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseArguments(String[])}.
     * --classical-pr option, reference and alignment as resolvable URLs
     * Expected: computing classical precision and recall is enabled.
     */
    @Test
    public final void testParseArgumentsClassicalPROption() throws Exception {
        String[] args = { "--classical-pr", referenceURL.toString(), alignmentURL2.toString() };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
        assertTrue(evaluator.computeClassicalPR());
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseArguments(String[])}.
     * --symmetric-pr option, reference and alignment as resolvable URLs
     * Expected: computing classical precision and recall is disabled,
     *           computing symmetric precision and recall is enabled.
     */
    @Test
    public final void testParseArgumentsSymmetricPROption() throws Exception {
        String[] args = { "--symmetric-pr", referenceURL.toString(), alignmentURL2.toString() };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
        assertFalse(evaluator.computeClassicalPR());
        assertTrue(evaluator.computeSymmetricPR());
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseArguments(String[])}.
     * --classical-pr and --symmetric-pr option, reference and alignment as resolvable URLs
     * Expected: computing classical precision and recall is disabled,
     *           computing symmetric precision and recall is enabled.
     */
    @Test
    public final void testParseArgumentsClassicalPRandSymmetricPROption() throws Exception {
        String[] args = { "--classical-pr", "--symmetric-pr", referenceURL.toString(), alignmentURL2.toString() };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
        assertTrue(evaluator.computeClassicalPR());
        assertTrue(evaluator.computeSymmetricPR());
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseReference(String)}.
     * Resolvable local URL given.
     * Expected: success
     */
    @Test
    public final void testParseReferenceResolvableLocalURL() throws Exception {
        Evaluator evaluator = new Evaluator();
        assertNotNull(evaluator.parseReference(alignmentURL2.toString()));
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseReference(String)}.
     * Not resolvable URL given.
     * Expected: success
     */
    @Test(expected = FileNotFoundException.class)
    public final void testParseReferenceNotResolvableURL() throws Exception {
        URL notResolvableLocalURL = new File(System.getProperty("java.io.tmpdir") + File.separator + "doesnotexist.rdf").toURI().toURL();
        Evaluator evaluator = new Evaluator();
        evaluator.parseReference(notResolvableLocalURL.toString());
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseReference(String)}.
     * Resolvable pathname given.
     * Expected: success
     */
    @Test
    public final void testParseReferenceResolvablePathname() throws Exception {
        Evaluator evaluator = new Evaluator();
        assertNotNull(evaluator.parseReference(alignmentURL2.getPath()));
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseReference(String)}.
     * Not resolvable pathname given.
     * Expected: success
     */
    @Test(expected = FileNotFoundException.class)
    public final void testParseReferenceNotResolvablePathname() throws Exception {
        Evaluator evaluator = new Evaluator();
        evaluator.parseReference(System.getProperty("java.io.tmpdir") + File.separator + "doesnotexist.rdf");
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseAlignment(String)}.
     * Resolvable local URL given.
     * Expected: success
     */
    @Test
    public final void testParseAlignmentResolvableLocalURL() throws Exception {
        Evaluator evaluator = new Evaluator();
        assertNotNull(evaluator.parseAlignment(alignmentURL2.toString()));
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseAlignment(String)}.
     * Not resolvable URL given.
     * Expected: success
     */
    @Test(expected = FileNotFoundException.class)
    public final void testParseAlignmentNotResolvableURL() throws Exception {
        URL notResolvableLocalURL = new File(System.getProperty("java.io.tmpdir") + File.separator + "doesnotexist.rdf").toURI().toURL();
        Evaluator evaluator = new Evaluator();
        evaluator.parseAlignment(notResolvableLocalURL.toString());
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseAlignment(String)}.
     * Resolvable pathname given.
     * Expected: success
     */
    @Test
    public final void testParseAlignmentResolvablePathname() throws Exception {
        Evaluator evaluator = new Evaluator();
        assertNotNull(evaluator.parseAlignment(alignmentURL2.getPath()));
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseAlignment(String)}.
     * Not resolvable pathname given.
     * Expected: success
     */
    @Test(expected = FileNotFoundException.class)
    public final void testParseAlignmentNotResolvablePathname() throws Exception {
        Evaluator evaluator = new Evaluator();
        evaluator.parseAlignment(System.getProperty("java.io.tmpdir") + File.separator + "doesnotexist.rdf");
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseAlignment(String)}.
     * Tests parsing of alignment text file
     * Expected: success
     */
    @Test
    public final void testParseAlignmentTxtFile() throws Exception {
        List<String> data = new ArrayList<String>();
        data.add(alignmentURL1.getPath());
        data.add(alignmentURL2.getPath());
        File alignmentfile = buildAlignmentFile(data, "alignment.txt");
        String[] args = { referenceURL.getPath(), alignmentfile.getPath() };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
        assertNotNull(evaluator.getReferenceLocation());
        assertEquals(referenceURL.getPath(), evaluator.getReferenceLocation());
        assertNotNull(evaluator.getAlignmentSource());
        assertEquals(alignmentfile.getPath(), evaluator.getAlignmentSource());
        assertEquals(2, evaluator.getAlignmentWrappers().size());
        assertNotNull(evaluator.getAlignmentWrappers().get(0).getAlignmentLocation());
        assertEquals(alignmentURL1.getPath(), evaluator.getAlignmentWrappers().get(0).getAlignmentLocation());
        assertNotNull(evaluator.getAlignmentWrappers().get(1).getAlignmentLocation());
        assertEquals(alignmentURL2.getPath(), evaluator.getAlignmentWrappers().get(1).getAlignmentLocation());
        alignmentfile.delete();
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseAlignment(String)}.
     * Tests deep scan of directory
     * Expected: successs
     */
    @Test
    public final void testParseAlignmentDeepScanDirectory() throws Exception {
        String[] args = { "-d", referenceURL.getPath(), alignmentDirURL.getPath() };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
        assertNotNull(evaluator.getReferenceLocation());
        assertEquals(referenceURL.getPath(), evaluator.getReferenceLocation());
        assertNotNull(evaluator.getAlignmentSource());
        assertEquals(4, evaluator.getAlignmentWrappers().size());
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseAlignment(String)}.
     * Tests deep scan of directory
     * Expected: successs
     */
    @Test
    public final void testParseAlignmentBatch() throws Exception {
        String[][] data = new String[2][3];
        data[0][0] = "101";
        data[0][1] = referenceURL.getPath();
        data[0][2] = alignmentURL1.getPath();
        data[1][0] = "102";
        data[1][1] = referenceURL.getPath();
        data[1][2] = alignmentURL2.getPath();
        File batchfile = buildBatchFile(data, "batch.txt");
        String[] args = { "-b " + batchfile.getPath() };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
        assertNotNull(evaluator.getBatchFileLocation());
        assertEquals(batchfile.getPath(), evaluator.getBatchFileLocation());
        assertEquals(2, evaluator.getAlignmentWrappers().size());
        assertNotNull(evaluator.getAlignmentWrappers().get(0).getAlignmentName());
        assertEquals("101", evaluator.getAlignmentWrappers().get(0).getAlignmentName());
        assertNotNull(evaluator.getAlignmentWrappers().get(0).getReference());
        assertNotNull(evaluator.getAlignmentWrappers().get(0).getAlignmentLocation());
        assertEquals(alignmentURL1.getPath(), evaluator.getAlignmentWrappers().get(0).getAlignmentLocation());
        assertNotNull(evaluator.getAlignmentWrappers().get(0).getAlignment());
        assertNotNull(evaluator.getAlignmentWrappers().get(1).getAlignmentName());
        assertEquals("102", evaluator.getAlignmentWrappers().get(1).getAlignmentName());
        assertNotNull(evaluator.getAlignmentWrappers().get(1).getReference());
        assertNotNull(evaluator.getAlignmentWrappers().get(1).getAlignmentLocation());
        assertEquals(alignmentURL2.getPath(), evaluator.getAlignmentWrappers().get(1).getAlignmentLocation());
        assertNotNull(evaluator.getAlignmentWrappers().get(1).getAlignment());
        batchfile.delete();
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseAlignment(String)}.
     * Tests deep scan of directory
     * Expected: successs
     */
    @Test
    @Ignore
    public final void testParseAlignmentCreateCSV() throws Exception {
        File file1 = new File(System.getProperty("java.io.tmpdir") + File.separator + "csv.csv");
        assertFalse(file1.exists());
        String[] args = { "-x" + file1.getPath(), referenceURL.getPath(), alignmentURL2.getPath() };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
        File file2 = new File(System.getProperty("java.io.tmpdir") + File.separator + "csv.csv");
        assertTrue(file2.exists());
        file2.delete();
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseAlignment(String)}.
     * Tests deep scan of directory
     * Expected: successs
     */
    @Test
    @Ignore
    public final void testParseAlignmentLongOptCreateCSV() throws Exception {
        File file1 = new File(System.getProperty("java.io.tmpdir") + File.separator + "csv.csv");
        assertFalse(file1.exists());
        String[] args = { "--csv=" + file1.getPath(), referenceURL.getPath(), alignmentURL2.getPath() };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
        File file2 = new File(System.getProperty("java.io.tmpdir") + File.separator + "csv.csv");
        assertTrue(file2.exists());
        file2.delete();
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseAlignment(String)}.
     * Tests deep scan of directory
     * Expected: successs
     */
    @Test
    @Ignore
    public final void testParseAlignmentCreateGNU() throws Exception {
        File file1 = new File(System.getProperty("java.io.tmpdir") + File.separator + "gnu.dat");
        assertFalse(file1.exists());
        String[] args = { "-g" + file1.getPath(), referenceURL.getPath(), alignmentURL2.getPath() };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
        File file2 = new File(System.getProperty("java.io.tmpdir") + File.separator + "gnu.dat");
        assertTrue(file2.exists());
        file2.delete();
    }

    /**
     * Test method for {@link de.fzi.kadmos.cmdutils.Evaluator#parseAlignment(String)}.
     * Tests deep scan of directory
     * Expected: successs
     */
    @Test
    @Ignore
    public final void testParseAlignmentLongOptCreateGNU() throws Exception {
        File file1 = new File(System.getProperty("java.io.tmpdir") + File.separator + "gnu.dat");
        assertFalse(file1.exists());
        String[] args = { "--gnu-plot=" + file1.getPath(), referenceURL.getPath(), alignmentURL2.getPath() };
        Evaluator evaluator = new Evaluator();
        evaluator.parseArguments(args);
        File file2 = new File(System.getProperty("java.io.tmpdir") + File.separator + "gnu.dat");
        assertTrue(file2.exists());
        file2.delete();
    }

    /**
     * Convenience method to copy a file. 
     * @param sourceFilename Source filename/path.
     * @param targetFilename Target filename/path.
     * @throws IOException if something goes wrong.
     */
    private void copyFile(String sourceFilename, String targetFilename) throws IOException {
        File source = new File(sourceFilename);
        File target = new File(targetFilename);
        InputStream in = new FileInputStream(source);
        OutputStream out = new FileOutputStream(target);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        in.close();
        out.close();
    }

    /**
     * Adjusts the absolute URL of local file.
     */
    private static void absolutizeURL(URL resource) {
        try {
            File file = new File(resource.getFile());
            boolean rewrite = false;
            if (file.exists()) {
                SAXBuilder builder = new SAXBuilder();
                URL ontologiesURL = EvaluatorTest.class.getResource(File.separator + "ontologies");
                if (ontologiesURL == null) {
                    return;
                }
                Document document = (Document) builder.build(file);
                Element root = document.getRootElement();
                if (root != null) {
                    Element alignment = (Element) root.getChildren().get(0);
                    if (alignment.getName() == "Alignment") {
                        Element onto1 = (Element) alignment.getChildren().get(3);
                        if (onto1.getName() == "onto1") {
                            Element ontology = (Element) onto1.getChildren().get(0);
                            if (ontology.getName() == "Ontology") {
                                Element onto1_location = (Element) ontology.getChildren().get(0);
                                if (onto1_location.getName() == "location") {
                                    String onto1_location_string = onto1_location.getText();
                                    if (onto1_location_string != null) {
                                        File onto1_location_file = new File(onto1_location_string);
                                        if (onto1_location_file != null) {
                                            onto1_location.setText(ontologiesURL.toString() + File.separator + onto1_location_file.getName());
                                            rewrite = true;
                                        }
                                    }
                                }
                            }
                        }
                        Element onto2 = (Element) alignment.getChildren().get(4);
                        if (onto2.getName() == "onto2") {
                            Element ontology = (Element) onto2.getChildren().get(0);
                            if (ontology.getName() == "Ontology") {
                                Element onto2_location = (Element) ontology.getChildren().get(0);
                                if (onto2_location.getName() == "location") {
                                    String onto2_location_string = onto2_location.getText();
                                    if (onto2_location_string != null) {
                                        File onto2_location_file = new File(onto2_location_string);
                                        if (onto2_location_file != null) {
                                            onto2_location.setText(ontologiesURL.toString() + File.separator + onto2_location_file.getName());
                                            rewrite = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (rewrite) {
                    String des = new XMLOutputter().outputString(document);
                    FileWriter fileWriter = new FileWriter(file);
                    fileWriter.write(des);
                    fileWriter.close();
                }
            } else {
                System.out.println("File does not exist");
            }
        } catch (IOException ex) {
            return;
        } catch (JDOMException e) {
            return;
        }
    }

    /**
     * Creates a file containing URLs
     */
    private File buildAlignmentFile(List<String> urls, String filename) {
        File file = new File(System.getProperty("java.io.tmpdir") + File.separator + filename);
        if (file.isFile()) {
            file.delete();
        }
        try {
            FileWriter writer = new FileWriter(file);
            Iterator<String> urlss = urls.iterator();
            while (urlss.hasNext()) {
                writer.append(urlss.next());
                if (urlss.hasNext()) writer.append("\n");
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("Could not create file.");
        }
        return file;
    }

    /**
     * Creates a batch file containing references/alignments
     */
    private File buildBatchFile(String[][] data, String filename) {
        File file = new File(System.getProperty("java.io.tmpdir") + File.separator + filename);
        if (file.isFile()) {
            file.delete();
        }
        try {
            FileWriter writer = new FileWriter(file);
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < data[i].length; j++) {
                    writer.append(data[i][j]);
                    if (data[i].length - 1 == j) {
                        writer.append("\n");
                    } else {
                        writer.append(",");
                    }
                }
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("Could not create file.");
        }
        return file;
    }
}
