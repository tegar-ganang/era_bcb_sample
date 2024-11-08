package org.xaware.ide.xadev.tools.gui.xmljsonconverter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.xaware.ide.shared.UserPrefs;
import org.xaware.ide.xadev.XA_Designer_Plugin;
import org.xaware.ide.xadev.common.ControlFactory;
import org.xaware.ide.xadev.common.ResourceUtils;
import org.xaware.ide.xadev.common.XML_JSON_Converter;
import org.xaware.ide.xadev.gui.XAChooser;
import org.xaware.ide.xadev.gui.XADialog;
import org.xaware.ide.xadev.gui.XAFileConstants;
import org.xaware.ide.xadev.gui.sourceViewer.XMLStructuredViewer;
import org.xaware.shared.i18n.Translator;
import org.xaware.shared.util.FileUtils;
import org.xaware.shared.util.XAwareException;

/**
 * Conversion tool used to convert XML to JSON and vice versa. This tool also provides options for importing the content
 * from file, exporting content to the file, copying the content to the clip board.
 * 
 * @author blueAlly
 * 
 */
public class XMLJSONConverterDlg extends XADialog implements SelectionListener {

    /** Reference to instance of XA_Designer_Plugin translator */
    private final Translator translator = Translator.getInstance();

    /** Parent sash form which holds the controls */
    private Composite parentComposite;

    /** Source viewer for XML content */
    private SourceViewer xmlSourceViewer;

    /** Text field for JSON content */
    private Text jsonText;

    /** Button to convert from XML to JSON */
    private Button convertXMLToJSONBtn;

    /** Button to convert from JSON to XML */
    private Button convertJSONToXMLBtn;

    /** Tool item to import XML content */
    private ToolItem xmlImportToolItem;

    /** Tool item to export XML content */
    private ToolItem xmlExportToolItem;

    /** Tool item to copy XML content to the clip board */
    private ToolItem xmlCopyToolItem;

    /** Tool item to import JSON content */
    private ToolItem jsonImportToolItem;

    /** Tool item to export JSON content */
    private ToolItem jsonExportToolItem;

    /** Tool item to copy JSON content to the clip board */
    private ToolItem jsonCopyToolItem;

    /**
     * @param shell
     * @param title
     */
    public XMLJSONConverterDlg(Shell shell, String title) {
        super(shell, null, null, title, true, false, new Point(850, 600));
        setOverrideGivenSize(false);
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#createContents(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        setToShow(createGUIControls(parent));
        final Control parentControl = super.createContents(parent);
        getButton(IDialogConstants.OK_ID).setText("Close");
        return parentControl;
    }

