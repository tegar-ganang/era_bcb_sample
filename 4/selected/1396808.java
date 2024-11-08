package com.benkyou.client.systems;

import com.benkyou.client.GameClient;
import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.scene.Spatial;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Austin Allman
 */
public class AnimationSystem implements AnimEventListener {

    private AnimChannel channel;

    private AnimControl control;

    private Spatial player;

    private boolean Crouch, Fly, Handshake, Hover, Point, Run, SitOnGround, SitOnGround1, SitOnObject, Stand, Walk, Walk2, Walk3, Wave, WaveWholeBody;

    /**
     * 
     */
    public static enum anim {

        /**
         * 
         */
        Crouch, /**
         * 
         */
        Fly, /**
         * 
         */
        Handshake, /**
         * 
         */
        Hover, /**
         * 
         */
        Point, /**
         * 
         */
        Run, /**
         * 
         */
        SitOnGround, /**
         * 
         */
        SitOnGround1, /**
         * 
         */
        SitOnObject, /**
         * 
         */
        Stand, /**
         * 
         */
        Walk, /**
         * 
         */
        Walk2, /**
         * 
         */
        Walk3, /**
         * 
         */
        Wave, /**
         * 
         */
        WaveWholeBody
    }

    ;

    /**
     * 
     */
    public AnimationSystem() {
        Crouch = false;
        Fly = false;
        Handshake = false;
        Hover = false;
        Point = false;
        Run = false;
        SitOnGround = false;
        SitOnGround1 = false;
        SitOnObject = false;
        Stand = false;
        Walk = false;
        Walk2 = false;
        Walk3 = false;
        Wave = false;
        WaveWholeBody = false;
    }

    /**
     * 
     * @param player
     */
    public AnimationSystem(Spatial player) {
        this.player = player;
    }

    /**
     * 
     */
    public void setupAnimations() {
        control = player.getControl(AnimControl.class);
        control.addListener(this);
        channel = control.createChannel();
        toggleAnim("Stand");
    }

    /**
     * 
     * @param a
     * @return
     */
    public AnimationSystem animate(anim a) {
        String animation = a.toString();
        toggleAnim(animation);
        return this;
    }

    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
        if (animName.equalsIgnoreCase("Walk")) {
            toggleAnim("Walk");
        }
    }

    private Boolean toggleAnim(String animName) {
        if (animName.equals("Crouch")) {
            return Crouch ? (Crouch = false) : (Crouch = true);
        }
        if (animName.equals("Fly")) {
            return Fly ? (Fly = false) : (Fly = true);
        }
        if (animName.equals("Handshake")) {
            return Handshake ? (Handshake = false) : (Handshake = true);
        }
        if (animName.equals("Hove")) {
            return Hover ? (Hover = false) : (Hover = true);
        }
        if (animName.equals("Point")) {
            return Point ? (Point = false) : (Point = true);
        }
        if (animName.equals("Run")) {
            channel.setAnim(animName);
            channel.setLoopMode(LoopMode.Loop);
            channel.setSpeed(1f);
            return Run ? (Run = false) : (Run = true);
        }
        if (animName.equals("SitOnGround")) {
            if (isSitOnGround()) {
                toggleAnim("Stand");
                return SitOnGround ? (SitOnGround = false) : (SitOnGround = true);
            } else {
                channel.setAnim(animName);
                channel.setLoopMode(LoopMode.DontLoop);
                channel.setSpeed(2f);
            }
            return SitOnGround ? (SitOnGround = false) : (SitOnGround = true);
        }
        if (animName.equals("SitOnGround1")) {
            return SitOnGround1 ? (SitOnGround1 = false) : (SitOnGround1 = true);
        }
        if (animName.equals("SitOnObject")) {
            return SitOnObject ? (SitOnObject = false) : (SitOnObject = true);
        }
        if (animName.equals("Walk")) {
            channel.setAnim(animName);
            channel.setLoopMode(LoopMode.Loop);
            channel.setSpeed(1f);
            return Walk ? (Walk = false) : (Walk = true);
        }
        if (animName.equals("Walk2")) {
            return Walk2 ? (Walk2 = false) : (Walk2 = true);
        }
        if (animName.equals("Walk3")) {
            return Walk3 ? (Walk3 = false) : (Walk3 = true);
        }
        if (animName.equals("Wave")) {
            return Wave ? (Wave = false) : (Wave = true);
        }
        if (animName.equals("WaveWholeBody")) {
            return WaveWholeBody ? (WaveWholeBody = false) : (WaveWholeBody = true);
        }
        if (animName.equalsIgnoreCase("Stand")) {
            channel.setAnim("Stand", 0.5f);
            channel.setLoopMode(LoopMode.DontLoop);
            channel.setSpeed(1f);
            return WaveWholeBody ? (WaveWholeBody = false) : (WaveWholeBody = true);
        }
        return false;
    }

    public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
    }

    /**
     * @return the channel
     */
    public AnimChannel getChannel() {
        return channel;
    }

    /**
     * @return the control
     */
    public AnimControl getControl() {
        return control;
    }

    /**
     * @return the Crouch
     */
    public boolean isCrouch() {
        return Crouch;
    }

    /**
     * @return the Fly
     */
    public boolean isFly() {
        return Fly;
    }

    /**
     * @return the Handshake
     */
    public boolean isHandshake() {
        return Handshake;
    }

    /**
     * @return the Hover
     */
    public boolean isHover() {
        return Hover;
    }

    /**
     * @return the Point
     */
    public boolean isPoint() {
        return Point;
    }

    /**
     * @return the Run
     */
    public boolean isRun() {
        return Run;
    }

    /**
     * @return the SitOnGround
     */
    public boolean isSitOnGround() {
        return SitOnGround;
    }

    /**
     * @return the SitOnGround1
     */
    public boolean isSitOnGround1() {
        return SitOnGround1;
    }

    /**
     * @return the SitOnObject
     */
    public boolean isSitOnObject() {
        return SitOnObject;
    }

    /**
     * @return the Stand
     */
    public boolean isStand() {
        return Stand;
    }

    /**
     * @return the Walk
     */
    public boolean isWalk() {
        return Walk;
    }

    /**
     * @return the Walk2
     */
    public boolean isWalk2() {
        return Walk2;
    }

    /**
     * @return the Walk3
     */
    public boolean isWalk3() {
        return Walk3;
    }

    /**
     * @return the Wave
     */
    public boolean isWave() {
        return Wave;
    }

    /**
     * @return the WaveWholeBody
     */
    public boolean isWaveWholeBody() {
        return WaveWholeBody;
    }
}
