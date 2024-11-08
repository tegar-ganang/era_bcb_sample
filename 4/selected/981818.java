package org.gamio.system;

import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.gamio.buffer.BufferFactoryImpl;
import org.gamio.channel.ChannelManager;
import org.gamio.channel.ChannelManagerImpl;
import org.gamio.channel.MsgletImpl;
import org.gamio.client.ClientManager;
import org.gamio.client.ClientManagerImpl;
import org.gamio.comm.DescriptorPool;
import org.gamio.comm.PackerImpl;
import org.gamio.conf.BufferPoolProps;
import org.gamio.conf.ChannelManagerProps;
import org.gamio.conf.ClientProps;
import org.gamio.conf.Configuration;
import org.gamio.conf.DescriptorProps;
import org.gamio.conf.InstProps;
import org.gamio.conf.Loader;
import org.gamio.conf.MsgQueueProps;
import org.gamio.conf.MsgletProps;
import org.gamio.conf.PackerProps;
import org.gamio.conf.ProcessorManagerProps;
import org.gamio.conf.ProcessorProps;
import org.gamio.conf.ServerManagerProps;
import org.gamio.conf.ServerProps;
import org.gamio.conf.ThreadPoolProps;
import org.gamio.logging.Log;
import org.gamio.logging.Logger;
import org.gamio.mq.MessageQueue;
import org.gamio.mq.Router;
import org.gamio.processor.ProcessorManager;
import org.gamio.processor.ProcessorManagerImpl;
import org.gamio.server.ServerManager;
import org.gamio.server.ServerManagerImpl;
import org.gamio.util.Helper;
import org.gamio.work.Worker;
import org.gamio.work.Workshop;
import org.gamio.work.WorkshopImpl;

/**
 * @author Agemo Cui <agemocui@gamio.org>
 * @version $Rev: 23 $ $Date: 2008-10-05 21:00:52 -0400 (Sun, 05 Oct 2008) $
 */
public final class Gamio {

    private static final Log log = Logger.getLogger(Gamio.class);

    private static final String GAMIO_HOME_DIR = "gamio.home.dir";

    private static final String GAMIO_INST_LOCAL_NAME = "gamio.inst.local.name";

    private static final String GAMIO_INST_HOME_DIR = "gamio.inst.home.dir";

    private static final String GAMIO_INST_CONF_DIR = "gamio.inst.conf.dir";

    private static final String GAMIO_INST_DESC_DIR = "gamio.inst.desc.dir";

    private static final String PRODUCT_VERSION = "product.version";

    private static final String VENDOR_NAME = "vendor.name";

    private static final String CONF_FILENAME = "gamio.xml";

    private static final String SCHEMA_FILENAME = "schema/gamio.xsd";

    private GamioState gamioState = GamioStopped.getInstance();

    private Lock lock = new ReentrantLock();

    private abstract static class GamioState {

        public void start(Gamio gamio, Properties properties) throws Exception {
        }

        public void stop(Gamio gamio) {
        }
    }

    private static final class GamioStopped extends GamioState {

        private static GamioStopped gamioStopped = new GamioStopped();

        public static GamioState getInstance() {
            return gamioStopped;
        }

        @Override
        public void start(Gamio gamio, Properties properties) throws Exception {
            log.info("Starting Gamio...");
            log.info("Version: ", properties.getProperty(PRODUCT_VERSION), " (", properties.getProperty(VENDOR_NAME), ")");
            gamio.initEnv(properties);
            Configuration configuration = gamio.loadConfiguration();
            InstProps instProps = configuration.getInstProps();
            log.info("Instance System Name: ", instProps.getName());
            log.info("Instance System ID: ", instProps.getId());
            gamio.initContext(configuration);
            Context context = Context.getInstance();
            context.setTimer(new Timer(true));
            context.getWorkshop().start();
            context.getProcessorManager().startAllProcessors();
            context.getChannelManager().start();
            context.getClientManager().startAllClients();
            context.getServerManager().start();
            context.getServerManager().startAllServers();
            context.getMessageQueue().start();
            gamio.changeState(GamioStarted.getInstance());
            log.info("Gamio[name<", instProps.getName(), ">, id<", instProps.getId(), ">] started");
        }
    }

    private static final class GamioStarted extends GamioState {

