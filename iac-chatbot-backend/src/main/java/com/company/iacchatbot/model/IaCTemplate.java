package com.company.iacchatbot.model;

import jakarta.persistence.*;

/**
 * Entité représentant un template IaC versionné (Terraform / OpenShift / KubeVirt)
 */
@Entity
@Table(name = "iac_templates")
public class IaCTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 20)
    private ResourceType resourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlatformType platform;

    @Column(name = "template_content", nullable = false, columnDefinition = "TEXT")
    private String templateContent;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(nullable = false)
    private Boolean active = true;

    // Constructors
    public IaCTemplate() {
    }

    public IaCTemplate(String name, ResourceType resourceType, PlatformType platform, String templateContent) {
        this.name = name;
        this.resourceType = resourceType;
        this.platform = platform;
        this.templateContent = templateContent;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public String getTemplateContent() {
        return templateContent;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "IaCTemplate{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", resourceType=" + resourceType +
                ", platform=" + platform +
                ", version=" + version +
                ", active=" + active +
                '}';
    }
}
