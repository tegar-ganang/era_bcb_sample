package cloudspace.ui.applet.io;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

public class CloudFileOutputStream extends OutputStream {

    private static final String REGISTER_CLOUD_FILE_OUTPUT_STREAM = "registerCloudFileOutputStream";

    private static final String WRITE_CLOUD_FILE_OUTPUT_STREAM = "writeCloudFileOutputStream";

    private static final String CLOSE_CLOUD_FILE_OUTPUT_STREAM = "closeCloudFileOutputStream";

    UUID id;

    public CloudFileOutputStream(File file) throws FileNotFoundException {
        this(file, false);
    }

    public CloudFileOutputStream(FileDescriptor fd) {
        throw new UnsupportedOperationException();
    }

    public CloudFileOutputStream(String fileName) throws FileNotFoundException {
        this(fileName, false);
    }

    public CloudFileOutputStream(File file, boolean append) throws FileNotFoundException {
        this(file != null ? file.getPath() : null, append);
    }

    public CloudFileOutputStream(String fileName, Boolean append) throws FileNotFoundException {
        if (fileName == null) throw new NullPointerException();
        CloudResult result = AppletVM.callJScriptFunc(REGISTER_CLOUD_FILE_OUTPUT_STREAM, fileName, append.toString());
        if (result.isError()) {
            Throwable throwing = result.getException();
            if (throwing instanceof FileNotFoundException) throw (FileNotFoundException) throwing; else throw new RuntimeException(throwing);
        }
        String localId = result.getResults()[0];
        id = UUID.fromString(localId);
    }

    public FileDescriptor getFD() {
        throw new UnsupportedOperationException();
    }

    public FileChannel getChannel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(int b) throws IOException {
        CloudResult result = AppletVM.callJScriptFunc(WRITE_CLOUD_FILE_OUTPUT_STREAM, id.toString(), new Integer(b).toString());
        if (result.isError()) {
            Throwable throwing = result.getException();
            if (throwing instanceof IOException) throw (IOException) throwing; else throw new RuntimeException(throwing);
        }
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        byte[] corrected = new byte[len];
        System.arraycopy(b, off, corrected, 0, len);
        String toWrite = BASE64.encodeContent(corrected);
        CloudResult result = AppletVM.callJScriptFunc(WRITE_CHUNK_JS_FUNC, id, toWrite, len);
    }

    @Override
    public void close() throws IOException {
        CloudResult result = AppletVM.callJScriptFunc(CLOSE_CLOUD_FILE_OUTPUT_STREAM, id.toString());
        if (result.isError()) {
            Throwable throwing = result.getException();
            if (throwing instanceof IOException) throw (IOException) throwing; else throw new RuntimeException(throwing);
        }
    }

    private static final String FINALIZE_FILE_OUTPUT_STREAM = "finalizeCloudOutputStream";

    private static final String FINALIZE_EVENT = "onFinalizeFileOutputStream";

    private static final String[] FINALIZE_PARAM = new String[] { "id" };

    private static String FINALIZE_FUNC = JSFactory.generateJSFunction(FINALIZE_FILE_OUTPUT_STREAM, FINALIZE_EVENT, FINALIZE_PARAM);

    @Override
    public void finalize() {
        try {
            AppletVM.callJScriptFunc(FINALIZE_FILE_OUTPUT_STREAM, id.toString());
        } catch (Exception e) {
        }
    }

    private static String CREATE_EVENT = "onRegisterCloudFileOutputStream";

    private static String WRITE_EVENT = "onWriteCloudFileOutputStream";

    private static String CLOSE_EVENT = "onCloseCloudFileOutputStream";

    private static final String[] CREATE_PARAM = new String[] { "file" };

    private static String CREATE_FUNC = JSFactory.generateJSFunction(REGISTER_CLOUD_FILE_OUTPUT_STREAM, CREATE_EVENT, CREATE_PARAM);

    private static final String[] WRITE_PARAM = new String[] { "id", "write" };

    private static String WRITE_FUNC = JSFactory.generateJSFunction(WRITE_CLOUD_FILE_OUTPUT_STREAM, WRITE_EVENT, WRITE_PARAM);

