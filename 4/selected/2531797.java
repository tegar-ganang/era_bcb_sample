package net.sourceforge.pharos.web;

import net.sourceforge.pharos.config.ApplicationConfig;
import net.sourceforge.pharos.constants.Constants;
import net.sourceforge.pharos.exception.ApplicationSecurityException;
import net.sourceforge.pharos.utils.FileUtils;
import net.sourceforge.pharos.utils.ParseUtils;
import net.sourceforge.pharos.utils.ReflectionUtils;
import net.sourceforge.pharos.utils.ResonseType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;

/**
 * @author anuradha
 *
 */
class StaticDataHelper {

    private final HttpServletRequest request;

    private final HttpServletResponse response;

    private final File file;

    /**
	 * Constructor for StaticDataHelper. 
	 * @param request
	 * @param response
	 * @param file
	 */
    StaticDataHelper(final HttpServletRequest request, final HttpServletResponse response, final File file) {
        super();
        this.request = request;
        this.response = response;
        this.file = file;
    }

    /**
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * 
	 */
    void writeResponse() throws FileNotFoundException, IOException {
        if (this.file.canRead()) {
            if (FileUtils.isTextContent(this.file.getName())) {
                writeTextData();
            } else {
                writeBinaryData();
            }
        } else {
            throw new ApplicationSecurityException("Access to this file is denied.");
        }
    }

    /**
	 * @param request
	 * @param response
	 * @param file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    private void writeTextData() throws FileNotFoundException, IOException {
        if (FileUtils.isHTML(this.file.getName())) {
            writePreparedHTML();
        } else if (FileUtils.isJavascript(this.file.getName())) {
            writePreparedJS();
        } else {
            writeBinaryData();
        }
    }

    /**
	 * @param request
	 * @param response
	 * @param file
	 * @throws IOException 
	 */
    private void writePreparedJS() throws IOException {
        final BufferedReader bufferedReader = new BufferedReader(new FileReader(this.file));
        String eachLine = bufferedReader.readLine();
        while (null != eachLine) {
            final Collection<String> tokensToReplace = ParseUtils.getTokensToReplace(eachLine);
            for (final Iterator<String> tkItr = tokensToReplace.iterator(); tkItr.hasNext(); ) {
                final String expression = tkItr.next();
                final String messageToken = expression.replaceAll("[$]", "").replaceAll("[{]", "").replace("[}]", "");
                final UserData userData = (UserData) this.request.getAttribute(Constants.USER_DETAILS);
                final ResourceBundle messageBundle = ApplicationConfig.getMessageBundle(userData.getLocale());
                if (null != messageBundle) {
                    String value = messageBundle.getString(messageToken);
                    eachLine = eachLine.replace(expression, value);
                }
            }
            this.response.getWriter().println(eachLine);
            eachLine = bufferedReader.readLine();
        }
        this.request.setAttribute(Constants.ACTION_RESPONSE, new Response() {

            @Override
            public Boolean isForCompression() {
                return Boolean.TRUE;
            }

            @Override
            public ResonseType getResponseType() {
                return ResonseType.STATIC;
            }
        });
    }

    /**
	 * @param request
	 * @param response
	 * @param file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    private void writePreparedHTML() throws FileNotFoundException, IOException {
        final Action action = (Action) this.request.getAttribute(Constants.ACTION_EXECUTOR);
        final Response execute = action.execute();
        final BufferedReader bufferedReader = new BufferedReader(new FileReader(this.file));
        final ResourceBundle messageBundle = ApplicationConfig.getMessageBundle(action.getAuthenticatedUser().getLocale());
        String eachLine = bufferedReader.readLine();
        while (null != eachLine) {
            final Collection<String> tokensToReplace = ParseUtils.getTokensToReplace(eachLine);
            for (final Iterator<String> tkItr = tokensToReplace.iterator(); tkItr.hasNext(); ) {
                final String expression = tkItr.next();
                final String methodToken = expression.replaceAll("[$]", "").replaceAll("[{]", "").replace("[}]", "");
                String value = null;
                if (methodToken.toLowerCase().endsWith("label")) {
                    final String labelKey = methodToken.substring(0, methodToken.length() - "label".length());
                    value = messageBundle.getString(labelKey);
                } else {
                    final String methodName = "get" + Character.toUpperCase(methodToken.charAt(0)) + methodToken.substring(1);
                    final Method method = ReflectionUtils.tryToGetMethod(action.getClass(), methodName);
                    final Object result = (null == method) ? null : ReflectionUtils.invokeMethodAndEatExceptions(method, action);
                    if (null == result) {
                        final Object attribute = this.request.getAttribute(methodToken);
                        value = String.valueOf(attribute);
                    } else {
                        value = String.valueOf(result);
                    }
                }
                eachLine = eachLine.replace(expression, value);
            }
            this.response.getWriter().println(eachLine);
            eachLine = bufferedReader.readLine();
        }
        this.request.setAttribute(Constants.ACTION_RESPONSE, execute);
    }

    /**
	 * @param response
	 * @param file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    private void writeBinaryData() throws FileNotFoundException, IOException {
        final FileChannel channel = new FileInputStream(this.file).getChannel();
        final ByteBuffer allocate = ByteBuffer.allocate((int) channel.size());
        channel.read(allocate);
        this.response.getOutputStream().write(allocate.array());
    }
}
