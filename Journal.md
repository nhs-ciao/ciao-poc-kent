# Journal
This document provides an open journal of the activities carried out to deploy and configure CIAO at East Kent Hospitals University NHS Foundation Trust as a
proof of concept. It also details any problems, issues and resolutions.

## Environment
East Kent Hospitals University NHS Foundation Trust has provisioned a Linux VM at IP address EASTKENT (not disclosed in this document for security reasons) on which CIAO
will be deployed. They have configured network access over the N3 network that allows the HSCIC CIAO team to SSH in from a single SSH bastion server at IP address
HSCISBASTION (not disclosed in this document for security reasons) on the HSCIC network.
They have provided a SSH username and password to login to EASTKENT. This account has sudoer rights.

## 26/02/2016 - Mike Kelly
###(1) SSH into HSCICBASTION from Windows 7 laptop on HSCIC network using PuTTY.

**Success**

###(2) SSH from HSCICBASTION to EASTKENT.

`$ ssh USER@EASTKENT`

**Success**

###(3) Check account has sudoer rights.

`$ sudo -v`

Asks for password, so has sudo access.

**Success** 

###(4) Check VM has internet connectively to allow Linux package updates and to be able to pull down docker images.

`$ wget http://www.google.com`

**Failure** Resolves IP address, but sits there waiting.

**[ISSUE 01]** - does a suitable proxy need to be configured?

###(5) Package updates.

```
$ sudo apt-get update
...
$ sudo apt-get upgrade
...
```

**Success**

###(6) Check that the version of Linux meets the minimum requirements for Docker:
* 64-bit installation
* kernel must be 3.10 at minimum
 
```
$ uname -mrs
Linux 3.19.0-25-generic x86_64
```

**Success**

###(7) Check if Docker Engine installed:

```
$ sudo docker version
Client:
 Version:      1.9.1
 API version:  1.21
 Go version:   go1.4.2
 Git commit:   a34a1d5
 Built:        Fri Nov 20 13:12:04 UTC 2015
 OS/Arch:      linux/amd64

Server:
 Version:      1.9.1
 API version:  1.21
 Go version:   go1.4.2
 Git commit:   a34a1d5
 Built:        Fri Nov 20 13:12:04 UTC 2015
 OS/Arch:      linux/amd64
```

**Success**

**[NOTE 01]** This is not the current Docker version, 1.10.1, but this should not cause any issues. 

###(8) Verify Docker is installed correctly:

```
$ sudo docker run hello-world

Hello from Docker.
This message shows that your installation appears to be working correctly.

To generate this message, Docker took the following steps:
 1. The Docker client contacted the Docker daemon.
 2. The Docker daemon pulled the "hello-world" image from the Docker Hub.
 3. The Docker daemon created a new container from that image which runs the
    executable that produces the output you are currently reading.
 4. The Docker daemon streamed that output to the Docker client, which sent it
    to your terminal.

To try something more ambitious, you can run an Ubuntu container with:
 $ docker run -it ubuntu bash

Share images, automate workflows, and more with a free Docker Hub account:
 https://hub.docker.com

For more examples and ideas, visit:
 https://docs.docker.com/userguide/
```

**Success**

###(9) Install Ansible:

`$ sudo apt-get install software-properties-common`

**Success**

`$ sudo apt-add-repository ppa:ansible/ansible`

**Failure** Hangs.

**[ISSUE 02]** Probably related to [ISSUE 01], as needs http access.

TO DO:

`$ sudo apt-get update`

`$ sudo apt-get install ansible`

## 11/03/2016 - Mike Kelly
### [ISSUE 01]
Internet access is locked down to specific host names and ports. This should not be present any problems, as general access to the Internet is not required.

### [ISSUE 02]
This maybe a proxy issue.

Check apt configuration in `/etc/apt/apt.conf`:

`Acquire::http::Proxy "http://A.B.C.D:E";`

Make sure sudo user is aware of proxy settings:

```
export http_proxy="http://username:password@your proxy":"port" 
export https_proxy="https://username:password@your proxy":"port"
```

Now try to install Ansible again:


`$ sudo apt-add-repository ppa:ansible/ansible`

**Success**

`$ sudo apt-get update`


`$ sudo apt-get install ansible`

**Success**

Check ansible installed OK.

```
$ ansible --version
ansible 2.0.1.0
  config file = /etc/ansible/ansible.cfg
  configured module search path = Default w/o overrides
teleologic@ekciaoprd01:~$ nano /etc/apt/apt.conf
teleologic@ekciaoprd01:~$ ansible --version
ansible 2.0.1.0
  config file = /etc/ansible/ansible.cfg
  configured module search path = Default w/o overrides
```

**Success**
 