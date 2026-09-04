package com.company.iacchatbot.dto;

import com.company.iacchatbot.model.PlatformType;
import com.company.iacchatbot.model.ResourceType;

/**
 * DTO pour les parametres extraits par Ollama (Llama 3)
 * Conforme au document officiel : VM ou CONTAINER sur VSPHERE ou OPENSHIFT
 */
public class ExtractedParameters {

    private ResourceType resourceType;   // VM ou CONTAINER
    private PlatformType platform;       // VSPHERE ou OPENSHIFT
    private String osImage;              // ubuntu-22.04, centos-9, windows-server-2022
    private Integer cpu;                 // Nombre de CPUs (1-32)
    private Integer ramGb;               // RAM en GB (1-128)
    private Integer storageGb;           // Stockage en GB (10-1000)
    private Integer replicas;            // Pour les containers (1-10)
    private String containerImage;       // nginx, mysql, etc.
    private String network;              // Nom du reseau (defaut: "default")

    // Constructors
    public ExtractedParameters() {}

    // Getters and Setters
    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public PlatformType getPlatform() {
        return platform;
    }

    public void setPlatform(PlatformType platform) {
        this.platform = platform;
    }

    public String getOsImage() {
        return osImage;
    }

    public void setOsImage(String osImage) {
        this.osImage = osImage;
    }

    public Integer getCpu() {
        return cpu;
    }

    public void setCpu(Integer cpu) {
        this.cpu = cpu;
    }

    public Integer getRamGb() {
        return ramGb;
    }

    public void setRamGb(Integer ramGb) {
        this.ramGb = ramGb;
    }

    public Integer getStorageGb() {
        return storageGb;
    }

    public void setStorageGb(Integer storageGb) {
        this.storageGb = storageGb;
    }

    public Integer getReplicas() {
        return replicas;
    }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }

    public String getContainerImage() {
        return containerImage;
    }

    public void setContainerImage(String containerImage) {
        this.containerImage = containerImage;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    @Override
    public String toString() {
        return "ExtractedParameters{" +
                "resourceType=" + resourceType +
                ", platform=" + platform +
                ", osImage='" + osImage + '\'' +
                ", cpu=" + cpu +
                ", ramGb=" + ramGb +
                ", storageGb=" + storageGb +
                ", replicas=" + replicas +
                ", containerImage='" + containerImage + '\'' +
                ", network='" + network + '\'' +
                '}';
    }
}
