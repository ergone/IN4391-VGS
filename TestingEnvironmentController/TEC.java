package TEC;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import static java.lang.Thread.sleep;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.opennebula.client.Client;
import org.opennebula.client.OneException;

/**
 *
 * @author Piotr Tekieli <p.s.tekieli@student.tudelft.nl>
 * Created on : 21.03.2016
 * Last revised : 11.04.2016
 */
public class TEC {

    private static final String CREDENTIALS = "username:password"; //FILL IN
    private static final String LOCATIONUTF16 = "/home/cld1595/OpenNebula/centos-smallnet-qcow2.one";
    private static String LOCATION;
    private static Client oneClient;
    private static final Logging LOGGER = new Logging();
    private static final List<File> PROPERTIES = new ArrayList<>();
    private static final List<File> JARS = new ArrayList<>();
    private static final List<File> SCRIPTS = new ArrayList<>();
    private static String RMIGM;

    //{"GRID.MANAGER", "GRID.SCHEDULER", "RESOURCE.MANAGER", "PROCESSOR"};
    private static NODE GRID_MANAGER;
    private static NODE CLIENT;

    private static final List<NODE> GRID_SCHEDULERS = new ArrayList<>();
    private static final List<NODE> RESOURCE_MANAGERS = new ArrayList<>();
    private static final List<NODE> PROCESSORS = new ArrayList<>();

    private static boolean created = false;
    private static boolean env_controller = true;
    private static int VMCOUNTER = 0;

    public static void main(String[] args) throws Exception {

        /* Create new XML-RPC client (connection with core) with authentication data
           copied from CREDENTIALS and endpoint set to $ONE_XMLRPC  */
        try {
            oneClient = new Client(CREDENTIALS, null);
        }
        catch (OneException client_ex) {
            System.out.println("Couldn't create Client instance, error :" + client_ex);
            System.exit(100);
        }

        FillFiles();
        SetLocationAsUTF8(); //Convert location string from UTF16 to UTF8
        ActionSelector(); //Move to selection menu and perform desired action
        LOGGER.CloseLogFile();
    }

    private static void FillFiles() {
        PROPERTIES.add(new File("gs.properties"));
        PROPERTIES.add(new File("rm.properties"));
        PROPERTIES.add(new File("processors.properties"));
        PROPERTIES.add(new File("das.properties"));

        JARS.add(new File("gm.jar"));
        JARS.add(new File("main.jar"));
        JARS.add(new File("client.jar"));

        SCRIPTS.add(new File("security.policy")); //POLICY
        SCRIPTS.add(new File("iptables.sh")); //DISABLE IPTABLES & INSTALL SCREEN
        SCRIPTS.add(new File("deploygsgm.sh")); //DEPLOY GM AND GS-ES
        SCRIPTS.add(new File("deployproc.sh")); //DEPLOY PROCESSORS
        SCRIPTS.add(new File("deployrm.sh")); //DEPLOY RMS
        SCRIPTS.add(new File("main.sh"));  //LOCALSCRIPT_MAIN
        SCRIPTS.add(new File("gm.sh")); //LOCALSCRIPT_GM
        SCRIPTS.add(new File("dscreen.sh")); //DISABLE SCREENS

        for (File f : SCRIPTS) {
            f.setExecutable(true);
        }
    }

    private static void ActionSelector() throws Exception {
        do {
            System.out.println("What action would you like to perform?");
            System.out.println("1 - Create Environment");
            System.out.println("2 - Deploy JARs");
            System.out.println("3 - Initialize Environment");
            System.out.println("4 - Delete active screens");
            System.out.println("5 - Remove Environment");
            System.out.println("6 - Exit");
            switch(GetAction()) {
                case 1:
                    System.out.println("Creating desired environment...");
                    CreateNodes();
                    System.out.println("All Done! Creating config file...");
                    CreateProperties();
                    CreateScripts();
                    created = true;
                    System.out.println("All Done!");
                    break;
                case 2:
                    if (created == true) {
                        System.out.println("Deploying configuration files and java runnables...");
                        DeployFiles();
                        System.out.println("All Done!");
                    }
                    else {
                        System.out.println("Error occured! You can re-try it again");
                    }
                    break;
                 case 3:
                    if (created == true) {
                        System.out.println("Initializing environment - Phase 1/4");
                        Process p = Runtime.getRuntime().exec("./"+SCRIPTS.get(1));
                        p.waitFor();
                        System.out.println("Initializing environment - Phase 2/4");
                        p = Runtime.getRuntime().exec("./"+SCRIPTS.get(2));
                        p.waitFor();
                        System.out.println("Initializing environment - Phase 3/4");
                        p = Runtime.getRuntime().exec("./"+SCRIPTS.get(3));
                        p.waitFor();
                        System.out.println("Initializing environment - Phase 4/4");
                        sleep(10000);
                        p = Runtime.getRuntime().exec("./"+SCRIPTS.get(4));
                        p.waitFor();
                        System.out.println("All Done!");
                    }
                    else {
                        System.out.println("Error occured! Remove enviornment and try again");
                    }
                    break;
                case 4:
                    System.out.println("Clearing active screens...");
                    Process p = Runtime.getRuntime().exec("./"+SCRIPTS.get(7));
                    p.waitFor();
                    System.out.println("All Done!");
                    break;
                case 5:
                    System.out.println("Clearing testing environment...");
                    ClearEnvironment();
                    System.out.println("All Done!");
                    break;
                case 6:
                    System.out.println("Exiting...");
                    env_controller = false;
                    break;
                default:
                    System.out.println("Wrong option! Select another one!");
                    break;
            }
        } while( env_controller == true );
    }

