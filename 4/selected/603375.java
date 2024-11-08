package ch.oblivion.comixviewer.engine.download;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ch.oblivion.comixviewer.engine.events.IProgressMonitor;
import ch.oblivion.comixviewer.engine.events.ProgressMonitorAdapter;
import ch.oblivion.comixviewer.engine.model.ComixPage;
import ch.oblivion.comixviewer.engine.model.ComixProfile;

public class DefaultPageBuilder implements IPageBuilder {

    private static final int BUFFER_SIZE = 1024 * 4;

    private File parentFile = new File(System.getProperty("user.home") + System.getProperty("file.separator") + ".comixViewer");

    @Override
    public ComixPage buildPage(ComixProfile profile, URL pageUrl, IProgressMonitor monitor) throws IOException {
        ComixPage page = null;
        if (pageUrl != null) {
            URLConnection connection = pageUrl.openConnection();
            monitor.start(connection.getContentLength());
            StringBuffer stringBuffer = new StringBuffer();
            char[] charBuffer = new char[BUFFER_SIZE];
            InputStream stream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            int read;
            while ((read = reader.read(charBuffer)) != -1) {
                stringBuffer.append(charBuffer, 0, read);
                monitor.worked(read, null);
                if (monitor.isCancelled()) {
                    reader.close();
                    stream.close();
                    return null;
                }
            }
            reader.close();
            stream.close();
            String pageContent = stringBuffer.toString();
            String title = findMatch(profile.getTitlePattern(), pageContent);
            String description = findMatch(profile.getDescriptionPattern(), pageContent);
            String nextPageUrl = findMatch(profile.getNextPageUrlPattern(), pageContent);
            String previousPageUrl = findMatch(profile.getPreviousPageUrlPattern(), pageContent);
            String imageUrl = findMatch(profile.getImageUrlPattern(), pageContent);
            String cacheName = findMatch(profile.getCacheNamePattern(), pageContent);
            if (imageUrl != null) {
                page = new ComixPage();
                page.setTitle(title);
                page.setDescription(description);
                page.setCurrentPageUrl(pageUrl);
                page.setImageUrl(new URL(profile.getSiteUrl(), imageUrl));
                page.setNextPageUrl(new URL(profile.getSiteUrl(), nextPageUrl));
                page.setPreviousPageUrl(new URL(profile.getSiteUrl(), previousPageUrl));
                page.setCacheName(getCacheName(profile, cacheName, page.getImageUrl()));
            }
            monitor.done(null);
        }
        return page;
    }

    /**
	 * This method is protected to as to allow an implementation to override it.
	 * @param profile
	 * @param cacheName
	 * @param fileUrl
	 * @return
	 */
    protected String getCacheName(ComixProfile profile, String cacheName, URL fileUrl) {
        String fileType = ".png";
        int start = fileUrl.getPath().lastIndexOf('.') + 1;
        if (start < fileUrl.getPath().length()) {
            fileType = "." + fileUrl.getPath().substring(start);
        }
        return cleanName(profile.getName()) + System.getProperty("file.separator") + cleanName(cacheName) + fileType;
    }

    protected String cleanName(String input) {
        String output = input.replace(',', '_');
        output = output.replace(' ', '_');
        output = output.replace('/', '_');
        output = output.replace('\\', '_');
        output = output.replace('.', '_');
        output = output.replace('?', '_');
        return output;
    }

    private String findMatch(Pattern pattern, String pageContent) {
        Matcher matcher = pattern.matcher(pageContent);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    @Override
    public InputStream getCachedImageFile(ComixProfile comixProfile, ComixPage page, IProgressMonitor monitor) throws IOException {
        File file = new File(parentFile, page.getCacheName());
        if (!file.exists()) {
            FileOutputStream outputStream = new FileOutputStream(file, false);
            writeImageToFile(page, outputStream, monitor);
            outputStream.close();
        }
        return new FileInputStream(file);
    }

    @Override
    public InputStream getCachedThumbImageFile(ComixProfile comixProfile, ComixPage page, ProgressMonitorAdapter monitor) throws IOException {
        return getCachedImageFile(comixProfile, page, monitor);
    }

    protected int writeImageToFile(ComixPage page, FileOutputStream outputStream, IProgressMonitor monitor) throws IOException {
        URLConnection connection = page.getImageUrl().openConnection();
        int contentLength = connection.getContentLength();
        monitor.start(contentLength);
        byte[] byteBuffer = new byte[BUFFER_SIZE];
        InputStream inputStream = connection.getInputStream();
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        BufferedOutputStream bos = new BufferedOutputStream(outputStream);
        int read;
        while ((read = bis.read(byteBuffer)) != -1) {
            bos.write(byteBuffer, 0, read);
            monitor.worked(read, null);
        }
        bos.close();
        bis.close();
        inputStream.close();
        monitor.done(null);
        return contentLength;
    }

    public File getParentFile() {
        return parentFile;
    }

    public void setParentFile(File parentFile) {
        this.parentFile = parentFile;
    }
}
