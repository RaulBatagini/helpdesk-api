# HelpDesk API

API REST para gerenciamento de chamados de suporte técnico, desenvolvida em Java como trabalho acadêmico de testes unitários automatizados.

> Este README está em construção. As seções marcadas com 🚧 serão preenchidas ao longo do desenvolvimento.

## Descrição

O sistema permite cadastrar usuários (solicitantes e técnicos), abrir chamados, atribuir técnicos, atualizar status e prioridade, consultar chamados e acompanhar o histórico de atendimento. Não há frontend: toda a demonstração é feita pelo Swagger.

## Tecnologias

| Item | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1.2 |
| Dependências | Maven |
| Banco de dados | MySQL 8 |
| Documentação da API | Swagger / OpenAPI (Springdoc) |
| Testes | TestNG + EasyMock + Hamcrest |
| Integração contínua | GitHub Actions 🚧 |

## Estrutura do projeto

```
helpdesk/
├── .github/workflows/        CI (GitHub Actions) 🚧
├── src/
│   ├── main/
│   │   ├── java/br/helpdesk/helpdesk/
│   │   │   ├── HelpdeskApplication.java
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── model/
│   │   │   │   └── enums/
│   │   │   ├── dto/
│   │   │   └── exception/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/br/helpdesk/helpdesk/
│           └── service/
├── .env.example
├── .gitignore
├── pom.xml
└── README.md
```

> Regra importante: todas as pastas de código ficam **dentro** do pacote `br.helpdesk.helpdesk`, onde está a classe `HelpdeskApplication`. Fora dele, o Spring não encontra as classes.

### O que vai em cada pasta

| Pasta | O que deve ficar aqui | Exemplo |
|---|---|---|
| `controller/` | Recebe as requisições HTTP (endpoints), chama o Service e devolve a resposta. **Não** tem regra de negócio. | `UsuarioController`, `ChamadoController` |
| `service/` | **Regras de negócio** e validações (RN01 a RN16). É a camada mais testada. | `UsuarioService`, `ChamadoService` |
| `repository/` | Acesso ao banco de dados. Interfaces que estendem `JpaRepository`. | `UsuarioRepository`, `ChamadoRepository` |
| `model/` | Entidades: classes que viram tabelas no banco. | `Usuario`, `Chamado`, `HistoricoChamado` |
| `model/enums/` | Conjuntos fixos de valores permitidos (veja a seção abaixo). | `Perfil`, `Categoria`, `Prioridade`, `StatusChamado` |
| `dto/` | Objetos que carregam dados de entrada e saída da API, sem expor as entidades diretamente. | `UsuarioRequest`, `ChamadoResponse` |
| `exception/` | Exceções de negócio e o tratador global que converte erros em respostas HTTP (404, 400, 409). | `RecursoNaoEncontradoException`, `GlobalExceptionHandler` |
| `src/test/.../service/` | Testes unitários do Service (TestNG + EasyMock + Hamcrest). | `UsuarioServiceTest`, `ChamadoServiceTest` |
| `resources/` | Configurações da aplicação. | `application.properties` |

### Caminho de uma requisição

```
Cliente (Swagger) → Controller → Service → Repository → MySQL
                        ↑            ↑
                       DTO     regras de negócio
```

## O que são Enums

Um **enum** é um tipo do Java com um **conjunto fixo de valores permitidos**. Ele impede valores inventados ou escritos errados (por exemplo, um status "FINALIZADOOO").

```java
public enum StatusChamado {
    ABERTO, EM_ATENDIMENTO, RESOLVIDO, FECHADO
}
```

Enums do projeto:

| Enum | Valores | Regra de negócio |
|---|---|---|
| `Perfil` | SOLICITANTE, TECNICO | RN04 |
| `Categoria` | HARDWARE, SOFTWARE, REDE, OUTROS | RN07 |
| `Prioridade` | BAIXA, MEDIA, ALTA, CRITICA | RN08 |
| `StatusChamado` | ABERTO, EM_ATENDIMENTO, RESOLVIDO, FECHADO | RN09, RN12 |

Ficam na pasta `model/enums/` apenas por organização; o Java aceitaria qualquer outro nome de pasta.

## Configuração do banco de dados

O Git **não** guarda o banco, só o código. Cada pessoa usa o MySQL da própria máquina.

