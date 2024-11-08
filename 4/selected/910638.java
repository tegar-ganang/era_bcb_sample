package ch.amotta.qweely.wave;

import ch.amotta.qweely.fft.FastFourierTransform;
import ch.amotta.qweely.fft.FrequencySpectrum;
import ch.amotta.qweely.utils.Stats;

/**
 *
 * @author Alessandro
 */
public class WAVESong {

    private int _numbChannels;

    private int _numbSamples;

    private long _sampleRate;

    private int _bitDepth;

    private long[][] _channels;

    private double[] _mono;

    private FrequencySpectrum _frequencySpectrum = null;

    public int getNumbChannels() {
        return _numbChannels;
    }

    public int getNumbSamples() {
        return _numbSamples;
    }

    public long getSampleRate() {
        return _sampleRate;
    }

    public int getDuration() {
        return (int) Math.round((double) _numbSamples / _sampleRate);
    }

    public int getBitDepth() {
        return _bitDepth;
    }

    public int getByteDepth() {
        return (int) Math.ceil(0.125 * _bitDepth);
    }

    public long[][] getChannels() {
        return _channels;
    }

    public double[] getResampled(int sampleRate, int start, int numbSamples) {
        if (sampleRate < 0) new IllegalArgumentException();
        if (numbSamples < 1) new IllegalArgumentException();
        int samplesPerSample = (int) ((double) _sampleRate / sampleRate);
        double sum;
        double[] mono = getMono();
        double[] output = new double[numbSamples];
        for (int currentSample = 0; currentSample < numbSamples; currentSample++) {
            sum = 0;
            for (int i = 0; i < samplesPerSample; i++) {
                sum += mono[start + currentSample * samplesPerSample + i];
            }
            output[currentSample] = sum / samplesPerSample;
        }
        return output;
    }

    public void setChannels(long[][] channels) {
        if (channels.length == 0) new IllegalArgumentException();
        if (channels[0].length == 0) new IllegalArgumentException();
        _channels = channels;
        _numbChannels = _channels.length;
        _numbSamples = _channels[0].length;
    }

    public void setSampleRate(long sampleRate) {
        _sampleRate = sampleRate;
    }

    public void setBitDepth(int bitDepth) {
        _bitDepth = bitDepth;
    }

    public FrequencySpectrum getFrequencySpectrum() {
        createFrequencySpectrum();
        return _frequencySpectrum;
    }

    private void createFrequencySpectrum() {
        if (_frequencySpectrum != null) return;
        double secondsPerAnalysis = 2.0;
        int numbAnalysis = (int) Math.max(_numbSamples / (_sampleRate * secondsPerAnalysis), 1);
        double analysisDuration = Math.min(_numbSamples / _sampleRate, 5);
        int analysisLength = ((0x1) << (int) (Math.log(analysisDuration * _sampleRate) / Math.log(2.0)));
        int analysisGap = (int) Math.floor((double) (_numbSamples - numbAnalysis * analysisLength) / (numbAnalysis - 1));
        FastFourierTransform fft = new FastFourierTransform(this);
        double[] frequencies = null;
        int start;
        int end;
        for (int i = 0; i < numbAnalysis; i++) {
            start = i * (analysisLength + analysisGap);
            end = start + analysisLength - 1;
            if (frequencies == null) {
                frequencies = fft.transform(start, end).getFrequencySpectrum();
            } else {
                frequencies = Stats.sumDoubleArrays(frequencies, fft.transform(start, end).getFrequencySpectrum());
            }
        }
        _frequencySpectrum = new FrequencySpectrum(frequencies);
    }

    public double[] getMono() {
        if (_mono == null) createMono();
        return _mono;
    }

    private void createMono() {
        _mono = new double[_numbSamples];
        long maxValue = ((0x1) << _bitDepth) - 1;
        long sum;
        for (int currentSample = 0; currentSample < _numbSamples; currentSample++) {
            sum = 0;
            for (int currentChannel = 0; currentChannel < _numbChannels; currentChannel++) {
                sum += _channels[currentChannel][currentSample];
            }
            _mono[currentSample] = (double) sum / (_numbChannels * maxValue);
        }
    }
}
