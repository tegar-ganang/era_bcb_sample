package hdr_plugin;

import hdr_plugin.calibration.ZMatrix.RandomZMatrixBuilder;
import hdr_plugin.response.debevec.DebevecCalculator;
import hdr_plugin.response.ResponseFunctionCalculatorSettings;
import hdr_plugin.helper.ImageJTools;
import hdr_plugin.response.mitsunaga.MitsunagaCalculator;
import hdr_plugin.response.ResponseFunctionCalculator;
import hdr_plugin.response.robertson.RobertsonCalculator;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.text.TextWindow;

/**
 *
 * @author Alexander Heidrich
 */
public class HDRResponseFunctionSetupFrame extends java.awt.Frame {

    /** Creates new form HDRResponseFunctionCalculatorFrame */
    public HDRResponseFunctionSetupFrame() {
        initComponents();
        initStackList();
    }

    private void initStackList() {
        chcStack.add("Please Select ...");
        int[] imgList = WindowManager.getIDList();
        String[] imgTitles = new String[imgList.length];
        for (int i = 0; i < imgList.length; i++) {
            ImagePlus imp = WindowManager.getImage(imgList[i]);
            if (imp != null) {
                imgTitles[i] = imp.getTitle();
            } else {
                imgTitles[i] = "";
            }
        }
        for (String title : imgTitles) {
            chcStack.add(title);
        }
        chcStack.select(0);
    }

    private void calcPixels() {
        try {
            int noOfImagesP = Integer.parseInt(txtNoOfImages.getText());
            int Zmin = Integer.parseInt(txtZmin.getText());
            int Zmax = Integer.parseInt(txtZmax.getText());
            Integer pix = ((Zmax - Zmin) / (noOfImagesP - 1)) + 1;
            if (pix < 0) {
                return;
            }
            txtNoOfPixels.setText(pix.toString());
        } catch (Exception e) {
            return;
        }
    }

