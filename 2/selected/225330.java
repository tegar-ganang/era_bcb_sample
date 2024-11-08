package org.virbo.dods;

import dods.dap.BaseType;
import dods.dap.BaseTypeFactory;
import dods.dap.DAS;
import dods.dap.DASException;
import dods.dap.DefaultFactory;
import dods.dap.parser.DASParser;
import dods.dap.parser.ParseException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 *
 * @author jbf
 */
public class MyDASParser {

    /** Creates a new instance of MyDASParser */
    public MyDASParser() {
    }

    private DAS myDAS;

    public void parse(InputStream in) throws ParseException, DASException {
        DASParser p = new DASParser(in);
        myDAS = new DAS();
        p.Attributes(myDAS);
    }

    String[] getVariableNames() {
        Enumeration en = myDAS.getNames();
        ArrayList<String> result = new ArrayList<String>();
        while (en.hasMoreElements()) {
            result.add(((BaseType) en.nextElement()).getName());
        }
        return result.toArray(new String[result.size()]);
    }

    public DAS getDAS() {
        return myDAS;
    }

    public static void main(String[] args) throws Exception {
        URL url = new URL("http://www.papco.org:8080/opendap/onera_cdf/lanl_1990_95/LANL_1990_095_H0_SOPA_ESP_19980505_V01.cdf.das");
        MyDASParser parser = new MyDASParser();
        parser.parse(url.openStream());
    }
}
