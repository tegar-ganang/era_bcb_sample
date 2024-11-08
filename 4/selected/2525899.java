package edu.harvard.iq.safe.saasystem.web.auditschema;

import java.io.*;
import javax.faces.bean.*;
import java.util.*;
import java.util.logging.*;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.*;
import edu.harvard.iq.safe.saasystem.util.*;
import edu.harvard.iq.safe.saasystem.auditschema.*;
import edu.harvard.iq.safe.saasystem.ejb.*;
import edu.harvard.iq.safe.saasystem.entities.*;
import edu.harvard.iq.safe.saasystem.versioning.SAASSubversionServiceBean;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.security.Principal;
import javax.ejb.EJB;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.tmatesoft.svn.core.SVNCommitInfo;

/**
 *
 * @author Akio Sone
 */
@ManagedBean(name = "editAuditSchemaMB")
@RequestScoped
public class EditAuditSchemaInstance implements Serializable {

    static final Logger logger = Logger.getLogger(EditAuditSchemaInstance.class.getName());

    @EJB
    ConfigFile configFile;

    @EJB
    SAASConfigurationRegistryBean saasConfigurationRegistry;

    @EJB
    LockssBoxTableFacade lockssBoxTableFacade;

    @EJB
    ArchivalUnitStatusTableFacade archivalUnitFacade;

    @EJB
    SAASSubversionServiceBean saasSubversionServiceBean;

    @EJB
    AuAttributesFacade auAttributesFacade;

    @EJB
    RegionCodeLabelFacade regionCodeLabelFacade;

    @EJB
    GeographicDataServiceBean geographicDataServiceBean;

    @EJB
    HostAttributesFacade hostAttributesFacade;

    @EJB
    OwnerDataServiceBean ownerDataServiceBean;

    @EJB
    SubjectDataServiceBean subjectDataServiceBean;

    AuditSchemaInstance auditSchemaInstance;

    List<LockssBoxTable> lockssBoxes = null;

    List<Host> excludedHosts = new ArrayList<Host>();

    static double SAAS_AU_SIZE_MAX = 1024;

    double saasAuSizeMax;

    static long NUMBER_REPLICATES = 3;

    static int VERIFICATION_FREQUENCY = 21;

    static int REPLICATION_DURATION = 21;

    static int UPDATE_FREQUENCY = 7;

    static int REPORT_INTERVAL = 7;

    static String AUDIT_REOIRT_INTERVAL_ORIGIN = "LAST";

    static Map<String, Object> auditReportDurationOptions = new LinkedHashMap<String, Object>();

    static String GEOUNITCODE_TO_LABEL_FILENAME = "us-state-code-to-label.properties";

    static int GEO_REDUNDANCY = 2;

    String geounitcodeToLabelFilename = null;

    Properties geounitcodeToLabelProps = new Properties();

    Map<String, Object> GEO_LOCATION = new TreeMap<String, Object>();

    Map<String, Object> AU_SUBJECT = new TreeMap<String, Object>();

    Map<String, Object> AU_OWNER = new TreeMap<String, Object>();

    static final double POWER_FOR_STORAGE_UNIT = 30D;

    static double GIG_DIVIDER = Math.pow(2D, POWER_FOR_STORAGE_UNIT);

    long numberReplicates;

    int verificationFrequency;

    int replicationCycle;

    int updateFrequency;

    int reportInterval;

    String auditReportIntervalOrigin = null;

    String geographicCoding = "from config file";

    String geographicSummaryScheme = "from config file";

    String subjectSummaryScheme = "from config file";

    String ownerInstSummaryScheme = "from config file";

    String currentLoggedInUser = null;

    int maxRegions = 0;

    int geoRedudancy;

    String ownerOtionSetFile = null;

    String subjectOtionSetFile = null;

    static boolean SAAS_AU_GROUP_POLICY_ENABLED = false;

    boolean auGroupPolicyEnabled;

    Map<String, String> ipAddressToGeographicLocationTable = null;

    Map<String, String> geounitcodeToLabelMap = null;

