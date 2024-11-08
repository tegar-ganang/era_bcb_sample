package net.sf.gwoc.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import net.sf.gwoc.client.RCPHelper;
import net.sf.gwoc.gwapi.GWSoapHelper;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.program.Program;

public class Attachment {

    private final String id;

    private final String name;

    private final String parent;

    private final byte[] bodyData;

    public Attachment(String id, String name, String parent) {
        this.id = id;
        this.name = name;
        this.parent = parent;
        bodyData = null;
    }

    public Attachment(String id, String name, String parent, byte[] bodyData) {
        this.id = id;
        this.name = name;
        this.parent = parent;
        this.bodyData = bodyData;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getParent() {
        return parent;
    }

    public byte[] getData() {
        File loc = new File(RCPHelper.getWorkbenchDataLocation() + "att/" + getBucket() + getFileNameSaveID() + getExtension());
        byte[] data = new byte[(int) loc.length()];
        try {
            FileInputStream fis = new FileInputStream(loc);
            fis.read(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public void openAttachment() {
        File loc = new File(RCPHelper.getWorkbenchDataLocation() + "att/" + getBucket() + getFileNameSaveID() + getExtension());
        File temp = new File(System.getProperty("java.io.tmpdir"), name);
        try {
            writeAtt(loc, temp);
            temp.deleteOnExit();
            Program program = Program.findProgram(getExtension());
            program.execute(temp.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeAtt(File from, File to) throws FileNotFoundException, IOException {
        if (!from.exists()) return;
        FileInputStream fis = new FileInputStream(from);
        FileOutputStream fos = new FileOutputStream(to);
        try {
            fis.getChannel().transferTo(0, from.length(), fos.getChannel());
        } finally {
            fis.close();
            fos.close();
        }
    }

    public String getExtension() {
        int dotIndex = name.lastIndexOf('.');
        String ext = ".dat";
        if (dotIndex >= 0) ext = name.substring(dotIndex);
        return ext;
    }

    public String getFileNameSaveID() {
        String[] split = id.split(":");
        return split[0];
    }

    public String getBucket() {
        return id.substring(0, 3) + '/';
    }

    public void store(String attLoc, IProgressMonitor monitor) {
        String ext = getExtension();
        if (bodyData != null) {
            FileOutputStream fos = null;
            try {
                new File(attLoc + getBucket()).mkdir();
                fos = new FileOutputStream(attLoc + getBucket() + getFileNameSaveID() + ext);
                fos.write(bodyData);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                JSEHelper.close(fos);
            }
        } else {
            new File(attLoc + getBucket()).mkdir();
            GWSoapHelper.writeAttachment(attLoc, id, getBucket() + getFileNameSaveID(), ext, monitor);
        }
    }
}
