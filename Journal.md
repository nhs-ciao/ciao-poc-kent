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

Now try to install Ansible again, but export the environment variables of the user you are currently using:


`$ sudo -E apt-add-repository ppa:ansible/ansible`

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

###(1) Git clone ciao-poc-kent repository from GitHub

For the POC the CIAO team have setup a public repository on GitHub at `http://github.com/nhs-ciao/ciao-poc-kent.git`

`$ git clone http://github.com/nhs-ciao/ciao-poc-kent.git`

**Success**

The basic structure of the repository is:

ciao-poc-kent/Journal.md - This journal in markdown format

ciao-poc-kent/playbooks - The ansible playbooks and associated template and confiuration files

ciao-poc-kent/code - The source code projects for the Kent specific CIAO components

**[Note]** The git repository has been cloned into the home directory of USER@EASTKENT  

###(2) Configure Ansible

Edit USER@EASTKENT `~/ciao-poc-kent/playbooks/ansible.cfg`


```
[defaults]
inventory = hosts-s
remote_port = 22
remote_user = USER
ask_pass = True
nocows = 1
ask_sudo_pass = True
become_method = sudo
```

Change USER to USER@EASTKENT.

Edit USER@EASTKENT `~/ciao-poc-kent/playbooks/hosts-s`

```
singleton ansible_ssh_host=X.X.X.X
```

Change X.X.X.X to ip address of EASTKENT.

###(3) Setup SSH host

Ansible configuration has been setup to use password based SSH, which means first time you connect to a SSH host you get prompted around accepting the ECDSA 
fingerprint and adding the host to the list of known hosts. This initial prompt stops ansible, so it is easier to just set this up before running ansible by SSH to 
each host ansible will deploy to. In this case it is one host EASTKENT.

```
$ ssh USER@EASTKENT
The authenticity of host 'EASTKENT (EASTKENT)' can't be established.
ECDSA key fingerprint is 37:a7:45:d2:7a:6c:40:cb:c4:43:b4:39:8d:5c:bc:19.
Are you sure you want to continue connecting (yes/no)? yes
Warning: Permanently added 'EASTKENT' (ECDSA) to the list of known hosts.
Write failed: Broken pipe
```

###(4) Install CIAO base services

CIAO consists of a set of services, each running in their own Docker container. These services are divided in to base services and application services. Base services provide 
the generic infrastructure services such as message broker, while the application services provide the care specific services such as parsing a discharge document or creating a CDA 
document.

The deployment and configuration instructions are defined in Ansible playbooks (playbooks are written in YAML).

Base services deployment and configuration is defined in its own playbook with the file naming pattern `ciao-X-base.yml`. An associated file containing variable definitions that 
will be shared across playbooks has the file naming pattern `ciao-X-base-vars.yml`.

Application services deployment and configuration is defined in its own playbook with the file naming pattern `ciao-X-app-Y.yml`. An associated file containing variable definitions that 
will be shared across playbooks has the file naming pattern `ciao-X-app-Y-vars.yml`.

The order of deployment and configuration should be base services first followed by application services. An additional playbook with the file naming pattern 
`ciao-X-cloud-Y.yml` just calls the base services and application services playbooks in the correct order. Within CIAO a CLOUD is considered to be a deployment and configuration of
base and application services on a set of hosts.

For the purposes of this POC base services will be deployed, configured and installed directly from their playbook to allow testing, before moving onto the application services.
 
For a singleton install (everything on one host) the ansible playbook to use is USER@EASTKENT `~/ciao-poc-kent/playbooks/ciao-s-base.yml` 

Run the platbook:

```
$ cd ~/ciao-poc-kent/playbooks
$ ansible-playbook ciao-s-base.yml
SSH password:
SUDO password[defaults to SSH password]:
...
...
TASK [Install docker-py as a workaround for Ansible issue] *********************
fatal: [singleton]: FAILED! => {"changed": false, "cmd": "/usr/bin/pip install -U docker-py", "failed": true, "msg": "stdout: Cannot fetch index base URL https://pypi.python.org/simple/\nCould not find any downloads that satisfy the requirement docker-py in /usr/local/lib/python2.7/dist-packages\nDownloading/unpacking docker-py\nCleaning up...\nNo distributions at all found for docker-py in /usr/local/lib/python2.7/dist-packages\nStoring debug log for failure in /root/.pip/pip.log\n"}
```
 
**Failure**

The task is trying to install the pip package docker-py, but fails. Assume it is the same proxy issue as before with sudo user.

Workaround.

Manually install pip package:

```
$ sudo -E pip install docker-py
```

**Success**

Edit ciao-s-base.yml and comment out task:

