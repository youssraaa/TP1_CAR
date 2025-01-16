package Server_FTP;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;
import java.util.stream.*;

public class Server {

	public static final Map<String, String> users = new HashMap<>();

    static {
        users.put("miage", "car");
    }

    public static void main(String[] args) {
    	
        int port = 2121; // Port FTP standard 
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur prêt à accepter des connexions sur le port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connecte : " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket, users)).start(); // Passe users ici
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
	
	    private Socket clientSocket;
	    private boolean authenticated = false;
	    private Map<String, String> users; 
	    
	    public ClientHandler(Socket clientSocket, Map<String, String> users) {
	        this.clientSocket = clientSocket;
	        this.users = users;
	    }
	    
	    @Override
	    public void run() {
	        try (
	            InputStream input = clientSocket.getInputStream();
	            OutputStream output = clientSocket.getOutputStream();
	            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
	            PrintWriter writer = new PrintWriter(output, true)
	        ) {
	        	
	            writer.println("220 Service ready");
	            String username = null;
	            while (true) {
	                String command = reader.readLine();
	                if (command == null) break;
	                
	                               
	                String[] parts = command.split(" ", 2);
	                String commandName = parts[0].toUpperCase();
	                
	                // Ignore la commande OPTS et continue
	                 if (commandName.equals("OPTS")) {
	                    writer.println("200 OPTS command ignored");
	                    continue; 
	                }
	         
	                String argument = parts.length > 1 ? parts[1] : "";
	                
	                switch (commandName) {
	                
	                    case "USER":
	                        if (users.containsKey(argument)) {
	                            username = argument;
	                            System.out.println("USER " + argument);
	                            writer.println("331 User name ok, need password");
	                        } else {
	                            writer.println("530 Unknown user");
	                        }
	                        break;
	                        
	                    case "PASS":
	                        if (username != null && users.get(username).equals(argument)) {
	                        	System.out.println("PASS " + argument);
	                            writer.println("230 User logged in ");
	                            authenticated = true;
	                        } else {
	                            writer.println("530 Authentication failed, please verify your credentials");
	                        }
	                        break;

	                    case "QUIT":
	                        writer.println("221 Logout");
	                        System.out.println("QUIT");
	                        clientSocket.close();
	                        return;
	                    	                        
	                    case "EPRT":
	                        writer.println("200 EPRT command ignored"); 
	                        break;

	                    case "LIST":
	                        if (authenticated) {
	                            // Mode passif configuré
	                            writer.println("229 Entering Extended Passive Mode (|||"+clientSocket.getLocalPort()+"|)");

	                            // Répertoire actuel
	                            Path currentDir = Paths.get("C:\\Users\\user\\Documents\\2024-2025\\MIAGE M1\\Semestre 2\\BCC 2 - Génie Logiciel\\[24-25] CAR\\TP1_CAR\\src\\Server_FTP\\shared");
	                            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(currentDir)) {
	                                writer.println("150 Accepted data connection");

	                                // Parcours des fichiers et envoi au client 
	                                for (Path path : directoryStream) {
	                                    File file = path.toFile();
	                                    String type = file.isDirectory() ? "d" : "-"; 
	                                    String permissions = type + "rw-r--r--"; 

	                                    long size = file.length(); 
	                                    String name = file.getName(); 

	                                    
	                                    writer.printf("%s %d %s%n", permissions, size, name); 
	                                    
	                                }
	                                
	                                System.out.printf("LIST\n"); 

	                                // Indiquer la fin de la liste
	                                writer.println("");
	                                writer.println("226 List transferred");
	                            } catch (IOException e) {
	                                writer.println("450 Requested file action not taken");
	                            }
	                        } else {
	                            writer.println("530 Please log in first");
	                        }
	                        break;


                   	                        
	                    default:
	                        writer.println("500 Unknown command : " + commandName);
	                        break;
	                }
	            }
	        } catch (IOException e) {
	            System.out.println("The client has disconnected.");
	        } finally {
	            try {
	                clientSocket.close();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }              
	    }
	}