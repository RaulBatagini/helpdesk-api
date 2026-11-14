package br.helpdesk.helpdesk.model;

import java.time.LocalDateTime;

import br.helpdesk.helpdesk.model.enums.Categoria;
import br.helpdesk.helpdesk.model.enums.Prioridade;
import br.helpdesk.helpdesk.model.enums.StatusChamado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

//Indica que a classe representa uma entidade no banco de dados
@Entity
//Defini o nome da tabela
@Table(name = "chamados")
public class Chamado{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String descricao;

    //Armazena a categoria como texto no banco
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    //Armazena a prioridade como texto no banco
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Prioridade prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusChamado status;

    // Define o relacionamento de muitos chamados para um solicitante sendo obrigatorio o solicitante
    @ManyToOne(optional = false)
    @JoinColumn(name = "solicitante_id")
    private Usuario solicitante;

    // Define o relacionamento de muitos chamados para um técnico
    @ManyToOne
    @JoinColumn(name = "tecnico_id")
    private Usuario tecnico;

    // Define a data de abertura como obrigatória.
    @Column(nullable = false)
    private LocalDateTime dataAbertura;

    //Construtor vazio para criar entidade
    protected Chamado() {
    }

    //Construtor usado para criar um novo chamado
    public Chamado(String titulo, String descricao, Categoria categoria, Prioridade prioridade,
                   Usuario solicitante) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.categoria = categoria;
        this.prioridade = prioridade;
        this.solicitante = solicitante;
        //Defini o status de um novo chamado como aberto, regra de negocio
        this.status = StatusChamado.ABERTO;
        this.dataAbertura = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public Categoria getCategoria() { return categoria; }
    public Prioridade getPrioridade() { return prioridade; }
    public StatusChamado getStatus() { return status; }
    public Usuario getSolicitante() { return solicitante; }
    public Usuario getTecnico() { return tecnico; }
    public LocalDateTime getDataAbertura() { return dataAbertura; }

    //Alterar a prioridade do chamado
    public void setPrioridade(Prioridade prioridade) {
        this.prioridade = prioridade;
    }

    //Alterar status do chamado
    public void setStatus(StatusChamado status) {
        this.status = status;
    }

    //Definir técnico responsável
    public void setTecnico(Usuario tecnico) {
        this.tecnico = tecnico;
    }
}
