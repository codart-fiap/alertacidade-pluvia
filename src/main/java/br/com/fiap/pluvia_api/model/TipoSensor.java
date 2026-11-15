package br.com.fiap.pluvia_api.model;

public enum TipoSensor {
    NIVEL_RIO("Nível do rio", "cm"),
    CHUVA("Chuva", "mm/h");

    private final String descricao;
    private final String unidade;

    TipoSensor(String descricao, String unidade) {
        this.descricao = descricao;
        this.unidade = unidade;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getUnidade() {
        return unidade;
    }
}
