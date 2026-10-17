package irc;

import jvn.JvnObject;
import jvn.JvnProxy;
import jvn.JvnServerImpl;

public class CacheFlushTest {

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

            // Modification de l'objet
            counter.increment();

            System.out.println(
                "Valeur avant flush = " + counter.getValue()
            );

            int id = jo.jvnGetObjectId();

            // Retrait du cache local
            js.jvnFlushObject(id);

            System.out.println("Objet retire du cache.");

            // On récupère à nouveau l'objet
            JvnObject jo2 = js.jvnLookupObject("COUNTER");

            CounterItf counter2 =
                (CounterItf) JvnProxy.newInstance(jo2);

            System.out.println(
                "Valeur apres nouveau lookup = "
                + counter2.getValue()
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}