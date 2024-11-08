package net.sf.jvdr.http.servlet.epg;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.exlp.util.DateUtil;
import net.sf.jvdr.cache.VdrCache;
import net.sf.jvdr.data.ejb.VdrConfigShowChannels;
import net.sf.jvdr.data.ejb.VdrUser;
import net.sf.jvdr.data.facade.VdrPersistence;
import net.sf.jvdr.util.JvdrTranslation;
import net.sf.jwan.servlet.exception.WanRenderException;
import net.sf.jwan.servlet.gui.elements.HtmlHref;
import net.sf.jwan.servlet.gui.elements.WanDiv;
import net.sf.jwan.servlet.gui.layer.AbstractWanServletLayer;
import net.sf.jwan.servlet.gui.layer.WanLayer;
import net.sf.jwan.servlet.gui.menu.WanMenu;
import net.sf.jwan.servlet.gui.menu.WanMenuEntry;
import net.sf.jwan.servlet.gui.renderable.WanRenderable;
import net.sf.jwan.servlet.util.ServletForm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hampelratte.svdrp.responses.highlevel.Channel;
import org.hampelratte.svdrp.responses.highlevel.EPGEntry;

public class ProgramServlet extends AbstractWanServletLayer {

    static Log logger = LogFactory.getLog(ProgramServlet.class);

    public static final long serialVersionUID = 1;

    private List<Pattern> pattern;

    private WanLayer lyrEpgDetail;

    public ProgramServlet() {
        super("lProgram");
        layerTitle = JvdrTranslation.get("epg", "playing");
        layerServletPath = "async";
    }

    public void init() throws ServletException {
        pattern = new ArrayList<Pattern>();
        pattern.add(Pattern.compile("NOW"));
        pattern.add(Pattern.compile("NEXT"));
        pattern.add(Pattern.compile("([\\d]+):([\\d]+)"));
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        alWanRenderables.clear();
        response.setContentType("text/xml");
        response.setStatus(HttpServletResponse.SC_OK);
        WanDiv wd = new WanDiv();
        wd.setDivclass(WanDiv.DivClass.iMenu);
        wd.addContent(createEpgForTime(request));
        alWanRenderables.add(wd);
        PrintWriter out = response.getWriter();
        try {
            out.println(renderAsync());
        } catch (WanRenderException e) {
            logger.error(e);
        } finally {
            out.close();
        }
    }

    public WanRenderable createEpgForTime(HttpServletRequest request) {
        VdrPersistence vdrP = (VdrPersistence) getServletContext().getAttribute(VdrPersistence.class.getSimpleName());
        VdrCache vdrC = (VdrCache) getServletContext().getAttribute(VdrCache.class.getSimpleName());
        VdrUser vu = (VdrUser) request.getSession().getAttribute(VdrUser.class.getSimpleName());
        ServletForm form = new ServletForm(request);
        List<EPGEntry> lEPG = fetchData(vdrP, vdrC, vu, form.get("time"));
        WanMenu wm = new WanMenu();
        wm.setMenuType(WanMenu.MenuType.IMAGE);
        logger.trace("WanMenu (" + wm.getMenuType() + ") created, population with " + lEPG.size() + " entries");
        for (EPGEntry e : lEPG) {
            WanMenuEntry wmi = new WanMenuEntry();
            wmi.setName(e.getTitle());
            HtmlHref href = new HtmlHref();
            if (form.get("time").equals("NOW")) {
                href.setTargetLink("async/switch");
                href.addHtPa("chid", e.getChannelID());
            } else {
                href = lyrEpgDetail.getLayerTarget();
                href.addHtPa("chNu", vdrC.getChNum(e.getChannelID()));
                href.addHtPa("st", e.getStartTime().getTimeInMillis());
            }
            href.setRev(HtmlHref.Rev.async);
            wmi.setHtmlref(href);
            wmi.setFooter(e.getChannelName());
            wmi.setHeader(DateUtil.sm(e.getStartTime()) + " - " + DateUtil.sm(e.getEndTime()));
            wm.addItem(wmi);
        }
        return wm;
    }

    private List<EPGEntry> fetchData(VdrPersistence vdrP, VdrCache vdrC, VdrUser vu, String time) {
        boolean printUnknwonPattern = true;
        List<EPGEntry> lEPG = new ArrayList<EPGEntry>();
        List<Channel> lC = vdrC.getChannelList();
        VdrConfigShowChannels vcsc = vdrP.fcVdrConfigShowChannels(vu);
        for (Channel c : lC) {
            int chnu = c.getChannelNumber();
            if (vcsc.showChannel(chnu, true)) {
                EPGEntry epg = null;
                boolean unknownPattern = true;
                for (int i = 0; i < pattern.size(); i++) {
                    Matcher m = pattern.get(i).matcher(time);
                    if (m.matches()) {
                        switch(i) {
                            case 0:
                                epg = vdrC.getEpgforTime(c.getChannelNumber(), new Date(), false);
                                break;
                            case 1:
                                epg = vdrC.getEpgforTime(c.getChannelNumber(), new Date(), true);
                                break;
                            case 2:
                                epg = getEpg(m, vdrC, c.getChannelNumber());
                                break;
                        }
                        unknownPattern = false;
                    }
                }
                if (unknownPattern && printUnknwonPattern) {
                    printUnknwonPattern = false;
                    logger.warn("Point in Time " + time + " not in correct format (HH:MM)");
                }
                if (epg != null) {
                    lEPG.add(epg);
                }
            }
        }
        return lEPG;
    }

    private EPGEntry getEpg(Matcher m, VdrCache vdrC, int chNu) {
        EPGEntry epg = null;
        GregorianCalendar gcRef = new GregorianCalendar();
        gcRef.setTime(new Date());
        GregorianCalendar gc = new GregorianCalendar();
        gc.set(GregorianCalendar.YEAR, gcRef.get(GregorianCalendar.YEAR));
        gc.set(GregorianCalendar.MONTH, gcRef.get(GregorianCalendar.MONTH));
        gc.set(GregorianCalendar.DAY_OF_MONTH, gcRef.get(GregorianCalendar.DAY_OF_MONTH));
        gc.set(GregorianCalendar.HOUR_OF_DAY, new Integer(m.group(1)));
        gc.set(GregorianCalendar.MINUTE, new Integer(m.group(2)));
        gc.set(GregorianCalendar.SECOND, 0);
        gc.set(GregorianCalendar.MILLISECOND, 0);
        epg = vdrC.getEpgforTime(chNu, new Date(gc.getTimeInMillis()), false);
        return epg;
    }

    public void setLyrEpgDetail(WanLayer lyrEpgDetail) {
        this.lyrEpgDetail = lyrEpgDetail;
    }
}
