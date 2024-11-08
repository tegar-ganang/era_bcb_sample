package org.iqual.chaplin.example.dci.transfer;

import org.iqual.chaplin.FromContext;

public abstract class MoneySinkTrait implements MoneySink {

    @FromContext
    private GUI gui;

    public void transferFrom(MoneySource source, int amount) {
        deposit(amount);
        gui.message("Deposited " + amount);
    }

    @FromContext
    protected abstract void deposit(int amount);
}
