/* 
 * @(#)file      NotificationEmitter.java 
 * @(#)author    Sun Microsystems, Inc. 
 * @(#)version   1.9 
 * @(#)lastedit      03/07/15 
 * 
 * Copyright 2003 Sun Microsystems, Inc. All rights reserved.
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * Copyright 2003 Sun Microsystems, Inc. Tous droits r�serv�s.
 * Ce logiciel est propriet� de Sun Microsystems, Inc.
 * Distribu� par des licences qui en restreignent l'utilisation. 
 */ 

package javax.management;

/**
 * <p>Interface implemented by an MBean that emits Notifications. It
 * allows a listener to be registered with the MBean as a notification
 * listener.</p>
 *
 * <p>This interface should be used by new code in preference to the
 * {@link NotificationBroadcaster} interface.</p>
 *
 * @since-jdkbundle 1.5
 * @since JMX 1.2
 */
public interface NotificationEmitter extends NotificationBroadcaster {
    /**
     * <p>Removes a listener from this MBean.  The MBean must have a
     * listener that exactly matches the given <code>listener</code>,
     * <code>filter</code>, and <code>handback</code> parameters.  If
     * there is more than one such listener, only one is removed.</p>
     *
     * <p>The <code>filter</code> and <code>handback</code> parameters
     * may be null if and only if they are null in a listener to be
     * removed.</p>
     *
     * @param listener A listener that was previously added to this
     * MBean.
     * @param filter The filter that was specified when the listener
     * was added.
     * @param handback The handback that was specified when the listener was
     * added.
     *
     * @exception ListenerNotFoundException The listener is not
     * registered with the MBean, or it is not registered with the
     * given filter and handback.
     */
    public void removeNotificationListener(NotificationListener listener,
					   NotificationFilter filter,
					   Object handback)
	    throws ListenerNotFoundException;
}
