package com.raidan.dclog.core.parser.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.raidan.dclog.core.Config;
import com.raidan.dclog.core.parser.ParseLineException;
import com.raidan.dclog.face.IVisual;

/**
 * Класс для построчного чтения из файла
 * 
 * @author raidan
 */
public abstract class AbstractTextReader {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTextReader.class);

    public void parseImpl(String pathToFile) throws IOException, SQLException, ParseLineException {
        File file = new File(pathToFile);
        if (!file.exists()) {
            logger.warn("Skipping loading file '{}'", pathToFile);
            return;
        }
        FileInputStream finp = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(finp, Config.getInstance().getDefaultEncoding()));
        int totalSize = (int) finp.getChannel().size();
        int currentSize = 0;
        IVisual progress = Config.getInstance().getProgressInstance();
        progress.processStart(false);
        try {
            String line;
            int pos = 0;
            while ((line = reader.readLine()) != null) {
                this.readLine(++pos, line);
                currentSize += line.length() + "\n".length();
                progress.process(currentSize, totalSize);
            }
            progress.processOver();
        } finally {
            reader.close();
        }
    }

    public abstract void readLine(int linePos, String line) throws SQLException, ParseLineException;
}
