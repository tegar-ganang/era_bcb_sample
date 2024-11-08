package org.crappydbms.main;

import java.io.File;
import java.util.ArrayList;
import junit.framework.Assert;
import org.crappydbms.datadictionary.DataDictionary;
import org.crappydbms.datadictionary.InvalidStoredRelationNameException;
import org.crappydbms.dbfiles.FilePageID;
import org.crappydbms.dbfiles.locking.DBFileLockManager;
import org.crappydbms.dbfiles.locking.PageLock;
import org.crappydbms.exceptions.InvalidAttributeNameException;
import org.crappydbms.queries.operations.DeleteOperation;
import org.crappydbms.queries.operators.selection.SelectionOperator;
import org.crappydbms.queries.predicates.Predicate;
import org.crappydbms.queries.predicates.simple.factories.EqualsPredicateFactory;
import org.crappydbms.relations.StoredRelation;
import org.crappydbms.relations.fields.IntegerField;
import org.crappydbms.relations.fields.StringField;
import org.crappydbms.relations.tuples.StoredTuple;
import org.crappydbms.transactions.Transaction;
import org.crappydbms.transactions.TransactionAbortedException;

/**
 * @author Facundo Manuel Quiroga
 * Jan 16, 2009
 * 
 */
public class DatabaseMultithreadedDeleteTest extends CrappyDBMSTestCase {

    static volatile int finishedTasks;

    static synchronized void finished() {
        DatabaseMultithreadedDeleteTest.finishedTasks++;
    }

    private Database theDatabase;

    File directory;

