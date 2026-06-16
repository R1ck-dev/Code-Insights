# CLAUDE.md

Este arquivo orienta o Claude Code (claude.ai/code) ao trabalhar neste repositório.

## O que é o CodeInsights

CodeInsights é uma **plataforma web de gestão de conhecimento, portfólio de código e análise de
métricas de aprendizado em programação**. É o objeto de uma Iniciação Científica (PIBIFSP / IFSP).

Diferente de um simples portfólio, o objetivo central é **científico**: investigar o processo de
aprendizado prático de programação a partir de métricas extraídas das resoluções dos alunos —
notadamente o **impacto da IA generativa na autonomia** e a **evolução da complexidade algorítmica
(Big O)**. A plataforma é, ao mesmo tempo, a ferramenta e o instrumento empírico da pesquisa.

A descrição completa do projeto está em [`Modelo de Projeto - CodeInsights.pdf`](Modelo%20de%20Projeto%20-%20CodeInsights.pdf)
(resumo, fundamentação teórica, objetivos, metodologia e cronograma). Leia-o antes de tomar decisões
de produto/arquitetura — ele define o porquê de cada funcionalidade.

### Métricas de aprendizado (núcleo da pesquisa)

O design **precisa ser modular/extensível** para novas métricas — este é um requisito explícito dos
objetivos específicos, não um detalhe. As métricas iniciais são:

- **Índice de Autonomia IA** — escala inteira de **1 a 5** medindo quanto da solução foi feita de
  forma autônoma vs. apoiada/gerada por IA. Quanto menor a dependência, maior a autonomia.
- **Complexidade algorítmica** — não existe algoritmo universal que infira a notação Big O exata de um
  código arbitrário (no caso geral, é indecidível). Por isso a métrica original de "Big O" é
  **decomposta em três indicadores objetivos e calculáveis** a partir do código submetido, que juntos
  mapeiam a curva de amadurecimento algorítmico do aluno (ex.: força bruta → estruturas refinadas):
  - **Análise de Big O via AST** — estimativa da complexidade de **tempo** a partir da Árvore Sintática
    Abstrata da solução (aninhamento de laços, recursão, chamadas), por análise estática e não execução.
  - **Complexidade Ciclomática (McCabe)** — número de caminhos independentes do fluxo de controle, como
    proxy de complexidade estrutural/ramificação da solução.
  - **Complexidade de Espaço** — uso de memória da solução (estruturas auxiliares, recursão), também
    estimado a partir da AST.
- **Snippets categorizados** — trechos de código reutilizáveis com categoria de conceito, como prática
  de gestão de conhecimento pessoal e metacognição.

Ao adicionar uma nova métrica, prefira soluções que não exijam reescrever o modelo existente: a
extração e análise de métricas deve crescer por adição, não por modificação.

### Implicações de pesquisa que afetam o código

- **Sujeitos humanos / ética**: a coleta de dados depende de aprovação do Comitê de Ética do IFSP.
- **Anonimização**: a análise empírica usa **relatórios anonimizados** extraídos do banco. Mantenha a
  separação entre dados identificáveis (usuário) e dados de métrica/solução analisáveis em mente ao
  modelar o schema e os endpoints de exportação.
- **Público-alvo do piloto**: grupos de estudo prático (ex.: clube de programação competitiva).

## Estado atual do repositório

Este diretório está **vazio** (apenas o PDF do projeto e este CLAUDE.md). O código ainda não começou.

Existem **duas bases de referência** (consultar, **não** copiar cegamente):

- **IFConecta** — `C:\Users\henri\OneDrive\Área de Trabalho\Geral\Projetos\IFConecta\ifconecta`, branch
  `main`. É a **referência primária de arquitetura e padrão de implementação**: a v2 segue o mesmo padrão
  (Spring Boot 4, Hexagonal/Clean *layer-first*, JWT com jjwt, Flyway, mapeamento manual via mappers).
  Ver a seção *Arquitetura* abaixo.
- **code-archive (v1)** — `C:\Users\henri\OneDrive\Área de Trabalho\Geral\Projetos\code-archive`.
  Pré-versão do próprio CodeInsights; serve como **referência de produto/domínio** (features, métricas,
  modelagem). Reavalie cada decisão à luz do foco em métricas de pesquisa.

Quando o código começar, a nomenclatura do projeto passa a ser **`codeinsights`** (pacote
`com.projeto.codeinsights`, nome do banco, containers, etc.), substituindo `code-archive`/`codearchive`.

## Stack planejada (referência IFConecta + v1; a confirmar quando o código iniciar)

- **Back-end**: Spring Boot **4** (Java 21), Maven wrapper (`mvnw`/`mvnw.cmd`), Lombok no build (fora do
  domínio). *Mantido — alinhado à v1 e ao IFConecta.*
- **Front-end**: React 19 + Vite + TypeScript, React Router 7, Tailwind CSS v4 (`@tailwindcss/vite`).
  *Mantido por enquanto — a melhor opção será reavaliada mais à frente.*
