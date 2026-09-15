# SPDX-FileCopyrightText: 2022-2025 TII (SSRC) and the Ghaf contributors
# SPDX-License-Identifier: Apache-2.0
{
  users.users = {
    eyad = {
      description = "Eyad Shaklab";
      isNormalUser = true;
      openssh.authorizedKeys.keys = [
        "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIIk35lv+XqSyMOF+mChNLGnc0/vCVrNicLg5ZGMwXsCe eyad.shaklab@tii.ae"
      ];
      extraGroups = [
        "wheel"
        "dialout"
      ];
    };
  };
}
