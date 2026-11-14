package br.helpdesk.helpdesk.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "historico_chamado")
public class HistoricoChamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chamado_id")
    private Chamado chamado;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    //Construtor vazio para criar entidade
    protected HistoricoChamado() {}

    //Novo histórico de chamado
    public HistoricoChamado(Chamado chamado, String descricao) {
        this.chamado = chamado;
        this.descricao = descricao;
        this.dataHora = LocalDateTime.now();
    }

    public Long getId() {return id;}
    public Chamado getChamado() {return chamado;}
    public String getDescricao() {return descricao;}
    public LocalDateTime getDataHora() {return dataHora;}
}
