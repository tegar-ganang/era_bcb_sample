package ces.platform.system.ui.config.form;

import org.apache.struts.action.ActionForm;

public class CommonForm extends ActionForm {

    private String dataDir;

    private String appServer;

    private String affixSize;

    private String server;

    private String track;

    private String retunReceipt;

    private String serverType;

    private String poolName;

    private String loadMessageCom;

    private String imServerPort;

    private String imServerIp;

    private String imReceivePort;

    private String mailSupply;

    private String bbsSupply;

    private String ipSupply;

    private String oaSupply;

    private String sysDataBaseType;

    private String sysUserName;

    private String sysPassWord;

    private String sysJdbc;

    private String sysUrl;

    private String infoDataBaseType;

    private String infoUserName;

    private String infoPassWord;

    private String infoJdbc;

    private String infoUrl;

    private String infoPoolName;

    private String infoDataDir;

    private String authority;

    private String usercontrol;

    private String imNotice;

    private String channelPath;

    private String doctypePath;

    private String load;

    private String leadChannelPath;

    private String left;

    private String right;

    private String buttom;

    private String reImNotice;

    private String send;

    private String meetImNotice;

    private String baseUrl;

    private String dataBaseUrl;

    private String filePath;

    private String dataBasePath;

    private String addressSupply;

    private String channelName;

    private String doctypeName;

    private String leadChannelName;

    private String calPlanLoad;

    private String calPlanTime;

    private String timingsend;

    private String tstime;

    private String traUrl;

    private String traForm;

    private String traIncl;

    private String traCook;

    private String traSess;

    private String traBean;

    public String getTimingsend() {
        return timingsend == null ? "" : timingsend;
    }

    public String getTstime() {
        return tstime;
    }

    public String getCalPlanLoad() {
        return calPlanLoad == null ? "" : calPlanLoad;
    }

    public String getCalPlanTime() {
        return calPlanTime;
    }

    public String getTraUrl() {
        return traUrl;
    }

    public String getTraForm() {
        return traForm;
    }

    public String getTraIncl() {
        return traIncl;
    }

    public String getTraCook() {
        return traCook;
    }

    public String getTraSess() {
        return traSess;
    }

    public String getTraBean() {
        return traBean;
    }

    public void setTimingsend(String timingsend) {
        this.timingsend = timingsend;
    }

    public void setTstime(String tstime) {
        this.tstime = tstime;
    }

    public void setCalPlanLoad(String calPlanLoad) {
        this.calPlanLoad = calPlanLoad;
    }

    public void setCalPlanTime(String calPlanTime) {
        this.calPlanTime = calPlanTime;
    }

    public void setTraUrl(String traUrl) {
        this.traUrl = traUrl;
    }

    public void setTraForm(String traForm) {
        this.traForm = traForm;
    }

    public void setTraIncl(String traIncl) {
        this.traIncl = traIncl;
    }

    public void setTraCook(String traCook) {
        this.traCook = traCook;
    }

    public void setTraSess(String traSess) {
        this.traSess = traSess;
    }

    public void setTraBean(String traBean) {
        this.traBean = traBean;
    }

    /**
	 * @return
	 */
    public String getDataDir() {
        return this.dataDir;
    }

    /**
	 * @return
	 */
    public String getAppServer() {
        return this.appServer;
    }

    /**
	 * @param string
	 */
    public void setAppServer(String appServer) {
        this.appServer = appServer;
    }

    /**
	 * @param string
	 */
    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    /**
	 * @return
	 */
    public String getAffixSize() {
        return affixSize;
    }

    /**
	 * @return
	 */
    public String getAuthority() {
        return authority;
    }

    /**
	 * @return
	 */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
	 * @return
	 */
    public String getBbsSupply() {
        return bbsSupply == null ? "" : bbsSupply;
    }

    public String getIpSupply() {
        return ipSupply == null ? "" : ipSupply;
    }

    public String getOaSupply() {
        return oaSupply == null ? "" : oaSupply;
    }

    /**
	 * @return
	 */
    public String getButtom() {
        return buttom;
    }

    /**
	 * @return
	 */
    public String getChannelPath() {
        return channelPath;
    }

    /**
	 * @return
	 */
    public String getDataBaseUrl() {
        return dataBaseUrl;
    }

    /**
	 * @return
	 */
    public String getDoctypePath() {
        return doctypePath;
    }

    /**
	 * @return
	 */
    public String getImNotice() {
        return imNotice;
    }

    /**
	 * @return
	 */
    public String getInfoDataBaseType() {
        return infoDataBaseType;
    }

