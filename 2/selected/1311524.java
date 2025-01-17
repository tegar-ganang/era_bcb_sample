package org.spnt.echo.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spnt.servlet.service.SpntEchoRepository;
import org.spnt.wami.SpntRelay;
import org.spnt.wami.util.WamiUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import edu.mit.csail.sls.wami.util.ServletUtils;

@Controller
public class PlayController {

    private static Logger LOG = LoggerFactory.getLogger(PlayController.class);

    public static final AudioFormat playFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);

    @Autowired
    private SpntEchoRepository spntEchoRepository;

    @RequestMapping(method = RequestMethod.GET, value = "/play")
    public void getPlay(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long lastTimestamp = findLastTimeStamp(request);
        if (isPollRequest(request)) {
            Long lastEvent = spntEchoRepository.findLastEvent();
            if (lastEvent > lastTimestamp) {
                getRelay(request).play(spntEchoRepository.findLastEventAudio(lastEvent));
                response.addHeader("LastTimestamp", "" + lastEvent);
            }
            doPollRequest(request, response);
            return;
        }
        if (isForwardRequest(request)) {
            doForwardRequest(request, response);
        }
    }

    /**
	 * 
	 * @param request
	 * @return
	 */
    private Long findLastTimeStamp(HttpServletRequest request) {
        @SuppressWarnings("unchecked") Enumeration<String> lastTimestampEnum = request.getHeaders("LastTimestamp");
        Long lastTimestamp = null;
        if (lastTimestampEnum != null && lastTimestampEnum.hasMoreElements()) {
            lastTimestamp = Long.valueOf(lastTimestampEnum.nextElement());
        }
        return lastTimestamp;
    }

    /**
	 * 
	 * @param request
	 * @return
	 */
    private boolean isForwardRequest(HttpServletRequest request) {
        return request.getParameter("url") != null;
    }

    private void doForwardRequest(HttpServletRequest request, HttpServletResponse response) {
        String urlstr = request.getParameter("url");
        AudioInputStream ais = getWavFromURL(urlstr);
        getRelay(request).play(ais);
    }

    public static AudioInputStream getWavFromURL(String urlstr) {
        URL url;
        AudioInputStream ais = null;
        try {
            url = new URL(urlstr);
            URLConnection c = url.openConnection();
            c.connect();
            InputStream stream = c.getInputStream();
            ais = new AudioInputStream(stream, playFormat, AudioSystem.NOT_SPECIFIED);
            LOG.debug("[getWavFromURL]Getting audio from URL: {}", url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ais;
    }

    /**
	 * 
	 * @param request
	 * @return
	 */
    public static String reconstructRequestURLandParams(HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        String params = "";
        for (Object key : request.getParameterMap().keySet()) {
            String paramName = (String) key;
            String paramValue = request.getParameter(paramName);
            params += "&" + paramName + "=" + paramValue;
        }
        if (!"".equals(params)) {
            params = params.replaceFirst("&", "?");
            url += params;
        }
        return url;
    }

    /**
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
    private void doPollRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String id = request.getParameter("sessionid");
            if (id == null) {
                id = request.getSession().getId();
            }
            int playPollTimeout = 1;
            InputStream in = getRelay(request).waitForAudio(playPollTimeout);
            if (in != null) {
                response.setContentType("audio/wav");
                OutputStream out = response.getOutputStream();
                ServletUtils.sendStream(in, out);
            } else {
                response.setContentType("text/xml");
                response.getOutputStream().close();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private SpntRelay getRelay(HttpServletRequest request) {
        return (SpntRelay) WamiUtil.getRelay(request);
    }

    /**
	 * 
	 * @param request
	 * @return
	 */
    private boolean isPollRequest(HttpServletRequest request) {
        return request.getParameter("poll") != null && Boolean.parseBoolean(request.getParameter("poll"));
    }
}
