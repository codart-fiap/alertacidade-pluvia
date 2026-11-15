package br.com.fiap.pluvia_api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "area_risco")
public class AreaRisco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "raio_metros", nullable = false)
    private double raioMetros;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_risco", nullable = false, length = 10)
    private NivelRisco nivelRisco;

    @Column(nullable = false)
    private boolean ativa = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public double getRaioMetros() { return raioMetros; }
    public void setRaioMetros(double raioMetros) { this.raioMetros = raioMetros; }
    public NivelRisco getNivelRisco() { return nivelRisco; }
    public void setNivelRisco(NivelRisco nivelRisco) { this.nivelRisco = nivelRisco; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
}
