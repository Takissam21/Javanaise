package irc;

import java.io.Serializable;

import jvn.JvnObject;
import jvn.JvnServerImpl;

public class CacheSaturationTest {

    public static void main(String[] args) {

        try {
            JvnServerImpl js = JvnServerImpl.jvnGetServer();

            for (int i = 1; i <= 4; i++) {

                Counter counter = new Counter();

                JvnObject jo =
                    js.jvnCreateObject((Serializable) counter);

                jo.jvnUnLock();

                String name = "CACHE_OBJECT_" + i;

                js.jvnRegisterObject(name, jo);

                System.out.println(
                    name
                    + " cree et enregistre. ID = "
                    + jo.jvnGetObjectId()
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}