package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {

    private static final long serialVersionUID = 1L; // a comprendre

    private enum LockState {
        NL,
        R,
        W,
        RC,
        WC,
        RWC
    }

    // Identifiant unique de l'objet partage
    private int objectId;

    // Copie locale de l'objet Java partage
    private Serializable objectState;

    // Etat actuel du verrou
    private LockState lockState;

    //une reference vers le serveur local 
    //transient = quand tu serialises cet objet, n'envoie pas cet attribut.
    private transient JvnLocalServer localServer;

    public JvnObjectImpl(Serializable objectState, int objectId,
                        JvnLocalServer localServer) {
        this.objectState = objectState;
        this.objectId = objectId;
        this.localServer = localServer;
        this.lockState = LockState.NL;
    }

    public JvnObjectImpl(Serializable objectState, int objectId,
                     JvnLocalServer localServer, boolean newObject) {
        this.objectState = objectState;
        this.objectId = objectId;
        this.localServer = localServer;
        this.lockState = newObject ? LockState.W : LockState.NL;
    }

    @Override
    public int jvnGetObjectId() throws JvnException {
        return objectId;
    }

    @Override
    public Serializable jvnGetObjectState() throws JvnException {
        return objectState;
    }

    @Override
    public void jvnLockRead() throws JvnException {
        // faux : je peux obtenir read localement
        // vrai : je dois demander au coordinateur
        boolean needRemote = false;
        synchronized(this){
            switch (lockState) {
                case NL:
                    // Aucun verrou en cache :
                    // il faut demander un verrou de lecture au serveur
                    needRemote = true;
                    break;

                case RC:
                    // Le verrou de lecture etait deja en cache
                    lockState = LockState.R;
                    break;

                case WC:
                    // On possede meme un verrou d'ecriture en cache,
                    // donc on peut evidemment lire
                    lockState = LockState.RWC;
                    break;

                default:
                    break;
            }
        }

        if (needRemote) {
            Serializable state = localServer.jvnLockRead(objectId);

            synchronized (this) {
                objectState = state;
                lockState = LockState.R;
            }
        }

    }

    @Override
    public void jvnLockWrite() throws JvnException {
        //false : je peux obtenir WRITE grace a mon cache local
        //true  : je dois demander WRITE au coordinateur
        boolean needRemote = false;
        synchronized (this) {
            switch (lockState) {
                case WC:
                    // J'ai deja le droit d'ecriture en cache
                    lockState = LockState.W;
                    break;

                case NL:
                case RC:
                    // Je dois demander WRITE au coordinateur
                    needRemote = true;
                    break;

                default:
                    break;
            }
        }
        if (needRemote) {
            Serializable state = localServer.jvnLockWrite(objectId);

            synchronized (this) {
                objectState = state;
                lockState = LockState.W;
            }
        }
    }

    @Override
    public synchronized void jvnUnLock() throws JvnException {

        switch (lockState) {

            case R:
                // Fin d'une lecture : on garde le droit de lecture en cache
                lockState = LockState.RC;
                break;

            case W:
                // Fin d'une ecriture : on garde le droit d'ecriture en cache
                lockState = LockState.WC;
                break;

            case RWC:
                // Fin de la lecture, mais le droit d'ecriture reste en cache
                lockState = LockState.WC;
                break;

            default:
                break;
        }
        notifyAll();
    }

    @Override
    public synchronized void jvnInvalidateReader() throws JvnException {

        while (lockState == LockState.R) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new JvnException("Interruption pendant l'invalidation du lecteur");
            }
        }

        lockState = LockState.NL;
    }

    @Override
    public synchronized Serializable jvnInvalidateWriter() throws JvnException {

        while (lockState == LockState.W || lockState == LockState.RWC) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new JvnException(
                    "Interruption pendant l'invalidation de l'ecrivain"
                );
            }
        }

        lockState = LockState.NL;

        return objectState;
    }
    
    @Override
    public synchronized Serializable jvnInvalidateWriterForReader()
            throws JvnException {

        while (lockState == LockState.W) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new JvnException(
                    "Interruption pendant l'invalidation WriterForReader"
                );
            }
        }

        if (lockState == LockState.RWC) {
            lockState = LockState.R;
        } else {
            lockState = LockState.RC;
        }

        return objectState;
    }

    public void setLocalServer(JvnLocalServer localServer) {
        this.localServer = localServer;
    }

    public void resetLockState() {
        this.lockState = LockState.NL;
    }
}