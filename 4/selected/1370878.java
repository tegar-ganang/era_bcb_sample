package org.systemsbiology.addama.sequencing.controllers;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSender;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.systemsbiology.addama.commons.httpclient.support.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.systemsbiology.addama.commons.httpclient.support.ssl.EasySSLProtocolSocketFactory;
import org.systemsbiology.addama.commons.web.views.JsonView;
import org.systemsbiology.addama.sequencing.util.TranslateSlimSeq;
import org.systemsbiology.addama.sequencing.util.Formatter;
import org.systemsbiology.addama.sequencing.util.RagtagLegend;
import sun.tools.tree.ThisExpression;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import javax.sound.sampled.Port;
import java.util.*;
import java.io.*;
import java.net.URLDecoder;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Controller class to handle sequencing submissions and updates
 * Provides webhook api with slimseq integration
 * 08-10 Added omics integration
 * 08-10 Added notification on add/status events; also failure states
 * 11-01 Refactored code and begin integrating SOLiD platform
 *
 * @author: jlin
 */
@Controller
public class SequencingController extends LIMSAbstractController implements ServletContextAware {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public static final String BREAKDELIMITER = "<br></br>";

    private String slimseqUrl;

    private String solrPostUrl;

    private String slimseqDudleyUser;

    private String slimseqDudleyPassword;

    private String solidContactList;

    private final Map<String, MultipartFile> filesByName = new HashMap<String, MultipartFile>();

    public static String WEBAPP_ROOT;

    ServletContext servletContext = null;

    private Credentials credentials;

    private HttpClientTemplate httpClientTemplate;

    private HttpClient httpClient;

    protected JdbcTemplate jdbcTemplate;

    private MailSender mailSender;

    private SimpleMailMessage templateMessage;

