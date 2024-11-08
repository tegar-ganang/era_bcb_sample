package org.quickconnectfamily.kvkit.KeyValueStore;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import org.quickconnect.json.JSONException;
import org.quickconnect.json.JSONUtilities;
import android.app.Activity;
import android.content.Context;

/**
 * <img style="float:left;" src="http://www.quickconnectfamily.org/qcnative/docs/ios/mediumQC-1.png" />
 *  KVStore is the class that stores and gives you access to all of your key value data. <br/> Use the class methods from anywhere in your application.
 * 
 * <br/><br/><br/><br/><br/><br/><br/><br/>
 * @author Lee Barney
 *
 */
public class KVStore {

    /** \var const int fileSize
    \brief Default size of the file on disk
*/
    private static ConcurrentHashMap<String, Serializable> valuesByKey;

    private static ConcurrentLinkedQueue<Serializable> valuesByTimeStamp;

    private static ConcurrentHashMap<String, Semaphore> fileSemaphores;

    private static Cipher theEncryptCipher;

    private static Cipher theDecryptCipher;

    private static int inMemoryStorageCount;

    private static KVStoreEventListener theListener;

    private static Executor runnableExecutor;

    private static Activity theActivity;

    static {
        valuesByKey = new ConcurrentHashMap<String, Serializable>();
        valuesByTimeStamp = new ConcurrentLinkedQueue<Serializable>();
        fileSemaphores = new ConcurrentHashMap<String, Semaphore>();
        inMemoryStorageCount = 10;
        runnableExecutor = Executors.newCachedThreadPool();
    }

    private KVStore() {
    }

    public static void claimSemaphore(String semaphoreKey, int claimSize) throws InterruptedException {
        Semaphore fileSemaphore = fileSemaphores.get(semaphoreKey);
        if (fileSemaphore == null) {
            fileSemaphore = new Semaphore(Integer.MAX_VALUE, true);
            fileSemaphores.put(semaphoreKey, fileSemaphore);
        }
        fileSemaphore.acquire(claimSize);
    }

    public static void releaseSemaphore(String semaphoreKey, int releaseSize) {
        Semaphore fileSemaphore = fileSemaphores.get(semaphoreKey);
        if (fileSemaphore != null) {
            fileSemaphore.release(releaseSize);
        }
    }

    /**
	 * Assign the store an Android Activity to use as the basis for file storage
	 * @param anActivity the Android Activity
	 */
    public static void setActivity(Activity anActivity) {
        theActivity = anActivity;
    }

    /**
	 * Returns the Android activity used as the basis for file storage
	 * @return the Android Activity previously set or null if none has been set
	 */
    public static Activity getActivity() {
        return theActivity;
    }

    /**
	 * Assigns the listener object that is to be notified when data storage or removal requests are made.
	 * @param aListener an instance of a class that implements KVStoreEventListener
	 */
    public static void setStoreEventListener(KVStoreEventListener aListener) {
        theListener = aListener;
    }

    /**
	 * Assigns a Cipher object to use to encode all data and keys
	 * @param aCipher an object that inherits from Cipher
	 */
    public static void setEncryptionCipher(Cipher aCipher) {
        theEncryptCipher = aCipher;
    }

    /**
	 * Assigns a Cipher object to use to decode all data and keys.  This may be the same as the encryption Cipher
	 * @param aCipher
	 */
    public static void setDecryptionCipher(Cipher aCipher) {
        theDecryptCipher = aCipher;
    }

    /**
	 * Assigns an integer value as the upper limit count of data entities in the KVStore in-memory cache
	 * @param aCount the upper limit count for the number of data entities to hold in the in-memory cache.  Must be greater than zero.  Default value is 10.
	 */
    public static void setInMemoryStorageCount(int aCount) {
        if (aCount > 0) {
            inMemoryStorageCount = aCount;
        }
    }

    /**
	 * Used to store data in the key value store.  This method should be called periodically when objects are being modified in order to persist them to disk within the store.  If a Cipher has been set for the 
	 *  store both the key and the value will be encrypted in the persistent data storage.
	 * @param key a String that uniquely identifies the value to be stored.  If a data entity already uses this key then it will be replaced with the new value.
	 * @param value a Serializable object to be tracked in the key value store.  
	 * @throws KVStorageException
	 */
    public static void storeValue(String key, Serializable value) throws KVStorageException {
        KVStore.storeValue(key, value, null);
    }

