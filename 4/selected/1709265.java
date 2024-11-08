package ch.laoe.audio;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.UnsupportedAudioFileException;
import ch.laoe.audio.capture.ACapture;
import ch.laoe.audio.capture.ACaptureFactory;
import ch.laoe.audio.load.ALoad;
import ch.laoe.audio.load.ALoadFactory;
import ch.laoe.audio.play.APlay;
import ch.laoe.audio.play.APlayFactory;
import ch.laoe.audio.save.ASave;
import ch.laoe.audio.save.ASaveFactory;
import ch.laoe.clip.AChannel;
import ch.laoe.clip.AClip;
import ch.laoe.clip.AClipStorage;
import ch.laoe.clip.ALayer;
import ch.laoe.ui.Debug;
import ch.laoe.ui.GLanguage;
import ch.laoe.ui.LProgressViewer;

/***********************************************************

This file is part of LAoE.

LAoE is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation; either version 2 of the License,
or (at your option) any later version.

LAoE is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with LAoE; if not, write to the Free Software Foundation,
Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


Class:			Audio
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	encapsulates all public audio functions. If you
					find some strange comments, excuse our cat 
					"moustique". He also likes my computer.

History:
Date:			Description:								Autor:
25.08.00		erster Entwurf								oli4
14.10.00		neues Audio-Framework					oli4
20.11.00		new stream-technic						oli4
30.03.01		exit from thread on destroy			oli4
20.09.01		forwind/rewind implemented				oli4
20.03.02		looping mode for play and capture	oli4

***********************************************************/
public class Audio implements Runnable {

    /**
	* constructor
	*/
    public Audio(AClip clip) {
        this.clip = clip;
        state = STOP;
        playPointer = 0;
        loopStartPointer = 0;
        setLoopEndPointer(clip.getMaxSampleLength());
        audioThread = new Thread(this);
        audioThread.setPriority(Thread.MAX_PRIORITY);
        running = true;
        audioThread.start();
        plotter = new AudioPlotter(this);
    }

    private AClip clip;

    public AClip getClip() {
        return clip;
    }

    private AudioFileFormat.Type fileType;

    public AudioFileFormat.Type getFileType() {
        return fileType;
    }

    public void setFileType(AudioFileFormat.Type ft) {
        fileType = ft;
    }

    public void setFileType(String extension) {
        if (extension.equals(".laoe")) setFileType(fileTypeLaoe); else if (extension.equals(".wav")) setFileType(AudioFileFormat.Type.WAVE); else if (extension.equals(".au")) setFileType(AudioFileFormat.Type.AU); else if (extension.equals(".snd")) setFileType(AudioFileFormat.Type.SND); else if (extension.equals(".aifc")) setFileType(AudioFileFormat.Type.AIFC); else if (extension.equals(".aiff")) setFileType(AudioFileFormat.Type.AIFF);
    }

    public static AudioFileFormat.Type[] getAllFileTypes() {
        AudioFileFormat.Type t[] = { fileTypeLaoe, AudioFileFormat.Type.AIFC, AudioFileFormat.Type.AIFF, AudioFileFormat.Type.AU, AudioFileFormat.Type.SND, AudioFileFormat.Type.WAVE };
        return t;
    }

    public static FileTypeLaoe fileTypeLaoe = new FileTypeLaoe();

    public static class FileTypeLaoe extends AudioFileFormat.Type {

        public FileTypeLaoe() {
            super("LAoE", ".laoe");
        }
    }

    private AFileOptions fileOptions;

    public static AFileOptions createFileOptions(String extension) {
        return AFileOptionsFactory.create(extension);
    }

    public void setFileOptions(AFileOptions fileOptions) {
        this.fileOptions = fileOptions;
    }

    public AFileOptions getFileOptions() {
        return fileOptions;
    }

