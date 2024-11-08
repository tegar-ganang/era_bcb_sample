package org.olga.rebus.calculations;

import java.util.LinkedList;
import org.olga.rebus.model.AbstractWord;
import org.olga.rebus.model.IRebus;
import org.olga.rebus.model.Rebus.VisibleWord;

/**
 * ����� ��������, ������� ������� � ������ ������������ 
 * �����.
 * 
 * �������� ��������� �������. ������� ������������� ������
 * ��������� ������������ ���� �� �����. ����� �������� �����
 * �� ������ ����� � ������ �����, ���� ��� �� �������, �� 
 * �������� ����� �� ��� ����� � �.�.
 * 
 * @author Olga
 * @see RebusEnumerator
 * @see IRebus
 *
 */
public class WordsEnumerator {

    private RebusEnumerator myEnumerator;

    private IRebus myRebus;

    private AbstractWord[] myWords;

    private EnumHelper[] myHelpers;

    /**
	 * ����������� ������.
	 * 
	 * @param words ��������� ����� ��� ��������.
	 * @param rebus �����, ��� �������� ������� ������������ �����.
	 * @param enumerator ���������� ���� ��������� �������� ������.
	 */
    public WordsEnumerator(IRebus rebus, RebusEnumerator enumerator) {
        AbstractWord[] words = new AbstractWord[rebus.getVisibleWords().size()];
        int k = 0;
        for (VisibleWord word : rebus.getVisibleWords()) {
            words[k++] = word.getElement();
        }
        for (int j = 0; j < words.length; j++) {
            for (int i = 0; i < words.length - 1; i++) {
                if (words[i].compareTo(words[i + 1]) == -1) {
                    AbstractWord w = words[i];
                    words[i] = words[i + 1];
                    words[i + 1] = w;
                }
            }
        }
        myEnumerator = enumerator;
        myRebus = rebus;
        myWords = words;
        myHelpers = new EnumHelper[words.length];
    }

    private boolean next() {
        int index = myHelpers.length - 1;
        while (!myHelpers[index].next()) {
            if (index - 1 >= 0) {
                if (myHelpers[index - 1].myCurrent < index - 1) {
                    myHelpers[index].myCurrent = myHelpers[index - 1].myCurrent + 1;
                    int n = index + 1;
                    while (n < myHelpers.length) {
                        if (n - index == 1) {
                            myHelpers[n].myCurrent = myHelpers[n - 1].myCurrent + 2;
                        } else {
                            myHelpers[n].myCurrent = myHelpers[n - 1].myCurrent + 1;
                        }
                        n++;
                    }
                    myHelpers[index - 1].myCurrent++;
                } else {
                    index--;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    /**
	 * �����, ������� ������� ������������ ����� ��� ������.
	 */
    public void findEnums() {
        LinkedList<AbstractWord> enums = new LinkedList<AbstractWord>();
        int len = myWords.length;
        for (int i = 0; i < len; i++) {
            myHelpers[i] = new EnumHelper(i);
        }
        while (next()) {
            enums.clear();
            for (int j = 0; j < myHelpers.length; j++) {
                if (myHelpers[j].myCurrent != -1) {
                    enums.add(myWords[myHelpers[j].myCurrent]);
                }
            }
            myRebus.setEnumWords(enums);
            if (myEnumerator.calculate()) {
                return;
            }
        }
    }

    /**
	 * ��������������� �����-�������� ��� ��������.
	 * 
	 * @author Olga
	 *
	 */
    private static class EnumHelper {

        int myEnd;

        int myCurrent;

        /**
		 * ����������� ������.
		 * 
		 * @param end �� ������ �������� ������� �������.
		 */
        EnumHelper(int end) {
            myEnd = end;
            myCurrent = -1;
        }

        /**
		 * �����, ������������� ������� ������� �� 1.
		 * 
		 * @return true, ���� ��� ��������,
		 * 		   false � ��������� ������.
		 */
        boolean next() {
            if (++myCurrent <= myEnd) {
                return true;
            } else {
                --myCurrent;
                return false;
            }
        }

        public String toString() {
            return "" + myCurrent;
        }
    }
}