```
---
# CIAO singleton base services
#

- name: Deploy ansible docker issues workaround
  hosts: all
  become: True
  tasks:
    - name: Install python pip
      apt: name=python-pip state=present update_cache=yes cache_valid_time=3600
#    - name: Install docker-py as a workaround for Ansible issue
#      pip: name=docker-py state=latest
```

Rerun playbook:

```
$ ansible-playbook ciao-s-base.yml
SSH password:                                                                                                                                                           SSH password:
SUDO password[defaults to SSH password]:

PLAY [Deploy ansible docker issues workaround] *********************************

TASK [setup] *******************************************************************
ok: [singleton]

TASK [Install python pip] ******************************************************
ok: [singleton]

PLAY [Deploy Nagios] ***********************************************************

TASK [setup] *******************************************************************
ok: [singleton]

TASK [Create Nagios configuration directory] ***********************************
ok: [singleton]

TASK [Create Nagios configuration commands sub-directory] **********************
ok: [singleton]

TASK [Create Nagios configuration timeperiods sub-directory] *******************
ok: [singleton]

TASK [Create Nagios configuration contacts sub-directory] **********************
ok: [singleton]

TASK [Create Nagios configuration hosts sub-directory] *************************
ok: [singleton]

TASK [Create Nagios configuration services sub-directory] **********************
ok: [singleton]

TASK [Install nagios.cfg] ******************************************************
ok: [singleton]

TASK [Install resource.cfg] ****************************************************
ok: [singleton]

TASK [Install templates.cfg] ***************************************************
ok: [singleton]

TASK [Install cgi.cfg] *********************************************************
ok: [singleton]

TASK [Install commands.cfg] ****************************************************
ok: [singleton]

TASK [Install timeperiods.cfg] *************************************************
ok: [singleton]

TASK [Install contacts.cfg] ****************************************************
ok: [singleton]

TASK [Install Nagios] **********************************************************
ok: [singleton]

PLAY [Deploy Nagios host and service configurations for monitoring] ************

TASK [setup] *******************************************************************
ok: [singleton]

TASK [Install host configurations for host in the CIAO cloud] ******************
ok: [singleton]

TASK [Install service configurations for host in the CIAO cloud] ***************
ok: [singleton]

PLAY [Deploy ELK] **************************************************************

TASK [setup] *******************************************************************
ok: [singleton]

TASK [Create Logstash configuration directory] *********************************
ok: [singleton]

TASK [Create Elastic data directory] *******************************************
ok: [singleton]

TASK [Install Logstash configuration file] *************************************
ok: [singleton]

TASK [Install ELK stack] *******************************************************
ok: [singleton]

PLAY [Deploy Nagios service configurations for monitoring ELK] *****************

TASK [setup] *******************************************************************
ok: [singleton]

TASK [Install service configurations for host running logstash] ****************
ok: [singleton]

TASK [Install service configurations for host running elastic] *****************
ok: [singleton]

TASK [Install service configurations for host running kibana] ******************
ok: [singleton]

PLAY [Deploy etcd Browser] *****************************************************

TASK [setup] *******************************************************************
ok: [singleton]

TASK [Install etcd browser] ****************************************************
ok: [singleton]

PLAY [Deploy Nagios service configurations for monitoring etcd Browser] ********

TASK [setup] *******************************************************************
ok: [singleton]

TASK [Install service configuration for host running etcd Browser] *************
ok: [singleton]

PLAY [Deploy Logspout] *********************************************************

TASK [setup] *******************************************************************
ok: [singleton]

TASK [Install Logspout] ********************************************************
ok: [singleton]

PLAY [Deploy Nagios service configurations for monitoring logspout] ************

TASK [setup] *******************************************************************
ok: [singleton]

TASK [Install service configurations for host running logspout] ****************
ok: [singleton]

PLAY [Deploy etcd] *************************************************************

TASK [setup] *******************************************************************
ok: [singleton]

TASK [Install etcd] ************************************************************
ok: [singleton]

PLAY [Deploy Nagios service configurations for monitoring etcd] ****************

TASK [setup] *******************************************************************
ok: [singleton]

TASK [Install service configurations for host running etcd] ********************
ok: [singleton]

PLAY [Deploy ActiveMQ] *********************************************************

TASK [setup] *******************************************************************
ok: [singleton]

TASK [Create ActiveMQ configuration directory] *********************************
ok: [singleton]

TASK [Install broker configuration file] ***************************************
ok: [singleton]

TASK [Install other configuration files] ***************************************
ok: [singleton] => (item=log4j.properties)
ok: [singleton] => (item=broker.ks)
ok: [singleton] => (item=broker.ts)
ok: [singleton] => (item=broker-localhost.cert)
ok: [singleton] => (item=client.ks)
ok: [singleton] => (item=client.ts)
ok: [singleton] => (item=credentials.properties)
ok: [singleton] => (item=credentials-enc.properties)
ok: [singleton] => (item=groups.properties)
ok: [singleton] => (item=jetty.xml)
ok: [singleton] => (item=jetty-realm.properties)
ok: [singleton] => (item=jmx.access)
ok: [singleton] => (item=jmx.password)
ok: [singleton] => (item=logging.properties)
ok: [singleton] => (item=login.config)
ok: [singleton] => (item=users.properties)

TASK [Install ActiveMQ] ********************************************************
ok: [singleton]

PLAY [Deploy Nagios service configurations for monitoring ActiveMQ] ************

TASK [setup] *******************************************************************
ok: [singleton]

TASK [Install service configurations for host running ActiveMQ Web Console] ****
ok: [singleton]

PLAY [Restart Nagios] **********************************************************

TASK [setup] *******************************************************************
ok: [singleton]

TASK [Restart Nagios] **********************************************************
changed: [singleton]

PLAY RECAP *********************************************************************
singleton                  : ok=50   changed=1    unreachable=0    failed=0

$
```

