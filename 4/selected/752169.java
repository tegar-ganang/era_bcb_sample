package gate.yam.depend;

import gate.persist.PersistenceException;
import gate.util.GateException;
import gate.yam.YamFile;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;
import org.springframework.core.io.FileSystemResource;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tests for Dependencies   
 * @author Angus Roberts
 */
public class DependenciesTest extends TestCase {

    /** Logger */
    static Logger log = Logger.getLogger("gate.yam.depend.DependenciesTest");

    /**
   * Matches the names of YamFiles as used in testing, capturing the numeric
   * part for use as a shorthand. (see @link #shorthandToPaths(String).
   * YamFiles used in testing are named using a conventionally, as defined in
   * this pattern.
   */
    private static Pattern testFilePattern = Pattern.compile("yam-depends-(\\d+).yam");

    /**
   * Some test files come in to versions - their original, and their modified
   * version. This pattern matches the original, capturing the parts that make
   * up the name of the actual file as used in tests.
   */
    private static Pattern originalTestFilePattern = Pattern.compile("(yam-depends-)ORIGINAL-(\\d+.yam)");

    /**
   * A FilenameFilter that accepts yam files used by DependenciesTest.
   * By convention, filenames are "yam-depends-\d+.yam"
   */
    class DependenciesTestFileFilter implements FilenameFilter {

        /** Accept a file if it is a yam file used in DependenciesTest*/
        public boolean accept(File dir, String name) {
            return testFilePattern.matcher(name).matches();
        }
    }

    /**
   * A FilenameFilter that accepts original versions of yam files used by
   * DependenciesTest. By convention, filenames are
   * "yam-depends-ORIGINAL-\d+.yam"
   */
    class DependenciesTestOriginalFileFilter implements FilenameFilter {

        /** Accept a file if it is the original version of
     * a yam file used in DependenciesTest
     */
        public boolean accept(File dir, String name) {
            return originalTestFilePattern.matcher(name).matches();
        }
    }

    /**
   * The directory that test yam files are found in.
   */
    private static File yamDir;

    /**
   * By convention, each yam file used in testing is named
   * "yam-depends-\d+.yam". This Map maps the digit part to the full name.
   * It is used so that we can refer to sets and graphs of files by just their
   * digit part.
   */
    Map<String, String> yamFileNameMap = new HashMap<String, String>();

    /**
   * As for @link #yamFileNameMap, but maps the digit part of the name to the
   * actual YamFile, post-generate().
   */
    Map<String, YamFile> yamFileMap = new HashMap<String, YamFile>();

    /**
   * A List of parsed YamFiles that make up a wiki
   */
    private List<YamFile> wiki1 = new ArrayList<YamFile>();

    /**
   * A Dependencies instance for wiki1
   */
    private Dependencies dep1;

    /** Create a Dependencies test case
   *
   * @param testName  The name of the test
   */
    public DependenciesTest(String testName) {
        super(testName);
    }

