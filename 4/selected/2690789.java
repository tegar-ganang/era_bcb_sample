package net.sf.jvdr.http.servlet.epg;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hampelratte.svdrp.responses.highlevel.Channel;

public class ChannelOverviewServlet extends AbstractWanServletLayer {

    static Log logger = LogFactory.getLog(ChannelOverviewServlet.class);

    public static final long serialVersionUID = 1;

    private WanLayer lyrChannelEpg;

    public ChannelOverviewServlet() {
        super("lTvC");
        layerTitle = JvdrTranslation.get("epg", "channels");
        layerServletPath = "async";
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/xml");
        response.setStatus(HttpServletResponse.SC_OK);
        WanDiv wd = new WanDiv();
        wd.setDivclass(WanDiv.DivClass.iMenu);
        wd.addContent(createChannelOverview(request));
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

    public WanRenderable createChannelOverview(HttpServletRequest request) {
        alWanRenderables.clear();
        VdrPersistence vdrP = (VdrPersistence) this.getServletContext().getAttribute(VdrPersistence.class.getSimpleName());
        VdrCache vdrC = (VdrCache) this.getServletContext().getAttribute(VdrCache.class.getSimpleName());
        VdrUser vu = (VdrUser) request.getSession().getAttribute(VdrUser.class.getSimpleName());
        VdrConfigShowChannels vcsc = vdrP.fcVdrConfigShowChannels(vu);
        List<Channel> lC = vdrC.getChannelList();
        WanMenu wm = new WanMenu();
        wm.setMenuType(WanMenu.MenuType.SIMPLE);
        for (Channel ch : lC) {
            int chnu = ch.getChannelNumber();
            if (vcsc.showChannel(chnu, true)) {
                WanMenuEntry wi = new WanMenuEntry();
                wi.setName(ch.getName());
                HtmlHref href = lyrChannelEpg.getLayerTarget();
                href.setRev(HtmlHref.Rev.async);
                href.addHtPa("chNu", chnu);
                href.addHtPa("first", true);
                wi.setHtmlref(href);
                wm.addItem(wi);
            }
        }
        return wm;
    }

    public void setLyrChannelEpg(WanLayer lyrChannelEpg) {
        this.lyrChannelEpg = lyrChannelEpg;
    }
}
