package verjinxer.subcommands;

import static org.junit.Assert.*;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import verjinxer.Globals;

/**
 * Test cases for the colorspace functionality of the Translater
 * 
 * @author Markus Kemmerling
 */
public class TranslaterSubcommandTest {

    private static TranslaterSubcommand translaterSubcommand;

    private static File testdataDirectory;

    /**
    * Creates an instance of Globals with 'data' as input directory and 'testdata' as output dir,
    * whereas the directory 'testdata' is created on disk. Than creates an instance of
    * TranslaterSubcommand with this instance of Globals.
    * 
    * @throws Exception
    */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        System.out.println("Setting up.");
        Globals g = new Globals();
        testdataDirectory = new File("testdata");
        if (testdataDirectory.exists()) {
            System.err.printf("The directory %s already exists. Exiting before damage some data.", testdataDirectory.getAbsolutePath());
            System.exit(-1);
        } else {
            testdataDirectory.mkdir();
            testdataDirectory.deleteOnExit();
            assert testdataDirectory.exists();
            assert testdataDirectory.isDirectory();
        }
        translaterSubcommand = new TranslaterSubcommand(g);
    }

    /**
    * Deletes the 'testdata' directory, that was used as output directory for the tests.
    * 
    * @throws Exception
    */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        System.out.println("Deleting directory.");
        assert testdataDirectory.exists();
        File[] files = testdataDirectory.listFiles();
        for (int i = 0; i < files.length; i++) {
            System.out.printf("File: %s%n", files[i].getAbsolutePath());
        }
        testdataDirectory.delete();
    }

    /**
    * Makes nothing.
    * 
    * @throws Exception
    */
    @Before
    public void setUp() throws Exception {
    }

    /**
    * Deletes each file in the 'testdata' directory that does not begin with a dot.
    * 
    * @throws Exception
    */
    @After
    public void tearDown() throws Exception {
        System.out.println("Deleting files in directory.");
        assert testdataDirectory.exists();
        File[] files = testdataDirectory.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                String name = pathname.getName();
                return !name.startsWith(".");
            }
        });
        for (File f : files) {
            assert f.isFile();
            f.delete();
            assert !f.exists() : String.format("The file %s was not deleted.%n", f.getAbsolutePath());
        }
    }

    /**
    * Tests the behavior for invalid options. Particular, what happens when an alphabet map is set
    * for a CSFASTA file and what happens when no alphabet map is set for a FASTA file.
    */
    @Test
    public void testRunWithWrongOptions() {
        String[] args;
        String[] alphabets = { "--dna", "--rconly", "--dnabi", "--protein", "-c", "--colorspace" };
        System.out.println("Testing, that you cannot set an alphabet map with a CSFASTA file.");
        args = new String[] { "-a", "data/colorspace.alphabet", "xyz.csfa" };
        assertEquals(1, translaterSubcommand.run(args));
        assertEquals(0, testdataDirectory.list().length);
        args = new String[] { "-a", "data/colorspace.alphabet", "agds.fa", "adfaflj.fasta", "afljafljasl.fasta", "xyz.csfa", "zppldgoe.fa", "afafiilrwe.fasta" };
        assertEquals(1, translaterSubcommand.run(args));
        assertEquals(0, testdataDirectory.list().length);
        args = new String[] { "-a", "data/colorspace.alphabet", "abc.csfasta" };
        assertEquals(1, translaterSubcommand.run(args));
        assertEquals(0, testdataDirectory.list().length);
        args = new String[] { "-a", "data/colorspace.alphabet", "agds.fa", "adfaflj.fasta", "afljafljasl.fasta", "xyz.csfasta", "zppldgoe.fa", "afafiilrwe.fasta" };
        assertEquals(1, translaterSubcommand.run(args));
        assertEquals(0, testdataDirectory.list().length);
        args = new String[] { "--dnarc", "#", "xyz.csfa" };
        assertEquals(1, translaterSubcommand.run(args));
        assertEquals(0, testdataDirectory.list().length);
        args = new String[] { "--dnarc", "#", "agds.fa", "adfaflj.fasta", "afljafljasl.fasta", "xyz.csfa", "zppldgoe.fa", "afafiilrwe.fasta" };
        assertEquals(1, translaterSubcommand.run(args));
        assertEquals(0, testdataDirectory.list().length);
        args = new String[] { "--dnarc", "#", "abc.csfasta" };
        assertEquals(1, translaterSubcommand.run(args));
        assertEquals(0, testdataDirectory.list().length);
        args = new String[] { "--dnarc", "#", "agds.fa", "adfaflj.fasta", "afljafljasl.fasta", "xyz.csfasta", "zppldgoe.fa", "afafiilrwe.fasta" };
        assertEquals(1, translaterSubcommand.run(args));
        assertEquals(0, testdataDirectory.list().length);
        for (String alphabet : alphabets) {
            args = new String[] { alphabet, "xyz.csfa" };
            assertEquals(String.format("Alphabet: %s", alphabet), 1, translaterSubcommand.run(args));
            assertEquals(0, testdataDirectory.list().length);
            args = new String[] { alphabet, "agds.fa", "adfaflj.fasta", "afljafljasl.fasta", "xyz.csfa", "zppldgoe.fa", "afafiilrwe.fasta" };
            assertEquals(1, translaterSubcommand.run(args));
            assertEquals(0, testdataDirectory.list().length);
            args = new String[] { alphabet, "abc.csfasta" };
            assertEquals(1, translaterSubcommand.run(args));
            assertEquals(0, testdataDirectory.list().length);
            args = new String[] { alphabet, "agds.fa", "adfaflj.fasta", "afljafljasl.fasta", "xyz.csfasta", "zppldgoe.fa", "afafiilrwe.fasta" };
            assertEquals(1, translaterSubcommand.run(args));
            assertEquals(0, testdataDirectory.list().length);
        }
        System.out.println("Testing, that you cannot omit the alphabet map by a FASTA file.");
        for (String option : new String[] { "--trim", "--masked", "--reverse", "-r", "--runs" }) {
            args = new String[] { option, "abc.fasta" };
            assertEquals(1, translaterSubcommand.run(args));
            assertEquals(0, testdataDirectory.list().length);
            args = new String[] { option, "xyasdgfasdf.fa" };
            assertEquals(1, translaterSubcommand.run(args));
            assertEquals(0, testdataDirectory.list().length);
        }
    }

    /**
    * Tests <code>verjinxer tr -c data/colorspace.fa<code>
    * 
    * @throws IOException
    */
    @Test
    public void testRunFastaWithC() throws IOException {
        String[] args = { "-i", testdataDirectory.getPath() + File.separator + "colorspace", "-c", "data/colorspace.fa" };
        int ret = translaterSubcommand.run(args);
        assertEquals(0, ret);
        assertEqualFiles("data" + File.separator + "colorspace.seq", testdataDirectory.getAbsolutePath() + File.separator + "colorspace.seq");
    }

    /**
    * Tests <code>verjinxer tr data/colorspace.csfasta<code>
    * 
    * @throws IOException
    */
    @Test
    public void testRunCSFASTA() throws IOException {
        String[] args = { "-i", testdataDirectory.getPath() + File.separator + "colorspace", "data/colorspace.csfasta" };
        int ret = translaterSubcommand.run(args);
        assertEquals(0, ret);
        assertEqualFiles("data" + File.separator + "colorspace.seq", testdataDirectory.getAbsolutePath() + File.separator + "colorspace.seq");
    }

    /**
    * Tests if the given files have equal content.
    * 
    * @param filename1
    * @param filename2
    * @throws IOException
    */
    private void assertEqualFiles(String filename1, String filename2) throws IOException {
        File file1 = new File(filename1);
        File file2 = new File(filename2);
        System.out.printf("Comparing the files %s and %s.%n", file1.getAbsoluteFile(), file2.getAbsoluteFile());
        FileInputStream fileStream1 = new FileInputStream(file1);
        FileInputStream fileStream2 = new FileInputStream(file2);
        FileChannel channel1 = fileStream1.getChannel();
        FileChannel channel2 = fileStream2.getChannel();
        assertEquals(String.format("The files %s and %s have different length.", filename1, filename2), file1.length(), file2.length());
        ByteBuffer buffer1 = ByteBuffer.allocate(file1.length() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) file1.length());
        ByteBuffer buffer2 = ByteBuffer.allocate(file2.length() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) file2.length());
        int pos1 = 1;
        int pos2 = 1;
        while (pos1 == pos2 && pos1 > 0) {
            System.out.printf("Comparing position %d.%n", pos1);
            pos1 = channel1.read(buffer1);
            pos2 = channel2.read(buffer2);
            assertEquals(String.format("The files %s and %s have different content.", filename1, filename2), 0, buffer1.compareTo(buffer2));
        }
        assertEquals(String.format("Reading the files %s and %s had end at different positions.", filename1, filename2), pos1, pos2);
        channel1.close();
        channel2.close();
        fileStream1.close();
        fileStream2.close();
    }
}
