package de.huxhorn.sulky.blobs.impl;

import de.huxhorn.sulky.blobs.AmbiguousIdException;
import de.huxhorn.sulky.blobs.BlobRepository;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayInputStream;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Set;

/**
 * This implementation of the BlobRepository interface is similar the internal structure used by a git repository.
 *
 * The ids generated are the SHA1 of the data. All methods accepting an id as argument will also work with a partial
 * id, i.e. only the start of the full id, as long as it is long enough to result in a unique result.
 *
 * If such a uniqueness is not given, an AmbiguousIdException is thrown containing the list of possible matches.
 *
 * Such an exception will never be thrown if the full id is used for reference.
 *
 * This implementation is NOT thread-safe.
 *
 * If validation is enabled and tampered data is detected during a get operation then null is returned.
 * The invalid data file is automatically deleted in that case.
 *
 * @see AmbiguousIdException the exception thrown if more than one blob would match a given partial id.
 */
public class BlobRepositoryImpl implements BlobRepository {

    private final Logger logger = LoggerFactory.getLogger(BlobRepositoryImpl.class);

    private File baseDirectory;

    private boolean validating = false;

    private boolean caseSensitive = true;

    private static final String ALGORITHM = "SHA1";

    private static final int HASH_DIRECTORY_NAME_LENGTH = 2;

    private static final int HASH_REMAINDER_NAME_LENGTH = 38;

    /**
	 *
	 * @return whether IDs are handled case-sensitive. Default is true.
	 */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
	 * Enables or disables case-sensitive handling of IDs.
	 *
	 * @param caseSensitive enables or disables case-sensitive handling of IDs.
	 */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    /**
	 *
	 * @return whether validation on get is active or not. Default is false.
	 */
    public boolean isValidating() {
        return validating;
    }

    /**
	 * Enables or disables validation on get.
	 *
	 * @param validating enables or disables validation
	 */
    public void setValidating(boolean validating) {
        this.validating = validating;
    }

    public File getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(File baseDirectory) {
        this.baseDirectory = baseDirectory;
        prepare();
    }

