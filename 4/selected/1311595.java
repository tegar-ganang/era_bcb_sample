package net.sf.poormans.view.context.object.tagtool;

import java.io.File;
import java.io.IOException;
import net.sf.poormans.exception.FatalException;
import net.sf.poormans.livecycle.PojoHelper;
import net.sf.poormans.tool.Link;
import net.sf.poormans.tool.PathTool;
import net.sf.poormans.view.ViewMode;
import net.sf.poormans.view.context.IContextObjectCommon;
import net.sf.poormans.view.context.IContextObjectNeedPojoHelper;
import net.sf.poormans.view.context.IContextObjectNeedViewMode;
import net.sf.poormans.wysisygeditor.CKFileResource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Construct an a-tag. Mainly used by other context objects.
 * 
 * @version $Id: LinkTagTool.java 2134 2011-07-17 12:14:41Z th-schwarz $
 * @author <a href="mailto:th-schwarz@users.sourceforge.net">Thilo Schwarz</a>
 */
@Component(value = "linktagtool")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class LinkTagTool extends CommonXhtmlTagTool implements IContextObjectCommon, IContextObjectNeedPojoHelper, IContextObjectNeedViewMode {

    private static Logger logger = Logger.getLogger(LinkTagTool.class);

    private PojoHelper pojoHelper;

    private boolean isExportView = false;

    private boolean isExternalLink = false;

    @Autowired
    private Link link;

    public LinkTagTool() {
        super("a");
    }

    @Override
    public void setViewMode(final ViewMode viewMode) {
        isExportView = viewMode.equals(ViewMode.EXPORT);
    }

    @Override
    public void setPojoHelper(PojoHelper pojoHelper) {
        this.pojoHelper = pojoHelper;
    }

    public LinkTagTool setHref(final String href) {
        link.init(href);
        isExternalLink = link.isExternal();
        String tempHref;
        if (isExternalLink) tempHref = href; else tempHref = href.replace(File.separatorChar, '/');
        return setAttribute("href", PathTool.encodePath(tempHref));
    }

    public LinkTagTool setAttribute(final String name, final String value) {
        super.setAttr(name, value);
        return this;
    }

    public LinkTagTool setTagValue(final String tagValue) {
        super.value(tagValue);
        return this;
    }

    /**
	 * Construct the a-tag.
	 */
    @Override
    public String toString() {
        String hrefString = super.getAttr("href");
        if (StringUtils.isBlank(hrefString)) throw new IllegalArgumentException("'href' isn't set!");
        if (!isExternalLink) {
            CKFileResource cKFileResource = new CKFileResource(this.pojoHelper.getSite());
            cKFileResource.consructFromTagFromView(hrefString);
            if (isExportView) {
                try {
                    File srcFile = cKFileResource.getFile();
                    File destFile = cKFileResource.getExportFile();
                    FileUtils.copyFile(srcFile, destFile);
                } catch (IOException e) {
                    logger.error("Error while copy [" + cKFileResource.getFile().getPath() + "] to [" + cKFileResource.getExportFile().getPath() + "]: " + e.getMessage(), e);
                    throw new FatalException("Error while copy [" + cKFileResource.getFile().getPath() + "] to [" + cKFileResource.getExportFile().getPath() + "]: " + e.getMessage(), e);
                }
                this.setHref(cKFileResource.getTagSrcForExport(this.pojoHelper.getLevel()));
            } else this.setHref(cKFileResource.getTagSrcForPreview());
        }
        isExternalLink = false;
        return super.contructTag();
    }
}
