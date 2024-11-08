package de.schwarzrot.themeedit.app;

import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import de.schwarzrot.app.domain.VideoPageFormat;
import de.schwarzrot.app.support.ApplicationServiceProvider;
import de.schwarzrot.data.access.jdbc.JDBCTestBase;
import de.schwarzrot.dvd.theme.MenueDefinition;
import de.schwarzrot.dvd.theme.Theme;
import de.schwarzrot.dvd.theme.domain.ElementSkin;
import de.schwarzrot.dvd.theme.domain.PageSkin;
import de.schwarzrot.dvd.theme.domain.ThemeElement;
import de.schwarzrot.dvd.theme.domain.data.MenueElementCategory;
import de.schwarzrot.dvd.theme.domain.data.MenuePageType;
import de.schwarzrot.dvd.theme.support.ExchangeHandler;
import de.schwarzrot.jar.JarExtensionHandler;
import de.schwarzrot.system.support.FileUtils;
import junit.framework.TestSuite;

public class ExchangeTest extends JDBCTestBase {

    private static final String WORK_DIR = "working.directory";

    private static final String EXT_DIR = "extension.directory";

    private static final String THEME_NAME = "theme.2.exchange";

    private static final String SKIN_NAME = "skin.2.exchange";

    private static final String FONT_DIR = "font.base.directory";

    private static final String ASPECT = "menue.page.format";

    private static final String PASSWORD = "sudo.password";

    private static final int BUF_SIZE = 32 * 1024;

    private static MenueDefinition menueDef;

    private static Map<String, List<File>> fontCache;

    private static File templateArchive;

    private byte buf[] = new byte[BUF_SIZE];

    private File workingDir;

    public ExchangeTest(String testCase) {
        super(testCase);
        appContextFiles[2] = "de/schwarzrot/app/startup/ctx/application-context.xml";
    }

    public void checkTTFonts() {
        if (fontCache == null) {
            fontCache = new HashMap<String, List<File>>();
            File fontBaseDir = new File(System.getProperty(FONT_DIR));
            loadFontCache(fontCache, fontBaseDir);
        }
        for (String family : fontCache.keySet()) {
            List<File> files = fontCache.get(family);
            System.out.println("font family [ " + family + " ] consists of ");
            for (File cur : files) {
                System.out.println("          ==> [" + cur + "]");
            }
        }
    }

    public void testExport() throws Exception {
        Theme theme2Export = menueDef.getTheme();
        Map<MenuePageType, PageSkin> skin2Export = menueDef.getSkin();
        File exportDir = new File(workingDir, theme2Export.getName());
        File themeExportFile = new File(exportDir, "sampleTheme.xml");
        File skinExportFile = new File(exportDir, "sampleSkin.xml");
        File targetFile;
        exportDir.mkdirs();
        repository.performExport(theme2Export, themeExportFile);
        repository.performExport(skin2Export, "SampleSkin", skinExportFile);
        for (MenuePageType mpt : MenuePageType.values()) {
            List<ThemeElement<?>> elems = theme2Export.getThemeElements(mpt);
            for (ThemeElement<?> te : elems) {
                if (te.getImageName() != null) {
                    System.err.println("check image [ " + te.getImageName().getAbsolutePath() + " ]");
                    targetFile = new File(exportDir, te.getImageName().getName());
                    if (!targetFile.exists()) FileUtils.copyFile(targetFile, te.getImageName());
                }
            }
        }
        File tmp;
        for (MenuePageType mpt : skin2Export.keySet()) {
            PageSkin skin = skin2Export.get(mpt);
            for (MenueElementCategory cat : skin.getElements().keySet()) {
                ElementSkin eSkin = skin.getElementSkin(cat);
                if (eSkin.getFont() != null) {
                    System.out.println("check font [" + eSkin.getFont() + "]");
                    if (fontCache.containsKey(eSkin.getFont())) {
                        for (File fontFile : fontCache.get(eSkin.getFont())) {
                            targetFile = new File(exportDir, fontFile.getName());
                            if (!targetFile.exists()) FileUtils.copyFile(targetFile, fontFile);
                        }
                    }
                }
                if (eSkin.getImage() != null) {
                    tmp = eSkin.getImage();
                    if (tmp.isFile() && tmp.canRead()) {
                        System.out.println("check image " + tmp.getAbsolutePath());
                        targetFile = new File(exportDir, tmp.getName());
                        if (tmp.isFile() && tmp.canRead() && !targetFile.exists()) {
                            FileUtils.copyFile(targetFile, tmp);
                        }
                    }
                }
            }
        }
        createArchive(exportDir);
        assertTrue("failed to create archive", new File(workingDir, exportDir.getName() + ".zip").exists());
        FileUtils.removeDirectory(exportDir);
    }

