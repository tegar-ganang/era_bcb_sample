package jfpsm;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import misc.SerializationHelper;

/**
 * Container of projects, it is a kind of workspace.
 * @author Julien Gouesse
 *
 */
public final class ProjectSet extends JFPSMUserObject {

    static {
        SerializationHelper.forceHandlingOfTransientModifiersForXMLSerialization(ProjectSet.class);
    }

    private static final ProjectFileFilter projectFileFilter = new ProjectFileFilter();

    private static final long serialVersionUID = 1L;

    private ArrayList<Project> projectsList;

    private transient boolean dirty;

    private transient File workspaceDirectory;

    public ProjectSet() {
        this("");
    }

    public ProjectSet(String name) {
        super(name);
        projectsList = new ArrayList<Project>();
        dirty = true;
        initializeWorkspaceDirectory();
    }

    final boolean addProject(Project project) {
        final boolean success;
        if (success = !projectsList.contains(project)) {
            projectsList.add(project);
            dirty = true;
        }
        return (success);
    }

    final String createProjectPath(String projectName) {
        return (createRawDataPath(projectName) + Project.getFileExtension());
    }

    final String createRawDataPath(String name) {
        return (workspaceDirectory.getAbsolutePath() + System.getProperty("file.separator") + name);
    }

    final void removeProject(Project project) {
        if (projectsList.remove(project)) {
            dirty = true;
            final File projectFile = new File(createProjectPath(project.getName()));
            if (projectFile.exists()) projectFile.delete();
        }
    }

    final void saveProject(Project project) {
        final String projectPath = createProjectPath(project.getName());
        final File projectFile = new File(projectPath);
        saveProject(project, projectFile);
    }

    final void saveProject(Project project, final File file) {
        if (projectsList.contains(project)) {
            if (project.isDirty() || !file.getParentFile().equals(workspaceDirectory)) {
                try {
                    if (!file.exists()) {
                        if (!file.createNewFile()) throw new IOException("cannot create file " + file.getAbsolutePath());
                    }
                    File tmpFile = File.createTempFile("JFPSM", ".tmp");
                    ZipOutputStream zoStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
                    zoStream.setMethod(ZipOutputStream.DEFLATED);
                    ZipEntry projectXMLEntry = new ZipEntry("project.xml");
                    projectXMLEntry.setMethod(ZipEntry.DEFLATED);
                    zoStream.putNextEntry(projectXMLEntry);
                    CustomXMLEncoder encoder = new CustomXMLEncoder(new BufferedOutputStream(new FileOutputStream(tmpFile)));
                    encoder.writeObject(project);
                    encoder.close();
                    int bytesIn;
                    byte[] readBuffer = new byte[1024];
                    FileInputStream fis = new FileInputStream(tmpFile);
                    while ((bytesIn = fis.read(readBuffer)) != -1) zoStream.write(readBuffer, 0, bytesIn);
                    fis.close();
                    ZipEntry entry;
                    String floorDirectory;
                    for (FloorSet floorSet : project.getLevelSet().getFloorSetsList()) for (Floor floor : floorSet.getFloorsList()) {
                        floorDirectory = "levelset/" + floorSet.getName() + "/" + floor.getName() + "/";
                        for (MapType type : MapType.values()) {
                            entry = new ZipEntry(floorDirectory + type.getFilename());
                            entry.setMethod(ZipEntry.DEFLATED);
                            zoStream.putNextEntry(entry);
                            ImageIO.write(floor.getMap(type).getImage(), "png", zoStream);
                        }
                    }
                    final String tileDirectory = "tileset/";
                    for (Tile tile : project.getTileSet().getTilesList()) for (int textureIndex = 0; textureIndex < tile.getMaxTextureCount(); textureIndex++) if (tile.getTexture(textureIndex) != null) {
                        entry = new ZipEntry(tileDirectory + tile.getName() + textureIndex + ".png");
                        entry.setMethod(ZipEntry.DEFLATED);
                        zoStream.putNextEntry(entry);
                        ImageIO.write(tile.getTexture(textureIndex), "png", zoStream);
                    }
                    zoStream.close();
                    tmpFile.delete();
                } catch (IOException ioe) {
                    throw new RuntimeException("The project " + project.getName() + " cannot be saved!", ioe);
                }
            }
        } else throw new IllegalArgumentException("The project " + project.getName() + " is not handled by this project set!");
    }

    @Override
    public final boolean isDirty() {
        boolean dirty = this.dirty;
        if (!dirty) for (Project project : projectsList) if (project.isDirty()) {
            dirty = true;
            break;
        }
        return (dirty);
    }

