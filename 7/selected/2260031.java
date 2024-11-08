package prefwork.datasource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import prefwork.Attribute;
import prefwork.CommonUtils;

public class IMDbMemoryNoList extends IMDbMemory {

    @SuppressWarnings("unused")
    private static Logger log = Logger.getLogger(IMDbMemoryNoList.class);

    protected static int[] lengths = { 4, 5, 2, 3, 1, 2, 5, 3, 1, 1 };

    public IMDbMemoryNoList() {
        super();
    }

    public List<Object> getMovie(Integer flixMovieId) {
        List<Object> l = new ArrayList<Object>();
        Integer movieId = IMDBMapFromFlix.get(flixMovieId);
        for (int i = 0; i < IMDBMaps.size(); i++) {
            l.add(getIMBDAttribute(i, movieId));
        }
        return l;
    }

    public Attribute[] getAttributes() {
        if (allAttributes != null) return allAttributes;
        List<Attribute> attrList = CommonUtils.getList(attributes.length + IMDBMaps.size() + 4);
        for (int i = 0; i < attributes.length; i++) {
            attrList.add(attributes[i]);
        }
        for (int i = 0; i < IMDBMaps.size(); i++) {
            for (int j = 0; j < lengths[i]; j++) {
                Attribute attr = new Attribute(null, attrList.size(), files[i] + j);
                attr.setType(Attribute.NOMINAL);
                attrList.add(attr);
            }
        }
        if (getLaserDisc) {
            for (int i = 0; i < 29; i++) {
                Attribute attr = new Attribute(null, attrList.size(), LaserDisc.names[i]);
                attr.setType(Attribute.NOMINAL);
                attrList.add(attr);
            }
        }
        if (getPlot) {
            Attribute attr = new Attribute(null, attrList.size(), "plot.csv");
            attr.setType(Attribute.NOMINAL);
            attrList.add(attr);
        }
        allAttributes = new Attribute[attrList.size()];
        allAttributes = attrList.toArray(allAttributes);
        return allAttributes;
    }

    /**
	 * Overriden because of handling of IMDb attributes.
	 */
    public void setAttributes(Attribute[] attrs) {
        for (int i = 0; i < attrs.length; i++) {
            if (attrs[i].getName().contains(".csv")) {
                Attribute[] attrs2 = new Attribute[attrs.length - 1];
                for (int j = 0; j < i; j++) {
                    attrs2[j] = attrs[j];
                }
                for (int j = i; j < attrs2.length; j++) {
                    attrs2[j] = attrs[j + 1];
                }
                attrs = attrs2;
                i = -1;
            }
        }
        super.setAttributes(attrs);
        allAttributes = null;
    }

    /**
	 * Overriden method from SQLMultiSource
	 */
    public List<Object> getRecord() {
        if (!hasNextRecord()) return null;
        List<Object> l = new ArrayList<Object>();
        getAttributes();
        try {
            int flixId = Integer.parseInt(records.getObject(2).toString());
            int imdbId = IMDBMapFromFlix.get(flixId);
            int i = 0;
            for (; i < this.attributes.length; i++) {
                l.add(records.getObject(i + 1));
            }
            for (i = 0; i < IMDBMaps.size(); i++) {
                List<Integer> attrValue = getIMBDAttribute(i, IMDBMapFromFlix.get(flixId));
                for (int j = 0; j < lengths[i]; j++) {
                    if (attrValue == null || j >= attrValue.size()) l.add(null); else l.add(attrValue.get(j));
                }
            }
            if (getLaserDisc) {
                LaserDisc ld = IMDBLaserDiscs.get(imdbId);
                for (i = 0; i < 29; i++) {
                    if (ld == null) l.add(""); else l.add(ld.line[i]);
                }
            }
            if (getPlot) {
                l.add(IMDBPlotMovies.get(imdbId));
            }
            records.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return l;
    }
}
