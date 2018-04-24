package domains;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author bowen
 */
@Entity(name = "GAM_GDBRAND")
public class Brand extends BaseModel{

    @Id
    @GeneratedValue
    @Column(name = "BR_ID")
    public int id;

    @Column(name = "BR_NAME")
    public String name;

    @Column(name = "ORDER_VAL")
    public int order;
}
