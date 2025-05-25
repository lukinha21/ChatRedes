package server;

import db.Database;
import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalTime;
import java.util.*;

public class ChatServer {
    private static final int PORT = 6789;
    public static List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    public static Map<String, Set<ClientHandler>> grupos = Collections.synchronizedMap(new HashMap<>());
    public static Map<String, List<Convite>> convitesPendentes = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) throws IOException {
        Database.init();
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Servidor ouvindo na porta " + PORT);

        // ✅ Carrega todos os grupos existentes do banco para a memória
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT DISTINCT grupo FROM grupo_usuario");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String grupo = rs.getString("grupo");
                ChatServer.grupos.computeIfAbsent(grupo, k -> Collections.synchronizedSet(new HashSet<>()));
            }
        } catch (Exception e) {
            System.out.println("Erro ao carregar grupos do banco: " + e.getMessage());
        }

        // Loop principal
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Novo cliente conectado: " + clientSocket);
            ClientHandler handler = new ClientHandler(clientSocket);
            clients.add(handler);
            new Thread(handler).start();
        }
    }


    public static void broadcast(String message, ClientHandler excludeClient) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != excludeClient) {
                    client.sendMessage(message);
                }
            }
        }
    }

    public static void enviarParaGrupo(String grupo, String mensagem, ClientHandler remetente) {
        Set<ClientHandler> membros = grupos.get(grupo);
        String hora = LocalTime.now().withSecond(0).withNano(0).toString();
        if (membros != null) {
            synchronized (membros) {
                for (ClientHandler c : membros) {
                    if (c != remetente) {
                        c.sendMessage("[Grupo: " + grupo + " (" + hora + ")]: "+ remetente.getNomeUsuario() + ": " + mensagem);
                    }
                }
            }
        } else {
            remetente.sendMessage("Grupo não encontrado.");
        }
    }

    public static void enviarParaGrupoSelecionados(String grupo, String mensagem, ClientHandler remetente, List<String> destinos) {
        Set<ClientHandler> membros = grupos.get(grupo);
        String hora = LocalTime.now().withSecond(0).withNano(0).toString();

        if (membros != null) {
            synchronized (membros) {
                for (ClientHandler c : membros) {
                    if (destinos.contains(c.getNomeUsuario())) {
                        c.sendMessage("[Privado no grupo: " + " (" + hora + ")]: " + remetente.getNomeUsuario() + ": " + mensagem);
                    }
                }
            }
        } else {
            remetente.sendMessage("Grupo não encontrado.");
        }
    }

    public static class Convite {
        public final String grupo;
        public final ClientHandler solicitante;
        public final ClientHandler convidado;
        public final Set<ClientHandler> aguardando;

        public Convite(String grupo, ClientHandler solicitante, ClientHandler convidado, Set<ClientHandler> aguardando) {
            this.grupo = grupo;
            this.solicitante = solicitante;
            this.convidado = convidado;
            this.aguardando = aguardando;
        }
    }
}
