package domains;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.math.BigDecimal;

/**
 * @author bowen
 */
@Entity(name = "GAM_SKU")
public class Sku extends BaseModel{

    @Id
    @GeneratedValue
    @Column(name = "SKUID")
    public int skuId;

    @Column(name = "GD_NAME")
    public String name;

    @Column(name = "BR_ID")
    public int brandId;

    @Column(name = "SALE_NUM")
    public int saleNum;

    @Column(name = "SALE_PRICE")
    public BigDecimal salePrice;
}
