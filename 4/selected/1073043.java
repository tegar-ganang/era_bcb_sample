package br.biofoco.p2p.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.codehaus.jackson.map.ObjectMapper;
import br.biofoco.p2p.rpc.messaging.WireMessage;

public class JsonSerializer implements Serializer {

    @Override
    public <T extends WireMessage> void serialize(T message, OutputStream out) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String response = mapper.writeValueAsString(message);
        DataOutputStream daos = new DataOutputStream(out);
        byte[] buf = response.getBytes();
        daos.writeInt(buf.length);
        daos.write(buf, 0, buf.length);
        daos.flush();
    }

    @Override
    public <T extends WireMessage> T unserialize(InputStream in, Class<T> value) throws IOException {
        DataInputStream dais = new DataInputStream(in);
        int size = dais.readInt();
        int read = 0;
        byte[] buf = new byte[64 * 1024];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (size > 0) {
            read = dais.read(buf, 0, buf.length);
            if (read > 0) {
                size = size - read;
                baos.write(buf, 0, read);
            }
        }
        byte[] result = baos.toByteArray();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(result, 0, result.length, value);
    }
}
