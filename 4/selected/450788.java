package org.pentahoadmin.PentahoAdmin.panels;

import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jdom.Document;
import org.pentahoadmin.PentahoAdmin.Exception.PentahoAdminXMLException;
import org.pentahoadmin.PentahoAdmin.config.ConfigLoader;
import org.pentahoadmin.PentahoAdmin.util.FileUtils;
import org.pentahoadmin.PentahoAdmin.util.JdomReader;
import org.pentahoadmin.PentahoAdmin.util.JdomWriter;

public class Passwords extends Composite {

    private Text passwordText;

    private List<Object> elements;

    private Document doc;

    private String seperator;

    private String filename;

    /**
	 * @param parent
	 * @param style
	 * @throws PentahoAdminXMLException 
	 */
    public Passwords(Composite parent, int style) throws PentahoAdminXMLException {
        super(parent, style);
        final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        setLayout(gridLayout);
        final Label passwordLabel = new Label(this, SWT.NONE);
        passwordLabel.setText("Publish Password");
        passwordText = new Text(this, SWT.BORDER | SWT.PASSWORD);
        final GridData gd_hostNametext = new GridData(SWT.FILL, SWT.CENTER, true, false);
        passwordText.setLayoutData(gd_hostNametext);
        init();
        final Composite composite = new Composite(this, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        composite.setLayout(new FillLayout());
        final Button button = new Button(composite, SWT.NONE);
        button.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(final SelectionEvent arg0) {
                saveFile();
            }
        });
        button.setText("Save User Modified File");
        final Button button_1 = new Button(composite, SWT.NONE);
        button_1.setText("Revert to Original");
    }

    private void init() throws PentahoAdminXMLException {
        boolean fileopened = loadFile();
        if (fileopened == true) {
            elements = JdomReader.getChildElements(doc);
            setPasswordText(JdomReader.getNodeWithTagValue(elements, "publisher-password"));
        }
    }

    private boolean loadFile() throws PentahoAdminXMLException {
        seperator = System.getProperty("file.separator");
        boolean fileopened = false;
        if (ConfigLoader.props.size() != 0) filename = ConfigLoader.props.get(1) + seperator + "system" + seperator + "publisher_config.xml";
        try {
            doc = JdomReader.loadXMLFile(filename);
            fileopened = true;
        } catch (Exception e) {
            System.out.println("File not found");
        }
        return fileopened;
    }

    private void saveFile() {
        FileUtils.copyFile(filename, filename + ".bak");
        JdomWriter.setNodeWithTagValue(elements, "publisher-password", getPasswordText());
        JdomWriter.saveXML(filename, doc);
    }

    private String getPasswordText() {
        return passwordText.getText();
    }

    private void setPasswordText(String password) {
        if (password != null) passwordText.setText(password);
    }
}
