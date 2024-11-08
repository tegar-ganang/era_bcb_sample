package net.sf.greengary.xynch.exifsynch;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.media.jai.JAI;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;

public class Thumbnail extends JPanel implements ActionListener, FocusListener, ItemListener {

    public static final String MODIFY_CAMERA_TIME_ACTION_COMMAND = "modifyCameraTime";

    public static final String MODIFY_IMAGE_TIME_ACTION_COMMAND = "modifyImageTime";

    private static final int SPACING = 5;

    private static final long serialVersionUID = 1L;

    static final DateFormat m_dfExif = new java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

    static final DateFormat m_dfJhead = new java.text.SimpleDateFormat("yyyy:MM:dd-HH:mm:ss");

    Image m_thumbBI;

    private final GridBagConstraints m_Constraints = new GridBagConstraints();

    private Date m_exifTime;

    /** Time parsed from the filename or null */
    private final Date m_filenameTime;

    private final Date m_originalTime;

    private final File m_imageFile;

    private final ThumbnailManager m_tm;

    private boolean m_selected;

    private String cameraModel = "";

    private String cameraSerialId = "";

    private final JTextField m_exifTimeTextFieldOriginal;

    private final JTextField m_exifTimeTextField;

    private final JTextField m_fileNameTextField;

    private ThumbnailOwner m_owner;

    private final ThumbnailPopup m_popup = new ThumbnailPopup(this, this);

    private PhotoBranch m_branch;

    public Thumbnail(final File file, final ThumbnailManager tm) throws Exception {
        m_tm = tm;
        m_imageFile = file;
        m_branch = m_tm.getDefaultPhotoBranch();
        m_exifTimeTextFieldOriginal = new JTextField();
        m_exifTimeTextFieldOriginal.setEditable(false);
        m_exifTimeTextField = new JTextField();
        m_exifTimeTextField.setEditable(true);
        m_exifTimeTextField.setInputVerifier(new DateInputVerifier(m_dfExif));
        m_exifTimeTextField.addFocusListener(this);
        m_fileNameTextField = new JTextField(m_imageFile.getName());
        m_fileNameTextField.setHorizontalAlignment(JTextField.RIGHT);
        m_fileNameTextField.setEditable(false);
        addMouseListener(new ThumbnailMouseListener());
        m_filenameTime = parseFilenameTime(m_imageFile, m_tm);
        if (m_filenameTime != null) {
            m_popup.setFromFilenameTitle(m_filenameTime);
        }
        try {
            getMetaData();
        } catch (Exception e) {
            System.out.println("Failed to get meta data for " + file);
            m_exifTimeTextFieldOriginal.setText(e.getMessage());
        }
        if (m_exifTime != null) {
            m_originalTime = (Date) m_exifTime.clone();
        } else {
            m_originalTime = null;
        }
        final MouseListener popupListener = new PopupListener(m_popup);
        m_popup.addMouseListener(popupListener);
        this.addMouseListener(popupListener);
        this.setPreferredSize(new Dimension(m_tm.getThumbnailSize() + (2 * SPACING), m_tm.getThumbnailSize() + 200));
        this.setLayout(new java.awt.GridBagLayout());
        m_Constraints.insets = new Insets(m_tm.getThumbnailSize() + SPACING, SPACING, SPACING, SPACING);
        m_Constraints.fill = GridBagConstraints.HORIZONTAL;
        m_Constraints.anchor = GridBagConstraints.NORTH;
        m_Constraints.weighty = 0.0;
        m_Constraints.weightx = 1.0;
        m_Constraints.gridwidth = GridBagConstraints.REMAINDER;
        add(m_fileNameTextField, m_Constraints);
        m_Constraints.insets = new Insets(0, SPACING, SPACING, SPACING);
        add(m_exifTimeTextFieldOriginal, m_Constraints);
        m_Constraints.gridheight = GridBagConstraints.REMAINDER;
        m_Constraints.weighty = 1.0;
        add(m_exifTimeTextField, m_Constraints);
    }

    private static Date parseFilenameTime(final File imageFile, final ThumbnailManager tm) {
        try {
            final SimpleDateFormat df = new SimpleDateFormat(tm.getFilenameDateFormatString());
            final Date d = df.parse(imageFile.getName());
            if (d.getYear() < 100) {
                d.setYear(tm.getYear() - 1900);
            }
            return d;
        } catch (Exception e) {
        }
        return null;
    }

    public void setOwner(ThumbnailOwner owner) {
        this.m_owner = owner;
        updateColor();
    }

    public ThumbnailOwner getOwner() {
        return m_owner;
    }

