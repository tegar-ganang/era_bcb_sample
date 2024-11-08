package net.sf.jvdr.http.servlet.media;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.exlp.util.DateUtil;
import net.sf.jvdr.cache.VdrCache;
import net.sf.jvdr.cache.VdrDataFetcherSvdrp;
import net.sf.jvdr.util.JvdrTranslation;
import net.sf.jwan.servlet.exception.WanRenderException;
import net.sf.jwan.servlet.gui.elements.HtmlHref;
import net.sf.jwan.servlet.gui.elements.WanDiv;
import net.sf.jwan.servlet.gui.elements.WanParagraph;
import net.sf.jwan.servlet.gui.layer.AbstractWanServletLayer;
import net.sf.jwan.servlet.gui.layer.WanLayer;
import net.sf.jwan.servlet.gui.menu.WanMenu;
import net.sf.jwan.servlet.gui.menu.WanMenuEntry;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hampelratte.svdrp.responses.highlevel.VDRTimer;

public class TimerOverviewServlet extends AbstractWanServletLayer {

    static Log logger = LogFactory.getLog(TimerOverviewServlet.class);

    public static final long serialVersionUID = 1;

    private Configuration config;

    private WanLayer lyrEpgTimer;

    public TimerOverviewServlet(Configuration config) {
        super("lTiO");
        this.config = config;
        layerTitle = JvdrTranslation.get("timer", "overview");
        layerServletPath = "async";
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/xml");
        response.setStatus(HttpServletResponse.SC_OK);
        alWanRenderables.clear();
        createTimerOverview();
        PrintWriter out = response.getWriter();
        try {
            out.println(renderAsync());
        } catch (WanRenderException e) {
            logger.error(e);
        } finally {
            out.close();
        }
    }

    public void createTimerOverview() {
        VdrCache vdrC = (VdrCache) this.getServletContext().getAttribute(VdrCache.class.getSimpleName());
        Configuration config = (Configuration) this.getServletContext().getAttribute(Configuration.class.getSimpleName());
        VdrDataFetcherSvdrp vdrD = new VdrDataFetcherSvdrp(config);
        vdrC.fetchTimer(vdrD);
        List<VDRTimer> lTimer = vdrC.getLTimer();
        if (lTimer.size() > 0) {
            Date d = new Date();
            WanDiv wd = new WanDiv();
            wd.setDivclass(WanDiv.DivClass.iBlock);
            String style = "float:right;margin:0 5px";
            style = "";
            WanParagraph wp = new WanParagraph();
            wp.setContent("<img src=\"async/timerchart?" + d.getTime() + "\" style=\"" + style + "\" />");
            wd.addContent(wp);
            alWanRenderables.add(wd);
            WanMenu wm = new WanMenu();
            wm.setMenuType(WanMenu.MenuType.IMAGE);
            for (VDRTimer timer : lTimer) {
                StringBuffer sbHeader = new StringBuffer();
                sbHeader.append(DateUtil.dayName(timer.getStartTime()));
                sbHeader.append(", " + DateUtil.tmj(timer.getStartTime()));
                sbHeader.append(", " + DateUtil.sm(timer.getStartTime()));
                sbHeader.append(" - " + DateUtil.sm(timer.getEndTime()));
                HtmlHref href = lyrEpgTimer.getLayerTarget();
                href.setRev(HtmlHref.Rev.async);
                href.addHtPa("tid", timer.getID());
                WanMenuEntry wmi = new WanMenuEntry();
                wmi.setName(timer.getTitle());
                wmi.setHtmlref(href);
                wmi.setHeader(sbHeader.toString());
                wmi.setFooter(vdrC.getChannel(timer.getChannelNumber()).getName());
                wm.addItem(wmi);
            }
            wd = new WanDiv();
            wd.setDivclass(WanDiv.DivClass.iMenu);
            wd.addContent(wm);
            alWanRenderables.add(wd);
        } else {
            WanDiv wd = new WanDiv();
            wd.setDivclass(WanDiv.DivClass.iBlock);
            WanParagraph wp = new WanParagraph();
            wp.setContent("Keine Timer definiert!");
            wd.addContent(wp);
            alWanRenderables.add(wd);
        }
    }

    public void setLyrEpgTimer(WanLayer lyrEpgTimer) {
        this.lyrEpgTimer = lyrEpgTimer;
    }
}
