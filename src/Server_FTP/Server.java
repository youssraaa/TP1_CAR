package Server_FTP;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
	
		   public static void main(String[] args) {
			   
		       int port = 2121; // Port sur lequel le serveur va écouter les connexions
		       try (ServerSocket serverSocket = new ServerSocket(port)) { // Crée un ServerSocket pour écouter les connexions entrantes
		           System.out.println("Serveur est ici pour accepter des connexion sur le port " + port);
		           while (true) {
		               Socket clientSocket = serverSocket.accept(); // Accepte la connexion d'un client
		               System.out.println("Client connecté : " + clientSocket.getInetAddress()); // Affiche l'adresse IP du client connecté
		           }
		       } catch (IOException e) {
		           e.printStackTrace();
		       }
		   }
		}


