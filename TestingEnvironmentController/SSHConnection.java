/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TEC;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;


/**
 * @author Arnost Valicek, http://stackoverflow.com/a/4932378
 * @editor Piotr Tekieli <p.s.tekieli@student.tudelft.nl>
 * Created on : 02.04.2016
 * Last revised : 02.04.2016
 */
public class SSHConnection {
    private JSch SSHChannel;
    private String SSH_IP;
    private String SSH_Username;    
    private int SSH_ConnectionPort;
    private int SSH_TimeOut;
    private Logging logger;
    private Session SSH_Session;
    
    SSHConnection(String SSH_Username, String SSH_IP, int SSH_ConnectionPort, int SSH_TimeOut, Logging logger) {
        SSHChannel = new JSch();
        this.logger = logger;
        this.SSH_Username = SSH_Username;
        //this.SSH_Password = SSH_Password;
        this.SSH_IP = SSH_IP;
        this.SSH_ConnectionPort = SSH_ConnectionPort;
        this.SSH_TimeOut = SSH_TimeOut;                
        try {
            SSHChannel.addIdentity("/home/cld1595/.ssh/id_dsa");
            SSHChannel.setKnownHosts("/home/cld1595/.ssh/known_hosts");
            
        } catch(JSchException jschX) {
            logger.SubmitEntry(jschX.getMessage());
            System.out.println("Error while setting SSH Channel: " + jschX.getMessage());
        }
        
        EstablishConnection();
    }
    
    private void EstablishConnection() {
        try {
            SSH_Session = SSHChannel.getSession(SSH_Username, SSH_IP, SSH_ConnectionPort);
            java.util.Properties config = new java.util.Properties(); 
            config.put("StrictHostKeyChecking", "no");
            SSH_Session.setConfig(config);
            SSH_Session.connect(SSH_TimeOut);
        }
        catch(JSchException jschX) {
            logger.SubmitEntry(jschX.getMessage());
            System.out.println("Error while setting SSH Connection: " + jschX.getMessage());
        }
    }
   
    public void SubmitFiles(List<File> PROPERTIES) {
        try {
            Channel channel = SSH_Session.openChannel("sftp"); 
            channel.connect();
            ChannelSftp channelSftp = (ChannelSftp)channel;
            channelSftp.cd(".");
            for (File f : PROPERTIES) {
                try {
                    channelSftp.put(new FileInputStream(f), f.getName());
                } catch (FileNotFoundException ex) {
                    System.out.println("Error while reading from config file: " + ex);
                }
            }
            channelSftp.disconnect();
            channel.disconnect();
        } catch (SftpException | JSchException ex) {
            System.out.println("Error while establishing SFTP connection: " + ex);
        }
    }
    
    public void SubmitFile(File PROPERTIES) {
        try {
            Channel channel = SSH_Session.openChannel("sftp"); 
            channel.connect();
            ChannelSftp channelSftp = (ChannelSftp)channel;
            channelSftp.cd(".");            
            try {
                channelSftp.put(new FileInputStream(PROPERTIES), PROPERTIES.getName());
            } catch (FileNotFoundException ex) {
                System.out.println("Error while writing to remote file: " + ex);
            }        
            channelSftp.disconnect();
            channel.disconnect();            
        } catch (SftpException | JSchException ex) {
            System.out.println("Error while establishing SFTP connection: " + ex);
        }
    }
    
    public void CloseConnection() {
        SSH_Session.disconnect();
    }   
}