    private static int GetAction() {
        Scanner reader = new Scanner(System.in);
        int n = reader.nextInt();
        return n;
    }

    private static void SetLocationAsUTF8() {
        try (FileInputStream fileInputStream = new FileInputStream(LOCATIONUTF16)) {
            byte[] buffer = new byte[fileInputStream.available()];
            int length = fileInputStream.read(buffer);
            LOCATION = new String(buffer, 0, length, Charset.forName("UTF-8"));
        }
        catch (IOException exception_conv) {
            System.out.println("An error has occured while trying to convert string to UTF-8 " + exception_conv);
        }
    }

    private static void AddNode(int COUNTER, Client ONE_CLIENT, String LOCATION, Logging LOGGER, String ROLE, BufferedWriter CONFIG) {
        for (int i = 0; i < COUNTER; i++) {
            try {
                if (VMCOUNTER % 100 == 0 && i != 0) {
                    sleep(120000); //Avoid invoking too many VMs at once
                }
                NODE vm = new NODE(ONE_CLIENT, LOCATION, ROLE, LOGGER);
                CONFIG.write(vm.VM_ID + " = " + vm.IP);
                CONFIG.newLine();
                switch (ROLE) {
                    case "GRID.MANAGER" :
                        GRID_MANAGER = vm;
                        break;
                    case "CLIENT" :
                        CLIENT = vm;
                        break;
                    case "GRID.SCHEDULER" :
                        GRID_SCHEDULERS.add(vm);
                        break;
                    case "RESOURCE.MANAGER" :
                        RESOURCE_MANAGERS.add(vm);
                        break;
                    case "PROCESSOR" :
                        PROCESSORS.add(vm);
                        break;
                    default :
                        break;
                }
                VMCOUNTER++;
            }
            catch (Exception e) {
                System.out.println("Error creating virtual machine. Exception : " + e);
            }
        }
    }

