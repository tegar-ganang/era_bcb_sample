package de.cabanis.unific.filesystem.virtual;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import de.cabanis.unific.filesystem.virtual.Operation;
import de.cabanis.unific.filesystem.virtual.OperationExecutionFailure;
import de.cabanis.unific.filesystem.virtual.OperationExecutionFailure.FailureType;
import junit.framework.TestCase;

/**
 * TODO javadoc
 * @author Nicolas Cabanis
 */
public class OperationTest extends TestCase {

    public OperationTest() {
        super("Virtual Filesystem Operation Test");
    }

    private File source = null;

    private File target = null;

    protected void setUp() throws Exception {
        source = null;
        target = null;
    }

    protected void tearDown() throws Exception {
        if (source != null && source.exists()) {
            source.delete();
        }
        if (target != null && target.exists()) {
            target.delete();
        }
    }

    public void testSuccessfulMoveOperation() {
        source = validExistentFile("source");
        target = validNonExistentFile("target");
        Operation operation = Operation.createMoveOperation(source, target);
        OperationExecutionFailure result = operation.test();
        if (result != null) {
            fail("The operation test failed: \n" + result.toString());
        }
        result = operation.execute();
        if (result != null) {
            fail("The operation execution failed: \n" + result.toString());
        }
        if (!source.exists() && target.exists()) {
        } else if (source.exists() && target.exists()) {
            fail("The source and target files exist - maybe the file was copied.");
        } else if (source.exists() && !target.exists()) {
            fail("The source file was not moved.");
        } else if (!source.exists() && !target.exists()) {
            fail("The source file was deleted without the target file being created.");
        } else {
            throw new RuntimeException("Test logic error: this point of " + "execution should not be reached.");
        }
    }

    public void testSuccessfulMoveOperationExecuteOnly() {
        source = validExistentFile("source");
        target = validNonExistentFile("target");
        Operation operation = Operation.createMoveOperation(source, target);
        OperationExecutionFailure result = operation.execute();
        if (result != null) {
            fail("The operation execution failed: \n" + result.toString());
        }
        if (!source.exists() && target.exists()) {
        } else if (source.exists() && target.exists()) {
            fail("The source and target files exist - maybe the file was copied.");
        } else if (source.exists() && !target.exists()) {
            fail("The source file was not moved.");
        } else if (!source.exists() && !target.exists()) {
            fail("The source file was deleted without the target file being created.");
        } else {
            throw new RuntimeException("Test logic error: this point of " + "execution should not be reached.");
        }
    }

    public void testSuccessfulMoveOperationToDifferentDirectory() {
        fail("not implemented");
    }

    public void testSuccessfulMoveOperationToDifferentDirectoryExecuteOnly() {
        fail("not implemented");
    }

    public void testSuccessfulMoveOperationToNonExistingDirectory() {
        fail("not implemented");
    }

    public void testSuccessfulMoveOperationToNonExistingDirectoryExecuteOnly() {
        fail("not implemented");
    }

    public void testFailedMoveOperationSourceNotExistent() {
        source = validNonExistentFile("source");
        target = validNonExistentFile("target");
        Operation operation = Operation.createMoveOperation(source, target);
        OperationExecutionFailure result = operation.test();
        if (result == null) {
            fail("The operation test returned no failure: expected was " + FailureType.SOURCE_FILE_MISSING);
        } else if (!FailureType.SOURCE_FILE_MISSING.equals(result.getType())) {
            fail("The operation test returned the wrong failure: \n" + "failure type: " + result.getType() + "\n" + "with message: " + result.getMessage() + "\n" + "expected was: " + FailureType.SOURCE_FILE_MISSING);
        }
        result = operation.execute();
        if (result == null) {
            fail("The operation execution returned no failure: expected was " + FailureType.SOURCE_FILE_MISSING);
        } else if (!FailureType.SOURCE_FILE_MISSING.equals(result.getType())) {
            fail("The operation execution returned the wrong failure: \n" + "failure type: " + result.getType() + "\n" + "with message: " + result.getMessage() + "\n" + "expected was: " + FailureType.SOURCE_FILE_MISSING);
        }
        if (!source.exists() && !target.exists()) {
        } else {
            fail("A source or target file appeared out of nothing.");
        }
    }

