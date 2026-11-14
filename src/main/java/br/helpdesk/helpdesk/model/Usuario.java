package br.helpdesk.helpdesk.model;
import br.helpdesk.helpdesk.model.enums.Perfil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

//Indica que a classe representa uma entidade no banco de dados
@Entity
//Defini o nome da tabela
@Table(name = "usuarios")
public class Usuario {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, length = 100)
        private String nome;

        //Define o email como obrigatório e único
        @Column(nullable = false,unique = true, length = 100)
        private String email;

        //Armazena o perfil como texto no banco
        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        private Perfil perfil;

        //Construtor vazio para criar entidade
        protected Usuario(){}

        public Usuario(String nome, String email, Perfil perfil){
            this.nome = nome;
            this.email = email;
            this.perfil = perfil;
        }

        //Retorna os dados do User
        public Long getId() {return id;}
        public String getNome() {return nome;}
        public String getEmail() {return email;}
        public Perfil getPerfil() {return perfil;}
}
