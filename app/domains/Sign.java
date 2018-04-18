package domains;

import javax.persistence.*;
import java.util.Date;

/**
 * @author bowen
 */
@Entity
@Table(name = "sign")
public class Sign extends BaseModel {

    @Id
    @GeneratedValue
    public int id;

    @Column(name = "usr_id")
    public int userId;

    @Column(name = "sign_time", nullable = false)
    public Date signTime;

    @Column(name = "prc_id", nullable = false)
    public int prcId;
}
