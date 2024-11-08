package com.frinika.videosynth;

import com.frinika.videosynth.transition.LinearTransition;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.sound.midi.MidiChannel;

/**
 *
 * @author Peter Johan Salomonsen
 */
public class ImageMovieViewerChannel implements MidiChannel {

    BufferedImage currentImage = null;

    int imageIndex = 1;

    private boolean playing = false;

    private int referenceFPS = 24;

    private int fps = 24;

    private long referenceTime = 0;

    private int referenceImageIndex = 1;

    private String imagePath;

    private float alpha = 1f;

    LinearTransition alphaTransition = null;

    FrinikaVideoSynth frinikaVideoSynth;

    ImageMovieViewerChannel(String imagePath, FrinikaVideoSynth frinikaVideoSynth) {
        this.imagePath = imagePath;
        this.frinikaVideoSynth = frinikaVideoSynth;
        try {
            currentImage = ImageIO.read(new URL(imagePath + imageIndex + ".jpg"));
        } catch (Exception ex) {
            Logger.getLogger(ImageMovieViewerChannel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void paint(Graphics2D g) {
        if (currentImage != null & alpha > 0f) {
            AffineTransform af = AffineTransform.getScaleInstance((double) frinikaVideoSynth.getWidth() / currentImage.getWidth(), (double) frinikaVideoSynth.getHeight() / currentImage.getHeight());
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.drawImage(currentImage, new AffineTransformOp(af, null), 0, 0);
        }
    }

    public void play(long referenceTime) {
        this.referenceTime = referenceTime;
        referenceImageIndex = imageIndex;
        this.playing = true;
    }

    public void stop() {
        this.playing = false;
    }

    public boolean isPlaying() {
        return playing;
    }

    public int getImageIndex() {
        return imageIndex;
    }

    public void setImageIndex(int imageIndex, long referenceTime) {
        this.imageIndex = imageIndex;
        referenceImageIndex = imageIndex;
        this.referenceTime = referenceTime;
    }

    public void setMilliSecondPos(int milliSecondPos, long referenceTime) {
        setImageIndex((milliSecondPos * referenceFPS / 1000), referenceTime);
    }

    public int getMilliSecondPos() {
        return getImageIndex() * 1000 / referenceFPS;
    }

    private void updateImageIndex(long time) {
        if (playing) {
            imageIndex = referenceImageIndex + (int) (((time - referenceTime) * fps) / 1000);
        }
    }

    public void updateImage(long time) {
        try {
            int currentImageIndex = getImageIndex();
            updateImageIndex(time);
            if (currentImageIndex != getImageIndex()) {
                currentImage = ImageIO.read(new URL(imagePath + imageIndex + ".jpg"));
            }
        } catch (Exception ex) {
            Logger.getLogger(ImageMovieViewerChannel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void noteOn(int noteNumber, int velocity) {
        long refTime = frinikaVideoSynth.getMicrosecondPosition() / 1000;
        setMilliSecondPos((int) (1000 * noteNumber + ((velocity - 1.0) / 100.0)), refTime);
        play(refTime);
    }

    @Override
    public void noteOff(int noteNumber, int velocity) {
        stop();
    }

    @Override
    public void noteOff(int noteNumber) {
        stop();
    }

    @Override
    public void setPolyPressure(int noteNumber, int pressure) {
    }

    @Override
    public int getPolyPressure(int noteNumber) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setChannelPressure(int pressure) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getChannelPressure() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void controlChange(int controller, int value) {
        switch(controller) {
            case 7:
                alpha = value / 127f;
                break;
        }
    }

    @Override
    public int getController(int controller) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void programChange(int program) {
    }

    @Override
    public void programChange(int bank, int program) {
    }

    @Override
    public int getProgram() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setPitchBend(int bend) {
        int millisecondpos = getMilliSecondPos();
        fps = (int) (referenceFPS * Math.pow(2, (bend - 8192) / (16384 / 8)));
        long refTime = frinikaVideoSynth.getMicrosecondPosition() / 1000;
        setMilliSecondPos(millisecondpos, refTime);
    }

    @Override
    public int getPitchBend() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void resetAllControllers() {
    }

    @Override
    public void allNotesOff() {
    }

    @Override
    public void allSoundOff() {
    }

    @Override
    public boolean localControl(boolean on) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setMono(boolean on) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getMono() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setOmni(boolean on) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean getOmni() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setMute(boolean mute) {
    }

    @Override
    public boolean getMute() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setSolo(boolean soloState) {
    }

    @Override
    public boolean getSolo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
