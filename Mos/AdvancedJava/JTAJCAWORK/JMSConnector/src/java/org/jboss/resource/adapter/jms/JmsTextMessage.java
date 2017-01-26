/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.resource.adapter.jms;

import javax.jms.JMSException;
import javax.jms.TextMessage;

/**
 * A wrapper for a message
 *
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 38315 $
 */
public class JmsTextMessage extends JmsMessage implements TextMessage
{
   /**
    * Create a new wrapper
    * 
    * @param message the message
    * @param session the session
    */
   public JmsTextMessage(TextMessage message, JmsSession session)
   {
      super(message, session);
   }

   public String getText() throws JMSException
   {
      return ((TextMessage) message).getText();
   }
   
   public void setText(String string) throws JMSException
   {
      ((TextMessage) message).setText(string);
   }
}
