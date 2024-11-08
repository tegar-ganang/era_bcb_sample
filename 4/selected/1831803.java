package org.anuta.xmltv.grabber.tvgidsnl;

import java.text.SimpleDateFormat;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.anuta.xmltv.XMLTVGrabberTask;
import org.anuta.xmltv.beans.Channel;
import org.anuta.xmltv.beans.Program;
import org.anuta.xmltv.beans.Rating;
import org.anuta.xmltv.beans.RatingMapper;
import org.anuta.xmltv.exceptions.TransportException;
import org.anuta.xmltv.grabber.tvgidsnl.json.JsonProgram;
import org.anuta.xmltv.transport.HTTPTransport;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class TVGidsJsonGrabber extends AbstractGrabber {

    private static final Log log = LogFactory.getLog(TVGidsJsonGrabber.class);

    private static final String JSON_DATE_PATTERN = "yyyy-MM-dd hh:mm:ss";

    public List<Program> getPrograms(Channel channel, Date date, int day) throws TransportException {
        String s = getTransport().getText(buildListUrl(channel, day));
        Type t1 = new TypeToken<Map<String, List<JsonProgram>>>() {
        }.getType();
        Type t2 = new TypeToken<Map<String, Map<String, JsonProgram>>>() {
        }.getType();
        Collection<JsonProgram> jsonPrograms;
        try {
            s = StringEscapeUtils.unescapeJavaScript(s);
            Gson gson = new Gson();
            try {
                jsonPrograms = ((Map<String, List<JsonProgram>>) gson.fromJson(s, t1)).get(channel.getChannelId());
                log.debug("Succesfully parsed json using strat 1.");
            } catch (JsonParseException e) {
                log.debug("Parsing json using strat 1 fails, falling back to strat 2.");
                jsonPrograms = ((Map<String, Map<String, JsonProgram>>) gson.fromJson(s, t2)).get(channel.getChannelId()).values();
            }
        } catch (JsonParseException e) {
            log.error("Error parsing json:\n" + s, e);
            throw new TransportException("Could not parse data.");
        }
        List<Program> programs = new ArrayList<Program>();
        for (JsonProgram jsonProgram : jsonPrograms) {
            Program p = new Program();
            p.setChannelId(channel.getChannelId());
            StringBuilder sb = new StringBuilder(getTvgidsurl());
            sb.append("/");
            if (jsonProgram.getArtikel_id() != null) {
                sb.append("artikel/");
                sb.append(jsonProgram.getArtikel_id());
            } else {
                sb.append("programma/");
                sb.append(jsonProgram.getDb_id());
            }
            sb.append("/");
            p.setUrl(sb.toString());
            p.setFullyLoaded(false);
            p.setTitle(jsonProgram.getTitel());
            SimpleDateFormat sdf = new SimpleDateFormat(JSON_DATE_PATTERN);
            try {
                p.setStartDate(sdf.parse(jsonProgram.getDatum_start()));
                p.setEndDate(sdf.parse(jsonProgram.getDatum_end()));
            } catch (ParseException e) {
                throw new RuntimeException(jsonProgram.getDatum_start() + " or " + jsonProgram.getDatum_end() + " not valid.");
            }
            p.setGanre(getMappedGanre(jsonProgram.getGenre()));
            if (jsonProgram.getKijkwijzer() != null) {
                Rating r = new Rating();
                r.setValue(jsonProgram.getKijkwijzer());
                p.getRating().add(r);
            }
            programs.add(p);
        }
        fixProgramTimes(programs);
        return programs;
    }

    private Object escape(String titel) {
        return StringUtils.replaceEachRepeatedly(titel, new String[] { " ", "&" }, new String[] { "_", "%24" });
    }

    public Program getProgram(Program p) throws TransportException {
        log.debug(p.getUrl());
        String s = getTransport().getText(p.getUrl());
        log.debug(s);
        grabFromDetailPage(s, p);
        return p;
    }

    public static void main(final String[] args) throws TransportException {
        AbstractGrabber grabber = new TVGidsJsonGrabber();
        HTTPTransport transport = new HTTPTransport();
        transport.setEncoding("ISO8859_1");
        grabber.setTransport(transport);
        grabber.setGanreMapping(new HashMap());
        grabber.setNoData("NODATA");
        grabber.setRatingMapper(new RatingMapper());
        grabber.setRoleMapping(new HashMap());
        Channel c0 = new Channel();
        c0.setChannelId("1");
        c0.setGrabber(grabber);
        System.out.println(grabber.getPrograms(c0, new Date(), 0));
    }
}
