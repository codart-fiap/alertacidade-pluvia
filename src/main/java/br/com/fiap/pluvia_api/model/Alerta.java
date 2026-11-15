package br.com.fiap.pluvia_api.model;

import jakarta.persistence.*;

/**
 * Alerta gerado quando uma leitura atinge o limite do sensor.
 * A area de risco e o horario NAO sao guardados aqui: obtem-se pela leitura
 * (alerta -> leitura -> sensor -> area_risco), o que mantem a tabela na 3FN.
 * Cada leitura gera no maximo um alerta (leitura_id unico).
 */
@Entity
@Table(name = "alerta")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "leitura_id", nullable = false, unique = true)
    private LeituraSensor leitura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NivelRisco nivel;

    @Column(nullable = false)
    private String mensagem;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LeituraSensor getLeitura() { return leitura; }
    public void setLeitura(LeituraSensor leitura) { this.leitura = leitura; }
    public NivelRisco getNivel() { return nivel; }
    public void setNivel(NivelRisco nivel) { this.nivel = nivel; }
    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
}
