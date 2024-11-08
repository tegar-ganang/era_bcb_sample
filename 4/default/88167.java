import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;
import org.apache.log4j.Level;
import fi.hip.gb.bluetooth.coordconv.LatitudeLongitude;
import fi.hip.gb.client.Main;
import fi.hip.gb.mobile.AgentApi;
import fi.hip.gb.mobile.AgentUndeployer;
import fi.hip.gb.mobile.MobileAgent;
import fi.hip.gb.utils.FileUtils;
import fi.hip.gb.utils.JpegImage;

/**
 * Agent for resizing/cropping/compressing a image 
 * from remote place.
 * 
 * @author Juho Karppinen
 */
@MobileAgent
public class ImageGallery {

    /** centerpoint, values are latitude, longitude, and radius in meters (for example 46.3,6.1,10000)*/
    public static final String CENTER = "CENTER";

    /** maximum size for the image (for example 120x120) */
    public static final String MAXSIZE = "MAXSIZE";

    /** area to be cropped, values (start_x, start_y, width, height) are between 0 and 1 (0,0,1,1 crops nothing) */
    public static final String CROP = "CROP";

    /** scale the image, between 0 and 1 */
    public static final String SCALE = "SCALE";

    /** jpeg compression level, can be LOW, MEDIUM, HIGH, ORIGINAL or any number between 0 and 100 */
    public static final String QUALITY = "QUALITY";

    /** scaled from original image, from 0 to 1 */
    private double scale = 1.f;

    /** properties used for image construction */
    private HashMap<String, String> lastProps = null;

    /** Last processed file name */
    private String targetFile = null;

    public ImageGallery() {
    }

    /**
     * Remote the agent from the server.
     */
    @AgentUndeployer
    public void undeploy() {
    }

    /**
     * Process the image. Use {@link ImageGallery#getImage()} to fetch 
     * the result file.
     * 
     * @param filePath path to the file, relative to storage directory or absolute if starts
     * with slash
     * @param center center point (latitude, longitude, meters)
     * @param crop cropped area, (start_x, start_y, width, height) are between 0 and 1 (0,0,1,1 crops nothing) 
     * @param maxsize maximum size XxY where X and Y in pixels
     * @param quality quality (LOW, MEDIUM, HIGH, ORIGINAl or integer 0-100)
     * @param scale scaling between 0 and 1
     * @return result files
     */
    public File[] processImage(String filePath, String center, String crop, String maxsize, String quality, String scale) throws IOException {
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(CENTER, center);
        props.put(CROP, crop);
        props.put(MAXSIZE, maxsize);
        props.put(QUALITY, quality);
        props.put(SCALE, scale);
        return processImage(filePath, props);
    }

    /**
	 * Process the image. Use {@link ImageGallery#getImage()} to fetch 
     * the result file.
     * 
     * @param filePath path to the file, relative to storage directory or absolute if starts
     * with slash
     * @param prop properties
     * @return result files
	 */
    public File[] processImage(String filePath, HashMap<String, String> prop) throws IOException {
        System.out.println("PROPS" + prop.toString() + filePath);
        URL file = null;
        if (filePath.startsWith("/")) file = new URL("file:" + filePath); else file = new URL("file:" + AgentApi.getStorage() + "/" + filePath);
        return processImage(file, prop);
    }

