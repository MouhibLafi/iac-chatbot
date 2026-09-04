# =====================================================================
# Template Terraform - VM VMware vSphere
# Genere par IaC Chatbot (Thymeleaf)
# =====================================================================
terraform {
  required_providers {
    vsphere = {
      source  = "hashicorp/vsphere"
      version = "~> 2.0"
    }
  }
}

provider "vsphere" {
  user                 = var.vsphere_user
  password             = var.vsphere_password
  vsphere_server       = var.vsphere_server
  allow_unverified_ssl = true
}

# --- Variables (credentials via variables d'environnement) ---
variable "vsphere_user" {
  description = "Utilisateur vCenter"
  type        = string
  sensitive   = true
}

variable "vsphere_password" {
  description = "Mot de passe vCenter"
  type        = string
  sensitive   = true
}

variable "vsphere_server" {
  description = "Adresse du serveur vCenter"
  type        = string
}

variable "datacenter_name" {
  description = "Nom du datacenter vSphere"
  type        = string
  default     = "DC1"
}

variable "datastore_name" {
  description = "Nom du datastore"
  type        = string
  default     = "datastore1"
}

variable "resource_pool_id" {
  description = "ID du resource pool"
  type        = string
  default     = ""
}

# --- Data sources ---
data "vsphere_datacenter" "dc" {
  name = var.datacenter_name
}

data "vsphere_datastore" "ds" {
  name          = var.datastore_name
  datacenter_id = data.vsphere_datacenter.dc.id
}

data "vsphere_network" "net" {
  name          = "[[${network}]]"
  datacenter_id = data.vsphere_datacenter.dc.id
}

data "vsphere_virtual_machine" "template" {
  name          = "[[${osImage}]]"
  datacenter_id = data.vsphere_datacenter.dc.id
}

# --- Machine Virtuelle ---
resource "vsphere_virtual_machine" "vm" {
  name             = "[[${vmName}]]"
  resource_pool_id = var.resource_pool_id
  datastore_id     = data.vsphere_datastore.ds.id

  num_cpus = [[${cpu}]]
  memory   = [[${ramGb}]] * 1024
  guest_id = data.vsphere_virtual_machine.template.guest_id

  network_interface {
    network_id = data.vsphere_network.net.id
  }

  disk {
    label            = "disk0"
    size             = [[${storageGb}]]
    thin_provisioned = true
  }

  clone {
    template_uuid = data.vsphere_virtual_machine.template.id

    customize {
      linux_options {
        host_name = "[[${vmName}]]"
        domain    = "company.local"
      }
      network_interface {}
    }
  }
}

# --- Outputs ---
output "vm_name" {
  value = vsphere_virtual_machine.vm.name
}

output "vm_ip" {
  value = vsphere_virtual_machine.vm.default_ip_address
}
