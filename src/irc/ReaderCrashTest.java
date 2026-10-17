package irc;

import jvn.JvnObject;
import jvn.JvnServerImpl;

public class ReaderCrashTest {

    public static void main(String[] args) {
        try {
            JvnServerImpl js = JvnServerImpl.jvnGetServer();

            JvnObject jo = js.jvnLookupObject("COUNTER");

            if (jo == null) {
                System.out.println("ERREUR : COUNTER n'existe pas.");
                return;
            }

            jo.jvnLockRead();

            System.out.println("Reader possède le verrou READ.");
            System.out.println("Tue ce processus maintenant avec kill -9.");

            Thread.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}