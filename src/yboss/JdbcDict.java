package yboss;

import java.sql.ResultSet;
import java.sql.SQLException;

import mysql.Jdbc;

public class JdbcDict extends Jdbc {
	public static final String HOSTNAME = "jdbc:mysql://localhost:3306";
	public static final String USERNAME = "epn";
	public static final String PASS = "";
	public static final String DBNAME = "yboss";
	public static final String TABLENAME = "data";
	public static final String COLNAME = "hits";
	
	public JdbcDict () throws Exception {
		super(HOSTNAME, USERNAME, PASS);
		selectDB(DBNAME);
	}

	public long getHits (String query, String table, String col) {
		try {
			ResultSet rs = queryDB ("select " + col + " from " + table + 
					" where query like '" + query + "'");
			if (rs.next()) {
				return Long.parseLong(rs.getString(col));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
	
	public void addHits (String query, long numHits) {
		try {
			executeUpdate("insert into data values ('" + query + "','" + numHits + "')");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
