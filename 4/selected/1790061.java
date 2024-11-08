package de.iqcomputing.flap;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.jdom.*;
import de.iqcomputing.jdom.*;

public class UserMenuEntry implements Cloneable {

    protected String name;

    protected String command;

    protected boolean showOutput;

    public UserMenuEntry(String name, String command, boolean showOutput) {
        this.name = name;
        this.command = command;
        this.showOutput = showOutput;
    }

    public UserMenuEntry(Element entry, int propertiesVersion) {
        name = entry.getAttributeValue("name");
        command = JDOMTools.getElementContentText(entry.getChild("command"));
        showOutput = Boolean.valueOf(entry.getAttributeValue("show-output")).booleanValue();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean getShowOutput() {
        return showOutput;
    }

    public void setShowOutput(boolean showOutput) {
        this.showOutput = showOutput;
    }

    public Element userMenuEntryElement() {
        Element entry = new Element("user-menu-entry");
        Element e;
        entry.setAttribute(new Attribute("name", name));
        e = new Element("command");
        e.addContent(command);
        entry.addContent(e);
        entry.setAttribute(new Attribute("show-output", String.valueOf(showOutput)));
        return entry;
    }

    public String run(MimeMessage msg) throws UserMenuEntryInvocationException {
        File f = null;
        try {
            StringBuffer output = new StringBuffer();
            BufferedReader in = null;
            Process p;
            f = writeTemporaryMessageFile(msg);
            try {
                String lineSep = System.getProperty("line.separator", "\n");
                String line;
                p = Runtime.getRuntime().exec(new String[] { getCommand(), f.getAbsolutePath() });
                p.waitFor();
                in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                while ((line = in.readLine()) != null) {
                    output.append(line);
                    output.append(lineSep);
                }
                return output.toString();
            } catch (Exception e) {
                throw new UserMenuEntryInvocationException(e.getMessage());
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }
            }
        } finally {
            if (f != null) f.delete();
        }
    }

    protected File writeTemporaryMessageFile(MimeMessage msg) throws UserMenuEntryInvocationException {
        String temp = Flap.getProperty("tempPath");
        if (temp != null) {
            File dir = new File(temp);
            if (dir.isDirectory()) {
                File f;
                do {
                    f = new File(dir, "flap-" + System.currentTimeMillis() + ".msg");
                    if (f.isFile()) {
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                        }
                    }
                } while (f.isFile());
                {
                    boolean folderOpen = false;
                    InputStream in = null;
                    BufferedOutputStream out = null;
                    PrintWriter outWriter = null;
                    try {
                        String lineSep = System.getProperty("line.separator", "\n");
                        byte[] buf = new byte[4096];
                        Enumeration headers = msg.getAllHeaderLines();
                        int len;
                        Flap.getFolderManager().open(msg.getFolder(), FolderManager.READ_ONLY);
                        folderOpen = true;
                        in = msg.getRawInputStream();
                        out = new BufferedOutputStream(new FileOutputStream(f));
                        outWriter = new PrintWriter(out);
                        while (headers.hasMoreElements()) {
                            outWriter.print((String) headers.nextElement());
                            outWriter.print(lineSep);
                        }
                        outWriter.print(lineSep);
                        outWriter.flush();
                        while ((len = in.read(buf, 0, buf.length)) >= 0) out.write(buf, 0, len);
                        return f;
                    } catch (IOException e) {
                        throw new UserMenuEntryInvocationException(e.getMessage());
                    } catch (MessagingException e) {
                        throw new UserMenuEntryInvocationException(e.getMessage());
                    } finally {
                        if (out != null) {
                            try {
                                out.close();
                            } catch (IOException e) {
                            }
                        }
                        if (in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                            }
                        }
                        if (folderOpen) {
                            try {
                                Flap.getFolderManager().close(msg.getFolder(), false);
                            } catch (MessagingException e) {
                            }
                        }
                    }
                }
            } else throw new UserMenuEntryInvocationException("temporary path is a file");
        } else throw new UserMenuEntryInvocationException("no temporary path specified");
    }

    public String toString() {
        return name;
    }

    public Object clone() throws CloneNotSupportedException {
        UserMenuEntry e = (UserMenuEntry) super.clone();
        e.name = name;
        e.command = command;
        e.showOutput = showOutput;
        return e;
    }
}
