package Server_FTP;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    private static final String USERNAME = "miage";
    private static final String PASSWORD = "car";

    public static void main(String[] args) throws IOException {
    	
        int port = 2121;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Serveur prêt à accepter des connexions sur le port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new FTPClientHandler(clientSocket).start();
        }
    }

    private static class FTPClientHandler extends Thread {
    	
        private Socket clientSocket;
        private String currentDirectory = System.getProperty("user.dir");
        private boolean authenticated = false;
        private String clientUsername = null;

        public FTPClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (
                InputStream inputStream = clientSocket.getInputStream();
                OutputStream outputStream = clientSocket.getOutputStream();
                Scanner inputScanner = new Scanner(inputStream);
                PrintWriter outputWriter = new PrintWriter(outputStream, true)
            ) {
                outputWriter.println("220 Service ready");

                while (inputScanner.hasNextLine()) {
                    String clientMessage = inputScanner.nextLine().trim();
                    String command = extractCommand(clientMessage);

                    switch (command) {
                        case "USER":
                            System.out.println("USER " + clientMessage.substring(5).trim());
                            String username = clientMessage.substring(5).trim();
                            if (USERNAME.equals(username)) {
                                clientUsername = username;
                                outputWriter.println("331 Username accepted. Enter password :");
                            } else {
                                outputWriter.println("430 Invalid username.");
                            }
                            break;

                        case "PASS":
                            System.out.println("PASS " + clientMessage.substring(5).trim());
                            String password = clientMessage.substring(5).trim();
                            if (PASSWORD.equals(password) && USERNAME.equals(clientUsername)) {
                                authenticated = true;
                                outputWriter.println("230 User authenticated.");
                            } else {
                                outputWriter.println("430 Invalid password.");
                            }
                            break;

                        case "QUIT":
                            System.out.println("Client disconnected.");
                            outputWriter.println("221 Disconnected from server.");
                            clientSocket.close();
                            return;

                        default:
                            if (!authenticated) {
                                outputWriter.println("530 Not logged in.");
                                break;
                            }
                            switch (command) {
                                case "RETR":
                                    System.out.println("RETR " + clientMessage.substring(5).trim());
                                    String fileName = clientMessage.substring(5).trim();
                                    File file = new File(currentDirectory, fileName);
                                    if (!file.exists() || !file.isFile()) {
                                        outputWriter.println("550 File not found.");
                                        break;
                                    }
                                    try (ServerSocket dataSocket = new ServerSocket(0)) {
                                        int dataPort = dataSocket.getLocalPort();
                                        String dataIp = InetAddress.getLocalHost().getHostAddress();
                                        outputWriter.println("227 Entering Passive Mode (" + dataIp.replace(".", ",") + "," + dataPort + ")");

                                        try (Socket dataConnection = dataSocket.accept();
                                             InputStream fileInput = new FileInputStream(file);
                                             OutputStream dataOutput = dataConnection.getOutputStream()) {
                                            outputWriter.println("150 Opening data connection.");
                                            byte[] buffer = new byte[4096];
                                            int bytesRead;
                                            while ((bytesRead = fileInput.read(buffer)) != -1) {
                                                dataOutput.write(buffer, 0, bytesRead);
                                            }
                                            outputWriter.println("226 Transfer complete.");
                                        }
                                    } catch (IOException e) {
                                        outputWriter.println("550 Error transferring file.");
                                    }
                                    break;

                                case "CWD":
                                    System.out.println("CWD " + clientMessage.substring(4).trim());
                                    String dir = clientMessage.substring(4).trim();
                                    File newDir = new File(dir);
                                    if (newDir.exists() && newDir.isDirectory()) {
                                        currentDirectory = dir;
                                        outputWriter.println("250 Directory changed successfully.");
                                    } else {
                                        outputWriter.println("550 Invalid directory.");
                                    }
                                    break;

                                case "LIST":
                                    System.out.println("LIST " + clientMessage);
                                    File directory = new File(currentDirectory);
                                    File[] files = directory.listFiles();
                                    if (files == null || files.length == 0) {
                                        outputWriter.println("550 No files to list.");
                                        break;
                                    }
                                    try (ServerSocket dataSocket = new ServerSocket(0)) {
                                        int dataPort = dataSocket.getLocalPort();
                                        String dataIp = InetAddress.getLocalHost().getHostAddress();
                                        outputWriter.println("227 Entering Passive Mode (" + dataIp.replace(".", ",") + "," + dataPort + ")");

                                        try (Socket dataConnection = dataSocket.accept();
                                             PrintWriter dataWriter = new PrintWriter(dataConnection.getOutputStream(), true)) {
                                            outputWriter.println("150 Listing directory.");
                                            for (File fileItem : files) {
                                                String type = fileItem.isDirectory() ? "d" : "-";
                                                dataWriter.println(type + " " + fileItem.getName());
                                            }
                                            outputWriter.println("226 Directory listing complete.");
                                        }
                                    } catch (IOException e) {
                                        outputWriter.println("550 Error listing directory.");
                                    }
                                    break;

                                default:
                                    outputWriter.println("502 Command not implemented.");
                            }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String extractCommand(String message) {
            int spaceIndex = message.indexOf(' ');
            return (spaceIndex == -1) ? message.toUpperCase() : message.substring(0, spaceIndex).toUpperCase();
        }
    }
}
