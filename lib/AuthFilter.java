/*package com.mychat;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI();
        HttpSession session = request.getSession(false);

        String sessionId = (session != null) ? (String) session.getAttribute("id") : null;
        String cookieId = gethAuth(request);

        boolean loggedIn = (sessionId != null && cookieId != null && sessionId.equals(cookieId));

        boolean isStatic = path.contains("/css/") || path.contains("/js/") || path.contains("/images/");
        boolean isLoginPage = path.endsWith("login.html") || path.endsWith("LoginServlet");
        boolean isChatPage = path.endsWith("chat.html") || path.endsWith("ChatServlet");
        boolean isRoot = path.equals("/") || path.equals(request.getContextPath()) || path.equals(request.getContextPath() + "/");

        if (isStatic) {
            chain.doFilter(req, res);
            return;
        }

        if (loggedIn) {
            if (!isChatPage) {
                System.out.println("Logged in and want to go to login page");
                response.sendRedirect("chat.html");
                return;
            }
        } else {
            if (!(isLoginPage || isRoot)) {
                System.out.println("Not logged in and want to go to chat page");
                response.sendRedirect("login.html");
                return;
            }
        }

        chain.doFilter(req, res);
    }

    private String gethAuth(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        String id = null, token = null;

        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("id".equals(c.getName())) id = c.getValue();
                if ("token".equals(c.getName())) token = c.getValue();
            }
        }

        if (id == null || token == null) return null;

        boolean valid = false;
        try {
            if (ChatUtil.userExists(id) && ChatUtil.validateSessionToken(id, token) && ChatUtil.verifyCookie(token)) {
                valid = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return valid ? id : null;
    }
}
*/
package com.mychat;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        
        System.out.println("Enters doFilter");
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI();
        if(path.contains("/ValidateSessionToken"))
        {
          System.out.println("Skips authentication");
          chain.doFilter(req,res);
          return;
        }
        HttpSession session = request.getSession(false);
        
        System.out.println("Session "+ session);
        String sessionId = (session != null && session.getAttribute("id") != null) ? (String) session.getAttribute("id"): null;
        
        String cookieId = gethAuth(request);

        System.out.println("SessionId: "+sessionId+"      "+"cookieId: "+cookieId);
        boolean isRoot = path.equals("/") ||
                         path.equals(request.getContextPath()) ||
                         path.equals(request.getContextPath() + "/");

        boolean loggedIn = (sessionId != null && cookieId != null && sessionId.equals(cookieId));
        
        System.out.println("Context path "+isRoot+" loggedIn "+loggedIn);
        if (path.contains("/css/") || path.contains("/js/") || path.contains("/images/")) {
            chain.doFilter(req, res);
            return;
        }

        if (loggedIn && (path.endsWith("login.html") || path.endsWith("LoginServlet"))) {
            System.out.println("User already logged in → Redirecting to chat.html");
            response.sendRedirect("chat.html");
            return;
        }

        if (!loggedIn) {
            if (path.endsWith("login.html") || path.endsWith("LoginServlet") || isRoot) {
                chain.doFilter(req, res);
            } else {
                System.out.println("Not logged in → Redirecting to login.html");
                response.sendRedirect("login.html");
            }
            return;
        }

        chain.doFilter(req, res);
    }

    private String gethAuth(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        System.out.println("Connection given in Auth session");
        String id = null, token = null;

        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("id".equals(c.getName())) id = c.getValue();
                if ("token".equals(c.getName())) token = c.getValue();
                System.out.println("Cookie: " + c.getName() + " = " + c.getValue());
            }
        }

        System.out.println("token in Auth Session: " + token);

        boolean valid = false;
        try {
            if(!ChatUtil.userExists(id))
              valid = true;
            else if (id != null && token != null)
                valid = ChatUtil.userExists(id)
                        && ChatUtil.validateSessionToken(id, token)
                        && ChatUtil.verifyCookie(token);
          
            if (!ChatUtil.validateSessionToken(id, token))
                System.out.println("Invalid Cookie");
            if (!ChatUtil.verifyCookie(token))
                System.out.println("Expired Cookie");
            if (!ChatUtil.userExists(id))
                System.out.println("User does not exist");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (valid) return id;
        return null;
    }
}


/*package com.mychat; 
import javax.servlet.*; 
import javax.servlet.annotation.WebFilter; 
import java.util.*; 
import java.io.*; 
import javax.servlet.http.*; 

@WebFilter("/*") 
public class AuthFilter implements Filter { 
  @Override 
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws ServletException, IOException { 
      HttpServletRequest request = (HttpServletRequest) req; 
      HttpServletResponse response = (HttpServletResponse) res; 
      String path = request.getRequestURI();  
      
      HttpSession session = request.getSession(false); 
      String loggedIn = null; 
      
      if(session != null && session.getAttribute("id") !=null)
        loggedIn = (String) session.getAttribute("id");
      String cookieValue = gethAuth(request);
      
      if (loggedIn.equals(cookieValue)) { 
        response.sendRedirect("chat.html");
      } 
      else { 
        response.sendRedirect("login.html"); 
      }
      chain.doFilter(req,res);
} 
      
      
    private String gethAuth(HttpServletRequest req)
    {
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
        } catch (Exception e) { 
          e.printStackTrace(); 
        }
        if(valid) return id;
        return null;
    }
  }
  */
