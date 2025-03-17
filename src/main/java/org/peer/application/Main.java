package org.peer.application;

import org.peer.model.Peer;
import org.peer.network.ConnectionPeer;
import org.peer.network.PeerSearch;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Digite seu nome de usuário: ");
        String username = scanner.nextLine();

        System.out.print("Digite a porta para ouvir conexões: ");
        while (!scanner.hasNextInt()) {
            System.out.println("Entrada inválida! Digite um número para a porta:");
            scanner.next();
        }
        int port = scanner.nextInt();
        scanner.nextLine();

        Peer peer = new Peer(username, port);
        ConnectionPeer connection = new ConnectionPeer(username, port);
        connection.start();

        if (port == 8080) {
            PeerSearch.startDiscoveryServer();
        }
        connection.announcePresence();

        System.out.print("Deseja listar os peers disponíveis? (S/N): ");
        String listar = scanner.nextLine().trim();
        if (listar.equalsIgnoreCase("S")) {
            List<String> peers = PeerSearch.requestPeersFromNetwork();
            if (peers.isEmpty()) {
                System.out.println("Nenhum peer encontrado na rede.");
            } else {
                System.out.println("Peers disponíveis:");
                for (String peerInfo : peers) {
                    System.out.println(peerInfo);
                }
            }
        }

        System.out.print("Deseja se conectar a outro peer? (S/N): ");
        String resposta = scanner.next();
        if (resposta.equalsIgnoreCase("S")) {
            System.out.print("Digite o IP do peer: ");
            String host = scanner.next();
            System.out.print("Digite a porta do peer: ");
            int peerPort = scanner.nextInt();
            scanner.nextLine();
            connection.connectToPeer(host, peerPort);
        }
    }


}