        private static GamioStarted gamioStarted = new GamioStarted();

        public static final GamioState getInstance() {
            return gamioStarted;
        }

        @Override
        public void stop(Gamio gamio) {
            log.info("Stopping Gamio...");
            Context context = Context.getInstance();
            context.getTimer().cancel();
            context.getTimer().purge();
            MessageQueue msgQueue = context.getMessageQueue();
            if (msgQueue != null) msgQueue.stop();
            ServerManager serverManager = context.getServerManager();
            if (serverManager != null) serverManager.stop();
            ClientManager clientManager = context.getClientManager();
            if (clientManager != null) clientManager.stopAllClients();
            Workshop workshop = context.getWorkshop();
            if (workshop != null) workshop.stop();
            ChannelManager channelManager = context.getChannelManager();
            if (channelManager != null) channelManager.stop();
            ProcessorManager processorManager = context.getProcessorManager();
            if (processorManager != null) processorManager.stopAllProcessors();
            InstProps serviceProps = context.getInstProps();
            context.clear();
            gamio.changeState(GamioStopped.getInstance());
            log.info("Gamio[name<", serviceProps.getName(), ">, id<", serviceProps.getId(), ">] stopped");
        }
    }

    public void start(Properties properties) throws Exception {
        lock.lock();
        try {
            gamioState.start(this, properties);
        } catch (Exception e) {
            log.error(e, "Failed to start Gamio");
            throw e;
        } finally {
            lock.unlock();
        }
    }

    public void stop() {
        lock.lock();
        try {
            gamioState.stop(this);
        } finally {
            lock.unlock();
        }
    }

    private void initEnv(Properties properties) throws Exception {
        Context context = Context.getInstance();
        String homeDir = properties.getProperty(GAMIO_HOME_DIR);
        if (homeDir == null) throw new Exception(Helper.buildString("Missing Property[", GAMIO_HOME_DIR, "]"));
        String instLocalName = properties.getProperty(GAMIO_INST_LOCAL_NAME);
        String instHomeDir = properties.getProperty(GAMIO_INST_HOME_DIR);
        if (instHomeDir == null) throw new Exception(Helper.buildString("Missing Property[", GAMIO_INST_HOME_DIR, "]"));
        String instConfDir = properties.getProperty(GAMIO_INST_CONF_DIR);
        if (instConfDir == null) {
            instConfDir = Helper.makePath(instHomeDir, "conf");
            properties.setProperty(GAMIO_INST_CONF_DIR, instConfDir);
        }
        String instDescDir = properties.getProperty(GAMIO_INST_DESC_DIR);
        if (instDescDir == null) {
            instDescDir = Helper.makePath(instHomeDir, "descriptor");
            properties.setProperty(GAMIO_INST_DESC_DIR, instDescDir);
        }
        log.info("Gamio Home Dir: ", homeDir);
        if (instLocalName != null) log.info("Instance Local Name: ", instLocalName);
        log.info("Instance Home Dir: ", instHomeDir);
        context.setHomeDir(homeDir);
        context.setInstHomeDir(instHomeDir);
        context.setInstConfDir(instConfDir);
        context.setInstDescDir(instDescDir);
        context.setProperties(properties);
    }

    private Configuration loadConfiguration() throws Exception {
        Context context = Context.getInstance();
        String confFilePath = Helper.makePath(context.getInstConfDir(), CONF_FILENAME);
        log.info("Loading configuration[", confFilePath, "]...");
        Configuration configuration = null;
        try {
            configuration = new Loader().load(confFilePath, SCHEMA_FILENAME);
        } catch (Exception e) {
            log.error(e, "Failed to load configuration[", confFilePath, "]");
            throw e;
        }
        log.info("Configuraion[", confFilePath, "] was loaded");
        return configuration;
    }

