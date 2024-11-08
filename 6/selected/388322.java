package org.compiere.model;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;
import org.apache.commons.net.ftp.*;
import org.compiere.util.*;

/**
 * 	Media Server Model
 *	
 *  @author Jorg Janke
 *  @version $Id: MMediaServer.java,v 1.3 2006/07/30 00:51:05 jjanke Exp $
 */
public class MMediaServer extends X_CM_Media_Server {

    /**
	 * 	Get Media Server
	 *	@param project
	 *	@return server list
	 */
    public static MMediaServer[] getMediaServer(MWebProject project) {
        ArrayList<MMediaServer> list = new ArrayList<MMediaServer>();
        PreparedStatement pstmt = null;
        String sql = "SELECT * FROM CM_Media_Server WHERE CM_WebProject_ID=? ORDER BY CM_Media_Server_ID";
        try {
            pstmt = DB.prepareStatement(sql, project.get_TrxName());
            pstmt.setInt(1, project.getCM_WebProject_ID());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new MMediaServer(project.getCtx(), rs, project.get_TrxName()));
            }
            rs.close();
            pstmt.close();
            pstmt = null;
        } catch (Exception e) {
            s_log.log(Level.SEVERE, sql, e);
        }
        try {
            if (pstmt != null) pstmt.close();
            pstmt = null;
        } catch (Exception e) {
            pstmt = null;
        }
        MMediaServer[] retValue = new MMediaServer[list.size()];
        list.toArray(retValue);
        return retValue;
    }

    /**	Logger	*/
    private static CLogger s_log = CLogger.getCLogger(MMediaServer.class);

    /**************************************************************************
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param CM_Media_Server_ID id
	 *	@param trxName transaction
	 */
    public MMediaServer(Properties ctx, int CM_Media_Server_ID, String trxName) {
        super(ctx, CM_Media_Server_ID, trxName);
    }

    /**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs request 
	 *	@param trxName transaction
	 */
    public MMediaServer(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }

    /**
	 * 	(Re-)Deploy all media
	 * 	@param media array of media to deploy
	 * 	@return true if deployed
	 */
    public boolean deploy(MMedia[] media) {
        if (this.getIP_Address().equals("127.0.0.1") || this.getName().equals("localhost")) {
            log.warning("You have not defined your own server, we will not really deploy to localhost!");
            return true;
        }
        FTPClient ftp = new FTPClient();
        try {
            ftp.connect(getIP_Address());
            if (ftp.login(getUserName(), getPassword())) log.info("Connected to " + getIP_Address() + " as " + getUserName()); else {
                log.warning("Could NOT connect to " + getIP_Address() + " as " + getUserName());
                return false;
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Could NOT connect to " + getIP_Address() + " as " + getUserName(), e);
            return false;
        }
        boolean success = true;
        String cmd = null;
        try {
            cmd = "cwd";
            ftp.changeWorkingDirectory(getFolder());
            cmd = "list";
            String[] fileNames = ftp.listNames();
            log.log(Level.FINE, "Number of files in " + getFolder() + ": " + fileNames.length);
            cmd = "bin";
            ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
            for (int i = 0; i < media.length; i++) {
                if (!media[i].isSummary()) {
                    log.log(Level.INFO, " Deploying Media Item:" + media[i].get_ID() + media[i].getExtension());
                    MImage thisImage = media[i].getImage();
                    byte[] buffer = thisImage.getData();
                    ByteArrayInputStream is = new ByteArrayInputStream(buffer);
                    String fileName = media[i].get_ID() + media[i].getExtension();
                    cmd = "put " + fileName;
                    ftp.storeFile(fileName, is);
                    is.close();
                }
            }
        } catch (Exception e) {
            log.log(Level.WARNING, cmd, e);
            success = false;
        }
        try {
            cmd = "logout";
            ftp.logout();
            cmd = "disconnect";
            ftp.disconnect();
        } catch (Exception e) {
            log.log(Level.WARNING, cmd, e);
        }
        ftp = null;
        return success;
    }
}
