package pl.umk.webclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;
import com.intel.gpe.client2.StandaloneClient;
import com.intel.gpe.client2.adapters.MessageAdapter;
import com.intel.gpe.client2.common.clientwrapper.ClientWrapper;
import com.intel.gpe.client2.common.configurators.FileProviderConfigurator;
import com.intel.gpe.client2.common.configurators.NetworkConfigurator;
import com.intel.gpe.client2.common.requests.FetchOutcomeRequest;
import com.intel.gpe.client2.common.requests.GetRegistriesRequest;
import com.intel.gpe.client2.common.requests.SubmitRequest;
import com.intel.gpe.client2.common.requests.utils.LocalGPEFileAccessor;
import com.intel.gpe.client2.common.requests.utils.RemoteGPEFileAccessor;
import com.intel.gpe.client2.defaults.Registries;
import com.intel.gpe.client2.gridbeans.GridBeanJob;
import com.intel.gpe.client2.panels.GPEPanel;
import com.intel.gpe.client2.providers.FileProvider;
import com.intel.gpe.client2.providers.MessageProvider;
import com.intel.gpe.client2.security.GPESecurityManager;
import com.intel.gpe.clients.api.Job;
import com.intel.gpe.clients.api.JobClient;
import com.intel.gpe.clients.api.JobType;
import com.intel.gpe.clients.api.RegistryClient;
import com.intel.gpe.clients.api.TargetSystemClient;
import com.intel.gpe.clients.api.TargetSystemFactoryClient;
import com.intel.gpe.clients.api.exceptions.GPEMiddlewareException;
import com.intel.gpe.clients.api.exceptions.GPEWrongJobTypeException;
import com.intel.gpe.gridbeans.GridBeanParameter;
import com.intel.gpe.gridbeans.IGridBean;
import pl.umk.webclient.exceptions.NoTargetSystemSelectedException;
import pl.umk.webclient.gridbeans.WebGridBean;
import pl.umk.webclient.gridbeans.WebGridBeanJob;
import pl.umk.webclient.impl.security.GPESecurityManagerImpl;
import pl.umk.webclient.util.Utility;

/**
 * 
 * @author Rafal Osowicki (rafal_osowicki@users.sourceforge.net)
 * 
 */
public class WebClient implements StandaloneClient, MessageProvider {

    private static Logger logger = Logger.getLogger(WebClient.class.getName());

    private JobCache jobCache = null;

    private WebUser webuser;

    private GPESecurityManager securityManager;

    private FileProvider fileProvider;

    private MessageAdapter messageAdapter;

    private List<ClientWrapper<RegistryClient, String>> registryList = null;

    public WebClient(WebUser webuser) {
        logger.fine("Constructing " + WebClient.class.getName());
        this.webuser = webuser;
    }

    public void startup() {
        logger.info("Webclient is starting up...");
        try {
            securityManager = new GPESecurityManagerImpl(webuser);
            securityManager.init(messageAdapter, null, this, null);
            fileProvider = FileProviderConfigurator.getConfigurator().getFileProvider();
            NetworkConfigurator.getConfigurator().configure();
            jobCache = new JobCache();
            messageAdapter = new MessageAdapter(this, null);
        } catch (Exception e) {
            logger.severe("Cannot initialize Webclient: " + e.getLocalizedMessage());
            e.printStackTrace();
            return;
        }
        logger.info("Webclient initialized successfully.");
    }

    public void shutdown() {
        logger.info("Webclient is shutting down...");
    }