    /**
	 * @return
	 */
    public String getInfoDataDir() {
        return infoDataDir;
    }

    /**
	 * @return
	 */
    public String getInfoJdbc() {
        return infoJdbc;
    }

    /**
	 * @return
	 */
    public String getInfoPassWord() {
        return infoPassWord;
    }

    /**
	 * @return
	 */
    public String getInfoPoolName() {
        return infoPoolName;
    }

    /**
	 * @return
	 */
    public String getInfoUrl() {
        return infoUrl;
    }

    /**
	 * @return
	 */
    public String getInfoUserName() {
        return infoUserName;
    }

    /**
	 * @return
	 */
    public String getLeadChannelPath() {
        return leadChannelPath;
    }

    /**
	 * @return
	 */
    public String getLeft() {
        return left;
    }

    /**
	 * @return
	 */
    public String getLoad() {
        return load;
    }

    /**
	 * @return
	 */
    public String getLoadMessageCom() {
        return loadMessageCom == null ? "" : loadMessageCom;
    }

    /**
	 * @return
	 */
    public String getImServerPort() {
        return imServerPort;
    }

    /**
	 * @return
	 */
    public String getImServerIp() {
        return imServerIp;
    }

    /**
	 * @return
	 */
    public String getImReceivePort() {
        return imReceivePort;
    }

    /**
	 * @return
	 */
    public String getMailSupply() {
        return mailSupply == null ? "" : mailSupply;
    }

    /**
	 * @return
	 */
    public String getMeetImNotice() {
        return meetImNotice;
    }

    /**
	 * @return
	 */
    public String getPoolName() {
        return poolName;
    }

    /**
	 * @return
	 */
    public String getReImNotice() {
        return reImNotice;
    }

    /**
	 * @return
	 */
    public String getRetunReceipt() {
        return retunReceipt == null ? "" : retunReceipt;
    }

    /**
	 * @return
	 */
    public String getRight() {
        return right;
    }

    /**
	 * @return
	 */
    public String getSend() {
        return send;
    }

    /**
	 * @return
	 */
    public String getServer() {
        return server;
    }

    /**
	 * @return
	 */
    public String getServerType() {
        return serverType;
    }

    /**
	 * @return
	 */
    public String getSysDataBaseType() {
        return sysDataBaseType;
    }

    /**
	 * @return
	 */
    public String getSysJdbc() {
        return sysJdbc;
    }

    /**
	 * @return
	 */
    public String getSysPassWord() {
        return sysPassWord;
    }

    /**
	 * @return
	 */
    public String getSysUrl() {
        return sysUrl;
    }

    /**
	 * @return
	 */
    public String getSysUserName() {
        return sysUserName;
    }

    /**
	 * @return
	 */
    public String getTrack() {
        return track == null ? "" : track;
    }

    /**
	 * @return
	 */
    public String getUsercontrol() {
        return usercontrol;
    }

    /**
	 * @param string
	 */
    public void setAffixSize(String affixSize) {
        this.affixSize = affixSize;
    }

    /**
	 * @param string
	 */
    public void setAuthority(String authority) {
        this.authority = authority;
    }

    /**
	 * @param string
	 */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
	 * @param string
	 */
    public void setBbsSupply(String bbsSupply) {
        this.bbsSupply = bbsSupply;
    }

    public void setIpSupply(String ipSupply) {
        this.ipSupply = ipSupply;
    }

    public void setOaSupply(String oaSupply) {
        this.oaSupply = oaSupply;
    }

    /**
	 * @param string
	 */
    public void setButtom(String buttom) {
        this.buttom = buttom;
    }

    /**
	 * @param string
	 */
    public void setChannelPath(String channelPath) {
        this.channelPath = channelPath;
    }

    /**
	 * @param string
	 */
    public void setDataBaseUrl(String dataBaseUrl) {
        this.dataBaseUrl = dataBaseUrl;
    }

    /**
	 * @param string
	 */
    public void setDoctypePath(String doctypePath) {
        this.doctypePath = doctypePath;
    }

    /**
	 * @param string
	 */
    public void setImNotice(String imNotice) {
        this.imNotice = imNotice;
    }

    /**
	 * @param string
	 */
    public void setInfoDataBaseType(String infoDataBaseType) {
        this.infoDataBaseType = infoDataBaseType;
    }

    /**
	 * @param string
	 */
    public void setInfoDataDir(String infoDataDir) {
        this.infoDataDir = infoDataDir;
    }

    /**
	 * @param string
	 */
    public void setInfoJdbc(String infoJdbc) {
        this.infoJdbc = infoJdbc;
    }