    /**
     * Creates the GUI controls.
     */
    private Composite createGUIControls(Composite parent) {
        parentComposite = new Composite(parent, SWT.NONE);
        parentComposite.setLayout(new GridLayout(3, false));
        parentComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        Composite xmlComposite = new Composite(parentComposite, SWT.NONE);
        xmlComposite.setLayout(new GridLayout(2, false));
        xmlComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        Label xmlLabel = new Label(xmlComposite, SWT.NONE);
        xmlLabel.setText(translator.getString("Enter XML content:"));
        ToolBar xmlToolBar = new ToolBar(xmlComposite, SWT.FLAT);
        xmlToolBar.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        xmlImportToolItem = new ToolItem(xmlToolBar, SWT.NONE);
        xmlImportToolItem.setImage(UserPrefs.getImageIconFor(UserPrefs.OPEN_FILE));
        xmlImportToolItem.setToolTipText(translator.getString("Import XML content from a file"));
        xmlImportToolItem.addSelectionListener(this);
        xmlExportToolItem = new ToolItem(xmlToolBar, SWT.NONE);
        xmlExportToolItem.setImage(UserPrefs.getImageIconFor(UserPrefs.SAVE_FILE));
        xmlExportToolItem.setToolTipText(translator.getString("Export XML content to a file"));
        xmlExportToolItem.addSelectionListener(this);
        xmlCopyToolItem = new ToolItem(xmlToolBar, SWT.NONE);
        xmlCopyToolItem.setImage(UserPrefs.getImageIconFor(UserPrefs.COPY));
        xmlCopyToolItem.setToolTipText(translator.getString("Copy XML content to the clip board"));
        xmlCopyToolItem.addSelectionListener(this);
        xmlSourceViewer = XMLStructuredViewer.getInstance().createViewer(xmlComposite, "");
        GridData xmlSourceViewerData = new GridData(GridData.FILL_BOTH);
        xmlSourceViewerData.horizontalSpan = 2;
        xmlSourceViewer.getTextWidget().setLayoutData(xmlSourceViewerData);
        xmlSourceViewer.getTextWidget().forceFocus();
        Composite convertBtnComposite = new Composite(parentComposite, SWT.NONE);
        convertBtnComposite.setLayout(new GridLayout(1, false));
        convertXMLToJSONBtn = new Button(convertBtnComposite, SWT.NONE);
        convertXMLToJSONBtn.setImage(UserPrefs.getImageIconFor("ServiceWizardNext"));
        convertXMLToJSONBtn.setToolTipText("Convert from XML to JSON");
        convertXMLToJSONBtn.addSelectionListener(this);
        convertJSONToXMLBtn = new Button(convertBtnComposite, SWT.NONE);
        convertJSONToXMLBtn.setImage(UserPrefs.getImageIconFor("ServiceWizardBack"));
        convertJSONToXMLBtn.setToolTipText("Convert from JSON to XML");
        convertJSONToXMLBtn.addSelectionListener(this);
        Composite jsonComposite = new Composite(parentComposite, SWT.NONE);
        jsonComposite.setLayout(new GridLayout(2, false));
        jsonComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        Label jsonLabel = new Label(jsonComposite, SWT.NONE);
        jsonLabel.setText(translator.getString("Enter JSON content:"));
        ToolBar jsonToolBar = new ToolBar(jsonComposite, SWT.FLAT);
        jsonToolBar.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        jsonImportToolItem = new ToolItem(jsonToolBar, SWT.NONE);
        jsonImportToolItem.setImage(UserPrefs.getImageIconFor(UserPrefs.OPEN_FILE));
        jsonImportToolItem.setToolTipText(translator.getString("Import JSON content from a file"));
        jsonImportToolItem.addSelectionListener(this);
        jsonExportToolItem = new ToolItem(jsonToolBar, SWT.NONE);
        jsonExportToolItem.setImage(UserPrefs.getImageIconFor(UserPrefs.SAVE_FILE));
        jsonExportToolItem.setToolTipText(translator.getString("Export JSON content to a file"));
        jsonExportToolItem.addSelectionListener(this);
        jsonCopyToolItem = new ToolItem(jsonToolBar, SWT.NONE);
        jsonCopyToolItem.setImage(UserPrefs.getImageIconFor(UserPrefs.COPY));
        jsonCopyToolItem.setToolTipText(translator.getString("Copy JSON content to the clip board"));
        jsonCopyToolItem.addSelectionListener(this);
        jsonText = new Text(jsonComposite, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        GridData jsonSourceViewerData = new GridData(GridData.FILL_BOTH);
        jsonSourceViewerData.horizontalSpan = 2;
        jsonText.setLayoutData(jsonSourceViewerData);
        return parentComposite;
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.xaware.ide.xadev.gui.XADialog#initializeBounds()
     */
    @Override
    protected void initializeBounds() {
        super.initializeBounds();
        XA_Designer_Plugin.alignDialogToCenter(getShell());
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
     */
    public void widgetDefaultSelected(SelectionEvent e) {
        widgetSelected(e);
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
     */
    public void widgetSelected(SelectionEvent event) {
        if (event.getSource() == xmlImportToolItem) {
            try {
                String xmlContent = getContentFromFile(XAFileConstants.XML);
                if (xmlContent != null) {
                    xmlSourceViewer.getTextWidget().setText(xmlContent);
                }
            } catch (IOException exception) {
                ControlFactory.showStackTrace("Error while importing the XML content:", exception);
                xmlSourceViewer.getTextWidget().setText("");
            }
        } else if (event.getSource() == xmlExportToolItem) {
            String xmlContent = xmlSourceViewer.getTextWidget().getText();
            if (xmlContent.trim().length() == 0) {
                ControlFactory.showMessageDialog("XML content is empty to export to the file.", "Information");
                xmlSourceViewer.getTextWidget().forceFocus();
            } else {
                try {
                    saveContentsToFile(XAFileConstants.XML, xmlContent);
                } catch (IOException exception) {
                    ControlFactory.showStackTrace("Error while exporting XML content to the file:", exception);
                }
            }
        } else if (event.getSource() == xmlCopyToolItem) {
            String xmlContent = xmlSourceViewer.getTextWidget().getText();
            if (xmlContent.trim().length() == 0) {
                ControlFactory.showMessageDialog("XML content is empty to copy to the clip board.", "Information");
                xmlSourceViewer.getTextWidget().forceFocus();
            } else {
                copyToClipBoard(xmlContent);
            }
        } else if (event.getSource() == convertXMLToJSONBtn) {
            String xmlContent = xmlSourceViewer.getTextWidget().getText();
            if (xmlContent.trim().length() == 0) {
                ControlFactory.showMessageDialog("Please enter XML content to convert to JSON.", "Information");
                xmlSourceViewer.getTextWidget().forceFocus();
            } else {
                try {
                    String jsonContent = XML_JSON_Converter.convertXMLToJSON(xmlContent);
                    jsonText.setText(jsonContent);
                } catch (XAwareException exception) {
                    ControlFactory.showStackTrace("Error while converting the XML content to JSON", exception);
                    jsonText.setText("");
                }
            }
        } else if (event.getSource() == jsonImportToolItem) {
            try {
                String jsonContent = getContentFromFile(XAFileConstants.JSON);
                if (jsonContent != null) {
                    jsonText.setText(jsonContent);
                }
            } catch (IOException exception) {
                ControlFactory.showStackTrace("Error while importing the JSON content:", exception);
                jsonText.setText("");
            }
        } else if (event.getSource() == jsonExportToolItem) {
            String jsonContent = jsonText.getText();
            if (jsonContent.trim().length() == 0) {
                ControlFactory.showMessageDialog("JSON content is empty to export to the file.", "Information");
                jsonText.forceFocus();
            } else {
                try {
                    saveContentsToFile(XAFileConstants.JSON, jsonContent);
                } catch (IOException exception) {
                    ControlFactory.showStackTrace("Error while exporting JSON content to the file:", exception);
                }
            }
        } else if (event.getSource() == jsonCopyToolItem) {
            String jsonContent = jsonText.getText();
            if (jsonContent.trim().length() == 0) {
                ControlFactory.showMessageDialog("JSON content is empty to copy to the clip board.", "Information");
                jsonText.forceFocus();
            } else {
                copyToClipBoard(jsonContent);
            }
        } else if (event.getSource() == convertJSONToXMLBtn) {
            String jsonContent = jsonText.getText();
            if (jsonContent.trim().length() == 0) {
                ControlFactory.showMessageDialog("Please enter JSON content to convert to XML.", "Information");
                jsonText.forceFocus();
            } else {
                try {
                    String xmlContent = XML_JSON_Converter.convertJsonToXML(jsonContent);
                    xmlSourceViewer.getTextWidget().setText(xmlContent);
                } catch (XAwareException exception) {
                    ControlFactory.showStackTrace("Error while converting the JSON content to XML", exception);
                    xmlSourceViewer.getTextWidget().setText("");
                }
            }
        }
    }

    /**
     * Copies the given content to the clip board so that it can be used any where by pasting it.
     * 
     * @param content
     *            content to be copied to the clip board.
     */
    private void copyToClipBoard(String content) {
        XA_Designer_Plugin.getClipboard().setContents(new String[] { content }, new Transfer[] { TextTransfer.getInstance() });
    }

    /**
     * This method opens the chooser with the given default filter to select the file. Once the file is selected, it
     * returns the contents of the selected file.
     * 
     * @param defaultFilter
     *            default filter
     * @return file content as string, if the valid file is selected, null otherwise.
     * @throws IOException
     *             thrown if the error occurs while getting the contents from the file.
     */
    private String getContentFromFile(int defaultFilter) throws IOException {
        String content = null;
        final XAChooser chooser = new XAChooser(Display.getCurrent().getActiveShell(), "", SWT.OPEN);
        chooser.addDefaultFilter(defaultFilter);
        chooser.addFilter(XAFileConstants.ALL_ONLY);
        final String selectedFilePath = chooser.open();
        if (selectedFilePath != null) {
            content = FileUtils.getFileContentsAsString(ResourceUtils.getFile(selectedFilePath));
        }
        return content;
    }

    /**
     * This method opens the XAware Save File dialog to save the given content to the file.
     * 
     * @param defaultFilter
     *            default filter.
     * @param content
     *            content to be saved to the file.
     * @throws IOException
     *             thrown if the error occurs while copying the contents to the file.
     */
    private void saveContentsToFile(int defaultFilter, String content) throws IOException {
        final XAChooser chooser = new XAChooser(Display.getCurrent().getActiveShell(), "", SWT.SAVE);
        chooser.addDefaultFilter(defaultFilter);
        chooser.addFilter(XAFileConstants.ALL_ONLY);
        final String selectedFilePath = chooser.open();
        if (selectedFilePath != null) {
            FileUtils.copyFile(new ByteArrayInputStream(content.getBytes()), ResourceUtils.getFile(selectedFilePath));
        }
    }
}
