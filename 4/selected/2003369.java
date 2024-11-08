package org.heresylabs.netbeans.p4;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to wrap p4 cli into Proc object, with full output and error streams inside.
 *
 * @author Aekold Helbrass <Helbrass@gmail.com>
 */
public class CliWrapper {

    /**
     * Execute p4 with given command on given file.<br/>
     * If {@code file} is file - it's name will be passed to p4 with {@code file.getParentFile()} as working dir.<br/>
     * If {@code file} is folder - it's name + {@code "/..."} will be passed to p4.
     * @param command p4 command to execute
     * @param file target argument for p4 command
     * @return Proc with output and error streams of p4 execution,
     * or null if there was no connection or some exception happened.
     */
    public Proc execute(File file, String... commandArgs) {
        if (file == null || !file.exists()) {
            return null;
        }
        Connection connection = PerforceVersioningSystem.getInstance().getConnectionForFile(file);
        if (connection == null) {
            PerforceVersioningSystem.print(true, "Connection is empty");
            return null;
        }
        List<String> args = new ArrayList<String>();
        args.add("p4");
        String user = connection.getUser();
        if (user != null && user.length() > 0) {
            args.add("-u");
            args.add(connection.getUser());
        }
        String client = connection.getClient();
        if (client != null && client.length() > 0) {
            args.add("-c");
            args.add(connection.getClient());
        }
        String server = connection.getServer();
        if (server != null && server.length() > 0) {
            args.add("-p");
            args.add(connection.getServer());
        }
        String password = connection.getPassword();
        if (password != null && password.length() > 0) {
            args.add("-P");
            args.add(connection.getPassword());
        }
        for (String argv : commandArgs) {
            args.add(argv);
        }
        String fileParam = file.getName();
        if (file.isDirectory()) {
            fileParam.concat("/...");
        }
        args.add(fileParam);
        return procExecute(args.toArray(new String[args.size()]), file.getParentFile());
    }

    /**
     * Util method to implement external p4 execution
     * @param command command argument for p4
     * @param dir working folder of process
     * @return Proc with execution outputs or null if exception thrown
     */
    private Proc procExecute(String[] commandArgs, File dir) {
        try {
            PerforceVersioningSystem.print(false, commandArgs);
            Process p = Runtime.getRuntime().exec(commandArgs, null, dir);
            String output = readStreamContent(p.getInputStream());
            String error = readStreamContent(p.getErrorStream());
            int exitValue = p.waitFor();
            return new Proc(exitValue, output, error);
        } catch (Exception e) {
            PerforceVersioningSystem.logError(this, e);
        }
        return null;
    }

    /**
     * Util method to read InputStream into String
     * @param in stream to read
     * @return String containing full strem content
     * @throws java.io.IOException
     */
    private String readStreamContent(InputStream in) throws IOException {
        byte[] buffer = new byte[4096];
        int read = in.read(buffer);
        if (read == -1) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        do {
            out.write(buffer, 0, read);
        } while ((read = in.read(buffer)) >= 0);
        return out.toString();
    }
}
