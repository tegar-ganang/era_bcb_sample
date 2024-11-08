package com.google.code.sagetvaddons.sre.server;

import gkusnick.sagetv.api.AiringAPI;
import gkusnick.sagetv.api.MediaFileAPI;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A Servlet that allows viewing and modifications to the SRE configuration options 
 * @version $Id: SREServlet.java 737 2010-01-04 22:32:11Z derek@battams.ca $
 */
public class SREServlet extends HttpServlet {

    private static final long serialVersionUID = -5678755661760866175L;

    /**
	 * Handles GET requests to the servlet
	 * @param req Holds the details of the servlet request
	 * @param resp Holds the details of the servlet response
	 * @throws ServletException
	 * @throws IOException
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String sub = req.getParameter("sub");
        if (sub != null) {
            doAjax(req, resp);
            return;
        }
        resp.setContentType("text/html");
        resp.setDateHeader("Expires", 0);
        resp.setHeader("Cache-Control", "no-cache, must-revalidate");
        PrintWriter out = resp.getWriter();
        resp.setStatus(301);
        resp.setHeader("Location", "/sre/SREClient.html");
        out.flush();
        return;
    }

    /**
	 * Handle post requests to the servlet
	 * @param req Holds details of the request made to the servlet
	 * @param resp Holds details of the response from the servlet back to the client
	 * @throws ServletException
	 * @throws IOException
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
        return;
    }

    private void doAjax(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> root = new HashMap<String, Object>();
        resp.setContentType("text/plain");
        resp.setDateHeader("Expires", new Date(0).getTime());
        resp.setHeader("Cache-Control", "no-cache, must-revalidate");
        PrintWriter out = resp.getWriter();
        String sub = req.getParameter("sub");
        if (sub.equals("conf")) {
            runConfig(req, root);
            loadConfig(root);
        } else if (sub.equals("echoFrm")) {
            loadConfig(root);
        } else if (sub.equals("echoOverride")) {
            loadOverride(root);
        } else if (sub.equals("addOverride")) {
            doOverride(req, root);
        } else if (sub.equals("echoProg")) {
            doEchoProgram(req, root);
        } else if (sub.equals("rmOverride")) {
            doDelOverride(req, root);
        } else if (sub.equals("debug")) {
            loadDebug(root);
        } else if (sub.equals("log")) {
            BufferedReader r = new BufferedReader(new FileReader("sre.log"));
            String line;
            while ((line = r.readLine()) != null) out.write(line + "\n");
            r.close();
            return;
        } else if (sub.equals("lastRun")) {
            out.write(DataStore.getInstance().getSetting(DataStore.LAST_SCAN, DataStore.DEFAULT_LAST_SCAN));
            out.flush();
            return;
        } else if (sub.equals("saveDebug")) {
            saveDebug(Boolean.parseBoolean(req.getParameter("test_mode")), Boolean.parseBoolean(req.getParameter("test_monitor")));
        } else throw new IOException("Invalid submodule requested");
        JSONObject json = new JSONObject();
        Collection<?> list = (Collection<?>) root.get("errors");
        if (list != null) {
            JSONArray jarr = new JSONArray();
            for (Object o : list) jarr.put(o);
            try {
                json.put("errors", jarr);
            } catch (JSONException e) {
            }
        }
        for (String s : root.keySet()) {
            try {
                json.put(s, root.get(s));
            } catch (JSONException e) {
            }
        }
        out.write(json.toString());
        out.flush();
        return;
    }

    private void doEchoProgram(HttpServletRequest req, Map<String, Object> root) throws IOException {
        String idStr = req.getParameter("id");
        if (idStr == null) throw new IOException("Airing ID is invalid");
        int id = Integer.parseInt(idStr);
        AiringAPI.Airing a = SageRecordingExtender.SageApi.airingAPI.GetAiringForID(id);
        if (a == null) throw new IOException("Airing ID is invalid");
        root.put("title", a.GetAiringTitle());
        root.put("subtitle", a.GetShow().GetShowEpisode());
    }

    private void doDelOverride(HttpServletRequest req, Map<String, Object> root) throws IOException {
        String idStr = req.getParameter("id");
        if (idStr == null) throw new IOException("Airing ID is invalid");
        int id = Integer.parseInt(idStr);
        if (!DataStore.getInstance().deleteOverride(id)) throw new IOException("Override deletion failed");
        AiringAPI.Airing a = SageRecordingExtender.SageApi.airingAPI.GetAiringForID(id);
        if (a == null) throw new IOException("Airing ID is invalid");
        root.put("title", a.GetAiringTitle());
        root.put("subtitle", a.GetShow().GetShowEpisode());
        root.put("override", false);
        root.put("id", Integer.toString(id));
        root.put("start", a.GetAiringStartTime());
        root.put("end", a.GetAiringEndTime());
        DataStore.getInstance().setMonitorStatus(a.GetAiringID(), AiringMonitor.ERROR);
        root.put("status", SageRecordingExtender.getMonitorStatus(a.GetAiringID()));
    }

    private void doOverride(HttpServletRequest req, Map<String, Object> root) throws IOException {
        String idStr = req.getParameter("id");
        String title = req.getParameter("title");
        String subtitle = req.getParameter("subtitle");
        if (idStr == null) throw new IOException("Airing ID is invalid");
        int id = Integer.parseInt(req.getParameter("id"));
        AiringAPI.Airing a = SageRecordingExtender.SageApi.airingAPI.GetAiringForID(id);
        if (a == null) throw new IOException("Airing ID is invalid");
        if (title == null || subtitle == null) throw new IOException("Title and subtitle must be specified");
        if (title.length() > 0 && subtitle.length() > 0 && !subtitle.equals(a.GetShow().GetShowEpisode())) {
            AiringMonitor mon = AiringMonitorFactory.getInstance(title, subtitle, a.GetAiringStartTime(), 0);
            if (mon != null && (mon.isValid() || SageRecordingExtender.airsInFuture(a.GetAiringStartTime()))) {
                if (!DataStore.getInstance().addOverride(id, title, subtitle)) throw new IOException("Updating override failed");
                root.put("id", Integer.toString(id));
                root.put("title", title);
                root.put("subtitle", subtitle);
                root.put("start", a.GetAiringStartTime());
                root.put("end", a.GetAiringEndTime());
                root.put("status", AiringMonitor.Status.FOUND.ordinal());
                root.put("origSubtitle", a.GetShow().GetShowEpisode());
                if (subtitle.length() != 0) root.put("override", true); else root.put("override", false);
                DataStore.getInstance().setMonitorStatus(id, AiringMonitor.Status.FOUND.ordinal());
            } else {
                doDelOverride(req, root);
                root.put("error", "Specified override does not create a valid AiringMonitor object!  Override deleted.");
            }
        }
        return;
    }

    private void loadDebug(Map<String, Object> root) {
        JSONArray jarr = new JSONArray();
        synchronized (SageRecordingExtender.monitorStats) {
            for (MediaFileAPI.MediaFile mf : SageRecordingExtender.SageApi.global.GetCurrentlyRecordingMediaFiles()) {
                JSONObject jobj = new JSONObject();
                try {
                    jobj.put("id", mf.GetMediaFileAiring().GetAiringID());
                    jobj.put("title", mf.GetMediaTitle());
                    SageRecordingExtender.MonitorStatus status = SageRecordingExtender.monitorStats.get(mf.GetMediaFileID());
                    if (status == null) status = SageRecordingExtender.MonitorStatus.UNKNOWN;
                    jobj.put("status", status.toString());
                    String monType;
                    AiringMonitor mon = new MonitoredAiring(mf.GetMediaFileAiring().GetAiringID()).getMonitor();
                    if (mon == null) monType = "none"; else monType = mon.getClass().getName();
                    jobj.put("type", monType);
                    jarr.put(jobj);
                } catch (JSONException e) {
                }
            }
        }
        root.put("dump", jarr);
        DataStore data = DataStore.getInstance();
        root.put("test_mode", Boolean.parseBoolean(data.getSetting(DataStore.TEST_MODE, DataStore.DEFAULT_TEST_MODE)));
        root.put("test_monitor", Boolean.parseBoolean(data.getSetting(DataStore.TEST_MODE_DONE, DataStore.DEFAULT_TEST_MODE_DONE)));
        return;
    }

    private void saveDebug(boolean state, boolean isDone) {
        DataStore data = DataStore.getInstance();
        data.setSetting(DataStore.TEST_MODE, Boolean.toString(state));
        data.setSetting(DataStore.TEST_MODE_DONE, Boolean.toString(isDone));
        return;
    }

    private void loadOverride(Map<String, Object> root) {
        boolean clearOverrideTbl = true;
        JSONArray jarr = new JSONArray();
        DataStore data;
        try {
            data = DataStore.getInstance();
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return;
        }
        String title, subtitle;
        boolean isOverride;
        for (AiringAPI.Airing a : SageRecordingExtender.SageApi.global.GetScheduledRecordings()) {
            int status = SageRecordingExtender.getMonitorStatus(a.GetAiringID());
            String[] override = data.getOverride(a.GetAiringID());
            if (override == null) {
                title = a.GetAiringTitle();
                subtitle = a.GetShow().GetShowEpisode();
                isOverride = false;
            } else {
                title = override[0];
                subtitle = override[1];
                isOverride = true;
                clearOverrideTbl = false;
            }
            JSONObject json = new JSONObject();
            try {
                json.put("id", Integer.toString(a.GetAiringID()));
                json.put("title", title);
                json.put("subtitle", subtitle);
                json.put("override", isOverride);
                json.put("status", status);
                json.put("start", a.GetAiringStartTime());
                json.put("end", a.GetAiringEndTime());
                jarr.put(json);
            } catch (JSONException e) {
                e.printStackTrace(System.out);
            }
        }
        root.put("overrideTbl", jarr);
        if (clearOverrideTbl) {
            DataStore.getInstance().clearOverrides();
        }
        return;
    }

    private void loadConfig(Map<String, Object> root) {
        DataStore data = DataStore.getInstance();
        root.put("status", Boolean.parseBoolean(data.getSetting(DataStore.WORKER_STATE, Boolean.toString(DataStore.DEFAULT_WORKER_STATE))));
        long mins = Long.parseLong(data.getSetting(DataStore.SLEEP_TIME, DataStore.DEFAULT_SLEEP_TIME)) / 60000;
        root.put("run_freq", mins);
        root.put("end_early", Boolean.parseBoolean(data.getSetting(DataStore.ALLOW_EARLY_END, DataStore.DEFAULT_ALLOW_EARLY_END)));
        mins = Long.parseLong(data.getSetting(DataStore.MAX_EXT_LENGTH, DataStore.DEFAULT_MAX_EXT_LENGTH)) / 60000;
        root.put("max_ext_time", mins / 60);
        mins = Long.parseLong(data.getSetting(DataStore.PADDING, DataStore.DEFAULT_PADDING)) / 60000;
        root.put("default_padding", mins);
        root.put("safety_net", Boolean.parseBoolean(data.getSetting(DataStore.ENABLE_SAFETY_NET, DataStore.DEFAULT_ENABLE_SAFETY_NET)));
        root.put("unmark_favs", Boolean.parseBoolean(data.getSetting(DataStore.UNMARK_FAVS, DataStore.DEFAULT_UNMARK_FAVS)));
        root.put("ignore_b2b", Boolean.parseBoolean(data.getSetting(DataStore.IGNORE_B2B, DataStore.DEFAULT_IGNORE_B2B)));
        root.put("exit_url", data.getSetting(DataStore.EXIT_URL, ""));
        root.put("notify_sage_alert", Boolean.parseBoolean(data.getSetting(DataStore.NOTIFY_SAGE_ALERT, DataStore.DEFAULT_NOTIFY_SAGE_ALERT)));
        root.put("sage_alert_url", data.getSetting(DataStore.SAGE_ALERT_URL, ""));
        root.put("sage_alert_id", data.getSetting(DataStore.SAGE_ALERT_ID, ""));
        root.put("sage_alert_pwd", data.getSetting(DataStore.SAGE_ALERT_PWD, ""));
        return;
    }

    private void runConfig(HttpServletRequest req, Map<String, Object> root) {
        List<String> errors = new ArrayList<String>();
        Map<String, String> sreOpts = new HashMap<String, String>();
        sreOpts.put(DataStore.WORKER_STATE, Boolean.valueOf(req.getParameter("status")).toString());
        sreOpts.put(DataStore.ALLOW_EARLY_END, Boolean.valueOf(req.getParameter("end_early")).toString());
        sreOpts.put(DataStore.TEST_MODE, Boolean.valueOf(req.getParameter("test_mode")).toString());
        sreOpts.put(DataStore.TEST_MODE_DONE, Boolean.valueOf(req.getParameter("test_monitor")).toString());
        sreOpts.put(DataStore.ENABLE_SAFETY_NET, Boolean.valueOf(req.getParameter("safety_net")).toString());
        sreOpts.put(DataStore.UNMARK_FAVS, Boolean.valueOf(req.getParameter("unmark_favs")).toString());
        sreOpts.put(DataStore.IGNORE_B2B, Boolean.valueOf(req.getParameter("ignore_b2b")).toString());
        sreOpts.put(DataStore.EXIT_URL, req.getParameter("exit_url"));
        sreOpts.put(DataStore.NOTIFY_SAGE_ALERT, Boolean.valueOf(req.getParameter("notify_sage_alert")).toString());
        sreOpts.put(DataStore.SAGE_ALERT_URL, req.getParameter("sage_alert_url"));
        sreOpts.put(DataStore.SAGE_ALERT_ID, req.getParameter("sage_alert_id"));
        sreOpts.put(DataStore.SAGE_ALERT_PWD, req.getParameter("sage_alert_pwd"));
        int run_freq;
        try {
            run_freq = Integer.parseInt(req.getParameter("run_freq"));
        } catch (NumberFormatException nfe) {
            errors.add("Run frequency must be an integer; using default value of 5.");
            run_freq = 5;
        }
        if (run_freq < 2 || run_freq > 20) {
            errors.add("Run frequency must be greater than 1 and less than 21; using default value of 5.");
            run_freq = 5;
        }
        sreOpts.put(DataStore.SLEEP_TIME, Integer.valueOf(run_freq * 60000).toString());
        int max_ext_time;
        try {
            max_ext_time = Integer.parseInt(req.getParameter("max_ext_time"));
        } catch (NumberFormatException nfe) {
            errors.add("Max extension time must be an integer; using default value of 8.");
            max_ext_time = 8;
        }
        if (max_ext_time < 1 || max_ext_time > 16) {
            errors.add("Max extension time must be greater than 0 and less than 17; using default value of 8.");
            max_ext_time = 8;
        }
        sreOpts.put(DataStore.MAX_EXT_LENGTH, Integer.valueOf(max_ext_time * 3600000).toString());
        int default_pad;
        try {
            default_pad = Integer.parseInt(req.getParameter("default_padding"));
        } catch (NumberFormatException e) {
            errors.add("Default padding must be an integer; using default value of 0.");
            default_pad = 0;
        }
        if (default_pad < 0 || default_pad > 120) {
            errors.add("Default padding must be between 0 and 120; using default value of 0.");
            default_pad = 0;
        }
        sreOpts.put(DataStore.PADDING, Integer.valueOf(default_pad * 60000).toString());
        for (String var : sreOpts.keySet()) DataStore.getInstance().setSetting(var, sreOpts.get(var));
        if (errors.size() > 0) root.put("errors", errors);
        return;
    }
}
