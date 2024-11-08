package org.middleheaven.license;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.middleheaven.crypto.Base64CipherAlgorithm;
import org.middleheaven.io.IOUtils;

public class ClassDefinition {

    private static Base64CipherAlgorithm base64 = new Base64CipherAlgorithm();

    public ClassDefinition() {
    }

    public void write(OutputStream out, String className, InputStream classDefStream) throws IOException {
        ByteArrayOutputStream a = new ByteArrayOutputStream();
        IOUtils.copy(classDefStream, a);
        a.close();
        DataOutputStream da = new DataOutputStream(out);
        da.writeUTF(className);
        da.writeUTF(new String(base64.cipher(a.toByteArray())));
    }

    public String read(InputStream in, OutputStream classDefStream) throws IOException {
        DataInputStream d = new DataInputStream(in);
        String name = d.readUTF();
        String code = d.readUTF();
        byte[] def = base64.revertCipher(code.getBytes());
        classDefStream.write(def);
        return name;
    }
}