    /**
   * Set up the tests for Dependencies
   */
    protected void setUp() {
        String testDirName = System.getProperty("gate.yam.depend.test.dir");
        if (testDirName == null) {
            testDirName = "test/scratch/dependencies";
        }
        File testDirFile = new File(testDirName);
        testDirFile.mkdirs();
        Dependencies.setSerializationDirectory(testDirFile);
        String yamDirName = System.getProperty("java.yam.resources.dir");
        if (yamDirName == null) {
            yamDirName = this.getClass().getResource("/gate/yam/resources").getFile();
        }
        log.info("Getting test yam files from: " + yamDirName);
        yamDir = new File(yamDirName);
        File[] originalVersions = yamDir.listFiles(new DependenciesTestOriginalFileFilter());
        for (File origFile : originalVersions) {
            Matcher matcher = originalTestFilePattern.matcher(origFile.getName());
            if (!matcher.matches()) {
                fail("Failed to setup - filename not conventional: " + origFile.getName());
            }
            String newName = matcher.group(1) + matcher.group(2);
            File newFile = new File(yamDir, newName);
            copy(origFile, newFile);
        }
        File[] testFiles = yamDir.listFiles(new DependenciesTestFileFilter());
        for (File file : testFiles) {
            YamFile yamFile = YamFile.get(new FileSystemResource(file));
            try {
                String canPath = yamFile.getCanonicalPath();
                log.info("Getting test file: " + canPath);
                Matcher matcher = testFilePattern.matcher(file.getName());
                if (!matcher.matches()) {
                    fail("Failed to setup - filename not conventional: " + file.getName());
                }
                String number = matcher.group(1);
                yamFile.generate();
                yamFileNameMap.put(number, canPath);
                yamFileMap.put(number, yamFile);
            } catch (GateException ge) {
                fail("Failed to setup: " + ge.getMessage());
            }
        }
        wiki1.add(yamFileMap.get("1"));
        wiki1.add(yamFileMap.get("2"));
        wiki1.add(yamFileMap.get("3"));
        wiki1.add(yamFileMap.get("4"));
        wiki1.add(yamFileMap.get("5"));
        try {
            Dependencies.remove("1");
            dep1 = Dependencies.get("1");
        } catch (PersistenceException pe) {
            fail("Failed to set up: " + pe.getMessage());
        }
        assertTrue("New Dependencies not empty", dep1.isEmpty());
        for (YamFile yam : wiki1) {
            dep1.created(yam);
        }
        assertFalse("Dependencies is empty after adding links", dep1.isEmpty());
    }

    /**
   * Test a few Dependencies basics: equality, hashCode, YamFile creation
   * and removal.
   * @throws Exception if the test fails
   */
    public void testCreateAndBasics() throws Exception {
        log.info("========== DependenciesTest.tesCreateAndBasics() ==============");
        log.info("testing dependencies.created(YamFile) and equality methods");
        Dependencies dep2 = Dependencies.get("2");
        for (YamFile yam : wiki1) {
            dep2.created(yam);
        }
        assertEquals("Dependencies linksTo not correct after create", shorthandToPaths("1:[2,3];2:[4,5];3:[5]"), dep2.linksToAsString());
        assertEquals("Dependencies linkedBy not correct after create", shorthandToPaths("2:[1];3:[1];4:[2];5:[2,3]"), dep2.linkedByAsString());
        assertEquals("Dependencies includes not correct after create", shorthandToPaths("1:[2,3,4];2:[4]"), dep2.includesAsString());
        assertEquals("Dependencies includedBy not correct after create", shorthandToPaths("2:[1];3:[1];4:[1,2]"), dep2.includedByAsString());
        assertEquals("Same Dependencies not equal", dep1, dep1);
        assertEquals("Dependencies not equal after get", dep1, Dependencies.get("1"));
        assertEquals("Identical Dependencies not equal", dep1, dep2);
        assertEquals("Identical Dependencies with different hash codes", dep1.hashCode(), dep2.hashCode());
        Dependencies.remove("2");
        assertFalse("Dependencies still exists after removal", Dependencies.exists("2"));
    }

