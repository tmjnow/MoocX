package com.transaction.isolationreadserializable;
import java.sql.Connection;
import java.sql.DriverManager;
import org.mk.training.util.DataSourceConfig;



public class SingleIncreaseMain {
	
	public static void main(String[] args) throws Exception {
		Class.forName(DataSourceConfig.DRIVER);
		Connection con=DriverManager.getConnection(DataSourceConfig.URL,
				DataSourceConfig.USERNAME,
				DataSourceConfig.PASSWORD);
		con.setTransactionIsolation(ReadSerializableConstants.ISOLATION_LEVEL);
		con.setAutoCommit(false);
		Thread.currentThread().setName("SINGLEWHAMMY");
		new CostIncreaseWork(con,10).run();
		
	}
	
}