1. Instale e inicie o MySQL 8 localmente.
2. Se o seu usuário e senha forem `root` / `root`, não precisa fazer mais nada.
3. Se forem diferentes, crie o arquivo `.env` (veja a seção abaixo).
4. Rode a aplicação. O schema `helpdesk` e as tabelas são criados automaticamente.

## O arquivo `.env`

O `.env` guarda **credenciais pessoais** (usuário e senha do seu MySQL). Ele existe porque a senha de cada integrante pode ser diferente, e senhas **nunca** devem ir para o GitHub.

| Arquivo | Vai para o Git? | Função |
|---|---|---|
| `.env` | **Não** (está no `.gitignore`) | Suas credenciais reais |
| `.env.example` | **Sim** | Modelo que mostra quais variáveis existem |

Como criar o seu:

```bash
# Linux / macOS / Git Bash
cp .env.example .env
```

No Windows (CMD): `copy .env.example .env`. Depois abra o `.env` e preencha:

```
DB_USER=root
DB_PASSWORD=sua_senha_do_mysql
```

O `application.properties` lê essas variáveis (`${DB_USER:root}` e `${DB_PASSWORD:root}`). Se o `.env` não existir, valem os valores padrão `root` / `root`. Execute a aplicação a partir da **raiz do projeto**, porque é lá que o Spring procura o `.env`.

## Como executar

```bash
mvn spring-boot:run
```

## Documentação Swagger

Com a aplicação rodando, acesse: http://localhost:8080/swagger-ui.html

## Testes automatizados

Framework: **TestNG** (execução), **EasyMock** (simula os repositories) e **Hamcrest** (asserções legíveis).

```bash
mvn test
```

Os testes são unitários, focados na camada Service, e **não dependem de banco de dados**. 🚧 A descrição de cada teste (cenário, entradas e saída esperada) será adicionada aqui conforme forem implementados.

## Git Flow

| Branch | Função |
|---|---|
| `main` | Versão estável, pronta para entrega |
| `develop` | Integração das funcionalidades aprovadas |
| `feature/USxxx-nome` | Uma branch por User Story, criada a partir de `develop` e integrada por Pull Request |
| `release/x.y.z` | Preparação da versão final, criada a partir de `develop` |
| `hotfix/nome` | Correção urgente, criada a partir de `main` |

Padrão de commits: `feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`.

## User Stories

| ID | User Story | Prioridade |
|---|---|---|
| US001 | Cadastrar usuário | Alta |
| US002 | Abrir chamado | Alta |
| US003 | Atribuir técnico ao chamado | Alta |
| US004 | Atualizar status do chamado | Alta |
| US005 | Consultar chamados | Média |
| US006 | Alterar prioridade | Média |
| US007 | Consultar histórico do chamado | Baixa |

Plano dos 8 testes
#	US	Cenário (a pergunta que o teste responde)	Entrada	Saída esperada	Técnica

1	001	E-mail repetido com maiúsculas e espaços conta como duplicado?	já existe ana@email.com, tento " ANA@Email.com "	erro 409, nenhum save	verify sem expect de save
2	002	O chamado é gravado na ordem certa e o histórico aponta para ele?	solicitante válido, prioridade null	busca usuário, salva chamado, salva histórico (ABERTO, mesmo chamado), prioridade MEDIA	createStrictMock + Capture
3	003	Atribuir técnico altera mais de uma coisa?	chamado ABERTO + usuário TECNICO	técnico gravado, status EM_ATENDIMENTO, 1 histórico novo	Capture + allOf
4	004	Só as transições permitidas funcionam?	as 16 combinações origem/destino	permitidas atualizam e gravam histórico; as outras dão erro 400 e não alteram nada	@DataProvider
5	004	Transição inválida deixa lixo no banco?	ABERTO → FECHADO	erro 400, nenhum save de chamado nem de histórico	verify sem expect
6	005	O filtro por status devolve só os chamados certos?	3 chamados (2 EM_ATENDIMENTO, 1 ABERTO), filtro EM_ATENDIMENTO	lista de 2, todos EM_ATENDIMENTO	hasSize + everyItem
7	006	O histórico guarda prioridade antiga e nova? Trocar pela mesma grava algo?	MEDIA → ALTA, depois ALTA → ALTA	1º grava histórico com as duas; 2º não grava nada	Capture + verify
8	007	“Chamado inexistente” e “sem histórico” são tratados diferente?	id 99 / chamado sem registros	404 / lista vazia sem erro	expectThrows + empty()