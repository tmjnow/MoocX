package com.standard.nonlexical;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

public class NonLexicalSimpleStandardMBeanMain {

	/**
	 * @param args
	 * @throws  
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		// Construct the ObjectName for the MBean we will register
		ObjectName empname = new ObjectName("com.NonLexicalSimpleStandardMBeanMain:type=Employee");

		ObjectName addname = new ObjectName("com.NonLexicalSimpleStandardMBeanMain:type=Address");
		StandardAddress add=new StandardAddress("MGRoad","Bangalore");
		StandardEmployee emp = new StandardEmployee("Cheeky","Monkey",1000,5.15f,false,add);

		//to escape the lexical rules instantiate the StandardMBean which defines the mapping
		//between the mbean and the interface
		StandardMBean addbean=new StandardMBean(add,I_Address.class,false);
		StandardMBean empbean=new StandardMBean(emp,EmpInterface.class,false);
		
		
		// Register the both the MBean
		mbs.registerMBean(addbean, addname);
		mbs.registerMBean(empbean, empname);
		
		//use mbeanserver interface to get details
		MBeanAttributeInfo mbai[]=mbs.getMBeanInfo(empname).getAttributes();
		for (int i = 0; i < mbai.length; i++) {
			System.out.print(" :: "+mbai[i].getName());
			System.out.print(" :: "+mbai[i].getType());
			System.out.println();
		}
		System.out.println(mbs.getAttribute(empname, "Address"));
		
		
		// Wait till jconsole is ready and set salary.
		Thread.sleep(30000);
		emp.setSalary(2000);
		
		
		//Wait forever
		System.out.println("Waiting forever...");
		Thread.sleep(Long.MAX_VALUE);
	    

	}
}
