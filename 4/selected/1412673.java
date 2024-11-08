package org.pointrel.pointrel20090201;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

public class ArchiveWithNoIndex implements Archive {

    ArchiveBackend backend;

    public List<Transaction> transactions;

    TreeMap<String, Transaction> transactionMap;

    public ArchiveWithNoIndex(ArchiveBackend backend) {
        this.backend = backend;
        resetCache();
    }

    protected void resetCache() {
        transactions = new ArrayList<Transaction>();
        transactionMap = new TreeMap<String, Transaction>();
    }

    public void reload() {
        resetCache();
        this.load();
    }

    private void addTransactionToCache(String transactionIdentifier, Transaction transaction) {
        this.transactions.add(transaction);
        this.transactionMap.put(transaction.identifier, transaction);
    }

    public void load() {
        ArrayList<String> transactionIdentifiersList = backend.getTransactionIdentifierList();
        for (String transactionIdentifier : transactionIdentifiersList) {
            Transaction transaction = loadTransaction(transactionIdentifier);
            if (transaction != null) {
                addTransactionToCache(transactionIdentifier, transaction);
            }
        }
    }

    public void loadAnyNewTransactions() {
        ArrayList<String> transactionIdentifiersList = backend.getTransactionIdentifierList();
        for (String transactionIdentifier : transactionIdentifiersList) {
            if (!this.transactionMap.containsKey(transactionIdentifier)) {
                Transaction transaction = loadTransaction(transactionIdentifier);
                if (transaction != null) {
                    addTransactionToCache(transactionIdentifier, transaction);
                }
            }
        }
    }

    private Transaction loadTransaction(String transactionIdentifier) {
        Transaction transaction = null;
        if (backend.supportsTransactionObjects()) {
            transaction = backend.retrieveTransactionForTransactionIdentifier(transactionIdentifier);
        } else {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            if (!backend.retrieveDataForTransactionIdentifier(transactionIdentifier, outputStream)) {
                return null;
            }
            transaction = new Transaction();
            InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            if (!transaction.readFromUTF8Stream(inputStream)) {
                return null;
            }
        }
        if (transaction == null) {
            return null;
        }
        if (!transaction.identifier.equals(transactionIdentifier)) {
            System.out.println("Requested identifier " + transactionIdentifier + " does not match read transaction identifier " + transaction.identifier);
        }
        return transaction;
    }

    public void addAndWriteTransaction(Transaction transaction) {
        if (backend.isReadOnly()) {
            System.out.println("Attempting to write new transaction to read only backend");
            return;
        }
        String transactionIdentifier = transaction.identifier;
        Boolean added = false;
        if (backend.supportsTransactionObjects()) {
            added = backend.storeTransactionForTransactionIdentifier(transactionIdentifier, transaction);
        } else {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            transaction.writeToUTF8Stream(outputStream);
            byte[] bytes = outputStream.toByteArray();
            InputStream inputStream = new ByteArrayInputStream(bytes);
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            added = backend.storeDataForTransactionIdentifier(transactionIdentifier, inputStream, bytes.length);
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (added) {
            addTransactionToCache(transactionIdentifier, transaction);
        } else {
            System.out.println("Could not add transaction.");
        }
    }

    public void search(Query query) {
        Iterator<Transaction> iterator = transactions.iterator();
        while (iterator.hasNext()) {
            Transaction transaction = iterator.next();
            transaction.search(query);
        }
    }
}
