package irc;

import jvn.JvnObject;
import jvn.JvnServerImpl;

public class CounterBurstTest {

    private static final int NB_INCREMENT = 20;

    public static void main(String[] args) {

        String clientName =
            args.length > 0 ? args[0] : "Client ?";

        try {

            JvnServerImpl js =
                JvnServerImpl.jvnGetServer();

            JvnObject jo =
                js.jvnLookupObject("COUNTER");

            if (jo == null) {
                System.out.println(
                    "ERREUR : COUNTER n'existe pas."
                );
                return;
            }

            long start = System.currentTimeMillis();

            // =========================
            // ÉCRITURES
            // =========================
            for (int i = 0; i < NB_INCREMENT; i++) {

                // Prendre le verrou WRITE
                jo.jvnLockWrite();

                Counter counter =
                    (Counter) jo.jvnGetObjectState();

                // Modifier le compteur
                counter.increment();

                System.out.println(
                    clientName
                    + " [WRITE] : valeur = "
                    + counter.getValue()
                );

                // Libérer le verrou
                jo.jvnUnLock();

                // Petit délai pour favoriser la concurrence
                Thread.sleep(10);
            }

            long end = System.currentTimeMillis();


            // =========================
            // Lecture finale seulement
            // =========================
            jo.jvnLockRead();

            Counter counter =
                (Counter) jo.jvnGetObjectState();

            int value = counter.getValue();

            jo.jvnUnLock();


            System.out.println(
                clientName
                + " : "
                + NB_INCREMENT
                + " incréments terminés."
                + " Valeur observée = "
                + value
            );

            System.out.println(
                clientName
                + " : temps = "
                + (end - start)
                + " ms"
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}