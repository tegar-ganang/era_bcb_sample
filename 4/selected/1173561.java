package net.myfigurecollection.android.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;
import net.myfigurecollection.android.data.objects.Category;
import net.myfigurecollection.android.data.objects.Figure;
import net.myfigurecollection.android.webservices.RequestListener;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.Filter;
import org.jdom.input.SAXBuilder;
import Utils.Constants;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

/**
 * An asyncTask to parse MyFigurCollection API XML
 * 
 * @author Climbatize
 * 
 */
public class XMLHandler extends AsyncTask<String, Integer, Void> {

    RequestListener listener;

    private static boolean isRequesting = false;

    Document document;

    Element root;

    private Context context;

    ProgressDialog pd;

    public String num_res = "0";

    public static final String CAT_OWNED = "owned";

    public static final String CAT_WISHED = "wished";

    public static final String CAT_ORDERED = "ordered";

    private long Insertdate = 0;

    public static final String folder = Environment.getExternalStorageDirectory().getPath() + "/Android/data/net.myfigurecollection/Datas";

    public Element getRoot() {
        return root;
    }

    public void setRoot(Element root) {
        this.root = root;
    }

    URL flux;

    private boolean refresh = false;

    public XMLHandler(Activity c, boolean forceRefresh) {
        context = c;
        refresh = forceRefresh;
        if (c instanceof RequestListener) listener = (RequestListener) c;
    }

    public void setListener(RequestListener listener) {
        this.listener = listener;
    }

    /**
	 * 
	 * @param flux
	 * @param status
	 * @param file
	 */
    public void loadXML(URL flux, int status, File file) {
        try {
            SAXBuilder sbx = new SAXBuilder();
            try {
                if (file.exists()) {
                    file.delete();
                }
                if (!file.exists()) {
                    URLConnection conn = flux.openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(10000);
                    InputStream is = conn.getInputStream();
                    OutputStream out = new FileOutputStream(file);
                    byte buf[] = new byte[1024];
                    int len;
                    while ((len = is.read(buf)) > 0) out.write(buf, 0, len);
                    out.close();
                    is.close();
                }
            } catch (Exception e) {
                Log.e(Constants.PROJECT_TAG, "Exeption retrieving XML", e);
            }
            try {
                document = sbx.build(new FileInputStream(file));
            } catch (Exception e) {
                Log.e(Constants.PROJECT_TAG, "xml error ", e);
            }
        } catch (Exception e) {
            Log.e(Constants.PROJECT_TAG, "TsukiQueryError", e);
        }
        if (document != null) {
            root = document.getRootElement();
            PopulateDatabase(root, status);
        }
    }

    /**
	 * 
	 * @param root2
	 * @param status
	 */
    private void PopulateDatabase(Element root2, int status) {
        Element elment = root2;
        if (status > -1) {
            elment = (Element) root2.getChild("collection").getChildren().get(1);
        }
        ContentValues[] bulkValues = new ContentValues[elment.getChildren("item").toArray().length];
        int i = 0;
        for (Object figure : elment.getChildren("item").toArray()) {
            ContentValues values = new ContentValues();
            final Element figure_data = ((Element) figure).getChild(Figure.DATA);
            final Element figure_category = ((Element) figure).getChild(Figure.CATEGORY);
            if (figure_category != null) values.put(Figure.CATEGORY, parseIntinString(figure_category.getChildText(Figure.ID)));
            if (figure_data != null) {
                if (figure_category != null) {
                    values.put(Category.COLOR, figure_category.getChildText(Category.COLOR));
                }
                values.put(Figure.ID, parseIntinString(figure_data.getChildText(Figure.ID)));
                values.put(Figure.JAN, figure_data.getChildText(Figure.JAN));
                values.put(Figure.ISBN, figure_data.getChildText(Figure.ISBN));
                values.put(Figure.NAME, figure_data.getChildText(Figure.NAME));
                values.put(Figure.PRICE, parseIntinString(figure_data.getChildText(Figure.PRICE)));
                if (figure_data.getChild("releaseDate") != null) values.put(Figure.DATE, parseIntinString(figure_data.getChild("releaseDate").getChildText("year")));
                String name = figure_data.getChildText(Figure.NAME);
                int manuStart = name.lastIndexOf("(", name.length() - 1);
                String manu = name.substring(manuStart + 1, name.length() - 1);
                values.put(Figure.MANUFACTURER, manu);
            }
            if (status != -1) {
                final Element figure_collection = ((Element) figure).getChild("mycollection");
                if (figure_collection != null) {
                    values.put(Figure.SCORE, parseIntinString(figure_collection.getChildText(Figure.SCORE)));
                    values.put(Figure.NUMBER_OWNED, parseIntinString(figure_collection.getChildText(Figure.NUMBER_OWNED)));
                    values.put(Figure.WISHABILITY, parseIntinString(figure_collection.getChildText(Figure.WISHABILITY)));
                }
            } else {
                values.put(Figure.SCORE, parseIntinString(root.getChildText("num_results")));
            }
            values.put(Figure.STATUS, status);
            values.put("InsertDate", Insertdate);
            bulkValues[i++] = values;
        }
        context.getContentResolver().delete(Figure.CONTENT_URI, "InsertDate" + "<" + Insertdate + " AND " + Figure.STATUS + "=" + status, null);
        context.getContentResolver().bulkInsert(Figure.CONTENT_URI, bulkValues);
    }

