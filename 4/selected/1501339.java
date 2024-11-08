package org.gudy.azureus2.pluginsimpl.local.sharing.test;

import java.util.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.sharing.*;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.peers.*;
import org.gudy.azureus2.core3.util.*;

public class ShareTester implements Plugin, PluginListener, ShareManagerListener {

    protected static AESemaphore init_sem = new AESemaphore("ShareTester");

    private static AEMonitor class_mon = new AEMonitor("ShareTester");

    protected static ShareTester singleton;

    protected Map seed_transport_map = new HashMap();

    public static ShareTester getSingleton() {
        try {
            class_mon.enter();
            if (singleton == null) {
                new AEThread("plugin initialiser ") {

                    public void runSupport() {
                        PluginManager.registerPlugin(ShareTester.class);
                        Properties props = new Properties();
                        props.put(PluginManager.PR_MULTI_INSTANCE, "true");
                        PluginManager.startAzureus(PluginManager.UI_SWT, props);
                    }
                }.start();
                init_sem.reserve();
            }
            return (singleton);
        } finally {
            class_mon.exit();
        }
    }

    protected PluginInterface plugin_interface;

    public void initialize(PluginInterface _pi) {
        plugin_interface = _pi;
        singleton = this;
        init_sem.release();
        LoggerChannel log = plugin_interface.getLogger().getChannel("Plugin Test");
        log.log(LoggerChannel.LT_INFORMATION, "Plugin Initialised");
        plugin_interface.addListener(this);
    }

    public void initializationComplete() {
        try {
            DownloadManager dm = plugin_interface.getDownloadManager();
            dm.addListener(new DownloadManagerListener() {

                public void downloadAdded(final Download download) {
                    System.out.println("downloadAdded: " + download);
                    download.addListener(new DownloadListener() {

                        public void stateChanged(Download dl, int old, int cur) {
                            System.out.println("statechange:" + old + "-> " + cur + "  (" + download + ")");
                        }

                        public void positionChanged(Download download, int old, int cur) {
                            System.out.println("statechange:" + old + "-> " + cur + "  (" + download + ")");
                        }
                    });
                    download.addTrackerListener(new DownloadTrackerListener() {

                        public void scrapeResult(DownloadScrapeResult result) {
                            System.out.println("scrapeResult:" + result.getSeedCount() + "/" + result.getNonSeedCount());
                        }

                        public void announceResult(DownloadAnnounceResult result) {
                            if (result.getResponseType() == DownloadAnnounceResult.RT_SUCCESS) {
                                System.out.println("announceResult:" + result.getReportedPeerCount() + "/" + result.getSeedCount() + "/" + result.getNonSeedCount());
                            } else {
                                System.out.println("announceResult:" + result.getError());
                            }
                        }
                    });
                    download.addPeerListener(new DownloadPeerListener() {

                        public void peerManagerAdded(Download download, PeerManager peer_manager) {
                            peer_manager.addListener(new PeerManagerListener() {

                                public void peerAdded(PeerManager manager, Peer peer) {
                                    System.out.println("peerAdded:" + peer.getIp());
                                }

                                public void peerRemoved(PeerManager manager, Peer peer) {
                                    System.out.println("peerRemoved:" + peer.getIp());
                                }
                            });
                        }

                        public void peerManagerRemoved(Download download, PeerManager peer_manager) {
                        }
                    });
                }

                public void downloadRemoved(Download download) {
                    System.out.println("downloadRemoved" + download);
                }
            });
            ShareManager sm = plugin_interface.getShareManager();
            sm.addListener(this);
            sm.initialise();
        } catch (ShareException e) {
            Debug.printStackTrace(e);
        } catch (Throwable e) {
            Debug.printStackTrace(e);
        }
    }

    public void closedownInitiated() {
    }

    public void closedownComplete() {
    }

    public void resourceAdded(ShareResource resource) {
        System.out.println("resource added:" + resource.getName());
        if (resource.getType() == ShareResource.ST_DIR_CONTENTS) {
            ShareResourceDirContents c = (ShareResourceDirContents) resource;
            ShareResource[] kids = c.getChildren();
            for (int i = 0; i < kids.length; i++) {
                System.out.println("\t" + kids[i].getName());
            }
        }
    }

    public void resourceModified(ShareResource old_resource, ShareResource new_resource) {
        System.out.println("resource modified:" + old_resource.getName());
    }

    public void resourceDeleted(ShareResource resource) {
        System.out.println("resource deleted:" + resource.getName());
    }

    public void reportProgress(int percent_complete) {
    }

    public void reportCurrentTask(String task_description) {
        System.out.println(task_description);
    }

    public static void main(String[] args) {
        getSingleton();
    }
}
