package irc;

import jvn.JvnRead;
import jvn.JvnWrite;

public interface SentenceItf {

    @JvnRead
    String read();

    @JvnWrite
    void write(String text);
}