    /**
   * Test Dependencies YamFile deletion
   * @throws Exception if the test fails
   */
    public void testDelete() throws Exception {
        log.info("============== DependenciesTest.testDelete() ==================");
        log.info("testing dependencies.deleted(YamFile)");
        Set<String> toRegenerate;
        toRegenerate = dep1.deleted(yamFileMap.get("1"));
        assertEquals("Regenerate set not correct after delete", new HashSet<String>(), toRegenerate);
        assertEquals("Dependencies linksTo not correct after delete", shorthandToPaths("2:[4,5];3:[5]"), dep1.linksToAsString());
        assertEquals("Dependencies linkedBy not correct after delete", shorthandToPaths("4:[2];5:[2,3]"), dep1.linkedByAsString());
        assertEquals("Dependencies includes not correct after delete", shorthandToPaths("2:[4]"), dep1.includesAsString());
        assertEquals("Dependencies includedBy not correct after delete", shorthandToPaths("4:[2]"), dep1.includedByAsString());
        toRegenerate = dep1.created(yamFileMap.get("1"));
        assertEquals("Regenerate set not correct after create", new HashSet<String>(), toRegenerate);
        assertEquals("Dependencies linksTo not correct after create", shorthandToPaths("1:[2,3];2:[4,5];3:[5]"), dep1.linksToAsString());
        assertEquals("Dependencies linkedBy not correct after create", shorthandToPaths("2:[1];3:[1];4:[2];5:[2,3]"), dep1.linkedByAsString());
        assertEquals("Dependencies includes not correct after create", shorthandToPaths("1:[2,3,4];2:[4]"), dep1.includesAsString());
        assertEquals("Dependencies includedBy not correct after create", shorthandToPaths("2:[1];3:[1];4:[1,2]"), dep1.includedByAsString());
    }

    /**
   * Test Dependencies YamFile renaming
   * @throws Exception if the test fails
   */
    public void testRename() throws Exception {
        log.info("============== DependenciesTest.testRename() ==================");
        log.info("testing dependencies.renamed(YamFile)");
        Set<String> toRegenerate;
        List<String> toRegenerateSorted;
        toRegenerate = dep1.renamed(yamFileMap.get("2"), yamFileMap.get("6"));
        toRegenerateSorted = new ArrayList<String>(toRegenerate);
        Collections.sort(toRegenerateSorted);
        assertEquals("Regenerate set not correct after rename", numbersToList("1"), toRegenerateSorted);
        assertEquals("Dependencies linksTo not correct after rename", shorthandToPaths("1:[3,6];3:[5];6:[4,5]"), dep1.linksToAsString());
        assertEquals("Dependencies linkedBy not correct after rename", shorthandToPaths("3:[1];4:[6];5:[3,6];6:[1]"), dep1.linkedByAsString());
        assertEquals("Dependencies includes not correct after rename", shorthandToPaths("1:[3,4,6];6:[4]"), dep1.includesAsString());
        assertEquals("Dependencies includedBy not correct after rename", shorthandToPaths("3:[1];4:[1,6];6:[1]"), dep1.includedByAsString());
        dep1.renamed(yamFileMap.get("6"), yamFileMap.get("2"));
    }

    /**
   * Test Dependencies - further YamFile deletion tests
   * @throws Exception if the test fails
   */
    public void testDelete2() throws Exception {
        log.info("============== DependenciesTest.testDelete2() =================");
        log.info("testing dependencies.deleted(YamFile)");
        Set<String> toRegenerate;
        List<String> toRegenerateSorted;
        toRegenerate = dep1.deleted(yamFileMap.get("4"));
        toRegenerateSorted = new ArrayList<String>(toRegenerate);
        Collections.sort(toRegenerateSorted);
        assertEquals("Regenerate set not correct after delete", numbersToList("1,2"), toRegenerateSorted);
        toRegenerate = dep1.deleted(yamFileMap.get("5"));
        toRegenerateSorted = new ArrayList<String>(toRegenerate);
        Collections.sort(toRegenerateSorted);
        assertEquals("Regenerate set not correct after delete", numbersToList("2,3"), toRegenerateSorted);
        dep1.created(yamFileMap.get("4"));
        dep1.created(yamFileMap.get("5"));
        assertEquals("Dependencies linksTo not correct after delete and create", shorthandToPaths("1:[2,3];2:[4,5];3:[5]"), dep1.linksToAsString());
        assertEquals("Dependencies linkedBy not correct after delete and create", shorthandToPaths("2:[1];3:[1];4:[2];5:[2,3]"), dep1.linkedByAsString());
        assertEquals("Dependencies includes not correct after delete and create", shorthandToPaths("1:[2,3,4];2:[4]"), dep1.includesAsString());
        assertEquals("Dependencies includedBy not correct after delete and create", shorthandToPaths("2:[1];3:[1];4:[1,2]"), dep1.includedByAsString());
    }

