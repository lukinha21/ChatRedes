ChatRedes
📡 Descrição
ChatRedes é um sistema de chat com múltiplos clientes desenvolvido em Java, utilizando Sockets e Threads para gerenciar comunicações simultâneas. O projeto foi desenvolvido como parte da disciplina Redes de Computadores II, sob orientação do Professor Robson Siscoutto na UNOESTE.

O sistema implementa comunicação cliente-servidor, com suporte a chats privados, em grupo, gerenciamento de status, autenticação de usuários e armazenamento de dados via banco de dados.

🎯 Funcionalidades
✅ Cadastro de usuários: nome completo, login, email e senha (sem duplicidade de nomes).
✅ Sistema de Login e Logout.
✅ Recuperação de senha mediante confirmação via e-mail.
✅ Alteração de status: online, offline, ocupado, etc.
✅ Listagem de grupos e usuários online.
✅ Mensagens privadas mediante aceite prévio.
✅ Criação de grupos, adição de usuários mediante convite e aceite.
✅ Comunicação em grupo com identificação de remetente e horário.
✅ Saída de grupo notificada aos demais membros.
✅ Solicitação de entrada em grupos mediante aprovação unânime.
✅ Envio de mensagens para um ou mais usuários, individual ou em grupo.
✅ Encaminhamento automático de mensagens não entregues quando usuário estiver online.
✅ Arquitetura multi-threaded: servidor aceita múltiplas conexões simultâneas.

🏗️ Arquitetura do Sistema
Servidor (Java Socket):

Thread principal para escutar novas conexões.

Nova thread para cada cliente conectado.

Cliente (Java Socket):

Thread para envio de mensagens.

Thread para recebimento de mensagens.

Banco de Dados:

Implementado com SQLite via JDBC.

Tabelas para usuários, grupos, mensagens e status.

IDE utilizada: IntelliJ IDEA

Documentação completa: incluída na pasta, em formato PDF.

🛠️ Tecnologias Utilizadas
Java SE

Java Socket API

Java Threads

JDBC (para acesso ao banco de dados)

IntelliJ IDEA (como ambiente de desenvolvimento)

🚀 Como Executar o Projeto
✅ Pré-requisitos
Java JDK 18 ou superior.

IntelliJ IDEA (ou outra IDE de sua preferência).

Banco de dados configurado conforme especificado na documentação PDF.

Bibliotecas adicionais incluídas ou listadas na documentação.

🔧 Passos
Clone o repositório:

git clone https://github.com/seuusuario/ChatRedes.git
Abra o projeto no IntelliJ IDEA:

File > Open > selecione a pasta do projeto.

Configure o banco de dados conforme instruções no PDF de documentação.

Compile o projeto:

Build > Build Project.

Execute o servidor:

Localize a classe Servidor.java e execute-a.

Execute os clientes:

Localize a classe Cliente.java e execute quantas instâncias desejar.

🗂️ Estrutura do Projeto
ChatRedes/
├── src/
│   ├── cliente/
│   │   └── Cliente.java
│   ├── servidor/
│   │   └── Servidor.java
│   ├── modelos/
│   │   ├── Usuario.java
├── database/
│   └── script.sql
├── doc/
│   └── Documentacao_ChatRedes.pdf
├── README.md
└── .gitignore

📝 Documentação
A documentação completa, contendo:

Diagramas

Modelagem do banco de dados

Descrição das classes

Fluxos de mensagens

Prints de execução

está disponível no arquivo:

/doc/Documentacao_ChatRedes.pdf

⚙️ Principais Comandos no Chat incluida na Documentação do projeto

👥 Contribuidores

Lucas Alexandre
Beatriz Araújo

🎓 Orientador
Professor Robson Siscoutto
University of the West of São Paulo - UNOESTE