    private boolean validateInput() {
        return true;
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        lblLogo = new javax.swing.JLabel();
        pnlButtons = new java.awt.Panel();
        btnCalcResp = new java.awt.Button();
        bntCancel = new java.awt.Button();
        bntHelp = new java.awt.Button();
        pnlContent = new java.awt.Panel();
        lblStack = new java.awt.Label();
        lblLevels = new java.awt.Label();
        lblImageNo = new java.awt.Label();
        lblPixelNo = new java.awt.Label();
        lblZmin = new java.awt.Label();
        lblExpTimes = new java.awt.Label();
        lblZmax = new java.awt.Label();
        chcStack = new java.awt.Choice();
        txtLevels = new java.awt.TextField();
        txtZmin = new java.awt.TextField();
        txtZmax = new java.awt.TextField();
        txtNoOfImages = new java.awt.TextField();
        txtExpTimes = new java.awt.TextField();
        txtNoOfPixels = new java.awt.TextField();
        setBackground(new java.awt.Color(255, 255, 255));
        setTitle("Response Function Calculator Setup");
        addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });
        setLayout(new java.awt.GridBagLayout());
        lblLogo.setIcon(new javax.swing.ImageIcon("/Users/Alex/Documents/Arbeit/HKI/ProgProj/HDR_Plugin/src/hdr_plugin/resources/log.jpg"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        add(lblLogo, gridBagConstraints);
        pnlButtons.setLayout(new java.awt.GridBagLayout());
        btnCalcResp.setLabel("Calculate Response Function");
        btnCalcResp.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCalcRespActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        pnlButtons.add(btnCalcResp, gridBagConstraints);
        bntCancel.setLabel("Cancel");
        bntCancel.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bntCancelActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        pnlButtons.add(bntCancel, gridBagConstraints);
        bntHelp.setFont(new java.awt.Font("Lucida Grande", 1, 13));
        bntHelp.setLabel("Help");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        pnlButtons.add(bntHelp, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        add(pnlButtons, gridBagConstraints);
        pnlContent.setLayout(new java.awt.GridBagLayout());
        lblStack.setText("HDR Stack:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        pnlContent.add(lblStack, gridBagConstraints);
        lblLevels.setText("Number of Levels");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        pnlContent.add(lblLevels, gridBagConstraints);
        lblImageNo.setText("No. of Images to Combine (P):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        pnlContent.add(lblImageNo, gridBagConstraints);
        lblPixelNo.setText("Selected Pixels (N):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        pnlContent.add(lblPixelNo, gridBagConstraints);
        lblZmin.setText("Min. Pixel Value (Zmin):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        pnlContent.add(lblZmin, gridBagConstraints);
        lblZmin.getAccessibleContext().setAccessibleName("Min Pixel Value:");
        lblExpTimes.setText("Exposure Times:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        pnlContent.add(lblExpTimes, gridBagConstraints);
        lblZmax.setText("Max. Pixel Value (Zmax):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        pnlContent.add(lblZmax, gridBagConstraints);
        chcStack.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                chcStackItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        pnlContent.add(chcStack, gridBagConstraints);
        txtLevels.setText("4096");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipadx = 50;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        pnlContent.add(txtLevels, gridBagConstraints);
        txtZmin.setText("0");
        txtZmin.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtZminActionPerformed(evt);
            }
        });
        txtZmin.addTextListener(new java.awt.event.TextListener() {

            public void textValueChanged(java.awt.event.TextEvent evt) {
                txtZminTextValueChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.ipadx = 50;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        pnlContent.add(txtZmin, gridBagConstraints);
        txtZmax.setText("0");
        txtZmax.addTextListener(new java.awt.event.TextListener() {

            public void textValueChanged(java.awt.event.TextEvent evt) {
                txtZmaxTextValueChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.ipadx = 50;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        pnlContent.add(txtZmax, gridBagConstraints);
        txtNoOfImages.setText("2");
        txtNoOfImages.addTextListener(new java.awt.event.TextListener() {

            public void textValueChanged(java.awt.event.TextEvent evt) {
                txtNoOfImagesTextValueChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipadx = 50;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        pnlContent.add(txtNoOfImages, gridBagConstraints);
        txtExpTimes.addTextListener(new java.awt.event.TextListener() {

            public void textValueChanged(java.awt.event.TextEvent evt) {
                test(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 200;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        pnlContent.add(txtExpTimes, gridBagConstraints);
        txtNoOfPixels.setEditable(false);
        txtNoOfPixels.setEnabled(false);
        txtNoOfPixels.setText("1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipadx = 50;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 1, 1, 1);
        pnlContent.add(txtNoOfPixels, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(6, 5, 0, 5);
        add(pnlContent, gridBagConstraints);
        pack();
    }

    /** Exit the Application */
    private void exitForm(java.awt.event.WindowEvent evt) {
        this.dispose();
    }

    private void btnCalcRespActionPerformed(java.awt.event.ActionEvent evt) {
        if (chcStack.getSelectedIndex() == 0) {
            IJ.error("Please select a stack to work with!");
            return;
        }
        if (txtExpTimes.getText().equals("")) {
            IJ.error("Please enter exposure times!");
            return;
        }
        try {
            ImagePlus imp = WindowManager.getImage(chcStack.getSelectedItem());
            int noOfImagesP = Integer.parseInt(txtNoOfImages.getText());
            int noOfPixelsN = Integer.parseInt(txtNoOfPixels.getText());
            int Zmin = Integer.parseInt(txtZmin.getText());
            int Zmax = Integer.parseInt(txtZmax.getText());
            int levels = Integer.parseInt(txtLevels.getText());
            String[] values = txtExpTimes.getText().split(",");
            double[] expTimes = new double[values.length];
            for (int i = 0; i < expTimes.length; i++) {
                expTimes[i] = new Double(values[i].trim().replaceAll(" ", ""));
            }
            if (noOfImagesP == 0) {
                IJ.error("Please select an appropriate number of images for the calculation of the camera response function");
                return;
            }
            if (noOfPixelsN == 0) {
                return;
            }
            if (noOfImagesP > imp.getStackSize()) {
                IJ.error("The number of images P is higher than the number of available images in the selected image stack!");
                return;
            }
            if (imp.getStackSize() < 2) {
                IJ.error("The size of the selected image stack is too small. You need at least two images for the calculation of the camera response function!");
                return;
            }
            if (!(noOfImagesP == expTimes.length)) {
                IJ.error("Exposure Times Missing!", "The number of images P is higher than the number of given exposure times!");
                return;
            }
            if (Zmin > Zmax) {
                IJ.error("Zmin is greater than Zmax!");
                return;
            }
            int arrayWidth = imp.getStack().getWidth();
            int arrayHeight = imp.getStack().getHeight();
            ResponseFunctionCalculatorSettings settings = new ResponseFunctionCalculatorSettings();
            settings.setExpTimes(expTimes);
            settings.setNoOfChannels(imp.getChannelProcessor().getNChannels());
            settings.setNoOfImages(noOfImagesP);
            settings.setNoOfPixelsN(noOfPixelsN);
            settings.setZmax(Zmax);
            settings.setZmin(Zmin);
            settings.setHeight(imp.getStack().getHeight());
            settings.setWidth(imp.getStack().getWidth());
            settings.setFileName((imp.getTitle()));
            settings.setType(imp.getType());
            settings.setLevels(levels);
            ResponseFunctionCalculator responseFunc = new RobertsonCalculator(imp, settings);
            HDRResponseFunctionCalculatorFrame gui = new HDRResponseFunctionCalculatorFrame(responseFunc);
            gui.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            new TextWindow("Error!", e.getMessage() + "An error occured while processing your input. Please make sure that you entered all information in the correct (numerical) format.", 400, 400).setVisible(true);
            return;
        }
    }

    private void txtZminActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void chcStackItemStateChanged(java.awt.event.ItemEvent evt) {
    }

    private void txtNoOfImagesTextValueChanged(java.awt.event.TextEvent evt) {
        calcPixels();
    }

    private void txtZminTextValueChanged(java.awt.event.TextEvent evt) {
        calcPixels();
    }

    private void txtZmaxTextValueChanged(java.awt.event.TextEvent evt) {
        calcPixels();
    }

    private void bntCancelActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
    }

    private void test(java.awt.event.TextEvent evt) {
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new HDRResponseFunctionSetupFrame().setVisible(true);
            }
        });
    }

    private java.awt.Button bntCancel;

    private java.awt.Button bntHelp;

    private java.awt.Button btnCalcResp;

    private java.awt.Choice chcStack;

    private java.awt.Label lblExpTimes;

    private java.awt.Label lblImageNo;

    private java.awt.Label lblLevels;

    private javax.swing.JLabel lblLogo;

    private java.awt.Label lblPixelNo;

    private java.awt.Label lblStack;

    private java.awt.Label lblZmax;

    private java.awt.Label lblZmin;

    private java.awt.Panel pnlButtons;

    private java.awt.Panel pnlContent;

    private java.awt.TextField txtExpTimes;

    private java.awt.TextField txtLevels;

    private java.awt.TextField txtNoOfImages;

    private java.awt.TextField txtNoOfPixels;

    private java.awt.TextField txtZmax;

    private java.awt.TextField txtZmin;
}
