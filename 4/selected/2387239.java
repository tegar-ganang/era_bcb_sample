package edu.harvard.iq.safe.saasystem.web.admin;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import edu.harvard.iq.safe.saasystem.util.ConfigFile;
import edu.harvard.iq.safe.saasystem.util.SAASConfigurationRegistryBean;
import edu.harvard.iq.safe.saasystem.web.util.SAASWebConstants;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

/**
 *
 * @author Akio Sone
 */
@ManagedBean(name = "systemConfigurationManagedBean")
@SessionScoped
public class SystemConfigurationManagedBean implements Serializable {

    static final String cl = SystemConfigurationManagedBean.class.getName();

    static final Logger logger = Logger.getLogger(cl);

    @EJB
    SAASConfigurationRegistryBean saasConfigRegistryBean;

    @EJB
    ConfigFile configFile;

    Properties saasConfigProperties;

    Properties saasAuditConfigProperties;

    CopyOnWriteArrayList<String> ipSet = new CopyOnWriteArrayList<String>();

    List<KeyValue> networkKV;

    List<KeyValue> auditKV;

    List<KeyValue> restrictedNetworkKV;

    List<KeyValue> restrictedAuditKV;

    XStream xstream = null;

    static Set<String> PROP_FILTER_NETWORK = new LinkedHashSet<String>();

    static Set<String> VIEW_ONLY_PROP_FILTER_NETWORK = new LinkedHashSet<String>();

    static Set<String> PROP_FILTER_AUDIT = new LinkedHashSet<String>();

    static Set<String> VIEW_ONLY_PROP_FILTER_AUDIT = new LinkedHashSet<String>();

    String timestampPattern = null;

    static String SAAS_TIME_STAMP_PATTERN = "yyyy-MM-dd'T'HH-mm-ss";

    static {
        PROP_FILTER_NETWORK.add("saas.daemonstatus.password");
        PROP_FILTER_NETWORK.add("saas.svnrepository.loginpassword");
        PROP_FILTER_NETWORK.add("saas.captcha.privatekey");
        VIEW_ONLY_PROP_FILTER_NETWORK.add("saas.daemonstatusdata.lastupdateattempt");
        VIEW_ONLY_PROP_FILTER_NETWORK.add("saas.daemonstatusdata.lastupdatesuccess");
        VIEW_ONLY_PROP_FILTER_NETWORK.add("saas.configproperties.lastsaved");
        VIEW_ONLY_PROP_FILTER_AUDIT.add("saas.auditconfigproperties.lastsaved");
        VIEW_ONLY_PROP_FILTER_NETWORK.add("saas.build.date");
        VIEW_ONLY_PROP_FILTER_NETWORK.add("saas.build.sourcefile.version");
        VIEW_ONLY_PROP_FILTER_NETWORK.add("saas.build.release.version");
    }

    boolean loggedAsAdministrator = false;

    public boolean isLoggedAsAdministrator() {
        return loggedAsAdministrator;
    }

    public void setLoggedAsAdministrator(boolean loggedAsAdministrator) {
        this.loggedAsAdministrator = loggedAsAdministrator;
    }

    String disabledStyleClassName = "";

    public String getDisabledStyleClassName() {
        return disabledStyleClassName;
    }

    public void setDisabledStyleClassName(String disabledStyleClassName) {
        this.disabledStyleClassName = disabledStyleClassName;
    }

    String disabledTitleText = "";

    public String getDisabledTitleText() {
        return disabledTitleText;
    }

    public void setDisabledTitleText(String disabledTitleText) {
        this.disabledTitleText = disabledTitleText;
    }

    /** Creates a new instance of SystemConfigurationManagedBean */
    public SystemConfigurationManagedBean() {
    }

