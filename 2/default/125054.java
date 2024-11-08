import com.google.visualization.datasource.DataSourceHelper;
import com.google.visualization.datasource.DataSourceServlet;
import com.google.visualization.datasource.base.DataSourceException;
import com.google.visualization.datasource.base.ReasonType;
import com.google.visualization.datasource.datatable.DataTable;
import com.google.visualization.datasource.query.Query;
import com.google.visualization.datasource.util.CsvDataSourceHelper;
import com.ibm.icu.util.ULocale;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;

/**
 * A demo servlet for serving a simple, constant data table.
 * This servlet extends DataSourceServlet, but does not override the default
 * empty implementation of method getCapabilities(). This servlet therefore ignores the
 * user query (as passed in the 'tq' url parameter), leaving the
 * query engine to apply it to the data table created here.
 *
 * @author Nimrod T.
 */
public class CsvDataSourceServlet extends DataSourceServlet {

    /**
   * Log.
   */
    private static final Log log = LogFactory.getLog(CsvDataSourceServlet.class.getName());

    /**
   * The name of the parameter that contains the url of the CSV to load.
   */
    private static final String URL_PARAM_NAME = "url";

    /**
   * Generates the data table.
   * This servlet assumes a special parameter that contains the CSV URL from which to load
   * the data.
   */
    @Override
    public DataTable generateDataTable(Query query, HttpServletRequest request) throws DataSourceException {
        String url = request.getParameter(URL_PARAM_NAME);
        if (StringUtils.isEmpty(url)) {
            log.error("url parameter not provided.");
            throw new DataSourceException(ReasonType.INVALID_REQUEST, "url parameter not provided");
        }
        Reader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
        } catch (MalformedURLException e) {
            log.error("url is malformed: " + url);
            throw new DataSourceException(ReasonType.INVALID_REQUEST, "url is malformed: " + url);
        } catch (IOException e) {
            log.error("Couldn't read from url: " + url, e);
            throw new DataSourceException(ReasonType.INVALID_REQUEST, "Couldn't read from url: " + url);
        }
        DataTable dataTable = null;
        ULocale requestLocale = DataSourceHelper.getLocaleFromRequest(request);
        try {
            dataTable = CsvDataSourceHelper.read(reader, null, true, requestLocale);
        } catch (IOException e) {
            log.error("Couldn't read from url: " + url, e);
            throw new DataSourceException(ReasonType.INVALID_REQUEST, "Couldn't read from url: " + url);
        }
        return dataTable;
    }

    /**
   * NOTE: By default, this function returns true, which means that cross
   * domain requests are rejected.
   * This check is disabled here so examples can be used directly from the
   * address bar of the browser. Bear in mind that this exposes your
   * data source to xsrf attacks.
   * If the only use of the data source url is from your application,
   * that runs on the same domain, it is better to remain in restricted mode.
   */
    @Override
    protected boolean isRestrictedAccessMode() {
        return false;
    }
}
