package org.activision.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import org.activision.model.player.Player;

public class Serializer {

    public static byte[] ObjectToByteArray(Object obj) throws IOException {
        if (obj == null) return null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);
        out.writeObject(obj);
        out.close();
        byte[] buf = bos.toByteArray();
        bos.close();
        return buf;
    }

    public static Object ByteArrayToObject(byte[] arrBytes) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(arrBytes));
        Object obj = in.readObject();
        in.close();
        return obj;
    }

    /**
     * Serialize the object o (and any Serializable objects it refers to) and
     * store its serialized state in File f.
     */
    public static void store(Serializable o, File f) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f));
        out.writeObject(o);
        out.close();
    }

    /**
     * Deserialize the contents of File f and return the resulting object
     */
    public static Object load(File f) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
        Object object = in.readObject();
        in.close();
        return object;
    }

    /**
     * Use object serialization to make a "deep clone" of the object o. This
     * method serializes o and all objects it refers to, and then deserializes
     * that graph of objects, which means that everything is copied. This
     * differs from the clone() method of an object which is usually implemented
     * to produce a "shallow" clone that copies references to other objects,
     * instead of copying all referenced objects.
     */
    public static Object deepclone(final Serializable o) throws IOException, ClassNotFoundException {
        final PipedOutputStream pipeout = new PipedOutputStream();
        PipedInputStream pipein = new PipedInputStream(pipeout);
        Thread writer = new Thread() {

            public void run() {
                ObjectOutputStream out = null;
                try {
                    out = new ObjectOutputStream(pipeout);
                    out.writeObject(o);
                } catch (IOException e) {
                } finally {
                    try {
                        out.close();
                    } catch (Exception e) {
                    }
                }
            }
        };
        writer.start();
        ObjectInputStream in = new ObjectInputStream(pipein);
        return in.readObject();
    }

    public static Player LoadAccount(String Username) {
        File f = new File("./data/savedgames/" + Username + ".ser");
        try {
            return (Player) Serializer.load(f);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Player LoadAccount(File f) {
        try {
            return (Player) Serializer.load(f);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void SaveAccount(Player account) {
        File f = new File("./data/savedgames/" + account.getUsername() + ".ser");
        try {
            store(account, f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
