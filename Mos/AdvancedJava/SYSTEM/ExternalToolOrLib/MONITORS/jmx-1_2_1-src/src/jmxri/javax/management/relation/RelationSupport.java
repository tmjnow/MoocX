/*
 * @(#)file      RelationSupport.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.28
 * @(#)lastedit      03/07/15
 *
 * Copyright 2000-2003 Sun Microsystems, Inc.  All rights reserved.
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 * 
 * Copyright 2000-2003 Sun Microsystems, Inc.  Tous droits r�serv�s.
 * Ce logiciel est propriet� de Sun Microsystems, Inc.
 * Distribu� par des licences qui en restreignent l'utilisation. 
 */

package javax.management.relation;

import javax.management.ObjectName;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.InstanceNotFoundException;
import javax.management.ReflectionException;
import javax.management.MBeanException;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

import com.sun.jmx.trace.Trace;

/**
 * A RelationSupport object is used internally by the Relation Service to
 * represent simple relations (only roles, no properties or methods), with an
 * unlimited number of roles, of any relation type. As internal representation,
 * it is not exposed to the user.
 * <P>RelationSupport class conforms to the design patterns of standard MBean. So
 * the user can decide to instantiate a RelationSupport object himself as
 * a MBean (as it follows the MBean design patterns), to register it in the
 * MBean Server, and then to add it in the Relation Service.
 * <P>The user can also, when creating his own MBean relation class, have it
 * extending RelationSupport, to retrieve the implementations of required
 * interfaces (see below).
 * <P>It is also possible to have in a user relation MBean class a member
 * being a RelationSupport object, and to implement the required interfaces by
 * delegating all to this member.
 * <P> RelationSupport implements the Relation interface (to be handled by the
 * Relation Service).
 * <P>It implements also the MBeanRegistration interface to be able to retrieve
 * the MBean Server where it is registered (if registered as a MBean) to access
 * to its Relation Service.
 *
 * @since-jdkbundle 1.5
 */