    /** Parse exif tags. 
	 * Tries to fill missing date information by file name. 
	 * @throws ImageReadException */
    private void getMetaData() throws Exception {
        final IImageMetadata metadata = Sanselan.getMetadata(m_imageFile);
        if (metadata instanceof JpegImageMetadata) {
            final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            m_thumbBI = jpegMetadata.getEXIFThumbnail();
            final TiffField dateTimeOriginal = jpegMetadata.findEXIFValue(TiffConstants.EXIF_TAG_CREATE_DATE);
            if (dateTimeOriginal == null) {
                System.out.println("EXIF_TAG_CREATE_DATE not found.");
            } else {
                m_exifTime = m_dfExif.parse(dateTimeOriginal.getStringValue());
                m_exifTimeTextFieldOriginal.setText(m_dfExif.format(m_exifTime));
            }
            final TiffField cameraModelField = jpegMetadata.findEXIFValue(TiffConstants.EXIF_TAG_MODEL);
            if (cameraModelField == null) {
                System.out.println("EXIF_TAG_MODEL not found.");
            } else {
                cameraModel = cameraModelField.getStringValue();
            }
            cameraSerialId = readCameraSerialId(jpegMetadata);
        }
        if (m_exifTime == null && m_filenameTime != null) {
            m_exifTime = (Date) m_filenameTime.clone();
            m_exifTimeTextFieldOriginal.setText(m_dfExif.format(m_exifTime));
        }
        if (m_thumbBI == null) {
            m_thumbBI = createBufferedImage(m_imageFile, m_tm.getThumbnailSize());
        } else {
            System.out.println("Using thumbnail from file");
        }
    }

    /** Try to read something unique for the camera 
	 * @param jpegMetadata 
	 * @throws ImageReadException */
    private static String readCameraSerialId(JpegImageMetadata jpegMetadata) throws ImageReadException {
        TiffField field = jpegMetadata.findEXIFValue(TiffConstants.EXIF_TAG_CAMERA_SERIAL_NUMBER);
        if (field == null) {
            field = jpegMetadata.findEXIFValue(TiffConstants.EXIF_TAG_SERIAL_NUMBER);
        }
        if (field == null) {
            field = jpegMetadata.findEXIFValue(TiffConstants.EXIF_TAG_UNIQUE_CAMERA_MODEL);
        }
        if (field == null) {
            field = jpegMetadata.findEXIFValue(TiffConstants.EXIF_TAG_ARTIST);
        }
        if (field == null) {
            field = jpegMetadata.findEXIFValue(TiffConstants.EXIF_TAG_MODEL);
        }
        if (field == null) {
            field = jpegMetadata.findEXIFValue(TiffConstants.EXIF_TAG_MODEL_2);
        }
        if (field == null) {
            return "";
        } else {
            return field.getStringValue().trim();
        }
    }

