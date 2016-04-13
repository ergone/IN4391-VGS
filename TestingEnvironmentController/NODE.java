package TEC;

import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.vm.VirtualMachine;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import org.xml.sax.InputSource;

/**
 *
 * @author Piotr Tekieli <p.s.tekieli@student.tudelft.nl>
 * Created on : 21.03.2016
 * Last revised : 11.04.2016
 */
public class NODE {   
    NODE(Client oc, String template, String MACHINE_ROLE, Logging logger) throws Exception {
        this.oc = oc;
        this.template = template;        
        this.MACHINE_ROLE = MACHINE_ROLE;        
        CreateVM(logger);
    }
    
    NODE(Client oc, int VMID) {
        this.oc = oc;
        VM_ID = VMID; 
        VM = new VirtualMachine(VM_ID, oc);
    }   
    
    private OneResponse rc;
    private Client oc;
    private String template;    
    
    private String MACHINE_ROLE;
    private VirtualMachine VM;
    protected int VM_ID;
    protected String IP;  
    protected String NAME; /* Naming convention : VMID_MACHINE_ROLE_IP */      
    
    private void CreateVM(Logging logger) throws Exception {        
        rc = VirtualMachine.allocate(oc, template);        
        if(rc.isError()) {
            System.out.println("Initialization of Virtual Machine failed!");
            throw new Exception(rc.getErrorMessage());            
        }
        
        /* The response message is the new NODE's ID */
        VM_ID = Integer.parseInt(rc.getMessage());
        System.out.println("OK! ID: " + VM_ID); 
        
        /* We can create a representation for the new NODE, using the returned NODE-ID */
        VM = new VirtualMachine(VM_ID, oc);         
        GetSetIP();        
        NAME = VM_ID + "_" + MACHINE_ROLE + "_" + IP; 
        logger.SubmitEntry("Created VM: " + VM_ID + ", NAMED: " + NAME);
    }
    
    public void RemoveVM(Logging logger) throws Exception {        
        rc = VM.finalizeVM();
        if(rc.isError()) {
            System.out.println("Deletion of Virtual Machine failed!");
            throw new Exception(rc.getErrorMessage());
        }
        else {
            System.out.println("Deleted: " + VM_ID);
        }
        logger.SubmitEntry("Deleted VM: " + VM_ID);        
    }
    
    private void GetSetIP() throws Exception {        
        rc = VM.info();
        if(rc.isError()) {
            System.out.println("Problem with obtaining VM's information!");
            throw new Exception(rc.getErrorMessage());
        }        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();        
        Document document = builder.parse(new InputSource(new StringReader(rc.getMessage())));
        document.getDocumentElement().normalize();
        Element root = document.getDocumentElement();
        IP = root.getElementsByTagName("IP").item(0).getTextContent();            
    }
}
