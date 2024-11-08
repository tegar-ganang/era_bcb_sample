package com.lingway.webapp.antui.dao.impl;

import com.lingway.webapp.antui.dao.intf.IProjectDao;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.AntClassLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * <dl>
 * <dt><b>Creation date :</b></dt>
 * <dd>9 mars 2007</dd>
 *
 * @author Cedric CHAMPEAU <cedric.champeau[at]lingway[dot]com>
 *         </dl>
 */
public class AntProjectFromFSDao implements IProjectDao {

    private static final String DEFAULT_DIRECTORY = "antrepo";

    protected Logger theLogger = Logger.getLogger(AntProjectFromFSDao.class);

    private List<Project> theProjects;

    private String theProjectsDirectory;

    public AntProjectFromFSDao() {
        theProjectsDirectory = DEFAULT_DIRECTORY;
    }

    public String getProjectsDirectory() {
        return theProjectsDirectory;
    }

    public void setProjectsDirectory(String aProjectsDirectory) {
        theProjectsDirectory = aProjectsDirectory;
    }

    public List<Project> getAll() {
        if (theProjects == null) synchronize();
        return theProjects;
    }

    public void save(Project aProject) {
        theLogger.error("Called save() on AntProjectDao but not implemented");
    }

    public void update(Project aProject) {
        theLogger.error("Called update() on AntProjectDao but not implemented");
    }

    public void delete(Project aProject) {
        theLogger.error("Called delete() on AntProjectDao but not implemented");
    }

    public void synchronize() {
        if (theProjectsDirectory == DEFAULT_DIRECTORY) {
            theLogger.warn("The configuration does not specify an alternate storage path for Ant projects." + " The projects will be stored in the application server directory, which is both unsecure and dirty.");
        }
        theProjects = new ArrayList<Project>();
        File root = new File(theProjectsDirectory);
        if (root.exists()) {
            File[] subdirs = root.listFiles();
            for (File subdir : subdirs) {
                if (subdir.isDirectory()) {
                    File[] projectFiles = subdir.listFiles();
                    for (File file : projectFiles) {
                        if (file.getName().toLowerCase().endsWith(".xml")) addProjectFromFile(file);
                    }
                }
            }
        }
    }

    public void saveProjectFile(File aFile) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
        File destDir = new File(theProjectsDirectory, sdf.format(Calendar.getInstance().getTime()));
        if (destDir.mkdirs()) {
            File outFile = new File(destDir, "project.xml");
            try {
                FileChannel sourceChannel = new FileInputStream(aFile).getChannel();
                FileChannel destinationChannel = new FileOutputStream(outFile).getChannel();
                sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
                sourceChannel.close();
                destinationChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                aFile.delete();
            }
        }
    }

    private void addProjectFromFile(File aFile) {
        try {
            AntClassLoader loader = new AntClassLoader() {

                protected synchronized Class loadClass(String aString, boolean b) throws ClassNotFoundException {
                    theLogger.debug("Loading class");
                    return super.loadClass(aString, b);
                }
            };
            File libs = new File(aFile.getParentFile(), "lib");
            if (libs.exists()) {
                for (File library : libs.listFiles()) {
                    loader.addPathElement(library.getAbsolutePath());
                }
            }
            Project p = new Project() {

                public void setCoreLoader(ClassLoader aClassLoader) {
                    theLogger.debug("Replacing core loader");
                    super.setCoreLoader(aClassLoader);
                }
            };
            p.setUserProperty("ant.file", aFile.getAbsolutePath());
            p.init();
            p.setCoreLoader(loader);
            loader.setParent(AntProjectFromFSDao.class.getClassLoader());
            ProjectHelper helper = ProjectHelper.getProjectHelper();
            p.addReference("ant.projectHelper", helper);
            helper.parse(p, aFile);
            theProjects.add(p);
        } catch (BuildException e) {
            theLogger.warn("Could not load Ant Project File " + aFile.getAbsolutePath(), e);
        }
    }
}
