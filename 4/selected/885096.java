package cart;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import resources.RootClass;

/**
 * @author laurent
 * @version $Id: ZipMap.java 64 2011-10-07 12:05:00Z laurent.mistahl $
 */
public class ZipMap extends RootClass {

    private LinkedHashMap<String, Set<ZipEntryRef>> zipMap = new LinkedHashMap<String, Set<ZipEntryRef>>();

    public void put(String folder, Set<ZipEntryRef> entries) {
        zipMap.put(folder, entries);
    }

    public Set<ZipEntryRef> get(String folder) {
        return zipMap.get(folder);
    }

    public Set<String> keySet() {
        return zipMap.keySet();
    }

    /**
	 * Copy and rename all files of the map into the reportdir.
	 * @param baseDir
	 * @throws Exception
	 */
    public void prepareDataFiles(String baseDir, String reportDir) throws Exception {
        emptyDirectory(new File(reportDir));
        for (Entry<String, Set<ZipEntryRef>> e : zipMap.entrySet()) {
            String node = e.getKey();
            String nodeDir = baseDir + File.separator + node;
            if (!isWorkingDirectoryValid(nodeDir)) {
                throw new Exception("Cannot acces to " + nodeDir);
            }
            Set<ZipEntryRef> zers = e.getValue();
            LinkedHashSet<ZipEntryRef> statusZers = new LinkedHashSet<ZipEntryRef>();
            for (ZipEntryRef zer : zers) {
                if (zer.getType() == ZipEntryRef.JOB) {
                    this.prepareJobFile(zer, nodeDir, reportDir, statusZers);
                } else {
                    this.prepareUrlFile(zer, nodeDir, reportDir);
                }
            }
            zers.addAll(statusZers);
        }
    }

    private void prepareUrlFile(ZipEntryRef zer, String nodeDir, String reportDir) throws Exception {
        URL url = new URL(zer.getUri());
        URLConnection conn = url.openConnection();
        String fcopyName = reportDir + File.separator + zer.getFilenameFromHttpHeader(conn.getHeaderFields());
        logger.debug("download " + zer.getUri() + " in " + fcopyName);
        BufferedOutputStream bw;
        bw = new BufferedOutputStream(new FileOutputStream(fcopyName));
        BufferedInputStream reader = new BufferedInputStream(conn.getInputStream());
        byte[] inputLine = new byte[100000];
        ;
        while (reader.read(inputLine) > 0) {
            bw.write(inputLine);
        }
        bw.close();
        reader.close();
        zer.setUri(fcopyName);
    }

    /**
	 * @param zer		 Zip Entry reference
	 * @param nodeDir    Base directory of the TAP node
	 * @param reportDir  Working directory of the ZIP builder
	 * @param statusZers Set of ZIP entries associated with status files added to the ZIP ball
	 * @throws Exception
	 */
    private void prepareJobFile(ZipEntryRef zer, String nodeDir, String reportDir, Set<ZipEntryRef> statusZers) throws Exception {
        String jobDir = nodeDir + File.separator + "job_" + zer.getUri();
        if (!isWorkingDirectoryValid(jobDir)) {
            throw new Exception("Cannot acces to " + jobDir);
        }
        File f = new File(jobDir + File.separator + "result.xml");
        if (!f.exists() || !f.isFile() || !f.canRead()) {
            throw new Exception("Cannot acces to result file " + f.getAbsolutePath());
        }
        String fcopyName = reportDir + File.separator + zer.getName() + ".xml";
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fcopyName));
        IOUtils.copy(bis, bos);
        bis.close();
        bos.close();
        zer.setUri(fcopyName);
        f = new File(jobDir + File.separator + "status.xml");
        if (!f.exists() || !f.isFile() || !f.canRead()) {
            throw new Exception("Cannot acces to status file " + f.getAbsolutePath());
        }
        fcopyName = reportDir + File.separator + zer.getName() + "_status.xml";
        bis = new BufferedInputStream(new FileInputStream(f));
        bos = new BufferedOutputStream(new FileOutputStream(fcopyName));
        IOUtils.copy(bis, bos);
        bis.close();
        bos.close();
        statusZers.add(new ZipEntryRef(ZipEntryRef.JOB, zer.getName(), fcopyName));
    }

    public static void main(String[] args) throws Exception {
        URL url = new URL("http://obs-he-lm:8888/XCATDR3/getproduct?obsid=0113060201&dtype=flatfiles&prd=P0113060201M2S003STSPLT8004.PDF");
        URLConnection conn = url.openConnection();
        Map<String, List<String>> map = conn.getHeaderFields();
        JSONObject jso = new JSONObject();
        for (Entry<String, List<String>> s : map.entrySet()) {
            System.out.println(s.getKey());
            for (String v : s.getValue()) {
                System.out.println("   " + v);
            }
            jso.put(s.getKey(), s.getValue().get(0));
        }
        conn.getInputStream().close();
        System.out.println(jso.toJSONString());
    }
}
