package com.mychat;

import java.io.*;
import java.sql.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import com.google.gson.Gson;

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {

    private static class LoginResponse {
        boolean success;
        String message;

        LoginResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String id = request.getParameter("id");
        String password = request.getParameter("password");
        String token = UUID.randomUUID().toString();

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        LoginResponse loginResponse;
        
        try
        {
            System.out.println("Checks for the database");
            if(!ChatUtil.userExists(id))     //new user
            {
              if(password == null || password.isEmpty())
              {
                System.out.println("New User but not given password");
                return;
              }
              else
              {
                System.out.println("New User");
                ChatUtil.addUser(id,password);
              }
            }
            else
            {
              if(!ChatUtil.verifyPassword(id, password))
              {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                loginResponse = new LoginResponse(false, "Invalid password");
                new Gson().toJson(loginResponse, response.getWriter());
                System.out.println("Invalid password...Check password");
                return;
              }
              else
              {
                System.out.println("Storing the data");
                ChatUtil.storeSessionToken(id, token);
              }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
        try 
        {
            boolean exists = ChatUtil.userExists(username, id);
            boolean validPassword = ChatUtil.verifyPassword(id, password);

            if ((exists && validPassword) || !exists) {

                Cookie idCookie = new Cookie("id", id);
                Cookie tokenCookie = new Cookie("token", token);

                idCookie.setMaxAge(60 * 30);     // 30 min
                tokenCookie.setMaxAge(60 * 30);
                idCookie.setPath("/");
                tokenCookie.setPath("/");
                tokenCookie.setHttpOnly(true);

                response.addCookie(idCookie);
                response.addCookie(tokenCookie);

                HttpSession session = request.getSession();
                session.setAttribute("username",username);
                session.setAttribute("id", id);
                session.setAttribute("password",password);

                loginResponse = new LoginResponse(true, "Login successful");

            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                loginResponse = new LoginResponse(false, "Invalid credentials");
            }

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            loginResponse = new LoginResponse(false, "Internal server error");
            e.printStackTrace();
        }
        Gson gson = new Gson();
        String json = gson.toJson(loginResponse);
        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }
}

