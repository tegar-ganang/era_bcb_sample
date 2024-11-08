package com.protocol7.casilda.editors;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;
import com.protocol7.casilda.model.BinaryMessageData;
import com.protocol7.casilda.model.MessageData;

public class DataPage extends Composite {

    private Text dataText = null;

    private Button loadFromFileButton = null;

    private BinaryMessageData messageData = new BinaryMessageData();

    public DataPage(Composite parent, int style) {
        super(parent, style);
        initialize();
    }

    private void initialize() {
        GridData gridData1 = new GridData();
        gridData1.horizontalAlignment = GridData.CENTER;
        gridData1.verticalAlignment = GridData.BEGINNING;
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        gridData.verticalAlignment = GridData.FILL;
        dataText = new Text(this, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        dataText.setLayoutData(gridData);
        dataText.addModifyListener(new org.eclipse.swt.events.ModifyListener() {

            public void modifyText(org.eclipse.swt.events.ModifyEvent e) {
                messageData.setBinaryData(dataText.getText().getBytes());
            }
        });
        loadFromFileButton = new Button(this, SWT.NONE);
        loadFromFileButton.setText("Load from file...");
        loadFromFileButton.setLayoutData(gridData1);
        loadFromFileButton.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {

            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                FileDialog fileDialog = new FileDialog(getShell());
                String filePath = fileDialog.open();
                FileInputStream fis = null;
                ByteArrayOutputStream baos = null;
                if (filePath != null) {
                    try {
                        fis = new FileInputStream(filePath);
                        baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int read = fis.read(buffer, 0, 1024);
                        while (read > -1) {
                            baos.write(buffer, 0, read);
                            read = fis.read(buffer, 0, 1024);
                        }
                        messageData.setBinaryData(baos.toByteArray());
                        initFromMessageData();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    } finally {
                        closeQuitely(fis);
                        closeQuitely(baos);
                    }
                }
            }
        });
        this.setLayout(gridLayout);
        setSize(new Point(825, 419));
    }

    protected void closeQuitely(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }

    protected void closeQuitely(OutputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }

    public MessageData getMessageData() {
        return messageData;
    }

    public void setMessageData(MessageData messageData) {
        this.messageData = (BinaryMessageData) messageData;
        initFromMessageData();
    }

    private void initFromMessageData() {
        dataText.setText(new String(messageData.getBinaryData()));
    }
}
