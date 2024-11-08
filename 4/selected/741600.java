package uk.ac.cam.caret.imscp.impl;

import java.util.*;
import java.io.*;
import java.util.zip.*;
import uk.ac.cam.caret.imscp.api.*;
import org.apache.commons.io.*;

public class PackageFileImpl implements PackageFile {

    private PackageDirectoryImpl dir;

    private ZipFile file;

    private String name;

    private ZipEntry entry;

    private Updates updates;

    PackageFileImpl(PackageDirectoryImpl dir, ZipFile file, String name, String full_name) {
        this.file = file;
        this.name = name;
        this.dir = dir;
        this.entry = file.getEntry(full_name);
        this.updates = dir.getUpdates();
    }

    PackageFileImpl(PackageDirectoryImpl dir, String name, InputStream data) throws IOException {
        this.dir = dir;
        this.name = name;
        this.updates = dir.getUpdates();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        IOUtils.copy(data, stream);
        updates.setNewData(getFullName(), stream.toByteArray());
        stream.close();
    }

    public InputStream getDataStream() throws IOException {
        byte[] new_data = updates.getNewData(getFullName());
        if (new_data != null) return new ByteArrayInputStream(new_data);
        return file.getInputStream(entry);
    }

    public String getFullName() {
        if (dir == null) return getName();
        String root = dir.getFullName();
        if (!"".equals(root)) root += "/";
        return root + getName();
    }

    public String getName() {
        return name;
    }

    public PackageDirectory getParent() {
        return dir;
    }

    public boolean isDirectory() {
        return false;
    }

    void serialize(ZipOutputStream out) throws IOException {
        if ("imsmanifest.xml".equals(getFullName())) return;
        out.putNextEntry(new ZipEntry(getFullName()));
        IOUtils.copy(getDataStream(), out);
        out.closeEntry();
    }

    public PackageFile cloneInto(ContentPackage in) throws BadParseException, IOException {
        List<PackageDirectory> parents = new ArrayList<PackageDirectory>();
        dir.pathList(parents);
        PackageDirectory here = null;
        for (PackageDirectory dir : parents) {
            if (here == null) here = in.getRootDirectory(); else {
                Entry next = here.getChild(dir.getName());
                if (next != null && !next.isDirectory()) throw new BadParseException("Cannot add directory, as exists as a path!");
                if (next == null) next = here.createChild(dir.getName());
                here = (PackageDirectory) next;
            }
        }
        PackageFile out = here.addFile(name, getDataStream());
        return out;
    }

    public void moveTo(PackageDirectory dir) {
        String old_full_name = getFullName();
        this.dir = (PackageDirectoryImpl) dir;
        this.dir.addFile(this);
        updates.rename(old_full_name, getFullName());
    }
}
