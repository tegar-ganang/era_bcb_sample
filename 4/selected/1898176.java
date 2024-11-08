package org.retro.gis;

import org.retro.scheme.Scheme;
import org.retro.gis.tools.BasicActionTreeImpl;
import org.retro.gis.tools.NullActionImpl;
import org.retro.gis.tools.InvalidBotException;
import org.retro.gis.tools.DefaultTaskInterface;
import org.retro.gis.tools.IllegalTaskState;
import org.retro.gis.util.BotRecord;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

/**
 * AlphaBotThread and BetaBotThread are subclasses of this abstract class,
 * the bot process thread is used to perform the bot's thinking processes
 * outside of the normal bot communication.
 *
 * @author Berlin Brown
 */
public abstract class BotProcessThread extends Thread {

    /** make sure to sync the bot when need be */
    private boolean running = true;

    /** Bot Object to send communication messages through */
    protected PircBot _bot = null;

    private BotServer _server = null;

    private org.retro.gis.NewQueue _msgQueue = null;

    private String _channel = null;

    private TaskModuleImpl tasker = new TaskModuleImpl();

    /** 
	 * Save a cache of a set number of messages, in a object vector fashion.
	 *  <ul>
	 *   <li>Field0: Message
	 *   <li>Field1: Time-Long
	 *   <li>Field2: User (me/channel)
	 *  </ul>
	 */
    private final int quickCacheMaxSize = 20;

    private int quickCacheIndex = 0;

    private Object[] quickCacheMessages = new Object[quickCacheMaxSize];

    public BotProcessThread() {
    }

    /**
	 * The NewQueue class is a 'waiting' message queue, once a new message
	 * arrives, message processing can continue.
	 * 
	 * <p>
	 * We are watching out for a String, see msgQueue.add(), not addPersonalMsg.
	 * 
	 * <p>
	 * The msgQueue might be null if the Thread is not loaded properly.
	 */
    public final Object getNext() throws InvalidMessageException {
        if (_msgQueue == null) return null;
        Object o = _msgQueue.next();
        if (o == null) throw new InvalidMessageException("The Current Queue is empty");
        if (o instanceof String) {
            String msg = (String) o;
            addQuickCacheMessage(msg, "channel");
        }
        return o;
    }

    public final boolean hasNext() {
        return (_msgQueue.size() != 0);
    }

    public final int getMessageSize() {
        if (_msgQueue != null) {
            return _msgQueue.size();
        } else {
            return -1;
        }
    }

    /**
	 * The quick cache is a simple way to get to message without going through
	 * the more complex message database system.
	 * 
	 * The message will be added to the cache when it is received from the queue system.
	 * 
     * @param msg
	 * @param from
	 */
    private final void addQuickCacheMessage(String msg, String from) {
        String m = msg;
        Long t = new Long(System.currentTimeMillis());
        String f = from;
        Object[] vec = new Object[3];
        vec[0] = m;
        vec[1] = t;
        vec[2] = f;
        quickCacheMessages[quickCacheIndex] = vec;
        quickCacheIndex++;
        if (quickCacheIndex >= quickCacheMaxSize) {
            quickCacheIndex = 0;
        }
    }

    public final void loadThread(PircBot _b, org.retro.gis.NewQueue q) {
        _bot = _b;
        _msgQueue = q;
        _channel = null;
        String[] str = _bot.getChannels();
        if (str != null) {
            if (str.length > 0) {
                _channel = str[0];
            } else {
                _channel = _bot.getAttemptedChannel();
            }
        }
        miscSetup();
    }

    public final void setRunning(boolean _b) {
        running = _b;
    }

    public final boolean getRunning() {
        return running;
    }

