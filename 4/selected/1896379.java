package ge.lowlevel.js;

import ge.engine.Log;
import ge.exceptions.FunctionNonSuported;
import ge.lowlevel.Interval;
import ge.lowlevel.UtilityEngine;
import ge.modules.utils.FileModule;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.user.client.Timer;

public class JSUtilityEngine extends UtilityEngine {

    public JSUtilityEngine() {
    }

    @Override
    public void setInterval(int time, final Interval object, boolean loop) throws FunctionNonSuported {
        Timer updateTimer = new Timer() {

            @Override
            public void run() {
                object.run();
            }
        };
        object.setConcretTimer(updateTimer);
        if (loop) updateTimer.scheduleRepeating(time); else updateTimer.schedule(time);
    }

    @Override
    public void cancelInterval(Interval object) throws FunctionNonSuported {
        Timer t = (Timer) object.getConcretTimer();
        t.cancel();
    }

    @Override
    public String readFile(final FileModule file) {
        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, file.getPath());
        try {
            requestBuilder.sendRequest(null, new RequestCallback() {

                public void onError(Request request, Throwable exception) {
                    Log.write("failed file reading" + exception);
                }

                public void onResponseReceived(Request request, Response response) {
                    String result = response.getText();
                    file.setContent(result);
                }
            });
        } catch (RequestException e) {
            Log.write("failed file reading" + e.getMessage());
        }
        return "";
    }
}
