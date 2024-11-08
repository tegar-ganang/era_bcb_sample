package org.heresylabs.netbeans.p4;

import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.Action;
import javax.swing.JOptionPane;
import org.heresylabs.netbeans.p4.FileStatusProvider.Status;
import org.heresylabs.netbeans.p4.actions.AddAction;
import org.heresylabs.netbeans.p4.actions.DeleteAction;
import org.heresylabs.netbeans.p4.actions.DiffAction;
import org.heresylabs.netbeans.p4.actions.DiffExternalAction;
import org.heresylabs.netbeans.p4.actions.EditAction;
import org.heresylabs.netbeans.p4.actions.OptionsAction;
import org.heresylabs.netbeans.p4.actions.RefreshAction;
import org.heresylabs.netbeans.p4.actions.RefreshRecursivelyAction;
import org.heresylabs.netbeans.p4.actions.RevertAction;
import org.heresylabs.netbeans.p4.actions.SyncAction;
import org.heresylabs.netbeans.p4.actions.SyncForceAction;
import org.netbeans.modules.versioning.spi.VCSAnnotator;
import org.netbeans.modules.versioning.spi.VCSAnnotator.ActionDestination;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.netbeans.modules.versioning.spi.VCSInterceptor;
import org.netbeans.modules.versioning.spi.VersioningSupport;
import org.netbeans.modules.versioning.spi.VersioningSystem;
import org.openide.cookies.SaveCookie;
import org.openide.nodes.Node;
import org.openide.util.NbPreferences;
import org.openide.util.actions.SystemAction;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;

/**
 *
 * @author Aekold Helbrass <Helbrass@gmail.com>
 */
public class PerforceVersioningSystem extends VersioningSystem {

    public static final String NAME = "Perforce";

    private static final String KEY_CONNECTIONS = "connections";

    private static final String KEY_PREFERENCES = "preferences";

    private static PerforceVersioningSystem INSTANCE;

    /**
     * Singleton provider.
     * @return
     */
    public static PerforceVersioningSystem getInstance() {
        if (INSTANCE == null) {
            logWarning(PerforceVersioningSystem.class, "PerforceVersioningSystem singleton is null");
        }
        return INSTANCE;
    }

    /**
     * Counstructs and inits perforce support, and assignes self to static singleton
     */
    public PerforceVersioningSystem() {
        synchronized (PerforceVersioningSystem.class) {
            if (INSTANCE != null) {
                logWarning(this, "PerforceVersioningSystem constructed again");
            }
            INSTANCE = this;
        }
        putProperty(PROP_DISPLAY_NAME, NAME);
        putProperty(PROP_MENU_LABEL, NAME);
        init();
    }

    private void init() {
        Preferences preferences = NbPreferences.forModule(getClass());
        loadConnections(preferences);
        String prefs = preferences.get(KEY_PREFERENCES, null);
        if (prefs == null) {
            perforcePreferences = new PerforcePreferences();
        } else {
            perforcePreferences = parsePreferences(prefs);
        }
        initPerformanceHacks();
    }

    /**
     * Small performance hack to check for workspaces as fast as possible
     */
    private void initPerformanceHacks() {
        workspaces = new String[connections.size()];
        for (int i = 0; i < connections.size(); i++) {
            Connection connection = connections.get(i);
            workspaces[i] = perforcePreferences.isCaseSensetiveWorkspaces() ? connection.getWorkspacePath() : connection.getWorkspacePath().toLowerCase();
        }
    }

    private String[] workspaces;

    private Annotator annotator = new Annotator();

    private Interceptor interceptor = new Interceptor();

    @Override
    public void getOriginalFile(File workingCopy, File originalFile) {
        Status status = fileStatusProvider.getFileStatus(workingCopy);
        if (status != Status.EDIT && status != Status.OUTDATED) {
            return;
        }
        String originalPath;
        try {
            originalPath = originalFile.getCanonicalPath();
        } catch (Exception e) {
            originalPath = originalFile.getAbsolutePath();
        }
        wrapper.execute(workingCopy, "print", "-o", originalPath, "-q");
    }

    @Override
    public File getTopmostManagedAncestor(File file) {
        Connection c = getConnectionForFile(file);
        if (c == null) {
            return null;
        }
        return new File(c.getWorkspacePath());
    }

    @Override
    public VCSAnnotator getVCSAnnotator() {
        return annotator;
    }

    @Override
    public VCSInterceptor getVCSInterceptor() {
        return interceptor;
    }

    private List<Connection> connections = new ArrayList<Connection>();

