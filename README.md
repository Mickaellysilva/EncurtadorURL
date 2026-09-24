# ShortLink — Encurtador de URLs com Analytics

API backend para encurtamento de URLs com cache, rate limiting e analytics de acesso. Projeto construído para demonstrar arquitetura de sistemas backend: cache, filas, testes e containerização.

##  Funcionalidades

- Criação de URLs curtas a partir de URLs longas
- Redirecionamento rápido via cache (Redis)
- Rate limiting por IP para evitar abuso
- Registro de analytics por clique (data/hora, IP, user-agent)
- Consulta de estatísticas por URL encurtada

##  Arquitetura

```
Cliente → API (Spring Boot) → Redis (cache de lookup)
                             → PostgreSQL (persistência)
                             → Fila (registro assíncrono de cliques)
```

**Fluxo de criação de URL:**
1. Cliente envia URL longa via `POST /urls`
2. API persiste no banco e gera código único (Base62 sobre o ID gerado)
3. Código + URL são cacheados no Redis

**Fluxo de redirecionamento:**
1. Cliente acessa `GET /{code}`
2. API busca no cache (Redis) — se não encontrar, busca no banco e popula o cache
3. API responde com redirect 302
4. Evento de clique é registrado de forma assíncrona (não bloqueia o redirect)

## Stack

- **Linguagem/Framework:** Java + Spring Boot
- **Banco de dados:** PostgreSQL
- **Cache:** Redis
- **Containerização:** Docker + Docker Compose
- **Testes:** JUnit + Mockito

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

CREATE INDEX idx_urls_code ON urls(code);
CREATE INDEX idx_clicks_url_id ON clicks(url_id);
```

## Estrutura de pacotes

```
com.shortlink
├── controller/
│   └── UrlController.java
├── service/
│   └── UrlService.java
├── repository/
│   └── UrlRepository.java
│   └── ClickRepository.java
├── entity/
│   └── Url.java
│   └── Click.java
├── dto/
│   └── CreateUrlRequest.java
│   └── UrlStatsResponse.java
└── config/
    └── RedisConfig.java
```

## Endpoints

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/urls` | Cria uma nova URL curta |
| `GET` | `/{code}` | Redireciona para a URL original |
| `GET` | `/urls/{code}/stats` | Retorna estatísticas de acesso |

### Exemplo — Criar URL

**Request:**
```json
POST /urls
{
  "url": "https://exemplo.com/pagina-muito-longa"
}
```

**Response:**
```json
{
  "code": "aZ3kD1",
  "shortUrl": "https://shortlink.dev/aZ3kD1",
  "createdAt": "2026-09-24T12:00:00Z"
}
```

### Exemplo — Estatísticas

**Request:**
```
GET /urls/aZ3kD1/stats
```

**Response:**
```json
{
  "code": "aZ3kD1",
  "originalUrl": "https://exemplo.com/pagina-muito-longa",
  "totalClicks": 42,
  "lastAccessedAt": "2026-09-24T15:30:00Z"
}
```

## Rodando localmente

```bash
# Clonar o repositório
git clone <repo-url>
cd shortlink

# Subir com Docker Compose (API + Postgres + Redis)
docker-compose up --build
```

A API estará disponível em `http://localhost:8080`.

## Testes

```bash
mvn test
```

Cobertura inclui:
- Testes unitários de geração de código (Base62) e validação de URL
- Testes de integração dos endpoints principais

##  Decisões técnicas

- **Geração de código via Base62 sobre o ID do banco**: evita colisões sem precisar de lógica de retry, e é simples de explicar (trade-off simplicidade vs. previsibilidade).
- **Cache-aside com Redis**: reduz drasticamente a latência de redirecionamento, que é a operação mais frequente do sistema.
- **Registro assíncrono de cliques**: evita que a gravação de analytics atrase a resposta do redirect ao usuário.
- **Rate limiting por IP**: proteção simples contra abuso na criação de URLs, sem adicionar complexidade excessiva ao MVP.

## Roadmap (possíveis evoluções)

- [ ] Expiração automática de URLs
- [ ] Geolocalização de cliques
- [ ] Dashboard de analytics
- [ ] Autenticação e URLs por usuário

##  Deploy

Deploy realizado em _(Railway / Fly.io / Render)_ — link: _(adicionar após deploy)_

---

Desenvolvido como projeto de portfólio para demonstrar práticas de engenharia backend: cache, resiliência, testes e containerização.
