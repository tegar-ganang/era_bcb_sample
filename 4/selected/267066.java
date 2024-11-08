package net.sf.jvdr.http.servlet.epg;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.exlp.util.DateUtil;
import net.sf.jvdr.cache.VdrCache;
import net.sf.jvdr.data.ejb.VdrSmartSearch;
import net.sf.jvdr.data.ejb.VdrUser;
import net.sf.jvdr.data.facade.VdrPersistence;
import net.sf.jvdr.util.JvdrTranslation;
import net.sf.jwan.servlet.exception.WanRenderException;
import net.sf.jwan.servlet.gui.elements.HtmlHref;
import net.sf.jwan.servlet.gui.elements.WanDiv;
import net.sf.jwan.servlet.gui.elements.WanParagraph;
import net.sf.jwan.servlet.gui.form.WanForm;
import net.sf.jwan.servlet.gui.form.WanFormFieldSet;
import net.sf.jwan.servlet.gui.form.WanFormInputCheckBox;
import net.sf.jwan.servlet.gui.form.WanFormInputHidden;
import net.sf.jwan.servlet.gui.form.WanFormInputText;
import net.sf.jwan.servlet.gui.form.WanFormSelect;
import net.sf.jwan.servlet.gui.form.WanFormSelectItem;
import net.sf.jwan.servlet.gui.layer.AbstractWanServletLayer;
import net.sf.jwan.servlet.gui.renderable.WanRenderable;
import net.sf.jwan.servlet.util.ServletForm;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hampelratte.svdrp.responses.highlevel.EPGEntry;

public class SmartEpgServlet extends AbstractWanServletLayer {

    static Log logger = LogFactory.getLog(SmartEpgServlet.class);

    public static final long serialVersionUID = 1;

    private Configuration config;

