package org.pointrel.pointrel20110330.archives;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

public class ArchiveUsingDirectory extends ArchiveAbstract {

    public static String archiveType = "file";

    public static String pathSeparator = "/";

    public static final String PointrelLogFilePrefix = "PL_";

    public static final String PointrelLogFileSuffix = ".plog";

    public static final String PointrelLogFileDirectoryName = "logs/";

    public static final int PointrelLogFileDateSubstringLength = 10;

    public static final String ChangesDirectory = "changes";

    public static final String ChangesFilePrefix = "PC_";

    public static final String ChangesFileSuffix = ".pchanges";

    public static final String VariableCacheFileSuffix = ".pointrel-cached-variable";

    protected boolean readConfiguration = false;

    protected int charactersPerLevel = 0;

    protected int directoryNesting = 0;

    protected String basePathForNestedResources = "0X0";

    private ObjectNode configuration;

    private File cachedChangesFileLocation = null;

    protected void setNesting(int charactersPerLevel, int directoryNesting) {
        if (charactersPerLevel < 0 || charactersPerLevel > 9) throw new RuntimeException("charactersPerLevel out of range 0 to 9");
        if (directoryNesting < 0 || directoryNesting > 9) throw new RuntimeException("directoryNesting out of range 0 to 9");
        this.charactersPerLevel = charactersPerLevel;
        this.directoryNesting = directoryNesting;
        if (directoryNesting == 0) this.charactersPerLevel = 0;
        this.basePathForNestedResources = Integer.toString(this.charactersPerLevel) + "X" + Integer.toString(this.directoryNesting);
    }

    public void setArchiveAccessSpecification(ArchiveAccessSpecification archiveAccessSpecification) {
        this.archiveAccessSpecification = archiveAccessSpecification;
    }

