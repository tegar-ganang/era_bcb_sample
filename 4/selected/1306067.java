package org.anuta.xmltv.grabber.tvgidsnl;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.anuta.xmltv.beans.Channel;
import org.anuta.xmltv.beans.Program;
import org.anuta.xmltv.beans.Rating;
import org.anuta.xmltv.beans.RatingMapper;
import org.anuta.xmltv.grabber.EPGGrabber;
import org.anuta.xmltv.transport.Transport;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.w3c.dom.Document;
import com.Oustermiller.util.StringHelper;

public abstract class AbstractGrabber implements EPGGrabber {

    private static final Log log = LogFactory.getLog(AbstractGrabber.class);

    private static final String INFO_XSL = "/info.xsl";

    private static final String DESCRIPTION_XSL = "/description.xsl";

    private static final String TITLE_XSL = "/title.xsl";

    private String xsltPath = "src/main/xslt";

    private Map ganreMapping = new HashMap();

    private String tvgidsurl = "http://www.tvgids.nl";

    private String listingQuery = "/json/lists/programs.php?channels={channel}&day={day}";

    private RatingMapper ratingMapper;

    private Transport transport;

    private Map roleMapping = new HashMap();

    private String noData = "Over dit programma zijn geen detailgegevens bekend.";

    private String xmltvSuffix = "";

    public void setGanreMapping(Map ganreMapping) {
        this.ganreMapping = ganreMapping;
    }

    public Map getGanreMapping() {
        return ganreMapping;
    }

    public final Transport getTransport() {
        return transport;
    }

    public final void setTransport(final Transport transport) {
        this.transport = transport;
    }

    public final String getMappedGanre(final String ganre) {
        if (getGanreMapping().containsKey(ganre.toLowerCase())) {
            return (String) getGanreMapping().get(ganre.toLowerCase());
        } else {
            return ganre;
        }
    }

    public final String getMappedRole(final String role) {
        if (getRoleMapping().containsKey(role.toLowerCase())) {
            return (String) getRoleMapping().get(role.toLowerCase());
        } else {
            return role;
        }
    }

    public final Map getRoleMapping() {
        return roleMapping;
    }

    public final void setRoleMapping(final Map roleMapping) {
        this.roleMapping = roleMapping;
    }

    public final String getTvgidsurl() {
        return tvgidsurl;
    }

    public final void setTvgidsurl(final String tvgidsurl) {
        this.tvgidsurl = tvgidsurl;
    }

    public final RatingMapper getRatingMapper() {
        return ratingMapper;
    }

    public final void setRatingMapper(final RatingMapper ratingMapper) {
        this.ratingMapper = ratingMapper;
    }

    public void setListingQuery(String listingQuery) {
        this.listingQuery = listingQuery;
    }

    public String getListingQuery() {
        return listingQuery;
    }

    protected String buildListUrl(final Channel channel, final int day) {
        String dayString = String.valueOf(day);
        String pageUrl = StringUtils.replaceEach(getTvgidsurl() + getListingQuery(), new String[] { "{day}", "{channel}" }, new String[] { dayString, channel.getChannelId() });
        return pageUrl;
    }

    protected void fixProgramTimes(List<Program> programs) {
        if (programs.size() > 1) {
            for (int i = 0; i < programs.size() - 1; i++) {
                programs.get(i).setEndDate(programs.get(i + 1).getStartDate());
            }
        }
    }

    /**
	 * Extract text from xml.
	 * 
	 * @param xml
	 * @param xslTemplate
	 * @return extracted text
	 */
    private String extractText(final String xml, final String xslTemplate) {
        try {
            log.debug("XML: " + xml);
            javax.xml.transform.TransformerFactory tFactory = javax.xml.transform.TransformerFactory.newInstance();
            javax.xml.transform.Source stylesheet = new StreamSource(new FileInputStream(getXsltPath() + xslTemplate));
            Transformer transformer = tFactory.newTransformer(stylesheet);
            StringWriter sw = new StringWriter();
            transformer.transform(new javax.xml.transform.stream.StreamSource(new ByteArrayInputStream(xml.getBytes("UTF-8"))), new StreamResult(sw));
            String ret = sw.toString().trim();
            ret = StringHelper.unescapeHTML(ret);
            log.debug("RETUS: " + ret);
            return ret;
        } catch (FileNotFoundException e) {
            if (log.isErrorEnabled()) {
                log.error("Error in xslt transformation", e);
            }
            return null;
        } catch (TransformerConfigurationException e) {
            if (log.isErrorEnabled()) {
                log.error("Error in xslt transformation", e);
            }
            return null;
        } catch (UnsupportedEncodingException e) {
            if (log.isErrorEnabled()) {
                log.error("Error in xslt transformation", e);
            }
            return null;
        } catch (TransformerFactoryConfigurationError e) {
            if (log.isErrorEnabled()) {
                log.error("Error in xslt transformation", e);
            }
            return null;
        } catch (TransformerException e) {
            if (log.isErrorEnabled()) {
                log.error("Error in xslt transformation", e);
            }
            return null;
        }
    }

