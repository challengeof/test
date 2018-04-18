package domains;

import javax.persistence.*;

/**
 * @author bowen
 */
@Entity
@Table(name = "usr")
public class User extends BaseModel{
    @Id
    @GeneratedValue
    @Column(name = "usr_id")
    public int id;

    @Column(length = 50, nullable = false, name = "usr_name")
    public String username;

    @Column(nullable = false, name = "prc_id")
    public int prcId;

}
