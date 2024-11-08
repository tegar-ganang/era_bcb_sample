package org.caleigo.core.service;

import java.io.*;
import org.caleigo.core.*;
import org.caleigo.toolkit.log.*;

public class TextFileDataService extends MemoryDataService {

    private static final String NULL = "<NULL>";

    private File mDirectoryFile;

    private int mFieldSeparator = ';';

    private int mRecordSeparator = '\n';

    private int mStringDelimiter = '"';

    public TextFileDataService(IDataSourceDescriptor dataSourceDescriptor, String dbDirectoryPath) {
        this(dataSourceDescriptor, dataSourceDescriptor.getSourceName(), dbDirectoryPath);
    }

    public TextFileDataService(IDataSourceDescriptor dataSourceDescriptor, Object serviceIdentity, String dbDirectoryPath) {
        super(dataSourceDescriptor, serviceIdentity);
        try {
            mDirectoryFile = new File(dbDirectoryPath);
            if (!mDirectoryFile.exists()) mDirectoryFile.mkdir();
            if (mDirectoryFile == null) Log.printError(this, "Failed to initialize FileDataService, path not found \"" + dbDirectoryPath + "\"!"); else if (!mDirectoryFile.canRead() || !mDirectoryFile.canWrite()) {
                Log.printError(this, "Failed to initialize FileDataService, no read/write access on \"" + dbDirectoryPath + "\"!");
                mDirectoryFile = null;
            } else Log.print(this, "FileDataService initialized on path \"" + dbDirectoryPath + "\".");
        } catch (Exception e) {
            Log.printError(this, "Failed to initialize FileDataService for path \"" + dbDirectoryPath + "\"!", e);
            mDirectoryFile = null;
        }
    }

    public boolean ping() {
        return mDirectoryFile != null;
    }

    protected ISelection loadTableSelection(IEntityDescriptor entityDescriptor) {
        ISelection dbSelection = null;
        try {
            File entityFile = new File(mDirectoryFile, entityDescriptor.getCodeName() + ".tdb");
            Log.print(this, "Loading table selection from: \"" + entityFile.getAbsolutePath() + "\"");
            if (entityFile.exists()) {
                Reader fileReader = new FileReader(entityFile);
                fileReader = new BufferedReader(fileReader, 10000);
                EntityReader entityReader = new EntityReader(fileReader, mFieldSeparator, mRecordSeparator);
                entityReader.setStringDelimiter(mStringDelimiter);
                dbSelection = entityReader.readMappedSelection(entityDescriptor);
                fileReader.close();
            } else dbSelection = new Selection(entityDescriptor);
            Log.print(this, "Loading completed. " + (dbSelection != null ? dbSelection.size() : 0) + " entities loaded.");
        } catch (Exception e) {
            Log.printError(this, "Failed to load complete table selection: " + entityDescriptor, e);
        }
        return dbSelection;
    }

    protected void storeTableSelection(ISelection dbSelection) {
        try {
            File entityFile = new File(mDirectoryFile, dbSelection.getEntityDescriptor().getCodeName() + ".tdb");
            Log.print(this, "Storing table selection to: \"" + entityFile.getAbsolutePath() + "\"");
            Writer fileWriter = new FileWriter(entityFile);
            fileWriter = new BufferedWriter(fileWriter, 10000);
            EntityWriter entityWriter = new EntityWriter(fileWriter, mFieldSeparator, mRecordSeparator);
            entityWriter.setStringDelimiter(mStringDelimiter);
            entityWriter.writeMappedSelection(dbSelection);
            entityWriter.flush();
            entityWriter.close();
            Log.print(this, "Store completed. " + (dbSelection != null ? dbSelection.size() : 0) + " entities stored.");
        } catch (Exception e) {
            Log.printError(this, "Failed to store table selection: " + dbSelection.getEntityDescriptor(), e);
        }
    }

    public int getFieldSeparator() {
        return mFieldSeparator;
    }

    public void setFieldSeparator(int character) {
        mFieldSeparator = character;
    }

    public int getRecordSeparator() {
        return mRecordSeparator;
    }

    public void setRecordSeparator(int character) {
        mRecordSeparator = character;
    }

    public int getStringDelimiter() {
        return mStringDelimiter;
    }

    public void setStringDelimiter(int character) {
        mStringDelimiter = character;
    }

    public File getDirectoryFile() {
        return mDirectoryFile;
    }
}