    @PostConstruct
    public void initialize() {
        logger.log(Level.INFO, "+++++++++++ SystemConfigurationManagedBean: initalize() is called +++++++++++");
        xstream = new XStream(new JsonHierarchicalStreamDriver());
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.alias("SystemConfigurationManagedBean", SystemConfigurationManagedBean.class);
        networkKV = new ArrayList<KeyValue>();
        restrictedNetworkKV = new ArrayList<KeyValue>();
        saasConfigProperties = saasConfigRegistryBean.getSaasConfigProperties();
        logger.log(Level.INFO, "system config size={0}", saasConfigProperties.size());
        for (String key : saasConfigProperties.stringPropertyNames()) {
            if (!VIEW_ONLY_PROP_FILTER_NETWORK.contains(key)) {
                networkKV.add(new KeyValue(key, saasConfigProperties.getProperty(key)));
            }
            if (!PROP_FILTER_NETWORK.contains(key)) {
                restrictedNetworkKV.add(new KeyValue(key, saasConfigProperties.getProperty(key)));
            }
        }
        logger.log(Level.INFO, "networkKV size={0}", networkKV.size());
        Collections.sort(networkKV);
        Collections.sort(restrictedNetworkKV);
        auditKV = new ArrayList<KeyValue>();
        restrictedAuditKV = new ArrayList<KeyValue>();
        saasAuditConfigProperties = saasConfigRegistryBean.getSaasAuditConfigProperties();
        logger.log(Level.INFO, "audit config size={0}", saasAuditConfigProperties.size());
        for (String key : saasAuditConfigProperties.stringPropertyNames()) {
            if (!VIEW_ONLY_PROP_FILTER_AUDIT.contains(key)) {
                auditKV.add(new KeyValue(key, saasAuditConfigProperties.getProperty(key)));
            }
            if (!PROP_FILTER_AUDIT.contains(key)) {
                restrictedAuditKV.add(new KeyValue(key, saasAuditConfigProperties.getProperty(key)));
            }
        }
        logger.log(Level.INFO, "auditKV size={0}", auditKV.size());
        Collections.sort(auditKV);
        Collections.sort(restrictedAuditKV);
        ipSet.addAll(saasConfigRegistryBean.getIpSet());
        logger.log(Level.INFO, "ip set={0}", ipSet.size());
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        logger.log(Level.INFO, "loggedInUser ={0}", request.getUserPrincipal());
        if (request.isUserInRole("Administrator")) {
            setLoggedAsAdministrator(true);
            logger.log(Level.INFO, "logged in as Administrator role");
        } else {
            logger.log(Level.INFO, "not logged in as Administrator");
            setDisabledStyleClassName("disabled");
            setDisabledTitleText("Your login credential-level is not high enough to use this functionality.");
        }
        logger.log(Level.INFO, "daemonstatusdata.lastupdateattempt={0}", saasConfigRegistryBean.getConfigProperty("saas.daemonstatusdata.lastupdateattempt"));
        logger.log(Level.INFO, "daemonstatusdata.lastupdatesuccess={0}", saasConfigRegistryBean.getConfigProperty("saas.daemonstatusdata.lastupdatesuccess"));
        timestampPattern = configFile.getTimestampPattern();
        if (StringUtils.isBlank(timestampPattern)) {
            timestampPattern = SAASWebConstants.SAAS_TIME_STAMP_PATTERN_FOR_WEB_PAGE;
        }
        logger.log(Level.INFO, "+++++++++++ SystemConfigurationManagedBean: initalize() end +++++++++++");
    }

    public Properties getSaasAuditConfigProperties() {
        return saasConfigRegistryBean.getSaasAuditConfigProperties();
    }

    public List<String> getSaasAuditConfigPropertiesKeys() {
        CopyOnWriteArrayList<String> temp = new CopyOnWriteArrayList<String>();
        temp.addAll(saasConfigRegistryBean.getSaasAuditConfigProperties().stringPropertyNames());
        return temp;
    }

    public Properties getSaasConfigProperties() {
        return saasConfigRegistryBean.getSaasConfigProperties();
    }

    public List<String> getSaasConfigPropertiesKeys() {
        CopyOnWriteArrayList<String> temp = new CopyOnWriteArrayList<String>();
        temp.addAll(saasConfigRegistryBean.getSaasConfigProperties().stringPropertyNames());
        return temp;
    }

    public CopyOnWriteArrayList<String> getIpSet() {
        return ipSet;
    }

    public List<KeyValue> getAuditKV() {
        return auditKV;
    }

    public void setAuditKV(List<KeyValue> auditKV) {
        this.auditKV = auditKV;
    }

    public List<KeyValue> getNetworkKV() {
        return networkKV;
    }

    public void setNetworkKV(List<KeyValue> networkKV) {
        this.networkKV = networkKV;
    }

    public List<KeyValue> getRestrictedAuditKV() {
        return restrictedAuditKV;
    }

    public void setRestrictedAuditKV(List<KeyValue> restrictedAuditKV) {
        this.restrictedAuditKV = restrictedAuditKV;
    }

    public List<KeyValue> getRestrictedNetworkKV() {
        return restrictedNetworkKV;
    }

    public void setRestrictedNetworkKV(List<KeyValue> restrictedNetworkKV) {
        this.restrictedNetworkKV = restrictedNetworkKV;
    }

    KeyValue propN = new KeyValue("enter key here", "enter value here");

    public KeyValue getPropN() {
        return propN;
    }

    public void setPropN(KeyValue propN) {
        this.propN = propN;
    }

    KeyValue propA = new KeyValue("enter key here", "enter value here");

    public KeyValue getPropA() {
        return propA;
    }

    public void setPropA(KeyValue propA) {
        this.propA = propA;
    }

    public String reinitPropN() {
        propN = new KeyValue("enter key here", "enter value here");
        return null;
    }

    public String reinitPropA() {
        propA = new KeyValue("enter key here", "enter value here");
        return null;
    }

    public void refreshRestrictedNetworkKV() {
        restrictedNetworkKV.clear();
        for (String key : saasConfigRegistryBean.getSaasConfigProperties().stringPropertyNames()) {
            if (!PROP_FILTER_NETWORK.contains(key)) {
                restrictedNetworkKV.add(new KeyValue(key, saasConfigRegistryBean.getSaasConfigProperties().getProperty(key)));
            }
        }
    }

