package org.brainypdm.modules.nagios.main;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import org.brainypdm.constants.ErrorCodes;
import org.brainypdm.exceptions.BaseException;
import org.brainypdm.modules.commons.configuration.BrainyConfiguration;
import org.brainypdm.modules.commons.log.BrainyLogger;
import org.brainypdm.modules.commons.log.ServerLogger;
import org.brainypdm.modules.commons.utils.ObjectFIFO;
import org.brainypdm.modules.commons.utils.ResourceInternalManager;
import org.brainypdm.modules.exceptions.ModuleException;
import org.brainypdm.modules.interfaces.ModuleServicable;
import org.brainypdm.modules.interfaces.Resourcable;
import org.brainypdm.modules.nagios.classdef.NagiosLineInfo;
import org.brainypdm.modules.nagios.exceptions.NagiosException;
import org.brainypdm.modules.nagios.listener.ListenerSrv;
import org.brainypdm.modules.nagios.listener.ListenerThread;
import org.brainypdm.modules.nagios.parser.NagiosLineParser;
import org.brainypdm.modules.nagios.parser.ThreadParser;
import org.brainypdm.modules.nagios.store.WriterThread;

/***
 * 
 * Starting nagios module
 * 
 * @author <a href="mailto:nico@brainypdm.org">Nico Bagari</a>
 *
 */
public class NagiosModuleStarter implements Resourcable {

    private static final long serialVersionUID = -1151460467184419433L;

    /**
	 * logger
	 */
    public static final BrainyLogger logger = new BrainyLogger(NagiosModuleStarter.class);

    /**
	 * the nagios module interface
	 */
    private transient NagiosServerInterfaceImpl myInterface;

    /**
	 * the internal resource list
	 */
    private transient ResourceInternalManager resourceList;

    /**
	 * the dispatcher
	 */
    private transient Dispatcher dispatcher;

    /**
	 * true if this instance is alive
	 */
    private boolean alive;

    /**
	 * the thread that store the non parsed line
	 */
    private transient WriterThread discard;

    /**
	 * the thread that store the non line wich no performance data is founded
	 */
    private transient WriterThread noPerfDataFound;

    /**
	 * the alertLog
	 */
    private transient WriterThread alertLog;

    /**
	 * getting the administration interface
	 */
    public synchronized ModuleServicable getInterface() throws ModuleException {
        try {
            if (myInterface == null) {
                myInterface = new NagiosServerInterfaceImpl(this);
            }
            return myInterface;
        } catch (Exception ex) {
            logger.error("Error: ", ex);
            throw new ModuleException(ErrorCodes.CODE_100, ex.getMessage());
        }
    }

    /**
	 * @return true if the module is alive
	 */
    public boolean isAlive() {
        return alive;
    }

    /**
	 * shutdown the module
	 */
    public boolean shutdown() throws ModuleException {
        if (alive) {
            try {
                resourceList.shutdownAll(ResourceInternalManager.SHUTDOWN_LEVEL_WAIT);
            } catch (Throwable ex) {
                logger.error("Problem to shutdown nagios module", ex);
            }
        }
        alive = false;
        return true;
    }

    /**
	 * startup of module
	 */
    public boolean startup() throws ModuleException {
        try {
            alive = false;
            NagiosPluginLoader loader = NagiosPluginLoader.getInstance();
            loader.load();
            resourceList = new ResourceInternalManager();
            NagiosPluginCore pluginCore = NagiosPluginCore.getInstance();
            pluginCore.loadDef();
            NagiosLineParser lineParser = NagiosLineParser.getInstance();
            lineParser.setUp();
            BrainyConfiguration configuration = BrainyConfiguration.getInstance();
            String aFileName = createDataNagiosDir(configuration);
            String bufferWriterName = aFileName + File.separatorChar + configuration.getString("nagios.buffer.writer.logname");
            WriterThread writer = createBufferWriter(bufferWriterName, configuration);
            resourceList.addAndStartResource(writer);
            String bufferDiscardName = aFileName + File.separatorChar + configuration.getString("nagios.discard.writer.logname");
            discard = createDiscardWriter(bufferDiscardName, configuration);
            resourceList.addAndStartResource(discard);
            String noPerfdataFoundName = aFileName + File.separatorChar + configuration.getString("nagios.discard.noperfdatafound.logname");
            noPerfDataFound = createDiscardWriter(noPerfdataFoundName, configuration);
            resourceList.addAndStartResource(noPerfDataFound);
            ObjectFIFO<NagiosLineInfo> bufferDispatcher = new ObjectFIFO<NagiosLineInfo>();
            dispatcher = new Dispatcher(writer, bufferDispatcher);
            resourceList.addAndStartResource(dispatcher);
            String alertNagiosLog = configuration.getString("nagios.alert.writer.dirname") + File.separatorChar + configuration.getString("nagios.alert.writer.logname");
            alertLog = createAlertWriter(alertNagiosLog, configuration);
            resourceList.addAndStartResource(alertLog);
            int numberOfParserThread = configuration.getInt("nagios.plugin.parserThread.number");
            String threadParserName = configuration.getString("nagios.plugin.parserThread.name");
            long sleepTime = configuration.getLong("nagios.plugin.parserThread.sleepTime");
            for (int i = 0; i < numberOfParserThread; i++) {
                ThreadParser threadParser = new ThreadParser(threadParserName + "-" + i, discard, noPerfDataFound, sleepTime, bufferDispatcher, alertLog);
                resourceList.addAndStartResource(threadParser);
            }
            int numberOfListener = configuration.getInt("nagios.plugin.listenerThread.number");
            String threadListenerName = configuration.getString("nagios.plugin.listenerThread.name");
            long sleepThreadTime = configuration.getLong("nagios.plugin.listenerThread.sleepTime");
            String charset = configuration.getString("nagios.listener.charset");
            ObjectFIFO<Socket> queue = new ObjectFIFO<Socket>();
            for (int i = 0; i < numberOfListener; i++) {
                String name = threadListenerName + "-" + i;
                logger.debug("Starting listener: " + name);
                ListenerThread listenerThread = new ListenerThread(name, charset, dispatcher, sleepThreadTime, queue);
                resourceList.addAndStartResource(listenerThread);
            }
            ListenerSrv srv = new ListenerSrv(queue);
            resourceList.addAndStartResource(srv);
            alive = true;
            return alive;
        } catch (Throwable ex) {
            logger.fatal("Module not started:", ex);
            return false;
        }
    }

