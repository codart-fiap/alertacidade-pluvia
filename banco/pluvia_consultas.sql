-- =====================================================================
-- Pluvia - Consultas de uso (PostgreSQL)
-- Rode depois do pluvia_script.sql. Cada consulta responde a uma pergunta real do sistema
-- e usa JOIN e/ou agregacao (GROUP BY, COUNT, AVG, MAX, HAVING, FILTER).
-- =====================================================================


-- ---------------------------------------------------------------------
-- 1) Resumo de cada sensor: quantas leituras, media, pico e quanto o pico chegou do limite
--    Pergunta: "quais sensores estiveram mais perto (ou acima) do limite?"
--    Tecnicas: JOIN de 3 tabelas, LEFT JOIN, COUNT, AVG, MAX, GROUP BY
-- ---------------------------------------------------------------------
SELECT a.nome                                            AS area,
       s.nome                                            AS sensor,
       s.tipo,
       COUNT(l.id)                                       AS total_leituras,
       ROUND(AVG(l.valor)::numeric, 1)                   AS media,
       MAX(l.valor)                                      AS pico,
       s.limite_alerta                                   AS limite,
       ROUND((MAX(l.valor) / s.limite_alerta * 100)::numeric, 0) AS pico_percent_do_limite
FROM area_risco a
JOIN sensor s            ON s.area_risco_id = a.id
LEFT JOIN leitura_sensor l ON l.sensor_id = s.id
GROUP BY a.nome, s.id, s.nome, s.tipo, s.limite_alerta
ORDER BY pico_percent_do_limite DESC NULLS LAST;


-- ---------------------------------------------------------------------
-- 2) Alertas por area e por nivel (BAIXO, MEDIO, ALTO)
--    Pergunta: "quais areas geram mais alertas e com que gravidade?"
--    Tecnicas: JOIN de 4 tabelas, COUNT com FILTER, GROUP BY
-- ---------------------------------------------------------------------
SELECT a.nome                                       AS area,
       COUNT(*)                                     AS total_alertas,
       COUNT(*) FILTER (WHERE al.nivel = 'BAIXO')   AS baixo,
       COUNT(*) FILTER (WHERE al.nivel = 'MEDIO')   AS medio,
       COUNT(*) FILTER (WHERE al.nivel = 'ALTO')    AS alto
FROM alerta al
JOIN leitura_sensor l ON l.id = al.leitura_id
JOIN sensor s         ON s.id = l.sensor_id
JOIN area_risco a     ON a.id = s.area_risco_id
GROUP BY a.id, a.nome
ORDER BY total_alertas DESC;


-- ---------------------------------------------------------------------
-- 3) Alertas mais recentes (o que o app mostra em GET /alertas)
--    Pergunta: "quais foram os ultimos alertas, onde, e qual valor os disparou?"
--    Tecnicas: JOIN de 4 tabelas, ORDER BY, LIMIT
-- ---------------------------------------------------------------------
SELECT l.data_hora,
       a.nome         AS area,
       s.nome         AS sensor,
       l.valor        AS valor_medido,
       s.limite_alerta AS limite,
       al.nivel,
       al.mensagem
FROM alerta al
JOIN leitura_sensor l ON l.id = al.leitura_id
JOIN sensor s         ON s.id = l.sensor_id
JOIN area_risco a     ON a.id = s.area_risco_id
ORDER BY l.data_hora DESC, al.id DESC
LIMIT 10;


-- ---------------------------------------------------------------------
-- 4) Sensores que passaram do limite mais de uma vez
--    Pergunta: "quais sensores estao instaveis ou com limite mal calibrado?"
--    Tecnicas: JOIN, WHERE, COUNT, MAX, GROUP BY, HAVING
-- ---------------------------------------------------------------------
SELECT a.nome         AS area,
       s.nome         AS sensor,
       s.limite_alerta AS limite,
       COUNT(*)       AS vezes_acima_do_limite,
       MAX(l.valor)   AS maior_valor
