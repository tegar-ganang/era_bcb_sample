package com.taobao.remote;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.taobao.remote.common.core.Parser;

/**
 * socket������
 * 
 * nioʵ��
 * 
 * @version 2007-1-31
 * @author xalinx at gmail dot com
 * 
 */
public abstract class NioSocketListener<E, M> extends Thread {

    protected final Log log = LogFactory.getLog(this.getClass());

    /**
	 * �����־
	 * 
	 * ��ʼ������
	 */
    private volatile boolean listenTag = false;

    /**
	 * �״̬��־
	 * 
	 * ��ʼ���
	 */
    private volatile boolean openTag = false;

    /**
	 * ����˿�
	 */
    private int port;

    /**
	 * �����ѹ����
	 */
    private int backlog;

    /**
	 * ����˿�
	 */
    private ServerSocketChannel serverSocketChannel;

    /**
	 * ���ն�ȡѡ����
	 */
    private Selector acceptSelector;

    /**
	 * ����key map
	 */
    private Map<String, SelectionKey> keyMap;

    /**
	 * ������˹�
	 */
    protected AbstractTaskPorter<E, M> taskPorter;

    public void setTaskPorter(AbstractTaskPorter<E, M> taskPorter) {
        this.taskPorter = taskPorter;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public int getPort() {
        return port;
    }

    protected NioSocketListener() {
    }

    public void run() {
        initResource();
        listenTag = true;
        openTag = true;
        log.info(getName() + " open on:" + port);
        while (listenTag) {
            try {
                acceptSelector.select();
            } catch (IOException e) {
                log.warn("run() selector.select():", e);
                continue;
            }
            Iterator<SelectionKey> itt = acceptSelector.selectedKeys().iterator();
            while (itt.hasNext()) {
                SelectionKey key = itt.next();
                itt.remove();
                if (key.isValid()) {
                    try {
                        if (key.isReadable()) {
                            doRead(key);
                        } else if (key.isWritable()) {
                            doWrite(key);
                        } else if (key.isAcceptable()) {
                            doAccept(key);
                        }
                    } catch (Throwable e) {
                        destroyKeyAndChannel(key);
                        log.warn("do key:", e);
                    }
                }
            }
        }
        clearResource();
        openTag = false;
    }

    /**
	 * �ж��Ƿ����״̬
	 * 
	 * @return
	 */
    public boolean isOpen() {
        return openTag;
    }

    /**
	 * �رռ�����
	 */
    public void doStop() {
        log.info(getName() + " stopping ...");
        listenTag = false;
        if (acceptSelector != null) {
            acceptSelector.wakeup();
        }
        try {
            this.join();
        } catch (InterruptedException e) {
            log.warn(getName() + " do stop:", e);
        }
        log.info(getName() + " stopped on:" + port);
    }

    public String[] getReadableConnections() {
        String[] conns = null;
        synchronized (keyMap) {
            int size = keyMap.size();
            if (size == 0) {
                conns = ArrayUtils.EMPTY_STRING_ARRAY;
            } else if (size != 0) {
                conns = new String[size];
                keyMap.keySet().toArray(conns);
            }
        }
        return conns;
    }

    public void pushTask(String host, int port, Task task) {
        SelectionKey key = null;
        synchronized (keyMap) {
            key = keyMap.get(host + ":" + port);
        }
        if (key != null) {
            TaskHelper.writeTask(key, task);
        } else {
            log.info(host + ":" + port + " not connect");
        }
    }

    public void pushTask(Task task) {
        SelectionKey[] keys = null;
        synchronized (keyMap) {
            if (keyMap.size() == 0) {
                return;
            }
            keys = keyMap.values().toArray(new SelectionKey[keyMap.size()]);
        }
        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                SelectionKey key = keys[i];
                TaskHelper.writeTask(key, task);
            }
            if (log.isDebugEnabled()) {
                log.debug(" push task:" + task.hashCode() + " to " + keys.length + " clients");
                for (int i = 0; i < keys.length; i++) {
                    log.debug("push task:" + task.hashCode() + " to " + keys[i].channel());
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("none client need push");
            }
        }
    }

    /**
	 * ����������ģ�淽��
	 * 
	 * @return
	 */
    protected abstract Task<E, M> createEmpetyTask();

    /**
	 * �������������ģ�淽��
	 * 
	 * @return
	 */
    protected abstract Parser<E> getRequestParser();

    /**
	 * ������Ӧ������ģ�淽��
	 * 
	 * @return
	 */
    protected abstract Parser<M> getResponseParser();

