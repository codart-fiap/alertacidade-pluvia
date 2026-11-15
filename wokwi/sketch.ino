/*
 * Pluvia - sensores IoT simulados no Wokwi (ESP32).
 *
 *  - Potenciometro 1 (GPIO 34): Regua de nivel do rio, 0 a 500 cm
 *  - Potenciometro 2 (GPIO 35): Pluviometro (chuva),   0 a 100 mm/h
 *
 * A cada INTERVALO_MS, o ESP32 envia as duas leituras para a API:
 *   POST {API_URL}/sensores/{id}/leituras     corpo: {"valor": 123.45}
 *   cabecalho X-Device-Key: <chave do dispositivo>
 * Se o valor passa do limite do sensor, a API gera o alerta e ativa a area de risco no mapa do app.
 *
 * Como usar: veja wokwi/LEIAME.md (a API roda no seu PC e precisa de um tunel publico,
 * porque o Wokwi roda na nuvem e nao enxerga o localhost).
 */
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <HTTPClient.h>

// ===================== CONFIGURE AQUI =====================
const char* WIFI_SSID = "Wokwi-GUEST";   // rede WiFi do simulador (nao muda)
const char* WIFI_PASS = "";

// URL publica da API (do tunel), SEM barra no final.
// Ex.: "https://exemplo-aleatorio.trycloudflare.com"
const char* API_URL = "https://COLE-AQUI-A-URL-DO-TUNEL";

// Mesma chave de app.device-key da API (demonstracao: em producao viria de local seguro)
const char* DEVICE_KEY = "PREENCHA_COM_A_DEVICE_KEY_LOCAL";

// IDs dos sensores na API (carga inicial: 1 = regua Area A, 2 = pluviometro Area A)
const int SENSOR_NIVEL_ID = 1;
const int SENSOR_CHUVA_ID = 2;

const unsigned long INTERVALO_MS = 5000;
// ==========================================================

const int PIN_NIVEL = 34;
const int PIN_CHUVA = 35;
const float NIVEL_MAX_CM = 500.0;
const float CHUVA_MAX_MMH = 100.0;

void conectarWifi() {
  Serial.print("Conectando ao WiFi");
  WiFi.begin(WIFI_SSID, WIFI_PASS, 6);
  while (WiFi.status() != WL_CONNECTED) {
    delay(250);
    Serial.print(".");
  }
  Serial.println(" conectado!");
}

// Envia uma leitura; devolve true se a API respondeu 201.
bool enviarLeitura(int sensorId, float valor, const char* rotulo, const char* unidade) {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi desconectado, tentando reconectar...");
    conectarWifi();
  }

  String url = String(API_URL) + "/sensores/" + String(sensorId) + "/leituras";
  String corpo = "{\"valor\":" + String(valor, 2) + "}";

  HTTPClient http;
  WiFiClientSecure clienteSeguro;
  WiFiClient clienteSimples;

  if (url.startsWith("https://")) {
    clienteSeguro.setInsecure();  // demonstracao: nao valida o certificado do tunel
    http.begin(clienteSeguro, url);
  } else {
    http.begin(clienteSimples, url);
  }
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-Device-Key", DEVICE_KEY);
  http.addHeader("ngrok-skip-browser-warning", "true");

  int status = http.POST(corpo);
  String resposta = http.getString();
  http.end();

  Serial.print("[");
  Serial.print(rotulo);
  Serial.print("] ");
  Serial.print(valor, 1);
  Serial.print(" ");
  Serial.print(unidade);
  Serial.print(" -> HTTP ");
  Serial.print(status);
  if (resposta.indexOf("\"alertaGerado\":true") >= 0) {
    Serial.print("  *** ALERTA GERADO ***");
  }
  Serial.println();
  return status == 201;
}

void setup() {
  Serial.begin(115200);
  delay(500);
  Serial.println("Pluvia - sensores IoT (ESP32)");
  conectarWifi();
}

void loop() {
  // ADC do ESP32: 0 a 4095
  float nivelCm = analogRead(PIN_NIVEL) * NIVEL_MAX_CM / 4095.0;
  float chuvaMmh = analogRead(PIN_CHUVA) * CHUVA_MAX_MMH / 4095.0;

  enviarLeitura(SENSOR_NIVEL_ID, nivelCm, "Nivel do rio", "cm");
  enviarLeitura(SENSOR_CHUVA_ID, chuvaMmh, "Chuva", "mm/h");
  Serial.println("---");

  delay(INTERVALO_MS);
}
