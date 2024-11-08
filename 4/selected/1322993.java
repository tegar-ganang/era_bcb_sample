package cloudspace.ui.applet.io;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import cloudspace.ui.applet.AppletVM;
import cloudspace.ui.applet.BASE64;
import cloudspace.ui.applet.CloudResult;
import cloudspace.ui.applet.CloudToApplet;
import cloudspace.ui.applet.JSFactory;
import cloudspace.vm.VM;

public class CloudFileInputStream extends InputStream {

    UUID id = null;

    public CloudFileInputStream(File file) throws FileNotFoundException {
        this(file != null ? file.getPath() : null);
    }

    public CloudFileInputStream(String fileName) throws FileNotFoundException {
        if (fileName == null) throw new NullPointerException();
        CloudResult result = AppletVM.callJScriptFunc(CREATE_JS_FUNC, fileName);
        if (result.isError()) {
            Throwable throwing = result.getException();
            if (throwing instanceof FileNotFoundException) throw (FileNotFoundException) throwing; else throw new RuntimeException(throwing);
        }
        String localId = result.getResults()[0];
        id = UUID.fromString(localId);
    }

    public CloudFileInputStream(FileDescriptor fd) {
        throw new UnsupportedOperationException();
    }

    public FileDescriptor getFD() {
        throw new UnsupportedOperationException();
    }

    public FileChannel getChannel() {
        throw new UnsupportedOperationException();
    }

