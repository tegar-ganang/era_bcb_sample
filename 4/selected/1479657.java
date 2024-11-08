package ddbserver.server;

import ddbserver.common.ExceptionHandler;
import ddbserver.common.GDDNode;
import ddbserver.common.ResultType;
import ddbserver.common.SQLResult;
import ddbserver.common.Site;
import ddbserver.common.Table;
import ddbserver.connections.GDDManager;
import ddbserver.connections.SiteManager;
import ddbserver.connections.SocketConnector;
import ddbserver.connections.TableManager;
import ddbserver.constant.Constant;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author qixiao
 */
public class PaserUtil {

    private SiteManager siteManager;

    private GDDManager gddManager;

    private TableManager tableManager;

    private HashMap<String, GDDNode> GDD;

    private List<SQLResult> commandsList;

    private String localSiteName = new String();

    private DatabaseUtil dbUtil;

    public PaserUtil(DatabaseUtil dbUtil, String localSiteName) {
        this.dbUtil = dbUtil;
        this.localSiteName = localSiteName;
        this.siteManager = SiteManager.getInstance();
        this.gddManager = GDDManager.getInstance();
        this.tableManager = TableManager.getInstance();
        this.GDD = new HashMap<String, GDDNode>();
        this.commandsList = new ArrayList<SQLResult>();
    }

    private void defineSite(String command) {
        String[] siteInfo = command.split("\\s", 2);
        siteManager.putSite(siteInfo[1]);
    }

    private String createDatabase(String command) {
        String[] dbInfo = command.split("\\s");
        String sql = "create database " + dbInfo[1];
        return sql;
    }

    private void createTable(String command) {
        String[] tableInfo = command.split("\\s", 3);
        String tableName = tableInfo[1].trim();
        Table table = new Table(tableName);
        String tempColsTypes = tableInfo[2].trim();
        tempColsTypes = tempColsTypes.substring(1, tempColsTypes.length() - 1);
        String[] colsType = tempColsTypes.split(",");
        String[] temp;
        for (int i = 0; i < colsType.length; i++) {
            temp = colsType[i].trim().split("\\s");
            table.insertCols(temp[0].trim(), temp[1].trim());
            if (temp.length == 3) {
                table.setKey(temp[0].trim());
            }
        }
        tableManager.addTable(tableName, table);
    }

    public void fragment(String command) {
        String[] splitCommands = command.split("into");
        String tableName = splitCommands[0].trim().split("\\s")[1].trim();
        String fragmentType = splitCommands[0].trim().split("\\s")[2].trim();
        GDDNode parent = new GDDNode();
        parent.setIsLeaf(false);
        String[] conditions = splitCommands[1].trim().split(",");
        List<String> predicate = new ArrayList<String>();
        if (fragmentType.equals(Constant.DDB_FRAGMENT_HORIZONATALLY)) {
            parent.setFragmentType(Constant.DDB_FRAGMENT_HORIZONATALLY);
            for (int i = 0; i < conditions.length; i++) {
                predicate.add(conditions[i]);
            }
            parent.setPredicate(predicate);
        } else if (fragmentType.equals(Constant.DDB_FRAGMENT_VERTICALLLY)) {
            parent.setFragmentType(Constant.DDB_FRAGMENT_VERTICALLLY);
            for (int i = 0; i < conditions.length; i++) {
                predicate.add((conditions[i].trim()).substring(1, conditions[i].indexOf(")")));
            }
            parent.setPredicate(predicate);
        }
        GDD.put(tableName, parent);
    }

