package org.regilo.modules.jobs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.regilo.modules.RegiloModulesPlugin;

public class GetModuleListJob extends Job {

    public GetModuleListJob(String name) {
        super(name);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            monitor.beginTask("Create Module list", 2);
            IPath stateLocation = Platform.getStateLocation(RegiloModulesPlugin.getDefault().getBundle());
            File file = new File(stateLocation.toFile(), "projects.xml");
            if (!file.exists()) {
                List<String> projects = createProjectInfoFile();
                String xml = buildXml(projects);
                monitor.worked(1);
                BufferedWriter out = new BufferedWriter(new FileWriter(file));
                out.write(xml);
                out.flush();
                out.close();
                monitor.worked(1);
            }
        } catch (SocketException e) {
            e.printStackTrace();
            return new Status(Status.ERROR, RegiloModulesPlugin.PLUGIN_ID, e.getMessage(), e);
        } catch (IOException e) {
            e.printStackTrace();
            return new Status(Status.ERROR, RegiloModulesPlugin.PLUGIN_ID, e.getMessage(), e);
        } finally {
            monitor.done();
        }
        return new Status(Status.OK, RegiloModulesPlugin.PLUGIN_ID, "success");
    }

    private List<String> createProjectInfoFile() throws SocketException, IOException {
        FTPClient client = new FTPClient();
        Set<String> projects = new HashSet<String>();
        client.connect("ftp.drupal.org");
        System.out.println("Connected to ftp.drupal.org");
        System.out.println(client.getReplyString());
        boolean loggedIn = client.login("anonymous", "info@regilo.org");
        if (loggedIn) {
            FTPFile[] files = client.listFiles("pub/drupal/files/projects");
            for (FTPFile file : files) {
                String name = file.getName();
                Pattern p = Pattern.compile("([a-zAZ_]*)-(\\d.x)-(.*)");
                Matcher m = p.matcher(name);
                if (m.matches()) {
                    String projectName = m.group(1);
                    String version = m.group(2);
                    if (version.equals("6.x")) {
                        projects.add(projectName);
                    }
                }
            }
        }
        List<String> projectList = new ArrayList<String>();
        for (String project : projects) {
            projectList.add(project);
        }
        Collections.sort(projectList);
        return projectList;
    }

    private String buildXml(List<String> projects) {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        builder.append("<projects>");
        for (String project : projects) {
            builder.append("<project>" + project + "</project>");
        }
        builder.append("</projects>");
        return builder.toString();
    }
}
