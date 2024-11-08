package com.sun.activation.viewers;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.beans.*;
import javax.activation.*;

public class TextEditor extends Panel implements CommandObject, ActionListener {

    private TextArea text_area = null;

    private GridBagLayout panel_gb = null;

    private Panel button_panel = null;

    private Button save_button = null;

    private File text_file = null;

    private String text_buffer = null;

    private InputStream data_ins = null;

    private FileInputStream fis = null;

    private DataHandler _dh = null;

    private boolean DEBUG = false;

    /**
	 * Constructor
	 */
    public TextEditor() {
        panel_gb = new GridBagLayout();
        setLayout(panel_gb);
        button_panel = new Panel();
        button_panel.setLayout(new FlowLayout());
        save_button = new Button("SAVE");
        button_panel.add(save_button);
        addGridComponent(this, button_panel, panel_gb, 0, 0, 1, 1, 1, 0);
        text_area = new TextArea("This is text", 24, 80, TextArea.SCROLLBARS_VERTICAL_ONLY);
        text_area.setEditable(true);
        addGridComponent(this, text_area, panel_gb, 0, 1, 1, 2, 1, 1);
        save_button.addActionListener(this);
    }

    /**
	 * adds a component to our gridbag layout
	 */
    private void addGridComponent(Container cont, Component comp, GridBagLayout mygb, int gridx, int gridy, int gridw, int gridh, int weightx, int weighty) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = gridx;
        c.gridy = gridy;
        c.gridwidth = gridw;
        c.gridheight = gridh;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = weighty;
        c.weightx = weightx;
        c.anchor = GridBagConstraints.CENTER;
        mygb.setConstraints(comp, c);
        cont.add(comp);
    }

    public void setCommandContext(String verb, DataHandler dh) throws IOException {
        _dh = dh;
        this.setInputStream(_dh.getInputStream());
    }

    /**
   * set the data stream, component to assume it is ready to
   * be read.
   */
    public void setInputStream(InputStream ins) throws IOException {
        byte data[] = new byte[1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int bytes_read = 0;
        while ((bytes_read = ins.read(data)) > 0) baos.write(data, 0, bytes_read);
        ins.close();
        text_buffer = baos.toString();
        text_area.setText(text_buffer);
    }

    private void performSaveOperation() {
        OutputStream fos = null;
        try {
            fos = _dh.getOutputStream();
        } catch (Exception e) {
        }
        String buffer = text_area.getText();
        if (fos == null) {
            System.out.println("Invalid outputstream in TextEditor!");
            System.out.println("not saving!");
            return;
        }
        try {
            fos.write(buffer.getBytes());
            fos.flush();
            fos.close();
        } catch (IOException e) {
            System.out.println("TextEditor Save Operation failed with: " + e);
        }
    }

    public void addNotify() {
        super.addNotify();
        invalidate();
    }

    public Dimension getPreferredSize() {
        return text_area.getMinimumSize(24, 80);
    }

    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() == save_button) {
            this.performSaveOperation();
        }
    }
}
