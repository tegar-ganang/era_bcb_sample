package org.brandao.brutos.type;

import java.io.File;
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
public class FileType implements Type {

    public FileType() {
    }

    public Class getClassType() {
        return File.class;
    }

    public Object getValue(Object value) {
        if (value instanceof BrutosFile) return ((BrutosFile) value).getFile(); else return null;
    }

    public void setValue(Object value) throws IOException {
        if (value instanceof File) {
            ConfigurableApplicationContext app = (ConfigurableApplicationContext) Invoker.getApplicationContext();
            MvcResponse response = app.getMvcResponse();
            File f = (File) value;
            response.setInfo("Content-Disposition", "attachment;filename=" + f.getName() + ";");
            response.setLength((int) f.length());
            InputStream in = new FileInputStream(f);
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
