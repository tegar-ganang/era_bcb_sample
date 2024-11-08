package org.iqual.chaplin.example.dci.transfer;

import org.iqual.chaplin.FromContext;

public abstract class MoneySourceTrait implements MoneySource {

    @FromContext
    private GUI gui;

    @FromContext
    private TransactionManager tm;

    public void transferMoneyTo(MoneySink sink, int amount) throws InsufficientFundsException {
        tm.begin();
        boolean failed = false;
        try {
            if (availableBalance() - amount < 0) {
                failed = true;
                throw new InsufficientFundsException("Available balance:" + availableBalance());
            }
            withdraw(amount);
            sink.transferFrom(this, amount);
            gui.message("Transferred " + amount);
        } finally {
            tm.end(failed);
        }
    }

    @FromContext
    protected abstract int availableBalance();

    @FromContext
    protected abstract void withdraw(int amount);
}
