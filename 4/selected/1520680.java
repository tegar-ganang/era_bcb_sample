package edu.ucsd.ncmir.synu.reader;

import edu.ucsd.ncmir.synu.Synu;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author spl
 */
public class ZipViewdataReader extends AbstractViewdataReader {

    private Hashtable<String, File> _file_hash = new Hashtable<String, File>();

    private BufferedReader _viewdata;

    public ZipViewdataReader(ZipInputStream stream) throws IOException {
        ZipEntry ze;
        while ((ze = stream.getNextEntry()) != null) {
            File temp = File.createTempFile("spool.", ".synu");
            temp.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(temp);
            byte[] buffer = new byte[1024 * 1024];
            int length;
            while ((length = stream.read(buffer)) != -1) fos.write(buffer, 0, length);
            fos.close();
            String name = ze.getName();
            String[] parts = name.split("[\\\\/]");
            this._file_hash.put(parts[parts.length - 1], temp);
        }
        stream.close();
        for (String key : this._file_hash.keySet()) if (key.endsWith("Viewdata")) {
            File f = this._file_hash.get(key);
            FileReader fr = new FileReader(f);
            this._viewdata = new BufferedReader(fr);
            break;
        }
        if (this._viewdata == null) throw new FileNotFoundException("No Viewdata found in ZIP file.");
    }

    public Synu[] readNext() throws IOException, URISyntaxException {
        String viewdata = this._viewdata.readLine();
        Synu[] synu = null;
        if (viewdata != null) {
            String[] vparts = viewdata.trim().split("[\\t ]+");
            String[] fnparts = vparts[0].split("[\\\\/]");
            File f = this._file_hash.get(fnparts[fnparts.length - 1]);
            if (f != null) {
                URI uri = f.toURI();
                synu = super.readObjects(vparts, uri);
            }
        }
        return synu;
    }

    public Synu[][] readAll(boolean ignore_broken) throws IOException, URISyntaxException {
        ArrayList<Synu[]> all = new ArrayList<Synu[]>();
        Synu[] synu;
        while ((synu = this.readNext()) != null) all.add(synu);
        return all.toArray(new Synu[0][]);
    }
}
