package irc;

import jvn.JvnObject;
import jvn.JvnProxy;
import jvn.JvnServerImpl;

public class MultiThreadCounterTest {

    private static final int NB_THREADS = 10;
    private static final int NB_INCREMENT = 1000;

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

            Thread[] threads = new Thread[NB_THREADS];

            long start = System.currentTimeMillis();

            for (int i = 0; i < NB_THREADS; i++) {

                threads[i] = new Thread(() -> {

                    for (int j = 0; j < NB_INCREMENT; j++) {
                        counter.increment();
                    }

                });

                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            long end = System.currentTimeMillis();

            int value = counter.getValue();

            System.out.println(
                "Valeur attendue = "
                + (NB_THREADS * NB_INCREMENT)
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