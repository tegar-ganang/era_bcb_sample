package com.nhncorp.cubridqa.console.bo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import com.nhncorp.cubridqa.configuration.DefTestDB;
import com.nhncorp.cubridqa.console.Executor;
import com.nhncorp.cubridqa.console.bean.Test;
import com.nhncorp.cubridqa.console.util.CommandUtil;
import com.nhncorp.cubridqa.console.util.ConfigureUtil;
import com.nhncorp.cubridqa.console.util.CubridConnManager;
import com.nhncorp.cubridqa.console.util.CubridConnection;
import com.nhncorp.cubridqa.console.util.CubridUtil;
import com.nhncorp.cubridqa.console.util.FileUtil;
import com.nhncorp.cubridqa.console.util.LogUtil;
import com.nhncorp.cubridqa.console.util.RepositoryPathUtil;
import com.nhncorp.cubridqa.console.util.StringUtil;
import com.nhncorp.cubridqa.console.util.SystemUtil;
import com.nhncorp.cubridqa.console.util.TestUtil;
import com.nhncorp.cubridqa.model.ChooseBuild;
import com.nhncorp.cubridqa.model.ChooseTestCase;
import com.nhncorp.cubridqa.model.Configuration;
import com.nhncorp.cubridqa.model.CreateDb;
import com.nhncorp.cubridqa.model.Parameter;
import com.nhncorp.cubridqa.model.PrePostWork;
import com.nhncorp.cubridqa.model.Schedule;
import com.nhncorp.cubridqa.model.ScheduleReplication;
import com.nhncorp.cubridqa.replication.config.SystemHandle;
import com.nhncorp.cubridqa.result.mail.ResultMailSender;
import com.nhncorp.cubridqa.schedule.ScheduleConstants;
import com.nhncorp.cubridqa.utils.MyDriverManager;
import com.nhncorp.cubridqa.utils.PropertiesUtil;
import com.nhncorp.cubridqa.utils.XstreamHelper;

/**
 * 
 * @ClassName: ScheduleBO
 * @Description: the schedule bo for execute schedule project in linux or window .
 * @date 2009-9-4
 * @version V1.0 Copyright (C) www.nhn.com
 */
public class ScheduleBO extends Executor {

    public static final String DB = "consolescheduledefaultdb2008";

    private String os = SystemUtil.getOS();

    private ConfigureUtil configureUtil;

    private String dbBuildServer;

    private String dbBuildServerUser;

    private String dbBuildServerPassword;

    private String dbBuildServerPath;

    private String caseResultsPath;

    private String currentDbConfFile;

    private String scheduleId;

    private String scheduleType;

    private boolean saveEveryone = true;

    private boolean useMonitor = false;

    public ScheduleBO() {
        this.logId = "ScheduleBO";
        this.setPrintType(Executor.PRINT_LOG);
    }

    private Set filesSet;

    public Set getFilesSet() {
        return filesSet;
    }

    public void setFilesSet(Set filesSet) {
        this.filesSet = filesSet;
    }

