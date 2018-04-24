package domains;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author bowen
 */
@Entity(name = "GAM_SHOWTYPE_SPU")
public class CategorySku extends BaseModel{

    @Id
    @GeneratedValue
    @Column(name = "RID")
    public int id;

    @Column(name = "ST_ID")
    public int categoryId;

    @Column(name = "SKUID")
    public int skuId;
}
