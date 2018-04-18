package domains;

import javax.persistence.*;

/**
 * @author bowen
 */
@Entity
@Table(name = "privilege")
public class Privilege extends BaseModel{

    @Id
    @GeneratedValue
    public int id;

    @Column(name = "mng_id")
    public int managerId;

    @Column(name = "prc_id", nullable = false)
    public int prcId;
}