    /**
     * @return copy of current connections
     */
    public List<Connection> getConnections() {
        return new ArrayList<Connection>(connections);
    }

    /**
     * Set new connections list, with full save and init cycle.
     * @param connections
     */
    public void setConnections(List<Connection> connections) {
        this.connections = connections;
        saveConnections(NbPreferences.forModule(getClass()));
        initPerformanceHacks();
        fireVersionedFilesChanged();
    }

    /**
     * Parse prefs for connections params
     * @param prefs
     */
    private void loadConnections(Preferences prefs) {
        List<String> connectionsStrings = getStringList(prefs, KEY_CONNECTIONS);
        if (connectionsStrings == null || connectionsStrings.isEmpty()) {
            return;
        }
        List<Connection> conns = new ArrayList<Connection>(connectionsStrings.size());
        for (int i = 0; i < connectionsStrings.size(); i++) {
            String string = connectionsStrings.get(i);
            conns.add(parseConnection(string));
        }
        connections = conns;
    }

    /**
     * Save all connection params to NbPreferences.
     * @param prefs
     */
    private void saveConnections(Preferences prefs) {
        List<String> conns = new ArrayList<String>(connections.size());
        for (int i = 0; i < connections.size(); i++) {
            conns.add(getConnectionAsString(connections.get(i)));
        }
        putStringList(prefs, KEY_CONNECTIONS, conns);
    }

    private PerforcePreferences perforcePreferences;

    public PerforcePreferences getPerforcePreferences() {
        return new PerforcePreferences(perforcePreferences.isCaseSensetiveWorkspaces(), perforcePreferences.isConfirmEdit(), perforcePreferences.isInterceptAdd(), perforcePreferences.isPrintOutput(), perforcePreferences.isShowAction(), perforcePreferences.getColorBase(), perforcePreferences.getColorLocal(), perforcePreferences.getColorUnknown(), perforcePreferences.getColorAdd(), perforcePreferences.getColorDelete(), perforcePreferences.getColorEdit(), perforcePreferences.getColorOutdated());
    }

    public void setPerforcePreferences(PerforcePreferences perforcePreferences) {
        this.perforcePreferences = perforcePreferences;
        Preferences preferences = NbPreferences.forModule(getClass());
        preferences.put(KEY_PREFERENCES, getPreferencesAsString(perforcePreferences));
        initPerformanceHacks();
    }

    /**
     * Implementation of Actions getter.
     * @see org.netbeans.modules.versioning.spi.VCSAnnotator#getActions(org.netbeans.modules.versioning.spi.VCSContext, org.netbeans.modules.versioning.spi.VCSAnnotator.ActionDestination)
     */
    private Action[] getPerforceActions(VCSContext context, ActionDestination destination) {
        if (destination == ActionDestination.PopupMenu) {
            return asArray(SystemAction.get(DiffAction.class), SystemAction.get(DiffExternalAction.class), null, SystemAction.get(AddAction.class), SystemAction.get(DeleteAction.class), null, SystemAction.get(RevertAction.class), null, SystemAction.get(EditAction.class), null, SystemAction.get(SyncAction.class), SystemAction.get(SyncForceAction.class), null, SystemAction.get(RefreshAction.class), SystemAction.get(RefreshRecursivelyAction.class));
        }
        return asArray(SystemAction.get(DiffAction.class), SystemAction.get(DiffExternalAction.class), null, SystemAction.get(AddAction.class), SystemAction.get(DeleteAction.class), null, SystemAction.get(RevertAction.class), null, SystemAction.get(EditAction.class), null, SystemAction.get(SyncAction.class), SystemAction.get(SyncForceAction.class), null, SystemAction.get(RefreshAction.class), SystemAction.get(RefreshRecursivelyAction.class), null, optionsAction);
    }

    /**
     * Find connection for given file. This method allows user to work with multiple workspaces, returning connection which workspace
     * includes specified file.
     */
    public Connection getConnectionForFile(File file) {
        if (file == null) {
            return null;
        }
        String filePath;
        try {
            filePath = file.getCanonicalPath();
        } catch (Exception e) {
            filePath = file.getAbsolutePath();
        }
        if (!perforcePreferences.isCaseSensetiveWorkspaces()) {
            filePath = filePath.toLowerCase();
        }
        for (int i = 0; i < workspaces.length; i++) {
            if (filePath.startsWith(workspaces[i])) {
                return connections.get(i);
            }
        }
        return null;
    }

    private CliWrapper wrapper = new CliWrapper();

    private FileStatusProvider fileStatusProvider = new FileStatusProvider();

    private final OptionsAction optionsAction = new OptionsAction();