    /**
	 * Process the image. Use {@link ImageGallery#getImage()} to fetch 
     * the result file.
	 * 
	 * @param fileURL URL of the result file
	 * @param props properties which are used to modify the image before it is
	 * returned
	 * @throws IOException if failed to process the image
     * @return result files
	 */
    private File[] processImage(URL fileURL, HashMap<String, String> props) throws IOException {
        this.lastProps = props;
        File imageFile = new File(fileURL.getFile());
        if (imageFile.exists() == false) throw new RemoteException("File " + fileURL + " not found");
        JpegImage img;
        float imageQuality = .75f;
        if (fileURL.getFile().toLowerCase().endsWith(".jpg")) {
            img = new JpegImage(fileURL);
        } else {
            throw new IOException("Unsupported filetype: " + fileURL);
        }
        String description = "Original image at " + AgentApi.getAPI().getHostname() + ":\n";
        description += "\tfile  " + fileURL.toString() + "\n";
        description += "\tsize " + img.getWidth() + "x" + img.getHeight() + " (" + FileUtils.getFormatedFileSize(fileURL) + ").\n\n";
        this.targetFile = FileUtils.getFilename(fileURL);
        if (this.targetFile.indexOf('.') != -1) {
            this.targetFile = this.targetFile.substring(0, this.targetFile.indexOf('.'));
        }
        this.targetFile = AgentApi.getAPI().getDirectory() + "/" + this.targetFile;
        String center = props.get(CENTER);
        if (center == null) center = "";
        StringTokenizer st = new StringTokenizer(center, ",", false);
        if (st.countTokens() == 3) {
            Float lat = LatitudeLongitude.parse(st.nextToken());
            Float lng = LatitudeLongitude.parse(st.nextToken());
            int radius = Integer.parseInt(st.nextToken());
            LatitudeLongitude ll = new LatitudeLongitude(lat, lng);
            this.targetFile += "_radius" + radius;
            description += "<br>Centered to " + ll.getLatitudeMinSec() + "x" + ll.getLongitudeMinSec();
            description += " with radius of " + radius + "m.";
        }
        String crop = props.get(CROP);
        if (crop == null) crop = "";
        st = new StringTokenizer(crop, ",", false);
        if (st.countTokens() == 4) {
            img.crop(Float.parseFloat(st.nextToken()), Float.parseFloat(st.nextToken()), Float.parseFloat(st.nextToken()), Float.parseFloat(st.nextToken()));
            this.targetFile += "_" + img.getWidth() + "x" + img.getHeight();
            description += "Cropped to size " + img.getWidth() + "x" + img.getHeight() + ".\n\n";
        }
        String size = props.get(MAXSIZE);
        if (size == null) size = "";
        String scale = props.get(SCALE);
        if (scale == null) scale = "";
        if (scale.length() > 0 || size.length() > 1) {
            String extraDescription = "";
            if (size.length() > 1) {
                int separator = size.indexOf('x');
                int width = Integer.parseInt(size.substring(0, separator));
                int height = Integer.parseInt(size.substring(separator + 1));
                double scaleH = (double) height / (double) img.getHeight();
                double scaleW = (double) width / (double) img.getWidth();
                this.scale = Math.min(scaleH, scaleW);
                extraDescription = " with bounds of " + width + "x" + height + ".";
            }
            if (scale.length() > 0) {
                this.scale = Double.parseDouble(scale);
            }
            if (this.scale < 1.f) {
                img.scalePercent(this.scale);
                description += "Scaled to " + (int) (this.scale * 100) + "% " + " (" + img.getWidth() + "x" + img.getHeight() + ")";
                description += extraDescription;
                description += ".\n\n";
            } else {
                this.scale = 1.f;
            }
            this.targetFile += "_scale" + (int) (this.scale * 100);
        }
        String qualityStr = props.get(QUALITY);
        if (qualityStr == null) qualityStr = "";
        if (qualityStr.equalsIgnoreCase("LOW")) {
            imageQuality = .30f;
        } else if (qualityStr.equalsIgnoreCase("MEDIUM")) {
            imageQuality = .75f;
        } else if (qualityStr.equalsIgnoreCase("HIGH")) {
            imageQuality = .95f;
        } else if (qualityStr.equalsIgnoreCase("ORIGINAL")) {
            imageQuality = 1.f;
        } else {
            try {
                imageQuality = Integer.parseInt(qualityStr) / 100.f;
            } catch (NumberFormatException nfe) {
            }
        }
        description += "Image quality is " + (int) (100 * imageQuality) + "%.\n\n";
        this.targetFile += "_q" + (int) (100 * imageQuality);
        this.targetFile += ".jpg";
        img.sendToFile(targetFile, imageQuality);
        File metaFile = new File(targetFile + ".txt");
        FileWriter fw = new FileWriter(metaFile);
        fw.write(description);
        fw.flush();
        fw.close();
        return new File[] { new File(targetFile), metaFile };
    }

    /**
     * Gets the description of image.
     * @return file name, cropping information, compression level etc.
     */
    public String getDescription() {
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader in = new BufferedReader(new FileReader(this.targetFile + ".txt"));
            String str;
            while ((str = in.readLine()) != null) {
                sb.append(str).append("\n");
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public File getDescriptionFile() {
        return new File(targetFile + ".txt");
    }

    /**
     * Gets the last used properties for image construction.
     * @return properties
     */
    public HashMap getProperties() {
        return this.lastProps;
    }

    /**
     * Gets the current scaling of the image
     * @return number between 0 and 1, 1 means original scale
     */
    public double getScale() {
        return this.scale;
    }

    /**
	 * Copies files into the persistant storage folder.
	 * 
	 * @param files all files to be uploaded
	 * @throws IOException if failed to copy files
	 */
    public void uploadImage(File[] files) throws IOException {
        for (File file : files) {
            FileUtils.copyFile(file.toURI().toURL(), new File(AgentApi.getStorage() + "/" + file.getName()));
        }
    }

    /**
     * For testing purposes.
     */
    public static void main(String[] args) throws Exception {
        new Main(Level.DEBUG);
        Properties prop = new Properties();
        prop.setProperty(MAXSIZE, "800x800");
        prop.setProperty(CENTER, "46d14m10.02sN,6d2m22.49sE,20000");
        prop.setProperty(CROP, "0.3,0.3,0.5,0.5");
    }
}
