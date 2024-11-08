package net.sf.jvdr.http.servlet.settings;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.jvdr.cache.VdrCache;
import net.sf.jvdr.data.ejb.VdrConfigShowChannel;
import net.sf.jvdr.data.ejb.VdrConfigShowChannels;
import net.sf.jvdr.data.ejb.VdrUser;
import net.sf.jvdr.data.facade.VdrPersistence;
import net.sf.jvdr.util.JvdrTranslation;
import net.sf.jwan.servlet.exception.WanRenderException;
import net.sf.jwan.servlet.gui.elements.WanDiv;
import net.sf.jwan.servlet.gui.elements.WanParagraph;
import net.sf.jwan.servlet.gui.form.WanForm;
import net.sf.jwan.servlet.gui.form.WanFormFieldSet;
import net.sf.jwan.servlet.gui.form.WanFormInputCheckBox;
import net.sf.jwan.servlet.gui.layer.AbstractWanServletLayer;
import net.sf.jwan.servlet.gui.renderable.WanRenderable;
import net.sf.jwan.servlet.util.ServletForm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hampelratte.svdrp.responses.highlevel.Channel;

public class SettingsShowChannelsServlet extends AbstractWanServletLayer {

    static Log logger = LogFactory.getLog(SettingsShowChannelsServlet.class);

    public static final long serialVersionUID = 1;

    public SettingsShowChannelsServlet() {
        super("lCnfShC");
        layerTitle = JvdrTranslation.get("settings", "settings");
        layerServletPath = "async";
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        alWanRenderables.clear();
        response.setContentType("text/xml");
        response.setStatus(HttpServletResponse.SC_OK);
        processSubmittedForm(request);
        PrintWriter out = response.getWriter();
        try {
            out.println(renderAsync());
        } catch (WanRenderException e) {
            logger.error(e);
        } finally {
            out.close();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        alWanRenderables.clear();
        response.setContentType("text/xml");
        response.setStatus(HttpServletResponse.SC_OK);
        WanDiv wd = new WanDiv();
        wd.setDivclass(WanDiv.DivClass.NONE);
        wd.addContent(createFrom(request));
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

    public WanRenderable createFrom(HttpServletRequest request) {
        VdrPersistence vdrP = (VdrPersistence) getServletContext().getAttribute(VdrPersistence.class.getSimpleName());
        VdrCache vdrC = (VdrCache) getServletContext().getAttribute(VdrCache.class.getSimpleName());
        VdrUser vu = (VdrUser) request.getSession().getAttribute(VdrUser.class.getSimpleName());
        VdrConfigShowChannels vcsc = vdrP.fcVdrConfigShowChannels(vu);
        if (vcsc.getVcsc() != null) {
            logger.debug(vcsc.getVcsc().size() + "Einträge");
        } else {
            logger.debug("vcsc==null");
        }
        WanForm wf = new WanForm();
        wf.setRightButtonTitel("Save");
        wf.setAction(layerServletPath + "/" + getLayerId());
        WanFormFieldSet wffs = new WanFormFieldSet(JvdrTranslation.get("settings", "showchannel"));
        wf.addFieldSet(wffs);
        for (Channel c : vdrC.getChannelList()) {
            WanFormInputCheckBox box = new WanFormInputCheckBox(c.getChannelNumber() + ": " + c.getName());
            box.setName("c" + c.getChannelNumber());
            box.setId("id" + c.getChannelNumber());
            box.setChecked(vcsc.showChannel(c.getChannelNumber(), true));
            wffs.addInput(box);
        }
        return wf;
    }

    public void processSubmittedForm(HttpServletRequest request) {
        VdrPersistence vdrP = (VdrPersistence) getServletContext().getAttribute(VdrPersistence.class.getSimpleName());
        VdrCache vdrC = (VdrCache) getServletContext().getAttribute(VdrCache.class.getSimpleName());
        VdrUser vu = (VdrUser) request.getSession().getAttribute(VdrUser.class.getSimpleName());
        VdrConfigShowChannels vcsc = vdrP.fcVdrConfigShowChannels(vu);
        ServletForm form = new ServletForm(request);
        for (Channel c : vdrC.getChannelList()) {
            int chnu = c.getChannelNumber();
            boolean show = form.getBoolean("c" + chnu, false);
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
        div.addContent(new WanParagraph("Einstellungen der angezeigten Kanäle gespeichert"));
        alWanRenderables.add(div);
    }
}
