package org.tritonus.midi.device.alsa;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.MetaMessage;
import org.tritonus.share.TDebug;
import org.tritonus.lowlevel.alsa.AlsaSeq;
import org.tritonus.lowlevel.alsa.AlsaSeqEvent;

/**
   This class sends events always to clients that have subscribed to the client
   passed as AlsaSeq object and the source port number passed.
   This class doesn't establish any subscriptions. They have to be
   established elsewhere.
 */
public class AlsaMidiOut {

    /**	The low-level object to interface to the ALSA sequencer.
	 */
    private AlsaSeq m_alsaSeq;

    /**	The source port to use for sending messages via the ALSA sequencer.
	 */
    private int m_nSourcePort;

    /**	The sequencer queue to use inside the ALSA sequencer.
	 *	This value is only used (and valid) if m_bImmediately
	 *	false. Otherwise, events are sent directely to the destination
	 *	client, circumventing queues.
	 */
    private int m_nQueue;

    private boolean m_bImmediately;

    private boolean m_bHandleMetaMessages;

    private AlsaSeqEvent m_event = new AlsaSeqEvent();

    public AlsaMidiOut(AlsaSeq aSequencer, int nSourcePort, int nQueue) {
        this(aSequencer, nSourcePort, nQueue, false);
    }

    public AlsaMidiOut(AlsaSeq aSequencer, int nSourcePort) {
        this(aSequencer, nSourcePort, -1, true);
    }

    private AlsaMidiOut(AlsaSeq aSequencer, int nSourcePort, int nQueue, boolean bImmediately) {
        if (TDebug.TraceAlsaMidiOut) {
            TDebug.out("AlsaMidiOut.<init>(AlsaSeq, int, int, boolean): begin");
        }
        m_alsaSeq = aSequencer;
        m_nSourcePort = nSourcePort;
        m_nQueue = nQueue;
        m_bImmediately = bImmediately;
        m_bHandleMetaMessages = false;
        if (TDebug.TraceAlsaMidiOut) {
            TDebug.out("AlsaMidiOut.<init>(AlsaSeq, int, int, boolean): end");
        }
    }

    private AlsaSeq getAlsaSeq() {
        return m_alsaSeq;
    }

    private int getSourcePort() {
        return m_nSourcePort;
    }

    private int getQueue() {
        return m_nQueue;
    }

    private boolean getImmediately() {
        return m_bImmediately;
    }

    public boolean getHandleMetaMessages() {
        return m_bHandleMetaMessages;
    }

    public void setHandleMetaMessages(boolean bHandleMetaMessages) {
        m_bHandleMetaMessages = bHandleMetaMessages;
    }

    public synchronized void enqueueMessage(MidiMessage event, long lTick) {
        if (TDebug.TraceAlsaMidiOut) {
            TDebug.out("AlsaMidiOut.enqueueMessage(): begin");
        }
        if (event instanceof ShortMessage) {
            enqueueShortMessage((ShortMessage) event, lTick);
        } else if (event instanceof SysexMessage) {
            enqueueSysexMessage((SysexMessage) event, lTick);
        } else if (event instanceof MetaMessage && getHandleMetaMessages()) {
            enqueueMetaMessage((MetaMessage) event, lTick);
        } else {
        }
        if (TDebug.TraceAlsaMidiOut) {
            TDebug.out("AlsaMidiOut.enqueueMessage(): end");
        }
    }

