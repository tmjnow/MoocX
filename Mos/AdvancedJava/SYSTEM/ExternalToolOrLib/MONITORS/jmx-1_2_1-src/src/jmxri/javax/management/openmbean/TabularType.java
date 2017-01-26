/*
 * @(#)file      TabularType.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   3.17
 * @(#)lastedit      03/07/15
 *
 * Copyright 2000-2003 Sun Microsystems, Inc.  All rights reserved.
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 * 
 * Copyright 2000-2003 Sun Microsystems, Inc.  Tous droits r�serv�s.
 * Ce logiciel est propriet� de Sun Microsystems, Inc.
 * Distribu� par des licences qui en restreignent l'utilisation. 
 *
 */


package javax.management.openmbean;


// java import
//
import java.io.Serializable;
import java.util.List;
import java.util.Iterator;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;

// jmx import
//


/**
 * The <code>TabularType</code> class is the <i> open type</i> class 
 * whose instances describe the types of {@link TabularData <code>TabularData</code>} values.
 *
 * @version     3.17  03/07/15
 * @author      Sun Microsystems, Inc.
 *
 * @since-jdkbundle 1.5
 * @since JMX 1.1
 */
public class TabularType 
    extends OpenType
    implements Serializable {
    
    
    /* Serial version */
    static final long serialVersionUID = 6554071860220659261L;


    /**
     * @serial The composite type of rows
     */
    private CompositeType  rowType;
    
    /**
     * @serial The items used to index each row element, kept in the order the user gave
     *         This is an unmodifiable {@link ArrayList}
     */
    private List        indexNames;


    private transient Integer myHashCode = null; // As this instance is immutable, these two values
    private transient String  myToString = null; // need only be calculated once.


    /* *** Constructor *** */

    /**
     * Constructs a <code>TabularType</code> instance, checking for the validity of the given parameters.
     * The validity constraints are described below for each parameter. 
     * <p>
     * The Java class name of tabular data values this tabular type represents 
     * (ie the class name returned by the {@link OpenType#getClassName() getClassName} method) 
     * is set to the string value returned by <code>TabularData.class.getName()</code>.
     * <p>
     * @param  typeName  The name given to the tabular type this instance represents; cannot be a null or empty string.
     * <br>&nbsp;
     * @param  description  The human readable description of the tabular type this instance represents; 
     *			    cannot be a null or empty string.
     * <br>&nbsp;
     * @param  rowType  The type of the row elements of tabular data values described by this tabular type instance;
     *			cannot be null.
     * <br>&nbsp;
     * @param  indexNames  The names of the items the values of which are used to uniquely index each row element in the 
     *			   tabular data values described by this tabular type instance; 
     *			   cannot be null or empty. Each element should be an item name defined in <var>rowType</var>
     *			   (no null or empty string allowed).
     *			   It is important to note that the <b>order</b> of the item names in <var>indexNames</var>
     *                     is used by the methods {@link TabularData#get(java.lang.Object[]) <code>get</code>} and 
     *			   {@link TabularData#remove(java.lang.Object[]) <code>remove</code>} of class  
     *			   <code>TabularData</code> to match their array of values parameter to items.
     * <br>&nbsp;
     * @throws IllegalArgumentException  if <var>rowType</var> is null, 
     *					 or <var>indexNames</var> is a null or empty array,
     *					 or an element in <var>indexNames</var> is a null or empty string,
     *					 or <var>typeName</var> or <var>description</var> is a null or empty string.
     * <br>&nbsp;
     * @throws OpenDataException  if an element's value of <var>indexNames</var> 
     *				  is not an item name defined in <var>rowType</var>.
     */
    public TabularType(String         typeName, 
		       String         description, 
		       CompositeType  rowType,
		       String[]       indexNames) throws OpenDataException {
	
	// Check and initialize state defined by parent.
	//
	super(TabularData.class.getName(), typeName, description);
	
	// Check rowType is not null
	//
	if (rowType == null) {
	    throw new IllegalArgumentException("Argument rowType cannot be null.");
	}	

	// Check indexNames is neither null nor empty and does not contain any null element or empty string
	//
	checkForNullElement(indexNames, "indexNames");
	checkForEmptyString(indexNames, "indexNames");

	// Check all indexNames values are valid item names for rowType
	//
	for (int i=0; i<indexNames.length; i++) {
	    if ( ! rowType.containsKey(indexNames[i]) ) {
		throw new OpenDataException("Argument's element value indexNames["+ i +"]=\""+ indexNames[i] +
					    "\" is not a valid item name for rowType.");
	    }	
	}
	
	// initialize rowType 
	//
	this.rowType    = rowType;

	// initialize indexNames 
	// (copy content so that subsequent modif to the array referenced by the indexNames parameter have no impact)
	//
	ArrayList tmpList = new ArrayList(indexNames.length + 1);
	for (int i=0; i<indexNames.length; i++) {
	    tmpList.add(indexNames[i]);
	}
	this.indexNames = Collections.unmodifiableList(tmpList);
    }
    
    /**
     * Checks that Object[] arg is neither null nor empty (ie length==0) 
     * and that it does not contain any null element.
     */
    private static void checkForNullElement(Object[] arg, String argName) {
	if ( (arg == null) || (arg.length == 0) ) {
	    throw new IllegalArgumentException("Argument "+ argName +"[] cannot be null or empty.");
	}
	for (int i=0; i<arg.length; i++) {
	    if (arg[i] == null) {
		throw new IllegalArgumentException("Argument's element "+ argName +"["+ i +"] cannot be null.");
	    }
	}
    }

    /**
     * Checks that String[] does not contain any empty (or blank characters only) string.
     */
    private static void checkForEmptyString(String[] arg, String argName) {
	for (int i=0; i<arg.length; i++) {
	    if (arg[i].trim().equals("")) {
		throw new IllegalArgumentException("Argument's element "+ argName +"["+ i +"] cannot be an empty string.");
	    }
	}
    }


    /* *** Tabular type specific information methods *** */

    /**
     * Returns the type of the row elements of tabular data values
     * described by this <code>TabularType</code> instance.
     *
     * @return the type of each row.
     */
    public CompositeType getRowType() {

	return rowType;
    }

    /**
     * <p>Returns, in the same order as was given to this instance's
     * constructor, an unmodifiable List of the names of the items the
     * values of which are used to uniquely index each row element of
     * tabular data values described by this <code>TabularType</code>
     * instance.</p>
     *
     * @return a List of String representing the names of the index
     * items.
     * 
     */
    public List getIndexNames() {

	return indexNames;
    }

    /**
     * Tests whether <var>obj</var> is a value which could be described by this <code>TabularType</code> instance.
     * <p>
     * If <var>obj</var> is null or is not an instance of <code>javax.management.openmbean.TabularData</code>,
     * <code>isValue</code> returns <code>false</code>. 
     * If <var>obj</var> is an instance of <code>javax.management.openmbean.TabularData</code>,
     * its tabular type is tested for equality with this tabular type instance, and <code>isValue</code>
     * returns <code>true</code> if and only if {@link #equals(java.lang.Object) <code>equals</code>} 
     * returns <code>true</code>.
     * <br>&nbsp;
     * @param  obj  the value whose open type is to be tested for equality with this <code>TabularType</code> instance.
     *
     * @return <code>true</code> if <var>obj</var> is a value for this tabular type, <code>false</code> otherwise.
     */
    public boolean isValue(Object obj) {

	// if obj is null, return false
	//
	if (obj == null) {
	    return false;
	}

	// if obj is not a TabularData, return false
	//
	TabularData value;
	try {
	    value = (TabularData) obj;
	} catch (ClassCastException e) {
	    return false;
	}

	// test value's TabularType for equality with this TabularType instance
	//
	return this.equals(value.getTabularType());
    }


    /* *** Methods overriden from class Object *** */

    /**
     * Compares the specified <code>obj</code> parameter with this <code>TabularType</code> instance for equality. 
     * <p>
     * Two <code>TabularType</code> instances are equal if and only if all of the following statements are true:
     * <ul>
     * <li>their type names are equal</li>
     * <li>their row types are equal</li>
     * <li>they use the same index names, in the same order</li>
     * </ul>
     * <br>&nbsp;
     * @param  obj  the object to be compared for equality with this <code>TabularType</code> instance;
     *		    if <var>obj</var> is <code>null</code>, <code>equals</code> returns <code>false</code>.
     * 
     * @return  <code>true</code> if the specified object is equal to this <code>TabularType</code> instance.
     */
    public boolean equals(Object obj) {

	// if obj is null, return false
	//
	if (obj == null) {
	    return false;
	}

	// if obj is not a TabularType, return false
	//
	TabularType other;
	try {
	    other = (TabularType) obj;
	} catch (ClassCastException e) {
	    return false;
	}

	// Now, really test for equality between this TabularType instance and the other:
	//
	
	// their names should be equal
	if ( ! this.getTypeName().equals(other.getTypeName()) ) {
	    return false;
	}

	// their row types should be equal
	if ( ! this.rowType.equals(other.rowType) ) {
	    return false;
	}

	// their index names should be equal and in the same order (ensured by List.equals())
	if ( ! this.indexNames.equals(other.indexNames) ) {
	    return false;
	}

	// All tests for equality were successfull
	//
	return true;
    }

    /**
     * Returns the hash code value for this <code>TabularType</code> instance. 
     * <p>
     * The hash code of a <code>TabularType</code> instance is the sum of the hash codes
     * of all elements of information used in <code>equals</code> comparisons 
     * (ie: name, row type, index names). 
     * This ensures that <code> t1.equals(t2) </code> implies that <code> t1.hashCode()==t2.hashCode() </code> 
     * for any two <code>TabularType</code> instances <code>t1</code> and <code>t2</code>, 
     * as required by the general contract of the method
     * {@link <a href="http://java.sun.com/j2se/1.3/docs/api/java/lang/Object.html#hashCode()"> 
     * <code>Object.hashCode</code> </a>}.
     * <p>
     * As <code>TabularType</code> instances are immutable, the hash code for this instance is calculated once,
     * on the first call to <code>hashCode</code>, and then the same value is returned for subsequent calls.
     *
     * @return  the hash code value for this <code>TabularType</code> instance
     */
    public int hashCode() {

	// Calculate the hash code value if it has not yet been done (ie 1st call to hashCode())
	//
	if (myHashCode == null) {
	    int value = 0;
	    value += this.getTypeName().hashCode();
	    value += this.rowType.hashCode();
	    for (Iterator k = indexNames.iterator(); k.hasNext();  ) {
		value += k.next().hashCode();
	    }
	    myHashCode = new Integer(value);
	}
	
	// return always the same hash code for this instance (immutable)
	//
	return myHashCode.intValue();
    }

    /**
     * Returns a string representation of this <code>TabularType</code> instance. 
     * <p>
     * The string representation consists of the name of this class (ie <code>javax.management.openmbean.TabularType</code>), 
     * the type name for this instance, the row type string representation of this instance,
     * and the index names of this instance.
     * <p>
     * As <code>TabularType</code> instances are immutable, the string representation for this instance is calculated once,
     * on the first call to <code>toString</code>, and then the same value is returned for subsequent calls.
     * 
     * @return  a string representation of this <code>TabularType</code> instance
     */
    public String toString() {

	// Calculate the string representation if it has not yet been done (ie 1st call to toString())
	//
	if (myToString == null) {
	    StringBuffer result = new StringBuffer()
		.append(this.getClass().getName())
		.append("(name=")
		.append(getTypeName())
		.append(",rowType=")
		.append(rowType.toString())
		.append(",indexNames=(");
	    int i=0;
	    Iterator k = indexNames.iterator();
	    while( k.hasNext() ) {
		if (i > 0) result.append(",");
		result.append(k.next().toString());
		i++;
	    }
	    result.append("))");
	    myToString = result.toString();
	}

	// return always the same string representation for this instance (immutable)
	//
	return myToString;
    }

}
