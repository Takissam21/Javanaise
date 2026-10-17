/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Contact:  
 *
 * Authors: 
 */

package jvn;

import java.rmi.server.UnicastRemoteObject;
import java.io.Serializable;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

public class JvnCoordImpl 	
              extends UnicastRemoteObject 
							implements JvnRemoteCoord{
	

  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
  // Compteur permettant de generer des identifiants uniques
  private int objectIdCounter = 0;
  // Nom global -> objet JVN
  private Map<String, JvnObject> objectsByName = new HashMap<>();

  // ID -> objet JVN
  private Map<Integer, JvnObject> objectsById = new HashMap<>();
  
  // ID de l'objet -> serveur qui possede le droit d'ecriture
  private Map<Integer, JvnRemoteServer> writers = new HashMap<>();

  // ID de l'objet -> serveurs qui possedent un droit de lecture
  private Map<Integer, Set<JvnRemoteServer>> readers = new HashMap<>();

  private Map<Integer, Serializable> objectStates = new HashMap<>();

/**
  * Default constructor
  * @throws JvnException
  **/
	private JvnCoordImpl() throws Exception {
		// to be completed
	}

  /**
  *  Allocate a NEW JVN object id (usually allocated to a 
  *  newly created JVN object)
  * @throws java.rmi.RemoteException,JvnException
  **/
  public int jvnGetObjectId()
  throws java.rmi.RemoteException,jvn.JvnException {
    
    return objectIdCounter++;
  }
  
  /**
  * Associate a symbolic name with a JVN object
  * @param jon : the JVN object name
  * @param jo  : the JVN object 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public synchronized void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
    objectsByName.put(jon, jo);
    objectsById.put(jo.jvnGetObjectId(), jo);
    writers.put(jo.jvnGetObjectId(), js);
    readers.put(jo.jvnGetObjectId(), new HashSet<>());
    objectStates.put(jo.jvnGetObjectId(), jo.jvnGetObjectState());
  }
  
  /**
  * Get the reference of a JVN object managed by a given JVN server 
  * @param jon : the JVN object name
  * @param js : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public synchronized JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
    
    return objectsByName.get(jon);
  }
  
  /**
  * Get a Read lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
    public synchronized Serializable jvnLockRead(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
    JvnRemoteServer writer = writers.get(joi);
    if (writer == null) {
        readers.get(joi).add(js);
        return objectStates.get(joi);
    }
    try {
        Serializable state = writer.jvnInvalidateWriterForReader(joi);

        objectStates.put(joi, state);
        writers.remove(joi);

        readers.get(joi).add(writer);
        readers.get(joi).add(js);

        return state;

    } catch (java.rmi.RemoteException e) {

        System.out.println(
            "Client writer en panne detecte pour l'objet " + joi
        );

        writers.remove(joi);
        readers.get(joi).add(js);

        return objectStates.get(joi);
    }
   }

  /**
  * Get a Write lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public synchronized Serializable jvnLockWrite(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
    JvnRemoteServer writer = writers.get(joi);

    if (writer != null && !writer.equals(js)) {
        try {
            Serializable state = writer.jvnInvalidateWriter(joi);
            objectStates.put(joi, state);

        } catch (java.rmi.RemoteException e) {
            System.out.println(
                "Client writer en panne detecte pour l'objet " + joi
            );

            writers.remove(joi);
        }
    }

    for (JvnRemoteServer reader : readers.get(joi)) {
        if (!reader.equals(js)) {
            try {
                reader.jvnInvalidateReader(joi);
            } catch (java.rmi.RemoteException e) {
                System.out.println(
                    "Client lecteur en panne detecte pour l'objet " + joi
                );
            }
        }
    }
    readers.get(joi).clear();
    writers.put(joi, js);
    return objectStates.get(joi);
   }

	/**
	* A JVN server terminates
	* @param js  : the remote reference of the server
	* @throws java.rmi.RemoteException, JvnException
	**/
    public void jvnTerminate(JvnRemoteServer js)
	 throws java.rmi.RemoteException, JvnException {
      for (Set<JvnRemoteServer> readerSet : readers.values()) {
          readerSet.remove(js);
      }

      Iterator<Map.Entry<Integer, JvnRemoteServer>> iterator =
              writers.entrySet().iterator();

      while (iterator.hasNext()) {
          Map.Entry<Integer, JvnRemoteServer> entry = iterator.next();

          Integer joi = entry.getKey();
          JvnRemoteServer writer = entry.getValue();

        if (writer.equals(js)) {
            try {
                Serializable state = writer.jvnInvalidateWriter(joi);
                objectStates.put(joi, state);

            } catch (java.rmi.RemoteException e) {
                System.out.println(
                    "Client writer deja en panne pour l'objet " + joi
                );
            }

            iterator.remove();
        }
      }
    }


    //main() qui cree le coordinateur et l enregistre dans le registre RMI sous le nom JvnCoord
    public static void main(String[] args) {
        try {
            // Demarre le registre RMI sur le port standard 1099
            LocateRegistry.createRegistry(1099);

            // Cree le coordinateur
            JvnCoordImpl coord = new JvnCoordImpl();

            // Enregistre le coordinateur sous le nom "JvnCoord"
            Naming.rebind("JvnCoord", coord);

            System.out.println("Coordinateur JVN demarre.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void jvnFlushObject(
            int joi,
            JvnRemoteServer js,
            Serializable state)
            throws java.rmi.RemoteException, JvnException {

        // Si ce client était lecteur, on retire son droit de lecture
        Set<JvnRemoteServer> readerSet = readers.get(joi);

        if (readerSet != null) {
            readerSet.remove(js);
        }

        // Si ce client était writer, sa copie est la plus récente
        JvnRemoteServer writer = writers.get(joi);

        if (writer != null && writer.equals(js)) {
            objectStates.put(joi, state);
            writers.remove(joi);
        }
    }
    
}

 
