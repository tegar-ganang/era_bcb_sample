package pulsarhunter.datatypes.presto;

import coordlib.Coordinate;
import coordlib.Dec;
import coordlib.RA;
import coordlib.Telescope;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel.MapMode;
import pulsarhunter.datatypes.TimeSeries;

/**
 *
 * @author mkeith
 */
public class PrestoTimeSeries extends TimeSeries {

    private int bufferSize;

    private File dataFile;

    private File headerFile;

    private FloatBuffer fb;

    private FileInputStream in;

    private long currentFilePos = Long.MAX_VALUE;

    private PrestoTimeSeries.Header header;

    private long fileLength;

    public PrestoTimeSeries(File dataFile, File headerFile, int bufferSize) {
        header = new PrestoTimeSeries.Header(headerFile);
        this.bufferSize = bufferSize;
        this.headerFile = headerFile;
        this.dataFile = dataFile;
        this.fileLength = this.dataFile.length();
    }

    public TimeSeries.Header getHeader() {
        return this.header;
    }

    public float[] getDataAsFloats() {
        return null;
    }

    public float getBin(long bin) {
        float f = -1.0f;
        try {
            if (in == null) {
                in = new FileInputStream(this.dataFile);
                ByteBuffer bb = in.getChannel().map(MapMode.READ_ONLY, bin * 4, this.bufferSize * 4);
                bb.order(ByteOrder.nativeOrder());
                fb = bb.asFloatBuffer();
                currentFilePos = bin;
            }
            long lim = 0L;
            if (bin >= currentFilePos + this.bufferSize) {
                if ((bin + this.bufferSize) * 4 > fileLength) {
                    lim = fileLength - (this.bufferSize * 4);
                } else {
                    lim = bin * 4;
                }
                ByteBuffer bb = in.getChannel().map(MapMode.READ_ONLY, lim, this.bufferSize * 4);
                bb.order(ByteOrder.nativeOrder());
                fb = bb.asFloatBuffer();
                currentFilePos = lim / 4;
            }
            if (bin > currentFilePos + fb.position()) {
                while (bin != currentFilePos + fb.position() - 1) {
                    fb.get();
                }
            }
            if (bin < currentFilePos + fb.position()) {
                in.close();
                in = new FileInputStream(this.dataFile);
                ByteBuffer bb = in.getChannel().map(MapMode.READ_ONLY, bin * 4, this.bufferSize * 4);
                bb.order(ByteOrder.nativeOrder());
                fb = bb.asFloatBuffer();
                currentFilePos = bin;
            }
            if (bin == currentFilePos + fb.position()) {
                f = fb.get();
            } else {
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return f;
    }

    private void readFloats(float[] readIn, long startbin) {
    }

    class Header extends TimeSeries.Header {

        String[] bands = new String[] { "Radio", "IR", "Optical", "UV", "X-ray", "Gamma" };

        String[] scopes = new String[] { "None (Artificial Data Set)", "Arecibo", "Parkes", "VLA", "MMT", "Las Campanas 2.5m", "Mt. Hopkins 48in", "Other" };

        double ra_s;

        double dec_s;

        double N;

        double dt;

        double fov;

        double mjd_f;

        double dm;

        double freq;

        double freqband;

        double chan_wid;

        double wavelen;

        double waveband;

        double energy;

        double energyband;

        double[] onoff = new double[40];

        int num_chan;

        int mjd_i;

        int ra_h;

        int ra_m;

        int dec_d;

        int dec_m;

        int bary;

        int numonoff;

        String notes;

        String name;

        String object;

        String instrument;

        String observer;

        String analyzer;

        String telescope;

        String band;

        String filt;

        boolean negdec;

        private Header(File infFile) {
            super();
            try {
                BufferedReader reader = new BufferedReader(new FileReader(infFile));
                String line = reader.readLine();
                this.name = line.split("=\\s")[1].trim();
                line = reader.readLine();
                this.telescope = line.split("=\\s")[1].trim();
                if (!name.equals(scopes[0])) {
                    line = reader.readLine();
                    this.instrument = line.split("=\\s")[1].trim();
                    line = reader.readLine();
                    this.object = line.split("=\\s")[1].trim();
                    line = reader.readLine();
                    String raString = line.split("=\\s")[1].trim();
                    String[] elems = raString.split("\\:");
                    this.ra_h = Integer.parseInt(elems[0].trim());
                    this.ra_m = Integer.parseInt(elems[1].trim());
                    this.ra_s = Double.parseDouble(elems[2].trim());
                    line = reader.readLine();
                    String deString = line.split("=\\s")[1].trim();
                    elems = deString.split("\\:");
                    negdec = elems[0].trim().charAt(0) == '-';
                    this.dec_d = Integer.parseInt(elems[0].trim());
                    this.dec_m = Integer.parseInt(elems[1].trim());
                    this.dec_s = Double.parseDouble(elems[2].trim());
                    line = reader.readLine();
                    this.observer = line.split("=\\s")[1].trim();
                    line = reader.readLine();
                    String mjdString = line.split("=\\s")[1].trim();
                    elems = mjdString.split("\\.");
                    this.mjd_i = Integer.parseInt(elems[0]);
                    this.mjd_f = Double.parseDouble("0." + elems[1]);
                    line = reader.readLine();
                    this.bary = Integer.parseInt(line.split("=\\s")[1].trim());
                } else {
                    this.object = "fake pulsar";
                }
                line = reader.readLine();
                this.N = Double.parseDouble(line.split("=\\s")[1]);
                line = reader.readLine();
                this.dt = Double.parseDouble(line.split("=\\s")[1]);
                line = reader.readLine();
                this.numonoff = Integer.parseInt(line.split("=\\s")[1].trim());
                if (this.numonoff > 0) {
                    int ii = 0;
                    do {
                        line = reader.readLine();
                        String datLine = line.split("=\\s")[1];
                        String[] elems = datLine.split(",");
                        this.onoff[ii] = Double.parseDouble(elems[0]);
                        this.onoff[ii + 1] = Double.parseDouble(elems[1]);
                        ii += 2;
                    } while ((this.onoff[ii - 1] < (this.N - 1)) && (ii < onoff.length));
                    this.numonoff = ii / 2;
                    if (this.numonoff > this.onoff.length) {
                        throw new IOException("There are two many OnOff values. Max: " + onoff.length);
                    }
                }
                if (!name.equals(scopes[0])) {
                    line = reader.readLine();
                    this.band = line.split("=\\s")[1].trim();
                    if (this.band.equals(this.bands[0])) {
                        line = reader.readLine();
                        this.fov = Double.parseDouble(line.split("=\\s")[1]);
                        line = reader.readLine();
                        this.dm = Double.parseDouble(line.split("=\\s")[1]);
                        line = reader.readLine();
                        this.freq = Double.parseDouble(line.split("=\\s")[1]);
                        line = reader.readLine();
                        this.freqband = Double.parseDouble(line.split("=\\s")[1]);
                        line = reader.readLine();
                        this.num_chan = Integer.parseInt(line.split("=\\s")[1].trim());
                        line = reader.readLine();
                        this.chan_wid = Double.parseDouble(line.split("=\\s")[1]);
                    } else if (this.band.equals(this.bands[4]) || this.band.equals(this.bands[5])) {
                        line = reader.readLine();
                        this.fov = Double.parseDouble(line.split("=\\s")[1]);
                        line = reader.readLine();
                        this.energy = Double.parseDouble(line.split("=\\s")[1]);
                        line = reader.readLine();
                        this.energyband = Double.parseDouble(line.split("=\\s")[1]);
                    } else {
                        line = reader.readLine();
                        this.filt = line.split("=\\s")[1].trim();
                        line = reader.readLine();
                        this.fov = Double.parseDouble(line.split("=\\s")[1]);
                        line = reader.readLine();
                        this.wavelen = Double.parseDouble(line.split("=\\s")[1]);
                        line = reader.readLine();
                        this.waveband = Double.parseDouble(line.split("=\\s")[1]);
                    }
                }
                line = reader.readLine();
                this.analyzer = line.split("=\\s")[1].trim();
                line = reader.readLine();
                line = reader.readLine();
                this.notes = line.trim();
                reader.close();
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        public void setSourceID(String sourceID) {
            this.object = sourceID;
        }

        public void setNPoints(long nPoints) {
            this.N = nPoints;
        }

        public void setFrequency(double frequency) {
            this.freq = frequency;
        }

        public void setTobs(double obstime) {
        }

        public void setBandwidth(double bandwidth) {
            this.freqband = bandwidth;
        }

        public void setTSamp(double tSamp) {
            this.dt = tSamp;
        }

        public void setMjdStart(double mjdStart) {
            this.mjd_i = (int) mjdStart;
            this.mjd_f = mjdStart - this.mjd_i;
        }

        public double getTSamp() {
            return this.dt;
        }

        public double getTobs() {
            return this.dt * this.N;
        }

        public double getFrequency() {
            return this.freq;
        }

        public long getNPoints() {
            return (long) this.N;
        }

        public double getBandwidth() {
            return this.freqband;
        }

        public double getMjdStart() {
            return this.mjd_i + this.mjd_f;
        }

        public String getSourceID() {
            return this.object;
        }

        @Override
        public Coordinate getCoord() {
            return new Coordinate(new RA(ra_h, ra_m, ra_s), new Dec(dec_d, ra_m, ra_s, negdec));
        }

        @Override
        public Telescope getTelescope() {
            Telescope tel = null;
            try {
                tel = Telescope.valueOf(telescope.toUpperCase());
            } catch (Exception e) {
                tel = Telescope.UNKNOWN;
            }
            return tel;
        }
    }

    public static float arr2float(byte[] arr, int start) {
        int i = 0;
        int len = 4;
        int cnt = 0;
        byte[] tmp = new byte[len];
        for (i = start; i < (start + len); i++) {
            tmp[cnt] = arr[i];
            cnt++;
        }
        int accum = 0;
        i = 0;
        for (int shiftBy = 0; shiftBy < 32; shiftBy += 8) {
            accum |= ((long) (tmp[i] & 0xff)) << shiftBy;
            i++;
        }
        return Float.intBitsToFloat(accum);
    }

    public void release() {
        this.dataFile = null;
        this.header = null;
        this.headerFile = null;
        this.fb = null;
        this.in = null;
    }

    public void flush() throws IOException {
        throw new UnsupportedOperationException("Flush not supported on TestTimeSeries");
    }
}
