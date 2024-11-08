package br.gov.frameworkdemoiselle.mail.internal.implementation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.activation.FileDataSource;
import br.gov.frameworkdemoiselle.mail.MailException;
import br.gov.frameworkdemoiselle.mail.internal.enums.ContentDisposition;

public class FileAttachment extends BaseAttachment {

    public FileAttachment(ContentDisposition contentDisposition, File file) {
        super();
        FileDataSource fileDataSource = new FileDataSource(file);
        try {
            super.setFileName(fileDataSource.getName());
            super.setMimeType(fileDataSource.getContentType());
            super.setContentDisposition(contentDisposition);
            super.setBytes(toByteArray(file));
        } catch (IOException e) {
            throw new MailException("Can't create File Attachment", e);
        }
    }

    public byte[] toByteArray(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        try {
            for (int readNum; (readNum = fis.read(buf)) != -1; ) {
                bos.write(buf, 0, readNum);
                System.out.println("read " + readNum + " bytes,");
            }
        } catch (IOException ex) {
        }
        byte[] bytes = bos.toByteArray();
        return bytes;
    }
}