    /**
	 * BotProcessThread Wrapper for the BotMessage Method.
	 * 
	 * @param _msg
	 * @throws InvalidBotException
	 */
    public final synchronized void sendMessage(String _msg) throws InvalidBotException {
        if (!running) return;
        if (_channel != null) {
            if (_msg != null) {
                if (_bot == null) throw new InvalidBotException();
                addQuickCacheMessage(_msg, "me");
                _bot.sendMessage(_channel, _msg);
            }
        }
    }

    public final void sendInternalMessage(String _internal_type, String _msg) throws InvalidBotException {
        if (!running) return;
        if (_msg != null) {
            if (_bot == null) throw new InvalidBotException();
            addQuickCacheMessage(_msg, "me");
            _bot.sendMessageInternalBotChannel(_internal_type, _msg);
        }
    }

    /**
	 * This method is similar to the default Bot-SendMessage, this method is used
	 * to communicate with the gui-client.
	 * 
	 * <p>
	 * Note : the use of the 'synchronized' will throw off the flow of this thread,
	 * use wisely.
	 * 
	 * @param _msg
	 * @throws Exception
	 */
    public final void sendClientMessage(String _msg) throws Exception {
        if (!this.getRunning()) throw new Exception("The Process-Thread is currently inactive, the message cannot be sent");
        if (_server == null) throw new Exception("This BotServer is invalid");
        _server.botPrepareFinalMessage(_msg);
        addQuickCacheMessage(_msg, "client");
    }

    /**
	 * Set the bot-server outside of thread; the PircBot might set this value.
	 * 
	 * @param _srv
	 */
    public final void setBotServer(BotServer _srv) {
        _server = _srv;
    }

    /**
	 * When a visit is performed on a BotRecord, the
	 * action network will come into play and the XML
	 * to Action-Object conversion will take place.
	 * 
	 * @param b		The BotRecord that contains Action-Tree
	 */
    public final void visitBotRecord(BotRecord b) {
        BasicActionTreeImpl rootTree = new BasicActionTreeImpl();
        NullActionImpl actionNodeTree = null;
        try {
            Element _root = b.getXMLCommand("action-network");
            actionNodeTree = new NullActionImpl(10, -900, 900);
            actionNodeTree.setKey("Action-Tree-01");
            actionNodeTree.setSchemeObject(this.getSchemeObject());
            actionNodeTree.setProcessThreadNode(this);
            NodeList children = _root.getChildNodes();
            for (int a = 0; a < children.getLength(); a++) {
                Node node = children.item(a);
                if (node instanceof Element) {
                    Element ac = (Element) node;
                    if (!ac.getTagName().equals("action")) throw new Exception("Invalid XML action tag-name");
                    String _type = ac.getAttribute("type");
                    String _cmd = ac.getAttribute("command");
                    if (_type.equalsIgnoreCase("scheme")) {
                        actionNodeTree.smartCreateBranch(_cmd, null);
                    }
                }
            }
            rootTree.addNode(actionNodeTree);
        } catch (Exception y) {
            y.printStackTrace();
        }
        rootTree.runActionSetList();
    }

