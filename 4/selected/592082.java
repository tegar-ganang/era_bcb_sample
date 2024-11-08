package com.ideo.jso.tag.includers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import org.apache.commons.io.IOUtils;
import com.ideo.jso.conf.AbstractGroupBuilder;
import com.ideo.jso.conf.Group;
import com.ideo.jso.retention.RetentionHelper;
import com.ideo.jso.util.URLUtils;

/**
 * Adds a tag that will get its data from a file into the disk. 
 * 
 * @author Julien Maupoux
 *
 */
class RetentionIncluder implements IIncluder {

    public void includeJs(Group group, Writer out, PageContext pageContext) throws IOException {
        includeResource(pageContext, out, RetentionHelper.buildRootRetentionFilePath(group, ".js"), JS_BEGIN_TAG, JS_END_TAG);
        ByteArrayOutputStream outtmp = new ByteArrayOutputStream();
        if (AbstractGroupBuilder.getInstance().buildGroupJsIfNeeded(group, outtmp, pageContext.getServletContext())) {
            FileOutputStream fileStream = new FileOutputStream(new File(RetentionHelper.buildFullRetentionFilePath(group, ".js")));
            IOUtils.copy(new ByteArrayInputStream(outtmp.toByteArray()), fileStream);
            fileStream.close();
        }
    }

    public void includeCss(Group group, Writer out, PageContext pageContext) throws IOException {
        ByteArrayOutputStream outtmp = new ByteArrayOutputStream();
        if (AbstractGroupBuilder.getInstance().buildGroupJsIfNeeded(group, outtmp, pageContext.getServletContext())) {
            FileOutputStream fileStream = null;
            try {
                fileStream = new FileOutputStream(new File(RetentionHelper.buildFullRetentionFilePath(group, ".css")));
                IOUtils.copy(new ByteArrayInputStream(outtmp.toByteArray()), fileStream);
            } finally {
                if (fileStream != null) fileStream.close();
            }
        }
    }

    /**
	 * Adds a tag into the flow to include a resource "the classic way", and suffix by a timestamp corresponding to the last modification date of the file
	 * @param pageContext
	 * @param out
	 * @param webPath
	 * @param tagBegin
	 * @param tagEnd
	 * @throws IOException
	 */
    private void includeResource(PageContext pageContext, Writer out, String webPath, String tagBegin, String tagEnd) throws IOException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        out.write(tagBegin);
        out.write(URLUtils.concatUrlWithSlaches(request.getContextPath(), webPath));
        out.write(tagEnd);
        out.write("\n");
    }
}
