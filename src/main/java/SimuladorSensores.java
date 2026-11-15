import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simulador de sensores do Pluvia (nivel do rio e chuva).
 *
 * Usa a chave de dispositivo (cabecalho X-Device-Key), como um sensor real faria: descobre os sensores
 * cadastrados (GET /sensores) e envia leituras simuladas (POST /sensores/{id}/leituras) a cada
 * intervalo. As leituras sobem aos poucos ate passar do limite de cada sensor, o que faz a API gerar
 * alertas e ativar a area de risco no mapa do app.
 *
 * Executar (Java 17+, sem dependencias), a partir da pasta do projeto, com a API no ar:
 *   java src/main/java/SimuladorSensores.java [urlBase] [intervaloSegundos] [rodadas]
 * Exemplo:
 *   java src/main/java/SimuladorSensores.java http://localhost:8080 5 60
 * rodadas = 0 (padrao) roda ate ser interrompido com Ctrl+C.
 *
 * A chave é lida de DEVICE_KEY no ambiente ou do .env local, igual à configuração da API.
 */
public class SimuladorSensores {

    static class SensorSimulado {
        long id;
        String nome;
        String tipo;
        double limite;
        double valor;
    }

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static String baseUrl;
    private static String chave;

    public static void main(String[] args) throws Exception {
        baseUrl = args.length > 0 ? args[0] : "http://localhost:8080";
        int intervaloSegundos = args.length > 1 ? Integer.parseInt(args[1]) : 5;
        int rodadas = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        chave = carregarDeviceKey();

        List<SensorSimulado> sensores = listarSensores();
        if (sensores.isEmpty()) {
            System.out.println("Nenhum sensor cadastrado na API.");
            return;
        }
        System.out.println(sensores.size() + " sensores encontrados. Enviando leituras a cada "
                + intervaloSegundos + "s (Ctrl+C para parar).");

        Random random = new Random();
        for (SensorSimulado s : sensores) {
            s.valor = s.limite * 0.4; // comeca bem abaixo do limite
        }

        for (int i = 1; rodadas == 0 || i <= rodadas; i++) {
            System.out.println("--- rodada " + i + " ---");
            for (SensorSimulado s : sensores) {
                s.valor = proximoValor(s, random);
                enviarLeitura(s);
            }
            Thread.sleep(intervaloSegundos * 1000L);
        }
    }

    private static String carregarDeviceKey() {
        String valorAmbiente = System.getenv("DEVICE_KEY");
        if (valorAmbiente != null && !valorAmbiente.isBlank()) {
            return valorAmbiente.trim();
        }

        try (var linhas = Files.lines(Path.of(".env"))) {
            String valorArquivo = linhas.map(String::trim)
                    .filter(linha -> linha.startsWith("DEVICE_KEY="))
                    .map(linha -> linha.substring("DEVICE_KEY=".length()).trim())
                    .findFirst().orElse("");
            if (!valorArquivo.isBlank()) {
                if (valorArquivo.length() >= 2 && valorArquivo.startsWith("\"") && valorArquivo.endsWith("\"")) {
                    return valorArquivo.substring(1, valorArquivo.length() - 1);
                }
                return valorArquivo;
            }
        } catch (java.io.IOException ignored) {
            // Se não houver .env, usa a chave de demonstração local abaixo.
        }
        return "pluvia-chave-dispositivo-dev-123";
    }

    /** Tendencia de subida com ruido; quando passa muito do limite, "baixa a mare" e recomeca. */
    static double proximoValor(SensorSimulado s, Random random) {
        double variacao = (random.nextDouble() * 0.20 - 0.07) * s.limite;
        double novo = s.valor + variacao;
        if (novo > s.limite * 1.7) {
            novo = s.limite * 0.5;
        }
        return Math.max(novo, s.limite * 0.1);
    }

    private static List<SensorSimulado> listarSensores() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/sensores"))
                .header("X-Device-Key", chave)
                .GET()
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 401) {
            throw new IllegalStateException("Chave de dispositivo recusada (HTTP 401). "
                    + "Confira SIM_DEVICE_KEY e app.device-key na API.");
        }
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("Falha ao listar sensores (HTTP " + resp.statusCode() + "): " + resp.body());
        }
        List<SensorSimulado> lista = new ArrayList<>();
        Matcher objetos = Pattern.compile("\\{[^{}]*\\}").matcher(resp.body());
        while (objetos.find()) {
            String json = objetos.group();
            SensorSimulado s = new SensorSimulado();
            s.id = Long.parseLong(extrair(json, "\"id\"\\s*:\\s*(\\d+)"));
            s.nome = extrair(json, "\"nome\"\\s*:\\s*\"([^\"]*)\"");
            s.tipo = extrair(json, "\"tipo\"\\s*:\\s*\"([^\"]*)\"");
            s.limite = Double.parseDouble(extrair(json, "\"limiteAlerta\"\\s*:\\s*([0-9.eE+-]+)"));
            lista.add(s);
        }
        return lista;
    }

    private static String extrair(String json, String regex) {
        Matcher m = Pattern.compile(regex).matcher(json);
        if (!m.find()) {
            throw new IllegalStateException("Campo nao encontrado (" + regex + ") em " + json);
        }
        return m.group(1);
    }

    private static void enviarLeitura(SensorSimulado s) throws Exception {
        String corpo = String.format(Locale.US, "{\"valor\":%.2f}", s.valor);
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/sensores/" + s.id + "/leituras"))
                .header("Content-Type", "application/json")
                .header("X-Device-Key", chave)
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        boolean alerta = resp.body().contains("\"alertaGerado\":true");
        System.out.printf(Locale.US, "[%s] %s = %.2f (limite %.1f) -> HTTP %d%s%n",
                s.tipo, s.nome, s.valor, s.limite, resp.statusCode(), alerta ? "  *** ALERTA GERADO ***" : "");
    }
}
