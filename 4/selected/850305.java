package cardgames.view.cards;

import cardgames.model.cards.FrenchCard;
import cardgames.model.cards.MauMauCard;
import cardgames.model.cards.MauMauDeck;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import javax.imageio.ImageIO;
import org.apache.log4j.Logger;

/**
 * Load cards' faces from local filesystem.
 * <p>
 * Loads {@link BufferedImage images} of {@link MauMauCard MauMauCards'} faces
 * from local filesystem using the package {@link javax.imageio}. The loader is
 * implemented to read faces' files from local filesystem instead of retrieving
 * them from the net. In addition to the both kind of faces included
 * (<code>gif</code> and <code>png</code>) the user can provide her / his own
 * faces by saving these faces' files in a subdirectory within the loader's
 * root-directory.
 * <a name="rootDirectory"></a>
 * <p>
 * If this root does not contain both faces <code>gif</code> and <code>png</code>
 * the loader creates both subdirectories and copies the face-files there. The
 * {@link #getFaceFileName(cardgames.model.cards.MauMauCard) names} of these files
 * denote the cards they depict. The both built-in-faces are included in classpath
 * of this jar and are copied to root on loaders creation.
 * <p>
 * Besides these two built-in-faces a user employ specific faces by storing them
 * in subdirectories of loaders root. The loader scans its root on creation for
 * subdirectories containing 32 different files that
 * <p>
 * <ol>
 *  <li> have filenames denoting a card according to the
 *       {@link #getFaceFileName(cardgames.model.cards.MauMauCard) naming-conventions},
 *  <li> have filename-extensions that match one of the
 *       {@link #FORMATS four supported encodings} and
 *  <li> are loadable by {@link ImageIO#read(java.io.File) ImageIO}.
 * </ol>
 * <p>
 * If the loader detects such a subdirectory the name of that directory can be used to
 * {@link #getFace(cardgames.model.cards.MauMauCard, java.lang.String) retrieve a face}.
 * The name of such a subdirectory is provided by the second parameter
 * <code>faceName</code> that is handled case-<strong>in</strong>sensitive
 * throughout the loader's operations.
 * <p>
 * If the loader is set to use a root-directory that does not exist and cannot
 * be written the loader enters a state where {@link #ready()} returns
 * <code>false</code>. Most of the operations will return <code>null</code>-values
 * then. Such a situation is <strong>not</strong> indicated by an exception that
 * occurs on creation – be sure that you loader is ready after creation!
 */
public class MauMauCardFaceLoader {

    static Logger LOGGER = Logger.getLogger(MauMauCardFaceLoader.class);

    static final MauMauDeck cards = new MauMauDeck();

    /**
     * List of image-formats supported.
     * <p>
     * {@link javax.imageio Javax} does read and write the standard image
     * formats <code>bmp</code>, <code>gif</code>, <code>jpg</code> and
     * <code>png</code>. The four entries in this list are used to generate
     * (guess) filenames while scanning subdirectories in loader's root.
     * <p>
     * When the loader enters a subdirectory it tries to load a face (image)
     * for every card. Every {@link #getFaceFileName(cardgames.model.cards.MauMauCard) facefile's name}
     * is extended with these entries. Every complete filename is used to test
     * if the current subdirectory contains such an image and that image is
     * loadable. If loading succeeds the next card's face is tested.
     * <p>
     * As all of these entries are tested with a facefile's name it is not
     * necessary to encode all faces in the same subdirectory the same way –
     * the first successful retrieval of a card's face ends testing for other
     * available encodings of the same card.
     * <p>
     * @see #getFaceFileName(cardgames.model.cards.MauMauCard) filename for card's face
     */
    public static final String[] FORMATS = { "jpg", "png", "bmp", "gif" };

    TreeMap<String, ReadGrid> loaderGrids = new TreeMap<String, ReadGrid>();

    String defaultFaceName = null;

    File loaderRootDir = null;

