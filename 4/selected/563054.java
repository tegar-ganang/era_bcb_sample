package org.posterita.businesslogic;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.MBankAccount;
import org.compiere.model.MCash;
import org.compiere.model.MCashBook;
import org.compiere.model.MCashLine;
import org.compiere.process.DocumentEngine;
import org.posterita.exceptions.OperationException;
import org.posterita.util.PoManager;

/**
 * @author Ashley G Ramdass
 * May 26, 2008
 */
public class CashTransferManager {

    /**
     * Transfers money from a Bank Account to another one.
     * 
     * @param ctx Context
     * @param fromBankAccountId From Bank Account
     * @param toBankAccountId To Bank Account
     * @param currencyId Currency
     * @param amount Amount to Transfer
     * @param dateAcct Accounting Date
     * @param trxName Transaction
     * @throws OperationException if documents can't be created or processed
     */
    public static void transferBankToBank(Properties ctx, int fromBankAccountId, int toBankAccountId, int currencyId, BigDecimal amount, Timestamp dateAcct, String trxName) throws OperationException {
        MBankAccount fromBankAccount = new MBankAccount(ctx, fromBankAccountId, trxName);
        MBankAccount toBankAccount = new MBankAccount(ctx, toBankAccountId, trxName);
        int fromCashBookId = OrganisationManager.getCreateTransferCashBook(ctx, fromBankAccount.getAD_Org_ID(), currencyId, trxName);
        int toCashBookId = OrganisationManager.getCreateTransferCashBook(ctx, toBankAccount.getAD_Org_ID(), currencyId, trxName);
        MCash fromCashJournal = MCash.get(ctx, fromCashBookId, dateAcct, trxName);
        if (fromCashJournal == null) {
            throw new OperationException("Could not create Cash Journal entry");
        }
        MCash toCashJournal = MCash.get(ctx, toCashBookId, dateAcct, trxName);
        if (toCashJournal == null) {
            throw new OperationException("Could not create Cash Journal entry");
        }
        String description = fromBankAccount.getAccountNo() + " (BA) -> " + toBankAccount.getAccountNo() + " (BA)";
        MCashLine fromCashLine = new MCashLine(fromCashJournal);
        fromCashLine.setC_Currency_ID(currencyId);
        fromCashLine.setDescription(description);
        fromCashLine.setCashType(MCashLine.CASHTYPE_BankAccountTransfer);
        fromCashLine.setC_BankAccount_ID(fromBankAccountId);
        fromCashLine.setAmount(amount);
        fromCashLine.setIsGenerated(true);
        PoManager.save(fromCashLine);
        MCashLine toCashLine = new MCashLine(toCashJournal);
        toCashLine.setC_Currency_ID(currencyId);
        toCashLine.setDescription(description);
        toCashLine.setCashType(MCashLine.CASHTYPE_BankAccountTransfer);
        toCashLine.setC_BankAccount_ID(toBankAccountId);
        toCashLine.setAmount(amount.negate());
        toCashLine.setIsGenerated(true);
        PoManager.save(toCashLine);
        PoManager.processIt(fromCashJournal, DocumentEngine.ACTION_Complete);
        if (fromCashJournal.get_ID() != toCashJournal.get_ID()) {
            PoManager.processIt(toCashJournal, DocumentEngine.ACTION_Complete);
        }
    }

    /**
     * Transfer money from a Cash Book to another
     * @param ctx Context
     * @param fromCashBookId From Cash Book
     * @param toCashBookId To Cash Book
     * @param currencyId Currency
     * @param amount Amount to transfer
     * @param dateAcct Accounting Date
     * @param trxName Transaction
     * @throws OperationException If documents can't be created or processed
     */
    public static void transferCashBookToCashBook(Properties ctx, int fromCashBookId, int toCashBookId, int currencyId, BigDecimal amount, Timestamp dateAcct, String trxName) throws OperationException {
        MCash fromJournal = MCash.get(ctx, fromCashBookId, dateAcct, trxName);
        transferFromJournalToCashBook(ctx, fromJournal, toCashBookId, currencyId, amount, dateAcct, trxName);
    }

    /**
     * Transfer money from a Cash Book to another
     * @param ctx Context
     * @param fromCashBookId From Cash Book
     * @param toCashBookId To Cash Book
     * @param currencyId Currency
     * @param amount Amount to transfer
     * @param dateAcct Accounting Date
     * @param trxName Transaction
     * @throws OperationException If documents can't be created or processed
     */
    public static void transferFromJournalToCashBook(Properties ctx, MCash cashJournal, int toCashBookId, int currencyId, BigDecimal amount, Timestamp dateAcct, String trxName) throws OperationException {
        MCashBook fromCashBook = new MCashBook(ctx, cashJournal.getC_CashBook_ID(), trxName);
        MCashBook toCashBook = new MCashBook(ctx, toCashBookId, trxName);
        int fromBankAccountId = OrganisationManager.getCreateTransferBankAccount(ctx, fromCashBook.getAD_Org_ID(), currencyId, null);
        int toBankAccountId = OrganisationManager.getCreateTransferBankAccount(ctx, toCashBook.getAD_Org_ID(), currencyId, null);
        MCash toCash = MCash.get(ctx, toCashBookId, dateAcct, trxName);
        String description = fromCashBook.getName() + " (CB) -> " + toCashBook.getName() + " (CB)";
        MCashLine fromCashLine = new MCashLine(cashJournal);
        fromCashLine.setDescription(description);
        fromCashLine.setC_Currency_ID(currencyId);
        fromCashLine.setCashType(MCashLine.CASHTYPE_BankAccountTransfer);
        fromCashLine.setC_BankAccount_ID(fromBankAccountId);
        fromCashLine.setAmount(amount.negate());
        fromCashLine.setIsGenerated(true);
        PoManager.save(fromCashLine);
        if (fromBankAccountId != toBankAccountId) {
            transferBankToBank(ctx, fromBankAccountId, toBankAccountId, currencyId, amount, dateAcct, trxName);
        }
        MCashLine toCashLine = new MCashLine(toCash);
        toCashLine.setDescription(description);
        toCashLine.setC_Currency_ID(currencyId);
        toCashLine.setCashType(MCashLine.CASHTYPE_BankAccountTransfer);
        toCashLine.setC_BankAccount_ID(toBankAccountId);
        toCashLine.setAmount(amount);
        toCashLine.setIsGenerated(true);
        PoManager.save(toCashLine);
    }
}
