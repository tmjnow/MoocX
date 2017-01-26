package com.remote.beans;

public interface StandardEmployeeMBean {
	public StandardAddress getAddress();
	public void setAddress(StandardAddress address);
	public String getFirstName() ;
	public void setFirstName(String firstName);
	public String getLastName() ;
	public void setLastName(String lastName);
	public boolean isManager() ;
	//public void setManager(boolean manager);
	public long getSalary() ;
	public void setSalary(long salary);
	public float getTax() ;
	public void setTax(float tax);
	public void reset();

}
