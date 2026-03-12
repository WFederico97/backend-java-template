package wfederico.backendjavacoretemplate.domain.player;

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
    private long id;

}
