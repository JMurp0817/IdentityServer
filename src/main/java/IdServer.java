import org.apache.commons.cli.*;

import java.io.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

/**
 *
 * @author Diklic, Stefan
 * @author Fernandez, Justin
 * @author Murphy, Joseph
 */
public class IdServer extends UnicastRemoteObject implements Queries {
    private static int defaultPort = 1099;
    private static Timer t;
    private static HashMap<String, User> dict;

    public IdServer(String s) throws RemoteException {
        super();
        dict = new HashMap<>();
    }

    @Override
    public String createLogin(String loginName, String realName, String password) throws RemoteException {
        System.out.println("Creating " + loginName + " in registry...");
        if(dict.containsKey(loginName)){
            throw new RemoteException("User already taken");
        }
        else {
            String ip = "";
            try {
                ip = getClientHost();
            } catch (ServerNotActiveException e) {
                e.printStackTrace();
            }

            User ud = new User(loginName, realName, password, ip);
            dict.put(loginName, ud);

            return ("New user created: "+ud.getLoginName()+"\nUUID: "+ud.getUUID());
        }
    }

    @Override
    public String lookupByLogin(String loginName) throws RemoteException{
        if(dict.containsKey(loginName)){
            User ud = dict.get(loginName);
            ud.updateLastRequestDate();

            return ud.toString();
        }
        else{
            throw new RemoteException("No user with login name: " + loginName);
        }
    }

    @Override
    public String lookupByUUID(String Uuid) throws RemoteException{
        String loginName = findLoginNameByUUID(Uuid);

        if(loginName != null){
            return lookupByLogin(loginName);
        }
        else{
            throw new RemoteException("No user found with UUID: " + Uuid);
        }
    }

    @Override
    public String modifyName(String oldLoginName, String newLoginName, String password) throws RemoteException {

        if(dict.containsKey(oldLoginName)) {
            User ud = dict.get(oldLoginName);
            if( (ud.hasPassword() && (ud.getPassword().equals(password)) ) || !ud.hasPassword()){
                ud.setLoginName(newLoginName);
                dict.remove(oldLoginName);
                dict.put(newLoginName, ud);
            }else{
                throw new RemoteException("Incorrect password");
            }
            return "New login name: "+newLoginName;
        }
        else {
            throw new RemoteException("Login name cannot be found");
        }

    }

    @Override
    public String deleteUser(String loginName, String password) throws RemoteException {

        if(dict.containsKey(loginName)) {
            User ud = dict.get(loginName);
            if((ud.hasPassword() && ud.getPassword().equals(password)) || !ud.hasPassword()) {
                dict.remove(loginName);
            } else {
                throw new RemoteException("Incorrect password.");
            }
            return ("User has been deleted: "+loginName);
        } else {
            throw new RemoteException("User does not exist");
        }
    }

    @Override
    public String getInfo(String type) throws RemoteException {
        if(type.equalsIgnoreCase("users")){
            return getUsers();
        } else if(type.equalsIgnoreCase("uuids")) {
            return getUUIDS();
        } else if(type.equalsIgnoreCase("all")) {
            return getAll();
        } else {
            return "Cannot get info on types other than [users|uuids|all]";
        }
    }

    /**
     * Helper function for getInfo to return information on all users
     *
     * @return list of all User objects as strings
     */
    private String getAll() {
        StringBuilder retVal = new StringBuilder();

        for(User usr: dict.values())
            retVal.append(usr.toString()).append("\n");

        return retVal.toString();
    }

    /**
     * Helper function for getInfo to return all UUID's in the registry
     *
     * @return list of all UUID's in the registry in string format
     */
    private String getUUIDS() {
        StringBuilder retVal = new StringBuilder();

        for(User usr: dict.values())
            retVal.append(usr.getUUID()).append("\n");

        return retVal.toString();
    }

    /**
     * Helper function for getInfo to return all User login names in the registry
     *
     * @return list of all login names for every user in string format
     */
    private String getUsers() {
        StringBuilder retVal = new StringBuilder();

        for(User usr: dict.values())
            retVal.append(usr.getLoginName()).append("\n");

        return retVal.toString();
    }

