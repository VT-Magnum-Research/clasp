package operation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import utils.BussinesProduct;
import utils.ZeroOneKnapsack;
import choco.Choco;
import choco.cp.solver.CPSolver;
import choco.kernel.model.Model;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.integer.IntegerExpressionVariable;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.Solver;
import choco.kernel.solver.variables.integer.IntDomainVar;
import es.us.isa.ChocoReasoner.ChocoResult;
import es.us.isa.ChocoReasoner.attributed.ChocoQuestion;
import es.us.isa.ChocoReasoner.attributed.ChocoReasoner;
import es.us.isa.FAMA.Benchmarking.PerformanceResult;
import es.us.isa.FAMA.Reasoner.Reasoner;
import es.us.isa.FAMA.models.FAMAAttributedfeatureModel.AttributedFeature;
import es.us.isa.FAMA.models.featureModel.GenericFeature;
import es.us.isa.FAMA.models.featureModel.Product;
import fitnessfunctions.Fitness;

public class ChocoCostOperationKnapSack extends ChocoQuestion implements CostQuestion {

	public boolean useInCSPCostConstraint=true;
	
	private int costMax;
	private double profit;
	public Set<Product> products;
	Collection<BussinesProduct> return_col;
	Fitness fitness;

	public double getProfit(){return profit;}
	
	public ChocoCostOperationKnapSack(Fitness f) {
		this.costMax = 0;
		this.products = new HashSet<Product>();
		fitness = f;
		return_col= new ArrayList<BussinesProduct>();
	}

	public PerformanceResult answer(Reasoner r) {
		ChocoReasoner reasoner = (ChocoReasoner) r;

		ChocoResult res = new ChocoResult();
		Solver sol = new CPSolver();
		Model p = reasoner.getProblem();
		Map<String, IntegerVariable> atributesVar = reasoner
				.getAttributesVariables();

		// primero cramos la coleccion con los atributos que nos interesan
		// dependiendo de la cadena de entrada

		Collection<IntegerVariable> selectedAtts = new ArrayList<IntegerVariable>();
		Iterator<Entry<String, IntegerVariable>> atributesIt = atributesVar
				.entrySet().iterator();
		while (atributesIt.hasNext()) {
			Entry<String, IntegerVariable> entry = atributesIt.next();
			if (entry.getKey().contains("." + "cost")) {
				selectedAtts.add(entry.getValue());
			}
		}

		// Ahora necesitamos crear una variable suma de todos los atributos
		// anteriores"
		IntegerVariable[] reifieds = new IntegerVariable[selectedAtts.size()];

		IntegerVariable suma = Choco
				.makeIntVar("_suma", 0, selectedAtts.size());// modify that TODO
		IntegerExpressionVariable sumatorio = Choco.sum(selectedAtts
				.toArray(reifieds));
		Constraint sumReifieds = Choco.eq(suma, sumatorio);

		p.addConstraint(sumReifieds);
		// wont allow products that exceed the maximum cost allowed
		Constraint lessThan = Choco.leq(suma, costMax);
		p.addConstraint(lessThan);
		sol.read(p);
		try {
			sol.propagate();
		} catch (ContradictionException e1) {
			System.err.println("No solutions with this cost can be find");
			// e1.printStackTrace();
			return res;
		}
		// Obtener todo los valores que tengan ese valor
		if (sol.solve() == Boolean.TRUE && sol.isFeasible()) {
			do {
				BussinesProduct prod = new BussinesProduct();
				for (int i = 0; i < p.getNbIntVars(); i++) {
					IntDomainVar aux = sol.getVar(p.getIntVar(i));
					if (aux.getVal() > 0) {
						AttributedFeature f = (AttributedFeature) getFeature(
								aux, reasoner);
						// Only get the leafs
						if (f != null && f.getNumberOfRelations() == 0) {
							prod.addFeature(f);
						}
					}
				}
				prod.setCost(sol.getVar(suma).getVal());
				prod.setValue(fitness.getValue(prod));
				products.add(prod);
			} while (sol.nextSolution() == Boolean.TRUE);
		}

		System.out.println(products.size());
		//products= get30000(products);
		//long start = System.currentTimeMillis();
		
		ZeroOneKnapsack knapsack = new ZeroOneKnapsack(new ArrayList(products), costMax);
		//System.out.println(System.currentTimeMillis()-start);
		return_col = knapsack.calcSolution();
		profit = knapsack.getProfit();
		return res;
		
	}
	
	private GenericFeature getFeature(IntDomainVar aux, ChocoReasoner reasoner) {
		String temp = new String(aux.toString().substring(0,
				aux.toString().indexOf(":")));
		GenericFeature f = reasoner.searchFeatureByName(temp);
		return f;
	}

	@Override
	public void setMaxCost(int cost) {
		this.costMax=cost;
		
	}

	@Override
	public Collection<BussinesProduct> getProducts() {
		return return_col;
	}
	private Set<Product> get30000(Collection<Product> products2) {
		Set<Product> res = new HashSet<Product>();
		int i=0;
		for(Iterator<Product> it=products2.iterator();it.hasNext()&&i<30000;i++){
			res.add(it.next());
		}
		return res;
	}
}
