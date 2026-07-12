# CodeInsights

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-2b2b2b?logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring%20Boot-4-2b2b2b?logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/React-19-2b2b2b?logo=react&logoColor=white" />
  <img src="https://img.shields.io/badge/TypeScript-5-2b2b2b?logo=typescript&logoColor=white" />
  <img src="https://img.shields.io/badge/PostgreSQL-2b2b2b?logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Arquitetura-Hexagonal-2b2b2b" />
  <img src="https://img.shields.io/badge/status-em%20desenvolvimento-orange" />
</p>

Plataforma web de **gestão de conhecimento, portfólio de código e análise de métricas de aprendizado em programação**.

O CodeInsights é o objeto de uma **Iniciação Científica (PIBIFSP / IFSP)**. Diferente de um portfólio comum, o objetivo é científico: investigar o processo de aprendizado prático de programação a partir de métricas extraídas das resoluções dos alunos — em especial o **impacto da IA generativa na autonomia** e a **evolução da complexidade algorítmica** ao longo do tempo. A plataforma é, ao mesmo tempo, a ferramenta e o instrumento empírico da pesquisa.

> **Status:** em desenvolvimento ativo. O núcleo (autenticação, desafios, resoluções, snippets, portfólio público, motor de métricas e dashboard) já está implementado; a interface está em processo de redesign.

---

## O problema

Um aluno que resolve um exercício de programação hoje e outro daqui a três meses evoluiu — mas *quanto*, e *em quê*? E quando ele usa uma IA generativa para chegar à resposta, o que isso faz com a autonomia dele ao longo do tempo?

Essas perguntas não têm resposta sem dado. O CodeInsights existe para produzi-lo: o aluno registra desafios, submete resoluções e a plataforma extrai automaticamente indicadores objetivos de cada solução, montando a curva de amadurecimento dele.

---

## Métricas de aprendizado

O núcleo da pesquisa. As métricas são calculadas por **análise estática** do código submetido, sem executá-lo.

### Índice de Autonomia IA

Escala de 1 a 5 declarada pelo autor, medindo quanto da solução foi feita de forma autônoma versus apoiada ou gerada por IA. É a variável central do estudo.

### Complexidade algorítmica

Não existe algoritmo universal capaz de inferir a notação Big O exata de um código arbitrário — no caso geral, o problema é **indecidível**. Por isso a métrica é decomposta em três indicadores objetivos e calculáveis, que juntos mapeiam a curva de amadurecimento algorítmico (força bruta → estruturas refinadas):

- **Big O via AST** — estimativa da complexidade de **tempo** a partir da Árvore Sintática Abstrata da solução, analisando aninhamento de laços, recursão e chamadas entre procedimentos. A análise de recorrências usa o **Teorema Mestre**.
- **Complexidade Ciclomática (McCabe)** — número de caminhos independentes no fluxo de controle, como proxy da complexidade estrutural da solução.
- **Complexidade de Espaço** — uso de memória (estruturas auxiliares, recursão), também estimado a partir da AST.

### Snippets categorizados

Trechos de código reutilizáveis, classificados por conceito — prática de gestão de conhecimento pessoal e metacognição.

O motor de métricas foi desenhado para crescer **por adição, não por modificação**: uma métrica nova entra implementando a porta `AnalisadorMetricas` do domínio, sem tocar no modelo existente. A análise roda de forma **assíncrona**, disparada por um evento de domínio quando uma resolução é submetida — assim a submissão não fica presa esperando o analisador.

---

## Funcionalidades

- **Identidade** — registro, ativação de conta por e-mail, login com JWT, redefinição de senha.
- **Desafios** — criação, edição e controle de visibilidade (público/privado).
- **Resoluções** — submissão, análise automática de métricas e reanálise sob demanda.
- **Snippets** — biblioteca pessoal de trechos de código, categorizados e vinculáveis a desafios.
- **Portfólio público** — perfis e desafios públicos, com um diretório para explorar portfólios de outros usuários.
- **Dashboard** — resumo e gráficos de evolução das métricas, com seletor de granularidade (mês, semana, dia).

---

## Arquitetura

**Arquitetura Hexagonal / Clean com sabor DDD**, em empacotamento *layer-first*: o primeiro nível é a **camada**, o segundo é o **bounded context**.

```
com.projeto.codeinsights/
├── domain/<contexto>/          # Java puro — sem Spring, sem JPA, sem Lombok
│   ├── model/                  # entidades e agregados (sem setters)
│   ├── port/                   # interfaces que a infraestrutura implementa
│   └── enums/, exception/
├── application/<contexto>/
│   ├── usecase/                # um caso de uso por operação, método único execute()
│   ├── dto/                    # records: *Input (entrada) e *DTO (saída)
│   └── event/, listener/       # eventos de domínio (análise assíncrona)
└── infrastructure/
    ├── web/<contexto>/         # @RestController + records *Request/*Response
    ├── persistence/<contexto>/ # entidades JPA, Spring Data, adapters e mappers
    └── config/                 # security, OpenAPI, exceptions, async
```

Contextos: **`identity`** (usuários, perfil, autenticação) e **`knowledge`** (desafios, resoluções, snippets e métricas — o núcleo da pesquisa).

As dependências apontam **sempre para dentro**: `infrastructure → application → domain`. O domínio não depende de ninguém. As entidades são POJOs sem anotação, que mudam de estado por **métodos de comportamento** (nunca por setters) e validam suas próprias invariantes — o Índice de Autonomia, por exemplo, rejeita qualquer valor fora de 1–5 no próprio domínio, não no controller.

O mapeamento domínio ↔ JPA é **manual**, isolado em mappers, de modo que o domínio nunca conhece a entidade de persistência.

---

## Stack

**Back-end**
- Java 21, Spring Boot 4
- Spring Web MVC, Spring Data JPA, Spring Security, Spring Validation
- PostgreSQL + Flyway (Flyway é dono do schema; Hibernate em `validate`)
- JWT stateless (`jjwt`)
- OpenAPI / Swagger UI

**Front-end**
- React 19 + TypeScript
- Vite
- Tailwind CSS v4
- Radix UI
- TanStack Query
- React Router 7

**Infra**
- Docker / Docker Compose (Postgres local)

---

## Como rodar

### Pré-requisitos

- JDK 21
- Docker + Docker Compose
- Node.js 18+ e npm

### Passos

```bash
# 1. Sobe a infra local (PostgreSQL)
docker compose up -d

# 2. Back-end (na raiz) — sobe em :8080
./mvnw spring-boot:run

# 3. Front-end
cd frontend
npm install
npm run dev        # Vite em :5173, com proxy de /api -> :8080
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

O `JWT_SECRET` é obrigatório e **não tem valor padrão** — defina-o como variável de ambiente antes de subir o backend.

## Autor

Desenvolvido por Henrique.

- GitHub: [https://github.com/R1ck-dev](https://github.com/R1ck-dev)
- E-mail: [henriquemarangoni.inacio1108@gmail.com](mailto:henriquemarangoni.inacio1108@gmail.com)
- LinkedIn: [https://www.linkedin.com/in/henrique-marangoni-484845239/](https://www.linkedin.com/in/henrique-marangoni-484845239/)
