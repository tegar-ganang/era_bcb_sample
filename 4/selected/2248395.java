package ch.unibe.inkml.util;

import ch.unibe.inkml.InkChannel;
import ch.unibe.inkml.InkMLComplianceException;
import ch.unibe.inkml.InkTracePoint;

public class BooleanFormatter extends Formatter {

    public BooleanFormatter(InkChannel c) {
        super(c);
    }

    @Override
    public String getNext(InkTracePoint sp) {
        return (sp.get(getChannel().getName()) > 0.5) ? "T" : "F";
    }

    public String getNext(double next) {
        return valueOf(next);
    }

    protected String valueOf(double n) {
        return (n > 0.5) ? "T" : "F";
    }

    /**
     * {@inheritDoc}
     * @throws InkMLComplianceException 
     */
    @Override
    public double consume(String result) throws InkMLComplianceException {
        if (result.equals("T")) {
            this.setLastValue(1.0);
            return 1.0;
        } else if (result.equals("F")) {
            this.setLastValue(0.0);
            return 0.0;
        } else if (result.equals("*")) {
            if (!hasLastValue()) {
                throw new InkMLComplianceException("For this channel the value '*'  is not accepted in the first place");
            }
            return getLastValue();
        } else if (!this.getChannel().isIntermittent()) {
            throw new InkMLComplianceException("A non intermittent boolean channel only accepts T or F or '*'");
        } else if (result.equals("?")) {
            unsetLastValue();
            return Double.NaN;
        } else {
            throw new InkMLComplianceException("A boolean channel accepts 'T' or 'F' or '?' or '*' or it can be left out when no other value is following");
        }
    }
}
