package org.gruposp2p.dnie.server.extras;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Helper class the provides file system access
 * services to the Service layer
 * 
 * Based on http://www.developer.com/img/2009/10/CardDealerHelper.java 
 */
public class FileSystemHelper {

    /**
     * The image directory location file spec. This member variable is set using
     * Spring injection
     * */
    private static String imageDirectoryLocationFileSpec;

    /**
     * The empty image file.
     * */
    private static String emptyImageFileSpec;

    /**
     * The empty file specification is the filename of the 
     * graphic that is to be displayed when theres no image associated.
     * 
     * @return the emptyImageFileSpec
     */
    public String getEmptyImageFileSpec() {
        return emptyImageFileSpec;
    }

    /**
     * This method provides static access to emptyImageFileSpec. This image
     * is the one that is to be displayed when there's no image
     * 
     * @return emptyImageFileSpec
     */
    public static String getEmptyImageFilename() {
        return emptyImageFileSpec;
    }

    /**
     * This method returns the location on the hosting computer of the directory
     * that has the image files that correspond the playing cards to be
     * displayed.
     * 
     * @return the image directory location file spec
     */
    public String getImageDirectoryLocationFileSpec() {
        return imageDirectoryLocationFileSpec;
    }

    /**
     * This method is a static proxy for getImageDirectoryLocation().
     * 
     * @return the image directory location
     * 
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static String getImageDirectoryLocation() throws IOException {
        return imageDirectoryLocationFileSpec;
    }

    /**
     * This method returns a list of image files in the image directory as
     * defined by imageDirectoryLocationFileSpec.
     * 
     * @return the images directory file names
     * 
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static String[] getImagesDirectoryFileNames() throws IOException {
        String imagesDirectory = getImageDirectoryLocation();
        File f = new File(imagesDirectory);
        return f.list();
    }

    /**
     * This is a utlitty method.
     * 
     * @param fileNames
     *            the file names
     * 
     * @return the array list< string>
     */
    public static ArrayList<String> copyToArrayList(ArrayList<String> arraylist) {
        ArrayList<String> list = new ArrayList<String>();
        for (String str : arraylist) {
            list.add(str);
        }
        return list;
    }

    /**
     * This method converts a image file to a byte array
     * 
     * @param imageFileSpec, the location of the image file on disk
     * @return a Byte Array that is the contents of a given image file
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static byte[] getImageBytesFromFileSpec(String imageFileSpec) throws FileNotFoundException, IOException {
        InputStream inputStream = new FileInputStream(new File(imageFileSpec));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(2048);
        byte[] bytes = new byte[2048];
        int readBytes;
        while ((readBytes = inputStream.read(bytes)) > 0) {
            outputStream.write(bytes, 0, readBytes);
        }
        byte[] byteData = outputStream.toByteArray();
        inputStream.close();
        outputStream.close();
        return byteData;
    }
}
