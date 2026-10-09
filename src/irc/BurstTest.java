package irc;

import java.io.Serializable;

import jvn.JvnObject;
import jvn.JvnServerImpl;

public class BurstTest {

    private static final int NB_OPERATIONS = 5000;

    public static void main(String[] args) {

        try {
            JvnServerImpl js = JvnServerImpl.jvnGetServer();

            JvnObject jo = js.jvnLookupObject("BURST");

            if (jo == null) {
                jo = js.jvnCreateObject((Serializable) new Sentence());
                jo.jvnUnLock();
                js.jvnRegisterObject("BURST", jo);

                System.out.println("Objet BURST créé.");
            }

            for (int i = 0; i < NB_OPERATIONS; i++) {

                // Une opération sur deux = écriture
                if (i % 2 == 0) {

                    jo.jvnLockWrite();

                    Sentence sentence =
                        (Sentence) jo.jvnGetObjectState();

                    sentence.write(
                        "Client " + args.length + " - valeur " + i
                    );

                    jo.jvnUnLock();

                // Une opération sur deux = lecture
                } else {

                    jo.jvnLockRead();

                    Sentence sentence =
                        (Sentence) jo.jvnGetObjectState();

                    sentence.read();

                    jo.jvnUnLock();
                }
            }

            System.out.println(
                "Test terminé : " + NB_OPERATIONS + " opérations."
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}