    /**
	 * @param string
	 */
    public void setInfoPassWord(String infoPassWord) {
        this.infoPassWord = infoPassWord;
    }

    /**
	 * @param string
	 */
    public void setInfoPoolName(String infoPoolName) {
        this.infoPoolName = infoPoolName;
    }

    /**
	 * @param string
	 */
    public void setInfoUrl(String infoUrl) {
        this.infoUrl = infoUrl;
    }

    /**
	 * @param string
	 */
    public void setInfoUserName(String infoUserName) {
        this.infoUserName = infoUserName;
    }

    /**
	 * @param string
	 */
    public void setLeadChannelPath(String leadChannelPath) {
        this.leadChannelPath = leadChannelPath;
    }

    /**
	 * @param string
	 */
    public void setLeft(String left) {
        this.left = left;
    }

    /**
	 * @param string
	 */
    public void setLoad(String load) {
        this.load = load;
    }

    /**
	 * @param string
	 */
    public void setLoadMessageCom(String loadMessageCom) {
        this.loadMessageCom = loadMessageCom;
    }

    /**
	 * @param string
	 */
    public void setImServerPort(String imServerPort) {
        this.imServerPort = imServerPort;
    }

    /**
	 * @param string
	 */
    public void setImServerIp(String imServerIp) {
        this.imServerIp = imServerIp;
    }

    /**
	 * @param string
	 */
    public void setImReceivePort(String imReceivePort) {
        this.imReceivePort = imReceivePort;
    }

    /**
	 * @param string
	 */
    public void setMailSupply(String mailSupply) {
        this.mailSupply = mailSupply;
    }

    /**
	 * @param string
	 */
    public void setMeetImNotice(String meetImNotice) {
        this.meetImNotice = meetImNotice;
    }

    /**
	 * @param string
	 */
    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    /**
	 * @param string
	 */
    public void setReImNotice(String reImNotice) {
        this.reImNotice = reImNotice;
    }

    /**
	 * @param string
	 */
    public void setRetunReceipt(String retunReceipt) {
        this.retunReceipt = retunReceipt;
    }

    /**
	 * @param string
	 */
    public void setRight(String right) {
        this.right = right;
    }

    /**
	 * @param string
	 */
    public void setSend(String send) {
        this.send = send;
    }

    /**
	 * @param string
	 */
    public void setServer(String server) {
        this.server = server;
    }

    /**
	 * @param string
	 */
    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    /**
	 * @param string
	 */
    public void setSysDataBaseType(String sysDataBaseType) {
        this.sysDataBaseType = sysDataBaseType;
    }

    /**
	 * @param string
	 */
    public void setSysJdbc(String sysJdbc) {
        this.sysJdbc = sysJdbc;
    }

    /**
	 * @param string
	 */
    public void setSysPassWord(String sysPassWord) {
        this.sysPassWord = sysPassWord;
    }

    /**
	 * @param string
	 */
    public void setSysUrl(String sysUrl) {
        this.sysUrl = sysUrl;
    }

    /**
	 * @param string
	 */
    public void setSysUserName(String sysUserName) {
        this.sysUserName = sysUserName;
    }

    /**
	 * @param string
	 */
    public void setTrack(String track) {
        this.track = track;
    }

    /**
	 * @param string
	 */
    public void setUsercontrol(String usercontrol) {
        this.usercontrol = usercontrol;
    }

    /**
	 * @return
	 */
    public String getFilePath() {
        return filePath;
    }

    /**
	 * @param string
	 */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
	 * @return
	 */
    public String getDataBasePath() {
        return dataBasePath;
    }

    /**
	 * @param string
	 */
    public void setDataBasePath(String dataBasePath) {
        this.dataBasePath = dataBasePath;
    }

    /**
	 * @return
	 */
    public String getAddressSupply() {
        return addressSupply == null ? "" : addressSupply;
    }

    /**
	 * @param string
	 */
    public void setAddressSupply(String addressSupply) {
        this.addressSupply = addressSupply;
    }

    /**
	 * @return
	 */
    public String getChannelName() {
        return channelName;
    }

    /**
	 * @return
	 */
    public String getDoctypeName() {
        return doctypeName;
    }

    /**
	 * @return
	 */
    public String getLeadChannelName() {
        return leadChannelName;
    }

    /**
	 * @param string
	 */
    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    /**
	 * @param string
	 */
    public void setDoctypeName(String doctypeName) {
        this.doctypeName = doctypeName;
    }

    /**
	 * @param string
	 */
    public void setLeadChannelName(String leadChannelName) {
        this.leadChannelName = leadChannelName;
    }
}
