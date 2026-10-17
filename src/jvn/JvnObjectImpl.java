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

    private boolean writeRequestPending = false;
    private boolean readRequestPending = false;
    private int activeReaders = 0;
    private boolean activeWriter = false;

    private boolean flushInProgress = false;
    private boolean evicted = false;

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

        boolean needRemote = false;
        if (evicted) {
            throw new JvnException(
                "Objet retire du cache"
            );
        }

        while (flushInProgress) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new JvnException(
                    "Interruption pendant l'attente du flush"
                );
            }

            if (evicted) {
                throw new JvnException(
                    "Objet retire du cache"
                );
            }
        }
        synchronized (this) {

            // Une lecture attend si un writer local utilise l'objet
            // ou si une demande WRITE/READ distante est en cours.
            while (activeWriter
                    || writeRequestPending
                    || readRequestPending) {

                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new JvnException(
                        "Interruption pendant l'attente du verrou de lecture"
                    );
                }
            }

            switch (lockState) {

                case NL:
                    needRemote = true;
                    readRequestPending = true;
                    break;

                case RC:
                    lockState = LockState.R;
                    activeReaders++;
                    break;

                case WC:
                    lockState = LockState.RWC;
                    activeReaders++;
                    break;

                case R:
                case RWC:
                    // Un autre thread lit déjà :
                    // plusieurs READ peuvent coexister.
                    activeReaders++;
                    break;

                default:
                    break;
            }
        }

        if (needRemote) {
            try {
                Serializable state = localServer.jvnLockRead(objectId);

                synchronized (this) {
                    objectState = state;
                    lockState = LockState.R;

                    readRequestPending = false;
                    activeReaders = 1;

                    notifyAll();
                }

            } catch (JvnException e) {

                synchronized (this) {
                    readRequestPending = false;
                    notifyAll();
                }

                throw e;
            }
        }
    }

    @Override
    public void jvnLockWrite() throws JvnException {

        boolean needRemote = false;
        if (evicted) {
            throw new JvnException(
                "Objet retire du cache"
            );
        }

        while (flushInProgress) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new JvnException(
                    "Interruption pendant l'attente du flush"
                );
            }

            if (evicted) {
                throw new JvnException(
                    "Objet retire du cache"
                );
            }
        }

        synchronized (this) {

            // Un seul writer local à la fois.
            // Une écriture attend aussi la fin des lectures locales.
            while (activeWriter
                    || activeReaders > 0
                    || writeRequestPending
                    || readRequestPending) {

                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new JvnException(
                        "Interruption pendant l'attente du verrou d'ecriture"
                    );
                }
            }

            switch (lockState) {

                case WC:
                    // WRITE déjà disponible dans le cache local
                    lockState = LockState.W;
                    activeWriter = true;
                    break;

                case NL:
                case RC:
                    // Il faut demander WRITE au coordinateur
                    needRemote = true;
                    writeRequestPending = true;
                    break;

                default:
                    break;
            }
        }

        if (needRemote) {
            try {
                Serializable state = localServer.jvnLockWrite(objectId);

                synchronized (this) {
                    objectState = state;
                    lockState = LockState.W;

                    writeRequestPending = false;
                    activeWriter = true;

                    notifyAll();
                }

            } catch (JvnException e) {

                synchronized (this) {
                    writeRequestPending = false;
                    notifyAll();
                }

                throw e;
            }
        }
    }

    @Override
    public synchronized void jvnUnLock() throws JvnException {

        switch (lockState) {

            case R:
                activeReaders--;

                // Tant qu'il reste au moins un lecteur,
                // on reste en état R
                if (activeReaders == 0) {
                    lockState = LockState.RC;
                }
                break;

            case W:
                // Fin de l'ecriture locale
                activeWriter = false;

                // On garde le droit d'ecriture en cache
                lockState = LockState.WC;
                break;

            case RWC:
                activeReaders--;

                // Tant qu'il reste un lecteur,
                // on garde RWC
                if (activeReaders == 0) {
                    lockState = LockState.WC;
                }
                break;

            default:
                break;
        }
        notifyAll();
    }

    @Override
    public synchronized void jvnInvalidateReader() throws JvnException {

        while (readRequestPending || activeReaders > 0) {
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

        while (writeRequestPending
                || activeWriter
                || activeReaders > 0) {
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

        while (writeRequestPending || activeWriter) {
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

    @Override
    public synchronized Serializable jvnPrepareFlush()
            throws JvnException {

        if (evicted) {
            throw new JvnException(
                "Objet deja retire du cache"
            );
        }

        // Empêche de nouveaux accès pendant le flush
        flushInProgress = true;

        // Attendre que toutes les utilisations locales soient terminées
        while (activeWriter
                || activeReaders > 0
                || writeRequestPending
                || readRequestPending) {

            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                flushInProgress = false;
                notifyAll();

                throw new JvnException(
                    "Interruption pendant le flush de l'objet"
                );
            }
        }

        return objectState;
    }

    @Override
    public synchronized void jvnConfirmFlush()
            throws JvnException {

        evicted = true;
        flushInProgress = false;
        lockState = LockState.NL;

        notifyAll();
    }

    @Override
    public synchronized void jvnCancelFlush()
            throws JvnException {

        flushInProgress = false;

        notifyAll();
    }

    public void setLocalServer(JvnLocalServer localServer) {
        this.localServer = localServer;
    }

    public void resetLockState() {
        this.lockState = LockState.NL;
    }

}