# Pluvia — API, Segurança e Simulação de IoT

Global Solution 2026/2 (FIAP) · tema **CitizenTech**.

O **Pluvia** avisa o cidadão sobre áreas com risco de enchente: o app recebe o alerta, mostra no mapa a área de risco destacada, e ao tocar em "Minha localização" a pessoa recebe um relatório personalizado dizendo se está dentro ou perto da área e para onde ir.

Este repositório contém as partes **API RESTful**, **Segurança** e **Simulação de IoT**. O banco (PostgreSQL) e o app mobile são entregues pelos outros integrantes do grupo, e o app consome esta API.

## Integrantes

> Preencher: nome de cada integrante e RM (os mesmos do `.txt` da entrega).

## Requisitos

- JDK 17 ou superior
- Maven 3.9+ (ou o Maven embutido do IntelliJ)
- PostgreSQL 14+

## Como executar

### 1. Banco de dados

Crie o banco vazio (pgAdmin ou `psql`):

```sql
CREATE DATABASE pluvia;
```

Copie `.env.example` para `.env` e configure valores locais únicos antes de subir o banco ou a API. No PowerShell:

```powershell
Copy-Item .env.example .env
```

Edite `.env` com segredos aleatórios locais. O arquivo é ignorado pelo Git. O projeto usa:

| Variável | Uso |
|---|---|
| `DB_URL` | URL JDBC (padrão local: `jdbc:postgresql://localhost:5432/pluvia`) |
| `DB_USER` | usuário do PostgreSQL (padrão local: `postgres`) |
| `DB_PASSWORD` | senha local do PostgreSQL (padrão de demonstração: `postgres`) |
| `JWT_SECRET` | chave de demonstração local, já configurada |
| `DEVICE_KEY` | chave de demonstração local, já configurada |
| `ADMIN_EMAIL` e `ADMIN_PASSWORD` | conta de demonstração, já configurada |
| `CORS_ALLOWED_ORIGINS` | opcional; origens do front local têm padrão configurado |

Os valores de demonstração permitem executar o projeto localmente sem configuração adicional. Altere-os se usar uma instalação PostgreSQL com outra senha. Para compartilhar ou publicar a API, substitua as chaves e credenciais por valores próprios e não publique o `.env`.

As tabelas são criadas automaticamente pelo Hibernate na primeira execução (`ddl-auto=update`). Nessa primeira execução também é feita uma carga inicial de **dados fictícios de demonstração** (2 áreas de risco, 4 sensores e 3 pontos seguros), só se as tabelas estiverem vazias. A conta ADMIN só é criada se `ADMIN_EMAIL` e `ADMIN_PASSWORD` estiverem configurados.

Alternativamente, rode o script `banco/pluvia_script.sql` no banco `pluvia` (cria as tabelas com PK, FK e constraints e uma carga inicial maior, com leituras e alertas) e depois suba a API normalmente. Ele recria as tabelas, então pode ser reexecutado. Veja a seção **Parte 1 — Banco de dados**.

### 2. API

Pelo terminal, na pasta do projeto (a que contém o `pom.xml`), depois de preencher o `.env`:

```bash
mvn spring-boot:run
```

Ou pelo IntelliJ: abra esta pasta (**File → Open**), aguarde a importação do Maven e execute a classe `PluviaApiApplication`.

A API sobe em `http://localhost:8080`.

- **Swagger (documentação interativa):** http://localhost:8080/swagger-ui.html. Faça `POST /auth/login`, copie o `token` e clique em **Authorize**.
- **Postman:** importe `docs/Pluvia.postman_collection.json`, preencha as variáveis locais `adminEmail` e `adminPassword`, rode **Login ADMIN** (o token é salvo sozinho) e use as demais requisições.

### 3. Simulador de sensores

Com a API no ar, em outro terminal, na pasta do projeto:

```bash
java src/main/java/SimuladorSensores.java http://localhost:8080 5
```

Defina `DEVICE_KEY` no ambiente deste terminal com o mesmo valor usado pela API (o `.env` não exporta variáveis para o PowerShell). Argumentos opcionais: `[urlBase] [intervaloEmSegundos] [numeroDeRodadas]` (rodadas = 0 roda até `Ctrl+C`). Não precisa instalar nada além do JDK.

### 4. Testes

```bash
mvn test
```

## Usuários

Credenciais de demonstração: `admin@pluvia.com` / `Admin@123`. Cidadãos podem se cadastrar pelo endpoint `/auth/registro`.

Se um banco local existente ainda tiver a antiga conta de demonstração, altere ou remova essa conta no banco; o `DataLoader` não apaga nem troca usuários já existentes automaticamente.

## Endpoints

