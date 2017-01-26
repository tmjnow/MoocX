/*
 * @(#)file      MatchQueryExp.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.23
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



/**
 * This class is used by the query-building mechanism to represent binary
 * relations.
 * @serial include
 *
 * @since-jdkbundle 1.5
 */
class MatchQueryExp extends QueryEval implements QueryExp { 
    
    /* Serial version */
    private static final long serialVersionUID = -7156603696948215014L;

    /**
     * @serial The attribute value to be matched
     */
    private AttributeValueExp exp;

    /**
     * @serial The pattern to be matched
     */
    private String pattern;


    /**
     * Basic Constructor.
     */
    public MatchQueryExp() { 
    } 

    /**
     * Creates a new MatchQueryExp where the specified AttributeValueExp matches
     * the specified pattern StringValueExp.
     */
    public MatchQueryExp(AttributeValueExp a, StringValueExp s) { 
	exp     = a;
	pattern = s.getValue();
    } 
    

    /**
     * Returns the attribute of the query.
     */    
    public AttributeValueExp getAttribute()  { 
	return exp;
    } 

    /**
     * Returns the pattern of the query.
     */
    public String getPattern()  { 
	return pattern;
    } 

    /**
     * Applies the MatchQueryExp on a MBean.
     *
     * @param name The name of the MBean on which the MatchQueryExp will be applied.
     *
     * @return  True if the query was successfully applied to the MBean, false otherwise.     
     *
     * @exception BadStringOperationException
     * @exception BadBinaryOpValueExpException
     * @exception BadAttributeValueExpException 
     * @exception InvalidApplicationException
     */
    public boolean apply(ObjectName name) throws BadStringOperationException, BadBinaryOpValueExpException,
	BadAttributeValueExpException, InvalidApplicationException  { 

	ValueExp val = exp.apply(name);	
	if (!(val instanceof StringValueExp)) {
	    return false;
	}	
	return wildmatch(((StringValueExp)val).getValue(), pattern);
    } 

    /**
     * Returns the string representing the object
     */
    public String toString()  { 
	return exp + " like " + new StringValueExp(likeTranslate(pattern));
    } 

    private static String likeTranslate(String s) {
	return s.replace('?', '_').replace('*', '%');
    }
    
    /*
     * Tests whether string s is matched by pattern p.
     * Supports "?", "*", "[", each of which may be escaped with "\";
     * Character classes may use "!" for negation and "-" for range.
     * Not yet supported: internationalization; "\" inside brackets.<P>
     * Wildcard matching routine by Karl Heuer.  Public Domain.<P>
     */
    private static boolean wildmatch(String s, String p) {
	char c;
        int si = 0, pi = 0;
        int slen = s.length();
        int plen = p.length();

        while (pi < plen) {            // While still string
            c = p.charAt(pi++);
            if (c == '?') {
                if (++si > slen)
                    return false;
            } else if (c == '[') {        // Start of choice
                boolean wantit = true;
                boolean seenit = false;
                if (p.charAt(pi) == '!') {
                    wantit = false;
                    ++pi;
                }
                while (++pi < plen && (c = p.charAt(pi)) != ']') {		
                    if (p.charAt(pi) == '-' && pi+1 < plen) {
                        if (s.charAt(si) >= c && s.charAt(si) <= p.charAt(pi+1)) {
                            seenit = true;		
			}
			pi += 1;                    
                    } else {
                        if (c == s.charAt(si)) {
                            seenit = true;
			}
                    }
                }
                if ((pi >= plen) || (wantit != seenit)) {
                    return false;
		}
		++pi;
                ++si;
            } else if (c == '*') {   // Wildcard	
                if (pi >= plen)
                    return true;
                do {
                    if (wildmatch(s.substring(si), p.substring(pi)))
                        return true;
                } while (++si < slen);
                return false;
            } else if (c == '\\') {
                if (pi >= plen || p.charAt(pi++) != s.charAt(si++))
                    return false;
            } else {
                if (si >= slen || c != s.charAt(si++)) {
                    return false;
		}
            }
        }
        return (si == slen);    
    }

 }
