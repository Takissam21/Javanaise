package irc;

import jvn.JvnObject;
import jvn.JvnProxy;
import jvn.JvnServerImpl;

public class CounterFinalReadTest {

    public static void main(String[] args) {

        try {
            JvnServerImpl js = JvnServerImpl.jvnGetServer();

            JvnObject jo = js.jvnLookupObject("COUNTER");

            CounterItf counter =
                (CounterItf) JvnProxy.newInstance(jo);

            int value = counter.getValue();

            System.out.println();
            System.out.println(
                ">>> VALEUR FINALE = " + value + " <<<"
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
