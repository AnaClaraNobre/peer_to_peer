package org.peer.network;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class ConnectionPeer {

    private ServerSocket serverSocket;
    private List<Socket> connections = new ArrayList<>();
    private String username;
    private List<String> messageHistory = new ArrayList<>();
    private volatile boolean running = true;

    public ConnectionPeer(String username, int port) {
        this.username = username;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Peer " + username + " está ouvindo na porta " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        new Thread(this::listenForConnections).start();
        new Thread(this::listenForUserInput).start();
    }

    private void listenForConnections() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                connections.add(socket);
                new Thread(() -> handleConnection(socket)).start();
            } catch (IOException e) {
                if (!running) {
                    System.out.println("Servidor encerrado. Não aceitando mais conexões.");
                    break;
                }
                System.out.println("Erro ao aceitar conexão: " + e.getMessage());
            }
        }
    }


    private void handleConnection(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println(message);
                messageHistory.add(message);
                broadcastMessage(message, socket);
                saveMessageToFile(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastMessage(String message, Socket sender) {
        for (Socket socket : connections) {
            if (socket != sender) {
                try {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(message);
                } catch (IOException e) {
                    System.out.println("Erro ao enviar mensagem: " + e.getMessage());
                }
            }
        }
    }


    private void listenForUserInput() {
        try (BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                String message = userInput.readLine();
                if (message.equalsIgnoreCase("/historico")) {
                    showMessageHistory();
                    continue;
                }
                broadcastMessage(message);
                saveMessageToFile("[Você] " + message);
                messageHistory.add("[Você] " + message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastMessage(String message) {
        for (Socket socket : connections) {
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(username + ": " + message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void connectToPeer(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            connections.add(socket);
            new Thread(() -> handleConnection(socket)).start();
            System.out.println("Conectado ao peer em " + host + ":" + port);
        } catch (IOException e) {
            System.out.println("Erro ao conectar ao peer em " + host + ":" + port);
        }
    }
    private void saveMessageToFile(String message) {
        try (FileWriter fw = new FileWriter("chat_history.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(message);
        } catch (IOException e) {
            System.out.println("Erro ao salvar mensagem no arquivo: " + e.getMessage());
        }
    }
    private void showMessageHistory() {
        System.out.println("Histórico de Mensagens:");
        if (messageHistory.isEmpty()) {
            System.out.println("Ainda não há mensagens.");
        } else {
            for (String msg : messageHistory) {
                System.out.println(msg);
            }
        }
    }

    public void announcePresence() {
        try (DatagramSocket socket = new DatagramSocket()) {
            String message = username + " disponível em " + serverSocket.getLocalPort();
            byte[] buffer = message.getBytes();
            InetAddress group = InetAddress.getByName("255.255.255.255");

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, 5000);
            socket.send(packet);
            System.out.println("Anunciando presença: " + message);
        } catch (IOException e) {
            System.out.println("Erro ao anunciar presença: " + e.getMessage());
        }
    }
    public List<String> getReceivedMessages() {
        return new ArrayList<>(messageHistory);
    }


}
