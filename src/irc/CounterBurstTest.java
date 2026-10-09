package irc;

import java.io.Serializable;

import jvn.JvnObject;
import jvn.JvnServerImpl;

public class CounterBurstTest {

    private static final int NB_INCREMENT = 10000;

    public static void main(String[] args) {

        try {
            JvnServerImpl js = JvnServerImpl.jvnGetServer();

            JvnObject jo = js.jvnLookupObject("COUNTER");

            if (jo == null) {
                System.out.println("ERREUR : COUNTER n'existe pas.");
                return;
            }

            long start = System.currentTimeMillis();

            for (int i = 0; i < NB_INCREMENT; i++) {

                jo.jvnLockWrite();

                Counter counter =
                    (Counter) jo.jvnGetObjectState();

                counter.increment();

                jo.jvnUnLock();
            }

            long end = System.currentTimeMillis();

            // Lecture de la valeur observée à la fin de ce client
            jo.jvnLockRead();

            Counter counter =
                (Counter) jo.jvnGetObjectState();

            int value = counter.getValue();

            jo.jvnUnLock();

            System.out.println(
                NB_INCREMENT
                + " incréments terminés. Valeur observée = "
                + value
            );

            System.out.println(
                "Temps d'exécution = " + (end - start) + " ms"
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