    /**
	 * Convert tag node to xml.
	 * 
	 * @param tn
	 * @return
	 */
    private String getXml(final TagNode tn) {
        try {
            HtmlCleaner cleaner = new HtmlCleaner();
            CleanerProperties props = cleaner.getProperties();
            props.setOmitComments(true);
            props.setOmitUnknownTags(true);
            props.setTranslateSpecialEntities(true);
            props.setAllowHtmlInsideAttributes(false);
            DomSerializer myDom = new DomSerializer(props, false);
            Document doc = myDom.createDOM(tn);
            TransformerFactory tranFactory = TransformerFactory.newInstance();
            Transformer aTransformer = tranFactory.newTransformer();
            Source src = new DOMSource(doc);
            Writer outWriter = new StringWriter();
            Result dest = new StreamResult(outWriter);
            aTransformer.transform(src, dest);
            String xml = outWriter.toString();
            xml = xml.replace("&#13;", "");
            xml = xml.replace("&#10;", "");
            xml = xml.replace('\n', ' ');
            xml = xml.replace('\r', ' ');
            xml = xml.replace('\t', ' ');
            return xml;
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Error in xml serialization", e);
            }
            return null;
        }
    }

    protected void grabFromDetailPage(String html, Program p) {
        HtmlCleaner cleaner = new HtmlCleaner();
        CleanerProperties props = cleaner.getProperties();
        props.setOmitComments(true);
        props.setOmitUnknownTags(true);
        props.setTranslateSpecialEntities(true);
        props.setAllowHtmlInsideAttributes(false);
        TagNode node = cleaner.clean(html);
        TagNode body = node.findElementByName("body", true);
        String xml = getXml(body);
        String description = extractText(xml, DESCRIPTION_XSL);
        String longTitle = extractText(xml, TITLE_XSL);
        p.setDescription(description);
        if ((longTitle == null) || (longTitle.trim().length() == 0)) {
            p.setLongTitle(p.getTitle());
        } else {
            p.setLongTitle(longTitle.trim());
        }
        String info = extractText(xml, INFO_XSL);
        if (info != null) {
            if (log.isDebugEnabled()) {
                log.debug(info);
            }
            StringTokenizer st = new StringTokenizer(info, "|^|");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (token == null) {
                    continue;
                } else {
                    token = token.trim();
                }
                if (log.isDebugEnabled()) {
                    log.debug("Found token: " + token);
                }
                int pos = token.indexOf(':');
                if (pos == -1) {
                    continue;
                }
                String key = token.substring(0, pos).trim().toLowerCase();
                String value = token.substring(pos + 1, token.length()).trim();
                if (key.length() == 0) {
                    continue;
                }
                if ("genre".equalsIgnoreCase(key)) {
                    if (value.length() == 0) {
                        value = "Overige";
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Found genre: " + value);
                    }
                    p.setGanre(value);
                } else if ("acteurs".equals(key)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Found acteurs: " + value);
                    }
                    p.setActors(value);
                } else if ("jaar van premiere".equals(key)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Found premiere: " + value);
                    }
                    p.setPremiere(value);
                } else if ("titel aflevering".equals(key)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Found sub title: " + value);
                    }
                    p.setSubTitle(value);
                } else if ("presentatie".equals(key)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Found presentors: " + value);
                    }
                    p.setPresentors(value);
                } else if ("bijzonderheden".equals(key)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Found specials: " + value);
                    }
                    p.setSpecials(value);
                } else if ("regisseur".equals(key)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Found director: " + value);
                    }
                    p.setDirectors(value);
                } else if ("kijkwijzer".equals(key)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Found kijkwijzer picture: " + value);
                    }
                    String src = "";
                    String text = "";
                    int spos = value.indexOf(" ");
                    if (spos == -1) {
                        src = value;
                    } else {
                        src = value.substring(0, spos).trim();
                        text = value.substring(spos + 1, value.length()).trim();
                        if (getRatingMapper() != null) {
                            if ((src != null) && (text != null)) {
                                Rating rat = new Rating();
                                rat.setSystem(getRatingMapper().getSystem());
                                int ls = src.lastIndexOf('/');
                                if ((ls != -1) && (ls < src.length())) {
                                    src = src.substring(ls + 1);
                                    rat.setIcon(src);
                                    rat.setValue(getRatingMapper().mapRating(text));
                                    p.getRating().add(rat);
                                }
                            }
                        }
                    }
                }
            }
        }
        p.setFullyLoaded(true);
    }

    public final String getXsltPath() {
        return xsltPath;
    }

    public void setXsltPath(String xsltPath) {
        this.xsltPath = xsltPath;
    }

    public final String getNoData() {
        return noData;
    }

    public final void setNoData(final String noData) {
        this.noData = noData;
    }

    public final String getMappedChannelId(final String channelId) {
        StringBuffer sb = new StringBuffer();
        sb.append(channelId);
        sb.append(getXmltvSuffix());
        return sb.toString();
    }

    public final String getXmltvSuffix() {
        return xmltvSuffix;
    }

    public final void setXmltvSuffix(final String xmltvSuffix) {
        this.xmltvSuffix = xmltvSuffix;
    }
}
