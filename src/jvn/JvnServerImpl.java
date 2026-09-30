/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Contact: 
 * 
 * Authors: 
 */

package jvn;

import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.rmi.Naming;
import java.util.Map;
import java.util.HashMap;
import java.rmi.RemoteException;

public class JvnServerImpl 	
              extends UnicastRemoteObject 
							implements JvnLocalServer, JvnRemoteServer{ 
	
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// A JVN server is managed as a singleton 
	private static JvnServerImpl js = null;

	// Reference distante vers le coordinateur JVN
	private JvnRemoteCoord jvnCoord;
	
	// Stocke les objets JVN locaux par leur identifiant pour pouvoir les retrouver
	// lors des demandes d'invalidation envoyees par le coordinateur.
	private Map<Integer, JvnObject> localObjects = new HashMap<>();

  /**
  * Default constructor
  * @throws JvnException
  **/
	private JvnServerImpl() throws Exception {
		super();
		// Recherche du coordinateur dans le registre RMI
    	jvnCoord = (JvnRemoteCoord) Naming.lookup("JvnCoord");
	}
	
  /**
    * Static method allowing an application to get a reference to 
    * a JVN server instance
    * @throws JvnException
    **/
	public static JvnServerImpl jvnGetServer() {
		if (js == null){
			try {
				js = new JvnServerImpl();
			} catch (Exception e) {
				return null;
			}
		}
		return js;
	}
	
	/**
	* The JVN service is not used anymore
	* @throws JvnException
	**/
	public  void jvnTerminate()
	throws jvn.JvnException {
        try {
			jvnCoord.jvnTerminate(this);
		} catch (RemoteException e) {
			throw new JvnException("Erreur RMI lors de la terminaison du serveur");
		}
	} 
	
	/**
	* creation of a JVN object
	* @param o : the JVN object state
	* @throws JvnException
	**/
	public  JvnObject jvnCreateObject(Serializable o)
	throws jvn.JvnException { 
		try {
			// Demande au coordinateur un nouvel identifiant unique
			int id = jvnCoord.jvnGetObjectId();
			// Creation de l'objet JVN avec un verrou d'ecriture W
			JvnObject jo = new JvnObjectImpl(o, id, this, true);

			localObjects.put(id, jo);

			return jo;

		} catch (java.rmi.RemoteException e) {
			throw new JvnException(
				"Erreur RMI lors de la creation de l'objet"
			);
		}
	}
	
	/**
	*  Associate a symbolic name with a JVN object
	* @param jon : the JVN object name
	* @param jo : the JVN object 
	* @throws JvnException
	**/
	public  void jvnRegisterObject(String jon, JvnObject jo)
	throws jvn.JvnException {
		    try {
				jvnCoord.jvnRegisterObject(jon, jo, this);
			} catch (java.rmi.RemoteException e) {
				throw new JvnException(
					"Erreur RMI lors de l'enregistrement de l'objet"
				);
			}
	}
	
	/**
	* Provide the reference of a JVN object beeing given its symbolic name
	* @param jon : the JVN object name
	* @return the JVN object 
	* @throws JvnException
	**/
	public  JvnObject jvnLookupObject(String jon)
	throws jvn.JvnException {
		try {
			// Demande l'objet au coordinateur grace a son nom
			JvnObject jo = jvnCoord.jvnLookupObject(jon, this);

			// Si l'objet existe, on le rattache a notre serveur local
			if (jo != null) {
				JvnObjectImpl obj = (JvnObjectImpl) jo;
				obj.setLocalServer(this);
				obj.resetLockState();

				localObjects.put(jo.jvnGetObjectId(), jo);
			}

			return jo;

		} catch (java.rmi.RemoteException e) {
			throw new JvnException(
				"Erreur RMI lors de la recherche de l'objet"
			);
		}
	}	
	
	/**
	* Get a Read lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
	public Serializable jvnLockRead(int joi)
	throws JvnException {

		try {
			return jvnCoord.jvnLockRead(joi, this);

		} catch (java.rmi.RemoteException e) {
			throw new JvnException(
				"Erreur RMI lors de la demande du verrou de lecture"
			);
		}
	}	
	/**
	* Get a Write lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
	public Serializable jvnLockWrite(int joi)
	throws JvnException {

		try {
			return jvnCoord.jvnLockWrite(joi, this);

		} catch (java.rmi.RemoteException e) {
			throw new JvnException(
				"Erreur RMI lors de la demande du verrou d'ecriture"
			);
		}
	}

	
  /**
	* Invalidate the Read lock of the JVN object identified by id 
	* called by the JvnCoord
	* @param joi : the JVN object id
	* @return void
	* @throws java.rmi.RemoteException,JvnException
	**/
  public void jvnInvalidateReader(int joi)
	throws java.rmi.RemoteException,jvn.JvnException {
		JvnObject jo = localObjects.get(joi);

		if (jo != null) {
			jo.jvnInvalidateReader();
		}
	};
	    
	/**
	* Invalidate the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
  public Serializable jvnInvalidateWriter(int joi)
	throws java.rmi.RemoteException,jvn.JvnException { 
		JvnObject jo = localObjects.get(joi);

		if (jo != null) {
			return jo.jvnInvalidateWriter();
		}

		return null;
	};
	
	/**
	* Reduce the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
   public Serializable jvnInvalidateWriterForReader(int joi)
	 throws java.rmi.RemoteException,jvn.JvnException { 
		JvnObject jo = localObjects.get(joi);

		if (jo != null) {
			return jo.jvnInvalidateWriterForReader();
		}

		return null;
	 };

}

 
