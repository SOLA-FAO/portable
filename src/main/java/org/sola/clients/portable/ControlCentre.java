/**
 * ******************************************************************************************
 * Copyright (C) 2015 - Food and Agriculture Organization of the United Nations
 * (FAO). All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,this
 * list of conditions and the following disclaimer. 2. Redistributions in binary
 * form must reproduce the above copyright notice,this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. 3. Neither the name of FAO nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT,STRICT LIABILITY,OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * *********************************************************************************************
 */
package org.sola.clients.portable;

import java.awt.Desktop;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.swing.ImageIcon;
import javax.swing.SwingWorker;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

/**
 * Can be used for a Standalone version of SOLA to control startup of the
 * database and Glassfish. Captures the output to the various log files and
 * provides some useful functions to help debug any issues.
 *
 * @author soladev
 */
public class ControlCentre extends javax.swing.JFrame {

    private static final String SCRIPTS_FOLDER = "SCRIPTS_FOLDER";
    private static final String LOGS_FOLDER = "LOGS_FOLDER";
    private static final String SHOW_DESKTOP = "SHOW_DESKTOP";
    private static final String SHOW_DATABASE = "SHOW_DATABASE";
    private static final String SHOW_COMMUNITY_SERVER = "SHOW_COMMUNITY_SERVER";
    private static final String SHOW_GLASSFISH = "SHOW_GLASSFISH";
    private static final String SCRIPT_EXTENSION = "SCRIPT_EXTENSION";
    private static final String DESKTOP_WEB_ADMIN_URL = "DESKTOP_WEB_ADMIN_URL";
    private static final String CS_GLASSFISH_ADMIN_URL = "CS_GLASSFISH_ADMIN_URL";
    private static final String GLASSFISH_ADMIN_URL = "GLASSFISH_ADMIN_URL";
    private static final String GEOSERVER_ADMIN_URL = "GEOSERVER_ADMIN_URL";
    private static final String CS_URL = "CS_URL";
    private static final String CS_WEB_ADMIN_URL = "CS_WEB_ADMIN_URL";
    private static final int SLEEP = 2000;
    Tailer glassfishTailer = null;
    Thread glassfishTailerThread = null;
    Tailer geoserverTailer = null;
    Thread geoserverTailerThread = null;
    private boolean moduleStateError = false;
    private boolean startServers = false;
    private boolean stopServers = false;
    private boolean glassfishRunning = false;
    private boolean databaseRunning = false;
    private boolean geoserverRunning = false;
    private boolean close = false;
    private String scriptExtension = ".cmd";

    /**
     * Creates new form ControlCentre
     */
    public ControlCentre() {
        initComponents();
        postInit();
    }

    private void postInit() {
        // Load the image icons to use for the ControlCentre. 
        List<Image> iconList = new ArrayList<Image>();
        ImageIcon img = new ImageIcon(this.getClass().getResource("/images/Favicon16x16.png"));
        if (img.getImage() != null) {
            iconList.add(img.getImage());
        }
        img = new ImageIcon(this.getClass().getResource("/images/Favicon32x32.png"));
        if (img.getImage() != null) {
            iconList.add(img.getImage());
        }
        img = new ImageIcon(this.getClass().getResource("/images/Favicon48x48.png"));
        if (img.getImage() != null) {
            iconList.add(img.getImage());
        }
        this.setIconImages(iconList);

        if (getConfig(SHOW_DESKTOP, "Y").equals("N")) {
            tabMain.removeTabAt(tabMain.indexOfComponent(tabDesktop));
        }

        if (getConfig(SHOW_DATABASE, "Y").equals("N")) {
            tabMain.removeTabAt(tabMain.indexOfComponent(tabDatabase));
        }

        if (getConfig(SHOW_COMMUNITY_SERVER, "Y").equals("N")) {
            tabMain.removeTabAt(tabMain.indexOfComponent(tabGeoserver));
        }

        if (getConfig(SHOW_GLASSFISH, "Y").equals("N")) {
            tabMain.removeTabAt(tabMain.indexOfComponent(tabGlassfish));
        }

        scriptExtension = getConfig(SCRIPT_EXTENSION, ".cmd");
    }

    private boolean isDatabaseRunning() {
        boolean result = true;
        if (getConfig(SHOW_DATABASE, "Y").equals("Y")) {
            result = databaseRunning;
        }
        return result;
    }

    private boolean isGlassfishRunning() {
        boolean result = true;
        if (getConfig(SHOW_DATABASE, "Y").equals("Y")) {
            result = glassfishRunning;
        }
        return result;
    }

    private boolean isGeoserverRunning() {
        boolean result = true;
        if (getConfig(SHOW_COMMUNITY_SERVER, "Y").equals("Y")) {
            result = geoserverRunning;
        }
        return result;
    }

