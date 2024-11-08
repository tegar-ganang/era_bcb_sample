package org.systemsbiology.addama.sequencing.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.systemsbiology.addama.commons.httpclient.support.HttpClientTemplate;
import org.systemsbiology.addama.commons.httpclient.support.JsonResponseCallback;
import org.systemsbiology.addama.commons.httpclient.support.IsExpectedStatusCodeResponseCallback;
import org.systemsbiology.addama.commons.httpclient.support.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.json.JSONObject;
import org.json.JSONArray;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.net.URLDecoder;

/**
 * @author: jlin
 * Class to set custom settings for IGV integration
 */
@Controller
public class IGVController extends LIMSAbstractController implements ServletContextAware {

    private static final Logger log = LoggerFactory.getLogger(IGVController.class);

    private ServletContext servletContext;

    private HttpClientTemplate httpClientTemplate;

    private String seqRepoName;

    public static final String SPLIT = "SPL!T";

    protected JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setHttpClientTemplate(HttpClientTemplate httpClientTemplate) {
        this.httpClientTemplate = httpClientTemplate;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public String getSeqRepoName() {
        return seqRepoName;
    }

    public void setSeqRepoName(String seqRepoName) {
        this.seqRepoName = seqRepoName;
    }

    @RequestMapping(value = "/**/jnlp", method = RequestMethod.GET)
    @ModelAttribute
    public void processJnlp(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String sampleLocus = null;
        String keyId = this.getStrainFromUri(getDecodedRequestUri(request), "custom_config", "/jnlp");
        log.info("keyId at jnlp " + keyId);
        if (keyId.indexOf("locuz:") != -1) {
            sampleLocus = keyId.split("locuz:")[1];
            keyId = keyId.split("locuz:")[0];
            if ((!sampleLocus.startsWith("chr") || sampleLocus.indexOf(":") == -1) && !sampleLocus.equals("2micron")) {
                sampleLocus = sampleLocus.toUpperCase();
                try {
                    sampleLocus = (String) this.jdbcTemplate.queryForObject("select systematic_name from yeast_gene_annotations where common_name = ?", new Object[] { sampleLocus }, String.class);
                } catch (Exception e) {
                    e.printStackTrace();
                    sampleLocus = null;
                }
            }
        }
        String baseUrl = getBaseUrl(request);
        String sessionURL = baseUrl + "/dudley-lims/sequencing/igv/custom_config" + keyId + "/dataSession";
        log.info("Begin LIMS JNLP Data Session for some sample:" + keyId + " sessionURL:" + sessionURL + " locus:" + sampleLocus);
        response.setContentType("application/x-java-jnlp-file");
        BufferedReader br = new BufferedReader(new InputStreamReader(servletContext.getResourceAsStream("/templates/igv_jnlp15.template")));
        ArrayList<String> lines = new ArrayList<String>();
        try {
            String nextLine = br.readLine();
            while (nextLine != null) {
                if (StringUtils.contains(nextLine, "@session")) {
                    lines.add("\t\t<argument>" + sessionURL + "?ignore=dummy.xml" + "</argument>");
                } else if (StringUtils.contains(nextLine, "@locus") && sampleLocus != null) {
                    lines.add("\t\t<argument>" + sampleLocus + "</argument>");
                } else if (StringUtils.contains(nextLine, "@codebasehost")) {
                    lines.add(StringUtils.replace(nextLine, "@codebasehost", baseUrl));
                } else {
                    lines.add(nextLine);
                }
                nextLine = br.readLine();
            }
        } finally {
            br.close();
        }
        PrintWriter out = response.getWriter();
        for (String line : lines) {
            out.println(line);
        }
    }

    @RequestMapping(value = "/**/prefs", method = RequestMethod.GET)
    @ModelAttribute
    public void processPropertyPrefs(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String patientId = this.getStrainFromUri(getDecodedRequestUri(request), "custom_config", "/prefs");
        log.info("processPropertyPrefs(" + request.getRequestURI() + ")");
        PrintWriter out = response.getWriter();
        out.println("PORT_ENABLED=true");
        out.println("IGVMainFrame.track.show.attribute.views=true");
        out.println("IGVMainFrame.single.track.pane=true");
        out.println("PROBE_MAPPING_KEY=false");
        String dataRegistryUri = getBaseUrl(request) + "/dudley-lims/sequencing/igv/custom_config" + patientId + "/dataRegistry";
        out.println("MASTER_RESOURCE_FILE_KEY=" + dataRegistryUri);
        out.println("migrated=true");
        out.println("IGVMainFrame.genome.sequence.dir=http://www.broadinstitute.org/igvdata/genomes/genomes.txt");
        out.println("IGVMainFrame.Bounds=189,98,1000,750");
        out.println("DEFAULT_GENOME_KEY=hg18");
        out.println("LAST_CHROMOSOME_VIEWED_KEY=chr7");
        out.println("CHECKED_RESOURCES_KEY=");
        out.println("http://www.broadinstitute.org/igvdata/mmgp_hg18.xml");
        out.println("http://www.broadinstitute.org/igvdata/epigenetics_hg18_public.xml");
        out.println("http://www.broadinstitute.org/igvdata/1KG/1KG.xml");
        out.flush();
        out.close();
    }

    @RequestMapping(value = "/**/dataSession", method = RequestMethod.GET)
    @ModelAttribute
    public void processDataSession(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String keyId = this.getStrainFromUri(getDecodedRequestUri(request), "custom_config", "/dataSession");
        log.info("KeyId " + keyId);
        response.setContentType("text/xml");
        PrintWriter out = response.getWriter();
        String host = this.getBaseUrl(request);
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<Global genome=\"sacCer2\">");
        out.println("<Files>");
        if (keyId.indexOf("YCR") == -1) {
            out.println("<DataFile name=\"" + host + "/igv" + keyId + "_RA.mut\"/>");
            out.println("<DataFile name=\"" + host + "/igv" + keyId + "_DN.mut\"/>");
            out.println("<DataFile name=\"" + host + "/igv" + keyId + "_IA.mut\"/>");
            out.println("<DataFile name=\"" + host + "/igv" + keyId + "_DN_contigs.mut\"/>");
            out.println("<DataFile name=\"" + host + "/igv" + keyId + "_RA_gaps.mut\"/>");
            out.println("<DataFile name=\"" + host + "/igv" + keyId + "_RA_changes_classification.mut\"/>");
            out.println("<DataFile name=\"" + host + "/igv" + keyId + ".bam\"/>");
        } else {
            String[] keys = keyId.split(":");
            log.info("Cross key merged mut file " + keys[0]);
            out.println("<DataFile name=\"" + host + "/igv" + keys[0] + "_merged.mut\"/>");
            log.info("Cross parents' mutations sources " + keys[1]);
            out.println("<DataFile name=\"" + host + "/igv/" + keys[1] + "_RA.mut\"/>");
            out.println("<DataFile name=\"" + host + "/igv/" + keys[1] + "_DN.mut\"/>");
            out.println("<DataFile name=\"" + host + "/igv/" + keys[1] + "_IA.mut\"/>");
            out.println("<DataFile name=\"" + host + "/igv/" + keys[1] + "_DN_contigs.mut\"/>");
            out.println("<DataFile name=\"" + host + "/igv/" + keys[1] + "_RA_gaps.mut\"/>");
            out.println("<DataFile name=\"" + host + "/igv/" + keys[1] + "_RA_changes_classification.mut\"/>");
            out.println("<DataFile name=\"" + host + "/igv/" + keys[2] + "_RA.mut\"/>");
            out.println("<DataFile name=\"" + host + "/igv/" + keys[2] + "_DN.mut\"/>");
            out.println("<DataFile name=\"" + host + "/igv/" + keys[2] + "_IA.mut\"/>");
            out.println("<DataFile name=\"" + host + "/igv/" + keys[2] + "_DN_contigs.mut\"/>");
            out.println("<DataFile name=\"" + host + "/igv/" + keys[2] + "_RA_gaps.mut\"/>");
            out.println("<DataFile name=\"" + host + "/igv/" + keys[2] + "_RA_changes_classification.mut\"/>");
        }
        out.println("</Files>");
        out.println("</Global>");
        out.flush();
        out.close();
    }

