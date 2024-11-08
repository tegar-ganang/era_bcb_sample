package penguin.helpers;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.zip.*;
import java.nio.channels.*;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import com.drew.metadata.*;
import com.drew.imaging.jpeg.*;
import penguin.Program;
import penguin.dataset.NestImage;
import penguin.dataset.Site;
import penguin.dataset.Template;
import penguin.gui.LoadingBar;
import penguin.gui.LogWindow;
import org.apache.xmlbeans.XmlException;
import org.penguinuri.penguin.*;
import org.penguinuri.siteSeasonDatabase.SiteSeasonDBDocument;
import org.penguinuri.siteSeasonDatabase.SiteSeasonDBDocument.SiteSeasonDB;
import org.penguinuri.siteSeasonDatabase.SiteSeasonDBDocument.SiteSeasonDB.Sites;

/**
 * File System utility class.
 * 
 * @author Tim Dunstan
 * @author Josh Dwyer
 *
 */
public abstract class FileSystem {

    public static class LocationMissingException extends Exception {

        public LocationMissingException(Exception ex) {
        }

        @Override
        public String getMessage() {
            return "No location information in zip file, you may need to manually add this data.";
        }
    }

    private static int BUFFER_LEN = 1024;

    public static final char[] ILLEGAL_CHARACTERS = { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' };

    /**
	 * Returns a list of the current seasons.
	 * @return
	 */
    public static String[] getSeasons() {
        File location = new File(Program.Settings().getDataFolder());
        if (!location.mkdir()) {
            FilenameFilter filter = new DirectoryFileFilter();
            return location.list(filter);
        }
        return null;
    }

    /** 
	 * Returns a list of seasons which belong to a site.
	 * 
	 * @param site The site to which the seasons belong.
	 * @return an array of seasons names as a string array.
	 */
    public static String[] getSeasons(final String site) {
        File location = new File(Program.Settings().getDataFolder());
        File[] allSeasons = location.listFiles(new DirectoryFileFilter());
        LinkedList<String> seasons = new LinkedList<String>();
        if (location.exists()) {
            for (int i = 0; i < allSeasons.length; i++) {
                File[] allSites = allSeasons[i].listFiles(new FileFilter() {

                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory() && f.getName().contains(site) && !f.getName().contains(".svn");
                    }
                });
                if (allSites.length > 0) {
                    seasons.add(allSeasons[i].getName());
                }
            }
        }
        String[] a = {};
        return seasons.toArray(a);
    }