    /**
     * @return wrapper class for command-line p4
     */
    public CliWrapper getWrapper() {
        return wrapper;
    }

    private void edit(File file) {
        wrapper.execute(file, "edit");
        refresh(file);
    }

    private void add(File file) {
        wrapper.execute(file, "add");
        refresh(file);
    }

    private void delete(File file) {
        wrapper.execute(file, "delete");
        refresh(file);
    }

    private void revert(File file) {
        wrapper.execute(file, "revert");
        refresh(file);
    }

    public void p4merge(File file) {
        String tmpPath = System.getProperty("java.io.tmpdir");
        File remoteFile = new File(tmpPath, file.getName() + System.currentTimeMillis());
        remoteFile.deleteOnExit();
        getOriginalFile(file, remoteFile);
        try {
            Runtime.getRuntime().exec("p4merge \"" + remoteFile.getCanonicalPath() + "\" \"" + file.getCanonicalPath() + "\"", null);
        } catch (Exception e) {
            logError(this, e);
        }
    }

    /**
     * Send set of files to background status refresh
     * @param files files to refresh
     */
    public void refresh(Set<File> files) {
        if (files != null && fileStatusProvider != null) {
            fileStatusProvider.refreshAsync(files.toArray(new File[files.size()]));
        }
    }

    /**
     * Send file to background refresh.
     * @param file file to refresh.
     */
    public void refresh(File file) {
        if (file != null && fileStatusProvider != null) {
            fileStatusProvider.refreshAsync(file);
        }
    }

    /**
     * Returns status of file from cache. If status was not found - file will be sent to background refresh and {@code UNKNOWN} status will
     * be returned.
     * @param file
     * @return {@code UNKNOWN} if status was not found in cache, valid status otherwise.
     */
    public Status getFileStatus(File file) {
        if (file != null && fileStatusProvider != null) {
            return fileStatusProvider.getFileStatus(file);
        }
        return Status.UNKNOWN;
    }

    private String annotatePerforceName(String name, VCSContext context) {
        if (context.getFiles().size() > 1) {
            return name;
        }
        File file = context.getRootFiles().iterator().next();
        if (file.isFile()) {
            Status status = fileStatusProvider.getFileStatus(file);
            String suffix;
            String nameColor = perforcePreferences.getColorBase();
            if (status == null) {
                return name;
            }
            if (status.isLocal()) {
                suffix = "Local Only";
                nameColor = perforcePreferences.getColorLocal();
            } else if (status.isUnknown()) {
                suffix = "...";
                nameColor = perforcePreferences.getColorUnknown();
            } else {
                suffix = fileStatusProvider.getFileRevision(file);
                switch(status) {
                    case ADD:
                        {
                            nameColor = perforcePreferences.getColorAdd();
                            break;
                        }
                    case DELETE:
                        {
                            nameColor = perforcePreferences.getColorDelete();
                            break;
                        }
                    case EDIT:
                        {
                            nameColor = perforcePreferences.getColorEdit();
                            break;
                        }
                    case OUTDATED:
                        {
                            nameColor = perforcePreferences.getColorOutdated();
                            break;
                        }
                }
            }
            StringBuilder nameBuilder = new StringBuilder();
            nameBuilder.append("<font color=\"#");
            nameBuilder.append(nameColor);
            nameBuilder.append("\">");
            nameBuilder.append(name);
            nameBuilder.append("</font>");
            boolean annotationsVisible = VersioningSupport.getPreferences().getBoolean(VersioningSupport.PREF_BOOLEAN_TEXT_ANNOTATIONS_VISIBLE, false);
            if (annotationsVisible) {
                nameBuilder.append("   <font color=\"#999999\">[ ");
                nameBuilder.append(suffix);
                if (perforcePreferences.isShowAction()) {
                    switch(status) {
                        case ADD:
                            {
                                nameBuilder.append(" : Add");
                                break;
                            }
                        case EDIT:
                            {
                                nameBuilder.append(" : Edit");
                                break;
                            }
                        case DELETE:
                            {
                                nameBuilder.append(" : Deleted");
                                break;
                            }
                        default:
                            break;
                    }
                }
                nameBuilder.append(" ]</font>");
            }
            return nameBuilder.toString();
        }
        return name;
    }

    /**
     * Just invoking protected method for outer classes.
     * @param files
     * @see #fireStatusChanged(java.io.File)
     * @see #fireStatusChanged(java.util.Set)
     */
    public void fireFilesRefreshed(Set<File> files) {
        if (files != null) {
            fireStatusChanged(files);
        }
    }

