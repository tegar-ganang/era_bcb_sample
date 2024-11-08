package jpatch.boundary;

import java.io.*;
import java.util.*;
import jpatch.entity.*;

public class WorkspaceManager {

    private static final FileFilter DIR_FILEFILTER = new FileFilter() {

        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    };

    private File workspaceDir;

    private File lock;

    private Project[] projects = new Project[0];

    public WorkspaceManager(File workspaceDir) throws IOException {
        this.workspaceDir = workspaceDir;
        lock = new File(workspaceDir, "jpatch.lock");
        if (!workspaceDir.exists()) {
            try {
                if (!workspaceDir.mkdirs()) {
                    throw new IOException("Can't reate workspace directory \"" + workspaceDir.getCanonicalPath() + "\".");
                }
            } catch (SecurityException e) {
                throw new IOException("Can't create workspace directory \"" + workspaceDir.getCanonicalPath() + "\": " + e.getMessage());
            }
        }
        if (!lock.exists()) {
            try {
                lock.createNewFile();
            } catch (IOException e) {
                throw new IOException("Can't create lock in workspace directory \"" + workspaceDir.getCanonicalPath() + "\": " + e.getMessage());
            }
        }
        if (new FileOutputStream(lock).getChannel().tryLock() == null) {
            throw new IOException("Can't acquire exclusive lock on workspace \"" + workspaceDir.getCanonicalPath() + "\".");
        }
    }

    public void refresh() {
        List<Project> projectList = new ArrayList<Project>();
        for (File file : workspaceDir.listFiles(DIR_FILEFILTER)) {
            try {
                Project project = new Project(this, file.getName());
                projectList.add(project);
            } catch (IOException e) {
                ;
            }
        }
        projects = projectList.toArray(new Project[projectList.size()]);
    }

    public Project[] getProjects() {
        return projects;
    }

    public File getDirectory() {
        return workspaceDir;
    }
}