    /**
     * Creates default-loader.
     * <p>
     * This loader uses the default temporary-directory of the system denoted by
     * system's property <code>java.io.tmpdir</code>. The default-value for
     * <code>faceName</code> is <code>png</code>.
     * <p>
     * @see #ready() init succeeded?
     */
    public MauMauCardFaceLoader() {
        this(new File(System.getProperty("java.io.tmpdir")), "png");
    }

    /**
     * Creates loader reading faces from <code>rootDir</code>.
     * <p>
     * This loader uses <code>rootDir</code> to <a href="#rootDirectory">read faces</a>.
     * The default-value for <code>faceName</code> is <code>png</code>.
     * <p>
     * @see #ready() init succeeded?
     * @param rootDir to load cards' faces from
     */
    public MauMauCardFaceLoader(File rootDir) {
        this(rootDir, "png");
    }

    /**
     * Creates loader reading faces from <code>rootDir</code>.
     * <p>
     * This loader uses <code>rootDir</code> to <a href="#rootDirectory">read faces</a>.
     * If there is no (valid) subdirectory named <code>faceName</code> in
     * <code>rootDir</code> then <code>gif</code> is set default.
     * <p>
     * @see #ready() init succeeded?
     * @param rootDir to load cards' faces from
     * @param faceName name of faces to load by default
     */
    public MauMauCardFaceLoader(File rootDir, String faceName) {
        if (rootDir.getPath().endsWith(File.separator)) this.loaderRootDir = rootDir; else loaderRootDir = new File(rootDir.getPath() + File.separator);
        if (!loaderRootDir.exists() && !loaderRootDir.mkdirs()) {
            LOGGER.error(loaderRootDir.getPath() + " can not be created");
            return;
        }
        if (!loaderRootDir.isDirectory() || !loaderRootDir.canRead() || !loaderRootDir.canWrite()) {
            LOGGER.error(loaderRootDir.getPath() + " is not directory with read-write-access");
            return;
        }
        initDefaultDirectories(false);
        this.defaultFaceName = faceName.trim().toLowerCase();
        if (!synchronize()) {
            LOGGER.warn("Try to recreate defaults ...");
            initDefaultDirectories(true);
            if (!synchronize()) LOGGER.error("Failed to initialize " + this + " (" + ready() + ')');
        }
    }

    private void initDefaultDirectories(boolean forceWriting) {
        boolean createdcreatedDirectory = false;
        File subDirRoot = new File(loaderRootDir.getPath() + File.separatorChar + "png");
        if (subDirRoot.exists() && subDirRoot.isFile()) {
            LOGGER.error(".initDefaultDirectories(" + forceWriting + ") - can not create directory " + subDirRoot.getPath() + ", there is a file with same name");
            return;
        }
        if (!subDirRoot.exists()) {
            if (subDirRoot.mkdirs()) createdcreatedDirectory = true; else {
                LOGGER.error(".initDefaultDirectories(" + forceWriting + ") - failed to create directory " + subDirRoot.getPath());
                return;
            }
        }
        if (createdcreatedDirectory || forceWriting) {
            LOGGER.info(".initDefaultDirectories(" + forceWriting + ") - writing to " + subDirRoot.getPath());
            File faceFile;
            MauMauCard card;
            Iterator<MauMauCard> iterator = cards.getAllCards().iterator();
            while (iterator.hasNext()) {
                card = iterator.next();
                try {
                    faceFile = new File(subDirRoot.getPath() + File.separatorChar + getFaceFileName(card) + ".png");
                    ImageIO.write(MauMauDeck.getBufferedPNGImage(card), "png", faceFile);
                    LOGGER.info("Wrote " + faceFile.getPath());
                } catch (IOException e) {
                    LOGGER.error("Failed to save face of " + card, e);
                }
            }
        }
        createdcreatedDirectory = false;
        subDirRoot = new File(loaderRootDir.getPath() + File.separatorChar + "gif");
        if (subDirRoot.exists() && subDirRoot.isFile()) {
            LOGGER.error(".initDefaultDirectories(" + forceWriting + ") - can not create directory " + subDirRoot.getPath() + ", there is a file with same name");
            return;
        }
        if (!subDirRoot.exists()) {
            if (subDirRoot.mkdirs()) createdcreatedDirectory = true; else {
                LOGGER.error(".initDefaultDirectories(" + forceWriting + ") - failed to create directory " + subDirRoot.getPath());
                return;
            }
        }
        if (createdcreatedDirectory || forceWriting) {
            LOGGER.info(".initDefaultDirectories(" + forceWriting + ") - writing to " + subDirRoot.getPath());
            File faceFile;
            MauMauCard card;
            Iterator<MauMauCard> iterator = cards.getAllCards().iterator();
            while (iterator.hasNext()) {
                card = iterator.next();
                try {
                    faceFile = new File(subDirRoot.getPath() + File.separatorChar + getFaceFileName(card) + ".gif");
                    ImageIO.write(MauMauDeck.getBufferedGIFImage(card), "gif", faceFile);
                    LOGGER.info("Wrote " + faceFile.getPath());
                } catch (IOException e) {
                    LOGGER.error("Failed to save face of " + card, e);
                }
            }
        }
    }

