package net.sourceforge.javautil.common.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import net.sourceforge.javautil.common.ByteUtil;
import net.sourceforge.javautil.common.IOUtil;
import net.sourceforge.javautil.common.StringUtil;
import net.sourceforge.javautil.common.encryption.EncryptingOutputStream;
import net.sourceforge.javautil.common.encryption.EncryptionIOHandler;
import net.sourceforge.javautil.common.encryption.IEncryptionProvider;
import net.sourceforge.javautil.common.encryption.impl.SimpleEncryptionKey;
import net.sourceforge.javautil.common.encryption.impl.SimpleEncryptionProvider;
import net.sourceforge.javautil.common.encryption.impl.SimpleEncryptionKey.Strength;
import net.sourceforge.javautil.common.io.impl.SystemFile;
import net.sourceforge.javautil.common.password.EncryptedPassword;
import net.sourceforge.javautil.common.password.IPassword;
import net.sourceforge.javautil.common.password.impl.StandardPasswordLocker;
import net.sourceforge.javautil.common.password.impl.UnencryptedPassword;
import org.testng.annotations.Test;

/**
 * Testing of encryption services
 *
 * @author elponderador
 * @author $Author: ponderator $
 * @version $Id: EncryptionTest.java 2712 2011-01-03 00:32:01Z ponderator $
 */
@Test
public class EncryptionTest {

    public void testPasswordStore() throws Exception {
        IEncryptionProvider provider = new SimpleEncryptionProvider(SimpleEncryptionKey.createUsing(Strength.STRONG, "AES", "Hello World".getBytes()));
        SystemFile store = new SystemFile("target/password.store");
        StandardPasswordLocker locker = new StandardPasswordLocker("test", provider, store, "Another Password".getBytes());
        IPassword newp = new UnencryptedPassword("some password");
        if (!store.isExists()) {
            locker.setPassword("some unique id", newp);
        }
        assert ByteUtil.equals(newp.getPassword(), locker.getPassword("some unique id").getPassword());
        locker.save();
    }

    public void testArtifact() throws Exception {
        IEncryptionProvider provider = new SimpleEncryptionProvider(SimpleEncryptionKey.createUsing(Strength.STRONG, "AES", "Hello World".getBytes()));
        String contents = "These are some contents";
        SystemFile file = new SystemFile("target/test.encrypted");
        file.setIOHandler(new EncryptionIOHandler(provider));
        if (file.isExists()) {
            String decrypted = IOUtil.transfer(file.getInputStream(), new ByteArrayOutputStream()).toString();
            assert decrypted.equals(contents);
        }
        OutputStream output = file.getOutputStream();
        IOUtil.transfer(new ByteArrayInputStream(contents.getBytes()), output);
        output.close();
    }

    public void testServices() throws Exception {
        IEncryptionProvider provider = new SimpleEncryptionProvider(SimpleEncryptionKey.createUsing(Strength.STRONG, "AES", "Hello World".getBytes()));
        String original = "This is some text";
        byte[] encrypted = provider.encrypt(original.getBytes());
        assert new String(provider.decrypt(encrypted)).equals(original);
        ByteArrayOutputStream encryptedStream = new ByteArrayOutputStream();
        OutputStream output = provider.getEncryptingOutputStream(encryptedStream);
        StringBuffer originalBuffer = new StringBuffer();
        for (int i = 0; i < 100; i++) {
            String data = StringUtil.repeat("Hello World", i);
            output.write(data.getBytes());
            originalBuffer.append(data);
        }
        output.close();
        String decryptedFirst = new String(provider.decrypt(encryptedStream.toByteArray()));
        assert decryptedFirst.equals(originalBuffer.toString());
        InputStream input = provider.getDecryptingInputStream(new ByteArrayInputStream(encryptedStream.toByteArray()));
        int read = -1;
        byte[] buffer = new byte[200];
        ByteArrayOutputStream decrypted = new ByteArrayOutputStream();
        while ((read = input.read(buffer)) != -1) {
            decrypted.write(ByteUtil.getSlice(buffer, 0, read));
        }
        assert decryptedFirst.equals(new String(decrypted.toByteArray()));
    }
}
