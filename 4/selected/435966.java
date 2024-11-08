package ossobook2010.controller;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.mail.MessagingException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ossobook2010.Messages;
import ossobook2010.StartOssoBook;
import ossobook2010.exceptions.EntriesException;
import ossobook2010.exceptions.MissingInputException;
import ossobook2010.exceptions.NoRightException;
import ossobook2010.exceptions.NotConnectedException;
import ossobook2010.exceptions.NotLoadedException;
import ossobook2010.exceptions.OssobookOutOfDateException;
import ossobook2010.exceptions.StatementNotExecutedException;
import ossobook2010.gui.MainFrame;
import ossobook2010.gui.components.content.Content;
import ossobook2010.helpers.CSVExport;
import ossobook2010.gui.SplashScreen;
import ossobook2010.gui.components.dialogs.PluginOutDated;
import ossobook2010.helpers.metainfo.Concordance;
import ossobook2010.helpers.metainfo.EntryData;
import ossobook2010.helpers.metainfo.MeasurementData;
import ossobook2010.helpers.metainfo.UserRight;
import ossobook2010.helpers.database.ConnectionType;
import ossobook2010.helpers.metainfo.CodeTablesEntry;
import ossobook2010.helpers.metainfo.DbDataHash;
import ossobook2010.helpers.metainfo.Key;
import ossobook2010.helpers.metainfo.Project;
import ossobook2010.helpers.metainfo.UniqueArrayList;
import ossobook2010.plugins.PluginLoader;
import ossobook2010.querys.ILanguageManager;
import ossobook2010.querys.IQueryManager;
import ossobook2010.querys.IUserManager.Right;
import ossobook2010.queries.InputUnitManager;
import ossobook2010.queries.QueryManager;
import ossobook2010.synchronization.Mail;
import ossobook2010.plugins.IPlugin;
import ossobook2010.plugins.PluginDatamanager;
import ossobook2010.plugins.PluginInformation;
import ossobook2010.querys.IInputUnitManager;

/**
 * The GUI controller holds important methods
 * which are neccessary from other parts of the application
 * than the graphical user interface (GUI).
 *
 * @author lord joda
 * @author Daniel Kaltenthaler
 */
public class GuiController implements IGuiController {

    /** The basic MainFrame object. */
    private MainFrame mainFrame;

    /**  */
    private static IQueryManager query;

    /**  */
    private static IQueryManager localSync;

    /**  */
    private static IQueryManager globalSync;

    /** Status if a project is load or not. */
    private boolean projectLoaded = false;

    /**the CSVExport*/
    private CSVExport export;

    /** The project which is currently load. Null if no project is load. */
    private Project project;

    /** The basic logging object. */
    private static final Log log = LogFactory.getLog(GuiController.class);

    private boolean installed;

    private HashMap<String, IPlugin> plugins;

    private ArrayList<PluginDatamanager> datamanagers = new ArrayList<PluginDatamanager>();

    /**
     * Constructor of the GuiController class.
     */
    public GuiController(boolean installed, SplashScreen splash) {
        plugins = new HashMap<String, IPlugin>();
        mainFrame = new MainFrame(this, splash, installed);
        this.installed = installed;
    }

    private void thisMethodeIsJustForRememberingWhatToChangeForSync() throws Exception {
        if (true) {
            throw new Exception("this methode is not to be invoked!");
        }
        query.getGlobalProjectData(project, 0, null);
        query.setSynchronized(project);
        query.setMessageNumber(project);
        query.insertData(null, project, installed);
    }

