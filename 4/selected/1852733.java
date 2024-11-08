package com.izforge.izpack.panels;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import com.izforge.izpack.gui.IzPanelLayout;
import com.izforge.izpack.gui.LabelFactory;
import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.installer.InstallerFrame;
import com.izforge.izpack.installer.IzPanel;
import com.izforge.izpack.installer.ResourceManager;
import com.izforge.izpack.util.HyperlinkHandler;

/**
 * The HTML info panel.
 *
 * @author Julien Ponge
 */
public class HTMLInfoPanel extends IzPanel {

    private static final long serialVersionUID = 3257008769514025270L;

    /** Resource prefix for panel. */
    protected String panelResourcePrefixStr;

    /** Resource name for panel content. */
    protected String panelResourceNameStr;

    /**
     * The text area.
     */
    private JEditorPane textArea;

    /**
     * The constructor.
     *
     * @param parent The parent.
     * @param idata  The installation data.
     */
    public HTMLInfoPanel(InstallerFrame parent, InstallData idata) {
        this(parent, idata, "HTMLInfoPanel", true);
    }

    /**
     * Alternate constructor with additional parameters.  For use with
     * subclasses.
     *
     * @param parent The parent.
     * @param idata  The installation data.
     * @param resPrefixStr prefix string for content resource name.
     * @param showInfoLabelFlag true to show "please read..." label
     * above content.
     */
    public HTMLInfoPanel(InstallerFrame parent, InstallData idata, String resPrefixStr, boolean showInfoLabelFlag) {
        super(parent, idata, new IzPanelLayout());
        panelResourcePrefixStr = resPrefixStr;
        panelResourceNameStr = resPrefixStr + ".info";
        if (showInfoLabelFlag) {
            add(LabelFactory.create(parent.langpack.getString("InfoPanel.info"), parent.icons.getImageIcon("edit"), LEADING), NEXT_LINE);
        }
        try {
            textArea = new JEditorPane() {

                protected InputStream getStream(URL urlObj) throws IOException {
                    final InputStream inStm = super.getStream(urlObj);
                    final ByteArrayOutputStream btArrOutStm = new ByteArrayOutputStream();
                    int b;
                    final byte[] buff = new byte[2048];
                    while ((b = inStm.read(buff, 0, buff.length)) > 0) btArrOutStm.write(buff, 0, b);
                    final String parsedStr = parseText(btArrOutStm.toString());
                    return new ByteArrayInputStream(parsedStr.getBytes());
                }
            };
            textArea.setContentType("text/html; charset=utf-8");
            textArea.setEditable(false);
            textArea.addHyperlinkListener(new HyperlinkHandler());
            JScrollPane scroller = new JScrollPane(textArea);
            textArea.setPage(loadHTMLInfoContent());
            textArea.setCaretPosition(0);
            add(scroller, NEXT_LINE);
        } catch (Exception err) {
            err.printStackTrace();
        }
        getLayoutHelper().completeLayout();
    }

    private URL loadHTMLInfoContent() {
        if (getMetadata() != null && getMetadata().getPanelid() != null) {
            try {
                String panelSpecificResName = panelResourcePrefixStr + '.' + this.getMetadata().getPanelid();
                String panelspecificResContent = ResourceManager.getInstance().getTextResource(panelSpecificResName);
                if (panelspecificResContent != null) {
                    panelResourceNameStr = panelSpecificResName;
                }
            } catch (Exception e) {
            }
        }
        try {
            return ResourceManager.getInstance().getURL(panelResourceNameStr);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Indicates wether the panel has been validated or not.
     *
     * @return Always true.
     */
    public boolean isValidated() {
        return true;
    }

    public void panelActivate() {
        try {
            textArea.setPage(loadHTMLInfoContent());
            textArea.setCaretPosition(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
