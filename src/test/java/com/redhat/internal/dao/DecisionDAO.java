package com.redhat.internal.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import io.cucumber.guice.ScenarioScoped;

@ScenarioScoped
public class DecisionDAO {
	
	public String getDecisionJson(final Long processInstanceId) {
		// creates three different Connection objects
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String json = "";
        try {
        	
        	String sql = "SELECT fd.json FROM Finaldecision fd WHERE fd.processInstanceId = ? ";
        	con = getDBConnection();
        	pstmt = con.prepareStatement(sql);
        	pstmt.setLong(1, processInstanceId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
            	json = rs.getString(1);
            }
        } catch (SQLException ex) {
            System.out.println("An error occurred. Maybe user/password is invalid");
            ex.printStackTrace();
        }
        finally {
        	try {
	        	if(rs != null) {
					rs.close();
	        	}
	        	if(pstmt != null) {
	        		pstmt.close();
	        	}
	        	if(con != null) {
	        		con.close();
	        	}
        	} 
        	catch (SQLException e) {
				e.printStackTrace();
			}
        }
        return json;
	}
	
	public void removeDecision() {
		// creates three different Connection objects
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
        	
        	String sql = "DELETE FROM Finaldecision";
        	con = getDBConnection();
        	pstmt = con.prepareStatement(sql);
            int rows = pstmt.executeUpdate();

        } catch (SQLException ex) {
            System.out.println("An error occurred. Maybe user/password is invalid");
            ex.printStackTrace();
        }
        finally {
        	try {
	        	if(rs != null) {
					rs.close();
	        	}
	        	if(pstmt != null) {
	        		pstmt.close();
	        	}
	        	if(con != null) {
	        		con.close();
	        	}
        	} 
        	catch (SQLException e) {
				e.printStackTrace();
			}
        }
	}
	
	public Connection getDBConnection() throws SQLException {
		String url = "jdbc:mysql://localhost:3306/dmauto?useSSL=false";
        Properties info = new Properties();
        info.put("user", "root");
        info.put("password", "Redhat@123");
        Connection conn = DriverManager.getConnection(url, info);
        return conn; 
	}
}
