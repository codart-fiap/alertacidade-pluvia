package br.com.fiap.pluvia_api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sensor")
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoSensor tipo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "area_risco_id", nullable = false)
    private AreaRisco areaRisco;

    @Column(name = "limite_alerta", nullable = false)
    private double limiteAlerta;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public TipoSensor getTipo() { return tipo; }
    public void setTipo(TipoSensor tipo) { this.tipo = tipo; }
    public AreaRisco getAreaRisco() { return areaRisco; }
    public void setAreaRisco(AreaRisco areaRisco) { this.areaRisco = areaRisco; }
    public double getLimiteAlerta() { return limiteAlerta; }
    public void setLimiteAlerta(double limiteAlerta) { this.limiteAlerta = limiteAlerta; }
}
