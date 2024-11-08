package org.bejug.javacareers.feeder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bejug.javacareers.common.search.SearchCriteriaFactory;
import org.bejug.javacareers.common.search.SearchCriteriaService;
import org.bejug.javacareers.jobs.service.AdminService;
import org.bejug.javacareers.jobs.service.JobService;
import org.bejug.javacareers.jobs.service.RssFeedService;
import org.bejug.javacareers.project.properties.JavaCareersConfig;
import org.gnu.stealthp.rsslib.RSSChannel;
import org.gnu.stealthp.rsslib.RSSImage;

/**
 * Base class for scheduled tasks
 *
 * @author Bavo Bruylandt (Last modified by $Author: bavo_jcs $)
 * @version $Revision: 1.3 $ - $Date: 2005/09/30 14:38:08 $
 */
public abstract class FeederTask {

    /**
     * The class logger.
     */
    private static final Log LOG = LogFactory.getLog(FeederTask.class);

    private FeederDaemonConfig feederDaemonConfig;

    private RssFeedService rssFeedService;

    private JobService jobService;

    private AdminService adminService;

    private SearchCriteriaFactory searchCriteriaFactory;

    /**
    * search service to use.
    */
    private SearchCriteriaService searchCriteriaService;

    /**
     * the javacareersconfig-file containing the javacareers props.
     */
    private JavaCareersConfig javaCareersConfig;

    /**
     * Is thread running.
     */
    private boolean running;

    /**
     * the filepath.
     */
    protected String filepath;

    /**
     * the rsschannel.
     */
    protected RSSChannel channel;

    /**
     * Starts running the task
     *
     * @param run boolean
     */
    public void setRunning(boolean run) {
        this.running = run;
    }

    /**
     * Is the task running
     *
     * @return boolean task is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * FeederDaemon feederDaemonConfig file is set through Springs IoC.
     *
     * @param config Sets the feederDaemonConfig object (Spring)
     */
    public void setFeederDaemonConfig(FeederDaemonConfig config) {
        LOG.info("Debug: >>>>>>>>>>>>>>>>> Setting config");
        this.feederDaemonConfig = config;
    }

    /**
     * @return Config object containg settings
     */
    public FeederDaemonConfig getFeederDaemonConfig() {
        return feederDaemonConfig;
    }

    /**
     * @return Returns the rssFeedService.
     */
    public RssFeedService getRssFeedService() {
        return rssFeedService;
    }

    /**
     * Spring-injection method for RSSFeedDAO
     *
     * @param rssFeedService The rssFeedService to set.
     */
    public void setRssFeedService(RssFeedService rssFeedService) {
        LOG.info("Debug: Setting config");
        this.rssFeedService = rssFeedService;
    }

    /**
     * Gets JobService to manage persistent jobs
     *
     * @return the JobService to manage persistent jobs
     */
    public JobService getJobService() {
        return jobService;
    }

    /**
     * Gets Admin service to manage users
     *
     * @return service to manage persistent users
     */
    public AdminService getAdminService() {
        return adminService;
    }

    /**
     * Sets service to manage persistent jobs
     *
     * @param jobService to manage persistent jobs
     */
    public void setJobService(JobService jobService) {
        this.jobService = jobService;
    }

    /**
     * @param adminService AdminService
     */
    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * @return JavaCareersConfig
     */
    public JavaCareersConfig getJavaCareersConfig() {
        return javaCareersConfig;
    }

    /**
     * @param javaCareersConfig JavaCareersConfig
     */
    public void setJavaCareersConfig(JavaCareersConfig javaCareersConfig) {
        this.javaCareersConfig = javaCareersConfig;
    }

    /**
     * @return the searchCriteriaFactory.
     */
    public SearchCriteriaFactory getSearchCriteriaFactory() {
        return searchCriteriaFactory;
    }

    /**
     * @param searchCriteriaFactory the searchCriteriaFactort through IoC
     */
    public void setSearchCriteriaFactory(SearchCriteriaFactory searchCriteriaFactory) {
        this.searchCriteriaFactory = searchCriteriaFactory;
    }

    /**
     * @return the searchCriteriaService.
     */
    public SearchCriteriaService getSearchCriteriaService() {
        return searchCriteriaService;
    }

    /**
     * @param searchCriteriaService the searchCriteriaService.
     */
    public void setSearchCriteriaService(SearchCriteriaService searchCriteriaService) {
        this.searchCriteriaService = searchCriteriaService;
    }

    /**
     * @param config FeederDaemonConfig
     */
    protected void constructFeeder(FeederDaemonConfig config) {
        LOG.info("Debug: filepath: " + filepath);
        channel = new RSSChannel();
        channel.setDescription(config.getChannelDescription());
        LOG.info("Debug: description: " + config.getChannelDescription());
        channel.setLink(config.getChannelLink());
        channel.setTitle(config.getChannelTitle());
        channel.setCopyright(config.getChannelCopyright());
        channel.setManagingEditor(config.getChannelEditor());
        channel.setWebMaster(config.getChannelWebmaster());
        RSSImage image = new RSSImage();
        image.setTitle(config.getChannelTitle());
        image.setLink(config.getChannelLink());
        image.setUrl(config.getChannelImage());
        channel.setRSSImage(image);
        LOG.info("Debug: Constructed");
    }
}
