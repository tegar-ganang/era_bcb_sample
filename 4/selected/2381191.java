package org.gamio.system;

import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TreeMap;
import org.gamio.buffer.BufferFactory;
import org.gamio.channel.ChannelManager;
import org.gamio.channel.Msglet;
import org.gamio.channel.MsgletImpl;
import org.gamio.client.ClientManager;
import org.gamio.comm.DescriptorPool;
import org.gamio.comm.Packer;
import org.gamio.comm.PackerImpl;
import org.gamio.conf.InstProps;
import org.gamio.conf.MessageTable;
import org.gamio.mq.MessageQueue;
import org.gamio.mq.Router;
import org.gamio.processor.ProcessorManager;
import org.gamio.server.ServerManager;
import org.gamio.work.Workshop;

/**
 * @author Agemo Cui <agemocui@gamio.org>
 * @version $Rev: 23 $ $Date: 2008-10-05 21:00:52 -0400 (Sun, 05 Oct 2008) $
 */
public final class Context {

    private Properties properties = null;

    private String homeDir = null;

    private String instHomeDir = null;

    private String instConfDir = null;

    private String instDescDir = null;

    private InstProps instProps = null;

    private BufferFactory bufferFactory = null;

    private Workshop workshop = null;

    private ChannelManager channelManager = null;

    private ServerManager serverManager = null;

    private ProcessorManager processorManager = null;

    private ClientManager clientManager = null;

    private MessageTable messageTable = null;

    private Router router = null;

    private MessageQueue messageQueue = null;

    private ClassLoader classLoader = null;

    private Map<String, MsgletImpl> msgletMap = null;

    private Map<String, PackerImpl> packerMap = null;

    private Map<String, DescriptorPool> descriptorPoolMap = null;

    private Timer timer = null;

    private static class ContextHolder {

        static Context context = new Context();
    }

    public static Context getInstance() {
        return ContextHolder.context;
    }

    private Context() {
        msgletMap = new TreeMap<String, MsgletImpl>();
        packerMap = new TreeMap<String, PackerImpl>();
        descriptorPoolMap = new TreeMap<String, DescriptorPool>();
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getHomeDir() {
        return homeDir;
    }

    public void setHomeDir(String homeDir) {
        this.homeDir = homeDir;
    }

    public String getInstHomeDir() {
        return instHomeDir;
    }

    public void setInstHomeDir(String instHomeDir) {
        this.instHomeDir = instHomeDir;
    }

    public String getInstConfDir() {
        return instConfDir;
    }

    public void setInstConfDir(String instConfDir) {
        this.instConfDir = instConfDir;
    }

    public String getInstDescDir() {
        return instDescDir;
    }

    public void setInstDescDir(String instDescDir) {
        this.instDescDir = instDescDir;
    }

    public InstProps getInstProps() {
        return instProps;
    }

    public void setInstProps(InstProps instProps) {
        this.instProps = instProps;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public BufferFactory getBufferFactory() {
        return bufferFactory;
    }

    public void setBufferFactory(BufferFactory bufferFactory) {
        this.bufferFactory = bufferFactory;
    }

    public Workshop getWorkshop() {
        return workshop;
    }

    public void setWorkshop(Workshop workshop) {
        this.workshop = workshop;
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }

    public void setChannelManager(ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    public ServerManager getServerManager() {
        return serverManager;
    }

    public void setServerManager(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    public ProcessorManager getProcessorManager() {
        return processorManager;
    }

    public void setProcessorManager(ProcessorManager processorManager) {
        this.processorManager = processorManager;
    }

    public ClientManager getClientManager() {
        return clientManager;
    }

    public void setClientManager(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    public void putMsglet(String name, MsgletImpl msgletImpl) {
        msgletMap.put(name, msgletImpl);
    }

    public Msglet getMsglet(String name) {
        return msgletMap.get(name);
    }

    public void putPacker(String name, PackerImpl packerImpl) {
        packerMap.put(name, packerImpl);
    }

    public Packer getPacker(String name) {
        return packerMap.get(name);
    }

    public void putDescriptorPool(String name, DescriptorPool descriptorPool) {
        descriptorPoolMap.put(name, descriptorPool);
    }

    public DescriptorPool getDescriptorPool(String name) {
        return descriptorPoolMap.get(name);
    }

    public MessageQueue getMessageQueue() {
        return messageQueue;
    }

    public void setMessageQueue(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    public Router getRouter() {
        return router;
    }

    public void setRouter(Router router) {
        this.router = router;
    }

    public MessageTable getMessageTable() {
        return messageTable;
    }

    public void setMessageTable(MessageTable messageTable) {
        this.messageTable = messageTable;
    }

    public Timer getTimer() {
        return timer;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    private void clearMsgletMap() {
        for (MsgletImpl msgletImpl : msgletMap.values()) msgletImpl.close();
        msgletMap = null;
    }

    private void clearPackerMap() {
        for (PackerImpl packerImpl : packerMap.values()) packerImpl.close();
        packerMap = null;
    }

    private void clearDescriptorPoolMap() {
        for (DescriptorPool descriptorPool : descriptorPoolMap.values()) descriptorPool.close();
        descriptorPoolMap = null;
    }

    public void clear() {
        clearMsgletMap();
        clearPackerMap();
        clearDescriptorPoolMap();
        properties = null;
        homeDir = null;
        instConfDir = null;
        instDescDir = null;
        instProps = null;
        bufferFactory = null;
        workshop = null;
        channelManager = null;
        serverManager = null;
        processorManager = null;
        clientManager = null;
        messageTable = null;
        router = null;
        messageQueue = null;
        classLoader = null;
        timer = null;
    }
}
