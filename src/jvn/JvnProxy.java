package jvn;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class JvnProxy implements InvocationHandler {

    private JvnObject jvnObject;

    public JvnProxy(JvnObject jvnObject) {
        this.jvnObject = jvnObject;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // 1. Prendre le bon verrou
        if (method.isAnnotationPresent(JvnRead.class)) {
            jvnObject.jvnLockRead();
        } else if (method.isAnnotationPresent(JvnWrite.class)) {
            jvnObject.jvnLockWrite();
        }

        try {
            // 2. Recuperer le véritable objet métier
            Object realObject = jvnObject.jvnGetObjectState();

            // 3. Exécuter réellement read(), write(), etc.
            return method.invoke(realObject, args);

        } finally {
            // 4. Libérer le verrou
            if (method.isAnnotationPresent(JvnRead.class)
                    || method.isAnnotationPresent(JvnWrite.class)) {
                jvnObject.jvnUnLock();
            }
        }
    }

    public static Object newInstance(JvnObject jvnObject) throws JvnException  {

        Object realObject = jvnObject.jvnGetObjectState();

        return Proxy.newProxyInstance(
            realObject.getClass().getClassLoader(),
            realObject.getClass().getInterfaces(),
            new JvnProxy(jvnObject)
        );
    }
}

