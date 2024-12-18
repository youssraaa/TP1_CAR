package Server_FTP;

import java.io.*;
import java.net.*;
import java.util.*;

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
                System.out.println("Client connect\u00e9 : " + clientSocket.getInetAddress());
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
        	
            writer.println("220 Bienvenue sur le mini-serveur FTP");

            String username = null;
            while (true) {
                String command = reader.readLine();
                if (command == null) break;
                               
                String[] parts = command.split(" ", 2);
                String commandName = parts[0].toUpperCase();

                // Traitement de la commande OPTS (ignore la commande et continue)
                if (commandName.equals("OPTS")) {
                    writer.println("200 Commande OPTS ignorée");
                    continue; 
                }
         
                String argument = parts.length > 1 ? parts[1] : "";

                switch (commandName) {
                    case "USER":
                        if (users.containsKey(argument)) {
                            username = argument;
                            System.out.println("USER " + argument);
                            writer.println("331 Nom d'utilisateur accept\u00e9, mot de passe requis");
                        } else {
                            writer.println("530 Utilisateur inconnu");
                        }
                        break;

                    case "PASS":
                        if (username != null && users.get(username).equals(argument)) {
                        	System.out.println("PASS " + argument);
                            writer.println("230 Authentification r\u00e9ussie, bienvenue " + username + " !");
                            authenticated = true;
                        } else {
                            writer.println("530 Authentification \u00e9chou\u00e9e, v\u00e9rifiez vos identifiants");
                        }
                        break;

                    case "QUIT":
                        writer.println("221 D\u00e9connexion en cours... Au revoir !");
                        System.out.println("Fin de connexion avec le client : " + clientSocket.getInetAddress());
                        clientSocket.close();
                        return;

                    default:
                        writer.println("500 Commande non reconnue : " + commandName);
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Le client s'est d\u00e9connect\u00e9.");
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
              
    }
}
