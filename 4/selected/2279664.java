package com.foursoft.foureveredit.view.impl.swing.action;

import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import com.foursoft.fourever.xmlfileio.Fragment;
import com.foursoft.foureveredit.controller.impl.ControllerManagerImpl;
import com.foursoft.foureveredit.view.HTMLView;
import com.foursoft.foureveredit.view.impl.swing.HTMLViewStateChangeEvent;
import com.foursoft.foureveredit.view.impl.swing.HTMLViewStateChangeListener;
import com.foursoft.foureveredit.view.impl.swing.ImageChooserDialog;
import com.foursoft.foureveredit.view.impl.swing.SwingUtils;
import com.foursoft.foureveredit.view.impl.swing.ViewManagerImpl;
import com.foursoft.foureveredit.view.impl.swing.editorcomponent.HTMLComponentUtils;
import com.foursoft.mvc.controller.Controller;

/**
 * @author kivlehan_adm
 * 
 * InsertImageAction which is designed to be used in a regular JButton.
 */
public class InsertImageAction extends ExtendedAbstractAction implements PropertyChangeListener, HTMLViewStateChangeListener {

    private HTMLView currentHTMLView = null;

    private boolean isChanging = false;

    private Fragment currentFragment = null;

    private static final String ATTRIBUTE_NAME = "image";

    /**
	 *  
	 */
    public InsertImageAction(String name, Controller controller) {
        super(name);
        controller.addControllerPropertyListener("focusedView", this);
        controller.addControllerPropertyListener("currentFragment", this);
        putValue(SMALL_ICON, ViewManagerImpl.getImageIcon("image_icon"));
        setEnabled(false);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        assert (evt.getPropertyName().equals("focusedView") || evt.getPropertyName().equals("currentFragment"));
        if (!isChanging) {
            isChanging = true;
            if (evt.getPropertyName().equals("focusedView") && ((evt.getNewValue() == null) || (evt.getNewValue() instanceof HTMLView))) {
                boolean localScopeEnabled = false;
                if (currentHTMLView != null) {
                    currentHTMLView.removeStateChangeListener(this, ATTRIBUTE_NAME);
                }
                currentHTMLView = (HTMLView) evt.getNewValue();
                if (currentHTMLView != null) {
                    currentHTMLView.addStateChangeListener(this, ATTRIBUTE_NAME);
                }
                localScopeEnabled = evt.getNewValue() != null && currentHTMLView.isEditable();
                setEnabled(localScopeEnabled);
            } else if (evt.getPropertyName().equals("currentFragment")) {
                currentFragment = (Fragment) evt.getNewValue();
            }
            isChanging = false;
        }
    }