    public void allocate(String command) {
        String[] splitCommands = command.split("\\s");
        String fragmentTableName = splitCommands[1].trim();
        String rootTableName = fragmentTableName.split("\\.")[0];
        String siteName = splitCommands[3].trim();
        GDDNode rootNode = GDD.get(rootTableName);
        Site site = siteManager.getSiteByName(siteName);
        GDDNode leafNode = new GDDNode();
        leafNode.setIsLeaf(true);
        leafNode.setLocation(site);
        if (fragmentTableName.split("\\.").length == 2) {
            List<GDDNode> sonsList = rootNode.getSons();
            sonsList.add(leafNode);
            rootNode.setSons(sonsList);
        } else if (fragmentTableName.split("\\.").length == 3) {
            String tempName = rootTableName + "." + fragmentTableName.split("\\.")[1];
            GDDNode parent = GDD.get(tempName);
            List<GDDNode> sonsList = parent.getSons();
            sonsList.add(leafNode);
            parent.setSons(sonsList);
            if (parent.getSons().size() == parent.getPredicate().size()) {
                List<GDDNode> levelTwoNodeList = rootNode.getSons();
                levelTwoNodeList.add(parent);
                rootNode.setSons(levelTwoNodeList);
            }
        }
    }

    public void importData(String command, String commandType) {
        String fileName = command.split("\\s", 2)[1].trim();
        HashMap<String, String> tableFileMap = new HashMap<String, String>();
        Set<String> tableNameSet = tableManager.getAllTableName();
        tableFileMap = prepareDataFile(fileName);
        importDataToTempTable(tableNameSet, tableFileMap);
        if (commandType == null || commandType.equals("Temp")) {
            importDataToDDBTable(tableNameSet, Constant.DDB_RECORD_BATCH);
        }
    }

    public void initialDDB() {
        this.gddManager.setGDD(GDD);
        this.sendInitialInformation(siteManager.getSites());
        createLocalTempTables();
        createDDBTables();
    }

    public List<SQLResult> insertSingleRecord(String sql, String tableName) {
        this.emptyCommandsList();
        if (sql != null) {
            dbUtil.execute(new SQLResult(siteManager.getMyname(), sql, Constant.LOCAL_COMMAND_NONRESULT));
        }
        Set<String> tableNameSet = new HashSet<String>();
        tableNameSet.add(tableName);
        this.importDataToDDBTable(tableNameSet, Constant.DDB_RECORD_SINGLE);
        return commandsList;
    }

    public List<SQLResult> deleteSingleRecord(String sql, List<String> tableList, List<String> conditionList) {
        this.emptyCommandsList();
        if (sql != null) {
            dbUtil.execute(new SQLResult(siteManager.getMyname(), sql, Constant.LOCAL_COMMAND_NONRESULT));
        }
        for (String tableName : tableList) {
            GDDNode rootNode = gddManager.getGDDNodeByTableName(tableName);
            this.deletSingleRecord(rootNode, null, rootNode.getFragmentType(), tableName, conditionList);
        }
        return commandsList;
    }

    public List<SQLResult> paserScript(SocketConnector socketConnector) {
        int commandNumber = Integer.valueOf(socketConnector.read());
        String command = new String();
        for (int i = 0; i < commandNumber; i++) {
            command = socketConnector.read();
            if (command.startsWith(Constant.DDB_DEFINESITE)) {
                defineSite(command);
            } else if (command.startsWith(Constant.DDB_CREATEDB)) {
                createDatabase(command);
            } else if (command.startsWith(Constant.DDB_CREATETABLE)) {
                createTable(command);
            } else if (command.startsWith(Constant.DDB_FRAGMENT)) {
                fragment(command);
            } else if (command.startsWith(Constant.DDB_ALLOCATE)) {
                allocate(command);
            } else if (command.startsWith(Constant.DDB_INITIALDATABASE)) {
                emptyCommandsList();
                initialDDB();
            } else if (command.startsWith(Constant.DDB_IMPORT)) {
                emptyCommandsList();
                importData(command, null);
            } else {
            }
        }
        return this.commandsList;
    }