    private synchronized boolean synchronize() {
        loaderGrids.clear();
        File[] filesInLoadRoot = loaderRootDir.listFiles();
        if (filesInLoadRoot == null) {
            LOGGER.error("Failed to load directory " + loaderRootDir.getPath());
            return false;
        }
        for (int i = 0; i < filesInLoadRoot.length; i++) {
            ReadGrid grid = new ReadGrid(filesInLoadRoot[i]);
            if (grid.grid != null) loaderGrids.put(filesInLoadRoot[i].getName().toLowerCase(), grid);
        }
        if (loaderGrids.isEmpty()) {
            LOGGER.error("Failed to load faces from " + loaderRootDir.getPath());
            return false;
        } else {
            LOGGER.info("Accessing " + loaderGrids.size() + " different faces in " + loaderRootDir.getPath() + ", available faces: " + getAvailableFaces());
            if (loaderGrids.containsKey(defaultFaceName)) LOGGER.info("Using default " + defaultFaceName); else {
                LOGGER.warn("Failed to load default: " + defaultFaceName);
                defaultFaceName = loaderGrids.keySet().toArray()[0].toString();
                LOGGER.warn("Using instead: " + defaultFaceName);
            }
            return true;
        }
    }

    /**
     * Returns name of faces loaded by default.
     * <p>
     * The value returned is the name of faces loaded if there is no parameter
     * specified on retrieval for {@link #getFace(cardgames.model.cards.MauMauCard)}
     * or {@link #getFace(cardgames.model.cards.MauMauCard, int, int)}.
     * <p>
     * If the loader has not been completely initialized (loaders root-directory
     * does not exist and can not be created, {@link #ready()} returns
     * <code>false</code>) <code>null</code> will be returned.
     * <p>
     * @return name of faces loaded by default
     */
    public String getDefault() {
        return defaultFaceName;
    }

    /**
     * Sets default face to load.
     * <p>
     * If there is a subdirectory named <code>newDefault</code> in loaders
     * <a href="#rootDirectory">root-directory</a> the faces are loaded from
     * that subdirectory by default and <code>true</code> is returned.
     * <p>
     * If there is no such subdirectory <code>false</code> is returned and
     * the loader does not change its default.
     * <p>
     * @param newDefault name of subdirectory to load faces from
     * @return <code>true</code> all faces can be loaded from <code>newDefault</code>; <code>false</code> otherwise
     */
    public boolean setDefault(String newDefault) {
        newDefault = newDefault.trim().toLowerCase();
        if (loaderGrids.containsKey(newDefault)) {
            defaultFaceName = newDefault;
            LOGGER.info("Using default " + defaultFaceName);
            return true;
        } else return false;
    }