    public void testFailedMoveOperationSourceNotExistentExecuteOnly() {
        source = validNonExistentFile("source");
        target = validNonExistentFile("target");
        Operation operation = Operation.createMoveOperation(source, target);
        OperationExecutionFailure result = operation.execute();
        if (result == null) {
            fail("The operation execution returned no failure: expected was " + FailureType.SOURCE_FILE_MISSING);
        } else if (!FailureType.SOURCE_FILE_MISSING.equals(result.getType())) {
            fail("The operation execution returned the wrong failure: \n" + "failure type: " + result.getType() + "\n" + "with message: " + result.getMessage() + "\n" + "expected was: " + FailureType.SOURCE_FILE_MISSING);
        }
        if (!source.exists() && !target.exists()) {
        } else {
            fail("A source or target file appeared out of nothing.");
        }
    }

    public void testFailedMoveOperationSourceLocked() {
        source = validExistentFile("source");
        target = validNonExistentFile("target");
        FileLock lock = lockFile(source);
        Operation operation = Operation.createMoveOperation(source, target);
        OperationExecutionFailure result = operation.test();
        if (result == null) {
            fail("The operation test returned no failure: expected was " + FailureType.WRITE_ACCESS_DENIED);
        } else if (!FailureType.WRITE_ACCESS_DENIED.equals(result.getType())) {
            fail("The operation test returned the wrong failure: \n" + "failure type: " + result.getType() + "\n" + "with message: " + result.getMessage() + "\n" + "expected was: " + FailureType.WRITE_ACCESS_DENIED);
        }
        result = operation.execute();
        if (result == null) {
            fail("The operation execution returned no failure: expected was " + FailureType.WRITE_ACCESS_DENIED);
        } else if (!FailureType.WRITE_ACCESS_DENIED.equals(result.getType())) {
            fail("The operation execution returned the wrong failure: \n" + "failure type: " + result.getType() + "\n" + "with message: " + result.getMessage() + "\n" + "expected was: " + FailureType.WRITE_ACCESS_DENIED);
        }
        if (source.exists() && !target.exists()) {
        } else if (source.exists() && target.exists()) {
            fail("The source and target files exist - maybe the file was copied.");
        } else {
            throw new RuntimeException("Test logic error: it seems the source " + "file was deleted although the file was locked.");
        }
        try {
            lock.release();
            lock = null;
        } catch (IOException e) {
            throw new RuntimeException("Unable to release file lock.");
        }
    }

    public void testFailedMoveOperationSourceLockedExecuteOnly() {
        source = validExistentFile("source");
        target = validNonExistentFile("target");
        FileLock lock = lockFile(source);
        Operation operation = Operation.createMoveOperation(source, target);
        OperationExecutionFailure result = operation.execute();
        if (result == null) {
            fail("The operation execution returned no failure: expected was " + FailureType.WRITE_ACCESS_DENIED);
        } else if (!FailureType.WRITE_ACCESS_DENIED.equals(result.getType())) {
            fail("The operation execution returned the wrong failure: \n" + "failure type: " + result.getType() + "\n" + "with message: " + result.getMessage() + "\n" + "expected was: " + FailureType.WRITE_ACCESS_DENIED);
        }
        if (source.exists() && !target.exists()) {
        } else if (source.exists() && target.exists()) {
            fail("The source and target files exist - maybe the file was copied.");
        } else {
            throw new RuntimeException("Test logic error: it seems the source " + "file was deleted although the file was locked.");
        }
        try {
            lock.release();
            lock = null;
        } catch (IOException e) {
            throw new RuntimeException("Unable to release file lock.");
        }
    }

    public void testFailedMoveOperationTargetExistent() {
        source = validExistentFile("source");
        target = validExistentFile("target");
        Operation operation = Operation.createMoveOperation(source, target);
        OperationExecutionFailure result = operation.test();
        if (result == null) {
            fail("The operation test returned no failure: expected was " + FailureType.TARGET_FILE_EXISTS);
        } else if (!FailureType.TARGET_FILE_EXISTS.equals(result.getType())) {
            fail("The operation test returned the wrong failure: \n" + "failure type: " + result.getType() + "\n" + "with message: " + result.getMessage() + "\n" + "expected was: " + FailureType.TARGET_FILE_EXISTS);
        }
        result = operation.execute();
        if (result == null) {
            fail("The operation execution returned no failure: expected was " + FailureType.TARGET_FILE_EXISTS);
        } else if (!FailureType.TARGET_FILE_EXISTS.equals(result.getType())) {
            fail("The operation execution returned the wrong failure: \n" + "failure type: " + result.getType() + "\n" + "with message: " + result.getMessage() + "\n" + "expected was: " + FailureType.TARGET_FILE_EXISTS);
        }
        if (source.exists() && target.exists()) {
        } else {
            fail("A source or target file disappeared out of nothing.");
        }
    }

