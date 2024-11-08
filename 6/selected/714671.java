package com.generatescape.ftp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.generatescape.baseobjects.CONSTANTS;
import com.generatescape.preferences.PrefPageOne;
import com.generatescape.threading.JobHandler;

/*******************************************************************************
 * Copyright (c) 2005, 2007 GenerateScape and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the GNU General Public License which accompanies this distribution, and is
 * available at http://www.gnu.org/copyleft/gpl.html
 * 
 * @author kentgibson : http://www.bigblogzoo.com
 * 
 ******************************************************************************/
public class ContinFTPClient {

    static Logger log = Logger.getLogger(ContinFTPClient.class.getName());

    private static HashSet set = new HashSet();

    static Set synchedSet = Collections.synchronizedSet(new HashSet(set));

    private static HashSet binaryset = new HashSet();

    static Set synchedBinarySet = Collections.synchronizedSet(new HashSet(binaryset));

    private static FTPClient ftpClient;

    public static boolean stopped = false;

    /**
   * @param host
   * @param port
   * @param username
   * @param pw
   * @return
   */
    public static String ftpPing(String root, String host, int port, String username, String pw) {
        try {
            ftpClient = new FTPClient();
            ftpClient.setRemoteAddr(InetAddress.getByName(host));
            ftpClient.setControlPort(port);
            ftpClient.setTimeout(4000);
            ftpClient.connect();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ftpClient.login(username, (pw));
            ftpClient.chdir(root);
            JobHandler.releaseFTPLock();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (FTPException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }
        try {
            ftpClient.quit();
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        } catch (FTPException e) {
            e.printStackTrace();
            return e.getMessage();
        }
        return null;
    }

    /**
   * @param from
   * @param to
   * @param renameTo
   * @param binary
   * @param monitor
   */
    public static void putWithUserSettings(String from, String to, String renameTo, boolean binary, IProgressMonitor monitor) {
        if (monitor != null && monitor.isCanceled()) {
            return;
        }
        FTPHolder ftpHolder = new FTPHolder(from, to, renameTo, binary);
        synchedSet.add(ftpHolder);
        int ftpqueuesize = PrefPageOne.getIntValue(CONSTANTS.PREF_FTPQUEUE);
        if (synchedSet.size() >= ftpqueuesize) {
            JobHandler.aquireFTPLock();
            try {
                ftpClient = new FTPClient();
                ftpClient.setRemoteAddr(InetAddress.getByName(PrefPageOne.getValue(CONSTANTS.PREF_HOST)));
                ftpClient.setControlPort(PrefPageOne.getIntValue(CONSTANTS.PREF_FTPPORT));
                ftpClient.connect();
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ftpClient.login((PrefPageOne.getValue(CONSTANTS.PREF_USERNAME)), FTPUtils.decrypt(PrefPageOne.getValue(CONSTANTS.PREF_PASSWORD)));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (monitor != null && monitor.isCanceled()) {
                    JobHandler.releaseFTPLock();
                    ftpClient.quit();
                    return;
                }
                synchronized (synchedSet) {
                    for (Iterator iter = synchedSet.iterator(); iter.hasNext(); ) {
                        if (monitor != null && monitor.isCanceled()) {
                            JobHandler.releaseFTPLock();
                            ftpClient.quit();
                            return;
                        }
                        Thread.yield();
                        FTPHolder element = (FTPHolder) iter.next();
                        if (element.binary) {
                            ftpClient.setType(FTPTransferType.BINARY);
                        } else {
                            ftpClient.setType(FTPTransferType.ASCII);
                        }
                        ftpClient.put(element.from, element.to);
                        if (element.renameTo != null) {
                            try {
                                ftpClient.delete(element.renameTo);
                            } catch (Exception e) {
                            }
                            ftpClient.rename(element.to, element.renameTo);
                            log.info("RENAME: " + element.to + "To: " + element.renameTo);
                        }
                    }
                    synchedSet.clear();
                }
                JobHandler.releaseFTPLock();
                ftpClient.quit();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (FTPException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
   * @param monitor
   * @param from
   * @param to
   * @param renameTo
   */
    public static void polishOff(IProgressMonitor monitor, String from, String to, String renameTo) {
        if (monitor != null && monitor.isCanceled()) {
            return;
        }
        try {
            ftpClient = new FTPClient();
            ftpClient.setRemoteAddr(InetAddress.getByName(PrefPageOne.getValue(CONSTANTS.PREF_HOST)));
            ftpClient.setControlPort(PrefPageOne.getIntValue(CONSTANTS.PREF_FTPPORT));
            ftpClient.connect();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ftpClient.login((PrefPageOne.getValue(CONSTANTS.PREF_USERNAME)), FTPUtils.decrypt(PrefPageOne.getValue(CONSTANTS.PREF_PASSWORD)));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (from != null) {
                FTPHolder ftpHolder = new FTPHolder(from, to, renameTo, false);
                synchedSet.add(ftpHolder);
            }
            JobHandler.aquireFTPLock();
            for (Iterator iter = synchedSet.iterator(); iter.hasNext(); ) {
                if (monitor != null && monitor.isCanceled()) {
                    JobHandler.releaseFTPLock();
                    ftpClient.quit();
                    return;
                }
                Thread.yield();
                FTPHolder element = (FTPHolder) iter.next();
                ftpClient.setType(FTPTransferType.ASCII);
                ftpClient.put(element.from, element.to);
                if (element.renameTo != null) {
                    try {
                        ftpClient.delete(element.renameTo);
                    } catch (Exception e) {
                    }
                    ftpClient.rename(element.to, element.renameTo);
                    log.info("RENAME: " + element.to + "To: " + element.renameTo);
                }
            }
            JobHandler.releaseFTPLock();
            ftpClient.quit();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (FTPException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        synchedSet.clear();
    }
}
