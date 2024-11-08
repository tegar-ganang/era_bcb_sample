package thread.liveness;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class DeadLockByLists {

    private List<String> listA = Collections.synchronizedList(new ArrayList<String>());

    private List<String> listB = Collections.synchronizedList(new ArrayList<String>());

    public String get(int i) {
        synchronized (listA) {
            listA.add(listB.indexOf("100000") + "");
            synchronized (listB) {
                return listA.get(i) + " = " + listB.get(i);
            }
        }
    }

    public void write(String a) {
        synchronized (listB) {
            listB.add(listA.indexOf("100000") + "");
            synchronized (listA) {
                listA.add(a);
                listB.add(a);
            }
        }
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        final DeadLockByLists r = new DeadLockByLists();
        Thread write = new Thread() {

            public void run() {
                for (int i = 0; i < 1000000000; i++) {
                    r.write(i + "");
                }
            }
        };
        Thread search = new Thread() {

            public void run() {
                for (int i = 0; i < 1000000000; i++) {
                    System.out.println(r.get(i));
                }
            }
        };
        new Thread(write, "write") {
        }.start();
        new Thread(search, "search") {
        }.start();
    }
}
