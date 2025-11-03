package com.mychat;

import java.io.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/ValidateSessionToken")
public class ValidateSessionToken extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
    res.setContentType("application/json");
    Cookie[] cookies = req.getCookies();
    System.out.println("Connection given in Validate session");
    String id = null, token = null;
    if (cookies != null) {
      for (Cookie c : cookies) {
        if ("id".equals(c.getName())) id = c.getValue();
        if ("token".equals(c.getName())) token = c.getValue();
        System.out.println("Cookie: "+c.getName()+" = "+c.getValue());
      }
    }
    System.out.println("token in Validate Session" + token);

    boolean valid = false;
    try {
      if (id != null && token != null)
        valid = ChatUtil.userExists(id) && ChatUtil.validateSessionToken(id, token)
              && ChatUtil.verifyCookie(token);
      if(!ChatUtil.validateSessionToken(id,token))
        System.out.println("Invalid Cookie");
      if(!ChatUtil.verifyCookie(token))
        System.out.println("Expired Cookie");
      if(!ChatUtil.userExists(id))
        System.out.println("User does not exist");
    } catch (Exception e) 
    { 
        e.printStackTrace();
    }

    res.getWriter().print("{\"valid\":" + valid + "}");
    res.getWriter().flush();
  }
}

