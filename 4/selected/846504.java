package org.brainypdm.modules.nagios.parser;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.brainypdm.constants.ErrorCodes;
import org.brainypdm.dto.Host;
import org.brainypdm.dto.PerformanceData;
import org.brainypdm.dto.Service;
import org.brainypdm.dto.ServiceData;
import org.brainypdm.dto.ServiceStatus;
import org.brainypdm.exceptions.BaseException;
import org.brainypdm.modules.commons.configuration.BrainyConfiguration;
import org.brainypdm.modules.commons.log.BrainyLogger;
import org.brainypdm.modules.commons.reflaction.ObjectUtils;
import org.brainypdm.modules.nagios.classdef.NagiosLineInfo;
import org.brainypdm.modules.nagios.classdef.NagiosParserRet;
import org.brainypdm.modules.nagios.classdef.NagiosPluginDef;
import org.brainypdm.modules.nagios.classdef.NagiosStatus;
import org.brainypdm.modules.nagios.classdef.PerformanceDataDef;
import org.brainypdm.modules.nagios.exceptions.NagiosException;
import org.brainypdm.modules.nagios.log.NagiosTestLogger;
import org.brainypdm.modules.nagios.main.NagiosPluginCore;
import org.brainypdm.modules.nagios.store.WriterThread;

/**
 * nagios parser This class parsing a line
 * 
 * @author <a href="mailto:nico@brainypdm.org">Nico Bagari</a>
 * 
 */
public class NagiosParser {

    /**
	 * maximum performance data name length
	 */
    private static final String NAGIOS_PERFORMANCE_DATA_NAME_MAXLENGTH = "nagios.performance.data.maxlength";

    /**
	 * logger
	 */
    public static final BrainyLogger logger = new BrainyLogger(NagiosParser.class);

    /**
	 * test logger
	 */
    public static final NagiosTestLogger testLogger = new NagiosTestLogger();

    /**
	 * line parser
	 */
    private final NagiosLineParser lineParser;

    /**
	 * plugin core
	 */
    private final NagiosPluginCore pluginCore;

    /**
	 * store the discarded line
	 */
    private final WriterThread discardedThread;

    /**
	 * store the line with no performance data found
	 */
    WriterThread noPerfDataFoundThread;

    /**
	 * the alert log
	 */
    WriteAlerter alertLog;

    /**
	 * private constructor
	 * 
	 */
    public NagiosParser(WriterThread aDiscardedThread, WriterThread noPerfDataFoundThreadRef, WriterThread aAlertNagiosLogger) {
        lineParser = NagiosLineParser.getInstance();
        pluginCore = NagiosPluginCore.getInstance();
        discardedThread = aDiscardedThread;
        noPerfDataFoundThread = noPerfDataFoundThreadRef;
        alertLog = new WriteAlerter(aAlertNagiosLogger);
    }

