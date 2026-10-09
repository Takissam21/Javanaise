package irc;

import java.io.Serializable;

public class Counter implements Serializable {

    private static final long serialVersionUID = 1L;

    private int value;

    public Counter() {
        value = 0;
    }

    public void increment() {
        value++;
    }

    public int getValue() {
        return value;
    }
}
