/**
 * @author: Diklic, Stefan
 * @author: Fernandez, Justin
 * @author: Murphy, Joseph
 * Code used for clients. Contains argument parsing and command usage assistance.
 */

import org.apache.commons.cli.*;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;



public class IdClient {
	private static String host;

	//The default port used if no port is specified
	private static int defaultPort = 1099;

	public static void main(String[] args) {
		CommandLine userInput = parseArguments(args);

		//Message used for errors during usage of creation command.
		String createCommandError = "c,--create <arg> -With this option, the client contacts the server and attempts to create the new login name. The client" +
				" optionally provides the real user name and password along with the request.";

		try {
			Registry registry = LocateRegistry.getRegistry(host, defaultPort);
			Queries methodStub = (Queries) registry.lookup("//" + host + ":" + defaultPort + "/IdServer");
			//If user is utilizing the create command, check for valid arguments
			if(userInput.hasOption("create")) {
				String password = null;
				if(userInput.hasOption("password")) password = userInput.getOptionValue("password");
				String[] createArgs = userInput.getOptionValues("create");
				if(createArgs == null){ //If user provides no arguments , print usage and exit.
					System.out.println(createCommandError);
					System.exit(1);
				}
				//If user only provided one argument, then get real name from system.
				if(createArgs.length == 1) {
					createLogin(methodStub, createArgs[0], System.getProperty("user.name"), password);
				} else if (createArgs.length == 2) { //If user provides 2 arguments, parse login name and real name from input.
					createLogin(methodStub, createArgs[0], createArgs[1], password);
				} else {
					System.out.println(createCommandError);
					System.exit(1);
				}
			}

			//Utilized for the lookup command, which searches for user by login name
			else if(userInput.hasOption("lookup")) {
				lookupByLogin(methodStub, userInput.getOptionValue("lookup"));
			}
			//Utilized for the reverse-lookup command, which searches for user by UUID
			else if(userInput.hasOption("reverse-lookup")) {
				lookupByUUID(methodStub, userInput.getOptionValue("reverse-lookup"));
			}
			//Utilized for the modify command
			if(userInput.hasOption("modify")) {
				String password = null; //If user set a password, its required for modification
				if(userInput.hasOption("password")) password = userInput.getOptionValue("password");
				String[] modifyArgs = userInput.getOptionValues("modify");
				modifyName(methodStub, modifyArgs[0], modifyArgs[1], password);
			}
			//Utilized for the delete command
			if(userInput.hasOption("delete")) {
				String password = null; //If user set a password, its required for deletion.
				if(userInput.hasOption("password")) password = userInput.getOptionValue("password");
				deleteUser(methodStub, userInput.getOptionValue("delete"), password);
			}//Utilized for the get command. Parse the type of get command to return appropriate info.
			if(userInput.hasOption("get")) {
				String option = userInput.getOptionValue("get");
				if(!(option.equals("users") || option.equals("uuids") || option.equals("all"))) {
					System.out.println("get expects one of: users, uuids, all");
					System.exit(1);
				}
				getInfo(methodStub, option);
			}
		} catch(RemoteException | NotBoundException e) {
			System.out.println(e.getMessage());
		}
	}

	//Creates a new login
	private static void createLogin(Queries methodStub, String loginName, String realName, String password) throws RemoteException {
		if(password != null) {
			try {
				password = trySHA(password);
			} catch (java.security.NoSuchAlgorithmException e) {
				System.err.println(e);
			}
		}

		System.out.println(methodStub.createLogin(loginName, realName, password));
	}
	//Used for the lookup command, finds users when given login name
	private static void lookupByLogin(Queries methodStub, String loginName) throws RemoteException {
		System.out.println(methodStub.lookupByLogin(loginName));
	}
	//Used for the reverse-lookup command, finds users when given a UUID
	private static void lookupByUUID(Queries methodStub, String uuid) throws RemoteException {
		System.out.println(methodStub.lookupByUUID(uuid));
	}
	//Used for modify command, changes login name
	private static void modifyName(Queries methodStub, String oldName, String newName, String password) throws RemoteException {
		if(password != null) {
			try {
				password = trySHA(password); //Requires a password if the given user set one upon creation
			} catch (java.security.NoSuchAlgorithmException e) {
				System.err.println(e);
			}
		}
		System.out.println(methodStub.modifyName(oldName, newName, password));
	}
	//Used for the delete command, deletes a user from registry
	private static void deleteUser(Queries methodStub, String loginName, String password) throws RemoteException {
		if(password != null) {
			try {
				password = trySHA(password);//Requires a password if the given user set one upon creation
			} catch (java.security.NoSuchAlgorithmException e) {
				System.err.println(e);
			}
		}
		System.out.println(methodStub.deleteUser(loginName, password));
	}
	//Used by the get command, 3 different types of info can be returned
	private static void getInfo(Queries methodStub, String type) throws RemoteException {
		System.out.println(methodStub.getInfo(type));
	}
	//Parses the arguments from the user
	private static CommandLine parseArguments(String[] args) {
		Options options = optionsList();

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch(ParseException e) { //If there is invalid arg input, prints correct usage.
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("IdClient", options);
			System.exit(1);
		}

		host = cmd.getOptionValue("server");
		if (cmd.hasOption("numport")) defaultPort = Integer.parseInt(cmd.getOptionValue("numport"));

		return cmd;
	}
	//SHA-512 hashing, used for passwords.
	private static String trySHA(String input) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-512");
		byte[] bytes = input.getBytes();
		md.reset();

		byte[] result = md.digest(bytes);
		StringBuilder retVal = new StringBuilder();
		for (byte b : result) retVal.append(String.format("%X", b));

		return retVal.toString();
	}
	//Builds the list of possible commands, also used as a proper usager message
	private static Options optionsList() {
		Options options = new Options();
		OptionGroup queries = new OptionGroup();
		options.addOption(new optionsListFormat(null, "server", true, "Specify serverhost to connect to"));
		options.addOption(new Option(null, "numport", true, "Specify port to connect to (runs on port 1099 by default)"));
		queries.addOption(Option.builder(null)
				.longOpt("create")
				.numberOfArgs(2)
				.optionalArg(true)
				.desc("The client contacts the server and attempts to create the new login name. The client optionally provides the real user name and password along with the request")
				.build());
		queries.addOption(new Option(null, "lookup", true, "The client connects with the server and looks up the login name and displays all information found associated with the login name"));
		queries.addOption(new Option(null, "reverse-lookup", true, "The client connects with the server and looks up the UUID and displays all information found associated with the UUID"));
		queries.addOption(Option.builder(null)
				.longOpt("modify")
				.numberOfArgs(2)
				.desc("The client " +
						"contacts the server and requests a login name change. If the new login name is available, the server changes the name")
				.build());
		queries.addOption(new Option(null, "delete", true, "The client contacts the server and requests to delete their login name. The client must supply the correct password for this operation to succeed"));
		queries.addOption(new Option(null, "get", true, "(users|uuids|all) The client contacts the server and obtains either a list all login names, list of all UUIDs or a list of user, UUID and string description all accounts"));
		options.addOptionGroup(queries);
		options.addOption(new Option(null, "password", true, "Sets/utilizes the password if required for the chosen option"));

		return options;
	}
	//Determines the optionList format
	private static class optionsListFormat extends Option {

		public optionsListFormat(String opt, String longOpt, boolean hasArg, String description) throws IllegalArgumentException {
			super(opt, longOpt, hasArg, description);
			this.setRequired(true);
		}
	}



}