    /**
     * Get all loadable faces' names
     * @return loadable faces' names
     */
    public Set<String> getAvailableFaces() {
        return loaderGrids.keySet();
    }

    /**
     * Initializes (scans) root-directory again.
     * <p>
     * This operation rescans root-directory. That is useful if you want to
     * change faces on the fly – you copy your faces in a subdirectory of the
     * root-directory, reload the root and can access your faces using the name
     * of your subdirectory as parameter <code>faceName</code> on retrieval.
     * <p>
     * @see <a href="#rootDirectory">initializing loader's root-directory</a>
     * @see #FORMATS loadable encodings
     * @return <code>true</code> loader managed to read at least one face from root-directory; <code>false</code> otherwise
     */
    public boolean reload() {
        return synchronize();
    }

    /**
     * Tests if loader is operational.
     * <p>
     * For a loader to by operational it is required that that loader can read
     * and write its root-directory. Reading is required to test if root-directory
     * exists and already contains both default-faces <code>gif</code> and
     * <code>png</code>. Writing is required to create root-directory if it does
     * not exist and to copy these faces into loader's root-directory if both
     * defaults are not found there.
     * <p>
     * This operation tests if the loader can access at least a single face and
     * the {@link #getDefault() default} is loadable. If the loader managed to
     * initialize the root-directory this operation returns <code>true</code>.
     * <p>
     * @return <code>true</code> loader managed to read at least one face from root-directory and the default is loadable; <code>false</code> otherwise
     */
    public boolean ready() {
        return ((loaderGrids.size() > 0) && loaderGrids.containsKey(defaultFaceName));
    }

    /**
     * Returns <code>card</code>'s default-face.
     * <p>
     * Loads default-face for <code>card</code> from filesystem. Any
     * <code>IOException</code> that may occur reading some file is reported to
     * {@link #getLogger() logger} and makes this operation return <code>null</code>.
     * <p>
     * @param card to load face for
     * @return face for <code>card</code> or <code>null</code> if the underlying file could not be read from filesystem
     */
    public BufferedImage getFace(MauMauCard card) {
        return getFace(card, defaultFaceName);
    }

    /**
     * Returns a specific face for <code>card</code>.
     * <p>
     * The parameter <code>faceName</code> denotes the subdirectory the loader
     * tries to read the face-file from. If that directory does not exist, the
     * {@link #getDefault() default} is used instead. Any <code>IOException</code>
     * that may occur reading some file is reported to {@link #getLogger() logger}
     * and makes this operation return <code>null</code>.
     * <p>
     * @param card to load face for
     * @param faceName name (kind) of face to load
     * @return face for <code>card</code> or <code>null</code> if the underlying file could not be read from filesystem
     */
    public BufferedImage getFace(MauMauCard card, String faceName) {
        if (!ready()) {
            LOGGER.error(".getFace(" + card + ", " + faceName + ") - not initialized");
            return null;
        }
        try {
            faceName = faceName.trim().toLowerCase();
            if (loaderGrids.containsKey(faceName)) return loaderGrids.get(faceName).getFace(card); else return loaderGrids.get(defaultFaceName).getFace(card);
        } catch (IOException i) {
            LOGGER.warn(".getFace(" + card + ", " + faceName + ") - failed to load file", i);
            initDefaultDirectories(true);
            if (!synchronize()) LOGGER.error(".getFace(" + card + ", " + faceName + ") - failed synchronzing " + loaderRootDir.getPath());
            return null;
        }
    }

