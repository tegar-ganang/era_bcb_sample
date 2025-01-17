package org.oboedit.gui.components.ontologyGeneration.extraction;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;
import javax.swing.JTable;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.ElementIterator;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import org.apache.log4j.Logger;
import org.jdesktop.swingworker.SwingWorker;
import org.oboedit.gui.components.ontologyGeneration.CandidateDefinition;
import org.oboedit.gui.components.ontologyGeneration.ProxyInfo;

/**
 * Tries to extend an existing definition by looking up its source HTML content.
 * If no extension could be found, the definition is left as it is.
 * 
 * @author Thomas Waechter (<href>waechter@biotec.tu-dresden.de</href>), 2010
 */
public class DefinitionExtensionWorker extends SwingWorker<Boolean, Object> {

    public static final String WAS_EXTENDED_PROPERTY = "wasExtended";

    private static final Logger logger = Logger.getLogger(DefinitionExtensionWorker.class);

    private CandidateDefinition definition;

    private final JTable table;

    public DefinitionExtensionWorker(CandidateDefinition definition, JTable table) {
        this.definition = definition;
        this.table = table;
    }

    @Override
    public Boolean doInBackground() {
        logger.trace("try to extend definition:\t" + this.definition.getDefinition());
        if (definitionExtraction()) {
            logger.trace("new extended definition:\t" + this.definition.getDefinition());
            return true;
        }
        return false;
    }

    @Override
    protected void done() {
        table.repaint();
        table.firePropertyChange(WAS_EXTENDED_PROPERTY, false, true);
    }

    /**
	 * Tries to extend an existing definition by looking up its source html
	 * content. If no extension could be found, the definition is left as it is.
	 * 
	 * @return true if extended, false otherwise
	 */
    private boolean definitionExtraction() {
        if (definition == null || definition.getCachedURLs() == null || definition.getCachedURLs().isEmpty()) {
            return false;
        }
        URL url;
        try {
            if (definition.getCachedURLs().get(0).endsWith("pdf") || definition.getCachedURLs().get(0).endsWith("ppt") || definition.getCachedURLs().get(0).endsWith("doc")) {
                logger.error("Definitions from files not in HTML are not extended.");
                return false;
            }
            url = new URL(definition.getCachedURLs().get(0));
            HTMLEditorKit kit = new HTMLEditorKit();
            HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
            doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
            URLConnection con = url.openConnection();
            con.setConnectTimeout(1000);
            if (System.getProperty(ProxyInfo.PROXY_HOST) != null) {
                final String proxyUser = System.getProperty(ProxyInfo.PROXY_USERNAME);
                final String proxyPassword = System.getProperty(ProxyInfo.PROXY_PASSWORD);
                Authenticator.setDefault(new Authenticator() {

                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(proxyUser, proxyPassword.toCharArray());
                    }
                });
            }
            con.connect();
            Reader HTMLReader = new InputStreamReader(con.getInputStream());
            kit.read(HTMLReader, doc, 0);
            String def = definition.getDefinition();
            if (def.endsWith("...")) {
                def = def.substring(0, def.length() - 3);
            }
            if (def.contains("[") || def.contains("]")) {
                StringTokenizer tokenizer = new StringTokenizer(def);
                String token;
                def = "";
                while (tokenizer.hasMoreTokens()) {
                    token = tokenizer.nextToken();
                    if (token.contains("[")) {
                        token = token.replace('[', ' ').trim();
                    } else if (token.contains("]")) {
                        token = token.replace(']', ' ').trim();
                    }
                    def = def + " " + token;
                }
            }
            ElementIterator iter = new ElementIterator(doc);
            Element elem;
            boolean run = true;
            while (run) {
                elem = iter.next();
                if (elem == null) {
                    run = false;
                    continue;
                }
                if (elem.getName().equals("content")) {
                    String element = elem.getDocument().getText(0, elem.getDocument().getLength() - 1);
                    int defLength = def.length();
                    if (defLength > 2 && element.contains(def.subSequence(0, defLength - 2))) {
                        int begin = element.indexOf(def.substring(0, defLength - 2));
                        boolean extending = true;
                        int i = begin + defLength - 2;
                        while (extending) {
                            i++;
                            if (element.charAt(i) == '.') {
                                extending = false;
                                String newDef = element.subSequence(begin, i) + ".";
                                if (def.equals(newDef)) {
                                    return false;
                                } else {
                                    definition.setDefinition(newDef);
                                    generateHTMLFormattedDefinition();
                                    return true;
                                }
                            }
                        }
                        break;
                    }
                }
            }
        } catch (MalformedURLException e) {
            return false;
        } catch (SocketTimeoutException e) {
            logger.error("Timeout during connection.", e);
            return false;
        } catch (IOException e) {
            return false;
        } catch (BadLocationException e) {
            return false;
        } catch (RuntimeException e) {
            logger.error("Error during fetching extended definitions. Definition is skipped.");
            return false;
        }
        return false;
    }

    /**
	 * Adds the extended definition to the already existing HTML formatted
	 * definition.
	 */
    private void generateHTMLFormattedDefinition() {
        String htmlFormatted = "";
        String originalHTMLDefSubStr = "";
        try {
            String originalHTMLDef = definition.getDefinitionHTMLFormatted().substring(0, definition.getDefinitionHTMLFormatted().length() - 7);
            originalHTMLDefSubStr = originalHTMLDef.substring(0, originalHTMLDef.lastIndexOf(">") + 1);
        } catch (IndexOutOfBoundsException e) {
            logger.error("HTML formatted Definition could not be generated", e);
            return;
        }
        int numberOfCharsRead = 0;
        boolean openedTag = false;
        for (int j = 0; j < originalHTMLDefSubStr.length(); j++) {
            if (!openedTag) {
                if (originalHTMLDefSubStr.charAt(j) == '<') {
                    openedTag = true;
                } else {
                    numberOfCharsRead++;
                }
            } else {
                if (originalHTMLDefSubStr.charAt(j) == '>') {
                    openedTag = false;
                }
            }
        }
        htmlFormatted += originalHTMLDefSubStr;
        htmlFormatted += definition.getDefinition().substring(numberOfCharsRead);
        htmlFormatted += "</html>";
        definition.setDefinitionHTMLFormatted(htmlFormatted);
    }
}
