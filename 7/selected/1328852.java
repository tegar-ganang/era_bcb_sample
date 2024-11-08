package dc;

import base.EndOfHublistException;

public class HubList {

    public int i = 0;

    public int curr = -1;

    public Hubs[] list = new Hubs[100000];

    private int j = 0;

    public synchronized Hubs Add(Hubs Hub) {
        Hubs Result;
        j = 0;
        while ((j < i) && !list[j].getAddr().toLowerCase().equals(Hub.getAddr().toLowerCase())) {
            j++;
        }
        if (j < i) {
            list[j].Update(Hub);
            list[j].setFresh(false);
            Result = list[j];
        } else {
            list[i] = Hub;
            Result = list[i];
            i++;
        }
        return Result;
    }

    public synchronized Hubs getHub(String HubAddr) {
        j = 0;
        while ((j < i) && !list[j].getAddr().toLowerCase().equals(HubAddr.toLowerCase()) && !list[j].getIP().equals(HubAddr)) {
            j++;
        }
        if (j == i) return null; else return list[j];
    }

    public void Delete(String HubAddr) {
        j = 0;
        while ((j < i) && (!list[j].getAddr().equals(HubAddr))) {
            j++;
        }
        if (j < i) {
            for (int x = j; x < i - 1; x++) {
                list[x] = list[x + 1];
            }
            i--;
        }
    }

    public Integer Count() {
        return i;
    }

    public synchronized boolean hasMoreHub() {
        if (curr < i - 1) return true; else return false;
    }

    public synchronized Hubs nextHub() throws EndOfHublistException {
        curr++;
        if (curr < i) return list[curr]; else throw new EndOfHublistException("nextHub at line 120");
    }

    public synchronized Hubs currHub() {
        return list[curr];
    }

    public void seek(int index) {
        curr = index;
    }

    public void reset() {
        curr = -1;
    }

    public synchronized void clear() {
        i = 0;
        curr = -1;
        j = 0;
    }
}
