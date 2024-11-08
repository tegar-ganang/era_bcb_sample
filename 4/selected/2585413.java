package org.apache.harmony.x.print;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Panel;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilePermission;
import java.net.URI;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.text.ParseException;
import java.util.Locale;
import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.ServiceUIFactory;
import javax.print.attribute.Attribute;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.TextSyntax;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.CopiesSupported;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.JobPriority;
import javax.print.attribute.standard.JobSheets;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.PrinterInfo;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import javax.print.attribute.standard.PrinterMakeAndModel;
import javax.print.attribute.standard.RequestingUserName;
import javax.print.attribute.standard.SheetCollate;
import javax.print.attribute.standard.Sides;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.NumberFormatter;
import org.apache.harmony.x.print.attributes.MediaMargins;

public class ServiceUIDialog extends ServiceUIDialogTemplate {

    public static final int APPROVE_PRINT = 1;

    public static final int CANCEL_PRINT = -1;

    public static final int SETUP_ERROR = 2;

    public static final int SETUP_OK = 3;

    int dialogResult = 0;

    public static final int PRINT_DIALOG = 1;

    public static final int PAGE_DIALOG = 2;

    private int dialogType = PRINT_DIALOG;

    PrintService[] services = null;

    private DocFlavor flavor = null;

    private PrintRequestAttributeSet attrs = null;

    private PrintRequestAttributeSet newAttrs = null;

    PrintService myService = null;

    ButtonGroup prnRngGrp = null;

    ButtonGroup orientGrp = null;

    ButtonGroup colorGrp = null;

    ButtonGroup sidesGrp = null;

    ButtonGroup qualGrp = null;

    OrientationRequested lastOrient = null;

    boolean firstUse = true;

    private Permission destPermission = new FilePermission("<<ALL FILES>>", "read,write");

    public ServiceUIDialog(GraphicsConfiguration gc, int x, int y, PrintService[] dialogServices, int defServiceIndex, DocFlavor dialogFlavor, PrintRequestAttributeSet dialogAttrs, Window owner) {
        if (GraphicsEnvironment.isHeadless()) {
            dialogResult = SETUP_ERROR;
            throw new HeadlessException();
        }
        if (owner instanceof Frame) {
            printDialog = new JDialog((Frame) owner, "Print", true, gc);
        } else if (owner instanceof Dialog) {
            printDialog = new JDialog((Dialog) owner, "Print", true, gc);
        } else {
            dialogResult = SETUP_ERROR;
        }
        if (printDialog != null) {
            printDialog.setSize(542, 444);
            printDialog.setLocation(x, y);
            printDialog.setContentPane(getPanel());
            printDialog.setResizable(false);
            dialogResult = setup(dialogServices, defServiceIndex, dialogFlavor, dialogAttrs);
        }
    }

    public ServiceUIDialog(GraphicsConfiguration gc, int x, int y, PrintService aService, PrintRequestAttributeSet dialogAttrs, Window owner) {
        dialogType = PAGE_DIALOG;
        if (GraphicsEnvironment.isHeadless()) {
            dialogResult = SETUP_ERROR;
            throw new HeadlessException();
        }
        if (owner instanceof Frame) {
            printDialog = new JDialog((Frame) owner, "Print", true, gc);
        } else if (owner instanceof Dialog) {
            printDialog = new JDialog((Dialog) owner, "Print", true, gc);
        } else {
            dialogResult = SETUP_ERROR;
        }
        if (printDialog != null) {
            printDialog.setSize(530, 400);
            printDialog.setLocation(x, y);
            printDialog.setContentPane(getPageDialogPanel());
            printDialog.setResizable(false);
            dialogResult = pageSetup(aService, dialogAttrs);
        }
    }

    public void show() {
        if (dialogResult == SETUP_OK) {
            AccessController.doPrivileged(new PrivilegedAction() {

                public Object run() {
                    printDialog.show();
                    return null;
                }
            });
        }
    }

    private int setup(PrintService[] dialogServices, int defServiceIndex, DocFlavor dialogFlavor, PrintRequestAttributeSet dialogAttrs) {
        if ((dialogServices == null) || (dialogServices.length <= 0) || (defServiceIndex < 0) || (defServiceIndex >= dialogServices.length) || (dialogAttrs == null)) {
            return SETUP_ERROR;
        }
        services = dialogServices;
        flavor = dialogFlavor;
        attrs = dialogAttrs;
        this.myService = services[defServiceIndex];
        if (servicesBox.getItemCount() <= 0) {
            for (int i = 0; i < services.length; i++) {
                servicesBox.addItem(services[i].getName());
            }
        }
        newAttrs = new HashPrintRequestAttributeSet(attrs);
        prepareDialog();
        servicesBox.setSelectedIndex(defServiceIndex);
        firstUse = false;
        return SETUP_OK;
    }

    private int pageSetup(PrintService aService, PrintRequestAttributeSet requestAttrs) {
        myService = (aService == null) ? PrintServiceLookup.lookupDefaultPrintService() : aService;
        if ((requestAttrs == null) || (aService == null)) {
            return SETUP_ERROR;
        }
        attrs = requestAttrs;
        newAttrs = new HashPrintRequestAttributeSet(attrs);
        myService = aService;
        prepareDialog();
        fillPageSetupFields();
        firstUse = false;
        return SETUP_OK;
    }

