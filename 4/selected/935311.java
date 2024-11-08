package rubbish.db.lob;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Clob;
import java.sql.SQLException;
import rubbish.db.exception.IORuntimeException;
import rubbish.db.exception.SQLRuntimeException;
import rubbish.db.util.IOUtils;

/**
 * �L�����N�^�I�u�W�F�N�g�̃��b�p�[
 * 
 * @author $Author: winebarrel $
 * @version $Revision: 1.1 $
 */
public class CharWrapper {

    protected String str = null;

    public CharWrapper(Clob clob) {
        try {
            Reader reader = clob.getCharacterStream();
            Writer writer = new StringWriter();
            IOUtils.pipe(reader, writer);
            this.str = writer.toString();
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    public CharWrapper(char[] cs) {
        this.str = new String(cs);
    }

    public String toString() {
        return str;
    }

    public void write(Writer writer) {
        StringReader reader = new StringReader(str);
        IOUtils.pipe(reader, writer);
    }

    public void write(String filename) {
        try {
            write(new FileWriter(filename));
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }
}
