# HelpDesk API

API REST para gerenciamento de chamados de suporte técnico, desenvolvida em Java como trabalho acadêmico da disciplina de Gestão de Qualidade, com foco em **testes unitários automatizados**.

## Descrição

O sistema permite cadastrar usuários (solicitantes e técnicos), abrir chamados, atribuir técnicos, atualizar status e prioridade, consultar chamados e acompanhar o histórico de atendimento. Não há frontend: toda a demonstração é feita pela interface do Swagger.

**Objetivo da aplicação:** oferecer um domínio pequeno, mas com regras de negócio reais (validações, transições de status, perfis de usuário e histórico), que permita demonstrar a importância dos testes automatizados na qualidade do software.

## Tecnologias

| Item | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1.2 |
| Dependências | Maven |
| Banco de dados | MySQL 8 |
| Documentação da API | Swagger / OpenAPI (Springdoc) |
| Testes | TestNG + EasyMock + Hamcrest |
| Integração contínua | GitHub Actions |
| Versionamento | Git Flow + GitHub |
| Gestão de tarefas | Kanban no Trello |

## Estrutura do projeto

```
helpdesk/
├── .github/workflows/        Pipeline de CI (GitHub Actions)
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

> Todas as pastas de código ficam **dentro** do pacote `br.helpdesk.helpdesk`, onde está a classe `HelpdeskApplication`. Fora dele, o Spring não encontra as classes.

### O que vai em cada pasta

| Pasta | O que fica aqui | Exemplo |
|---|---|---|
| `controller/` | Recebe as requisições HTTP, chama o Service e devolve a resposta. Sem regra de negócio. | `UsuarioController`, `ChamadoController` |
| `service/` | Regras de negócio e validações (RN01 a RN16). É a camada mais testada. | `UsuarioService`, `ChamadoService` |
| `repository/` | Acesso ao banco. Interfaces que estendem `JpaRepository`. | `UsuarioRepository`, `ChamadoRepository` |
| `model/` | Entidades: classes que viram tabelas no banco. | `Usuario`, `Chamado`, `HistoricoChamado` |
| `model/enums/` | Conjuntos fixos de valores permitidos. | `Perfil`, `Categoria`, `Prioridade`, `StatusChamado` |
| `dto/` | Objetos de entrada e saída da API, sem expor as entidades. | `UsuarioRequest`, `UsuarioResponse` |
| `exception/` | Exceções de negócio e o tratador global que as converte em respostas HTTP. | `RegraNegocioException`, `GlobalExceptionHandler` |
| `src/test/.../service/` | Testes unitários do Service. | `UsuarioServiceTest`, `ChamadoServiceTest` |
| `resources/` | Configurações da aplicação. | `application.properties` |

### Caminho de uma requisição

```
Cliente (Swagger) → Controller → Service → Repository → MySQL
                        ↑            ↑
                       DTO     regras de negócio
```

### O que são Enums

Um **enum** é um tipo do Java com um **conjunto fixo de valores permitidos**. Ele impede valores inventados ou escritos errados (por exemplo, um status "FINALIZADOOO").

| Enum | Valores | Regra |
|---|---|---|
| `Perfil` | SOLICITANTE, TECNICO | RN04 |
| `Categoria` | HARDWARE, SOFTWARE, REDE, OUTROS | RN07 |
| `Prioridade` | BAIXA, MEDIA, ALTA, CRITICA | RN08 |
| `StatusChamado` | ABERTO, EM_ATENDIMENTO, RESOLVIDO, FECHADO | RN09, RN12 |

## Instalação e execução

### Pré-requisitos

- Java 21
- MySQL 8 instalado e em execução
- Git

### 1. Clonar o repositório

```bash
git clone https://github.com/<USUARIO>/<REPOSITORIO>.git
cd <REPOSITORIO>
```

### 2. Configurar o banco de dados

O Git **não** guarda o banco, só o código. Cada pessoa usa o MySQL da própria máquina.

- Se o seu MySQL usa usuário `root` e senha `root`, não precisa configurar nada.
- Se forem diferentes, crie o arquivo `.env` (próxima seção).

O schema `helpdesk` e as tabelas são criados automaticamente na primeira execução.

### 3. O arquivo `.env`

O `.env` guarda **credenciais pessoais** do banco. Ele **não** vai para o GitHub (está no `.gitignore`). O que vai é o `.env.example`, um modelo com as variáveis necessárias.

```bash
# Linux / macOS / Git Bash
cp .env.example .env

