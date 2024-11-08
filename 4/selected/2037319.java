package drayson.weboca;

import andyr.jtokeniser.BreakIteratorTokeniser;
import andyr.jtokeniser.RegexTokeniser;
import drayson.weboca.gui.SaveURLWizard;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import org.jdesktop.swingworker.SwingWorker;

/**
 * This class organises the downloading of text content from the pages returned for the search engine
 *
 * @author Michael Drayson
 * @version 1.0
 */
public class DownloadUrls extends SwingWorker<Integer, Integer> {

    private static final int MAX_BUFFER_SIZE = 1024;

    public static long localCorpusSize = 0;

    private List<String> urls;

    private String corpusOutputFilename;

    private String format;

    private long WordCount;

    private int customSize;

    private boolean advanced;

    private DownloadStatus downloadStatus;

    public String[] used_urls;

    public int tempcount = 0;

    public DownloadUrls(List<String> urls, String corpusOutputFilename, String format, int WordCount, int customSize, boolean advanced) {
        this.format = format;
        this.corpusOutputFilename = corpusOutputFilename;
        this.urls = urls;
        this.WordCount = Long.valueOf(WordCount);
        this.customSize = customSize;
        this.advanced = advanced;
        downloadStatus = new DownloadStatus();
        System.out.println("WordCount has been passed to DownloadUrls as: " + WordCount);
        System.out.println("customSize has been passed to DownloadUrls as: " + customSize);
        System.out.println("advanced has been passed to DownloadUrls as: " + advanced);
    }

    public float getDownloadProgress(int downloaded, int size) {
        return ((float) downloaded / size) * 100;
    }