    /**
	 * parse every thing from a info sended by nagios
	 * 
	 * @aNagiosInfo nagios information
	 * @return host parsed from line or null if an error occour
	 * 
	 */
    public NagiosParserRet parse(NagiosLineInfo aNagiosInfo) {
        NagiosLineInfo info = null;
        try {
            logger.debug("Parsing new line ...");
            info = lineParser.parseLine(aNagiosInfo);
            if (info != null && info.getStatus() != null && info.getStatus().equals(NagiosStatus.STATUS_UNKNOWN)) {
                alertLog.writeAlertWarn(new NagiosException(ErrorCodes.CODE_1009), info);
            }
            log("Search plugin list for host name: " + info.getHostName() + " and service name: " + info.getServiceName() + "...", info);
            NagiosPluginDef[] pluginList = pluginCore.getPluginList(info.getHostName(), info.getServiceName());
            if ((pluginList == null) || (pluginList.length == 0)) {
                log("No plugin found.", info);
                discardedThread.writeLine(aNagiosInfo.getOriginalInfo());
                alertLog.writeAlertWarn(new NagiosException(ErrorCodes.CODE_1000), info);
                return null;
            } else {
                if (info.isTestLine()) {
                    testLogger.info("line parsed: " + aNagiosInfo.getOriginalInfo());
                    testLogger.info("Host: " + info.getHostName());
                    testLogger.info("Service: " + info.getServiceName());
                    testLogger.info("Last Check: " + info.getLastCheck());
                    testLogger.info("Status: " + info.getStatus());
                    testLogger.info("check duration: " + info.getCheckDuration());
                }
                HashMap<String, PerformanceDataDef> perfDataFound = new HashMap<String, PerformanceDataDef>();
                for (int i = 0; i < pluginList.length; i++) {
                    NagiosPluginDef myPlugin = pluginList[i];
                    boolean isTest = info.isTestLine() || myPlugin.isTest();
                    boolean storeResult = info.isTestLine() || !myPlugin.isTest();
                    NagiosParserable parser = myPlugin.getInstance();
                    List<PerformanceDataDef> perfDataList = null;
                    if (myPlugin.isParseOutput() && myPlugin.matchOutput(info.getOutput())) {
                        try {
                            log("Parsing output ...", info, myPlugin);
                            perfDataList = parser.parseOutput(info.getOutput());
                            if (perfDataList != null) {
                                if (storeResult) {
                                    analyzePerformanceData(perfDataList, perfDataFound, myPlugin, info);
                                }
                                printPerformancesData(isTest, new ArrayList<PerformanceDataDef>(perfDataList));
                            }
                        } catch (NagiosException ex) {
                            logWarn("Error parsing output: " + ex.toString(), info);
                            alertLog.writeAlertWarn(new NagiosException(ErrorCodes.CODE_1001, ex.toString()), info);
                        }
                    }
                    if (myPlugin.isParsePerfData()) {
                        try {
                            if (perfDataList != null) {
                                perfDataList.clear();
                            }
                            log("Parsing performance data output ...", info);
                            perfDataList = parser.parsePerformanceData(info.getPerfDataOutput());
                            if (perfDataList != null) {
                                if (storeResult) {
                                    analyzePerformanceData(perfDataList, perfDataFound, myPlugin, info);
                                }
                                printPerformancesData(isTest, new ArrayList<PerformanceDataDef>(perfDataList));
                            }
                        } catch (NagiosException ex) {
                            logWarn("Error parsing perfData: " + ex.toString(), info);
                            alertLog.writeAlertWarn(new NagiosException(ErrorCodes.CODE_1002, ex.toString()), info);
                        }
                    }
                }
                Host ret = new Host();
                ret.setName(info.getHostName());
                Service service = new Service();
                service.setName(info.getServiceName());
                ret.addService(service);
                ServiceStatus status = new ServiceStatus();
                status.setCheckDate(new Timestamp(info.getLastCheck().getTime()));
                status.setStatus(info.getStatus().getState());
                status.setVerificationTime(info.getCheckDuration());
                for (PerformanceDataDef perfData : perfDataFound.values()) {
                    logger.debug("found performance data: " + perfData);
                    ServiceData srvData = new ServiceData();
                    srvData.setName(perfData.getName());
                    srvData.setUom(perfData.getUom());
                    PerformanceData pdata = new PerformanceData();
                    pdata.setValue(perfData.getValue());
                    pdata.setMinValue(perfData.getMinValue());
                    pdata.setMaxValue(perfData.getMaxValue());
                    pdata.setWarning(perfData.getWarning());
                    pdata.setCritical(perfData.getCritical());
                    srvData.addPerformanceData(pdata);
                    service.addServiceData(srvData);
                }
                if (perfDataFound.size() == 0) {
                    noPerfDataFoundThread.writeLine(aNagiosInfo.getOriginalInfo());
                    alertLog.writeAlertWarn(new NagiosException(ErrorCodes.CODE_1003), info);
                }
                service.addStatus(status);
                return new NagiosParserRet(ret, info.isTestLine());
            }
        } catch (BaseException ex) {
            logError("Error parsing line: " + ex.toString(), info);
            alertLog.writeAlertError(new NagiosException(ErrorCodes.CODE_1004), info);
            try {
                discardedThread.writeLine(aNagiosInfo.getOriginalInfo());
            } catch (Exception iox) {
                logError("Error writing line to discarded file: ", iox, info);
            }
            return null;
        } catch (Throwable ex) {
            logError("Error parsing line:", ex, info);
            alertLog.writeAlertError(new NagiosException(ErrorCodes.CODE_1004), info);
            try {
                discardedThread.writeLine(aNagiosInfo.getOriginalInfo());
            } catch (Exception iox) {
                logError("Error writing line to discarded file: ", iox, info);
            }
            return null;
        }
    }

