package org.clarent.ivyidea.resolve.http;

import com.intellij.util.net.HttpConfigurable;
import org.apache.ivy.util.CopyProgressListener;
import org.apache.ivy.util.url.AbstractURLHandler;
import org.apache.ivy.util.url.URLHandler;
import org.apache.ivy.util.url.URLHandlerDispatcher;
import org.apache.ivy.util.url.URLHandlerRegistry;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author Guy Mahieu
*/
public class IntellijProxyURLHandler extends AbstractURLHandler {

    private final URLHandler delegate;

    public static void setupHttpProxy() {
        URLHandlerDispatcher dispatcher = new URLHandlerDispatcher();
        URLHandler httpHandler = new IntellijProxyURLHandler(URLHandlerRegistry.getHttp());
        dispatcher.setDownloader("http", httpHandler);
        dispatcher.setDownloader("https", httpHandler);
        URLHandlerRegistry.setDefault(dispatcher);
    }

    private IntellijProxyURLHandler(URLHandler delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate can't be null");
        }
        this.delegate = delegate;
    }

    public URLInfo getURLInfo(URL url) {
        return getURLInfo(url, 0);
    }

    public URLInfo getURLInfo(URL url, int timeout) {
        try {
            prepareURL(url);
            return delegate.getURLInfo(url, timeout);
        } catch (IOException e) {
            return URLHandler.UNAVAILABLE;
        }
    }

    public InputStream openStream(URL url) throws IOException {
        prepareURL(url);
        return delegate.openStream(url);
    }

    public void download(URL src, File dest, CopyProgressListener l) throws IOException {
        prepareURL(src);
        delegate.download(src, dest, l);
    }

    public void upload(File src, URL dest, CopyProgressListener l) throws IOException {
        prepareURL(dest);
        delegate.upload(src, dest, l);
    }

    public void setRequestMethod(int requestMethod) {
        delegate.setRequestMethod(requestMethod);
    }

    private void prepareURL(URL dest) throws IOException {
        HttpConfigurable.getInstance().prepareURL(dest.toExternalForm());
    }
}
