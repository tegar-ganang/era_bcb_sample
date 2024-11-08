package org.maverickdbms.util;

import org.maverickdbms.basic.Session;
import org.maverickdbms.basic.ConstantString;
import org.maverickdbms.basic.MaverickException;
import org.maverickdbms.basic.MaverickString;

public class InputNode implements ParagraphNode {

    private MaverickString result;

    private ConstantString prompt;

    private ConstantString prefix;

    private ConstantString suffix;

    private ParagraphNode listener;

    private ParagraphNode next;

    public InputNode(MaverickString result, ConstantString prompt, ConstantString prefix, ConstantString suffix, ParagraphNode next) {
        this.result = result;
        this.prompt = prompt;
        this.prefix = prefix;
        this.suffix = suffix;
        this.next = next;
    }

    public ParagraphNode getNext() {
        return next;
    }

    public void registerListener(ParagraphNode listener) {
        if (this.listener != null) {
            ParagraphNode tmp = this.listener;
            while (tmp.getNext() != null) {
                tmp = tmp.getNext();
            }
            tmp.setNext(listener);
        } else {
            this.listener = listener;
        }
    }

    public ConstantString run(Session session, MaverickString[] args) throws MaverickException {
        ParagraphNode tmp = listener;
        while (tmp != null) {
            tmp.run(session, args);
            tmp = tmp.getNext();
        }
        MaverickString status = session.getStatus();
        session.getChannel(Session.SCREEN_CHANNEL).PRINT(prompt, false, status);
        session.getInputChannel().INPUT(result, ConstantString.ZERO, true, true, status);
        String s = result.toString();
        result.set(prefix);
        result.append(s);
        result.append(suffix);
        return null;
    }

    public void setNext(ParagraphNode next) {
        this.next = next;
    }
}