    public void runSchedule(String scheduleXml, int runMode) {
        if (null == scheduleXml || scheduleXml.trim().length() == 0) {
            System.out.println("scheduleXml is null!!!");
            return;
        } else {
            scheduleXml = scheduleXml.trim();
            System.out.println("scheduleXml=" + scheduleXml);
        }
        init();
        Schedule schedule = (Schedule) XstreamHelper.fromXml(scheduleXml);
        if (null != schedule.getVersion() && "32bits".equalsIgnoreCase(schedule.getVersion())) {
            TestUtil.OTHER_ANSWERS_32 = "answers32";
        } else {
            TestUtil.OTHER_ANSWERS_32 = "answers";
        }
        List procedureList = schedule.getProcedures();
        if (procedureList == null || procedureList.size() == 0) {
            return;
        }
        scheduleId = schedule.getScheduleName();
        scheduleType = schedule.getScheduleType();
        try {
            for (int i = 0; i < procedureList.size(); i++) {
                Object o = procedureList.get(i);
                if (o == null) {
                    continue;
                }
                if (o instanceof ChooseBuild) {
                    chooseBuild((ChooseBuild) o);
                } else if (o instanceof Configuration) {
                    configure((Configuration) o);
                } else if (o instanceof CreateDb) {
                    CreateDb createDb = (CreateDb) o;
                    createDb(createDb, schedule);
                    String dbUrl = createDb.getDbUrl();
                    Connection connection = MyDriverManager.giveConnection("cubrid.jdbc.driver.CUBRIDDriver", dbUrl, "dba", "");
                    if (connection != null) {
                        DatabaseMetaData metaData = connection.getMetaData();
                        String databaseProductVersion = metaData.getDatabaseProductVersion();
                        StringTokenizer stringTokenizer = new StringTokenizer(databaseProductVersion, ".");
                        String[] strings = new String[stringTokenizer.countTokens()];
                        int m = 0;
                        String dbVersion = "";
                        while (stringTokenizer.hasMoreTokens()) {
                            strings[m] = stringTokenizer.nextToken();
                            m++;
                        }
                        for (int k = 0; k < strings.length - 1; k++) {
                            dbVersion += strings[k] + ".";
                        }
                        dbVersion = dbVersion.substring(0, dbVersion.length() - 1);
                        PropertiesUtil.setValue("dbversion", dbVersion);
                        PropertiesUtil.setValue("dbbuildnumber", strings[strings.length - 1]);
                    }
                } else if (o instanceof ChooseTestCase) {
                    ChooseTestCase ctc = (ChooseTestCase) o;
                    filesSet = ctc.getCheckedItems();
                    runTestCase((ChooseTestCase) o, runMode, schedule.getVersion());
                    String mailContent = "";
                    String category = "function";
                    if ("performance".equalsIgnoreCase(scheduleType)) {
                        mailContent = ResultMailSender.getMailContent(caseResultsPath, false, filesSet, ctc);
                        category = "performance";
                    } else {
                        mailContent = ResultMailSender.getMailContent(caseResultsPath, true, filesSet, ctc);
                        category = "function";
                    }
                    ResultMailSender.sendMail(mailContent, caseResultsPath);
                    ResultScpBO.doScp(caseResultsPath);
                } else if (o instanceof PrePostWork) {
                    prePostWork((PrePostWork) o);
                } else if (o instanceof ScheduleReplication) {
                    scheduleReplication((ScheduleReplication) o);
                }
            }
        } catch (Exception e) {
            LogUtil.log(logId, LogUtil.getExceptionMessage(e));
        }
        LogUtil.log(logId, "*******Schedule Done.");
        System.exit(0);
    }

    /**
	 * @deprecated
	 * @param replication
	 */
    private void scheduleReplication(ScheduleReplication replication) {
    }

    /**
	 * execute the pre-job or post-job of the schedule .
	 * 
	 * @param work
	 */
    private void prePostWork(PrePostWork work) {
        LogUtil.log(logId, "prePostWork:");
        String command = work.getAction();
        if (command == null || command.trim().equals("")) {
            return;
        }
        LogUtil.log(logId, "[prepostwork]:" + command);
        String message = CommandUtil.execute(command, null);
        LogUtil.log(logId, message);
    }

