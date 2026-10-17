package irc;

import jvn.JvnObject;
import jvn.JvnProxy;
import jvn.JvnServerImpl;

public class MultiThreadReadWriteTest {

    private static final int NB_WRITERS = 5;
    private static final int NB_READERS = 5;

    private static final int NB_INCREMENT = 1000;
    private static final int NB_READ = 1000;

    public static void main(String[] args) {

        try {
            JvnServerImpl js = JvnServerImpl.jvnGetServer();

            JvnObject jo = js.jvnLookupObject("COUNTER");

            if (jo == null) {
                System.out.println("ERREUR : COUNTER n'existe pas.");
                return;
            }

            CounterItf counter =
                (CounterItf) JvnProxy.newInstance(jo);

            Thread[] threads =
                new Thread[NB_WRITERS + NB_READERS];

            int index = 0;

            // Threads écrivains
            for (int i = 0; i < NB_WRITERS; i++) {

                threads[index++] = new Thread(() -> {

                    for (int j = 0; j < NB_INCREMENT; j++) {
                        counter.increment();
                    }

                });
            }

            // Threads lecteurs
            for (int i = 0; i < NB_READERS; i++) {

                threads[index++] = new Thread(() -> {

                    for (int j = 0; j < NB_READ; j++) {
                        counter.getValue();
                    }

                });
            }

            long start = System.currentTimeMillis();

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            long end = System.currentTimeMillis();

            int value = counter.getValue();

            System.out.println(
                "Valeur attendue = "
                + (NB_WRITERS * NB_INCREMENT)
            );

            System.out.println(
                "Valeur obtenue = " + value
            );

            System.out.println(
                "Temps d'execution = "
                + (end - start)
                + " ms"
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}