**Success**

###(5) Check CIAO base services are running

See what docker containers are running:

```
$ sudo docker ps

CONTAINER ID        IMAGE                        COMMAND                  CREATED             STATUS              PORTS                                                                        NAMES
67d61569d5e6        hscic/ciao-activemq          "bin/bash -c '/opt/ac"   10 minutes ago      Up 10 minutes       0.0.0.0:8161->8161/tcp, 0.0.0.0:61616->61616/tcp, 0.0.0.0:61619->61619/tcp   ciao-activemq
c69573029f87        quay.io/coreos/etcd:v2.0.8   "/etcd"                  10 minutes ago      Up 10 minutes       0.0.0.0:2379-2380->2379-2380/tcp, 0.0.0.0:4001->4001/tcp, 7001/tcp           ciao-etcd
633a542d8f4b        gliderlabs/logspout          "/bin/logspout syslog"   10 minutes ago      Up 10 minutes       0.0.0.0:8000->8000/tcp                                                       ciao-logspout
cd2cbbe6b665        buddho/etcd-browser          "nodejs server.js"       10 minutes ago      Up 10 minutes       0.0.0.0:7999->8000/tcp                                                       ciao-etcdbrowser
7e4707278960        hscic/ciao-elk               "/usr/bin/supervisord"   11 minutes ago      Up 11 minutes       0.0.0.0:514->514/tcp, 0.0.0.0:9200->9200/tcp, 0.0.0.0:8080->80/tcp           ciao-elk
9dbc8288cc80        tpires/nagios                "/usr/local/bin/start"   12 minutes ago      Up 4 minutes        0.0.0.0:8081->80/tcp                                                         ciao-nagios
```

**Success**

We have the base services running:

* The message broker: ciao-activemq
* The key value store: ciao-etcd
* The log collector: ciao-logspout
* The key value store browser: ciao-etcdbrowser
* The log store and reporting dashboard: ciao-elk
* The service management console: ciao-nagios

TO DO - check consoles and logs of base services.

## 14/03/2016 - Mike Kelly

###(1) Check base services running

```
$ sudo docker ps
```
Shows that all the base services have been up and running since install 2 days ago.

###(2) Check base services logs

```
$ sudo docker logs ciao-activemq
...
$ sudo docker logs ciao-etcd
...
$ sudo docker logs ciao-logspout
...
$ sudo docker logs ciao-etcdbrowser
...
$ sudo docker logs ciao-elk
...
$ sudo docker logs ciao-nagios
...
```

Issue with logspout:

```
2016/03/12 05:50:11 syslog: write udp 10.136.219.57:514: connection refused
2016/03/12 05:50:12 syslog: write udp 10.136.219.57:514: connection refused
2016/03/12 05:50:13 syslog: write udp 10.136.219.57:514: connection refused
2016/03/12 05:50:17 syslog: write udp 10.136.219.57:514: connection refused
```

Trying to UDP to syslog - which is logstash running in the ELK container.

Issue with ELK:

```
2016-03-12 03:17:50,788 INFO spawned: 'logstash' with pid 31537
2016-03-12 03:17:51,790 INFO success: logstash entered RUNNING state, process has stayed up for > than 1 seconds (startsecs)
2016-03-12 03:17:56,420 INFO exited: logstash (exit status 1; not expected)
2016-03-12 03:17:57,422 INFO spawned: 'logstash' with pid 31568
2016-03-12 03:17:58,424 INFO success: logstash entered RUNNING state, process has stayed up for > than 1 seconds (startsecs)
2016-03-12 03:18:03,113 INFO exited: logstash (exit status 1; not expected)
```
Logstash is crashing. As logstash acts as the syslog server, might be the cause of the logspout issue. Therefore investigate this issue first (TO DO).