    public boolean connect() {
        if (!readConfiguration) {
            if (!this.exists()) return false;
            ObjectMapper m = new ObjectMapper();
            try {
                this.configuration = (ObjectNode) m.readValue(new File(this.basePathForConfiguration() + "configuration.json"), JsonNode.class);
                JsonNode firstLayoutItem = this.configuration.get("resourceDirectoryLayout").get(0);
                int characters = firstLayoutItem.get("characters").getIntValue();
                int nesting = firstLayoutItem.get("nesting").getIntValue();
                this.setNesting(characters, nesting);
            } catch (JsonParseException e) {
                e.printStackTrace();
                return false;
            } catch (JsonMappingException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            this.readConfiguration = true;
            new File(this.basePathForTemporary()).mkdirs();
            new File(this.basePathForLogging()).mkdirs();
            new File(this.basePathForChanges()).mkdirs();
            new File(this.basePathForVariables()).mkdirs();
            new File(this.basePathForDeleted()).mkdirs();
            new File(this.basePathForIndexes()).mkdirs();
        }
        setConnected(true);
        return true;
    }

    @Override
    public boolean exists() {
        return (new File(this.basePathForConfiguration() + "configuration.json")).exists();
    }

    @Override
    public boolean supportsCreatingNewArchive() {
        return true;
    }

    @Override
    public boolean createArchive(String options) {
        if (this.exists()) throw new RuntimeException("Archive already exists: " + this.archiveAccessSpecification.getLocationOnServer());
        int characters = 2;
        int nesting = 2;
        if (options != null) {
            if (options.indexOf("tiny") != -1) {
                characters = 0;
                nesting = 0;
            } else if (options.indexOf("small") != -1) {
                nesting = 1;
            } else if (options.indexOf("medium") != -1) {
                nesting = 2;
            } else if (options.indexOf("large") != -1) {
                nesting = 3;
            }
            if (options.indexOf("huge") != -1) {
                nesting = 4;
            }
        }
        boolean createdOK = false;
        createdOK = new File(this.basePathForConfiguration()).mkdirs();
        String fileContents = "{\n" + "  \"version\": \"Pointrel20110330.0.1.0\",\n" + "  \"resourceDirectoryLayout\": [{\"characters\": " + characters + ", \"nesting\": " + nesting + "}],\n" + "  \"changes\": \"true\",\n" + "  \"logging\": \"true\",\n" + "  \"indexing\": \"false\"\n" + "}";
        try {
            FileUtils.writeStringToFile(new File(this.basePathForConfiguration() + "configuration.json"), fileContents, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        createdOK = createdOK && new File(this.basePathForChanges()).mkdirs();
        createdOK = createdOK && new File(this.basePathForLogging()).mkdirs();
        createdOK = createdOK && new File(this.basePathForTemporary()).mkdirs();
        createdOK = createdOK && new File(this.basePathForDeleted()).mkdirs();
        createdOK = createdOK && new File(this.basePathForIndexes()).mkdirs();
        createdOK = createdOK && new File(this.basePathForVariables()).mkdirs();
        return createdOK;
    }

    @Override
    public ChangesList basicGetChangesList(String suffix, String fromToken, String toToken, ProgressCallback progressCallback) {
        System.out.println("basicGetResourceReferenceList: start");
        System.out.println("basicGetResourceReferenceList: fromToken: " + fromToken);
        System.out.println("basicGetResourceReferenceList: toToken: " + toToken);
        File changesFile = this.currentChangesFile();
        String changesFileName = changesFile.getName();
        long currentLength = changesFile.length();
        long changesStartPosition = 0;
        long changesEndPosition = currentLength;
        if (fromToken != null) {
            String[] fromTokenElements = fromToken.split(":");
            if (fromTokenElements.length == 2 && changesFileName.equals(fromTokenElements[0])) {
                changesStartPosition = Long.valueOf(fromTokenElements[1]);
                if (changesStartPosition > currentLength) changesStartPosition = currentLength;
            }
        }
        if (toToken != null) {
            String[] toTokenElements = toToken.split(":");
            if (toTokenElements.length == 2 && changesFileName.equals(toTokenElements[0])) {
                changesEndPosition = Long.valueOf(toTokenElements[1]);
                if (changesEndPosition > currentLength) changesEndPosition = currentLength;
            }
        }
        ChangesList resultList = new ChangesList(suffix, fromToken, toToken);
        if (changesStartPosition == changesEndPosition) {
            System.out.println("Start == end");
            return resultList;
        }
        RandomAccessFile changesRandomAccessFile = null;
        try {
            changesRandomAccessFile = new RandomAccessFile(changesFile, "r");
            changesRandomAccessFile.seek(changesStartPosition);
            ObjectMapper m = new ObjectMapper();
            while (true) {
                String lineRaw = changesRandomAccessFile.readLine();
                if (lineRaw == null) break;
                if (changesRandomAccessFile.getFilePointer() > changesEndPosition) break;
                byte[] lineUTF8Bytes = lineRaw.getBytes();
                String line = new String(lineUTF8Bytes, "UTF-8");
                try {
                    ObjectNode change = (ObjectNode) m.readValue(line, JsonNode.class);
                    String resourceFileName = change.get("resource").getTextValue();
                    if ((suffix == null) || resourceFileName.endsWith(suffix)) {
                        resultList.add(resourceFileName);
                    }
                } catch (java.io.IOException e) {
                    System.out.println("Problem parsing one line from the changes file as JSON: " + line);
                    e.printStackTrace();
                }
                if (progressCallback != null) {
                    boolean cancel = progressCallback.progress((int) (100 * (changesRandomAccessFile.getFilePointer() - changesStartPosition) / (changesEndPosition - changesStartPosition)));
                    if (cancel) {
                        System.out.println("basicGetResourceReferenceList: cancelling");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Some kind of exception occurred");
            System.out.println(e);
            e.printStackTrace();
        } finally {
            if (changesRandomAccessFile != null) {
                try {
                    changesRandomAccessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("basicGetResourceReferenceList: done");
        return resultList;
    }

    public String basePathForConfiguration() {
        return this.archiveAccessSpecification.getLocationOnServer() + ArchiveUsingDirectory.pathSeparator;
    }

    public String basePathForLogging() {
        return this.archiveAccessSpecification.getLocationOnServer() + ArchiveUsingDirectory.pathSeparator + PointrelLogFileDirectoryName + ArchiveUsingDirectory.pathSeparator;
    }

    public String basePathForChanges() {
        return this.archiveAccessSpecification.getLocationOnServer() + ArchiveUsingDirectory.pathSeparator + ChangesDirectory + ArchiveUsingDirectory.pathSeparator;
    }

    public String basePathForWriting() {
        return this.archiveAccessSpecification.getLocationOnServer() + ArchiveUsingDirectory.pathSeparator + "resources" + ArchiveUsingDirectory.pathSeparator + basePathForNestedResources + ArchiveUsingDirectory.pathSeparator;
    }

    public String basePathForTemporary() {
        return this.archiveAccessSpecification.getLocationOnServer() + ArchiveUsingDirectory.pathSeparator + "resources" + ArchiveUsingDirectory.pathSeparator + "temporary" + ArchiveUsingDirectory.pathSeparator;
    }

    public String basePathForDeleted() {
        return this.archiveAccessSpecification.getLocationOnServer() + ArchiveUsingDirectory.pathSeparator + "resources" + ArchiveUsingDirectory.pathSeparator + "deleted" + ArchiveUsingDirectory.pathSeparator;
    }

    public String basePathForIndexes() {
        return this.archiveAccessSpecification.getLocationOnServer() + ArchiveUsingDirectory.pathSeparator + "indexes" + ArchiveUsingDirectory.pathSeparator;
    }

    public String basePathForVariables() {
        return this.archiveAccessSpecification.getLocationOnServer() + ArchiveUsingDirectory.pathSeparator + "variables" + ArchiveUsingDirectory.pathSeparator;
    }

    public File fileForResourceReference(String resourceReference) {
        if (directoryNesting == 0) return new File(this.basePathForWriting() + resourceReference);
        String nestedDirectories = "";
        int start = ResourceFileSupport.ResourceFilePrefix.length();
        for (int nesting = 0; nesting < this.directoryNesting; nesting++) {
            nestedDirectories += resourceReference.substring(start, start + this.charactersPerLevel) + pathSeparator;
            start += this.charactersPerLevel;
        }
        return new File(this.basePathForWriting() + nestedDirectories + resourceReference);
    }

    public boolean basicRetrieveResourceToStream(String resourceReference, OutputStream outputStream) {
        File file = this.fileForResourceReference(resourceReference);
        try {
            FileInputStream inputStream = new FileInputStream(file);
            ResourceFileSupport.copyInputStreamToOutputStream(inputStream, outputStream);
            inputStream.close();
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    String basicAddResourceFromStream(InputStream inputStream, String extension, String prefix, String requestedResourceReference) {
        if (prefix != null) {
            if (!this.isSupportedPrefix(prefix)) {
                System.out.println("Unsupported resource prefix: " + prefix);
                return null;
            }
        }
        String tempFileName = "TEMP_" + Standards.newUUID() + extension;
        File resourceFile = new File(this.basePathForTemporary(), tempFileName);
        try {
            FileOutputStream resourceFileOutputStream = new FileOutputStream(resourceFile);
            try {
                ResourceFileSupport.copyInputStreamToOutputStream(inputStream, resourceFileOutputStream);
            } finally {
                resourceFileOutputStream.close();
            }
            String resourceReference = Standards.getResourceReferenceWithSHA256HashAsHexEncodedString(resourceFile);
            if ((requestedResourceReference != null) && (!resourceReference.equals(requestedResourceReference))) {
                System.out.println("Requested resource reference does not match generated one: " + requestedResourceReference + " != " + resourceReference);
                if (!resourceFile.delete()) {
                    System.out.println("Could not delete temp file: " + resourceFile.getAbsolutePath());
                }
                return null;
            }
            File destinationFile = this.fileForResourceReference(resourceReference);
            if (destinationFile.exists()) {
                System.out.println("Resource file already exists");
                if (!resourceFile.delete()) {
                    System.out.println("Could not delete temp file: " + resourceFile.getAbsolutePath());
                }
                return resourceReference;
            }
            destinationFile.getParentFile().mkdirs();
            if (!resourceFile.renameTo(destinationFile)) {
                System.out.println("Target does not exist, but could not rename file to: " + destinationFile.getName());
            } else {
            }
            RandomAccessFile changesRandomAccessFile = null;
            String changeLogStateToken = null;
            try {
                String information = "{\"action\":\"PUT\", \"resource\":\"" + resourceReference + "\"}\n";
                byte[] utf8 = information.getBytes("UTF-8");
                File changesFile = this.currentChangesFile();
                changesRandomAccessFile = new RandomAccessFile(changesFile, "rws");
                FileLock lock = changesRandomAccessFile.getChannel().lock(Long.MAX_VALUE - 1, 1, true);
                try {
                    changesRandomAccessFile.seek(changesRandomAccessFile.length());
                    changesRandomAccessFile.write(utf8);
                } finally {
                    lock.release();
                }
                changeLogStateToken = changesFile.getName() + ":" + changesRandomAccessFile.getFilePointer();
            } catch (Exception error) {
                System.out.println("Some kind of exception occurred");
                System.out.println(error);
            } finally {
                if (changesRandomAccessFile != null) {
                    changesRandomAccessFile.close();
                }
            }
            RandomAccessFile logRandomAccessFile = null;
            try {
                String currentTimestampString = Standards.getCurrentTimestamp();
                String currentDateString = currentTimestampString.substring(0, PointrelLogFileDateSubstringLength);
                File logFile = new File(this.basePathForLogging() + PointrelLogFilePrefix + currentDateString + PointrelLogFileSuffix);
                String logLine = currentTimestampString + " PUT " + resourceReference + "\n";
                byte[] utf8 = logLine.getBytes("UTF-8");
                logRandomAccessFile = new RandomAccessFile(logFile, "rws");
                FileLock lock = logRandomAccessFile.getChannel().lock(Long.MAX_VALUE - 1, 1, true);
                try {
                    logRandomAccessFile.seek(logRandomAccessFile.length());
                    logRandomAccessFile.write(utf8);
                } finally {
                    lock.release();
                }
            } catch (Exception error) {
                System.out.println("Some kind of exception occurred");
                System.out.println(error);
            } finally {
                if (logRandomAccessFile != null) {
                    logRandomAccessFile.close();
                }
            }
            if (ResourceFileSupport.VariableResourceFileSuffix.equals(extension)) {
                if (this.cachedVariables == null) this.updateCachedVariablesIfNeeded();
                this.updateVariableValueIfNeeded(resourceReference, "ADD", true);
                if (changeLogStateToken != null) this.stateTokenForVariableCache = changeLogStateToken;
            }
            return resourceReference;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private File currentChangesFile() {
        if (this.cachedChangesFileLocation != null) return this.cachedChangesFileLocation;
        File changesDirectory = new File(this.basePathForChanges());
        File[] files = changesDirectory.listFiles();
        for (File file : files) {
            String name = file.getName();
            if (name.startsWith(ChangesFilePrefix) && name.endsWith(ChangesFileSuffix)) return file;
        }
        String newChangesFileName = ChangesFilePrefix + UUID.randomUUID() + ChangesFileSuffix;
        this.cachedChangesFileLocation = new File(this.basePathForChanges() + pathSeparator + newChangesFileName);
        return this.cachedChangesFileLocation;
    }

    public String getCurrentStateToken() {
        File changesFile = this.currentChangesFile();
        return changesFile.getName() + ":" + changesFile.length();
    }

    public boolean hasResource(String resourceReference) {
        return this.fileForResourceReference(resourceReference).exists();
    }

    protected void clearPersistentCacheOfCollaborativeVariables() {
        File variableDirectory = new File(this.basePathForVariables());
        String[] files = variableDirectory.list();
        for (String fileName : files) {
            if (fileName.endsWith(VariableCacheFileSuffix)) {
                new File(fileName).delete();
            }
        }
    }

    public boolean loadPersistentCacheOfCollaborativeVariables() {
        File variableDirectory = new File(this.basePathForVariables());
        String[] files = variableDirectory.list();
        for (String fileName : files) {
            if (fileName.endsWith(VariableCacheFileSuffix)) {
                String variableName = fileName.substring(0, fileName.lastIndexOf("."));
                String[] details = this.getDetailsForCollaborativeVariableFromPersistentCache(variableName);
                String resourceReference = details[1];
                this.updateVariableValueIfNeeded(resourceReference, "ADD", false);
            }
        }
        return true;
    }

    protected File fileForVariable(String variableName) {
        if (!Standards.isVariableNameOK(variableName)) throw new RuntimeException("Problem with variable name: " + variableName);
        return new File(this.basePathForVariables() + variableName + VariableCacheFileSuffix);
    }

    protected String[] getDetailsForCollaborativeVariableFromPersistentCache(String variableName) {
        File variableFile = fileForVariable(variableName);
        if (!variableFile.exists()) return null;
        try {
            String contents = FileUtils.readFileToString(variableFile, "UTF-8");
            String[] items = contents.split(" ", 3);
            if (items.length != 3) throw new RuntimeException("Problem with reading variable segments: " + variableName + " : " + contents);
            return items;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Problem with reading variable: " + variableName);
        }
    }

    protected void updatePersistentCacheOfCollaborativeVariables(VariableInArchive variableInArchive) {
        String variableName = variableInArchive.getVariableName();
        String newValue = variableInArchive.getVariableValue();
        File variableFile = fileForVariable(variableName);
        if (newValue == null) {
            if (!variableFile.delete()) {
                throw new RuntimeException("Problem deleting variable: " + variableName);
            }
        } else {
            try {
                FileUtils.writeStringToFile(variableFile, variableInArchive.getTimestamp() + " " + variableInArchive.getResourceReference() + " " + newValue, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Problem writing new value of variable: " + variableName);
            }
        }
    }
}
