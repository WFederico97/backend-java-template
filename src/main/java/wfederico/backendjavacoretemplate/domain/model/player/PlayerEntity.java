package wfederico.backendjavacoretemplate.domain.model.player;

import jakarta.persistence.*;
import lombok.*;
import wfederico.backendjavacoretemplate.core.data.EntityBase;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerEntity extends EntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String position;

    @Column(nullable = false)
    private String alterPosition;


}
