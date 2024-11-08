package it.could.confluence.autoexport;

import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.BlogPost;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.pages.actions.ViewPageAction;
import com.atlassian.confluence.pages.thumbnail.ThumbnailManager;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.util.ConfluenceRenderUtils;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.core.util.FileUtils;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.renderer.WikiStyleRenderer;
import com.atlassian.spring.container.ContainerManager;
import com.opensymphony.util.TextUtils;
import com.opensymphony.xwork.ActionContext;
import it.could.confluence.autoexport.engine.ExportBeautifier;
import it.could.confluence.autoexport.engine.ExportUtils;
import it.could.confluence.autoexport.engine.Notifiable;
import it.could.confluence.localization.LocalizedComponent;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.MethodInvocationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * <p>The {@link ExportManager} class represents the core object exporting
 * content out of Confluence.</p>
 */
public class ExportManager extends LocalizedComponent {

    /** <p>The key for the request in the current {@link ActionContext}.</p> */
    private static final String AC_REQUEST_KEY = "com.opensymphony.xwork.dispatcher.HttpServletRequest";

    /** <p>The {@link TemplatesManager} used by this instance.</p> */
    private final TemplatesManager templatesManager;

    /** <p>The {@link LocationManager} used by this instance.</p> */
    private final LocationManager locationManager;

    /** <p>The {@link ConfigurationManager} used by this instance.</p> */
    private final ConfigurationManager configurationManager;

    /** <p>The {@link PageManager} used by this instance.</p> */
    private final PageManager pageManager;

    /** <p>The {@link SpaceManager} used by this instance.</p> */
    private final SpaceManager spaceManager;

    /** <p>The {@link ThumbnailManager} used by this instance.</p> */
    private final ThumbnailManager thumbnailManager;

    /** <p>The {@link WikiStyleRenderer} rendering content.</p> */
    private final WikiStyleRenderer wikiStyleRenderer;

    /** <p>The {@link PluginAccessor} gathering plugin details.</p> */
    private final PluginAccessor pluginAccessor;

    /** <p>Create a new {@link ExportManager} instance.</p> */
    public ExportManager(TemplatesManager templatesManager, LocationManager locationManager, ConfigurationManager configurationManager, SpaceManager spaceManager, PageManager pageManager, ThumbnailManager thumbnailManager, WikiStyleRenderer wikiStyleRenderer, PluginAccessor pluginAccessor) {
        this.templatesManager = templatesManager;
        this.locationManager = locationManager;
        this.configurationManager = configurationManager;
        this.spaceManager = spaceManager;
        this.pageManager = pageManager;
        this.thumbnailManager = thumbnailManager;
        this.wikiStyleRenderer = wikiStyleRenderer;
        this.pluginAccessor = pluginAccessor;
        this.log.info("Instance created");
    }

    /**
     * <p>Export all the content from all specified spaces.</p>
     */
    public void export(String spaceKeys[], Notifiable notifiable, boolean exportPages) {
        if (spaceKeys == null) return;
        for (int x = 0; x < spaceKeys.length; x++) {
            this.export(spaceKeys[x], notifiable, exportPages);
        }
    }

    /**
     * <p>Export all the content from the specified space.</p>
     */
    public void export(String spaceKey, Notifiable notifiable, boolean exportPages) {
        if (spaceKey == null) return;
        final Space space = this.spaceManager.getSpace(spaceKey);
        this.export(space, notifiable, exportPages);
    }

    /**
     * <p>Export all the content from the specified space.</p>
     */
    public void export(Space space, Notifiable notifiable, boolean exportPages) {
        if (space == null) return;
        if (!this.locationManager.exportable(space)) {
            this.message(notifiable, "msg.locked-space", space, null, null);
            return;
        }
        this.debug("msg.exporting-space", space, null, null);
        if (exportPages) {
            final List pagesList = this.pageManager.getPages(space, true);
            final Iterator pages = pagesList.iterator();
            while (pages.hasNext()) {
                this.export((Page) pages.next(), notifiable);
            }
            final List postsList = this.pageManager.getBlogPosts(space, true);
            final Iterator posts = postsList.iterator();
            while (posts.hasNext()) {
                this.export((BlogPost) posts.next(), notifiable);
            }
        }
        final String styleData = ConfluenceRenderUtils.renderSpaceStylesheet(space);
        final File styleFile = this.locationManager.getFile(space, "space.css");
        try {
            final File resourcesDir = styleFile.getParentFile();
            if (!resourcesDir.isDirectory()) resourcesDir.mkdirs();
            FileOutputStream stream = new FileOutputStream(styleFile);
            OutputStreamWriter writer = new OutputStreamWriter(stream, "UTF-8");
            writer.write(styleData);
            writer.flush();
            stream.flush();
            writer.close();
            stream.close();
        } catch (IOException exception) {
            this.error(notifiable, exception, "err.stylesheet", space, null, styleFile);
        }
        this.message(notifiable, "msg.exported-space", space, null, null);
    }