    public SmartEpgServlet(Configuration config) {
        super("lSmEpg");
        this.config = config;
        layerTitle = "SmartEPG";
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
        wd.addContent(createForm(request));
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

    public WanRenderable createForm(HttpServletRequest request) {
        VdrCache vdrC = (VdrCache) this.getServletContext().getAttribute(VdrCache.class.getSimpleName());
        VdrPersistence vdrP = (VdrPersistence) getServletContext().getAttribute(VdrPersistence.class.getSimpleName());
        VdrUser vu = (VdrUser) request.getSession().getAttribute(VdrUser.class.getSimpleName());
        ServletForm form = new ServletForm(request);
        boolean isNew = form.getBoolean("new");
        logger.trace("Mode new?" + isNew);
        VdrSmartSearch vss;
        if (isNew) {
            int chNu = new Integer(form.get("chNu"));
            EPGEntry epg = vdrC.getEpg(chNu, form.get("st"));
            vss = new VdrSmartSearch();
            vss.setVdruser(vu);
            vss.setLimitToChannel(false);
            vss.setChannel(chNu);
            vss.setSuche(epg.getTitle());
            vss.setEpgStart(epg.getStartTime().getTime());
            vss.setLimitRange(false);
            vss.setRangeDown(config.getInt("vdr/smartepg/@rangeDown"));
            vss.setRangeUp(config.getInt("vdr/smartepg/@rangeUp"));
            vss.setAktiv(true);
        } else {
            int id = new Integer(form.get("vssid"));
            vss = (VdrSmartSearch) vdrP.findObject(VdrSmartSearch.class, id);
        }
        WanForm wf = new WanForm();
        wf.setRightButtonTitel("Save");
        wf.setAction(layerServletPath + "/" + getLayerId());
        WanFormFieldSet wffs = new WanFormFieldSet();
        wffs.setName("Allgemein");
        wf.addFieldSet(wffs);
        WanFormInputCheckBox boxChannel = new WanFormInputCheckBox(JvdrTranslation.get("epg", "onlyfor") + " \"" + vdrC.getChName(vss.getChannel()) + "\"");
        boxChannel.setName(VdrSmartSearch.Key.limitToChannel.toString());
        boxChannel.setId("idBc");
        boxChannel.setButtonTitle("JA|NEIN");
        boxChannel.setChecked(vss.isLimitToChannel());
        wffs.addInput(boxChannel);
        WanFormInputCheckBox boxAktiv = new WanFormInputCheckBox("SmartEpg aktiv");
        boxAktiv.setName(VdrSmartSearch.Key.aktiv.toString());
        boxAktiv.setId("idBa");
        boxAktiv.setButtonTitle("JA|NEIN");
        boxAktiv.setChecked(vss.isAktiv());
        wffs.addInput(boxAktiv);
        wffs.addInput(new WanFormInputHidden(VdrSmartSearch.Key.channel.toString(), vss.getChannel()));
        wffs.addInput(new WanFormInputHidden("epgStart", vss.getEpgStart().getTime()));
        wffs.addInput(new WanFormInputHidden("new", isNew));
        if (!isNew) {
            wffs.addInput(new WanFormInputHidden("vssid", vss.getId()));
        }
        WanFormFieldSet wffs2 = new WanFormFieldSet();
        wffs2.setName("Suche nach");
        wf.addFieldSet(wffs2);
        WanFormInputText textWort = new WanFormInputText();
        textWort.setName(VdrSmartSearch.Key.suche.toString());
        textWort.setPlaceholderText("Titelanfang eintragen");
        textWort.setDefaultText(vss.getSuche());
        wffs2.addInput(textWort);
        WanFormFieldSet wffs3 = new WanFormFieldSet();
        wffs3.setName("Zeiten");
        wf.addFieldSet(wffs3);
        WanFormInputCheckBox boxLimitRange = new WanFormInputCheckBox(JvdrTranslation.get("epg", "timeframe"));
        boxLimitRange.setName(VdrSmartSearch.Key.limitRange.toString());
        boxLimitRange.setId("idBl");
        boxLimitRange.setButtonTitle("JA|NEIN");
        boxLimitRange.setChecked(vss.isLimitRange());
        wffs3.addInput(boxLimitRange);
        WanFormInputText textStart = new WanFormInputText();
        textStart.setName("d1");
        textStart.setDefaultText("Beginn: " + DateUtil.dayName(vss.getEpgStart()) + ", " + DateUtil.tmj(vss.getEpgStart()) + " " + DateUtil.sm(vss.getEpgStart()));
        wffs3.addInput(textStart);
        WanFormSelect wfsDown = new WanFormSelect("Suchbereich vorher", VdrSmartSearch.Key.rangeDown.toString());
        String[] downSteps = config.getStringArray("vdr/smartepg/down/t");
        for (String downStep : downSteps) {
            int value = new Integer(downStep);
            WanFormSelectItem wfsi = new WanFormSelectItem("-" + value + " min", value);
            if (value == vss.getRangeDown()) {
                wfsi.setSelected(true);
            }
            wfsDown.addItem(wfsi);
        }
        wffs3.addInput(wfsDown);
        WanFormSelect wfsUp = new WanFormSelect("Suchbereich Nachher", VdrSmartSearch.Key.rangeUp.toString());
        String[] upSteps = config.getStringArray("vdr/smartepg/up/t");
        for (String upStep : upSteps) {
            int value = new Integer(upStep);
            WanFormSelectItem wfsi = new WanFormSelectItem("+" + value + " min", value);
            if (value == vss.getRangeUp()) {
                wfsi.setSelected(true);
            }
            wfsUp.addItem(wfsi);
        }
        wffs3.addInput(wfsUp);
        return wf;
    }

    public void processSubmittedForm(HttpServletRequest request) {
        VdrPersistence vdrP = (VdrPersistence) getServletContext().getAttribute(VdrPersistence.class.getSimpleName());
        VdrCache vdrC = (VdrCache) getServletContext().getAttribute(VdrCache.class.getSimpleName());
        VdrUser vu = (VdrUser) request.getSession().getAttribute(VdrUser.class.getSimpleName());
        ServletForm form = new ServletForm(request);
        form.debug();
        boolean isNew = form.getBoolean("new");
        int chNu = form.getInt(VdrSmartSearch.Key.channel.toString());
        VdrSmartSearch vss = new VdrSmartSearch();
        vss.setVdruser(vu);
        vss.setLimitToChannel(form.getBoolean(VdrSmartSearch.Key.limitToChannel.toString(), false));
        vss.setChannel(chNu);
        vss.setSuche(form.get(VdrSmartSearch.Key.suche.toString()));
        vss.setEpgStart(new Date(new Long(form.get("epgStart"))));
        vss.setLimitRange(form.getBoolean(VdrSmartSearch.Key.limitRange.toString(), false));
        vss.setRangeDown(form.getInt(VdrSmartSearch.Key.rangeDown.toString()));
        vss.setRangeUp(form.getInt(VdrSmartSearch.Key.rangeUp.toString()));
        vss.setAktiv(form.getBoolean(VdrSmartSearch.Key.aktiv.toString(), false));
        logger.trace(vss);
        if (isNew) {
            vdrP.newObject(vss);
        } else {
            vss.setId(form.getInt("vssid"));
            vdrP.updateObject(vss);
        }
        WanParagraph wpResult = new WanParagraph("SmartEpg gespeichert.");
        HtmlHref refMain = new HtmlHref("Hauptmenü", "");
        WanDiv div = new WanDiv();
        div.setDivclass(WanDiv.DivClass.iBlock);
        div.addContent(wpResult);
        div.addContent(new WanParagraph("Weiter zum " + refMain.render() + "."));
        alWanRenderables.add(div);
    }
}
