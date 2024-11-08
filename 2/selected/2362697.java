package com.commonsware.android.EMusicDownloader;

import java.net.URL;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.TextView;

public class ReviewList extends ListActivity {

    public ReviewList thisActivity;

    private TextView txtinfo;

    private ImageView nextButton;

    private ImageView previousButton;

    public ListView reviewList;

    private String urlAddress;

    private String urlAddress_orig;

    private String title;

    private String[] titles;

    private String[] reviews;

    private String[] ratings;

    private String[] listtext;

    private String[] authors;

    private int nReviewsPerPage = 20;

    private int nReviewsOnPage;

    private int nTotalReviews;

    private int iPageNumber = 1;

    private int iFirstReviewOnPage = 1;

    private int statuscode = 200;

    private Boolean vLoaded = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.searchlist);
        Intent myIntent = getIntent();
        myIntent.getStringExtra("keyname");
        title = myIntent.getStringExtra("keytitle");
        urlAddress = myIntent.getStringExtra("keyurl");
        Log.d("EMD - ", "Reviews " + urlAddress);
        urlAddress_orig = urlAddress;
        nextButton = (ImageView) findViewById(R.id.nextbutton);
        previousButton = (ImageView) findViewById(R.id.previousbutton);
        thisActivity = this;
        txtinfo = (TextView) findViewById(R.id.txt);
        getInfoFromXML();
    }

    private void getInfoFromXML() {
        final ProgressDialog dialog = ProgressDialog.show(this, "", getString(R.string.loading), true, true);
        setProgressBarIndeterminateVisibility(true);
        Thread t3 = new Thread() {

            public void run() {
                waiting(200);
                txtinfo.post(new Runnable() {

                    public void run() {
                        txtinfo.setText("Searching");
                    }
                });
                try {
                    URL url = new URL(urlAddress);
                    SAXParserFactory spf = SAXParserFactory.newInstance();
                    SAXParser sp = spf.newSAXParser();
                    XMLReader xr = sp.getXMLReader();
                    XMLHandlerReviews myXMLHandler = new XMLHandlerReviews();
                    xr.setContentHandler(myXMLHandler);
                    xr.parse(new InputSource(url.openStream()));
                    if (statuscode != 200 && statuscode != 206) {
                        throw new Exception();
                    }
                    nReviewsOnPage = myXMLHandler.nItems;
                    statuscode = myXMLHandler.statuscode;
                    if (nReviewsOnPage > 0) {
                        authors = new String[nReviewsOnPage];
                        reviews = new String[nReviewsOnPage];
                        ratings = new String[nReviewsOnPage];
                        titles = new String[nReviewsOnPage];
                        listtext = new String[nReviewsOnPage];
                        for (int i = 0; i < nReviewsOnPage; i++) {
                            reviews[i] = myXMLHandler.reviews[i];
                            authors[i] = myXMLHandler.authors[i];
                            titles[i] = myXMLHandler.titles[i];
                            ratings[i] = myXMLHandler.ratings[i];
                            if (authors[i] == null || authors[i] == "") {
                                authors[i] = "Anonymous";
                            }
                            if (ratings[i] == null || ratings[i] == "") {
                                listtext[i] = titles[i] + " - " + reviews[i] + " - " + authors[i];
                            } else {
                                listtext[i] = "Score: " + ratings[i] + " - " + titles[i] + " - " + reviews[i] + " - " + authors[i];
                            }
                        }
                        nTotalReviews = myXMLHandler.nTotalItems;
                        final int fnmin = iFirstReviewOnPage;
                        final int fnmax = iFirstReviewOnPage + nReviewsOnPage - 1;
                        final int fntotalitems = nTotalReviews;
                        if (nTotalReviews > fnmax) {
                            nextButton.post(new Runnable() {

                                public void run() {
                                    nextButton.setVisibility(0);
                                }
                            });
                        } else {
                            nextButton.post(new Runnable() {

                                public void run() {
                                    nextButton.setVisibility(8);
                                }
                            });
                        }
                        if (iFirstReviewOnPage > 1) {
                            previousButton.post(new Runnable() {

                                public void run() {
                                    previousButton.setVisibility(0);
                                }
                            });
                        } else if (nTotalReviews > fnmax) {
                            previousButton.post(new Runnable() {

                                public void run() {
                                    previousButton.setVisibility(8);
                                }
                            });
                        } else {
                            previousButton.post(new Runnable() {

                                public void run() {
                                    previousButton.setVisibility(4);
                                }
                            });
                        }
                        txtinfo.post(new Runnable() {

                            public void run() {
                                if (title != null && title != "") {
                                    txtinfo.setText(title + "\n" + getString(R.string.showing) + " " + fnmin + " " + getString(R.string.through) + " " + fnmax + " " + getString(R.string.of) + " " + fntotalitems);
                                } else {
                                    txtinfo.setText(getString(R.string.showing) + " " + fnmin + " " + getString(R.string.through) + " " + fnmax + " " + getString(R.string.of) + " " + fntotalitems);
                                }
                            }
                        });
                        handlerSetList.sendEmptyMessage(0);
                    } else {
                        txtinfo.post(new Runnable() {

                            public void run() {
                                txtinfo.setText(title + getString(R.string.no_reviews_for_this_album));
                            }
                        });
                    }
                } catch (Exception e) {
                    final Exception ef = e;
                    txtinfo.post(new Runnable() {

                        public void run() {
                            txtinfo.setText(R.string.search_failed);
                        }
                    });
                }
                dialog.dismiss();
                handlerDoneLoading.sendEmptyMessage(0);
            }
        };
        t3.start();
    }

    public void onListItemClick(ListView parent, View v, int position, long id) {
    }

    private Handler handlerSetList = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            vLoaded = true;
            setListAdapter(new ArrayAdapter<String>(thisActivity, R.layout.reviewlist_item, R.id.text, listtext));
        }
    };

    private void waiting(int n) {
        long t0, t1;
        t0 = System.currentTimeMillis();
        do {
            t1 = System.currentTimeMillis();
        } while (t1 - t0 < n);
    }

    public void nextPressed(View button) {
        iFirstReviewOnPage = iFirstReviewOnPage + nReviewsPerPage;
        iPageNumber = iPageNumber + 1;
        urlAddress = urlAddress_orig + "&page=" + iPageNumber;
        getInfoFromXML();
    }

    public void previousPressed(View button) {
        iFirstReviewOnPage = iFirstReviewOnPage - nReviewsPerPage;
        iPageNumber = iPageNumber - 1;
        urlAddress = urlAddress_orig + "&page=" + iPageNumber;
        getInfoFromXML();
    }

    private Handler handlerDoneLoading = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            setProgressBarIndeterminateVisibility(false);
        }
    };

    public void logoPressed(View buttoncover) {
        String browseurl = "http://www.emusic.com/";
        Intent browseIntent = new Intent(this, WebWindowBrowse.class);
        browseIntent.putExtra("keyurl", browseurl);
        startActivity(browseIntent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.searchlist);
        nextButton = (ImageView) findViewById(R.id.nextbutton);
        previousButton = (ImageView) findViewById(R.id.previousbutton);
        txtinfo = (TextView) findViewById(R.id.txt);
        if (vLoaded) {
            final int fnmin = iFirstReviewOnPage;
            final int fnmax = iFirstReviewOnPage + nReviewsOnPage - 1;
            final int fntotalitems = nTotalReviews;
            if (nTotalReviews > fnmax) {
                nextButton.setVisibility(0);
            } else {
                nextButton.setVisibility(8);
            }
            if (iFirstReviewOnPage > 1) {
                previousButton.setVisibility(0);
            } else if (nTotalReviews > fnmax) {
                previousButton.setVisibility(8);
            } else {
                previousButton.setVisibility(4);
            }
            if (title != null && title != "") {
                txtinfo.setText(title + "\n" + getString(R.string.showing) + " " + fnmin + " " + getString(R.string.through) + " " + fnmax + " " + getString(R.string.of) + " " + fntotalitems);
            } else {
                txtinfo.setText(getString(R.string.showing) + " " + fnmin + " " + getString(R.string.through) + " " + fnmax + " " + getString(R.string.of) + " " + fntotalitems);
            }
            handlerSetList.sendEmptyMessage(0);
        }
    }
}
