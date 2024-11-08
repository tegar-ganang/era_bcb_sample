package com.sts.webmeet.content.client.audio;

import java.util.Enumeration;
import java.util.Properties;
import com.sts.webmeet.api.MessageRouter;
import com.sts.webmeet.content.common.audio.AudioDataMessage;
import com.sts.webmeet.content.common.audio.AudioDecoder;
import com.sts.webmeet.content.common.audio.AudioEncoder;
import com.sts.webmeet.content.common.audio.WHAudioFormat;
import com.sts.webmeet.content.common.audio.LevelListener;

public class MessageSoundSystem implements SoundSystem {

    public void setAudioMessageObservable(AudioMessageObservable amo) {
        audioObservable = amo;
    }

    public void setMessageRouter(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }

    public void setMicLevelListener(LevelListener ll) {
        micLevelListener = ll;
    }

    public void setSpeakerLevelListener(LevelListener ll) {
        speakerLevelListener = ll;
    }

    public void setEncoderClass(String strEncoderClass) throws Exception {
        System.out.println("setting encoder class: " + strEncoderClass);
        this.encoder = (AudioEncoder) Class.forName(strEncoderClass).newInstance();
    }

    public void setDecoderClass(String strDecoderClass) throws Exception {
        System.out.println("setting decoder class: " + strDecoderClass);
        this.decoder = (AudioDecoder) Class.forName(strDecoderClass).newInstance();
    }

    public void setMicImpl(Microphone mic) {
        this.mic = mic;
        micDataSource = new MicDataSource(mic, audioFormat);
        micDataSource.setAtomSize(this.encoder.getInputFrameSizeInBytes());
    }

