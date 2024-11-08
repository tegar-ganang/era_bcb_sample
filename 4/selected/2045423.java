package au.com.gworks.jump.app.wiki.server.mockimpl;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.javaongems.runtime.io.PathUtils;
import org.javaongems.server.EntryPointPage;
import org.javaongems.server.GemEntryPointPager;
import au.com.gworks.jump.app.util.EnvironmentUtils;
import au.com.gworks.jump.app.util.FormatUtils;
import au.com.gworks.jump.app.util.RequestPathInfo;
import au.com.gworks.jump.app.wiki.client.service.WikiRpc;
import au.com.gworks.jump.app.wiki.server.WikiEntryPointPage;
import au.com.gworks.jump.io.PathStatus;
import au.com.gworks.jump.system.ApplicationManager;

public class WikiUrlContentPageFactory implements GemEntryPointPager.ContextFactory {

    public static final String DEFAULT_HTML = "Index.html";

    public static final int CTX_ID = WikiRpc.OPEN_RESOURCE_CTX;

    public EntryPointPage create(HttpServletRequest req) {
        return new WikiUrlContentPage();
    }

    public class WikiUrlContentPage extends WikiEntryPointPage {

        protected void buildPageContent() throws Exception {
            FileSystemProvider provider = FileSystemProvider.getInstance();
            redirectToAlternativesIfValid(requestPathInfo, provider, request, response);
            String filename = FilenameUtils.getName(requestPathInfo.path);
            boolean binaryResource = isTypicalBinary(filename);
            if (!binaryResource) binaryResource = !hasDefaultTextExt(filename);
            if (binaryResource) {
                InputStream is = provider.openFile(requestPathInfo.path);
                loadBinaryStream(filename, is, requestPathInfo.attributes.getSize(), request, response);
            } else {
                String content = provider.openFileAsText(requestPathInfo.path);
                loadWikiPage(content);
            }
        }

        protected int getContextId() {
            return CTX_ID;
        }

        private void loadBinaryStream(String streamName, InputStream streamToLoad, long sz, HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType(getContentType(req, streamName));
            resp.setHeader("Content-Disposition", "inline;filename=" + streamName);
            resp.setContentLength((int) sz);
            OutputStream out = resp.getOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(out, 2048);
            try {
                IOUtils.copy(streamToLoad, bos);
            } finally {
                IOUtils.closeQuietly(streamToLoad);
                IOUtils.closeQuietly(bos);
            }
            getCargo().put(GWT_ENTRY_POINT_PAGE_PARAM, null);
        }

        private void loadWikiPage(String content) throws Exception {
            Map cargo = getCargo();
            String linkPath = FormatUtils.toPathLinks(request, requestPathInfo.attributes.getParentPath());
            cargo.put("banner", ApplicationManager.getBanner());
            cargo.put("breadcrumbLinks", linkPath);
            cargo.put("mainContent", content);
        }

        private void redirectToAlternativesIfValid(RequestPathInfo requestPathInfo, FileSystemProvider provider, HttpServletRequest req, HttpServletResponse resp) throws Exception {
            boolean forceExplore = EnvironmentUtils.EXPLORE_VALUE.equals(req.getParameter(EnvironmentUtils.ACTION_PARAM));
            String[] defResource = new String[1];
            if (!forceExplore && doesDefaultFileExist(provider, requestPathInfo.path, defResource)) {
                redirectToEntry(defResource[0]);
            } else if (requestPathInfo.isFolderPath()) {
                redirectToExplorerView();
            }
        }

        private boolean doesDefaultFileExist(FileSystemProvider provider, String path, String[] resource) {
            resource[0] = PathUtils.appendSlashIfRequired(path) + DEFAULT_HTML;
            return (PathStatus.IS_DOCUMENT == provider.getPathStatus(resource[0]));
        }

        private void redirectToEntry(String entry) throws Exception {
            StringBuffer buff = new StringBuffer();
            buff.append(requestPathInfo.getOrigRevisionContext(true)).append(entry);
            String uri = buff.toString();
            EnvironmentUtils.redirectToSubContextUri(response, uri);
        }

        private void redirectToExplorerView() throws Exception {
            StringBuffer buff = new StringBuffer();
            buff.append(requestPathInfo.getOrigRevisionContext(true));
            buff.append(requestPathInfo.path).append(EnvironmentUtils.EXPLORE_QUERYSTRING);
            String uri = buff.toString();
            EnvironmentUtils.redirectToSubContextUri(response, uri);
        }
    }

    private static boolean isTypicalBinary(String fileName) {
        if (isMatchInExtList(ApplicationManager.treatAsRawDefaultExtensions, fileName)) return true;
        String mimeType = ApplicationManager.mimeTypeQuerier.getMimeType(fileName);
        if (mimeType == null) return false;
        return !mimeType.startsWith("text/");
    }

    private static boolean hasDefaultTextExt(String fileName) {
        if (isMatchInExtList(ApplicationManager.treatAsTextDefaultExtensions, fileName)) return true;
        return false;
    }

    private static boolean isMatchInExtList(List extList, String fileName) {
        int size = extList.size();
        for (int cntr = 0; cntr < size; cntr++) if (fileName.endsWith((String) extList.get(cntr))) return true;
        return false;
    }

    private static String getContentType(HttpServletRequest req, String name) {
        String contentType = req.getSession().getServletContext().getMimeType(name);
        return contentType;
    }
}