    /**
	 *	returns informations about the audio-system
	 */
    public static String getAudioSystemInfo() {
        StringBuilder sb = new StringBuilder();
        Mixer.Info mi[] = AudioSystem.getMixerInfo();
        sb.append("AudioSystem:\n");
        sb.append("\naudio file types:\n");
        AudioFileFormat.Type aft[] = AudioSystem.getAudioFileTypes();
        for (int i = 0; i < aft.length; i++) {
            sb.append("  " + aft[i].toString() + "\n");
        }
        for (int i = 0; i < mi.length; i++) {
            try {
                sb.append("mixer[" + i + "] = " + mi[i].toString() + "\n");
                Line.Info tli[] = AudioSystem.getMixer(mi[i]).getTargetLineInfo();
                sb.append("  number of target lines = " + tli.length + "\n");
                for (int j = 0; j < tli.length; j++) {
                    sb.append("    target line[" + j + "] = " + tli[j].toString() + "\n");
                    AudioFormat taf[] = ((DataLine.Info) tli[j]).getFormats();
                    for (int k = 0; k < taf.length; k++) {
                        sb.append("      format[" + k + "] = " + taf[k].toString() + "\n");
                    }
                }
                Line.Info sli[] = AudioSystem.getMixer(mi[i]).getSourceLineInfo();
                sb.append("  number of source lines = " + sli.length + "\n");
                for (int j = 0; j < sli.length; j++) {
                    sb.append("    source line[" + j + "] = " + sli[j].toString() + "\n");
                    AudioFormat saf[] = ((DataLine.Info) sli[j]).getFormats();
                    for (int k = 0; k < saf.length; k++) {
                        sb.append("      format[" + k + "] = " + saf[k].toString() + "\n");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new String(sb);
    }

    private AudioFormat.Encoding encoding;

    public AudioFormat.Encoding getEncoding() {
        return encoding;
    }

    public void setEncoding(AudioFormat.Encoding e) {
        encoding = e;
    }

    public static AudioFormat.Encoding[] getAllEncodings() {
        AudioFormat.Encoding e[] = { AudioFormat.Encoding.ALAW, AudioFormat.Encoding.PCM_SIGNED, AudioFormat.Encoding.PCM_UNSIGNED, AudioFormat.Encoding.ULAW };
        return e;
    }

    private File file;

    /**
	*	open a soundfile, convert to a AClip. 
	*/
    public void open(File f) throws AudioException {
        boolean supports = false;
        try {
            supports = AClipStorage.supports(f);
        } catch (IOException e) {
            throw new AudioException("ioError");
        } catch (Exception e) {
            Debug.printStackTrace(5, e);
            throw new AudioException("unexpectedError");
        }
        if (supports) {
            try {
                AClipStorage.load(clip, f);
            } catch (IOException e) {
                throw new AudioException("ioError");
            } catch (Exception e) {
                Debug.printStackTrace(5, e);
                throw new AudioException("unexpectedError");
            }
        } else {
            ALoad load = ALoadFactory.create(f);
            ALayer l = new ALayer(load.getChannels(), load.getSampleLength());
            try {
                setFileType(AudioSystem.getAudioFileFormat(f).getType());
                setEncoding(AudioSystem.getAudioInputStream(f).getFormat().getEncoding());
                Debug.println(3, "open clip " + f.getName() + ", " + l.getNumberOfChannels() + " channels, " + l.getMaxSampleLength() + " samples, " + AudioSystem.getAudioInputStream(f).getFormat().getEncoding().toString() + " encoding, " + AudioSystem.getAudioInputStream(f).getFormat().getSampleSizeInBits() + " bits, big-endian " + AudioSystem.getAudioInputStream(f).getFormat().isBigEndian());
            } catch (UnsupportedAudioFileException uafe) {
                Debug.printStackTrace(5, uafe);
                throw new AudioException("unsupportedAudioFormat");
            } catch (IOException ioe) {
                Debug.printStackTrace(5, ioe);
                throw new AudioException("ioError");
            } catch (Exception e) {
                Debug.printStackTrace(5, e);
                throw new AudioException("unexpectedError");
            }
            int s = 0;
            int d;
            int i = 0;
            try {
                LProgressViewer.getInstance().entrySubProgress(0.9, "loadSamples");
                while ((d = load.read(l, s, 4000)) >= 0) {
                    s += d;
                    Debug.println(3, "load sample " + s);
                    if (i++ % 256 == 0) {
                        LProgressViewer.getInstance().setNote(GLanguage.translate("load") + " " + (s / (int) load.getSampleRate()) + " " + GLanguage.translate("seconds") + "...");
                        if (load.getSampleLength() > 1) {
                            if (LProgressViewer.getInstance().setProgress(s / load.getSampleLength())) return;
                        } else {
                            if (LProgressViewer.getInstance().setUnknownProgress()) return;
                        }
                    }
                    Thread.yield();
                }
                LProgressViewer.getInstance().exitSubProgress();
            } catch (IOException ioe) {
                Debug.printStackTrace(5, ioe);
                throw new AudioException("ioError");
            } catch (Exception e) {
                Debug.printStackTrace(5, e);
                throw new AudioException("unexpectedError");
            }
            load.close();
            clip.add(l);
            clip.setSampleRate(load.getSampleRate());
            clip.setSampleWidth(load.getSampleWidth());
            clip.getPlotter().autoScale();
            setLoopEndPointer(clip.getMaxSampleLength());
        }
        file = f;
        clip.setName(f.getName());
    }

    /**
	*	save the current AClip in the current file. If no current file is defined,
	*	it returns false, else true.
	*/
    public boolean save() throws AudioException {
        if (file != null) {
            if (file.isFile()) {
                saveAs(getFile());
                return true;
            }
        }
        return false;
    }

    /**						�
	*	���������������������
	*	save the current AClip in a new file
	*/
    public void saveAs(File f) throws AudioException {
        if (f.isDirectory()) {
            throw new AudioException("isDirectory");
        }
        setFile(f);
        clip.setName(f.getName());
        boolean supports = false;
        try {
            supports = AClipStorage.supports(f);
        } catch (IOException ioe) {
            Debug.printStackTrace(5, ioe);
            throw new AudioException("ioError");
        } catch (Exception e) {
            Debug.printStackTrace(5, e);
            throw new AudioException("unexpectedError");
        }
        if (supports) {
            try {
                AClipStorage.save(clip, f);
            } catch (IOException ioe) {
                Debug.printStackTrace(5, ioe);
                throw new AudioException("ioError");
            } catch (Exception e) {
                Debug.printStackTrace(5, e);
                throw new AudioException("unexpectedError");
            }
        } else {
            if ((clip.getNumberOfLayers() > 1) || (clip.getMaxNumberOfChannels() > 2)) {
                throw new AudioException("layerChannelOutOfRange");
            }
            String fn = f.getName();
            if (fn.endsWith(".wav")) {
                setFileType(AudioFileFormat.Type.WAVE);
                setEncoding(AudioFormat.Encoding.PCM_SIGNED);
            } else if (fn.endsWith(".aiff")) {
                setFileType(AudioFileFormat.Type.AIFF);
                setEncoding(AudioFormat.Encoding.PCM_SIGNED);
            } else if (fn.endsWith(".snd")) {
                throw new AudioException("unsupportedAudioFormat");
            } else if (fn.endsWith(".au")) {
                throw new AudioException("unsupportedAudioFormat");
            } else {
                throw new AudioException("unsupportedAudioFormat");
            }
            clip.prepareMask();
            ASave save = ASaveFactory.create(clip, f);
            {
                Debug.println(3, "file=" + f);
                Debug.println(3, "save clip as " + f.getName() + ", " + clip.getMaxNumberOfChannels() + " channels, " + clip.getMaxSampleLength() + " samples, " + getEncoding().toString() + " encoding, " + clip.getSampleWidth() + " bits, big-endian " + clip.isBigEndian());
            }
            try {
                LProgressViewer.getInstance().entrySubProgress(0.9, "saveSamples");
                save.write();
                LProgressViewer.getInstance().exitSubProgress();
            } catch (IOException ioe) {
                Debug.printStackTrace(5, ioe);
                throw new AudioException("ioError");
            } catch (Exception e) {
                Debug.printStackTrace(5, e);
                throw new AudioException("unexpectedError");
            }
        }
    }

    /**
	*	set the current file
	*/
    private void setFile(File f) {
        file = f;
    }

    /**
	*	get the current file
	*/
    public File getFile() {
        return file;
    }

    private Thread audioThread;

    private boolean running;

    private Object monitor;

    public void run() {
        monitor = new Object();
        while (running) {
            try {
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                Thread.yield();
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ie) {
                }
                switch(state) {
                    case PLAY:
                        onPlay();
                        break;
                    case REC:
                        onRec();
                        break;
                    case PAUSE:
                        break;
                    default:
                        doWait();
                        break;
                }
            } catch (Exception e) {
                Debug.printStackTrace(5, e);
            }
        }
    }

    /**
	 *	destroys the running tasks. call when not using anymore.
	 */
    public void destroy() {
        running = false;
        clip = null;
    }

    private void doWait() {
        try {
            synchronized (monitor) {
                monitor.wait();
            }
        } catch (InterruptedException ie) {
        }
    }

    private void wakeup() {
        synchronized (monitor) {
            monitor.notify();
        }
    }

    private boolean looping = false;

    public void setLooping(boolean l) {
        looping = l;
    }

    public boolean isLooping() {
        return looping;
    }

    private boolean autoGrowing = false;

    public void setAutoGrowing(boolean ag) {
        autoGrowing = ag;
        if (autoGrowing) {
            looping = false;
        }
    }

    public boolean isAutoGrowing() {
        return autoGrowing;
    }

    public static final int STOP = 1;

    public static final int PLAY = 2;

    public static final int PAUSE = 3;

    public static final int REC = 4;

    private int state;

    private APlay player;

    private int playOffset;

    private static int playPacketSize = 3000;

    private static int playPointerAdjustment = -40000;

    public static void setPlayBlockSize(int s) {
        playPacketSize = s;
    }

    public static int getPlayBlockSize() {
        return playPacketSize;
    }

    public static void setPlayPointerAdjustment(int s) {
        playPointerAdjustment = s;
    }

    public static int getPlayPointerAdjustment() {
        return playPointerAdjustment;
    }

    /**
	*	start playing sound
	*/
    public void play() throws AudioException {
        noWinding();
        if (state == STOP) {
            clip.prepareMask();
            playOffset = getLoopStartPointer();
            player = APlayFactory.create(clip);
            player.goTo(playOffset);
            player.start();
            Debug.println(3, "start playing...");
            state = PLAY;
        }
        wakeup();
        fireAudioEvent();
    }

    /**
	 * plays a short piece of audio only. this is useful when playing in function of mouse motion. 
	 * @param position
	 */
    public synchronized void scratch(int position) {
        try {
            switch(state) {
                case PLAY:
                case PAUSE:
                    if (position < 0) {
                        position = 0;
                    }
                    int e = getLoopEndPointer() - playPacketSize;
                    if (position > e) {
                        position = e - 1;
                    }
                    player.goTo(position);
                    player.write(playPacketSize);
                    setPlayPointer(position);
                    playOffset = position;
                    Debug.println(7, "scratch...");
                    break;
            }
        } catch (Exception e) {
        }
    }

    private synchronized void onPlay() {
        try {
            int n = getLoopEndPointer() - playOffset;
            boolean lastPacket = true;
            if (n > playPacketSize) {
                n = playPacketSize;
                lastPacket = false;
            } else if (n < 0) {
                n = 0;
            }
            int r = player.write(n);
            Debug.println(8, "play sample " + playOffset + ", packet size " + n + " written " + r);
            onWinding();
            if (lastPacket || (r < 0)) {
                if (looping) {
                    playOffset = getLoopStartPointer();
                    player.goTo(playOffset);
                    Debug.println(3, "...loop back");
                } else {
                    state = STOP;
                    playOffset = getLoopEndPointer();
                    setPlayPointer(getLoopEndPointer());
                    Debug.println(3, "...autostop playing");
                    fireAudioEvent();
                    return;
                }
            } else {
                playOffset += r;
            }
            setPlayPointer(playOffset + playPointerAdjustment);
        } catch (Exception ioe) {
            Debug.println(3, "...play problem!!!");
            ioe.printStackTrace();
            state = STOP;
            player.stop();
            fireAudioEvent();
        }
    }

    /**
	 *	changes the samplerate on the fly!
	 */
    public void changeSampleRate(float sr) {
        if (player != null) {
            player.changeSampleRate(sr);
        }
    }

    private int winding;

    private static final int FORWIND = 1;

    private static final int REWIND = 2;

    private static final int OFFWIND = 0;

    /**
      *	forwind
      */
    public void forwind() {
        winding = FORWIND;
    }

    /**
      *	rewind
      */
    public void rewind() {
        winding = REWIND;
    }

    /**
      *	rewind
      */
    public void noWinding() {
        winding = OFFWIND;
    }

    private void onWinding() {
        switch(winding) {
            case FORWIND:
                playOffset += 2 * playPacketSize;
                player.goTo(playOffset);
                break;
            case REWIND:
                playOffset -= 3 * playPacketSize;
                if (playOffset < 0) {
                    playOffset = 0;
                    noWinding();
                }
                player.goTo(playOffset);
                break;
        }
    }

    /**
	*	stop playing/looping/recording sound
	*/
    public void stop() {
        noWinding();
        {
            state = STOP;
            if (player != null) {
                player.flush();
                player.stop();
                player = null;
            }
            if (capture != null) {
                capture.stop();
                capture = null;
            }
            Debug.println(3, "...stop");
            fireAudioEvent();
        }
    }

    private ACapture capture;

    private int captureOffset;

    private static int capturePacketSize = 3000;

    private static int capturePointerAdjustment = -40000;

    public static void setCaptureBlockSize(int s) {
        capturePacketSize = s;
    }

    public static int getCaptureBlockSize() {
        return capturePacketSize;
    }

    public static void setCapturePointerAdjustment(int s) {
        capturePointerAdjustment = s;
    }

    public static int getCapturePointerAdjustment() {
        return capturePointerAdjustment;
    }

    /**
	*	start record sound
	*/
    public void rec() throws AudioException {
        if (state == STOP) {
            captureOffset = getLoopStartPointer();
            capture = ACaptureFactory.create(clip, clip.getSelectedLayer());
            Debug.println(3, "start recording...");
            state = REC;
            wakeup();
            fireAudioEvent();
        }
    }

    private synchronized void onRec() {
        try {
            int n;
            if (autoGrowing) {
                n = capturePacketSize;
            } else {
                n = getLoopEndPointer() - captureOffset;
                if (n > capturePacketSize) {
                    n = capturePacketSize;
                }
            }
            int r = capture.read(captureOffset, n);
            clip.getSelectedLayer().markChange();
            Debug.println(8, "capture sample " + captureOffset + ", packet size " + n + " read " + r);
            captureOffset += r;
            setPlayPointer(captureOffset + capturePointerAdjustment);
            if (looping) {
                if ((n < capturePacketSize) || (r < 0)) {
                    captureOffset = getLoopStartPointer();
                    Debug.println(3, "...loop back");
                }
            } else {
                if ((n < capturePacketSize) || (r < 0)) {
                    state = STOP;
                    capture.stop();
                    Debug.println(3, "...autostop capture");
                    fireAudioEvent();
                }
            }
        } catch (Exception ioe) {
            Debug.println(3, "...recording problem!!!");
            state = STOP;
            capture.stop();
            fireAudioEvent();
            Debug.printStackTrace(5, ioe);
        }
    }

    /**
	*	start pause. Pause is terminated by playing or looping further.
	*/
    private int stateBeforePause;

    public void pause() {
        if (state != PAUSE) {
            stateBeforePause = state;
            if (player != null) {
                player.flush();
            }
            state = PAUSE;
            Debug.println(3, "...pause...");
        } else {
            state = stateBeforePause;
            wakeup();
            Debug.println(3, "...continue...");
        }
        fireAudioEvent();
    }

    public boolean isActive() {
        switch(state) {
            case PLAY:
            case PAUSE:
            case REC:
                return true;
        }
        return false;
    }

    public int getState() {
        return state;
    }

    private AudioListener listener;

    /**
	 *	set unique audio-listener
	 */
    public void setAudioListener(AudioListener l) {
        listener = l;
    }

    private void fireAudioEvent() {
        listener.onStateChange(getState());
    }

    public float getPeakLevel(int channelIndex) {
        float peak = 0;
        float actual = 0;
        switch(state) {
            case PLAY:
                {
                    int max = clip.getMaxSampleLength();
                    for (int i = playPointer; i < playPointer + playPacketSize; i++) {
                        if (player != null) {
                            actual = Math.abs(player.getSample(channelIndex, i % max));
                        }
                        if (actual > peak) {
                            peak = actual;
                        }
                    }
                    return peak;
                }
            case REC:
                {
                    AChannel s = clip.getSelectedLayer().getChannel(channelIndex);
                    int max = clip.getSelectedLayer().getMaxSampleLength();
                    for (int i = playPointer; i < playPointer + capturePacketSize; i++) {
                        actual = Math.abs(s.getSample(i % max));
                        if (actual > peak) {
                            peak = actual;
                        }
                    }
                    return peak;
                }
        }
        return 0;
    }

    private int playPointer;

    private int loopStartPointer;

    private int loopEndPointer;

    private int limitLoopPointerRange(int p) {
        int mtl = clip.getMaxSampleLength();
        if (p < 0) return 0; else if (p > mtl - 1) return mtl - 1; else return p;
    }

    /**
	*	set play pointer
	*/
    public void setPlayPointer(int p) {
        playPointer = limitLoopPointerRange(p);
    }

    /**
	*	get play pointer
	*/
    public int getPlayPointer() {
        return playPointer;
    }

    /**
	*	set loop start pointer
	*/
    public void setLoopStartPointer(int p) {
        loopStartPointer = limitLoopPointerRange(p);
        if (loopStartPointer > loopEndPointer) loopEndPointer = loopStartPointer;
    }

    /**
	*	get loop start pointer
	*/
    public int getLoopStartPointer() {
        return loopStartPointer;
    }

    /**
	*	set loop end pointer
	*/
    public void setLoopEndPointer(int p) {
        loopEndPointer = limitLoopPointerRange(p);
        if (loopEndPointer < loopStartPointer) loopStartPointer = loopEndPointer;
    }

    /**
	*	get loop end pointer
	*/
    public int getLoopEndPointer() {
        return loopEndPointer;
    }

    /**
	 *	rearrange loop-pointer to valid values
	 */
    public void limitLoopPointers() {
        setLoopStartPointer(getLoopStartPointer());
        setLoopEndPointer(getLoopEndPointer());
    }

    protected AudioPlotter plotter;

    /**
	*	returns the plotter
	*/
    public AudioPlotter getPlotter() {
        return plotter;
    }
}
