package com.sun.activation.viewers;

import java.awt.*;
import java.io.*;
import java.beans.*;
import javax.activation.*;

public class TextViewer extends Panel implements CommandObject {

    private TextArea text_area = null;

    private File text_file = null;

    private String text_buffer = null;

    private DataHandler _dh = null;

    private boolean DEBUG = false;

    /**
     * Constructor
     */
    public TextViewer() {
        setLayout(new GridLayout(1, 1));
        text_area = new TextArea("", 24, 80, TextArea.SCROLLBARS_VERTICAL_ONLY);
        text_area.setEditable(false);
        add(text_area);
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
        int bytes_read = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte data[] = new byte[1024];
        while ((bytes_read = ins.read(data)) > 0) baos.write(data, 0, bytes_read);
        ins.close();
        text_buffer = baos.toString();
        text_area.setText(text_buffer);
    }

    public void addNotify() {
        super.addNotify();
        invalidate();
    }

    public Dimension getPreferredSize() {
        return text_area.getMinimumSize(24, 80);
    }
}