    /**
     * Returns <code>card</code>'s default-face accurate to size.
     * <p>
     * The face loaded is resized to given <code>width</code> and <code>height</code>.
     * This implementation does not use <a href="http://www.mkyong.com/java/how-to-resize-an-image-in-java/" target="_BLANK"><code>RenderingHints</code></a>
     * to maintain best quality. The value of both parameters <code>width</code>
     * and <code>height</code> must be greater than <code>0</code> or an
     * <code>IllegalArgumentException</code> occurs.
     * <p>
     * @see <a href="http://www.mkyong.com/java/how-to-resize-an-image-in-java/" target="_BLANK">How to resize an image in Java?</a>
     * @param card to load face for
     * @param width of image returned
     * @param height of image returned
     * @throws IllegalArgumentException if <code>(width &lt;= 0) | (height &lt;= 0)</code>
     * @return face for <code>card</code> or <code>null</code> if the underlying file could not be read from filesystem
     */
    public BufferedImage getFace(MauMauCard card, int width, int height) {
        return getFace(card, defaultFaceName, width, height);
    }

    /**
     * Returns <code>card</code>'s face accurate to size.
     * <p>
     * The <code>faceName</code> denotes the subdirectory the loader tries to
     * read the face-file from. If that directory does not exist, the
     * {@link #getDefault() default} is used instead. Any <code>IOException</code>
     * that may occur reading some file is reported to {@link #getLogger() logger}
     * and makes this operation return <code>null</code>.
     * <p>
     * The face loaded is resized to given <code>width</code> and <code>height</code>.
     * This implementation does not use <a href="http://www.mkyong.com/java/how-to-resize-an-image-in-java/" target="_BLANK"><code>RenderingHints</code></a>
     * to maintain best quality. The value of both parameters <code>width</code>
     * and <code>height</code> must be greater than <code>0</code> or an
     * <code>IllegalArgumentException</code> occurs.
     * <p>
     * @see <a href="http://www.mkyong.com/java/how-to-resize-an-image-in-java/" target="_BLANK">How to resize an image in Java?</a>
     * @param card to load face for
     * @param faceName name (kind) of face to load
     * @param width of image returned
     * @param height of image returned
     * @throws IllegalArgumentException if <code>(width &lt;= 0) | (height &lt;= 0)</code>
     * @return face for <code>card</code> or <code>null</code> if the underlying file could not be read from filesystem
     */
    public BufferedImage getFace(MauMauCard card, String faceName, int width, int height) {
        BufferedImage original = getFace(card, faceName);
        if (original != null) {
            BufferedImage resized = new BufferedImage(width, height, original.getType());
            Graphics2D g = resized.createGraphics();
            g.drawImage(original, 0, 0, width, height, null);
            g.dispose();
            return resized;
        } else return null;
    }

    /**
     * Translates a MauMau-card to a filename to load that card's face from.
     * <p>
     * Each {@link MauMauCard card} has a {@link MauMauCard#getSuit() suit} and
     * a {@link MauMauCard#getRank() rank}. The suit's number is translated into
     * the {@link FrenchCard#SUITS English name} of that suit. This name is the
     * first (alphabetic) part of the face's filename. The second is the
     * double-digit rank.
     * <p>
     * Please note that the names returned do <strong>not</strong> have any
     * extension such as <code>.bmp</code>, <code>.gif</code>, <code>.jpg</code>
     * or <code>.png</code>. The returned name will always be one of the following:
     * <code>Clubs02</code>, <code>Clubs03</code>, <code>Clubs04</code>,
     * <code>Clubs07</code>, <code>Clubs08</code>, <code>Clubs09</code>,
     * <code>Clubs10</code>, <code>Clubs11</code>, <code>Diamonds02</code>,
     * <code>Diamonds03</code>, <code>Diamonds04</code>, <code>Diamonds07</code>,
     * <code>Diamonds08</code>, <code>Diamonds09</code>, <code>Diamonds10</code>,
     * <code>Diamonds11</code>, <code>Hearts02</code>, <code>Hearts03</code>,
     * <code>Hearts04</code>, <code>Hearts07</code>, <code>Hearts08</code>,
     * <code>Hearts09</code>, <code>Hearts10</code>, <code>Hearts11</code>,
     * <code>Spades02</code>, <code>Spades03</code>, <code>Spades04</code>,
     * <code>Spades07</code>, <code>Spades08</code>, <code>Spades09</code>,
     * <code>Spades10</code> or <code>Spades11</code>.
     * <p>
     * @see #FORMATS filenames' extensions
     * @param card to get face's filename for
     * @return name of file to load card's face from
     */
    public static String getFaceFileName(MauMauCard card) {
        StringBuilder name = new StringBuilder(128);
        name.append(FrenchCard.SUITS[card.getSuit()]);
        if (card.getRank() < 10) name.append('0');
        name.append(card.getRank());
        return name.toString();
    }

