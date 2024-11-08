package au.edu.uq.itee.eresearch.dimer.webapp.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import au.edu.uq.itee.eresearch.dimer.core.security.CustomSystemSession;
import au.edu.uq.itee.eresearch.dimer.webapp.app.util.UserUtils;
import com.ibm.icu.util.Calendar;

public class FileMonitorJob implements Job {

    private static Logger log = LoggerFactory.getLogger(FileMonitorJob.class);

    private static String ftpHostname = GlobalProperties.properties.getProperty("fileMonitor.ftp.hostname");

    private static String ftpUsername = GlobalProperties.properties.getProperty("fileMonitor.ftp.username");

    private static String ftpPassword = GlobalProperties.properties.getProperty("fileMonitor.ftp.password");

    public static Repository r;

    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("FileMonitorJob - executing its JOB at " + new Date() + " by " + context.getTrigger().getName());
        FTPClient client = new FTPClient();
        OutputStream outStream = null;
        Calendar filterCal = Calendar.getInstance();
        filterCal.set(Calendar.DAY_OF_MONTH, filterCal.get(Calendar.DAY_OF_MONTH) - 1);
        Date aDayAgo = filterCal.getTime();
        try {
            Session session = CustomSystemSession.create(r);
            client.connect(ftpHostname);
            client.login(ftpUsername, ftpPassword);
            FTPFile[] users = client.listFiles();
            if (users != null) {
                for (FTPFile user : users) {
                    String userName = user.getName();
                    client.changeWorkingDirectory("/" + userName + "/");
                    Node userNode = null;
                    @SuppressWarnings("deprecation") Query query = session.getWorkspace().getQueryManager().createQuery("/jcr:root/users/element(*, user)[\n" + "  @alias = '" + userName.replaceAll("'", "''") + "'\n" + "]\n" + "order by @lastModified descending", Query.XPATH);
                    NodeIterator results = query.execute().getNodes();
                    if (results.hasNext()) {
                        userNode = results.nextNode();
                    } else if (session.getRootNode().hasNode("users/" + userName)) {
                        userNode = session.getRootNode().getNode("users/" + userName);
                    }
                    FTPFile[] experiments = client.listFiles();
                    if (experiments != null && userNode != null) {
                        for (FTPFile experiment : experiments) {
                            String experimentName = experiment.getName();
                            client.changeWorkingDirectory("/" + userName + "/" + experimentName + "/");
                            FTPFile[] datasets = client.listFiles();
                            if (datasets != null) {
                                for (FTPFile dataset : datasets) {
                                    String datasetName = dataset.getName();
                                    client.changeWorkingDirectory("/" + userName + "/" + experimentName + "/" + datasetName + "/");
                                    Date collectionDate = dataset.getTimestamp().getTime();
                                    if (collectionDate.after(aDayAgo)) {
                                        FTPFile[] images = client.listFiles();
                                        if (images != null) {
                                            for (FTPFile image : images) {
                                                processImage(userName, experimentName, datasetName, collectionDate, image, client, userNode, session);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            session.logout();
            client.logout();
        } catch (IOException ioe) {
            log.info("Error communicating with FTP server.");
            log.error("Error communicating with FTP server.", ioe);
            ioe.printStackTrace();
        } catch (RepositoryException ioe) {
            log.info("Error communicating with repository.");
            log.error("Error communicating with repository.", ioe);
            ioe.printStackTrace();
        } finally {
            IOUtils.closeQuietly(outStream);
            try {
                client.disconnect();
            } catch (IOException e) {
                log.error("Problem disconnecting from FTP server", e);
            }
        }
    }

    public static String harvestForUser(Node userNode, String alias, Boolean all) {
        FTPClient client = new FTPClient();
        OutputStream outStream = null;
        Calendar filterCal = Calendar.getInstance();
        filterCal.set(Calendar.DAY_OF_MONTH, filterCal.get(Calendar.DAY_OF_MONTH) - 1);
        Date aDayAgo = filterCal.getTime();
        String outputRecord = "";
        try {
            Session session = CustomSystemSession.create(r);
            client.connect(ftpHostname);
            client.login(ftpUsername, ftpPassword);
            FTPFile[] users = client.listFiles();
            if (users != null) {
                for (FTPFile user : users) {
                    String userName = user.getName();
                    if (alias.equals(userName)) {
                        outputRecord += "Found account " + userName + ".\n";
                        client.changeWorkingDirectory("/" + userName + "/");
                        FTPFile[] experiments = client.listFiles();
                        if (experiments != null && userNode != null) {
                            for (FTPFile experiment : experiments) {
                                String experimentName = experiment.getName();
                                outputRecord += "Exploring " + userName + "/" + experimentName + ".\n";
                                client.changeWorkingDirectory("/" + userName + "/" + experimentName + "/");
                                FTPFile[] datasets = client.listFiles();
                                if (datasets != null) {
                                    for (FTPFile dataset : datasets) {
                                        String datasetName = dataset.getName();
                                        outputRecord += "Exploring " + userName + "/" + experimentName + "/" + datasetName + ".\n";
                                        client.changeWorkingDirectory("/" + userName + "/" + experimentName + "/" + datasetName + "/");
                                        Date collectionDate = dataset.getTimestamp().getTime();
                                        if (collectionDate.after(aDayAgo) || all) {
                                            FTPFile[] images = client.listFiles();
                                            if (images != null) {
                                                for (FTPFile image : images) {
                                                    outputRecord += processImage(userName, experimentName, datasetName, collectionDate, image, client, userNode, session);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            client.logout();
        } catch (IOException ioe) {
            log.info("Error communicating with FTP server.");
            log.error("Error communicating with FTP server.", ioe);
            ioe.printStackTrace();
        } catch (RepositoryException ioe) {
            log.info("Error communicating with repository.");
            log.error("Error communicating with repository.", ioe);
            ioe.printStackTrace();
        } finally {
            IOUtils.closeQuietly(outStream);
            try {
                client.disconnect();
            } catch (IOException e) {
                log.error("Problem disconnecting from FTP server", e);
            }
        }
        return outputRecord;
    }

    private static String processImage(String userName, String experimentName, String datasetName, Date collectionDate, FTPFile imageFile, FTPClient client, Node user, Session session) {
        String outputString = "";
        try {
            Node project;
            if (session.nodeExists("/projects/filemonitor-" + user.getName())) {
                project = session.getNode("/projects/filemonitor-" + user.getName());
            } else {
                project = session.getRootNode().addNode("projects/filemonitor-" + user.getName(), "project");
                project.setProperty("title", "Harvested Experiments for " + UserUtils.getFullNameFirst(user));
                project.setProperty("description", "");
                project.setProperty("published", false);
                project.addNode("managers", "accessors");
                project.addNode("writers", "accessors");
                project.addNode("readers", "accessors");
                project.addNode("experiments", "experiments");
                project.getNode("managers").setProperty("users", new String[0]);
                project.getNode("managers").setProperty("groups", new String[0]);
                project.getNode("writers").setProperty("users", new String[0]);
                project.getNode("writers").setProperty("groups", new String[0]);
                project.getNode("readers").setProperty("users", new String[] { user.getIdentifier() });
                project.getNode("readers").setProperty("groups", new String[0]);
                outputString += "Created project filemonitor-" + user.getName() + ".\n";
            }
            Node experiment;
            if (session.nodeExists("/projects/filemonitor-" + user.getName() + "/experiments/" + experimentName)) {
                experiment = session.getNode("/projects/filemonitor-" + user.getName() + "/experiments/" + experimentName);
            } else {
                experiment = project.addNode("experiments/" + experimentName, "experiment");
                experiment.setProperty("title", experimentName);
                experiment.setProperty("description", "");
                experiment.setProperty("published", false);
                experiment.addNode("datasets", "datasets");
                outputString += "Created experiment " + experimentName + ".\n";
            }
            Node dataset;
            if (session.nodeExists("/projects/filemonitor-" + user.getName() + "/experiments/" + experimentName + "/datasets/" + datasetName)) {
                dataset = session.getNode("/projects/filemonitor-" + user.getName() + "/experiments/" + experimentName + "/datasets/" + datasetName);
            } else {
                dataset = experiment.addNode("datasets/" + datasetName, "diffractionDataset");
                dataset.addNode("files", "files");
                dataset.setProperty("title", datasetName);
                outputString += "Created dataset " + datasetName + ".\n";
            }
            if (!session.nodeExists("/projects/filemonitor-" + user.getName() + "/experiments/" + experimentName + "/datasets/" + datasetName + "/files/" + imageFile.getName())) {
                InputStream in = client.retrieveFileStream(imageFile.getName());
                Node file = dataset.getNode("files").addNode(imageFile.getName(), "nt:file");
                Node content = file.addNode("jcr:content", "nt:resource");
                Binary binary = session.getValueFactory().createBinary(in);
                content.setProperty("jcr:data", binary);
                content.setProperty("jcr:mimeType", "application/octet-stream");
                in.close();
                client.completePendingCommand();
                outputString += userName + "/" + experimentName + "/" + datasetName + "/" + imageFile.getName() + " added to local repository.\n";
            } else {
                outputString += userName + "/" + experimentName + "/" + datasetName + "/" + imageFile.getName() + " already in local repository.\n";
            }
            session.save();
        } catch (Exception e) {
            log.error("Problem ingesting image file (" + userName + "/" + experimentName + "/" + datasetName + "/" + imageFile.getName() + ").", e);
        }
        return outputString;
    }
}
