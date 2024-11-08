package ch.oblivion.comixviewer;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ch.oblivion.comixviewer.data.DatabaseConstants;
import ch.oblivion.comixviewer.data.ProfileConstants;
import ch.oblivion.comixviewer.engine.IDataAccessor;
import ch.oblivion.comixviewer.engine.download.DefaultPageBuilder;
import ch.oblivion.comixviewer.engine.download.DownloadManager;
import ch.oblivion.comixviewer.engine.events.IProgressMonitor;
import ch.oblivion.comixviewer.engine.model.ComixPage;
import ch.oblivion.comixviewer.engine.model.ComixProfile;

public class TestProfiles {

    private static final String connectionString = "jdbc:hsqldb:mem:ComixViewerEngineText;shutdown=true";

    private static Connection connection;

    IProgressMonitor monitor = new IProgressMonitor() {

        @Override
        public void worked(int worked, Object information) {
            System.out.println("Worked " + worked);
        }

        @Override
        public void start(int totalWork) {
            System.out.println("Start " + totalWork);
        }

        @Override
        public void done(Object information) {
            System.out.println("Done");
        }

        @Override
        public void cancel() {
            System.out.println("Cancelled");
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    };

    @BeforeClass
    public static void startDatabase() {
        try {
            connection = DriverManager.getConnection(connectionString);
            execute(DatabaseConstants.CREATE_TABLES_FILE);
            execute(DatabaseConstants.SAMPLE_DATA_FILE);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Could not start the test database " + connectionString, e);
        } catch (IOException e) {
            throw new RuntimeException("Could not read the sql data file " + DatabaseConstants.SAMPLE_DATA_FILE, e);
        }
    }

    private static void execute(String fileName) throws IOException, SQLException {
        InputStream input = DatabaseConstants.class.getResourceAsStream(fileName);
        StringWriter writer = new StringWriter();
        IOUtils.copy(input, writer);
        String sql = writer.toString();
        Statement statement = connection.createStatement();
        statement.execute(sql);
    }

    @AfterClass
    public static void stopDatabase() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Test the profile builder. 
	 * @throws SQLException 
	 * @throws IOException 
	 */
    @Test
    public void testReadProfile() throws SQLException, IOException {
        ComixProfile profile = readTestProfile();
        assertTrue(profile.getName().equals("Dilbert"));
        int pageCount = profile.getPageCount();
        assertTrue(pageCount == 0);
        ComixPage page = profile.getPage(pageCount, monitor);
        assertNotNull(page);
        InputStream imageFile = profile.getCachedImageFile(page, monitor);
        assertNotNull(imageFile);
        imageFile.close();
        System.out.println("Wrote '" + page.getTitle() + "' to " + page.getCacheName());
        pageCount = profile.getPageCount();
        assertTrue(pageCount == 1);
        page = profile.getPage(pageCount, monitor);
        assertNotNull(page);
        imageFile = profile.getCachedImageFile(page, monitor);
        assertNotNull(imageFile);
        imageFile.close();
        System.out.println("Wrote '" + page.getTitle() + "' to " + page.getCacheName());
        IProgressMonitor failMonitor = new IProgressMonitor() {

            @Override
            public void worked(int worked, Object information) {
                assertTrue("Should come from cache", false);
            }

            @Override
            public void start(int totalWork) {
                assertTrue("Should come from cache", false);
            }

            @Override
            public void done(Object information) {
                assertTrue("Should come from cache", false);
            }

            @Override
            public void cancel() {
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        };
        page = profile.getPage(pageCount, failMonitor);
        assertNotNull(page);
        imageFile = profile.getCachedImageFile(page, monitor);
        assertNotNull(imageFile);
        imageFile.close();
    }

    @Test
    public void testDownloadManager() throws MalformedURLException, SQLException, InterruptedException {
        DownloadManager manager = new DownloadManager(new IDataAccessor() {

            @Override
            public void saveProfile(ComixProfile profile) {
                System.out.println("Save profile " + profile.getName());
            }

            @Override
            public void savePage(ComixProfile profile, ComixPage page) {
                System.out.println("Save page " + page.getTitle());
            }

            @Override
            public List<ComixPage> getPages(ComixProfile profile) {
                return null;
            }

            @Override
            public List<ComixProfile> findAllProfiles() {
                return null;
            }
        });
        ComixProfile profile = readTestProfile();
        manager.start(profile, monitor);
        Thread.sleep(2000);
        System.out.println("pausing");
        manager.cancel(profile);
    }

    private ComixProfile readTestProfile() throws SQLException, MalformedURLException {
        ComixProfile profile = new ComixProfile(new DefaultPageBuilder());
        Statement statement = connection.createStatement();
        ResultSet set = statement.executeQuery("SELECT * FROM comix_profile");
        int flags = Pattern.DOTALL;
        while (set.next()) {
            profile.setIdentifier(set.getInt(ProfileConstants.PK));
            profile.setName(set.getString(ProfileConstants.NAME));
            profile.setFirstPageUrl(new URL(set.getString(ProfileConstants.FIRST_PAGE_URL)));
            profile.setTitlePattern(Pattern.compile(set.getString(ProfileConstants.TITLE_PATTERN), flags));
            profile.setDescriptionPattern(Pattern.compile(set.getString(ProfileConstants.DESCRIPTION_PATTERN), flags));
            profile.setCacheNamePattern(Pattern.compile(set.getString(ProfileConstants.CACHE_NAME_PATTERN), flags));
            profile.setSiteUrl(new URL(set.getString(ProfileConstants.SITE_URL)));
            profile.setNextPageUrlPattern(Pattern.compile(set.getString(ProfileConstants.NEXT_PAGE_URL_PATTERN), flags));
            profile.setPreviousPageUrlPattern(Pattern.compile(set.getString(ProfileConstants.PREVIOUS_PAGE_URL_PATTERN), flags));
            profile.setImageUrlPattern(Pattern.compile(set.getString(ProfileConstants.IMAGE_URL_PATTERN), flags));
        }
        return profile;
    }
}
