package com.dynomedia.esearch.util.groupkeycache;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import com.dynomedia.esearch.util.groupkeycache.util.GkcConfig;

/**
 * 具有特定标识的关键词组对象。add,write方法线程同步保护，并提供内部topkey对象排序及Groupkey容量控制。
 * @author Administrator
 * @version 1.0
 * @created 19-十月-2009 16:50:13
 */
public class Groupkey {

    private static final Log logger = LogFactory.getLog(Groupkey.class);

    private Long id;

    /**
	 * 高频关键字组名称
	 */
    private String name;

    /**
	 * 外部文件写操作标志 true of false
	 */
    private boolean writeFlag = false;

    /** 
	 * 缓存数据排序间隔
	 */
    private Integer sortInterval;

    /**
	 * 缓存排序时间点
	 */
    private Date sortTime;

    /**
	 * Groupkey缓存对象数。
	 */
    private Integer size = GkcConfig.getInstance().getGroupkeySize();

    /**
	 * 采样容器的最大容量设定
	 */
    private Integer samplingSize = size * GkcConfig.getInstance().getGroupkeySamplingMatchfactor();

    /**
	 * flush采样容器标识
	 */
    private boolean isFlush = GkcConfig.getInstance().isFlush();

    /**
	 * key值的采样容器
	 */
    private Map<String, Topkey> sampledTopkeys = new HashMap<String, Topkey>();

    /**
	 * 经过排序后的topkey对象数组，最大容量 = size
	 */
    private List<Topkey> sortedTopkeys = new ArrayList<Topkey>(0);

    /**
	 * 负责去重的Set容器
	 */
    private Map<String, Topkey> distinctTopkeys = new HashMap<String, Topkey>();

