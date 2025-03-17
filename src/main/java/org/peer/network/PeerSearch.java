package org.peer.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PeerSearch {
    private static final int DISCOVERY_PORT = 5000;
    private static final List<String> peers = new ArrayList<>();

    public static void startDiscoveryServer() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(DISCOVERY_PORT, InetAddress.getByName("0.0.0.0"))) {
                socket.setBroadcast(true);
                byte[] buffer = new byte[1024];
                System.out.println("Servidor de descoberta ativo na porta " + DISCOVERY_PORT);

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength());

                    if (message.equals("REQUEST_PEERS")) {
                        System.out.println("Pedido de lista de peers recebido!");
                        sendPeersList(socket, packet.getAddress(), packet.getPort());
                        continue;
                    }
                    if (!peers.contains(message)) {
                        peers.add(message);
                        System.out.println("Novo peer descoberto: " + message);
                    }
                }
            } catch (IOException e) {
                System.out.println("Erro no servidor de descoberta: " + e.getMessage());
            }
        }).start();
    }

    private static void sendPeersList(DatagramSocket socket, InetAddress address, int port) throws IOException {
        String peerList = String.join(";", peers);
        byte[] buffer = peerList.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
        socket.send(packet);
        System.out.println("Enviando lista de peers para " + address.getHostAddress() + ":" + port);
    }

    public static List<String> requestPeersFromNetwork() {
        List<String> discoveredPeers = new ArrayList<>();

        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] request = "REQUEST_PEERS".getBytes();
            InetAddress serverAddress = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(request, request.length, serverAddress, DISCOVERY_PORT);
            socket.send(packet);

            byte[] buffer = new byte[1024];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(responsePacket);
            String receivedData = new String(responsePacket.getData(), 0, responsePacket.getLength());

            if (!receivedData.isEmpty()) {
                discoveredPeers = Arrays.asList(receivedData.split(";"));
            }

        } catch (IOException e) {
            System.out.println("Erro ao solicitar lista de peers: " + e.getMessage());
        }

        return discoveredPeers;
    }








}
