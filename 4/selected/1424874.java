package edu.harvard.iq.safe.saasystem.web.auditschema;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.*;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.*;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import edu.harvard.iq.safe.saasystem.ejb.*;
import edu.harvard.iq.safe.saasystem.entities.*;
import edu.harvard.iq.safe.saasystem.util.*;
import javax.ejb.EJB;
import edu.harvard.iq.safe.saasystem.auditschema.*;
import edu.harvard.iq.safe.saasystem.versioning.SAASSubversionServiceBean;
import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.tmatesoft.svn.core.SVNCommitInfo;

/**
 *
 * @author Akio Sone
 */
@ManagedBean(name = "createAuditSchemaMB")
@RequestScoped
public class CreateAuditSchemaInstance implements Serializable {

    static final Logger logger = Logger.getLogger(CreateAuditSchemaInstance.class.getName());

    @EJB
    ConfigFile configFile;

    @EJB
    SAASConfigurationRegistryBean saasConfigurationRegistry;

    @EJB
    LockssBoxTableFacade lockssBoxFacade;

    @EJB
    ArchivalUnitStatusTableFacade archivalUnitFacade;

    @EJB
    SAASSubversionServiceBean saasSubversionServiceBean;

    @EJB
    RegionCodeLabelFacade regionCodeLabelFacade;

    @EJB
    GroupwiseAuditPolicyFacade groupwiseAuditPolicyFacade;

    @EJB
    AuAttributesFacade auAttributesFacade;

    @EJB
    GeographicDataServiceBean geographicDataServiceBean;

    @EJB
    HostAttributesFacade hostAttributesFacade;

    @EJB
    OwnerDataServiceBean ownerDataServiceBean;

    @EJB
    SubjectDataServiceBean subjectDataServiceBean;

    List<LockssBoxTable> lockssBoxes = null;

    List<ArchivalUnitStatusTable> archivalUnits = null;

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

    Map<String, Object> GEO_LOCATION = new TreeMap<String, Object>();

    static final double POWER_FOR_STORAGE_UNIT = 30D;

    static double GIG_DIVIDER = Math.pow(2D, POWER_FOR_STORAGE_UNIT);

    static String AU_SUBJECT_BLANK_TOKEN = "NA";

    static String AU_OWNER_INST_BLANK_TOKEN = "NA";

    static int GEO_REDUNDANCY = 2;

    Map<String, Object> AU_SUBJECT = new TreeMap<String, Object>();

    Map<String, Object> AU_OWNER = new TreeMap<String, Object>();

    long numberReplicates;

    int verificationFrequency;

    int replicationCycle;

    int updateFrequency;

    int reportInterval;

    String auditReportIntervalOrigin = null;

    String geographicCoding = null;

    static String GEOGRAPHIC_CODING = "ANSI-INCITS 38:2009 two-letter alphabetic (modified)";

    String geographicSummaryScheme = null;

    static String GEOGRAPHIC_SUMMARY_SCHEME = "US Census Bureau 4-Region partition scheme";

    String subjectSummaryScheme = null;

    static String SUBJECT_SUMMARY_SCHEME = "US Library of Congress Classification scheme";

    String ownerInstSummaryScheme = null;

    static String OWNER_INST_SUMMARY_SCHEME = "SAFE-Project owner-institution naming scheme";

    String currentLoggedInUser = null;

    XStream xstream = new XStream(new JsonHierarchicalStreamDriver());

    List<Host> hosts = null;

    List<ArchivalUnit> aus = null;

    String geounitcodeToLabelFilename = null;

    Properties geounitcodeToLabelProps = new Properties();

    int maxRegions = 0;

    int geoRedudancy;

    String ownerOtionSetFile = null;

    String subjectOtionSetFile = null;

    List<ArchivalUnitGroupPolicy> auGroupPolicyList = null;

    Map<String, String> ipAddressToGeographicLocationTable = null;

    Map<String, String> geounitcodeToLabelMap = null;

    static boolean SAAS_AU_GROUP_POLICY_ENABLED = false;

    boolean auGroupPolicyEnabled;

    int plnQuorum = 5;

    int plnMemberHosts = 0;

    int crawlCycle = 14;

    static String SAAS_SUBJECT_PLACEHOLDER = "-unclassified-";

    static String SAAS_OWNER_INST_PLACEHOLDER = "-unspecified-";

    static String SAAS_GEO_LOCATION_PLACEHOLDER = "-OTHER-";

    static String EMAIL_ADDRESS_VALIDATION_REGEX = "[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?";

