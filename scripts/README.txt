                  *** SOLAPortable Deployment Package ***

Target Platform: Windows 32bit or Windows 64bit. For Mac OS Yosemite 10.10 see
addendum below for additional install instructions 

SOLAPortable can be used to run Standalone SOLA applications from a host
computer with minimal installation and configuration. This package includes 
a Java Development Kit (JDK), a Postgresql server initialized with PostGIS 
and loaded with a demonstration SOLA database, a pre-configured Glassfish 
instance as well as supporting applications and scripts. 
 
This README provides installation instructions for this package and describes
how to use the SOLA Portable Control Panel. The Control Panel allows you to 
start and stop the Postgresql database and the Glassfish application server 
which are required services for running SOLA applications. It also captures
the log file output from these applications to assist with tracing errors and
resolving environment issues.

1. Installation
 
   a) Unzip the SOLAPortable.zip to your local hard disk or a portable hard 
      drive. Avoid extracting this package to a flash drive (i.e. USB stick) 
	  as the SOLA application and associated services may run very slowly. 
   b) The Postgresql database is dependent on a number of C++ libraries
      that are no longer shipped with the Microsoft Windows operating system.
      To install these libraries, run the vcredist_x86.exe file located in
      the installers directory. Use the default options for installation.  
      NOTE: You will need to run this file with administrator privileges. 

2) Using the SOLA Portable Control Panel (Basic Operation) 

   a) Double click SOLAPortable.bat to start the SOLA Portable Control 
      Panel. 
   b) Click the Start Servers button to launch the Postgresql database
      and the Glassfish server. Postgresql should start within 10 seconds
      however it can take up to 2 minutes for Glassfish to fully initialize
      and start. Wait until both Glassfish and the Database tabs show a 
      green globe before proceeding. 
   c) Click Start Desktop to launch the SOLA Desktop application. Login to
      the SOLA Desktop with the username and password of test, test. 
   d) When you have completed using the SOLA Desktop, return to the SOLA 
      Portable Control Panel and click the Stop Servers button. This will 
      shutdown both the Database and Glassfish services. Alternatively close
      the Control Panel and the services will be shutdown automatically.   	  
   
3) Using the SOLA Portable Control Panel (Other Features)  
   
   a) Start pgAdmin on the Database tab can be used to launch the pgAdmin
      application to view the contents of the demonstration database. You 
      will need to create a connection to the database with the following
      connection parameters; Host=localhost, Port=5444, Username=postgres
      Password=sola
   b) To copy any of the logs displayed in the Control Panel, click the 
      appropriate Copy Log button. This will copy the log into the clipboard
      allowing you to paste it into an email or another document. 
   c) The Show Working Directory can be used to identify the current working
      directory for the Control Panel. This is the location the 
	  config.properties file should be placed if you need to override the 
      default settings used by the Control Panel. 	  

4) Configuration Options

   Use the config.properties file to override the location of the scripts
   folder and/or the logs folder. You will need to use these overrides if
   the Control Panel is unable to locate the scripts used to start and stop
   the database and Glassfish servers.    

5) Troubleshooting

   a) If the database takes longer than 30 seconds to start, it may indicate
      an issue with the environment. Close the Control Panel and locate the 
      start-db.cmd script in the scripts folder. Double click this script to
      start Postgresql. If an error occurs during the startup, an error message
      should display that will make it easier to determine the cause of the
      problem. 
   b) If you encounter other issues running the database, Glassfish or the
      SOLA Desktop, copy the log files and send them to 
      andrew.mcdowell@fao.org.
	  
6) Product Versions in this Package (Windows)
 
   - Postgresql 9.4.1 l  username=postgres password=sola port=5444
   - PostGIS 2.1.5
   - Glassfish 4.1       default username and password
   - Geoserver 2.7.1    default username=admin password=geoserver
   - Java JDK8 update 40 Windows 32 bit
   - SOLA State Land Desktop v1503a
   
7) Mac Installation - Postgres Installation

    a)  It is necessary to do a standard installation of Postgres.App (a copy
        is in the SOLAPortable/installers folder)
    b)  Edit .profile file (in Home folder) and add an additional line -
        export PATH=/Applications/Postgres.app/Contents/Versions/9.4/bin:$PATH
    b)  Start Postgres from the Launch Pad
    c)  From the Postgres Control Panel (Elephant icon in menu bar at the top of
        the screen) edit the Data Directory to refer to Postgres data in the
        SOLA Portable folders ie something like /Users/<computer name>/<SOLA
        Portable folder>/postgresql/MacOS/ver-9.4
    d)  From the Postgres Control Panel Close Postgres and then re-launch
        Postgres from Launch Pad. Check that it is running on Port 5444
    e)  Install PgAdmin (a copy is in the SOLAPortable/installers folder) in the 
        /Applications folder
    
8) Mac Installation - Other Steps
    
    a)  Edit the config.properties file (in the SOLAPortable folder) so that the
        first line refers to the Mac scripts. ie the first line should read 
        something like SCRIPTS_FOLDER=/Users/<computer name>/<location of SOLA
        Portable folder>/SOLAPortable/scripts/macos
    b)  Delete SOLAPortable/jdk folder and copy the SOLAPortable/jdk_mac folder 
        and renaming the copied folder jdk
    b)  Run SOLA Portable by double click on SOLAPortable.command
    c)  Note that Postgres Database Start and Stop commands within SOLA Portable
        do not work and those operations need to be done through Launch Pad and
        Postgres. Likewise the Postgres log is not copied into /SOLAPortable/
        logs/postgresl.log

6) Product Versions in this Package (Mac)
 
   - Postgresql 9.4.2.0  username=postgres password=sola port=5444
   - PostGIS 2.1.7
   - pgAdmin 1.20.0
   - Glassfish 4.1       default username and password
   - Geoserver 2.7.1    default username=admin password=geoserver
   - Java JDK8 update 45 Mac 64 bit
   - SOLA Registry Desktop v1503a
   - SOLA Community Server v1.0 username=test   password=test
   - SOLA Web Admin v1.2  username=test   password=test       
        
