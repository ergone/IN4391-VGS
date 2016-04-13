/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TEC;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;

/**
 *
 * @author Piotr Tekieli <p.s.tekieli@student.tudelft.nl>
 * Created on : 01.04.2016
 * Last revised : 08.04.2016
 */
public class Logging {
    protected BufferedWriter logger;
    protected static final File logfile = new File("das.log"); 
    
    Logging() {
        try {
            logger = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logfile, true), "UTF-8"));            
        } catch (FileNotFoundException | UnsupportedEncodingException bw_ex) {
            System.out.println("Error occured while writing to config and/or log file: " + bw_ex);
            System.exit(0); //Set Error ID
        }
    }
    
    protected void SubmitEntry(String message) {
        try {            
            logger.write(new Date() + ", " + message);
            logger.newLine();
        } catch(IOException io_ex) {
            System.out.println("Error writing to log file: " + io_ex);
        }        
    }
    
    protected void CloseLogFile() {
        try {
            logger.close();
        } catch(IOException io_ex) {
            System.out.println("Error closing log file: " + io_ex);
        }
    }
}