/*
 * @(#)file      Attribute.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   4.20
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

package javax.management;


// java import
import java.io.Serializable;


/**
 * Represents an MBean attribute by associating its name with its value.
 * The MBean server and other objects use this class to get and set attributes values.
 *
 * @since-jdkbundle 1.5
 */
public class Attribute implements Serializable   { 

    /* Serial version */
    private static final long serialVersionUID = 2484220110589082382L;
    
    /**
     * @serial Attribute name.
     */
    private String name;
    
    /**
     * @serial Attribute value
     */
    private Object value= null;


    /**
     * Constructs an Attribute object which associates the given attribute name with the given value.
     *
     * @param name A String containing the name of the attribute to be created. Cannot be null.
     * @param value The Object which is assigned to the attribute. This object must be of the same type as the attribute.
     *
     */   
    public Attribute(String name, Object value) { 

	if (name == null) {
	    throw new RuntimeOperationsException(new IllegalArgumentException("Attribute name cannot be null "));
	}

	this.name = name;
	this.value = value;
    } 


    /**
     * Returns a String containing the  name of the attribute.
     *
     * @return the name of the attribute.
     */
    public String getName()  { 
	return name;
    } 
    
    /**
     * Returns an Object that is the value of this attribute.
     *
     * @return the value of the attribute.
     */
    public Object getValue()  { 
	return value;
    } 
    
    /**
     * Compares the current Attribute Object with another Attribute Object.
     *
     * @param object  The Attribute that the current Attribute is to be compared with.
     *
     * @return  True if the two Attribute objects are equal, otherwise false.
     */
    
    
    public boolean equals(Object object)  { 
	if (!(object instanceof Attribute)) {
	    return false;
	}    
	Attribute val = (Attribute) object;

	if (value == null) {
	    if (val.getValue() == null) {
		return name.equals(val.getName());
	    } else {
		return false;
	    }
	}

	return ((name.equals(val.getName())) && 
		(value.equals(val.getValue())));
    } 
   
 }