    /**
   * Test Dependencies YamFile modification
   * @throws Exception if the test fails
   */
    public void testModify() throws Exception {
        log.info("============== DependenciesTest.testModify() ==================");
        log.info("testing dependencies.modified(YamFile)");
        Set<String> toRegenerate;
        List<String> toRegenerateSorted;
        File currentFile = new File(yamDir, "yam-depends-2.yam");
        File modFile = new File(yamDir, "yam-depends-MODIFIED-2.yam");
        copy(modFile, currentFile);
        yamFileMap.get("2").generate();
        toRegenerate = dep1.modified(yamFileMap.get("2"));
        toRegenerateSorted = new ArrayList<String>(toRegenerate);
        Collections.sort(toRegenerateSorted);
        assertEquals("Regenerate set not correct after modify", numbersToList("1"), toRegenerateSorted);
        assertEquals("Dependencies linksTo not correct after modify", shorthandToPaths("1:[2,3];2:[4,7];3:[5]"), dep1.linksToAsString());
        assertEquals("Dependencies linkedBy not correct after modify", shorthandToPaths("2:[1];3:[1];4:[2];5:[3];7:[2]"), dep1.linkedByAsString());
        assertEquals("Dependencies includes not correct after modify", shorthandToPaths("1:[2,3,4];2:[4]"), dep1.includesAsString());
        assertEquals("Dependencies includedBy not correct after modify", shorthandToPaths("2:[1];3:[1];4:[1,2]"), dep1.includedByAsString());
        currentFile = new File(yamDir, "yam-depends-1.yam");
        modFile = new File(yamDir, "yam-depends-MODIFIED-1.yam");
        copy(modFile, currentFile);
        yamFileMap.get("1").generate();
        toRegenerate = dep1.modified(yamFileMap.get("1"));
        assertEquals("Regenerate set not correct after modify", new HashSet<String>(), toRegenerate);
        assertEquals("Dependencies linksTo not correct after modify", shorthandToPaths("1:[2,3];2:[4,7];3:[5]"), dep1.linksToAsString());
        assertEquals("Dependencies linkedBy not correct after modify", shorthandToPaths("2:[1];3:[1];4:[2];5:[3];7:[2]"), dep1.linkedByAsString());
        assertEquals("Dependencies includes not correct after modify", shorthandToPaths("1:[3,4];2:[4]"), dep1.includesAsString());
        assertEquals("Dependencies includedBy not correct after modify", shorthandToPaths("3:[1];4:[1,2]"), dep1.includedByAsString());
        copy(new File(yamDir, "yam-depends-ORIGINAL-1.yam"), new File(yamDir, "yam-depends-1.yam"));
        copy(new File(yamDir, "yam-depends-ORIGINAL-2.yam"), new File(yamDir, "yam-depends-2.yam"));
        yamFileMap.get("1").generate();
        yamFileMap.get("2").generate();
    }