    public void testFailedMoveOperationTargetExistentExecuteOnly() {
        source = validExistentFile("source");
        target = validExistentFile("target");
        Operation operation = Operation.createMoveOperation(source, target);
        OperationExecutionFailure result = operation.execute();
        if (result == null) {
            fail("The operation execution returned no failure: expected was " + FailureType.TARGET_FILE_EXISTS);
        } else if (!FailureType.TARGET_FILE_EXISTS.equals(result.getType())) {
            fail("The operation execution returned the wrong failure: \n" + "failure type: " + result.getType() + "\n" + "with message: " + result.getMessage() + "\n" + "expected was: " + FailureType.TARGET_FILE_EXISTS);
        }
        if (source.exists() && target.exists()) {
        } else {
            fail("A source or target file disappeared out of nothing.");
        }
    }

    public void testFailedMoveOperationTargetWriteAccessDenied() {
    }

    public void testFailedMoveOperationTargetWriteAccessDeniedExecuteOnly() {
        source = validExistentFile("source");
        target = validNonExistentNonWritableFile();
        Operation operation = Operation.createMoveOperation(source, target);
        OperationExecutionFailure result = operation.execute();
        if (result == null) {
            fail("The operation execution returned no failure: expected was " + FailureType.UNKNOWN_REASON + " as the real reason cannot " + "be found out: " + FailureType.WRITE_ACCESS_DENIED);
        } else if (!FailureType.UNKNOWN_REASON.equals(result.getType())) {
            fail("The operation execution returned the wrong failure: \n" + "failure type: " + result.getType() + "\n" + "with message: " + result.getMessage() + "\n" + "expected was: " + FailureType.UNKNOWN_REASON);
        }
        if (source.exists() && !target.exists()) {
        } else if (!source.exists() && target.exists()) {
            throw new RuntimeException("Test logic error: the file seems to " + "be moved, although the destination should have been " + "non-writable.");
        } else if (source.exists() && target.exists()) {
            throw new RuntimeException("Test logic error: the file seems to " + "be copied, although the destination should have been " + "non-writable.");
        } else if (!source.exists() && !target.exists()) {
            fail("The source file has been deleted.");
        }
    }

    private File validExistentFile(String prefix) {
        File result = null;
        try {
            result = File.createTempFile(prefix, null);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create temporary file.", e);
        }
        if (!result.exists()) {
            throw new RuntimeException("Unable to create temporary file.");
        }
        return result;
    }

    private File validNonExistentFile(String prefix) {
        File result = null;
        try {
            result = File.createTempFile(prefix, null);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create temporary file.", e);
        }
        result.delete();
        if (result.exists()) {
            throw new RuntimeException("Unable to get valid non-existing " + "target for test file.");
        }
        return result;
    }

    private File validNonExistentNonWritableFile() {
        if ("Windows XP".equals(System.getProperty("os.name"))) {
            File sysdir = new File("c:/System Volume Information");
            if (!sysdir.exists()) {
                throw new RuntimeException("No directory with restrictive " + "access rights found.");
            }
            File file = null;
            for (int i = 0; i < 100; i++) {
                file = new File(sysdir, "temp_file_" + i + ".tmp");
                if (!file.exists()) {
                    break;
                }
            }
            if (file == null || file.exists()) {
                throw new RuntimeException("No non-existent file path found " + "within the directory: " + sysdir.getAbsolutePath());
            }
            try {
                file.createNewFile();
                throw new RuntimeException("Found no file with restrictive " + "access rights.");
            } catch (IOException e) {
                if ("Access is denied".equals(e.getMessage())) {
                    return file;
                }
                throw new RuntimeException("Unexpected kind of IOException. " + "Expected was 'Access is denied'", e);
            }
        }
        throw new RuntimeException("Currently this is only supported with Windows XP");
    }

    private FileLock lockFile(File file) {
        try {
            FileOutputStream stream = new FileOutputStream(file);
            return stream.getChannel().lock();
        } catch (Exception e) {
            throw new RuntimeException("Unable to obtain file lock for file '" + file.getAbsolutePath() + "'.", e);
        }
    }
}
