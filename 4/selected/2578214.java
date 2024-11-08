package org.caleigo.core.service;

import java.io.*;
import org.caleigo.core.*;
import org.caleigo.toolkit.log.*;

public class BinaryFileDataService extends MemoryDataService {

    private File mDirectoryFile;

    public BinaryFileDataService(IDataSourceDescriptor dataSourceDescriptor, String dbDirectoryPath) {
        this(dataSourceDescriptor, dataSourceDescriptor.getSourceName(), dbDirectoryPath);
    }

    public BinaryFileDataService(IDataSourceDescriptor dataSourceDescriptor, Object serviceIdentity, String dbDirectoryPath) {
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
        ISelection dbSelection = new Selection(entityDescriptor);
        if (mDirectoryFile != null) {
            try {
                File entityFile = new File(mDirectoryFile, entityDescriptor.getCodeName() + ".db");
                if (entityFile.exists()) {
                    ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(new FileInputStream(entityFile), 10000));
                    int entityCount = input.readInt();
                    for (int j = 0; j < entityCount; j++) dbSelection.addEntity((IEntity) input.readObject());
                    input.close();
                }
            } catch (Exception e) {
                Log.printError(this, "Failed to load complete entity(" + entityDescriptor + ") file!", e);
            }
        }
        return dbSelection;
    }

    protected void storeTableSelection(ISelection dbSelection) {
        if (dbSelection != null && mDirectoryFile != null) {
            try {
                File entityFile = new File(mDirectoryFile, dbSelection.getEntityDescriptor().getCodeName() + ".db");
                if (entityFile.exists()) entityFile.delete();
                ObjectOutputStream output = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(entityFile), 10000));
                output.writeInt(dbSelection.size());
                for (int j = 0; j < dbSelection.size(); j++) output.writeObject(dbSelection.getEntity(j));
                output.close();
            } catch (Exception e) {
                Log.printError(this, "Failed to store complete entity(" + dbSelection.getEntityDescriptor() + ") file!", e);
            }
        }
    }

    public File getDirectoryFile() {
        return mDirectoryFile;
    }
}
