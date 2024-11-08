package randres.kindle.covers;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Vector;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import randres.kindle.retriever.OptionRecord;
import randres.kindle.retriever.InteractionRequest;
import randres.kindle.retriever.StdinResponser;

public class CoversRetriever extends Thread {

    private static final String HTTP_PHOTO_GOODREADS_COM_BOOKS = "http://photo.goodreads.com/books/";

    public static final String ISO_8859_1 = "ISO-8859-1";

    public static final String SEARCH_FIELD = "sitesearch_field";

    public static final String NEEDS_CLARIFICATION = "NEEDS_CLARIFICATION";

    public static final String SUCCESS = "FINISHED";

    public static final String FAIL = "FAIL";

    public static final String NOT_FOUND = "NOT_FOUND";

    private static String GOODREADS_WEB_PAGE = "http://www.goodreads.com/search/search?query=";

    private HttpClient m_client;

    private String value, filename;

    private HtmlCleaner m_cleaner = new HtmlCleaner();

    protected final PropertyChangeSupport m_propertyChangeSupport;

    public CoversRetriever(String value, String filename) {
        m_propertyChangeSupport = new PropertyChangeSupport(this);
        m_client = new HttpClient();
        this.value = value;
        this.filename = filename;
    }

    @Override
    public void run() {
        searchSynopsis();
    }

    private void searchSynopsis() {
        String targetReqId = "[ Retriever ] " + value;
        try {
            m_propertyChangeSupport.firePropertyChange("TARGET_REQUEST", "", targetReqId);
            String location = GOODREADS_WEB_PAGE + value;
            GetMethod searchPage = new GetMethod(location);
            m_client.executeMethod(searchPage);
            List<OptionRecord> list = processSearchResponse(searchPage);
            String linkToImage = getLinkToCoverImage(list);
            if (linkToImage != null) {
                GetMethod getImage = new GetMethod(linkToImage);
                m_client.executeMethod(getImage);
                InputStream imageStream = getImage.getResponseBodyAsStream();
                BufferedInputStream bufferedInput = new BufferedInputStream(imageStream);
                BufferedOutputStream bufferedOutput = new BufferedOutputStream(new FileOutputStream(filename));
                byte[] buffer = new byte[1024 * 16];
                int read = 0;
                while ((read = bufferedInput.read(buffer)) != -1) {
                    bufferedOutput.write(buffer, 0, read);
                }
                bufferedOutput.close();
                m_propertyChangeSupport.firePropertyChange(SUCCESS, "", targetReqId);
            } else {
                m_propertyChangeSupport.firePropertyChange(NOT_FOUND, "", value);
            }
        } catch (Exception e) {
            e.printStackTrace();
            m_propertyChangeSupport.firePropertyChange(FAIL, "", targetReqId);
        }
    }

    private String getLinkToCoverImage(List<OptionRecord> list) {
        String linkToCover = null;
        try {
            if (list.size() == 1) {
                linkToCover = list.get(0).getLink();
            } else if (list.size() > 1) {
                InteractionRequest request = new InteractionRequest("Help request", list);
                synchronized (request) {
                    m_propertyChangeSupport.firePropertyChange(NEEDS_CLARIFICATION, null, request);
                    request.wait();
                }
                linkToCover = request.getLink();
            }
        } catch (Exception e) {
            linkToCover = null;
        }
        return linkToCover;
    }

    private List<OptionRecord> processSearchResponse(GetMethod searchPage) throws IOException {
        List<OptionRecord> list = new Vector<OptionRecord>();
        TagNode node = m_cleaner.clean(searchPage.getResponseBodyAsStream(), ISO_8859_1);
        TagNode results = node.findElementByAttValue("class", "tableList", true, true);
        if (results != null) {
            List<TagNode> bookList = results.getElementListByName("tr", true);
            for (TagNode book : bookList) {
                TagNode titleNode = book.findElementByAttValue("class", "bookTitle", true, true);
                TagNode authorNode = book.findElementByAttValue("class", "authorName", true, true);
                TagNode imageLinkNode = book.findElementHavingAttribute("title", true);
                if (imageLinkNode != null) {
                    TagNode imageNode = imageLinkNode.findElementByName("img", true);
                    OptionRecord br = new OptionRecord();
                    String title = titleNode.getText().toString();
                    String author = authorNode.getText().toString();
                    br.setLabel(title + "-" + author);
                    String linkToCover = imageNode.getAttributeByName("src");
                    linkToCover = convertLinkToCover(linkToCover);
                    br.setLink(linkToCover);
                    list.add(br);
                }
            }
        }
        return list;
    }

    private String convertLinkToCover(String linkToCover) {
        int searchIndex = linkToCover.indexOf(HTTP_PHOTO_GOODREADS_COM_BOOKS);
        String result = linkToCover;
        if (searchIndex != -1) {
            String id = linkToCover.substring(HTTP_PHOTO_GOODREADS_COM_BOOKS.length());
            id = id.replace("s/", "l/");
            result = HTTP_PHOTO_GOODREADS_COM_BOOKS + id;
        }
        return result;
    }

    private void showHeaders(PostMethod loginRequest) {
        for (Header h : loginRequest.getRequestHeaders()) {
            System.out.println(h.getName() + "= " + h.getValue());
        }
        System.out.println("========================================");
        for (Header h : loginRequest.getResponseHeaders()) {
            System.out.println(h.getName() + "= " + h.getValue());
        }
        for (NameValuePair pair : loginRequest.getParameters()) {
            System.out.println(pair.getName() + "= " + pair.getValue());
        }
    }

    /**Add a property change listener for a specific property.
	  @param propertyName The name of the property to listen on.
	  @param listener The <code>PropertyChangeListener</code>
	      to be added.
	 */
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        m_propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public static void main(String[] args) {
        CoversRetriever sr = new CoversRetriever("delirio", "/home/randres/imagen.jpg");
        sr.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName() == CoversRetriever.NEEDS_CLARIFICATION) {
                    InteractionRequest ir = (InteractionRequest) evt.getNewValue();
                    StdinResponser resp = new StdinResponser(ir);
                    resp.start();
                }
            }
        });
        sr.start();
    }
}
