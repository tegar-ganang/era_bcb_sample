package org.speechforge.apps.demos;

import java.io.IOException;
import javax.media.rtp.InvalidSessionAddressException;
import org.apache.log4j.Logger;
import org.asteriskjava.manager.TimeoutException;
import org.mrcp4j.client.MrcpInvocationException;
import org.mrcp4j.message.MrcpEvent;
import org.mrcp4j.message.header.IllegalValueException;
import org.speechforge.cairo.client.recog.RecognitionResult;
import org.speechforge.cairo.client.recog.RuleMatch;
import org.speechforge.zanzibar.speechlet.InvalidContextException;
import org.speechforge.zanzibar.speechlet.Speechlet;
import org.speechforge.zanzibar.telephony.TelephonyClient;
import org.speechforge.cairo.client.NoMediaControlChannelException;
import org.speechforge.cairo.client.SpeechClient;
import org.speechforge.cairo.client.SpeechEventListener;

/**
 * DTMF Demo Speech Application.  Looks for 4 digit dtmf sequences.  Shows the use of regex for pattern matching. (until you say quit or stop).
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class CallXferDemo extends Speechlet implements SpeechEventListener {

    private static Logger _logger = Logger.getLogger(CallXferDemo.class);

    private String prompt;

    private String grammar;

    private String pbxContext;

    SpeechClient sClient;

    TelephonyClient tClient;

    String channelName;

    public CallXferDemo() {
        super();
    }

    protected void runApplication() throws NoMediaControlChannelException, InvalidSessionAddressException {
        try {
            sClient = this.getContext().getSpeechClient();
            tClient = this.getContext().getTelephonyClient();
            channelName = this.getContext().getPBXSession().getChannelName();
            sClient.turnOnBargeIn();
            String result = "";
            sClient.enableDtmf("[0-9]{4}", this, 0, 0);
            while (!stopFlag) {
                _logger.debug("Calling play and Recognize...");
                RecognitionResult r = sClient.playAndRecognizeBlocking(false, result + prompt, grammar, false);
                if ((r != null) && (!r.isOutOfGrammar())) {
                    _logger.debug("Got a result: " + r.getText());
                    String main = null;
                    String phoneType = null;
                    for (RuleMatch rule : r.getRuleMatches()) {
                        _logger.info(rule.getTag() + ":" + rule.getRule());
                        if (rule.getRule().equals("main")) {
                            main = rule.getTag();
                        } else if (rule.getRule().equals("phoneType")) {
                            phoneType = rule.getTag();
                        } else {
                        }
                    }
                    if (main != null) {
                        if (main.equals("QUIT")) {
                            stopFlag = true;
                            try {
                                this.getContext().dialogCompleted();
                            } catch (InvalidContextException e) {
                                e.printStackTrace();
                            }
                        } else {
                            String employee = main;
                            if (phoneType != null) {
                                _logger.info("phonetype is sepecified and it is: " + phoneType);
                                employee = employee + "-" + phoneType;
                            }
                            tClient.redirectBlocking(channelName, pbxContext, employee);
                            stopFlag = true;
                        }
                    } else {
                        _logger.warn("Invalid results, could not intepret the utterance.");
                    }
                } else {
                    _logger.debug("No recognition result...");
                    result = "I did not understand.  ";
                }
            }
        } catch (MrcpInvocationException e) {
            _logger.info("MRCP Response status code is: " + e.getResponse().getStatusCode());
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IllegalValueException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    public void recognitionEventReceived(SpeechEventType event, RecognitionResult r) {
        _logger.info("Recog Event Received: " + event.toString() + "\nResult: " + r.getText());
    }

    public void speechSynthEventReceived(SpeechEventType event) {
        _logger.info("Speech Synth Event Received: " + event.toString());
    }

    public void characterEventReceived(String c, DtmfEventType status) {
        _logger.info("Character Event.  Status is " + status + ".  Code is " + c);
        if (c != null) {
            try {
                tClient.redirectBlocking(channelName, pbxContext, c);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
            stopFlag = true;
        } else {
            _logger.warn("Could not determine a phone number for the employee");
        }
    }

    /**
     * @return the grammar
     */
    public String getGrammar() {
        return grammar;
    }

    /**
     * @param grammar the grammar to set
     */
    public void setGrammar(String grammar) {
        this.grammar = grammar;
    }

    /**
     * @return the prompt
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * @param prompt the prompt to set
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * @return the pbxContext
     */
    public String getPbxContext() {
        return pbxContext;
    }

    /**
     * @param pbxContext the pbxContext to set
     */
    public void setPbxContext(String pbxContext) {
        this.pbxContext = pbxContext;
    }
}