    private static final String[] CLOSE_PARAM = new String[] { "id" };

    private static String CLOSE_FUNC = JSFactory.generateJSFunction(CLOSE_CLOUD_FILE_OUTPUT_STREAM, CLOSE_EVENT, CLOSE_PARAM);

    private static final String WRITE_CHUNK_JS_FUNC = "writeChunkCloudFileInputStream";

    private static final String WRITE_CHUNK_EVENT = "onWriteChunkCloudFileInputStream";

    private static final String[] WRITE_CHUNK_PARAM = new String[] { "id", "write", "len" };

    private static String WRITE_CHUNK_FUNC = JSFactory.generateJSFunction(WRITE_CHUNK_JS_FUNC, WRITE_CHUNK_EVENT, WRITE_CHUNK_PARAM);

    public static List<String> getJavaScript() {
        List<String> toInstall = new ArrayList<String>();
        toInstall.add(WRITE_FUNC);
        toInstall.add(CREATE_FUNC);
        toInstall.add(CLOSE_FUNC);
        toInstall.add(WRITE_CHUNK_FUNC);
        toInstall.add(FINALIZE_FUNC);
        return toInstall;
    }

    public static Map<String, CommandHandler> getCommandHandlers() {
        Map<String, CommandHandler> toInstall = new HashMap<String, CommandHandler>();
        toInstall.put(CREATE_EVENT, new CreateHandler());
        toInstall.put(WRITE_EVENT, new WriteHandler());
        toInstall.put(CLOSE_EVENT, new CloseHandler());
        toInstall.put(WRITE_CHUNK_EVENT, new WriteChunkHandler());
        toInstall.put(FINALIZE_EVENT, new FinalizeHandler());
        return toInstall;
    }

    private static class WriteChunkHandler implements CommandHandler {

        public CloudToApplet handleRequest(Map<String, Object> params, Map<String, Object> objects) throws Exception {
            String localId = (String) params.get("id");
            String toWrite = (String) params.get("write");
            int len = Integer.parseInt((String) params.get("len"));
            FileOutputStream out = (FileOutputStream) objects.get(localId);
            byte[] toWriteBuf = new byte[len];
            BASE64.decodeContent(toWrite, toWriteBuf, 0, len);
            out.write(toWriteBuf, 0, len);
            return new CloudToApplet();
        }
    }

    private static class CreateHandler implements CommandHandler {

        public CloudToApplet handleRequest(Map<String, Object> params, Map<String, Object> objects) throws Exception {
            boolean append = false;
            String file = (String) params.get("file");
            String appendS = (String) params.get("append");
            if (appendS != null) {
                append = Boolean.parseBoolean(appendS);
            }
            UUID id = UUID.randomUUID();
            FileOutputStream out = new FileOutputStream(VM.currentVM().getLocalRoot() + File.separatorChar + file, append);
            objects.put(id.toString(), out);
            return new CloudToApplet(id.toString());
        }
    }

    private static class WriteHandler implements CommandHandler {

        public CloudToApplet handleRequest(Map<String, Object> params, Map<String, Object> objects) throws Exception {
            String id = (String) params.get("id");
            String toWrite = (String) params.get("write");
            int part = Integer.parseInt(toWrite);
            FileOutputStream out = (FileOutputStream) objects.get(id);
            out.write(part);
            return new CloudToApplet();
        }
    }

    private static class CloseHandler implements CommandHandler {

        public CloudToApplet handleRequest(Map<String, Object> params, Map<String, Object> objects) throws Exception {
            String id = (String) params.get("id");
            FileOutputStream out = (FileOutputStream) objects.get(id);
            out.close();
            return new CloudToApplet();
        }
    }

    private static class FinalizeHandler implements CommandHandler {

        public CloudToApplet handleRequest(Map<String, Object> params, Map<String, Object> objects) throws Exception {
            String localId = (String) params.get("id");
            FileOutputStream out = (FileOutputStream) objects.get(localId);
            out.close();
            params.remove(localId);
            return new CloudToApplet();
        }
    }
}