    /**
     * Creates an options list to be parsed when the IdServer is run
     *
     * @return a list of all options and their descriptions/characteristics
     */
    public static Options optionsList() {
        Options options = new Options();

        options.addOption(new Option(null, "numport", true, "Specify port to connect to (runs on port 1099 by default)"));
        options.addOption(new Option(null, "verbose", false,"Makes the server print detailed messages on the operations as it executes them"));

        return options;
    }

    /**
     * Parses through a list of all arguments and retrieves their values that were passed through the command
     *
     * @param args arguments passed through the main function
     * @return the command line object with values and options
     */
    public static CommandLine parseArguments(String[] args) {
        Options options = optionsList();

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch(ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("IdClient", options);
            System.exit(1);
        }

        return cmd;
    }

    /**
     * Takes care of parsing the command line arguments and setting up the registry connection
     * as well as the shutdown hook
     *
     * @param args list of arguments passed through
     */
    public static void main(String[] args) {
        boolean verbose = false;

        CommandLine userInput = parseArguments(args);

        if(userInput.hasOption("numport")) {
            defaultPort = Integer.parseInt(userInput.getOptionValue("numport"));
        }

        if(userInput.hasOption("verbose")) {
            verbose = true;
        }

        Registry registry = null;
        try {
            registry = LocateRegistry.getRegistry(defaultPort);
            registry.list();
        } catch (RemoteException e) {
            try {
                if(verbose)
                    System.out.println("Registry not found on port " +defaultPort+ ", creating new registry...");
                registry = LocateRegistry.createRegistry(defaultPort);
                if(verbose)
                    System.out.println("Registry created.");
            } catch (RemoteException e2) {
                System.out.println("No valid registry: " + e2.getMessage());
                System.exit(0);
            }
        }

        try {
            System.setSecurityManager(new SecurityManager());
            if(verbose) {
                System.out.println("Security manager set");
                System.out.println("Registry obtained");
            }
            IdServer serv = new IdServer("//IdServer");
            if(verbose)
                System.out.println("Valid server has been setup");
            registry.rebind("//localhost:" + defaultPort + "/IdServer", serv);
            readFile();
            System.out.println("Server bound to registry at port: "+defaultPort);
        }
        catch (IOException | ClassNotFoundException e){
            if(verbose)
                System.out.println("No valid backup available... creating new backup");
        }
        catch (Exception e) {
            System.out.println("IdServer err: " + e.getMessage());
            e.printStackTrace();
        }

        t = new Timer();
        // New timer scheduled for 2 min
        t.schedule(new Task(dict), 120000);
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }

    /**
     * The shutdown hook for backing up the registry before closing
     */
    static class ShutdownHook extends Thread {

        public void run() {
            System.out.println("Backing up registry and shutting down...");
            try {
                Task.writeToFile(dict);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * The timer for taking care of backing up the registry
     */
    static class Task extends TimerTask {
        private HashMap<String, User> dict;

        public Task(HashMap<String, User> dict){
            this.dict = dict;
        }

        public void run() {
            System.out.println("Backing up registry...");

            try {
                writeToFile(this.dict);
            }
            catch(IOException e){
                e.printStackTrace();
            }

            resetTimer();
        }

        public static void writeToFile(HashMap<String, User> dict) throws IOException {
            File f = new File("registry.backup");
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
            oos.writeObject(dict);
            oos.flush();
            oos.close();
        }
    }

    /**
     * Read all users into the dict object from the registry file
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static void readFile() throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("registry.backup"));
        dict = (HashMap<String, User>) ois.readObject();
    }

    /**
     * Reset the timer on the timer thread
     */
    public static void resetTimer() {
        t.cancel();
        t = new Timer();
        t.schedule(new Task(dict), 120000);
    }

    /**
     * Helper function for getInfo to find the login name via UUID
     *
     * @param uuid UUID used to find login name
     * @return a login name connected via UUID
     */
    private String findLoginNameByUUID(String uuid) {
        for(User usr: dict.values()){
            if(usr.getUUID().toString().equals(uuid)){
                return usr.getLoginName();
            }
        }

        return null;
    }
}
