package model;

public class Usuario {
    private String nome, login, email, senha, status;

    public Usuario(String nome, String login, String email, String senha, String status) {
        this.nome = nome;
        this.login = login;
        this.email = email;
        this.senha = senha;
        this.status = status;
    }

    public String getNome() { return nome; }
    public String getLogin() { return login; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