    protected Integer doInBackground() throws Exception {
        RandomAccessFile file = null;
        InputStream stream = null;
        SaveURLWizard sw = new SaveURLWizard();
        int downloaded = 0;
        int size = -1;
        int status;
        File corpusOutputFile = new File(corpusOutputFilename);
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(corpusOutputFile), "utf8"));
        } catch (IOException ex) {
            ex.printStackTrace();
            return Integer.valueOf(DownloadStatus.ERROR);
        }
        URL currentUrl;
        int currentUrlCount = 0;
        for (final String urlString : urls) {
            currentUrlCount++;
            downloaded = 0;
            size = -1;
            currentUrl = new URL(urlString);
            try {
                if ((localCorpusSize >= WordCount) && (WordCount != 0)) {
                    System.out.println("localCorpusSize is :" + localCorpusSize + " - Should be bigger than:");
                    System.out.println("WordCount is :" + WordCount);
                    System.out.println("Therefore in the continue loop");
                    continue;
                }
                setProgress(0);
                downloadStatus.setStatus(DownloadStatus.CONNECTING);
                firePropertyChange("status", null, downloadStatus);
                HttpURLConnection connection = (HttpURLConnection) currentUrl.openConnection();
                connection.setRequestProperty("Range", "bytes=" + downloaded + "-");
                connection.connect();
                if (connection.getResponseCode() / 100 != 2) {
                    downloadStatus.setStatus(DownloadStatus.ERROR);
                    firePropertyChange("status", null, downloadStatus);
                    downloadStatus.setNumErrors(downloadStatus.getNumErrors() + 1);
                    throw new Exception("Invalid response code");
                }
                int contentLength = connection.getContentLength();
                System.out.println("The current pages length is " + contentLength);
                if (advanced) {
                    if (customSize != 0) {
                        if (contentLength > customSize) {
                            System.out.println("The current page length is bigger than the customSize");
                            downloadStatus.setNumInvalid(downloadStatus.getNumInvalid() + 1);
                            throw new Exception("Invalid content length");
                        }
                        if (size == -1) {
                            size = contentLength;
                        }
                        if (!HTMLUtils.isValidSize(size)) {
                            System.out.println("HTMLUtils has passed on that this page is not a valid size");
                            continue;
                        }
                    } else {
                        if (contentLength < 1) {
                            System.out.println("This page's length is less than 1, exception thrown");
                            throw new Exception("Invalid content length");
                        }
                        if (size == -1) {
                            size = contentLength;
                        }
                        if (!HTMLUtils.isValidSize(size)) {
                            System.out.println("HTMLUtils has passed on that this page is not a valid size");
                            downloadStatus.setNumInvalid(downloadStatus.getNumInvalid() + 1);
                            continue;
                        }
                    }
                } else {
                    if (contentLength < 1) {
                        System.out.println("This page's length is less than 1, exception thrown");
                        throw new Exception("Invalid content length");
                    }
                    if (size == -1) {
                        size = contentLength;
                    }
                    if (!HTMLUtils.isValidSize(size)) {
                        System.out.println("HTMLUtils has passed on that this page is not a valid size");
                        downloadStatus.setNumInvalid(downloadStatus.getNumInvalid() + 1);
                        continue;
                    }
                }
                System.out.println("Page size checked has been passed, continuing to download");
                downloadStatus.setStatus(DownloadStatus.DOWNLOADING);
                firePropertyChange("status", null, downloadStatus);
                file = new RandomAccessFile(getFileName(currentUrl), "rw");
                file.seek(downloaded);
                stream = connection.getInputStream();
                while (downloadStatus.getStatus() == DownloadStatus.DOWNLOADING) {
                    byte buffer[];
                    if (size - downloaded > MAX_BUFFER_SIZE) {
                        buffer = new byte[MAX_BUFFER_SIZE];
                    } else {
                        buffer = new byte[size - downloaded];
                    }
                    int read = stream.read(buffer);
                    if (read == -1) break;
                    file.write(buffer, 0, read);
                    downloaded += read;
                    setProgress((int) getDownloadProgress(downloaded, size));
                    firePropertyChange("overall", Integer.valueOf(0), Integer.valueOf((100 * (currentUrlCount - 1)) + (int) getDownloadProgress(downloaded, size)));
                }
                if (downloadStatus.getStatus() == DownloadStatus.DOWNLOADING) {
                    downloadStatus.setStatus(DownloadStatus.DOWNLOADED);
                    firePropertyChange("status", null, downloadStatus);
                }
            } catch (Exception e) {
                System.out.println("Exception was thrown:");
                System.out.println(e);
                downloadStatus.setStatus(DownloadStatus.ERROR);
                firePropertyChange("status", null, downloadStatus);
                setProgress(100);
                firePropertyChange("overall", Integer.valueOf(0), Integer.valueOf((100 * (currentUrlCount - 1)) + 100));
            } finally {
                if (file != null) {
                    try {
                        file.close();
                    } catch (Exception e) {
                    }
                }
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Exception e) {
                    }
                }
                System.out.println("File closed and connection to server gone!");
            }
            if (downloadStatus.getStatus() == DownloadStatus.DOWNLOADED) {
                downloadStatus.setStatus(DownloadStatus.TOKENISING);
                firePropertyChange("status", null, downloadStatus);
                File currentFile = new File(getFileName(currentUrl));
                System.out.println("Processing: " + currentFile.getPath());
                try {
                    System.out.println("Getting document...");
                    String document = HTMLUtils.getDocument(currentFile);
                    System.out.println("Done.");
                    System.out.println("Number of chars: " + document.length());
                    System.out.print("Tokenising for word count... ");
                    BreakIteratorTokeniser tokeniser = new BreakIteratorTokeniser(document);
                    System.out.println("Done.");
                    System.out.print("Setting token property change... ");
                    firePropertyChange("tokens", Integer.valueOf(0), Integer.valueOf(tokeniser.countTokens()));
                    System.out.println("Done.");
                    System.out.print("Setting downloadStatus numWords... ");
                    downloadStatus.setNumWords(downloadStatus.getNumWords() + tokeniser.countTokens());
                    System.out.println("Done.");
                    System.out.println("Format: " + format);
                    try {
                        if (format.equals("raw")) {
                            System.out.print("Writing RAW corpus... ");
                            writer.write("CURRENT URL " + currentUrl.getFile());
                            writer.newLine();
                            writer.write(document + System.getProperty("line.separator"));
                            System.out.println("Done.");
                        } else if (format.equals("vertical")) {
                            System.out.print("Writing VERT corpus... ");
                            writer.write("<doc id=\"" + downloadStatus.getNumDownloads() + 1 + "\" url=\"" + currentUrl.toString() + "\">");
                            writer.newLine();
                            RegexTokeniser ret = new RegexTokeniser(document, "\\w+", true);
                            String token = "";
                            while (ret.hasMoreTokens()) {
                                token = ret.nextToken();
                                if (!token.trim().equals("")) {
                                    writer.write(token);
                                    writer.newLine();
                                }
                            }
                            writer.write("</doc>");
                            writer.newLine();
                            System.out.println("Done.");
                        } else {
                            System.out.println("Can't determine which corpus format to use.");
                        }
                        downloadStatus.setNumDownloads(downloadStatus.getNumDownloads() + 1);
                        downloadStatus.setCorpusSize(corpusOutputFile.length());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } catch (FileNotFoundException ex) {
                    ex.printStackTrace();
                }
            }
            downloadStatus.setStatus(DownloadStatus.COMPLETE);
            firePropertyChange("status", null, downloadStatus);
            localCorpusSize = downloadStatus.getNumWords();
        }
        writer.close();
        return Integer.valueOf(DownloadStatus.COMPLETE);
    }

    public String getFileName(URL url) {
        String fileName = url.getFile();
        return System.getProperty("java.io.tmpdir") + fileName.substring(fileName.lastIndexOf('/') + 1);
    }
}
