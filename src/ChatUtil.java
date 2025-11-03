package com.mychat;

import java.sql.*;
import java.util.*;
import org.mindrot.jbcrypt.BCrypt;

public class ChatUtil
{
  private static String url = "jdbc:mysql://localhost:3306/chat_app";
  private static String root = "test";
  private static String password = "WeeMee@#5588";
  
  public static Connection getConnection() throws SQLException
  {
    try 
    {
        Class.forName("com.mysql.cj.jdbc.Driver"); 
    } 
    catch (ClassNotFoundException e) {
        e.printStackTrace();
    }
    Connection con = DriverManager.getConnection(url,root,password);
    System.out.print("Successfully connected");
    return con;
  }
  
  public static boolean userExists(String username, String userid) throws SQLException
  {
    String sql = "SELECT COUNT(*) FROM users where username=? AND id=?";
    try(Connection con = getConnection(); PreparedStatement stmt = con.prepareStatement(sql))
    {
      stmt.setString(1,username);
      if(userid!=null)
      stmt.setInt(2,Integer.parseInt(userid));
      else
      stmt.setInt(2,0);
      ResultSet rs = stmt.executeQuery();
      rs.next();              //to point to the first row
      return rs.getInt(1)>0;
    } 
  }
  
  public static boolean userExists(String userid) throws SQLException
  {
    String sql = "SELECT COUNT(*) FROM users where id=?";
    try(Connection con = getConnection(); PreparedStatement stmt = con.prepareStatement(sql))
    {
      if(userid!=null)
      stmt.setInt(1,Integer.parseInt(userid));
      else
      stmt.setInt(1,0);
      ResultSet rs = stmt.executeQuery();
      rs.next();              //to point to the first row
      return rs.getInt(1)>0;
    } 
  }
  
  public static boolean verifyPassword(String userid, String in_password) throws SQLException
  {
    boolean isVerify=false;
    String sql = "SELECT password FROM users where id=?";
    try(Connection con = getConnection(); PreparedStatement stmt = con.prepareStatement(sql))
    {
      stmt.setInt(1,Integer.parseInt(userid));
      
      ResultSet rs = stmt.executeQuery();
      if(rs.next())
      {
        String retrieved_pass = rs.getString("password");
        return BCrypt.checkpw(in_password,retrieved_pass);
      }
      return false;
    }
  }
  
  public static boolean verifyCookie(String cookie) throws SQLException
  {
    String sql = "SELECT cookies_expiry from users WHERE cookies=?";
    try(Connection con = getConnection();PreparedStatement stmt = con.prepareStatement(sql))
    {
      stmt.setString(1,cookie);
      
      ResultSet r=stmt.executeQuery();
      if(!r.next()) return false;
      Timestamp expiry = r.getTimestamp("cookies_expiry", Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")));
      if(expiry==null) return false;
      Timestamp now = new Timestamp(System.currentTimeMillis());
      
      System.out.println("expiry "+expiry+" Current time "+now);
      if(expiry.after(now))
      return true;
      return false;
    }
  }
  
  public static void addUser(String userid, String in_password) throws SQLException
  {
    String sql = "UPDATE users SET password = ? WHERE id = ?";
    try(Connection con = getConnection(); PreparedStatement stmt = con.prepareStatement(sql))
    {
      in_password = BCrypt.hashpw(in_password , BCrypt.gensalt());
      con.setAutoCommit(true);
      stmt.setString(1,in_password);
      stmt.setInt(2,Integer.parseInt(userid));
      stmt.executeUpdate();
    }
  }
  
  public static void saveMessages(String username, String recipient, String content) throws SQLException
  {
    String sql = "INSERT INTO messages(sender,recipient,content) VALUES(?,?,?)";
    try(Connection con = getConnection(); PreparedStatement stmt = con.prepareStatement(sql))
    {
      con.setAutoCommit(true);
      stmt.setString(1,username);
      stmt.setString(2,recipient);
      stmt.setString(3,content);
      stmt.executeUpdate();
    }  
  }
  
  public static List<Map<String,String>> getAllMessages(String username) 
  {
    String sql = "SELECT sender, recipient, content FROM messages WHERE sender = ? OR recipient = ? ORDER BY created_at ASC";
        
        List<Map<String, String>> messageHistory = new ArrayList<>();
        try (Connection con = getConnection();
             PreparedStatement stmt = con.prepareStatement(sql))
        {
            stmt.setString(1, username);	
            stmt.setString(2, username);
            
            try (ResultSet rs = stmt.executeQuery())
            {
                while(rs.next()) {
                    Map<String, String> message = new HashMap<>();
                    message.put("sender", rs.getString("sender"));
                    message.put("recipient", rs.getString("recipient"));
                    message.put("content", rs.getString("content"));
                    messageHistory.add(message);
                }
            } 
        } 
        catch(SQLException e)
        {
          e.printStackTrace();
        }
        
        return messageHistory;
    
  }
  
    private static Map<String, String> sessionTokens = new HashMap<>();  
    public static void storeSessionToken(String id,String token) throws SQLException
    {
      String sql = "UPDATE users SET cookies=?, cookies_expiry=? WHERE id=?";
      try (Connection con = getConnection(); PreparedStatement stmt = con.prepareStatement(sql)) 
      {
        Timestamp expiry = new Timestamp(System.currentTimeMillis() + (30 * 60 * 1000));
        
        stmt.setString(1, token);
        stmt.setTimestamp(2, expiry);
        stmt.setInt(3,Integer.parseInt(id));
        
        stmt.executeUpdate();
    }
    }
    
    public static boolean validateSessionToken(String id,String token) throws SQLException
    {          
        String sql = "SELECT 1 FROM users WHERE id = ? AND cookies = ?";

        if (id == null || token == null || id.isEmpty() || token.isEmpty()) {
            return false;
        }

        try (Connection con = getConnection(); 
             PreparedStatement stmt = con.prepareStatement(sql)) {
            
            stmt.setInt(1, Integer.parseInt(id));
            stmt.setString(2, token);
            
            try (ResultSet rs = stmt.executeQuery()) {
            System.out.println("id in validateSessionToken" + id +" token in validateSessionToken"+token);
            System.out.println("ResultSet: "+rs);
                return rs.next(); 
            }
        }
    }
    
    public static void removeCookie(String token)
    {
      sessionTokens.remove(token);
    }
    
    public static void removeInvalidCookie(String cookie) throws SQLException
    {
      if(cookie==null) return;
      String sql = "UPDATE users SET cookies=? , cookies_expiry=? WHERE cookies=?";
      try(Connection con = getConnection();PreparedStatement stmt = con.prepareStatement(sql))
      {
        stmt.setString(1,"");
        stmt.setString(2,null);
        stmt.setString(3,cookie);
        stmt.executeUpdate();
      }
    }

}
