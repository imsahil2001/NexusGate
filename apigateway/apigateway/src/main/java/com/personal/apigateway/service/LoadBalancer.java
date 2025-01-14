package com.personal.apigateway.service;

import com.personal.apigateway.config.ServerProperties;
import com.personal.apigateway.model.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Service
public class LoadBalancer {
    private final ServerProperties properties;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final List<String> serverList;

    @Autowired
    public LoadBalancer(ServerProperties properties){
        this.properties = properties;
        serverList = this.properties.getServers().stream().map(Server::getUrl).collect(Collectors.toList());
    }

    public String getNextServerUrl(){
        int index = counter.getAndIncrement() % serverList.size();
        return serverList.get(index);
    }
}
