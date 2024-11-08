package kotan.server.embedded;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class DevAppServerProcess {

    private enum State {

        READY, RUNNING, SHUTDOWN
    }

    private final DevAppServer appServer;

    private Process process;

    private InputStream inputStream;

    private State state = State.READY;

    protected DevAppServerProcess(DevAppServer server) {
        this.appServer = server;
    }

    protected synchronized void start() throws IOException {
        if (state != State.READY) throw new IllegalStateException("server is running or alrady shutdown.");
        copyLibs();
        ArrayList<String> command = new ArrayList<String>();
        command.add("java");
        command.add("-ea");
        command.add("-cp");
        command.add(appServer.env.getSdkHome() + File.separator + "lib" + File.separator + "appengine-tools-api.jar");
        command.add("com.google.appengine.tools.KickStart");
        command.add("com.google.appengine.tools.development.DevAppServerMain");
        command.add("--port=" + appServer.env.getPort());
        command.add("--jvm_flag=-Ddatastore.backing_store=" + appServer.env.getDatastorePath());
        command.add("--jvm_flag=-Dcom.google.appengine.application.id=" + appServer.env.getApplicationId());
        command.add("--jvm_flag=-Dcom.google.appengine.application.version=" + appServer.env.getApplicationVersion());
        command.add("--disable_update_check");
        command.add("server");
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        process = builder.start();
        inputStream = process.getInputStream();
        state = State.RUNNING;
    }

    private void copyLibs() {
        File sdkHome = new File(appServer.env.getSdkHome());
        File lib = new File(sdkHome, "lib");
        File impl = new File(lib, "impl");
        File dest = new File("server" + File.separator + "WEB-INF" + File.separator + "lib");
        copy(impl, dest, "appengine-api.jar");
        copy(impl, dest, "appengine-api-labs.jar");
    }

    private void copy(File src, File dest, String name) {
        File srcFile = new File(src, name);
        File destFile = new File(dest, name);
        if (destFile.exists()) {
            if (destFile.lastModified() == srcFile.lastModified()) return;
            destFile.delete();
        }
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(srcFile).getChannel();
            out = new FileOutputStream(destFile).getChannel();
            in.transferTo(0, in.size(), out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException e) {
            }
            try {
                if (out != null) out.close();
            } catch (IOException e) {
            }
        }
        destFile.setLastModified(srcFile.lastModified());
    }

    protected synchronized InputStream getInputStream() {
        if (state != State.RUNNING) throw new IllegalStateException("server is not running.");
        return inputStream;
    }

    protected synchronized void kill() {
        if (process != null) {
            try {
                process.getInputStream().close();
            } catch (IOException e) {
            }
            try {
                process.getErrorStream().close();
            } catch (IOException e) {
            }
            try {
                process.getOutputStream().close();
            } catch (IOException e) {
            }
            process.destroy();
            process = null;
        }
        state = State.SHUTDOWN;
    }

    @Override
    protected void finalize() throws Throwable {
        kill();
    }
}
