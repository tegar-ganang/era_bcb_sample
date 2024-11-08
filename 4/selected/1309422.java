package jp.locky.stumbler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

/**
 * LockyStumbler�̉����������N���X
 * @author Hiroshi Yoshida
 */
public class LockyStumblerSound implements Runnable {

    private int type_ = 1;

    private String soundFileDirectory_ = "." + File.separator + "sound";

    private ArrayList<String> playList_;

    private Player player_;

    private boolean run_ = false;

    /**
	 * �W���R���X�g���N�^
	 */
    public LockyStumblerSound() {
        playList_ = new ArrayList<String>(30);
    }

    /**
	 * �T�E���h�t�@�C���̃f�B���N�g����ݒ肷��R���X�g���N�^
	 * @param directory �T�E���h�t�@�C����u�����f�B���N�g��
	 */
    public LockyStumblerSound(String directory) {
        soundFileDirectory_ = directory;
        playList_ = new ArrayList<String>(30);
    }

    public static void Alert() {
        try {
            File soundFile = new File("." + File.separator + "sound" + File.separator + "ALERT.wav");
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundFile);
            AudioFormat format = ais.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            int readBytes = 0;
            byte[] data = new byte[128000];
            while (readBytes != -1) {
                readBytes = ais.read(data, 0, data.length);
                if (0 <= readBytes) {
                    line.write(data, 0, readBytes);
                }
            }
            line.drain();
            line.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        } catch (LineUnavailableException exception) {
            exception.printStackTrace();
        } catch (UnsupportedAudioFileException exception) {
            exception.printStackTrace();
        }
    }

    public int getSoundType() {
        return type_;
    }

    public static void GPSConnect() {
        try {
            File soundFile = new File("." + File.separator + "sound" + File.separator + "GPS Connect.wav");
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundFile);
            AudioFormat format = ais.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            int readBytes = 0;
            byte[] data = new byte[128000];
            while (readBytes != -1) {
                readBytes = ais.read(data, 0, data.length);
                if (0 <= readBytes) {
                    line.write(data, 0, readBytes);
                }
            }
            line.drain();
            line.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        } catch (LineUnavailableException exception) {
            exception.printStackTrace();
        } catch (UnsupportedAudioFileException exception) {
            exception.printStackTrace();
        }
    }

    public static void GPSDisconnect() {
        try {
            File soundFile = new File("." + File.separator + "sound" + File.separator + "GPS Disconnect.wav");
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundFile);
            AudioFormat format = ais.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            int readBytes = 0;
            byte[] data = new byte[128000];
            while (readBytes != -1) {
                readBytes = ais.read(data, 0, data.length);
                if (0 <= readBytes) {
                    line.write(data, 0, readBytes);
                }
            }
            line.drain();
            line.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        } catch (LineUnavailableException exception) {
            exception.printStackTrace();
        } catch (UnsupportedAudioFileException exception) {
            exception.printStackTrace();
        }
    }

    public static void GPSTimeout() {
        try {
            File soundFile = new File("." + File.separator + "sound" + File.separator + "GPS Timeout.wav");
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundFile);
            AudioFormat format = ais.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            int readBytes = 0;
            byte[] data = new byte[128000];
            while (readBytes != -1) {
                readBytes = ais.read(data, 0, data.length);
                if (0 <= readBytes) {
                    line.write(data, 0, readBytes);
                }
            }
            line.drain();
            line.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        } catch (LineUnavailableException exception) {
            exception.printStackTrace();
        } catch (UnsupportedAudioFileException exception) {
            exception.printStackTrace();
        }
    }

    public static void LogStart() {
        try {
            File soundFile = new File("." + File.separator + "sound" + File.separator + "START.wav");
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundFile);
            AudioFormat format = ais.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            int readBytes = 0;
            byte[] data = new byte[128000];
            while (readBytes != -1) {
                readBytes = ais.read(data, 0, data.length);
                if (0 <= readBytes) {
                    line.write(data, 0, readBytes);
                }
            }
            line.drain();
            line.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        } catch (LineUnavailableException exception) {
            exception.printStackTrace();
        } catch (UnsupportedAudioFileException exception) {
            exception.printStackTrace();
        }
    }

    public static void LogStop() {
        try {
            File soundFile = new File("." + File.separator + "sound" + File.separator + "STOP.wav");
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundFile);
            AudioFormat format = ais.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            int readBytes = 0;
            byte[] data = new byte[128000];
            while (readBytes != -1) {
                readBytes = ais.read(data, 0, data.length);
                if (0 <= readBytes) {
                    line.write(data, 0, readBytes);
                }
            }
            line.drain();
            line.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        } catch (LineUnavailableException exception) {
            exception.printStackTrace();
        } catch (UnsupportedAudioFileException exception) {
            exception.printStackTrace();
        }
    }

    /**
	 * ��ɗ^����ꂽ�^�C�g�����Đ�����
	 * @param title
	 */
    public void play(String title) {
        try {
            FileInputStream fis = new FileInputStream(new File(soundFileDirectory_ + File.separator + title));
            stop();
            player_ = new Player(fis);
            player_.play();
            fis.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        } catch (JavaLayerException exception) {
            exception.printStackTrace();
        }
    }

    /**
	 * �v���C���X�g�ɓo�^����Ă���T�E���h�t�@�C���������_���ɍĐ�����
	 */
    public void randomPlay() {
        run_ = true;
        new Thread(this).start();
    }

    public void run() {
        try {
            while (run_) {
                int index = (int) System.currentTimeMillis() % playList_.size();
                FileInputStream fis = new FileInputStream(new File(playList_.get(index)));
                player_ = new Player(fis);
                player_.play();
                fis.close();
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        } catch (JavaLayerException exception) {
            exception.printStackTrace();
        }
    }

    public void setPlayList(String directory) {
        File dir = new File(directory);
        File[] files = dir.listFiles();
        playList_.clear();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().endsWith(".mp3")) {
                    playList_.add(files[i].getPath());
                }
            }
        }
    }

    public void setSoundFileDirectory(String directory) {
        soundFileDirectory_ = directory;
        setPlayList(directory);
    }

    public void setSoundType(int type) {
        type_ = type;
    }

    /**
	 * �Đ����̃T�E���h���~����
	 */
    public void stop() {
        run_ = false;
        if (player_ != null) {
            player_.close();
        }
    }

    public static void WiFiFound() {
        try {
            File soundFile = new File("." + File.separator + "sound" + File.separator + "WiFi Found.wav");
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundFile);
            AudioFormat format = ais.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            int readBytes = 0;
            byte[] data = new byte[128000];
            while (readBytes != -1) {
                readBytes = ais.read(data, 0, data.length);
                if (0 <= readBytes) {
                    line.write(data, 0, readBytes);
                }
            }
            line.drain();
            line.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        } catch (LineUnavailableException exception) {
            exception.printStackTrace();
        } catch (UnsupportedAudioFileException exception) {
            exception.printStackTrace();
        }
    }

    public static void WiFiNotFound() {
        try {
            File soundFile = new File("." + File.separator + "sound" + File.separator + "WiFi Not Found.wav");
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundFile);
            AudioFormat format = ais.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            int readBytes = 0;
            byte[] data = new byte[128000];
            while (readBytes != -1) {
                readBytes = ais.read(data, 0, data.length);
                if (0 <= readBytes) {
                    line.write(data, 0, readBytes);
                }
            }
            line.drain();
            line.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        } catch (LineUnavailableException exception) {
            exception.printStackTrace();
        } catch (UnsupportedAudioFileException exception) {
            exception.printStackTrace();
        }
    }
}
