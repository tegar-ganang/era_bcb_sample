package es.randres.aemet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import es.randres.aemet.model.Estacion;
import es.randres.aemet.model.Observacion;

public final class AemetRetriever {

    private static final Logger LOGGER = Logger.getLogger(AemetRetriever.class.getCanonicalName());

    private static final String FIELD_SEPARATOR = ",";

    private static final int START_DATA_FIELDS = 6;

    private static final String VALUE_SEPARATOR = "=";

    private static final String HOST = "ftpdatos.aemet.es";

    private static final String USERNAME = "anonymous";

    private static final String PASSWORD = "a@b.c";

    private static final String PATHNAME_PATTERN = "datos_observacion/observaciones_diezminutales/%s_diezminutales/";

    private static final int ID_FIELD = 0;

    private static final int NAME_FIELD = 1;

    private static final int LAT_FIELD = 2;

    private static final int LONG_FIELD = 3;

    private static final int START_OBS_FIELD = 4;

    private static Estacion processLine(String line) {
        Map<String, String> data = new Hashtable<String, String>();
        String[] fields = line.split(FIELD_SEPARATOR);
        Estacion estacion = new Estacion(fields[ID_FIELD], fields[NAME_FIELD], Double.parseDouble(fields[LAT_FIELD]), Double.parseDouble(fields[LONG_FIELD]), Long.parseLong(fields[START_OBS_FIELD]));
        LOGGER.info(fields[NAME_FIELD]);
        for (int index = START_DATA_FIELDS; index < fields.length; index++) {
            String field = fields[index];
            int sepIndex = field.lastIndexOf(VALUE_SEPARATOR);
            if (sepIndex != -1) {
                String key = field.substring(0, sepIndex);
                String value = field.substring(sepIndex + 1);
                if (!key.startsWith("Q")) {
                    data.put(key, value);
                }
            }
        }
        estacion.setDatos(data);
        return estacion;
    }

    public static Observacion load() {
        Observacion obs = new Observacion(new Date());
        FTPClient f = new FTPClient();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        final String datestamp = sdf.format(new Date());
        String pathname = String.format(PATHNAME_PATTERN, datestamp);
        try {
            InetAddress server = InetAddress.getByName(HOST);
            f.connect(server);
            f.login(USERNAME, PASSWORD);
            FTPFile[] files = f.listFiles(pathname, new FTPFileFilter() {

                @Override
                public boolean accept(FTPFile file) {
                    return file.getName().startsWith(datestamp);
                }
            });
            FTPFile file = files[files.length - 1];
            f.setFileTransferMode(FTPClient.BINARY_FILE_TYPE);
            boolean download = false;
            String remote = pathname + "/" + file.getName();
            if (download) {
                File out = new File("/home/randres/Desktop/" + file.getName());
                FileOutputStream fout = new FileOutputStream(out);
                fout.flush();
                fout.close();
            } else {
                GZIPInputStream gzipin = new GZIPInputStream(f.retrieveFileStream(remote));
                LineNumberReader lreader = new LineNumberReader(new InputStreamReader(gzipin, "Cp1250"));
                String line = null;
                while ((line = lreader.readLine()) != null) {
                    obs.addEstacion(AemetRetriever.processLine(line));
                }
                lreader.close();
            }
            f.disconnect();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Cannot retrieve data from FTP", e);
        }
        return obs;
    }
}