    public void testHandlerExport() {
        ExchangeHandler eh = new ExchangeHandler(workingDir);
        templateArchive = eh.exportTemplateArchive();
    }

    public void testHandlerImport() {
        ExchangeHandler eh = new ExchangeHandler(workingDir);
        if (templateArchive != null) {
            System.out.println("found archive " + templateArchive + " to extract");
            eh.importTemplateArchive(templateArchive, System.getProperty(PASSWORD));
        }
    }

    @SuppressWarnings("unchecked")
    public void testImport() throws Exception {
        File fontBaseDir = new File(System.getProperty(FONT_DIR));
        String sudoPassword = System.getProperty(PASSWORD);
        Theme theme2Import;
        Map<MenuePageType, PageSkin> skin2Import;
        File tmp;
        File targetFile;
        for (File archive : workingDir.listFiles()) {
            if (!archive.getName().toUpperCase().endsWith(".ZIP")) continue;
            System.out.println("found archive " + archive + " to extract");
            extractArchive(archive);
            File importDir = new File(workingDir, archive.getName().substring(0, archive.getName().length() - 4));
            assertTrue("import directory " + importDir.getAbsolutePath() + " does not exists", importDir.exists() && importDir.isDirectory());
            theme2Import = null;
            skin2Import = null;
            for (File archiveEntry : importDir.listFiles()) {
                if (archiveEntry.getName().toUpperCase().endsWith(".TTF")) {
                    System.err.println(archiveEntry.getName() + " should be copied to fonts directory");
                    targetFile = new File(fontBaseDir, archiveEntry.getName());
                    FileUtils.copyFile(targetFile, archiveEntry, sudoPassword);
                    continue;
                }
                if (archiveEntry.getName().endsWith(".xml")) {
                    Object any = repository.performImport(archiveEntry);
                    if (any instanceof Theme) theme2Import = (Theme) any; else if (any instanceof Map<?, ?>) skin2Import = (Map<MenuePageType, PageSkin>) any;
                }
            }
            if (theme2Import != null) {
                for (MenuePageType mpt : MenuePageType.values()) {
                    List<ThemeElement<?>> elems = theme2Import.getThemeElements(mpt);
                    for (ThemeElement<?> te : elems) {
                        if (te.getImageName() != null) {
                            targetFile = te.getImageName();
                            if (targetFile == null) continue;
                            if (targetFile.getName().length() < 2) continue;
                            tmp = new File(importDir, targetFile.getName());
                            if (tmp.exists()) {
                                System.out.println("should copy " + targetFile.getName() + " to " + targetFile);
                                FileUtils.copyFile(targetFile, tmp);
                            }
                        }
                    }
                }
            }
            if (skin2Import != null) {
                for (MenuePageType mpt : skin2Import.keySet()) {
                    PageSkin skin = skin2Import.get(mpt);
                    for (MenueElementCategory cat : skin.getElements().keySet()) {
                        ElementSkin eSkin = skin.getElementSkin(cat);
                        targetFile = eSkin.getImage();
                        if (targetFile == null) continue;
                        if (targetFile.getName().length() < 2) continue;
                        tmp = new File(importDir, targetFile.getName());
                        if (tmp.exists()) {
                            System.out.println("should copy " + targetFile.getName() + " to " + targetFile);
                            FileUtils.copyFile(targetFile, tmp);
                        }
                    }
                }
            }
        }
    }