    /**
	 * create the socketClientList thread
	 * @param aFileName the file name
	 * @param configuration configuration parameter
	 * @return the thread writer
	 * @throws IOException
	 * @throws NagiosException
	 */
    private WriterThread createBufferWriter(String aFileName, BrainyConfiguration configuration) throws IOException, NagiosException, BaseException {
        long cicle = configuration.getLong("nagios.buffer.writer.sleep");
        String threadName = configuration.getString("nagios.buffer.thread.name");
        long rollingSize = configuration.getLong("nagios.buffer.thread.rolling.size") * (long) 1024;
        boolean renameAtStartup = configuration.getBoolean("nagios.buffer.thread.rename.file.atstartup");
        boolean isTransactional = configuration.getBoolean("nagios.buffer.writer.istransactional");
        ServerLogger.info("NagiosModule BufferWriter isTransactional: " + isTransactional);
        logger.info("NagiosModule BufferWriter isTransactional: " + isTransactional);
        return new WriterThread(aFileName, threadName, cicle, rollingSize, renameAtStartup, isTransactional);
    }

    /**
	 * create the discard thread
	 * @param aFileName
	 * @param configuration
	 * @return
	 * @throws IOException
	 * @throws NagiosException
	 */
    private WriterThread createDiscardWriter(String aFileName, BrainyConfiguration configuration) throws IOException, NagiosException, BaseException {
        long cicle = configuration.getLong("nagios.discard.writer.sleep");
        String threadName = configuration.getString("nagios.discard.thread.name");
        long rollingSize = configuration.getLong("nagios.discard.thread.rolling.size") * (long) 1024;
        boolean renameAtStartup = configuration.getBoolean("nagios.discard.thread.rename.file.atstartup");
        return new WriterThread(aFileName, threadName, cicle, rollingSize, renameAtStartup);
    }

    private WriterThread createAlertWriter(String aFileName, BrainyConfiguration configuration) throws IOException, NagiosException, BaseException {
        long cicle = configuration.getLong("nagios.alert.writer.sleep");
        String threadName = configuration.getString("nagios.alert.thread.name");
        long rollingSize = configuration.getLong("nagios.alert.thread.rolling.size") * (long) 1024;
        boolean renameAtStartup = configuration.getBoolean("nagios.alert.thread.rename.file.atstartup");
        return new WriterThread(aFileName, threadName, cicle, rollingSize, renameAtStartup);
    }

    /**
	 * create (if not exist) the nagios directory
	 * @return nagios directory name
	 * @throws NagiosException
	 */
    private String createDataNagiosDir(BrainyConfiguration configuration) throws NagiosException, BaseException {
        String dataNagiosDir = configuration.getString("nagios.plugin.dataDir.name");
        File dir = new File(dataNagiosDir);
        if (!dir.exists()) {
            boolean created = dir.mkdir();
            if (!created) {
                throw new NagiosException(ErrorCodes.CODE_802, dataNagiosDir);
            }
        }
        return dataNagiosDir;
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public WriterThread getDiscard() {
        return discard;
    }

    public WriterThread getNoPerfDataFound() {
        return noPerfDataFound;
    }

    public WriterThread getAlertLog() {
        return alertLog;
    }
}
