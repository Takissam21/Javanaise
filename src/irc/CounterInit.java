package irc;

import java.io.Serializable;
import jvn.JvnObject;
import jvn.JvnServerImpl;

public class CounterInit {

    public static void main(String[] args) {

        try {
            JvnServerImpl js = JvnServerImpl.jvnGetServer();

            JvnObject jo = js.jvnLookupObject("COUNTER");

            if (jo == null) {
                jo = js.jvnCreateObject((Serializable) new Counter());
                jo.jvnUnLock();
                js.jvnRegisterObject("COUNTER", jo);

                System.out.println("COUNTER créé et enregistré.");
            } else {
                System.out.println("COUNTER existe déjà.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
