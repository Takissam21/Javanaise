package irc;

import jvn.JvnObject;
import jvn.JvnServerImpl;

public class WriterCrashTest {

    public static void main(String[] args) {

        try {
            JvnServerImpl js = JvnServerImpl.jvnGetServer();

            JvnObject jo = js.jvnLookupObject("COUNTER");

            if (jo == null) {
                System.out.println("ERREUR : COUNTER n'existe pas.");
                return;
            }

            jo.jvnLockWrite();

            Counter counter =
                (Counter) jo.jvnGetObjectState();

            counter.increment();

            System.out.println("Writer possede le verrou WRITE.");
            System.out.println("Valeur locale apres increment = "
                    + counter.getValue());
            System.out.println(
                "Tue ce processus maintenant avec kill -9."
            );

            Thread.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}