    /**
     * <p>Export the specified page.</p>
     */
    public void export(AbstractPage page, Notifiable notifiable) {
        if (page == null) return;
        if (!this.locationManager.exportable(page)) {
            this.message(notifiable, "msg.locked-page", null, page, null);
            return;
        }
        this.debug("msg.exporting-page", null, page, null);
        try {
            final VelocityContext context = new VelocityContext();
            final Template template = this.templatesManager.getTemplate(page.getSpaceKey());
            final String body = this.wikiStyleRenderer.convertWikiToXHtml(page.toPageContext(), page.getContent());
            final String styleUri = this.locationManager.getLocation(page.getSpace(), "space.css").toString();
            final String confluenceUrl = this.configurationManager.getConfluenceUrl();
            final ViewPageAction action = new ViewPageAction();
            ContainerManager.autowireComponent(action);
            action.setPage(page);
            context.put("generalUtil", new GeneralUtil());
            context.put("webwork", new TextUtils());
            context.put("autoexport", new ExportUtils(this.configurationManager, this.wikiStyleRenderer, this.pluginAccessor));
            context.put("pageManager", this.pageManager);
            context.put("confluenceUri", confluenceUrl);
            context.put("stylesheet", styleUri);
            context.put("action", action);
            context.put("page", page);
            context.put("body", body);
            context.put("req", ActionContext.getContext().get(AC_REQUEST_KEY));
            final File pageFile = this.locationManager.getFile(page);
            final File spaceDir = pageFile.getParentFile();
            if (!spaceDir.isDirectory()) spaceDir.mkdirs();
            try {
                final StringWriter writer = new StringWriter();
                template.merge(context, writer);
                writer.flush();
                writer.close();
                final ExportBeautifier beautifier = new ExportBeautifier(page, this.configurationManager, this.pageManager, this.spaceManager, this.locationManager);
                beautifier.beautify(writer.toString(), pageFile);
            } catch (MethodInvocationException exception) {
                Throwable throwable = exception.getWrappedThrowable();
                if (throwable != null) this.error(notifiable, throwable, "err.invoking-method", null, page, null);
                this.error(notifiable, exception, "err.exporting-page", null, page, null);
                System.err.println(exception.getReferenceName());
            } catch (Exception exception) {
                this.error(notifiable, exception, "err.exporting-page", null, page, null);
            }
            final Iterator iterator = page.getAttachments().iterator();
            while (iterator.hasNext()) {
                final Attachment attachment = (Attachment) iterator.next();
                final File aFile = this.locationManager.getFile(attachment, false);
                final File aDir = aFile.getParentFile();
                if (!aDir.isDirectory()) aDir.mkdirs();
                try {
                    final InputStream aInput = attachment.getContentsAsStream();
                    FileUtils.copyFile(aInput, aFile, true);
                    aInput.close();
                    this.debug("msg.exported-attachment", null, page, aFile.getName());
                } catch (IOException exception) {
                    this.error(notifiable, exception, "err.exporting-attachment", null, page, aFile.getName());
                }
                if (!this.thumbnailManager.isThumbnailable(attachment)) continue;
                this.thumbnailManager.getThumbnail(attachment);
                final File sFile = this.thumbnailManager.getThumbnailFile(attachment);
                if (sFile.exists()) try {
                    final File tFile = this.locationManager.getFile(attachment, true);
                    final File tDir = tFile.getParentFile();
                    if (!tDir.isDirectory()) tDir.mkdirs();
                    if (tFile.exists()) tFile.delete();
                    FileUtils.copyFile(sFile, tFile);
                    this.debug("msg.exported-thumbnail", null, page, tFile.getName());
                } catch (IOException exception) {
                    this.error(notifiable, exception, "err.exporting-thumbnail", null, page, aFile.getName());
                }
            }
        } catch (Exception exception) {
            this.error(notifiable, exception, "err.exporting-page", null, page, null);
        }
        this.message(notifiable, "msg.exported-page", null, page, null);
    }

    private Object[] getParams(Space space, AbstractPage page, Object arg) {
        if (page == null) {
            final String sname = space == null ? null : space.getName();
            return new Object[] { null, sname, arg };
        } else if (page instanceof BlogPost) {
            final Space sinst = space == null ? page.getSpace() : space;
            final String sname = sinst == null ? null : sinst.getName();
            final Date date = page.getCreationDate();
            final StringBuffer buffer = new StringBuffer();
            buffer.append(new SimpleDateFormat("yyyy/MM/dd: ").format(date));
            buffer.append(page.getTitle());
            return new Object[] { buffer.toString(), sname, arg };
        } else {
            final Space sinst = space == null ? page.getSpace() : space;
            final String sname = sinst == null ? null : sinst.getName();
            return new Object[] { page.getTitle(), sname, arg };
        }
    }

    private void error(Notifiable notifiable, Throwable exception, String key, Space space, AbstractPage page, Object arg) {
        final Object params[] = this.getParams(space, page, arg);
        final String message = this.localizeMessage(key, params);
        if (notifiable != null) {
            notifiable.notify(message);
            notifiable.notify(exception);
        }
        this.log.warn(message, exception);
    }

    private void message(Notifiable notifiable, String key, Space space, AbstractPage page, Object arg) {
        final Object params[] = this.getParams(space, page, arg);
        final String message = this.localizeMessage(key, params);
        if (notifiable != null) notifiable.notify(message);
        this.log.info(message);
    }

    private void debug(String key, Space space, AbstractPage page, Object arg) {
        final Object params[] = this.getParams(space, page, arg);
        final String message = this.localizeMessage(key, params);
        this.log.debug(message);
    }
}
