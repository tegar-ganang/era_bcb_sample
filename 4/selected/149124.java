package lo.local.dreamrec.logic;

import com.webkitchen.eeg.acquisition.EEGAcquisitionController;
import com.webkitchen.eeg.acquisition.IRawSampleListener;
import com.webkitchen.eeg.acquisition.RawSample;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class EEGDataProvider implements IncomingDataProvider, IRawSampleListener {

    private final int AVERAGING_PERIOD = 25;

    private long producedDataNumber;

    private List<IncomingDataListener> listeners = new ArrayList<IncomingDataListener>();

    private int eegDataIndex;

    private int eegDataValueSum;

    private int eegDataLostPacketsNumber;

    private static final Log log = LogFactory.getLog(EEGDataProvider.class);

    public EEGDataProvider() {
        EEGAcquisitionController.getInstance().getChannelSampleGenerator().addSampleListener(this, new int[] { 2 });
    }

    public void addIncomingDataListener(IncomingDataListener incomingDataListener) {
        listeners.add(incomingDataListener);
    }

    public double getIncomingDataFrequency() {
        throw new UnsupportedOperationException("todo");
    }

    public void start() throws IOException {
        producedDataNumber = 0;
        eegDataIndex = 0;
        resetAveragingPeriodValues();
        EEGAcquisitionController.getInstance().startReading(false);
    }

    public void stop() {
        EEGAcquisitionController.getInstance().stopReading();
    }

    public void receiveSample(RawSample rawSample) {
        accumulateAveragingPeriodValues(rawSample.getPacketNumber(), rawSample.getSamples()[0]);
        if (producedDataNumber % AVERAGING_PERIOD == 0) {
            invokeListeners(produceData(), producedDataNumber);
            if (eegDataLostPacketsNumber > 0) {
                log.warn(eegDataLostPacketsNumber + " data samples were lost.");
            }
            resetAveragingPeriodValues();
        }
    }

    private int produceData() {
        int eegDataAverageValue = eegDataValueSum / (AVERAGING_PERIOD - eegDataLostPacketsNumber);
        producedDataNumber++;
        return eegDataAverageValue;
    }

    private void invokeListeners(int dataValue, long dataNumber) {
        final int dataValue_ = dataValue;
        final long dataNumber_ = dataNumber;
        for (final IncomingDataListener listener : listeners) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    listener.dataReceived(dataValue_, dataNumber_);
                }
            });
        }
    }

    private void accumulateAveragingPeriodValues(int eegDataNextIndex, int eegDataNextValue) {
        if (eegDataIndex != 0) {
            eegDataLostPacketsNumber += eegDataNextIndex - eegDataIndex - 1;
        }
        eegDataIndex = eegDataNextIndex;
        eegDataValueSum += eegDataNextValue;
    }

    private void resetAveragingPeriodValues() {
        eegDataValueSum = 0;
        eegDataLostPacketsNumber = 0;
    }
}
