package net.sf.poormans.gui.listener;

import java.io.File;
import java.io.IOException;
import net.sf.poormans.configuration.InitializationManager;
import net.sf.poormans.exception.FatalException;
import net.sf.poormans.gui.BrowserManager;
import net.sf.poormans.gui.WorkspaceToolBarManager;
import net.sf.poormans.gui.dialog.DialogManager;
import net.sf.poormans.gui.treeview.TreeViewManager;
import net.sf.poormans.livecycle.SiteHolder;
import net.sf.poormans.model.domain.PoPathInfo;
import net.sf.poormans.model.domain.pojo.Macro;
import net.sf.poormans.model.domain.pojo.Site;
import net.sf.poormans.model.domain.pojo.Template;
import net.sf.poormans.model.domain.pojo.TemplateType;
import net.sf.poormans.tool.file.FileTool;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

/**
 * Listener for adding a new {@link Site}.
 * 
 * @version $Id: ListenerAddSite.java 2122 2011-07-02 09:31:26Z th-schwarz $
 * @author <a href="mailto:th-schwarz@users.sourceforge.net">Thilo Schwarz</a>
 */
public class ListenerAddSite implements SelectionListener {

    private static Logger logger = Logger.getLogger(ListenerAddSite.class);

    @Override
    public void widgetDefaultSelected(SelectionEvent e) {
    }

    @Override
    public void widgetSelected(SelectionEvent e) {
        logger.debug("SEL add site");
        Site site = new Site();
        if (DialogManager.startDialogPersitentPojo(e.display.getActiveShell(), site)) {
            File defaultResourceDir = new File(InitializationManager.getProperty("poormans.dir.defaultresources"));
            File srcDir = new File(defaultResourceDir.getAbsoluteFile(), "sites");
            File srcConfigDir = new File(srcDir, "configuration");
            File destDir = PoPathInfo.getSiteDirectory(site);
            File destConfigDir = new File(destDir, "configuration");
            destConfigDir.mkdirs();
            try {
                FileUtils.copyFileToDirectory(new File(srcDir, "format.css"), destDir);
                FileUtils.copyFileToDirectory(new File(srcConfigDir, "fckconfig.js"), destConfigDir);
                FileUtils.copyFileToDirectory(new File(srcConfigDir, "fckstyles.xml"), destConfigDir);
                site.add(buildTemplate(srcDir, "layout.html", site, null));
                File srcTemplatedir = new File(srcDir, "templates");
                site.add(buildTemplate(srcTemplatedir, "gallery.html", site, TemplateType.GALLERY));
                site.add(buildTemplate(srcTemplatedir, "image.html", site, TemplateType.IMAGE));
                site.add(buildTemplate(srcTemplatedir, "page.html", site, TemplateType.PAGE));
                Macro macro = new Macro();
                macro.setParent(site);
                macro.setName("user_menu.vm");
                macro.setText(FileTool.toString(new File(srcConfigDir, "user_menu.vm")));
            } catch (IOException e1) {
                throw new FatalException("While construct the default file structure of a site: " + e1.getMessage(), e1);
            }
            SiteHolder siteHolder = InitializationManager.getBean(SiteHolder.class);
            siteHolder.setSite(site);
            TreeViewManager treeViewManager = InitializationManager.getBean(TreeViewManager.class);
            treeViewManager.fillAndExpands(site);
            BrowserManager browserManager = InitializationManager.getBean(BrowserManager.class);
            browserManager.showHelp();
            WorkspaceToolBarManager.actionAfterSiteRenamed(site);
        }
    }

    private Template buildTemplate(File templatedir, String templateName, Site site, TemplateType type) throws IOException {
        File templateFile = new File(templatedir, templateName);
        Template template = new Template();
        template.setParent(site);
        template.setName(templateName);
        template.setType(type);
        template.setText(FileTool.toString(templateFile));
        return template;
    }
}