    int plnQuorum = 5;

    int plnMemberHosts = 0;

    int crawlCycle = 14;

    static String SAAS_SUBJECT_PLACEHOLDER = "-unclassified-";

    static String SAAS_OWNER_INST_PLACEHOLDER = "-unspecified-";

    static String SAAS_GEO_LOCATION_PLACEHOLDER = "-OTHER-";

    static String EMAIL_ADDRESS_VALIDATION_REGEX = "[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?";

    static {
    }

    XStream xstream = new XStream(new JsonHierarchicalStreamDriver());

    @PostConstruct
    public void initialize() {
        logger.info("+++++ EditAuditSchema: postConstruct step: start ++++++++");
        if (StringUtils.isNotBlank(saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.au.size.max"))) {
            try {
                saasAuSizeMax = Double.parseDouble(saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.au.size.max"));
            } catch (NumberFormatException e) {
                logger.warning("The user-specified value cannot be converted to an integer:" + " value=" + saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.au.size.max"));
                saasAuSizeMax = SAAS_AU_SIZE_MAX;
                logger.info("Use the default value:" + saasAuSizeMax);
            } finally {
                if (saasAuSizeMax <= 0) {
                    logger.warning("the MAX value must be positive");
                    saasAuSizeMax = SAAS_AU_SIZE_MAX;
                    logger.info("Use the default value:" + saasAuSizeMax);
                }
            }
        } else {
            saasAuSizeMax = SAAS_AU_SIZE_MAX;
        }
        if (StringUtils.isNotBlank(saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.au.grouppolicy.enabled"))) {
            String rawString = saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.au.grouppolicy.enabled").trim();
            if (rawString.equalsIgnoreCase("true")) {
                auGroupPolicyEnabled = true;
            } else {
                auGroupPolicyEnabled = SAAS_AU_GROUP_POLICY_ENABLED;
            }
        } else {
            auGroupPolicyEnabled = SAAS_AU_GROUP_POLICY_ENABLED;
        }
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.alias("EditAuditSchemaInstance", EditAuditSchemaInstance.class);
        if (StringUtils.isNotBlank(saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.au.numberreplicates"))) {
            numberReplicates = Long.parseLong(saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.au.numberreplicates"));
        } else {
            numberReplicates = NUMBER_REPLICATES;
        }
        if (StringUtils.isNotBlank(saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.au.verificationfrequency"))) {
            verificationFrequency = Integer.parseInt(saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.au.verificationfrequency"));
        } else {
            verificationFrequency = VERIFICATION_FREQUENCY;
        }
        if (StringUtils.isNotBlank(saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.au.replicationduration"))) {
            replicationCycle = Integer.parseInt(saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.au.replicationduration"));
        } else {
            replicationCycle = REPLICATION_DURATION;
        }
        if (StringUtils.isNotBlank(saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.au.updatefrequency"))) {
            updateFrequency = Integer.parseInt(saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.au.updatefrequency"));
        } else {
            updateFrequency = UPDATE_FREQUENCY;
        }
        if (StringUtils.isNotBlank(saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.report.interval"))) {
            reportInterval = Integer.parseInt(saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.report.interval"));
        } else {
            reportInterval = UPDATE_FREQUENCY;
        }
        GEO_LOCATION = geographicDataServiceBean.getGeounitcodeToLabelTable();
        ipAddressToGeographicLocationTable = hostAttributesFacade.getIpAddressToGeographicLocationTable();
        logger.fine("ipAddressToGeographicLocationTable" + xstream.toXML(ipAddressToGeographicLocationTable));
        geounitcodeToLabelMap = geographicDataServiceBean.getGeounitcodeToLabelMap();
        logger.fine("geounitcodeToLabelTable" + xstream.toXML(geounitcodeToLabelMap));
        if (StringUtils.isNotBlank(saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.report.interval.origin"))) {
            auditReportIntervalOrigin = saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.report.interval.origin");
            logger.info("origin datum is specified:" + auditReportIntervalOrigin);
        } else {
            logger.info("origin datum is not specified; use the default[LAST]");
            auditReportIntervalOrigin = AUDIT_REOIRT_INTERVAL_ORIGIN;
        }
        logger.info("auditReportIntervalOrigin=" + auditReportIntervalOrigin);
        if (auditReportIntervalOrigin.toUpperCase().equals("FIRST")) {
            auditReportDurationOptions.put("Daily", new Integer("1"));
            auditReportDurationOptions.put("Weekly: Monday", new Integer("7"));
            auditReportDurationOptions.put("Biweekly: 1st/15th days", new Integer("14"));
            auditReportDurationOptions.put("Monthly: 1st day", new Integer("30"));
            auditReportDurationOptions.put("Bimonthly: Jan, Mar, May, Jul, Sep, Nov", new Integer("60"));
            auditReportDurationOptions.put("Quarterly: Jan, Apr, Jul, Oct", new Integer("90"));
            auditReportDurationOptions.put("Biannual: Jan, Jul", new Integer("180"));
            auditReportDurationOptions.put("Annual: Dec", new Integer("365"));
        } else if (auditReportIntervalOrigin.toUpperCase().equals("LAST")) {
            auditReportDurationOptions.put("Daily", new Integer("1"));
            auditReportDurationOptions.put("Weekly: Sunday", new Integer("7"));
            auditReportDurationOptions.put("Biweekly: 14th/last days", new Integer("14"));
            auditReportDurationOptions.put("Monthly: last day", new Integer("30"));
            auditReportDurationOptions.put("Bimonthly: Feb, Apr, Jun, Aug, Oct, Dec", new Integer("60"));
            auditReportDurationOptions.put("Quarterly: Mar, Jun, Sep, Dec", new Integer("90"));
            auditReportDurationOptions.put("Biannual: Jun, Dec", new Integer("180"));
            auditReportDurationOptions.put("Annual: Dec", new Integer("365"));
        }
        AU_OWNER = ownerDataServiceBean.getAuOwners();
        AU_SUBJECT = subjectDataServiceBean.getAuSubjects();
        SAASAuditSchemaDOMParser auditSchemaDOMParser = new SAASAuditSchemaDOMParser();
        try {
            auditSchemaInstance = auditSchemaDOMParser.read(configFile.getAuditSchemaFile());
            hosts = auditSchemaInstance.getHosts();
            selectedHosts = auditSchemaInstance.getHosts().toArray(new Host[auditSchemaInstance.getHosts().size()]);
            Set<String> readbacIpSet = new TreeSet<String>();
            for (Host host : hosts) {
                readbacIpSet.add(host.getHostIpAddress());
            }
            if (lockssBoxes == null) {
                lockssBoxes = lockssBoxTableFacade.findAll();
                logger.info("lockssBoxes has been filled: size=" + lockssBoxes.size());
            }
            List<LockssBoxTable> lockssBoxesAllLatest = lockssBoxTableFacade.findAll();
            logger.info("lockssBoxes has been filled: size=" + lockssBoxesAllLatest.size());
            Set<String> IpsFromDB = new TreeSet<String>();
            for (LockssBoxTable lb : lockssBoxesAllLatest) {
                IpsFromDB.add(lb.getIpAddress());
            }
            IpsFromDB.removeAll(readbacIpSet);
            logger.info("Ips from DB not found in asi.xml:size=" + IpsFromDB.size());
            if (!IpsFromDB.isEmpty()) {
                for (LockssBoxTable lb : lockssBoxesAllLatest) {
                    if (IpsFromDB.contains(lb.getIpAddress())) {
                        Double repoSize = lb.getTotalRepoSize() / Math.pow(1024D, 3D);
                        logger.info("ip address for this row=" + lb.getIpAddress());
                        if (ipAddressToGeographicLocationTable.containsKey(lb.getIpAddress())) {
                            hosts.add(new Host(lb.getHostName(), lb.getIpAddress(), repoSize, repoSize.longValue(), geounitcodeToLabelMap.get(ipAddressToGeographicLocationTable.get(lb.getIpAddress())).toString()));
                        } else {
                            hosts.add(new Host(lb.getHostName(), lb.getIpAddress(), repoSize, repoSize.longValue(), SAAS_GEO_LOCATION_PLACEHOLDER));
                        }
                    }
                }
            }
            for (LockssBoxTable lb : lockssBoxes) {
                if (!readbacIpSet.contains(lb.getIpAddress())) {
                    Double repoSize = lb.getTotalRepoSize() / Math.pow(1024D, 3D);
                    excludedHosts.add(new Host(lb.getHostName(), lb.getIpAddress(), repoSize, repoSize.longValue()));
                }
            }
            maxRegions = regionCodeLabelFacade.getMaximumRegions() <= hosts.size() ? regionCodeLabelFacade.getMaximumRegions() : hosts.size();
            geoRedudancy = GEO_REDUNDANCY;
            if (geoRedudancy <= maxRegions) {
                logger.info("geographic redundancy value is equal or " + "less than the maximum " + "number of regions");
            } else {
                geoRedudancy = maxRegions;
            }
            numberReplicates = numberReplicates <= hosts.size() ? numberReplicates : hosts.size();
            aus = auditSchemaInstance.getAus();
            rawAus.addAll(0, aus);
            logger.info("number of aus (before adjustment)=" + aus.size());
            List<ArchivalUnitStatusTable> auAllLatest = archivalUnitFacade.findUniqueAu();
            Set<String> auIdsFromDB = new TreeSet<String>();
            for (ArchivalUnitStatusTable austbl : auAllLatest) {
                auIdsFromDB.add(austbl.getAuId());
            }
            Set<String> auIdsFromXML = new TreeSet<String>();
            for (ArchivalUnit au : auditSchemaInstance.getAus()) {
                auIdsFromXML.add(au.getAuId());
            }
            auIdsFromDB.removeAll(auIdsFromXML);
            logger.info("au Ids from DB not found in asi.xml:size=" + auIdsFromDB.size());
            if (auIdsFromDB != null && !auIdsFromDB.isEmpty()) {
                for (ArchivalUnitStatusTable au : auAllLatest) {
                    if (auIdsFromDB.contains(au.getAuId())) {
                        logger.info("auSize=" + au.getAuSize());
                        BigInteger auSizeInGig = getAuSizeInGig(au.getAuSize());
                        logger.info("auSizeInGig=" + auSizeInGig);
                        if (auSizeInGig != null) {
                            String subject = SAAS_SUBJECT_PLACEHOLDER;
                            String ownerInst = SAAS_OWNER_INST_PLACEHOLDER;
                            List<AuAttributes> auAttributesList = auAttributesFacade.findAuAttributesByAuName(au.getAuName());
                            if (auAttributesList != null && !auAttributesList.isEmpty()) {
                                if (auAttributesList.size() > 1) {
                                    logger.warning("non-unique au-names were found for " + au.getAuName());
                                }
                                AuAttributes auAttRow = auAttributesList.get(0);
                                if (StringUtils.isNotBlank(auAttRow.getOwnerInst())) {
                                    ownerInst = auAttRow.getOwnerInst();
                                }
                                if (StringUtils.isNotBlank(auAttRow.getSubject())) {
                                    subject = auAttRow.getSubject();
                                }
                            }
                            aus.add(new ArchivalUnit(au.getAuName(), numberReplicates, verificationFrequency, replicationCycle, updateFrequency, auSizeInGig, au.getAuId(), subject, ownerInst, geoRedudancy));
                        } else {
                            String subject = SAAS_SUBJECT_PLACEHOLDER;
                            String ownerInst = SAAS_OWNER_INST_PLACEHOLDER;
                            aus.add(new ArchivalUnit(au.getAuName(), numberReplicates, verificationFrequency, replicationCycle, updateFrequency, BigInteger.ONE, au.getAuId(), subject, ownerInst, geoRedudancy));
                        }
                    }
                }
            } else {
            }
            logger.info("number of aus (after adjustment)=" + aus.size());
            List<ArchivalUnit> workAus = new ArrayList<ArchivalUnit>(aus.size());
            workAus.addAll(0, rawAus);
            selectedAus = workAus.toArray(new ArchivalUnit[workAus.size()]);
            logger.info("The contents of the read-back au=selectedAus\n" + xstream.toXML(selectedAus));
            auditSchemaInstanceVersion = auditSchemaInstance.getAudit().getSchemaVersion();
            auditSchemaInstanceId = auditSchemaInstance.getAudit().getAuditId();
            auditReportEmail = auditSchemaInstance.getAudit().getAuditReportEmail();
            auditReportInterval = auditSchemaInstance.getAudit().getAuditReportInterval();
            groupName = auditSchemaInstance.getNewtwork().getGroupName();
            netAdminEmail = auditSchemaInstance.getNewtwork().getNetAdminEmail();
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, "audit schema instance file was not found", ex);
        }
        if (StringUtils.isNotBlank(saasConfigurationRegistry.getSaasConfigProperties().getProperty("saas.audit.pln.config.quorum"))) {
            plnQuorum = Integer.parseInt(saasConfigurationRegistry.getSaasConfigProperties().getProperty("saas.audit.pln.config.quorum"));
        }
        if (StringUtils.isNotBlank(saasConfigurationRegistry.getSaasConfigProperties().getProperty("saas.ip.fromlockssxml"))) {
            String ipList = saasConfigurationRegistry.getSaasConfigProperties().getProperty("saas.ip.fromlockssxml");
            int ipListSize = ipList.split(",").length;
            plnMemberHosts = ipListSize;
        } else {
            plnMemberHosts = lockssBoxes.size();
        }
        logger.info("+++++ EditAuditSchema: postConstruct step: end ++++++++");
    }

    List<Host> hosts = new ArrayList<Host>();

    List<ArchivalUnit> rawAus = new ArrayList<ArchivalUnit>();

    public List<ArchivalUnit> getRawAus() {
        return rawAus;
    }

    public void setRawAus(List<ArchivalUnit> rawAus) {
        this.rawAus = rawAus;
    }

    List<ArchivalUnit> aus = new ArrayList<ArchivalUnit>();

    public List<Host> getHosts() {
        return hosts;
    }

    public void setHosts(List<Host> hosts) {
        this.hosts = hosts;
    }

    public List<ArchivalUnit> getAus() {
        return aus;
    }

    public void setAus(List<ArchivalUnit> aus) {
        this.aus = aus;
    }

    ArchivalUnit[] selectedAus;

    public ArchivalUnit[] getSelectedAus() {
        return selectedAus;
    }

    public void setSelectedAus(ArchivalUnit[] selectedAus) {
        this.selectedAus = selectedAus;
    }

    Host[] selectedHosts;

    public Host[] getSelectedHosts() {
        return selectedHosts;
    }

    public void setSelectedHosts(Host[] selectedHosts) {
        this.selectedHosts = selectedHosts;
    }

    /** Creates a new instance of EditAuditSchemaInstance */
    public EditAuditSchemaInstance() {
    }

    protected String groupName;

    /**
     * Get the value of groupName
     *
     * @return the value of groupName
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Set the value of groupName
     *
     * @param groupName new value of groupName
     */
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    protected String netAdminEmail;

    /**
     * Get the value of netAdminEmail
     *
     * @return the value of netAdminEmail
     */
    public String getNetAdminEmail() {
        return netAdminEmail;
    }

    /**
     * Set the value of netAdminEmail
     *
     * @param netAdminEmail new value of netAdminEmail
     */
    public void setNetAdminEmail(String netAdminEmail) {
        this.netAdminEmail = netAdminEmail;
    }

    protected String auVerifiedReplicas;

    /**
     * Get the value of auVerifiedReplicas
     *
     * @return the value of auVerifiedReplicas
     */
    public String getAuVerifiedReplicas() {
        return auVerifiedReplicas;
    }

    /**
     * Set the value of auVerifiedReplicas
     *
     * @param auVerifiedReplicas new value of auVerifiedReplicas
     */
    public void setAuVerifiedReplicas(String auVerifiedReplicas) {
        this.auVerifiedReplicas = auVerifiedReplicas;
    }

    protected String lastSuccPollEnd;

    /**
     * Get the value of lastSuccPollEnd
     *
     * @return the value of lastSuccPollEnd
     */
    public String getLastSuccPollEnd() {
        return lastSuccPollEnd;
    }

    /**
     * Set the value of lastSuccPollEnd
     *
     * @param lastSuccPollEnd new value of lastSuccPollEnd
     */
    public void setLastSuccPollEnd(String lastSuccPollEnd) {
        this.lastSuccPollEnd = lastSuccPollEnd;
    }

    protected String crawlDuration;

    /**
     * Get the value of crawlDuration
     *
     * @return the value of crawlDuration
     */
    public String getCrawlDuration() {
        return crawlDuration;
    }

    /**
     * Set the value of crawlDuration
     *
     * @param crawlDuration new value of crawlDuration
     */
    public void setCrawlDuration(String crawlDuration) {
        this.crawlDuration = crawlDuration;
    }

    protected String replicationDuration;

    /**
     * Get the value of replicationDuration
     *
     * @return the value of replicationDuration
     */
    public String getReplicationDuration() {
        return replicationDuration;
    }

    /**
     * Set the value of replicationDuration
     *
     * @param replicationDuration new value of replicationDuration
     */
    public void setReplicationDuration(String replicationDuration) {
        this.replicationDuration = replicationDuration;
    }

    protected int auditReportInterval;

    /**
     * Get the value of auditReportInterval
     *
     * @return the value of auditReportInterval
     */
    public int getAuditReportInterval() {
        return auditReportInterval;
    }

    /**
     * Set the value of auditReportInterval
     *
     * @param auditReportInterval new value of auditReportInterval
     */
    public void setAuditReportInterval(int auditReportInterval) {
        this.auditReportInterval = auditReportInterval;
    }

    protected String auditReportEmail;

    /**
     * Get the value of auditReportEmail
     *
     * @return the value of auditReportEmail
     */
    public String getAuditReportEmail() {
        return auditReportEmail;
    }

    /**
     * Set the value of auditReportEmail
     *
     * @param auditReportEmail new value of auditReportEmail
     */
    public void setAuditReportEmail(String auditReportEmail) {
        this.auditReportEmail = auditReportEmail;
    }

    protected String auId;

    /**
     * Get the value of auId
     *
     * @return the value of auId
     */
    public String getAuId() {
        return auId;
    }

    /**
     * Set the value of auId
     *
     * @param auId new value of auId
     */
    public void setAuId(String auId) {
        this.auId = auId;
    }

    /**
     *
     */
    protected String auditSchemaInstanceVersion;

    /**
     *
     * @return
     */
    public String getAuditSchemaInstanceVersion() {
        return auditSchemaInstanceVersion;
    }

    /**
     *
     * @param auditSchemaInstanceVersion
     */
    public void setAuditSchemaInstanceVersion(String auditSchemaInstanceVersion) {
        this.auditSchemaInstanceVersion = auditSchemaInstanceVersion;
    }

    /**
     *
     */
    protected String auditSchemaInstanceId;

    /**
     *
     * @return
     */
    public String getAuditSchemaInstanceId() {
        return auditSchemaInstanceId;
    }

    /**
     *
     * @param auditSchemaInstanceId
     */
    public void setAuditSchemaInstanceId(String auditSchemaInstanceId) {
        this.auditSchemaInstanceId = auditSchemaInstanceId;
    }

    public Map<String, Object> getAuditReportDurationOptions() {
        return auditReportDurationOptions;
    }

    public void setAuditReportDurationOptions(Map<String, Object> auditReportDurationOptions) {
        CreateAuditSchemaInstance.auditReportDurationOptions = auditReportDurationOptions;
    }

    public void addExcludedHosts(ActionEvent actionEvent) {
        String returnedMssage = null;
        returnedMssage = excludedHosts.size() + " hosts were added.";
        logger.info("current host size=" + hosts.size());
        hosts.addAll(excludedHosts);
        logger.info("new host size=" + hosts.size());
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Previously excluded hosts were added", returnedMssage));
    }

    public void update(ActionEvent actionEvent) {
        logger.info("\n+++++++++++++++ EditAuditSchemaInstance: within update() +++++++++++++++++++++");
        logger.info("selected hosts:" + (selectedHosts.length));
        logger.info("selected aus:" + (selectedAus.length));
        logger.info("groupName=" + groupName);
        logger.info("auditReportEmail=" + auditReportEmail);
        logger.info("netAdminEmail=" + netAdminEmail);
        logger.info("auditReportInterval=" + auditReportInterval);
        logger.info("selectedHosts=" + xstream.toXML(selectedHosts));
        logger.info("selectedAus=" + xstream.toXML(selectedAus));
        auditSchemaInstanceId = "asiId_" + RandomStringUtils.randomAlphanumeric(4);
        logger.info("auditSchemaInstanceId to be issued=" + auditSchemaInstanceId);
        AuditSchemaInstance auditSchema = convertToAuditSchema(auditSchemaInstanceId);
        File lastVersion = new File(configFile.getAuditSchemaFile());
        String msgToken = null;
        try {
            boolean bkupResult = false;
            if (lastVersion.exists()) {
                bkupResult = backupLastAuditSchema(lastVersion);
            } else {
                bkupResult = true;
            }
            if (bkupResult) {
                auditSchema.write(configFile.getAuditSchemaFile());
            } else {
                logger.warning("last audit schema file was not saved");
            }
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, "AuditSchemaFile was not found", ex);
        }
        File newAuditSchemaFile = new File(configFile.getAuditSchemaFile());
        if ((newAuditSchemaFile != null) && (newAuditSchemaFile.exists()) && (newAuditSchemaFile.length() > 0)) {
            msgToken = "The audit schema instance was successfully updated: ID=" + auditSchemaInstanceId;
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Update action succeeded", msgToken));
        } else {
            msgToken = "The audit schema instance was not updated: please try again";
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Update action Failed", msgToken));
            return;
        }
        logger.info("\ncommit this audit schema instance to the subversion repository");
        String pathToFile = configFile.getInputFilesSvnPath();
        String fileName = configFile.getAuditSchemaFileName();
        String newFile = configFile.getAuditSchemaFile();
        String logMessage = auditSchema.getVersionControlLogMessage(configFile.getTimestampPattern());
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        Principal loginUser = request.getUserPrincipal();
        currentLoggedInUser = loginUser.getName();
        StringBuilder sb = new StringBuilder(logMessage);
        sb.append("; saved by " + currentLoggedInUser + ";");
        logger.info("\nsvn logMessage=" + sb.toString());
        SVNCommitInfo result = saasSubversionServiceBean.commit(pathToFile, fileName, newFile, sb.toString());
        logger.info("result:\n" + xstream.toXML(result));
        if (result != null) {
            logger.info("returned commit info:" + result.getNewRevision() + "\nmessage=" + result.getErrorMessage());
        } else {
            logger.warning("svn commit failed");
        }
        logger.info("\n+++++++++++++++ EditAuditSchemaInstance: update(): end +++++++++++++++++++++");
        return;
    }

    /**
     *
     * @return
     */
    public AuditSchemaInstance convertToAuditSchema(String auditSchemaInstanceId) {
        AuditSchemaInstance auditSchema = null;
        Audit audit = new Audit(auditSchemaInstanceId, auditSchemaInstanceVersion, auditReportEmail, auditReportInterval, geographicSummaryScheme, subjectSummaryScheme, ownerInstSummaryScheme, maxRegions);
        Network network = new Network(groupName, netAdminEmail);
        auditSchema = new AuditSchemaInstance(audit, network, Arrays.asList(selectedHosts), Arrays.asList(selectedAus));
        return auditSchema;
    }

    public boolean backupLastAuditSchema(File lastAuditSchema) {
        boolean isBkupFileOK = false;
        String writeTimestamp = DateFormatUtils.format(new java.util.Date(), configFile.getTimestampPattern());
        File target = new File(configFile.getAuditSchemaFileDir() + File.separator + configFile.getAuditSchemaFileName() + ".bkup_" + writeTimestamp);
        FileChannel sourceChannel = null;
        FileChannel targetChannel = null;
        try {
            sourceChannel = new FileInputStream(lastAuditSchema).getChannel();
            targetChannel = new FileOutputStream(target).getChannel();
            targetChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IO exception occurred while copying file", e);
        } finally {
            if ((target != null) && (target.exists()) && (target.length() > 0)) {
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
                logger.warning("closing channels failed");
            }
        }
        return isBkupFileOK;
    }

    public String prepareList() {
        return "ListAuditSchemaInstances.xhtml?faces-redirect=true";
    }

    public String gotoSchemaInstanceList() {
        return "ListAuditSchemaInstances.xhtml?faces-redirect=true";
    }

    public String gotoEditOptionSetForSubject() {
        return "EditAuditSchemaOptionSetForSubject.xhtml?faces-redirect=true";
    }

    public String gotoEditOptionSetForOwner() {
        return "EditAuditSchemaOptionSetForOwner.xhtml?faces-redirect=true";
    }

    public String gotoEditHostAttributes() {
        return "EditHostAttributes.xhtml?faces-redirect=true";
    }

    public String gotoEditArchivalUnitAttributes() {
        return "EditArchivalUnitAttributes.xhtml?faces-redirect=true";
    }

    Host host = new Host("enter host name", "enter Ip address", 0D, 0L);

    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = host;
    }

    public String reinitHost() {
        logger.info("reinitHost called");
        logger.info("hosts:size:" + (hosts.size()));
        logger.info("hosts" + xstream.toXML(hosts));
        host = new Host("enter host name", "enter Ip address", 0D, 0L);
        return null;
    }

    public Map<String, Object> getGeoLocations() {
        return GEO_LOCATION;
    }

    public void setGeoLocations(Map<String, Object> geoLocations) {
        GEO_LOCATION = geoLocations;
    }

    public int getMaximumRegions() {
        return maxRegions;
    }

    public Map<String, Object> getAuOwner() {
        return AU_OWNER;
    }

    public void setAuOwner(Map<String, Object> auOwner) {
        AU_OWNER = auOwner;
    }

    public Map<String, Object> getAuSubject() {
        return AU_SUBJECT;
    }

    public void setAuSubject(Map<String, Object> auSubject) {
        AU_SUBJECT = auSubject;
    }

    public double getSaasAuSizeMax() {
        return saasAuSizeMax;
    }

    BigInteger getAuSizeInGig(BigInteger auSize) {
        BigInteger auSizeInGig = null;
        if (auSize != null) {
            Long auSizeInGigL = (new Double(Math.ceil(auSize.doubleValue() / GIG_DIVIDER))).longValue();
            logger.info("auSizeInGigL=" + auSizeInGigL);
            auSizeInGig = new BigInteger(auSizeInGigL.toString());
            if (auSizeInGig.compareTo(BigInteger.ONE) == -1) {
                logger.info("auSizeInGig is less than 1");
                auSizeInGig = BigInteger.ONE;
            }
        } else {
            auSizeInGig = BigInteger.ONE;
        }
        return auSizeInGig;
    }

    public int getPlnQuorum() {
        return plnQuorum;
    }

    public int getPlnMemberHosts() {
        return plnMemberHosts;
    }

    public int getCrawlCycle() {
        return crawlCycle;
    }

    public String getEmailAddressValidationRegex() {
        return EMAIL_ADDRESS_VALIDATION_REGEX;
    }
}
