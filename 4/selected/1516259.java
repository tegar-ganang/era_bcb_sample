package Rich_client.com.study.pepper.client.utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import com.study.jslib.json.JSONObject;

public class ServerManager {

    private static final ServerManager INSTANCE = new ServerManager();

    private ServerManager() {
    }

    public static synchronized ServerManager getInstance() {
        return INSTANCE;
    }

    private String executeRequest(String json) {
        try {
            String result = post(json);
            return result;
        } catch (Exception ex) {
            System.err.println("Ошибка!!");
            return null;
        }
    }

    private String post(String json) throws Exception {
        byte[] bRequest;
        byte[] buffer;
        int len;
        URL servlet;
        StringBuffer headerbuf;
        Socket socket;
        BufferedOutputStream outBuffer;
        bRequest = json.getBytes();
        servlet = new URL("http://" + "localhost" + ":" + "8080" + "/Pepper/client");
        headerbuf = new StringBuffer();
        headerbuf.append("POST ").append(servlet.getFile()).append(" HTTP/1.0\r\n").append("Host: ").append(servlet.getHost()).append("\r\n").append("Content-Length: ").append(bRequest.length).append("\r\n\r\n");
        socket = new Socket(servlet.getHost(), servlet.getPort());
        outBuffer = new BufferedOutputStream(socket.getOutputStream());
        outBuffer.write(headerbuf.toString().getBytes());
        outBuffer.write(bRequest);
        outBuffer.flush();
        socket.getOutputStream().flush();
        OutputStream os = socket.getOutputStream();
        InputStream is = socket.getInputStream();
        ByteArrayOutputStream resultBuf = new ByteArrayOutputStream();
        buffer = new byte[1024];
        while ((len = socket.getInputStream().read(buffer)) > 0) resultBuf.write(buffer, 0, len);
        os.close();
        is.close();
        resultBuf.close();
        String temp = resultBuf.toString();
        int pos = temp.indexOf("\r\n\r\n");
        return temp.substring(pos + 4);
    }

    private String createSelectRequest(RequestEnum operation) {
        JSONObject params = new JSONObject();
        params = new JSONObject();
        params.put("size", 0);
        return createRequest(operation, params);
    }

    private String createUpdateRequest(RequestEnum operation, JSONObject params) {
        params.put("size", params.length());
        return createRequest(operation, params);
    }

    private String createRequest(RequestEnum operation, JSONObject params) {
        JSONObject json = new JSONObject();
        json.put("operation", operation.getMethodName());
        json.put("params", params);
        return json.toString();
    }

    public String getCinemasList() {
        String request = createSelectRequest(RequestEnum.CINEMAS_LIST);
        return executeRequest(request);
    }

    public String updateCinema(JSONObject json) {
        String request = createUpdateRequest(RequestEnum.CINEMAS_UPDATE, json);
        return executeRequest(request);
    }

    public String getFilmsList() {
        String request = createSelectRequest(RequestEnum.FILMS_LIST);
        return executeRequest(request);
    }

    public String updateFilm(JSONObject json) {
        String request = createUpdateRequest(RequestEnum.FILMS_UPDATE, json);
        return executeRequest(request);
    }

    public String getHallsList() {
        String request = createSelectRequest(RequestEnum.HALLS_LIST);
        return executeRequest(request);
    }

    public String updateHall(JSONObject json) {
        String request = createUpdateRequest(RequestEnum.HALLS_UPDATE, json);
        return executeRequest(request);
    }
}
