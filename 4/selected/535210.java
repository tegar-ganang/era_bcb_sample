package org.pointrel.pointrel20090201;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public abstract class RepositoryAbstract implements Repository {

    public static String repositoryType = "SubclassesMustDefineThis";

    public RepositoryAccessSpecification repositoryAccessSpecification;

    protected OutputStream logStream;

    protected boolean connectionRequired;

    protected boolean connected;

    protected boolean readOnly;

    protected SearchableTransactionCache transactionCache;

    RepositoryAbstract() {
        repositoryAccessSpecification = null;
        connectionRequired = false;
        connected = false;
        readOnly = false;
        transactionCache = new SearchableTransactionCacheWithMemoryIndex(this);
    }

    public String getRepositoryType() {
        String repositoryTypeValueFromReflection = null;
        try {
            repositoryTypeValueFromReflection = (String) this.getClass().getDeclaredField("repositoryType").get(null);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return repositoryTypeValueFromReflection;
    }

    public RepositoryAccessSpecification getRepositoryAccessSpecification() {
        return repositoryAccessSpecification;
    }

    public void setRepositoryAccessSpecification(RepositoryAccessSpecification repositoryAccessSpecification) {
        this.repositoryAccessSpecification = repositoryAccessSpecification;
    }

    public boolean supportsLogStream() {
        return true;
    }

    public boolean setLogStream(OutputStream logStream) {
        this.logStream = logStream;
        return true;
    }

    public OutputStream getLogStream() {
        return this.logStream;
    }

    public boolean log(String string) {
        if (!this.supportsLogStream()) return false;
        if (string == null) string = "null";
        if (this.logStream == null) {
            System.out.println("Logging: " + string);
            return false;
        }
        try {
            this.logStream.write(string.getBytes());
            this.logStream.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean connect() {
        setConnected(true);
        return true;
    }

    public boolean disconnect() {
        setConnected(false);
        return true;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getRepositoryUUID() {
        return null;
    }

    public abstract ArrayList<String> basicGetResourceFileReferenceList(String suffix);

    public ArrayList<String> getResourceFileReferenceList(String suffix) {
        boolean wasDisconnected = !this.isConnected();
        if (wasDisconnected && !this.connect()) {
            return null;
        }
        try {
            return this.basicGetResourceFileReferenceList(suffix);
        } finally {
            if (wasDisconnected && !this.disconnect()) {
                System.out.println("Disconnection failed");
            }
        }
    }

    public String basicGetLatestResourceFileReferenceWithSuffixUsingTimestampInReference(String suffix, boolean discardItemsWithFutureDates) {
        String latestTimestamp = null;
        String latestResourceFileReference = null;
        String currentTimestamp = Standards.makeTimestampSuitableForFileName(Standards.getCurrentTimestamp());
        ArrayList<String> resourceFileReferenceList = this.basicGetResourceFileReferenceList(suffix);
        for (String resourceFileReference : resourceFileReferenceList) {
            String timestamp = ResourceFileSupport.extractTimestampFromResourceFileReferenceIfPossible(resourceFileReference, suffix);
            if (timestamp == null) continue;
            if (discardItemsWithFutureDates && timestamp.compareTo(currentTimestamp) > 0) continue;
            if (latestTimestamp == null || timestamp.compareTo(latestTimestamp) > 0) {
                latestTimestamp = timestamp;
                latestResourceFileReference = resourceFileReference;
            }
        }
        return latestResourceFileReference;
    }

    public String getLatestResourceFileReferenceWithSuffixUsingTimestampInReference(String suffix, boolean discardItemsWithFutureDates) {
        boolean wasDisconnected = !this.isConnected();
        if (wasDisconnected && !this.connect()) {
            return null;
        }
        try {
            return this.basicGetLatestResourceFileReferenceWithSuffixUsingTimestampInReference(suffix, discardItemsWithFutureDates);
        } finally {
            if (wasDisconnected && !this.disconnect()) {
                System.out.println("Disconnection failed");
            }
        }
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public Transaction retrieveTransactionByResourceFileReference(String resourceFileReference) {
        InputStream inputStream = this.getInputStreamForResourceFile(resourceFileReference);
        if (inputStream == null) return null;
        try {
            Transaction transaction = new Transaction();
            transaction.transactionResourceFileReference = resourceFileReference;
            try {
                if (!transaction.readTransactionMetadataAndTriplesFromUTF8Stream(inputStream)) {
                    System.out.println("Something went wrong reading transaction metadata and triples: " + resourceFileReference);
                    return null;
                }
            } finally {
                inputStream.close();
            }
            return transaction;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String basicStoreTransaction(Transaction transaction) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (!transaction.writeTransactionMetadataAndTriplesToUTF8Stream(outputStream)) {
            System.out.println("Something went wrong writing transaction metadata and triples: " + transaction.uuid);
            return null;
        }
        byte[] bytes = outputStream.toByteArray();
        String resourceFileReference = this.basicAddResourceFile(new ByteArrayInputStream(bytes), transaction.getExtension());
        if (resourceFileReference != null) transaction.transactionResourceFileReference = resourceFileReference;
        return resourceFileReference;
    }

    public String storeTransaction(Transaction transaction) {
        if (this.isReadOnly()) {
            System.out.println("Attempting to write new transaction to read only repository");
            return null;
        }
        boolean wasDisconnected = !this.isConnected();
        if (wasDisconnected && !this.connect()) {
            return null;
        }
        try {
            String transactionUUID = transaction.uuid;
            String resourceFileReference = null;
            System.out.println("Writing transaction: " + transactionUUID);
            resourceFileReference = this.basicStoreTransaction(transaction);
            if (resourceFileReference == null) {
                System.out.println("Could not add transaction.");
            }
            if (transactionCache != null) this.transactionCache.addTransaction(transaction);
            return resourceFileReference;
        } finally {
            if (wasDisconnected && !this.disconnect()) {
                System.out.println("Disconnection failed");
            }
        }
    }

    abstract String basicAddResourceFile(InputStream inputStream, String extension);

    abstract boolean basicRetrieveResourceFile(String resourceFileReference, OutputStream outputStream);

    String basicAddResourceFile(byte[] bytes, String extension) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        return this.basicAddResourceFile(inputStream, extension);
    }

    String basicAddResourceFile(File file, String extension) {
        try {
            FileInputStream inputStream = new FileInputStream(file);
            if (extension == null) {
                extension = ResourceFileSupport.getExtensionWithDotOrEmptyString(file.getName(), false);
            }
            return this.basicAddResourceFile(inputStream, extension);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    InputStream basicGetInputStreamForResourceFile(String resourceFileReference) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (!this.basicRetrieveResourceFile(resourceFileReference, outputStream)) return null;
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    byte[] basicRetrieveResourceFileBytes(String resourceFileReference) {
        System.out.println("Trying to retrieve: " + resourceFileReference);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (!this.basicRetrieveResourceFile(resourceFileReference, outputStream)) return null;
        return outputStream.toByteArray();
    }

    public byte[] retrieveResourceFileBytes(String resourceFileReference) {
        if (!ResourceFileSupport.isValidResourceFileName(resourceFileReference)) {
            System.out.println("ResourceFileReference not in acceptable form: " + resourceFileReference);
            return null;
        }
        boolean wasDisconnected = !this.isConnected();
        if (wasDisconnected && !this.connect()) {
            return null;
        }
        try {
            return this.basicRetrieveResourceFileBytes(resourceFileReference);
        } finally {
            if (wasDisconnected && !this.disconnect()) {
                System.out.println("Disconnection failed");
            }
        }
    }

    public boolean retrieveResourceFile(String resourceFileReference, OutputStream outputStream) {
        if (!ResourceFileSupport.isValidResourceFileName(resourceFileReference)) {
            System.out.println("ResourceFileReference not in acceptable form: " + resourceFileReference);
            return false;
        }
        boolean wasDisconnected = !this.isConnected();
        if (wasDisconnected && !this.connect()) {
            return false;
        }
        try {
            return this.basicRetrieveResourceFile(resourceFileReference, outputStream);
        } finally {
            if (wasDisconnected && !this.disconnect()) {
                System.out.println("Disconnection failed");
            }
        }
    }

    public InputStream getInputStreamForResourceFile(String resourceFileReference) {
        if (!ResourceFileSupport.isValidResourceFileName(resourceFileReference)) {
            System.out.println("ResourceFileReference not in acceptable form: " + resourceFileReference);
            return null;
        }
        boolean wasDisconnected = !this.isConnected();
        if (wasDisconnected && !this.connect()) {
            return null;
        }
        try {
            return this.basicGetInputStreamForResourceFile(resourceFileReference);
        } finally {
            if (wasDisconnected && !this.disconnect()) {
                System.out.println("Disconnection failed");
            }
        }
    }

    public String addResourceFile(byte[] bytes, String extension) {
        if (!ResourceFileSupport.isValidResourceFileExtension(extension)) {
            System.out.println("ResourceFileReference extension not in acceptable form: " + extension);
            return null;
        }
        boolean wasDisconnected = !this.isConnected();
        if (wasDisconnected && !this.connect()) {
            return null;
        }
        try {
            return this.basicAddResourceFile(bytes, extension);
        } finally {
            if (wasDisconnected && !this.disconnect()) {
                System.out.println("Disconnection failed");
            }
        }
    }

    public String addResourceFile(File file, String extension) {
        if (!ResourceFileSupport.isValidResourceFileExtension(extension)) {
            System.out.println("ResourceFileReference extension not in acceptable form: " + extension);
            return null;
        }
        boolean wasDisconnected = !this.isConnected();
        if (wasDisconnected && !this.connect()) {
            return null;
        }
        try {
            return this.basicAddResourceFile(file, extension);
        } finally {
            if (wasDisconnected && !this.disconnect()) {
                System.out.println("Disconnection failed");
            }
        }
    }

    public String addResourceFile(InputStream inputStream, String extension) {
        if (!ResourceFileSupport.isValidResourceFileExtension(extension)) {
            System.out.println("ResourceFileReference extension not in acceptable form: " + extension);
            return null;
        }
        boolean wasDisconnected = !this.isConnected();
        if (wasDisconnected && !this.connect()) {
            return null;
        }
        try {
            return this.basicAddResourceFile(inputStream, extension);
        } finally {
            if (wasDisconnected && !this.disconnect()) {
                System.out.println("Disconnection failed");
            }
        }
    }

    public boolean supportsSearch() {
        return transactionCache != null;
    }

    public boolean search(Query query) {
        if (transactionCache == null) {
            System.out.println("No transaction cache: query not supported");
            return false;
        }
        return transactionCache.search(query);
    }

    public void reloadTransactionCache() {
        System.out.println("reloadTransactionCache");
        transactionCache = new SearchableTransactionCacheWithMemoryIndex(this);
        if (transactionCache != null) transactionCache.reload(this);
    }

    public void loadAnyNewTransactionsIntoTransactionCache() {
        System.out.println("loadAnyNewTransactionsIntoTransactionCache");
        if (transactionCache != null) {
            System.out.println("valid cache");
            transactionCache.loadAnyNewTransactions(this);
        } else {
            System.out.println("No cache");
        }
    }
}
