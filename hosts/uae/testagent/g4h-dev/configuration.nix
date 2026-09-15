# SPDX-FileCopyrightText: 2022-2026 TII (SSRC) and the Ghaf contributors
# SPDX-License-Identifier: Apache-2.0
{
  self,
  inputs,
  lib,
  config,
  ...
}:
{
  imports = [
    ../uae.nix
    ./disk-config.nix
    inputs.disko.nixosModules.disko
  ]
  ++ (with self.nixosModules; [
    testagent
    common
    openssh
    user-eyad
    team-devenv
    team-testers
  ]);

  sops.defaultSopsFile = ./secrets.yaml;

  boot = {
    initrd.availableKernelModules = [
      "xhci_pci"
      "nvme"
      "usbhid"
      "usb_storage"
      "sd_mod"
    ];
    kernelModules = [
      "kvm-intel"
      "sg"
    ];
  };

  networking.hostName = "g4h-testagent-dev";
  services.testagent = {
    enable = true;
    relayBoard.enable = true;
    variant = "dev";
    hardware = [ "orin-nx" ];
    credentialsFile = lib.mkForce ./credentials.yaml;
  };

  # this host has been installed with 25.11
  system.stateVersion = lib.mkForce "25.11";

  # the testagent is a laptop
  services.logind.settings.Login.HandleLidSwitch = "ignore";

  # udev rules for test devices serial connections
  services.udev.extraRules = ''
    # Orin NX
    SUBSYSTEM=="tty", ENV{ID_PATH}=="pci-0000:00:14.0-usb-0:1.3.1:1.0", ENV{ID_VENDOR_ID}=="067b", ENV{ID_MODEL_ID}=="2303", SYMLINK+="ttyORINNX1", MODE="0666", GROUP="dialout"
    # SSD-drive
    SUBSYSTEM=="block", KERNEL=="sd[a-z]", ENV{ID_SERIAL_SHORT}=="323535303432343030313539", SYMLINK+="ssdORINNX1", MODE="0666", GROUP="dialout"
  '';

  # Details of the hardware devices connected to this host
  environment.etc."jenkins/test_config.json".text =
    let
      location = config.networking.hostName;
    in
    builtins.toJSON {
      addresses = {
        relay_serial_port = "/dev/serial/by-id/usb-FTDI_FT232R_USB_UART_BG03IPGA-if00-port0";
        OrinNX1 = {
          inherit location;
          device_id = "00-31-60-10-98";
          netvm_hostname = "ghaf-0828379288";
          serial_port = "/dev/ttyORINNX1";
          relay_number = 1;
          device_ip_address = "10.44.0.21";
          socket_ip_address = "NONE";
          plug_type = "NONE";
          switch_bot = "NONE";
          usbhub_serial = "8E25534A";
          ext_drive_by-id = "/dev/ssdORINNX1";
          threads = 8;
        };
      };
    };
}