    /** 
	 * Returns a list of seasons which belong to a site.
	 * 
	 * @param site The site to which the seasons belong.
	 * @return an array of seasons names as a string array.
	 */
    public static void renameSite(final String from, String to) {
        File location = new File(Program.Settings().getDataFolder());
        File[] allSeasons = location.listFiles(new DirectoryFileFilter());
        if (location.exists()) {
            for (int i = 0; i < allSeasons.length; i++) {
                File[] allSites = allSeasons[i].listFiles(new FileFilter() {

                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory() && f.getName().contains(from);
                    }
                });
                if (allSites.length > 0) {
                    File newName = new File(String.format("%s/%s/%s", Program.Settings().getDataFolder(), allSeasons[i].getName(), to));
                    allSites[0].renameTo(newName);
                }
            }
        }
    }

    /**
	 * Remove a site from the local folder structure by name.
	 * @param name the name of the site.
	 */
    public static void removeSite(String name) {
        String[] seasons = getSeasons(name);
        LoadingBar lb = new LoadingBar(seasons.length);
        lb.setTitle("Calculating...");
        lb.setVisible(true);
        for (int i = 0; i < seasons.length; i++) {
            String path = String.format("%s/%s/%s", Program.Settings().getDataFolder(), seasons[i], name);
            recursiveFileDelete(path);
            String pathCheck = String.format("%s/%s/", Program.Settings().getDataFolder(), seasons[i]);
            File season = new File(pathCheck);
            if (season.listFiles().length <= 0) {
                season.delete();
            }
            lb.increment("Removed " + path);
        }
        lb.increment();
        lb.setVisible(false);
    }

    public static void removeSeason(String site, String season) {
        Site s = new Site(site, season);
        try {
            Delete(s);
        } catch (Exception e) {
        }
    }

    public static void removeSiteSeason(Site site) {
        String path = String.format("%s/%s/%s/", Program.Settings().getDataFolder(), site.getSeason(), site.getSite());
        Debug.print(FileSystem.class, path);
        recursiveFileDelete(path);
    }

    private static void recursiveFileDelete(String location) {
        File folder = new File(location);
        File[] listOfFiles = folder.listFiles();
        Debug.print(FileSystem.class, folder.getAbsolutePath());
        Debug.print(FileSystem.class, listOfFiles.length + "");
        for (int i = 0; i < listOfFiles.length; i++) {
            File current = listOfFiles[i];
            if (listOfFiles[i].isDirectory()) {
                recursiveFileDelete(current.getAbsolutePath());
            }
            Debug.print(FileSystem.class, "Removing: " + current.getAbsolutePath());
            while (current.exists()) {
                current.delete();
            }
        }
        folder.delete();
        Debug.print(FileSystem.class, "Folder cleared");
    }

    public static File getFirstImage(String location) {
        return recursiveFileSearch(location, "");
    }

    private static File recursiveFileSearch(String location, String name) {
        File found = null;
        File folder = new File(location);
        File[] listOfFiles = folder.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                boolean isJPG = file.getName().toLowerCase().endsWith(".jpg");
                boolean isDir = file.isDirectory();
                return isJPG || isDir;
            }
        });
        for (int i = 0; i < listOfFiles.length && found == null; i++) {
            File current = listOfFiles[i];
            if (!current.isDirectory() && current.getName().contains(name)) {
                found = current;
                break;
            } else {
                found = recursiveFileSearch(current.getAbsolutePath(), name);
            }
        }
        return found;
    }

    public static DateRange findDateRange(String location) {
        DateRange dr = new DateRange();
        return recursiveFileSearch(location, dr);
    }

    private static DateRange recursiveFileSearch(String location, DateRange range) {
        File folder = new File(location);
        File[] listOfFiles = folder.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                boolean isJPG = file.getName().toLowerCase().endsWith(".jpg");
                boolean isDir = file.isDirectory();
                return isJPG || isDir;
            }
        });
        for (int i = 0; i < listOfFiles.length; i++) {
            File current = listOfFiles[i];
            if (!current.isDirectory()) {
                Date d = null;
                try {
                    d = DateFromExif(current.getAbsoluteFile());
                } catch (IOException e) {
                    Debug.print(e);
                }
                if (range.getStart() == null) {
                    range.setStart(range.setEnd(d));
                } else if (d.before(range.getStart())) {
                    range.setStart(d);
                } else if (d.after(range.getEnd())) {
                    range.setEnd(d);
                }
            } else {
                range = recursiveFileSearch(current.getAbsolutePath(), range);
            }
        }
        return range;
    }

    public static class DateRange {

        private Date start, end;

        public DateRange() {
            setStart(setEnd(null));
        }

        public DateRange(Date s, Date e) {
            setStart(s);
            setEnd(e);
        }

        public void setStart(Date start) {
            this.start = start;
        }

        public Date getStart() {
            return start;
        }

        public Date setEnd(Date end) {
            this.end = end;
            return end;
        }

        public Date getEnd() {
            return end;
        }
    }

    /**
	 * Returns a list of the current sites within a specific season.
	 * @param season The season to look for sites.
	 * @return
	 */
    public static String[] getSites(String season) {
        File location = new File(Program.Settings().getDataFolder() + season);
        if (!location.mkdir()) {
            FilenameFilter filter = new DirectoryFileFilter();
            return location.list(filter);
        }
        return null;
    }

    /**
	 * A FilenameFilter to check if a file is a directory or not.
	 * @author Tim Dunstan
	 *
	 */
    private static class DirectoryFileFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            boolean isDir = new File(dir.getAbsoluteFile() + "//" + name).isDirectory() && !name.toLowerCase().equals(".svn");
            return isDir;
        }
    }

    private static class XMLFileFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            boolean isDir = new File(dir.getAbsoluteFile() + "/" + name).isDirectory();
            boolean isXML = name.toLowerCase().endsWith(".xml");
            return !isDir && isXML;
        }
    }

    ;

    /**
	 * Zip a Site objects contents.
	 * 
	 * @param saveLocation The location the zipfile is located.
	 * @param s The Site which is to be zipped.
	 * @param images Whether images should be included in the zipping process.
	 */
    public static void exportArchive(String saveLocation, Site s, final boolean images) {
        String season = s.getSeason();
        String site = s.getSite();
        String location = String.format("%s/%s/%s", Program.Settings().getDataFolder(), season, site);
        java.io.File f = new java.io.File(String.format("%s/PenguinData.xml", location));
        SiteDocument xmlSite = null;
        try {
            xmlSite = org.penguinuri.penguin.SiteDocument.Factory.parse(f);
            SiteDocument.Site sd = xmlSite.getSite();
            sd.setSeason(season);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        File baseFolder = new File(location);
        File templateFolder = new File(location + "/templates");
        FilenameFilter filter = new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                boolean isDir = new File(dir.getAbsoluteFile() + "/" + name).isDirectory();
                boolean isJPG = name.toLowerCase().endsWith(".jpg");
                boolean isXML = name.toLowerCase().endsWith(".xml");
                return !isDir && (isXML || images && isJPG);
            }
        };
        String[] baseFiles = baseFolder.list(filter);
        String[] templateFiles = templateFolder.list(filter);
        int total = 0;
        try {
            total = baseFiles.length;
            total += templateFiles.length;
        } catch (Exception ex) {
        }
        total += 2;
        byte[] buf = new byte[BUFFER_LEN];
        LoadingBar lb = new LoadingBar(total);
        lb.setVisible(true);
        try {
            File zipFile = new File(saveLocation);
            if (zipFile.exists()) {
                while (zipFile.delete()) {
                }
            }
            String outFilename = saveLocation;
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename));
            lb.increment("Compressing season database objects.");
            try {
                SiteSeasonDBDocument localDB = Program.getLocalSiteDatabase();
                SiteSeasonDBDocument.SiteSeasonDB.Sites.Site siteToCopy = null;
                Sites localSites = localDB.getSiteSeasonDB().getSites();
                for (int i = 0; i < localSites.sizeOfSiteArray(); i++) {
                    if (localSites.getSiteArray(i).getName() == null) continue;
                    SiteSeasonDBDocument.SiteSeasonDB.Sites.Site tempSite = localSites.getSiteArray(i);
                    if (tempSite.getName().toLowerCase().equals(site.toLowerCase())) {
                        siteToCopy = tempSite;
                        break;
                    }
                }
                if (siteToCopy != null) {
                    File exportDBFile = new File("./temp/exportdb.xml");
                    SiteSeasonDBDocument exportDB = SiteSeasonDBDocument.Factory.newInstance();
                    SiteSeasonDBDocument.SiteSeasonDB.Sites.Site exportSite = exportDB.addNewSiteSeasonDB().addNewSites().addNewSite();
                    exportSite.setLat(siteToCopy.getLat());
                    exportSite.setLong(siteToCopy.getLong());
                    exportSite.setName(siteToCopy.getName());
                    exportSite.setReferencePhoto(siteToCopy.getReferencePhoto());
                    exportSite.setStringValue(siteToCopy.getStringValue());
                    exportDB.save(exportDBFile);
                    FileInputStream in = new FileInputStream(exportDBFile);
                    out.putNextEntry(new ZipEntry("locations/exportdb.xml"));
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.closeEntry();
                    File exportImageFile = new File(String.format("%s/%s", Program.Settings().getLocationFolder(), siteToCopy.getReferencePhoto()));
                    FileInputStream inImg = new FileInputStream(exportImageFile);
                    out.putNextEntry(new ZipEntry(String.format("locations/%s", siteToCopy.getReferencePhoto())));
                    int lenImg;
                    while ((lenImg = inImg.read(buf)) > 0) {
                        out.write(buf, 0, lenImg);
                    }
                    inImg.close();
                    out.closeEntry();
                }
            } catch (Exception ex) {
                Debug.print(ex);
            }
            for (int i = 0; i < baseFiles.length; i++) {
                String file = String.format("%s/%s", location, baseFiles[i]);
                FileInputStream in = new FileInputStream(file);
                out.putNextEntry(new ZipEntry(baseFiles[i]));
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                lb.increment("Compressing: " + baseFiles[i]);
                out.closeEntry();
                in.close();
            }
            for (int i = 0; templateFiles != null && i < templateFiles.length; i++) {
                String file = String.format("%s/templates/%s", location, templateFiles[i]);
                FileInputStream in = new FileInputStream(file);
                out.putNextEntry(new ZipEntry(String.format("templates/%s", templateFiles[i])));
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                lb.increment("Compressing XML files.");
                out.closeEntry();
            }
            lb.increment("Finalising");
            out.close();
        } catch (IOException e) {
            Debug.print(e);
        }
    }

    /**
	 * UnZip a site/season.
	 * 
	 * @param location The location of the zipfile to unpack.
	 * @throws IOException If the zipfile doesn't exist, the PenguinData.xml files doesn't exist or various other possible IO problems.
	 * @throws XmlException If there is a problem with the XML from PenguinData.xml
	 * @throws Exception General Exceptions.
	 */
    public static void importArchive(String location) throws IOException, XmlException, Exception {
        String strTempFolder = "./temp/";
        String strTempDataLocation = strTempFolder + "TempData.xml";
        String strExportDBLocation = strTempFolder + "exportdb.xml";
        new File(strTempFolder).mkdir();
        ZipFile zf = new ZipFile(location);
        Debug.print("Archive: " + location);
        File pData = getFileFromZip(zf, "PenguinData.xml", strTempDataLocation);
        SiteDocument s = org.penguinuri.penguin.SiteDocument.Factory.parse(pData);
        SiteDocument.Site sd = s.getSite();
        File lData;
        SiteSeasonDBDocument lDataXML;
        try {
            lData = getFileFromZip(zf, "locations/exportdb.xml", strExportDBLocation);
            if (!lData.exists()) throw new Exception();
            lDataXML = SiteSeasonDBDocument.Factory.parse(lData);
        } catch (Exception ex) {
            throw new FileSystem.LocationMissingException(ex);
        }
        String season = sd.getSeason().split("/")[0];
        String site = lDataXML.getSiteSeasonDB().getSites().getSiteArray(0).getName();
        String path = String.format("%s/%s/%s/", Program.Settings().getDataFolder(), season, site);
        if (new File(path).exists()) {
            String message = String.format("The season %s, already exists.", sd.getSeason());
            throw new Exception(message);
        }
        LoadingBar lb = new LoadingBar(zf.size() + 2);
        lb.setVisible(true);
        for (Enumeration entries = zf.entries(); entries.hasMoreElements(); ) {
            ZipEntry zipEntry = (ZipEntry) entries.nextElement();
            if (zipEntry.isDirectory()) {
                (new File(zipEntry.getName())).mkdirs();
            } else {
                String zipEntryName = zipEntry.getName();
                Debug.print(" inflating: " + zipEntryName);
                String tempFile = path + zipEntryName;
                new File(tempFile.substring(0, tempFile.lastIndexOf('/'))).mkdirs();
                OutputStream out = new FileOutputStream(tempFile);
                InputStream in = zf.getInputStream(zipEntry);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            }
            lb.increment("Decompressing: " + zipEntry.getName());
        }
        lb.increment("Fixing local season database.");
        File tempLocationFolder = new File(path + "/locations/");
        if (tempLocationFolder.exists()) {
            File exportDBFile = new File(path + "/locations/exportdb.xml");
            SiteSeasonDBDocument exportDB = SiteSeasonDBDocument.Factory.parse(exportDBFile);
            SiteSeasonDBDocument.SiteSeasonDB.Sites.Site siteToCopy = exportDB.getSiteSeasonDB().getSites().getSiteArray(0);
            File localDBFile = new File(String.format("%s/localDB.xml", Program.Settings().getLocationFolder()));
            if (!localDBFile.exists()) {
                new File(Program.Settings().getLocationFolder()).mkdirs();
                SiteSeasonDBDocument localDB = SiteSeasonDBDocument.Factory.newInstance();
                localDB.addNewSiteSeasonDB().addNewSites();
                localDB.save(localDBFile);
            }
            SiteSeasonDBDocument localDB = Program.getLocalSiteDatabase();
            Sites localSites = localDB.getSiteSeasonDB().getSites();
            boolean found = false;
            for (int i = 0; i < localSites.sizeOfSiteArray(); i++) {
                SiteSeasonDBDocument.SiteSeasonDB.Sites.Site tempSite = localSites.getSiteArray(i);
                if (tempSite.getName().equals(siteToCopy.getName())) {
                    found = true;
                    break;
                }
            }
            File picFrom = new File(path + "/locations/" + siteToCopy.getReferencePhoto()), picTo = new File(String.format("%s/%s", Program.Settings().getLocationFolder(), siteToCopy.getReferencePhoto()));
            if (!found) {
                SiteSeasonDBDocument.SiteSeasonDB.Sites.Site localSite = localSites.addNewSite();
                localSite.setLat(siteToCopy.getLat());
                localSite.setLong(siteToCopy.getLong());
                localSite.setName(siteToCopy.getName());
                localSite.setReferencePhoto(siteToCopy.getReferencePhoto());
                localSite.setStringValue(siteToCopy.getStringValue());
                copyFile(picFrom, picTo);
            }
            localDB.save(localDBFile);
            picFrom.delete();
            exportDBFile.delete();
            tempLocationFolder.delete();
        }
        pData.delete();
        lData.delete();
        zf.close();
        lb.increment();
        JOptionPane.showMessageDialog(null, String.format("Import of %s complete!", sd.getSeason()));
    }

    private static File getFileFromZip(ZipFile zf, String from, String to) throws IOException {
        ZipEntry xmlFile = zf.getEntry(from);
        OutputStream xout = new FileOutputStream(to);
        InputStream xin = zf.getInputStream(xmlFile);
        byte[] xbuf = new byte[1024];
        int xlen;
        while ((xlen = xin.read(xbuf)) > 0) {
            xout.write(xbuf, 0, xlen);
        }
        xout.close();
        xin.close();
        return new File(to);
    }

    public static void Delete(Site s) throws IOException {
        String season = s.getSeason();
        String site = s.getSite();
        String location = String.format("%s/%s/%s", Program.Settings().getDataFolder(), season, site);
        File baseFolder = new File(location);
        File templateFolder = new File(location + "/templates");
        File[] baseFiles = baseFolder.listFiles();
        File[] templateFiles = templateFolder.listFiles();
        int total = 0;
        try {
            total = baseFiles.length;
            total += templateFiles.length;
        } catch (Exception ex) {
        }
        LoadingBar lb = new LoadingBar(total);
        lb.setVisible(true);
        for (int i = 0; i < baseFiles.length; i++) {
            File f = baseFiles[i];
            String str = "Removing: " + f.getName();
            lb.increment(str);
            f.delete();
        }
        for (int i = 0; templateFiles != null && i < templateFiles.length; i++) {
            File f = templateFiles[i];
            String str = "Removing: " + f.getName();
            Debug.print(str);
            lb.increment(str);
            f.delete();
        }
        templateFolder.delete();
        baseFolder.delete();
    }

    /**
	 * Copy a file from one location to another.
	 * 
	 * @param in The file to copy.
	 * @param out The location the file is to be copied to.
	 * @throws IOException
	 */
    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }

    /**
 * Import images from another Directory and displays a loading bar.
 * @param location The location where the images are stored.
 * @param destination The destination folder where the images are to be saved.
 * @param site The site object which the images are to be added to. 
 * @throws IOException
 */
    public static void importFromMedia(String location, Site site) throws IOException, Exception {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.YEAR, 1900);
        Calendar end = Calendar.getInstance();
        end.set(Calendar.YEAR, 3000);
        importFromMedia(location, site, new DateRange(start.getTime(), end.getTime()));
    }

    /**
	 * Import images for a specific location into correct folder structure depending
	 * on site name and season. Will only import images between the specific date range.
	 * @param location
	 * @param site
	 * @param dateRange
	 * @throws IOException
	 */
    public static void importFromMedia(String location, Site site, DateRange dateRange) throws IOException, Exception {
        if (site != null) {
            String d = String.format("%s/%s/%s", Program.Settings().getDataFolder(), site.getSeason(), site.getSite());
            FileSize fs = FileSystem.getFileTotal(location);
            LoadingBar lb = new LoadingBar(fs.getNumberOfFiles());
            lb.setVisible(true);
            new File(d).mkdirs();
            if (fs.getFileSize() > new File(d).getFreeSpace()) {
                lb.setVisible(false);
                lb.dispose();
                throw new Exception("Not enough freespace.");
            }
            String[] c = Program.Settings().getDefaultCountNames();
            if (site.getCountNames().size() < c.length) {
                for (int i = 0; i < c.length; i++) {
                    site.getCountNames().add(c[i]);
                }
            }
            String s = "-= Import log =- \n" + "Imported images between " + Conversions.getDateFormat().format(dateRange.getStart()) + " and " + Conversions.getDateFormat().format(dateRange.getEnd()) + "\n\n";
            String imported = importFromMedia(location, d, site, dateRange.getStart(), dateRange.getEnd(), lb);
            if (site.isEmpty()) {
                s += "No sites have been Added.. Please check your dates.\n\n";
            }
            s += imported;
            Debug.print(s);
            LogWindow.showDialog(s);
        }
    }

    /**
	 * The main recursive method of the importFromMedia method.
	 * @param location
	 * @param destination
	 * @param site
	 * @param lb
	 * @throws IOException
	 */
    private static String importFromMedia(String location, String destination, Site site, Date start, Date end, LoadingBar lb) throws IOException {
        String log = "";
        File folder = new File(location);
        File[] listOfFiles = folder.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                boolean isJPG = file.getName().toLowerCase().endsWith(".jpg");
                boolean isDir = file.isDirectory();
                return isJPG || isDir;
            }
        });
        String[] fileNames = new String[listOfFiles.length];
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String loc = String.format("%s/%s", folder.getCanonicalPath(), listOfFiles[i].getName());
                String oldFileName = listOfFiles[i].getName();
                String ext = oldFileName.substring(oldFileName.lastIndexOf("."));
                fileNames[i] = listOfFiles[i].getName();
                java.util.Date d = DateFromExif(listOfFiles[i]);
                Debug.print(d);
                if (d.after(start) && d.before(end)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                    String fileName = String.format("%s_%s%s", site.getSite(), sdf.format(d), ext);
                    String dest = String.format("%s/%s", destination, fileName);
                    File dfile = new File(dest);
                    if (!dfile.exists()) {
                        String txt = "Copying " + fileNames[i] + " " + fileName;
                        lb.increment(txt);
                        log += "Added and renamed \"" + fileNames[i] + "\" as " + fileName + "\n";
                        NestImage ni = site.addImage(d, fileName);
                        ni.setTemplate(site.getMainTemplate());
                        if (ni.getTemplate() != null) log += " -- Applied template (" + ni.getTemplate().toString() + ")\n";
                        for (int j = 0; j < site.getCountNames().size(); j++) {
                            ni.setCountAsCounted(j, false);
                        }
                        copyFile(new File(loc), dfile);
                    } else {
                        String txt = "Skipped " + fileNames[i] + " file exists as " + fileName;
                        lb.increment(txt);
                        log += txt + "\n";
                    }
                } else {
                    String txt = "Skipped " + fileNames[i] + " - out of date range. (" + Conversions.getFullDate().format(d) + ")";
                    lb.increment(txt);
                    log += txt + "\n";
                }
            } else {
                String txt = "Skipped " + fileNames[i] + " - not a file..";
                lb.increment(txt);
                log += txt + "\n";
            }
            if (listOfFiles[i].isDirectory()) {
                String locationTemp = folder.toString() + "/" + listOfFiles[i].getName();
                Debug.print("" + locationTemp + " " + destination);
                log += importFromMedia(locationTemp, destination, site, start, end, lb);
            }
        }
        return log;
    }

    /**
	 * Count the number of files within a directory tree structure.
	 * @param location Where the images are which need counting.
	 * @return
	 * @throws IOException
	 */
    public static FileSize getFileTotal(String location) throws IOException {
        File folder = new File(location);
        File[] listOfDirs = folder.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                boolean isDir = file.isDirectory();
                return isDir;
            }
        });
        File[] listOfFiles = folder.listFiles(new FileFilter() {

            @Override
            public boolean accept(File file) {
                return file.getName().toLowerCase().endsWith(".jpg");
            }
        });
        long size = 0;
        for (int i = 0; i < listOfFiles.length; i++) {
            size += listOfFiles[i].length();
        }
        int total = listOfFiles.length;
        for (int i = 0; i < listOfDirs.length; i++) {
            if (listOfDirs[i].isDirectory()) {
                String locationTemp = folder.toString() + "/" + listOfDirs[i].getName();
                FileSize fs = getFileTotal(locationTemp);
                total += fs.getNumberOfFiles();
                size += fs.getFileSize();
            }
        }
        FileSize f = new FileSize(total, size);
        return f;
    }

    public static class FileSize {

        int f;

        long s;

        public FileSize(int files, long size) {
            f = files;
            s = size;
        }

        public int getNumberOfFiles() {
            return f;
        }

        public long getFileSize() {
            return s;
        }
    }

    /** Extract a Date object from a JPEG files ExIF meta data pertaining to when the image was taken. 
	 * 
	 * @param jpegFile The file to be examined.
	 * @return The Date which the JPEG was taken.
	 * @throws IOException
	 */
    public static java.util.Date DateFromExif(File jpegFile) throws IOException {
        java.util.Date date = null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        try {
            Metadata metadata = JpegMetadataReader.readMetadata(jpegFile);
            Iterator directories = metadata.getDirectoryIterator();
            while (directories.hasNext()) {
                Directory directory = (Directory) directories.next();
                Iterator tags = directory.getTagIterator();
                while (tags.hasNext()) {
                    Tag tag = (Tag) tags.next();
                    if (tag.getTagName().toLowerCase().contains("date/time")) {
                        java.util.Date taggedDate = sdf.parse(tag.getDescription());
                        if (date == null || taggedDate.before(date)) date = taggedDate;
                    }
                }
            }
        } catch (Exception ex) {
            Debug.print(ex);
        }
        return date;
    }

    public static void listUnUsedTemplates() {
        DirectoryFileFilter filter = new DirectoryFileFilter();
        int count = 0;
        for (File seasonFolder : new File(Program.Settings().getDataFolder()).listFiles(filter)) {
            count += seasonFolder.listFiles(filter).length;
        }
        LoadingBar Loadbar = new LoadingBar(count);
        Loadbar.setVisible(true);
        Loadbar.setTitle("Checking templates");
        String strUsedTemps = "";
        String strUnusedTemps = "";
        String strDataFolder = Program.Settings().getDataFolder();
        for (final File seasonFolder : new File(strDataFolder).listFiles(filter)) {
            String strSeasonFolder = seasonFolder.getName();
            Debug.print("CurrentSeason: " + strSeasonFolder);
            for (final String siteFolder : seasonFolder.list(filter)) {
                Debug.print("  CurrentSite: " + siteFolder);
                Loadbar.setTitle(String.format("Checking templates for: %s/%s", strSeasonFolder, siteFolder));
                Site s = new Site(siteFolder, strSeasonFolder);
                try {
                    s.load(false);
                } catch (Exception e) {
                    Debug.print(e);
                    continue;
                }
                for (Template t : s.getTemplates().values()) {
                    Debug.print("    CurrentTemplate: " + t.toString());
                    boolean templateFound = false;
                    if (t != null) {
                        for (penguin.dataset.Day d : s.getDays()) {
                            Debug.print("      CurrentDay: " + d.getDate());
                            if (d != null) {
                                for (penguin.dataset.NestImage n : d.getNestImages()) {
                                    Debug.print("        CurrentTime: " + n.getTimeDateTaken());
                                    if (n != null && n.getTemplate() != null && n.getTemplate().equals(t)) {
                                        Debug.print("        CurrentTimeTemplate: " + n.getTemplate());
                                        templateFound = true;
                                    }
                                }
                            }
                        }
                    }
                    String strTemp = String.format("%s/%s/templates/%s", seasonFolder.getName(), siteFolder, t.toString());
                    if (templateFound) strUsedTemps += "\n" + strTemp; else strUnusedTemps += "\n" + strTemp;
                }
                Loadbar.increment();
            }
        }
        if (strUnusedTemps.equals("")) strUnusedTemps = "No Unused templates found.";
        strUsedTemps = "=Used Templates=\n" + strUsedTemps;
        strUnusedTemps = "=Unused Templates=\n" + strUnusedTemps;
        boolean showall = false;
        if (showall) LogWindow.showDialog(String.format("==Template Summary==\n\n%s\n\n%s", strUsedTemps, strUnusedTemps)); else LogWindow.showDialog(String.format("==Template Summary==\n\n%s", strUnusedTemps));
    }
}
