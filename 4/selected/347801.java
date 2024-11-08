package org.tritonus.test.api.midi.synthesizer;

import javax.sound.midi.Synthesizer;

/**	Tests for class javax.sound.midi.Synthesizer.
 */
public class IllegalStateTestCase extends BaseSynthesizerTestCase {

    public IllegalStateTestCase(String strName) {
        super(strName);
    }

    protected void checkSynthesizer(Synthesizer synth) throws Exception {
        checkMethods(synth, false);
        synth.open();
        checkMethods(synth, true);
        synth.close();
    }

    private void checkMethods(Synthesizer synth, boolean bOpen) throws Exception {
        boolean bExpectingException = false;
        checkMethod(synth, "getMaxPolyphony()", bExpectingException, bOpen);
        checkMethod(synth, "getLatency()", bExpectingException, bOpen);
        checkMethod(synth, "getChannels()", bExpectingException, bOpen);
        checkMethod(synth, "getVoiceStatus()", bExpectingException, bOpen);
        checkMethod(synth, "getDefaultSoundbank()", bExpectingException, bOpen);
        checkMethod(synth, "getAvailableInstruments()", bExpectingException, bOpen);
        checkMethod(synth, "getLoadedInstruments()", bExpectingException, bOpen);
    }

    private void checkMethod(Synthesizer synth, String strMethodName, boolean bExceptionExpected, boolean bOpen) throws Exception {
        try {
            if ("getMaxPolyphony()".equals(strMethodName)) synth.getMaxPolyphony(); else if ("getLatency()".equals(strMethodName)) synth.getLatency(); else if ("getChannels()".equals(strMethodName)) synth.getChannels(); else if ("getVoiceStatus()".equals(strMethodName)) synth.getVoiceStatus(); else if ("getDefaultSoundbank()".equals(strMethodName)) synth.getDefaultSoundbank(); else if ("getAvailableInstruments()".equals(strMethodName)) synth.getAvailableInstruments(); else if ("getLoadedInstruments()".equals(strMethodName)) synth.getLoadedInstruments(); else throw new RuntimeException("unknown method name");
            if (bExceptionExpected) {
                fail(constructErrorMessage(synth, strMethodName, bExceptionExpected, bOpen));
            }
        } catch (IllegalStateException e) {
            if (!bExceptionExpected) {
                fail(constructErrorMessage(synth, strMethodName, bExceptionExpected, bOpen));
            }
        }
    }

    private static String constructErrorMessage(Synthesizer synth, String strMethodName, boolean bExceptionExpected, boolean bOpen) {
        String strMessage = ": IllegalStateException ";
        strMessage += (bExceptionExpected ? "not thrown" : "thrown");
        strMessage += " on " + strMethodName;
        return BaseSynthesizerTestCase.constructErrorMessage(synth, strMessage, bOpen);
    }
}