    public void actionPerformed(ActionEvent e) {
        Window parent = SwingUtils.getContainingWindow(e.getSource());
        if (!isChanging) {
            isChanging = true;
            try {
                File fileToOpenChooserWith = null;
                File existingImage = null;
                Fragment current = currentFragment;
                String altTextString = "";
                String exportwidthString = "";
                String exportheightString = "";
                String originalAlignment = null;
                String id = null;
                if (current == null) {
                    assert false : "Cant deal with images if we don't know their relative base";
                }
                String currentImageProperties = currentHTMLView.getState(ATTRIBUTE_NAME);
                if (currentImageProperties != null) {
                    exportwidthString = HTMLComponentUtils.getAttributeValue("exportwidth", currentImageProperties);
                    exportheightString = HTMLComponentUtils.getAttributeValue("exportheight", currentImageProperties);
                    altTextString = HTMLComponentUtils.getAttributeValue("alt", currentImageProperties);
                    originalAlignment = HTMLComponentUtils.getAttributeValue("align", currentImageProperties);
                    id = HTMLComponentUtils.getAttributeValue("id", currentImageProperties);
                    String existingSource = HTMLComponentUtils.getAttributeValue("src", currentImageProperties);
                    if (existingSource.startsWith("file:///")) {
                        existingSource = existingSource.substring("file:///".length());
                    }
                    existingImage = new File(existingSource);
                }
                File imagesDirectory = getCurrentImagesDirectory();
                if ((existingImage != null) && existingImage.exists()) {
                    fileToOpenChooserWith = existingImage;
                } else {
                    if (imagesDirectory.exists()) {
                        fileToOpenChooserWith = imagesDirectory;
                    } else {
                        int proceedWithcreate = JOptionPane.showConfirmDialog(parent, ViewManagerImpl.getMessage("com.foursoft.foureveredit.view.noImageDirectoryButCreateQuestion", new String[] { imagesDirectory.getAbsolutePath() }), ViewManagerImpl.getMessage("com.foursoft.foureveredit.view.createMissingimagesDirectory"), JOptionPane.YES_NO_OPTION);
                        if (proceedWithcreate == JOptionPane.NO_OPTION) {
                            JOptionPane.showMessageDialog(parent, ViewManagerImpl.getMessage("com.foursoft.foureveredit.view.cannotAddImageNoImageDirectory", new String[] { imagesDirectory.getAbsolutePath() }), ViewManagerImpl.getMessage("com.foursoft.foureveredit.view.cannotAddImage"), JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            imagesDirectory.mkdirs();
                            fileToOpenChooserWith = imagesDirectory;
                        }
                    }
                }
                if (fileToOpenChooserWith != null) {
                    Window win = SwingUtils.getContainingWindow(e.getSource());
                    ImageChooserDialog imageChooser = new ImageChooserDialog((win instanceof Frame) ? (Frame) win : null, fileToOpenChooserWith);
                    imageChooser.initialiseWithVariables(exportwidthString, exportheightString, originalAlignment, altTextString);
                    Point location = SwingUtils.getCenterLocationForDialog(win, imageChooser);
                    imageChooser.setLocation(location);
                    imageChooser.setVisible(true);
                    File chosenFile = imageChooser.getChosenImageFile();
                    String altText = imageChooser.getAltText();
                    String exportWidth = imageChooser.getImageWidth();
                    String exportHeight = imageChooser.getImageHeight();
                    String alignment = imageChooser.getImageAlignment();
                    if ((id == null) || id.trim().length() == 0) {
                        id = Long.toString(System.currentTimeMillis());
                    }
                    if (chosenFile != null) {
                        boolean imageInCorrectDirectory = false;
                        if (chosenFile.getParentFile().equals(imagesDirectory)) {
                            imageInCorrectDirectory = true;
                        } else {
                            ControllerManagerImpl.getLog().debug("Copying " + chosenFile.getName() + " from " + chosenFile.getParent() + " to " + imagesDirectory);
                            imageInCorrectDirectory = copyImageToImagesFolder(chosenFile, imagesDirectory, parent);
                            chosenFile = new File(imagesDirectory, chosenFile.getName());
                        }
                        if (imageInCorrectDirectory) {
                            int imageDisplayWidth = 0;
                            int imageDisplayHeight = 0;
                            ImageIcon instantiatedImage = new ImageIcon(chosenFile.getAbsolutePath());
                            int actualImageWidth = instantiatedImage.getIconWidth();
                            int actualImageHeight = instantiatedImage.getIconHeight();
                            float maxWidth = 600;
                            maxWidth = Math.max(maxWidth, 200);
                            if ((actualImageWidth <= maxWidth)) {
                                imageDisplayWidth = actualImageWidth;
                                imageDisplayHeight = actualImageHeight;
                            } else {
                                imageDisplayWidth = (int) maxWidth;
                                imageDisplayHeight = (int) ((actualImageHeight * maxWidth) / actualImageWidth);
                            }
                            String href = chosenFile.getAbsolutePath();
                            href = href.replace('\\', '/');
                            StringBuffer buff = new StringBuffer();
                            buff.append("src=\"" + "file:///" + href + "\"");
                            buff.append(" width=\"" + imageDisplayWidth + "\"");
                            buff.append(" height=\"" + imageDisplayHeight + "\"");
                            buff.append(" id=\"" + id + "\" ");
                            if (alignment != null) {
                                buff.append(" align=\"" + alignment + "\"");
                            }
                            if (altText != null) {
                                buff.append(" alt=\"" + altText + "\"");
                            }
                            if ((exportWidth != null) && (exportWidth.length() > 0)) {
                                buff.append(" exportwidth=\"" + exportWidth + "\"");
                            }
                            if ((exportHeight != null) && (exportHeight.length() > 0)) {
                                buff.append(" exportheight=\"" + exportHeight + "\"");
                            }
                            currentHTMLView.setState(ATTRIBUTE_NAME, buff.toString());
                        }
                    }
                }
            } catch (Exception ex) {
                setEnabled(false);
            }
            isChanging = false;
        }
    }

    /***************************************************************************
	 * Get the Images directory for the current fragment's location. The images
	 * folder is returned even if it does not yet exist.
	 **************************************************************************/
    private File getCurrentImagesDirectory() {
        File currentImagesDirectory = null;
        if (currentFragment != null) {
            File xmlFile = currentFragment.getLocation();
            String imageSubDirName = getImageDirName();
            assert imageSubDirName != null;
            String parentDirString;
            try {
                File xmlFileCopy = new File(xmlFile.getCanonicalPath());
                parentDirString = xmlFileCopy.getParent();
            } catch (Exception ex) {
                parentDirString = xmlFile.getParent();
            }
            assert parentDirString != null;
            String imagesDirectoryString = parentDirString + File.separator + imageSubDirName;
            File imagesDirectory = new File(imagesDirectoryString);
            currentImagesDirectory = imagesDirectory;
        } else {
            assert false : "shouldnt get this far if no fragment/ xml file, is specified ";
        }
        return currentImagesDirectory;
    }

    /**
	 * Copy the image at the location originalLocation to the images folder that
	 * rests beside the XML file (i.e. a position relative to it) A message will
	 * be displayed in a dialog to notify of success or failure of copy.
	 * 
	 * @param originalLocation,
	 * @param imageFolder 
	 * @param parentWindow optional parameter to pass the parent window, frame or dialog, 
	 * which will make any dialogs in this dialog act correctly in a modal manner and also 
	 * take the common icon for the top left corner. 
	 * @return boolean success
	 */
    private boolean copyImageToImagesFolder(File originalLocation, File imageFolder, Window parentWindow) {
        boolean imageCopied = false;
        if ((originalLocation != null) && (originalLocation.getAbsolutePath() != null) && (originalLocation.getAbsolutePath().lastIndexOf(File.separator) != -1)) {
            String correctImageFolder;
            String chosenImageFolder;
            boolean isImageInCorrectFolder = false;
            try {
                correctImageFolder = imageFolder.getCanonicalPath();
                chosenImageFolder = originalLocation.getCanonicalPath().substring(0, originalLocation.getCanonicalPath().lastIndexOf(File.separator));
                isImageInCorrectFolder = correctImageFolder.equals(chosenImageFolder);
            } catch (Exception ex) {
                ViewManagerImpl.getLog().warn("Cannot use canonical path to compare image folders. Will use Absolute paths instead");
                correctImageFolder = imageFolder.getAbsolutePath();
                chosenImageFolder = originalLocation.getAbsolutePath().substring(0, originalLocation.getAbsolutePath().lastIndexOf(File.separator));
                isImageInCorrectFolder = correctImageFolder.equals(chosenImageFolder);
            }
            if (!isImageInCorrectFolder) {
                imageCopied = copyImageImpl(originalLocation.getName(), chosenImageFolder, correctImageFolder);
                if (imageCopied) {
                    originalLocation = new File(correctImageFolder + File.separator + originalLocation.getName());
                    JOptionPane.showMessageDialog(null, ViewManagerImpl.getMessage("com.foursoft.foureveredit.view.imageSucessfullyCopied", new Object[] { originalLocation.getName(), chosenImageFolder, correctImageFolder }));
                } else {
                    JOptionPane.showMessageDialog(null, ViewManagerImpl.getMessage("com.foursoft.foureveredit.view.imageCopyFailed", new Object[] { originalLocation.getName(), correctImageFolder, chosenImageFolder }));
                }
            }
        }
        return imageCopied;
    }

    /**
	 * 
	 * do the actual image copying.
	 * 
	 * @param imageName
	 * @param sourceFolder
	 * @param destinationFolder
	 * 
	 * @return
	 */
    private boolean copyImageImpl(String imageName, String sourceFolder, String destinationFolder) {
        boolean successful = false;
        FileCopy copier = new FileCopy();
        try {
            File existingFileInTargetFolder = new File(destinationFolder + File.separator + imageName);
            int proceed = JOptionPane.YES_OPTION;
            if (existingFileInTargetFolder.exists()) {
                proceed = JOptionPane.showConfirmDialog(null, ViewManagerImpl.getMessage("com.foursoft.foureveredit.view.proceedWithOverwrite", new Object[] { imageName, destinationFolder }), ViewManagerImpl.getMessage("com.foursoft.foureveredit.view.proceedWithOverwriteTitle"), JOptionPane.YES_NO_OPTION);
            }
            if (proceed == JOptionPane.YES_OPTION) {
                copier.copy(sourceFolder + File.separator + imageName, destinationFolder + File.separator + imageName);
                successful = true;
            }
        } catch (IOException ioExc) {
            successful = false;
            ViewManagerImpl.getLog().error("problem in copying image file", ioExc);
        }
        return successful;
    }

    /**
	 * evaluate the location of all the embedded images relative to the file
	 * which is opened.
	 */
    private String getImageDirName() {
        return "images";
    }

    public void stateChanged(HTMLViewStateChangeEvent stateChangeEvent) {
        assert (stateChangeEvent.getSource() == currentHTMLView);
        if (!isChanging) {
            isChanging = true;
            try {
                setEnabled(stateChangeEvent.getSource().isEditable() && stateChangeEvent.getSource().canModifyState(ATTRIBUTE_NAME));
            } catch (Exception ex) {
                setEnabled(false);
            }
            isChanging = false;
        }
    }

    /**
	 * <<Description>>
	 * 
	 * @author <<unknown>>
	 * @version $Revision: 1.8 $
	 */
    public class FileCopy {

        /**
		 * 
		 * Copy a file from one location to another. If the file already exists
		 * at the destination then it is overwritten. It is up to the caller to
		 * check that the user is happy that the file is overwritten!
		 * 
		 * @param source_name
		 * @param dest_name
		 * 
		 * @throws IOException
		 */
        void copy(String source_name, String dest_name) throws IOException {
            File source_file = new File(source_name);
            File destination_file = new File(dest_name);
            FileInputStream source = null;
            FileOutputStream destination = null;
            byte[] buffer;
            int bytes_read;
            try {
                if (!source_file.exists() || !source_file.isFile()) {
                    throw new FileCopyException("FileCopy: no such source file: " + source_name);
                }
                if (!source_file.canRead()) {
                    throw new FileCopyException("FileCopy: source file " + "is unreadable: " + source_name);
                }
                if (!destination_file.exists()) {
                    File parentdir = parent(destination_file);
                    if (!parentdir.exists()) {
                        throw new FileCopyException("FileCopy: destination " + "directory doesn't exist: " + dest_name);
                    }
                    if (!parentdir.canWrite()) {
                        throw new FileCopyException("FileCopy: destination " + "directory is unwriteable: " + dest_name);
                    }
                }
                source = new FileInputStream(source_file);
                destination = new FileOutputStream(destination_file);
                buffer = new byte[1024];
                while (true) {
                    bytes_read = source.read(buffer);
                    if (bytes_read == -1) {
                        break;
                    }
                    destination.write(buffer, 0, bytes_read);
                }
            } finally {
                if (source != null) {
                    try {
                        source.close();
                    } catch (IOException e) {
                    }
                }
                if (destination != null) {
                    try {
                        destination.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        /**
		 * File.getParent() can return null when the file is specified without a
		 * directory or is in the root directory. This method handles those
		 * cases.
		 * 
		 * @param f
		 *            file to open
		 * 
		 * @return
		 */
        File parent(File f) {
            String dirname = f.getParent();
            if (dirname == null) {
                if (f.isAbsolute()) {
                    return new File(File.separator);
                }
                return new File(System.getProperty("user.dir"));
            }
            return new File(dirname);
        }
    }

    class FileCopyException extends IOException {

        /**
		 * 
		 * @param msg
		 */
        public FileCopyException(String msg) {
            super(msg);
        }
    }
}
