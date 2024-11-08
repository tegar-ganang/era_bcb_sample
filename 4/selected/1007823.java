package net.sf.hsdd.telnet;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.StringTokenizer;
import net.sf.hsdd.conf.HostAlreadyExist;
import net.sf.hsdd.conf.HostManager;
import net.sf.hsdd.conf.HostProperties;
import net.sf.hsdd.conf.SchemeNotSupported;
import net.sf.hsdd.conf.usr.AuthInfo;
import net.sf.hsdd.util.Validator;
import net.wimpi.telnetd.io.BasicTerminalIO;
import net.wimpi.telnetd.io.terminal.ColorHelper;
import net.wimpi.telnetd.net.Connection;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

/**
 *
 * @author Alexey.Kurgan
 */
public class CreateHostCommand extends AbstractTelnetCommand {

    public CreateHostCommand() {
        super("createhost", "Create new host configuration (only for user admin)", "createhost <host uri>\n  Create new host configuration (only for user admin)\n" + "  <host uri> - must be in URI format. Example: http://www.lambda.com\n");
    }

    public void run(String line, BasicTerminalIO tio, Connection connection, AuthInfo authInfo) throws IOException, ShutdownException, StopException {
        StringTokenizer tokens = new StringTokenizer(line);
        if (!authInfo.getUser().getUserName().equals("admin")) {
            tio.write(ColorHelper.colorizeText("This command granted only for user admin\n", ColorHelper.RED));
        } else if (tokens.countTokens() < 2) {
            tio.write("Host URI must be specified\n");
        } else {
            tokens.nextToken();
            String hostURI = trimAndRemoveQuotes(tokens.nextToken());
            if (!Validator.isEmpty(hostURI)) {
                try {
                    URI uri = new URI(hostURI, true);
                    HostProperties p = HostManager.addHost(uri.getScheme(), uri.getHost(), uri.getPort(), false);
                    tio.write(MessageFormat.format("Host configuration created with id {0,number,#######}\n", p.getId()));
                } catch (URIException ex) {
                    tio.write("Specified host URI is wrong\n");
                } catch (HostAlreadyExist ex) {
                    tio.write("Specified host already exist\n");
                } catch (SchemeNotSupported ex) {
                    tio.write("Specified protocol not supported\n");
                }
            } else {
                tio.write("Host URI must be specified\n");
            }
        }
        tio.flush();
    }
}