# Windows (CMD)
copy .env.example .env
```

Depois edite o `.env`:

```
DB_USER=root
DB_PASSWORD=sua_senha_do_mysql
```

O `application.properties` lê essas variáveis (`${DB_USER:root}` e `${DB_PASSWORD:root}`). Sem `.env`, valem os valores padrão `root` / `root`. Execute a aplicação sempre a partir da **raiz do projeto**, que é onde o Spring procura o `.env`.

### 4. Executar a aplicação

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS / Git Bash
./mvnw spring-boot:run
```

(Se tiver o Maven instalado, `mvn spring-boot:run` também funciona.)

## Documentação da API (Swagger)

Com a aplicação rodando, acesse:

**http://localhost:8080/swagger-ui.html**

Pelo Swagger é possível visualizar e executar todas as requisições.

### Endpoints

| Método | Rota | User Story | Descrição |
|---|---|---|---|
| POST | `/usuarios` | US001 | Cadastra um usuário |
| POST | `/chamados` | US002 | Abre um chamado |
| PUT | `/chamados/{id}/atribuicao` | US003 | Atribui um técnico ao chamado |
| PATCH | `/chamados/{id}/status` | US004 | Atualiza o status do chamado |
| GET | `/chamados` | US005 | Lista chamados (filtro opcional `?status=`) |
| GET | `/chamados/{id}` | US005 | Consulta um chamado |
| PATCH | `/chamados/{id}/prioridade` | US006 | Altera a prioridade do chamado |
| GET | `/chamados/{id}/historico` | US007 | Consulta o histórico do chamado |

Exemplo de corpo para `POST /usuarios`:

```json
{
  "nome": "Ana",
  "email": "ana@email.com",
  "perfil": "SOLICITANTE"
}
```
Rodas testes
```json
{
  .\mvnw.cmd test
}
```
### Respostas de erro

| Código | Quando acontece |
|---|---|
| 400 | Regra de negócio violada, campo obrigatório ausente ou valor inválido |
| 404 | Usuário ou chamado não encontrado |
| 409 | Conflito, como e-mail já cadastrado |

## Regras de negócio

| ID | Regra |
|---|---|
| RN01 | Nome obrigatório |
| RN02 | E-mail com formato válido |
| RN03 | E-mail único no sistema |
| RN04 | Perfil deve ser SOLICITANTE ou TECNICO |
| RN05 | Só usuário SOLICITANTE pode abrir chamado |
| RN06 | Título e descrição obrigatórios |
| RN07 | Categoria: HARDWARE, SOFTWARE, REDE ou OUTROS |
| RN08 | Prioridade: BAIXA, MEDIA, ALTA ou CRITICA (padrão: MEDIA) |
| RN09 | Todo chamado nasce com status ABERTO |
| RN10 | Só usuário TECNICO pode ser atribuído a um chamado |
| RN11 | Ao atribuir técnico, o status passa de ABERTO para EM_ATENDIMENTO |
| RN12 | Fluxo permitido: ABERTO → EM_ATENDIMENTO → RESOLVIDO → FECHADO |
| RN13 | Não é permitido pular etapas nem retroceder status |
| RN14 | Chamado FECHADO não pode ser alterado |
| RN15 | Não é possível alterar a prioridade de chamado FECHADO |
| RN16 | Toda mudança gera registro no histórico |

## Testes automatizados

**Frameworks:**
- **TestNG**: estrutura e execução dos testes (`@Test`, `@BeforeMethod`, `@DataProvider`).
- **EasyMock**: simula os repositories, para testar o Service isolado, sem banco de dados.
- **Hamcrest**: asserções legíveis, como `assertThat(x, is(y))`.

Os testes são **unitários**, focados na camada Service, e não dependem de banco, servidor ou rede.

### Como executar

```bash
# Windows
.\mvnw.cmd test

# Linux / macOS / Git Bash
./mvnw test
```

Resultado esperado: `BUILD SUCCESS` com `Failures: 0, Errors: 0`.

### Testes implementados

Os testes seguem a estrutura **Arrange / Act / Assert**.