    private void enqueueShortMessage(ShortMessage shortMessage, long lTime) {
        int nChannel = shortMessage.getChannel();
        switch(shortMessage.getCommand()) {
            case ShortMessage.NOTE_OFF:
                sendNoteOffEvent(lTime, nChannel, shortMessage.getData1(), shortMessage.getData2());
                break;
            case ShortMessage.NOTE_ON:
                sendNoteOnEvent(lTime, nChannel, shortMessage.getData1(), shortMessage.getData2());
                break;
            case ShortMessage.POLY_PRESSURE:
                sendKeyPressureEvent(lTime, nChannel, shortMessage.getData1(), shortMessage.getData2());
                break;
            case ShortMessage.CONTROL_CHANGE:
                sendControlChangeEvent(lTime, nChannel, shortMessage.getData1(), shortMessage.getData2());
                break;
            case ShortMessage.PROGRAM_CHANGE:
                sendProgramChangeEvent(lTime, nChannel, shortMessage.getData1());
                break;
            case ShortMessage.CHANNEL_PRESSURE:
                sendChannelPressureEvent(lTime, nChannel, shortMessage.getData1());
                break;
            case ShortMessage.PITCH_BEND:
                sendPitchBendEvent(lTime, nChannel, get14bitValue(shortMessage.getData1(), shortMessage.getData2()));
                break;
            case 0xF0:
                switch(shortMessage.getStatus()) {
                    case ShortMessage.MIDI_TIME_CODE:
                        sendMTCEvent(lTime, shortMessage.getData1());
                        break;
                    case ShortMessage.SONG_POSITION_POINTER:
                        sendSongPositionPointerEvent(lTime, get14bitValue(shortMessage.getData1(), shortMessage.getData2()));
                        break;
                    case ShortMessage.SONG_SELECT:
                        sendSongSelectEvent(lTime, shortMessage.getData1());
                        break;
                    case ShortMessage.TUNE_REQUEST:
                        sendTuneRequestEvent(lTime);
                        break;
                    case ShortMessage.TIMING_CLOCK:
                        sendMidiClockEvent(lTime);
                        break;
                    case ShortMessage.START:
                        sendStartEvent(lTime);
                        break;
                    case ShortMessage.CONTINUE:
                        sendContinueEvent(lTime);
                        break;
                    case ShortMessage.STOP:
                        sendStopEvent(lTime);
                        break;
                    case ShortMessage.ACTIVE_SENSING:
                        sendActiveSensingEvent(lTime);
                        break;
                    case ShortMessage.SYSTEM_RESET:
                        sendSystemResetEvent(lTime);
                        break;
                    default:
                        TDebug.out("AlsaMidiOut.enqueueShortMessage(): UNKNOWN EVENT TYPE: " + shortMessage.getStatus());
                }
                break;
            default:
                TDebug.out("AlsaMidiOut.enqueueShortMessage(): UNKNOWN EVENT TYPE: " + shortMessage.getStatus());
        }
    }

    private static int get14bitValue(int nLSB, int nMSB) {
        return (nLSB & 0x7F) | ((nMSB & 0x7F) << 7);
    }

    private void sendNoteOffEvent(long lTime, int nChannel, int nNote, int nVelocity) {
        sendNoteEvent(AlsaSeq.SND_SEQ_EVENT_NOTEOFF, lTime, nChannel, nNote, nVelocity);
    }

    private void sendNoteOnEvent(long lTime, int nChannel, int nNote, int nVelocity) {
        sendNoteEvent(AlsaSeq.SND_SEQ_EVENT_NOTEON, lTime, nChannel, nNote, nVelocity);
    }

    private void sendNoteEvent(int nType, long lTime, int nChannel, int nNote, int nVelocity) {
        setCommon(nType, 0, lTime);
        m_event.setNote(nChannel, nNote, nVelocity, 0, 0);
        sendEvent();
    }

    private void sendKeyPressureEvent(long lTime, int nChannel, int nNote, int nPressure) {
        sendControlEvent(AlsaSeq.SND_SEQ_EVENT_KEYPRESS, lTime, nChannel, nNote, nPressure);
    }

    private void sendControlChangeEvent(long lTime, int nChannel, int nControl, int nValue) {
        sendControlEvent(AlsaSeq.SND_SEQ_EVENT_CONTROLLER, lTime, nChannel, nControl, nValue);
    }

    private void sendProgramChangeEvent(long lTime, int nChannel, int nProgram) {
        sendControlEvent(AlsaSeq.SND_SEQ_EVENT_PGMCHANGE, lTime, nChannel, 0, nProgram);
    }

    private void sendChannelPressureEvent(long lTime, int nChannel, int nPressure) {
        sendControlEvent(AlsaSeq.SND_SEQ_EVENT_CHANPRESS, lTime, nChannel, 0, nPressure);
    }

    private void sendPitchBendEvent(long lTime, int nChannel, int nPitch) {
        sendControlEvent(AlsaSeq.SND_SEQ_EVENT_PITCHBEND, lTime, nChannel, 0, nPitch);
    }

    private void sendControlEvent(int nType, long lTime, int nChannel, int nParam, int nValue) {
        setCommon(nType, 0, lTime);
        m_event.setControl(nChannel, nParam, nValue);
        sendEvent();
    }

    private void sendMTCEvent(long lTime, int nData) {
        sendControlEvent(AlsaSeq.SND_SEQ_EVENT_QFRAME, lTime, 0, 0, nData);
    }

    private void sendSongPositionPointerEvent(long lTime, int nPosition) {
        sendControlEvent(AlsaSeq.SND_SEQ_EVENT_SONGPOS, lTime, 0, 0, nPosition);
    }

