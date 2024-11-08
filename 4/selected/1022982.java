package jseki.common.game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import jseki.common.lang.Node;
import jseki.common.sgf.SGFFile;
import jseki.common.sgf.SGFInfo;
import jseki.kgs.statistics.model.Constants;
import jseki.kgs.statistics.model.GameAttribute;
import jseki.sgfast.common.MutableSGFNode;
import jseki.sgfast.common.SGFNode;
import jseki.sgfast.parser.SGFReader;
import org.apache.log4j.Logger;

public class GameRecord {

    private static final Logger log = Logger.getLogger(GameRecord.class);

    private String archive;

    private String filename;

    private SGFInfo cachedInfo;

    public GameRecord(ZipFile zipFile, ZipEntry entry) {
        this.archive = zipFile.getName();
        this.filename = entry.getName();
    }

    public GameRecord(File file) {
        this.archive = null;
        try {
            this.filename = file.getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SGFFile parseFile() {
        log.info("Parsing file: " + archive + ", " + filename);
        try {
            return new SGFFile(getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream getInputStream() {
        try {
            if (archive == null) {
                return new FileInputStream(new File(filename));
            } else {
                ZipFile zipFile = new ZipFile(new File(archive));
                ZipEntry entry = zipFile.getEntry(filename);
                return zipFile.getInputStream(entry);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<GameAttribute> getAttributes(List<String> usernames) {
        SGFInfo info = getInfo();
        Set<GameAttribute> gameAttributes = new HashSet<GameAttribute>();
        String result = info.getResult();
        if (result != null) {
            gameAttributes.add(GameAttribute.FINISHED);
            if (result.endsWith("+Resign")) {
                gameAttributes.add(GameAttribute.RESIGNED);
            }
        }
        if (usernames.contains(info.getPlayerWhite().toLowerCase())) {
            gameAttributes.add(GameAttribute.WHITE);
            if (result != null && result.charAt(0) == 'W') {
                gameAttributes.add(GameAttribute.WON);
            }
        }
        if (usernames.contains(info.getPlayerBlack().toLowerCase())) {
            gameAttributes.add(GameAttribute.BLACK);
            if (result != null && result.charAt(0) == 'B') {
                gameAttributes.add(GameAttribute.WON);
            }
        }
        return gameAttributes;
    }

    public String getFilename() {
        return filename;
    }

    public String getDate() {
        return getInfo().getDate();
    }

    public String getWinner() {
        return getInfo().getWinner();
    }

    public List<Move> getMoves() {
        return parseFile().getMoves();
    }

    public Node<Move> getMoveTree() {
        return parseFile().getMoveTree();
    }

    public SGFFile getSGFFile() {
        return parseFile();
    }

    private SGFInfo getInfo() {
        if (cachedInfo == null) {
            try {
                Reader input = new InputStreamReader(getInputStream(), "UTF-8");
                SGFReader reader = new SGFReader(input);
                SGFNode header = reader.parse();
                ((MutableSGFNode) header).removeChildren();
                cachedInfo = new SGFInfo(header.getProperties());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return cachedInfo;
    }

    @Override
    public String toString() {
        return getInfo().toString();
    }

    public File getTemporaryFile() throws IOException {
        File file = File.createTempFile(Constants.APP_NAME.replace(' ', '-'), ".sgf");
        file.deleteOnExit();
        FileOutputStream out = new FileOutputStream(file);
        InputStream in = getInputStream();
        byte[] buf = new byte[256];
        int read = 0;
        while ((read = in.read(buf)) > 0) {
            out.write(buf, 0, read);
        }
        in.close();
        out.close();
        return file;
    }
}
