package domains;

import javax.persistence.*;

/**
 * @author bowen
 */
@Entity
@Table(name = "manager")
public class Manager extends BaseModel{

    @Id
    @GeneratedValue
    @Column(name = "mng_id")
    public int id;

    @Column(name = "mng_name", length = 50, nullable = false)
    public String name;

    @Column(name = "mng_psw", length = 50, nullable = false)
    public String password;
}