    public void createLocalTempTables() {
        Set<String> tableNameSet = tableManager.getAllTableName();
        Set<String> colSet;
        Table table;
        String createTableSql = new String();
        for (String tableName : tableNameSet) {
            String column = new String();
            table = tableManager.getTable(tableName);
            colSet = table.getCols();
            int count = 0;
            for (String col : colSet) {
                column = column + col + " " + table.getType(col);
                if (col.equalsIgnoreCase(table.getKey()) || col.equals("id")) {
                    column = column + " primary key";
                }
                count++;
                if (count != table.getSize()) {
                    column = column + ",";
                }
            }
            createTableSql = "create table temp_" + tableName + " (" + column + ")";
            for (String siteName : siteManager.getSites()) {
                Site site = siteManager.getSite(siteName);
                SQLResult sr;
                if (site.getName().equals(localSiteName)) {
                    sr = new SQLResult(localSiteName, createTableSql, Constant.LOCAL_COMMAND_NONRESULT);
                } else {
                    if (site.getIP().equals(siteManager.getSiteByName(localSiteName).getIP())) {
                        continue;
                    }
                    sr = new SQLResult(site.getName(), createTableSql, Constant.REMOTE_COMMAND_NONRESULT);
                }
                commandsList.add(sr);
            }
        }
    }

    public void createDDBTables() {
        Set<String> tableNameSet = tableManager.getAllTableName();
        for (String tableName : tableNameSet) {
            GDDNode gddNode = gddManager.getGDDNodeByTableName(tableName);
            if (gddNode.getFragmentType().equals(Constant.DDB_FRAGMENT_HORIZONATALLY)) {
                fragmentForCreateTable(gddNode, Constant.DDB_PREDICATE_ALLCOLUMNS, Constant.DDB_FRAGMENT_HORIZONATALLY, tableName);
            } else {
                fragmentForCreateTable(gddNode, null, Constant.DDB_FRAGMENT_VERTICALLLY, tableName);
            }
        }
    }

    public void fragmentForCreateTable(GDDNode gddNode, String predicate, String fragmentType, String tableName) {
        SQLResult sqlResult;
        if (gddNode.isIsLeaf()) {
            String sql = new String();
            if (fragmentType.equals(Constant.DDB_FRAGMENT_HORIZONATALLY)) {
                Table hTable = tableManager.getTable(tableName);
                String hTableName = gddNode.getLocation().getName() + "_" + tableName;
                if (predicate.equalsIgnoreCase(Constant.DDB_PREDICATE_ALLCOLUMNS)) {
                    sql = hTable.generateCreateTableSQL(hTableName);
                } else {
                    String[] cols = predicate.split("\\s");
                    sql = hTable.generateCreateTableSQL(cols, hTableName);
                }
            } else if (fragmentType.equals(Constant.DDB_FRAGMENT_VERTICALLLY)) {
                Table vTable = tableManager.getTable(tableName);
                String[] vCols = predicate.split("\\s");
                String vTableName = gddNode.getLocation().getName() + "_" + tableName;
                sql = vTable.generateCreateTableSQL(vCols, vTableName);
            }
            if (gddNode.getLocation().getName().equalsIgnoreCase(localSiteName)) {
                sqlResult = new SQLResult(gddNode.getLocation().getName(), sql, Constant.LOCAL_COMMAND_NONRESULT);
            } else {
                sqlResult = new SQLResult(gddNode.getLocation().getName(), sql, Constant.REMOTE_COMMAND_NONRESULT);
            }
            commandsList.add(sqlResult);
        } else {
            List<GDDNode> sons = gddNode.getSons();
            List<String> predicateList = gddNode.getPredicate();
            for (int i = 0; i < sons.size(); i++) {
                if (gddNode.getFragmentType().equalsIgnoreCase(Constant.DDB_FRAGMENT_HORIZONATALLY)) {
                    fragmentForCreateTable(sons.get(i), predicate, fragmentType, tableName);
                } else {
                    fragmentForCreateTable(sons.get(i), predicateList.get(i), Constant.DDB_FRAGMENT_VERTICALLLY, tableName);
                }
            }
        }
    }

