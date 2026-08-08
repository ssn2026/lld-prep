package buffer;

/** Common contract so worker classes don't care whether they're driving the safe or unsafe buffer. */
public interface Buffer<T> {

    void put(T item) throws InterruptedException;

    T take() throws InterruptedException;
}
