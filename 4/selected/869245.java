package com.study.pepper.client.utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.Date;
import com.study.jslib.json.JSONArray;
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
            ex.printStackTrace();
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
        servlet = new URL("http://" + MainProperties.getProperty("server.host") + ":" + MainProperties.getProperty("server.port") + "/Pepper/client");
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

    private String createSelectRequest(RequestEnum operation, JSONObject params) {
        return createRequest(operation, params);
    }

    private String createUpdateRequest(RequestEnum operation, JSONObject params) {
        params.put("size", params.length());
        return createRequest(operation, params);
    }

    private String createRemoveRequest(RequestEnum operation, JSONArray params) {
        JSONObject json = new JSONObject();
        json.put("operation", operation.getMethodName());
        json.put("params", params);
        return json.toString();
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

    public String removeFilm(JSONArray json) {
        String request = createRemoveRequest(RequestEnum.FILMS_REMOVE, json);
        return executeRequest(request);
    }

    public String removeCinema(JSONArray json) {
        String request = createRemoveRequest(RequestEnum.CINEMAS_REMOVE, json);
        return executeRequest(request);
    }

    public String removeHall(JSONArray json) {
        String request = createRemoveRequest(RequestEnum.HALLS_REMOVE, json);
        return executeRequest(request);
    }

    public String getHallsList(int cinemaId) {
        String id = String.valueOf(cinemaId);
        String src = "{\"cinemaid\":" + id + "}";
        JSONObject param = new JSONObject(src);
        String request = createSelectRequest(RequestEnum.HALLS_LIST, param);
        return executeRequest(request);
    }

    public String removePlace(JSONArray json) {
        String request = createRemoveRequest(RequestEnum.PLACES_REMOVE, json);
        return executeRequest(request);
    }

    public String getPlaceList(int hallId) {
        String idh = String.valueOf(hallId);
        String src = "{\"hallid\":" + idh + "}";
        JSONObject param = new JSONObject(src);
        String request = createSelectRequest(RequestEnum.PLACES_LIST, param);
        return executeRequest(request);
    }

    public String updateHall(JSONObject json, int cinemaId) {
        json.put("cinemaId", cinemaId);
        String id = String.valueOf(cinemaId);
        String src = "{\"cinemaid\":" + id + "}";
        JSONObject param = new JSONObject(src);
        String request = createUpdateRequest(RequestEnum.HALLS_UPDATE, json);
        return executeRequest(request);
    }

    public String updateHall(JSONObject json, int cinemaId, int row, int col) {
        json.put("cinemaId", cinemaId);
        json.put("row", row);
        json.put("col", col);
        String request = createUpdateRequest(RequestEnum.HALLS_UPDATE, json);
        return executeRequest(request);
    }

    public String getMembersList() {
        String request = createSelectRequest(RequestEnum.MEMBERS_LIST);
        return executeRequest(request);
    }

    public String updateMember(JSONObject json) {
        String request = createUpdateRequest(RequestEnum.MEMBERS_UPDATE, json);
        return executeRequest(request);
    }

    public String removeMember(JSONArray json) {
        String request = createRemoveRequest(RequestEnum.MEMBERS_REMOVE, json);
        return executeRequest(request);
    }

    public String getEmployeesList() {
        String request = createSelectRequest(RequestEnum.EMPLOYEES_LIST);
        return executeRequest(request);
    }

    public String updateEmployee(JSONObject json) {
        String request = createUpdateRequest(RequestEnum.EMPLOYEES_UPDATE, json);
        return executeRequest(request);
    }

    public String removeEmployee(JSONArray json) {
        String request = createRemoveRequest(RequestEnum.EMPLOYEES_REMOVE, json);
        return executeRequest(request);
    }

    public String getSessionsList(String date) {
        JSONObject param = new JSONObject();
        if (!date.isEmpty()) {
            param.put("date", new Date());
        }
        String request = createSelectRequest(RequestEnum.SESSIONS_LIST, param);
        return executeRequest(request);
    }

    public String updateSession(JSONObject json) {
        String request = createUpdateRequest(RequestEnum.SESSIONS_UPDATE, json);
        return executeRequest(request);
    }

    public String removeSession(JSONArray json) {
        String request = createRemoveRequest(RequestEnum.SESSIONS_REMOVE, json);
        return executeRequest(request);
    }

    public String getNotFreePlaseList(JSONObject json) {
        String request = createSelectRequest(RequestEnum.NOTFREEPLACE_LIST, json);
        return executeRequest(request);
    }

    public String updateZakaz(JSONObject json) {
        String request = createUpdateRequest(RequestEnum.ZAKAZ_UPDATE, json);
        return executeRequest(request);
    }

    public String removeZakaz(JSONArray json) {
        String request = createRemoveRequest(RequestEnum.ZAKAZ_REMOVE, json);
        return executeRequest(request);
    }

    public String checkUser(String login, String md5) {
        JSONObject json = new JSONObject();
        json.put("login", login);
        json.put("password", md5);
        String request = createUpdateRequest(RequestEnum.PASSWORD_LIST, json);
        return executeRequest(request);
    }

    public String getZakazList() {
        String request = createSelectRequest(RequestEnum.ZAKAZ_LIST);
        return executeRequest(request);
    }
}