    private static XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());

    public Groupkey() {
    }

    /**
	 * 构造
	 * 
	 * @param id
	 * @param name
	 * @param size
	 */
    public Groupkey(Long id, String name, Integer size, Integer sortInterval) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.sortInterval = sortInterval;
        init();
    }

    /**
	 * 读取外部缓存的Groupkey数据，用于构造初始化，init应当保证Groupkey从外部文件中初始化size条记录。
	 */
    private void init() {
        loadFromPersistent();
        reset();
    }

    private void reset() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.SECOND, sortInterval);
        sortTime = calendar.getTime();
        writeFlag = false;
        if (isFlush) sampledTopkeys.clear();
    }

    /**
	 * GroupkeyCache调用
	 * 
	 * @param key
	 */
    public boolean add(String key) {
        Topkey topkey = get(key);
        if (topkey != null) {
            topkey.updateCount(1);
        } else {
            if (isFlush && sampledTopkeys.size() > samplingSize) return true;
            if (key.length() < GkcConfig.getInstance().getKeyMinlength()) return true;
            if (key.length() > GkcConfig.getInstance().getKeyMaxlength()) key = key.substring(0, GkcConfig.getInstance().getKeyMaxlength());
            (newTopkey(0, key)).updateCount(1);
        }
        writeFlag = true;
        return true;
    }

    /**
	 * 
	 * @param key
	 */
    protected Topkey get(String key) {
        return sampledTopkeys.get(key);
    }

    /**
	 * 创建新的Topkey对象。进行二次同步检查，如果topkeys中已经存在对应的topkey，则直接返回该topkey对象。
	 * 
	 * @param count
	 * @param key
	 */
    private synchronized Topkey newTopkey(Integer count, String key) {
        if (get(key) != null) return get(key);
        Topkey topkey = new Topkey(key, 0);
        sampledTopkeys.put(key, topkey);
        if (logger.isDebugEnabled()) logger.debug("Groupkey create new Topkey for key = " + key);
        return topkey;
    }

    /**
	 * 返回writeFlag，外部线程获得排序操作状态
	 */
    public boolean needSort() {
        return (writeFlag && sortTime.before(new Date()));
    }

    /**
	 * sort方法同时实现了该groupkey内部的topkey排序及groupkey的大小检查。
	 */
    public void sort() {
        long startime = System.currentTimeMillis();
        synchronized (this) {
            if (!needSort()) return;
            if (logger.isDebugEnabled()) logger.debug("Groupkey write start!Groupkey status：" + toString() + " by Thread:" + Thread.currentThread().getName());
            for (Topkey key : sortedTopkeys) distinctTopkeys.put(key.getKey(), key);
            distinctTopkeys.putAll(sampledTopkeys);
            reset();
        }
        List<Topkey> sortedArr = new ArrayList<Topkey>(distinctTopkeys.values());
        distinctTopkeys.clear();
        Collections.sort(sortedArr);
        if (sortedArr.size() > size) {
            sortedArr.subList(size, sortedArr.size()).clear();
        }
        this.sortedTopkeys = sortedArr;
        if (GkcConfig.getInstance().isStore() && sortedTopkeys.size() > 0) cachePersistent();
        long timeEclipse = System.currentTimeMillis() - startime;
        if (timeEclipse > 1000) logger.warn("Groupkey write time too long! timeElapse:" + timeEclipse + " ms KeyBuffer.size:" + KeyBuffer.size());
        if (logger.isDebugEnabled()) logger.debug("Groupkey status:" + toString() + "sortedTopkeys:" + descSortedTopkeys());
    }

    /**
	 * 缓存数据持久化
	 * baipeng wrote at 下午04:39:01 2009-10-27
	 */
    private void cachePersistent() {
        Element root = new Element("groupkey");
        root.setAttribute("id", String.valueOf(id));
        root.setAttribute("name", name);
        root.setAttribute("time", DateFormatUtils.format(new Date(), "yyyyMMddHHmmss"));
        Element topkeys = new Element("topkeys");
        root.addContent(topkeys);
        List<Topkey> sortedTopkeys = this.sortedTopkeys;
        for (Topkey key : sortedTopkeys) {
            Element innerkey = new Element("key");
            innerkey.setAttribute("key", key.getKey());
            innerkey.setAttribute("count", String.valueOf(key.getCount()));
            topkeys.addContent(innerkey);
        }
        String content = outputter.outputString(new Document(root));
        File cachefile = new File(GkcConfig.getInstance().getDataDir(), String.valueOf(id) + File.separatorChar + id + ".xml");
        try {
            if (cachefile.exists()) cachefile.delete();
            FileUtils.writeStringToFile(cachefile, content, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Groupkey cachePersistent error!", e);
        }
    }

    private void loadFromPersistent() {
        File cachefile = new File(GkcConfig.getInstance().getDataDir(), String.valueOf(id) + File.separatorChar + id + ".xml");
        try {
            if (!cachefile.getParentFile().exists() && GkcConfig.getInstance().isStore()) {
                FileUtils.forceMkdir(cachefile.getParentFile());
                if (logger.isInfoEnabled()) logger.info("Groupkey create new dir for groupkey cachedir:" + cachefile.getParentFile().getAbsolutePath());
                return;
            }
            if (!cachefile.exists()) return;
            Document doc = (new SAXBuilder()).build(cachefile);
            this.name = doc.getRootElement().getAttributeValue("name");
            List<Element> cachedtopkeys = doc.getRootElement().getChild("topkeys").getChildren("key");
            for (Element e : cachedtopkeys) {
                Topkey topkey = new Topkey(e.getAttributeValue("key"), Integer.valueOf(e.getAttributeValue("count")));
                sampledTopkeys.put(e.getAttributeValue("key"), topkey);
                sortedTopkeys.add(topkey);
            }
            Collections.sort(sortedTopkeys);
            if (logger.isInfoEnabled()) logger.info("Groupkey load from " + cachefile.getAbsolutePath() + " persistent(" + doc.getRootElement().getAttributeValue("time") + ") sampledTopkeys.size:" + sampledTopkeys.size() + " status:" + this);
        } catch (Exception e) {
            logger.error("Groupkey loadFromPersistent error!cachefile:" + cachefile.getAbsolutePath(), e);
        }
    }

    public Long getId() {
        return id;
    }

    public int hashCode() {
        return this.id.hashCode();
    }

    /**
	 * 
	 * @param anObject
	 */
    public boolean equals(Object anObject) {
        if (anObject instanceof Groupkey) {
            Groupkey anGroupkey = (Groupkey) anObject;
            return (this.id).equals(anGroupkey.getId());
        }
        return false;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("\r\t[id]:" + id + "\t").append("[name]:" + name + "\t").append("[writeFlag]:" + writeFlag + "\t").append("[sampledTopkeys.size]:" + sampledTopkeys.size() + "\t").append("[sortedTopkeys.size]:" + sortedTopkeys.size() + "\r\t");
        return sb.toString();
    }

    public String descSortedTopkeys() {
        StringBuffer sb = new StringBuffer();
        sb.append("\r\t[sortedTopkeys.size]:" + sortedTopkeys.size() + "\r\t").append("[sortedTopkeys]:" + "\r\t");
        for (Topkey topkey : sortedTopkeys) {
            sb.append(topkey + "\r\t");
        }
        return sb.toString();
    }

    public List<Topkey> getSortedTopkeys() {
        return sortedTopkeys;
    }

    public Map<String, Topkey> getSampledTopkeys() {
        return sampledTopkeys;
    }
}
