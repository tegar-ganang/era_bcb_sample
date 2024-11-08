package org.compiere.grid.ed;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.logging.*;
import javax.swing.*;
import org.compiere.apps.*;
import org.compiere.model.*;
import org.compiere.swing.*;
import org.compiere.util.*;

/**
 *  Image Dialog
 *
 *  @author   Jorg Janke
 *  @version  $Id: VImageDialog.java,v 1.4 2006/07/30 00:51:28 jjanke Exp $
 */
public class VImageDialog extends CDialog implements ActionListener {

    /**
	 *  Constructor
	 *  @param owner
	 *  @param mImage
	 */
    public VImageDialog(Frame owner, MImage mImage) {
        super(owner, Msg.translate(Env.getCtx(), "AD_Image_ID"), true);
        log.info("MImage=" + mImage);
        m_mImage = mImage;
        try {
            jbInit();
        } catch (Exception ex) {
            log.log(Level.SEVERE, "", ex);
        }
        if (m_mImage == null) m_mImage = MImage.get(Env.getCtx(), 0);
        fileButton.setText(m_mImage.getName());
        imageLabel.setIcon(m_mImage.getIcon());
        AEnv.positionCenterWindow(owner, this);
    }

    /**  Image Model            */
    private MImage m_mImage = null;

    /**	Logger					*/
    private static CLogger log = CLogger.getCLogger(VImageDialog.class);

    /** */
    private CPanel mainPanel = new CPanel();

    private BorderLayout mainLayout = new BorderLayout();

    private CPanel parameterPanel = new CPanel();

    private CLabel fileLabel = new CLabel();

    private CButton fileButton = new CButton();

    private CLabel imageLabel = new CLabel();

    private ConfirmPanel confirmPanel = new ConfirmPanel(true);

    /**
	 *  Static Init
	 *  @throws Exception
	 */
    void jbInit() throws Exception {
        mainPanel.setLayout(mainLayout);
        fileLabel.setText(Msg.getMsg(Env.getCtx(), "SelectFile"));
        fileButton.setText("-");
        imageLabel.setBackground(Color.white);
        imageLabel.setBorder(BorderFactory.createRaisedBevelBorder());
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        getContentPane().add(mainPanel);
        mainPanel.add(parameterPanel, BorderLayout.NORTH);
        parameterPanel.add(fileLabel, null);
        parameterPanel.add(fileButton, null);
        mainPanel.add(imageLabel, BorderLayout.CENTER);
        mainPanel.add(confirmPanel, BorderLayout.SOUTH);
        fileButton.addActionListener(this);
        confirmPanel.addActionListener(this);
    }

    /**
	 *  ActionListener
	 *  @param e
	 */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == fileButton) cmd_file(); else if (e.getActionCommand().equals(ConfirmPanel.A_OK)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            if (m_mImage.save()) dispose(); else setCursor(Cursor.getDefaultCursor());
        } else if (e.getActionCommand().equals(ConfirmPanel.A_CANCEL)) {
            m_mImage = null;
            dispose();
        }
    }

    /**
	 *  Load file & display
	 */
    private void cmd_file() {
        JFileChooser jfc = new JFileChooser();
        jfc.setMultiSelectionEnabled(false);
        jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        jfc.showOpenDialog(this);
        File imageFile = jfc.getSelectedFile();
        if (imageFile == null || imageFile.isDirectory() || !imageFile.exists()) return;
        String fileName = imageFile.getAbsolutePath();
        byte[] data = null;
        try {
            FileInputStream fis = new FileInputStream(imageFile);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024 * 8];
            int length = -1;
            while ((length = fis.read(buffer)) != -1) os.write(buffer, 0, length);
            fis.close();
            data = os.toByteArray();
            os.close();
            ImageIcon image = new ImageIcon(data, fileName);
            imageLabel.setIcon(image);
        } catch (Exception e) {
            log.log(Level.WARNING, "load image", e);
            return;
        }
        fileButton.setText(imageFile.getAbsolutePath());
        pack();
        m_mImage.setName(fileName);
        m_mImage.setImageURL(fileName);
        m_mImage.setBinaryData(data);
    }

    /**
	 * 	Get Image ID
	 *	@return ID or 0
	 */
    public int getAD_Image_ID() {
        if (m_mImage != null) return m_mImage.getAD_Image_ID();
        return 0;
    }
}
