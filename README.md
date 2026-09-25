# 🔗 ShortLink — Encurtador de URLs com Analytics

API REST em Java + Spring Boot para encurtamento de URLs, com cache via Redis, persistência em PostgreSQL e registro de analytics de acesso. Projeto de portfólio construído para demonstrar arquitetura backend: cache, resiliência, boas práticas de código e containerização.

## Funcionalidades

- Criação de URLs curtas a partir de URLs longas
- Geração de código único via Base62 (a partir do ID gerado pelo banco)
- Redirecionamento rápido com cache em Redis
- Registro de cada clique (IP e user-agent) de forma associada à URL
- Consulta de estatísticas por URL encurtada (total de cliques e último acesso)
- Tratamento de erro centralizado (404 para código inexistente)

##  Arquitetura

```
Cliente → API (Spring Boot) → Redis (cache de lookup)
                             → PostgreSQL (persistência)
```

**Fluxo de criação de URL:**
1. Cliente envia URL longa via `POST /urls`
2. API persiste no banco (gerando o `id`) e calcula o código Base62 a partir dele
3. Código + URL original são cacheados no Redis (TTL de 24h)

**Fluxo de redirecionamento:**
1. Cliente acessa `GET /{code}`
2. API busca o registro correspondente e responde com redirect 302
3. Um `Click` é registrado, associado à `Url` (IP, user-agent, timestamp)

##  Stack

- **Linguagem/Framework:** Java 17 + Spring Boot
- **Persistência:** Spring Data JPA + PostgreSQL
- **Cache:** Spring Data Redis
- **Validação:** Spring Validation (Bean Validation)
- **Build:** Maven
- **Containerização (infraestrutura):** Docker + Docker Compose (Postgres + Redis)

## Schema

```sql
CREATE TABLE urls (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) UNIQUE NOT NULL,
    original_url TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE clicks (
    id BIGSERIAL PRIMARY KEY,
    url_id BIGINT NOT NULL REFERENCES urls(id),
    accessed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ip_address VARCHAR(45),
    user_agent TEXT
);
```
> As tabelas são geradas automaticamente pelo Hibernate a partir das entidades JPA (`spring.jpa.hibernate.ddl-auto=update`).

## Estrutura de pacotes

```
com.shortlink.shortlink
├── controller/
│   ├── UrlController.java
│   └── GlobalExceptionHandler.java
├── service/
│   └── UrlService.java
├── repository/
│   ├── UrlRepository.java
│   └── ClickRepository.java
├── entity/
│   ├── Url.java
│   └── Click.java
└── dto/
    ├── CreateUrlRequest.java
    ├── UrlResponse.java
    └── UrlStatsResponse.java
```

##  Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/urls` | Cria uma nova URL curta |
| `GET` | `/{code}` | Redireciona para a URL original e registra o clique |
| `GET` | `/urls/{code}/stats` | Retorna estatísticas de acesso |

### Exemplo — Criar URL

**Request:**
```json
POST /urls
{
  "url": "https://www.google.com"
}
```

**Response (201 Created):**
```json
{
  "code": "1",
  "shortUrl": "http://localhost:8080/1",
  "createdAt": "2026-09-25T10:11:33.93Z"
}
```

### Exemplo — Estatísticas

**Request:**
```
GET /urls/1/stats
```

**Response (200 OK):**
```json
{
  "code": "1",
  "originalUrl": "https://www.google.com",
  "totalClicks": 3,
  "lastAccessedAt": "2026-09-25T10:20:00Z"
}
```

### Erros

Código inexistente retorna **404 Not Found**, tratado de forma centralizada por um `@RestControllerAdvice`.

## Rodando localmente

**1. Suba a infraestrutura (Postgres + Redis) via Docker:**
```bash
docker-compose up -d
```

**2. Configure o `src/main/resources/application.properties`** (já incluso no projeto) com as credenciais definidas no `docker-compose.yml`.

**3. Rode a aplicação:**
```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

## Decisões técnicas

- **Geração de código via Base62 sobre o ID do banco**: evita colisões sem necessidade de lógica de retry.
- **Cache-aside com Redis (TTL de 24h)**: reduz a latência de redirecionamento, a operação mais frequente do sistema.
- **Tratamento de erro centralizado (`@RestControllerAdvice`)**: separa a lógica de negócio do tratamento de exceções HTTP, evitando `try/catch` espalhado pelos controllers.
- **DTOs em vez de expor entidades diretamente na API**: desacopla o contrato da API da estrutura do banco, e permite compor dados calculados (como `totalClicks`) sem "sujar" as entidades.

## Roadmap

- [x] Modelagem de entidades e persistência (JPA + PostgreSQL)
- [x] Cache de redirecionamento (Redis)
- [x] Registro de cliques e estatísticas
- [x] Tratamento de erro (404)
- [ ] Rate limiting por IP
- [ ] Testes automatizados (unitários e de integração)
- [ ] Dockerfile da própria aplicação (Docker Compose completo, incluindo a API)
- [ ] Deploy (Railway / Fly.io / Render)

---

Desenvolvido como projeto de portfólio para demonstrar práticas de engenharia backend: cache, resiliência, arquitetura em camadas e boas práticas de código Java/Spring.
