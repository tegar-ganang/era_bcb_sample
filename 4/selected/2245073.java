package com.appspot.battlerafts.utils;

import com.appspot.battlerafts.jsons.ResponseJSON;
import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.gson.Gson;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by IntelliJ IDEA.
 * User: Tonis
 * Date: 28.03.12
 * Time: 19:06
 * To change this template use File | Settings | File Templates.
 */
public class Util {

    public static String getToken(Long uid) {
        ChannelService channelService = ChannelServiceFactory.getChannelService();
        return channelService.createChannel(String.valueOf(uid));
    }

    public static void returnError(HttpServletResponse response, String reason) throws IOException {
        ResponseJSON responseJSON = new ResponseJSON();
        responseJSON.status = "FAILURE";
        responseJSON.reason = reason;
        Gson gson = new Gson();
        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();
        writer.write(gson.toJson(responseJSON));
        return;
    }

    public static void returnSuccess(HttpServletResponse response) throws IOException {
        ResponseJSON errorResponse = new ResponseJSON();
        errorResponse.status = "SUCCESS";
        Gson gson = new Gson();
        response.setContentType("application/json");
        PrintWriter writer = response.getWriter();
        writer.write(gson.toJson(errorResponse));
        return;
    }

    public static void notifYPlayers(Long uid, String rawInput) {
        ChannelService channelService = ChannelServiceFactory.getChannelService();
        channelService.sendMessage(new ChannelMessage(String.valueOf(uid), rawInput));
    }

    private static final char[] hexChar = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    public static String unicodeEscape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >> 7) > 0) {
                sb.append("\\u");
                sb.append(hexChar[(c >> 12) & 0xF]);
                sb.append(hexChar[(c >> 8) & 0xF]);
                sb.append(hexChar[(c >> 4) & 0xF]);
                sb.append(hexChar[c & 0xF]);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