    public boolean turnMicOn() {
        boolean bRet = false;
        if (bIsMicOn) {
            return true;
        }
        if (null == mic) {
            String strJVM = System.getProperty("java.vendor");
            if (strJVM.indexOf("Microsoft") != -1) {
                try {
                    if (Class.forName("com.ms.security.PolicyEngine") != null) {
                        com.ms.security.PolicyEngine.assertPermission(com.ms.security.PermissionID.SYSTEM);
                    }
                    Class classWin32Mic = Class.forName("com.sts.webmeet.content.client.audio.Win32Mic");
                    mic = (Microphone) classWin32Mic.newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (strJVM.toLowerCase().indexOf("apple") != -1) {
                try {
                    Class classJava2Mic = Class.forName("com.sts.webmeet.content.client.audio.Java2MacMic");
                    mic = (Microphone) classJava2Mic.newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    Class classJava2Mic = Class.forName("com.sts.webmeet.content.client.audio.Java2Mic");
                    mic = (Microphone) classJava2Mic.newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            micDataSource = new MicDataSource(mic, audioFormat);
            micDataSource.setAtomSize(this.encoder.getInputFrameSizeInBytes());
            bRet = this.mic != null;
        }
        if (null != mic) {
            bContinueMic = true;
            micLevelDataSource = new LevelDataSource(micDataSource, "Mic");
            compressingDataSource = new AudioCompressingDataSource(micLevelDataSource, this.encoder);
            micThread = new MicThread();
            bRet = micDataSource.openMic();
            if (bRet) {
                micThread.start();
            }
        }
        bIsMicOn = bRet;
        if (bIsMicOn) {
            System.out.println("Capturing audio...");
        }
        return bRet;
    }

    public boolean turnMicOff() {
        if (mic != null) {
            bContinueMic = false;
            try {
                micDataSource.interruptConsumers();
                micThread.join(2000);
                System.out.println("...stopped capturing audio.");
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            } finally {
                try {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mic = null;
            micDataSource = null;
        }
        bIsMicOn = false;
        return true;
    }

    public boolean turnSpeakerOn() {
        System.out.println(getClass().getName() + ".turnSpeakerOn");
        boolean bRet = false;
        if (bIsSpeakerOn) {
            System.out.println(getClass().getName() + ".turnSpeakerOn -- speaker already on");
            return true;
        }
        messageDataSource = new MessageDataSource(audioObservable);
        ads = new AudioAtomicDataSource(messageDataSource, this.decoder);
        LevelDataSource speakerLevelDataSource = new LevelDataSource(ads, "Speaker");
        speakerLevelDataSource.setLevelListener(speakerLevelListener);
        String strJVM = System.getProperty("java.version");
        try {
            if (strJVM.charAt(2) > '2') {
                Class classJava2Speaker = Class.forName("com.sts.webmeet.content.client.audio.Java2Speaker");
                speaker = (Speaker) classJava2Speaker.newInstance();
            } else {
                speaker = new AuSpeaker();
            }
            speaker.startPlaying(audioFormat.getChannelCount(), (new Float(audioFormat.getSamplesPerSecond())).floatValue(), audioFormat.getBitsPerSample(), speakerLevelDataSource);
            bRet = true;
        } catch (Exception e) {
            e.printStackTrace();
            speaker = new NullSpeaker();
            try {
                speaker.startPlaying(audioFormat.getChannelCount(), (new Float(audioFormat.getSamplesPerSecond())).floatValue(), audioFormat.getBitsPerSample(), speakerLevelDataSource);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        bIsSpeakerOn = bRet;
        return bRet;
    }

    public boolean turnSpeakerOff() {
        if (null != messageDataSource) {
            messageDataSource.close();
        }
        this.speaker.stopPlaying();
        bIsSpeakerOn = false;
        return true;
    }

    public void setVOXThreshold(int iThreshold) {
        this.iVoxThreshold = iThreshold;
    }

    public void setVOXThreshold(String str) {
        this.setVOXThreshold(Integer.parseInt(str));
    }

    private Speaker speaker;

    private AtomicDataSource ads;

    private AudioMessageObservable audioObservable;

    private MicDataSource micDataSource;

    private VoxDataSource voxDataSource;

    private LevelDataSource micLevelDataSource;

    private AudioCompressingDataSource compressingDataSource;

    private MessageDataSource messageDataSource;

    private Microphone mic = null;

    private MessageRouter messageRouter;

    private MicThread micThread;

    private boolean bContinueMic = false;

    private LevelListener speakerLevelListener;

    private LevelListener micLevelListener;

    private WHAudioFormat audioFormat = null;

    private boolean bIsMicOn;

    private boolean bIsSpeakerOn;

    private int iVoxThreshold = VoxDataSource.DEFAULT_THRESHOLD;

    private AudioEncoder encoder;

    private AudioDecoder decoder;

    private int iFramesPerMessage = 20;

    class MicThread extends Thread {

        public MicThread() {
            setName(getClass().getName());
        }

        public void run() {
            try {
                while (bContinueMic) {
                    AudioDataMessage message = new AudioDataMessage();
                    for (int i = 0; i < MessageSoundSystem.this.iFramesPerMessage && bContinueMic == true; i++) {
                        byte[] baCodedAudio = new byte[compressingDataSource.getDataLength()];
                        compressingDataSource.getData(baCodedAudio, 0);
                        message.addFrame(baCodedAudio);
                    }
                    if (bContinueMic) {
                        sendAudioDataMessage(message);
                    }
                }
            } catch (java.io.IOException ioe) {
                ioe.printStackTrace();
            }
            mic.close();
        }
    }

    void sendAudioDataMessage(AudioDataMessage message) {
        messageRouter.sendMessage(message);
    }

    public void setOptions(Properties props) {
        Enumeration enumer = props.keys();
        while (enumer.hasMoreElements()) {
            String strKey = (String) enumer.nextElement();
            this.encoder.setOption(strKey, props.getProperty(strKey));
            this.decoder.setOption(strKey, props.getProperty(strKey));
        }
        if (null != props.getProperty("webhuddle.property.audio.vox.threshold")) {
            this.setVOXThreshold(props.getProperty("webhuddle.property.audio.vox.threshold"));
        }
        this.audioFormat = this.decoder.getFormat();
    }
}
