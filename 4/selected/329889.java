package net.sf.jvdr.http.servlet.epg;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.NoResultException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.exlp.util.DateUtil;
import net.sf.jvdr.cache.VdrCache;
import net.sf.jvdr.data.ejb.VdrConfigChannelProgram;
import net.sf.jvdr.data.ejb.VdrImageSearch;
import net.sf.jvdr.data.ejb.VdrThumbnail;
import net.sf.jvdr.data.ejb.VdrUser;
import net.sf.jvdr.data.facade.VdrPersistence;
import net.sf.jvdr.util.JvdrTranslation;
import net.sf.jwan.servlet.exception.WanRenderException;
import net.sf.jwan.servlet.gui.elements.HtmlHref;
import net.sf.jwan.servlet.gui.elements.HtmlImage;
import net.sf.jwan.servlet.gui.elements.WanDiv;
import net.sf.jwan.servlet.gui.layer.AbstractWanServletLayer;
import net.sf.jwan.servlet.gui.layer.WanLayer;
import net.sf.jwan.servlet.gui.menu.WanMenu;
import net.sf.jwan.servlet.gui.menu.WanMenuEntry;
import net.sf.jwan.servlet.gui.menu.WanMenuMore;
import net.sf.jwan.servlet.gui.renderable.WanRenderable;
import net.sf.jwan.servlet.util.AsyncHelper;
import net.sf.jwan.servlet.util.ServletForm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hampelratte.svdrp.responses.highlevel.EPGEntry;

public class EpgChannelServlet extends AbstractWanServletLayer {

    static Log logger = LogFactory.getLog(EpgChannelServlet.class);

    public static final long serialVersionUID = 1;

    private WanLayer lyrEpgDetail;

    public EpgChannelServlet() {
        super("lEpgC");
        layerTitle = JvdrTranslation.get("epg", "overview");
        layerServletPath = "async";
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        alWanRenderables.clear();
        response.setContentType("text/xml");
        response.setStatus(HttpServletResponse.SC_OK);
        createEpgChannel(request);
        PrintWriter out = response.getWriter();
        try {
            out.println(renderAsync());
        } catch (WanRenderException e) {
            logger.error(e);
        } finally {
            out.close();
        }
    }