- **Banco**: PostgreSQL, schema versionado por **Flyway**; Hibernate em `ddl-auto: validate` e
  `open-in-view: false`.
- **Autenticação**: JWT stateless. Biblioteca **`jjwt` (`io.jsonwebtoken`, HMAC-SHA)**, no padrão do
  IFConecta. *Correção em relação à v1, que usava auth0 `java-jwt`.*
- **Docs de API**: OpenAPI / Swagger UI (`springdoc-openapi`).
- **Docker**: será usado, mas **a forma de tratar o Docker ainda será definida**. Referência do IFConecta:
  `docker-compose` sobe apenas a **infra local** (Postgres [+ MailHog]) e o backend é empacotado por um
  `Dockerfile` próprio (deploy em nuvem), em vez de um único compose orquestrando todo o stack.
- **Idioma de domínio**: majoritariamente **português** no código e na documentação.

## Arquitetura (Hexagonal + Clean Architecture, padrão do IFConecta)

A v2 adota o **mesmo padrão de implementação do IFConecta** (branch `main`): Arquitetura Hexagonal /
Clean com forte sabor DDD, **empacotamento por camada (layer-first)** e **domínio completamente isolado**.

> **Mudança em relação à v1/code-archive**: a v1 empacotava **por bounded context** no primeiro nível
> (`identity/`, `knowledge/`) e só depois por camada. A v2 **inverte**: o **primeiro nível é a camada**
> (`application/`, `domain/`, `infrastructure/`) e o **segundo nível é o bounded context**.

### Estrutura de pacotes (sob `com.projeto.codeinsights`)

Três pacotes de topo, um por camada; dentro de cada um, subpacotes por bounded context. As dependências
apontam **sempre para dentro** (`infrastructure → application → domain`; o domínio não depende de
ninguém). Contextos-semente: **`identity`** (usuários, perfil, autenticação) e **`knowledge`** (desafios,
resoluções, snippets e métricas — núcleo do portfólio e da pesquisa). Novos contextos/métricas entram
por **adição**, não por modificação.

- `domain/<contexto>/` — **Java puro, sem nenhuma anotação** (nem Spring, nem JPA, nem Lombok).
  - `model/` — entidades e agregados de domínio (getters à mão, sem setters).
  - `port/` — **interfaces (portas)** que o domínio define e a infraestrutura implementa: tanto
    repositórios (`*Repository`) quanto serviços externos (`TokenServicePort`, `PasswordEncoderPort`,
    `EmailSenderPort`, …).
  - `enums/`, `exception/` — enums e exceções de negócio (`NegocioException`).
  - `domain/shared/` — tipos transversais de domínio (ex.: `Pagina<>` para paginação).
- `application/<contexto>/`
  - `usecase/` — **um caso de uso por operação**, classe `*UseCase`, `@Service`, com um único método
    público `execute(...)`. Orquestra domínio + portas; `@Transactional` nas operações de escrita.
  - `dto/` — **records**: `*Input` (entrada do caso de uso) e `*DTO` (saída/retorno do caso de uso).
- `infrastructure/`
  - `web/<contexto>/controller/` — `@RestController` (`/api/...`); o HTTP entra e sai aqui.
  - `web/<contexto>/dto/` — **records** de transporte HTTP: `*Request` (corpo da requisição, validado com
    `@Valid`) e `*Response` quando a resposta é específica de web (ex.: `TokenResponse`).
  - `persistence/<contexto>/entity/` — entidades JPA `*JpaEntity` (aqui ficam as anotações JPA).
  - `persistence/<contexto>/repository/` — `SpringData*Repository` (Spring Data, estende `JpaRepository`).
  - `persistence/<contexto>/adapter/` — `*RepositoryAdapter` (`@Component`) **implementa a porta** do
    domínio, delegando ao Spring Data + mapper.
  - `persistence/<contexto>/mapper/` — `*Mapper` (`@Component`) faz o **mapeamento manual domínio ↔ JPA**
    (`toEntity`/`toDomain`); usa `EntityManager.getReference(...)` para resolver relações a partir de IDs.
  - `config/security/`, `config/openapi/`, `config/exception/` — fiação Spring Security/JWT, OpenAPI e
    `GlobalExceptionHandler`. Adapters de portas **não-repositório** (JWT, BCrypt, e-mail) também vivem na
    infraestrutura (em `config/security` ou no `persistence/<contexto>/adapter` do contexto dono).

> **Não há camada `presentation/` separada** (como na v1): os controllers são detalhe de entrega e ficam
> em `infrastructure/web`.

### Convenções-chave

- **Domínio isolado e rico**: entidades são POJOs sem anotações; mudam estado por **métodos de
  comportamento** (ex.: `publish()`, `ativarConta()`, `solicitarEntrada(...)`), **nunca por setters**, e
  validam invariantes lançando `NegocioException` (ex.: Índice de Autonomia IA deve estar entre 1 e 5).
  Cada entidade tem **dois construtores**: um de **criação** (gera `UUID`, aplica defaults/invariantes) e
  um de **reconstituição** (estado completo, usado pelo mapper ao carregar do banco). Referências entre
  agregados são guardadas como **IDs** (`autorId`, `desafioId`), não como grafos de objetos.
