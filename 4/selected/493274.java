package com.webobjects.monitor.application;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import com.webobjects.appserver.WOApplication;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSData;

public class MigrationPage extends MonitorComponent {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -1317986389844426074L;

    public String host;

    public String username;

    public NSData sshIdentityContent;

    public String sshIdentityFilepath;

    public String remoteFilepath;

    public Boolean shouldRestartApache;

    public String migrationStackTrace = null;

    public String adaptorConfigContent;

    public String adaptorConfigLocalFilepath;

    public MigrationPage(WOContext aWocontext) {
        super(aWocontext);
    }

    public String adaptorConfigContent() {
        @SuppressWarnings("unused") final Application app = (Application) WOApplication.application();
        adaptorConfigContent = siteConfig().generateHttpWebObjectsConfig().toString();
        return adaptorConfigContent;
    }

    public WOComponent migrate() {
        FileOutputStream adaptorConfigFileOutputStream = null;
        try {
            adaptorConfigFileOutputStream = new FileOutputStream(new File("/tmp/http-webobjects.conf"));
            FileChannel fc = adaptorConfigFileOutputStream.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(adaptorConfigContent().length());
            buffer.put(adaptorConfigContent.getBytes());
            buffer.flip();
            while (buffer.hasRemaining()) {
                fc.write(buffer);
            }
            if (shouldRestartApache.booleanValue()) {
            }
            migrationStackTrace = "";
        } catch (Exception e) {
            migrationStackTrace = e.getMessage();
        } finally {
            try {
                if (adaptorConfigFileOutputStream != null) {
                    adaptorConfigFileOutputStream.close();
                }
            } catch (Exception e) {
                migrationStackTrace = migrationStackTrace + "\n" + e.getMessage();
            }
        }
        return null;
    }

    public boolean getIsMigrationCompleted() {
        if (migrationStackTrace != null && migrationStackTrace.length() == 0) return true;
        return false;
    }

    public String getMigrationStackTrace() {
        return migrationStackTrace;
    }

    public boolean getIsFailed() {
        return (migrationStackTrace != null && migrationStackTrace.length() > 0);
    }
}
