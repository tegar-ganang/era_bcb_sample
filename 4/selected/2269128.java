package net.assimilator.codeserver.codestore;

import junit.framework.TestCase;
import net.assimilator.codeserver.JarDescriptor;
import net.assimilator.codeserver.RelConfigParameters;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Logger;

public class CodeStoreManagerImplTest extends TestCase {

    /**
     * the logger for this class
     */
    private static final Logger logger = Logger.getLogger("net.assimilator.codeserver.codestore");

    private static final String onlyKnownPath = "Assimilator";

    private String relativePath = "modules/experimental/test/net/assimilator/codeserver/";

    String pathToHere;

    String testjars;

    private static final String testDir = "./testCD";

    private static final String jarstorePath = "./testCD/jarstore";

    CodeStoreManagerImpl codeStoreManagerImpl;

    private RelConfigParameters relConfig;

    @Override
    public void setUp() {
        String currentPath = new File(".'").getAbsolutePath().replace('\\', '/');
        pathToHere = getCorrectPath(currentPath);
        testjars = pathToHere + relativePath + "testjars";
        relConfig = new RelConfigParameters();
    }

    @Override
    public void tearDown() {
        removeCodeStore();
    }

    public void testCodeStoreManagerImplCreateCodeDir() throws Exception {
        doCreation();
        createCodeStoreWithFiles();
    }

    public void testCodeStoreManagerImplClearTrue() throws Exception {
        createCodeStoreWithFiles();
        relConfig.setCodebaseDirectory(testDir);
        relConfig.setInitialClearState(true);
        relConfig.setPersistentState(false);
        codeStoreManagerImpl = new CodeStoreManagerImpl(relConfig);
        File file = new File(testDir);
        if (!file.exists()) {
            fail("codebase dir was not created");
        }
        File file2 = new File(testDir + File.separatorChar + "jarstore");
        if (!file2.exists()) {
            fail("jarstore dir was not created");
        }
    }

    public void testShutdownTransient() throws Exception {
        String testFileName1 = "cybernode-dl.jar";
        String testFileName2 = "platform-capabilities.jar";
        String testFileName3 = "webster-dl.jar";
        createCodeStoreWithFiles();
        relConfig.setCodebaseDirectory(testDir);
        relConfig.setInitialClearState(false);
        relConfig.setPersistentState(false);
        codeStoreManagerImpl = new CodeStoreManagerImpl(relConfig);
        File file = new File(testDir);
        if (!file.exists()) {
            fail("codebase dir was not created");
        }
        File file2 = new File(testDir + File.separatorChar + "jarstore");
        if (!file2.exists()) {
            fail("jarstore dir was not created");
        }
        createCodeStoreWithFiles();
        codeStoreManagerImpl.shutdown();
        doTestRemoval(testFileName1);
        doTestRemoval(testFileName2);
        doTestRemoval(testFileName3);
    }

    public void testShutdownPersistent() throws Exception {
        String testFileName1 = "cybernode-dl.jar";
        String testFileName2 = "platform-capabilities.jar";
        String testFileName3 = "webster-dl.jar";
        createCodeStoreWithFiles();
        relConfig.setCodebaseDirectory(testDir);
        relConfig.setInitialClearState(false);
        relConfig.setPersistentState(true);
        codeStoreManagerImpl = new CodeStoreManagerImpl(relConfig);
        File file = new File(testDir);
        if (!file.exists()) {
            fail("codebase dir was not created");
        }
        File file2 = new File(testDir + File.separatorChar + "jarstore");
        if (!file2.exists()) {
            fail("jarstore dir was not created");
        }
        createCodeStoreWithFiles();
        codeStoreManagerImpl.shutdown();
        doTestExistence(testFileName1);
        doTestExistence(testFileName2);
        doTestExistence(testFileName3);
    }

