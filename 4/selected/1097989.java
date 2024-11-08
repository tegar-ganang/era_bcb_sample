package oxygen.wiki.actions;

import java.io.InputStream;
import java.io.OutputStream;
import oxygen.util.CloseUtils;
import oxygen.util.StringUtils;
import oxygen.web.WebInteractionContext;
import oxygen.web.WebLocal;
import oxygen.wiki.WikiAttachmentProvider;
import oxygen.wiki.WikiCategoryEngine;
import oxygen.wiki.WikiEngine;
import oxygen.wiki.WikiException;
import oxygen.wiki.WikiLinkHolder;
import oxygen.wiki.WikiLocal;
import oxygen.wiki.WikiProvidedObject;

/**
 * Handles viewing an attachment screen 
 * @author ugorji
 */
public class ViewAttachmentAction extends GenericWikiWebAction {

    {
        setFlag(FLAG_REQUIRES_EXTRAINFO);
        setFlag(FLAG_REQUIRES_PAGENAME);
        setFlag(FLAG_HONORS_VERSION);
        setFlag(FLAG_NOT_HANDLED_BY_PORTLET);
        setFlag(FLAG_MAKE_SHORTHAND);
    }

    public int render() throws Exception {
        WebInteractionContext request = WebLocal.getWebInteractionContext();
        InputStream in = null;
        OutputStream out = null;
        try {
            WikiEngine we = WikiLocal.getWikiEngine();
            WikiCategoryEngine wce = WikiLocal.getWikiCategoryEngine();
            WikiLinkHolder wlh = WikiLocal.getWikiLinkHolder();
            WikiAttachmentProvider prov = wce.getAttachmentProvider();
            String attname = wlh.getExtrainfo();
            if (StringUtils.isBlank(attname)) {
                throw new WikiException("No attachment name is provided");
            }
            WikiProvidedObject att = new WikiProvidedObject(attname);
            att.setVersion(wlh.getVersion());
            String mimetype = request.getMimeType(att.getName());
            if (mimetype == null) {
                mimetype = "application/binary";
            }
            request.setContentType(mimetype);
            request.setHeader("Content-Disposition", "inline; filename=\"" + att.getName() + "\";");
            in = prov.getAttachmentInputStream(wlh.getWikiPage(), att);
            out = request.getOutputStream();
            int read = 0;
            byte buffer[] = new byte[8192];
            while ((read = in.read(buffer)) > -1) {
                out.write(buffer, 0, read);
            }
            return RENDER_COMPLETED;
        } finally {
            CloseUtils.close(in);
            CloseUtils.close(out);
        }
    }
}