    public HashMap<String, String> prepareDataFile(String fileName) {
        File originalDataFile = new File(fileName);
        String tableName = new String();
        String line = new String();
        BufferedReader br;
        BufferedWriter bw;
        long recordsNumber;
        File tempDataFile;
        HashMap<String, String> tableFileMap = new HashMap<String, String>();
        String filePath = new String();
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(originalDataFile)));
            if (br.ready()) {
                if ((line = br.readLine()) != null) {
                    do {
                        tableName = "temp_" + getTableName(line);
                        tempDataFile = new File("./Data/" + tableName + ".txt");
                        tempDataFile.createNewFile();
                        tableFileMap.put(tableName, tempDataFile.getCanonicalPath());
                        recordsNumber = getRecordNumber(line);
                        bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempDataFile)));
                        for (long i = 0; i < recordsNumber; i++) {
                            bw.write(br.readLine() + "\r\n");
                        }
                        bw.close();
                    } while ((line = br.readLine()) != null);
                }
                br.close();
                return tableFileMap;
            }
        } catch (Exception e) {
            ExceptionHandler.handleExcptin(e);
        }
        return null;
    }

    public void importDataToTempTable(Set<String> tableNameSet, HashMap<String, String> tableFileMap) {
        String sql = new String();
        SQLResult sr;
        for (String tableName : tableNameSet) {
            Table tempTable = tableManager.getTable(tableName);
            tableName = "temp_" + tableName;
            for (String tempTableName : tableFileMap.keySet()) {
                if (tempTableName.equals(tableName)) {
                    sql = tempTable.generateImportDataSQL(tableFileMap.get(tempTableName));
                }
            }
            sr = new SQLResult("localhost", sql, Constant.LOCAL_COMMAND_NONRESULT);
            dbUtil.execute(sr);
        }
    }

    public void importDataToDDBTable(Set<String> tableNameSet, String recordType) {
        for (String rootTableName : tableNameSet) {
            GDDNode rootGDDNode = gddManager.getGDDNodeByTableName(rootTableName);
            this.importDataToDDBSite(rootGDDNode, null, rootGDDNode.getFragmentType(), rootTableName, recordType);
        }
    }

    public String getTableName(String line) {
        String tableName = line.split("\\(")[0].trim();
        return tableName;
    }

    public int getRecordNumber(String line) {
        Integer number = new Integer(line.split("\\)")[1].trim());
        return number.intValue();
    }

    public void importDataToDDBSite(GDDNode gddNode, String predicate, String fragmentType, String tableName, String recordType) {
        SQLResult sqlResult;
        if (gddNode.isIsLeaf()) {
            String sql = new String();
            if (!predicate.startsWith("select")) {
                predicate = "select * " + predicate;
            }
            ResultSet rs = dbUtil.getQueryResult(predicate);
            ResultType rt = new ResultType();
            rt.setResutlVector(rs);
            if (recordType.equals(Constant.DDB_RECORD_SINGLE)) {
                sql = "insert into " + gddNode.getLocation().getName() + "_" + tableName + " values";
            } else if (recordType.equals(Constant.DDB_RECORD_BATCH)) {
                sql = "insert into " + gddNode.getLocation().getName() + "_" + tableName + " " + predicate;
            }
            if (gddNode.getLocation().getName().equals(localSiteName)) {
                if (recordType.equals(Constant.DDB_RECORD_BATCH)) {
                    sqlResult = new SQLResult(gddNode.getLocation().getName(), sql, Constant.LOCAL_COMMAND_NONRESULT);
                    sqlResult.setResult(rt);
                    commandsList.add(sqlResult);
                } else if (recordType.equals(Constant.DDB_RECORD_SINGLE)) {
                    sqlResult = new SQLResult(gddNode.getLocation().getName(), sql, Constant.LOCAL_COMMAND_RESULET);
                    sqlResult.setResult(rt);
                    commandsList.add(sqlResult);
                }
            } else {
                if (recordType.equals(Constant.DDB_RECORD_BATCH)) {
                    sqlResult = new SQLResult(gddNode.getLocation().getName(), sql, Constant.REMOTE_COMMAND_NONRESULT);
                    sqlResult.setResult(rt);
                    commandsList.add(sqlResult);
                } else if (recordType.equals(Constant.DDB_RECORD_SINGLE)) {
                    sqlResult = new SQLResult(gddNode.getLocation().getName(), sql, Constant.REMOTE_COMMAND_RESULTE);
                    sqlResult.setResult(rt);
                    commandsList.add(sqlResult);
                }
            }
        } else {
            List<GDDNode> sons = gddNode.getSons();
            for (int i = 0; i < sons.size(); i++) {
                String temp = new String();
                temp = predicate;
                if (gddNode.getFragmentType().equals(Constant.DDB_FRAGMENT_HORIZONATALLY)) {
                    if (predicate == null) {
                        predicate = "from temp_" + tableName + " where " + gddNode.getPredicate().get(i);
                    } else {
                        predicate = predicate + " where " + gddNode.getPredicate().get(i);
                    }
                    importDataToDDBSite(sons.get(i), predicate, fragmentType, tableName, recordType);
                    predicate = temp;
                } else if (gddNode.getFragmentType().equals(Constant.DDB_FRAGMENT_VERTICALLLY)) {
                    String[] columns = gddNode.getPredicate().get(i).split("\\s");
                    String predicateCols = new String();
                    for (int j = 0; j < columns.length; j++) {
                        predicateCols = predicateCols + columns[j];
                        if (j != columns.length - 1) {
                            predicateCols = predicateCols + ",";
                        }
                    }
                    if (predicate == null) {
                        predicate = "select " + predicateCols + " from temp_" + tableName + " ";
                    } else {
                        predicate = "select " + predicateCols + " " + predicate;
                    }
                    importDataToDDBSite(sons.get(i), predicate, fragmentType, tableName, recordType);
                    predicate = temp;
                }
            }
        }
    }

    public void deletSingleRecord(GDDNode gddNode, String predicate, String fragmentType, String tableName, List<String> condition) {
        SQLResult sqlResult;
        if (gddNode.isIsLeaf()) {
            String sql = new String();
            for (int i = 0; i < condition.size(); i++) {
                if (i != condition.size() - 1) {
                    sql = sql + condition.get(i) + " and ";
                } else {
                    sql = sql + condition.get(i);
                }
            }
            sql = "delete from " + gddNode.getLocation().getName() + "_" + tableName + " where " + sql;
            if (gddNode.getLocation().getName().equals(localSiteName)) {
                sqlResult = new SQLResult(gddNode.getLocation().getName(), sql, Constant.LOCAL_COMMAND_NONRESULT);
                commandsList.add(sqlResult);
            } else {
                sqlResult = new SQLResult(gddNode.getLocation().getName(), sql, Constant.REMOTE_COMMAND_NONRESULT);
                commandsList.add(sqlResult);
            }
        } else {
            List<GDDNode> sons = gddNode.getSons();
            for (int i = 0; i < sons.size(); i++) {
                String temp = new String();
                temp = predicate;
                if (gddNode.getFragmentType().equals(Constant.DDB_FRAGMENT_HORIZONATALLY)) {
                    deletSingleRecord(sons.get(i), predicate, fragmentType, tableName, condition);
                    predicate = temp;
                } else if (gddNode.getFragmentType().equals(Constant.DDB_FRAGMENT_VERTICALLLY)) {
                    String[] columns = gddNode.getPredicate().get(i).split("\\s");
                    List<String> vCondition = new ArrayList<String>();
                    for (int j = 0; j < columns.length; j++) {
                        for (int l = 0; l < condition.size(); l++) {
                            if (condition.get(l).contains(columns[j])) {
                                vCondition.add(condition.get(l));
                            }
                        }
                    }
                    deletSingleRecord(sons.get(i), predicate, fragmentType, tableName, vCondition);
                    predicate = temp;
                }
            }
        }
    }

    public void compareHCondition(String predicate, List<String> conditions, String tableName) {
        Table table = tableManager.getTable(tableName);
        String[] predicates = predicate.split("and");
        List<String> newConditions = new ArrayList<String>();
        int conditionCount = conditions.size();
        String opregex = "(<=)||(<>)||(=)||(>=)||(>)||(<)";
        HashMap<String, Integer> great = new HashMap<String, Integer>();
        HashMap<String, Integer> greatEqual = new HashMap<String, Integer>();
        HashMap<String, Integer> less = new HashMap<String, Integer>();
        HashMap<String, Integer> lessEqual = new HashMap<String, Integer>();
        HashMap<String, String> equal = new HashMap<String, String>();
        HashMap<String, Integer> conditionGreat = new HashMap<String, Integer>();
        HashMap<String, Integer> conditionGreatEqual = new HashMap<String, Integer>();
        HashMap<String, Integer> conditionLess = new HashMap<String, Integer>();
        HashMap<String, Integer> conditionLessEqual = new HashMap<String, Integer>();
        HashMap<String, String> conditionEqual = new HashMap<String, String>();
        for (int i = 0; i < conditionCount; i++) {
            if (predicate.contains(conditions.get(i).split(opregex)[0].trim())) {
                continue;
            } else {
                newConditions.add(conditions.get(i));
                conditions.remove(i);
                i--;
                conditionCount--;
            }
        }
        for (int i = 0; i < predicates.length; i++) {
            if (predicates[i].contains(">")) {
                if (predicates[i].contains("=")) {
                    greatEqual.put(predicates[i].split(">=")[0].trim(), new Integer(predicates[i].split(">=")[1].trim()));
                } else {
                    great.put(predicates[i].split(">")[0].trim(), new Integer(predicates[i].split(">")[1].trim()));
                }
            } else if (predicates[i].contains("<")) {
                if (predicates[i].contains("=")) {
                    lessEqual.put(predicates[i].split("<=")[0].trim(), new Integer(predicates[i].split("<=")[1].trim()));
                } else {
                    less.put(predicates[i].split("<")[0].trim(), new Integer(predicates[i].split("<")[1].trim()));
                }
            } else if (predicates[i].contains("=")) {
                equal.put(predicates[i].split("=")[0].trim(), predicates[i].split("=")[1].trim());
            }
        }
        for (int j = 0; j < conditions.size(); j++) {
            if (conditions.get(j).contains(">")) {
                if (conditions.get(j).contains("=")) {
                    conditionGreatEqual.put(conditions.get(j).split(">=")[0].trim(), new Integer(conditions.get(j).split(">=")[1].trim()));
                } else {
                    conditionGreat.put(conditions.get(j).split(">")[0].trim(), new Integer(conditions.get(j).split(">")[1].trim()));
                }
            } else if (conditions.get(j).contains("<")) {
                if (conditions.get(j).contains("=")) {
                    conditionLessEqual.put(conditions.get(j).split("<=")[0].trim(), new Integer(conditions.get(j).split("<=")[1].trim()));
                } else {
                    conditionLess.put(conditions.get(j).split("<")[0].trim(), new Integer(conditions.get(j).split("<")[1].trim()));
                }
            } else if (conditions.get(j).contains("=")) {
                conditionEqual.put(conditions.get(j).split("=")[0].trim(), conditions.get(j).split("=")[1].trim());
            }
        }
        for (int i = 0; i < conditions.size(); i++) {
            if (conditionGreat.size() != 0) {
            }
        }
    }

    public void sendInitialInformation(List<String> siteNameSet) {
        String[] fileNames = { "GDD.properties", "tablelist.properties", "sitelist.properties", "Data.txt" };
        for (String siteName : siteNameSet) {
            Site site = siteManager.getSite(siteName);
            if (!site.getName().equals(localSiteName)) {
                for (int i = 0; i < fileNames.length; i++) {
                    if (i != fileNames.length - 1) {
                        site.sendFile(fileNames[i], Constant.DDB_INITIAL_PROPERTIES_FILE);
                    } else {
                        site.sendFile(fileNames[i], Constant.DDB_INITIAL_DATA_FILE);
                    }
                }
            }
        }
    }

    public void receiveInitialInformation(SocketConnector sc) {
        File[] files = { new File("./Data/GDD.properties"), new File("./Data/tablelist.properties"), new File("./Data/sitelist.properties") };
        try {
            for (int i = 0; i < files.length; i++) {
                if (!files[i].exists()) {
                    files[i].createNewFile();
                }
                sc.receiveFile(files[i].getAbsolutePath());
            }
        } catch (Exception e) {
            ExceptionHandler.handleExcptin(e);
        }
    }

    public void emptyCommandsList() {
        if (commandsList.size() == 0) {
            return;
        } else {
            int count = commandsList.size();
            for (int i = 0; i < count; i++) {
                commandsList.remove(0);
            }
        }
    }
}
