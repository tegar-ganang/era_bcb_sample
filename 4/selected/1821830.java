package downloadmanager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Time;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author MX
 */
public class Download extends Observable implements Serializable, Runnable {

    private static final long serialVersionUID = 1L;

    private static final int BUFFER_SIZE = 1024;

    private Integer _id;

    private URL _source;

    private File _destination;

    private String _filename;

    private boolean _scheduled;

    private Date _date;

    private Integer _size;

    private Integer _done;

    private State _state;

    private transient Timer _timer;

    public enum State {

        BEGIN("Inicio"), STOPPED("Parado"), ACTIVE("Activo"), CONNECTING("Conectando"), FINISHED("Terminado"), ERROR("Erro na ligaçao");

        private final String _message;

        State(String message) {
            _message = message;
        }

        public String getMessage() {
            return _message;
        }
    }

    public Download(int id, String uri) throws MalformedURLException {
        setId(id);
        _source = new URL(uri);
        setFilename(sanitizeFilename(_source.getPath()));
        _state = State.STOPPED;
        _size = -1;
        _done = 0;
        _scheduled = false;
        _date = new Date();
        _timer = new Timer();
    }

    public Download(int id, String uri, String destination) throws MalformedURLException {
        this(id, uri);
        setDestination(new File(destination, getFilename()));
    }

    public void init() {
        _timer = new Timer();
        System.out.println("Init " + toString() + " :: Scheduled: " + isScheduled() + " to " + (_date.getTime() - System.currentTimeMillis()));
        if (isScheduled()) {
            long remaingMilliSecs = _date.getTime() - System.currentTimeMillis();
            if (remaingMilliSecs > 0) {
                _timer.schedule(new ScheduleTask(), remaingMilliSecs);
            } else {
                download();
            }
        }
    }

    public void setState(State s) {
        _state = s;
        updateState();
    }

    public String getState() {
        if (isScheduled()) {
            return "Programado " + _date.getHours() + ":" + _date.getMinutes();
        } else {
            return _state.getMessage();
        }
    }

    public State getTheState() {
        return _state;
    }

    public void updateState() {
        setChanged();
        notifyObservers();
    }

    public void error(String s) {
        setScheduled(false);
        setState(State.ERROR);
    }

    public void stop() {
        setScheduled(false);
        setState(State.STOPPED);
    }

    public void download() {
        setScheduled(false);
        setState(State.CONNECTING);
        Thread t = new Thread(this);
        t.start();
    }

    public void run() {
        RandomAccessFile file = null;
        InputStream stream = null;
        try {
            HttpURLConnection connection = (HttpURLConnection) _source.openConnection();
            connection.setRequestProperty("Range", "bytes=" + getDone() + "-");
            connection.connect();
            if (connection.getResponseCode() < 200 || connection.getResponseCode() > 299) {
                error("Response Code");
                return;
            }
            int contentLength = connection.getContentLength();
            if (contentLength < 1) {
                error("Content Length");
                return;
            }
            if (getSize() == -1) {
                setSize(contentLength);
            }
            file = new RandomAccessFile(_destination, "rw");
            file.seek(_done);
            stream = connection.getInputStream();
            setState(State.ACTIVE);
            while (_state == State.ACTIVE) {
                byte buffer[];
                if (_size - _done > BUFFER_SIZE) {
                    buffer = new byte[BUFFER_SIZE];
                } else {
                    buffer = new byte[_size - _done];
                }
                int read = stream.read(buffer);
                if (read == -1) {
                    break;
                }
                file.write(buffer, 0, read);
                addDone(read);
            }
            if (_state == State.ACTIVE) {
                setState(State.FINISHED);
            }
        } catch (IOException e) {
            error(e.toString());
        } finally {
            if (file != null) {
                try {
                    file.close();
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void setId(Integer id) {
        _id = id;
    }

    public Integer getId() {
        return _id;
    }

    public URL getSource() {
        return _source;
    }

    public void setFilename(String fn) {
        _filename = fn;
        updateState();
    }

    public String getFilename() {
        return _filename;
    }

    public void setDestination(File f) {
        _destination = f;
        updateState();
    }

    public File getDestination() {
        return _destination;
    }

    public void setScheduled(boolean b) {
        _scheduled = b;
        updateState();
    }

    public boolean isScheduled() {
        return _scheduled;
    }

    public void setScheduleTime(int hours, int minutes) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.set(gc.get(gc.YEAR), gc.get(gc.MONTH), gc.get(gc.DAY_OF_MONTH), hours, minutes, 0);
        _date.setTime(gc.getTimeInMillis());
        long remainingMilSecs = gc.getTimeInMillis() - System.currentTimeMillis();
        if (remainingMilSecs > 0) {
            setState(State.STOPPED);
            setScheduled(true);
            _timer.schedule(new ScheduleTask(), remainingMilSecs);
        }
        updateState();
    }

    public Time getScheduleTime() {
        return (Time) _date;
    }

    public void setSize(int s) {
        _size = s;
        updateState();
    }

    public int getSize() {
        return _size;
    }

    public void addDone(int d) {
        _done += d;
        updateState();
    }

    public void resetDone() {
        _done = 0;
        updateState();
    }

    public int getDone() {
        return _done;
    }

    public float getProgress() {
        return (((float) getDone() / (float) getSize()) * 100);
    }

    private String sanitizeFilename(String fn) {
        String[] pieces = fn.split("/");
        return pieces[pieces.length - 1];
    }

    public static String formattedBytes(Integer bytes) {
        Double d = bytes * 1.0;
        String s = "B";
        if (bytes > 1073741824) {
            d = bytes / 1073741824.0;
            s = "GB";
        } else if (bytes > 1048576) {
            d = bytes / 1048576.0;
            s = "MB";
        } else if (bytes > 1024) {
            d = bytes / 1024.0;
            s = "KB";
        }
        DecimalFormat formatter = new DecimalFormat("####.00");
        formatter.setDecimalSeparatorAlwaysShown(true);
        return formatter.format(d) + " " + s;
    }

    class ScheduleTask extends TimerTask {

        @Override
        public void run() {
            download();
            setScheduled(false);
        }
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (_id != null ? _id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Download)) {
            return false;
        }
        Download other = (Download) object;
        if ((_id == null && other._id != null) || (_id != null && !_id.equals(other._id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "DMFile " + getId() + " | " + getFilename();
    }
}
