package mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Dictionary;
import java.util.Enumeration;

public class Jdbc extends Dictionary<String,String> 
{
	Connection con;
	
	/**
	 * Connects to the specified database
	 * @param url: The database url
	 * @param user: The username
	 * @param pass: The password
	 * @throws Exception
	 */
	public Jdbc (String url, String user, String pass) 
		throws Exception
	{
		Class.forName("com.mysql.jdbc.Driver");
		con = DriverManager.getConnection(url,user,pass);
	}
	
	/**
	 * Specifies which Database to use
	 * @param dbName
	 * @throws SQLException
	 */
	public void selectDB (String dbName) throws SQLException
	{
		Statement stmt = con.createStatement();
		stmt.executeUpdate("USE " + dbName);
	}
	
	/**
	 * Executes a query and returns the results
	 * @param query
	 * @return
	 * @throws SQLException
	 */
	public ResultSet queryDB (String query) throws SQLException {
		ResultSet rs = null;
		Statement stmt = con.createStatement();
		rs = stmt.executeQuery(query);
		return rs;
	}
	
	/**
	 * Executes a command not expecting results
	 * @param query
	 * @throws SQLException
	 */
	public void executeUpdate (String query) throws SQLException {
		Statement stmt = con.createStatement();
		stmt.executeUpdate(query);
	}
	
	/**
	 * Disconnects from the database
	 * @throws SQLException
	 */
	public void disconnect () throws SQLException
	{
		con.close();
	}

	@Override
	public Enumeration<String> elements() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String get(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Enumeration<String> keys() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String put(String key, String value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String remove(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public static void main (String[] args) {
		Jdbc j = null;
		try {
			j = new Jdbc("jdbc:mysql://localhost:3306", "epn", "");
			j.selectDB("yboss");
			//System.out.println(j.getHits("boss","data","hits"));
			j.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
