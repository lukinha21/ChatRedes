package server;

import db.Database;
import model.Usuario;
import java.io.*;
import java.net.Socket;
import java.time.LocalTime;

import java.sql.*;
import java.util.*;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private static final Set<String> aceitePrivado = new HashSet<>();
    private static final Map<String, List<String>> mensagensPendentes = new HashMap<>();


    private Usuario usuario;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String[] cmd = inputLine.split(" ", 2);
                switch (cmd[0].toLowerCase()) {
                    case "login": handleLogin(cmd[1]); break;
                    case "cadastrar": handleCadastro(cmd[1]); break;
                    case "status": handleStatus(cmd[1]); break;
                    case "msg": handlePrivateMessage(cmd[1]); break;
                    case "criargrupo": handleCriarGrupo(cmd[1]); break;
                    case "msggrupo": handleMensagemGrupo(cmd[1]); break;
                    case "convidar": handleConvidarGrupo(cmd[1]); break;
                    case "aceitar": handleRespostaConvite(cmd[1], true); break;
                    case "aceitarprivado": handleAceitePrivado(cmd[1]); break;
                    case "aceitarconvite": handleAceitarConvite(cmd[1]); break;
                    case "listarusuarios": handleListarUsuarios(); break;
                    case "listargrupos": handleListarGrupos(); break;
                    case "recuperarsenha": handleRecuperarSenha(cmd[1]); break;
                    case "listarpendentes": handleListarPendentes(); break;
                    case "recusar": handleRespostaConvite(cmd[1], false); break;
                    case "sairgrupo": handleSairGrupo(cmd[1]); break;
                    case "msgprivgrupo": handleMsgGrupoPrivado(cmd[1]); break;
                    case "solicitarentrada": handleSolicitarEntradaGrupo(cmd[1]); break;
                    case "help": handleHelp(); break;
                    case "sair": handleLogout(); return;
                    default: send("Comando inválido.");
                }
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado.");
        }
    }
    private void handleHelp() throws IOException {
        send("\n=== MENU DE COMANDOS DISPONÍVEIS ===");
        send("cadastrar NomeCompleto,login,email,senha    → Cria um novo usuário");
        send("ex: cadastrar Robson_Silva,robson,robson@email.com,1234");
        send("login login,senha                          → Faz login no sistema");
        send("ex: login robson,1234");
        send("status novo_status                         → Atualiza seu status (Online, Ocupado, etc)");
        send("msg nome mensagem                          → Envia mensagem privada após aceite");
        send("aceitarprivado nome                         → Aceita trocar mensagens com o nome indicado");
        send("criargrupo nome                             → Cria um grupo de chat");
        send("msggrupo nome msg                          → Envia mensagem para todos do grupo");
        send("msgprivgrupo grupo user1,user2 msg         → Mensagem privada no grupo para usuários específicos");
        send("convidar grupo nome                         → Convida alguém para o grupo (requer aceitação dos membros)");
        send("solicitarentrada grupo                      → Solicita entrar em um grupo existente");
        send("aceitar grupo nome                          → Aceita entrada de alguém no grupo");
        send("recusar grupo nome                          → Recusa entrada de alguém no grupo");
        send("sairgrupo grupo                             → Sai de um grupo e avisa os outros membros");
        send("listarusuarios                              → Mostra todos os usuários online com status");
        send("listargrupos                                → Mostra grupos disponíveis e número de membros");
        send("listarpendentes                             → Mostra mensagens que estavam pendentes durante seu logout");
        send("recuperarsenha email                        → Recupera sua senha com base no email");
        send("help                                        → Mostra este menu a qualquer momento");
        send("sair                                        → Encerra sua sessão e define status como Offline");
    }

    public void sendMessage(String msg) {
        try {
            out.write(msg);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            System.out.println("Erro ao enviar mensagem: " + e.getMessage());
        }
    }
    private void handleLogout() {
        try {
            if (usuario != null) {
                usuario.setStatus("Offline");
                try (Connection conn = Database.connect();
                     PreparedStatement stmt = conn.prepareStatement("UPDATE usuarios SET status='Offline' WHERE nome=?")) {
                    stmt.setString(1, usuario.getNome());
                    stmt.executeUpdate();
                }

                // Remover apenas da memória ativa (não da base de dados)
                for (Set<ClientHandler> grupoMembros : ChatServer.grupos.values()) {
                    grupoMembros.remove(this);
                }

                ChatServer.clients.remove(this);
                send("Logout realizado. Sessão finalizada e status definido como Offline.");
            }
            socket.close();
        } catch (Exception e) {
            System.out.println("Erro ao fazer logout: " + e.getMessage());
        }
    }

    private void send(String msg) throws IOException {
        out.write(msg);
        out.newLine();
        out.flush();
    }


    private void handleLogin(String dados) throws IOException {
        String[] p = dados.split(",");
        String login = p[0];
        String senha = p[1];

        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM usuarios WHERE login=? AND senha=?")) {
            stmt.setString(1, login);
            stmt.setString(2, senha);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                for (ClientHandler c : ChatServer.clients) {
                    if (c != this && c.usuario != null && c.usuario.getLogin().equalsIgnoreCase(login)) {
                        send("Este usuário já está conectado em outra sessão.");
                        return;
                    }
                }
                usuario = new Usuario(
                        rs.getString("nome"),
                        rs.getString("login"),
                        rs.getString("email"),
                        rs.getString("senha"),
                        "Online"
                );
                try (PreparedStatement updateStatus = conn.prepareStatement("UPDATE usuarios SET status='Online' WHERE login=?")) {
                    updateStatus.setString(1, login);
                    updateStatus.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement("SELECT remetente FROM aceite_privado WHERE destinatario = ?")) {
                    ps.setString(1, usuario.getNome());
                    ResultSet rsAceites = ps.executeQuery();
                    while (rsAceites.next()) {
                        String remetente = rsAceites.getString("remetente");
                        aceitePrivado.add(usuario.getNome() + "->" + remetente);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement("SELECT grupo FROM grupo_usuario WHERE usuario = ?")) {
                    ps.setString(1, usuario.getNome());
                    ResultSet rsGrupos = ps.executeQuery();
                    while (rsGrupos.next()) {
                        String grupo = rsGrupos.getString("grupo");
                        ChatServer.grupos.computeIfAbsent(grupo, k -> Collections.synchronizedSet(new HashSet<>())).add(this);
                    }
                }

                send("Login realizado como " + usuario.getNome());
                List<String> pendentes = mensagensPendentes.getOrDefault(usuario.getNome(), new ArrayList<>());
                if (!pendentes.isEmpty()) {
                    send("\n=== Mensagens pendentes ===");
                    for (String m : pendentes) {
                        send("[PENDENTE] " + m);
                    }
                    mensagensPendentes.remove(usuario.getNome());
                }
            } else {
                send("Login falhou.");
            }
        } catch (SQLException e) {
            send("Erro ao fazer login: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private void salvarParticipacaoGrupo(String grupo) {
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO grupo_usuario (grupo, usuario) VALUES (?, ?)")) {
            ps.setString(1, grupo);
            ps.setString(2, usuario.getNome());
            ps.executeUpdate();
        } catch (SQLException e) {
            sendMessage("Erro ao salvar grupo: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleListarPendentes() throws IOException {
        List<String> pendentes = mensagensPendentes.getOrDefault(usuario.getNome(), new ArrayList<>());
        if (pendentes.isEmpty()) {
            send("Nenhuma mensagem pendente.");
        } else {
            send("\n=== Mensagens pendentes ===");
            for (String m : pendentes) {
                send("[PENDENTE] " + m);
            }
        }
    }

    private void handleCadastro(String dados) throws IOException {
        String[] p = dados.split(",");
        String nome = p[0];
        String login = p[1];
        String email = p[2];
        String senha = p[3];

        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO usuarios (nome, login, email, senha) VALUES (?, ?, ?, ?);")) {
            stmt.setString(1, nome);
            stmt.setString(2, login);
            stmt.setString(3, email);
            stmt.setString(4, senha);
            stmt.executeUpdate();
            send("Usuário " + nome + " cadastrado com sucesso.");
        } catch (SQLException e) {
            send("Erro ao cadastrar: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleStatus(String novoStatus) throws IOException {
        if (usuario == null) {
            send("Você precisa estar logado para mudar o status.");
            return;
        }

        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement("UPDATE usuarios SET status=? WHERE nome=?")) {
            stmt.setString(1, novoStatus);
            stmt.setString(2, usuario.getNome());
            stmt.executeUpdate();
            usuario.setStatus(novoStatus);
            send("Status atualizado para " + novoStatus);

            if (novoStatus.equalsIgnoreCase("Online")) {
                List<String> pendentes = mensagensPendentes.getOrDefault(usuario.getNome(), new ArrayList<>());
                if (!pendentes.isEmpty()) {
                    send("\n=== Mensagens pendentes ===");
                    for (String m : pendentes) {
                        send("[PENDENTE] " + m);
                    }
                    mensagensPendentes.remove(usuario.getNome());
                }
            }
        } catch (SQLException e) {
            send("Erro ao atualizar status: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleListarGrupos() throws IOException {
        StringBuilder sb = new StringBuilder("Grupos existentes:\n");
        for (String grupo : ChatServer.grupos.keySet()) {
            sb.append("- ").append(grupo).append(" (membros: ").append(ChatServer.grupos.get(grupo).size()).append(")\n");
        }
        send(sb.toString());
    }
    private void handleRecuperarSenha(String email) throws IOException {
        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement("SELECT senha FROM usuarios WHERE email=?")) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                send("Sua senha é: " + rs.getString("senha"));
            } else {
                send("Email não encontrado.");
            }
        } catch (SQLException e) {
            send("Erro ao recuperar senha: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handlePrivateMessage(String dados) throws IOException {
        if (usuario == null) {
            send("Você precisa estar logado para enviar mensagens.");
            return;
        }

        String[] partes = dados.split(" ", 2);
        String destinatario = partes[0];
        String mensagem = partes[1];
        String chaveAceite = destinatario + "->" + usuario.getNome();

        if (!aceitePrivado.contains(chaveAceite)) {
            for (ClientHandler c : ChatServer.clients) {
                if (c.usuario != null && c.usuario.getNome().equalsIgnoreCase(destinatario)) {
                    c.sendMessage("[Solicitação] " + usuario.getNome() + " deseja trocar mensagens privadas com você. Digite: aceitarprivado " + usuario.getNome());
                    break;
                }
            }
            send("Mensagem não enviada. Aguarde o destinatário aceitar o contato.");
            return;
        }

        boolean entregue = false;
        for (ClientHandler c : ChatServer.clients) {
            if (c.usuario != null && c.usuario.getNome().equalsIgnoreCase(destinatario)) {
                if ("Offline".equalsIgnoreCase(c.usuario.getStatus())) {
                    mensagensPendentes.computeIfAbsent(destinatario, k -> new ArrayList<>())
                            .add("[Privado de " + usuario.getNome() + "]: " + mensagem);
                    send("Usuário está offline. Mensagem salva como pendente.");
                    return;
                }
                String hora = LocalTime.now().withSecond(0).withNano(0).toString();
                c.sendMessage("[Privado de " + usuario.getNome() + " (" + hora + ")]: " + mensagem);
                entregue = true;
                break;
            }
        }

        if (!entregue) {
            mensagensPendentes.computeIfAbsent(destinatario, k -> new ArrayList<>())
                    .add("[Privado de " + usuario.getNome() + "]: " + mensagem);
        }

        send("Mensagem para " + destinatario + (entregue ? " entregue." : " salva como pendente."));
    }

    private void handleCriarGrupo(String nomeGrupo) throws IOException {
        if (usuario == null) {
            send("Você precisa estar logado.");
            return;
        }
        if (ChatServer.grupos.containsKey(nomeGrupo)) {
            send("Grupo já existe.");
        } else {
            Set<ClientHandler> membros = Collections.synchronizedSet(new HashSet<>());
            membros.add(this);
            ChatServer.grupos.put(nomeGrupo, membros);
            send("Grupo '" + nomeGrupo + "' criado e você foi adicionado.");
        }
    }

    private void handleMensagemGrupo(String dados) throws IOException {
        if (usuario == null) {
            send("Você precisa estar logado.");
            return;
        }
        String[] partes = dados.split(" ", 2);
        String grupo = partes[0];
        String mensagem = partes[1];
        if (!ChatServer.grupos.containsKey(grupo)) {
            send("Grupo não encontrado.");
            return;
        }
        if (!ChatServer.grupos.get(grupo).contains(this)) {
            send("Você não está neste grupo.");
            return;
        }
        ChatServer.enviarParaGrupo(grupo, mensagem, this);
    }

    public String getNomeUsuario() {
        return usuario != null ? usuario.getNome() : "Anon";
    }
    private void handleConvidarGrupo(String dados) throws IOException {
        String[] partes = dados.split(" ", 2);
        String grupo = partes[0];
        String nomeConvidado = partes[1];

        if (!ChatServer.grupos.containsKey(grupo)) {
            send("Grupo não existe.");
            return;
        }

        Set<ClientHandler> membros = ChatServer.grupos.get(grupo);
        if (!membros.contains(this)) {
            send("Você não faz parte do grupo.");
            return;
        }

        ClientHandler convidado = null;
        for (ClientHandler c : ChatServer.clients) {
            if (c.usuario != null && c.usuario.getNome().equalsIgnoreCase(nomeConvidado)) {
                convidado = c;
                break;
            }
        }

        if (convidado == null) {
            send("Usuário não encontrado ou não está online.");
            return;
        }



        String chaveConvite = grupo + ":" + nomeConvidado;
        ChatServer.convitesPendentes.computeIfAbsent(nomeConvidado, k -> new ArrayList<>())
                .add(new ChatServer.Convite(grupo, this, convidado, membros.size() > 1 ? new HashSet<>(membros) : Collections.emptySet()));

        convidado.sendMessage("Você foi convidado para entrar no grupo '" + grupo + "'. Para aceitar, digite: aceitarconvite " + grupo);
        send("Convite enviado para " + nomeConvidado + ". Aguardando aceite pessoal.");
    }

    private void handleRespostaConvite(String dados, boolean aceitar) throws IOException {
        String[] partes = dados.split(" ", 2);
        String grupo = partes[0];
        String nomeConvidado = partes[1];

        List<ChatServer.Convite> convites = ChatServer.convitesPendentes.getOrDefault(nomeConvidado, new ArrayList<>());
        ChatServer.Convite alvo = null;

        for (ChatServer.Convite c : convites) {
            if (c.grupo.equals(grupo) && c.aguardando.contains(this)) {
                alvo = c;
                break;
            }
        }

        if (alvo == null) {
            send("Nenhum convite pendente encontrado.");
            return;
        }

        alvo.aguardando.remove(this);
        send("Resposta registrada. Ainda aguardando: " + alvo.aguardando.size());

        if (!aceitar) {
            convites.remove(alvo);
            alvo.convidado.sendMessage("Sua entrada no grupo '" + grupo + "' foi negada.");
            return;
        }

        if (alvo.aguardando.isEmpty()) {
            ChatServer.grupos.get(grupo).add(alvo.convidado);
            alvo.convidado.salvarParticipacaoGrupo(grupo);
            convites.remove(alvo);
            alvo.convidado.sendMessage("Você agora faz parte do grupo '" + grupo + "'.");
            ChatServer.enviarParaGrupo(grupo, "Usuário " + nomeConvidado + " entrou no grupo.", alvo.convidado);
        }
    }
    private void handleSairGrupo(String nomeGrupo) throws IOException {
        if (!ChatServer.grupos.containsKey(nomeGrupo)) {
            send("Grupo não existe.");
            return;
        }

        Set<ClientHandler> membros = ChatServer.grupos.get(nomeGrupo);
        if (!membros.contains(this)) {
            send("Você não está no grupo.");
            return;
        }

        // Envia notificação antes de remover
        for (ClientHandler membro : new HashSet<>(membros)) {
            if (membro != this) {
                membro.sendMessage("[Grupo: " + nomeGrupo + "] Usuário " + getNomeUsuario() + " saiu do grupo.");
            }
        }

        membros.remove(this);
        send("Você saiu do grupo '" + nomeGrupo + "'.");
    }

    private void handleMsgGrupoPrivado(String dados) throws IOException {
        String[] partes = dados.split(" ", 3);
        if (partes.length < 3) {
            send("Uso: msgprivgrupo <grupo> <destinos_separados_por_virgula> <mensagem>");
            return;
        }
        String grupo = partes[0];
        String[] destinatarios = partes[1].split(",");
        String mensagem = partes[2];

        List<String> listaDestinos = Arrays.asList(destinatarios);
        ChatServer.enviarParaGrupoSelecionados(grupo, mensagem, this, listaDestinos);
    }

    private void handleSolicitarEntradaGrupo(String grupo) throws IOException {
        if (!ChatServer.grupos.containsKey(grupo)) {
            send("Grupo não existe.");
            return;
        }

        Set<ClientHandler> membros = ChatServer.grupos.get(grupo);
        if (membros.contains(this)) {
            send("Você já está no grupo.");
            return;
        }

        Set<ClientHandler> aguardando = new HashSet<>(membros);
        ChatServer.Convite convite = new ChatServer.Convite(grupo, this, this, aguardando);
        ChatServer.convitesPendentes.computeIfAbsent(usuario.getNome(), k -> new ArrayList<>()).add(convite);

        for (ClientHandler m : aguardando) {
            m.sendMessage("[Grupo: " + grupo + "] Usuário " + usuario.getNome() + " solicitou entrada. Responda com: aceitar " + grupo + " " + usuario.getNome() + " ou recusar " + grupo + " " + usuario.getNome());
        }
        send("Solicitação enviada. Aguardando aprovação dos membros do grupo.");
    }

    private void handleAceitarConvite(String grupo) throws IOException {
        if (!ChatServer.grupos.containsKey(grupo)) {
            send("Grupo não existe.");
            return;
        }

        List<ChatServer.Convite> pendentes = ChatServer.convitesPendentes.getOrDefault(usuario.getNome(), new ArrayList<>());
        ChatServer.Convite conviteAlvo = null;
        for (ChatServer.Convite c : pendentes) {
            if (c.grupo.equals(grupo)) {
                conviteAlvo = c;
                break;
            }
        }

        if (conviteAlvo == null) {
            send("Nenhum convite encontrado para este grupo.");
            return;
        }

        if (conviteAlvo.aguardando.isEmpty()) {
            // Grupo tem 1 membro apenas → entrada imediata
            ChatServer.grupos.get(grupo).add(this);
            salvarParticipacaoGrupo(grupo);
            pendentes.remove(conviteAlvo);
            send("Você entrou no grupo '" + grupo + "'.");
            ChatServer.enviarParaGrupo(grupo, "Usuário " + usuario.getNome() + " entrou no grupo.", this);
        } else {
            // Grupo com 2+ membros → agora aguarda aprovação dos membros
            for (ClientHandler m : conviteAlvo.aguardando) {
                m.sendMessage("[Grupo: " + grupo + "] Aceita entrada de " + usuario.getNome() + "? Responda com: aceitar " + grupo + " " + usuario.getNome());
            }
            send("Convite aceito. Aguardando aprovação dos membros do grupo.");
        }
    }

    private void handleAceitePrivado(String nome) throws IOException {
        if (usuario == null) {
            send("Você precisa estar logado.");
            return;
        }
        String chave = usuario.getNome() + "->" + nome;
        aceitePrivado.add(chave);
        send("Você aceitou receber mensagens de " + nome);

        // Persistir aceite
        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement("INSERT OR IGNORE INTO aceite_privado (remetente, destinatario) VALUES (?, ?);")) {
            stmt.setString(1, usuario.getNome());
            stmt.setString(2, nome);
            stmt.executeUpdate();
        } catch (SQLException e) {
            send("Erro ao salvar aceite: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void handleListarUsuarios() throws IOException {
        StringBuilder sb = new StringBuilder("Usuários online:\n");
        for (ClientHandler c : ChatServer.clients) {
            if (c.usuario != null) {
                sb.append("- ").append(c.usuario.getNome()).append(" (status: ").append(c.usuario.getStatus()).append(")\n");
            }
        }
        send(sb.toString());
    }
}