    private void prepareDialog() {
        JRadioButton[] orientArr = new JRadioButton[] { portraitBtn, landscapeBtn, rvportraitBtn, rvlandscapeBtn };
        organizeButtonGroup(orientGrp, orientArr);
        sourceBox.setVisible(false);
        sourceLabel.setVisible(false);
        if (dialogType == PRINT_DIALOG) {
            JRadioButton[] rangesArr = new JRadioButton[] { allRngBtn, pageRngBtn };
            JRadioButton[] colorsArr = new JRadioButton[] { monoBtn, colorBtn };
            JRadioButton[] sidesArr = new JRadioButton[] { oneSideBtn, tumbleBtn, duplexBtn };
            JRadioButton[] qualityArr = new JRadioButton[] { draftBtn, normalBtn, highBtn };
            organizeButtonGroup(prnRngGrp, rangesArr);
            organizeButtonGroup(colorGrp, colorsArr);
            organizeButtonGroup(sidesGrp, sidesArr);
            organizeButtonGroup(qualGrp, qualityArr);
            propertiesBtn.setVisible(false);
            prtSpinner.setModel(new SpinnerNumberModel(1, 1, 100, 1));
            cpSpinner.addChangeListener(new CopiesChangeListener());
            allRngBtn.addChangeListener(new PagesButtonChangeListener());
            pageRngBtn.addChangeListener(new PagesButtonChangeListener());
            servicesBox.addActionListener(new ServicesActionListener());
        }
        portraitBtn.addChangeListener(new OrientationChangeListener());
        landscapeBtn.addChangeListener(new OrientationChangeListener());
        rvportraitBtn.addChangeListener(new OrientationChangeListener());
        rvlandscapeBtn.addChangeListener(new OrientationChangeListener());
        printBtn.addActionListener(new OKButtonListener());
        cancelBtn.addActionListener(new cancelButtonListener());
        printDialog.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent event) {
                dialogResult = CANCEL_PRINT;
            }
        });
    }

    private void organizeButtonGroup(ButtonGroup group, JRadioButton[] buttons) {
        group = new ButtonGroup();
        for (int i = 0; i < buttons.length; i++) {
            group.add(buttons[i]);
        }
    }

    class ServicesActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (firstUse || (myService != services[servicesBox.getSelectedIndex()])) {
                myService = services[servicesBox.getSelectedIndex()];
                fillGeneralFields();
                fillPageSetupFields();
                fillAppearanceFields();
                fillVendorSuppliedTab();
            }
        }
    }

    void fillGeneralFields() {
        fillStatusField();
        fillTypeField();
        fillInfoField();
        filltoFileBox();
        fillCopiesFields();
        fillPrintRangeFields();
    }

    void fillStatusField() {
        String text;
        PrinterIsAcceptingJobs job = (PrinterIsAcceptingJobs) myService.getAttribute(PrinterIsAcceptingJobs.class);
        if (job != null) {
            text = job.equals(PrinterIsAcceptingJobs.ACCEPTING_JOBS) ? "Accepting jobs" : "Not accepting jobs";
        } else {
            text = "";
        }
        statusText.setText(text);
    }

    void fillTypeField() {
        PrinterMakeAndModel type = (PrinterMakeAndModel) myService.getAttribute(PrinterMakeAndModel.class);
        typeText.setText(type == null ? "" : type.getValue());
    }

    void fillInfoField() {
        PrinterInfo info = (PrinterInfo) myService.getAttribute(PrinterInfo.class);
        infoText.setText(info == null ? "" : info.getValue());
    }

    void filltoFileBox() {
        if (firstUse && attrs.containsKey(Destination.class)) {
            toFileBox.setSelected(true);
        }
        toFileBox.setEnabled(checkFilePermission(destPermission) && myService.isAttributeCategorySupported(Destination.class));
    }

    boolean checkFilePermission(Permission permission) {
        SecurityManager manager = System.getSecurityManager();
        if (manager != null) {
            try {
                manager.checkPermission(permission);
                return true;
            } catch (SecurityException e) {
                return false;
            }
        }
        return true;
    }

    void fillCopiesFields() {
        fillCopiesSpinner();
        fillCollateBox();
    }

    void fillCopiesSpinner() {
        boolean isEnabled = myService.isAttributeCategorySupported(Copies.class);
        copiesLabel.setEnabled(isEnabled);
        cpSpinner.setEnabled(isEnabled);
        if (firstUse && !isEnabled) {
            int value = (attrs.containsKey(Copies.class) ? ((Copies) (attrs.get(Copies.class))).getValue() : 1);
            cpSpinner.setModel(new SpinnerNumberModel(value, value, value, 1));
        }
        if (isEnabled) {
            int value = (firstUse && attrs.containsKey(Copies.class) ? ((Copies) (attrs.get(Copies.class))).getValue() : ((Integer) cpSpinner.getValue()).intValue());
            CopiesSupported supported = (CopiesSupported) myService.getSupportedAttributeValues(Copies.class, flavor, attrs);
            Copies defaul = (Copies) myService.getDefaultAttributeValue(Copies.class);
            if (supported == null) {
                supported = new CopiesSupported((defaul != null) ? defaul.getValue() : 1);
            }
            int[][] range = supported.getMembers();
            if (!supported.contains(value)) {
                value = (((defaul == null) || (!supported.contains(defaul.getValue()))) ? range[0][0] : defaul.getValue());
            }
            cpSpinner.setModel(new SpinnerNumberModel(value, range[0][0], range[0][1], 1));
        }
    }

    class CopiesChangeListener implements ChangeListener {

        public void stateChanged(ChangeEvent e) {
            fillCollateBox();
        }
    }

    void fillCollateBox() {
        boolean isSupported = myService.isAttributeCategorySupported(SheetCollate.class);
        SheetCollate[] supported = (SheetCollate[]) (myService.getSupportedAttributeValues(SheetCollate.class, flavor, attrs));
        Attribute attr = attrs.get(SheetCollate.class);
        int spinnerValue = ((Integer) cpSpinner.getValue()).intValue();
        if ((supported == null) || !isSupported) {
            if (attrs.containsKey(SheetCollate.class)) {
                collateBox.setSelected(attr.equals(SheetCollate.COLLATED));
            }
        } else {
            boolean isValueSupported = myService.isAttributeValueSupported(SheetCollate.COLLATED, flavor, attrs);
            if (attrs.containsKey(SheetCollate.class) && isValueSupported) {
                collateBox.setSelected(attr.equals(SheetCollate.COLLATED));
            } else {
                Object defaul = myService.getDefaultAttributeValue(SheetCollate.class);
                collateBox.setSelected(defaul != null ? defaul.equals(SheetCollate.COLLATED) : true);
            }
        }
        collateBox.setEnabled(isSupported && (spinnerValue > 1) && (!(supported == null || supported.length <= 1)));
    }

    void fillPrintRangeFields() {
        if (firstUse) {
            if (attrs.containsKey(PageRanges.class)) {
                PageRanges aRange = (PageRanges) (attrs.get(PageRanges.class));
                int[][] range = aRange.getMembers();
                fromTxt.setText(range.length > 0 ? Integer.toString(range[0][0]) : "1");
                toTxt.setText(range.length > 0 ? Integer.toString(range[0][1]) : "1");
                pageRngBtn.setSelected(true);
            } else {
                allRngBtn.setSelected(true);
                fromTxt.setEnabled(false);
                toTxt.setEnabled(false);
                fromTxt.setText("1");
                toTxt.setText("1");
                toLabel.setEnabled(false);
            }
        }
    }

    class PagesButtonChangeListener implements ChangeListener {

        public void stateChanged(ChangeEvent e) {
            fromTxt.setEnabled(pageRngBtn.isSelected());
            toTxt.setEnabled(pageRngBtn.isSelected());
            toLabel.setEnabled(pageRngBtn.isSelected());
        }
    }

    void fillPageSetupFields() {
        fillMediaFields();
        fillOrientationFields();
        fillMarginsFields();
    }

    void fillMediaFields() {
        if (myService.isAttributeCategorySupported(Media.class)) {
            Media[] mediaList = (Media[]) myService.getSupportedAttributeValues(Media.class, flavor, attrs);
            Media oldMedia = (sizeBox.getItemCount() <= 0) ? null : (Media) sizeBox.getSelectedItem();
            sizeBox.removeAllItems();
            if ((mediaList != null) && (mediaList.length > 0)) {
                for (int i = 0; i < mediaList.length; i++) {
                    sizeBox.addItem(mediaList[i]);
                }
                selectMedia(oldMedia);
            }
            sizeBox.setEnabled((mediaList != null) && (mediaList.length > 0));
            sizeLabel.setEnabled((mediaList != null) && (mediaList.length > 0));
        } else {
            sizeBox.setEnabled(false);
            sizeLabel.setEnabled(false);
        }
        sizeBox.updateUI();
    }

    void selectMedia(Media oldMedia) {
        if (sizeBox.getItemCount() > 0) {
            if ((oldMedia == null) && attrs.containsKey(Media.class)) {
                oldMedia = (Media) attrs.get(Media.class);
            }
            sizeBox.setSelectedItem(oldMedia);
            if ((sizeBox.getSelectedIndex() < 0) || (!sizeBox.getSelectedItem().equals(oldMedia))) {
                Object media = myService.getDefaultAttributeValue(Media.class);
                if (media != null) {
                    sizeBox.setSelectedItem(media);
                }
            }
            if (sizeBox.getSelectedIndex() < 0) {
                sizeBox.setSelectedIndex(0);
            }
        }
    }

    void fillOrientationFields() {
        OrientationRequested orient = (OrientationRequested) attrs.get(OrientationRequested.class);
        boolean isSupported = myService.isAttributeCategorySupported(OrientationRequested.class);
        OrientationRequested[] supportedList = (isSupported ? (OrientationRequested[]) myService.getSupportedAttributeValues(OrientationRequested.class, flavor, attrs) : null);
        enableOrient(supportedList);
        if (firstUse) {
            if (orient != null) {
                selectOrient(orient);
            } else {
                OrientationRequested defaul = (OrientationRequested) myService.getDefaultAttributeValue(OrientationRequested.class);
                selectOrient(isSupported ? defaul : null);
            }
        }
        if (supportedList != null) {
            OrientationRequested oldValue = getOrient();
            if (!orientEnabled(oldValue)) {
                selectOrient(orientEnabled(orient) ? orient : supportedList[0]);
            }
        }
    }

    private void selectOrient(OrientationRequested par) {
        if (par == null) {
            par = OrientationRequested.PORTRAIT;
        }
        if (par.equals(OrientationRequested.LANDSCAPE)) {
            landscapeBtn.setSelected(true);
        } else if (par.equals(OrientationRequested.REVERSE_LANDSCAPE)) {
            rvlandscapeBtn.setSelected(true);
        } else if (par.equals(OrientationRequested.REVERSE_PORTRAIT)) {
            rvportraitBtn.setSelected(true);
        } else {
            portraitBtn.setSelected(true);
        }
    }

    private void enableOrient(OrientationRequested[] list) {
        portraitBtn.setEnabled(false);
        landscapeBtn.setEnabled(false);
        rvportraitBtn.setEnabled(false);
        rvlandscapeBtn.setEnabled(false);
        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                if (list[i].equals(OrientationRequested.LANDSCAPE)) {
                    landscapeBtn.setEnabled(true);
                } else if (list[i].equals(OrientationRequested.PORTRAIT)) {
                    portraitBtn.setEnabled(true);
                } else if (list[i].equals(OrientationRequested.REVERSE_LANDSCAPE)) {
                    rvlandscapeBtn.setEnabled(true);
                } else if (list[i].equals(OrientationRequested.REVERSE_PORTRAIT)) {
                    rvportraitBtn.setEnabled(true);
                }
            }
        }
    }

    OrientationRequested getOrient() {
        if (portraitBtn.isSelected()) {
            return OrientationRequested.PORTRAIT;
        } else if (landscapeBtn.isSelected()) {
            return OrientationRequested.LANDSCAPE;
        } else if (rvportraitBtn.isSelected()) {
            return OrientationRequested.REVERSE_PORTRAIT;
        } else if (rvlandscapeBtn.isSelected()) {
            return OrientationRequested.REVERSE_LANDSCAPE;
        } else {
            return null;
        }
    }

    private boolean orientEnabled(OrientationRequested par) {
        if (par == null) {
            return false;
        } else if (par.equals(OrientationRequested.LANDSCAPE)) {
            return landscapeBtn.isEnabled();
        } else if (par.equals(OrientationRequested.PORTRAIT)) {
            return portraitBtn.isEnabled();
        } else if (par.equals(OrientationRequested.REVERSE_LANDSCAPE)) {
            return rvlandscapeBtn.isEnabled();
        } else if (par.equals(OrientationRequested.REVERSE_PORTRAIT)) {
            return rvportraitBtn.isEnabled();
        } else {
            return false;
        }
    }

    private boolean isOrientSupported() {
        return landscapeBtn.isEnabled() || portraitBtn.isEnabled() || rvlandscapeBtn.isEnabled() || rvportraitBtn.isEnabled();
    }

    class OrientationChangeListener implements ChangeListener {

        public void stateChanged(ChangeEvent e) {
            OrientationRequested now = getOrient();
            if ((lastOrient != null) && (now != null) && (!lastOrient.equals(now))) {
                String txt = leftTxt.getText();
                if ((lastOrient.equals(OrientationRequested.PORTRAIT) && now.equals(OrientationRequested.LANDSCAPE)) || (lastOrient.equals(OrientationRequested.LANDSCAPE) && now.equals(OrientationRequested.REVERSE_PORTRAIT)) || (lastOrient.equals(OrientationRequested.REVERSE_PORTRAIT) && now.equals(OrientationRequested.REVERSE_LANDSCAPE)) || (lastOrient.equals(OrientationRequested.REVERSE_LANDSCAPE) && now.equals(OrientationRequested.PORTRAIT))) {
                    leftTxt.setText(bottomTxt.getText());
                    bottomTxt.setText(rightTxt.getText());
                    rightTxt.setText(topTxt.getText());
                    topTxt.setText(txt);
                } else if ((lastOrient.equals(OrientationRequested.PORTRAIT) && now.equals(OrientationRequested.REVERSE_PORTRAIT)) || (lastOrient.equals(OrientationRequested.LANDSCAPE) && now.equals(OrientationRequested.REVERSE_LANDSCAPE)) || (lastOrient.equals(OrientationRequested.REVERSE_PORTRAIT) && now.equals(OrientationRequested.PORTRAIT)) || (lastOrient.equals(OrientationRequested.REVERSE_LANDSCAPE) && now.equals(OrientationRequested.LANDSCAPE))) {
                    leftTxt.setText(rightTxt.getText());
                    rightTxt.setText(txt);
                    txt = topTxt.getText();
                    topTxt.setText(bottomTxt.getText());
                    bottomTxt.setText(txt);
                } else {
                    leftTxt.setText(topTxt.getText());
                    topTxt.setText(rightTxt.getText());
                    rightTxt.setText(bottomTxt.getText());
                    bottomTxt.setText(txt);
                }
            }
            if (now != null) {
                lastOrient = now;
            }
        }
    }

    void fillMarginsFields() {
        boolean isMediaSupported = myService.isAttributeCategorySupported(Media.class);
        boolean isPaSupported = myService.isAttributeCategorySupported(MediaPrintableArea.class);
        boolean isMarginsSupported = myService.isAttributeCategorySupported(MediaMargins.class);
        boolean isMarginsEnabled = ((dialogType == PAGE_DIALOG) || isMarginsSupported || (isMediaSupported && isPaSupported && (sizeBox.getSelectedItem() != null)));
        enableMargins(isMarginsEnabled);
        if (firstUse) {
            MediaMargins margins = null;
            if (isMarginsEnabled) {
                Media selectedMedia = (Media) sizeBox.getSelectedItem();
                boolean isMediaSizeSelected = (selectedMedia == null) ? false : selectedMedia.getClass().isAssignableFrom(MediaSizeName.class);
                MediaSize selectedSize = isMediaSizeSelected ? MediaSize.getMediaSizeForName((MediaSizeName) selectedMedia) : null;
                if (isMediaSupported && isPaSupported && attrs.containsKey(Media.class) && attrs.containsKey(MediaPrintableArea.class) && attrs.get(Media.class).equals(selectedMedia) && isMediaSizeSelected) {
                    try {
                        MediaPrintableArea attrsPA = (MediaPrintableArea) attrs.get(MediaPrintableArea.class);
                        margins = new MediaMargins(selectedSize, attrsPA);
                    } catch (IllegalArgumentException e) {
                    }
                }
                if ((margins == null) && (isMarginsSupported || (dialogType == PAGE_DIALOG))) {
                    margins = (MediaMargins) (attrs.containsKey(MediaMargins.class) ? attrs.get(MediaMargins.class) : myService.getDefaultAttributeValue(MediaMargins.class));
                }
                if ((margins == null) && isPaSupported && isMediaSupported && isMediaSizeSelected) {
                    try {
                        MediaPrintableArea defaultPA = (MediaPrintableArea) myService.getDefaultAttributeValue(MediaPrintableArea.class);
                        if ((defaultPA != null) && (selectedSize != null)) {
                            margins = new MediaMargins(selectedSize, defaultPA);
                        }
                    } catch (IllegalArgumentException e) {
                    }
                }
                if (margins == null) {
                    margins = new MediaMargins(25.4F, 25.4F, 25.4F, 25.4F, MediaMargins.MM);
                }
            } else {
                margins = (attrs.containsKey(MediaMargins.class) ? (MediaMargins) attrs.get(MediaMargins.class) : new MediaMargins(25.4F, 25.4F, 25.4F, 25.4F, MediaMargins.MM));
            }
            setMargins(margins);
        }
    }

    private void enableMargins(boolean flg) {
        leftLabel.setEnabled(flg);
        rightLabel.setEnabled(flg);
        topLabel.setEnabled(flg);
        bottomLabel.setEnabled(flg);
        leftTxt.setEnabled(flg);
        rightTxt.setEnabled(flg);
        topTxt.setEnabled(flg);
        bottomTxt.setEnabled(flg);
    }

    private void setMargins(MediaMargins margins) {
        NumberFormatter fmt = getFloatFormatter();
        try {
            leftTxt.setText(fmt.valueToString(new Float(margins.getX1(MediaMargins.MM))));
            rightTxt.setText(fmt.valueToString(new Float(margins.getX2(MediaMargins.MM))));
            topTxt.setText(fmt.valueToString(new Float(margins.getY1(MediaMargins.MM))));
            bottomTxt.setText(fmt.valueToString(new Float(margins.getY2(MediaMargins.MM))));
        } catch (ParseException e) {
        }
    }

    void fillAppearanceFields() {
        fillColorFields();
        fillQualityFields();
        fillSidesFields();
        fillJobAttributesFields();
    }

    void fillColorFields() {
        boolean lastIsMonochrome = getLastColor();
        monoBtn.setEnabled(false);
        colorBtn.setEnabled(false);
        if (myService.isAttributeCategorySupported(Chromaticity.class)) {
            Chromaticity[] supported = (Chromaticity[]) (myService.getSupportedAttributeValues(Chromaticity.class, flavor, attrs));
            if (supported != null) {
                if (supported.length == 1) {
                    lastIsMonochrome = setMonochrome((supported[0]).equals(Chromaticity.MONOCHROME));
                } else if (supported.length > 1) {
                    monoBtn.setEnabled(true);
                    colorBtn.setEnabled(true);
                }
            }
        }
        if (lastIsMonochrome) {
            monoBtn.setSelected(true);
        } else {
            colorBtn.setSelected(true);
        }
    }

    private boolean getLastColor() {
        if (firstUse) {
            if (attrs.containsKey(Chromaticity.class)) {
                Attribute value = attrs.get(Chromaticity.class);
                return value.equals(Chromaticity.MONOCHROME);
            }
            Object defaul = myService.getDefaultAttributeValue(Chromaticity.class);
            return (myService.isAttributeCategorySupported(Chromaticity.class) && (defaul != null)) ? defaul.equals(Chromaticity.MONOCHROME) : true;
        }
        return monoBtn.isSelected();
    }

    private boolean setMonochrome(boolean flg) {
        monoBtn.setEnabled(flg);
        colorBtn.setEnabled(!flg);
        return flg;
    }

    void fillQualityFields() {
        PrintQuality quality = (PrintQuality) attrs.get(PrintQuality.class);
        if (firstUse) {
            selectQualityButton(quality);
        }
        PrintQuality[] aList = (myService.isAttributeCategorySupported(PrintQuality.class) ? (PrintQuality[]) myService.getSupportedAttributeValues(PrintQuality.class, flavor, attrs) : null);
        enableQualityButtons(aList);
        if ((aList != null) && (!qualityIsEnabled(getSelectedQuality()))) {
            selectQualityButton(qualityIsEnabled(quality) ? quality : (PrintQuality) (myService.getDefaultAttributeValue(PrintQuality.class)));
        }
    }

    private void selectQualityButton(PrintQuality par) {
        if (par == null) {
            par = PrintQuality.NORMAL;
        }
        if (par.equals(PrintQuality.DRAFT)) {
            draftBtn.setSelected(true);
        } else if (par.equals(PrintQuality.HIGH)) {
            highBtn.setSelected(true);
        } else {
            normalBtn.setSelected(true);
        }
    }

    private void enableQualityButtons(PrintQuality[] list) {
        normalBtn.setEnabled(false);
        draftBtn.setEnabled(false);
        highBtn.setEnabled(false);
        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                if (list[i].equals(PrintQuality.DRAFT)) {
                    draftBtn.setEnabled(true);
                } else if (list[i].equals(PrintQuality.NORMAL)) {
                    normalBtn.setEnabled(true);
                } else if (list[i].equals(PrintQuality.HIGH)) {
                    highBtn.setEnabled(true);
                }
            }
        }
    }

    private PrintQuality getSelectedQuality() {
        if (normalBtn.isSelected()) {
            return PrintQuality.NORMAL;
        } else if (draftBtn.isSelected()) {
            return PrintQuality.DRAFT;
        } else if (highBtn.isSelected()) {
            return PrintQuality.HIGH;
        } else {
            return null;
        }
    }

    private boolean qualityIsEnabled(PrintQuality par) {
        if (par == null) {
            return false;
        } else if (par.equals(PrintQuality.NORMAL)) {
            return normalBtn.isEnabled();
        } else if (par.equals(PrintQuality.DRAFT)) {
            return draftBtn.isEnabled();
        } else if (par.equals(PrintQuality.HIGH)) {
            return highBtn.isEnabled();
        } else {
            return false;
        }
    }

    private boolean isQualitySupported() {
        return (normalBtn.isEnabled() || draftBtn.isEnabled() || highBtn.isEnabled());
    }

    void fillSidesFields() {
        Sides side = (Sides) attrs.get(Sides.class);
        if (firstUse) {
            selectSidesButton(side);
        }
        Sides[] aList = (myService.isAttributeCategorySupported(Sides.class) ? (Sides[]) (myService.getSupportedAttributeValues(Sides.class, flavor, attrs)) : null);
        enableSidesButtons(aList);
        if ((aList != null) && !sideIsEnabled(getSelectedSide())) {
            selectSidesButton(sideIsEnabled(side) ? side : (Sides) (myService.getDefaultAttributeValue(Sides.class)));
        }
    }

    private void selectSidesButton(Sides par) {
        if (par == null) {
            par = Sides.ONE_SIDED;
        }
        if (par.equals(Sides.TUMBLE) || par.equals(Sides.TWO_SIDED_SHORT_EDGE)) {
            tumbleBtn.setSelected(true);
        } else if (par.equals(Sides.DUPLEX) || par.equals(Sides.TWO_SIDED_LONG_EDGE)) {
            duplexBtn.setSelected(true);
        } else {
            oneSideBtn.setSelected(true);
        }
    }

    private void enableSidesButtons(Sides[] list) {
        oneSideBtn.setEnabled(false);
        duplexBtn.setEnabled(false);
        tumbleBtn.setEnabled(false);
        if (list != null) {
            for (int i = 0; i < list.length; i++) {
                if (list[i].equals(Sides.ONE_SIDED)) {
                    oneSideBtn.setEnabled(true);
                } else if (list[i].equals(Sides.DUPLEX) || list[i].equals(Sides.TWO_SIDED_LONG_EDGE)) {
                    duplexBtn.setEnabled(true);
                } else if (list[i].equals(Sides.TUMBLE) || list[i].equals(Sides.TWO_SIDED_SHORT_EDGE)) {
                    tumbleBtn.setEnabled(true);
                }
            }
        }
    }

    private Sides getSelectedSide() {
        if (oneSideBtn.isSelected()) {
            return Sides.ONE_SIDED;
        } else if (duplexBtn.isSelected()) {
            return Sides.DUPLEX;
        } else if (tumbleBtn.isSelected()) {
            return Sides.TUMBLE;
        } else {
            return null;
        }
    }

    private boolean sideIsEnabled(Sides par) {
        if (par == null) {
            return false;
        } else if (par.equals(Sides.ONE_SIDED)) {
            return oneSideBtn.isEnabled();
        } else if (par.equals(Sides.DUPLEX) || par.equals(Sides.TWO_SIDED_LONG_EDGE)) {
            return duplexBtn.isEnabled();
        } else if (par.equals(Sides.TUMBLE) || par.equals(Sides.TWO_SIDED_SHORT_EDGE)) {
            return tumbleBtn.isEnabled();
        } else {
            return false;
        }
    }

    private boolean isSidesSupported() {
        return (oneSideBtn.isEnabled() || duplexBtn.isEnabled() || tumbleBtn.isEnabled());
    }

    void fillJobAttributesFields() {
        fillBannerPageField();
        fillPriorityField();
        fillJobNameField();
        fillUserNameField();
    }

    void fillBannerPageField() {
        JobSheets[] supported = (myService.isAttributeCategorySupported(JobSheets.class) ? (JobSheets[]) (myService.getSupportedAttributeValues(JobSheets.class, flavor, attrs)) : null);
        Attribute value = attrs.get(JobSheets.class);
        if ((supported != null) && (supported.length == 0)) {
            supported = null;
        }
        if (supported == null) {
            if (firstUse && attrs.containsKey(JobSheets.class)) {
                bannerBox.setSelected(value.equals(JobSheets.STANDARD));
            }
        } else {
            if (supported.length == 1) {
                bannerBox.setSelected(supported[0] == JobSheets.STANDARD);
            } else if (attrs.containsKey(JobSheets.class)) {
                bannerBox.setSelected(value.equals(JobSheets.STANDARD));
            } else {
                Object def = myService.getDefaultAttributeValue(JobSheets.class);
                bannerBox.setSelected(def == null ? false : def.equals(JobSheets.STANDARD));
            }
        }
        bannerBox.setEnabled((supported != null) && (supported.length > 1));
    }

    void fillPriorityField() {
        boolean enabled = myService.isAttributeCategorySupported(JobPriority.class);
        priorityLabel.setEnabled(enabled);
        prtSpinner.setEnabled(enabled);
        if (firstUse) {
            if (attrs.containsKey(JobPriority.class)) {
                JobPriority value = (JobPriority) (attrs.get(JobPriority.class));
                prtSpinner.setValue(new Integer(value.getValue()));
            } else {
                if (enabled) {
                    JobPriority defaul = (JobPriority) (myService.getDefaultAttributeValue(JobPriority.class));
                    prtSpinner.setValue(defaul == null ? new Integer(1) : new Integer(defaul.getValue()));
                } else {
                    prtSpinner.setValue(new Integer(1));
                }
            }
        }
    }

    void fillJobNameField() {
        boolean supported = myService.isAttributeCategorySupported(JobName.class);
        jobNameTxt.setEnabled(supported);
        jobNameLabel.setEnabled(supported);
        if (firstUse && attrs.containsKey(JobName.class)) {
            jobNameTxt.setText(((TextSyntax) attrs.get(JobName.class)).getValue());
        }
        if (supported && (jobNameTxt.getText().length() <= 0)) {
            TextSyntax txt = (TextSyntax) (myService.getDefaultAttributeValue(JobName.class));
            jobNameTxt.setText(txt == null ? "" : txt.getValue());
        }
    }

    void fillUserNameField() {
        boolean flg = myService.isAttributeCategorySupported(RequestingUserName.class);
        userNameTxt.setEnabled(flg);
        userNameLabel.setEnabled(flg);
        if (firstUse && attrs.containsKey(RequestingUserName.class)) {
            userNameTxt.setText(((TextSyntax) attrs.get(RequestingUserName.class)).getValue());
        }
        if (flg && (userNameTxt.getText().length() <= 0)) {
            RequestingUserName defaul = (RequestingUserName) (myService.getDefaultAttributeValue(RequestingUserName.class));
            userNameTxt.setText(defaul == null ? "" : (String) (defaul.getValue()));
        }
    }

    void fillVendorSuppliedTab() {
        ServiceUIFactory factory = myService.getServiceUIFactory();
        if (tabbedPane.getTabCount() > 3) {
            tabbedPane.remove(3);
        }
        if (factory != null) {
            JComponent swingUI = (JComponent) factory.getUI(ServiceUIFactory.MAIN_UIROLE, ServiceUIFactory.JCOMPONENT_UI);
            if (swingUI != null) {
                tabbedPane.addTab("Vendor Supplied", swingUI);
                tabbedPane.setMnemonicAt(3, 'V');
            } else {
                Panel panelUI = (Panel) factory.getUI(ServiceUIFactory.MAIN_UIROLE, ServiceUIFactory.PANEL_UI);
                if (panelUI != null) {
                    tabbedPane.addTab("Vendor Supplied", panelUI);
                    tabbedPane.setMnemonicAt(3, 'V');
                }
            }
        }
    }

    class OKButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            if (updateAttributes()) {
                dialogResult = APPROVE_PRINT;
                printDialog.hide();
            }
        }
    }

    class cancelButtonListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            dialogResult = CANCEL_PRINT;
            printDialog.hide();
        }
    }

    public int getResult() {
        return dialogResult;
    }

    public PrintRequestAttributeSet getAttributes() {
        return newAttrs;
    }

    public PrintService getPrintService() {
        return (dialogResult == APPROVE_PRINT) ? myService : null;
    }

    public PrintService[] getServices() {
        return services;
    }

    public PrintService getSelectedService() {
        return myService;
    }

    public DocFlavor getFlavor() {
        return flavor;
    }

    protected boolean updateAttributes() {
        newAttrs = new HashPrintRequestAttributeSet(attrs);
        if (dialogType == PRINT_DIALOG) {
            updateCopies();
            updateCollate();
            if (!updatePrintRange()) {
                JOptionPane.showMessageDialog(printDialog, "Incorrect Print Range!", "Incorrect parameter", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            updateColor();
            updateQuality();
            updateSides();
            updateBannerPage();
            updatePriority();
            updateJobName();
            updateUserName();
            updatePrintToFile();
        }
        updateMedia();
        updateOrientation();
        if (!updateMargins()) {
            JOptionPane.showMessageDialog(printDialog, "Incorrect margins!", "Incorrect parameter", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private void updatePrintToFile() {
        if (toFileBox.isEnabled() && toFileBox.isSelected()) {
            Destination dest = (Destination) (newAttrs.containsKey(Destination.class) ? newAttrs.get(Destination.class) : myService.getDefaultAttributeValue(Destination.class));
            File file = null;
            DestinationChooser chooser = new DestinationChooser();
            if (dest == null) {
                dest = new Destination((new File("out.prn")).toURI());
            }
            try {
                file = new File(dest.getURI());
            } catch (Exception e) {
                file = new File("out.prn");
            }
            chooser.setSelectedFile(file);
            chooser.setDialogTitle("Print to file");
            int chooserResult = chooser.showDialog(printDialog, "OK");
            if (chooserResult == JFileChooser.APPROVE_OPTION) {
                try {
                    URI selectedFile = chooser.getSelectedFile().toURI();
                    newAttrs.add(new Destination(selectedFile));
                } catch (Exception e) {
                    removeAttribute(Destination.class);
                }
            }
        } else {
            removeAttribute(Destination.class);
        }
    }

    private void updateCopies() {
        if (cpSpinner.isEnabled()) {
            int copiesValue = ((SpinnerNumberModel) (cpSpinner.getModel())).getNumber().intValue();
            newAttrs.add(new Copies(copiesValue));
        } else {
            removeAttribute(Copies.class);
        }
    }

    private void updateCollate() {
        if (collateBox.isEnabled()) {
            newAttrs.add(collateBox.isSelected() ? SheetCollate.COLLATED : SheetCollate.UNCOLLATED);
        } else {
            removeAttribute(SheetCollate.class);
        }
    }

    protected boolean updatePrintRange() {
        if (pageRngBtn.isEnabled() && pageRngBtn.isSelected()) {
            try {
                int fromValue = Integer.valueOf(fromTxt.getText()).intValue();
                int toValue = Integer.valueOf(toTxt.getText()).intValue();
                if (fromValue > toValue) {
                    throw new NumberFormatException();
                }
                newAttrs.add(new PageRanges(fromValue, toValue));
            } catch (NumberFormatException e) {
                return false;
            } catch (IllegalArgumentException e) {
                return false;
            }
        } else {
            removeAttribute(PageRanges.class);
        }
        return true;
    }

    private void updateMedia() {
        if (sizeBox.isEnabled() && (sizeBox.getItemCount() > 0)) {
            newAttrs.add((Media) (sizeBox.getSelectedItem()));
        } else {
            removeAttribute(Media.class);
        }
    }

    private void updateOrientation() {
        if (isOrientSupported()) {
            newAttrs.add(getOrient());
        } else {
            removeAttribute(OrientationRequested.class);
        }
    }

    private boolean updateMargins() {
        float x1;
        float y1;
        float x2;
        float y2;
        NumberFormatter format = getFloatFormatter();
        if (!leftTxt.isEnabled()) {
            removeAttribute(MediaPrintableArea.class);
            removeAttribute(MediaMargins.class);
            return true;
        }
        try {
            x1 = ((Float) format.stringToValue(leftTxt.getText())).floatValue();
            x2 = ((Float) format.stringToValue(rightTxt.getText())).floatValue();
            y1 = ((Float) format.stringToValue(topTxt.getText())).floatValue();
            y2 = ((Float) format.stringToValue(bottomTxt.getText())).floatValue();
        } catch (ParseException e) {
            return false;
        }
        if (sizeBox.isEnabled() && (sizeBox.getSelectedItem() instanceof MediaSizeName) && myService.isAttributeCategorySupported(MediaPrintableArea.class)) {
            MediaSize mediaSize = MediaSize.getMediaSizeForName((MediaSizeName) sizeBox.getSelectedItem());
            float paperWidth = mediaSize.getX(Size2DSyntax.MM);
            float paperHeight = mediaSize.getY(Size2DSyntax.MM);
            if ((x1 + x2 >= paperWidth) || (y1 + y2 >= paperHeight)) {
                return false;
            }
            newAttrs.add(new MediaPrintableArea(x1, y1, paperWidth - x1 - x2, paperHeight - y1 - y2, MediaPrintableArea.MM));
        } else {
            removeAttribute(MediaPrintableArea.class);
        }
        if (myService.isAttributeCategorySupported(MediaMargins.class)) {
            newAttrs.add(new MediaMargins(x1, y1, x2, y2, MediaMargins.MM));
        } else {
            removeAttribute(MediaMargins.class);
        }
        return true;
    }

    private void updateColor() {
        if (monoBtn.isEnabled() && monoBtn.isSelected()) {
            newAttrs.add(Chromaticity.MONOCHROME);
        } else if (colorBtn.isEnabled() && colorBtn.isSelected()) {
            newAttrs.add(Chromaticity.COLOR);
        } else {
            removeAttribute(Chromaticity.class);
        }
    }

    private void updateQuality() {
        if (isQualitySupported()) {
            newAttrs.add(getSelectedQuality());
        } else {
            removeAttribute(PrintQuality.class);
        }
    }

    private void updateSides() {
        if (isSidesSupported()) {
            newAttrs.add(getSelectedSide());
        } else {
            removeAttribute(Sides.class);
        }
    }

    private void updateBannerPage() {
        if (bannerBox.isEnabled()) {
            newAttrs.add(bannerBox.isSelected() ? JobSheets.STANDARD : JobSheets.NONE);
        } else {
            removeAttribute(JobSheets.class);
        }
    }

    private void updatePriority() {
        if (prtSpinner.isEnabled()) {
            int priority = ((Integer) (prtSpinner.getValue())).intValue();
            newAttrs.add(new JobPriority(priority));
        } else {
            removeAttribute(JobPriority.class);
        }
    }

    private void updateJobName() {
        if (jobNameTxt.isEnabled()) {
            String name = jobNameTxt.getText();
            if (name.length() == 0) {
                removeAttribute(JobName.class);
            } else {
                newAttrs.add(new JobName(name, Locale.getDefault()));
            }
        } else {
            removeAttribute(JobName.class);
        }
    }

    private void updateUserName() {
        if (userNameTxt.isEnabled()) {
            String name = userNameTxt.getText();
            if (name.length() == 0) {
                removeAttribute(RequestingUserName.class);
            } else {
                newAttrs.add(new RequestingUserName(name, Locale.getDefault()));
            }
        } else {
            removeAttribute(RequestingUserName.class);
        }
    }

    private void removeAttribute(Class cls) {
        if (newAttrs.containsKey(cls)) {
            newAttrs.remove(cls);
        }
    }

    private JPanel getPageDialogPanel() {
        JPanel pageDialogPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gridBagConstraints182 = new GridBagConstraints();
        GridBagConstraints gridBagConstraints172 = new GridBagConstraints();
        pageDialogPanel.setPreferredSize(new java.awt.Dimension(100, 100));
        pageDialogPanel.setSize(532, 389);
        gridBagConstraints172.gridx = 0;
        gridBagConstraints172.gridy = 1;
        gridBagConstraints172.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints172.gridwidth = 3;
        gridBagConstraints182.gridx = 0;
        gridBagConstraints182.gridy = 0;
        gridBagConstraints182.weightx = 1.0;
        gridBagConstraints182.weighty = 1.0;
        gridBagConstraints182.fill = java.awt.GridBagConstraints.BOTH;
        pageDialogPanel.add(getButtonsPanel(), gridBagConstraints172);
        pageDialogPanel.add(getPageSetupPanel(), gridBagConstraints182);
        return pageDialogPanel;
    }

    private class DestinationChooser extends JFileChooser {

        private static final long serialVersionUID = 5429146989329327138L;

        public void approveSelection() {
            boolean doesFileExists = false;
            boolean result = true;
            try {
                doesFileExists = getSelectedFile().exists();
            } catch (Exception e) {
            }
            if (doesFileExists) {
                FilePermission delPermission = new FilePermission(getSelectedFile().getAbsolutePath(), "delete");
                if (checkFilePermission(delPermission)) {
                    String msg = "File " + getSelectedFile() + " is already exists.\n" + "Do you want to overwrite it?";
                    int approveDelete = JOptionPane.showConfirmDialog(null, "File exists!", msg, JOptionPane.YES_NO_OPTION);
                    result = (approveDelete == JOptionPane.YES_OPTION);
                } else {
                    JOptionPane.showMessageDialog(null, "Can not delete file " + getSelectedFile());
                    result = false;
                }
            }
            if (result) {
                super.approveSelection();
            }
        }
    }
}
