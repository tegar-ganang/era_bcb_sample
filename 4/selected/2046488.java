package org.brandao.brutos.type;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.brandao.brutos.ConfigurableApplicationContext;
import org.brandao.brutos.Invoker;
import org.brandao.brutos.MvcResponse;
import org.brandao.brutos.web.http.BrutosFile;

/**
 *
 * @author Afonso Brandao
 */
public class BrutosFileType implements Type {

    public BrutosFileType() {
    }

    public Class getClassType() {
        return BrutosFile.class;
    }

    public Object getValue(Object value) {
        if (value instanceof BrutosFile) return value;
        if (value == null) return null; else throw new UnknownTypeException(value.getClass().getName());
    }

    public void setValue(Object value) throws IOException {
        if (value instanceof BrutosFile) {
            ConfigurableApplicationContext app = (ConfigurableApplicationContext) Invoker.getApplicationContext();
            MvcResponse response = app.getMvcResponse();
            BrutosFile f = (BrutosFile) value;
            if (f.getFile() != null) {
                response.setInfo("Content-Disposition", "attachment;filename=" + f.getFileName() + ";");
            }
            response.setLength((int) f.getFile().length());
            InputStream in = new FileInputStream(f.getFile());
            OutputStream out = response.processStream();
            try {
                byte[] buffer = new byte[3072];
                int length;
                while ((length = in.read(buffer)) != -1) out.write(buffer, 0, length);
            } finally {
                if (in != null) in.close();
            }
        }
    }
}