    /**
   * Test links to non-yam files and urls
   * @throws Exception if the test fails
   */
    public void testNonYamLinks() throws Exception {
        log.info("============ DependenciesTest.testNonYamLinks() ===============");
        log.info("testing links to non yam files and urls");
        YamFile yf = yamFileMap.get("8");
        wiki1.add(yf);
        dep1.created(yf);
        assertEquals("Dependencies linksTo not correct after create", shorthandToPaths("1:[2,3];2:[4,5];3:[5];" + "8:[../../non-existent.html,non-existent.html]"), dep1.linksToAsString());
        assertEquals("Dependencies linkedBy not correct after create", shorthandToPaths("../../non-existent.html:[8];" + "" + "non-existent.html:[8];2:[1];3:[1];4:[2];5:[2,3]"), dep1.linkedByAsString());
        assertEquals("Dependencies includes not correct after create", shorthandToPaths("1:[2,3,4];2:[4]"), dep1.includesAsString());
        assertEquals("Dependencies includedBy not correct after create", shorthandToPaths("2:[1];3:[1];4:[1,2]"), dep1.includedByAsString());
        dep1.deleted(yf);
        assertEquals("Dependencies linksTo not correct after delete", shorthandToPaths("1:[2,3];2:[4,5];3:[5]"), dep1.linksToAsString());
        assertEquals("Dependencies linkedBy not correct after delete", shorthandToPaths("2:[1];3:[1];4:[2];5:[2,3]"), dep1.linkedByAsString());
        assertEquals("Dependencies includes not correct after delete", shorthandToPaths("1:[2,3,4];2:[4]"), dep1.includesAsString());
        assertEquals("Dependencies includedBy not correct after delete", shorthandToPaths("2:[1];3:[1];4:[1,2]"), dep1.includedByAsString());
    }

    /**
   * Test Dependencies File creation, deletion, and renaming
   * @throws Exception if the test fails
   */
    public void testNonYamChanges() throws Exception {
        log.info("========= DependenciesTest.testNonYamChanges() =============");
        Set<String> toRegenerate;
        List<String> toRegenerateSorted;
        log.info("testing dependencies.created(File)");
        File nonYam = new File(yamDir, "nonYamFile.abc");
        toRegenerate = dep1.created(nonYam);
        assertEquals("Regenerate set not correct after created(File)", new HashSet<String>(), toRegenerate);
        assertEquals("Dependencies linksTo not correct after create", shorthandToPaths("1:[2,3];2:[4,5];3:[5]"), dep1.linksToAsString());
        assertEquals("Dependencies linkedBy not correct after create", shorthandToPaths("2:[1];3:[1];4:[2];5:[2,3]"), dep1.linkedByAsString());
        assertEquals("Dependencies includes not correct after create", shorthandToPaths("1:[2,3,4];2:[4]"), dep1.includesAsString());
        assertEquals("Dependencies includedBy not correct after create", shorthandToPaths("2:[1];3:[1];4:[1,2]"), dep1.includedByAsString());
        toRegenerate = dep1.deleted(nonYam);
        assertEquals("Regenerate set not correct after deleted(File)", new HashSet<String>(), toRegenerate);
        YamFile linkingFile = yamFileMap.get("9");
        wiki1.add(linkingFile);
        toRegenerate = dep1.created(linkingFile);
        assertEquals("Regenerate set not correct after created(YamFile)", new HashSet<String>(), toRegenerate);
        assertEquals("Dependencies linksTo not correct after create", shorthandToPaths("1:[2,3];2:[4,5];3:[5];9:[nonYamFile.abc]"), dep1.linksToAsString());
        assertEquals("Dependencies linkedBy not correct after create", shorthandToPaths("nonYamFile.abc:[9];2:[1];3:[1];4:[2];5:[2,3]"), dep1.linkedByAsString());
        assertEquals("Dependencies includes not correct after create", shorthandToPaths("1:[2,3,4];2:[4]"), dep1.includesAsString());
        assertEquals("Dependencies includedBy not correct after create", shorthandToPaths("2:[1];3:[1];4:[1,2]"), dep1.includedByAsString());
        toRegenerate = dep1.created(nonYam);
        toRegenerateSorted = new ArrayList<String>(toRegenerate);
        Collections.sort(toRegenerateSorted);
        assertEquals("Regenerate set not correct after created(File)", numbersToList("9"), toRegenerateSorted);
        log.info("testing dependencies.renamed(File, File)");
        File nonYamRenamed = new File(yamDir, "nonYamFileRenamed.abc");
        toRegenerate = dep1.renamed(nonYam, nonYamRenamed);
        toRegenerateSorted = new ArrayList<String>(toRegenerate);
        Collections.sort(toRegenerateSorted);
        assertEquals("Regenerate set not correct after renamed(File)", numbersToList("9"), toRegenerateSorted);
        assertEquals("Dependencies linksTo not correct after rename", shorthandToPaths("1:[2,3];2:[4,5];3:[5];9:[nonYamFileRenamed.abc]"), dep1.linksToAsString());
        assertEquals("Dependencies linkedBy not correct after rename", shorthandToPaths("nonYamFileRenamed.abc:[9];2:[1];3:[1];4:[2]" + ";5:[2,3]"), dep1.linkedByAsString());
        assertEquals("Dependencies includes not correct after rename", shorthandToPaths("1:[2,3,4];2:[4]"), dep1.includesAsString());
        assertEquals("Dependencies includedBy not correct after rename", shorthandToPaths("2:[1];3:[1];4:[1,2]"), dep1.includedByAsString());
        log.info("testing dependencies.deleted(File)");
        toRegenerate = dep1.deleted(nonYamRenamed);
        toRegenerateSorted = new ArrayList<String>(toRegenerate);
        Collections.sort(toRegenerateSorted);
        assertEquals("Regenerate set not correct after deleted(File)", numbersToList("9"), toRegenerateSorted);
        assertEquals("Dependencies linksTo not correct after delete", shorthandToPaths("1:[2,3];2:[4,5];3:[5];9:[nonYamFileRenamed.abc]"), dep1.linksToAsString());
        assertEquals("Dependencies linkedBy not correct after delete", shorthandToPaths("nonYamFileRenamed.abc:[9];2:[1];3:[1];4:[2]" + ";5:[2,3]"), dep1.linkedByAsString());
        assertEquals("Dependencies includes not correct after delete", shorthandToPaths("1:[2,3,4];2:[4]"), dep1.includesAsString());
        assertEquals("Dependencies includedBy not correct after delete", shorthandToPaths("2:[1];3:[1];4:[1,2]"), dep1.includedByAsString());
        toRegenerate = dep1.deleted(linkingFile);
        assertEquals("Regenerate set not correct after deleted(YamFile)", new HashSet<String>(), toRegenerate);
        assertEquals("Dependencies linksTo not correct after delete", shorthandToPaths("1:[2,3];2:[4,5];3:[5]"), dep1.linksToAsString());
        assertEquals("Dependencies linkedBy not correct after delete", shorthandToPaths("2:[1];3:[1];4:[2]" + ";5:[2,3]"), dep1.linkedByAsString());
        assertEquals("Dependencies includes not correct after delete", shorthandToPaths("1:[2,3,4];2:[4]"), dep1.includesAsString());
        assertEquals("Dependencies includedBy not correct after delete", shorthandToPaths("2:[1];3:[1];4:[1,2]"), dep1.includedByAsString());
    }