    /**
     * Sorry NB guys, but "friends only" restriction for Util classes is not right!
     */
    private static List<String> getStringList(Preferences prefs, String key) {
        List<String> retval = new ArrayList<String>();
        try {
            String[] keys = prefs.keys();
            for (int i = 0; i < keys.length; i++) {
                String k = keys[i];
                if (k != null && k.startsWith(key)) {
                    int idx = Integer.parseInt(k.substring(k.lastIndexOf('.') + 1));
                    retval.add(idx + "." + prefs.get(k, null));
                }
            }
            List<String> rv = new ArrayList<String>(retval.size());
            rv.addAll(retval);
            for (String s : retval) {
                int pos = s.indexOf('.');
                int index = Integer.parseInt(s.substring(0, pos));
                rv.set(index, s.substring(pos + 1));
            }
            return rv;
        } catch (Exception ex) {
            Logger.getLogger(PerforceVersioningSystem.class.getName()).log(Level.INFO, null, ex);
            return new ArrayList<String>(0);
        }
    }

    /**
     * Sorry NB guys, but "friends only" restriction for Util classes is not right!
     */
    private static void putStringList(Preferences prefs, String key, List<String> value) {
        try {
            String[] keys = prefs.keys();
            for (int i = 0; i < keys.length; i++) {
                String k = keys[i];
                if (k != null && k.startsWith(key + ".")) {
                    prefs.remove(k);
                }
            }
            int idx = 0;
            for (String s : value) {
                prefs.put(key + "." + idx++, s);
            }
        } catch (BackingStoreException ex) {
            Logger.getLogger(PerforceVersioningSystem.class.getName()).log(Level.INFO, null, ex);
        }
    }

    private static final String RC_DELIMITER = "~=~";

    private static String getConnectionAsString(Connection connection) {
        StringBuilder sb = new StringBuilder();
        sb.append(connection.getServer());
        sb.append(RC_DELIMITER);
        sb.append(connection.getUser());
        sb.append(RC_DELIMITER);
        sb.append(connection.getClient());
        sb.append(RC_DELIMITER);
        sb.append(connection.getPassword());
        sb.append(RC_DELIMITER);
        sb.append(connection.getWorkspacePath());
        return sb.toString();
    }

    private static Connection parseConnection(String string) {
        String[] lines = string.split(RC_DELIMITER);
        return new Connection(lines[0], lines[1], lines[2], lines[3], lines[4]);
    }

    private static String getPreferencesAsString(PerforcePreferences p) {
        StringBuilder sb = new StringBuilder();
        sb.append(p.isCaseSensetiveWorkspaces() ? 't' : 'f');
        sb.append(p.isConfirmEdit() ? 't' : 'f');
        sb.append(p.isInterceptAdd() ? 't' : 'f');
        sb.append(p.isPrintOutput() ? 't' : 'f');
        sb.append(p.isShowAction() ? 't' : 'f');
        sb.append(p.isInvalidateOnRefresh() ? 't' : 'f');
        sb.append(RC_DELIMITER);
        sb.append(p.getColorAdd());
        sb.append(RC_DELIMITER);
        sb.append(p.getColorBase());
        sb.append(RC_DELIMITER);
        sb.append(p.getColorDelete());
        sb.append(RC_DELIMITER);
        sb.append(p.getColorEdit());
        sb.append(RC_DELIMITER);
        sb.append(p.getColorLocal());
        sb.append(RC_DELIMITER);
        sb.append(p.getColorOutdated());
        sb.append(RC_DELIMITER);
        sb.append(p.getColorUnknown());
        return sb.toString();
    }

    private static PerforcePreferences parsePreferences(String s) {
        PerforcePreferences p = new PerforcePreferences();
        p.setCaseSensetiveWorkspaces(s.charAt(0) == 't');
        p.setConfirmEdit(s.charAt(1) == 't');
        p.setInterceptAdd(s.charAt(2) == 't');
        p.setPrintOutput(s.charAt(3) == 't');
        p.setShowAction(s.charAt(4) == 't');
        p.setInvalidateOnRefresh(s.charAt(5) == 't');
        String[] colors = s.split(RC_DELIMITER);
        p.setColorAdd(colors[1]);
        p.setColorBase(colors[2]);
        p.setColorDelete(colors[3]);
        p.setColorEdit(colors[4]);
        p.setColorLocal(colors[5]);
        p.setColorOutdated(colors[6]);
        p.setColorUnknown(colors[7]);
        return p;
    }

    public static void saveNodes(Node[] nodes) {
        for (int i = 0; i < nodes.length; i++) {
            Node node = nodes[i];
            SaveCookie save = node.getCookie(SaveCookie.class);
            if (save != null) {
                try {
                    save.save();
                } catch (Exception e) {
                    logError(PerforceVersioningSystem.class, e);
                }
            }
        }
    }

