package domains;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author bowen
 */
@Entity(name = "GAM_SHOWTYPE")
public class Category extends BaseModel{

    @Id
    @GeneratedValue
    @Column(name = "ST_ID")
    public int id;

    @Column(name = "ST_NAME")
    public String name;

    @Column(name = "PARENT_ID")
    public int parentId;
}