    @Override
    public final void unmarkDirty() {
        dirty = false;
    }

    @Override
    public final void markDirty() {
        dirty = true;
    }

    public final ArrayList<Project> getProjectsList() {
        return (projectsList);
    }

    public final void setProjectsList(ArrayList<Project> projectsList) {
        this.projectsList = projectsList;
    }

    private final void initializeWorkspaceDirectory() {
        workspaceDirectory = new File(System.getProperty("user.home") + System.getProperty("file.separator") + "jfpsm");
        if (!workspaceDirectory.exists() || !workspaceDirectory.isDirectory()) workspaceDirectory.mkdir();
        if (!workspaceDirectory.exists() || !workspaceDirectory.isDirectory() || !workspaceDirectory.canRead() || !workspaceDirectory.canWrite()) throw new RuntimeException("The workspace directory " + workspaceDirectory.getAbsolutePath() + " cannot be used!");
    }

    @Override
    public final void resolve() {
        initializeWorkspaceDirectory();
    }

    /**
     * 
     * @return files of the projects in the file system
     */
    final File[] getProjectFiles() {
        File[] files = workspaceDirectory.listFiles(projectFileFilter);
        return (files == null ? new File[0] : files);
    }

    /**
     * 
     * @return names of the projects in the file system
     */
    final String[] getProjectNames() {
        File[] files = getProjectFiles();
        String[] names = new String[files.length];
        String fullname;
        for (int i = 0; i < names.length; i++) {
            fullname = files[i].getName();
            names[i] = fullname.substring(0, fullname.length() - Project.getFileExtension().length());
        }
        return (names);
    }

    /**
     * load a project from a file
     * @param projectFile project file
     * @return the newly loaded project or the previous one if it had been already loaded
     */
    final Project loadProject(File projectFile) {
        String fullname = projectFile.getName();
        Project project = null;
        if (projectFile.getName().endsWith(Project.getFileExtension())) {
            int nameLength = fullname.length();
            String projectName = fullname.substring(0, nameLength - Project.getFileExtension().length());
            try {
                ZipFile zipFile = new ZipFile(projectFile);
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                ZipEntry entry;
                BufferedImage imageMap;
                String[] path;
                int textureIndex;
                String textureIndexString;
                while (entries.hasMoreElements()) {
                    entry = entries.nextElement();
                    if (entry.getName().equals("project.xml")) {
                        CustomXMLDecoder decoder = new CustomXMLDecoder(zipFile.getInputStream(entry));
                        project = (Project) decoder.readObject();
                        decoder.close();
                    } else {
                        if (!entry.isDirectory()) {
                            path = entry.getName().split("/");
                            if (path.length == 4 && path[0].equals("levelset")) {
                                for (FloorSet floorSet : project.getLevelSet().getFloorSetsList()) for (Floor floor : floorSet.getFloorsList()) if (path[1].equals(floorSet.getName()) && path[2].equals(floor.getName())) {
                                    imageMap = ImageIO.read(zipFile.getInputStream(entry));
                                    for (MapType type : MapType.values()) if (path[3].equals(type.getFilename())) {
                                        floor.getMap(type).setImage(imageMap);
                                        break;
                                    }
                                    break;
                                }
                            } else if (path.length == 2 && path[0].equals("tileset")) {
                                for (Tile tile : project.getTileSet().getTilesList()) if (path[1].startsWith(tile.getName()) && path[1].endsWith(".png")) {
                                    textureIndex = -1;
                                    textureIndexString = path[1].substring(tile.getName().length(), path[1].lastIndexOf(".png"));
                                    try {
                                        textureIndex = Integer.parseInt(textureIndexString);
                                    } catch (NumberFormatException nfe) {
                                    }
                                    if (textureIndex != -1) tile.setTexture(textureIndex, ImageIO.read(zipFile.getInputStream(entry)));
                                }
                            }
                        }
                    }
                }
                zipFile.close();
                if (project != null) {
                    if (!addProject(project)) {
                        for (Project existingProject : projectsList) if (existingProject.getName().equals(projectName)) {
                            project = existingProject;
                        }
                    }
                } else throw new IllegalArgumentException("The project file " + fullname + " does not contain any file named \"project.xml\"!");
            } catch (Throwable throwable) {
                throw new RuntimeException("The project " + projectName + " cannot be loaded!", throwable);
            }
        } else throw new IllegalArgumentException("The file " + fullname + " is not a JFPSM project file!");
        return (project);
    }

    @Override
    final boolean canInstantiateChildren() {
        return (true);
    }

    @Override
    final boolean isOpenable() {
        return (false);
    }

    @Override
    final boolean isRemovable() {
        return (false);
    }
}
