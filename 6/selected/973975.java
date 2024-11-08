package es.randres.jaxb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.zip.GZIPInputStream;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import es.randres.aemet.VarDescription;

public class Aeminuto {

    private static final String FIELD_SEPARATOR = ",";

    private static final int START_DATA_FIELDS = 6;

    private static final String VALUE_SEPARATOR = "=";

    public static Hashtable<String, String> processLine(String line) {
        Hashtable<String, String> data = new Hashtable<String, String>();
        String[] fields = line.split(FIELD_SEPARATOR);
        System.out.println(String.format("Ind.     Estacion: %s", fields[0]));
        System.out.println(String.format("Nombre   Estacion: %s", fields[1]));
        System.out.println(String.format("Latitud  (grados): %s", fields[2]));
        System.out.println(String.format("Longitud (grados): %s", fields[3]));
        System.out.println(String.format("Hora inicio obser: %s", new Date(Long.parseLong(fields[4]))));
        System.out.println(String.format("Hora fin observac: %s", new Date(Long.parseLong(fields[5]))));
        for (int index = START_DATA_FIELDS; index < fields.length; index++) {
            String field = fields[index];
            int sepIndex = field.lastIndexOf(VALUE_SEPARATOR);
            if (sepIndex != -1) {
                String key = field.substring(0, sepIndex);
                String value = field.substring(sepIndex + 1);
                if (!key.startsWith("Q")) {
                    System.out.println(VarDescription.getInstance().getDescription(key) + " " + value + " " + VarDescription.getInstance().getUnit(key));
                }
            }
        }
        return data;
    }

    public static void main(String[] args) {
        FTPClient f = new FTPClient();
        String host = "ftpdatos.aemet.es";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        final String datestamp = sdf.format(new Date());
        System.out.println(datestamp);
        String pathname = "datos_observacion/observaciones_diezminutales/" + datestamp + "_diezminutales/";
        try {
            InetAddress server = InetAddress.getByName(host);
            f.connect(server);
            String username = "anonymous";
            String password = "a@b.c";
            f.login(username, password);
            FTPFile[] files = f.listFiles(pathname, new FTPFileFilter() {

                @Override
                public boolean accept(FTPFile file) {
                    return file.getName().startsWith(datestamp);
                }
            });
            FTPFile file = files[files.length - 2];
            f.setFileTransferMode(FTPClient.BINARY_FILE_TYPE);
            boolean download = false;
            String remote = pathname + "/" + file.getName();
            if (download) {
                File out = new File("/home/randres/Desktop/" + file.getName());
                FileOutputStream fout = new FileOutputStream(out);
                System.out.println(f.retrieveFile(remote, fout));
                fout.flush();
                fout.close();
            } else {
                GZIPInputStream gzipin = new GZIPInputStream(f.retrieveFileStream(remote));
                LineNumberReader lreader = new LineNumberReader(new InputStreamReader(gzipin, "Cp1250"));
                String line = null;
                while ((line = lreader.readLine()) != null) {
                    Aeminuto.processLine(line);
                }
                lreader.close();
            }
            f.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