    /**
	 * 
	 * Used to store data in the key value store where both the key and value are to be encrypted using a Cipher.  Using this method 
	 * overrides any Cipher set for the entire store only for this method call.  This method should be called periodically when objects are being modified in order to persist them to disk within the store.
	 * @param key a String that uniquely identifies the value to be stored.  If a data entity already uses this key then it will be replaced with the new value.
	 * @param value a Serializable object to be tracked in the key value store.  
	 * @param aCipher the Cipher used to encrypt this key and value.  
	 * @throws KVStorageException
	 */
    public static void storeValue(String key, Serializable value, Cipher aCipher) throws KVStorageException {
        if (key == null) {
            throw new KVStorageException("Missing Key", new NullPointerException("Key can not be null"));
        }
        if (value == null) {
            throw new KVStorageException("Missing Value", new NullPointerException("Value can not be null"));
        }
        if (aCipher == null) {
            aCipher = theEncryptCipher;
        }
        runnableExecutor.execute(new PersistanceRunnable(key, value, aCipher, inMemoryStorageCount, valuesByTimeStamp, valuesByKey, inMemoryStorageCount, theListener, theActivity));
    }

    /**
	 * Used to retrieve data stored earlier
	 * @param key the un-encrypted key previously used to store data.  If encryption has been set for the entire store KVStore will handle encryption itself.
	 * @return the data stored under the key if any exists or else null if no data exists for the key
	 */
    public static Serializable getValue(String key) {
        return KVStore.getValue(key, theDecryptCipher);
    }

    /**
	 * Used to retrieve data stored earlier.  The Cipher passed in this method overrides any Cipher set for the entire store only for this method call.
	 * @param key the un-encrypted key previously used to store data.
	 * @param aCipher the Cipher used to encrypt the previously stored data.
	 * @return the data stored under the key if any exists or else null if no data exists for the key
	 */
    public static Serializable getValue(String key, Cipher aCipher) {
        Serializable existingValue = valuesByKey.get(key);
        if (existingValue == null) {
            String keyToUse = key;
            if (aCipher == null) {
                aCipher = theDecryptCipher;
            }
            if (aCipher != null) {
                try {
                    keyToUse = new String(aCipher.doFinal(keyToUse.getBytes()));
                } catch (Exception e) {
                    if (theListener != null) {
                        theListener.errorHappened(key, null, new KVStorageException("Bad Key", e));
                    }
                    return null;
                }
            }
            existingValue = buildValueFromFile(key, aCipher, keyToUse);
        }
        valuesByKey.put(key, existingValue);
        valuesByTimeStamp.remove(existingValue);
        valuesByTimeStamp.add(existingValue);
        while (valuesByTimeStamp.size() > inMemoryStorageCount) {
            System.out.println("values before remove: " + valuesByTimeStamp);
            Object valueToRemove = valuesByTimeStamp.poll();
            Set<Entry<String, Serializable>> allPairs = valuesByKey.entrySet();
            for (Entry<?, ?> aPair : allPairs) {
                if (aPair.getValue().equals(valueToRemove)) {
                    valuesByKey.remove(aPair.getKey());
                    break;
                }
            }
            System.out.println("values after remove: " + valuesByTimeStamp);
        }
        return existingValue;
    }

    protected static Serializable buildValueFromFile(String key, Cipher aCipher, String encryptedKey) {
        Serializable aFoundEntity = null;
        try {
            KVStore.claimSemaphore(key, 1);
        } catch (InterruptedException e) {
            theListener.errorHappened(key, null, new KVStorageException("Persistance failure", e));
            return null;
        }
        try {
            FileInputStream persistanceFileInputStream = theActivity.openFileInput(encryptedKey);
            long size = persistanceFileInputStream.getChannel().size();
            if (size == 0) {
                return null;
            }
            byte[] fileBytes = new byte[(int) size];
            persistanceFileInputStream.read(fileBytes);
            persistanceFileInputStream.close();
            if (aCipher != null) {
                fileBytes = aCipher.doFinal(fileBytes);
            }
            String textString = new String(fileBytes);
            aFoundEntity = (Serializable) JSONUtilities.parse(textString);
        } catch (Exception e) {
            if (theListener != null) {
                theListener.errorHappened(key, null, new KVStorageException("Persistance retrieval error", e));
            }
            KVStore.releaseSemaphore(key, 1);
            return null;
        }
        KVStore.releaseSemaphore(key, 1);
        return aFoundEntity;
    }