    /**
	 * 
	 * @param s
	 * @return
	 */
    int parseIntinString(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
	 * 
	 * @param id
	 * @return
	 */
    public Filter getFigureId(final String id) {
        return new Filter() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean matches(Object ob) {
                if (!(ob instanceof Element)) {
                    return false;
                }
                Element element = ((Element) ob).getChild(Figure.DATA);
                if (element != null) {
                    return (element.getChild(Figure.ID).getText().equals(id));
                }
                return false;
            }
        };
    }

    @Override
    protected Void doInBackground(String... params) {
        if (!isRequesting) {
            isRequesting = true;
            try {
                String f;
                String nFolder = "/data/data/net.myfigurecollection/";
                File dir = (new File(folder));
                dir.mkdirs();
                if (dir.isDirectory()) {
                    nFolder = folder + "/";
                }
                f = (nFolder + "retrieved.mfc");
                File file = new File(f);
                if (params.length > 1 && "refresh".equals(params[1])) file.delete();
                if (!refresh) refresh = (!file.exists() || (Calendar.getInstance().getTimeInMillis() - file.lastModified()) > 1000 * 60 * 60 * 4);
                if (params[0].contains(Constants.API_MODE_SEARCH)) {
                    URL url;
                    url = new URL(params[0]);
                    loadXML(url, -1, file);
                    publishProgress(100, -1);
                } else if (refresh) for (int i = 0; i < 3; i++) {
                    URL url;
                    url = new URL(params[0] + "&status=" + i);
                    loadXML(url, i, file);
                    if (file.exists()) {
                        int num_pages = Integer.parseInt(((Element) root.getChild("collection").getChildren().get(1)).getChildText("num_pages"));
                        for (int j = 2; j <= num_pages; j++) {
                            loadXML(new URL(url + "&page=" + j), i, file);
                            publishProgress((int) ((j / (float) num_pages) * 100), i);
                        }
                    }
                    publishProgress(100, i);
                }
            } catch (MalformedURLException e) {
                Log.e(Constants.PROJECT_TAG, "MalformedURLException in doInBackground", e);
            }
            isRequesting = false;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void voi) {
        {
            try {
                if (pd != null && pd.isShowing()) {
                    pd.dismiss();
                }
            } catch (IllegalArgumentException e) {
                Log.e(Constants.PROJECT_TAG, "IllegalArgumentException", e);
            }
            if (listener != null) listener.onRequestcompleted(0, voi);
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        switch(values[1]) {
            case 0:
                pd.setTitle(CAT_WISHED);
                break;
            case 1:
                pd.setTitle(CAT_ORDERED);
                break;
            case 2:
                pd.setTitle(CAT_OWNED);
                break;
            default:
                break;
        }
        pd.setProgress(values[0]);
    }

    ;

    @Override
    protected void onPreExecute() {
        if (!isRequesting) {
            Insertdate = (new Date()).getTime();
            pd = new ProgressDialog(context);
            pd.setCancelable(false);
            pd.setMessage("Downloading datas");
            pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pd.show();
        }
        super.onPreExecute();
    }
}