    public static void logError(Object caller, Throwable e) {
        Logger.getLogger(caller.getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
    }

    public static void logWarning(Object caller, String warning) {
        Logger.getLogger(caller.getClass().getName()).log(Level.WARNING, warning);
    }

    public static void print(boolean error, String... messageArgs) {
        if (!error && !getInstance().perforcePreferences.isPrintOutput()) {
            return;
        }
        InputOutput io = IOProvider.getDefault().getIO("Perforce", false);
        OutputWriter out = error ? io.getErr() : io.getOut();
        out.print('[');
        out.print(getTime());
        out.print("] ");
        boolean isPasswd = false;
        for (String av : messageArgs) {
            out.print(isPasswd ? "****" : av);
            out.print(' ');
            isPasswd = "-P".equals(av);
        }
        out.println();
        out.flush();
    }

    private static final Date currentDate = new Date();

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    private static String getTime() {
        synchronized (dateFormat) {
            currentDate.setTime(System.currentTimeMillis());
            return dateFormat.format(currentDate);
        }
    }

    /**
     * Utility method to convert vararg to array
     */
    private static <T> T[] asArray(T... arg) {
        return arg;
    }

    private int showConfirmation(String message, String filename) {
        if (filename.length() > 60) {
            filename = filename.substring(0, 60) + '\n' + filename.substring(60);
        }
        String[] options = { "Yes", "No" };
        int res = JOptionPane.showOptionDialog(null, message + filename, "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        return res;
    }

    private class Annotator extends VCSAnnotator {

        @Override
        public Image annotateIcon(Image icon, VCSContext context) {
            return super.annotateIcon(icon, context);
        }

        @Override
        public String annotateName(String name, VCSContext context) {
            return annotatePerforceName(name, context);
        }

        @Override
        public Action[] getActions(VCSContext context, ActionDestination destination) {
            return getPerforceActions(context, destination);
        }
    }

    private class Interceptor extends VCSInterceptor {

        @Override
        public boolean isMutable(File file) {
            return super.isMutable(file);
        }

        @Override
        public boolean beforeDelete(File file) {
            return file.isFile() && !fileStatusProvider.getFileStatusForce(file).isLocal();
        }

        @Override
        public void doDelete(File file) throws IOException {
            int res = showConfirmation("Are you sure you want to delete ", file.getAbsolutePath());
            if (res == JOptionPane.NO_OPTION) {
                return;
            }
            Status status = fileStatusProvider.getFileStatusForce(file);
            if (status == null) {
                return;
            }
            if (status.isLocal()) {
                logWarning(this, file.getName() + " is not revisioned. Should not be deleted by p4nb");
                return;
            }
            if (status != Status.NONE) {
                revert(file);
            }
            if (status == Status.ADD) {
                file.delete();
                return;
            }
            delete(file);
        }

        @Override
        public boolean beforeMove(File from, File to) {
            if (from.isFile()) {
                return !fileStatusProvider.getFileStatusForce(from).isLocal();
            }
            return false;
        }

        @Override
        public void doMove(File from, File to) throws IOException {
            int res = showConfirmation("File will be moved in p4, are you sure to move ", from.getAbsolutePath());
            if (res == JOptionPane.NO_OPTION) {
                return;
            }
            Status status = fileStatusProvider.getFileStatusForce(from);
            if (status == null) {
                return;
            }
            if (status.isLocal()) {
                logWarning(this, from.getName() + " is not revisioned. Should not be deleted by p4nb");
                return;
            }
            to.getParentFile().mkdirs();
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(from));
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(to));
            byte[] buffer = new byte[8192];
            int read = 0;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
            if (status != Status.NONE) {
                revert(from);
            }
            if (status != Status.ADD) {
                delete(from);
            } else {
                from.delete();
            }
            add(to);
        }

        @Override
        public void afterMove(File from, File to) {
            super.afterMove(from, to);
        }

        @Override
        public void afterCreate(File file) {
            if (file.isFile() && perforcePreferences.isInterceptAdd()) {
                add(file);
            }
        }

        @Override
        public void beforeEdit(File file) {
            if (file.canWrite()) {
                return;
            }
            if (perforcePreferences.isConfirmEdit()) {
                int res = showConfirmation("Are you sure you want to \"p4 edit\" file \n", file.getAbsolutePath());
                if (res == JOptionPane.NO_OPTION) {
                    return;
                }
            }
            edit(file);
        }
    }
}
