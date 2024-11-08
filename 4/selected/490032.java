package net.sf.jvdr.http.servlet.settings;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.jvdr.cache.VdrCache;
import net.sf.jvdr.data.ejb.VdrConfigShowChannel;
import net.sf.jvdr.data.ejb.VdrConfigShowChannels;
import net.sf.jvdr.data.ejb.VdrSmartSearch;
import net.sf.jvdr.data.ejb.VdrUser;
import net.sf.jvdr.data.facade.VdrPersistence;
import net.sf.jwan.servlet.exception.WanRenderException;
import net.sf.jwan.servlet.gui.elements.HtmlHref;
import net.sf.jwan.servlet.gui.elements.WanDiv;
import net.sf.jwan.servlet.gui.elements.WanParagraph;
import net.sf.jwan.servlet.gui.layer.AbstractWanServletLayer;
import net.sf.jwan.servlet.gui.layer.WanLayer;
import net.sf.jwan.servlet.gui.menu.WanMenu;
import net.sf.jwan.servlet.gui.menu.WanMenuEntry;
import net.sf.jwan.servlet.util.ServletForm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hampelratte.svdrp.responses.highlevel.Channel;

public class SettingsSmartEpgOverviewServlet extends AbstractWanServletLayer {

    static Log logger = LogFactory.getLog(SettingsSmartEpgOverviewServlet.class);

    public static final long serialVersionUID = 1;

    private WanLayer lyrSmartEpg;

    public SettingsSmartEpgOverviewServlet() {
        super("lCnfSmEpgO");
        layerTitle = "SmartEPG Verwaltung";
        layerServletPath = "async";
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        alWanRenderables.clear();
        response.setContentType("text/xml");
        response.setStatus(HttpServletResponse.SC_OK);
        createSmartEpgOverview(request);
        PrintWriter out = response.getWriter();
        try {
            out.println(renderAsync());
        } catch (WanRenderException e) {
            logger.error(e);
        } finally {
            out.close();
        }
    }

    public void createSmartEpgOverview(HttpServletRequest request) {
        VdrPersistence vdrP = (VdrPersistence) getServletContext().getAttribute(VdrPersistence.class.getSimpleName());
        VdrUser vu = (VdrUser) request.getSession().getAttribute(VdrUser.class.getSimpleName());
        List<VdrSmartSearch> lVss = vdrP.lVdrSmartSearch(vu);
        if (lVss != null && lVss.size() > 0) {
            WanMenu wm = new WanMenu();
            wm.setMenuType(WanMenu.MenuType.SIMPLE);
            for (VdrSmartSearch vss : lVss) {
                HtmlHref htmlref = lyrSmartEpg.getLayerTarget();
                htmlref.setRev(HtmlHref.Rev.async);
                htmlref.addHtPa("new", "false");
                htmlref.addHtPa("vssid", vss.getId());
                htmlref.addHtPa("new", false);
                WanMenuEntry wi = new WanMenuEntry();
                wi.setName(vss.getSuche());
                wi.setHtmlref(htmlref);
                if (vss.isAktiv()) {
                    wi.setHeader("Aktiv");
                } else {
                    wi.setHeader("Inaktiv");
                }
                wm.addItem(wi);
            }
            WanDiv wd = new WanDiv();
            wd.setDivclass(WanDiv.DivClass.iMenu);
            wd.addContent(wm);
            alWanRenderables.add(wd);
        } else {
            WanDiv div = new WanDiv();
            div.setDivclass(WanDiv.DivClass.iBlock);
            div.addContent(new WanParagraph("Keine Smart-Epg Einträge vorhanden"));
            alWanRenderables.add(div);
        }
    }

    public void processSubmittedForm(HttpServletRequest request) {
        VdrPersistence vdrP = (VdrPersistence) getServletContext().getAttribute(VdrPersistence.class.getSimpleName());
        VdrCache vdrC = (VdrCache) getServletContext().getAttribute(VdrCache.class.getSimpleName());
        VdrUser vu = (VdrUser) request.getSession().getAttribute(VdrUser.class.getSimpleName());
        VdrConfigShowChannels vcsc = vdrP.fcVdrConfigShowChannels(vu);
        ServletForm form = new ServletForm(request);
        for (Channel c : vdrC.getChannelList()) {
            int chnu = c.getChannelNumber();
            boolean show = form.getBoolean("c" + chnu);
            VdrConfigShowChannel v = vcsc.fVdrConfigShowChannel(chnu);
            if (v == null) {
                v = new VdrConfigShowChannel();
                v.setChnu(chnu);
                v.setVdrconfigchannel(vcsc);
                v.setShow(show);
                vcsc.add(v);
            } else {
                v.setShow(show);
            }
        }
        vcsc.setVcsc(vcsc.getVcsc());
        vdrP.updateObject(vcsc);
        WanDiv div = new WanDiv();
        div.setDivclass(WanDiv.DivClass.iBlock);
        div.addContent(new WanParagraph("Einstellungen der angezeigten Kan�le gespeichert"));
        alWanRenderables.add(div);
    }

    public void setLyrSmartEpg(WanLayer lyrSmartEpg) {
        this.lyrSmartEpg = lyrSmartEpg;
    }
}