    public TargetSystemClient findClient(String targetSystemName) {
        for (ClientWrapper<RegistryClient, String> registry : registryList) {
            List<TargetSystemClient> list = null;
            try {
                list = registry.getClient().getTargetSystemServices();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            for (TargetSystemClient targetSystem : list) {
                String name = null;
                try {
                    name = targetSystem.getName();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
                if (name.equals(targetSystemName)) {
                    return targetSystem;
                }
            }
        }
        return null;
    }

    public void prepareNewJob(WebGridBean gridbean, String selectedNjs) throws GPEWrongJobTypeException, NoTargetSystemSelectedException {
        TargetSystemClient targetSystem = findClient(selectedNjs);
        if (selectedNjs == null || "".equals(selectedNjs) || targetSystem == null) {
            throw new NoTargetSystemSelectedException();
        }
        Job job = targetSystem.newJob(targetSystem.getJobType(JobType.JobDefinitions.GPEJSDL));
        getJobCache().addJob(new WebJobContainer(gridbean, job));
    }

    public Job prepareNewJobForWorkflow(WebGridBean gridbean, String selectedNjs) throws GPEWrongJobTypeException, NoTargetSystemSelectedException {
        TargetSystemClient targetSystem = findClient(selectedNjs);
        if (selectedNjs == null || "".equals(selectedNjs) || targetSystem == null) {
            throw new NoTargetSystemSelectedException();
        }
        Job job = targetSystem.newJob(targetSystem.getJobType(JobType.JobDefinitions.GPEJSDL));
        return job;
    }

    public Job saveJob(WebJobContainer job) {
        IGridBean model = job.getGridBean().getModel();
        try {
            model.setupJobDefinition(job.getJob());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return job.getJob();
    }

    public void submitJob(WebJobContainer jobContainer, String selectedNjs) {
        Calendar terminationTime = Calendar.getInstance();
        terminationTime.add(Calendar.MILLISECOND, Integer.parseInt(WebClientSettings.MAX_EXECUTION_TIME));
        TargetSystemClient targetSystem = findClient(selectedNjs);
        IGridBean model = jobContainer.getGridBean().getModel();
        logger.info("Submitting job " + jobContainer.getGridBean().getName());
        try {
            Object result = new SubmitRequest(new ClientWrapper<TargetSystemClient, String>(targetSystem, selectedNjs), jobContainer.getJob(), new LocalGPEFileAccessor(model, model.getInputParameters()), new RemoteGPEFileAccessor(model, model.getInputParameters()), terminationTime, fileProvider, securityManager).perform();
            if (result instanceof JobClient) {
                JobClient jobClient = (JobClient) result;
                GridBeanJob gridBeanJob = new WebGridBeanJob(WebClientSettings.WORKING_DIR, model, jobContainer.getGridBean());
                Utility.sleep(1500);
                Object result1 = new FetchOutcomeRequest(new ClientWrapper<JobClient, String>(jobClient, ""), gridBeanJob, fileProvider, securityManager).perform();
                if (result1 instanceof WebGridBeanJob) {
                    GridBeanJob gbJob = (WebGridBeanJob) result1;
                    String path = WebClientSettings.getOutcomePath() + jobContainer.getGridBean().getMetadata().getId() + "/";
                    File jobDir = new File(path);
                    jobDir.mkdir();
                    logger.info("Creating directory: " + path);
                    List<String> outputFiles = new ArrayList<String>();
                    for (GridBeanParameter parameter : gbJob.getOutputParameters()) {
                        outputFiles.add(parameter.getName().getLocalPart());
                    }
                    for (String filename : outputFiles) {
                        System.out.println("Copying file: " + filename);
                        File fromFile = new File(gbJob.getWorkingDirPrefix() + filename);
                        File targetFile = new File(path + filename);
                        if (fromFile.exists() && fromFile.canRead()) {
                            targetFile.createNewFile();
                            copyFile(fromFile, targetFile);
                        } else System.out.println("Cannot read source file: " + filename);
                    }
                    jobContainer.setUpdated(true);
                }
            }
        } catch (Throwable e) {
            logger.severe("Job Request failed: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }

    public synchronized List<ClientWrapper<RegistryClient, String>> getRegistriesList() {
        if (registryList == null || webuser.forceCacheReload) {
            Registries<String> registries = webuser.getRegistries();
            try {
                logger.info("Getting registries list.");
                registryList = new GetRegistriesRequest(securityManager, registries).perform();
                Calendar terminationTime = Calendar.getInstance();
                terminationTime.add(Calendar.MILLISECOND, Integer.parseInt(WebClientSettings.MAX_EXECUTION_TIME));
                for (ClientWrapper<RegistryClient, String> registry : registryList) {
                    try {
                        List<TargetSystemFactoryClient> factories = registry.getClient().getTargetSystemFactories();
                        for (TargetSystemFactoryClient factory : factories) {
                            ClientWrapper<TargetSystemFactoryClient, String> wrapper = new ClientWrapper<TargetSystemFactoryClient, String>(factory, "factory");
                            List<TargetSystemClient> targetSystems = factory.getTargetSystems();
                            if (targetSystems.size() == 0) {
                                logger.info("Creating new target system on registry: " + registry.getClient().toString());
                                factory.createTargetSystem(terminationTime);
                                wrapper.update(null, messageAdapter, this);
                            }
                        }
                    } catch (GPEMiddlewareException e) {
                        logger.warning("Cannot get target systems from registry: " + registry.getCache());
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        webuser.forceCacheReload = false;
        return registryList;
    }

    public JobCache getJobCache() {
        return jobCache;
    }

    public void show(GPEPanel panel) {
    }
}
