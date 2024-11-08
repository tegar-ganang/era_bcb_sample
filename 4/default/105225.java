import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.xml.rpc.ServiceException;
import org.apache.axis.AxisProperties;
import org.apache.axis.configuration.EngineConfigurationFactoryDefault;
import org.apache.log4j.Level;
import org.edg.data.spitfire.service.SpitfireResult;
import org.edg.data.spitfire.service.SpitfireRow;
import org.edg.data.spitfire.service.admin.SpitfireAdmin;
import org.edg.data.spitfire.service.admin.SpitfireAdminServiceLocator;
import org.edg.data.spitfire.service.base.SpitfireBase;
import org.edg.data.spitfire.service.base.SpitfireBaseServiceLocator;
import unosat.EcwImage;
import fi.hip.gb.bluetooth.coordconv.LatitudeLongitude;
import fi.hip.gb.client.DefaultSession;
import fi.hip.gb.client.Main;
import fi.hip.gb.core.Config;
import fi.hip.gb.core.JobResult;
import fi.hip.gb.core.WorkDescription;
import fi.hip.gb.data.EventData;
import fi.hip.gb.data.TextualEventData;
import fi.hip.gb.mobile.AgentApi;
import fi.hip.gb.mobile.Combiner;
import fi.hip.gb.mobile.DefaultProcessor;
import fi.hip.gb.mobile.Job;
import fi.hip.gb.mobile.Observer;
import fi.hip.gb.mobile.Processor;
import fi.hip.gb.utils.FileUtils;
import fi.hip.gb.utils.JpegImage;

/**
 * Agent for resizing/cropping/compressing and retrieving a image 
 * from remote place. File can be copied from local disk, from
 * Spitfire database with assosiated meta-data or 
 * using GSIFTP (not yet implemented).
 * 
 * @author Juho Karppinen
 * @version $Id: ImageGallery.java 250 2005-05-18 11:38:18Z jkarppin $
 */
public final class ImageGallery extends DefaultProcessor implements Job {

    private String database = "GRID";

    private String table = "gallery";