## 15/03/2016 - Mike Kelly
###(1) Install MESH test client

Check apt configuration in `/etc/apt/apt.conf`:

`Acquire::http::Proxy "http://A.B.C.D:E";`

Make sure sudo user is aware of proxy settings:

```
export http_proxy="http://username:password@your proxy":"port" 
export https_proxy="https://username:password@your proxy":"port"
```
Get the current mesh test client:

```
$ sudo -E wget http://systems.hscic.gov.uk/ddc/mesh/test-client/mesh-6.0.0.jar
```

**Success**

MESH needs Java 1.7. So check if Java installed:

```
$ java -version
The program 'java' can be found in the following packages:
 * default-jre
 * gcj-4.8-jre-headless
 * openjdk-7-jre-headless
 * gcj-4.6-jre-headless
 * openjdk-6-jre-headless
Try: sudo apt-get install <selected package>
```

Java not installed so install it:

```
$ sudo apt-get install openjdk-7-jdk
```

Check if Java now installed:

```
$ java -version
java version "1.7.0_95"
OpenJDK Runtime Environment (IcedTea 2.6.4) (7u95-2.6.4-0ubuntu0.14.04.1)
OpenJDK 64-Bit Server VM (build 24.95-b01, mixed mode)
```

**Success**

Install MESH client (the install dialogue is very verbose!):

```
$ java -jar mesh-6.0.0.jar
15-Mar-2016 14:31:58 INFO: Logging initialized at level 'INFO'
15-Mar-2016 14:31:59 INFO: Commandline arguments:
15-Mar-2016 14:31:59 INFO: Detected platform: ubuntu_linux,version=3.19.0-25-generic,arch=x64,symbolicName=null,javaVersion=1.7.0_95
15-Mar-2016 14:31:59 INFO: Cannot find named resource: 'userInputLang.xml' AND 'userInputLang.xml_eng'
15-Mar-2016 14:31:59 INFO: Cannot find named resource: 'userInputLang.xml' AND 'userInputLang.xml_eng'
15-Mar-2016 14:31:59 INFO: Cannot find named resource: 'userInputLang.xml' AND 'userInputLang.xml_eng'
15-Mar-2016 14:32:00 INFO: Cannot find named resource: 'userInputLang.xml' AND 'userInputLang.xml_eng'
Welcome to the installation of HSCIC MESH Client 6.0.0_rc1_20160309!

Press 1 to continue, 2 to quit, 3 to redisplay
1
Select the installation path:  [/home/teleologic/MESH-APP-HOME]


Press 1 to continue, 2 to quit, 3 to redisplay
1
15-Mar-2016 14:32:20 INFO: Cannot find named resource: 'userInputLang.xml' AND 'userInputLang.xml_eng'
Legacy DTS Client Settings

Is the legacy DTS Client already installed on this computer?
0  [ ] Yes
1  [x] No
Input selection:
1

Press 1 to continue, 2 to quit, 3 to redisplay
1
15-Mar-2016 14:32:29 INFO: Cannot find named resource: 'userInputLang.xml' AND 'userInputLang.xml_eng'
Select location for data files

------------------------------------------

 [/home/teleologic/MESH-DATA-HOME]

-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
Message
The target directory will be created:
/home/teleologic/MESH-DATA-HOME
-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
Enter O for OK, C to Cancel:
O

Press 1 to continue, 2 to quit, 3 to redisplay
1
15-Mar-2016 14:32:48 INFO: Cannot find named resource: 'userInputLang.xml' AND 'userInputLang.xml_eng'
Allow Automatic Updates

Allow new versions of the MESH Client to be downloaded and automatically installed?
0  [ ] Yes
1  [x] No
Input selection:
1

Press 1 to continue, 2 to quit, 3 to redisplay
1
[ Starting to unpack ]
[ Processing package: Main Application (1/2) ]
[ Processing package: MESH Files (2/2) ]
[ Unpacking finished ]
[ Starting processing ]
Starting process DTSConverter (1/1)
DTS Configuration Converter
Set LogFile variable
Copied variables from Wizard
Installing Mesh Client Application
Checking whether DTS Client configuration is to be used...
DTS Reconfiguration not required. Setting up Single Mailbox
-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
Generate an automatic installation script
-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
Enter Y for Yes, N for No:
Y
Select the installation script (path must be absolute)[/home/teleologic/MESH-APP-HOME/auto-install.xml]

Installation was successful
application installed on /home/teleologic/MESH-APP-HOME
[ Writing the uninstaller data ... ]
[ Console installation done ]
```

TO DO - setup keystore and meshclient.cfg file.
 