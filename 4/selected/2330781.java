package com.mac.chriswjohnson.mazewars.bot;

import edu.utexas.its.eis.tools.qwicap.template.xml.immutable.XMLDocument;
import edu.utexas.its.eis.tools.qwicap.template.xml.immutable.ImmutableMarkup;
import edu.utexas.its.eis.tools.qwicap.servlet.QwicapException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.mac.chriswjohnson.cookie.CookieList;

/**
 * Created by IntelliJ IDEA.
 * User: chrisj
 * Date: Jan 1, 2010
 * Time: 9:47:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class Communicator {

    private static final Logger Log = Logger.getLogger(Communicator.class.getName());

    private static final int kMessageLengthGuess = 4 * 1024;

    private static final int kReadBuffLength = 4 * 1024;

    private final CookieList Cookies;

    private final String GameURLStr;

    private final Plan PlanImpl;

    private final StringBuilder CmdBuff = new StringBuilder(128);

    private final ByteArrayOutputStreamHE BOut = new ByteArrayOutputStreamHE(kMessageLengthGuess);

    private final byte[] ReadBuff = new byte[kReadBuffLength];

    public Communicator(final CookieList Cookies, final URL GameURL, final Plan PlanImpl) {
        this.Cookies = Cookies;
        this.GameURLStr = GameURL.toString();
        this.PlanImpl = PlanImpl;
    }

    public BotState sendCommand(final Object... NamesAndValues) throws IOException, QwicapException {
        return sendCommand(assembleCommand(NamesAndValues));
    }

    public BotState sendCommand(final String CommandStr) throws IOException, QwicapException {
        final BotState NewState = new BotState(makeRequest(CommandStr));
        PlanImpl.acceptNewState(NewState);
        return NewState;
    }

    private String assembleCommand(final Object... NamesAndValues) throws IOException {
        CmdBuff.setLength(0);
        for (int Index = 0, Count = NamesAndValues.length; Index < Count; Index++) {
            final String ElemStr = NamesAndValues[Index].toString();
            if (Index % 2 == 0) CmdBuff.append(Index == 0 ? '?' : '&'); else CmdBuff.append('=');
            CmdBuff.append(URLEncoder.encode(ElemStr, "UTF-8"));
        }
        return CmdBuff.toString();
    }

    private ImmutableMarkup makeRequest(final String URIArguments) throws QwicapException, IOException {
        final URL ReqURL = new URL(GameURLStr + URIArguments);
        final HttpURLConnection Conn = (HttpURLConnection) ReqURL.openConnection();
        Cookies.writeTo(Conn);
        Conn.setDefaultUseCaches(false);
        Conn.setUseCaches(false);
        Conn.setRequestProperty("Connection", "close");
        Conn.setRequestMethod("GET");
        Conn.setDoOutput(false);
        Conn.setDoInput(true);
        Conn.setConnectTimeout(0);
        Conn.setReadTimeout(0);
        Conn.connect();
        final InputStream ConnIn = Conn.getInputStream();
        try {
            Cookies.readFrom(Conn);
            BOut.reset();
            for (int BytesRead = ConnIn.read(ReadBuff); BytesRead >= 0; BytesRead = ConnIn.read(ReadBuff)) BOut.write(ReadBuff, 0, BytesRead);
        } finally {
            try {
                ConnIn.close();
            } catch (Exception e) {
                Log.log(Level.WARNING, "ConnIn.close() failed.", e);
            }
            try {
                Conn.disconnect();
            } catch (Exception e) {
                Log.log(Level.WARNING, "Conn.disconnect() failed.", e);
            }
        }
        return new XMLDocument(BOut.getInputStream(), BOut.size());
    }
}