    public void refreshRestrictedAuditKV() {
        restrictedAuditKV.clear();
        for (String key : saasConfigRegistryBean.getSaasAuditConfigProperties().stringPropertyNames()) {
            if (!PROP_FILTER_AUDIT.contains(key)) {
                restrictedAuditKV.add(new KeyValue(key, saasConfigRegistryBean.getSaasAuditConfigProperties().getProperty(key)));
            }
        }
    }

    public void saveNetworkKV(ActionEvent actionEvent) {
        logger.log(Level.INFO, "---------------------- saveNetworkKV starts ---------------------");
        logger.log(Level.INFO, "network key-value(new)={0}", xstream.toXML(networkKV));
        Properties newNetworkProp = new Properties();
        for (KeyValue ky : networkKV) {
            String tmpV = StringUtils.trim(ky.value);
            if (tmpV == null) {
                tmpV = "";
            }
            newNetworkProp.put(StringUtils.trim(ky.key), tmpV);
        }
        saasConfigRegistryBean.setSaasConfigProperties(newNetworkProp);
        logger.log(Level.INFO, "SaasConfigProperties has been updated");
        String networkPropFileName = saasConfigRegistryBean.getSassLocalConfigFileName();
        saasConfigRegistryBean.saveNetworkSettings(networkPropFileName);
        String savedTime = saasConfigRegistryBean.getSaasConfigProperties().getProperty("saas.configproperties.lastsaved");
        for (KeyValue ky : networkKV) {
            if (ky.getKey().equals("saas.configproperties.lastsaved")) {
                ky.value = savedTime;
            }
        }
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Saving Network Properties Finished: ", "saved at " + savedTime));
        refreshRestrictedNetworkKV();
        logger.log(Level.INFO, "---------------------- saveNetworkKV ends ---------------------");
    }

    public void saveAuditKV(ActionEvent actionEvent) {
        logger.log(Level.INFO, "---------------------- saveAuditKV starts ---------------------");
        logger.log(Level.INFO, "audit={0}", xstream.toXML(auditKV));
        Properties newAuditProp = new Properties();
        for (KeyValue ky : auditKV) {
            String tmpV = StringUtils.trim(ky.value);
            if (tmpV == null) {
                tmpV = "";
            }
            newAuditProp.put(StringUtils.trim(ky.key), tmpV);
        }
        saasConfigRegistryBean.setSaasAuditConfigProperties(newAuditProp);
        logger.log(Level.INFO, "SaasAuditConfigProperties has been updated");
        String auditPropFileName = saasConfigRegistryBean.getSaasAuditConfigFileName();
        saasConfigRegistryBean.saveAuditSettings(auditPropFileName);
        String savedTime = saasConfigRegistryBean.getSaasAuditConfigProperties().getProperty("saas.auditconfigproperties.lastsaved");
        for (KeyValue ky : auditKV) {
            if (ky.getKey().equals("saas.auditconfigproperties.lastsaved")) {
                ky.value = savedTime;
            }
        }
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Saving Audit Properties Finished: ", "saved at " + savedTime));
        refreshRestrictedAuditKV();
        logger.log(Level.INFO, "---------------------- saveAuditKV ends ---------------------");
    }

    public void savePropertiesFile(Properties newProp, File newPropFileName) {
        String writeTimestamp = DateFormatUtils.format(new java.util.Date(), timestampPattern);
        OutputStream os = null;
        try {
            os = new FileOutputStream(newPropFileName);
            newProp.storeToXML(os, "new set of properties saved at:" + writeTimestamp, "UTF-8");
        } catch (IOException e) {
            logger.severe("failed to save new data in the config-file");
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "failed to close the config file", ex);
                }
            }
        }
    }

    public boolean backupFile(File oldFile, File newFile) {
        boolean isBkupFileOK = false;
        FileChannel sourceChannel = null;
        FileChannel targetChannel = null;
        try {
            sourceChannel = new FileInputStream(oldFile).getChannel();
            targetChannel = new FileOutputStream(newFile).getChannel();
            targetChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IO exception occurred while copying file", e);
        } finally {
            if ((newFile != null) && (newFile.exists()) && (newFile.length() > 0)) {
                isBkupFileOK = true;
            }
            try {
                if (sourceChannel != null) {
                    sourceChannel.close();
                }
                if (targetChannel != null) {
                    targetChannel.close();
                }
            } catch (IOException e) {
                logger.log(Level.INFO, "closing channels failed");
            }
        }
        return isBkupFileOK;
    }

    public String prepareEditNetworkSettingsPage() {
        return "EditNetworkSettings.xhtml?faces-redirect=true";
    }

    public String prepareEditAuditSettingsPage() {
        return "EditAuditSettings.xhtml?faces-redirect=true";
    }
}
