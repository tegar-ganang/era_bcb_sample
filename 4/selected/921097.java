package net.sf.jvdr.http.servlet.epg;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.exlp.util.DateUtil;
import net.sf.jvdr.cache.VdrCache;
import net.sf.jvdr.cache.VdrDataFetcherSvdrp;
import net.sf.jvdr.data.ejb.VdrConfigTimer;
import net.sf.jvdr.data.ejb.VdrUser;
import net.sf.jvdr.data.facade.VdrPersistence;
import net.sf.jwan.servlet.exception.WanRenderException;
import net.sf.jwan.servlet.gui.elements.HtmlHref;
import net.sf.jwan.servlet.gui.elements.WanDiv;
import net.sf.jwan.servlet.gui.elements.WanParagraph;
import net.sf.jwan.servlet.gui.form.WanForm;
import net.sf.jwan.servlet.gui.form.WanFormFieldSet;
import net.sf.jwan.servlet.gui.form.WanFormInputCheckBox;
import net.sf.jwan.servlet.gui.form.WanFormInputHidden;
import net.sf.jwan.servlet.gui.form.WanFormSelect;
import net.sf.jwan.servlet.gui.form.WanFormSelectItem;
import net.sf.jwan.servlet.gui.layer.AbstractWanServletLayer;
import net.sf.jwan.servlet.gui.layer.WanLayer;
import net.sf.jwan.servlet.gui.renderable.WanRenderable;
import net.sf.jwan.servlet.util.ServletForm;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hampelratte.svdrp.commands.DELT;
import org.hampelratte.svdrp.commands.MODT;
import org.hampelratte.svdrp.commands.NEWT;
import org.hampelratte.svdrp.responses.highlevel.EPGEntry;
import org.hampelratte.svdrp.responses.highlevel.VDRTimer;

public class EpgTimerServlet extends AbstractWanServletLayer {

    static Log logger = LogFactory.getLog(EpgTimerServlet.class);

    public static final long serialVersionUID = 1;

    private Configuration config;

    private WanLayer lyrTimer;