    public void testClear() throws Exception {
        String testFileName1 = "cybernode-dl.jar";
        String testFileName2 = "platform-capabilities.jar";
        String testFileName3 = "webster-dl.jar";
        doCreation();
        doCreateOfFile(testFileName1);
        doCreateOfFile(testFileName2);
        doCreateOfFile(testFileName3);
        doTestExistence(testFileName1);
        doTestExistence(testFileName2);
        doTestExistence(testFileName3);
        codeStoreManagerImpl.clear();
        doTestRemoval(testFileName1);
        doTestRemoval(testFileName2);
        doTestRemoval(testFileName3);
    }

    public void testInstallCodeBase() throws Exception {
        String testFileName = "cybernode-dl.jar";
        doCreation();
        doCreateOfFile(testFileName);
        doTestExistence(testFileName);
    }

    public void testRemoveCodeBase() throws Exception {
        String testFileName = "cybernode-dl.jar";
        doCreation();
        doCreateOfFile(testFileName);
        doTestExistence(testFileName);
        codeStoreManagerImpl.removeCodeBase(testFileName);
        doTestRemoval(testFileName);
    }

    public void testGetJarsInRepository() throws Exception {
        String testFileName1 = "cybernode-dl.jar";
        String testFileName2 = "platform-capabilities.jar";
        String testFileName3 = "webster-dl.jar";
        doCreation();
        doCreateOfFile(testFileName1);
        doCreateOfFile(testFileName2);
        doCreateOfFile(testFileName3);
        doTestExistence(testFileName1);
        doTestExistence(testFileName2);
        doTestExistence(testFileName3);
        List<String> jarMap = codeStoreManagerImpl.getJarsInRepository();
        if (jarMap.size() != 3) {
            fail("retrieved jars is=" + jarMap.size() + " should be=3");
        }
        if (!jarMap.contains(testFileName1)) {
            fail("expected:" + testFileName1 + " but was not there");
        }
        if (!jarMap.contains(testFileName2)) {
            fail("expected:" + testFileName2 + " but was not there");
        }
        if (!jarMap.contains(testFileName3)) {
            fail("expected:" + testFileName3 + " but was not there");
        }
    }

    public void testUpdateCodeBase() throws Exception {
        String testFileName = "cybernode-dl.jar";
        String testFileNameUpdate = "cybernode-dl.jar";
        doCreation();
        doCreateOfFile(testFileName);
        doTestExistence(testFileName);
        File testJarsDir = new File(testjars);
        File jarToRead = new File(testJarsDir + File.separator + testFileNameUpdate);
        byte[] fileRead = readFileIntoArray(jarToRead);
        JarDescriptor jarDescriptor = new JarDescriptor(jarToRead.getName(), "1", "2");
        codeStoreManagerImpl.updateCodeBase(jarDescriptor, fileRead);
    }

    private void doTestExistence(String testFileName) throws IOException {
        File dir = new File(jarstorePath);
        File fileWritten = new File(dir.getCanonicalPath() + File.separator + testFileName);
        if (!fileWritten.exists()) {
            fail("expected file:" + fileWritten.getCanonicalPath() + "was not written");
        }
    }

    private void doTestRemoval(String testFileName) throws IOException {
        File dir = new File(jarstorePath);
        File fileWritten = new File(dir.getCanonicalPath() + File.separator + testFileName);
        if (fileWritten.exists()) {
            fail("expected file:" + fileWritten.getCanonicalPath() + "was not removed");
        }
    }

    private void doCreateOfFile(String testFileName1) {
        File testJarsDir = new File(testjars);
        File jarToRead = new File(testJarsDir + File.separator + testFileName1);
        byte[] fileRead = readFileIntoArray(jarToRead);
        JarDescriptor jarDescriptor = new JarDescriptor(jarToRead.getName(), "1", "1");
        codeStoreManagerImpl.installCodeBase(jarDescriptor, fileRead);
    }

