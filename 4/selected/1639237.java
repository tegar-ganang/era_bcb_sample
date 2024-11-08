package ru.ksu.niimm.cll.mocassin.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.csvreader.CsvReader;
import com.google.common.collect.Maps;

public class IzvestiyaUtil {

    private static final String INPUT_DIR = "<path>";

    private static final String OUTPUT_DIR = "/opt/mocassin/tex";

    private static final Map<String, String> id2filename = Maps.newHashMap();

    private static final Logger logger = Logger.getLogger(IzvestiyaUtil.class.getName());

    private static void readIzvestiyaArticles() throws IOException {
        CsvReader reader = new CsvReader(new InputStreamReader(IzvestiyaUtil.class.getClassLoader().getResourceAsStream("mathnet_izvestiya.csv")), ';');
        reader.setTrimWhitespace(true);
        try {
            while (reader.readRecord()) {
                String id = reader.get(0);
                String filename = reader.get(1);
                StringTokenizer st = new StringTokenizer(filename, "-.");
                String name = st.nextToken();
                String volume = st.nextToken();
                String year = st.nextToken();
                String extension = st.nextToken();
                String filepath = String.format("%s/%s/%s-%s.%s", year, volume.length() == 1 ? "0" + volume : volume, name, volume, extension);
                id2filename.put(id, filepath);
            }
        } finally {
            reader.close();
        }
        for (Map.Entry<String, String> entry : id2filename.entrySet()) {
            String filepath = String.format("%s/%s", INPUT_DIR, entry.getValue());
            filepath = new File(filepath).exists() ? filepath : filepath.replace(".tex", ".TEX");
            if (new File(filepath).exists()) {
                InputStream in = new FileInputStream(filepath);
                FileOutputStream out = new FileOutputStream(String.format("%s/%s.tex", OUTPUT_DIR, entry.getKey()), false);
                try {
                    org.apache.commons.io.IOUtils.copy(in, out);
                } catch (Exception e) {
                    org.apache.commons.io.IOUtils.closeQuietly(in);
                    org.apache.commons.io.IOUtils.closeQuietly(out);
                }
            } else {
                logger.log(Level.INFO, "File with the path=" + filepath + " doesn't exist");
            }
        }
    }

    /**
	 * @param args
	 * @throws IOException
	 */
    public static void main(String[] args) throws IOException {
        readIzvestiyaArticles();
    }
}