    private void sendSongSelectEvent(long lTime, int nSong) {
        sendControlEvent(AlsaSeq.SND_SEQ_EVENT_SONGSEL, lTime, 0, 0, nSong);
    }

    private void sendTuneRequestEvent(long lTime) {
        sendEvent(AlsaSeq.SND_SEQ_EVENT_TUNE_REQUEST, lTime);
    }

    private void sendMidiClockEvent(long lTime) {
        sendQueueControlEvent(AlsaSeq.SND_SEQ_EVENT_CLOCK, lTime, 0, 0, 0);
    }

    private void sendStartEvent(long lTime) {
        sendQueueControlEvent(AlsaSeq.SND_SEQ_EVENT_START, lTime, 0, 0, 0);
    }

    private void sendContinueEvent(long lTime) {
        sendQueueControlEvent(AlsaSeq.SND_SEQ_EVENT_CONTINUE, lTime, 0, 0, 0);
    }

    private void sendStopEvent(long lTime) {
        sendQueueControlEvent(AlsaSeq.SND_SEQ_EVENT_STOP, lTime, 0, 0, 0);
    }

    private void sendActiveSensingEvent(long lTime) {
        sendEvent(AlsaSeq.SND_SEQ_EVENT_SENSING, lTime);
    }

    private void sendSystemResetEvent(long lTime) {
        sendEvent(AlsaSeq.SND_SEQ_EVENT_RESET, lTime);
    }

    private void sendQueueControlEvent(int nType, long lTime, int nQueue, int nValue, long lControlTime) {
        setCommon(nType, 0, lTime);
        m_event.setQueueControl(nQueue, nValue, lControlTime);
        sendEvent();
    }

    private void sendEvent(int nType, long lTime) {
        setCommon(nType, 0, lTime);
        sendEvent();
    }

    private void enqueueSysexMessage(SysexMessage message, long lTick) {
        byte[] abData = message.getMessage();
        int nLength = message.getLength();
        if ((abData[0] & 0xFF) == SysexMessage.SYSTEM_EXCLUSIVE) {
            sendVarEvent(AlsaSeq.SND_SEQ_EVENT_SYSEX, lTick, abData, 0, nLength);
        } else {
            sendVarEvent(AlsaSeq.SND_SEQ_EVENT_SYSEX, lTick, abData, 1, nLength - 1);
        }
    }

    private void enqueueMetaMessage(MetaMessage message, long lTick) {
        byte[] abData = message.getData();
        byte[] abTransferData = new byte[abData.length + 1];
        abTransferData[0] = (byte) message.getType();
        System.arraycopy(abData, 0, abTransferData, 1, abData.length);
        sendVarEvent(AlsaSeq.SND_SEQ_EVENT_USR_VAR4, lTick, abTransferData, 0, abTransferData.length);
    }

    private void sendVarEvent(int nType, long lTime, byte[] abData, int nOffset, int nLength) {
        setCommon(nType, AlsaSeq.SND_SEQ_EVENT_LENGTH_VARIABLE, lTime);
        m_event.setVar(abData, 0, nLength);
        sendEvent();
    }

    private void setCommon(int nType, int nAdditionalFlags, long lTime) {
        if (getImmediately()) {
            if (TDebug.TraceAlsaMidiOut) {
                TDebug.out("AlsaMidiOut.enqueueShortMessage(): sending noteoff message (immediately)");
            }
            m_event.setCommon(nType, AlsaSeq.SND_SEQ_TIME_STAMP_REAL | AlsaSeq.SND_SEQ_TIME_MODE_REL | nAdditionalFlags, 0, AlsaSeq.SND_SEQ_QUEUE_DIRECT, 0L, 0, getSourcePort(), AlsaSeq.SND_SEQ_ADDRESS_SUBSCRIBERS, AlsaSeq.SND_SEQ_ADDRESS_UNKNOWN);
        } else {
            if (TDebug.TraceAlsaMidiOut) {
                TDebug.out("AlsaMidiOut.enqueueShortMessage(): sending noteoff message (timed)");
            }
            m_event.setCommon(nType, AlsaSeq.SND_SEQ_TIME_STAMP_TICK | AlsaSeq.SND_SEQ_TIME_MODE_ABS | nAdditionalFlags, 0, getQueue(), lTime, 0, getSourcePort(), AlsaSeq.SND_SEQ_ADDRESS_SUBSCRIBERS, AlsaSeq.SND_SEQ_ADDRESS_UNKNOWN);
        }
    }

    /**	Puts the event into the queue.
	 */
    private void sendEvent() {
        getAlsaSeq().eventOutput(m_event);
        getAlsaSeq().drainOutput();
    }
}
