package com.irveni.doorbell.Models;

public class IPCameras {

    long id;
    String title;
    String ip;
    String status;

    public IPCameras(long id, String title, String ip, String status) {
        this.id = id;
        this.title = title;
        this.ip = ip;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "'id':"+id+",'title':"+title+",'ipaddress':"+ip;
    }
}