| Método | Endpoint | Quem acessa | Descrição |
|---|---|---|---|
| POST | `/auth/registro` | público | Cadastra cidadão |
| POST | `/auth/login` | público | Devolve o token JWT |
| GET | `/areas-risco` | público | Lista áreas (o app desenha as ativas no mapa) |
| GET | `/areas-risco/{id}` | público | Busca uma área |
| POST | `/areas-risco` | ADMIN | Cria área |
| PUT | `/areas-risco/{id}` | ADMIN | Atualiza área |
| DELETE | `/areas-risco/{id}` | ADMIN | Exclui área (409 se houver sensores) |
| GET | `/sensores` | público | Lista sensores, última leitura recebida e horário |
| POST | `/sensores/{id}/leituras` | ADMIN ou dispositivo | Registra leitura; acima do limite gera alerta |
| GET | `/alertas` | público | 50 alertas mais recentes |
| GET | `/pontos-seguros` | público | Lista pontos seguros |
| POST | `/pontos-seguros` | ADMIN | Cadastra ponto seguro |
| GET | `/recomendacao?lat=&lng=` | público | Relatório da tela "Minha localização" |

As consultas `GET` são públicas para permitir o acesso cidadão sem conta. Escritas administrativas exigem `Authorization: Bearer <token>` com perfil ADMIN; leituras de sensores também aceitam o cabeçalho `X-Device-Key: <chave>`.

Para conectar o front `alertacidade-web`, configure `VITE_API_URL=http://localhost:8080` no `.env` do front (sem sufixo `/api`). O CORS aceita por padrão `localhost` e `127.0.0.1` nas portas `5173` e `5174`; ajuste `CORS_ALLOWED_ORIGINS` se o front estiver em outra origem.

Códigos de status usados: `200`, `201`, `204`, `400` (dados inválidos), `401` (sem token / credenciais inválidas), `403` (perfil sem permissão), `404`, `409` (conflito), `500` (erro interno sem detalhes técnicos).

### Fluxo do app (conforme o wireframe)

1. O app consulta `GET /alertas` periodicamente e mostra a notificação "Alerta Área A".
2. Ao tocar, a tela de mapa usa `GET /areas-risco` e destaca em vermelho as áreas com `ativa = true` (círculo com `latitude`, `longitude` e `raioMetros`). Pode haver várias.
3. Ao tocar em "Minha localização", o app chama `GET /recomendacao?lat=..&lng=..` com a posição do aparelho e exibe o campo `mensagem` (dentro da área, próximo da área ou fora) e o `pontoSeguro` mais próximo.

### Como a recomendação é calculada

Por regras simples, sem IA: a API calcula a distância da pessoa ao centro de cada área ativa (fórmula de Haversine) e subtrai o raio. Dentro da área (distância até a borda ≤ 0) → `DENTRO_DA_AREA_DE_RISCO`; até 500 m da borda → `PROXIMO_DA_AREA_DE_RISCO`; mais longe → `FORA_DA_AREA_DE_RISCO`. Nos dois primeiros casos, indica o ponto seguro mais próximo que **não esteja dentro** de nenhuma área de risco ativa; se não houver nenhum, orienta ligar para a Defesa Civil (199).

## Parte 1 — Banco de dados

Arquivos na pasta `banco/`:

| Arquivo | O que é |
|---|---|
| `diagrama_er.png` (fonte em `diagrama_er.mmd`) | Diagrama ER: 6 entidades, com PK, FK e cardinalidades |
| `normalizacao.md` | Justificativa da normalização (1FN, 2FN, 3FN) e das constraints |
| `pluvia_script.sql` | `CREATE TABLE` com PK, FK e constraints, mais `INSERT`s de carga inicial. Roda sem erro e pode ser reexecutado |
| `pluvia_consultas.sql` | 8 `SELECT`s de uso real, com JOIN e agregação |

Modelo: `area_risco` 1—N `sensor` 1—N `leitura_sensor` 1—0..1 `alerta`; `usuario` e `ponto_seguro` são independentes. A API usa exatamente estas tabelas (nomes de tabelas e colunas iguais aos das entidades JPA).

## Arquitetura

```
Controller  →  Service  →  Repository  →  PostgreSQL
(HTTP/JSON)    (regras)    (Spring Data JPA)
```

- `controller/` recebe e responde HTTP, sem regra de negócio
- `service/` contém as regras (alertas, recomendação, autenticação)
- `repository/` acesso ao banco (Spring Data JPA)
- `model/` entidades JPA · `dto/` objetos de entrada e saída (a API nunca expõe entidades nem a senha)
- `security/` JWT, chave de dispositivo e regras de acesso · `exception/` tratamento global de erros

Entidades (tabelas): `usuario`, `area_risco`, `sensor`, `leitura_sensor`, `alerta`, `ponto_seguro`.

## Parte 5 — Segurança

