package org.sss.housekeeping.store.file;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.sss.housekeeping.cfg.StoreConfig;
import org.sss.housekeeping.cfg.TaskConfig;
import org.sss.housekeeping.store.BaseStore;
import org.sss.housekeeping.store.FileStore;
import org.sss.housekeeping.util.ResultTree;

/**
 * FTP協議文件系統訪問
 * @author Jason Hoo (latest modification by $Author: hujianxin78728 $)
 * @version $Revision: 406 $ $Date: 2009-06-10 08:09:48 -0400 (Wed, 10 Jun 2009) $
 */
public class FTPStore extends FileStore {

    private static Log log = LogFactory.getLog(FTPStore.class);

    private FTPClient client;

    public FTPStore(StoreConfig config) {
        super(config);
    }

    public boolean login() {
        if (super.isAuthenticated()) return true;
        try {
            if (client == null) {
                client = new FTPClient();
                FTPClientConfig config = new FTPClientConfig();
                client.configure(config);
            }
            if (!client.isConnected()) {
                client.connect(super.getStoreConfig().getServerName(), new Integer(super.getStoreConfig().getServerPort()).intValue());
            }
            if (client.login(super.getStoreConfig().getUserName(), super.getStoreConfig().getPassword(), super.getStoreConfig().getServerName())) {
                super.setAuthenticated(true);
                return true;
            }
            log.error("Login ftp server error");
        } catch (Exception e) {
            log.info("FTPStore.login", e);
        }
        return false;
    }

    public void logout() {
        if (client != null && client.isConnected()) {
            try {
                client.logout();
                client.disconnect();
            } catch (Exception e) {
            }
        }
        super.setAuthenticated(false);
    }

    public List pre_task(TaskConfig taskConfig) {
        List list = new ArrayList();
        return list;
    }

    public void do_copy(BaseStore targetStore, ResultTree resultTree) {
    }

    public void do_move(BaseStore targetStore, ResultTree resultTree) {
    }

    public void do_delete(BaseStore targetStore, ResultTree resultTree) {
    }

    public void do_report(ResultTree resultTree) {
    }
}
