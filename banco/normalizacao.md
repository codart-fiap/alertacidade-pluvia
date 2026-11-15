# Pluvia — Modelo relacional e normalização

Diagrama: `diagrama_er.png` (fonte editável em `diagrama_er.mmd`). Script: `pluvia_script.sql`. Consultas: `pluvia_consultas.sql`.

## 1. Entidades e relacionamentos

São **6 entidades**. Todas têm chave primária substituta (`id`, `BIGINT` gerado pelo banco).

| Relacionamento | Cardinalidade | Chave estrangeira | Regra de negócio |
|---|---|---|---|
| `area_risco` → `sensor` | 1 para N (0..N) | `sensor.area_risco_id` | Uma área é monitorada por vários sensores (régua de nível, pluviômetro); cada sensor pertence a uma única área. |
| `sensor` → `leitura_sensor` | 1 para N (0..N) | `leitura_sensor.sensor_id` | Um sensor envia muitas leituras; cada leitura vem de um único sensor. |
| `leitura_sensor` → `alerta` | 1 para 0..1 | `alerta.leitura_id` (UNIQUE) | Uma leitura que atinge o limite do sensor gera no máximo um alerta; todo alerta nasce de exatamente uma leitura. |

`usuario` (quem acessa a API) e `ponto_seguro` (abrigos consultados pela localização da pessoa) não se ligam a outras tabelas: o `usuario` só autentica, e o `ponto_seguro` é um catálogo consultado por distância, sem vínculo fixo com uma área.

Integridade referencial: todas as chaves estrangeiras usam `ON DELETE RESTRICT`. Não se apaga uma área que ainda tem sensores, nem um sensor que tem leituras, nem uma leitura que gerou alerta, o que preserva o histórico. A API devolve `409 Conflict` nesses casos.

## 2. Primeira forma normal (1FN)

- Todos os atributos são **atômicos**: a posição é guardada em duas colunas numéricas (`latitude`, `longitude`), sem listas nem campos compostos em texto.
- Não há grupos repetidos: as várias leituras de um sensor ficam em linhas de `leitura_sensor`, e não em colunas como `leitura1`, `leitura2`.
- Toda tabela tem chave primária.

## 3. Segunda forma normal (2FN)

Todas as chaves primárias são de **uma única coluna** (`id`), então não existe dependência parcial de parte de uma chave composta. Todas as tabelas estão em 2FN por construção.

## 4. Terceira forma normal (3FN)

Nenhum atributo comum depende de outro atributo comum (sem dependência transitiva). Dependências funcionais por tabela:

| Tabela | Dependências funcionais | 3FN |
|---|---|---|
| `usuario` | `id → nome, email, senha_hash, perfil` · `email → id` (chave candidata, `UNIQUE`) | Sim |
| `area_risco` | `id → nome, latitude, longitude, raio_metros, nivel_risco, ativa` | Sim |
| `ponto_seguro` | `id → nome, latitude, longitude` | Sim |
| `sensor` | `id → nome, tipo, area_risco_id, limite_alerta` | Sim |
| `leitura_sensor` | `id → sensor_id, valor, data_hora` | Sim |
| `alerta` | `id → leitura_id, nivel, mensagem` · `leitura_id → id` (`UNIQUE`) | Sim |

## 5. Decisões de projeto que mantêm a 3FN

1. **`alerta` não guarda a área nem o horário.** Numa primeira versão do modelo, `alerta` tinha `area_risco_id` e `data_hora`. Mas `leitura_id → sensor → area_risco` já determina a área, e a hora do alerta é a da própria leitura. Guardar de novo seria uma **dependência transitiva** (`leitura_id → area_risco_id`, `leitura_id → data_hora`), com risco de inconsistência (alerta apontando para uma área diferente da do sensor). Os dois campos foram removidos, e a área e o horário são obtidos por JOIN (alerta → leitura → sensor → área), como nas consultas 2, 3 e 6.
2. **`sensor.tipo` guarda só o código** (`NIVEL_RIO` ou `CHUVA`). A descrição e a unidade (cm, mm/h) dependem do tipo, e não do sensor; por isso não ficam como colunas de `sensor`, o que evita repetição. Se esses dados precisassem ser editáveis, virariam uma tabela `tipo_sensor`. Como são só dois valores fixos, o domínio é garantido por `CHECK`. O mesmo vale para `usuario.perfil` (`ADMIN`, `CIDADAO`) e para os níveis de risco.
3. **`alerta.nivel` e `alerta.mensagem` são o registro do momento.** O nível depende de `valor / limite_alerta` no instante da leitura. Se o limite do sensor for alterado depois, o nível do alerta antigo não pode ser recalculado, então ele precisa ficar gravado como histórico. A mensagem segue a mesma lógica: preserva o texto emitido na ocasião.
4. **`area_risco.nivel_risco` e `ativa` são o estado atual da área**, atualizados pelo sistema quando um sensor passa do limite e pelo administrador quando desativa a área. São o que o app desenha no mapa, não uma cópia de outro dado.

## 6. Constraints do script

| Tipo | Onde |
|---|---|
| Chave primária | `id` de todas as tabelas |
| Chave estrangeira | `sensor.area_risco_id`, `leitura_sensor.sensor_id`, `alerta.leitura_id` |
| `UNIQUE` | `usuario.email`, `alerta.leitura_id` |
| `CHECK` | latitude (-90 a 90), longitude (-180 a 180), `raio_metros > 0`, `limite_alerta > 0`, `valor >= 0`, e os domínios de `perfil`, `tipo` e `nivel` |
| `NOT NULL` | todas as colunas |
| `DEFAULT` | `area_risco.ativa = TRUE` |
