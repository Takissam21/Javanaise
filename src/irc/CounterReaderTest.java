package irc;

import jvn.JvnObject;
import jvn.JvnServerImpl;

public class CounterReaderTest {

    private static final int NB_READ = 20;

    public static void main(String[] args) {

        String clientName =
            args.length > 0 ? args[0] : "Reader";

        try {

            JvnServerImpl js =
                JvnServerImpl.jvnGetServer();

            JvnObject jo =
                js.jvnLookupObject("COUNTER");

            if (jo == null) {
                System.out.println("ERREUR : COUNTER n'existe pas.");
                return;
            }

            for (int i = 0; i < NB_READ; i++) {

                jo.jvnLockRead();

                Counter counter =
                    (Counter) jo.jvnGetObjectState();

                System.out.println(
                    clientName
                    + " [READ] : valeur = "
                    + counter.getValue()
                );

                jo.jvnUnLock();

                Thread.sleep(10);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