    protected void createArchive(File dir2Archive) {
        if (dir2Archive.getParentFile().canWrite()) {
            ZipOutputStream out = null;
            String prefix = dir2Archive.getName() + "/";
            ZipEntry entry;
            int cRead;
            try {
                FileOutputStream stream = new FileOutputStream(new File(dir2Archive.getParentFile(), dir2Archive.getName() + ".zip"));
                out = new ZipOutputStream(stream);
                entry = new ZipEntry(prefix);
                entry.setTime(dir2Archive.lastModified());
                out.putNextEntry(entry);
                for (File cur : dir2Archive.listFiles()) {
                    if (cur.isFile() && cur.canRead()) {
                        entry = new ZipEntry(prefix + cur.getName());
                        entry.setTime(cur.lastModified());
                        entry.setSize(cur.length());
                        out.putNextEntry(entry);
                        FileInputStream in = new FileInputStream(cur);
                        while ((cRead = in.read(buf, 0, buf.length)) > 0) {
                            out.write(buf, 0, cRead);
                        }
                        in.close();
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                try {
                    if (out != null) out.close();
                } catch (Throwable t) {
                }
            }
        }
    }

    protected void extractArchive(File archive) {
        ZipInputStream zis = null;
        FileOutputStream fos;
        ZipEntry entry;
        File curEntry;
        int n;
        try {
            zis = new ZipInputStream(new FileInputStream(archive));
            while ((entry = zis.getNextEntry()) != null) {
                curEntry = new File(workingDir, entry.getName());
                if (entry.isDirectory()) {
                    System.out.println("skip directory: " + entry.getName());
                    continue;
                }
                System.out.print("zip-entry (file): " + entry.getName());
                System.out.println(" ==> real path: " + curEntry.getAbsolutePath());
                if (!curEntry.getParentFile().exists()) curEntry.getParentFile().mkdirs();
                fos = new FileOutputStream(curEntry);
                while ((n = zis.read(buf, 0, buf.length)) > -1) fos.write(buf, 0, n);
                fos.close();
                zis.closeEntry();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            try {
                if (zis != null) zis.close();
            } catch (Throwable t) {
            }
        }
    }

    protected void loadFontCache(Map<String, List<File>> cache, File baseDir) {
        for (File cur : baseDir.listFiles()) {
            if (cur.isFile() && cur.getName().toUpperCase().endsWith("TTF")) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(cur);
                    Font font = Font.createFont(Font.TRUETYPE_FONT, fis);
                    System.err.println("found font: " + font.getFontName());
                    if (!cache.containsKey(font.getFamily())) cache.put(font.getFamily(), new ArrayList<File>());
                    cache.get(font.getFamily()).add(cur);
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    try {
                        if (fis != null) fis.close();
                    } catch (Throwable t) {
                    }
                }
            } else if (cur.isDirectory()) {
                loadFontCache(cache, cur);
            }
        }
    }

    @Override
    protected void setUp() {
        super.setUp();
        JarExtensionHandler jeh = new JarExtensionHandler();
        jeh.loadExtensions(System.getProperty(EXT_DIR));
        if (menueDef == null) {
            menueDef = (MenueDefinition) ApplicationServiceProvider.getService(MenueDefinition.class);
        }
        workingDir = new File(System.getProperty(WORK_DIR));
        workingDir.mkdirs();
        assertNotNull("working dir", workingDir);
        assertTrue("working directory must exist!", workingDir.exists() && workingDir.isDirectory());
        assertTrue("working directory must be writable!", workingDir.canWrite());
        menueDef.setAspect(VideoPageFormat.valueOf(System.getProperty(ASPECT)));
        menueDef.selectTheme(System.getProperty(THEME_NAME));
        menueDef.selectSkin(System.getProperty(SKIN_NAME));
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new ExchangeTest("testHandlerExport"));
        suite.addTest(new ExchangeTest("testHandlerImport"));
        return suite;
    }
}
