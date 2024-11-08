package phworld;

import dsc.netgame.*;
import java.util.*;

public class AutotransferWCM extends PlanetActivityWCM {

    private int transferTo;

    public AutotransferWCM(int transferFrom, int transferTo) {
        super(transferFrom);
        this.transferTo = transferTo;
    }

    public MaterialBundle getMaterials() {
        return null;
    }

    public int getTransferTarget() {
        return transferTo;
    }
}