FROM sensor s
JOIN area_risco a      ON a.id = s.area_risco_id
JOIN leitura_sensor l  ON l.sensor_id = s.id
WHERE l.valor >= s.limite_alerta
GROUP BY a.nome, s.id, s.nome, s.limite_alerta
HAVING COUNT(*) >= 2
ORDER BY vezes_acima_do_limite DESC, maior_valor DESC;


-- ---------------------------------------------------------------------
-- 5) Situacao atual de cada sensor (ultima leitura recebida)
--    Pergunta: "como esta cada sensor agora?"
--    Tecnicas: subconsulta com MAX + GROUP BY, JOIN de 4 tabelas, CASE
-- ---------------------------------------------------------------------
SELECT a.nome          AS area,
       s.nome          AS sensor,
       l.valor         AS ultimo_valor,
       s.limite_alerta AS limite,
       l.data_hora     AS lida_em,
       CASE WHEN l.valor >= s.limite_alerta THEN 'ACIMA DO LIMITE' ELSE 'normal' END AS situacao
FROM sensor s
JOIN area_risco a ON a.id = s.area_risco_id
JOIN (SELECT sensor_id, MAX(data_hora) AS ultima
      FROM leitura_sensor
      GROUP BY sensor_id) u ON u.sensor_id = s.id
JOIN leitura_sensor l ON l.sensor_id = u.sensor_id AND l.data_hora = u.ultima
ORDER BY a.nome, s.nome;


-- ---------------------------------------------------------------------
-- 6) Movimento por dia e por area: leituras recebidas e alertas gerados
--    Pergunta: "quanto o sistema recebeu e alertou em cada dia?"
--    Tecnicas: JOIN, LEFT JOIN, COUNT, GROUP BY por DATE()
-- ---------------------------------------------------------------------
SELECT DATE(l.data_hora) AS dia,
       a.nome            AS area,
       COUNT(l.id)       AS leituras,
       COUNT(al.id)      AS alertas
FROM leitura_sensor l
JOIN sensor s          ON s.id = l.sensor_id
JOIN area_risco a      ON a.id = s.area_risco_id
LEFT JOIN alerta al    ON al.leitura_id = l.id
GROUP BY DATE(l.data_hora), a.nome
ORDER BY dia, a.nome;


-- ---------------------------------------------------------------------
-- 7) Ponto seguro mais proximo de cada area ativa, fora do circulo de risco
--    Pergunta: "para onde orientar as pessoas de cada area em alerta?" (base da recomendacao)
--    Tecnicas: CROSS JOIN, subconsulta LATERAL com a formula de Haversine, DISTINCT ON
-- ---------------------------------------------------------------------
SELECT DISTINCT ON (a.id)
       a.nome                         AS area_ativa,
       p.nome                         AS ponto_seguro,
       ROUND(d.metros::numeric, 0)    AS distancia_metros
FROM area_risco a
CROSS JOIN ponto_seguro p
CROSS JOIN LATERAL (
    SELECT 2 * 6371000 * ASIN(SQRT(
               POWER(SIN(RADIANS(p.latitude - a.latitude) / 2), 2)
             + COS(RADIANS(a.latitude)) * COS(RADIANS(p.latitude))
             * POWER(SIN(RADIANS(p.longitude - a.longitude) / 2), 2))) AS metros
) d
WHERE a.ativa
  AND d.metros > a.raio_metros
ORDER BY a.id, d.metros;


-- ---------------------------------------------------------------------
-- 8) Quantidade de usuarios por perfil
--    Pergunta: "quantos administradores e cidadaos existem?"
--    Tecnicas: COUNT, GROUP BY
-- ---------------------------------------------------------------------
SELECT perfil, COUNT(*) AS usuarios
FROM usuario
GROUP BY perfil
ORDER BY perfil;
