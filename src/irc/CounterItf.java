package irc;

import jvn.JvnRead;
import jvn.JvnWrite;

public interface CounterItf {

    @JvnWrite
    void increment();

    @JvnRead
    int getValue();
}