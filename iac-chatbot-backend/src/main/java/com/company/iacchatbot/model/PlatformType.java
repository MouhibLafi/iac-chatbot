package com.company.iacchatbot.model;

/**
 * Plateforme cible de deploiement (document officiel societe)
 */
public enum PlatformType {
    VSPHERE,    // VMware vSphere (machines virtuelles)
    OPENSHIFT   // OpenShift Container Platform (conteneurs + VMs KubeVirt)
}
