package be.lassi.cues;

import static be.lassi.lanbox.domain.FadeType.SPLIT_FADE_IN;
import static be.lassi.lanbox.domain.FadeType.SPLIT_FADE_OUT;
import static be.lassi.lanbox.domain.Time.FOREVER;
import static be.lassi.util.Util.newArrayList;
import java.util.List;
import be.lassi.lanbox.cuesteps.CuePreviousScene;
import be.lassi.lanbox.cuesteps.CueScene;
import be.lassi.lanbox.cuesteps.CueStep;
import be.lassi.lanbox.cuesteps.WaitLayer;
import be.lassi.lanbox.domain.ChannelChanges;
import be.lassi.lanbox.domain.FadeType;
import be.lassi.lanbox.domain.Time;

public class LightCueStepsFactory {

    private static final int IN = 1;

    private static final int OUT = 2;

    private final LightCueDetail detail;

    private final Timing timing;

    private final int fadeInTime;

    private final int fadeOutTime;

    private final int fadeInDelay;

    private final int fadeOutDelay;

    private final int waitTime;

    private final List<CueStep> cueSteps = newArrayList();

    public LightCueStepsFactory(final LightCueDetail detail) {
        this.detail = detail;
        timing = detail.getTiming();
        fadeInTime = timing.getFadeInTime().getMillis();
        fadeOutTime = timing.getFadeOutTime().getMillis();
        fadeInDelay = timing.getFadeInDelay().getMillis();
        fadeOutDelay = timing.getFadeOutDelay().getMillis();
        waitTime = timing.getWaitTime().getMillis();
        init();
    }

    public List<CueStep> getCueSteps() {
        return cueSteps;
    }

    private void init() {
        if (waitTime > 0) {
            hold(timing.getWaitTime());
        }
        if (fadeInDelay == fadeOutDelay) {
            if (fadeInDelay > 0) {
                hold(timing.getFadeInDelay());
            }
            if (!timing.isSplitFade()) {
                crossFade();
            } else {
                if (fadeInTime < fadeOutTime) {
                    fade1(IN, Time.get(1));
                    fade2(OUT);
                } else {
                    fade1(OUT, Time.get(1));
                    fade2(IN);
                }
            }
        } else {
            if (fadeInDelay < fadeOutDelay) {
                if (fadeInDelay > 0) {
                    hold(timing.getFadeInDelay());
                }
                Time holdTime = Time.fromMillis(fadeOutDelay - fadeInDelay);
                fade1(IN, holdTime);
                fade2(OUT);
            } else {
                if (fadeOutDelay > 0) {
                    hold(timing.getFadeOutDelay());
                }
                Time holdTime = Time.fromMillis(fadeInDelay - fadeOutDelay);
                fade1(OUT, holdTime);
                fade2(IN);
            }
        }
    }

    private void hold(final Time time) {
        cueSteps.add(new WaitLayer(time));
    }

    private void crossFade() {
        fade(true, FadeType.CROSS_FADE, timing.getFadeInTime(), detail.getChannelChanges());
    }

    private void fade(final boolean hold, final FadeType fadeType, final Time fadeTime, final ChannelChanges changes) {
        Time holdTime = Time.get(1);
        if (hold) {
            holdTime = timing.getHoldTime();
            if (holdTime != FOREVER) {
                holdTime = holdTime.plus(fadeTime);
            }
        }
        if (changes.size() > 0) {
            cueSteps.add(new CueScene(fadeType, fadeTime, holdTime, changes));
        } else {
            hold(holdTime);
        }
    }

    private void fade1(final int mode, final Time holdTime) {
        FadeType fadeType = null;
        Time fadeTime = null;
        if (mode == IN) {
            fadeType = SPLIT_FADE_IN;
            fadeTime = timing.getFadeInTime();
        } else {
            fadeType = SPLIT_FADE_OUT;
            fadeTime = timing.getFadeOutTime();
        }
        cueSteps.add(new CueScene(fadeType, fadeTime, holdTime, detail.getChannelChanges()));
    }

    private void fade2(final int mode) {
        FadeType fadeType = null;
        Time fadeTime = null;
        Time holdTime = timing.getHoldTime();
        if (mode == IN) {
            fadeType = SPLIT_FADE_IN;
            fadeTime = timing.getFadeInTime();
            if (holdTime != FOREVER) {
                holdTime = holdTime.plus(timing.getFadeInTime());
            }
        } else {
            fadeType = SPLIT_FADE_OUT;
            fadeTime = timing.getFadeOutTime();
            if (holdTime != FOREVER) {
                holdTime = holdTime.plus(timing.getFadeInTime());
            }
        }
        cueSteps.add(new CuePreviousScene(fadeType, fadeTime, holdTime));
    }
}