- **Mapeamento domínio ↔ JPA é manual**, isolado no `*Mapper`; o domínio nunca conhece a entidade JPA.
- **Injeção por construtor apenas** (via Lombok `@RequiredArgsConstructor`; Lombok só **fora** do domínio).
- **Fluxo de requisição**: Controller recebe `*Request` (+ `@CurrentUserId UUID` do usuário autenticado)
  → monta o record `*Input` → chama `useCase.execute(input)` → o caso de uso usa domínio + portas →
  `*RepositoryAdapter` → `*Mapper` + Spring Data. A resposta volta como `*DTO`/`Pagina<*DTO>` (ou
  `*Response` quando específica de web).
- **Flyway é dono do schema**; Hibernate em `validate` (nunca gera DDL) e `open-in-view: false`. Mudanças
  entram como novos scripts versionados em `db/migration/` no estilo do IFConecta
  (`V1__Create__Usuario__Table.sql`, `V2__...sql`, …). **Nunca edite uma migration já aplicada.**
- **Configuração por perfis** (`application.yml` + `application-local.yml` / `application-prod.yml`).
  Datasource e segredos vêm de variáveis de ambiente com fallback local: `DB_USER`/`DB_PASSWORD` (URL
  JDBC por perfil), `JWT_SECRET` (**sem default** — obrigatório) e `CORS_ALLOWED_ORIGINS`.

### Segurança (padrão IFConecta)

- **JWT stateless com jjwt** (`io.jsonwebtoken`, HMAC-SHA): um `JwtAuthenticationFilter` roda **antes do
  `UsernamePasswordAuthenticationFilter`**, lê `Authorization: Bearer ...`, valida a assinatura e popula o
  `SecurityContext`. O token carrega as claims `id` (UUID do usuário) e `role`. A emissão/validação fica
  atrás da porta de domínio `TokenServicePort`, implementada por `JwtTokenAdapter` na infraestrutura.
- Controllers obtêm o usuário atual via anotação **`@CurrentUserId UUID`** (resolvida por um
  `HandlerMethodArgumentResolver`), e **não** via `authentication.getName()` (como fazia a v1).
- `SecurityConfig` define um `SecurityFilterChain`: **CSRF off, sessão STATELESS**, CORS ligado, rotas
  públicas via `permitAll` (registro, login, ativação, Swagger), `/api/admin/**` com `hasRole("ADMIN")`,
  o resto autenticado. Erros de auth/autorização saem em **JSON** (`JsonAuthenticationEntryPoint`,
  `JsonAccessDeniedHandler`). Senhas com `BCryptPasswordEncoder` (porta `PasswordEncoderPort`).
- O segredo do JWT vem de `api.security.jwt.secret` (env `JWT_SECRET`); **não há default inseguro** —
  defina-o sempre, sobretudo em produção/coleta de dados.

## Comandos (válidos quando o código existir; baseados na v1)

No Windows use `mvnw.cmd` e a sintaxe do PowerShell.

```bash
# Back-end (raiz do projeto)
./mvnw spring-boot:run        # sobe o backend em :8080 (precisa de um Postgres rodando)
./mvnw clean package          # build do jar (roda os testes)
./mvnw test                   # roda todos os testes
./mvnw test -Dtest=DesafioTest                         # uma classe de teste (domínio)
./mvnw test -Dtest=CriarDesafioUseCaseTest#nomeDoTeste # um método de teste

# Front-end (stack/local a confirmar; hoje em React/Vite na v1)
npm run dev      # Vite dev server em :5173, faz proxy de /api -> localhost:8080
npm run build    # tsc -b && vite build
npm run lint     # eslint

# Docker (abordagem ainda a definir — referência IFConecta: compose = só infra local)
docker compose up -d          # sobe a infra local (Postgres [+ MailHog])
docker compose down -v        # derruba e apaga o volume do Postgres
```

Swagger UI: `http://localhost:8080/swagger-ui.html`.

## Convenções de commit

- **Conventional Commits**: `<tipo>[escopo]: <descrição>`, modo imperativo, sem ponto final
  (`feat`, `fix`, `chore`, `docs`, `refactor`, `perf`, `test`).
- **Nunca** adicione trailer `Co-Authored-By` (nem qualquer co-autor automático) — regra global do usuário.

## Referências

- Visão e metodologia: [`Modelo de Projeto - CodeInsights.pdf`](Modelo%20de%20Projeto%20-%20CodeInsights.pdf).
- **Arquitetura / padrão de implementação (primária)**: IFConecta, branch `main` —
  `C:\Users\henri\OneDrive\Área de Trabalho\Geral\Projetos\IFConecta\ifconecta`.
- **Produto / domínio (v1)**: `C:\Users\henri\OneDrive\Área de Trabalho\Geral\Projetos\code-archive`
  (ver o `CLAUDE.md` e o `Arquitetura.txt` dela).