    private static void CreateNodes() throws Exception {
        try (BufferedWriter das_properties = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(PROPERTIES.get(3)), "UTF-8"))) {
            AddNode(1, oneClient, LOCATION, LOGGER, "GRID.MANAGER", das_properties);
            RMIGM = "rmi://" + GRID_MANAGER.IP + ":1099/1099";
            System.out.println("How many GS nodes do you need?");
            AddNode(GetAction(), oneClient,LOCATION, LOGGER, "GRID.SCHEDULER", das_properties);
            System.out.println("How many RM nodes do you need?");
            AddNode(GetAction(), oneClient,LOCATION, LOGGER, "RESOURCE.MANAGER", das_properties);
            System.out.println("How many PROC nodes do you need?");
            AddNode(GetAction(), oneClient,LOCATION, LOGGER, "PROCESSOR", das_properties);
            System.out.println("Creating client machine... ");
            AddNode(1, oneClient,LOCATION, LOGGER, "CLIENT", das_properties);
            System.out.println("Done!");
            das_properties.close();
        } catch (FileNotFoundException | UnsupportedEncodingException bw_ex) {
            System.out.println("Error occured while writing to config file: " + bw_ex);
            System.exit(0); //Set Error ID
        }
    }

    private static void CreateScripts() {
        try {
            BufferedWriter iptables_script = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(SCRIPTS.get(1)), "UTF-8"));
            BufferedWriter deploy_gsgm = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(SCRIPTS.get(2)), "UTF-8"));
            BufferedWriter deploy_proc = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(SCRIPTS.get(3)), "UTF-8"));
            BufferedWriter deploy_rm = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(SCRIPTS.get(4)), "UTF-8"));
            BufferedWriter main_script = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(SCRIPTS.get(5)), "UTF-8"));
            BufferedWriter gm_script = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(SCRIPTS.get(6)), "UTF-8"));
            BufferedWriter screen_script = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(SCRIPTS.get(7)), "UTF-8"));

            /* Fill script files with #!/bin/bash header */
            iptables_script.write("#!/bin/bash"); iptables_script.newLine(); deploy_gsgm.write("#!/bin/bash"); deploy_gsgm.newLine();
            deploy_proc.write("#!/bin/bash"); deploy_proc.newLine(); deploy_rm.write("#!/bin/bash"); deploy_rm.newLine();
            gm_script.write("#!/bin/bash"); gm_script.newLine();  main_script.write("#!/bin/bash");  main_script.newLine();
            screen_script.write("#!/bin/bash"); screen_script.newLine();

            iptables_script.write("ssh root@" + GRID_MANAGER.IP + " 'yum -y install screen'"); iptables_script.newLine();
            iptables_script.write("ssh root@" + GRID_MANAGER.IP + " '/etc/init.d/iptables stop'"); iptables_script.newLine();
            screen_script.write("ssh root@" + GRID_MANAGER.IP + " 'killall screen'"); screen_script.newLine();
            deploy_gsgm.write("ssh root@" + GRID_MANAGER.IP + " 'chmod +x " + SCRIPTS.get(6) + "'"); deploy_gsgm.newLine();
            deploy_gsgm.write("ssh root@" + GRID_MANAGER.IP + " 'screen -dmS gmrmi ./" + SCRIPTS.get(6) + "'"); deploy_gsgm.newLine();
            gm_script.write("java -jar gm.jar " + RMIGM);  gm_script.newLine(); gm_script.close();

            screen_script.write("ssh root@" + CLIENT.IP + " 'killall screen'"); screen_script.newLine();
            iptables_script.write("ssh root@" + CLIENT.IP + " 'yum -y install screen'"); iptables_script.newLine();
            iptables_script.write("ssh root@" + CLIENT.IP + " '/etc/init.d/iptables stop'"); iptables_script.newLine();

            for (NODE x : GRID_SCHEDULERS) {
               iptables_script.write("ssh root@" + x.IP + " 'yum -y install screen'"); iptables_script.newLine();
               iptables_script.write("ssh root@" + x.IP + " '/etc/init.d/iptables stop'"); iptables_script.newLine();

               deploy_gsgm.write("ssh root@" + x.IP + " 'chmod +x " + SCRIPTS.get(5) + "'"); deploy_gsgm.newLine();
               deploy_gsgm.write("ssh root@" + x.IP + " 'screen -dmS gsrmi ./" + SCRIPTS.get(5) + "'"); deploy_gsgm.newLine();

               screen_script.write("ssh root@" + x.IP + " 'killall screen'"); screen_script.newLine();
            }
            for (NODE x : RESOURCE_MANAGERS) {
               iptables_script.write("ssh root@" + x.IP + " 'yum -y install screen'"); iptables_script.newLine();
               iptables_script.write("ssh root@" + x.IP + " '/etc/init.d/iptables stop'"); iptables_script.newLine();

               deploy_rm.write("ssh root@" + x.IP + " 'chmod +x " + SCRIPTS.get(5) + "'"); deploy_rm.newLine();
               deploy_rm.write("ssh root@" + x.IP + " 'screen -dmS rmrmi ./" + SCRIPTS.get(5) + "'"); deploy_rm.newLine();

               screen_script.write("ssh root@" + x.IP + " 'killall screen'"); screen_script.newLine();
            }
            for (NODE x : PROCESSORS) {
               iptables_script.write("ssh root@" + x.IP + " 'yum -y install screen'"); iptables_script.newLine();
               iptables_script.write("ssh root@" + x.IP + " '/etc/init.d/iptables stop'"); iptables_script.newLine();

               deploy_proc.write("ssh root@" + x.IP + " 'chmod +x " + SCRIPTS.get(5) + "'"); deploy_proc.newLine();
               deploy_proc.write("ssh root@" + x.IP + " 'screen -dmS procrmi ./" + SCRIPTS.get(5) + "'"); deploy_proc.newLine();

               screen_script.write("ssh root@" + x.IP + " 'killall screen'"); screen_script.newLine();
            }

            main_script.write("IP=$(ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}')");
            main_script.newLine();

            for (int i = 0; i < 10; i++) {
                main_script.write("java -jar main.jar rmi://$IP:" + (1099 + i) + "/" + (1099 + i) + " " + RMIGM);
                main_script.newLine();
            }

            iptables_script.close();
            deploy_gsgm.close();
            deploy_proc.close();
            deploy_rm.close();
            main_script.close();
            screen_script.close();
        } catch (IOException bw_ex) {
            System.out.println("Error occured while writing to config files: " + bw_ex);
            System.exit(0); //Set Error ID
        }
    }

    private static void CreateProperties() {
        try {
            BufferedWriter gs_properties = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(PROPERTIES.get(0)), "UTF-8"));
            BufferedWriter rm_properties = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(PROPERTIES.get(1)), "UTF-8"));
            BufferedWriter proc_properties = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(PROPERTIES.get(2)), "UTF-8"));

            for (NODE x : GRID_SCHEDULERS) {
               gs_properties.write(x.VM_ID + " = rmi://" + x.IP + ":1099/1099");
               gs_properties.newLine();
            }
            for (NODE x : RESOURCE_MANAGERS) {
               rm_properties.write(x.VM_ID + " = rmi://" + x.IP + ":1099/1099");
               rm_properties.newLine();
            }
            for (NODE x : PROCESSORS) {
               proc_properties.write(x.VM_ID + " = rmi://" + x.IP + ":1099/1099");
               proc_properties.newLine();
            }

            gs_properties.close();
            rm_properties.close();
            proc_properties.close();
        } catch (IOException bw_ex) {
            System.out.println("Error occured while writing to config file: " + bw_ex);
            System.exit(0); //Set Error ID
        }
    }

    private static void ClearEnvironment() throws IOException, Exception {
            try (FileReader fr = new FileReader("das.properties")){
                try (BufferedReader br = new BufferedReader(fr)) {
                    String line;
                    String delims = "[=]";
                    while ((line = br.readLine()) != null) {
                        line = line.replaceAll("\\s+","");
                        String[] tokens = line.split(delims);
                        NODE vm = new NODE(oneClient, Integer.parseInt(tokens[0]));
                        vm.RemoveVM(LOGGER);
                    }
                    br.close();
                } catch(IOException reader_ex) {
                    System.out.println("Error reading from das.properties file: " + reader_ex);
                    System.exit(0);
                }
            } catch (FileNotFoundException | UnsupportedEncodingException file_ex) {
                System.out.println("Error opening das.properties file: " + file_ex);
                System.exit(0);
            }
        for (File f : PROPERTIES)
            f.delete();
        for (File f : SCRIPTS)
            if (f.getName() != "security.policy")
            f.delete();
    }

    private static void DeployFiles() throws InterruptedException {
        SSHConnection ssh;
        ssh = new SSHConnection("root", GRID_MANAGER.IP, 22, 480, LOGGER);
        ssh.SubmitFile(JARS.get(0));
        ssh.SubmitFiles(PROPERTIES);
        ssh.SubmitFile(SCRIPTS.get(6));
        ssh.SubmitFile(SCRIPTS.get(0));
        ssh.CloseConnection();

        ssh = new SSHConnection("root", CLIENT.IP, 22, 480, LOGGER);
        ssh.SubmitFile(JARS.get(2));
        ssh.SubmitFiles(PROPERTIES);
        ssh.SubmitFile(SCRIPTS.get(0));
        ssh.CloseConnection();

        for (NODE x : GRID_SCHEDULERS) {
            ssh = new SSHConnection("root", x.IP, 22, 480, LOGGER);
            ssh.SubmitFile(JARS.get(1));
            ssh.SubmitFiles(PROPERTIES);
            ssh.SubmitFile(SCRIPTS.get(5));
            ssh.SubmitFile(SCRIPTS.get(0));
            ssh.CloseConnection();
        }
        for (NODE x : RESOURCE_MANAGERS) {
            ssh = new SSHConnection("root", x.IP, 22, 480, LOGGER);
            ssh.SubmitFile(JARS.get(1));
            ssh.SubmitFiles(PROPERTIES);
            ssh.SubmitFile(SCRIPTS.get(5));
            ssh.SubmitFile(SCRIPTS.get(0));
            ssh.CloseConnection();
        }
        for (NODE x : PROCESSORS) {
            ssh = new SSHConnection("root", x.IP, 22, 480, LOGGER);
            ssh.SubmitFile(JARS.get(1));
            ssh.SubmitFiles(PROPERTIES);
            ssh.SubmitFile(SCRIPTS.get(5));
            ssh.SubmitFile(SCRIPTS.get(0));
            ssh.CloseConnection();
        }
    }
}