public class RelationSupport
    implements RelationSupportMBean, MBeanRegistration {

    //
    // Private members
    //

    // Relation identifier (expected to be unique in the Relation Service where
    // the RelationSupport object will be added)
    private String myRelId = null;

    // ObjectName of the Relation Service where the relation will be added
    // REQUIRED if the RelationSupport is created by the user to be registered as
    // a MBean, as it will have to access the Relation Service via the MBean
    // Server to perform the check regarding the relation type.
    // Is null if current object is directly created by the Relation Service,
    // as the object will directly access it.
    private ObjectName myRelServiceName = null;

    // Reference to the MBean Server where the Relation Service is
    // registered
    // REQUIRED if the RelationSupport is created by the user to be registered as
    // a MBean, as it will have to access the Relation Service via the MBean
    // Server to perform the check regarding the relation type.
    // If the Relationbase object is created by the Relation Service (use of
    // createRelation() method), this is null as not needed, direct access to
    // the Relation Service.
    // If the Relationbase object is created by the user and registered as a
    // MBean, this is set by the preRegister() method below.
    private MBeanServer myRelServiceMBeanServer = null;

    // Relation type name (must be known in the Relation Service where the
    // relation will be added)
    private String myRelTypeName = null;

    // Role map, mapping <role-name> -> <Role>
    // Initialised by role list in the constructor, then updated:
    // - if the relation is a MBean, via setRole() and setRoles() methods, or
    //   via Relation Service setRole() and setRoles() methods
    // - if the relation is internal to the Relation Service, via
    //   setRoleInt() and setRolesInt() methods.
    private HashMap myRoleName2ValueMap = new HashMap();

    // Flag to indicate if the object has been added in the Relation Service
    private Boolean myInRelServFlg = null;

    //
    // Constructors
    //

    /**
     * Creates object.
     * <P>This constructor has to be used when the RelationSupport object will be
     * registered as a MBean by the user, or when creating a user relation
     * MBean those class extends RelationSupport.
     * <P>Nothing is done at the Relation Service level, i.e. the RelationSupport
     * object is not added, and no check if the provided values are correct.
     * The object is always created, EXCEPT if:
     * <P>- one mandatory parameter is not provided
     * <P>- the same name is used for two roles.
     * <P>To be handled as a relation, the object has then to be added in the
     * Relation Service using Relation Service method addRelation().
     *
     * @param theRelId  relation identifier, to identify the relation in the
     * Relation Service.
     * <P>Expected to be unique in the given Relation Service.
     * @param theRelServiceName  ObjectName of the Relation Service where
     * the relation will be registered.
     * <P>It is required as this is the Relation Service that is aware of the
     * definition of the relation type of given relation, so that will be able
     * to check update operations (set).
     * @param theRelTypeName  Name of relation type.
     * <P>Expected to have been created in given Relation Service.
     * @param theRoleList  list of roles (Role objects) to initialised the
     * relation. Can be null.
     * <P>Expected to conform to relation info in associated relation type.
     *
     * @exception InvalidRoleValueException  if the same name is used for two
     * roles.
     * @exception IllegalArgumentException  if a required value (Relation
     * Service Object Name, etc.) is not provided as parameter.
     */
    public RelationSupport(String theRelId,
			ObjectName theRelServiceName,
			String theRelTypeName,
			RoleList theRoleList)
	throws InvalidRoleValueException,
               IllegalArgumentException {

	super();

	if (isTraceOn())
            trace("Constructor: entering", null);

	// Can throw InvalidRoleValueException and IllegalArgumentException
	initMembers(theRelId,
		    theRelServiceName,
		    null,
		    theRelTypeName,
		    theRoleList);

	if (isTraceOn())
	    trace("Constructor: exiting", null);
    }

    /**
     * Creates object.
     * <P>This constructor has to be used when the user relation MBean
     * implements the interfaces expected to be supported by a relation by
     * delegating to a RelationSupport object.
     * <P>This object needs to know the Relation Service expected to handle the
     * relation. So it has to know the MBean Server where the Relation Service
     * is registered.
     * <P>According to a limitation, a relation MBean must be registered in the
     * same MBean Server as the Relation Service expected to handle it. So the
     * user relation MBean has to be created and registered, and then the
     * wrapped RelationSupport object can be created with identified MBean
     * Server.
     * <P>Nothing is done at the Relation Service level, i.e. the RelationSupport
     * object is not added, and no check if the provided values are correct.
     * The object is always created, EXCEPT if:
     * <P>- one required parameter is not provided
     * <P>- the same name is used for two roles.
     * <P>To be handled as a relation, the object has then to be added in the
     * Relation Service using the Relation Service method addRelation().
     *
     * @param theRelId  relation identifier, to identify the relation in the
     * Relation Service.
     * <P>Expected to be unique in the given Relation Service.
     * @param theRelServiceName  ObjectName of the Relation Service where
     * the relation will be registered.
     * <P>It is required as this is the Relation Service that is aware of the
     * definition of the relation type of given relation, so that will be able
     * to check update operations (set).
     * @param theRelServiceMBeanServer  MBean Server where the wrapping MBean
     * is or will be registered.
     * <P>Expected to be the MBean Server where the Relation Service is or will
     * be registered.
     * @param theRelTypeName  Name of relation type.
     * <P>Expected to have been created in given Relation Service.
     * @param theRoleList  list of roles (Role objects) to initialised the
     * relation. Can be null.
     * <P>Expected to conform to relation info in associated relation type.
     *
     * @exception InvalidRoleValueException  if the same name is used for two
     * roles.
     * @exception IllegalArgumentException  if a required value (Relation
     * Service Object Name, etc.) is not provided as parameter.
     */
    public RelationSupport(String theRelId,
			ObjectName theRelServiceName,
			MBeanServer theRelServiceMBeanServer,
			String theRelTypeName,
			RoleList theRoleList)
	throws InvalidRoleValueException,
               IllegalArgumentException {

	super();

	if (theRelServiceMBeanServer == null) {
	    // Revisit [cebro] Localize message
	    String excMsg = "Invalid parameter.";
	    throw new IllegalArgumentException(excMsg);
	}

	if (isTraceOn())
            trace("Constructor: entering", null);

	// Can throw InvalidRoleValueException and
	// IllegalArgumentException
	initMembers(theRelId,
		    theRelServiceName,
		    theRelServiceMBeanServer,
		    theRelTypeName,
		    theRoleList);

	if (isTraceOn())
	    trace("Constructor: exiting", null);
    }

    //
    // Relation Interface
    //

    /**
     * Retrieves role value for given role name.
     * <P>Checks if the role exists and is readable according to the relation
     * type.
     *
     * @param theRoleName  name of role
     *
     * @return the ArrayList of ObjectName objects being the role value
     *
     * @exception IllegalArgumentException  if null role name
     * @exception RoleNotFoundException  if:
     * <P>- there is no role with given name
     * <P>- the role is not readable.
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     *
     * @see #setRole
     */
    public List getRole(String theRoleName)
	throws IllegalArgumentException,
	       RoleNotFoundException,
	       RelationServiceNotRegisteredException {

	if (theRoleName == null) {
	    // Revisit [cebro] Localize message
	    String excMsg = "Invalid parameter.";
	    throw new IllegalArgumentException(excMsg);
	}

	if (isTraceOn())
	    trace("getRole: entering", theRoleName);

	// Can throw RoleNotFoundException and
	// RelationServiceNotRegisteredException
	ArrayList result = (ArrayList)
	    (getRoleInt(theRoleName, false, null, false));

	if (isTraceOn())
	    trace("getRole: exiting", null);
	return result;
    }

    /**
     * Retrieves values of roles with given names.
     * <P>Checks for each role if it exists and is readable according to the
     * relation type.
     *
     * @param theRoleNameArray  array of names of roles to be retrieved
     *
     * @return a RoleResult object, including a RoleList (for roles
     * succcessfully retrieved) and a RoleUnresolvedList (for roles not
     * retrieved).
     *
     * @exception IllegalArgumentException  if null role name
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     *
     * @see #setRoles
     */
    public RoleResult getRoles(String[] theRoleNameArray)
	throws IllegalArgumentException,
	       RelationServiceNotRegisteredException {

	if (theRoleNameArray == null) {
	    // Revisit [cebro] Localize message
	    String excMsg = "Invalid parameter.";
	    throw new IllegalArgumentException(excMsg);
	}

	if (isTraceOn())
            trace("getRoles: entering", null);

	// Can throw RelationServiceNotRegisteredException
	RoleResult result = getRolesInt(theRoleNameArray, false, null);

	if (isTraceOn())
            trace("getRoles: exiting", null);
	return result;
    }

    /**
     * Returns all roles present in the relation.
     *
     * @return a RoleResult object, including a RoleList (for roles
     * succcessfully retrieved) and a RoleUnresolvedList (for roles not
     * readable).
     *
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     */
    public RoleResult getAllRoles()
    	throws RelationServiceNotRegisteredException {

	if (isTraceOn())
            trace("getAllRoles: entering", null);

	RoleResult result = null;
	try {
	    result = getAllRolesInt(false, null);
	} catch (IllegalArgumentException exc) {
	    // OK : Invalid parameters, ignore...
	}

	if (isTraceOn())
            trace("getAllRoles: exiting", null);
	return result;
    }

    /**
     * Returns all roles in the relation without checking read mode.
     *
     * @return a RoleList
     */
    public RoleList retrieveAllRoles() {

	if (isTraceOn())
            trace("retrieveAllRoles: entering", null);

	RoleList result = null;
	synchronized(myRoleName2ValueMap) {
	    result =
		new RoleList(new ArrayList(myRoleName2ValueMap.values()));
	}

	if (isTraceOn())
            trace("retrieveAllRoles: exiting", null);
	return result;
    }

    /**
     * Returns the number of MBeans currently referenced in the given role.
     *
     * @param theRoleName  name of role
     *
     * @return the number of currently referenced MBeans in that role
     *
     * @exception IllegalArgumentException  if null role name
     * @exception RoleNotFoundException  if there is no role with given name
     */
    public Integer getRoleCardinality(String theRoleName)
	throws IllegalArgumentException,
	       RoleNotFoundException {

	if (theRoleName == null) {
	    // Revisit [cebro] Localize message
	    String excMsg = "Invalid parameter.";
	    throw new IllegalArgumentException(excMsg);
	}

	if (isTraceOn())
            trace("getRoleCardinality: entering", theRoleName);

	// Try to retrieve the role
	Role role = null;
	synchronized(myRoleName2ValueMap) {
	    // No null Role is allowed, so direct use of get()
	    role = (Role)(myRoleName2ValueMap.get(theRoleName));
	}
	if (role == null) {
	    int pbType = RoleStatus.NO_ROLE_WITH_NAME;
	    // Will throw a RoleNotFoundException
	    //
	    // Will not throw InvalidRoleValueException, so catch it for the
	    // compiler
	    try {
		RelationService.throwRoleProblemException(pbType,
							  theRoleName);
	    } catch (InvalidRoleValueException exc) {
		// OK : Do not throw InvalidRoleValueException as
		//      a RoleNotFoundException will be thrown.
	    }
	}

	ArrayList roleValue = (ArrayList)(role.getRoleValue());

	if (isTraceOn())
            trace("getRoleCardinality: exiting", null);
	return new Integer(roleValue.size());
    }

    /**
     * Sets the given role.
     * <P>Will check the role according to its corresponding role definition
     * provided in relation's relation type
     * <P>Will send a notification (RelationNotification with type
     * RELATION_BASIC_UPDATE or RELATION_MBEAN_UPDATE, depending if the
     * relation is a MBean or not).
     *
     * @param theRole  role to be set (name and new value)
     *
     * @exception IllegalArgumentException  if null role
     * @exception RoleNotFoundException  if the role is not writable (no
     * test on the write access mode performed when initialising the role)
     * @exception InvalidRoleValueException  if value provided for
     * role is not valid, i.e.:
     * <P>- the number of referenced MBeans in given value is less than
     * expected minimum degree
     * <P>- the number of referenced MBeans in provided value exceeds expected
     * maximum degree
     * <P>- one referenced MBean in the value is not an Object of the MBean
     * class expected for that role
     * <P>- a MBean provided for that role does not exist
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception RelationTypeNotFoundException  if the relation type has not
     * been declared in the Relation Service
     * @exception RelationNotFoundException  if the relation has not been
     * added in the Relation Service.
     *
     * @see #getRole
     */
    public void setRole(Role theRole)
	throws IllegalArgumentException,
	       RoleNotFoundException,
	       RelationTypeNotFoundException,
	       InvalidRoleValueException,
	       RelationServiceNotRegisteredException,
               RelationNotFoundException {

	if (theRole == null) {
	    // Revisit [cebro] Localize message
	    String excMsg = "Invalid parameter.";
	    throw new IllegalArgumentException(excMsg);
	}

	if (isTraceOn())
            trace("setRole: entering", theRole.toString());

	// Will return null :)
	Object result = setRoleInt(theRole, false, null, false);

	if (isTraceOn())
            trace("setRole: exiting", null);
	return;
    }

    /**
     * Sets the given roles.
     * <P>Will check the role according to its corresponding role definition
     * provided in relation's relation type
     * <P>Will send one notification (RelationNotification with type
     * RELATION_BASIC_UPDATE or RELATION_MBEAN_UPDATE, depending if the
     * relation is a MBean or not) per updated role.
     *
     * @param theRoleList  list of roles to be set
     *
     * @return a RoleResult object, including a RoleList (for roles
     * succcessfully set) and a RoleUnresolvedList (for roles not
     * set).
     *
     * @exception IllegalArgumentException  if null role name
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception RelationTypeNotFoundException  if the relation type has not
     * been declared in the Relation Service.
     * @exception RelationNotFoundException  if the relation MBean has not been
     * added in the Relation Service.
     *
     * @see #getRoles
     */
    public RoleResult setRoles(RoleList theRoleList)
	throws IllegalArgumentException,
	       RelationServiceNotRegisteredException,
               RelationTypeNotFoundException,
               RelationNotFoundException {

	if (theRoleList == null) {
	    // Revisit [cebro] Localize message
	    String excMsg = "Invalid parameter.";
	    throw new IllegalArgumentException(excMsg);
	}

	if (isTraceOn())
            trace("setRoles: entering", theRoleList.toString());

	RoleResult result = setRolesInt(theRoleList, false, null);

	if (isTraceOn())
            trace("setRoles: exiting", null);
	return result;
    }

    /**
     * Callback used by the Relation Service when a MBean referenced in a role
     * is unregistered.
     * <P>The Relation Service will call this method to let the relation
     * take action to reflect the impact of such unregistration.
     * <P>BEWARE. the user is not expected to call this method.
     * <P>Current implementation is to set the role with its current value
     * (list of ObjectNames of referenced MBeans) without the unregistered
     * one.
     *
     * @param theObjName  ObjectName of unregistered MBean
     * @param theRoleName  name of role where the MBean is referenced
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RoleNotFoundException  if role does not exist in the
     * relation or is not writable
     * @exception InvalidRoleValueException  if role value does not conform to
     * the associated role info (this will never happen when called from the
     * Relation Service)
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception RelationTypeNotFoundException  if the relation type has not
     * been declared in the Relation Service.
     * @exception RelationNotFoundException  if this method is called for a
     * relation MBean not added in the Relation Service.
     */
    public void handleMBeanUnregistration(ObjectName theObjName,
					  String theRoleName)
	throws IllegalArgumentException,
               RoleNotFoundException,
               InvalidRoleValueException,
               RelationServiceNotRegisteredException,
               RelationTypeNotFoundException,
               RelationNotFoundException {

	if (theObjName == null || theRoleName == null) {
	    // Revisit [cebro] Localize message
	    String excMsg = "Invalid parameter.";
	    throw new IllegalArgumentException(excMsg);
	}

	if (isTraceOn())
            trace("handleMBeanUnregistration: entering",
		  "theObjName " + theObjName + ", theRoleName " + theRoleName);

	// Can throw RoleNotFoundException, InvalidRoleValueException,
	// or RelationTypeNotFoundException
	handleMBeanUnregistrationInt(theObjName,
				     theRoleName,
				     false,
				     null);

	if (isTraceOn())
            trace("handleMBeanUnregistration: exiting", null);
	return;
    }

    /**
     * Retrieves MBeans referenced in the various roles of the relation.
     *
     * @return a HashMap mapping:
     * <P> ObjectName -> ArrayList of String (role names)
     */
    public Map getReferencedMBeans() {

	if (isTraceOn())
            trace("getReferencedMBeans: entering", null);

	HashMap refMBeanMap = new HashMap();

	synchronized(myRoleName2ValueMap) {

	    for (Iterator roleIter = (myRoleName2ValueMap.values()).iterator();
		 roleIter.hasNext();) {

		Role currRole = (Role)(roleIter.next());

		String currRoleName = currRole.getRoleName();
		// Retrieves ObjectNames of MBeans referenced in current role
		ArrayList currRefMBeanList = (ArrayList)
		    (currRole.getRoleValue());

		for (Iterator mbeanIter = currRefMBeanList.iterator();
		     mbeanIter.hasNext();) {

		    ObjectName currRoleObjName =
			(ObjectName)(mbeanIter.next());

		    // Sees if current MBean has been already referenced in
		    // roles already seen
		    ArrayList mbeanRoleNameList =
			(ArrayList)(refMBeanMap.get(currRoleObjName));

		    boolean newRefFlg = false;
		    if (mbeanRoleNameList == null) {
			newRefFlg = true;
			mbeanRoleNameList = new ArrayList();
		    }
		    mbeanRoleNameList.add(currRoleName);
		    if (newRefFlg) {
			refMBeanMap.put(currRoleObjName, mbeanRoleNameList);
		    }
		}
	    }
	}

	if (isTraceOn())
            trace("getReferencedMBeans: exiting", null);
	return refMBeanMap;		    
    }

    /**
     * Returns name of associated relation type.
     */
    public String getRelationTypeName() {
	return myRelTypeName;
    }

    /**
     * Returns ObjectName of the Relation Service handling the relation.
     *
     * @return the ObjectName of the Relation Service.
     */
    public ObjectName getRelationServiceName() {
	return myRelServiceName;
    }

    /**
     * Returns relation identifier (used to uniquely identify the relation
     * inside the Relation Service).
     *
     * @return the relation id.
     */
    public String getRelationId() {
	return myRelId;
    }

    //
    // MBeanRegistration interface
    //

    // Pre-registration: retrieves the MBean Server (useful to access to the
    // Relation Service)
    // This is the way to retrieve the MBean Server when the relation object is
    // a MBean created by the user outside of the Relation Service.
    //
    // No exception thrown.
    public ObjectName preRegister(MBeanServer server,
				  ObjectName name)
	throws Exception {

	myRelServiceMBeanServer = server;
	return name;
    }

    // Post-registration: does nothing
    public void postRegister(Boolean registrationDone) {
	return;
    }

    // Pre-unregistration: does nothing
    public void preDeregister()
	throws Exception {
	return;
    }

    // Post-unregistration: does nothing
    public void postDeregister() {
	return;
    }

    //
    // Others
    //

    /**
     * Returns an internal flag specifying if the object is still handled by
     * the Relation Service.
     */
    public Boolean isInRelationService() {
	Boolean result = null;
	synchronized(myInRelServFlg) {
	    result = new Boolean(myInRelServFlg.booleanValue());
	}
	return result;
    }

    public void setRelationServiceManagementFlag(Boolean theFlg)
	throws IllegalArgumentException {

	if (theFlg == null) {
	    // Revisit [cebro] Localize message
	    String excMsg = "Invalid parameter.";
	    throw new IllegalArgumentException(excMsg);
	}
	synchronized(myInRelServFlg) {
	    myInRelServFlg = new Boolean(theFlg.booleanValue());
	}
	return;
    }

    //
    // Misc
    //

    // Gets the role with given name
    // Checks if the role exists and is readable according to the relation
    // type.
    //
    // This method is called in getRole() above.
    // It is also called in the Relation Service getRole() method.
    // It is also called in getRolesInt() below (used for getRoles() above
    // and for Relation Service getRoles() method).
    //
    // Depending on parameters reflecting its use (either in the scope of
    // getting a single role or of getting several roles), will return:
    // - in case of success:
    //   - for single role retrieval, the ArrayList of ObjectNames being the
    //     role value
    //   - for multi-role retrieval, the Role object itself
    // - in case of failure (except critical exceptions):
    //   - for single role retrieval, if role does not exist or is not
    //     readable, an RoleNotFoundException exception is raised
    //   - for multi-role retrieval, a RoleUnresolved object
    //
    // -param theRoleName  name of role to be retrieved
    // -param theRelServCallFlg  true if call from the Relation Service; this
    //  will happen if the current RelationSupport object has been created by
    //  the Relation Service (via createRelation()) method, so direct access is
    //  possible.
    // -param theRelServ  reference to Relation Service object, if object
    //  created by Relation Service.
    // -param theMultiRoleFlg  true if getting the role in the scope of a
    //  multiple retrieval.
    //
    // -return:
    //  - for single role retrieval (theMultiRoleFlg false):
    //    - ArrayList of ObjectName objects, value of role with given name, if
    //      the role can be retrieved
    //    - raise a RoleNotFoundException exception else
    //  - for multi-role retrieval (theMultiRoleFlg true):
    //    - the Role object for given role name if role can be retrieved
    //    - a RoleUnresolved object with problem.
    //
    // -exception IllegalArgumentException  if null parameter
    // -exception RoleNotFoundException  if theMultiRoleFlg is false and:
    //  - there is no role with given name
    //  or
    //  - the role is not readable.
    // -exception RelationServiceNotRegisteredException  if the Relation
    //  Service is not registered in the MBean Server
    Object getRoleInt(String theRoleName,
		      boolean theRelServCallFlg,
		      RelationService theRelServ,
		      boolean theMultiRoleFlg)
	throws IllegalArgumentException,
	       RoleNotFoundException,
	       RelationServiceNotRegisteredException {

	if (theRoleName == null ||
	    (theRelServCallFlg && theRelServ == null)) {
	    // Revisit [cebro] Localize message
	    String excMsg = "Invalid parameter.";
	    throw new IllegalArgumentException(excMsg);
	}

	if (isDebugOn()) {
	    String str = "theRoleName " + theRoleName;
	    debug("getRoleInt: entering", str);
	}

	int pbType = 0;

	Role role = null;
	synchronized(myRoleName2ValueMap) {
	    // No null Role is allowed, so direct use of get()
	    role = (Role)(myRoleName2ValueMap.get(theRoleName));
	}

	if (role == null) {
		pbType = RoleStatus.NO_ROLE_WITH_NAME;

	} else {
	    // Checks if the role is readable
	    Integer status = null;

	    if (theRelServCallFlg) {

		// Call from the Relation Service, so direct access to it,
		// avoiding MBean Server
		// Shall not throw a RelationTypeNotFoundException
		try {
		    status = theRelServ.checkRoleReading(theRoleName,
							 myRelTypeName);
		} catch (RelationTypeNotFoundException exc) {
		    throw new RuntimeException(exc.getMessage());
		}

	    } else {

		// Call from getRole() method above
		// So we have a MBean. We must access the Relation Service
		// via the MBean Server.		
		Object[] params = new Object[2];
		params[0] = theRoleName;
		params[1] = myRelTypeName;
		String[] signature = new String[2];
		signature[0] = "java.lang.String";
		signature[1] = "java.lang.String";
		// Can throw InstanceNotFoundException if the Relation
		// Service is not registered (to be catched in any case and
		// transformed into RelationServiceNotRegisteredException).
		//
		// Shall not throw a MBeanException, or a ReflectionException
		// or an InstanceNotFoundException
		try {
		    status = (Integer)
			(myRelServiceMBeanServer.invoke(myRelServiceName,
							"checkRoleReading",
							params,
							signature));
		} catch (MBeanException exc1) {
		    throw new RuntimeException("incorrect relation type");
		} catch (ReflectionException exc2) {
		    throw new RuntimeException(exc2.getMessage());
		} catch (InstanceNotFoundException exc3) {
		    throw new RelationServiceNotRegisteredException(
							    exc3.getMessage());
		}
	    }

	    pbType = status.intValue();
	}

	Object result = null;

	if (pbType == 0) {
	    // Role can be retrieved

	    if (!(theMultiRoleFlg)) {
		// Single role retrieved: returns its value
		// Note: no need to test if role value (list) not null before
		//       cloning, null value not allowed, empty list if
		//       nothing.
		result = (ArrayList)
		    (((ArrayList)(role.getRoleValue())).clone());

	    } else {
		// Role retrieved during multi-role retrieval: returns the
		// role
		result = (Role)(role.clone());
	    }

	} else {
	    // Role not retrieved

	    if (!(theMultiRoleFlg)) {
		// Problem when retrieving a simple role: either role not
		// found or not readable, so raises a RoleNotFoundException.
		try {
		    RelationService.throwRoleProblemException(pbType,
							      theRoleName);
		    // To keep compiler happy :)
		    return null;
		} catch (InvalidRoleValueException exc) {
		    throw new RuntimeException(exc.getMessage());
		}

	    } else {
		// Problem when retrieving a role in a multi-role retrieval:
		// returns a RoleUnresolved object
		result = new RoleUnresolved(theRoleName, null, pbType);
	    }
	}

	if (isDebugOn())
	    debug("getRoleInt: exiting", null);	
	return result;
    }

    // Gets the given roles
    // For each role, verifies if the role exists and is readable according to
    // the relation type.
    //
    // This method is called in getRoles() above and in Relation Service
    // getRoles() method.
    //
    // -param theRoleNameArray  array of names of roles to be retrieved
    // -param theRelServCallFlg  true if call from the Relation Service; this
    //  will happen if the current RelationSupport object has been created by
    //  the Relation Service (via createRelation()) method, so direct access is
    //  possible.
    // -param theRelServ  reference to Relation Service object, if object
    //  created by Relation Service.
    //
    // -return a RoleResult object
    //
    // -exception IllegalArgumentException  if null parameter
    // -exception RelationServiceNotRegisteredException  if the Relation
    //  Service is not registered in the MBean Server
    RoleResult getRolesInt(String[] theRoleNameArray,
			   boolean theRelServCallFlg,
			   RelationService theRelServ)
	throws IllegalArgumentException,
	       RelationServiceNotRegisteredException {

	if (theRoleNameArray == null ||
	    (theRelServCallFlg && theRelServ == null)) {
	    // Revisit [cebro] Localize message
	    String excMsg = "Invalid parameter.";
	    throw new IllegalArgumentException(excMsg);
	}

	if (isDebugOn())
	    debug("getRolesInt: entering", null);

	RoleList roleList = new RoleList();
	RoleUnresolvedList roleUnresList = new RoleUnresolvedList();

	for (int i = 0; i < theRoleNameArray.length; i++) {
	    String currRoleName = theRoleNameArray[i];

	    Object currResult = null;

	    // Can throw RelationServiceNotRegisteredException
	    //
	    // RoleNotFoundException: not possible but catch it for compiler :)
	    try {
		currResult = getRoleInt(currRoleName,
					theRelServCallFlg,
					theRelServ,
					true);

	    } catch (RoleNotFoundException exc) {
		return null; // :)
	    }

	    if (currResult instanceof Role) {
		// Can throw IllegalArgumentException if role is null
		// (normally should not happen :(
		try {
		    roleList.add((Role)currResult);
		} catch (IllegalArgumentException exc) {
		    throw new RuntimeException(exc.getMessage());
		}

	    } else if (currResult instanceof RoleUnresolved) {
		// Can throw IllegalArgumentException if role is null
		// (normally should not happen :(
		try {
		    roleUnresList.add((RoleUnresolved)currResult);
		} catch (IllegalArgumentException exc) {
		    throw new RuntimeException(exc.getMessage());
		}
	    }
	}

	RoleResult result = new RoleResult(roleList, roleUnresList);
	if (isDebugOn())
	    debug("getRolesInt: exiting", null);
	return result;
    }

    // Returns all roles present in the relation
    //
    // -return a RoleResult object, including a RoleList (for roles
    //  succcessfully retrieved) and a RoleUnresolvedList (for roles not
    //  readable).
    //
    // -exception IllegalArgumentException if null parameter
    // -exception RelationServiceNotRegisteredException  if the Relation
    //  Service is not registered in the MBean Server
    //
    RoleResult getAllRolesInt(boolean theRelServCallFlg,
			      RelationService theRelServ)
	throws IllegalArgumentException,
	       RelationServiceNotRegisteredException {

	if (theRelServCallFlg && theRelServ == null) {
	    // Revisit [cebro] Localize message
	    String excMsg = "Invalid parameter.";
	    throw new IllegalArgumentException(excMsg);
	}

	if (isDebugOn())
	    debug("getAllRolesInt: entering", null);

	ArrayList roleNameList = null;
	synchronized(myRoleName2ValueMap) {
	    roleNameList =
		new ArrayList(myRoleName2ValueMap.keySet());
	}
	String[] roleNames = new String[roleNameList.size()];
	int i = 0;
	for (Iterator roleNameIter = roleNameList.iterator();
	     roleNameIter.hasNext();) {
	    String currRoleName = (String)(roleNameIter.next());
	    roleNames[i] = currRoleName;
	    i++;
	}

	RoleResult result = getRolesInt(roleNames,
					theRelServCallFlg,
					theRelServ);

	if (isDebugOn())
	    debug("getAllRolesInt: exiting", null);
	return result;
    }

    // Sets the role with given value
    //
    // This method is called in setRole() above.
    // It is also called by the Relation Service setRole() method.
    // It is also called in setRolesInt() method below (used in setRoles()
    // above and in RelationService setRoles() method).
    //
    // Will check the role according to its corresponding role definition
    // provided in relation's relation type
    // Will send a notification (RelationNotification with type
    // RELATION_BASIC_UPDATE or RELATION_MBEAN_UPDATE, depending if the
    // relation is a MBean or not) if not initialisation of role.
    //
    // -param theRole  role to be set (name and new value)
    // -param theRelServCallFlg  true if call from the Relation Service; this
    //  will happen if the current RelationSupport object has been created by
    //  the Relation Service (via createRelation()) method, so direct access is
    //  possible.
    // -param theRelServ  reference to Relation Service object, if internal
    //  relation
    // -param theMultiRoleFlg  true if getting the role in the scope of a
    //  multiple retrieval.
    //
    // -return (except other "critical" exceptions):
    //  - for single role retrieval (theMultiRoleFlg false):
    //    - null if the role has been set
    //    - raise an InvalidRoleValueException
    // else
    //  - for multi-role retrieval (theMultiRoleFlg true):
    //    - the Role object for given role name if role has been set
    //    - a RoleUnresolved object with problem else.
    //
    // -exception IllegalArgumentException if null parameter
    // -exception RoleNotFoundException  if theMultiRoleFlg is false and:
    //  - internal relation and the role does not exist
    //  or
    //  - existing role (i.e. not initialising it) and the role is not
    //    writable.
    // -exception InvalidRoleValueException  iftheMultiRoleFlg is false and
    //  value provided for:
    //   - the number of referenced MBeans in given value is less than
    //     expected minimum degree
    //   or
    //   - the number of referenced MBeans in provided value exceeds expected
    //     maximum degree
    //   or
    //   - one referenced MBean in the value is not an Object of the MBean
    //     class expected for that role
    //   or
    //   - a MBean provided for that role does not exist
    // -exception RelationServiceNotRegisteredException  if the Relation
    //  Service is not registered in the MBean Server
    // -exception RelationTypeNotFoundException  if relation type unknown
    // -exception RelationNotFoundException  if a relation MBean has not been
    //  added in the Relation Service
    Object setRoleInt(Role theRole,
		      boolean theRelServCallFlg,
		      RelationService theRelServ,
		      boolean theMultiRoleFlg)
	throws IllegalArgumentException,
	       RoleNotFoundException,
	       InvalidRoleValueException,
	       RelationServiceNotRegisteredException,
	       RelationTypeNotFoundException,
               RelationNotFoundException {

	if (theRole == null ||
	    (theRelServCallFlg && theRelServ == null)) {
	    // Revisit [cebro] Localize message
	    String excMsg = "Invalid parameter.";
	    throw new IllegalArgumentException(excMsg);
	}

	if (isDebugOn()) {
	    String str =
		"theRole " + theRole
		+ ", theRelServCallFlg " + theRelServCallFlg
		+ ", theMultiRoleFlg " + theMultiRoleFlg;
	    debug("setRoleInt: entering" , str);
	}

	String roleName = theRole.getRoleName();
	int pbType = 0;

	// Checks if role exists in the relation
	// No error if the role does not exist in the relation, to be able to
	// handle initialisation of role when creating the relation
	// (roles provided in the RoleList parameter are directly set but
	// roles automatically initialised are set using setRole())
	Role role = null;
	synchronized(myRoleName2ValueMap) {
	    role = (Role)(myRoleName2ValueMap.get(roleName));
	}

	ArrayList oldRoleValue = null;
	Boolean initFlg = null;

	if (role == null) {
	    initFlg = new Boolean(true);
	    oldRoleValue = new ArrayList();

	} else {
	    initFlg = new Boolean(false);
	    oldRoleValue = (ArrayList)(role.getRoleValue());
	}

	// Checks if the role can be set: is writable (except if
	// initialisation) and correct value
	try {
	    Integer status = null;

	    if (theRelServCallFlg) {

		// Call from the Relation Service, so direct access to it,
		// avoiding MBean Server
		//
		// Shall not raise a RelationTypeNotFoundException
		status = theRelServ.checkRoleWriting(theRole,
						     myRelTypeName,
						     initFlg);

	    } else {

		// Call from setRole() method above
		// So we have a MBean. We must access the Relation Service
		// via the MBean Server.		
		Object[] params = new Object[3];
		params[0] = theRole;
		params[1] = myRelTypeName;
		params[2] = initFlg;
		String[] signature = new String[3];
		signature[0] = "javax.management.relation.Role";
		signature[1] = "java.lang.String";
		signature[2] = "java.lang.Boolean";
		// Can throw InstanceNotFoundException if the Relation Service
		// is not registered (to be transformed into
		// RelationServiceNotRegisteredException in any case).
		//
		// Can throw a MBeanException wrapping a
		// RelationTypeNotFoundException:
		// throw wrapped exception.
		//
		// Shall not throw a ReflectionException
		status = (Integer)
		    (myRelServiceMBeanServer.invoke(myRelServiceName,
						    "checkRoleWriting",
						    params,
						    signature));
	    }

	    pbType = status.intValue();

	} catch (MBeanException exc2) {

	    // Retrieves underlying exception
	    Exception wrappedExc = exc2.getTargetException();
	    if (wrappedExc instanceof RelationTypeNotFoundException) {
		throw ((RelationTypeNotFoundException)wrappedExc);

	    } else {
		throw new RuntimeException(wrappedExc.getMessage());
	    }

	} catch (ReflectionException exc3) {
	    throw new RuntimeException(exc3.getMessage());

	} catch (RelationTypeNotFoundException exc4) {
	    throw new RuntimeException(exc4.getMessage());

	} catch (InstanceNotFoundException exc5) {
	    throw new RelationServiceNotRegisteredException(exc5.getMessage());
	}

	Object result = null;

	if (pbType == 0) {
	    // Role can be set
	    if (!(initFlg.booleanValue())) {

		// Not initialising the role
		// If role being initialised:
		// - do not send an update notification
		// - do not try to update internal map of Relation Service
		//   listing referenced MBeans, as role is initialised to an
		//   empty list

		// Sends a notification (RelationNotification)
		// Can throw a RelationNotFoundException
		sendRoleUpdateNotification(theRole,
					   oldRoleValue,
					   theRelServCallFlg,
					   theRelServ);

		// Updates the role map of the Relation Service
		// Can throw RelationNotFoundException
		updateRelationServiceMap(theRole,
					 oldRoleValue,
					 theRelServCallFlg,
					 theRelServ);

	    }

	    // Sets the role
	    synchronized(myRoleName2ValueMap) {
		myRoleName2ValueMap.put(roleName,
					(Role)(theRole.clone()));
	    }

	    // Single role set: returns null: nothing to set in result

	    if (theMultiRoleFlg) {
		// Multi-roles retrieval: returns the role
		result = theRole;
	    }

	} else {

	    // Role not set

	    if (!(theMultiRoleFlg)) {
		// Problem when setting a simple role: either role not
		// found, not writable, or incorrect value:
		// raises appropriate exception, RoleNotFoundException or
		// InvalidRoleValueException
		RelationService.throwRoleProblemException(pbType,
							  roleName);
		// To keep compiler happy :)
		return null;

	    } else {
		// Problem when retrieving a role in a multi-role retrieval:
		// returns a RoleUnresolved object
		result = new RoleUnresolved(roleName,
					    theRole.getRoleValue(),
					    pbType);
	    }
	}

	if (isDebugOn())
	    debug("setRoleInt: exiting", null);
	return result;
    }

    // Requires the Relation Service to send a notification
    // RelationNotification, with type being either:
    // - RelationNotification.RELATION_BASIC_UPDATE if the updated relation is
    //   a relation internal to the Relation Service
    // - RelationNotification.RELATION_MBEAN_UPDATE if the updated relation is
    //   a relation MBean.
    //
    // -param theNewRole  new role
    // -param theOldRoleValue  old role value (ArrayList of ObjectNames)
    // -param theRelServCallFlg  true if call from the Relation Service; this
    //  will happen if the current RelationSupport object has been created by
    //  the Relation Service (via createRelation()) method, so direct access is
    //  possible.
    // -param theRelServ  reference to Relation Service object, if object
    //  created by Relation Service.
    //
    // -exception IllegalArgumentException  if null parameter provided
    // -exception RelationServiceNotRegisteredException  if the Relation
    //  Service is not registered in the MBean Server
    // -exception RelationNotFoundException if:
    //  - relation MBean
    //  and
    //  - it has not been added into the Relation Service
    private void sendRoleUpdateNotification(Role theNewRole,
					    List theOldRoleValue,
					    boolean theRelServCallFlg,
					    RelationService theRelServ)
	throws IllegalArgumentException,
	       RelationServiceNotRegisteredException,
	       RelationNotFoundException {

	if (theNewRole == null ||
	    theOldRoleValue == null ||
	    (theRelServCallFlg && theRelServ == null)) {
	    // Revisit [cebro] Localize message
	    String excMsg = "Invalid parameter.";
	    throw new IllegalArgumentException(excMsg);
	}

	if (isDebugOn()) {
	    String str =
		"theNewRole " + theNewRole
		+ ", theOldRoleValue " + theOldRoleValue
		+ ", theRelServCallFlg " + theRelServCallFlg;
	    debug("sendRoleUpdateNotification: entering", str);
	}

	if (theRelServCallFlg) {
	    // Direct call to the Relation Service
	    // Shall not throw a RelationNotFoundException for an internal
	    // relation
	    try {
		theRelServ.sendRoleUpdateNotification(myRelId,
						      theNewRole,
						      theOldRoleValue);
	    } catch (RelationNotFoundException exc) {
		throw new RuntimeException(exc.getMessage());
	    }

	} else {

	    Object[] params = new Object[3];
	    params[0] = myRelId;
	    params[1] = theNewRole;
	    params[2] = ((ArrayList)theOldRoleValue);
	    String[] signature = new String[3];
	    signature[0] = "java.lang.String";
	    signature[1] = "javax.management.relation.Role";
	    signature[2] = "java.util.List";

	    // Can throw InstanceNotFoundException if the Relation Service
	    // is not registered (to be transformed).
	    //
	    // Can throw a MBeanException wrapping a
	    // RelationNotFoundException (to be raised in any case): wrapped
	    // exception to be thrown
	    //
	    // Shall not throw a ReflectionException
	    try {
		myRelServiceMBeanServer.invoke(myRelServiceName,
					       "sendRoleUpdateNotification",
					       params,
					       signature);
	    } catch (ReflectionException exc1) {
		throw new RuntimeException(exc1.getMessage());
	    } catch (InstanceNotFoundException exc2) {
		throw new RelationServiceNotRegisteredException(
							    exc2.getMessage());
	    } catch (MBeanException exc3) {
		Exception wrappedExc = exc3.getTargetException();
		if (wrappedExc instanceof RelationNotFoundException) {
		    throw ((RelationNotFoundException)wrappedExc);
		} else {
		    throw new RuntimeException(wrappedExc.getMessage());
		}
	    }
	}

	if (isDebugOn())
	    debug("sendRoleUpdateNotification: exiting", null);
	return;
    }

    // Requires the Relation Service to update its internal map handling
    // MBeans referenced in relations.
    // The Relation Service will also update its recording as a listener to
    // be informed about unregistration of new referenced MBeans, and no longer
    // informed of MBeans no longer referenced.
    //
    // -param theNewRole  new role
    // -param theOldRoleValue  old role value (ArrayList of ObjectNames)
    // -param theRelServCallFlg  true if call from the Relation Service; this
    //  will happen if the current RelationSupport object has been created by
    //  the Relation Service (via createRelation()) method, so direct access is
    //  possible.
    // -param theRelServ  reference to Relation Service object, if object
    //  created by Relation Service.
    //
    // -exception IllegalArgumentException  if null parameter
    // -exception RelationServiceNotRegisteredException  if the Relation
    //  Service is not registered in the MBean Server
    // -exception RelationNotFoundException if:
    //  - relation MBean
    //  and
    //  - the relation is not added in the Relation Service
    private void updateRelationServiceMap(Role theNewRole,
					  List theOldRoleValue,
					  boolean theRelServCallFlg,
					  RelationService theRelServ)
	throws IllegalArgumentException,
	       RelationServiceNotRegisteredException,
	       RelationNotFoundException {

	if (theNewRole == null ||
	    theOldRoleValue == null ||
	    (theRelServCallFlg && theRelServ == null)) {
	    // Revisit [cebro] Localize message
	    String excMsg = "Invalid parameter.";
	    throw new IllegalArgumentException(excMsg);
	}

	if (isDebugOn()) {
	    String str =
		"theNewRole " + theNewRole
		+ ", theOldRoleValue " + theOldRoleValue
		+ ", theRelServCallFlg " + theRelServCallFlg;
	    debug("updateRelationServiceMap: entering", str);
	}

	if (theRelServCallFlg) {
	    // Direct call to the Relation Service
	    // Shall not throw a RelationNotFoundException
	    try {
		theRelServ.updateRoleMap(myRelId,
					 theNewRole,
					 theOldRoleValue);
	    } catch (RelationNotFoundException exc) {
		throw new RuntimeException(exc.getMessage());
	    }

	} else {
	    Object[] params = new Object[3];
	    params[0] = myRelId;
	    params[1] = theNewRole;
	    params[2] = theOldRoleValue;
	    String[] signature = new String[3];
	    signature[0] = "java.lang.String";
	    signature[1] = "javax.management.relation.Role";
	    signature[2] = "java.util.List";
	    // Can throw InstanceNotFoundException if the Relation Service
	    // is not registered (to be transformed).
	    // Can throw a MBeanException wrapping a RelationNotFoundException:
	    // wrapped exception to be thrown
	    //
	    // Shall not throw a ReflectionException
	    try {
		myRelServiceMBeanServer.invoke(myRelServiceName,
					       "updateRoleMap",
					       params,
					       signature);
	    } catch (ReflectionException exc1) {
		throw new RuntimeException(exc1.getMessage());
	    } catch (InstanceNotFoundException exc2) {
		throw new
		     RelationServiceNotRegisteredException(exc2.getMessage());
	    } catch (MBeanException exc3) {
		Exception wrappedExc = exc3.getTargetException();
		if (wrappedExc instanceof RelationNotFoundException) {
		    throw ((RelationNotFoundException)wrappedExc);
		} else {
		    throw new RuntimeException(wrappedExc.getMessage());
		}
	    }
	}

	if (isDebugOn())
	    debug("updateRelationServiceMap: exiting", null);
	return;
    }

    // Sets the given roles
    // For each role:
    // - will check the role according to its corresponding role definition
    //   provided in relation's relation type
    // - will send a notification (RelationNotification with type
    //   RELATION_BASIC_UPDATE or RELATION_MBEAN_UPDATE, depending if the
    //   relation is a MBean or not) for each updated role.
    //
    // This method is called in setRoles() above and in Relation Service
    // setRoles() method.
    //
    // -param theRoleList  list of roles to be set
    // -param theRelServCallFlg  true if call from the Relation Service; this
    //  will happen if the current RelationSupport object has been created by
    //  the Relation Service (via createRelation()) method, so direct access is
    //  possible.
    // -param theRelServ  reference to Relation Service object, if object
    //  created by Relation Service.
    //
    // -return a RoleResult object
    //
    // -exception IllegalArgumentException  if null parameter
    // -exception RelationServiceNotRegisteredException  if the Relation
    //  Service is not registered in the MBean Server
    // -exception RelationTypeNotFoundException if:
    //  - relation MBean
    //  and
    //  - unknown relation type
    // -exception RelationNotFoundException if:
    //  - relation MBean
    // and
    // - not added in the RS
    RoleResult setRolesInt(RoleList theRoleList,
			   boolean theRelServCallFlg,
			   RelationService theRelServ)
	throws IllegalArgumentException,
	       RelationServiceNotRegisteredException,
	       RelationTypeNotFoundException,
               RelationNotFoundException {

	if (theRoleList == null ||
	    (theRelServCallFlg && theRelServ == null)) {
	    // Revisit [cebro] Localize message
	    String excMsg = "Invalid parameter.";
	    throw new IllegalArgumentException(excMsg);
	}

	if (isDebugOn()) {
	    String str =
		"theRoleList " + theRoleList
		+ ", theRelServCallFlg " + theRelServCallFlg;
	    debug("setRolesInt: entering", str);
	}

	RoleList roleList = new RoleList();
	RoleUnresolvedList roleUnresList = new RoleUnresolvedList();

	for (Iterator roleIter = theRoleList.iterator();
	     roleIter.hasNext();) {

	    Role currRole = (Role)(roleIter.next());

	    Object currResult = null;
	    // Can throw:
	    // RelationServiceNotRegisteredException,
	    // RelationTypeNotFoundException
	    //
	    // Will not throw, due to parameters, RoleNotFoundException or
	    // InvalidRoleValueException, but catch them to keep compiler
	    // happy
	    try {
		currResult = setRoleInt(currRole,
					theRelServCallFlg,
					theRelServ,
					true);
	    } catch (RoleNotFoundException exc1) {
		// OK : Do not throw a RoleNotFoundException.
	    } catch (InvalidRoleValueException exc2) {
		// OK : Do not throw an InvalidRoleValueException.
	    }

	    if (currResult instanceof Role) {
		// Can throw IllegalArgumentException if role is null
		// (normally should not happen :(
		try {
		    roleList.add((Role)currResult);
		} catch (IllegalArgumentException exc) {
		    throw new RuntimeException(exc.getMessage());
		}

	    } else if (currResult instanceof RoleUnresolved) {
		// Can throw IllegalArgumentException if role is null
		// (normally should not happen :(
		try {
		    roleUnresList.add((RoleUnresolved)currResult);
		} catch (IllegalArgumentException exc) {
		    throw new RuntimeException(exc.getMessage());
		}
	    }
	}

	RoleResult result = new RoleResult(roleList, roleUnresList);

	if (isDebugOn())
	    debug("setRolesInt: exiting", null);
	return result;
    }

    // Initialises all members
    //
    // -param theRelId  relation identifier, to identify the relation in the
    // Relation Service.
    // Expected to be unique in the given Relation Service.
    // -param theRelServiceName  ObjectName of the Relation Service where
    // the relation will be registered.
    // It is required as this is the Relation Service that is aware of the
    // definition of the relation type of given relation, so that will be able
    // to check update operations (set). Direct access via the Relation
    // Service (RelationService.setRole()) do not need this information but
    // as any user relation is a MBean, setRole() is part of its management
    // interface and can be called directly on the user relation MBean. So the
    // user relation MBean must be aware of the Relation Service where it will
    // be added.
    // -param theRelTypeName  Name of relation type.
    // Expected to have been created in given Relation Service.
    // -param theRoleList  list of roles (Role objects) to initialised the
    // relation. Can be null.
    // Expected to conform to relation info in associated relation type.
    //
    // -exception InvalidRoleValueException  if the same name is used for two
    //  roles.
    // -exception IllegalArgumentException  if a required value (Relation
    //  Service Object Name, etc.) is not provided as parameter.
    private void initMembers(String theRelId,
			     ObjectName theRelServiceName,
			     MBeanServer theRelServiceMBeanServer,
			     String theRelTypeName,
			     RoleList theRoleList)
	throws InvalidRoleValueException,
               IllegalArgumentException {

	if (theRelId == null ||
	    theRelServiceName == null ||
	    theRelTypeName == null) {
	    // Revisit [cebro] Localize message
	    String excMsg = "Invalid parameter.";
	    throw new IllegalArgumentException(excMsg);
	}

	if (isDebugOn()) {
	    StringBuffer strB =
		new StringBuffer("theRelId " + theRelId
				 + ", theRelServiceName "
				 + theRelServiceName.toString()
				 + ", theRelTypeName " + theRelTypeName);
	    if (theRoleList != null) {
		strB.append(", theRoleList " + theRoleList.toString());
	    }
	    debug("initMembers: entering", strB.toString());
	}

	myRelId = theRelId;
	myRelServiceName = theRelServiceName;
	myRelServiceMBeanServer = theRelServiceMBeanServer;
	myRelTypeName = theRelTypeName;
	// Can throw InvalidRoleValueException
	initRoleMap(theRoleList);
	myInRelServFlg = new Boolean(false);

	if (isDebugOn())
	    debug("initMembers: exiting", null);
	return;
    }

    // Initialise the internal role map from given RoleList parameter
    //
    // -param theRoleList  role list. Can be null.
    //  As it is a RoleList object, it cannot include null (rejected).
    //
    // -exception InvalidRoleValueException  if the same role name is used for
    //  several roles.
    //
    private void initRoleMap(RoleList theRoleList)
	throws InvalidRoleValueException {

	if (theRoleList == null) {
	    return;
	}

	if (isDebugOn())
	    debug("initRoleMap: entering", theRoleList.toString());

	synchronized(myRoleName2ValueMap) {

	    for (Iterator roleIter = theRoleList.iterator();
		 roleIter.hasNext();) {

		// No need to check if role is null, it is not allowed to store
		// a null role in a RoleList :)
		Role currRole = (Role)(roleIter.next());
		String currRoleName = currRole.getRoleName();

		if (myRoleName2ValueMap.containsKey(currRoleName)) {
		    // Role already provided in current list
		    // Revisit [cebro] Localize message
		    StringBuffer excMsgStrB = new StringBuffer("Role name ");
		    excMsgStrB.append(currRoleName);
		    excMsgStrB.append(" used for two roles.");
		    throw new InvalidRoleValueException(excMsgStrB.toString());
		}

		myRoleName2ValueMap.put(currRoleName,
					(Role)(currRole.clone()));
	    }
	}

	if (isDebugOn())
	    debug("initRoleMap: exiting", null);
	return;
    }

    // Callback used by the Relation Service when a MBean referenced in a role
    // is unregistered.
    // The Relation Service will call this method to let the relation
    // take action to reflect the impact of such unregistration.
    // Current implementation is to set the role with its current value
    // (list of ObjectNames of referenced MBeans) without the unregistered
    // one.
    //
    // -param theObjName  ObjectName of unregistered MBean
    // -param theRoleName  name of role where the MBean is referenced
    // -param theRelServCallFlg  true if call from the Relation Service; this
    //  will happen if the current RelationSupport object has been created by
    //  the Relation Service (via createRelation()) method, so direct access is
    //  possible.
    // -param theRelServ  reference to Relation Service object, if internal
    //  relation
    //
    // -exception IllegalArgumentException if null parameter
    // -exception RoleNotFoundException  if:
    //  - the role does not exist
    //  or
    //  - role not writable.
    // -exception InvalidRoleValueException  if value provided for:
    //   - the number of referenced MBeans in given value is less than
    //     expected minimum degree
    //   or
    //   - the number of referenced MBeans in provided value exceeds expected
    //     maximum degree
    //   or
    //   - one referenced MBean in the value is not an Object of the MBean
    //     class expected for that role
    //   or
    //   - a MBean provided for that role does not exist
    // -exception RelationServiceNotRegisteredException  if the Relation
    //  Service is not registered in the MBean Server
    // -exception RelationTypeNotFoundException if unknown relation type
    // -exception RelationNotFoundException if current relation has not been
    //  added in the RS
    void handleMBeanUnregistrationInt(ObjectName theObjName,
				      String theRoleName,
				      boolean theRelServCallFlg,
				      RelationService theRelServ)
	throws IllegalArgumentException,
               RoleNotFoundException,
               InvalidRoleValueException,
	       RelationServiceNotRegisteredException,
	       RelationTypeNotFoundException,
               RelationNotFoundException {

	if (theObjName == null ||
	    theRoleName == null ||
	    (theRelServCallFlg && theRelServ == null)) {
	    // Revisit [cebro] Localize message
	    String excMsg = "Invalid parameter.";
	    throw new IllegalArgumentException(excMsg);
	}

	if (isDebugOn()) {
	    String str =
		"theObjName " + theObjName
		+ ", theRoleName " + theRoleName
		+ ", theRelServCallFlg " + theRelServCallFlg;
	    debug("handleMBeanUnregistrationInt: entering", str);
	}

	// Retrieves current role value
	Role role = null;
	synchronized(myRoleName2ValueMap) {
	    role = (Role)(myRoleName2ValueMap.get(theRoleName));
	}

	if (role == null) {
	    StringBuffer excMsgStrB = new StringBuffer();
	    // Revisit [cebro] Localize message
	    String excMsg = "No role with name ";
	    excMsgStrB.append(excMsg);
	    excMsgStrB.append(theRoleName);
	    throw new RoleNotFoundException(excMsgStrB.toString());
	}
	ArrayList currRoleValue = (ArrayList)(role.getRoleValue());

	// Note: no need to test if list not null before cloning, null value
	//       not allowed for role value.
	ArrayList newRoleValue = (ArrayList)(currRoleValue.clone());
	newRoleValue.remove(theObjName);
	Role newRole = new Role(theRoleName, newRoleValue);

	// Can throw InvalidRoleValueException,
	// RelationTypeNotFoundException
	// (RoleNotFoundException already detected)
	Object result =
	    setRoleInt(newRole, theRelServCallFlg, theRelServ, false);

	if (isDebugOn())
	    debug("handleMBeanUnregistrationInt: exiting", null);
	return;
    }

    // stuff for Tracing

    private static String localClassName = "RelationSupport";

    // trace level
    private boolean isTraceOn() {
        return Trace.isSelected(Trace.LEVEL_TRACE, Trace.INFO_RELATION);
    }

//    private void trace(String className, String methodName, String info) {
//        Trace.send(Trace.LEVEL_TRACE, Trace.INFO_RELATION, className, methodName, info);
//    }

    private void trace(String methodName, String info) {
        Trace.send(Trace.LEVEL_TRACE, Trace.INFO_RELATION, localClassName, methodName, info);
	Trace.send(Trace.LEVEL_TRACE, Trace.INFO_RELATION, "", "", "\n");
    }

//    private void trace(String className, String methodName, Exception e) {
//        Trace.send(Trace.LEVEL_TRACE, Trace.INFO_RELATION, className, methodName, e);
//    }

//    private void trace(String methodName, Exception e) {
//        Trace.send(Trace.LEVEL_TRACE, Trace.INFO_RELATION, localClassName, methodName, e);
//    }

    // debug level
    private boolean isDebugOn() {
        return Trace.isSelected(Trace.LEVEL_DEBUG, Trace.INFO_RELATION);
    }

//    private void debug(String className, String methodName, String info) {
//        Trace.send(Trace.LEVEL_DEBUG, Trace.INFO_RELATION, className, methodName, info);
//    }

    private void debug(String methodName, String info) {
        Trace.send(Trace.LEVEL_DEBUG, Trace.INFO_RELATION, localClassName, methodName, info);
	Trace.send(Trace.LEVEL_DEBUG, Trace.INFO_RELATION, "", "", "\n");
    }

//    private void debug(String className, String methodName, Exception e) {
//        Trace.send(Trace.LEVEL_DEBUG, Trace.INFO_RELATION, className, methodName, e);
//    }

//    private void debug(String methodName, Exception e) {
//        Trace.send(Trace.LEVEL_DEBUG, Trace.INFO_RELATION, localClassName, methodName, e);
//    }							 
}