    private static Integer getInteger(String response) {
        if (response == null) {
            return null;
        }
        try {
            Integer parsed = Integer.parseInt(response);
            return parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public int read() throws IOException {
        CloudResult result = AppletVM.callJScriptFunc(READ_JS_FUNC, id.toString());
        if (result.isError()) {
            Throwable throwing = result.getException();
            if (throwing instanceof IOException) throw (IOException) throwing; else throw new RuntimeException(throwing);
        }
        String response = result.getResults()[0];
        Integer read = getInteger(response);
        if (read == null) {
            throw new IOException(response);
        }
        return read;
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        try {
            if (b == null) {
                throw new NullPointerException();
            } else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }
            CloudResult result = AppletVM.callJScriptFunc(READ_CHUNK_JS_FUNC, id.toString(), Integer.toString(len));
            if (result.isError()) {
                Throwable throwing = result.getException();
                if (throwing instanceof IOException) throw (IOException) throwing; else throw (RuntimeException) throwing;
            }
            String content = result.getResult(0);
            int length = Integer.parseInt(result.getResult(1));
            if (length == -1) return length;
            BASE64.decodeContent(content, b, off, length);
            return length;
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        AppletVM.callJScriptFunc(CLOSE_FILE_INPUT_STREAM, id.toString());
    }

    private static final String FINALIZE_FILE_INPUT_STREAM = "finalizeCloudInputStream";

    private static final String FINALIZE_EVENT = "onFinalizeFileInputStream";

    private static final String[] FINALIZE_PARAM = new String[] { "id" };

    private static String FINALIZE_FUNC = JSFactory.generateJSFunction(FINALIZE_FILE_INPUT_STREAM, FINALIZE_EVENT, FINALIZE_PARAM);

    @Override
    public void finalize() {
        try {
            AppletVM.callJScriptFunc(FINALIZE_FILE_INPUT_STREAM, id.toString());
        } catch (Exception e) {
        }
    }

    private static final String CREATE_JS_FUNC = "registerCloudFileInputStream";

    private static final String CREATE_EVENT = "onRegisterCloudFileInputStream";

    private static final String[] CREATE_PARAM = new String[] { "file" };

    private static String CREATE_FUNC = JSFactory.generateJSFunction(CREATE_JS_FUNC, CREATE_EVENT, CREATE_PARAM);

    private static final String READ_JS_FUNC = "readCloudFileInputStream";

    private static final String READ_EVENT = "onReadCloudFileInputStream";

    private static final String[] READ_PARAM = new String[] { "id" };

    private static String READ_FUNC = JSFactory.generateJSFunction(READ_JS_FUNC, READ_EVENT, READ_PARAM);

    private static final String READ_CHUNK_JS_FUNC = "readChunkCloudFileInputStream";

    private static final String READ_CHUNK_EVENT = "onReadChunkCloudFileInputStream";

    private static final String[] READ_CHUNK_PARAM = new String[] { "id", "len" };

    private static String READ_CHUNK_FUNC = JSFactory.generateJSFunction(READ_CHUNK_JS_FUNC, READ_CHUNK_EVENT, READ_CHUNK_PARAM);

    private static final String CLOSE_FILE_INPUT_STREAM = "closeFileInputStream";

    private static final String CLOSE_EVENT = "onCloseFileInputStream";

    private static final String[] CLOSE_PARAM = new String[] { "id" };

    private static String CLOSE_FUNC = JSFactory.generateJSFunction(CLOSE_FILE_INPUT_STREAM, CLOSE_EVENT, CLOSE_PARAM);

    private static class CreateHandler implements CommandHandler {

        public CloudToApplet handleRequest(Map<String, Object> params, Map<String, Object> objects) throws Exception {
            String filename = (String) params.get("file");
            UUID id = UUID.randomUUID();
            objects.put(id.toString(), (Object) new FileInputStream(VM.currentVM().getLocalRoot() + "/" + filename));
            return new CloudToApplet(id.toString());
        }
    }

    private static class ReadHandler implements CommandHandler {

        public CloudToApplet handleRequest(Map<String, Object> params, Map<String, Object> objects) throws Exception {
            String localId = (String) params.get("id");
            FileInputStream in = (FileInputStream) objects.get(localId);
            return new CloudToApplet(Integer.toString(in.read()));
        }
    }

    private static class ReadChunkHandler implements CommandHandler {

        public CloudToApplet handleRequest(Map<String, Object> params, Map<String, Object> objects) throws Exception {
            String localId = (String) params.get("id");
            int len = Integer.parseInt((String) params.get("len"));
            FileInputStream in = (FileInputStream) objects.get(localId);
            byte[] toRead = new byte[len];
            int read = in.read(toRead, 0, len);
            String result = BASE64.encodeContent(toRead);
            return new CloudToApplet(result, read);
        }
    }

    private static class CloseHandler implements CommandHandler {

        public CloudToApplet handleRequest(Map<String, Object> params, Map<String, Object> objects) throws Exception {
            String localId = (String) params.get("id");
            FileInputStream in = (FileInputStream) objects.get(localId);
            in.close();
            return new CloudToApplet();
        }
    }

    private static class FinalizeHandler implements CommandHandler {

        public CloudToApplet handleRequest(Map<String, Object> params, Map<String, Object> objects) throws Exception {
            String localId = (String) params.get("id");
            FileInputStream in = (FileInputStream) objects.get(localId);
            in.close();
            params.remove(localId);
            return new CloudToApplet();
        }
    }

    private static final List<String> localScripts = new ArrayList<String>();

    private static final Map<String, CommandHandler> localEvents = new HashMap<String, CommandHandler>();

    static {
        localScripts.add(READ_FUNC);
        localScripts.add(CREATE_FUNC);
        localScripts.add(CLOSE_FUNC);
        localScripts.add(READ_CHUNK_FUNC);
        localScripts.add(FINALIZE_FUNC);
        localEvents.put(CREATE_EVENT, new CreateHandler());
        localEvents.put(READ_EVENT, new ReadHandler());
        localEvents.put(CLOSE_EVENT, new CloseHandler());
        localEvents.put(READ_CHUNK_EVENT, new ReadChunkHandler());
        localEvents.put(FINALIZE_EVENT, new FinalizeHandler());
    }

    public static List<String> getJavaScript() {
        return localScripts;
    }

    public static Map<String, CommandHandler> getCommandHandlers() {
        return localEvents;
    }
}
