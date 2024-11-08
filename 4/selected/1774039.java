package org.lightmtv.response;

import internal.sitemesh.CharArrayReader;
import internal.web.PageResponseWrapper;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.lightcommons.io.IOUtils;

/**
 * 一般来说，你不需要创建他。但你可能会想要拦截他。
 * @author GL
 *
 */
public class WrappedResponse extends AbstractContentResponse {

    private PageResponseWrapper responseWrapper;

    public WrappedResponse(PageResponseWrapper responseWrapper) {
        super();
        this.responseWrapper = responseWrapper;
    }

    public char[] getContent() throws IOException {
        return responseWrapper.getContents();
    }

    @Override
    protected void sendQuietly(HttpServletResponse response) throws Exception {
        IOUtils.copy(new CharArrayReader(responseWrapper.getContents()), response.getOutputStream(), getEncoding());
    }
}