| Item | Como foi feito |
|---|---|
| Login seguro | Senha armazenada com **BCrypt** (`BCryptPasswordEncoder`); nunca em texto puro nem devolvida pela API. Login devolve **JWT** assinado (HMAC-SHA) com expiração de 120 min. |
| Controle de acesso | Perfis `ADMIN` e `CIDADAO` (pessoas) e `DISPOSITIVO` (sensores). Cidadão só faz leituras (GET); POST/PUT/DELETE exigem ADMIN (`SecurityConfig`); o dispositivo só pode enviar leituras. Cadastro público sempre cria `CIDADAO`. |
| Boas práticas | (1) **Validação de entrada** com Bean Validation (`@Valid`, `@NotBlank`, `@Email`, faixas de latitude/longitude); (2) **proteção contra SQL Injection**: acesso ao banco só por JPA/Spring Data com parâmetros, sem concatenar SQL; (3) **hash de senha** com BCrypt; (4) **tratamento global de erros** que não vaza stack trace nem detalhes internos; (5) **sessão stateless** com token, sem cookies (por isso CSRF desabilitado); (6) **chave de dispositivo** para os sensores, comparada em tempo constante, sem guardar login de administrador no dispositivo; (7) segredos carregados de variáveis de ambiente ou do `.env` local ignorado pelo Git. |
| XSS | A API só devolve JSON (não renderiza HTML) e o app mobile não interpreta HTML, então o risco é baixo; a validação de entrada também limita tamanho e formato dos campos. |

## Parte 6 — Simulação de IoT

| Sensor | O que mede | Como influencia o sistema |
|---|---|---|
| Régua de nível do rio/córrego (`NIVEL_RIO`) | Altura da água, em **cm** | Se a leitura atinge o limite do sensor (ex.: 300 cm), a API cria um **alerta** e **ativa a área de risco** no mapa |
| Pluviômetro (`CHUVA`) | Intensidade da chuva, em **mm/h** | Mesmo mecanismo (ex.: limite de 50 mm/h) |

- Há **4 sensores** simulados (2 por área de risco), cadastrados na carga inicial.
- **Simulador em Java** (`src/main/java/SimuladorSensores.java`): envia leituras por `POST /sensores/{id}/leituras` a cada poucos segundos, com a chave de dispositivo. Os valores sobem com ruído até passar do limite e depois "baixam", repetindo o ciclo. É a versão que roda só seguindo este README.
- **Simulação com hardware no Wokwi** (`wokwi/`): um ESP32 com dois potenciômetros (nível do rio e chuva) que envia as mesmas leituras para a API. Passo a passo em `wokwi/LEIAME.md`. Link do projeto no Wokwi: *(colar aqui)*.
- Gravidade do alerta pela razão `valor / limite`: até 1,2 → `BAIXO`; de 1,2 a 1,5 → `MEDIO`; a partir de 1,5 → `ALTO`.
- Os alertas ficam em `GET /alertas` e a área passa a aparecer em `GET /areas-risco` com `ativa = true` e o nível atual, que é o que o app mostra. Assim os sensores enviam dados pela API e o app exibe os alertas.
- Limitação conhecida do MVP: a área só é desativada manualmente (`PUT /areas-risco/{id}` com `ativa: false`).

## Vídeo pitch

> Colar aqui o link público do vídeo (YouTube, Google Drive ou Vimeo) quando estiver pronto.

## Uso de IA generativa

>## Uso de IA

Durante o desenvolvimento deste projeto, utilizamos ferramentas de IA generativa (**Claude** e **ChatGPT**) como apoio em algumas etapas, sempre com revisão e entendimento da equipe antes de aplicar qualquer sugestão ao projeto final.

**Onde foram usadas:**

- **Depuração e explicação de erros:** auxílio para interpretar mensagens de erro (ex.: falhas de conexão com o banco, exceptions da API, problemas de CORS) e entender possíveis causas antes de corrigirmos o código.
- **Discussão de abordagens e implementações:** apoio para avaliar diferentes formas de estruturar camadas da API (Controller/Service/Repository) e validar se a abordagem escolhida pela equipe fazia sentido.
- **Funcionalidade de mapa e cálculos de localização — nossa maior dificuldade no projeto:** essa foi a parte mais desafiadora para a equipe. Tínhamos bastante dificuldade em três pontos específicos: (1) exibir o mapa de forma visual no front, com os marcadores das áreas de risco e pontos seguros; (2) calcular corretamente se uma coordenada estava dentro ou fora do raio de uma área circular (lógica de geometria que nenhum de nós tinha visto antes); e (3) integrar essas partes com a API e com o restante do sistema (sensor gerando leitura → cálculo de distância → decisão de alerta → exibição no mapa). Como não conseguimos avançar sozinhos nessas partes, usamos trechos de código gerados com apoio de IA como base, principalmente para a função de cálculo de distância (fórmula de Haversine) e para a estrutura de integração entre o cálculo e o endpoint de recomendação. A partir dessa base, estudamos o funcionamento, testamos, ajustamos às nossas entidades (área de risco, ponto seguro) e validamos os resultados com os dados de teste do grupo até entender e conseguir explicar o que cada parte fazia.
- **Revisão e formatação de textos:** apoio na organização e clareza dos textos finais (README, plano de testes, descrições de endpoints), sem alterar o conteúdo técnico decidido pela equipe.

> **Importante:** nenhuma parte do código ou da documentação foi usada sem revisão da equipe. Todo o código gerado com apoio de IA foi lido, testado e ajustado até que o grupo compreendesse seu funcionamento por completo.

## Observação

Os dados de áreas, coordenadas, sensores e pontos seguros da carga inicial são **fictícios**, criados só para demonstração.
