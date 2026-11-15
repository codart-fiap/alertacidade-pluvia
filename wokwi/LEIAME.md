# Sensores IoT no Wokwi (ESP32)

Demonstração visual da simulação de IoT: um ESP32 com dois potenciômetros que fazem o papel dos sensores e enviam as leituras para a API do Pluvia.

| Peça | Pino | Representa | Faixa |
|---|---|---|---|
| Potenciômetro 1 | GPIO 34 | Régua de nível do rio | 0 a 500 cm |
| Potenciômetro 2 | GPIO 35 | Pluviômetro (chuva) | 0 a 100 mm/h |

> A versão que **qualquer pessoa roda só seguindo o README** é o simulador em Java (`src/main/java/SimuladorSensores.java`). O Wokwi é a demonstração com hardware simulado e precisa de um túnel, porque roda na nuvem e não enxerga o `localhost`.

## Passo a passo

1. **Suba a API** no seu computador (porta 8080).
2. **Abra um túnel público** para a porta 8080. Uma opção sem cadastro é o Cloudflare Tunnel:
   ```
   cloudflared tunnel --url http://localhost:8080
   ```
   Ele mostra uma URL como `https://algo-aleatorio.trycloudflare.com`. (Alternativa: `ngrok http 8080`.) A URL muda a cada execução.
3. **Crie o projeto no Wokwi:** em https://wokwi.com, novo projeto **ESP32** (Arduino). Cole o conteúdo de `sketch.ino` no editor e o de `diagram.json` na aba `diagram.json`.
4. **Configure** no começo do `sketch.ino`: `API_URL` com a URL do túnel (sem barra no final) e substitua o placeholder `DEVICE_KEY` por uma chave local, igual à variável `DEVICE_KEY` da API. Não publique essa chave no projeto Wokwi.
5. **Rode** (botão verde). No monitor serial aparece cada leitura enviada.
6. **Gire os potenciômetros** (clique e arraste) para passar do limite do sensor (régua da Área A: 300 cm; pluviômetro da Área A: 50 mm/h). Quando passar, o serial mostra `*** ALERTA GERADO ***`, e `GET /alertas` e `GET /areas-risco` na API passam a refletir o alerta.

## Se algo der errado

- **HTTP -1 ou erro de conexão:** URL do túnel errada, túnel fechado ou API fora do ar.
- **HTTP 401:** `DEVICE_KEY` diferente da `app.device-key` da API.
- **HTTP 404:** o ID do sensor não existe. Confira `GET /sensores` e ajuste `SENSOR_NIVEL_ID` e `SENSOR_CHUVA_ID`.
- **Fio solto no diagrama:** ligue manualmente VCC dos potenciômetros no 3V3, GND no GND e os sinais (SIG) no D34 e D35.

## Para o vídeo

Mostre o Wokwi ao lado do Postman ou do app: gire o potenciômetro, mostre o alerta no serial e depois a área de risco ativa na API.
