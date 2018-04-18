package domains;

import javax.persistence.*;

/**
 * @author bowen
 */
@Entity
@Table(name = "project")
public class Project extends BaseModel {

    @Id
    @GeneratedValue
    @Column(name = "prc_id")
    public int id;

    @Column(name = "prc_name", length = 50, nullable = false)
    public String prcName;
}