    /**
	 * clean the performance data
	 * @param pDataDef
	 */
    private void cleanPerformanceData(PerformanceDataDef pDataDef, NagiosLineInfo info) throws BaseException {
        String name = pDataDef.getName();
        if (name != null) {
            name = name.trim();
            int maxLength = BrainyConfiguration.getInstance().getInt(NAGIOS_PERFORMANCE_DATA_NAME_MAXLENGTH);
            if (name.length() > maxLength) {
                String realName = (String) ObjectUtils.deepCopy(name);
                name = name.substring(0, maxLength);
                alertLog.writeAlertWarn(new NagiosException(ErrorCodes.CODE_1006, realName, name), info);
            }
        }
        pDataDef.setName(name);
    }

    /**
	 * analyze the response of line parse
	 * 
	 * @param perfDataList the list of performance data founded
	 * @param perfDataFound the "cache" of founded performance data
	 * @param myPlugin plugin definition
	 */
    private void analyzePerformanceData(List<PerformanceDataDef> perfDataList, HashMap<String, PerformanceDataDef> perfDataFound, NagiosPluginDef myPlugin, NagiosLineInfo info) throws BaseException {
        if ((perfDataList == null) || (perfDataList.size() == 0)) {
            log("No performance data found", info);
        } else {
            Iterator<PerformanceDataDef> iterator = perfDataList.iterator();
            while (iterator.hasNext()) {
                PerformanceDataDef perfDataTmp = iterator.next();
                if (!perfDataTmp.isValid()) {
                    log("Performance data not valid (no name or UOM found) ... " + perfDataTmp, info);
                } else {
                    cleanPerformanceData(perfDataTmp, info);
                    log("Analize performance data " + perfDataTmp.getName() + " ...", info);
                    if (perfDataFound.containsKey(perfDataTmp.getName())) {
                        log("Discarded performace data becouse it is found before.", info);
                    } else {
                        if (myPlugin.isInIgnoreList(perfDataTmp.getName())) {
                            log("Discarded performace data becouse it match with ignore list", info);
                        } else {
                            log("Adding performance data " + perfDataTmp.getName(), info);
                            perfDataFound.put(perfDataTmp.getName(), perfDataTmp);
                        }
                    }
                }
            }
        }
    }

    /**
	 * write msg on log
	 * 
	 * @param msg
	 */
    private void log(String msg, NagiosLineInfo info, NagiosPluginDef def) {
        logger.debug(msg);
        if (((info != null) && info.isTestLine()) || ((def != null) && def.isTest())) {
            testLogger.info(msg);
        }
    }

    /**
	 * write msg on log
	 * 
	 * @param msg
	 */
    private void log(String msg, NagiosLineInfo info) {
        log(msg, info, null);
    }

    /**
	 * write msg on error log
	 * 
	 * @param msg
	 */
    private void logError(String msg, Throwable ex, NagiosLineInfo info) {
        logger.error(msg, ex);
        if ((info != null) && info.isTestLine()) {
            testLogger.error(msg, ex);
        }
    }

    /**
	 * write msg on error log
	 * 
	 * @param msg
	 */
    private void logError(String msg, NagiosLineInfo info) {
        logger.error(msg);
        if ((info != null) && info.isTestLine()) {
            testLogger.error(msg);
        }
    }

    /**
	 * write msg on error log
	 * 
	 * @param msg
	 */
    private void logWarn(String msg, NagiosLineInfo info) {
        String line = "";
        if (info != null) {
            line = info.getOriginalInfo();
        }
        logger.warn(msg + " - line with error: " + line);
        if ((info != null) && info.isTestLine()) {
            testLogger.error(msg);
        }
    }

    /**
	 * print on test logger the performance data found
	 * 
	 * @param performanceDataList
	 */
    private void printPerformancesData(boolean isTest, ArrayList<PerformanceDataDef> performanceDataList) {
        if (isTest) {
            if (performanceDataList.size() > 0) {
                Iterator<PerformanceDataDef> iterator = performanceDataList.iterator();
                while (iterator.hasNext()) {
                    PerformanceDataDef perfData = iterator.next();
                    String uomLable = perfData.getUom() != null ? perfData.getUom().getLabel() : "notFound";
                    testLogger.info("PerformanceData found: " + perfData.getName() + "=" + perfData.getValue() + uomLable + ";" + perfData.getWarning() + ";" + perfData.getCritical() + ";" + perfData.getMinValue() + ";" + perfData.getMaxValue());
                }
            } else {
                testLogger.info("No performance data found !");
            }
        }
    }
}
