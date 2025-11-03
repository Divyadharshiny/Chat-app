package com.mychat;

import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.ServerEndpointConfig;
import javax.servlet.http.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Configurator extends ServerEndpointConfig.Configurator {
    
    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        Map<String, List<String>> headers = request.getHeaders();
        List<String> cookieHeader = headers.get("Cookie");
        HttpSession session = (HttpSession) request.getHttpSession();
        if (cookieHeader != null && !cookieHeader.isEmpty()) {
            config.getUserProperties().put("cookie", cookieHeader.get(0));
            if(session!=null)
            config.getUserProperties().put("HttpSession", session);
            System.out.println("Cookie in Configurator: "+cookieHeader.get(0));
        }
        else{
          System.out.print("No cookie found");
            config.getUserProperties().put("cookie",null);
        }
        //super.modifyHandshake(config,request,response);
    }
}

