package io.coti.common.model;

import java.time.Instant;

public class NodeInformation {

    public Instant uptime;
    public String name;
    public String ipAddress;

    public NodeInformation(String name, String ipAddress) {
        this.name = name;
        this.ipAddress = ipAddress;
        uptime = Instant.now();
    }

    @Override
    public String toString(){
        return name + " " + ipAddress + " " + uptime;
    }
}