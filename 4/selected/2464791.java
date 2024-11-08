package com.flyox.game.militarychess.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.Hashtable;
import javax.swing.JOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.flyox.game.militarychess.CONSTS;
import com.flyox.game.militarychess.bean.ChessDesk;
import com.flyox.game.militarychess.bean.ChessMan;
import com.flyox.game.militarychess.server.minaserver.MinaServer;
import com.flyox.game.militarychess.server.multicastserver.MulticastServer;
import com.flyox.game.militarychess.server.services.DeskService;
import com.flyox.game.militarychess.server.services.LayoutService;

/**
 * server端入口
 * 
 * @author sunwei
 * 
 */
public class Start {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static void main(String[] args) {
        Start s = new Start();
        if (!s.tryLock()) {
            JOptionPane.showMessageDialog(null, "只能启动一个程序实例！");
        }
        try {
            s.logger.trace("init game desk ...");
            s.initDesks();
            s.logger.trace("Server starting ...");
            MinaServer ms = new MinaServer(CONSTS.SYS_CFG_NIO_PORT);
            ms.start();
            MulticastServer mserver = new MulticastServer();
            Thread t = new Thread(mserver);
            t.start();
        } catch (Exception e) {
            s.logger.trace("Server error: " + e.getMessage());
            System.exit(-1);
        }
    }

    /**
	 * 初始化房间内的桌面信息
	 */
    private void initDesks() {
        DeskService chessDeskService = new DeskService();
        LayoutService layoutService = new LayoutService();
        Hashtable<Integer, ChessDesk> desks = chessDeskService.getDesks();
        Hashtable<Integer, ChessMan[][]> chessLayouts = layoutService.getChessLayouts();
        for (int i = 0; i < CONSTS.defaultDeskNum; i++) {
            desks.put(i, new ChessDesk(i));
            chessLayouts.put(i, new ChessMan[CONSTS.rows][CONSTS.cols]);
        }
    }

    private boolean tryLock() {
        FileOutputStream fo = null;
        File file = new File(".lock");
        try {
            fo = new FileOutputStream(file);
            FileLock lock = fo.getChannel().tryLock();
            if (lock == null) {
                logger.warn("<Warning> <Could not start epayment. this application is still alive.>");
                return false;
            } else {
                return true;
            }
        } catch (FileNotFoundException e) {
            logger.error("checkSftp: lock file can't be created.error:" + e);
            return false;
        } catch (IOException e) {
            logger.error("checkSftp: lock file can't be created.error:" + e);
            return false;
        }
    }
}
