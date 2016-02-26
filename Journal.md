# Journal
This document provides an open journal of the activities carried out to deploy and configure CIAO at East Kent Hospitals University NHS Foundation Trust as a
proof of concept. It also details any problems, issues and resolutions.

## Environment
East Kent Hospitals University NHS Foundation Trust has provisioned a Linux VM at IP address EASTKENT (not disclosed in this document for security reasons) on which CIAO
will be deployed. They have configured network access over the N3 network that allows the HSCIC CIAO team to SSH in from a single SSH bastion server at IP address
HSCISBASTION (not disclosed in this document for security reasons) on the HSCIC network.
They have provided a SSH username and password to login to EASTKENT. This account has sudoer rights.

## 26/02/2016 - Mike Kelly
SSH into HSCICBASTION from Windows 7 laptop on HSCIC network using PuTTY.

*Success.

SSH from HSCICBASTION to EASTKENT.
`$ ssh USER@EASTKENT

*Success.



 