    /**
	 * When a visit is performed on a BotRecord, the
	 * action network will come into play and the XML
	 * to Action-Object conversion will take place.
	 * 
	 * <p>
	 * The auto method visits action nodes and the botnetwork,
	 *  links. (recursive)
	 * 
	 * @param b		  The BotRecord that contains Action-Tree
	 * @param _super  Use Auto over the default-visit when hitting the
	 * 	              links, the recursive call may be dangerous.
	 */
    public final void visitBotRecordAuto(BotRecord b, boolean _super) {
        BasicActionTreeImpl rootTree = new BasicActionTreeImpl();
        NullActionImpl actionNodeTree = null;
        try {
            Element _root = b.getXMLCommand("action-network");
            actionNodeTree = new NullActionImpl(10, -900, 900);
            actionNodeTree.setKey("Action-Tree-01");
            actionNodeTree.setSchemeObject(this.getSchemeObject());
            actionNodeTree.setProcessThreadNode(this);
            NodeList children = _root.getChildNodes();
            for (int a = 0; a < children.getLength(); a++) {
                Node node = children.item(a);
                if (node instanceof Element) {
                    Element ac = (Element) node;
                    if (!ac.getTagName().equals("action")) throw new Exception("Invalid XML action tag-name");
                    String _type = ac.getAttribute("type");
                    String _cmd = ac.getAttribute("command");
                    if (_type.equalsIgnoreCase("scheme")) {
                        actionNodeTree.smartCreateBranch(_cmd, null);
                    }
                }
            }
            rootTree.addNode(actionNodeTree);
            Element WEBroot = b.getXMLCommand("web-network");
            NodeList WEBchildren = WEBroot.getChildNodes();
            for (int z = 0; z < WEBchildren.getLength(); z++) {
                Node node = WEBchildren.item(z);
                if (node instanceof Element) {
                    Element ac = (Element) node;
                    if (!ac.getTagName().equals("link")) throw new Exception("Invalid XML action tag-name [" + ac.getTagName() + "]");
                    String _dbx = ac.getAttribute("db-name");
                    int _idx = Integer.parseInt(ac.getAttribute("id"));
                    if (_idx >= 0) {
                        BotRecord rc = findRecordIDProcess(_dbx, _idx);
                        if (_super) visitBotRecordAuto(rc, true); else visitBotRecord(rc);
                    } else {
                        System.out.println("WARNING: Invalid Bot-Record ID : " + _dbx + " : " + _idx);
                    }
                }
            }
        } catch (Exception y) {
            y.printStackTrace();
        }
        rootTree.runActionSetList();
    }

    /**
	 * A wrapper for the RelateDatabaseEngine find record.
	 * 
	 * <p>
	 * Due to the wrapper nature of this method, we will identify
	 * this method with <code>Bot</code> for the bot-wrapper and
	 * <code>Process</code> for the Process-Thread wrapper.
	 * 
	 * @param database		String of Database-Name  in Relate-Engine
	 * @param id		  
	 * @return
	 * @throws Exception	In case of invalid database or record not found
	 * 
	 * @see RelateDatabaseImpl
	 * @see BotProcessThread 
	 */
    public abstract BotRecord findRecordIDProcess(String database, int id) throws Exception;

    public abstract void addPersonalMsg(String _ms);

    public abstract void miscSetup();

    public abstract void shutdown();

    public abstract void run();

    public abstract Scheme getSchemeObject();

    /**
	 * Each bot-process will have an internal DefaultTaskInterface, a task,
	 * the producing-process, should call:
	 * 
	 * <ul>
	 *  <li><code>setTask()</code> - init the internal task
	 *	<li><code>runTask()</code> - when ready run the task and hopefully it will finish
	 * </ul>
	 *
	 * <P>
	 * You cannot set a task while another task is running, this may cause an
	 * exception or other unexpected events.
	 * 
	 * @see TaskModule
	 * @see TaskModuleHandler
	 * @see TaskModuleImpl
	 * @see DefaultTaskInterface
	 */
    public final void setTask(DefaultTaskInterface _task) {
        try {
            tasker.setupTask(_task);
        } catch (IllegalTaskState e) {
            System.out.println(e.getMessage());
        }
    }

    /**
	 * Poll the task handler and make sure the task has stopped or invalid.
	 * 	
	 * @see  	org.retro.gis.tools.DefaultTaskInterface
	 * @return 	int		Check for the DefaultTaskInterface.TASK_STOPPED
	 *             		or the TASK_ERR.
	 */
    public final int pollTaskRunningFlag() {
        return tasker.getTaskRunningState();
    }

    public final void startRunTask() {
        tasker.flipRunFlag();
    }

    public final void waitTaskDone() {
        tasker.waitEventRunTaskSwitch();
    }

    public final void killTask() {
        if (tasker.getTask() != null) {
            tasker.killTask();
        }
    }
}