    @PostConstruct
    public void initialize() {
        logger.info("+++++ CreateAuditSchema: postConstruct step: start ++++++++");
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.alias("CreateAuditSchema", CreateAuditSchemaInstance.class);
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
        logger.info("saasAuSizeMax=" + saasAuSizeMax);
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
        hosts = new ArrayList<Host>();
        aus = new ArrayList<ArchivalUnit>();
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
        if (StringUtils.isNotBlank(saasConfigurationRegistry.getSaasConfigProperties().getProperty("saas.daemonstatus.groupname"))) {
            groupName = saasConfigurationRegistry.getSaasConfigProperties().getProperty("saas.daemonstatus.groupname");
        }
        if (StringUtils.isNotBlank(configFile.getGeounitcodeToLabelFilename())) {
            geounitcodeToLabelFilename = configFile.getGeounitcodeToLabelFilename();
            logger.info("saas.audit.schema.geounitcodetolabel.filename is specified");
        } else {
            logger.info("saas.audit.schema.geounitcodetolabel.filename is not specified;" + " use the factory default");
            geounitcodeToLabelFilename = configFile.getAuditSchemaFileDir() + "/" + GEOUNITCODE_TO_LABEL_FILENAME;
        }
        logger.info("geounitcodeToLabelFilename=" + geounitcodeToLabelFilename);
        GEO_LOCATION = geographicDataServiceBean.getGeounitcodeToLabelTable();
        ipAddressToGeographicLocationTable = hostAttributesFacade.getIpAddressToGeographicLocationTable();
        logger.fine("ipAddressToGeographicLocationTable" + xstream.toXML(ipAddressToGeographicLocationTable));
        geounitcodeToLabelMap = geographicDataServiceBean.getGeounitcodeToLabelMap();
        logger.fine("geounitcodeToLabelTable" + xstream.toXML(geounitcodeToLabelMap));
        if (StringUtils.isNotBlank(saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.geographic.coding"))) {
            geographicCoding = saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.geographic.coding");
        } else {
            geographicCoding = GEOGRAPHIC_CODING;
        }
        if (StringUtils.isNotBlank(saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.geographic.summaryscheme"))) {
            geographicSummaryScheme = saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.geographic.summaryscheme");
        } else {
            geographicSummaryScheme = GEOGRAPHIC_SUMMARY_SCHEME;
        }
        if (StringUtils.isNotBlank(saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.ownerInst.summaryscheme"))) {
            ownerInstSummaryScheme = saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.ownerInst.summaryscheme");
        } else {
            ownerInstSummaryScheme = OWNER_INST_SUMMARY_SCHEME;
        }
        AU_OWNER = ownerDataServiceBean.getAuOwners();
        AU_SUBJECT = subjectDataServiceBean.getAuSubjects();
        if (StringUtils.isNotBlank(saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.subject.summaryscheme"))) {
            subjectSummaryScheme = saasConfigurationRegistry.getSaasAuditConfigProperties().getProperty("saas.audit.schema.subject.summaryscheme");
        } else {
            subjectSummaryScheme = SUBJECT_SUMMARY_SCHEME;
        }
        if (auGroupPolicyEnabled) {
            if (groupwiseAuditPolicyFacade.getNumbeOfRows() > 0) {
                auGroupPolicyList = new ArrayList<ArchivalUnitGroupPolicy>();
                List<GroupwiseAuditPolicy> allRows = groupwiseAuditPolicyFacade.findAll();
                logger.info("how many group-policy rows=" + allRows.size());
                for (GroupwiseAuditPolicy row : allRows) {
                    auGroupPolicyList.add(new ArchivalUnitGroupPolicy(row.getGroupName(), row.getRecrawlFreq(), row.getStorageReqired(), row.getNReplicates(), row.getGeographicRedundancy(), row.getVerificationFreq(), row.getAcceptableUpdateDuration()));
                }
                logger.info("auGroupPolicyList from DB=" + auGroupPolicyList);
                Set<String> groupNameSet = new TreeSet<String>();
                Map<String, ArchivalUnitGroupPolicy> groupNameToPolicyTable = new LinkedHashMap<String, ArchivalUnitGroupPolicy>();
                for (ArchivalUnitGroupPolicy row : auGroupPolicyList) {
                    String tmpV = StringUtils.trim(row.getGroup());
                    if (tmpV == null) {
                        continue;
                    }
                    logger.info("value=" + tmpV);
                    groupNameSet.add(tmpV);
                    groupNameToPolicyTable.put(tmpV, row);
                }
                logger.info("how many group names=" + groupNameSet.size());
            }
        } else {
            logger.info("au-group policy is not enabled");
        }
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
        if (lockssBoxes == null) {
            lockssBoxes = lockssBoxFacade.findAll();
            logger.info("lockssBoxes has been filled: size=" + lockssBoxes.size());
        }
        hosts.clear();
        for (LockssBoxTable lb : lockssBoxes) {
            Double repoSize = lb.getTotalRepoSize() / Math.pow(1024D, 3D);
            logger.info("ip address for this row=" + lb.getIpAddress());
            if (ipAddressToGeographicLocationTable.containsKey(lb.getIpAddress())) {
                hosts.add(new Host(lb.getHostName(), lb.getIpAddress(), repoSize, repoSize.longValue(), geounitcodeToLabelMap.get(ipAddressToGeographicLocationTable.get(lb.getIpAddress())).toString()));
            } else {
                hosts.add(new Host(lb.getHostName(), lb.getIpAddress(), repoSize, repoSize.longValue(), SAAS_GEO_LOCATION_PLACEHOLDER));
            }
        }
        selectedHosts = hosts.toArray(new Host[hosts.size()]);
        maxRegions = regionCodeLabelFacade.getMaximumRegions() <= hosts.size() ? regionCodeLabelFacade.getMaximumRegions() : hosts.size();
        geoRedudancy = GEO_REDUNDANCY;
        if (geoRedudancy <= maxRegions) {
            logger.info("geographic redundancy value is equal or " + "less than the maximum " + "number of regions");
        } else {
            geoRedudancy = maxRegions;
        }
        numberReplicates = numberReplicates <= hosts.size() ? numberReplicates : hosts.size();
        if (archivalUnits == null) {
            archivalUnits = archivalUnitFacade.findUniqueAu();
            logger.info("archivalUnits has been filled: size=" + archivalUnits.size());
        }
        aus.clear();
        for (ArchivalUnitStatusTable au : archivalUnits) {
            logger.info("setting policy values for " + au.getAuName());
            logger.info("raw au size=" + au.getAuSize());
            BigInteger auSizeInGig = getAuSizeInGig(au.getAuSize());
            logger.info("auSizeInGig=" + auSizeInGig);
            List<AuAttributes> auAttributesList = auAttributesFacade.findAuAttributesByAuName(au.getAuName());
            if (auAttributesList == null || auAttributesList.isEmpty()) {
                logger.info("Pre-registered AU attributes are not found for this au=" + au.getAuName() + " use the default values");
                String subject = SAAS_SUBJECT_PLACEHOLDER;
                String ownerInst = SAAS_OWNER_INST_PLACEHOLDER;
                aus.add(new ArchivalUnit(au.getAuName(), numberReplicates, verificationFrequency, replicationCycle, updateFrequency, auSizeInGig, au.getAuId(), subject, ownerInst, geoRedudancy));
                continue;
            }
            if (auAttributesList.size() > 1) {
                logger.warning("non-unique au-names were found for " + au.getAuName());
            }
            AuAttributes auAttRow = auAttributesList.get(0);
            String ownerInst = auAttRow.getOwnerInst();
            if (StringUtils.isNotBlank(ownerInst)) {
                logger.info("owner's info is available");
                List<GroupwiseAuditPolicy> groupPolicyList = groupwiseAuditPolicyFacade.findGroupwiseAuditPolicyByOwner(ownerInst);
                String subject = StringUtils.isNotBlank(auAttRow.getSubject()) ? auAttRow.getSubject() : SAAS_SUBJECT_PLACEHOLDER;
                if (groupPolicyList == null || groupPolicyList.isEmpty()) {
                    logger.info("Group policy set was not found for this owner:" + ownerInst + " use the default values");
                    aus.add(new ArchivalUnit(au.getAuName(), numberReplicates, verificationFrequency, replicationCycle, updateFrequency, auSizeInGig, au.getAuId(), subject, ownerInst, geoRedudancy));
                } else {
                    if (auGroupPolicyEnabled) {
                        logger.info("setting parameters group-wise is enabled");
                        logger.info("group policy set was found for this owner:" + ownerInst + " registered values are used");
                        GroupwiseAuditPolicy groupPolicy = groupPolicyList.get(0);
                        aus.add(new ArchivalUnit(au.getAuName(), groupPolicy.getNReplicates(), groupPolicy.getVerificationFreq(), groupPolicy.getAcceptableUpdateDuration(), groupPolicy.getRecrawlFreq(), groupPolicy.getStorageReqired(), au.getAuId(), subject, ownerInst, groupPolicy.getGeographicRedundancy()));
                    } else {
                        logger.info("setting parameters group-wise is NOT enabled");
                        logger.info("use the default values");
                        aus.add(new ArchivalUnit(au.getAuName(), numberReplicates, verificationFrequency, replicationCycle, updateFrequency, auSizeInGig, au.getAuId(), subject, ownerInst, geoRedudancy));
                    }
                }
            } else {
                logger.info("owner's info is not available: use the default values");
                String subject = StringUtils.isNotBlank(auAttRow.getSubject()) ? auAttRow.getSubject() : SAAS_SUBJECT_PLACEHOLDER;
                ownerInst = SAAS_OWNER_INST_PLACEHOLDER;
                aus.add(new ArchivalUnit(au.getAuName(), numberReplicates, verificationFrequency, replicationCycle, updateFrequency, auSizeInGig, au.getAuId(), subject, ownerInst, geoRedudancy));
            }
        }
        selectedAus = aus.toArray(new ArchivalUnit[aus.size()]);
        logger.info("aus: size =" + aus.size());
        logger.info("selectedAus: length =" + selectedAus.length);
        auditReportInterval = reportInterval;
        host.setHostIpAddress("Ip address here");
        host.setHostName("host name here");
        host.setTotalRepoSize(0.0D);
        host.setStorageAvailable(0L);
        logger.info("hosts" + xstream.toXML(hosts));
        logger.info("aus=" + xstream.toXML(aus));
        logger.info("selectedHsts" + xstream.toXML(selectedHosts));
        logger.info("selectedAus=" + xstream.toXML(selectedAus));
        auditSchemaInstanceVersion = "1.0.0";
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
        logger.info("+++++ CreateAuditSchema: postConstruct step: end ++++++++");
    }

    /** Creates a new instance of CreateAuditSchemaInstance */
    public CreateAuditSchemaInstance() {
    }

    public List<Host> getHosts() {
        return hosts;
    }

    public void setHosts(List<Host> hosts) {
        this.hosts = hosts;
    }

    public List<ArchivalUnit> getAus() {
        return aus;
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

    /**
     *
     * @param actionEvent
     */
    public void save(ActionEvent actionEvent) {
        logger.info("\n+++++++++++++++ CreateAuditSchema: save() +++++++++++++++++++++");
        logger.info("selected hosts:" + selectedHosts.length);
        logger.info("selected hosts=" + xstream.toXML(selectedHosts));
        logger.info("selected aus:" + selectedAus.length);
        logger.info("selected aus=" + xstream.toXML(selectedAus));
        logger.info("groupName=" + groupName);
        logger.info("auditReportEmail=" + auditReportEmail);
        logger.info("netAdminEmail=" + netAdminEmail);
        logger.info("auditReportInterval=" + auditReportInterval);
        logger.info("auditSchemaInstanceVersion=" + auditSchemaInstanceVersion);
        auditSchemaInstanceId = "asiId_" + RandomStringUtils.randomAlphanumeric(4);
        logger.info("auditSchemaInstanceId to be issued=" + auditSchemaInstanceId);
        AuditSchemaInstance auditSchema = convertToAuditSchema(auditSchemaInstanceId);
        File lastVersion = new File(configFile.getAuditSchemaFile());
        String msgToken = null;
        try {
            if (lastVersion.exists()) {
                boolean bkupResult = backupLastAuditSchema(lastVersion);
                if (!bkupResult) {
                    logger.warning("last audit schema file was not saved");
                }
            }
            auditSchema.write(configFile.getAuditSchemaFile());
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, "AuditSchemaFile was not found", ex);
        }
        File newAuditSchemaFile = new File(configFile.getAuditSchemaFile());
        if ((newAuditSchemaFile != null) && (newAuditSchemaFile.exists()) && (newAuditSchemaFile.length() > 0)) {
            msgToken = "The new audit schema file was successfully created: ID=" + auditSchemaInstanceId;
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Save action succeeded", msgToken));
        } else {
            msgToken = "The new audit schema file was not created: please try again";
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Save action Failed", msgToken));
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
        logger.info("\n+++++++++++++++ CreateAuditSchema: save(): end +++++++++++++++++++++");
        return;
    }

    /**
     *
     * @return
     */
    public AuditSchemaInstance convertToAuditSchema(String auditSchemaInstanceId) {
        AuditSchemaInstance auditSchema = null;
        Audit audit = new Audit(auditSchemaInstanceId, auditSchemaInstanceVersion, auditReportEmail, auditReportInterval, geographicSummaryScheme, subjectSummaryScheme, ownerInstSummaryScheme, maxRegions);
        Network network = new Network(groupName, netAdminEmail, geographicCoding);
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
                logger.info("closing channels failed");
            }
        }
        return isBkupFileOK;
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

    int checkBlankCells(ArchivalUnit[] aus) {
        int changeCounter = 0;
        for (ArchivalUnit au : aus) {
            if (StringUtils.isBlank(au.getSubject())) {
                au.setSubject(AU_SUBJECT_BLANK_TOKEN);
                changeCounter++;
            }
            if (StringUtils.isBlank(au.getOwnerInstitution())) {
                au.setOwnerInstitution(AU_OWNER_INST_BLANK_TOKEN);
                changeCounter++;
            }
        }
        return changeCounter;
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

    public double getSaasAuSizeMax() {
        return saasAuSizeMax;
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
