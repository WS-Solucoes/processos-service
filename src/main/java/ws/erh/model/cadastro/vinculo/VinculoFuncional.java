package ws.erh.model.cadastro.vinculo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "vinculo_funcional")
@Getter
@Setter
public class VinculoFuncional {

    @Id
    private Long id;

    @Column(name = "matricula")
    private String matricula;
}
