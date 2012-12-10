package operation;

import java.util.Collection;

import utils.BussinesProduct;

import es.us.isa.FAMA.models.featureModel.Product;

public interface CostQuestion {
	public void setMaxCost(int cost);
	public Collection<BussinesProduct> getProducts();
}
