package org.lifxue.jqda.util;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *运行实例监测类
 * @author lifxue
 */
public class InstanceRunning {

    private static Log logger = LogFactory.getLog(InstanceRunning.class);

    /**
     * 依据特定文件是否被锁定，来判断受否有实例在运行
     * 如果其他实例在运行，结束本实例
     */
    public static void isInstanceRunning() {
        RandomAccessFile f = null;
        FileChannel fc = null;
        FileLock fl = null;
        try {
            File temp = new File(GlobalVar.USER_HOME + GlobalVar.FILE_PATH_DELIMITER + GlobalConstant.DEFAULT_CONFIG_DIR + GlobalVar.FILE_PATH_DELIMITER + GlobalConstant.LOCK);
            File tempFolder = temp.getParentFile();
            if (tempFolder == null || !tempFolder.exists()) {
                tempFolder.mkdir();
            }
            if (temp == null || !temp.exists()) {
                temp.createNewFile();
            }
            f = new RandomAccessFile(GlobalVar.USER_HOME + GlobalVar.FILE_PATH_DELIMITER + GlobalConstant.DEFAULT_CONFIG_DIR + GlobalVar.FILE_PATH_DELIMITER + GlobalConstant.LOCK, "rwd");
            fc = f.getChannel();
            fl = fc.tryLock();
            if (!fl.isValid()) {
                logger.debug("另外一个实例正在运行");
                System.exit(1);
            }
        } catch (Exception e) {
            logger.debug("另外一个实例正在运行");
            System.exit(1);
        }
    }
}
