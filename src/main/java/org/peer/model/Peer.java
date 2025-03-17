package org.peer.model;

public class Peer {

    private String username;
    private int port;

    public Peer(String username, int port) {
        this.username = username;
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public int getPort() {
        return port;
    }
}
