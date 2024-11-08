package org.isi.monet.core.producers;

import java.io.IOException;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EventObject;
import java.util.HashSet;
import java.util.LinkedHashMap;
import org.isi.monet.core.agents.AgentDatabase;
import org.isi.monet.core.agents.AgentFilesystem;
import org.isi.monet.core.configuration.Configuration;
import org.isi.monet.core.constants.Common;
import org.isi.monet.core.constants.Database;
import org.isi.monet.core.constants.ErrorCode;
import org.isi.monet.core.constants.Strings;
import org.isi.monet.core.exceptions.DataException;
import org.isi.monet.core.library.LibraryString;
import org.isi.monet.core.library.LibraryXSL;
import org.isi.monet.core.model.DataStore;
import org.isi.monet.core.model.DatabaseQuery;
import org.isi.monet.core.utils.BufferedQuery;

public class ProducerDataStore extends Producer {

    public ProducerDataStore() {
        super();
    }

    protected String[] getKeywords(String sData) {
        String[] aKeywords = LibraryString.getKeywords(sData, null);
        if (aKeywords.length <= 0) {
            aKeywords = new String[1];
            aKeywords[0] = Strings.EMPTY;
        }
        return aKeywords;
    }

    public Boolean exists(String code) {
        AgentDatabase oAgentDatabase = AgentDatabase.getInstance();
        LinkedHashMap<String, Object> hmParameters = new LinkedHashMap<String, Object>();
        ResultSet oResult;
        hmParameters.put(Database.QueryFields.DATA_STORE, code);
        oResult = oAgentDatabase.executeSelectQuery(Database.Queries.DATA_USER_EXIST, hmParameters);
        try {
            oResult.beforeFirst();
            oResult.next();
            oResult.getString("table_name");
            return true;
        } catch (SQLException oException) {
            return false;
        } finally {
            this.oAgentDatabase.closeQuery(oResult);
        }
    }

    public DataStore create(String code, String sSourceFilename) {
        LinkedHashMap<String, Object> hmParameters = new LinkedHashMap<String, Object>();
        DataStore oDataStore = new DataStore();
        HashSet<DatabaseQuery> hsQueries = new HashSet<DatabaseQuery>();
        DatabaseQuery[] aQueries;
        Configuration oConfiguration = Configuration.getInstance();
        String sDataStoresDir = oConfiguration.getBusinessModelDataSourcesDir();
        oDataStore.setCode(code);
        try {
            hmParameters.put(Database.QueryFields.DATA_STORE, code);
            hsQueries.add(new DatabaseQuery(Database.Queries.DATA_SOURCE_CREATE, hmParameters));
            hsQueries.add(new DatabaseQuery(Database.Queries.DATA_INDEX_CREATE, hmParameters));
            hsQueries.add(new DatabaseQuery(Database.Queries.DATA_INDEX_CREATE_SEQUENCE, hmParameters));
            hsQueries.add(new DatabaseQuery(Database.Queries.DATA_INDEX_CREATE_WORDS, hmParameters));
            hsQueries.add(new DatabaseQuery(Database.Queries.DATA_USER_CREATE, hmParameters));
            aQueries = new DatabaseQuery[hsQueries.size()];
            this.oAgentDatabase.executeQueries(hsQueries.toArray(aQueries));
            AgentFilesystem.writeFile(sDataStoresDir + Strings.BAR45 + code + Common.FileExtensions.XML, (sSourceFilename != null) ? AgentFilesystem.readFile(sSourceFilename) : "");
        } catch (Exception oException) {
            this.remove(code);
            throw new DataException(ErrorCode.CREATE_DATASTORE, code, oException);
        }
        return oDataStore;
    }

    public DataStore create(String code) {
        return this.create(code, null);
    }

    public Boolean remove(String code) {
        LinkedHashMap<String, Object> hmParameters = new LinkedHashMap<String, Object>();
        HashSet<DatabaseQuery> hsQueries = new HashSet<DatabaseQuery>();
        DatabaseQuery[] aQueries;
        Configuration oConfiguration = Configuration.getInstance();
        String sDataStoreFilename = oConfiguration.getBusinessModelDataSourcesDir() + Strings.BAR45 + code + Common.FileExtensions.XML;
        hmParameters.put(Database.QueryFields.DATA_STORE, code);
        hsQueries.add(new DatabaseQuery(Database.Queries.DATA_SOURCE_REMOVE, hmParameters));
        hsQueries.add(new DatabaseQuery(Database.Queries.DATA_INDEX_REMOVE, hmParameters));
        hsQueries.add(new DatabaseQuery(Database.Queries.DATA_INDEX_REMOVE_SEQUENCE, hmParameters));
        hsQueries.add(new DatabaseQuery(Database.Queries.DATA_INDEX_REMOVE_WORDS, hmParameters));
        hsQueries.add(new DatabaseQuery(Database.Queries.DATA_USER_REMOVE, hmParameters));
        aQueries = new DatabaseQuery[hsQueries.size()];
        this.oAgentDatabase.executeQueries(hsQueries.toArray(aQueries));
        if (AgentFilesystem.existFile(sDataStoreFilename)) AgentFilesystem.removeFile(sDataStoreFilename);
        return true;
    }

    public Boolean populate(String code) {
        Configuration oConfiguration = Configuration.getInstance();
        String sDataStoreFilename = oConfiguration.getBusinessModelDataSourcesDir() + Strings.BAR45 + code + Common.FileExtensions.XML;
        String sDataStoreConverterFilename = oConfiguration.getDatastoreConverterFilename();
        Reader oReaderData = LibraryXSL.compileFromFile(sDataStoreConverterFilename, sDataStoreFilename);
        BufferedQuery oBufferedQuery;
        if (oReaderData == null) throw new DataException(ErrorCode.POPULATE_DATASTORE, code);
        oBufferedQuery = new BufferedQuery(oReaderData);
        if (!this.exists(code)) {
            this.create(code, sDataStoreFilename);
        }
        try {
            this.oAgentDatabase.executeUpdateTransaction(oBufferedQuery);
        } finally {
            try {
                oBufferedQuery.close();
            } catch (IOException oException) {
            }
        }
        return true;
    }

    public Boolean populate(String code, String sSourceFilename) {
        Configuration oConfiguration = Configuration.getInstance();
        String sDataStoresDir = oConfiguration.getBusinessModelDataSourcesDir();
        if (!this.exists(code)) {
            this.create(code, sSourceFilename);
        }
        AgentFilesystem.writeFile(sDataStoresDir + Strings.BAR45 + code + Common.FileExtensions.XML, AgentFilesystem.readFile(sSourceFilename));
        return this.populate(code);
    }

    public Object newObject() {
        return new DataStore();
    }

    public Boolean loadAttribute(EventObject oEventObject, String sAttribute) {
        return true;
    }
}