    class ReadGrid {

        File gridRoot = null;

        File[][] grid = null;

        public ReadGrid(File root) {
            this.gridRoot = root;
            String gridRootPath;
            try {
                gridRootPath = gridRoot.getCanonicalPath();
            } catch (IOException i) {
                LOGGER.warn("Failed to determine canonical path", i);
                gridRootPath = gridRoot.getPath();
            }
            if (!gridRoot.isDirectory() || !gridRoot.canRead()) {
                LOGGER.warn(gridRootPath + " is not a readable directory");
                return;
            }
            File[] filesInGridRoot = gridRoot.listFiles();
            if (filesInGridRoot == null) {
                LOGGER.warn(gridRootPath + " is empty");
                return;
            }
            if (filesInGridRoot.length < cards.getCardsCount()) {
                LOGGER.warn(gridRootPath + " contains just " + filesInGridRoot.length + " entries, at least " + cards.getCardsCount() + " expected");
                return;
            }
            LOGGER.info("Try to build grid from " + gridRootPath + " (" + filesInGridRoot.length + " files) ...");
            MauMauCard card;
            File fileToLoadCardsFaceFrom = null;
            String nameOfFileToLoadCardsFaceFrom;
            grid = new File[FrenchCard.SUIT_CLUBS + 1][MauMauCard.RANK_ACE + 1];
            Iterator<MauMauCard> iterator = cards.getAllCards().iterator();
            while (iterator.hasNext()) {
                card = iterator.next();
                for (int i = 0; i < FORMATS.length; i++) {
                    nameOfFileToLoadCardsFaceFrom = getFaceFileName(card) + '.' + FORMATS[i];
                    for (int ii = 0; (ii < filesInGridRoot.length) && (fileToLoadCardsFaceFrom == null); ii++) if (filesInGridRoot[ii].getName().equalsIgnoreCase(nameOfFileToLoadCardsFaceFrom)) fileToLoadCardsFaceFrom = filesInGridRoot[ii];
                    if (fileToLoadCardsFaceFrom == null) fileToLoadCardsFaceFrom = new File(gridRootPath + File.separatorChar + nameOfFileToLoadCardsFaceFrom);
                    if (fileToLoadCardsFaceFrom.exists() && fileToLoadCardsFaceFrom.isFile() && fileToLoadCardsFaceFrom.canRead() && (fileToLoadCardsFaceFrom.getTotalSpace() > 0)) {
                        try {
                            ImageIO.read(fileToLoadCardsFaceFrom);
                            grid[card.getSuit()][card.getRank()] = fileToLoadCardsFaceFrom;
                            LOGGER.info("Read face for " + card + " from " + fileToLoadCardsFaceFrom.getCanonicalPath() + ", " + fileToLoadCardsFaceFrom.getTotalSpace() + " bytes @grid(s" + card.getSuit() + ", r" + card.getRank() + ')');
                            i = FORMATS.length;
                        } catch (IOException iE) {
                            LOGGER.warn("Failed to load face for " + card, iE);
                        }
                    }
                    fileToLoadCardsFaceFrom = null;
                }
            }
            try {
                Long[][] crcGrid = new Long[FrenchCard.SUIT_CLUBS + 1][MauMauCard.RANK_ACE + 1];
                MauMauCard outerCard, innerCard;
                Iterator<MauMauCard> innerIterator;
                Iterator<MauMauCard> outerIterator = cards.getAllCards().iterator();
                while (outerIterator.hasNext()) {
                    outerCard = outerIterator.next();
                    innerIterator = cards.getAllCards().iterator();
                    while (innerIterator.hasNext()) {
                        innerCard = innerIterator.next();
                        if (!outerCard.equals(innerCard)) {
                            if (grid[innerCard.getSuit()][innerCard.getRank()] == null) {
                                LOGGER.warn("Grid (s" + innerCard.getSuit() + ", r" + innerCard.getRank() + ") is empty");
                                grid = null;
                                return;
                            }
                            if (grid[outerCard.getSuit()][outerCard.getRank()] == null) {
                                LOGGER.warn("Grid (s" + outerCard.getSuit() + ", r" + outerCard.getRank() + ") is empty");
                                grid = null;
                                return;
                            }
                            if (grid[innerCard.getSuit()][innerCard.getRank()].equals(grid[outerCard.getSuit()][outerCard.getRank()])) {
                                LOGGER.warn("Grids (s" + innerCard.getSuit() + ", r" + innerCard.getRank() + ") and (s" + outerCard.getSuit() + ", r" + outerCard.getRank() + ") denote the same file: " + grid[outerCard.getSuit()][outerCard.getRank()].getCanonicalPath());
                                grid = null;
                                return;
                            }
                            if (grid[outerCard.getSuit()][outerCard.getRank()].getTotalSpace() == grid[innerCard.getSuit()][innerCard.getRank()].getTotalSpace()) {
                                if (crcGrid[outerCard.getSuit()][outerCard.getRank()] == null) crcGrid[outerCard.getSuit()][outerCard.getRank()] = new Long(getCRC32(grid[outerCard.getSuit()][outerCard.getRank()]));
                                if (crcGrid[innerCard.getSuit()][innerCard.getRank()] == null) crcGrid[innerCard.getSuit()][innerCard.getRank()] = new Long(getCRC32(grid[innerCard.getSuit()][innerCard.getRank()]));
                                if (crcGrid[outerCard.getSuit()][outerCard.getRank()].equals(crcGrid[innerCard.getSuit()][innerCard.getRank()])) {
                                    LOGGER.warn("Got two different files ( " + grid[innerCard.getSuit()][innerCard.getRank()].getTotalSpace() + " bytes) with same CRC32 (" + crcGrid[innerCard.getSuit()][innerCard.getRank()] + ')' + System.getProperty("line.separator") + grid[innerCard.getSuit()][innerCard.getRank()].getCanonicalPath() + System.getProperty("line.separator") + grid[outerCard.getSuit()][outerCard.getRank()].getCanonicalPath());
                                    grid = null;
                                    return;
                                }
                            }
                        }
                    }
                    LOGGER.info("Verified face for " + outerCard + " from " + grid[outerCard.getSuit()][outerCard.getRank()].getCanonicalPath());
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to validate grid", e);
                grid = null;
            }
        }

        /**
         * @see http://www.jguru.com/faq/view.jsp?EID=216274
         */
        private long getCRC32(File checkFile) throws IOException {
            FileInputStream file = new FileInputStream(checkFile);
            CheckedInputStream check = new CheckedInputStream(file, new CRC32());
            BufferedInputStream in = new BufferedInputStream(check);
            while (in.read() != -1) {
            }
            return check.getChecksum().getValue();
        }

        public BufferedImage getFace(MauMauCard card) throws IOException {
            return ImageIO.read(grid[card.getSuit()][card.getRank()]);
        }
    }

    /**
     * Returns static logger
     * @return classe's logger
     */
    public static Logger getLogger() {
        return LOGGER;
    }

    /**
     * Sets static logger
     * @param logger to use
     */
    public static void setLogger(Logger logger) {
        LOGGER = logger;
    }
}