    /**
	 * �пͻ���������������
	 * 
	 * @param acceptKey
	 * @throws IOException
	 */
    private void doAccept(SelectionKey acceptKey) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("do accept");
        }
        ServerSocketChannel ssc = (ServerSocketChannel) acceptKey.channel();
        SocketChannel channel = ssc.accept();
        if (channel == null) {
            throw new IOException("accept nothing when do accept key");
        }
        channel.configureBlocking(false);
        log.info("do accept: " + channel);
        SelectionKey taskKey = channel.register(acceptSelector, SelectionKey.OP_READ);
        TaskReader<E, M> reader = new TaskReader<E, M>(createEmpetyTask(), taskKey, getRequestParser(), getResponseParser());
        TaskWriter writer = new TaskWriter(taskKey);
        TaskAttach<E, M> attach = new TaskAttach<E, M>(reader, writer);
        taskKey.attach(attach);
        Socket socket = channel.socket();
        String host = socket.getInetAddress().getHostAddress();
        int port = socket.getPort();
        addKey(host, port, taskKey);
    }

    /**
	 * ��ȡ����ݣ���װ��Ҫ�����task,����task porter
	 * 
	 * @param readKey
	 * @throws IOException
	 * @throws IOException
	 * @throws IOException
	 * @throws IOException
	 * @throws InterruptedException
	 */
    @SuppressWarnings("unchecked")
    private void doRead(SelectionKey readKey) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("do read");
        }
        TaskAttach<E, M> attach = (TaskAttach<E, M>) readKey.attachment();
        TaskReader<E, M> reader = attach.getTaskReader();
        int count = reader.read();
        if (count < 0) {
            log.warn("client abnormity close: " + readKey.channel());
            destroyKeyAndChannel(readKey);
            return;
        }
        if (reader.isReadFinish()) {
            int taskLength = reader.getTaskLength();
            if (taskLength == -1) {
                log.info("client exist: " + readKey.channel());
                destroyKeyAndChannel(readKey);
            } else if (taskLength > 0) {
                Task<E, M> task = reader.getTask();
                if (task == null) {
                    throw new IllegalStateException("valid task shouldn't be null");
                }
                if (task != null) {
                    taskPorter.add(task);
                }
            } else if (taskLength == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("keep alive: " + readKey.channel());
                }
            }
            if (taskLength != -1) {
                reader.reset(createEmpetyTask());
            }
        }
    }

    /**
	 * д�����
	 * 
	 * @param writeKey
	 * @throws IOException
	 */
    @SuppressWarnings("unchecked")
    private void doWrite(SelectionKey writeKey) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("do write");
        }
        TaskAttach<E, M> attach = (TaskAttach<E, M>) writeKey.attachment();
        TaskWriter writer = attach.getTaskWriter();
        long count = writer.flush();
        if (log.isDebugEnabled()) {
            log.debug("write count:" + count + " to " + writeKey.channel());
        }
    }

    /**
	 * ��ʼ��������Դ
	 * 
	 * @see com.taobao.remote.NioSocketListener#initResource()
	 */
    private void initResource() {
        taskPorter.setDaemon(true);
        taskPorter.start();
        while (!taskPorter.isOpen()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        this.keyMap = new HashMap<String, SelectionKey>();
        try {
            InetSocketAddress address = new InetSocketAddress(port);
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().bind(address, backlog);
            acceptSelector = Selector.open();
            serverSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            log.error("run() register server socket channel:", e);
            throw new RuntimeException(e);
        }
        log.debug("port:" + port);
    }

    /**
	 * ��ټ�����Դ
	 */
    private void clearResource() {
        try {
            serverSocketChannel.close();
            acceptSelector.close();
        } catch (IOException e) {
            log.error("stop listener: ", e);
        }
        synchronized (this.keyMap) {
            this.keyMap = null;
        }
        this.taskPorter.doStop();
    }

    private void addKey(String host, int port, SelectionKey key) {
        synchronized (this.keyMap) {
            this.keyMap.put(host + ":" + port, key);
        }
    }

    private void removeKey(String host, int port) {
        synchronized (this.keyMap) {
            this.keyMap.remove(host + ":" + port);
        }
    }

    private void destroyKeyAndChannel(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            if (key.attachment() != null) {
                TaskAttach attach = (TaskAttach) key.attachment();
                try {
                    attach.close();
                } catch (Throwable e) {
                    log.error("destroy key attach:", e);
                }
                attach = null;
            }
            Socket socket = channel.socket();
            String host = socket.getInetAddress().getHostAddress();
            int port = socket.getPort();
            try {
                removeKey(host, port);
            } catch (Throwable e) {
                log.error("remove key store:", e);
            }
        } finally {
            try {
                key.cancel();
            } catch (Throwable e) {
                log.error("destroy key:" + key, e);
            }
            try {
                channel.close();
            } catch (Throwable e) {
                log.error("destroy channel:" + channel, e);
            }
        }
    }
}