    private void doCreation() {
        relConfig.setCodebaseDirectory(testDir);
        relConfig.setInitialClearState(false);
        relConfig.setPersistentState(false);
        codeStoreManagerImpl = new CodeStoreManagerImpl(relConfig);
        File file = new File(testDir);
        if (!file.exists()) {
            fail("codebase dir was not created");
        }
        File file2 = new File(testDir + File.separatorChar + "jarstore");
        if (!file2.exists()) {
            fail("jarstore dir was not created");
        }
    }

    private void createCodeStoreWithFiles() {
        File dir = new File(jarstorePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File testJarsDir = new File(testjars);
        for (File fileToAdd : testJarsDir.listFiles()) {
            if (fileToAdd.getName().equals(".svn")) {
                continue;
            }
            copyFile(dir, fileToAdd);
        }
    }

    private void copyFile(File dir, File fileToAdd) {
        try {
            byte[] readBuffer = new byte[1024];
            File file = new File(dir.getCanonicalPath() + File.separatorChar + fileToAdd.getName());
            if (file.createNewFile()) {
                FileInputStream fis = new FileInputStream(fileToAdd);
                FileOutputStream fos = new FileOutputStream(file);
                int bytesRead;
                do {
                    bytesRead = fis.read(readBuffer);
                    fos.write(readBuffer, 0, bytesRead);
                } while (bytesRead == 0);
                fos.flush();
                fos.close();
                fis.close();
            } else {
                logger.severe("unable to create file:" + file.getAbsolutePath());
            }
        } catch (IOException ioe) {
            logger.severe("unable to create file:" + ioe);
        }
    }

    private void removeCodeStore() {
        File file = new File(jarstorePath);
        for (File fileToDelete : file.listFiles()) {
            fileToDelete.delete();
        }
        file.delete();
        File file2 = new File(testDir);
        file2.delete();
    }

    private byte[] readFileIntoArray(File fileToRead) {
        byte[] readBuffer = new byte[(int) fileToRead.length()];
        try {
            FileInputStream fis = new FileInputStream(fileToRead);
            int bytesRead = fis.read(readBuffer);
            if (bytesRead != fileToRead.length()) {
                logger.warning(MessageFormat.format("read={0} bytes of file length={1} bytes", bytesRead, fileToRead.length()));
            }
            fis.close();
        } catch (IOException ioe) {
            logger.severe("unable to read the file specified:" + fileToRead);
        }
        return readBuffer;
    }

    private String getCorrectPath(String path) {
        String convertPath = "";
        String pathElements[] = path.split("/");
        int lastLocation = pathElements.length - 1;
        if (pathElements[pathElements.length - 1].equals(".")) {
            lastLocation--;
        }
        if (pathElements[lastLocation].equals(onlyKnownPath)) {
            convertPath = makePath(pathElements, lastLocation + 1, path.charAt(0) == '/');
        } else if (findAssimilatorInPath(pathElements)) {
            convertPath = fixAssimilatorInPath(pathElements, path.charAt(0) == '/');
        }
        return convertPath;
    }

    private String fixAssimilatorInPath(String pathElements[], boolean unixAbsPath) {
        StringBuffer path = new StringBuffer();
        if (unixAbsPath) {
            path.append("/");
        }
        for (String pathEl : pathElements) {
            path.append(pathEl).append("/");
            if (pathEl.equals(onlyKnownPath)) break;
        }
        return path.toString();
    }

    private boolean findAssimilatorInPath(String pathElements[]) {
        for (String pathEl : pathElements) {
            if (pathEl.equals(onlyKnownPath)) return true;
        }
        return false;
    }

    private String makePath(String[] pathElements, int end, boolean unixAbsPath) {
        StringBuffer path = new StringBuffer();
        if (unixAbsPath) {
            path.append("/");
        }
        for (int i = 0; i < end; i++) {
            path.append(pathElements[i]).append("/");
        }
        return path.toString();
    }
}
