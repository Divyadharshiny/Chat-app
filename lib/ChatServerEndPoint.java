package com.mychat;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.sql.*;
import javax.servlet.http.*;

@ServerEndpoint(value = "/chat",configurator=Configurator.class)
public class ChatServerEndPoint 
{

    private static final Map<String, Session> activeUsers = new ConcurrentHashMap<>();
    private String token= null ,cookieHeader=null, id=null;
    
    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
    try
    {
      cookieHeader = (String) config.getUserProperties().get("cookie");
      token = isValidateCookie(cookieHeader);
      System.out.println("token in onOpen " + token);
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    System.out.println("New connection opened, session ID: " + session.getId());
  }

    @OnMessage
    public void onMessage(String message, Session session) 
    {
        System.out.println("Received message: " + message);
        synchronized(session)
        {
            try
            {
              if(session.isOpen())
              {
                if (message.contains("\"type\":\"login\"")) {
                    handleLogin(message, session);
                } else if (message.contains("\"type\":\"chat\"") || message.contains("\"type\":\"private\"")) {
                    handleChat(message, session);
                } else {
                    System.out.println("Unknown message type received.");
                }
              }
              else
                System.out.println("Tried to send to closed session");
            }
            catch(Exception e)
            {
              e.printStackTrace();
            }
        }
    }

    private void handleLogin(String message, Session session) 
    {
        HttpSession httpsession = (HttpSession) session.getUserProperties().get("HttpSession");
        String userKey = id;
        String username = (String) httpsession.getAttribute("username");
        
        activeUsers.put(userKey, session);
        if(activeUsers.containsKey(userKey))
        {
            List<Map<String, String>> history = ChatUtil.getAllMessages(username);
            System.out.println("getAllMessages");
            
            for(Map<String, String> data : history) 
            {
                String sender = data.get("sender");
                String recipient = data.get("recipient");
                String content = data.get("content");

                String msgType = (recipient == null || recipient.isEmpty()) ? "message" : "private";
                String displaySender = sender.equals(username) ? "You" : sender;
                
                System.out.println("Displaying All Messages");
                String historyMsg;
                if (msgType.equals("private")) 
                {
                    String target = sender.equals(userKey) ? "You -> " + recipient : sender;
                    historyMsg = "{\"type\":\"private\",\"sender\":\"" + target + "\",\"content\":\"" + content + "\"}";
                } 
                else 
                {
                    historyMsg = "{\"type\":\"message\",\"sender\":\"" + sender + "\",\"content\":\"" + content + "\"}";
                }
                try
                {
                session.getBasicRemote().sendText(historyMsg);
                }
                catch(Exception e)
                {
                  e.printStackTrace();
                }
             }
          }
            
        String entryMsg = "{\"type\":\"status\",\"content\":\"" + username + " ("+ userKey + ") has joined the chat.\"}";
        broadcast(entryMsg);
        System.out.println("User joined: " + userKey);
          
    }

    private void handleChat(String message, Session session) {
        HttpSession httpsession = (HttpSession) session.getUserProperties().get("HttpSession");
        String userKey = id;
        String username = (String) httpsession.getAttribute("username");
        String recipient = extractValue(message,"recipient");
        String content = extractValue(message,"content");    
        
        try
        {
          System.out.println("saving Messages");
          ChatUtil.saveMessages(username,recipient.isEmpty() ? null : recipient,content);
        }
        catch(Exception e)
        {
          e.printStackTrace();
        }

        String chatMsg = "{\"type\":\"message\",\"sender\":\"" + username + "\",\"content\":\"" + content + "\"}";

        if (recipient != null && !recipient.isEmpty() && !recipient.equals("All")) {
            sendPrivate(recipient, chatMsg, username);
        } else {
            broadcast(chatMsg);
        }
    }

    private void sendPrivate(String recipient, String message, String senderKey) {
        Session recipientSession = activeUsers.get(recipient);
        if (recipientSession != null && recipientSession.isOpen()) {
            try {
                recipientSession.getBasicRemote().sendText(message);

                Session senderSession = activeUsers.get(senderKey);
                if (senderSession != null && senderSession.isOpen()) {
                    senderSession.getBasicRemote().sendText(
                        "{\"type\":\"status\",\"content\":\"Private message sent to " + recipient + "\"}"
                    );
                }
            } catch (IOException e) {
                System.err.println("Error sending private message: " + e.getMessage());
            }
        } 
        else
        {
        try {
              Session senderSession = activeUsers.get(senderKey);
                if (senderSession != null && senderSession.isOpen()) {
                    senderSession.getBasicRemote().sendText(
                        "{\"type\":\"status\",\"content\":\"User " + recipient + " not found or offline.\"}"
                    );
                }
            } catch (IOException e) {
                System.err.println("Error notifying sender: " + e.getMessage());
            }
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {

            activeUsers.remove(id);

            String exitMsg = "{\"type\":\"status\",\"content\":\"" + "user has left the chat.\"}";
            broadcast(exitMsg);
            
            System.out.println("User disconnected: " );
            //session.close();
            System.out.println("Connection closed: " + reason.getReasonPhrase());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("Error in session " + session.getId() + ": " + throwable.getMessage());
    }

    private static void broadcast(String message) {
        System.out.println("The activeUsers "+activeUsers);
        for (Session session : activeUsers.values()) {
          System.out.println("Session "+session);
            
            if (session.isOpen()) {
                try {
                    System.out.println("Session open and in the broadcast");
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    System.err.println("Broadcast error: " + e.getMessage());
                }
            }
        }
    }

    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }
    
    private String isValidateCookie(String cookieHeader)
    {
        String []cookies = cookieHeader.split(";");
        for(String c: cookies)
        {
          String []pair = c.trim().split("=");
          if(pair.length==2)
          {
            if(pair[0].equals("id")) id = pair[1];
            if(pair[0].equals("token")) token = pair[1];
          }
        }
        if(id == null || token==null) return null;
           
       try
       {
        System.out.println("Taking cookie and validating "+token);
        if(ChatUtil.validateSessionToken(id,token))
        {
          System.out.println("Cookie is found and correct");
          if(ChatUtil.verifyCookie(token))
          return token;
          System.out.println("Cookie is expired");
        }
        System.out.println("Cookie is not validated");
       }
       catch(Exception e)
       {
        e.printStackTrace();
       }
       return null;           
    }
}

