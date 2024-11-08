package tufts.vue;

import javax.swing.*;
import java.util.Vector;
import java.util.*;
import java.awt.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileSystemView;
import java.io.*;
import osid.filing.*;
import tufts.oki.remoteFiling.*;
import tufts.oki.localFiling.*;
import tufts.oki.shared.*;
import tufts.vue.action.*;
import tufts.Util;

/**
 * @version $Revision: 1.35 $ / $Date: 2010-02-03 19:17:40 $ / $Author: mike $
 * @author  rsaigal
 */
public class LocalFileDataSource extends BrowseDataSource implements Publishable {

    private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(LocalFileDataSource.class);

    private static final LocalFilingManager LocalFileManager = produceManager();

    private static LocalFilingManager produceManager() {
        try {
            return new LocalFilingManager();
        } catch (FilingException t) {
            tufts.Util.printStackTrace(t, "new LocalFilingManager");
        }
        return null;
    }

    public static LocalFilingManager getLocalFilingManager() {
        return LocalFileManager;
    }

    public LocalFileDataSource() {
    }

    public LocalFileDataSource(String displayName, String address) throws DataSourceException {
        if (DEBUG.DR) out("NEW: name=" + Util.tags(displayName) + "; address=" + Util.tag(address) + "; " + address);
        this.setDisplayName(displayName);
        this.setAddress(address);
    }

    @Override
    public String getTypeName() {
        return "Local Directory";
    }

