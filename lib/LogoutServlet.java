package com.mychat;

import javax.servlet.*;
import java.io.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/LogoutServlet")
public class LogoutServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        HttpSession session = request.getSession(false);
        if (session != null) 
        {
          session.invalidate();
          System.out.println("Invalidating the session");
        }
        System.out.println("Add cookie to max age of 0");
        Cookie idCookie = new Cookie("id", "");
        Cookie tokenCookie = new Cookie("token", "");
        idCookie.setMaxAge(0);
        tokenCookie.setMaxAge(0);
        idCookie.setPath("/");
        tokenCookie.setPath("/");
        System.out.println("Add cookie in the response");
        Cookie jsession = new Cookie("JSESSIONID", "");
        jsession.setMaxAge(0);             
        jsession.setPath(request.getContextPath()); 
        response.addCookie(jsession);
        response.addCookie(idCookie);
        response.addCookie(tokenCookie);

        response.sendRedirect("login.html");
    }
}