    public void writeBack(File destinationFile, boolean makeCopy) throws IOException {
        if (makeCopy) {
            FileChannel sourceChannel = new java.io.FileInputStream(getFile()).getChannel();
            FileChannel destinationChannel = new java.io.FileOutputStream(destinationFile).getChannel();
            sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
            sourceChannel.close();
            destinationChannel.close();
        } else {
            getFile().renameTo(destinationFile);
        }
        if (getExifTime() != null && getOriginalTime() != null && !getExifTime().equals(getOriginalTime())) {
            String adjustArgument = "-ts" + m_dfJhead.format(getExifTime());
            ProcessBuilder pb = new ProcessBuilder(m_tm.getJheadCommand(), adjustArgument, destinationFile.getAbsolutePath());
            pb.directory(destinationFile.getParentFile());
            System.out.println(pb.command().get(0) + " " + pb.command().get(1) + " " + pb.command().get(2));
            final Process p = pb.start();
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        final java.awt.Graphics2D g2 = ((java.awt.Graphics2D) (g));
        m_exifTimeTextField.setText(getDateString());
        super.paint(g);
        final double scale = Math.min((double) m_tm.getThumbnailSize() / m_thumbBI.getWidth(this), (double) m_tm.getThumbnailSize() / m_thumbBI.getHeight(this));
        final java.awt.geom.AffineTransform afTranslate = new java.awt.geom.AffineTransform();
        afTranslate.setToTranslation(SPACING, SPACING);
        final java.awt.geom.AffineTransform afScaleTranslate = new java.awt.geom.AffineTransform();
        afScaleTranslate.setToScale(scale, scale);
        afScaleTranslate.concatenate(afTranslate);
        g2.drawImage(m_thumbBI, afScaleTranslate, this);
        if (m_selected) {
            java.awt.Color c = new java.awt.Color(0, 0, 0, 127);
            g2.setColor(c);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    String getDateString() {
        if (m_exifTime == null) {
            return "invalid date";
        } else {
            return m_dfExif.format(m_exifTime);
        }
    }

    private static BufferedImage createBufferedImage(File f, int thumbnailSize) {
        try {
            final RenderedImage ri = JAI.create("fileload", f.getPath());
            final double scale = Math.min((double) thumbnailSize / ri.getWidth(), (double) thumbnailSize / ri.getHeight());
            final BufferedImage bi = new BufferedImage((int) (scale * ri.getWidth()), (int) (scale * ri.getHeight()), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = (Graphics2D) (bi.getGraphics());
            g = (Graphics2D) (g.create(0, 0, bi.getWidth(), bi.getHeight()));
            java.awt.geom.AffineTransform af = new java.awt.geom.AffineTransform();
            af.setToScale(scale, scale);
            g.drawRenderedImage(ri, af);
            return bi;
        } catch (RuntimeException e) {
            System.out.println("File '" + f.toString() + "' could not be read.");
            return new BufferedImage(thumbnailSize, thumbnailSize, BufferedImage.TYPE_3BYTE_BGR);
        }
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getActionCommand() == "thumbUpdate") {
            m_thumbBI = createBufferedImage(m_imageFile, m_tm.getThumbnailSize());
            updateUI();
        } else if (ae.getActionCommand() == "show") {
            showPhotoInFrame();
        } else if (ae.getActionCommand() == "swapLeft") {
            m_tm.swapLeft(this);
        } else if (ae.getActionCommand() == "swapRight") {
            m_tm.swapRight(this);
        } else if (ae.getActionCommand() == MODIFY_CAMERA_TIME_ACTION_COMMAND) {
            m_tm.modifyCameraTime(((ModifierMenuItem) (ae.getSource())).getValue(), getOwner());
        } else if (ae.getActionCommand() == MODIFY_IMAGE_TIME_ACTION_COMMAND) {
            setOffset(((ModifierMenuItem) (ae.getSource())).getValue());
            m_tm.orderChanged();
        } else if (ae.getSource() instanceof BranchMenuItem) {
            BranchMenuItem selectedBranch = (BranchMenuItem) (ae.getSource());
            m_tm.branch(this, selectedBranch.getPhotoBranch());
        } else if (ae.getActionCommand() == "fromFilename") {
            if (m_filenameTime != null) {
                m_exifTime = (Date) m_filenameTime.clone();
                m_tm.orderChanged();
            }
        }
    }

    private void showPhotoInFrame() {
        final RenderedImage ri = JAI.create("fileload", m_imageFile.getPath());
        final ShowPhotoFrame sp = new ShowPhotoFrame(m_imageFile.getPath(), ri, m_thumbBI);
        sp.setVisible(true);
    }

    public Date getExifTime() {
        return m_exifTime;
    }

    public Object getOriginalTime() {
        return m_originalTime;
    }

    public static void swapExifTime(Thumbnail t1, Thumbnail t2) {
        final Date tmp = t1.m_exifTime;
        t1.m_exifTime = t2.m_exifTime;
        t2.m_exifTime = tmp;
    }

    public void focusGained(FocusEvent arg0) {
    }

    public void focusLost(FocusEvent arg) {
        try {
            m_exifTime = m_dfExif.parse(m_exifTimeTextField.getText());
            m_tm.orderChanged();
        } catch (ParseException e) {
        }
    }

    public void setOffset(int offset) {
        if (m_exifTime != null) {
            m_exifTime = org.apache.commons.lang.time.DateUtils.addSeconds(m_exifTime, offset);
            System.out.println("Add " + offset + "s to image " + m_imageFile);
            repaint();
        }
    }

    /** Checkbox "select for synch"
	 * @param ie Item triggering event
	 */
    public void itemStateChanged(ItemEvent ie) {
        setSelecterForSynch(ie.getItemSelectable().getSelectedObjects() != null);
        if (isSelecterForSynch()) {
            m_tm.selectForSynchCalled(this);
        }
    }

    public boolean isSelecterForSynch() {
        return m_selected;
    }

    public void setSelecterForSynch(boolean selected) {
        m_selected = selected;
        m_popup.setSelectedForSynch(selected);
        updateUI();
    }

    void updateBranchMenu(BranchProvider bp) {
        m_popup.updateBranchMenu(bp, this);
    }

    public PhotoBranch getPhotoBranch() {
        return m_branch;
    }

    public void setPhotoBranch(PhotoBranch branch) {
        m_branch = branch;
    }

    public File getFile() {
        return m_imageFile;
    }

    class ThumbnailMouseListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent me) {
            if (me.getButton() == MouseEvent.BUTTON1) {
                final Cursor previousCursor = getCursor();
                try {
                    setCursor(new Cursor(Cursor.WAIT_CURSOR));
                    m_thumbBI = createBufferedImage(m_imageFile, m_tm.getThumbnailSize());
                    updateUI();
                    showPhotoInFrame();
                } finally {
                    setCursor(previousCursor);
                }
            }
        }
    }

    public String getCameraModel() {
        return cameraModel;
    }

    public String getCameraSerialId() {
        return cameraSerialId;
    }

    void updateColor() {
        if (m_owner != null && m_owner.getColor() != null) {
            setBackground(m_owner.getColor());
        }
    }
}