    @Override
    public boolean login(String name, String password, ConnectionType type) throws NotConnectedException, StatementNotExecutedException {
        query = new QueryManager(name, password, type);
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    if (isConnectedToLocal()) {
                        if (!checkVersion()) {
                            mainFrame.displayCustomMessage(Messages.getString("AN_UPDATE_IS_AVAILABLE"), new Color(47, 155, 239), Color.WHITE, 60000);
                        }
                    }
                } catch (NotConnectedException ex) {
                    Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }).start();
        export = new CSVExport(this, query.getUnderlyingConnection());
        return query.checkVersion();
    }

    @Override
    public Project newProject(String projectName, int yearFrom, int yearTo, String country, String place, String altitude, String latitude, String longitude, String note, String institution) throws StatementNotExecutedException, MissingInputException {
        if (projectName.length() > 0) {
            project = query.newProject(projectName, yearFrom, yearTo, country, place, altitude, latitude, longitude, note, institution);
            return project;
        } else {
            throw new MissingInputException();
        }
    }

    @Override
    public ArrayList<Project> getProjects() throws NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getProjectManager().getProjects();
    }

    @Override
    public Right loadProject(Project project) throws StatementNotExecutedException {
        projectLoaded = true;
        boolean admin = query.isAdmin();
        Right right = Right.WRITE;
        if (!admin) {
            right = query.getUserManager().getRight(project.getProjectKey());
        }
        this.project = null;
        if (right != Right.NORIGHTS) {
            this.project = project;
        }
        return right;
    }

    @Override
    public boolean isConnectedToDatabase() {
        return (query != null);
    }

    @Override
    public boolean isProjectLoaded() {
        return projectLoaded;
    }

    @Override
    public void logout() {
        query = null;
        project = null;
        projectLoaded = false;
    }

    @Override
    public void unloadProject() {
        String oldProjectName = project.getName();
        projectLoaded = false;
        project = null;
        mainFrame.displayConfirmation(Messages.getString("PROJECT_UNLOADED", oldProjectName));
    }

    /**
     * Writes the data from the vector into the given strings to be used for SQL query to update or insert
     * into the database.
     *
     * Used to convert strings into the corresponding IDs.
     *
     * @param values
     *		A string in which the values of the columns is storend
     * @param names
     *		A string in which the name of the columns is stored
     * @param vector
     *		The vector in which the orginal data is contained
     * @throws EntriesException
     * @throws DatabaseExceptions
     */
    private void splitDataHash(ArrayList<DbDataHash> vector, ArrayList<String> values, ArrayList<String> names) throws EntriesException, StatementNotExecutedException, NotConnectedException, NotLoadedException, NoRightException {
        Iterator<DbDataHash> it = vector.iterator();
        while (it.hasNext()) {
            DbDataHash data = it.next();
            String columnName = data.getDbColumnName();
            String entry = data.getData();
            if (columnName == null || columnName.equals("")) {
                continue;
            }
            if (entry == null) {
                System.out.println("column name to be removed: " + columnName);
                continue;
            }
            values.add(String.valueOf(entry));
            names.add(columnName);
        }
    }

    @Override
    public boolean saveEntry(ArrayList<DbDataHash> dataHash, ArrayList<Integer> animals, ArrayList<MeasurementData> measurements, ArrayList<Integer> wearstage2, ArrayList<String> genes, ArrayList<Integer> boneElement) throws NotLoadedException, EntriesException, StatementNotExecutedException, NoRightException, NotConnectedException {
        checkWriteRights();
        ArrayList<String> values = new ArrayList<String>();
        ArrayList<String> columns = new ArrayList<String>();
        Key artefactID = new Key(-1, -1);
        Key massID = new Key(-1, -1);
        Key wearstage2ID = new Key(-1, -1);
        splitDataHash(dataHash, values, columns);
        if (measurements != null && measurements.size() > 0) {
            massID = query.getMeasurementManager().insertIntoMasse(measurements);
        }
        if (wearstage2 != null && wearstage2.size() > 0) {
            wearstage2ID = query.getWearstage2Manager().insertIntoWearstage2Values(wearstage2);
        }
        Key recordKey = query.getInputUnitManager().insertIntoInputUnit(project, columns, values, artefactID, massID, wearstage2ID);
        if (genes != null && genes.size() > 0) {
            query.getGeneLibaryManager().insertValues(genes, recordKey);
        }
        if (boneElement != null && boneElement.size() > 0) {
            query.getBoneElementManager().insertValues(boneElement, recordKey);
        }
        if (animals != null && animals.size() > 0) {
            query.getAnimalManager().updateValues(animals, recordKey);
        }
        return true;
    }

    @Override
    public void updateEntry(int id, int dbid, ArrayList<DbDataHash> dataHash, ArrayList<Integer> animals, ArrayList<MeasurementData> measurements, ArrayList<Integer> wearstage2, ArrayList<String> genes, ArrayList<Integer> boneElement, boolean animalSkelettonChanged) throws EntriesException, NotLoadedException, NoRightException, StatementNotExecutedException, NotConnectedException {
        checkWriteRights();
        Key recordKey = new Key(id, dbid);
        ArrayList<String> values = new ArrayList<String>();
        ArrayList<String> columns = new ArrayList<String>();
        Key artefactID = new Key(-1, -1);
        Key massID;
        Key wearstage2ID;
        splitDataHash(dataHash, values, columns);
        massID = query.getInputUnitManager().getMassKey(recordKey);
        wearstage2ID = query.getInputUnitManager().getWearstage2Key(recordKey);
        if (massID.getID() == -1 || massID.getDBNumber() == -1) {
            if (measurements != null && !measurements.isEmpty()) {
                massID = query.getMeasurementManager().insertIntoMasse(measurements);
            }
        } else {
            if (measurements != null && !measurements.isEmpty()) {
                query.getMeasurementManager().insertIntoMasse(measurements, massID);
            } else {
                query.getMeasurementManager().deleteValue(massID);
            }
        }
        if (wearstage2 != null && (wearstage2ID.getID() == -1 || wearstage2ID.getDBNumber() == -1)) {
            if (!wearstage2.isEmpty()) {
                wearstage2ID = query.getWearstage2Manager().insertIntoWearstage2Values(wearstage2);
            }
        } else {
            if (wearstage2 != null && !wearstage2.isEmpty()) {
                query.getWearstage2Manager().insertIntoWearstage2Values(wearstage2, wearstage2ID);
            } else {
                query.getWearstage2Manager().deleteValue(wearstage2ID);
            }
        }
        if (genes == null) {
            query.getGeneLibaryManager().deleteValue(recordKey);
        } else {
            query.getGeneLibaryManager().updateValues(genes, recordKey);
        }
        if (boneElement == null) {
            query.getBoneElementManager().deleteValue(recordKey);
        } else {
            query.getBoneElementManager().updateValues(boneElement, recordKey);
        }
        if (animals == null) {
            query.getAnimalManager().deleteValue(recordKey);
        } else {
            query.getAnimalManager().updateValues(animals, recordKey);
        }
        query.getInputUnitManager().updateEntry(recordKey, project, columns, values, wearstage2ID, artefactID, massID);
    }

    @Override
    public void deleteProject(Project project) throws NotConnectedException, EntriesException, NoRightException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        if (!isProjectOwner(project) && !isAdmin()) {
            throw new NoRightException(NoRightException.Rights.NOPROJECTOWNER);
        }
        query.getProjectManager().delete(project);
        query.getMeasurementManager().delete(project);
        query.getWearstage2Manager().delete(project);
        query.getAnimalManager().delete(project);
        query.getConcordanceManager().delete(project);
        query.getGeneLibaryManager().delete(project);
        query.getBoneElementManager().delete(project);
        query.getInputUnitManager().delete(project);
    }

    @Override
    public EntryData loadEntry(int id, int dbid) throws NotConnectedException, NoRightException, StatementNotExecutedException, NotLoadedException, EntriesException {
        checkReadRights();
        Key recordKey = new Key(id, dbid);
        Key projectKey = query.getProjectManager().getCorrespondingProjectKey(recordKey);
        if (!project.getProjectKey().equals(projectKey)) {
            throw new NotLoadedException();
        }
        HashMap<String, String> input = query.getInputUnitManager().getDataRecord(recordKey);
        Key measurementKey = query.getInputUnitManager().getMassKey(recordKey);
        ArrayList<MeasurementData> measurement = query.getMeasurementManager().getMeasurementEntry(measurementKey);
        Key wearstage2Key = query.getInputUnitManager().getWearstage2Key(recordKey);
        ArrayList<Integer> wearstage2 = query.getWearstage2Manager().getWearstage2Entry(wearstage2Key);
        ArrayList<String> geneLibary = query.getGeneLibaryManager().getValues(recordKey);
        ArrayList<Integer> boneElement = query.getBoneElementManager().getValues(recordKey);
        ArrayList<Integer> animals = query.getAnimalManager().getValues(recordKey);
        System.out.println(animals);
        return new EntryData(input, measurement, wearstage2, animals, geneLibary, boneElement);
    }

    @Override
    public EntryData loadEntry(int id, int dbid, Key project) throws NotConnectedException, NoRightException, StatementNotExecutedException, NotLoadedException, EntriesException {
        checkReadRights(project);
        Key recordKey = new Key(id, dbid);
        HashMap<String, String> input = query.getInputUnitManager().getDataRecord(recordKey);
        Key measurementKey = query.getInputUnitManager().getMassKey(recordKey);
        ArrayList<MeasurementData> measurement = query.getMeasurementManager().getMeasurementEntry(measurementKey);
        Key wearstage2Key = query.getInputUnitManager().getWearstage2Key(recordKey);
        ArrayList<Integer> wearstage2 = query.getWearstage2Manager().getWearstage2Entry(wearstage2Key);
        ArrayList<String> geneLibary = query.getGeneLibaryManager().getValues(recordKey);
        ArrayList<Integer> boneElement = query.getBoneElementManager().getValues(recordKey);
        ArrayList<Integer> animals = query.getAnimalManager().getValues(recordKey);
        System.out.println(animals);
        return new EntryData(input, measurement, wearstage2, animals, geneLibary, boneElement);
    }

    @Override
    public int getNumberOfEntries() throws StatementNotExecutedException, NotConnectedException, NotLoadedException, NoRightException {
        checkReadRights();
        int[] projectKey = { project.getNumber(), project.getDatabaseNumber() };
        return query.getInputUnitManager().getNumberofEntrys(projectKey);
    }

    @Override
    public void deleteEntry(int id, int dbid) throws StatementNotExecutedException, EntriesException, NoRightException {
        Key recordID = new Key(id, dbid);
        Key projID = query.getInputUnitManager().getProjectID(recordID);
        Project proj = query.getProjectManager().getProject(projID.getID(), projID.getDBNumber());
        Right right = query.getUserManager().getRight(project.getProjectKey());
        if (right != Right.WRITE) {
            throw new NoRightException(NoRightException.Rights.NOWRITE);
        }
        Key massid = query.getInputUnitManager().getMassKey(recordID);
        query.getMeasurementManager().deleteValue(massid);
        Key wearstage2Key = query.getInputUnitManager().getWearstage2Key(recordID);
        Key artefactID = query.getInputUnitManager().getArtefactId(recordID);
        query.getArtefactManager().deleteValue(artefactID);
        query.getInputUnitManager().deleteValue(recordID);
        query.getWearstage2Manager().deleteValue(wearstage2Key);
        query.getAnimalManager().deleteValue(recordID);
        query.getBoneElementManager().deleteValue(recordID);
        query.getGeneLibaryManager().deleteValue(recordID);
    }

    @Override
    public ArrayList<String[]> getProjectInformation() throws StatementNotExecutedException, NotConnectedException, NotLoadedException, NoRightException {
        checkReadRights();
        return query.getProjectManager().getProjektInformations(project);
    }

    @Override
    public ArrayList<String[]> getArchaeologicalUnitInformation() throws NotConnectedException, NotLoadedException, StatementNotExecutedException, NoRightException {
        checkReadRights();
        return query.getInputUnitManager().getFkCount(project);
    }

    @Override
    public ArrayList<String[]> getAnimalListingInformation() throws NotConnectedException, NotLoadedException, StatementNotExecutedException, NoRightException {
        checkReadRights();
        return query.getInputUnitManager().getAnimalCount(project);
    }

    @Override
    public ArrayList<String[]> getOtherStatisticsInformation() throws NotConnectedException, NotLoadedException, StatementNotExecutedException, NoRightException {
        checkReadRights();
        ArrayList<String[]> data = new ArrayList<String[]>();
        data.add(query.getInputUnitManager().getBoneCount(project));
        data.add(new String[] { Messages.getString("ARTEFACTNOTECOUNT"), query.getInputUnitManager().getArtifactNoteCount(project) });
        data.add(new String[] { Messages.getString("INVENTORYNUMBERCOUNT"), query.getInputUnitManager().getInventoryNumberCount(project) });
        int count = query.getInputUnitManager().getBonesNumber(project);
        double weight = query.getInputUnitManager().getOverallWeight(project);
        data.add(new String[] { Messages.getString("TOTALWEIGHT"), String.valueOf(weight) });
        data.add(new String[] { Messages.getString("WEIGHT/BONES"), String.valueOf(InputUnitManager.roundTwoDecimals(weight / count)) });
        return data;
    }

    @Override
    public String getUserName() throws NotConnectedException {
        return query.getUsername();
    }

    @Override
    public boolean isLocalDatabaseLoaded() {
        return query.isLocal();
    }

    /**
     * Checks if a project is loaded, if there is a connection and if the
     * user has the right to read the current project
     *
     * @throws NotConnectedException
     *		If there is no connection to a database
     * @throws NotLoadedException
     *		If no project was loaded
     * @throws StatementNotExecutedException
     *		If a SQL error appeared
     * @throws NoRightException
     *		If the user has no right to read the current Project
     */
    private void checkReadRights() throws NotConnectedException, NotLoadedException, StatementNotExecutedException, NoRightException {
        if (query == null) {
            throw new NotConnectedException();
        }
        if (project == null) {
            throw new NotLoadedException();
        }
        if (query.getUserManager().getRight(project.getProjectKey()) == Right.NORIGHTS && !query.isAdmin()) {
            throw new NoRightException(NoRightException.Rights.NOREAD);
        }
    }

    /**
     * Checks if a project is loaded, if there is a connection and if the
     * user has the right to read the current project
     *
     * @throws NotConnectedException
     *		If there is no connection to a database
     * @throws NotLoadedException
     *		If no project was loaded
     * @throws StatementNotExecutedException
     *		If a SQL error appeared
     * @throws NoRightException
     *		If the user has no right to read the current Project
     */
    private void checkReadRights(Key project) throws NotConnectedException, NotLoadedException, StatementNotExecutedException, NoRightException {
        if (query == null) {
            throw new NotConnectedException();
        }
        if (query.getUserManager().getRight(project) == Right.NORIGHTS && !query.isAdmin()) {
            throw new NoRightException(NoRightException.Rights.NOREAD);
        }
    }

    /**
     * Checks if a project is loaded, if there is a connection and if the user has the right to read the
     * current project
     *
     * @throws NotConnectedException
     *		If there is no connection to a database
     * @throws NotLoadedException
     *		If no project was loaded
     * @throws StatementNotExecutedException
     *		If a SQL error appeared
     * @throws NoRightException
     *		If the user has no right to read the current Project
     */
    private void checkWriteRights() throws NotConnectedException, NotLoadedException, StatementNotExecutedException, NoRightException {
        if (query == null) {
            throw new NotConnectedException();
        }
        if (project == null) {
            throw new NotLoadedException();
        }
        if (query.getUserManager().getRight(project.getProjectKey()) != Right.WRITE && !query.isAdmin()) {
            throw new NoRightException(NoRightException.Rights.NOWRITE);
        }
    }

    /**
	 * Checks if there is a connection and if the user has the right to read the
	 * given project
	 *
	 * @throws NotConnectedException
	 *		If there is no connection to a database
	 * @throws NotLoadedException
	 *		If no project was loaded
	 * @throws StatementNotExecutedException
	 *		If a SQL error appeared
	 * @throws NoRightException
	 *		If the user has no right to read the current Project
	 */
    private void checkWriteRights(Project project) throws NotConnectedException, StatementNotExecutedException, NoRightException {
        if (query == null) {
            throw new NotConnectedException();
        }
        if (query.getUserManager().getRight(project.getProjectKey()) != Right.WRITE && !query.isAdmin()) {
            throw new NoRightException(NoRightException.Rights.NOWRITE);
        }
    }

    @Override
    public String[][] loadArtefakte(int id, int dbnumber) throws NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        int[] artefactKey = new int[] { id, dbnumber };
        return query.getArtefactManager().getArtefaktmaske(artefactKey);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof IGuiController.Event) {
            switch((IGuiController.Event) arg) {
                case LOGIN:
                    mainFrame.reloadGui(Content.Id.LOGIN);
                    break;
                case SEARCH:
                    mainFrame.reloadGui(Content.Id.ENTRY_SEARCH);
                    break;
                case PROJECT:
                    mainFrame.reloadGui(Content.Id.PROJECT);
                    break;
                case EXIT:
                    System.exit(0);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void CSVExport(ArrayList<String> list) throws NotConnectedException, NotLoadedException, NoRightException, StatementNotExecutedException {
        checkReadRights();
        String queryMeasurements;
        try {
            String select = "Select * From " + InputUnitManager.TABLENAME_INPUT_UNIT + " WHERE " + InputUnitManager.PROJECT_ID + "=" + project.getNumber() + " AND " + InputUnitManager.PROJECT_DATABASE_NUMBER + "=" + project.getDatabaseNumber();
            ArrayList<String> selectedFields = new ArrayList<String>();
            export.exportData(project, list);
            mainFrame.displayConfirmation(Messages.getString("CSV_EXPORT_SUCCESSFULL"));
        } catch (Exception exception) {
            log.error("CSVEXPORT", exception);
            mainFrame.displayError(Messages.getString("ERROR_WHILE_EXPORTING"));
            mainFrame.getFooter().setProgressBarVisible(false);
        }
    }

    /**
     * Returns a New Local manager if no one is existing
     *
     * @return
     *		A new Local manager if no one is existing
     * @throws NotConnectedException
     * @throws StatementNotExecutedException
     */
    public static IQueryManager getLocalManager() throws NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        if (localSync == null) {
            localSync = query.createLocalManager();
        }
        return localSync;
    }

    /**
     * Returns a new Sync manager if no one is existing.
     *
     * @return
     *		A new Sync manager if no one is existing
     * @throws NotConnectedException
     * @throws StatementNotExecutedException
     */
    public static IQueryManager getSyncManager() throws NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        if (globalSync == null) {
            globalSync = query.createSyncManager();
        }
        return globalSync;
    }

    @Override
    public boolean checkSyncIndicator() throws StatementNotExecutedException {
        return query.checkSyncIndicator();
    }

    @Override
    public void changePasswort(String password) {
        String sql = "SET PASSWORD FOR 'root'@'localhost' = PASSWORD('password')";
    }

    @Override
    public void saveProject(int projectID, int databaseNumber, String projectName, int yearFrom, int yearTo, String country, String place, String altitude, String latitude, String longitude, String note, String institution) throws NotConnectedException, StatementNotExecutedException, NoRightException, EntriesException {
        if (query == null) {
            throw new NotConnectedException();
        }
        if (!isProjectOwner(query.getProjectManager().getProject(projectID, databaseNumber)) && !isAdmin()) {
            throw new NoRightException(NoRightException.Rights.NOPROJECTOWNER);
        }
        query.getProjectManager().saveProject(projectID, databaseNumber, projectName, yearFrom, yearTo, country, place, altitude, latitude, longitude, note, institution);
    }

    @Override
    public ArrayList<ArrayList<String>> getEntries(int offset, int count) throws NotConnectedException, NotLoadedException, StatementNotExecutedException, NoRightException {
        checkReadRights();
        return export.getProjectExport(project, getAvailableExportEntries(), count, offset);
    }

    @Override
    public ArrayList<ArrayList<String>> getEntryExport(Key key) throws StatementNotExecutedException {
        return export.getEntryExport(key, getAvailableExportEntries());
    }

    @Override
    public ArrayList<ArrayList<String>> getEntryExport(Key key, ArrayList<String> list) throws StatementNotExecutedException {
        return export.getEntryExport(key, list);
    }

    @Override
    public ArrayList<ArrayList<String>> getEntries(int offset) throws NotConnectedException, NotLoadedException, StatementNotExecutedException, NoRightException {
        return getEntries(offset, 50);
    }

    @Override
    public ArrayList<String> getFeatureName1Information() throws NotConnectedException, NotLoadedException, StatementNotExecutedException, NoRightException {
        checkReadRights();
        UniqueArrayList data = query.getInputUnitManager().getInformation(IInputUnitManager.FEATURENAME);
        data.addAll(getData(IInputUnitManager.FEATURENAME));
        return data;
    }

    @Override
    public ArrayList<String> getFeatureName2Information() throws NotConnectedException, NotLoadedException, StatementNotExecutedException, NoRightException {
        checkReadRights();
        UniqueArrayList data = query.getInputUnitManager().getInformation(IInputUnitManager.FEATURENAMEDETAILED);
        data.addAll(getData(IInputUnitManager.FEATURENAMEDETAILED));
        return data;
    }

    @Override
    public ArrayList<String> getExcavationMethodInformation() throws NotConnectedException, NotLoadedException, NoRightException, StatementNotExecutedException {
        checkReadRights();
        UniqueArrayList data = query.getInputUnitManager().getInformation(IInputUnitManager.EXCAVATIONMETHOD);
        data.addAll(getData(IInputUnitManager.EXCAVATIONMETHOD));
        return data;
    }

    @Override
    public void updateDatabase() throws NotConnectedException, StatementNotExecutedException, OssobookOutOfDateException {
        IQueryManager local;
        IQueryManager global;
        local = query.createDBUpdateLocalManager();
        global = query.createSyncManager();
        String dbVersion = local.getDatabaseManager().getDatabaseVersion();
        do {
            String[] updateCommand = global.getUpdateManager().getUpdate(dbVersion);
            local.getUpdateManager().applyUpdate(updateCommand);
            dbVersion = local.getDatabaseManager().getDatabaseVersion();
        } while (!dbVersion.equals(StartOssoBook.DATABASE_VERSION));
        local.getUpdateManager().setSyncIndicator();
    }

    public static IQueryManager getDbUpdateManager() throws NotConnectedException, StatementNotExecutedException, OssobookOutOfDateException {
        return query.createDBUpdateLocalManager();
    }

    @Override
    public void startUpdater(String fileName, String parameter) {
        try {
            log.info("Updating Updater");
            URL fileUrl = new URL("http://ossobook.svn.sourceforge.net/svnroot/ossobook/trunk/update/" + fileName);
            URLConnection filecon = fileUrl.openConnection();
            ReadableByteChannel rbc = Channels.newChannel(fileUrl.openStream());
            File testFile = new File(fileName);
            int size = filecon.getContentLength();
            if (testFile.length() == size) {
            } else {
                FileOutputStream fos = new FileOutputStream(fileName);
                fos.getChannel().transferFrom(rbc, 0, 1 << 24);
                fos.close();
            }
            Runtime.getRuntime().exec(new String[] { "java", "-jar", fileName, parameter });
            System.exit(0);
        } catch (IOException ex) {
            Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public String convertSkeletonIdToString(int skeletonId) throws EntriesException, NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getSkeletonManager().getSkeletonName(skeletonId);
    }

    @Override
    public int convertSkeletonStringToId(String skeletonName) throws EntriesException, NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getSkeletonManager().getSkeletonCode(skeletonName);
    }

    @Override
    public ArrayList<CodeTablesEntry> getMeasurementFieldIds(int animalId, int skeletonElementId) throws NotConnectedException, EntriesException, NotLoadedException, StatementNotExecutedException, NoRightException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getMeasurementManager().getMeasurements(query.getAnimalManager().getAnimalLabel(animalId), query.getSkeletonManager().getSkeletonLabel(skeletonElementId));
    }

    public ArrayList<CodeTablesEntry> getMeasurementFieldNames(ArrayList<Integer> list) throws StatementNotExecutedException, EntriesException, NotConnectedException, NotLoadedException, NoRightException {
        return query.getMeasurementManager().getMeasurementNames(list);
    }

    @Override
    public ArrayList<String[]> getWearStage2Information(int animalID, int skeleton) throws NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getInputUnitManager().getWearstage2Information(animalID, skeleton);
    }

    @Override
    public ArrayList<String> getPathologyInformation() throws NotConnectedException, StatementNotExecutedException, NotLoadedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        if (project == null) {
            throw new NotLoadedException();
        }
        UniqueArrayList data = query.getInputUnitManager().getInformation(IInputUnitManager.PATHOLOGY);
        data.addAll(getData(IInputUnitManager.PATHOLOGY));
        return data;
    }

    @Override
    public String getNewUnusedIndividualNumber() throws NotConnectedException, StatementNotExecutedException {
        try {
            checkReadRights();
            String individualNr = "";
            individualNr += query.getDatabaseManager().getDatabaseNumber() + "P";
            individualNr += project.getNumber() + "D";
            individualNr += project.getDatabaseNumber() + "N";
            int number = query.getInputUnitManager().getNextFreeIndividualNr(project);
            while (!query.getInputUnitManager().isIndividualNrFree(individualNr + number)) {
                number++;
            }
            return individualNr + number;
        } catch (NotLoadedException ex) {
            Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoRightException ex) {
            Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }

    @Override
    public ArrayList<Integer> getCurrentAnimalIdsOfEntry(int entryId, int entryDBID) throws EntriesException, NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getAnimalManager().getValues(new Key(entryId, entryDBID));
    }

    @Override
    public int getLabelOfAnimalId(int animalId) throws NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getAnimalManager().getAnimalLabel(animalId);
    }

    @Override
    public Integer getCurrentSkeletonElementIdsOfEntry(int entryId, int entryDBID) throws EntriesException, NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getInputUnitManager().getSkeletonIdOfEntry(entryId, entryDBID);
    }

    @Override
    public int getLabelOfSkelettonElementId(int skelettonElement) throws NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getSkeletonManager().getSkeletonLabel(skelettonElement);
    }

    @Override
    public ArrayList<CodeTablesEntry> getSortedEntriesWithID(String tablename) throws NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getInputUnitManager().getSortedEntriesWithID(tablename);
    }

    @Override
    public String convertMeasurementIdToString(int measurementId) throws NotConnectedException, EntriesException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getMeasurementManager().getMeasurementName(measurementId);
    }

    @Override
    public void sendTestMessage() {
    }

    @Override
    public String getLocalDatabaseVersion() throws NotConnectedException, StatementNotExecutedException {
        return getLocalManager().getDatabaseManager().getDatabaseVersion();
    }

    @Override
    public String getGlobalDatabaseVersion() throws NotConnectedException, StatementNotExecutedException {
        return getSyncManager().getDatabaseManager().getDatabaseVersion();
    }

    @Override
    public String getLocalProgrammVersion() throws NotConnectedException, StatementNotExecutedException {
        return getLocalManager().getDatabaseManager().getOssobookVersion();
    }

    @Override
    public String getGlobalProgrammVersion() throws NotConnectedException, StatementNotExecutedException {
        return getSyncManager().getDatabaseManager().getOssobookVersion();
    }

    @Override
    public boolean isConnectedToLocal() throws NotConnectedException {
        return query.isLocal();
    }

    @Override
    public boolean isConnectedToGlobal() throws NotConnectedException {
        return !query.isLocal();
    }

    @Override
    public boolean checkVersion() {
        try {
            return getSyncManager().checkVersion();
        } catch (NotConnectedException ex) {
            Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (StatementNotExecutedException ex) {
            Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    @Override
    public boolean existsWearstage(int animal, int skeleton) throws StatementNotExecutedException, NotConnectedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getWearstage2Manager().existsWearstage(animal, skeleton);
    }

    @Override
    public Key insertIntoWearstage2Values(ArrayList<Integer> wearstage2List) throws NoRightException, StatementNotExecutedException, NotConnectedException, NotLoadedException {
        checkReadRights();
        return query.getWearstage2Manager().insertIntoWearstage2Values(wearstage2List);
    }

    @Override
    public boolean isAdmin() throws StatementNotExecutedException, NotConnectedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        if (StartOssoBook.isDevelopmentMode) {
            return true;
        } else {
            return query.isAdmin();
        }
    }

    @Override
    public void saveUserRights(ArrayList<UserRight> data) throws StatementNotExecutedException, NoRightException, NotLoadedException, NotConnectedException {
        checkReadRights();
        if (!query.isAdmin()) {
            throw new NoRightException(NoRightException.Rights.NOADMIN);
        }
        for (UserRight right : data) {
            query.getUserManager().changeRight(project, query.getUserManager().getUserID(right.getName()), right.getRight());
            query.getUserManager().setAdmin(right.getName(), right.isAdmin());
        }
    }

    @Override
    public void saveUserRightsProject(ArrayList<UserRight> data) throws StatementNotExecutedException, NoRightException, NotLoadedException, NotConnectedException {
        checkReadRights();
        if (!isProjectOwner(project)) {
            throw new NoRightException(NoRightException.Rights.NOADMIN);
        }
        for (UserRight right : data) {
            System.out.println(right.getName() + ", " + right.getRight());
            query.getUserManager().changeRight(project, query.getUserManager().getUserID(right.getName()), right.getRight());
        }
    }

    @Override
    public ArrayList<String> getUserNames() throws StatementNotExecutedException, NotConnectedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getUserManager().getUserNames();
    }

    @Override
    public ArrayList<String> getListOfUsedIndividualNumbers() throws StatementNotExecutedException, NotConnectedException, NotLoadedException, NoRightException {
        checkReadRights();
        return query.getInputUnitManager().getListOfUsedIndividualNumbers(project);
    }

    @Override
    public ArrayList<CodeTablesEntry> getBoneElementComboInformation(int skeletonElement) throws NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getInputUnitManager().getBoneElement(skeletonElement);
    }

    @Override
    public ArrayList<UserRight> getAllUserRights() throws StatementNotExecutedException, NotConnectedException, NoRightException, NotLoadedException {
        checkReadRights();
        return query.getUserManager().getUserRights(project);
    }

    @Override
    public int displayBoneElementGraphic(int skeletonElement) throws StatementNotExecutedException, NotConnectedException, NoRightException, EntriesException, NotLoadedException {
        checkReadRights();
        return query.getSkeletonManager().displayBoneElementGraphic(skeletonElement);
    }

    @Override
    public UserRight getCurrentUserRights() throws StatementNotExecutedException, NotConnectedException, NoRightException, NotLoadedException, EntriesException {
        checkReadRights();
        return query.getUserManager().getUserRight(project);
    }

    @Override
    public UserRight getCurrentUserRights(Project p) throws StatementNotExecutedException, NotConnectedException, NoRightException, NotLoadedException, EntriesException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getUserManager().getUserRight(p);
    }

    @Override
    public ArrayList<Concordance> getConcordanceInformation() throws StatementNotExecutedException, NotConnectedException, NoRightException, NotLoadedException, EntriesException {
        checkReadRights();
        return query.getConcordanceManager().getConcordanceInformation(project);
    }

    @Override
    public void saveConcordanceInformation(ArrayList<Concordance> concordances) throws StatementNotExecutedException, NotConnectedException, NoRightException, NotLoadedException, EntriesException {
        query.getConcordanceManager().saveConcordanceInformation(concordances, project);
    }

    @Override
    public ArrayList<String> getRelativeDatationInformation() throws StatementNotExecutedException, NotConnectedException, NoRightException, NotLoadedException {
        checkReadRights();
        ArrayList<String> data = query.getInputUnitManager().getInformation(InputUnitManager.RELATIVEDATING);
        data.addAll(getData(IInputUnitManager.RELATIVEDATING));
        return data;
    }

    @Override
    public boolean isProjectOwner(Project p) throws NotConnectedException {
        return p.getOwner().equals(getUserName());
    }

    @Override
    public boolean isProjectOwner() throws NotConnectedException {
        if (project == null) {
            throw new NotConnectedException();
        }
        return project.getOwner().equals(getUserName());
    }

    @Override
    public ArrayList<PluginInformation> getPluginInformation() {
        ArrayList<PluginInformation> al = new ArrayList<PluginInformation>();
        File dir = new File(System.getProperty("user.dir") + System.getProperty("file.separator") + "plugins");
        if (StartOssoBook.isDevelopmentMode) {
            try {
                IPlugin plugin = new analysis.Main();
                PluginInformation info = plugin.getPluginInformation();
                String command = info.getName();
                while (plugins.containsKey(command)) {
                    command += "1";
                }
                info.setActionCommand(command);
                plugins.put(command, plugin);
                al.add(info);
                System.out.println("this");
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
        if (!dir.exists()) {
            return al;
        }
        for (File file : dir.listFiles()) {
            try {
                if (file.isDirectory()) {
                    continue;
                }
                Class<IPlugin> clasz = PluginLoader.loadPlugin(file.getName());
                IPlugin plugin = clasz.newInstance();
                PluginInformation info = plugin.getPluginInformation();
                String command = info.getName();
                while (plugins.containsKey(command)) {
                    command += "1";
                }
                info.setActionCommand(command);
                plugins.put(command, plugin);
                al.add(info);
            } catch (InstantiationException ex) {
                Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchMethodError ex) {
                Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return al;
    }

    @Override
    public ArrayList<CodeTablesEntry> getEntriesWithID(String tablename) throws NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getInputUnitManager().getEntriesWithID(tablename);
    }

    @Override
    public boolean runPlugin(String command) {
        IPlugin plugin = plugins.get(command);
        if (plugin.getPluginInformation().getVersion() < StartOssoBook.PLUGIN_VERSION) {
            if (PluginOutDated.display(mainFrame, plugin.getPluginInformation().getName()) == PluginOutDated.CANCEL) {
                return false;
            }
        }
        plugin.initialize(this);
        return true;
    }

    @Override
    public void deactivatePlugin(String command) {
        plugins.get(command).deactivate();
    }

    @Override
    public ArrayList<CodeTablesEntry> getEntriesWithID(String tablename, int id) throws NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getInputUnitManager().getEntriesWithID(tablename, id);
    }

    @Override
    public ArrayList<String> getAvailableExportEntries() throws StatementNotExecutedException {
        ArrayList<String> names = new ArrayList<String>();
        names.add(IInputUnitManager.USER_ID);
        names.add(IInputUnitManager.EXCAVATIONNR);
        names.add(IInputUnitManager.ARCHAEOLOGICALUNIT);
        names.add(IInputUnitManager.FEATURENAME);
        names.add(IInputUnitManager.FEATURENUMBER);
        names.add(IInputUnitManager.FEATURENAMEDETAILED);
        names.add(IInputUnitManager.VERTICALSTRATIGRAPHY1);
        names.add(IInputUnitManager.VERTICALSTRATIGRAPHY2);
        names.add(IInputUnitManager.VERTICALSTRATIGRAPHY3);
        names.add(IInputUnitManager.HORIZONTALSTRATIGRAPHY1);
        names.add(IInputUnitManager.HORIZONTALSTRATIGRAPHY2);
        names.add(IInputUnitManager.COORDX);
        names.add(IInputUnitManager.COORDY);
        names.add(IInputUnitManager.COORDZ);
        names.add(IInputUnitManager.ADDINFOARCH);
        names.add(IInputUnitManager.FEATURENOTE);
        names.add(IInputUnitManager.DATE);
        names.add(IInputUnitManager.DATENOTE);
        names.add(IInputUnitManager.AGEDETERMINATIONPERIOD);
        names.add(IInputUnitManager.ABSOLUTEDATING);
        names.add(IInputUnitManager.RELATIVEDATING);
        names.add(IInputUnitManager.EXCAVATIONMETHOD);
        names.add(IInputUnitManager.MESHSIZE);
        names.add(IInputUnitManager.VOLUME);
        names.add(CSVExport.ANIMAL);
        names.add(IInputUnitManager.CF);
        names.add(IInputUnitManager.SKELETON_ELEMENT);
        names.add(IInputUnitManager.FUSED);
        names.add(IInputUnitManager.BONEELEMENT);
        names.add(IInputUnitManager.NUMBER);
        names.add(IInputUnitManager.WEIGHT);
        names.add(IInputUnitManager.MINIMUM_NUMBER_OF_ELEMENTS);
        names.add(IInputUnitManager.AGEGROUPDESCRIPT);
        names.add(IInputUnitManager.WEARSTAGE2);
        names.add(IInputUnitManager.WEARSTAGE1);
        names.add(IInputUnitManager.AGE1BASEL);
        names.add(IInputUnitManager.AGE2BASEL);
        names.add(IInputUnitManager.BONEFUSION);
        names.add(IInputUnitManager.SEX);
        names.add(IInputUnitManager.BODYSIDE);
        names.add(IInputUnitManager.PATHOLOGY);
        names.add(IInputUnitManager.INDIVIDUALNO);
        names.add(IInputUnitManager.INDIVIDALNUMBERNOTSURE);
        names.add(IInputUnitManager.OBJECTNOTE);
        names.add(IInputUnitManager.MEASUREMENT_NUMBER);
        names.add(IInputUnitManager.MEASUREMENTSNOTE);
        names.add(IInputUnitManager.ESTIMATED_SIZE);
        names.add(IInputUnitManager.ESTIMATED_LENGTH_MIN);
        names.add(IInputUnitManager.ESTIMATED_LENGTH_MAX);
        names.add(IInputUnitManager.SEASON);
        names.add(IInputUnitManager.COUNTANNUALRINGFISH);
        names.add(IInputUnitManager.MUMMIFIED);
        names.add(IInputUnitManager.DNAANALYSIS);
        names.add(IInputUnitManager.ISOTOPANALYSIS);
        names.add(IInputUnitManager.FRACTUREEDGE);
        names.add(IInputUnitManager.FRACTUREEDGE2);
        names.add(IInputUnitManager.SURFACEPRESERVATION);
        names.add(IInputUnitManager.SURFACEPRESERVATION2);
        names.add(IInputUnitManager.BUTCHERINGMARK1);
        names.add(IInputUnitManager.BUTCHERINGMARK2);
        names.add(IInputUnitManager.GNAWING);
        names.add(IInputUnitManager.DIGESTED);
        names.add(IInputUnitManager.TRACESOFBURNING);
        names.add(IInputUnitManager.DECOLOURATIONPATINA);
        names.add(IInputUnitManager.ENCRUSTATION);
        names.add(IInputUnitManager.ROOTEDGING);
        names.add(IInputUnitManager.FATTYGLOSS);
        names.add(IInputUnitManager.WATERLOGGEDDEPOSIT);
        names.add(IInputUnitManager.TAPHONOMYNOTE);
        names.add(IInputUnitManager.INVENTORYNO);
        names.add(IInputUnitManager.ARTEFACTNOTE);
        names.add(IInputUnitManager.SAMPLE_NR_14C);
        names.add(IInputUnitManager.M_14C_BP);
        names.add(IInputUnitManager.M_14C_CAL_BC);
        names.add(IInputUnitManager.M_2_SIGMA_14C);
        names.add(IInputUnitManager.SAMPLE_NR_ISO);
        names.add(IInputUnitManager.COLLAGEN_CN);
        names.add(IInputUnitManager.M_2H_1H);
        names.add(IInputUnitManager.M_18O_16O);
        names.add(IInputUnitManager.M_13C_12C);
        names.add(IInputUnitManager.M_15N_14N);
        names.add(IInputUnitManager.M_34S_32S);
        names.add(IInputUnitManager.M_87SR_86SR);
        names.add(IInputUnitManager.M_206PB_204PB);
        names.add(IInputUnitManager.M_207PB_204PB);
        names.add(IInputUnitManager.M_208PB_204PB);
        names.add(CSVExport.GENBANK);
        return names;
    }

    @Override
    public String convertAnimalClassIdToString(int animalId) throws EntriesException, NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getAnimalManager().getClassName(animalId);
    }

    @Override
    public int convertAnimalClassStringToId(String animalString) throws EntriesException, NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getAnimalManager().getClassID(animalString);
    }

    @Override
    public String convertAnimalIdToString(int animalId) throws EntriesException, NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getAnimalManager().getAnimalName(animalId);
    }

    @Override
    public int convertAnimalStringToId(String animalString) throws EntriesException, NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getAnimalManager().getAnimalCode(animalString);
    }

    @Override
    public String getAnimalClass(int animalId) throws EntriesException, NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getAnimalManager().getAnimalClass(animalId);
    }

    @Override
    public ArrayList<String> getAllAnimals(int animalClassId) throws StatementNotExecutedException, NotConnectedException, EntriesException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getAnimalManager().getAllAnimals(animalClassId);
    }

    @Override
    public ArrayList<String> getAllAnimalNames() throws StatementNotExecutedException, NotConnectedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getAnimalManager().getAllAnimalNames(false);
    }

    @Override
    public HashMap<Integer, String> getAllAnimals() throws StatementNotExecutedException, NotConnectedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getAnimalManager().getAllAnimals();
    }

    @Override
    public ArrayList<String> getAllAnimalClasses(boolean arrangeAlphabetically) throws StatementNotExecutedException, NotConnectedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getAnimalManager().getAllAnimalClasses(arrangeAlphabetically);
    }

    @Override
    public HashMap<Integer, String> getAllSkeletonElements() throws StatementNotExecutedException, NotConnectedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getSkeletonManager().getAllSkeleton();
    }

    @Override
    public HashMap<Integer, String> getAllEntrys(String tableName) throws StatementNotExecutedException, NotConnectedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getInputUnitManager().getAllEntrys(tableName);
    }

    public MainFrame getMainFrame() {
        return mainFrame;
    }

    private String getColumnNameWithoutTableName(String tableName) {
        return tableName.substring(tableName.lastIndexOf(".") + 1);
    }

    @Override
    public ArrayList<String> getAllAnimals(boolean arrangeAlphabetically) throws StatementNotExecutedException, NotConnectedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getAnimalManager().getAllAnimalNames(arrangeAlphabetically);
    }

    @Override
    public ArrayList<String> getAllSkeletonElements(boolean arrangeAlphabetically) throws StatementNotExecutedException, NotConnectedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getSkeletonManager().getAllSkeletonNames(arrangeAlphabetically);
    }

    @Override
    public HashMap<Integer, HashMap<Integer, String>> getBoneElements() throws StatementNotExecutedException, NotConnectedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getInputUnitManager().getBoneElements();
    }

    @Override
    public HashMap<Integer, Integer> getBonelementGraphicMapping() throws StatementNotExecutedException, NotConnectedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getSkeletonManager().getBonelementGraphicMapping();
    }

    @Override
    public HashMap<String, ArrayList<String>> getAllConcordances() throws StatementNotExecutedException, NotConnectedException, NotLoadedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        if (project == null) {
            throw new NotLoadedException();
        }
        return query.getConcordanceManager().getAllConcordances(project.getProjectKey());
    }

    public HashMap<String, ArrayList<String>> getAllConcordances(Key project) throws StatementNotExecutedException, NotConnectedException, NotLoadedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getConcordanceManager().getAllConcordances(project);
    }

    @Override
    public HashMap<Integer, String> getUsernameMapping() throws StatementNotExecutedException, NotConnectedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getUserManager().getUsernameMapping();
    }

    @Override
    public Connection getConnection() throws NotConnectedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getUnderlyingConnection();
    }

    @Override
    public String getProjectName(int id, int dbNumber) throws NotConnectedException, StatementNotExecutedException, EntriesException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getProjectManager().getProjectName(id, dbNumber);
    }

    @Override
    public String getDbName() throws NotConnectedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getDbName();
    }

    @Override
    public void addLocalUser(String username, String password) {
        try {
            QueryManager qm = new QueryManager(username, password, ConnectionType.CONNECTION_SYNCHRONIZE);
            if (qm != null) {
                String grant = "GRANT ALL ON " + qm.getDbName() + ".* to '" + username + "'@'localhost' IDENTIFIED BY '" + password + "'";
                QueryManager loc = new QueryManager("root", "", ConnectionType.CONNECTION_LOCAL);
                loc.getUnderlyingConnection().createStatement().execute(grant);
                mainFrame.displayConfirmation(Messages.getString("USER_ADDED"));
            }
        } catch (SQLException ex) {
            Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
            mainFrame.displayError(Messages.getString("ERROR_OCCURED_WHILE_COMMUNICATING_WITH_THE_DATABASE"));
        } catch (NotConnectedException ex) {
            Logger.getLogger(GuiController.class.getName()).log(Level.SEVERE, null, ex);
            mainFrame.displayError(Messages.getString("WRONG_USERNAME_OR_PASSWORD"));
        }
    }

    @Override
    public Project getProject(String projectName) throws NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getProjectManager().getProject(projectName);
    }

    @Override
    public ArrayList<Integer> getWearstage2Entry(Key wearstage2Key) throws NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getWearstage2Manager().getWearstage2Entry(wearstage2Key);
    }

    @Override
    public String getWearStage2Label(ArrayList<Integer> animalId, Integer skeletonElementId, int wearStage2CodeNumber) throws NotConnectedException, StatementNotExecutedException {
        if (animalId == null || animalId.isEmpty() || skeletonElementId == null) {
            return "" + wearStage2CodeNumber;
        }
        if (query == null) {
            throw new NotConnectedException();
        }
        try {
            return query.getInputUnitManager().getWearstage2Label(wearStage2CodeNumber, animalId.get(0), skeletonElementId);
        } catch (EntriesException ex) {
            return "" + wearStage2CodeNumber;
        }
    }

    @Override
    public String getWearStage2Label(Integer animalId, Integer skeletonElementId, int wearStage2CodeNumber) throws NotConnectedException, StatementNotExecutedException {
        ArrayList<Integer> animalList = new ArrayList<Integer>();
        animalList.add(animalId);
        return getWearStage2Label(animalList, skeletonElementId, wearStage2CodeNumber);
    }

    @Override
    public ArrayList<String> getAllSkeletonElementsForAnimalID(int animalID, boolean sorted) throws NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        int label = query.getAnimalManager().getAnimalLabel(animalID);
        ArrayList<Integer> skeletonLabels = query.getMeasurementManager().getLabelsOfCorrespondingSkelletons(label);
        return query.getSkeletonManager().getSkeletonNames(skeletonLabels, sorted);
    }

    @Override
    public boolean isInitialized() {
        return installed;
    }

    @Override
    public ILanguageManager getLanguageManager() throws NotConnectedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getLanguageManager();
    }

    @Override
    public int getUserID() throws NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getUserManager().getCurrentUserId();
    }

    @Override
    public String convertBoneElementIdToString(int skeletonElementId, int boneElementId) throws EntriesException, NotConnectedException, StatementNotExecutedException {
        if (query == null) {
            throw new NotConnectedException();
        }
        return query.getBoneElementManager().getBoneElementName(skeletonElementId, boneElementId);
    }

    @Override
    public void sendErrorMessage(String message) throws EntriesException, StatementNotExecutedException, NotConnectedException, MessagingException {
        if (query == null) {
            throw new NotConnectedException();
        }
        ArrayList<String> recipients = query.getUserManager().getTecMail();
        Mail mail = new Mail(recipients);
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("log/ossobooklog.zip"));
            FileInputStream fis = new FileInputStream("log/ossobook.log");
            ZipEntry entry = new ZipEntry("ossobook.log");
            zos.putNextEntry(entry);
            byte[] buffer = new byte[8192];
            int read = 0;
            while ((read = fis.read(buffer, 0, 1024)) != -1) {
                zos.write(buffer, 0, read);
            }
            zos.closeEntry();
            fis.close();
            zos.close();
            mail.sendErrorMessage(message, new File("log/ossobooklog.zip"), getUserName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void addDatamanager(PluginDatamanager manager) {
        datamanagers.add(manager);
    }

    @Override
    public void removeDatamanager(PluginDatamanager manager) {
        datamanagers.remove(manager);
    }

    private ArrayList<String> getData(String table) {
        ArrayList<String> data = new ArrayList<String>();
        for (PluginDatamanager datamanager : datamanagers) {
            data.addAll(datamanager.getData(table));
        }
        return data;
    }

    @Override
    public void deleteProjectLocaly(Project project) throws NotConnectedException, StatementNotExecutedException, NoRightException {
        checkWriteRights(project);
        if (!isConnectedToLocal()) {
            throw new NoRightException(NoRightException.Rights.NOPROJECTOWNER);
        }
        query.deleteProjectPermanent(project, QueryManager.DELETE_ALL);
    }

    @Override
    public Project getLoadedProject() {
        return project;
    }

    @Override
    public HashMap<Integer, Double> getMeasurementEntries(Key MeasurementKey) throws NotConnectedException, StatementNotExecutedException {
        if (query == null) throw new NotConnectedException();
        return query.getMeasurementManager().getMeasurementMappingData(MeasurementKey);
    }

    @Override
    public ArrayList<String> getAnimalsWithSameLabel(String animalName) throws EntriesException, NotConnectedException, StatementNotExecutedException {
        if (query == null) throw new NotConnectedException();
        return query.getAnimalManager().getAnimalsWithLabel(query.getAnimalManager().getAnimalLabel(query.getAnimalManager().getAnimalCode(animalName)));
    }

    @Override
    public ArrayList<String> getAnimalsWithSameLabel(int animalId) throws EntriesException, NotConnectedException, StatementNotExecutedException {
        if (query == null) throw new NotConnectedException();
        return query.getAnimalManager().getAnimalsWithLabel(query.getAnimalManager().getAnimalLabel(animalId));
    }
}
