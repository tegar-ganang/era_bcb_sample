package com.cirnoworks.spk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author Cloudee
 * 
 */
public class DirectoryToSPK extends SPKWriter {

    /**
	 * @param seed
	 */
    public DirectoryToSPK(long seed) {
        super(seed);
    }

    protected InputStream getInputStream(String base, String name) throws IOException {
        return new FileInputStream(base + name);
    }

    protected void checkSource(String toCheck) throws IOException {
        File f = new File(toCheck);
        if (!f.exists() || !f.isDirectory()) {
            throw new IOException();
        }
    }

    protected void listFile(String base, NameFilter filter, List<FileEntry> files) throws IOException {
        File file = new File(base);
        listFile(file, "", filter, files);
    }

    private void listFile(File file, String name, NameFilter filter, List<FileEntry> files) throws IOException {
        if (filter != null) {
            if (!filter.accept(name)) {
                return;
            }
        }
        System.out.println("LIST " + file.getCanonicalPath());
        if (file.isDirectory()) {
            File[] list = file.listFiles();
            for (File f : list) {
                listFile(f, name + "/" + f.getName(), filter, files);
            }
        } else {
            files.add(new FileEntry(name));
        }
    }

    public static void main(String[] args) {
        SPKWriter spw = new DirectoryToSPK(112233332244l);
        try {
            spw.writeSPK(".", null, new File("out.spk"));
            SPKReader reader = new SPKReader(new File("out.spk").toURI().toURL(), 0x1A21A0BE14l);
            byte[] buf = new byte[65536];
            for (String file : reader.files()) {
                FileData data = reader.getFileData(file);
                System.out.println("EXTRACT " + file + " " + data.getPos() + " " + data.getLength());
                File outf = new File("out" + file);
                outf.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(outf);
                InputStream is = reader.getDecStream(file);
                int read;
                while ((read = is.read(buf)) >= 0) {
                    if (read == 0) {
                        continue;
                    }
                    fos.write(buf, 0, read);
                }
                fos.close();
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