    /**
	 * run the selected case file .
	 * 
	 * @param cases
	 * @param runMode
	 * @param version
	 */
    private void runTestCase(ChooseTestCase cases, int runMode, String version) {
        LogUtil.log(logId, "runTestCase:");
        String cubridJdbcSrc = CubridUtil.getCubridJdbcFile();
        String cubridJdbcTarget = configureUtil.getRepositoryPath() + "/qatool_bin/console/lib/cubrid_jdbc.jar";
        FileUtil.copyFile(cubridJdbcSrc, cubridJdbcTarget);
        try {
            Set<String> fileSet = cases.getFiles();
            if (fileSet == null) {
                return;
            }
            String testPosition = cases.getTestPosition();
            String resultPosition = cases.getResultPosition();
            Object[] files = fileSet.toArray();
            Arrays.sort(files);
            String[] caseFiles = new String[files.length];
            if ("svn position".equalsIgnoreCase(testPosition)) {
                for (int i = 0; i < files.length; i++) {
                    String caseFile = (String) files[i];
                    if (caseFile == null || "".equals(caseFile.trim())) {
                        continue;
                    }
                    CommandUtil.svnUpdate(caseFile, null);
                }
            }
            for (int i = 0; i < files.length; i++) {
                String caseFile = (String) files[i];
                if (caseFile == null || "".equals(caseFile.trim())) {
                    continue;
                }
                caseFile = caseFile + "?db=" + DB;
                caseFiles[i] = caseFile;
            }
            ConsoleBO bo = new ConsoleBO(useMonitor, saveEveryone);
            String testId = TestUtil.getTestId("schedule", scheduleId);
            Test test = new Test(testId);
            test.setRunMode(runMode);
            test.setCases(caseFiles);
            test.setVersion(version);
            if ("performance".equalsIgnoreCase(scheduleType)) {
                test.setType(Test.TYPE_PERFORMANCE);
            }
            bo.runTest(test);
            caseResultsPath = TestUtil.getResultPreDir(test.getTestId());
            if ("svn position".equalsIgnoreCase(resultPosition)) {
                System.out.println("[svn submit results]****");
                String repositoryPath = configureUtil.getRepositoryPath();
                String yearDir = TestUtil.getYearDirName();
                String monthDir = TestUtil.getMonthDirName();
                CommandUtil.svnSubmitFile(repositoryPath + "/result", null);
                CommandUtil.svnSubmitFile(repositoryPath + "/result/" + yearDir, null);
                CommandUtil.svnSubmitFile(repositoryPath + "/result/" + yearDir + "/" + monthDir, null);
                String testResultDir = repositoryPath + "/result/" + yearDir + "/" + monthDir + "/" + test.getTestId();
                CommandUtil.svnSubmitFile(testResultDir, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * create the db that the case needed .
	 * 
	 * @param db
	 */
    private void createDb(CreateDb db, Schedule schedule) {
        String command = db.getScript();
        String dbName = db.getDbName();
        String dbPath = CubridUtil.getCubridDatabasesPath() + "/" + dbName;
        String dbConfFile = RepositoryPathUtil.getScheduleDbPath() + "/" + DB + ".xml";
        String flag = db.getTestDb();
        String charset = db.getCharset();
        if (null == charset || charset.length() == 0) {
            charset = "UTF-8";
        }
        CubridConnManager.setGroupMap(new HashMap<String, Map<String, Map<String, List<CubridConnection>>>>());
        CubridConnManager.setDataBaseDefine(new HashMap<String, Object>());
        if (scheduleType.equalsIgnoreCase("performance")) {
            dbConfFile = RepositoryPathUtil.getPerformance_Db_DestPath() + "/" + DB + ".xml";
        } else if (scheduleType.equalsIgnoreCase("function")) {
            dbConfFile = RepositoryPathUtil.getFunction_Db_DestPath() + "/" + DB + ".xml";
        } else {
            System.out.println("ERROR:please set the schedule type with the correct value!!");
        }
        if (ScheduleConstants.CREATE_NEW_DB.equals(flag)) {
            if (FileUtil.isFileExist(dbPath)) {
                CommandUtil.execute("cub_commdb -S " + dbName, null);
                CommandUtil.execute("cub_admin deletedb " + dbName, null);
                FileUtil.deleteFile(dbPath);
            }
            FileUtil.createDir(dbPath);
            String[] commands = new String[2];
            if (os.startsWith("window")) {
                commands[0] = "cd /d " + dbPath;
                commands[1] = command;
                CommandUtil.getExecuteFile(commands, this);
                CommandUtil.execute(commands, this);
            } else {
                commands[0] = "cd " + dbPath;
                commands[1] = command;
                CommandUtil.execute(commands, this);
            }
            LogUtil.log(logId, "[createdb]:" + command);
            DefTestDB dbConf = new DefTestDB();
            dbConf.setDburl(db.getDbUrl());
            dbConf.setDbuser("dba");
            dbConf.setDbpassword("");
            dbConf.setDbaPwd("");
            dbConf.setName(dbName);
            dbConf.setId(dbName);
            dbConf.setCharSet(charset);
            dbConf.setConnectionType("DriverManager");
            dbConf.setScript("");
            dbConf.setVersion(schedule.getVersion());
            String xml = XstreamHelper.toXml(dbConf);
            FileUtil.writeToFile(dbConfFile, xml);
            return;
        } else if (ScheduleConstants.USE_FUNCTION_DB.equals(flag)) {
            String srcFile = configureUtil.getRepositoryPath() + "/configuration/Function_Db/" + dbName + ".xml";
            FileUtil.copyFile(srcFile, dbConfFile);
            return;
        } else if (ScheduleConstants.USE_PERFORMANCE_DB.equals(flag)) {
            String srcFile = dbPath = configureUtil.getRepositoryPath() + "/configuration/Performance_Db/" + dbName + ".xml";
            FileUtil.copyFile(srcFile, dbConfFile);
            return;
        } else {
            System.out.println("EROR:  flag is null,please check your schedule config.");
            return;
        }
    }

    /**
	 * execute the configure operation.
	 * 
	 * @param configuration
	 */
    private void configure(Configuration configuration) {
        System.out.println("configue:");
        if (configuration == null) {
            return;
        }
        configureDb(configuration);
        configureBroker(configuration);
        configureSystem(configuration);
        configureBrokerEnv(configuration);
    }

    /**
	 * configure the broker server through the configuration.
	 * 
	 * @param configuration
	 */
    private void configureBrokerEnv(Configuration configuration) {
        List<Parameter> parameterList = configuration.getCubridEnvs();
        if (parameterList == null) {
            return;
        }
        String brokerConfFile = CubridUtil.getCubridBrokerConfFile();
        Map brokerConf = parseBrokerConf(brokerConfFile);
        Map brokers = (Map) brokerConf.get("brokers");
        String brokerName = configuration.getId();
        Map<String, String> broker = (Map<String, String>) brokers.get(brokerName);
        if (broker == null) {
            return;
        }
        String envFile = SystemUtil.getUserHomePath() + "/broker_" + brokerName + ".env";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parameterList.size(); i++) {
            Parameter parameter = (Parameter) parameterList.get(i);
            if (parameter == null) {
                continue;
            }
            String name = parameter.getParameter();
            if (name == null) {
                continue;
            }
            String value = StringUtil.nullToEmpty(parameter.getCurrentValue());
            sb.append(name + "    " + value + "\n");
        }
        String content = sb.toString();
        FileUtil.writeToFile(envFile, content);
        broker.put("SOURCE_ENV", envFile);
        saveBrokerConfFile(brokerConf, brokerConfFile);
    }

    /**
	 * parse the configuration and set up system parameter.
	 * 
	 * @param configuration
	 */
    private void configureSystem(Configuration configuration) {
        List<Parameter> parameterList = configuration.getSystemParameters();
        if (parameterList == null) {
            return;
        }
        String cmd = SystemUtil.getEnvSetCmd();
        for (int i = 0; i < parameterList.size(); i++) {
            Parameter parameter = (Parameter) parameterList.get(i);
            if (parameter == null) {
                continue;
            }
            String name = parameter.getParameter();
            if (name == null) {
                continue;
            }
            String value = StringUtil.nullToEmpty(parameter.getCurrentValue());
            envList.add(cmd + " " + name + "=" + value);
        }
    }

    /**
	 * configure the broker configuration file.
	 * 
	 * @param configuration
	 */
    private void configureBroker(Configuration configuration) {
        List<Parameter> parameterList = configuration.getBrokerParameters();
        if (parameterList == null) {
            return;
        }
        String brokerConfFile = CubridUtil.getCubridBrokerConfFile();
        Map brokerConf = parseBrokerConf(brokerConfFile);
        Map brokers = (Map) brokerConf.get("brokers");
        String brokerName = configuration.getBrokerName();
        Map<String, String> broker = new Hashtable<String, String>();
        if (parameterList.size() != 0) {
            brokers.put(brokerName, broker);
            for (int i = 0; i < parameterList.size(); i++) {
                Parameter parameter = (Parameter) parameterList.get(i);
                if (parameter == null) {
                    continue;
                }
                String name = parameter.getParameter();
                if (name == null) {
                    continue;
                }
                String value = StringUtil.nullToEmpty(parameter.getCurrentValue());
                broker.put(name, "=" + value);
            }
            saveBrokerConfFile(brokerConf, brokerConfFile);
        }
    }

    /**
	 * save the broker parameter to broker configuration file .
	 * 
	 * @param brokerConf
	 * @param brokerConfFile
	 */
    private void saveBrokerConfFile(Map brokerConf, String brokerConfFile) {
        List<String> commentList = (List<String>) brokerConf.get("commentList");
        Map brokers = (Map) brokerConf.get("brokers");
        StringBuilder sb = new StringBuilder();
        if (commentList != null) {
            for (int i = 0; i < commentList.size(); i++) {
                String comment = (String) commentList.get(i);
                if (comment != null) {
                    sb.append(comment + System.getProperty("line.separator"));
                }
            }
        }
        Iterator<String> iter = brokers.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            Map oneBroker = (Map) brokers.get(key);
            sb.append("[%" + key + "%]" + System.getProperty("line.separator"));
            Iterator<String> infoIter = oneBroker.keySet().iterator();
            while (infoIter.hasNext()) {
                String name = (String) infoIter.next();
                String value = (String) oneBroker.get(name);
                sb.append(name + "    " + value + System.getProperty("line.separator"));
            }
            sb.append(System.getProperty("line.separator"));
        }
        LogUtil.log(logId, "[configure Broker]:" + brokerConfFile);
        String readFile = FileUtil.readFile(brokerConfFile);
        FileUtil.writeToFile(brokerConfFile, readFile + System.getProperty("line.separator") + sb.toString());
    }

    /**
	 * configure the cubrid database configuration file .
	 * 
	 * @param configuration
	 */
    private void configureDb(Configuration configuration) {
        List<Parameter> parameterList = configuration.getCubridParameters();
        if (parameterList == null) {
            return;
        }
        StringBuilder currentSb = new StringBuilder();
        StringBuilder defaultSb = new StringBuilder();
        for (int i = 0; i < parameterList.size(); i++) {
            Parameter parameter = (Parameter) parameterList.get(i);
            if (parameter == null) {
                continue;
            }
            String name = parameter.getParameter();
            if (name == null) {
                continue;
            }
            String value = StringUtil.nullToEmpty(parameter.getCurrentValue());
            if (value != null) {
                boolean isNumber = true;
                try {
                    Double.parseDouble(value);
                } catch (Exception e) {
                    isNumber = false;
                }
                if (isNumber) {
                } else {
                    if (value.equalsIgnoreCase("null")) {
                        value = "NULL";
                    } else {
                        value = value;
                    }
                }
            } else {
                value = "";
            }
            currentSb.append(name + "=" + value + System.getProperty("line.separator"));
            defaultSb.append(name + "=" + value + System.getProperty("line.separator"));
        }
        String currentConf = currentSb.toString();
        String defaultConf = defaultSb.toString();
        String confFileName = FileUtil.getFileName(CubridUtil.getCubridBrokerConfFile());
        String currentConfFile = SystemUtil.getUserHomePath() + "/" + confFileName;
        FileUtil.writeToFile(currentConfFile, currentConf);
        currentDbConfFile = currentConfFile;
        String defaultConfFile = CubridUtil.getDefaultSqlXConfFile();
        String readFile = FileUtil.readFile(defaultConfFile);
        FileUtil.writeToFile(defaultConfFile, readFile + System.getProperty("line.separator") + defaultConf);
        this.onMessage("[configure /conf/cubrid.conf]:" + defaultConfFile);
    }

    /**
	 * select the different cubrid version to test from remote machine.
	 * 
	 * @param build
	 * @throws Exception
	 */
    private void chooseBuild(ChooseBuild build) {
        LogUtil.log(logId, "chooseBuild:");
        if (build == null) {
            return;
        }
        dbBuildServer = configureUtil.getDbBuildServer();
        dbBuildServerUser = configureUtil.getDbBuildServerUser();
        dbBuildServerPassword = configureUtil.getDbBuildServerPassword();
        dbBuildServerPath = configureUtil.getDbBuildServerPath();
        String version = build.getVersion();
        String buildNumber = build.getBuildNumber();
        String testEngine = build.getTestEngine();
        String preFix = build.getPreFix();
        String postFix = build.getPostFix();
        String extension = build.getExtension();
        boolean needDownload = (!"Use Current Local Version".equalsIgnoreCase(testEngine));
        if (needDownload) {
            if ("max".equalsIgnoreCase(buildNumber)) {
                buildNumber = this.getMaxBuildNumber(dbBuildServer, dbBuildServerUser, dbBuildServerPassword, dbBuildServerPath, version);
            }
            LogUtil.log(logId, "max=" + buildNumber);
            String installFile = preFix + version + "." + buildNumber + postFix + "." + extension;
            String srcFile = dbBuildServerPath + "/" + version + "." + buildNumber + "/drop/" + installFile;
            String retFile = SystemUtil.getUserHomePath() + "/" + installFile;
            LogUtil.log(logId, "[downloadDB]:" + srcFile + ">>" + retFile);
            FileUtil.downloadFile(dbBuildServer, dbBuildServerUser, dbBuildServerPassword, srcFile, retFile);
            System.out.println(installFile);
            System.out.println(installFile);
            if (FileUtil.isFileExist(retFile)) {
                PropertiesUtil.setValue("dbbuildnumber", buildNumber);
                installDb(retFile);
            } else {
                System.out.println("=============================== download file failed ================================");
            }
        }
    }

    /**
	 * stop the cubrid through command .
	 */
    private void stopCubrid() {
        String[] commands = new String[3];
        commands[0] = "cubrid broker stop";
        commands[1] = "cubrid service stop";
        commands[2] = "ps -eaf|grep cub|grep -v grep|grep -v cubridqa|awk '{print $2}'|xargs kill -9";
        CommandUtil.execute(commands, null);
        try {
            Thread.sleep(4000);
        } catch (Exception e) {
        }
    }

    /**
	 * start the cubrid through command .
	 */
    private void startCubrid() {
        String[] commands = new String[2];
        commands[0] = "cubrid service start";
        commands[1] = "cubrid broker start";
        CommandUtil.execute(commands, null);
        try {
            Thread.sleep(4000);
        } catch (Exception e) {
        }
    }

    /**
	 * install the cubrid through installFile in linux or window platform.
	 * 
	 * @param installFile
	 */
    private void installDb(String installFile) {
        if (installFile == null) {
            return;
        }
        LogUtil.log(logId, "[installDB]:" + installFile);
        String cubridFile = installFile;
        int positionSlash = installFile.lastIndexOf("/");
        if (positionSlash != -1) {
            cubridFile = installFile.substring(positionSlash + 1);
        }
        String cubridPath = CubridUtil.getCubridPath();
        stopCubrid();
        if (os.indexOf("window") != -1) {
            String dbListFile = "ordblist.txt";
            String srcFile = CubridUtil.getCubridDatabasesPath() + "/" + dbListFile;
            CommandUtil.execute("rm -rf " + cubridPath, null);
            new File(cubridPath).mkdirs();
            FileUtil.copyFile(srcFile, SystemUtil.getUserHomePath());
            FileUtil.copyFile(installFile, cubridPath);
            StringTokenizer stringTokenizer = new StringTokenizer(cubridPath, "/");
            String cubridWindowsPath = "";
            while (stringTokenizer.hasMoreTokens()) {
                cubridWindowsPath = cubridWindowsPath + stringTokenizer.nextToken() + File.separator;
            }
            String[] commands = new String[2];
            commands[0] = "cd /d " + cubridWindowsPath;
            commands[1] = "unzip -o " + cubridFile;
            CommandUtil.execute(commands, null);
            FileUtil.copyFile(SystemUtil.getUserHomePath() + "/" + dbListFile, CubridUtil.getCubridDatabasesPath());
            FileUtil.deleteFile(SystemUtil.getUserHomePath() + "/" + dbListFile);
            FileUtil.deleteFile(cubridPath + "/" + cubridFile);
        } else {
            String cmd = "expect";
            String shPath = RepositoryPathUtil.getScriptConfPath();
            String command = cmd + " " + shPath + "/installCubrid.sh" + " -installFile  " + installFile + " -userHome " + SystemUtil.getUserHomePath();
            CommandUtil.execute(command, null);
        }
    }

    /**
	 * get the max build number of cubrid in the remote machine .
	 * 
	 * @param host
	 * @param user
	 * @param passwd
	 * @param path
	 * @param version
	 * @return
	 */
    private String getMaxBuildNumber(String host, String user, String passwd, String path, String version) {
        if (host == null || user == null || passwd == null || path == null || version == null) {
            return null;
        }
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        String ret = null;
        String tempFile = "ls.ret";
        String cmd = "expect";
        String shPath = RepositoryPathUtil.getScriptConfPath();
        if (com.nhncorp.cubridqa.utils.FileUtil.isLinux()) {
            String command = cmd + " " + shPath + "/ls.sh" + " -user " + user + " -host " + host + " -password " + passwd + " -path " + path + " -version " + version;
            CommandUtil.execute(command, null);
        } else {
            String command = shPath + "/ls.sh" + " -user " + user + " -host " + host + " -password " + passwd + " -path " + path + " -version " + version;
            SystemHandle.executeWCmdShell(command);
        }
        FileUtil.downloadFile(host, user, passwd, tempFile, tempFile);
        BufferedReader reader = null;
        try {
            File f = new File(tempFile);
            if (!f.exists()) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            String line = reader.readLine();
            while (line != null) {
                int position = line.lastIndexOf(".");
                if (position != -1) {
                    line = line.substring(position + 1);
                    if (!line.trim().equals("")) {
                        ret = line.replaceAll("[^0-9]", "");
                        break;
                    }
                }
                line = reader.readLine();
            }
            reader.close();
            f.delete();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return ret;
    }

    /**
	 * parse the conf file to map .
	 * 
	 * @param file
	 * @return
	 */
    private Map parseBrokerConf(String file) {
        Map map = new Hashtable();
        List<String> commentList = new ArrayList<String>();
        Map brokers = new Hashtable();
        map.put("commentList", commentList);
        map.put("brokers", brokers);
        BufferedReader reader = null;
        try {
            File f = new File(file);
            if (!f.exists()) {
                return map;
            }
            Map<String, String> broker = new Hashtable<String, String>();
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            String line = reader.readLine();
            while (line != null) {
                line = line.trim();
                if (line.startsWith("[%")) {
                    String brokerName = line.substring("[% ".length());
                    brokers.put(brokerName, broker);
                    line = reader.readLine();
                    continue;
                } else if (line.startsWith("[")) {
                    commentList.add(line);
                } else {
                    line = line.replaceAll("[ ]+", " ");
                    String[] parts = line.split("=");
                    if (parts.length > 1) {
                        String key = parts[0].trim();
                        String value = parts[1].trim();
                        broker.put(key, value);
                    }
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        return map;
    }

    /**
	 * 
	 */
    protected void init() {
        this.logId = "ScheduleBO";
        LogUtil.clearLog(logId);
        configureUtil = new ConfigureUtil();
        initEnvs();
    }
}