    public static void main(String[] args) throws Exception {
        new Main(Level.DEBUG);
        WorkDescription wds = new WorkDescription(new URL("http://moi"));
        wds.setJobID(new Long(-1));
        wds.getSecurity().setProxyFile(Config.getProxyFile());
        wds.getExecutable().getFlags().setProperty("MAXSIZE", "800x800");
        wds.getExecutable().getFlags().setProperty("CENTER", "46d14m10.02sN,6d2m22.49sE,20000");
        wds.getExecutable().getFlags().setProperty("CROP", "0.3,0.3,0.5,0.5");
        AgentApi api = new AgentApi(new DefaultSession(Config.getServiceURL(), wds), null);
        Processor processor = new ImageGallery();
        processor.init(api);
        processor.beforeFirstEvent();
        try {
            TextualEventData data = new TextualEventData("../../p196r028_7t20010721_z31_3218_virt_ecw.ecw", wds.getExecutable());
            processor.processEvent(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ImageGallery() {
    }

    public void processEvent(final EventData d) throws RemoteException, Exception {
        TextualEventData se = (TextualEventData) d;
        String parameter = se.getData();
        Properties prop = se.getFlags();
        URL[] attachments = se.getAttachments();
        String database = prop.getProperty("DATABASE", "file");
        System.out.println("PROPS" + prop.toString());
        if (database.equalsIgnoreCase("file")) {
            processImage(new URL("file:" + this.api.getStorage() + "/" + parameter), prop, parameter + "@" + this.api.getHostname());
        } else {
            if (!database.startsWith("http")) {
                database = "https://" + database;
            }
            if (database.indexOf(':', database.indexOf("://") + 1) == -1) {
                database += (database.startsWith("http://") ? ":8080" : ":8443");
            }
            String admin = prop.getProperty("ADMIN");
            if (admin != null) {
                administrate(database, admin);
            } else {
                URL endpoint = new URL(database + "/Spitfire/services/SpitfireBase");
                AxisProperties.setProperty(EngineConfigurationFactoryDefault.OPTION_CLIENT_CONFIG_FILE, Config.getHomeDirectory() + "/WEB-INF/classes/client-config.wsdd");
                SpitfireBase sfBase = new SpitfireBaseServiceLocator().getSpitfireBase(endpoint);
                if (attachments.length > 0) {
                    uploadImage(attachments, sfBase, endpoint, parameter, prop);
                } else {
                    downloadImage(sfBase, endpoint, parameter, prop);
                }
            }
        }
        System.out.println("done");
    }

    /**
	 * Adds an image to the results which could be first resized/cropped/compressed
	 * if the description tells so.
	 * 
	 * @param fileURL URL of the result file
	 * @param prop properties which are used to modify the image before it is
	 * given to the user
	 * @param description some additional information about the result
	 * @throws IOException if failed to process the image
	 */
    private void processImage(URL fileURL, Properties prop, String description) throws IOException {
        File imageFile = new File(fileURL.getFile());
        if (imageFile.exists() == false) throw new RemoteException("File " + fileURL + " not found");
        JpegImage img;
        float imageQuality = .75f;
        if (fileURL.getFile().toLowerCase().endsWith(".ecw")) {
            img = new EcwImage(fileURL);
        } else if (fileURL.getFile().toLowerCase().endsWith(".jpg")) {
            img = new JpegImage(fileURL);
        } else {
            throw new IOException("Unsupported filetype: " + fileURL);
        }
        description += "<br>Original image size is " + img.getWidth() + "x" + img.getHeight() + " (" + FileUtils.getFormatedFileSize(fileURL) + ").";
        String targetFile = FileUtils.getFilename(fileURL);
        if (targetFile.indexOf('.') != -1) {
            targetFile = targetFile.substring(0, targetFile.indexOf('.'));
        }
        targetFile = this.api.getDirectory() + "/" + targetFile;
        StringTokenizer st = new StringTokenizer(prop.getProperty("CENTER", ""), ",", false);
        if (st.countTokens() == 3 && img instanceof EcwImage) {
            Float lat = LatitudeLongitude.parse(st.nextToken());
            Float lng = LatitudeLongitude.parse(st.nextToken());
            int radius = Integer.parseInt(st.nextToken());
            LatitudeLongitude ll = new LatitudeLongitude(lat, lng);
            ((EcwImage) img).center(lat.floatValue(), lng.floatValue(), radius);
            targetFile += "_radius" + radius;
            description += "<br>Centered to " + ll.getLatitudeMinSec() + "x" + ll.getLongitudeMinSec();
            description += " with radius of " + radius + "m.";
        }
        st = new StringTokenizer(prop.getProperty("CROP", ""), ",", false);
        if (st.countTokens() == 4) {
            ((EcwImage) img).crop(Float.parseFloat(st.nextToken()), Float.parseFloat(st.nextToken()), Float.parseFloat(st.nextToken()), Float.parseFloat(st.nextToken()));
            targetFile += "_" + img.getWidth() + "x" + img.getHeight();
            description += "<br>Cropped to size " + img.getWidth() + "x" + img.getHeight();
        }
        String size = prop.getProperty("MAXSIZE", "");
        String scale = prop.getProperty("SCALE", "");
        if (scale.length() > 0 || size.length() > 1) {
            double percent = 0;
            String extraDescription = "";
            if (size.length() > 1) {
                int separator = size.indexOf('x');
                int width = Integer.parseInt(size.substring(0, separator));
                int height = Integer.parseInt(size.substring(separator + 1));
                double scaleH = (double) height / (double) img.getHeight();
                double scaleW = (double) width / (double) img.getWidth();
                percent = Math.min(scaleH, scaleW);
                extraDescription = " with bounds of " + width + "x" + height + ".";
            } else {
                percent = Double.parseDouble(scale);
            }
            if (percent < 1.f) {
                img.scalePercent(percent);
                description += "<br>Scaled to " + (int) (percent * 100) + "% " + " (" + img.getWidth() + "x" + img.getHeight() + ")";
                description += extraDescription;
            } else {
                percent = 1.f;
            }
            targetFile += "_scale" + (int) (percent * 100);
        }
        String qualityStr = prop.getProperty("QUALITY", "");
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
        targetFile += "_q" + (int) (100 * imageQuality);
        description += "<br>Image quality is " + (int) (100 * imageQuality) + "%.";
        targetFile += ".jpg";
        img.sendToFile(targetFile, imageQuality);
        JobResult res = new JobResult(new URL("file:" + targetFile), prop, description);
        this.results.insertResult(res);
        this.api.sendResults(this.results);
        this.api.waitInput();
    }

    /**
	 * Administrate the database. This needs administrator priviledges to the
	 * Spitfire database
	 * @param hostname hostname of the Spitfire database
	 * @param operation operation to be executed, either CREATE_TABLE or DROP_TABLE
	 * @throws RemoteException if database operation failed
	 * @throws ServiceException if the spitfire service was not found 
	 */
    private void administrate(String hostname, String operation) throws RemoteException, ServiceException {
        URL endpoint = null;
        try {
            endpoint = new URL("https://" + hostname + "/Spitfire/services/SpitfireAdmin");
        } catch (MalformedURLException e) {
            throw new RemoteException("Endpoint URL: " + e.getMessage());
        }
        SpitfireAdmin sfAdmin = new SpitfireAdminServiceLocator().getSpitfireAdmin(endpoint);
        if (operation.equalsIgnoreCase("CREATE_TABLE")) {
            sfAdmin.createTable(database, table, new String[] { "name VARCHAR(64)", "owner VARCHAR(128)", "uploaded date", "location VARCHAR(128)", "keywords VARCHAR(255)", "description VARCHAR(255)" });
        } else if (operation.equalsIgnoreCase("DROP_TABLE")) {
            sfAdmin.dropTable(database, table);
        }
    }

    /**
	 * Copies given files into the persistant storage, and adds their
	 * metadata into the Spitfire database.
	 * <p>
	 * Adds successfull messages to the results. 
	 * 
	 * @param files all files to be saved, first element is always the 
	 * JAR file which will be skipped
	 * @param sfBase spitfire database
	 * @param endpoint the URL of the database
	 * @param parameter the given parameter which is the description for the data
	 * @param prop metadata which is added to the database 
	 * @throws RemoteException if failed to contact Spitfire server
	 * @throws IOException if failed to copy the file, or encode its URL
	 */
    private void uploadImage(URL[] files, SpitfireBase sfBase, URL endpoint, String parameter, Properties prop) throws RemoteException, IOException {
        for (int i = 0; i < files.length; i++) {
            String filename = FileUtils.getFilename(files[i]);
            String outputFile = System.currentTimeMillis() + "_" + filename;
            FileUtils.copyFile(files[i], new File(this.api.getStorage() + "/" + outputFile));
            String outputURL = "file:" + this.api.getStorage() + "/" + outputFile;
            int rows = sfBase.insert(database, table, new String[] { "name", "owner", "uploaded", "location", "keywords", "description" }, new String[] { filename, this.api.getDescription().getInfo().getOwner(), new Timestamp(System.currentTimeMillis()).toString(), URLEncoder.encode(outputURL.toString(), "iso-8859-1"), prop.getProperty("KEYWORD"), parameter });
            if (rows > 0) this.results.insertResult("succeeded", endpoint.getHost(), "inserted " + rows + " rows to database on " + endpoint.toString()); else this.results.insertResult("failed", endpoint.getHost(), "no rows inserted to database on " + endpoint.toString());
        }
    }

    /**
	 * Downloads files from the persistant storage which metadata on
	 * Spitfire database match the query.
	 * <p>
	 * Adds found files to the results. 
	 * 
	 * @param sfBase spitfire database
	 * @param endpoint the URL of the database
	 * @param parameter the given search string
	 * @param prop additional search criterias 
	 * @throws RemoteException if failed to contact Spitfire server
	 * @throws IOException if failed to copy the file, or decode its URL
	 */
    private void downloadImage(SpitfireBase sfBase, URL endpoint, String parameter, Properties prop) throws RemoteException, IOException {
        String column = prop.getProperty("QUERY", "DESCRIPTION");
        if (column.equalsIgnoreCase("all")) column = "name,owner,uploaded,location,keywords,description";
        SpitfireResult result = sfBase.select(database, new String[] { "name", "owner", "uploaded", "location", "keywords", "description" }, new String[] { table }, column + " like '%" + parameter + "%'", 0);
        for (int i = 0; i < result.getNumberOfRows(); i++) {
            SpitfireRow row = result.getRow(i);
            String name = row.getField(0);
            String owner = row.getField(1);
            String uploaded = row.getField(2);
            String location = URLDecoder.decode(row.getField(3), "iso-8859-1");
            String keywords = row.getField(4);
            String description = row.getField(5);
            String descriptions = "";
            if (name != null) descriptions += "filename " + name;
            if (owner != null) descriptions += " owned by " + owner;
            if (uploaded != null) descriptions += " uploaded on " + uploaded + " to database " + endpoint.getHost();
            if (keywords != null) descriptions += " keywords " + keywords;
            if (description != null) descriptions += " description " + description;
            System.out.println("found image " + location + " : " + descriptions);
            processImage(new URL(location), prop, descriptions);
        }
    }

    public String getDescription() {
        return "Distributed image gallery.";
    }

    public String[] getSupportedParameters() {
        return new String[] { "Query string for image filename" };
    }

    public Object[][] getSupportedFlags() {
        return new Object[][] { { "DATABASE", (Object) new String[] { "", "localhost:8443", "file" }, "URL for Spitfire database if not local host" }, { "MAXSIZE", (Object) new String[] { "", "120x120", "240x240", "480x480", "640x480", "800x600" }, "maximum size for the image" }, { "SCALE", (Object) new String[] { "", "0.10,0.30,0.50,0.80" }, "scale the image, between 0 and 1" }, { "CENTER", (Object) new String[] { "", "46.3,6.1,10000" }, "centerpoint, values are latitude, longitude, and radius in meters" }, { "CROP", (Object) new String[] { "", "0,0,1,1" }, "area to be cropped, values (start_x, start_y, width, height) are between 0 and 1" }, { "QUALITY", (Object) new String[] { "", "LOW", "MEDIUM", "HIGH", "ORIGINAL" }, "jpeg compression level, can be also a number between 0 and 100" }, { "ADMIN", (Object) new String[] { "CREATE_TABLE", "DROP_TABLE" }, "Available admin services" } };
    }

    public Processor getProcessor() {
        return this;
    }

    public Observer getObserver() {
        return new ImageObserver(this.api);
    }

    public Combiner getCombiner() {
        return null;
    }
}