    public EpgTimerServlet(Configuration config) {
        super("lEpgTimer");
        this.config = config;
        layerTitle = "Timer";
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
        ServletForm form = new ServletForm(request);
        VdrCache vdrC = (VdrCache) getServletContext().getAttribute(VdrCache.class.getSimpleName());
        VdrPersistence vdrP = (VdrPersistence) getServletContext().getAttribute(VdrPersistence.class.getSimpleName());
        VdrUser vu = (VdrUser) request.getSession().getAttribute(VdrUser.class.getSimpleName());
        VdrConfigTimer vcr = vdrP.fVdrConfigTimer(vu);
        WanDiv wd = new WanDiv();
        wd.setDivclass(WanDiv.DivClass.NONE);
        if (form.isAvailable("tid")) {
            wd.addContent(createFormTimer(form.getInt("tid"), vdrC));
        } else {
            wd.addContent(createFormFromEpg(request, form, vdrC, vcr));
        }
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

    public WanRenderable createFormTimer(int tid, VdrCache vdrC) {
        VDRTimer timer = vdrC.getTimer(tid);
        WanForm wf = new WanForm();
        wf.setRightButtonTitel("Save");
        wf.setAction(layerServletPath + "/" + getLayerId());
        WanFormFieldSet wffs = new WanFormFieldSet();
        wf.addFieldSet(wffs);
        WanFormInputCheckBox wficb = new WanFormInputCheckBox("L�sche Timer");
        wficb.setName("deltimer");
        wffs.addInput(wficb);
        wffs.addInput(new WanFormInputHidden("tid", timer.getID()));
        int steps[] = { -25, -10, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 10, 25 };
        WanFormSelect wfsBefore = new WanFormSelect("Beginn: " + DateUtil.sm(timer.getStartTime()), "before");
        for (int value : steps) {
            WanFormSelectItem wfsi = new WanFormSelectItem(value + " min", value + "");
            if (value == 0) {
                wfsi.setSelected(true);
            }
            wfsBefore.addItem(wfsi);
        }
        wffs.addInput(wfsBefore);
        WanFormSelect wfsAfter = new WanFormSelect("Ende: " + DateUtil.sm(timer.getEndTime()), "after");
        for (int value : steps) {
            WanFormSelectItem wfsi = new WanFormSelectItem(value + " min", value + "");
            if (value == 0) {
                wfsi.setSelected(true);
            }
            wfsAfter.addItem(wfsi);
        }
        wffs.addInput(wfsAfter);
        StringBuffer sbTitel = new StringBuffer();
        sbTitel.append(vdrC.getChName(timer.getChannelNumber()) + "<br/>");
        sbTitel.append("<b>" + timer.getTitle() + "</b><br/>");
        sbTitel.append(DateUtil.dayName(timer.getStartTime()) + ", " + DateUtil.tm(timer.getStartTime()));
        WanParagraph wp = new WanParagraph(sbTitel.toString());
        WanDiv div = new WanDiv();
        div.setDivclass(WanDiv.DivClass.iBlock);
        div.addContent(wp);
        wf.addDiv(div, WanDiv.Orientation.TOP);
        return wf;
    }

    public WanRenderable createFormFromEpg(HttpServletRequest request, ServletForm form, VdrCache vdrC, VdrConfigTimer vcr) {
        String chNu = form.get("chNu");
        String st = form.get("st");
        EPGEntry epg = vdrC.getEpg(chNu, st);
        WanForm wf = new WanForm();
        wf.setRightButtonTitel("Save");
        wf.setAction(layerServletPath + "/" + getLayerId());
        WanFormFieldSet wffs = new WanFormFieldSet();
        wf.addFieldSet(wffs);
        WanFormSelect wfsBefore = new WanFormSelect("Beginn: " + DateUtil.sm(epg.getStartTime()), "before");
        int defaultBefore;
        if (vcr != null && vcr.isOwn()) {
            defaultBefore = vcr.getTimeBefore();
        } else {
            defaultBefore = config.getInt("vdr/timer/@minBefore");
        }
        boolean beforeSet = false;
        for (int i = 0; i < 10; i++) {
            int value = i * 5;
            WanFormSelectItem wfsi = new WanFormSelectItem("-" + value + " min", value + "");
            if (value == defaultBefore) {
                wfsi.setSelected(true);
                beforeSet = true;
            }
            wfsBefore.addItem(wfsi);
        }
        if (!beforeSet) {
            WanFormSelectItem wfsi = new WanFormSelectItem("- " + defaultBefore, defaultBefore + "");
            wfsi.setSelected(true);
            wfsBefore.addItem(wfsi);
        }
        wffs.addInput(wfsBefore);
        wffs.addInput(new WanFormInputHidden("chNu", chNu));
        wffs.addInput(new WanFormInputHidden("st", st));
        WanFormSelect wfsAfter = new WanFormSelect("Ende: " + DateUtil.sm(epg.getEndTime()), "after");
        int defaultAfter;
        if (vcr != null && vcr.isOwn()) {
            defaultAfter = vcr.getTimeAfter();
        } else {
            defaultAfter = config.getInt("vdr/timer/@minAfter");
        }
        boolean afterSet = false;
        for (int i = 0; i < 10; i++) {
            int value = i * 5;
            WanFormSelectItem wfsi = new WanFormSelectItem("+" + value + " min", value + "");
            if (value == defaultAfter) {
                wfsi.setSelected(true);
                afterSet = true;
            }
            wfsAfter.addItem(wfsi);
        }
        if (!afterSet) {
            WanFormSelectItem wfsi = new WanFormSelectItem("+" + defaultAfter + " min", defaultAfter + "");
            wfsi.setSelected(true);
            wfsAfter.addItem(wfsi);
        }
        wffs.addInput(wfsAfter);
        StringBuffer sbTitel = new StringBuffer();
        sbTitel.append(epg.getChannelName() + "<br/>");
        sbTitel.append("<b>" + epg.getTitle() + "</b><br/>");
        sbTitel.append(DateUtil.dayName(epg.getStartTime()) + ", " + DateUtil.tm(epg.getStartTime()));
        WanParagraph wp = new WanParagraph(sbTitel.toString());
        WanDiv div = new WanDiv();
        div.setDivclass(WanDiv.DivClass.iBlock);
        div.addContent(wp);
        wf.addDiv(div, WanDiv.Orientation.TOP);
        return wf;
    }

    public void processSubmittedForm(HttpServletRequest request) {
        VdrCache vdrC = (VdrCache) getServletContext().getAttribute(VdrCache.class.getSimpleName());
        VdrPersistence vdrP = (VdrPersistence) getServletContext().getAttribute(VdrPersistence.class.getSimpleName());
        VdrUser vu = (VdrUser) request.getSession().getAttribute(VdrUser.class.getSimpleName());
        ServletForm form = new ServletForm(request);
        VDRTimer vdrTimer;
        WanParagraph wpResult = new WanParagraph();
        if (form.isAvailable("tid")) {
            VdrDataFetcherSvdrp vdrD = new VdrDataFetcherSvdrp(config);
            vdrTimer = vdrC.getTimer(form.getInt("tid"));
            if (form.getBoolean("deltimer", false)) {
                logger.debug("Timer will be deleted.");
                wpResult.setContent("Timer deleted");
                try {
                    vdrD.connect();
                    vdrD.getSvdrp().send(new DELT(vdrTimer));
                    vdrC.fetchTimer(vdrD);
                } catch (IOException e) {
                    wpResult.setContent("Fehler beim L�schen des Timers");
                    logger.error(e);
                } finally {
                    vdrD.disconnect();
                }
            } else {
                logger.debug("Timer will be modified.");
                wpResult.setContent("Timer modified");
                int before = new Integer(form.get("before"));
                int after = new Integer(form.get("after"));
                Calendar recStart = vdrTimer.getStartTime();
                recStart.add(Calendar.MINUTE, before);
                Calendar recStop = vdrTimer.getEndTime();
                recStop.add(Calendar.MINUTE, after);
                vdrTimer.setStartTime(recStart);
                vdrTimer.setEndTime(recStop);
                try {
                    vdrD.connect();
                    vdrD.getSvdrp().send(new MODT(vdrTimer.getID(), vdrTimer));
                    vdrC.fetchTimer(vdrD);
                } catch (IOException e) {
                    wpResult.setContent("Fehler beim Modifizieren des Timers");
                    logger.error(e);
                } finally {
                    vdrD.disconnect();
                }
            }
        } else {
            String chNu = form.get("chNu");
            String st = form.get("st");
            int before = new Integer(form.get("before"));
            int after = new Integer(form.get("after"));
            logger.trace("Form submitted: chNu=" + chNu + " st=" + st + " before=" + before + " after=" + after);
            EPGEntry epg = vdrC.getEpg(chNu, st);
            Calendar recStart = epg.getStartTime();
            recStart.add(Calendar.MINUTE, -before);
            Calendar recStop = epg.getEndTime();
            recStop.add(Calendar.MINUTE, after);
            vdrTimer = new VDRTimer();
            vdrTimer.setChannelNumber(new Integer(chNu));
            vdrTimer.setStartTime(recStart);
            vdrTimer.setEndTime(recStop);
            vdrTimer.setPriority(99);
            vdrTimer.setLifetime(99);
            vdrTimer.setTitle(epg.getTitle());
            vdrTimer.setDescription(epg.getDescription());
            NEWT nt = new NEWT(vdrTimer);
            wpResult.setContent("Timer gespeichert.");
            logger.trace("NEWT: " + nt.getCommand());
            VdrDataFetcherSvdrp vdrD = new VdrDataFetcherSvdrp(config);
            try {
                vdrD.connect();
                vdrD.getSvdrp().send(nt);
                vdrC.fetchTimer(vdrD);
            } catch (IOException e) {
                wpResult.setContent("Fehler beim Anlegen des Timers");
                logger.error(e);
            } finally {
                vdrD.disconnect();
            }
        }
        HtmlHref refTimer = lyrTimer.getLayerTarget();
        refTimer.setContent("Timer�bersicht");
        refTimer.setRev(HtmlHref.Rev.async);
        HtmlHref refMain = new HtmlHref("Hauptmen�", "");
        WanDiv div = new WanDiv();
        div.setDivclass(WanDiv.DivClass.iBlock);
        div.addContent(wpResult);
        div.addContent(new WanParagraph("Weiter zum " + refMain.render() + " oder " + refTimer.render() + "."));
        alWanRenderables.add(div);
    }

    public void setLyrTimer(WanLayer lyrTimer) {
        this.lyrTimer = lyrTimer;
    }
}
