package irc;

import jvn.JvnObject;
import jvn.JvnProxy;
import jvn.JvnServerImpl;

public class CounterBurstTest {

    private static final int NB_INCREMENT = 20;

    public static void main(String[] args) {

        String clientName =
            args.length > 0 ? args[0] : "Client ?";

        try {

            JvnServerImpl js = JvnServerImpl.jvnGetServer();

            JvnObject jo = js.jvnLookupObject("COUNTER");

            if (jo == null) {
                System.out.println("ERREUR : COUNTER n'existe pas.");
                return;
            }

            // Création du proxy JVN2
            CounterItf counter =
                (CounterItf) JvnProxy.newInstance(jo);

            for (int i = 0; i < NB_INCREMENT; i++) {

                // @JvnWrite → verrou WRITE automatique
                counter.increment();

                System.out.println(
                    clientName
                    + " [WRITE] : valeur = "
                    + counter.getValue()
                );

                Thread.sleep(10);
            }

            System.out.println(
                clientName + " : 20 ecritures terminees."
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