| # | US | Cenário | Entrada | Saída esperada | Técnica |
|---|---|---|---|---|---|
| 1 | US001 | E-mail repetido, com maiúsculas e espaços, conta como duplicado? | Já existe `ana@email.com`; tenta cadastrar `" ANA@Email.com "` | Erro 409 e nenhum `save` | `verify` sem `expect` de `save` |
| 2 | US002 | O chamado é gravado na ordem certa e o histórico aponta para ele? | Solicitante válido, prioridade `null` | Busca o usuário, salva o chamado, salva o histórico (ABERTO, mesmo chamado) e a prioridade vira MEDIA | `createStrictMock` + `Capture` |
| 3 | US003 | Atribuir técnico altera mais de uma coisa? | Chamado ABERTO + usuário TECNICO | Técnico gravado, status EM_ATENDIMENTO e 1 histórico novo | `Capture` + `allOf` |
| 4 | US004 | Só as transições permitidas funcionam? | As 16 combinações origem/destino | Permitidas atualizam e gravam histórico; as demais dão erro 400 e não alteram nada | `@DataProvider` |
| 5 | US004 | Transição inválida deixa lixo no banco? | ABERTO → FECHADO | Erro 400, sem `save` de chamado nem de histórico | `verify` sem `expect` |
| 6 | US005 | O filtro por status devolve só os chamados certos? | 3 chamados (2 EM_ATENDIMENTO, 1 ABERTO), filtro EM_ATENDIMENTO | Lista com 2 itens, todos EM_ATENDIMENTO | `hasSize` + `everyItem` |
| 7 | US006 | O histórico guarda a prioridade antiga e a nova? Trocar pela mesma grava algo? | MEDIA → ALTA, depois ALTA → ALTA | O 1º grava histórico com as duas prioridades; o 2º não grava nada | `Capture` + `verify` |
| 8 | US007 | "Chamado inexistente" e "sem histórico" são tratados de forma diferente? | ID 99 / chamado sem registros | 404 / lista vazia sem erro | `expectThrows` + `empty()` |

Cada teste cobre pelo menos uma regra de negócio, e o conjunto abrange cenários de sucesso, entradas inválidas, campos obrigatórios, tratamento de erros e situações de exceção.

## Integração contínua

O GitHub Actions executa os testes automaticamente a cada push e a cada Pull Request nas branches principais. O workflow fica em `.github/workflows/`. O resultado aparece na aba **Actions** do repositório e no próprio Pull Request.

## Git Flow

| Branch | Função |
|---|---|
| `main` | Versão estável, pronta para entrega |
| `develop` | Integração das funcionalidades aprovadas |
| `feature/USxxx-nome` | Uma branch por User Story, criada a partir de `develop` e integrada por Pull Request |
| `release/x.y.z` | Preparação da versão final, criada a partir de `develop` |
| `hotfix/nome` | Correção urgente, criada a partir de `main` |

Branches de feature utilizadas:

- `feature/setup-projeto-base`
- `feature/US001-cadastro-usuario`
- `feature/US002-abertura-chamado`
- `feature/US003-atribuicao-tecnico`
- `feature/US004-atualizacao-status`
- `feature/US005-consulta-chamados`
- `feature/US006-alteracao-prioridade`
- `feature/US007-historico-chamado`

**Padrão de commits:** `feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`.

Todas as integrações em `develop` foram feitas por Pull Request, com revisão de outro integrante.

## Gestão do projeto (Kanban)

As tarefas foram organizadas no Trello, com cada User Story em um card contendo objetivo, usuário, fluxo esperado, checklist de regras de negócio e critérios de aceitação. Colunas: Product Backlog, To Do, In Progress, Testing e Done.

| ID | User Story | Prioridade |
|---|---|---|
| US001 | Cadastrar usuário | Alta |
| US002 | Abrir chamado | Alta |
| US003 | Atribuir técnico ao chamado | Alta |
| US004 | Atualizar status do chamado | Alta |
| US005 | Consultar chamados | Média |
| US006 | Alterar prioridade | Média |
| US007 | Consultar histórico do chamado | Baixa |

## Equipe

| Integrante     |
|----------------|
| Raul Batagini  |
| Luis Felipe    |
| Maicon Douglas |
| Diego Freitas  |