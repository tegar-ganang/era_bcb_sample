package fr.inria.zvtm.cluster;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import jsky.science.Coordinates;

/**
 * Performs queries on the Simbad catalog.
 */
class CatQuery {

    public static void main(String[] args) throws Exception {
        List<AstroObject> objs = makeSimbadCoordQuery(1, 4, 12);
        for (AstroObject obj : objs) {
            System.err.println(obj);
        }
    }

    static List<AstroObject> makeSimbadCoordQuery(double ra, double dec, int radmin) throws IOException {
        List<AstroObject> retval = new ArrayList<AstroObject>();
        URL queryUrl = makeSimbadCoordQueryUrl(ra, dec, radmin);
        return parseObjectList(readLines(queryUrl));
    }

    private static URL makeSimbadCoordQueryUrl(double ra, double dec, int radMin) {
        try {
            Coordinates coords = new Coordinates(ra, dec);
            String script = String.format("output console=off script=off\n" + "format object \"%%IDLIST(1)|%%COO(d;A)|%%COO(d;D)\"\n" + "query coo %s %s radius=%dm", coords.raToString().replace(',', '.'), coords.decToString().replace(',', '.'), radMin);
            return makeSimbadScriptQueryUrl(script);
        } catch (MalformedURLException ex) {
            throw new Error(ex);
        }
    }

    private static URL makeSimbadScriptQueryUrl(String script) throws MalformedURLException {
        String prefix = "http://simbak.cfa.harvard.edu/simbad/sim-script?script=";
        try {
            return new URL(prefix + URLEncoder.encode(script, "UTF-8"));
        } catch (UnsupportedEncodingException eex) {
            throw new Error(eex);
        }
    }

    private static List<String> readLines(URL url) throws IOException {
        URLConnection uc = url.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
        List<String> result = new ArrayList<String>();
        String toAppend;
        while ((toAppend = in.readLine()) != null) {
            result.add(toAppend);
        }
        in.close();
        return result;
    }

    private static List<AstroObject> parseObjectList(List<String> strList) {
        ArrayList<AstroObject> retval = new ArrayList<AstroObject>();
        for (String objStr : strList) {
            AstroObject candidate = AstroObject.fromSimbadRow(objStr);
            if (candidate != null) {
                retval.add(candidate);
            }
        }
        return retval;
    }
}
