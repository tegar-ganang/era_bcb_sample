/**
 *
 * @author Administrator
 */
public class ArrayOfBalls {

    private int maxSize;

    private int _lenght;

    Ball[] array;

    public ArrayOfBalls(int maxSize) {
        this.maxSize = maxSize;
        array = new Ball[maxSize];
        _lenght = 0;
    }

    public int lenght() {
        return _lenght;
    }

    public void push(Ball ball) {
        if (_lenght < maxSize) {
            array[_lenght] = ball;
            _lenght++;
        }
    }

    public Ball unshift() {
        Ball ball = array[0];
        if (_lenght > 0) {
            for (int i = 0; i < _lenght - 1; i++) {
                array[i] = array[i + 1];
            }
            _lenght--;
            array[_lenght] = null;
        }
        return ball;
    }

    public Ball getBallAt(int index) {
        if (index >= 0 && index < _lenght) return array[index];
        return null;
    }
}