    public void setUp() throws Exception {
        CrappyDBMSTestCase.deleteTestDirectory();
        directory = CrappyDBMSTestCase.createTestDirectory();
        DataDictionary dataDictionary = new DataDictionary(directory);
        theDatabase = new Database(dataDictionary);
        DatabaseMultithreadedDeleteTest.finishedTasks = 0;
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testRemoveTuples1() {
        this.testRemoveTuples(10, 2, 5, 1, true);
    }

    public void testRemoveTuples2() {
        this.testRemoveTuples(100, 4, 5, 5, true);
    }

    public void testRemoveTuples3() {
        this.testRemoveTuples(1000, 20, 5, 10, true);
    }

    public void testRemoveTuples4() {
        this.testRemoveTuples(1000, 20, 5, 10, true);
    }

    public void testRemoveTuples(int initialNumberOfTuples, int numberOfTasks, int transactionsPerTask, int tuplesRemovedPerTransaction, boolean shouldAbort) {
        if (initialNumberOfTuples < numberOfTasks * transactionsPerTask * tuplesRemovedPerTransaction) {
            throw new CrappyDBMSTestingError("Cannot remove more tuples than those that where added");
        }
        CrappyDBMSTestCase.createTestingAssetsStoredRelationNamed(theDatabase, "assets");
        Transaction transaction = theDatabase.newTransaction();
        StoredRelation relation;
        try {
            relation = theDatabase.getStoredRelationNamed("assets");
            ArrayList<StoredTuple> tuples = CrappyDBMSTestCase.createRandomTuplesForAssetsRelation(initialNumberOfTuples, relation.getSchema());
            for (StoredTuple tuple : tuples) {
                relation.addTuple(tuple, transaction);
            }
            transaction.commit();
        } catch (TransactionAbortedException e) {
            throw new CrappyDBMSTestingError("Error adding tuples to the 'assets' relation");
        } catch (InvalidStoredRelationNameException e) {
            Assert.fail("Assets StoredRelation should exist " + e.getMessage());
        }
        int tuplesRemovedPerTask = transactionsPerTask * tuplesRemovedPerTransaction;
        for (int i = 0; i < numberOfTasks; i++) {
            int startingWithTupleNumber = i * tuplesRemovedPerTask;
            TuplesDeleter tuplesDeleter = new TuplesDeleter(i, transactionsPerTask, tuplesRemovedPerTransaction, startingWithTupleNumber, this.theDatabase, shouldAbort);
            new Thread(tuplesDeleter).start();
        }
        try {
            while (DatabaseMultithreadedDeleteTest.finishedTasks < numberOfTasks) {
                Thread.sleep(2000);
            }
            CrappyDBMSTestCase.checkDatabaseLocksAreFree(this.theDatabase);
        } catch (InterruptedException e) {
            Assert.fail("Should not be interrupted");
        }
    }

    protected void printTransactionsAndPages(DBFileLockManager dbFileLockManager) {
        this.printTransactions();
        this.printPages(dbFileLockManager);
    }

    private void printPages(DBFileLockManager dbFileLockManager) {
        for (FilePageID filePageId : dbFileLockManager.getPageLocks().keySet()) {
            PageLock pl = dbFileLockManager.getPageLocks().get(filePageId);
            System.out.println(filePageId + " lock:" + " writers: " + pl.getWriters() + " readers: " + pl.getReaders() + " w-writers " + pl.getWritersWaitingCount() + " w-readers " + pl.getReadersWaitingCount() + " w-upgraders " + pl.getUpgradersWaitingCount());
        }
    }

    private void printTransactions() {
        for (Transaction transaction : this.theDatabase.getTransactionManager().getTransactions()) {
            System.out.println(transaction.toString() + " s " + transaction.getStatus() + " ls " + transaction.getLockingStatus());
        }
    }

    public class TuplesDeleter implements Runnable {

        protected int id;

        protected int tuplesRemovedPerTransaction;

        protected int transactions;

        protected Database database;

        protected Transaction transaction;

        protected boolean shouldAbort;

        protected int startingWithTupleNumber;

        public TuplesDeleter(int id, int transactions, int tuplesRemovedPerTransaction, int startingWithTupleNumber, Database database, boolean shouldAbort) {
            this.id = id;
            this.tuplesRemovedPerTransaction = tuplesRemovedPerTransaction;
            this.database = database;
            this.shouldAbort = shouldAbort;
            this.transactions = transactions;
            this.startingWithTupleNumber = startingWithTupleNumber;
        }

        public void run() {
            String attributeName = "name";
            try {
                StoredRelation relation = database.getStoredRelationNamed("assets");
                for (int t = 0; t < this.transactions; t++) {
                    this.transaction = database.newTransaction();
                    for (int i = 0; i < this.tuplesRemovedPerTransaction; i++) {
                        String name = "juan" + startingWithTupleNumber;
                        Predicate predicate = EqualsPredicateFactory.newPredicate(attributeName, StringField.valueOf(name));
                        DeleteOperation deleteOperation = new DeleteOperation(relation, predicate);
                        IntegerField count = deleteOperation.perform(transaction);
                        if (count.getValue() != 1) {
                            String message = "id " + id + " t " + t + " Could not delete tuple with attribute " + attributeName + " = " + name;
                            try {
                                transaction.abort(message);
                            } catch (TransactionAbortedException e) {
                            }
                            Assert.fail(message);
                        }
                        SelectionOperator select = new SelectionOperator(relation, predicate);
                        Assert.assertFalse("Should not have a tuple with " + attributeName + " = " + name, select.iterator(transaction).hasNext());
                        startingWithTupleNumber++;
                    }
                    this.transaction.commit();
                    Thread.sleep(200);
                }
            } catch (TransactionAbortedException e) {
                if (!shouldAbort) {
                    Assert.fail("Id " + id + " should not abort" + e.getMessage());
                } else {
                }
            } catch (InvalidStoredRelationNameException e) {
                Assert.fail("Id" + id + " Assets StoredRelation should exist " + e.getMessage());
            } catch (InterruptedException e) {
                Assert.fail("Id " + id + " should not be interrupted" + e.getMessage());
            } catch (InvalidAttributeNameException e) {
                Assert.fail("Id " + id + "Invalid attribute name: was " + attributeName + ", error :" + e.getMessage());
            } finally {
                DatabaseMultithreadedDeleteTest.finished();
            }
        }
    }
}