    /**
	 * {@inheritDoc}
	 */
    public String put(InputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null!");
        }
        prepare();
        File tempFile = File.createTempFile("Blob", ".tmp", baseDirectory);
        if (logger.isDebugEnabled()) logger.debug("Created temporary file '{}'.", tempFile);
        String hashString = copyAndHash(input, tempFile);
        long tempLength = tempFile.length();
        if (tempLength == 0) {
            if (tempFile.delete()) {
                if (logger.isDebugEnabled()) logger.debug("Deleted empty file '{}'.");
            } else {
                if (logger.isErrorEnabled()) logger.error("Failed to delete temporary file '{}'!", tempFile.getAbsolutePath());
            }
            throw new IllegalArgumentException("input must not be empty!");
        }
        File destinationFile = prepareFile(hashString);
        if (destinationFile.isFile()) {
            long destinationLength = destinationFile.length();
            if (destinationLength == tempLength) {
                if (logger.isInfoEnabled()) logger.info("Blob {} did already exist.", hashString);
                deleteTempFile(tempFile);
                return hashString;
            } else {
                if (logger.isWarnEnabled()) logger.warn("A different blob with the hash {} does already exist!", hashString);
                deleteTempFile(tempFile);
                return null;
            }
        }
        if (tempFile.renameTo(destinationFile)) {
            if (logger.isDebugEnabled()) logger.debug("Created blob file '{}'", destinationFile.getAbsolutePath());
            if (logger.isInfoEnabled()) logger.info("Created blob {} containing {} bytes.", hashString, tempLength);
            return hashString;
        }
        if (logger.isWarnEnabled()) logger.warn("Couldn't rename temp file '{}' to destination file '{}'!", tempFile.getAbsolutePath(), destinationFile.getAbsolutePath());
        deleteTempFile(tempFile);
        return null;
    }

    /**
	 * {@inheritDoc}
	 */
    public String put(byte[] bytes) throws IOException {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes must not be null!");
        }
        if (bytes.length == 0) {
            throw new IllegalArgumentException("bytes must not be empty!");
        }
        return put(new ByteArrayInputStream(bytes));
    }

    /**
	 * {@inheritDoc}
	 */
    public InputStream get(String id) throws AmbiguousIdException, IOException {
        prepare();
        id = prepareId(id);
        File file = getFileFor(id);
        if (file == null) {
            return null;
        }
        if (valid(id, file)) {
            return new FileInputStream(file);
        }
        if (file.delete()) {
            if (logger.isInfoEnabled()) logger.info("Deleted invalid entry for id {}.", id);
        } else {
            if (logger.isErrorEnabled()) logger.error("Failed to delete invalid entry for id {}! ({})", id, file.getAbsolutePath());
        }
        return null;
    }

    /**
	 * {@inheritDoc}
	 */
    public boolean delete(String id) throws AmbiguousIdException {
        prepare();
        id = prepareId(id);
        File file = getFileFor(id);
        if (file == null) {
            return false;
        }
        File parent = file.getParentFile();
        if (file.delete()) {
            if (logger.isInfoEnabled()) logger.info("Deleted blob {}{}.", parent.getName(), file.getName());
            deleteIfEmpty(parent);
            return true;
        }
        if (logger.isWarnEnabled()) logger.warn("Couldn't delete blob {}{}!", parent.getName(), file.getName());
        return false;
    }

    /**
	 * {@inheritDoc}
	 */
    public boolean contains(String id) throws AmbiguousIdException {
        prepare();
        id = prepareId(id);
        return getFileFor(id) != null;
    }

    /**
	 * {@inheritDoc}
	 */
    public long sizeOf(String id) throws AmbiguousIdException {
        prepare();
        id = prepareId(id);
        File file = getFileFor(id);
        if (file == null) {
            return -1;
        }
        return file.length();
    }

    /**
	 * {@inheritDoc}
	 */
    public Set<String> idSet() {
        prepare();
        Set<String> result = new HashSet<String>();
        File[] subDirs = baseDirectory.listFiles(new MatchingDirectoriesFileFilter());
        for (File current : subDirs) {
            File[] contained = current.listFiles(new MatchingFilesFileFilter());
            for (File curBlob : contained) {
                result.add(current.getName() + curBlob.getName());
            }
        }
        if (logger.isDebugEnabled()) logger.debug("Returning idSet {}.", result);
        return result;
    }

    private void prepare() {
        if (baseDirectory == null) {
            String message = "baseDirectory must not be null!";
            if (logger.isErrorEnabled()) logger.error(message);
            throw new IllegalStateException(message);
        }
        if (!baseDirectory.exists()) {
            if (!baseDirectory.mkdirs()) {
                String message = "Couldn't create directory '" + baseDirectory.getAbsolutePath() + "'!";
                if (logger.isWarnEnabled()) logger.warn(message);
            } else {
                if (logger.isDebugEnabled()) logger.debug("Created directory '{}'.", baseDirectory.getAbsolutePath());
            }
        }
        if (!baseDirectory.isDirectory()) {
            String message = "baseDirectory '" + baseDirectory.getAbsolutePath() + " is not a directory!";
            if (logger.isErrorEnabled()) logger.error(message);
            throw new IllegalStateException(message);
        }
    }

    private File getFileFor(String id) throws AmbiguousIdException {
        if (logger.isDebugEnabled()) logger.debug("Hash: {}", id);
        if (id == null) {
            throw new IllegalArgumentException("id must not be null!");
        }
        if (id.length() < HASH_DIRECTORY_NAME_LENGTH) {
            throw new IllegalArgumentException("id must have at least " + HASH_DIRECTORY_NAME_LENGTH + " characters!");
        }
        String hashStart = id.substring(0, HASH_DIRECTORY_NAME_LENGTH);
        String hashRest = id.substring(HASH_DIRECTORY_NAME_LENGTH);
        if (logger.isDebugEnabled()) logger.debug("HashStart='{}', hashRest='{}'", hashStart, hashRest);
        File parent = new File(baseDirectory, hashStart);
        if (!parent.isDirectory()) {
            return null;
        }
        File[] files = parent.listFiles(new StartsWithFileFilter(hashRest));
        int count = files.length;
        if (count == 0) {
            return null;
        }
        if (count == 1) {
            return files[0];
        }
        String[] candidates = new String[count];
        for (int i = 0; i < count; i++) {
            File current = files[i];
            candidates[i] = current.getParentFile().getName() + current.getName();
        }
        Arrays.sort(candidates);
        throw new AmbiguousIdException(id, candidates);
    }

    private void deleteTempFile(File tempFile) {
        if (tempFile.delete()) {
            if (logger.isDebugEnabled()) logger.debug("Deleted temporary file '{}'.", tempFile.getAbsolutePath());
        } else {
            if (logger.isWarnEnabled()) logger.warn("Couldn't delete temporary file '{}'!", tempFile.getAbsolutePath());
        }
    }

    private String prepareId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null!");
        }
        if (caseSensitive) {
            return id;
        }
        return id.toLowerCase();
    }

    private File prepareFile(String id) {
        if (logger.isDebugEnabled()) logger.debug("Hash: {}", id);
        String hashStart = id.substring(0, HASH_DIRECTORY_NAME_LENGTH);
        String hashRest = id.substring(HASH_DIRECTORY_NAME_LENGTH);
        if (logger.isDebugEnabled()) logger.debug("HashStart='{}', hashRest='{}'", hashStart, hashRest);
        File parentFile = new File(baseDirectory, hashStart);
        if (parentFile.mkdirs()) {
            if (logger.isDebugEnabled()) logger.debug("Created directory {}.", parentFile.getAbsolutePath());
        }
        return new File(parentFile, hashRest);
    }

    private MessageDigest createMessageDigest() {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException ex) {
            String message = "Can't generate hash! Algorithm " + ALGORITHM + " does not exist!";
            if (logger.isErrorEnabled()) logger.error(message, ex);
            throw new IllegalStateException(message, ex);
        }
        return digest;
    }

    private boolean valid(String id, File file) {
        if (!validating) {
            return true;
        }
        MessageDigest digest = createMessageDigest();
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            DigestInputStream dis = new DigestInputStream(input, digest);
            for (; ; ) {
                if (dis.read() < 0) {
                    break;
                }
            }
            byte[] hash = digest.digest();
            Formatter formatter = new Formatter();
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            return formatter.toString().equals(id);
        } catch (IOException e) {
        } finally {
            IOUtils.closeQuietly(input);
        }
        return false;
    }

    private String copyAndHash(InputStream input, File into) throws IOException {
        MessageDigest digest = createMessageDigest();
        DigestInputStream dis = new DigestInputStream(input, digest);
        IOException ex;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(into);
            IOUtils.copyLarge(dis, fos);
            byte[] hash = digest.digest();
            Formatter formatter = new Formatter();
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        } catch (IOException e) {
            ex = e;
        } finally {
            IOUtils.closeQuietly(dis);
            IOUtils.closeQuietly(fos);
        }
        if (logger.isWarnEnabled()) logger.warn("Couldn't retrieve data from input!", ex);
        deleteTempFile(into);
        throw ex;
    }

    private void deleteIfEmpty(File parent) {
        File[] files = parent.listFiles();
        if (files == null) {
            if (logger.isWarnEnabled()) logger.warn("File {} isn't a directory!", parent.getAbsolutePath());
            return;
        }
        if (files.length == 0) {
            if (parent.delete()) {
                if (logger.isDebugEnabled()) logger.debug("Deleted directory {}.", parent.getAbsolutePath());
            } else {
                if (logger.isWarnEnabled()) logger.warn("Couldn't delete directory {}!", parent.getAbsolutePath());
            }
        } else {
            if (logger.isDebugEnabled()) logger.debug("Directory {} isn't empty.", parent.getAbsolutePath());
        }
    }

    private static class StartsWithFileFilter implements FileFilter {

        private String filenamePart;

        public StartsWithFileFilter(String filenamePart) {
            this.filenamePart = filenamePart;
        }

        public boolean accept(File file) {
            return file.isFile() && file.getName().startsWith(filenamePart);
        }
    }

    private static class MatchingDirectoriesFileFilter implements FileFilter {

        public boolean accept(File file) {
            return file.isDirectory() && file.getName().length() == HASH_DIRECTORY_NAME_LENGTH;
        }
    }

    private static class MatchingFilesFileFilter implements FileFilter {

        public boolean accept(File file) {
            return file.isFile() && file.getName().length() == HASH_REMAINDER_NAME_LENGTH;
        }
    }
}