    @Override
    protected JComponent buildResourceViewer() {
        if (DEBUG.Enabled) out("buildResourceViewer...");
        Vector cabVector = new Vector();
        if (getDisplayName().equals("My Computer")) {
            if (DEBUG.Enabled) out("installDesktopFolders...");
            installDesktopFolders(cabVector);
            if (DEBUG.Enabled) out("installDesktopFolders: " + cabVector);
        }
        if (this.getAddress().length() > 0) {
            osid.shared.Agent agent = null;
            LocalCabinet rootNode = LocalCabinet.instance(this.getAddress(), agent, null);
            CabinetResource res = CabinetResource.create(rootNode);
            cabVector.add(res);
        }
        VueDragTree fileTree = new VueDragTree(cabVector, this.getDisplayName());
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);
        fileTree.expandRow(0);
        fileTree.setRootVisible(false);
        if (DEBUG.Enabled) out("buildResourceViewer: completed.");
        return fileTree;
    }

    private void installDesktopFolders(Vector cabVector) {
        osid.shared.Agent agent = null;
        File home = new File(VUE.getSystemProperty("user.home"));
        if (home.exists() && home.canRead()) {
            String[] dirs = { "Desktop", "My Documents", "Documents", "Pictures", "My Documents\\My Pictures", "Photos", "My Documents\\My Photos", "Music", "My Documents\\My Music" };
            int added = 0;
            for (int i = 0; i < dirs.length; i++) {
                File dir = new File(home, dirs[i]);
                if (dir.exists() && dir.canRead() && dir.isDirectory()) {
                    CabinetResource r = CabinetResource.create(LocalCabinet.instance(dir, agent, null));
                    cabVector.add(r);
                    added++;
                }
            }
            if (added == 0 || tufts.Util.isWindowsPlatform() == false) {
                CabinetResource r = CabinetResource.create(LocalCabinet.instance(home, agent, null));
                String title = "Home";
                String user = VUE.getSystemProperty("user.name");
                if (user != null) title += " (" + user + ")";
                r.setTitle(title);
                cabVector.add(r);
            }
        }
        boolean gotSlash = false;
        File volumes = null;
        if (tufts.Util.isMacPlatform()) {
            volumes = new File("/Volumes");
        } else if (tufts.Util.isUnixPlatform()) {
            volumes = new File("/mnt");
        }
        if (volumes != null && volumes.exists() && volumes.canRead()) {
            File[] vols = volumes.listFiles();
            for (int i = 0; i < vols.length; i++) {
                File v = vols[i];
                if (!v.canRead() || v.getName().startsWith(".")) continue;
                CabinetResource r = CabinetResource.create(LocalCabinet.instance(v, agent, null));
                r.setTitle(v.getName());
                try {
                    if (v.getCanonicalPath().equals("/")) gotSlash = true;
                } catch (Exception e) {
                    System.err.println(e);
                }
                cabVector.add(r);
            }
        }
        try {
            FileSystemView fsview = null;
            if (!tufts.Util.isWindowsPlatform()) fsview = FileSystemView.getFileSystemView();
            final LocalFilingManager manager = getLocalFilingManager();
            LocalCabinetEntryIterator rootCabs = (LocalCabinetEntryIterator) manager.listRoots();
            while (rootCabs.hasNext()) {
                LocalCabinetEntry rootNode = (LocalCabinetEntry) rootCabs.next();
                CabinetResource res = CabinetResource.create(rootNode);
                if (rootNode instanceof LocalCabinet) {
                    File f = ((LocalCabinet) rootNode).getFile();
                    try {
                        if (f.getCanonicalPath().equals("/") && gotSlash) continue;
                    } catch (Exception e) {
                        System.err.println(e);
                    }
                    String sysName = null;
                    if (!tufts.Util.isWindowsPlatform()) sysName = fsview.getSystemDisplayName(f); else sysName = f.toString();
                    if (sysName != null) res.setTitle(sysName);
                }
                cabVector.add(res);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            VueUtil.alert(null, ex.getMessage(), VueResources.getString("dialog.settingreserroe.title"));
        }
    }

    public int[] getPublishableModes() {
        int modes[] = { Publishable.PUBLISH_MAP, Publishable.PUBLISH_CMAP, Publishable.PUBLISH_ZIP };
        return modes;
    }

    public boolean supportsMode(int mode) {
        if (mode == Publishable.PUBLISH_ALL) return false; else return true;
    }

    public void publish(int mode, LWMap map) throws IOException {
        System.out.println("ZIP File: " + map.getFile() + "," + map + ", mode:" + mode);
        if (mode == Publishable.PUBLISH_MAP) publishMap(map); else if (mode == Publishable.PUBLISH_CMAP) publishCMap(map); else if (mode == Publishable.PUBLISH_ALL) publishAll(map); else if (mode == Publishable.PUBLISH_ZIP) publishZip(map);
    }

    private void publishMap(LWMap map) throws IOException {
        File savedMap = PublishUtil.saveMap(map);
        InputStream istream = new BufferedInputStream(new FileInputStream(savedMap));
        OutputStream ostream = new BufferedOutputStream(new FileOutputStream(ActionUtil.selectFile("ConceptMap", "vue")));
        int fileLength = (int) savedMap.length();
        byte bytes[] = new byte[fileLength];
        while (istream.read(bytes, 0, fileLength) != -1) ostream.write(bytes, 0, fileLength);
        istream.close();
        ostream.close();
    }

    private void publishCMap(LWMap map) throws IOException {
        try {
            File savedCMap = PublishUtil.createIMSCP(Publisher.resourceVector);
            InputStream istream = new BufferedInputStream(new FileInputStream(savedCMap));
            OutputStream ostream = new BufferedOutputStream(new FileOutputStream(ActionUtil.selectFile("IMSCP", "zip")));
            int fileLength = (int) savedCMap.length();
            byte bytes[] = new byte[fileLength];
            while (istream.read(bytes, 0, fileLength) != -1) ostream.write(bytes, 0, fileLength);
            istream.close();
            ostream.close();
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            System.out.println(ex);
            VueUtil.alert(VUE.getDialogParent(), VueResources.getString("dialog.export.message") + ex.getMessage(), VueResources.getString("dialog.export.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void publishZip(LWMap map) {
        try {
            if (map.getFile() == null) {
                VueUtil.alert(VueResources.getString("dialog.mapsave.message"), VueResources.getString("dialog.mapsave.title"));
                return;
            }
            File savedCMap = PublishUtil.createZip(map, Publisher.resourceVector);
            InputStream istream = new BufferedInputStream(new FileInputStream(savedCMap));
            OutputStream ostream = new BufferedOutputStream(new FileOutputStream(ActionUtil.selectFile("Export to Zip File", "zip")));
            int fileLength = (int) savedCMap.length();
            byte bytes[] = new byte[fileLength];
            while (istream.read(bytes, 0, fileLength) != -1) ostream.write(bytes, 0, fileLength);
            istream.close();
            ostream.close();
        } catch (Exception ex) {
            System.out.println(ex);
            VueUtil.alert(VUE.getDialogParent(), VueResources.getString("dialog.export.message") + ex.getMessage(), VueResources.getString("dialog.export.title"), JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void publishAll(LWMap map) {
        VueUtil.alert(VUE.getDialogParent(), VueResources.getString("dialog.exportall.message"), VueResources.getString("dialog.export.title"), JOptionPane.PLAIN_MESSAGE);
    }

    private void out(String s) {
        Log.debug(String.format("@%x; %s(addr=%s): %s", System.identityHashCode(this), getDisplayName(), getAddress(), s));
    }
}