    /**
	 * Used to remove both data from the in-memory cache and persistent storage.  If there is no key match the request is ignored.  If a Cipher has been set for the entire store then it will be used to accomplish the retrieval.
	 * @param key  the key that represents the data to be removed
	 */
    public static void removeValue(String key) {
        removeValue(key, null);
    }

    /**
	 * Used to remove both data from the in-memory cache and persistent storage.  If there is no key match the request is ignored.  The Cipher passed in this method overrides any Cipher set for the entire store only for this method call.
	 * @param key
	 * @param aCipher
	 */
    public static void removeValue(String key, Cipher aCipher) {
        if (key != null && theListener.shouldDelete(key)) {
            if (aCipher == null) {
                aCipher = theEncryptCipher;
            }
            String keyToUse = key;
            if (aCipher != null) {
                try {
                    keyToUse = new String(aCipher.doFinal(keyToUse.getBytes()));
                } catch (Exception e) {
                    if (theListener != null) {
                        theListener.errorHappened(key, null, new KVStorageException("Key encryption failure", e));
                        return;
                    }
                }
            }
            runnableExecutor.execute(new PersistanceDeletionRunnable(theActivity, key, keyToUse, fileSemaphores, valuesByTimeStamp, valuesByKey, theListener));
        }
    }

    /**
	 * This method is PRE-ALPHA.  Do not use it in production or testing.  It is incomplete.
	 * @param keyPath
	 * @param matchValue
	 * @param inMemoryOnly
	 * @param aCipher
	 * @return
	 */
    public static Set<Object> getEntities(String keyPath, Object matchValue, boolean inMemoryOnly, Cipher aCipher) {
        Set<Object> retValue = new HashSet<Object>();
        String[] keysInPath = null;
        if (keyPath.indexOf(".") == -1) {
            keysInPath = new String[1];
            keysInPath[0] = keyPath;
        } else {
            keysInPath = keyPath.split(".");
        }
        System.out.println("keys: " + keysInPath);
        ExecutorService searchThreadPool = Executors.newCachedThreadPool();
        Collection topLevelValues = valuesByKey.values();
        Iterator valueIterator = topLevelValues.iterator();
        while (valueIterator.hasNext()) {
            searchThreadPool.execute(new SearchRunnable(keysInPath, matchValue, valueIterator.next(), retValue, searchThreadPool, theListener));
        }
        if (!inMemoryOnly) {
            if (aCipher == null) {
                aCipher = theDecryptCipher;
            }
            String[] fileNames = theActivity.fileList();
            for (String aFileName : fileNames) {
                searchThreadPool.execute(new LoadAndSearchRunnable(aFileName, aCipher, keysInPath, matchValue, valueIterator.next(), retValue, searchThreadPool, theListener));
            }
        }
        searchThreadPool.shutdown();
        return retValue;
    }

    /**
	 * This method is PRE-ALPHA.  Do not use it in production or testing.  It is incomplete.
	 * @param keyPath
	 * @param matchValue
	 * @param inMemoryOnly
	 * @return
	 */
    public static Set<Object> getEntities(String keyPath, Object matchValue, boolean inMemoryOnly) {
        return getEntities(keyPath, matchValue, inMemoryOnly, null);
    }

    /**
	 * This method is PRE-ALPHA.  Do not use it in production or testing.  It is incomplete.
	 * @param keyPath
	 * @param regex
	 * @param inMemoryOnly
	 * @return
	 */
    public static Set<Object> getEntities(String keyPath, Pattern regex, boolean inMemoryOnly) {
        return getEntities(keyPath, (Object) regex, inMemoryOnly, null);
    }

    /**
	 * This method is PRE-ALPHA.  Do not use it in production or testing.  It is incomplete.
	 * @param keyPath
	 * @param regex
	 * @param inMemoryOnly
	 * @param aCipher
	 * @return
	 */
    public static Set<Object> getEntities(String keyPath, Pattern regex, boolean inMemoryOnly, Cipher aCipher) {
        return getEntities(keyPath, (Object) regex, inMemoryOnly, aCipher);
    }
}