    /**
     * Used to execute a batch script at a command line on a background thread.
     * The batch scripts can be configured to start other processes like the
     * Database or Glassfish, and provide any additional parameters or
     * environmental setup as required.
     *
     * @param displayLog The log to show the output from the script or any
     * subprocess that writes to Standard Out or Standard Error
     * @param command command to execute. Usually just the location and name of
     * the script but can include script arguments (as extra string parameters)
     * as required.
     */
    private void runProcess(final javax.swing.JTextArea displayLog, final String... command) {
        SwingWorker<String, String> task = new SwingWorker() {
            @Override
            protected String doInBackground() throws Exception {
                BufferedReader is = null;

                try {
                    // Use ProcessBuilder to execute the command
                    ProcessBuilder pb = new ProcessBuilder(command);
                    // Redirect Standard Error to Standard Out to simplify capture 
                    // of script error messages. 
                    pb = pb.redirectErrorStream(true);
                    Process dbProcess = pb.start();

                    if (displayLog != null) {
                        // Capture the output of the new process and show it in 
                        // the specified display log. Check for messages that indicate
                        // the process has executed successfully or  errors. 
                        is = new BufferedReader(new InputStreamReader(dbProcess.getInputStream()));
                        String line;
                        while ((line = is.readLine()) != null) {
                            if (displayLog == txtDatabaseLog) {
                                if (line.matches("(.*)server started(.*)")) {
                                    writeUserMessage("The database is now running");
                                    setDatabaseRunning(true);
                                } else if (line.matches("(.*)server stopped(.*)")) {
                                    writeUserMessage("The database has shutdown");
                                    setDatabaseRunning(false);
                                } else if (line.matches("(.*)The process cannot access the file because it is being used by another process(.*)")) {
                                    writeUserMessage("The database is already running");
                                    setDatabaseRunning(true);
                                } else if (line.matches("(.*)psql: could not connect to server: Connection refused(.*)")) {
                                    writeUserMessage("The database was not running");
                                    setDatabaseRunning(false);
                                } else if (line.matches("(.*)waiting for server to start(.*)stopped waiting(.*)")) {
                                    writeUserMessage("Error starting the database. See the postgresql.log for details.");
                                    setDatabaseRunning(false);
                                } else if (line.matches("(.*)pgadmin3.lng(.*)")) {
                                    // A fake error when starting pgAdmin. Don't display it in the log.
                                    line = null;
                                } else if (line.matches("(.*)override start(.*)")) {
                                    writeUserMessage("Override in place, showing database as running...");
                                    line = null;
                                    setDatabaseRunning(true);
                                } else if (line.matches("(.*)override stop(.*)")) {
                                    writeUserMessage("Override in place, showing database as stopped...");
                                    setDatabaseRunning(false);
                                    line = null;
                                }
                            } else if (displayLog == txtGlassfishLog) {
                                if (line.matches("(.*)Command stop-domain executed successfully(.*)")) {
                                    writeUserMessage("Glassfish has shutdown");
                                    setGlassfishRunning(false);
                                } else if (line.matches("(.*)There is a process already using the admin port 4848(.*)")) {
                                    writeUserMessage("Glassfish is already running.");
                                    setGlassfishRunning(true);
                                } else if (line.matches("(.*)override start(.*)")) {
                                    writeUserMessage("Override in place, showing Glassfish as running...");
                                    line = null;
                                    setGlassfishRunning(true);
                                } else if (line.matches("(.*)override stop(.*)")) {
                                    writeUserMessage("Override in place, showing Glassfish as stopped...");
                                    setGlassfishRunning(false);
                                    line = null;
                                }
                            } else if (displayLog == txtGeoserverLog) {
                                if (line.matches("(.*)Command stop-domain executed successfully(.*)")) {
                                    writeUserMessage("Community Server has shutdown");
                                    setGeoserverRunning(false);
                                } else if (line.matches("(.*)There is a process already using the admin port 4849(.*)")) {
                                    writeUserMessage("Community Server is already running.");
                                    setGeoserverRunning(true);
                                } else if (line.matches("(.*)override start(.*)")) {
                                    writeUserMessage("Override in place, showing Geoserver as running...");
                                    line = null;
                                    setGeoserverRunning(true);
                                } else if (line.matches("(.*)override stop(.*)")) {
                                    writeUserMessage("Override in place, showing Geoserver as stopped...");
                                    setGeoserverRunning(false);
                                    line = null;
                                }
                            }
                            if (line != null) {
                                displayLog.append(line + System.lineSeparator());
                            }
                        }
                    }
                    return "";
                } catch (Exception ex) {
                    writeException(displayLog, ex);
                    return null;
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            writeException(displayLog, e);
                        }
                    }
                }
            }
        };
        task.execute();
    }

    private void setDatabaseRunning(boolean isRunning) {
        databaseRunning = isRunning;
        if (isRunning) {
            if (tabMain.indexOfComponent(tabDatabase) >= 0) {
                tabMain.setIconAt(tabMain.indexOfComponent(tabDatabase), new ImageIcon(this.getClass().getResource("/images/status.png")));
            }
            if (startServers) {
                // Start Glassfish once the database is running. 
                startGlassfish();
            }
        } else {
            if (tabMain.indexOfComponent(tabDatabase) >= 0) {
                tabMain.setIconAt(tabMain.indexOfComponent(tabDatabase), new ImageIcon(this.getClass().getResource("/images/status-busy.png")));
            }
            if (close) {
                // Close the screen if all services are shutdown. 
                close();
            }
        }
    }

    private void setGlassfishRunning(boolean isRunning) {
        glassfishRunning = isRunning;
        if (isRunning) {
            if (tabMain.indexOfComponent(tabGlassfish) >= 0) {
                tabMain.setIconAt(tabMain.indexOfComponent(tabGlassfish), new ImageIcon(this.getClass().getResource("/images/status.png")));
            }
            if (startServers) {
                startGeoserver();
            }
        } else {
            if (stopServers) {
                // Stop the database once Glassfish has stopped. 
                stopGeoserver();
            }
            if (tabMain.indexOfComponent(tabGlassfish) >= 0) {
                tabMain.setIconAt(tabMain.indexOfComponent(tabGlassfish), new ImageIcon(this.getClass().getResource("/images/status-busy.png")));
            }
        }
    }

    private void setGeoserverRunning(boolean isRunning) {
        geoserverRunning = isRunning;
        if (isRunning) {
            if (tabMain.indexOfComponent(tabGeoserver) >= 0) {
                tabMain.setIconAt(tabMain.indexOfComponent(tabGeoserver), new ImageIcon(this.getClass().getResource("/images/status.png")));
            }
        } else {
            if (stopServers) {
                // Stop the database once Glassfish has stopped. 
                stopDatabase();
            }
            if (tabMain.indexOfComponent(tabGeoserver) >= 0) {
                tabMain.setIconAt(tabMain.indexOfComponent(tabGeoserver), new ImageIcon(this.getClass().getResource("/images/status-busy.png")));
            }
        }
    }

    /**
     * Writes a message to the Text Area at the top of the screen summarizing
     * the current state of the services along with any other information
     * messages.
     *
     * @param message
     */
    private void writeUserMessage(String message) {
        txtOut.append(message + System.lineSeparator());
        txtOut.setCaretPosition(txtOut.getDocument().getLength());
    }

    /**
     * Create a Tailer to track any output to the process log file and display
     * it to the user. Can also parse log file output to determine if specific
     * errors occur and/or when the process is ready to accept further input or
     * connections.
     *
     * @param displayLog The TextArea to output the log file messages to.
     * @param logFile The path and name of the logfile. Can be a relative path
     * file name.
     * @return
     */
    private Tailer createTailer(final javax.swing.JTextArea displayLog, String logPathFileName) {
        TailerListenerAdapter tailerListener = new TailerListenerAdapter() {

            @Override
            public void handle(String line) {
                if (displayLog == txtGlassfishLog) {
                    // Check for known Exception messages in the message line
                    if (line.matches("(.*)-- Inconsistent Module State(.*)")) {
                        moduleStateError = true;
                        // Delete the generated folder in glassfish
                        writeUserMessage("***Module State Error detected*** " + System.lineSeparator()
                                + " Wait for Glassfish to complete its startup, then stop Glassfish and delete "
                                + "the folder called generated in glassfish/glassfish/domains/domain1. "
                                + "Once the generated folder is deleted, restart Glassfish again. " + System.lineSeparator());
                    } else if (line.matches("(.*)Loading application sola-(.*)services-ear done in(.*)")) {
                        // Checks when Glassfish is completely started up
                        writeUserMessage("Glassfish is now running");
                        setGlassfishRunning(true);
                    } else if (line.matches("(.*)Connection refused. Check that the hostname and port are correct(.*)")) {
                        // Delete the generated folder in glassfish
                        writeUserMessage("*** Database not running on port 5444 *** " + System.lineSeparator()
                                + " Wait for Glassfish to complete its startup, then stop Glassfish. Start up "
                                + "the Postgresql database on port 5444 and once its started, "
                                + "try to start Glassfish again. " + System.lineSeparator());
                    }
                } else if (displayLog == txtGeoserverLog) {
                    // Check for known Exception messages in the message line
                    if (line.matches("(.*)-- Inconsistent Module State(.*)")) {
                        moduleStateError = true;
                        // Delete the generated folder in glassfish
                        writeUserMessage("***Module State Error detected*** " + System.lineSeparator()
                                + " Wait for Community Server to complete its startup, then stop Community Server and delete "
                                + "the folder called generated in glassfish/glassfish/domains/geoserver. "
                                + "Once the generated folder is deleted, restart Community Server again. " + System.lineSeparator());
                    } else if (line.matches("(.*)Loading application geoserver done in(.*)")) {
                        // Checks when Glassfish is completely started up
                        writeUserMessage("Geoserver is now running");
                        setGeoserverRunning(true);
                    } else if (line.matches("(.*)Loading application sola-cs-services-ear done in(.*)")) {
                        // Checks when Glassfish is completely started up
                        writeUserMessage("Community Server is now running");
                        setGeoserverRunning(true);
                    } else if (line.matches("(.*)Connection refused. Check that the hostname and port are correct(.*)")) {
                        // Delete the generated folder in glassfish
                        writeUserMessage("*** Database not running on port 5444 *** " + System.lineSeparator()
                                + " Wait for Community Server to complete its startup, then stop Community Server. Start up "
                                + "the Postgresql database on port 5444 and once its started, "
                                + "try to start Community Server again. " + System.lineSeparator());
                    }
                }

                // Auto scroll the display log to the bottom as each line is added. 
                displayLog.append(line
                        + System.lineSeparator());
                displayLog.setCaretPosition(displayLog.getDocument().getLength());
            }

        };
        return new Tailer(
                new File(logPathFileName), tailerListener, SLEEP, true);
    }

    /**
     * Writes exception details to the specified display log
     *
     * @param displayLog
     * @param ex
     */
    private void writeException(final javax.swing.JTextArea displayLog, Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        displayLog.append(sw.toString());
    }

    /**
     * Retrieves the config.properties to allow the default script and log
     * locations to be reset if the current working directory is not in the
     * expected location.
     *
     * @param propertyName
     * @param defaulValue
     * @return
     */
    private String getConfig(String propertyName, String defaulValue) {
        Properties props = new Properties();
        InputStream input = null;
        String result = defaulValue;
        try {
            input = new FileInputStream("config.properties");

            // load a properties file
            props.load(input);
            String value = props.getProperty(propertyName);
            if (value != null && !"".equals(value.trim())) {
                result = value;
            }

        } catch (IOException ex) {
            writeException(txtOut, ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    writeException(txtOut, e);
                }
            }
        }
        return result;
    }

    private void startDatabase() {
        if (getConfig(SHOW_DATABASE, "Y").equals("Y")) {
            writeUserMessage("Starting up the database...");
            tabMain.setIconAt(tabMain.indexOfComponent(tabDatabase), new ImageIcon(this.getClass().getResource("/images/status-away.png")));
            tabMain.setSelectedIndex(tabMain.indexOfComponent(tabDatabase));
            txtDatabaseLog.setText("");
            // The location of the scripts folder can be overriden in config.properties. 
            runProcess(txtDatabaseLog, getConfig(SCRIPTS_FOLDER, "./scripts") + "/db-start" + scriptExtension);
        } else {
            setDatabaseRunning(true);
        }
    }

    private void stopDatabase() {
        stopServers = false;
        if (getConfig(SHOW_DATABASE, "Y").equals("Y")) {
            if (tabMain.indexOfComponent(tabDatabase) >= 0) {
                tabMain.setSelectedIndex(tabMain.indexOfComponent(tabDatabase));
            }
            // The location of the scripts folder can be overriden in config.properties. 
            runProcess(txtDatabaseLog, getConfig(SCRIPTS_FOLDER, "./scripts") + "/db-stop" + scriptExtension);
        } else {
            setDatabaseRunning(false);
        }
    }

    private void startGlassfish() {
        if (getConfig(SHOW_GLASSFISH, "Y").equals("Y")) {
            writeUserMessage("Starting up Glassfish. This can take 1 to 2 minutes. Please wait...");
            if (tabMain.indexOfComponent(tabGlassfish) >= 0) {
                tabMain.setIconAt(tabMain.indexOfComponent(tabGlassfish), new ImageIcon(this.getClass().getResource("/images/status-away.png")));
                tabMain.setSelectedIndex(tabMain.indexOfComponent(tabGlassfish));
            }
            moduleStateError = false;
            txtGlassfishLog.setText("");
            // The location of the logs folder can be overriden in config.properties. 
            glassfishTailer = createTailer(txtGlassfishLog, getConfig(LOGS_FOLDER, "./logs") + "/glassfish.log");
            glassfishTailerThread = new Thread(glassfishTailer);
            glassfishTailerThread.start();
            // The location of the scripts folder can be overriden in config.properties. 
            runProcess(txtGlassfishLog, getConfig(SCRIPTS_FOLDER, "./scripts") + "/glassfish-start" + scriptExtension);
        } else {
            setGlassfishRunning(true);
        }
    }

    private void stopGlassfish() {
        if (getConfig(SHOW_GLASSFISH, "Y").equals("Y")) {
            try {
                if (tabMain.indexOfComponent(tabGlassfish) >= 0) {
                    tabMain.setSelectedIndex(tabMain.indexOfComponent(tabGlassfish));
                }
                // The location of the scripts folder can be overriden in config.properties. 
                runProcess(txtGlassfishLog, getConfig(SCRIPTS_FOLDER, "./scripts") + "/glassfish-stop" + scriptExtension);
            } finally {
                // Make sure the glassfishTrailer is stopped and the thread killed off/interrupted.  
                if (glassfishTailer != null) {
                    glassfishTailer.stop();
                    glassfishTailer = null;
                }
                if (glassfishTailerThread != null) {
                    glassfishTailerThread.interrupt();
                    glassfishTailerThread = null;
                }
            }
        } else {
            setGlassfishRunning(false);
        }
    }

    private void startGeoserver() {
        if (getConfig(SHOW_COMMUNITY_SERVER, "Y").equals("Y")) {
            writeUserMessage("Starting up Community Server & Geoserver. This can take 1 to 2 minutes. Please wait...");
            if (tabMain.indexOfComponent(tabGeoserver) >= 0) {
                tabMain.setIconAt(tabMain.indexOfComponent(tabGeoserver), new ImageIcon(this.getClass().getResource("/images/status-away.png")));
                tabMain.setSelectedIndex(tabMain.indexOfComponent(tabGeoserver));
            }
            txtGeoserverLog.setText("");
            startServers = false;
            // The location of the logs folder can be overriden in config.properties. 
            geoserverTailer = createTailer(txtGeoserverLog, getConfig(LOGS_FOLDER, "./logs") + "/geoserver.log");
            geoserverTailerThread = new Thread(geoserverTailer);
            geoserverTailerThread.start();
            // The location of the scripts folder can be overriden in config.properties. 
            runProcess(txtGeoserverLog, getConfig(SCRIPTS_FOLDER, "./scripts") + "/geoserver-start" + scriptExtension);
        } else {
            setGeoserverRunning(true);
        }
    }

    private void stopGeoserver() {
        if (getConfig(SHOW_COMMUNITY_SERVER, "Y").equals("Y")) {
            try {
                if (tabMain.indexOfComponent(tabGeoserver) >= 0) {
                    tabMain.setSelectedIndex(tabMain.indexOfComponent(tabGeoserver));
                }
                // The location of the scripts folder can be overriden in config.properties. 
                runProcess(txtGeoserverLog, getConfig(SCRIPTS_FOLDER, "./scripts") + "/geoserver-stop" + scriptExtension);
            } finally {
                // Make sure the geoserverTrailer is stopped and the thread killed off/interrupted.  
                if (geoserverTailer != null) {
                    geoserverTailer.stop();
                    geoserverTailer = null;
                }
                if (geoserverTailerThread != null) {
                    geoserverTailerThread.interrupt();
                    geoserverTailerThread = null;
                }
            }
        } else {
            setGeoserverRunning(false);
        }
    }

    private void startServers() {
        startServers = true;
        startDatabase();
        // Glassfish will starte once the database is started. 
        // See setDatabaseRunning method
    }

    private void stopServers() {
        stopServers = true;
        stopGlassfish();
        // Database will stop once the database is stopped
        // See setGlassfishRunning method
    }

    private void startDesktop() {
        if (isGlassfishRunning() && isDatabaseRunning()) {
            writeUserMessage("Starting the Desktop...");
            if (tabMain.indexOfComponent(tabDesktop) >= 0) {
                tabMain.setSelectedIndex(tabMain.indexOfComponent(tabDesktop));
            }
            txtDesktopLog.setText("");
            runProcess(txtDesktopLog, getConfig(SCRIPTS_FOLDER, "./scripts") + "/desktop-start" + scriptExtension);
        } else {
            writeUserMessage("Servers are not running. Start the servers before running the Desktop.");
        }
    }

    private void startPgAdmin() {
        if (isDatabaseRunning()) {
            writeUserMessage("Starting up pgAdmin...");
            runProcess(txtDatabaseLog, getConfig(SCRIPTS_FOLDER, "./scripts") + "/db-pgadmin" + scriptExtension);
            writeUserMessage("Use the following connection parameters > Host=localhost, Port=5444, Username=postgres, Password=sola");
        } else {
            writeUserMessage("Database must be running before you can start pgAdmin.");
        }
    }

    private void openWebPage(String msg, String url) {
        writeUserMessage(String.format(msg, url));
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
            }
        }
    }

    /**
     * Copies the contents of the specified log to the clipboard so the user can
     * paste those contents into an email or document.
     *
     * @param displayLog
     * @param logName
     */
    private void copyLog(final javax.swing.JTextArea displayLog, String logName) {
        StringSelection ss = new StringSelection(displayLog.getText());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
        writeUserMessage(logName + " log has been copied to the clipboard");
    }

    private void close() {
        this.dispose();
        System.exit(0);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSeparator7 = new javax.swing.JSeparator();
        jPanel1 = new javax.swing.JPanel();
        tabMain = new javax.swing.JTabbedPane();
        tabDesktop = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        txtDesktopLog = new javax.swing.JTextArea();
        jToolBar2 = new javax.swing.JToolBar();
        btnStartDesktopB = new javax.swing.JButton();
        btnDesktopAdmin = new javax.swing.JButton();
        jSeparator9 = new javax.swing.JToolBar.Separator();
        btnDesktopCopy = new javax.swing.JButton();
        tabGlassfish = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        txtGlassfishLog = new javax.swing.JTextArea();
        jToolBar3 = new javax.swing.JToolBar();
        btnStartGlassfish = new javax.swing.JButton();
        btnStopGlassfish = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        btnGlassfishAdminConsole = new javax.swing.JButton();
        jSeparator6 = new javax.swing.JToolBar.Separator();
        btnGlassfishCopy = new javax.swing.JButton();
        tabGeoserver = new javax.swing.JPanel();
        jToolBar5 = new javax.swing.JToolBar();
        btnStartGeoserver = new javax.swing.JButton();
        btnStopGeoserver = new javax.swing.JButton();
        jSeparator10 = new javax.swing.JToolBar.Separator();
        btnCSWebsite = new javax.swing.JButton();
        btnCSAdmin = new javax.swing.JButton();
        jSeparator11 = new javax.swing.JToolBar.Separator();
        btnGeoserverAdminConsole = new javax.swing.JButton();
        btnGeoserverAdmin = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        btnGeoserverCopy = new javax.swing.JButton();
        jScrollPane5 = new javax.swing.JScrollPane();
        txtGeoserverLog = new javax.swing.JTextArea();
        tabDatabase = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtDatabaseLog = new javax.swing.JTextArea();
        jToolBar4 = new javax.swing.JToolBar();
        btnStartDB = new javax.swing.JButton();
        btnStopDB = new javax.swing.JButton();
        jSeparator5 = new javax.swing.JToolBar.Separator();
        btnStartPgAdmin = new javax.swing.JButton();
        jSeparator8 = new javax.swing.JToolBar.Separator();
        btnDatabaseCopy = new javax.swing.JButton();
        jToolBar1 = new javax.swing.JToolBar();
        btnStartServers = new javax.swing.JButton();
        btnStopServers = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        btnWorkingDir = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtOut = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/sola/clients/portable/Bundle"); // NOI18N
        setTitle(bundle.getString("ControlCentre.title")); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        txtDesktopLog.setEditable(false);
        txtDesktopLog.setColumns(20);
        txtDesktopLog.setRows(5);
        jScrollPane4.setViewportView(txtDesktopLog);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 713, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 292, Short.MAX_VALUE)
                .addContainerGap())
        );

        jToolBar2.setFloatable(false);
        jToolBar2.setRollover(true);

        btnStartDesktopB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/control.png"))); // NOI18N
        btnStartDesktopB.setText(bundle.getString("ControlCentre.btnStartDesktopB.text")); // NOI18N
        btnStartDesktopB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartDesktopBActionPerformed(evt);
            }
        });
        jToolBar2.add(btnStartDesktopB);

        btnDesktopAdmin.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/wrench-screwdriver.png"))); // NOI18N
        btnDesktopAdmin.setText(bundle.getString("ControlCentre.btnDesktopAdmin.text")); // NOI18N
        btnDesktopAdmin.setFocusable(false);
        btnDesktopAdmin.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnDesktopAdmin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDesktopAdminActionPerformed(evt);
            }
        });
        jToolBar2.add(btnDesktopAdmin);
        jToolBar2.add(jSeparator9);

        btnDesktopCopy.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/scripts.png"))); // NOI18N
        btnDesktopCopy.setText(bundle.getString("ControlCentre.btnDesktopCopy.text")); // NOI18N
        btnDesktopCopy.setFocusable(false);
        btnDesktopCopy.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnDesktopCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDesktopCopyActionPerformed(evt);
            }
        });
        jToolBar2.add(btnDesktopCopy);

        javax.swing.GroupLayout tabDesktopLayout = new javax.swing.GroupLayout(tabDesktop);
        tabDesktop.setLayout(tabDesktopLayout);
        tabDesktopLayout.setHorizontalGroup(
            tabDesktopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, tabDesktopLayout.createSequentialGroup()
                .addComponent(jToolBar2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        tabDesktopLayout.setVerticalGroup(
            tabDesktopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tabDesktopLayout.createSequentialGroup()
                .addComponent(jToolBar2, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tabMain.addTab(bundle.getString("ControlCentre.tabDesktop.TabConstraints.tabTitle"), tabDesktop); // NOI18N

        txtGlassfishLog.setEditable(false);
        txtGlassfishLog.setColumns(20);
        txtGlassfishLog.setRows(5);
        jScrollPane3.setViewportView(txtGlassfishLog);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 292, Short.MAX_VALUE)
                .addContainerGap())
        );

        jToolBar3.setFloatable(false);
        jToolBar3.setRollover(true);

        btnStartGlassfish.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/control-power.png"))); // NOI18N
        btnStartGlassfish.setText(bundle.getString("ControlCentre.btnStartGlassfish.text")); // NOI18N
        btnStartGlassfish.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartGlassfishActionPerformed(evt);
            }
        });
        jToolBar3.add(btnStartGlassfish);

        btnStopGlassfish.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/control-stop-square.png"))); // NOI18N
        btnStopGlassfish.setText(bundle.getString("ControlCentre.btnStopGlassfish.text")); // NOI18N
        btnStopGlassfish.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopGlassfishActionPerformed(evt);
            }
        });
        jToolBar3.add(btnStopGlassfish);
        jToolBar3.add(jSeparator2);

        btnGlassfishAdminConsole.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/wrench.png"))); // NOI18N
        btnGlassfishAdminConsole.setText(bundle.getString("ControlCentre.btnGlassfishAdminConsole.text")); // NOI18N
        btnGlassfishAdminConsole.setFocusable(false);
        btnGlassfishAdminConsole.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnGlassfishAdminConsole.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGlassfishAdminConsoleActionPerformed(evt);
            }
        });
        jToolBar3.add(btnGlassfishAdminConsole);
        jToolBar3.add(jSeparator6);

        btnGlassfishCopy.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/scripts.png"))); // NOI18N
        btnGlassfishCopy.setText(bundle.getString("ControlCentre.btnGlassfishCopy.text")); // NOI18N
        btnGlassfishCopy.setFocusable(false);
        btnGlassfishCopy.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnGlassfishCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGlassfishCopyActionPerformed(evt);
            }
        });
        jToolBar3.add(btnGlassfishCopy);

        javax.swing.GroupLayout tabGlassfishLayout = new javax.swing.GroupLayout(tabGlassfish);
        tabGlassfish.setLayout(tabGlassfishLayout);
        tabGlassfishLayout.setHorizontalGroup(
            tabGlassfishLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jToolBar3, javax.swing.GroupLayout.DEFAULT_SIZE, 733, Short.MAX_VALUE)
        );
        tabGlassfishLayout.setVerticalGroup(
            tabGlassfishLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tabGlassfishLayout.createSequentialGroup()
                .addComponent(jToolBar3, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tabMain.addTab(bundle.getString("ControlCentre.tabGlassfish.TabConstraints.tabTitle"), new javax.swing.ImageIcon(getClass().getResource("/images/status-busy.png")), tabGlassfish); // NOI18N

        jToolBar5.setFloatable(false);
        jToolBar5.setRollover(true);

        btnStartGeoserver.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/control-power.png"))); // NOI18N
        btnStartGeoserver.setText(bundle.getString("ControlCentre.btnStartGeoserver.text")); // NOI18N
        btnStartGeoserver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartGeoserverActionPerformed(evt);
            }
        });
        jToolBar5.add(btnStartGeoserver);

        btnStopGeoserver.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/control-stop-square.png"))); // NOI18N
        btnStopGeoserver.setText(bundle.getString("ControlCentre.btnStopGeoserver.text")); // NOI18N
        btnStopGeoserver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopGeoserverActionPerformed(evt);
            }
        });
        jToolBar5.add(btnStopGeoserver);
        jToolBar5.add(jSeparator10);

        btnCSWebsite.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/control.png"))); // NOI18N
        btnCSWebsite.setText(bundle.getString("ControlCentre.btnCSWebsite.text")); // NOI18N
        btnCSWebsite.setFocusable(false);
        btnCSWebsite.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnCSWebsite.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCSWebsiteActionPerformed(evt);
            }
        });
        jToolBar5.add(btnCSWebsite);

        btnCSAdmin.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/wrench-screwdriver.png"))); // NOI18N
        btnCSAdmin.setText(bundle.getString("ControlCentre.btnCSAdmin.text")); // NOI18N
        btnCSAdmin.setFocusable(false);
        btnCSAdmin.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnCSAdmin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCSAdminActionPerformed(evt);
            }
        });
        jToolBar5.add(btnCSAdmin);
        jToolBar5.add(jSeparator11);

        btnGeoserverAdminConsole.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/wrench.png"))); // NOI18N
        btnGeoserverAdminConsole.setText(bundle.getString("ControlCentre.btnGeoserverAdminConsole.text")); // NOI18N
        btnGeoserverAdminConsole.setFocusable(false);
        btnGeoserverAdminConsole.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnGeoserverAdminConsole.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGeoserverAdminConsoleActionPerformed(evt);
            }
        });
        jToolBar5.add(btnGeoserverAdminConsole);

        btnGeoserverAdmin.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/globe-green.png"))); // NOI18N
        btnGeoserverAdmin.setText(bundle.getString("ControlCentre.btnGeoserverAdmin.text")); // NOI18N
        btnGeoserverAdmin.setFocusable(false);
        btnGeoserverAdmin.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnGeoserverAdmin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGeoserverAdminActionPerformed(evt);
            }
        });
        jToolBar5.add(btnGeoserverAdmin);
        jToolBar5.add(jSeparator3);

        btnGeoserverCopy.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/scripts.png"))); // NOI18N
        btnGeoserverCopy.setText(bundle.getString("ControlCentre.btnGeoserverCopy.text")); // NOI18N
        btnGeoserverCopy.setFocusable(false);
        btnGeoserverCopy.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnGeoserverCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGeoserverCopyActionPerformed(evt);
            }
        });
        jToolBar5.add(btnGeoserverCopy);

        txtGeoserverLog.setColumns(20);
        txtGeoserverLog.setRows(5);
        jScrollPane5.setViewportView(txtGeoserverLog);

        javax.swing.GroupLayout tabGeoserverLayout = new javax.swing.GroupLayout(tabGeoserver);
        tabGeoserver.setLayout(tabGeoserverLayout);
        tabGeoserverLayout.setHorizontalGroup(
            tabGeoserverLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jToolBar5, javax.swing.GroupLayout.DEFAULT_SIZE, 733, Short.MAX_VALUE)
            .addGroup(tabGeoserverLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane5)
                .addContainerGap())
        );
        tabGeoserverLayout.setVerticalGroup(
            tabGeoserverLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tabGeoserverLayout.createSequentialGroup()
                .addComponent(jToolBar5, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 292, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabMain.addTab(bundle.getString("ControlCentre.tabGeoserver.TabConstraints.tabTitle"), new javax.swing.ImageIcon(getClass().getResource("/images/status-busy.png")), tabGeoserver); // NOI18N

        txtDatabaseLog.setEditable(false);
        txtDatabaseLog.setColumns(20);
        txtDatabaseLog.setRows(5);
        jScrollPane2.setViewportView(txtDatabaseLog);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 292, Short.MAX_VALUE)
                .addContainerGap())
        );

        jToolBar4.setFloatable(false);
        jToolBar4.setRollover(true);

        btnStartDB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/control-power.png"))); // NOI18N
        btnStartDB.setText(bundle.getString("ControlCentre.btnStartDB.text")); // NOI18N
        btnStartDB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartDBActionPerformed(evt);
            }
        });
        jToolBar4.add(btnStartDB);

        btnStopDB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/control-stop-square.png"))); // NOI18N
        btnStopDB.setText(bundle.getString("ControlCentre.btnStopDB.text")); // NOI18N
        btnStopDB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopDBActionPerformed(evt);
            }
        });
        jToolBar4.add(btnStopDB);
        jToolBar4.add(jSeparator5);

        btnStartPgAdmin.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/postgresql.png"))); // NOI18N
        btnStartPgAdmin.setText(bundle.getString("ControlCentre.btnStartPgAdmin.text")); // NOI18N
        btnStartPgAdmin.setFocusable(false);
        btnStartPgAdmin.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnStartPgAdmin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartPgAdminActionPerformed(evt);
            }
        });
        jToolBar4.add(btnStartPgAdmin);
        jToolBar4.add(jSeparator8);

        btnDatabaseCopy.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/scripts.png"))); // NOI18N
        btnDatabaseCopy.setText(bundle.getString("ControlCentre.btnDatabaseCopy.text")); // NOI18N
        btnDatabaseCopy.setFocusable(false);
        btnDatabaseCopy.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnDatabaseCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDatabaseCopyActionPerformed(evt);
            }
        });
        jToolBar4.add(btnDatabaseCopy);

        javax.swing.GroupLayout tabDatabaseLayout = new javax.swing.GroupLayout(tabDatabase);
        tabDatabase.setLayout(tabDatabaseLayout);
        tabDatabaseLayout.setHorizontalGroup(
            tabDatabaseLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jToolBar4, javax.swing.GroupLayout.DEFAULT_SIZE, 733, Short.MAX_VALUE)
        );
        tabDatabaseLayout.setVerticalGroup(
            tabDatabaseLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tabDatabaseLayout.createSequentialGroup()
                .addComponent(jToolBar4, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        tabMain.addTab(bundle.getString("ControlCentre.tabDatabase.TabConstraints.tabTitle"), new javax.swing.ImageIcon(getClass().getResource("/images/status-busy.png")), tabDatabase); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabMain)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabMain, javax.swing.GroupLayout.Alignment.TRAILING)
        );

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        btnStartServers.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/control-power.png"))); // NOI18N
        btnStartServers.setText(bundle.getString("ControlCentre.btnStartServers.text")); // NOI18N
        btnStartServers.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartServersActionPerformed(evt);
            }
        });
        jToolBar1.add(btnStartServers);

        btnStopServers.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/control-stop-square.png"))); // NOI18N
        btnStopServers.setText(bundle.getString("ControlCentre.btnStopServers.text")); // NOI18N
        btnStopServers.setFocusable(false);
        btnStopServers.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnStopServers.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopServersActionPerformed(evt);
            }
        });
        jToolBar1.add(btnStopServers);
        jToolBar1.add(jSeparator1);

        btnWorkingDir.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/information.png"))); // NOI18N
        btnWorkingDir.setText(bundle.getString("ControlCentre.btnWorkingDir.text")); // NOI18N
        btnWorkingDir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnWorkingDirActionPerformed(evt);
            }
        });
        jToolBar1.add(btnWorkingDir);

        txtOut.setEditable(false);
        txtOut.setColumns(20);
        txtOut.setLineWrap(true);
        txtOut.setRows(5);
        txtOut.setWrapStyleWord(true);
        jScrollPane1.setViewportView(txtOut);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 119, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnStartDBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartDBActionPerformed
        startDatabase();
    }//GEN-LAST:event_btnStartDBActionPerformed

    private void btnStopDBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopDBActionPerformed
        stopDatabase();
    }//GEN-LAST:event_btnStopDBActionPerformed

    private void btnWorkingDirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnWorkingDirActionPerformed
        writeUserMessage("The current working directory is " + System.getProperty("user.dir")
                + " To override default settings, place config.properties in this directory.");
    }//GEN-LAST:event_btnWorkingDirActionPerformed

    private void btnStartGlassfishActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartGlassfishActionPerformed
        startGlassfish();
    }//GEN-LAST:event_btnStartGlassfishActionPerformed

    private void btnStopGlassfishActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopGlassfishActionPerformed
        stopGlassfish();
    }//GEN-LAST:event_btnStopGlassfishActionPerformed

    private void btnStartDesktopBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartDesktopBActionPerformed
        startDesktop();
    }//GEN-LAST:event_btnStartDesktopBActionPerformed

    private void btnStartServersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartServersActionPerformed
        startServers();
    }//GEN-LAST:event_btnStartServersActionPerformed

    private void btnStopServersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopServersActionPerformed
        stopServers();
    }//GEN-LAST:event_btnStopServersActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if (isGlassfishRunning() || isDatabaseRunning()) {
            close = true;
            stopServers();
        } else {
            close();
        }
    }//GEN-LAST:event_formWindowClosing

    private void btnStartPgAdminActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartPgAdminActionPerformed
        startPgAdmin();
    }//GEN-LAST:event_btnStartPgAdminActionPerformed

    private void btnGlassfishCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGlassfishCopyActionPerformed
        copyLog(txtGlassfishLog, "Glassfish");
    }//GEN-LAST:event_btnGlassfishCopyActionPerformed

    private void btnDatabaseCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDatabaseCopyActionPerformed
        copyLog(txtDatabaseLog, "Database");
    }//GEN-LAST:event_btnDatabaseCopyActionPerformed

    private void btnDesktopCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDesktopCopyActionPerformed
        copyLog(txtDesktopLog, "Desktop");
    }//GEN-LAST:event_btnDesktopCopyActionPerformed

    private void btnStartGeoserverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartGeoserverActionPerformed
        startGeoserver();
    }//GEN-LAST:event_btnStartGeoserverActionPerformed

    private void btnStopGeoserverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopGeoserverActionPerformed
        stopGeoserver();
    }//GEN-LAST:event_btnStopGeoserverActionPerformed

    private void btnGeoserverCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGeoserverCopyActionPerformed
        copyLog(txtGeoserverLog, "Community Server");
    }//GEN-LAST:event_btnGeoserverCopyActionPerformed

    private void btnGeoserverAdminConsoleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGeoserverAdminConsoleActionPerformed
        openWebPage("Opening Community Server Glassfish Admin Console at %s...", getConfig(CS_GLASSFISH_ADMIN_URL, "http://localhost:4849"));
    }//GEN-LAST:event_btnGeoserverAdminConsoleActionPerformed

    private void btnGlassfishAdminConsoleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGlassfishAdminConsoleActionPerformed
        openWebPage("Opening Registry Glassfish Admin Console at %s...", getConfig(GLASSFISH_ADMIN_URL, "http://localhost:4848"));
    }//GEN-LAST:event_btnGlassfishAdminConsoleActionPerformed

    private void btnDesktopAdminActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDesktopAdminActionPerformed
        openWebPage("Opening Registry Admin at %s...", getConfig(DESKTOP_WEB_ADMIN_URL, "https://localhost:8181/sola/admin"));
    }//GEN-LAST:event_btnDesktopAdminActionPerformed

    private void btnGeoserverAdminActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGeoserverAdminActionPerformed
        openWebPage("Opening Geoserver Admin at %s.    Username=admin Password=geoserver", getConfig(GEOSERVER_ADMIN_URL, "http://localhost:8085/geoserver"));
    }//GEN-LAST:event_btnGeoserverAdminActionPerformed

    private void btnCSAdminActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCSAdminActionPerformed
        openWebPage("Opening Community Server Admin at %s...", getConfig(CS_WEB_ADMIN_URL, "http://localhost:8085/sola/admin"));
    }//GEN-LAST:event_btnCSAdminActionPerformed

    private void btnCSWebsiteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCSWebsiteActionPerformed
        openWebPage("Opening Community Server %s...", getConfig(CS_URL, "http://localhost:8085"));
    }//GEN-LAST:event_btnCSWebsiteActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCSAdmin;
    private javax.swing.JButton btnCSWebsite;
    private javax.swing.JButton btnDatabaseCopy;
    private javax.swing.JButton btnDesktopAdmin;
    private javax.swing.JButton btnDesktopCopy;
    private javax.swing.JButton btnGeoserverAdmin;
    private javax.swing.JButton btnGeoserverAdminConsole;
    private javax.swing.JButton btnGeoserverCopy;
    private javax.swing.JButton btnGlassfishAdminConsole;
    private javax.swing.JButton btnGlassfishCopy;
    private javax.swing.JButton btnStartDB;
    private javax.swing.JButton btnStartDesktopB;
    private javax.swing.JButton btnStartGeoserver;
    private javax.swing.JButton btnStartGlassfish;
    private javax.swing.JButton btnStartPgAdmin;
    private javax.swing.JButton btnStartServers;
    private javax.swing.JButton btnStopDB;
    private javax.swing.JButton btnStopGeoserver;
    private javax.swing.JButton btnStopGlassfish;
    private javax.swing.JButton btnStopServers;
    private javax.swing.JButton btnWorkingDir;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator10;
    private javax.swing.JToolBar.Separator jSeparator11;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar.Separator jSeparator5;
    private javax.swing.JToolBar.Separator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JToolBar.Separator jSeparator8;
    private javax.swing.JToolBar.Separator jSeparator9;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JToolBar jToolBar3;
    private javax.swing.JToolBar jToolBar4;
    private javax.swing.JToolBar jToolBar5;
    private javax.swing.JPanel tabDatabase;
    private javax.swing.JPanel tabDesktop;
    private javax.swing.JPanel tabGeoserver;
    private javax.swing.JPanel tabGlassfish;
    private javax.swing.JTabbedPane tabMain;
    private javax.swing.JTextArea txtDatabaseLog;
    private javax.swing.JTextArea txtDesktopLog;
    private javax.swing.JTextArea txtGeoserverLog;
    private javax.swing.JTextArea txtGlassfishLog;
    private javax.swing.JTextArea txtOut;
    // End of variables declaration//GEN-END:variables
}