    /**
   * Test Dependencies serialization
   * @throws Exception if the test fails
   */
    public void testSerialization() throws Exception {
        log.info("============= DependenciesTest.testSerialization() ============");
        log.info("testing serialization and deserialization of Dependencies");
        Dependencies.remove("1");
        Dependencies dep1 = Dependencies.get("1");
        for (YamFile yam : wiki1) {
            dep1.created(yam);
        }
        Dependencies.serialize();
        Dependencies.clear();
        assertTrue("Dependencies removed", Dependencies.exists("1"));
        Dependencies dep1Reloaded = Dependencies.get("1");
        assertEquals("Dependencies not consistently serialized / deserialized", dep1, dep1Reloaded);
        Dependencies.remove("1");
    }

    /**
   * Suite of tests for Dependencies
   * @return The suite of tests
   */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new DependenciesTest("testCreateAndBasics"));
        suite.addTest(new DependenciesTest("testDelete"));
        suite.addTest(new DependenciesTest("testRename"));
        suite.addTest(new DependenciesTest("testDelete2"));
        suite.addTest(new DependenciesTest("testModify"));
        suite.addTest(new DependenciesTest("testNonYamLinks"));
        suite.addTest(new DependenciesTest("testNonYamChanges"));
        suite.addTest(new DependenciesTest("testSerialization"));
        return suite;
    }

    /**
   * <p>Replace all file numbers in str with the equivalent canonical paths in
   * yamFileNameMap. All files used in Dependencies testing are named by
   * convention as "yam-depends-\d+.yam". Dependencies will report its
   * internal state graphs in the form "pathA:[pathB,pathC];pathB[pathD]",
   * where pathX is the canonical path of a file.</p>
   * <p>For test files, we can refer to a Dependencies graph in a shorthand form
   * using just the digit part of the test name, e.g. "1:[2,3];2[4]" and then
   * translate this to full canonical paths using this method.</p>
   * <p>Non-yam files are referred to by their path relative to their linking
   * node. They are replaced by their full canonical path. For example,
   * "1:[some-file.html]", where some-file is not from yam, will become
   * "/parent/path/yam-depends-1.yam:[/parent/path/some-file.html]"
   * </p>
   * @param str A string in which we want to replace numbers with test file
   * canonical paths
   * @return   The equivalent string with numbers replaced by test file
   * canonical paths
   */
    public String shorthandToPaths(String str) {
        StringBuilder bldr = new StringBuilder();
        for (String nodeAndArcs : str.split("(\\]\\;)|(\\])")) {
            String[] nodeAndArcsSplit = nodeAndArcs.split("\\:\\[");
            String node = nodeAndArcsSplit[0];
            String arcs = nodeAndArcsSplit[1];
            String nodePath = null;
            if (node.matches("\\d+")) {
                nodePath = yamFileNameMap.get(node);
            } else {
                File yamParent = new File(yamFileNameMap.get(arcs)).getParentFile();
                try {
                    nodePath = new File(yamParent, node).getCanonicalPath();
                } catch (IOException ioe) {
                    fail("Couldn't resolve link canonical path: " + ioe.getMessage());
                }
            }
            bldr.append(nodePath);
            bldr.append(":[");
            for (String arc : arcs.split("\\,")) {
                if (arc.matches("\\d+")) {
                    bldr.append(yamFileNameMap.get(arc));
                } else {
                    File yamParent = new File(nodePath).getParentFile();
                    File link = new File(yamParent, arc);
                    try {
                        bldr.append(link.getCanonicalPath());
                    } catch (IOException ioe) {
                        fail("Couldn't resolve link canonical path: " + ioe.getMessage());
                    }
                }
                bldr.append(",");
            }
            bldr.deleteCharAt(bldr.length() - 1);
            bldr.append("];");
        }
        bldr.deleteCharAt(bldr.length() - 1);
        return bldr.toString();
    }

    /**
   * Replace all file numbers in str with the equivalent canonical paths in
   * yamFileNameMap, as described in
   * @link DependenciesTest#shorthandToPaths(String).
   * By convention, the file numbers in str are comma separated
   * @param str A comma separated list of numbers
   * @return A List of canonical paths equivalent to the numbers in str
   */
    private List<String> numbersToList(String str) {
        List<String> pathList = new ArrayList<String>();
        for (String number : str.split(",")) {
            pathList.add(yamFileNameMap.get(number));
        }
        return pathList;
    }

    /**
   * Copy one File to another. Testing fails if the copy fails.
   * @param in The File that will be copied
   * @param out The File to which in will be copied
   */
    private void copy(File in, File out) {
        log.info("Copying yam file from: " + in.getName() + " to: " + out.getName());
        try {
            FileChannel ic = new FileInputStream(in).getChannel();
            FileChannel oc = new FileOutputStream(out).getChannel();
            ic.transferTo(0, ic.size(), oc);
            ic.close();
            oc.close();
        } catch (IOException ioe) {
            fail("Failed testing while copying modified file: " + ioe.getMessage());
        }
    }
}
