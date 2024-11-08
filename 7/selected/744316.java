package aggregators;

import java.util.*;
import java.lang.reflect.*;
import backend.*;

public class SessionAggregator {

    public static Map mapGames(Set sessions, String method) {
        Map retMap = new TreeMap();
        try {
            String[] elements = method.split("~");
            String strMethod = elements[0];
            String[] params = new String[elements.length - 1];
            Class[] cArr = new Class[params.length];
            for (int i = 0; i < params.length; i++) {
                params[i] = elements[i + 1];
                cArr[i] = String.class;
            }
            java.lang.reflect.Method meth = Game.class.getMethod(strMethod, cArr);
            Iterator sIt = sessions.iterator();
            while (sIt.hasNext()) {
                Iterator gIt = ((Session) sIt.next()).getGames().iterator();
                while (gIt.hasNext()) {
                    Game tempGame = (Game) gIt.next();
                    Object key = meth.invoke(tempGame, params);
                    Set gameList = new TreeSet();
                    if (retMap.containsKey(key)) gameList = (Set) retMap.get(key);
                    gameList.add(tempGame);
                    retMap.put(key, gameList);
                }
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return retMap;
    }

    public static Set aggregateSessions(Set sessions, String method) {
        Set retSet = new TreeSet();
        try {
            String[] elements = method.split("~");
            String strMethod = elements[0];
            String[] params = new String[elements.length - 1];
            Class[] cArr = new Class[params.length];
            for (int i = 0; i < params.length; i++) {
                params[i] = elements[i + 1];
                cArr[i] = String.class;
            }
            java.lang.reflect.Method meth = Session.class.getMethod(strMethod, cArr);
            Iterator sIt = sessions.iterator();
            while (sIt.hasNext()) retSet.addAll((Collection) meth.invoke(sIt.next(), params));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return retSet;
    }
}
