package irc;

import jvn.JvnObject;
import jvn.JvnServerImpl;
import jvn.JvnProxy;

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

            CounterItf counter = (CounterItf) JvnProxy.newInstance(jo);

            long start = System.currentTimeMillis();

            for (int i = 0; i < NB_INCREMENT; i++) {
                counter.increment();
            }

            long end = System.currentTimeMillis();

            int value = counter.getValue();

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