    public void createEpgChannel(HttpServletRequest request) {
        List<WanRenderable> alMenuSections = new ArrayList<WanRenderable>();
        ServletForm form = new ServletForm(request);
        int chNu = form.getInt("chNu");
        boolean showFirst = form.getBoolean("first");
        VdrPersistence vdrP = (VdrPersistence) getServletContext().getAttribute(VdrPersistence.class.getSimpleName());
        VdrCache vdrC = (VdrCache) getServletContext().getAttribute(VdrCache.class.getSimpleName());
        VdrUser vu = (VdrUser) request.getSession().getAttribute(VdrUser.class.getSimpleName());
        VdrConfigChannelProgram vcc = vdrP.fVdrConfigChannelProgram(vu);
        List<EPGEntry> lEPG = vdrC.getEpgForChNu(chNu, new Date());
        int fromIndex = 0;
        int toIndex = lEPG.size();
        String oldDay = "";
        WanMenuMore wmm = null;
        if (vcc != null && vcc.isLimitEntries()) {
            logger.trace("Page is rendered at first time? " + showFirst);
            boolean limitReached = false;
            for (int index = 0; index < lEPG.size(); index++) {
                int linkRange = 0;
                if (showFirst) {
                    switch(vcc.getUnitShowFirst()) {
                        case Entries:
                            if (index == vcc.getShowFirst()) {
                                limitReached = true;
                                linkRange = index;
                            }
                            ;
                            break;
                    }
                } else {
                    oldDay = form.get("oldDay");
                    switch(vcc.getUnitShowMore()) {
                        case Entries:
                            fromIndex = new Integer(form.getInt("fEl"));
                            if (index == (fromIndex + vcc.getShowMore())) {
                                limitReached = true;
                                linkRange = index;
                            }
                            break;
                    }
                }
                if (limitReached) {
                    wmm = new WanMenuMore();
                    wmm.setId("wa" + getLayerId() + chNu);
                    HtmlHref href = this.getLayerTarget();
                    href.setTitle("Loading");
                    href.setContent("Mehr");
                    href.setRev(HtmlHref.Rev.async);
                    href.setTargetLayer(null);
                    href.addHtPa("chNu", chNu);
                    href.addHtPa("first", false);
                    href.addHtPa("fEl", linkRange);
                    wmm.setHref(href);
                    toIndex = index;
                    break;
                }
            }
        }
        logger.trace("Rendering from " + fromIndex + " to " + toIndex);
        ArrayList<WanRenderable> alWr = new ArrayList<WanRenderable>();
        WanMenu wm = null;
        int i = 0;
        if (showFirst) {
            wm = new WanMenu();
            wm.setMenuType(WanMenu.MenuType.IMAGE);
        }
        for (EPGEntry e : lEPG.subList(fromIndex, toIndex)) {
            String dayName = DateUtil.dayName(e.getStartTime());
            if (!dayName.equals(oldDay)) {
                oldDay = dayName;
                dayName = "<font color=\"red\">" + dayName + "</font>";
            }
            StringBuffer sbHeader = new StringBuffer();
            sbHeader.append(dayName + " " + DateUtil.tm(e.getStartTime()) + ": " + DateUtil.sm(e.getStartTime()) + " - " + DateUtil.sm(e.getEndTime()));
            HtmlHref href = lyrEpgDetail.getLayerTarget();
            href.setRev(HtmlHref.Rev.async);
            href.addHtPa("chNu", vdrC.getChNum(e.getChannelID()));
            href.addHtPa("st", e.getStartTime().getTimeInMillis());
            HtmlImage htmlImage = null;
            try {
                VdrImageSearch vis = vdrP.fVdrImageSearch(e.getTitle());
                if (vis.getShowThumbId() == 0) {
                    logger.warn("VdrImageSearchg.etShowThumbId()==0");
                    throw new NoResultException("VdrImageSearch");
                }
                VdrThumbnail vt = (VdrThumbnail) vdrP.findObject(VdrThumbnail.class, vis.getShowThumbId());
                htmlImage = new HtmlImage("async/lRecImage?tid=" + vt.getId());
                htmlImage.setImgClass(HtmlImage.ImgClass.CUSTOM);
                switch(vt.getRatio()) {
                    case a4z3:
                        htmlImage.setStyle("padding-top: 0px;");
                        htmlImage.setCustomClass("jvdr4z3");
                        break;
                    case a16z9:
                        htmlImage.setStyle("padding-top: 14px;");
                        htmlImage.setCustomClass("jvdr16z9");
                        break;
                }
            } catch (NoResultException nre) {
                htmlImage = new HtmlImage("resources/images/jvdr/blank.gif", HtmlImage.ImgClass.iFull);
            }
            WanMenuEntry wme = new WanMenuEntry();
            wme.setName(e.getTitle());
            wme.setHtmlImage(htmlImage);
            wme.setHtmlref(href);
            wme.setFooter(e.getChannelName());
            wme.setHeader(sbHeader.toString());
            wme.setMenuType(WanMenu.MenuType.IMAGE);
            if (showFirst) {
                wm.addItem(wme);
            } else {
                alWr.add(wme);
            }
        }
        if (wmm != null) {
            wmm.getHref().addHtPa("oldDay", oldDay);
            if (showFirst) {
                wm.addItem(wmm);
            } else {
                alWr.add(wmm);
            }
        }
        if (showFirst) {
            asyncMode = AsyncHelper.AsyncMode.replace;
            asyncZone = "wa" + getLayerId();
            alMenuSections.add(wm);
            WanDiv wd = new WanDiv();
            wd.setDivclass(WanDiv.DivClass.iMenu);
            wd.addContent(alMenuSections);
            alWanRenderables.add(wd);
        } else {
            asyncMode = AsyncHelper.AsyncMode.self;
            asyncZone = "wa" + getLayerId() + chNu;
            alMenuSections.addAll(alWr);
            alWanRenderables.addAll(alMenuSections);
        }
    }

    public void setLyrEpgDetail(WanLayer lyrEpgDetail) {
        this.lyrEpgDetail = lyrEpgDetail;
    }
}