    @RequestMapping(value = "/**/dataSessionLocal", method = RequestMethod.GET)
    @ModelAttribute
    public void processDataSessionLocal(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String patientId = this.getStrainFromUri(getDecodedRequestUri(request), "custom_config", "/dataSession");
        log.info("Begin processDataTCGAExternal(" + request.getRequestURI() + "," + patientId + " )");
        response.setContentType("text/xml");
        PrintWriter out = response.getWriter();
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<Global genome=\"hg18\" version=\"2\">");
        out.println("<Files>");
        out.println("<DataFile name=\"http://ravioli-2:8080/igv/TCGA-13-0883-01A-02W-0420-08.bam.tdf\"/>");
        out.println("<DataFile name=\"http://ravioli-2:8080/igv/TCGA-13-0883-10A-01W-0420-08.bam.tdf\"/>");
        out.println("<DataFile name=\"http://ravioli-2:8080/igv/TCGA-13-0883-01A-02W-0420-08.bam.breakdancer.out.trans.bed\"/>");
        out.println("<DataFile name=\"http://ravioli-2:8080/igv/TCGA-13-0883-10A-01W-0420-08.bam.breakdancer.out.trans.bed\"/>");
        out.println("<DataFile name=\"http://ravioli-2:8080/igv/TCGA-13-0883-01A-02W-0420-08.bam.mappdM.out.bed\"/>");
        out.println("<DataFile name=\"http://ravioli-2:8080/igv/TCGA-13-0883-10A-01W-0420-08.bam.mappdM.out.bed\"/>");
        out.println("</Files>");
        out.println("</Global>");
        out.flush();
        out.close();
        log.info("Done processDataTCGAExternal(" + request.getRequestURI() + "," + patientId + " )");
    }

    @RequestMapping(value = "/**/dataRegistry", method = RequestMethod.GET)
    @ModelAttribute
    public void processDataRegistry(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("Begin processDataRegistry(" + request.getRequestURI() + ")");
        PrintWriter out = response.getWriter();
        out.println(getBaseUrl(request) + "/dudley-lims/sequencing/igv/custom_config/labspace");
        out.flush();
        out.close();
        log.info("Done processDataRegistry(" + request.getRequestURI() + ")");
    }

    @RequestMapping(value = "/**/labspace", method = RequestMethod.GET)
    @ModelAttribute
    public void processLabspace(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> crossingRefList = new ArrayList<String>();
        List<String> mutationList = new ArrayList<String>();
        crossingRefList.addAll(getCrossingRefSet(request));
        mutationList.addAll(getMutationSet(request));
        response.setContentType("text/xml");
        PrintWriter out = response.getWriter();
        log.info("Begin processLabspace(" + request.getRequestURI() + ")");
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<Global name=\"Dudley Lab@ISB\" hyperlink=\"http://www.systemsbiology.org/scientists_and_research/faculty_groups/Dudley_Group\" version=\"1\">");
        out.println("<Category name=\"Labspace\">");
        out.println("<Category name=\"Crossing Ref\">");
        for (String mut : mutationList) {
            if (mut.indexOf("YCR") != -1) {
                String[] mutArray = mut.split(SPLIT);
                out.println("<Resource name=\"" + mutArray[0] + "\" path=\"" + mutArray[1] + "\"/>");
            }
        }
        out.println("</Category>");
        out.println("<Category name=\"Mutation - Reference Assembly\">");
        for (String mut : mutationList) {
            if (mut.indexOf("_RA.mut") != -1) {
                String[] mutArray = mut.split(SPLIT);
                out.println("<Resource name=\"" + mutArray[0] + "\" path=\"" + mutArray[1] + "\"/>");
            }
        }
        out.println("</Category>");
        out.println("<Category name=\"Classification - Reference Assembly\">");
        for (String mut : mutationList) {
            if (mut.indexOf("_classification.mut") != -1) {
                String[] mutArray = mut.split(SPLIT);
                out.println("<Resource name=\"" + mutArray[0] + "\" path=\"" + mutArray[1] + "\"/>");
            }
        }
        out.println("</Category>");
        out.println("<Category name=\"Mutation - De Novo\">");
        for (String mut : mutationList) {
            if (mut.indexOf("_DN.mut") != -1) {
                String[] mutArray = mut.split(SPLIT);
                out.println("<Resource name=\"" + mutArray[0] + "\" path=\"" + mutArray[1] + "\"/>");
            }
        }
        out.println("</Category>");
        out.println("<Category name=\"Mutation - Insert Analysis\">");
        for (String mut : mutationList) {
            if (mut.indexOf("_IA.mut") != -1) {
                String[] mutArray = mut.split(SPLIT);
                out.println("<Resource name=\"" + mutArray[0] + "\" path=\"" + mutArray[1] + "\"/>");
            }
        }
        out.println("</Category>");
        out.println("</Category>");
        out.println("</Global>");
        log.info("Done processLabspace(" + request.getRequestURI() + ")");
    }

    private List<String> getCrossingRefSet(HttpServletRequest request) {
        log.info("getCrossingRefSet " + request.getRequestURI());
        final ArrayList<String> crossings = new ArrayList<String>();
        String baseUrl = getBaseUrl(request);
        try {
            if (this.login(getHttpsUrl(request))) {
                GetMethod get = new GetMethod(baseUrl + "/addama-rest/dudley-sample/path/datasource/YCR/search?FREE_TEXT=*&PROJECTION=parentA&PROJECTION=parentAlpha&PROJECTION=parentAlpha&PROJECTION=name");
                httpClientTemplate.executeMethod(get, new JsonResponseCallback() {

                    public Object onResponse(int statusCode, HttpMethod method, JSONObject json) throws Exception {
                        if (statusCode == 200) {
                            JSONArray res = json.getJSONArray("results");
                            for (int i = 0; i < res.length(); i++) {
                                JSONObject ycr = res.getJSONObject(i);
                                crossings.add(ycr.getString("name") + ":" + ycr.getString("parentA") + "-" + ycr.getString("parentAlpha"));
                            }
                            return crossings;
                        } else {
                            log.info("crossing ref search results error status " + statusCode);
                            return null;
                        }
                    }
                });
            }
        } catch (Exception e) {
            log.warn("getCrossingRefSet " + request.getRequestURI() + "\n" + e);
        }
        return crossings;
    }

    private List<String> getMutationSet(HttpServletRequest request) {
        log.info("getMutationSet " + request.getRequestURI());
        final ArrayList<String> mutations = new ArrayList<String>();
        String baseUrl = getBaseUrl(request);
        try {
            if (this.login(getHttpsUrl(request))) {
                GetMethod get = new GetMethod(baseUrl + "/addama-rest/" + this.getSeqRepoName() + "/path/igv/labspace/mutation/annotations");
                httpClientTemplate.executeMethod(get, new JsonResponseCallback() {

                    public Object onResponse(int statusCode, HttpMethod method, JSONObject json) throws Exception {
                        if (statusCode == 200) {
                            log.info("crossing ref search results " + json);
                            Iterator keys = json.keys();
                            while (keys.hasNext()) {
                                String key = (String) keys.next();
                                mutations.add(key.toString() + SPLIT + json.getString(key));
                            }
                            return mutations;
                        } else {
                            log.info("mutations ref search results error status " + statusCode);
                            return null;
                        }
                    }
                });
            }
        } catch (Exception e) {
            log.warn("getMutationSet " + request.getRequestURI() + "\n" + e);
        }
        return mutations;
    }

    @RequestMapping(value = "/**/dataTCGAExternalTemplate", method = RequestMethod.GET)
    @ModelAttribute
    public void processDataTCGAExternalTemplate(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("Begin processDataTCGAExternal(" + request.getRequestURI() + ")");
        List<String> poorPatients = new ArrayList<String>();
        poorPatients.addAll(this.getBoylePatientSet(request, "Poor"));
        String[] bamFiles = poorPatients.toArray(new String[poorPatients.size()]);
        response.setContentType("text/xml");
        PrintWriter out = response.getWriter();
        InputStream is = servletContext.getResourceAsStream("/templates/jboyle_external.template");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        try {
            String nextLine;
            while ((nextLine = br.readLine()) != null) {
                if (nextLine.trim().equals("@PATIENT_BAMS")) {
                    if (bamFiles.length > 0) {
                        for (String bamFile : bamFiles) {
                            log.info("bam files for patientId:" + bamFile);
                            String[] splitted = bamFile.split("/");
                            out.println("<Resource name=\"" + splitted[splitted.length - 1] + "\" path=\"" + bamFile + "\"/>");
                        }
                        out.println("</Category>");
                    }
                } else {
                    out.println(nextLine);
                }
            }
        } finally {
            out.close();
            br.close();
            is.close();
        }
        log.info("Done processDataTCGAExternal(" + request.getRequestURI() + ")");
    }

    private List<String> getBoylePatientSet(HttpServletRequest request, String patientType) {
        log.info("getBoylePatientSet(" + request.getRequestURI() + "," + patientType + ")");
        final ArrayList<String> patients = new ArrayList<String>();
        try {
            GetMethod get = new GetMethod("/addama/indexes/workspaces/jboyle/Patients/" + patientType + "/dir");
            httpClientTemplate.executeMethod(get, new JsonResponseCallback() {

                public Object onResponse(int statusCode, HttpMethod method, JSONObject json) throws Exception {
                    if (statusCode == 200) {
                        if (json.has("files")) {
                            JSONArray files = json.getJSONArray("files");
                            for (int i = 0; i < files.length(); i++) {
                                JSONObject patient = files.getJSONObject(i);
                                if (patient.has("uri")) {
                                    String patientUri = patient.getString("uri");
                                    log.info("patientUri=" + patientUri);
                                    patients.add(patientUri);
                                }
                            }
                        }
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            log.warn("getBoylePatientSet(" + request.getRequestURI() + "," + patientType + "): " + e);
        }
        return patients;
    }

    private String[] getFilePaths(String patientId, HttpServletRequest request, String fileType) {
        final String baseUrl = getBaseUrl(request);
        log.info("getFilePaths(" + patientId + "," + request.getRequestURI() + "," + fileType + ")");
        final ArrayList<String> filePaths = new ArrayList<String>();
        try {
            GetMethod get = new GetMethod("/addama/indexes" + patientId + "/" + fileType + "/dir");
            httpClientTemplate.executeMethod(get, new JsonResponseCallback() {

                public Object onResponse(int statusCode, HttpMethod method, JSONObject json) throws Exception {
                    if (statusCode == 200) {
                        if (json.has("files")) {
                            JSONArray files = json.getJSONArray("files");
                            for (int i = 0; i < files.length(); i++) {
                                JSONObject obj = files.getJSONObject(i);
                                String uri = obj.getString("uri");
                                String fileUri = StringUtils.replace(uri, "/path/", "/file/");
                                log.info("filePaths:" + fileUri);
                                filePaths.add(baseUrl + fileUri);
                            }
                        }
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            log.warn("getFilePaths(" + filePaths + "): " + e);
        }
        return filePaths.toArray(new String[filePaths.size()]);
    }

    private void populatePatientFiles(HttpServletRequest request, List<String> patients, StringBuffer dancerBuff, StringBuffer bamBuff, StringBuffer discordantBuff, StringBuffer copyNumberBuff, String patientCategory) {
        for (int i = 0; i < patients.size(); i++) {
            log.info("Poor Patient Id in Boyle workspace:" + patients.get(i));
            String patientAddamaId = patients.get(i);
            String[] patientSplitted = patientAddamaId.split("/");
            String patientId = patientSplitted[patientSplitted.length - 1];
            if (i == 0) {
                dancerBuff.append("<Category name=\"").append(patientCategory).append("\">\n");
                discordantBuff.append("<Category name=\"").append(patientCategory).append("\">\n");
                bamBuff.append("<Category name=\"").append(patientCategory).append("\">\n");
                copyNumberBuff.append("<Category name=\"").append(patientCategory).append("\">\n");
            }
            String[] igvPathList = this.getFilePaths(patientAddamaId, request, "IGV%20Files");
            for (String igvPath : igvPathList) {
                int iof = igvPath.indexOf(patientId);
                log.info("FilePath at IGV " + igvPath + " patientId:" + patientId);
                String igvFileName = igvPath.substring(iof, igvPath.length());
                log.info(" fileName:" + igvFileName);
                String desc = "";
                if (igvPath.indexOf(patientId + "-01") >= 1) {
                    desc = "Solid Tumor";
                } else if (igvPath.indexOf(patientId + "-10") >= 1) {
                    desc = "Normal Blood";
                } else if (igvPath.indexOf(patientId + "-11") >= 1) {
                    desc = "Normal Tissue";
                } else if (igvPath.indexOf(patientId + "-20") >= 1) {
                    desc = "Cell Line";
                } else if (igvPath.indexOf(patientId + "-12") >= 1) {
                    desc = "Buccal Smear";
                }
                if (igvPath.indexOf("breakdancer.out.trans.bed") >= 1) {
                    dancerBuff.append("<Resource name=\"").append(desc).append(" ").append(igvFileName).append(" \" path=\"").append(igvPath).append("\"/>\n");
                }
                if (igvPath.indexOf("mappdM.out.bed") >= 1) {
                    discordantBuff.append("<Resource name=\"").append(desc).append(" ").append(igvFileName).append(" \" path=\"").append(igvPath).append("\"/>\n");
                }
                if (igvPath.indexOf("MPCBS.seg") >= 1) {
                    copyNumberBuff.append("<Resource name=\"").append(desc).append(" ").append(igvFileName).append(" \" path=\"").append(igvPath).append("\"/>\n");
                }
            }
            String[] bamPathList = this.getFilePaths(patientAddamaId, request, "BAM%20Files");
            for (String bamPath : bamPathList) {
                int bof = bamPath.indexOf(patientId);
                String bamFileName = bamPath.substring(bof, bamPath.length());
                String desc = "";
                if (bamPath.indexOf(patientId + "-01") >= 1) {
                    desc = "Solid Tumor";
                } else if (bamPath.indexOf(patientId + "-10") >= 1) {
                    desc = "Normal Blood";
                } else if (bamPath.indexOf(patientId + "-11") >= 1) {
                    desc = "Normal Tissue";
                } else if (bamPath.indexOf(patientId + "-20") >= 1) {
                    desc = "Cell Line";
                } else if (bamPath.indexOf(patientId + "-12") >= 1) {
                    desc = "Buccal Smear";
                }
                bamBuff.append("<Resource name=\"").append(desc).append(" ").append(bamFileName).append(" \" path=\"").append(bamPath).append("\"/>\n");
            }
            if (i == patients.size() - 1) {
                dancerBuff.append("</Category>\n");
                discordantBuff.append("</Category>\n");
                bamBuff.append("</Category>\n");
                copyNumberBuff.append("</Category>\n");
            }
        }
    }

    public boolean login(String baseUrl) {
        try {
            StringBuilder queryString = new StringBuilder();
            log.info("login with username:password " + this.getAddamaDudleyUser() + ":" + this.getAddamaDudleyPassword());
            queryString.append("username=").append(this.getAddamaDudleyUser());
            queryString.append("&password=").append(this.getAddamaDudleyPassword());
            Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(), 8443));
            PostMethod post = new PostMethod(baseUrl + "/addama-login/login");
            post.setQueryString(queryString.toString());
            return (Boolean) this.httpClientTemplate.executeMethod(post, new IsExpectedStatusCodeResponseCallback(200));
        } catch (Exception e) {
            log.error("login(" + this.getAddamaDudleyUser() + ")", e);
            return false;
        }
    }

    private String getStrainFromUri(String requestUri, String cc, String service) {
        log.info("URI:" + requestUri);
        int i1 = requestUri.indexOf(cc);
        int i2 = requestUri.indexOf(service);
        return requestUri.substring(i1 + cc.length(), i2);
    }

    private String getDecodedRequestUri(HttpServletRequest request) {
        try {
            return URLDecoder.decode(request.getRequestURI(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