    public void setMailSender(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void setTemplateMessage(SimpleMailMessage templateMessage) {
        this.templateMessage = templateMessage;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getSlimseqDudleyUser() {
        return slimseqDudleyUser;
    }

    public void setSlimseqDudleyUser(String slimseqDudleyUser) {
        this.slimseqDudleyUser = slimseqDudleyUser;
    }

    public String getSlimseqDudleyPassword() {
        return slimseqDudleyPassword;
    }

    public void setSlimseqDudleyPassword(String slimseqDudleyPassword) {
        this.slimseqDudleyPassword = slimseqDudleyPassword;
    }

    public String getSlimseqUrl() {
        return slimseqUrl;
    }

    public void setSlimseqUrl(String slimseqUrl) {
        this.slimseqUrl = slimseqUrl;
    }

    public String getSolrPostUrl() {
        return solrPostUrl;
    }

    public void setSolrPostUrl(String solrPostUrl) {
        this.solrPostUrl = solrPostUrl;
    }

    public String getSolidContactList() {
        return solidContactList;
    }

    public void setSolidContactList(String solidContactList) {
        this.solidContactList = solidContactList;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public void setHttpClientTemplate(HttpClientTemplate httpClientTemplate) {
        this.httpClientTemplate = httpClientTemplate;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void afterPropertiesSet() throws Exception {
        this.httpClient.getState().setCredentials(AuthScope.ANY, this.credentials);
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(byte[].class, new ByteArrayMultipartFileEditor());
    }

    @RequestMapping(value = "/**/samplestatus/*", method = RequestMethod.POST)
    @ModelAttribute
    public void processSequencingSampleStatus(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final HttpServletRequest frequest = request;
        String yseqKey = this.getYseqFromUri(getDecodedRequestUri(request), "samplestatus/");
        String jsonStr = ServletRequestUtils.getStringParameter(request, "JSON");
        JSONObject jsonRequest = new JSONObject(jsonStr);
        String user = jsonRequest.getString("username");
        String password = jsonRequest.getString("password");
        if (!user.equals(this.getSlimseqDudleyUser()) || !password.equals(this.getSlimseqDudleyPassword())) {
            if (!user.equals(this.getSlimseqDudleyUser())) {
                log.error("User:" + user + " is unauthorized");
            }
            if (!password.equals(this.getSlimseqDudleyPassword())) {
                log.error("Password for User:" + user + " is incorrect");
            }
            throw new Exception("Invalid User/Unauthorized Exception");
        }
        log.info("Begin processSequencingSampleStatus ()," + " sequencing batch " + yseqKey + " )" + new Date());
        final String sampleKey = jsonRequest.getString("sample_description");
        final String status = jsonRequest.getString("status");
        String flow_cell_name = jsonRequest.getString("flow_cell_name");
        String lane = jsonRequest.getString("lane");
        final String solrSeqUrl = "/solr/select/?q=yseqKey:" + yseqKey + "&rows=1&wt=json&fl=yseqKey,yseqDate,yseqState,yseqSampleCount,yseqReads,yseqProject,yseqContact,yseqBudget,yseqStart,yseqEnd,yseqNotes,yseqReadFormat,yseqRefGenome,yseqSamplePrep,yseqSamples,yseqSampleAlias,yseqInstrument,yseqAdaptorKit,yseqShearMethod,yseqMultiplexDegree,yseqKit,yseqSlider,yseqPrimer,yseqNFSSolid";
        final String solrSeqSmplUrl = "/solr/select/?q=yseqSmplKey:" + yseqKey + "_" + sampleKey + "&rows=1&wt=json&fl=yseqSmplKey,yseqSmpl,yseqSmplStatus,yseqSmplStatusDate,yseqSmplFlowcell,yseqSmplLane,yseqSmplComments";
        final String addamaUrl = "/addama-rest/dudley-sample/path/datasource/YSEQ/" + yseqKey + "/" + sampleKey;
        final String seqUrl = getBaseUrl(request) + "/addama-rest/dudley-sample/path/datasource/YSEQ/" + yseqKey;
        final String seqKey = yseqKey;
        final String meta = flow_cell_name + ":" + lane;
        JSONObject solrSeqResponse = this.getSolr(this.getSolrHost() + solrSeqUrl);
        try {
            JSONObject jsonPost = new JSONObject();
            jsonPost.put("sampleStatus", status);
            jsonPost.put("flowcell", flow_cell_name);
            jsonPost.put("lane", lane);
            JSONObject solrSeqSmplResponse = this.getSolr(this.getSolrHost() + solrSeqSmplUrl);
            JSONArray solrSeqSmplArray = solrSeqSmplResponse.getJSONArray("docs");
            JSONObject solrSeqSmplObj = solrSeqSmplArray.getJSONObject(0);
            solrSeqSmplObj.put("yseqSmplStatus", status);
            solrSeqSmplObj.put("yseqSmplFlowcell", flow_cell_name);
            solrSeqSmplObj.put("yseqSmplLane", lane);
            solrSeqSmplObj.put("yseqSmplStatusDate", Formatter.getDateString(new Date(), "MM/dd/yyyy"));
            this.postSolrUpdate(this.getSolrHost() + "/solr/update/json?commit=true", solrSeqSmplObj);
            final String msg = "SlimSeq update: YSEQ:" + yseqKey + " Sample:" + sampleKey + " updated with status:" + status + " with flowcell_lane:" + flow_cell_name + "_" + lane;
            if (this.login(getHttpsUrl(request))) {
                log.info("Authenticated");
                log.info("solrResponse\n" + solrSeqResponse.toString());
                final JSONArray seqSampleArray = solrSeqResponse.getJSONArray("docs");
                if (seqSampleArray == null || seqSampleArray.length() < 1) {
                    log.error("There are 0 samples in seq batch:" + solrSeqUrl);
                    throw new Exception("There are 0 samples in seq batch:" + solrSeqUrl);
                }
                final JSONObject solrSeqObj = seqSampleArray.getJSONObject(0);
                PostMethod post = new PostMethod(getBaseUrl(request) + addamaUrl);
                post.setQueryString(new NameValuePair[] { new NameValuePair("JSON", jsonPost.toString()) });
                this.httpClientTemplate.executeMethod(post, new ResponseCallback() {

                    public Object onResponse(int i, HttpMethod httpMethod) throws HttpClientResponseException {
                        if (i != 200) {
                            log.error("The post request failed");
                            throw new HttpClientResponseException(i, httpMethod, null);
                        } else {
                            log.info("Posted OKAY request on:" + addamaUrl);
                            String omicsSample = sampleKey;
                            if (sampleKey.indexOf("YMULTI") != -1) {
                                omicsSample = "*";
                            }
                            processOmicsView(seqKey, "NGS", omicsSample, "", status, meta, "", "", "UPDATE");
                            try {
                                String sampleStatus = "";
                                int numStatusDiff = 0;
                                if (!(sampleKey.indexOf("YMULTI") != -1 || sampleKey.indexOf("YOLIGO") != -1 || seqSampleArray.length() == 1)) {
                                    for (int ss = 0; ss < seqSampleArray.length(); ss++) {
                                        JSONObject obj = seqSampleArray.getJSONObject(ss);
                                        sampleStatus = obj.getString("yseqState");
                                        if (sampleStatus.indexOf("Error") != -1 || (sampleStatus.indexOf("error") != -1) || sampleStatus.indexOf("ERROR") != -1) {
                                            updatingAnnouncements(frequest, "Error status: " + msg);
                                            processSequencingBatchStatus(seqUrl, "Error");
                                            return null;
                                        }
                                        if (!status.equalsIgnoreCase(sampleStatus)) {
                                            log.info("Partial is true because of two different status, between multiple samples:" + status + " " + sampleStatus);
                                            numStatusDiff++;
                                        }
                                    }
                                }
                                String postStatus = status;
                                if (numStatusDiff > 1) {
                                    postStatus = postStatus + " Partial";
                                }
                                processSequencingBatchStatus(seqUrl, postStatus);
                                updatingAnnouncements(frequest, msg);
                                solrSeqObj.put("yseqState", postStatus);
                                postSolrUpdate(getSolrHost() + "/solr/update/json?commit=true", solrSeqObj);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        return null;
                    }
                });
            } else {
                log.error("Failed Authentication");
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.sendNotification("LIMS Error:Updating Status with " + seqKey + " sampleKey " + sampleKey, "Error updating status from slimseq hook\n" + seqKey + " sampleKey " + sampleKey + "\nerror msg:" + e);
            log.error("Email sent\nError exception:" + e + "\nURL:" + seqUrl);
        }
        this.sendNotification("LIMS Update Success: Status:" + seqKey, "updating status from slimseq hook\n" + seqKey + " sampleKey " + sampleKey + " \nstatus:" + status + " flowcell:lane " + meta);
        log.info("End processSequencingSampleStatus (" + seqUrl + ")," + " sequencing batch )");
    }

    protected void sendNotification(String subject, String message) {
        this.templateMessage.setSubject(subject);
        SimpleMailMessage smsg = new SimpleMailMessage(this.templateMessage);
        smsg.setText(message);
        this.mailSender.send(smsg);
    }

    @RequestMapping(value = "/**/test", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView processTestMapping(HttpServletRequest request, NativeWebRequest nativeWebRequest) throws Exception {
        log.info("\nBegin Testing POST");
        JSONObject json = new JSONObject();
        json.put("uri", "someURI");
        json.put("status", "Completed");
        json.put("cgsX", "nextDistinctX");
        ModelAndView mav = new ModelAndView(new JsonView());
        mav.addObject("json", json);
        log.info("\nDone Testing POST");
        return mav;
    }

    @RequestMapping(value = "/**/exportascsv", method = RequestMethod.GET)
    @ModelAttribute
    public ModelAndView processGetExportAsCsv(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("\nBegin GET ExportAsCsv");
        return processExportAsCsv(request, response);
    }

    @RequestMapping(value = "/**/exportascsv", method = RequestMethod.POST)
    @ModelAttribute
    public ModelAndView processExportAsCsv(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("\nBegin POST exportasCsv");
        Map reqParams = request.getParameterMap();
        JSONObject jsonObject = new JSONObject();
        for (Object keyObj : reqParams.keySet()) {
            String key = (String) keyObj;
            for (String value : request.getParameterValues(key)) {
                value = URLDecoder.decode(value);
                jsonObject.put(key, value);
            }
        }
        response.setContentType("text/csv");
        response.setHeader("Content-disposition", "attachment; filename=\"" + jsonObject.get("filename") + "\"");
        PrintWriter out = response.getWriter();
        String csvString = jsonObject.get("contenttext").toString();
        csvString = csvString.replace("\n", "");
        String[] csvContent = csvString.split(URLDecoder.decode(BREAKDELIMITER));
        for (String cline : csvContent) {
            cline = cline.trim();
            if (!cline.equals("") && !cline.equals("\n")) out.println(cline);
        }
        log.info("\nDone exportascsv");
        return null;
    }

    @RequestMapping(value = "/**/submit", method = RequestMethod.GET)
    @ModelAttribute
    public void processSequencingSubmissionGet(HttpServletRequest request, NativeWebRequest nativeWebRequest) throws Exception {
        processSequencingSubmission(request, nativeWebRequest);
    }

    @RequestMapping(value = "/**/submit", method = RequestMethod.POST)
    @ModelAttribute
    public void processSequencingSubmission(HttpServletRequest request, NativeWebRequest nativeWebRequest) throws Exception {
        log.info("\nBegin processSequencingSubmission POST");
        try {
            final String postbackUrl = getBaseUrl(request);
            JSONObject jsonObject = new JSONObject();
            JSONObject solrJsonObject = new JSONObject();
            String msg = "New Sequence submitted:";
            final String url = this.getSlimseqUrl() + "/sample_sets.json";
            Map reqParams = request.getParameterMap();
            for (Object keyObj : reqParams.keySet()) {
                String key = (String) keyObj;
                for (String value : request.getParameterValues(key)) {
                    log.info("sequence work order request parameters -> key: " + key + " value: " + value);
                    if (key.startsWith("yseq")) {
                        solrJsonObject.put(key, value);
                    }
                    jsonObject.put(key, value);
                }
            }
            boolean isMultiplex = false;
            if (jsonObject.getString("yseqSamplePrep").indexOf("Multiplex") != -1) {
                log.info("Request is of type Multiplex");
                isMultiplex = true;
                this.addMultipartParam(jsonObject, nativeWebRequest);
            }
            String seqName = jsonObject.getString("yseqKey");
            String instrument = jsonObject.getString("yseqInstrument");
            final String[] seqNameArray = seqName.split("SEQ");
            jsonObject.put("yseqState", "Submitted");
            solrJsonObject.put("yseqState", "Submitted");
            processSequenceSamples(seqName, solrJsonObject.getString("yseqSamples"));
            processSequencingJCRCreate(request, nativeWebRequest, jsonObject, "Submitted", isMultiplex);
            PostMethod post = new PostMethod(url);
            int nextRADSeq = -1;
            int nextRNASeq = -1;
            int statusCode = 0;
            String seqJsonParam = this.translateSlimseqWebHookApi(jsonObject, postbackUrl);
            if (instrument.equals("Illumina")) {
                log.info("Posting slimSeqJsonParam:\n " + seqJsonParam);
                post.setRequestEntity(new StringRequestEntity(seqJsonParam, "application/json", null));
                try {
                    post.setDoAuthentication(true);
                    post.setRequestHeader("Content-Type", "application/json");
                    this.httpClient.getParams().setAuthenticationPreemptive(true);
                    this.httpClient.getState().setCredentials(AuthScope.ANY, this.credentials);
                    statusCode = this.httpClient.executeMethod(post);
                    log.info("execute(" + url + "): SLIMSEQstatusCode: " + statusCode);
                    if (statusCode != 200) {
                        throw new Exception("Failed posting sequence work order " + jsonObject.getString("yseqKey") + " to slimSeq");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    this.sendNotification("LIMS Notification:Submission Status Error:" + url, "SlimSeq error processing " + url + "\nWith status\n" + statusCode + "\nReturn message:" + post.getResponseBodyAsString() + "\n" + e + "\nJake will be looking into it.");
                    log.error("Exception from slimseq:" + e + "\nURL:" + url);
                } finally {
                    post.releaseConnection();
                    log.info("released connection for url " + postbackUrl);
                }
            } else {
                log.info("Sending email to:" + this.templateMessage.getTo());
                this.templateMessage.setTo(this.getSolidContactList().split(","));
                this.sendNotification("LIMS News - New SOLiD Submission Parameters:" + seqName, translateSolidNotification(jsonObject));
            }
            String samples = jsonObject.getString("yseqSamples");
            if (samples.indexOf("RAD") != -1) {
                String[] samplesArray = samples.split("_");
                nextRADSeq = Integer.parseInt(samplesArray[2]);
            } else if (samples.indexOf("Oligo") != -1) {
                String[] samplesArray = samples.split("_");
                nextRNASeq = Integer.parseInt(samplesArray[2]);
            }
            processSequencingJCRAnnotation(request, Integer.parseInt(seqNameArray[1]), nextRADSeq, nextRNASeq);
            processSequencingJCRAnnouncements(request, jsonObject, isMultiplex, msg);
            this.postSolrUpdate(this.getSolrHost() + "/solr/update/json?commit=true", solrJsonObject);
            this.sendNotification("LIMS News - " + instrument + " Submitted Sucessful:YSEQ" + Integer.parseInt(seqNameArray[1]), "Submitted sequence order\n" + jsonObject + " Sequencing parameters::\n" + seqJsonParam + " status:" + statusCode);
            log.info("End processSequencingSubmission ");
        } catch (Exception e) {
            e.printStackTrace();
            this.sendNotification("LIMS Notification:Submission Error: url " + getBaseUrl(request), "Exception \n" + e);
            throw e;
        }
    }

    private void processSequenceSamples(String seqkey, String samples) throws Exception {
        try {
            String[] sampleList = samples.split(" ");
            for (String sample : sampleList) {
                JSONObject seqSmplJson = new JSONObject();
                seqSmplJson.put("yseqSmplKey", seqkey + "_" + sample);
                seqSmplJson.put("yseqSmplName", sample);
                seqSmplJson.put("yseqSmplStatus", "Submitted");
                this.postSolrUpdate(this.getSolrHost() + "/solr/update/json?commit=true", seqSmplJson);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private String translateSolidNotification(JSONObject jsonObject) throws JSONException {
        StringBuilder sb = new StringBuilder();
        sb.append("Dudley SOLiD Sequencing Submission Key:" + jsonObject.getString("yseqKey") + "\n");
        sb.append("Project:" + jsonObject.getString("yseqProject") + "\n");
        sb.append("Samples:" + jsonObject.getString("yseqSamples") + " alias:" + jsonObject.getString("yseqSampleAlias") + "\n\n");
        sb.append("SamplePrepKit:" + jsonObject.getString("yseqSamplePrep") + "\n");
        sb.append("Fragmentation:" + jsonObject.getString("yseqShearMethod") + "\n");
        sb.append("AdaptorKit:" + jsonObject.getString("yseqAdaptorKit") + "\n");
        sb.append("Primer:" + jsonObject.getString("yseqPrimer") + "\n");
        sb.append("Slider:" + jsonObject.getString("yseqSlider") + "\n");
        if (jsonObject.getString("yseqProject").indexOf("RAD") != -1) {
            sb.append("Multiplex Degree:" + jsonObject.getString("yseqMultiplexDegree") + "\n");
        }
        sb.append("ReadLength:" + jsonObject.getString("yseqReads") + " start:" + jsonObject.getString("yseqStart") + " end:" + jsonObject.getString("yseqEnd") + "\n\n");
        sb.append("Notes/Comments:" + jsonObject.getString("yseqNotes") + "\n");
        sb.append("NetworkFileServerPath:" + jsonObject.getString("yseqNFSSolid") + "\n");
        return sb.toString();
    }

    private String translateSlimseqWebHookApi(JSONObject jsonObject, String postbackUrl) throws JSONException {
        TranslateSlimSeq ssTranslate = new TranslateSlimSeq();
        String instrument = jsonObject.getString("yseqInstrument");
        String readFormat = jsonObject.getString("yseqReadFormat");
        String comments = "";
        if (jsonObject.getString("yseqNotes") != null) {
            comments = jsonObject.getString("yseqNotes");
        }
        String readFormatMsg = "";
        String solidMsg = "";
        if (readFormat.equals("Single")) {
            readFormatMsg = "\"alignment_start_position\": " + jsonObject.getString("yseqStart") + "," + "\"alignment_end_position\":" + jsonObject.getString("yseqEnd") + "," + "\"desired_read_length\": " + jsonObject.getString("yseqReads") + ",";
        } else {
            readFormatMsg = "\"read_format\": \"" + readFormat + "\"," + "\"desired_read_length_1\": \"" + jsonObject.getString("yseqReads") + "\"," + "\"alignment_start_position_1\": \"" + jsonObject.getString("yseqStart") + "\"," + "\"alignment_end_position_1\": \"" + jsonObject.getString("yseqEnd") + "\"," + "\"desired_read_length_2\": \"" + jsonObject.getString("yseqReads") + "\"," + "\"alignment_start_position_2\": \"" + jsonObject.getString("yseqStart") + "\"," + "\"alignment_end_position_2\": \"" + jsonObject.getString("yseqEnd") + "\",";
        }
        return "{\"sample_set\":{\"naming_scheme_id\": null, " + "\"sample_prep_kit_id\":" + TranslateSlimSeq.getSamplePrepId(jsonObject.getString("yseqSamplePrep")) + "," + "\"reference_genome_id\": " + TranslateSlimSeq.getRefGenomeId(jsonObject.getString("yseqRefGenome")) + "," + "\"project_id\": " + TranslateSlimSeq.getProjectId(jsonObject.getString("yseqProject")) + "," + readFormatMsg + "\"eland_parameter_set_id\":" + TranslateSlimSeq.getElandParamId() + "," + "\"budget_number\": \"" + jsonObject.getString("yseqBudget") + "\"," + "\"platform_id\": \"" + TranslateSlimSeq.getPlatformId(instrument) + "\"," + "\"primer_id\": \"" + TranslateSlimSeq.getPrimerId(jsonObject.getString("yseqPrimer")) + "\"," + solidMsg + "\"submitted_by\": \"" + jsonObject.getString("yseqContact").split("@")[0] + "\"," + "\"sample_mixtures\":[" + ssTranslate.translateSamples(postbackUrl, jsonObject.getString("yseqKey"), jsonObject.getString("yseqSamples").toUpperCase(), comments) + "]}}";
    }

    private void processSequencingJCRAnnouncements(HttpServletRequest request, JSONObject jsonObj, boolean isMultiplex, String msg) {
        log.info("Beginning of processSequencingJCRAnnouncements");
        final String announcementUrl = "/addama-rest/dudley-sample/path/announcement/annotations";
        GetMethod addamaGet = new GetMethod(getBaseUrl(request) + announcementUrl);
        try {
            String seqName = jsonObj.getString("yseqKey");
            String seqType = jsonObj.getString("yseqSamplePrep");
            String sampleDesc = jsonObj.getString("yseqSampleAlias");
            if (isMultiplex) {
                sampleDesc = jsonObj.getString("yseqNotes");
            }
            JSONObject jsonPost = new JSONObject();
            jsonPost.put("annDate", org.systemsbiology.addama.sequencing.util.Formatter.getDateString(new Date(), "MM/dd/yyyy"));
            jsonPost.put("text", msg + seqName + " type:" + seqType + " samples:" + sampleDesc);
            if (this.login(getHttpsUrl(request))) {
                addamaGet.setRequestHeader("Accept", "application/json");
                JSONObject jsonRespObj = (JSONObject) this.httpClientTemplate.executeMethod(addamaGet, new JsonResponseCallback());
                int annCount = jsonRespObj.getInt("ann_entry");
                final int annEntry = annCount + 1;
                final HttpServletRequest req = request;
                final String newAnnouncementUrl = "/addama-rest/dudley-sample/path/announcement/ann" + annEntry + "/create";
                final PostMethod post = new PostMethod(getBaseUrl(request) + newAnnouncementUrl);
                post.setQueryString(new NameValuePair[] { new NameValuePair("JSON", jsonPost.toString()) });
                this.httpClientTemplate.executeMethod(post, new ResponseCallback() {

                    public Object onResponse(int i, HttpMethod httpMethod) throws HttpClientResponseException {
                        if (i != 200) {
                            log.error("The post request failed on new announcements!");
                            post.releaseConnection();
                            throw new HttpClientResponseException(i, httpMethod, null);
                        } else {
                            log.info("Posted OKAY request on:" + newAnnouncementUrl);
                            processSequencingJCRAnnouncementEntry(req, annEntry);
                        }
                        post.releaseConnection();
                        return null;
                    }
                });
            } else {
                log.error("Failed Authentication");
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error exception:" + e + "\nURL:" + announcementUrl);
        } finally {
            addamaGet.releaseConnection();
            log.info("released connection for url " + announcementUrl);
        }
        log.info("End of processSequencingJCRAnnouncements");
    }

    private void updatingAnnouncements(HttpServletRequest request, String msg) {
        log.info("Beginning of updatingAnnouncements");
        final String announcementUrl = "/addama-rest/dudley-sample/path/announcement/annotations";
        GetMethod addamaGet = new GetMethod(getBaseUrl(request) + announcementUrl);
        try {
            JSONObject jsonPost = new JSONObject();
            jsonPost.put("annDate", org.systemsbiology.addama.sequencing.util.Formatter.getDateString(new Date(), "MM/dd/yyyy"));
            jsonPost.put("text", msg);
            if (this.login(getHttpsUrl(request))) {
                addamaGet.setRequestHeader("Accept", "application/json");
                JSONObject jsonRespObj = (JSONObject) this.httpClientTemplate.executeMethod(addamaGet, new JsonResponseCallback());
                int annCount = jsonRespObj.getInt("ann_entry");
                final int annEntry = annCount + 1;
                final HttpServletRequest req = request;
                final String newAnnouncementUrl = "/addama-rest/dudley-sample/path/announcement/ann" + annEntry + "/create";
                final PostMethod post = new PostMethod(getBaseUrl(request) + newAnnouncementUrl);
                post.setQueryString(new NameValuePair[] { new NameValuePair("JSON", jsonPost.toString()) });
                this.httpClientTemplate.executeMethod(post, new ResponseCallback() {

                    public Object onResponse(int i, HttpMethod httpMethod) throws HttpClientResponseException {
                        processSequencingJCRAnnouncementEntry(req, annEntry);
                        post.releaseConnection();
                        return null;
                    }
                });
            } else {
                log.error("Failed Authentication");
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error exception:" + e + "\nURL:" + announcementUrl);
        }
        log.info("End of updatingAnnouncements");
    }

    private void processSequencingJCRAnnouncementEntry(HttpServletRequest request, int annEntry) {
        log.info("Beginning of processSequencingJCRAnnouncements");
        final String newAnnouncementUrl = "/addama-rest/dudley-sample/path/announcement/annotations";
        final PostMethod post = new PostMethod(getBaseUrl(request) + newAnnouncementUrl);
        try {
            JSONObject jsonPost = new JSONObject();
            jsonPost.put("ann_entry", annEntry);
            if (this.login(getHttpsUrl(request))) {
                post.setQueryString(new NameValuePair[] { new NameValuePair("JSON", jsonPost.toString()) });
                this.httpClientTemplate.executeMethod(post, new ResponseCallback() {

                    public Object onResponse(int i, HttpMethod httpMethod) throws HttpClientResponseException {
                        if (i != 200) {
                            log.error("The post request failed on new announcements!");
                            throw new HttpClientResponseException(i, httpMethod, null);
                        } else {
                            log.info("Posted OKAY request on update annotation entry count:" + newAnnouncementUrl);
                        }
                        return null;
                    }
                });
            } else {
                log.error("Failed Authentication");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            post.releaseConnection();
        }
        log.info("End of processSequencingJCRAnnouncements");
    }

    private void processSequencingJCRCreate(HttpServletRequest request, NativeWebRequest nativeWebRequest, JSONObject jsonObj, String status, boolean isMultiplex) throws Exception {
        String seqName = jsonObj.getString("yseqKey");
        final String[] seqNameArray = seqName.split("SEQ");
        final String seqDate = jsonObj.getString("yseqDate");
        jsonObj.put("yseqState", status);
        final boolean isMultiplexFinal = isMultiplex;
        final HttpServletRequest frequest = request;
        final String sampleStatus = status;
        final String url = "/addama-rest/dudley-sample/path/datasource/YSEQ/YSEQ" + seqNameArray[1] + "/create";
        final String baseUrl = getBaseUrl(request);
        final String samplesInString = jsonObj.getString("yseqSamples").toUpperCase();
        log.info("Begin processSequencingJCRCreate (" + url + ")," + new Date() + " sequencing batch  )");
        final String seqKey = "YSEQ" + seqNameArray[1];
        final String project = jsonObj.getString("yseqProject");
        PostMethod postCreate = new PostMethod(baseUrl + url);
        try {
            if (this.login(getHttpsUrl(request))) {
                postCreate.setQueryString(new NameValuePair[] { new NameValuePair("JSON", jsonObj.toString()) });
                if (isMultiplexFinal) {
                    forwardAttachments(nativeWebRequest, postCreate, seqKey);
                }
                this.httpClientTemplate.executeMethod(postCreate, new ResponseCallback() {

                    public Object onResponse(int status, HttpMethod httpMethod) throws HttpClientResponseException {
                        if (status != 200) {
                            throw new HttpClientResponseException(status, httpMethod, null);
                        } else {
                            log.info("CREATE " + url + " successful: Call function to create samples (swap space with underscore):" + samplesInString);
                            if (!isMultiplexFinal) {
                                if (samplesInString.length() > 1) {
                                    String[] samples = samplesInString.split(" ");
                                    for (int i = 0; i < samples.length; i++) {
                                        String sampleUrl = "/addama-rest/dudley-sample/path/datasource/YSEQ/YSEQ" + seqNameArray[1] + "/" + samples[i];
                                        processSequencingJCRSamples(baseUrl + sampleUrl + "/create", sampleStatus, seqDate);
                                        setDudleySharingView(frequest, sampleUrl);
                                        processOmicsView(seqKey, "NGS", samples[i], project, "submitted", "", "", "", "INSERT");
                                    }
                                }
                            } else {
                                String sampleUrl = "/addama-rest/dudley-sample/path/datasource/YSEQ/YSEQ" + seqNameArray[1] + "/" + samplesInString;
                                sampleUrl = sampleUrl.replaceAll(" ", "_");
                                processSequencingJCRSamples(baseUrl + sampleUrl + "/create", sampleStatus, seqDate);
                                setDudleySharingView(frequest, sampleUrl);
                            }
                            return null;
                        }
                    }
                });
            } else {
                log.error("Failed Authentication");
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.sendNotification("LIMS Notification:Create Status Error:" + url, "Create error while processing " + url + "\nExeception\n" + e + "\nJake will be looking into it.");
            throw e;
        } finally {
            postCreate.releaseConnection();
            log.info("released connection for url " + url);
        }
        log.info("End processSequencingJCRCreate (" + url + ")," + new Date() + " sequencing batch )");
    }

    private void processSequencingJCRSamples(String url, String status, String seqDate) {
        log.info("Begin processSequencingJCRSamples: " + url + " " + new Date());
        PostMethod samplePost = new PostMethod(url);
        final String sampleUrl = url;
        JSONObject sampleJson = new JSONObject();
        try {
            sampleJson.put("sampleStatus", status);
            sampleJson.put("sampleStatusDate", seqDate);
            sampleJson.put("flow_cell_name", "");
            sampleJson.put("lane", "");
            sampleJson.put("comment", "");
            samplePost.setQueryString(new NameValuePair[] { new NameValuePair("JSON", sampleJson.toString()) });
            this.httpClientTemplate.executeMethod(samplePost, new ResponseCallback() {

                public Object onResponse(int status, HttpMethod httpMethod) throws HttpClientResponseException {
                    if (status != 200) {
                        log.error("The post create request failed:" + status);
                        throw new HttpClientResponseException(status, httpMethod, null);
                    } else {
                        log.info("Sample " + sampleUrl + " Posted SUCCESSFULLY");
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            try {
                throw e;
            } catch (Exception e1) {
                e1.printStackTrace();
                samplePost.releaseConnection();
            }
        } finally {
            samplePost.releaseConnection();
            log.info("released connection for url " + url);
        }
        log.info("Done processSequencingJCRSamples: " + url + " " + new Date());
    }

    private void processSequencingJCRAnnotation(HttpServletRequest request, int seq, int radSeq, int oligoSeq) throws Exception {
        log.info("Handling sequencing jcr update annotation");
        JSONObject resp = this.getSolr(this.getSolrHost() + "/solr/select/?q=limsadminKey:dudley_limsadminkey&wt=json&fl=limsadminKey,limsadminYoCount,limsadminYoMaxNum,limsadminYoBoxNum,limsadminYoPosition,limsadminYadCount,limsadminYadMaxNum,limsadminYadBoxNum,limsadminYadPosition,limsadminYpgCount,limsadminYpgMaxNum,limsadminYpgBoxNum,limsadminYpgPosition,limsadminChemCount,limsadminChemMaxNum,limsadminDOligoCount,limsadminDOligoMaxNum,limsadminLOligoCount,limsadminLOligoMaxNum,limsadminPlasmidCount,limsadminPlasmidMaxNum,limsadminPlasmidBoxNum,limsadminCrossingCount,limsadminYseqCount,limsadminYseqOligoCount,limsadminYseqRADCount,limsadminYcgsCount");
        JSONArray jsonArray = resp.getJSONArray("docs");
        JSONObject adminObj = (JSONObject) jsonArray.get(0);
        final String addamaUrl = "/addama-rest/dudley-sample/path/datasource/YSEQ/annotations";
        PostMethod post = new PostMethod(getBaseUrl(request) + addamaUrl);
        try {
            JSONObject annotationJson = new JSONObject();
            annotationJson.put("yseqCount", seq + "");
            annotationJson.put("yseqMaxNum", seq + "");
            adminObj.put("limsadminYseqCount", seq + "");
            if (radSeq != -1) {
                annotationJson.put("yseqRADCount", radSeq + "");
                adminObj.put("limsadminYseqRADCount", radSeq + "");
            }
            if (oligoSeq != -1) {
                annotationJson.put("yseqOligoCount", oligoSeq + "");
                adminObj.put("limsadminYseqOligoCount", radSeq + "");
            }
            this.postSolrUpdate(this.getSolrHost() + "/solr/update/json?commit=true", adminObj);
            post.setQueryString(new NameValuePair[] { new NameValuePair("JSON", annotationJson.toString()) });
            this.httpClientTemplate.executeMethod(post, new ResponseCallback() {

                public Object onResponse(int status, HttpMethod httpMethod) throws HttpClientResponseException {
                    if (status != 200) {
                        log.error("processSequencingJCRAnnotation post request failed with status:" + status);
                        throw new HttpClientResponseException(status, httpMethod, null);
                    } else {
                        log.info("Posted OKAY request on:" + addamaUrl);
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error exception:" + e + "\nURL:" + addamaUrl);
            this.sendNotification("LIMS Notification:Annotation Error:" + addamaUrl, "Create error while processing " + addamaUrl + "\nExeception\n" + e + "\nJake will be looking into it.");
        } finally {
            post.releaseConnection();
            log.info("released connection for url " + addamaUrl);
        }
        log.info("End processSequencingJCRAnnotation (" + addamaUrl + ")," + " sequencing batch )");
    }

    private void processSequencingBatchStatus(String seqBatchUri, String status) throws Exception {
        log.info("Begin processSequencingBatchStatus() " + "uri " + seqBatchUri + " status " + status);
        final String addamaUrl = seqBatchUri;
        PostMethod post = new PostMethod(addamaUrl);
        try {
            JSONObject annotationJson = new JSONObject();
            annotationJson.put("yseqState", status);
            post.setQueryString(new NameValuePair[] { new NameValuePair("JSON", annotationJson.toString()) });
            this.httpClientTemplate.executeMethod(post, new ResponseCallback() {

                public Object onResponse(int status, HttpMethod httpMethod) throws HttpClientResponseException {
                    if (status != 200) {
                        log.error("processSequencingBatchStatus post request failed with status:" + status);
                        throw new HttpClientResponseException(status, httpMethod, null);
                    } else {
                        log.info("Posted OKAY request on:" + addamaUrl);
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error exception:" + e + "\nURL:" + addamaUrl);
            this.sendNotification("LIMS Notification:BatchStatus Error:" + addamaUrl, "Create error while processing " + addamaUrl + "\nExeception\n" + e + "\nJake will be looking into it.");
        } finally {
            post.releaseConnection();
            log.info("released connection for url " + addamaUrl);
        }
        log.info("End processSequencingBatchStatus (" + addamaUrl + ")");
    }

    private void setDudleySharingView(HttpServletRequest request, String uri) {
        log.info("Begin setDudleySharingView - read/write for Dudley Lab");
        final String addamaUrl = "/addama-sharing/viewing" + uri;
        PostMethod post = new PostMethod(getBaseUrl(request) + addamaUrl);
        try {
            JSONObject annotationJson = new JSONObject();
            annotationJson.put("everybody", "true");
            post.setQueryString(new NameValuePair[] { new NameValuePair("acl", annotationJson.toString()) });
            this.httpClientTemplate.executeMethod(post, new ResponseCallback() {

                public Object onResponse(int status, HttpMethod httpMethod) throws HttpClientResponseException {
                    if (status != 200) {
                        log.error("setDudleySharingView post request failed with status:" + status);
                        throw new HttpClientResponseException(status, httpMethod, null);
                    } else {
                        log.info("Posted OKAY request on:" + addamaUrl);
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error exception:" + e + "\nURL:" + addamaUrl);
        } finally {
            post.releaseConnection();
            log.info("released connection for url " + addamaUrl);
        }
        log.info("End setDudleySharingView (" + addamaUrl + ")," + " uri: " + uri);
    }

    @RequestMapping(value = "/**/state", method = RequestMethod.POST)
    @ModelAttribute
    public void processSequencingState(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String sample = request.getParameter("sample");
        String json = request.getParameter("JSON");
        final String url = this.getSlimseqUrl();
        GetMethod get = new GetMethod(url);
        try {
            get.setDoAuthentication(true);
            get.setRequestHeader("Accept", "application/json");
            this.httpClient.getParams().setAuthenticationPreemptive(true);
            Credentials defaultcreds = new UsernamePasswordCredentials("slimbot", "test");
            this.httpClient.getState().setCredentials(AuthScope.ANY, defaultcreds);
            int statusCode = this.httpClient.executeMethod(get);
            log.info("execute(" + url + "): statusCode: " + statusCode);
            log.info(" response body:" + get.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error exception:" + e + "\nURL:" + url);
        } finally {
            get.releaseConnection();
            log.info("released connection for url " + url);
        }
        log.info("End processSequencingState (" + url + ")," + new Date() + " sequencing batch " + sample + " )");
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

    private String getYseqFromUri(String requestUri, String pathKey) {
        log.info("uri:" + requestUri);
        int i1 = requestUri.indexOf(pathKey);
        log.info("uri:" + requestUri + " index:" + i1 + " pathKey:" + pathKey);
        return requestUri.substring(i1 + pathKey.length(), requestUri.length());
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    private String getDecodedRequestUri(HttpServletRequest request) {
        try {
            return URLDecoder.decode(request.getRequestURI(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private void addMultipartParam(JSONObject jsonObject, NativeWebRequest nativeWebRequest) throws JSONException {
        Object nativeRequest = nativeWebRequest.getNativeRequest();
        if (nativeRequest instanceof DefaultMultipartHttpServletRequest) {
            DefaultMultipartHttpServletRequest multipartRequest = (DefaultMultipartHttpServletRequest) nativeRequest;
            filesByName.putAll(multipartRequest.getFileMap());
            Iterator it = this.filesByName.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry mp = (Map.Entry) it.next();
                MultipartFile file = (MultipartFile) mp.getValue();
                log.info("Processing file:" + file.getOriginalFilename());
                jsonObject.put(mp.getKey().toString(), file.getOriginalFilename());
            }
        }
    }

    private void forwardAttachments(NativeWebRequest nativeWebRequest, EntityEnclosingMethod method, String seqKey) throws IOException {
        Object nativeRequest = nativeWebRequest.getNativeRequest();
        if (nativeRequest instanceof MultipartHttpServletRequest) {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) nativeRequest;
            Map<String, MultipartFile> filesByName = multipartRequest.getFileMap();
            ArrayList<Part> parts = new ArrayList<Part>();
            for (Map.Entry<String, MultipartFile> entry : filesByName.entrySet()) {
                log.info("Processing seq file:" + entry.getValue().getOriginalFilename());
                try {
                    parts.add(new FilePart(entry.getValue().getOriginalFilename(), new MultipartFilePartSource(entry.getValue())));
                    InputStream inputStream = entry.getValue().getInputStream();
                    Reader r = new InputStreamReader(inputStream, "US-ASCII");
                    BufferedReader br = new BufferedReader(r);
                    try {
                        String line;
                        while ((line = br.readLine()) != null) {
                            String[] tokens = line.split(",");
                            log.info("Processing rad csv line:" + line + " tk.size:" + tokens.length);
                            String project = tokens[0];
                            String plate = tokens[1];
                            String position = tokens[2];
                            String tetrad = tokens[3];
                            String sample = tokens[4].replaceAll(" ", "_");
                            String barcode_index = tokens[5];
                            String barcode = tokens[6];
                            String comment = plate + ":" + position + ":" + tetrad + ":" + barcode + ":" + barcode_index;
                            processOmicsViewCSV(seqKey, "NGS", sample, project, "submitted", "", comment, barcode, barcode_index, position, tetrad, plate, "INSERT");
                        }
                    } finally {
                        br.close();
                    }
                    inputStream.close();
                } catch (IOException e) {
                    log.error("IO Exception at reading upload file stream");
                    this.sendNotification("LIMS Notification:Multiplex-Omics populating Error:" + seqKey, "SlimSeq error processing " + seqKey + "\nWith exception\n" + e);
                    throw e;
                }
            }
            if (!parts.isEmpty()) {
                method.setRequestEntity(new MultipartRequestEntity(parts.toArray(new Part[parts.size()]), method.getParams()));
            }
        }
    }

    protected void processOmicsView(String yseqKey, String type, String sample, String project, String status, String key_meta, String comments, String barcode, String op) {
        log.info("Begin processOmicsView (" + yseqKey + " " + new Date() + " sequencing batch " + sample + " )");
        final String key = yseqKey;
        final String ftype = type;
        final String si = sample;
        final String fproject = project;
        final String fstatus = status;
        final String fmeta = key_meta;
        final String fcomments = comments;
        final String fbarcode = barcode;
        final java.sql.Date fdate = new java.sql.Date(System.currentTimeMillis());
        PreparedStatementSetter insertPS = new PreparedStatementSetter() {

            public void setValues(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, key);
                preparedStatement.setString(2, ftype);
                preparedStatement.setDate(3, fdate);
                preparedStatement.setString(4, fproject);
                preparedStatement.setString(5, fstatus);
                preparedStatement.setDate(6, fdate);
                preparedStatement.setString(7, si);
                preparedStatement.setString(8, fmeta);
                preparedStatement.setString(9, fcomments);
                preparedStatement.setString(10, fbarcode);
            }
        };
        if (op.equals("INSERT")) {
            String insertSql = "INSERT INTO omics_samples (omics_key,omics_type,submitted_date,project,status,status_date,strains,key_meta,comments,barcode) VALUES (?,?,?,?,?,?,?,?,?,?)";
            jdbcTemplate.update(insertSql, insertPS);
        } else {
            if (sample.equals("*")) {
                String updateSql = "UPDATE omics_samples set status=?, status_date=?, key_meta=? where omics_key = ?";
                jdbcTemplate.update(updateSql, new Object[] { fstatus, fdate, fmeta, key });
            } else {
                String updateSql = "UPDATE omics_samples set status=?, status_date=?, key_meta=? where omics_key = ? and strains = ?";
                jdbcTemplate.update(updateSql, new Object[] { fstatus, fdate, fmeta, key, sample });
            }
        }
        log.info("Done processOmicsView (" + yseqKey + " " + new Date() + " sequencing batch " + sample + " )");
    }

    protected void processOmicsViewCSV(String yseqKey, String type, String sample, String project, String status, String key_meta, String comments, String barcode, String barcode_index, String position, String tetrad, String plate, String op) {
        log.info("Begin processOmicsViewCSV (" + yseqKey + " " + new Date() + " sequencing batch " + sample + " )");
        final String key = yseqKey;
        final String ftype = type;
        final String si = sample;
        final String fproject = project;
        final String fstatus = status;
        final String fmeta = key_meta;
        final String fcomments = comments;
        final String fbarcode = barcode;
        final String fbarcode_index = barcode_index;
        final String fposition = position;
        final String ftetrad = tetrad;
        final String fplate = plate;
        final java.sql.Date fdate = new java.sql.Date(System.currentTimeMillis());
        PreparedStatementSetter insertPS = new PreparedStatementSetter() {

            public void setValues(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, key);
                preparedStatement.setString(2, ftype);
                preparedStatement.setDate(3, fdate);
                preparedStatement.setString(4, fproject);
                preparedStatement.setString(5, fstatus);
                preparedStatement.setDate(6, fdate);
                preparedStatement.setString(7, si);
                preparedStatement.setString(8, fmeta);
                preparedStatement.setString(9, fcomments);
                preparedStatement.setString(10, fbarcode);
                preparedStatement.setString(11, fbarcode_index);
                preparedStatement.setString(12, fposition);
                preparedStatement.setString(13, ftetrad);
                preparedStatement.setString(14, fplate);
            }
        };
        if (op.equals("INSERT")) {
            String insertSql = "INSERT INTO omics_samples (omics_key,omics_type,submitted_date,project,status,status_date,strains,key_meta,comments,barcode,barcode_index,position,tetrad, plate) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            jdbcTemplate.update(insertSql, insertPS);
        }
        log.info("Done processOmicsViewCSV (" + yseqKey + " " + new Date() + " sequencing batch " + sample + " )");
    }
}
