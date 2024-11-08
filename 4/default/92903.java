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
import org.edg.data.spitfire.service.SpitfireResult;
import org.edg.data.spitfire.service.SpitfireRow;
import org.edg.data.spitfire.service.admin.SpitfireAdmin;
import org.edg.data.spitfire.service.admin.SpitfireAdminServiceLocator;
import org.edg.data.spitfire.service.base.SpitfireBase;
import org.edg.data.spitfire.service.base.SpitfireBaseServiceLocator;
import unosat.EcwImage;
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
 * Mobilecode for resizing/cropping/compressing and retrieving a image 
 * from remote place. File can be copied from local disk, from
 * Spitfire database with assosiated meta-data or using GSIFTP.
 * 
 * @author Juho Karppinen
 * @version $Id: ImageGallery.java 102 2004-11-12 14:31:37Z jkarppin $
 */
public final class ImageGallery extends DefaultProcessor implements Job {

    private String database = "GRID";

    private String table = "gallery";

    public static void main(String[] args) throws Exception {
        new Main(0);
        WorkDescription wds = new WorkDescription(new URL("http://moi"));
        wds.setJobID(new Long(0));
        wds.getSecurity().setProxyFile(Config.getProxyFile());
        wds.getExecutable().getFlags().setProperty("MAXSIZE", "400x400");
        wds.getExecutable().getFlags().setProperty("CROP", "0.5,0,0.5,1");
        AgentApi api = new AgentApi(new DefaultSession(Config.getServiceURL(), wds), null);
        Processor processor = new ImageGallery(api);
        processor.beforeFirstEvent();
        try {
            TextualEventData data = new TextualEventData("D:/Files/Pictures/SatelliteImages/Unosat/p196r028_7t20010721_z31_3218_virt_ecw.ecw", wds.getExecutable());
            processor.processEvent(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * @see fi.hip.gb.mobile.DefaultProcessor#DefaultProcessor(AgentApi)
	 */
    public ImageGallery(AgentApi api) {
        super(api);
    }

    /**
	 * @see fi.hip.gb.mobile.Processor#processEvent(fi.hip.gb.data.EventData)
	 */
    public void processEvent(final EventData d) throws RemoteException, Exception {
        TextualEventData se = (TextualEventData) d;
        String parameter = se.getData();
        Properties prop = se.getFlags();
        URL[] attachments = se.getAttachments();
        String database = prop.getProperty("DATABASE", "localhost");
        System.out.println("PROPS" + prop.toString());
        if (database.equalsIgnoreCase("file")) {
            processImage(new URL("file:" + _api.getStorage() + "/" + parameter), prop, parameter + "@" + _api.getHostname());
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
        targetFile = _api.getDirectory() + "/" + targetFile;
        StringTokenizer st = new StringTokenizer(prop.getProperty("CROP", ""), ",", false);
        if (st.countTokens() == 4) {
            int x = 0, y = 0, w = 0, h = 0;
            if (st.countTokens() == 4) {
                x = (int) (img.getWidth() * Float.parseFloat(st.nextToken()));
                y = (int) (img.getHeight() * Float.parseFloat(st.nextToken()));
                w = (int) (img.getWidth() * Float.parseFloat(st.nextToken()));
                h = (int) (img.getHeight() * Float.parseFloat(st.nextToken()));
            }
            img.crop(x, y, w, h);
            targetFile += "_" + img.getWidth() + "x" + img.getHeight();
            description += "<br>Cropped to size " + img.getWidth() + "x" + img.getHeight();
            description += " between points (" + x + "," + y + ") and (" + (x + w) + "," + (y + h) + ").";
        }
        String size = prop.getProperty("MAXSIZE", "");
        if (size.length() > 1) {
            int separator = size.indexOf('x');
            int width = Integer.parseInt(size.substring(0, separator));
            int height = Integer.parseInt(size.substring(separator + 1));
            double scaleH = (double) height / (double) img.getHeight();
            double scaleW = (double) width / (double) img.getWidth();
            double percent = Math.min(scaleH, scaleW);
            if (percent < 1.f) {
                img.scalePercent(percent);
                description += "<br>Scaled to " + (int) (percent * 100) + "% " + " (" + img.getWidth() + "x" + img.getHeight() + ")" + " with bounds of " + width + "x" + height + ".";
                System.out.println(description);
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
        _results.insertResult(res);
        _api.sendResults(_results);
        _api.waitInput();
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
            FileUtils.copyFile(files[i], new File(_api.getStorage() + "/" + outputFile));
            String outputURL = "file:" + _api.getStorage() + "/" + outputFile;
            int rows = sfBase.insert(database, table, new String[] { "name", "owner", "uploaded", "location", "keywords", "description" }, new String[] { filename, _api.getDescription().getInfo().getOwner(), new Timestamp(System.currentTimeMillis()).toString(), URLEncoder.encode(outputURL.toString(), "iso-8859-1"), prop.getProperty("KEYWORD"), parameter });
            if (rows > 0) _results.insertResult("succeeded", endpoint.getHost(), "inserted " + rows + " rows to database on " + endpoint.toString()); else _results.insertResult("failed", endpoint.getHost(), "no rows inserted to database on " + endpoint.toString());
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
        return new Object[][] { { "DATABASE", (Object) new String[] { "", "localhost:8443", "file" }, "URL for Spitfire database if not local host" }, { "MAXSIZE", (Object) new String[] { "", "120x120", "240x240", "480x480", "640x480", "800x600" }, "maximum size for the image" }, { "CROP", (Object) new String[] { "", "0,0,1,1" }, "area to be cropped, values (start_x, start_y, width, height) are between 0 and 1" }, { "QUALITY", (Object) new String[] { "", "LOW", "MEDIUM", "HIGH", "ORIGINAL" }, "jpeg compression level, can be also a number between 0 and 100" }, { "ADMIN", (Object) new String[] { "CREATE_TABLE", "DROP_TABLE" }, "Available admin services" } };
    }

    public Processor getProcessor() {
        return this;
    }

    public Observer getObserver() {
        return null;
    }

    public Combiner getCombiner() {
        return null;
    }
}