    private void initContext(Configuration configuration) throws Exception {
        Context context = Context.getInstance();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        context.setClassLoader(classLoader);
        InstProps instProps = configuration.getInstProps();
        context.setInstProps(instProps);
        initMsglets(context, configuration.getMsgletPropsList());
        initDescriptors(context, configuration.getDescriptorPropsList());
        initPackers(context, configuration.getPackerPropsList());
        MsgQueueProps msgQueueProps = configuration.getMsgQueueProps();
        BufferPoolProps bufferPoolProps = configuration.getBufferPoolProps();
        ThreadPoolProps threadPoolProps = configuration.getThreadPoolProps();
        ChannelManagerProps channelManagerProps = configuration.getChannelManagerProps();
        Worker.setBufSize(channelManagerProps.getChannelByteBufferSize());
        context.setBufferFactory(BufferFactoryImpl.getInstance(bufferPoolProps));
        context.setMessageTable(configuration.getMessageTable().initMessageTable());
        context.setRouter(new Router(instProps.getId(), configuration.getRouteTable()));
        context.setWorkshop(new WorkshopImpl(threadPoolProps));
        initMsgQueue(context, msgQueueProps);
        int cltMaxTimeout = initClientManager(context, configuration.getClientPropsList());
        initProcessorManager(context, configuration.getProcessorManagerProps(), configuration.getProcessorPropsList());
        int srvMaxTimeout = initServerManager(context, configuration.getServerManagerProps(), configuration.getServerPropsList());
        ChannelManager channelManager = new ChannelManagerImpl(channelManagerProps, srvMaxTimeout, cltMaxTimeout);
        context.setChannelManager(channelManager);
    }

    private void initMsgQueue(Context context, MsgQueueProps msgQueueProps) throws Exception {
        MessageQueue messageQueue = null;
        if (msgQueueProps.isJms()) {
            Class<?> clazz = context.getClassLoader().loadClass("org.gamio.mq.JmsProxy");
            messageQueue = (MessageQueue) clazz.getConstructor(MsgQueueProps.class).newInstance(msgQueueProps);
        } else {
            Class<?> clazz = context.getClassLoader().loadClass("org.gamio.mq.MemMsgQueue");
            messageQueue = (MessageQueue) clazz.getConstructor(int.class, int.class).newInstance(msgQueueProps.getCapacity(), msgQueueProps.getInternalMessageCacheSize());
        }
        context.setMessageQueue(messageQueue);
    }

    private void initMsglets(Context context, List<MsgletProps> msgletPropsList) throws Exception {
        for (MsgletProps msgletProps : msgletPropsList) context.putMsglet(msgletProps.getName(), MsgletImpl.create(msgletProps));
    }

    private void initDescriptors(Context context, List<DescriptorProps> descriptorPropsList) throws Exception {
        for (DescriptorProps descriptorProps : descriptorPropsList) context.putDescriptorPool(descriptorProps.getName(), DescriptorPool.create(descriptorProps));
    }

    private void initPackers(Context context, List<PackerProps> packerPropsList) throws Exception {
        for (PackerProps packerProps : packerPropsList) context.putPacker(packerProps.getName(), PackerImpl.create(packerProps));
    }

    private int initClientManager(Context context, List<ClientProps> clientPropsList) {
        ClientManager clientManager = new ClientManagerImpl();
        int cltMaxTimeout = 0;
        for (ClientProps clientProps : clientPropsList) {
            if (cltMaxTimeout < clientProps.getTimeout()) cltMaxTimeout = clientProps.getTimeout();
            if (cltMaxTimeout < clientProps.getKeepAliveTime()) cltMaxTimeout = clientProps.getKeepAliveTime();
            clientManager.registerClient(clientProps);
        }
        context.setClientManager(clientManager);
        return cltMaxTimeout;
    }

    private void initProcessorManager(Context context, ProcessorManagerProps processorManagerProps, List<ProcessorProps> processorPropsList) {
        ProcessorManager processorManager = new ProcessorManagerImpl(processorManagerProps);
        for (ProcessorProps processorProps : processorPropsList) processorManager.registerProcessor(processorProps);
        context.setProcessorManager(processorManager);
    }

    private int initServerManager(Context context, ServerManagerProps serverManagerProps, List<ServerProps> serverPropsList) {
        ServerManager serverManager = new ServerManagerImpl(serverManagerProps);
        int srvMaxTimeout = 0;
        for (ServerProps serverProps : serverPropsList) {
            if (srvMaxTimeout < serverProps.getChannelIdleTimeout()) srvMaxTimeout = serverProps.getChannelIdleTimeout();
            serverManager.registerServer(serverProps);
        }
        context.setServerManager(serverManager);
        return srvMaxTimeout;
    }

    private void changeState(GamioState gamioState) {
        this.gamioState = gamioState;
    }
}
