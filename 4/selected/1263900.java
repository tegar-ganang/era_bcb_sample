package net.sf.poormans.view.renderer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import net.sf.poormans.Constants;
import net.sf.poormans.configuration.InitializationManager;
import net.sf.poormans.exception.FatalException;
import net.sf.poormans.livecycle.GuiObjectHolder;
import net.sf.poormans.model.domain.IPersistentPojo;
import net.sf.poormans.model.domain.IRenderable;
import net.sf.poormans.model.domain.pojo.Macro;
import net.sf.poormans.model.domain.pojo.Template;
import net.sf.poormans.model.domain.pojo.TemplateType;
import net.sf.poormans.server.lifecycle.Context;
import net.sf.poormans.tool.PathTool;
import net.sf.poormans.tool.ckeditor.CKeditorWrapper;
import net.sf.poormans.view.PojoHelper;
import net.sf.poormans.view.ViewMode;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * This is the main rendering object. The major task is to find and call the right renderer method. It
 * recognizes, if a fatal error has happened. If so, an error page will be rendered. Otherwise, depending on the
 * extension of the requested file, the file will be rendered as static resource or {@link VelocityRenderer} is called.
 * 
 * @version $Id: Renderer.java 1594 2009-04-13 08:33:35Z th-schwarz $
 * @author <a href="mailto:th-schwarz@users.sourceforge.net">Thilo Schwarz</a>
 */
public class Renderer {

    private static Logger logger = Logger.getLogger(Renderer.class);

    private Context context;

    public Renderer(final Context context) {
        this.context = context;
    }

    public void doRender() {
        try {
            boolean objectNotFound = false;
            String objectToRender = null;
            if (context.getTypeDescriptor() != null && (context.getTypeDescriptor().equals(Constants.LINK_TYPE_MACRO) || context.getTypeDescriptor().equals(Constants.LINK_TYPE_TEMPLATE))) {
                IPersistentPojo persistentPojo = context.getPersistentPojo();
                if (context.getTypeDescriptor().equals(Constants.LINK_TYPE_MACRO)) {
                    Macro macro;
                    if (persistentPojo == null) {
                        macro = new Macro();
                        macro.setSite(GuiObjectHolder.getInstance().getSite());
                    } else macro = (Macro) persistentPojo;
                    VelocityRenderer.render(context.getServletResponse().getWriter(), macro);
                } else if (context.getTypeDescriptor().endsWith(Constants.LINK_TYPE_TEMPLATE)) {
                    Template template;
                    if (persistentPojo == null) {
                        template = new Template();
                        template.setType(TemplateType.PAGE);
                        template.setSite(GuiObjectHolder.getInstance().getSite());
                    } else template = (Template) persistentPojo;
                    VelocityRenderer.render(context.getServletResponse().getWriter(), template);
                }
            } else {
                IRenderable renderable = null;
                if (context.isPreview() || context.isEditView()) {
                    renderable = context.getRenderable();
                } else objectToRender = context.getFullRequestedResource();
                if (renderable != null) {
                    Map<String, Object> additionalContextObjects = null;
                    if (context.getViewMode().equals(ViewMode.EDIT)) {
                        additionalContextObjects = new HashMap<String, Object>();
                        additionalContextObjects.put("editor", new CKeditorWrapper(new PojoHelper(renderable).getSite(), context.getServletRequest()));
                    }
                    VelocityRenderer.render(context.getServletResponse().getWriter(), renderable, context.getViewMode(), additionalContextObjects);
                } else if ((objectToRender != null) && (!context.getRequestedResourceExtention().equalsIgnoreCase("css") || !context.getRequestedResourceExtention().equalsIgnoreCase("js") || ((Constants.RENDERED_EXT.equalsIgnoreCase(context.getRequestedResourceExtention()) && context.isSitesRequest()) || context.isPreview() || context.isEditView()) || !InitializationManager.isImageExtention(context.getRequestedResourceExtention()))) {
                    objectNotFound = !renderStaticResource(objectToRender, context.getServletResponse());
                } else {
                    logger.debug("Nothing to render!");
                    return;
                }
            }
            if (objectNotFound) context.getServletResponse().sendError(HttpServletResponse.SC_NOT_FOUND); else context.getServletResponse().setHeader("Cache-Control", "no-cache");
        } catch (Exception e) {
            try {
                e.printStackTrace();
            } catch (Exception e1) {
                throw new FatalException(e1.getMessage(), e1);
            }
        }
    }

    /**
	 * @param requestedResource
	 * @param servletResponse
	 * @return 'true', if 'requestedResource' was rendered successful.
	 * @throws IOException
	 */
    private static boolean renderStaticResource(final String requestedResource, HttpServletResponse servletResponse) throws IOException {
        boolean successfull = true;
        String fileName = PathTool.getFSPathOfResource(requestedResource);
        File file = new File(fileName);
        if (!file.exists()) {
            logger.error("Static resource not found: " + fileName);
            return false;
        }
        if (fileName.endsWith("xml") || fileName.endsWith("asp")) servletResponse.setContentType("text/xml"); else if (fileName.endsWith("css")) servletResponse.setContentType("text/css"); else if (fileName.endsWith("js")) servletResponse.setContentType("text/javascript");
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            IOUtils.copy(in, servletResponse.getOutputStream());
            logger.debug("Static resource rendered: ".concat(fileName));
        } catch (FileNotFoundException e) {
            logger.error("Static resource not found: " + fileName);
            successfull = false;
        } finally {
            IOUtils.closeQuietly(in);
        }
        return successfull;
    }
}
