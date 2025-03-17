package org.peer.application.front;

import org.peer.network.ConnectionPeer;
import org.peer.network.PeerSearch;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PeerUI extends JFrame {
    private JTextField usernameField, portField, ipField, peerPortField, messageField;
    private JTextArea chatArea;
    private JButton startPeerButton, connectButton, sendButton, historyButton;
    private DefaultListModel<String> peerListModel;
    private JList<String> peerList;
    private ConnectionPeer connectionPeer;
    private final Set<String> displayedMessages = new HashSet<>();

    public PeerUI() {
        setTitle("P2P Chat");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Painel superior para entrada de nome e porta
        JPanel topPanel = new JPanel(new GridLayout(3, 2));
        topPanel.add(new JLabel("Nome:"));
        usernameField = new JTextField();
        topPanel.add(usernameField);

        topPanel.add(new JLabel("Porta:"));
        portField = new JTextField();
        topPanel.add(portField);

        startPeerButton = new JButton("Iniciar Peer");
        topPanel.add(startPeerButton);
        add(topPanel, BorderLayout.NORTH);

        // Painel lateral esquerdo com a lista de peers
        JPanel peerPanel = new JPanel(new BorderLayout());
        peerPanel.add(new JLabel("Lista de peers"), BorderLayout.NORTH);
        peerListModel = new DefaultListModel<>();
        peerList = new JList<>(peerListModel);
        peerPanel.add(new JScrollPane(peerList), BorderLayout.CENTER);
        add(peerPanel, BorderLayout.WEST);

        // Painel de conexão manual
        JPanel connectPanel = new JPanel(new GridLayout(3, 2));
        connectPanel.add(new JLabel("IP:"));
        ipField = new JTextField();
        connectPanel.add(ipField);

        connectPanel.add(new JLabel("Porta:"));
        peerPortField = new JTextField();
        connectPanel.add(peerPortField);

        connectButton = new JButton("Conectar a este peer");
        connectPanel.add(connectButton);
        peerPanel.add(connectPanel, BorderLayout.SOUTH);

        // Área de chat
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // Painel inferior para mensagens e histórico
        JPanel messagePanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Enviar");
        historyButton = new JButton("Histórico"); // Botão de histórico

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(sendButton);
        buttonPanel.add(historyButton);

        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(buttonPanel, BorderLayout.EAST);
        add(messagePanel, BorderLayout.SOUTH);

        // Eventos dos botões
        startPeerButton.addActionListener(e -> startPeer());
        connectButton.addActionListener(e -> connectToPeer());
        sendButton.addActionListener(e -> sendMessage());
        historyButton.addActionListener(e -> showChatHistory()); // Novo evento do botão de histórico

        setVisible(true);
    }

    private void startPeer() {
        String username = usernameField.getText();
        int port;
        try {
            port = Integer.parseInt(portField.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Porta inválida!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        connectionPeer = new ConnectionPeer(username, port);
        connectionPeer.start();
        PeerSearch.startDiscoveryServer();
        connectionPeer.announcePresence();

        new Thread(this::updatePeerList).start();
        listenForMessages();
    }

    private void updatePeerList() {
        while (true) {
            List<String> peers = PeerSearch.requestPeersFromNetwork();
            SwingUtilities.invokeLater(() -> {
                peerListModel.clear();
                for (String peer : peers) {
                    peerListModel.addElement(peer);
                }
            });

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void connectToPeer() {
        String host = ipField.getText();
        int peerPort;
        try {
            peerPort = Integer.parseInt(peerPortField.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Porta inválida!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (connectionPeer != null) {
            connectionPeer.connectToPeer(host, peerPort);
            chatArea.append("Conectado ao peer em " + host + ":" + peerPort + "\n");
        }
    }

    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty() && connectionPeer != null) {
            connectionPeer.broadcastMessage(message);

            String formattedMessage = "Você: " + message;
            chatArea.append(formattedMessage + "\n");
            connectionPeer.getReceivedMessages().add(formattedMessage);
            messageField.setText("");
        }
    }


    private void listenForMessages() {
        new Thread(() -> {
            while (true) {
                if (connectionPeer != null) {
                    List<String> messages = connectionPeer.getReceivedMessages();
                    SwingUtilities.invokeLater(() -> {
                        for (String message : messages) {
                            if (!displayedMessages.contains(message)) {
                                chatArea.append(message + "\n");
                                displayedMessages.add(message);
                            }
                        }
                    });
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }).start();
    }

    private void showChatHistory() {
        if (connectionPeer == null) {
            JOptionPane.showMessageDialog(this, "Nenhum peer iniciado!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<String> history = connectionPeer.getReceivedMessages();
        if (history.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhuma mensagem no histórico.", "Histórico", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JTextArea historyArea = new JTextArea();
            historyArea.setEditable(false);
            for (String message : history) {
                historyArea.append(message + "\n");
            }

            JScrollPane scrollPane = new JScrollPane(historyArea);
            scrollPane.setPreferredSize(new Dimension(400, 300));

            JOptionPane.showMessageDialog(this, scrollPane, "Histórico de Mensagens", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(PeerUI::new);
    }
}
