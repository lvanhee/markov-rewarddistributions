package markov.rewarddistributions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import markov.probas.DiscreteProbabilityDistribution;
import markov.probas.DiscreteProbabilityDistributionImpl;
import markov.probas.DiscreteProbabilityDistributionAccuracyParameters;

public class RewardDistributionMapImpl implements RewardDistribution {
	
	private final DiscreteProbabilityDistribution<Double> distr;
	private final DiscreteProbabilityDistributionAccuracyParameters accuracyParams;
	
	private RewardDistributionMapImpl(DiscreteProbabilityDistribution<Double> pdd, DiscreteProbabilityDistributionAccuracyParameters accuracyParams) {
		this.distr = DiscreteProbabilityDistributionImpl.newInstance(pdd, accuracyParams);
		this.accuracyParams = accuracyParams;
	}

	public RewardDistributionMapImpl(Map<Double, Double> m, DiscreteProbabilityDistributionAccuracyParameters params2) {
		this.distr = DiscreteProbabilityDistributionImpl.newInstance(m, params2);
		this.accuracyParams = params2;
	}

	@Override
	public double getProbabilityOf(Double reward) {
		return distr.getProbabilityOf(reward);
	}
	
	@Override
	public Map<Double, Double> getMap() {
		return distr.getMap();
	}
	
	public String toString()
	{
		String res="";
		SortedSet<Double> sorted = new TreeSet<Double>(distr.getItems());
		for(Double d:sorted)
		{
			res+=d+"->"+distr.getProbabilityOf(d)+",";
		}
		return res.substring(0,res.length()-1);
	}
	
	public boolean equals(Object o)
	{
		if(o==this) return true;
		return distr.equals(((RewardDistributionMapImpl)o).distr);
	}
	
	public int hashCode()
	{
		return distr.hashCode();
	}

	public static RewardDistribution 
	newInstanceAdd(RewardDistribution newInstanceMerge, double rewardForPlaying, DiscreteProbabilityDistributionAccuracyParameters params) {
		Map<Double,Double> res = new HashMap<Double, Double>();
		for(Double d: newInstanceMerge.getMap().keySet())
		{
			//fixiung issues caused by merging doubles
			double next = d+rewardForPlaying;
			if(!res.containsKey(next))
				res.put(next, 0d);
			res.put(next, res.get(next)+newInstanceMerge.getProbabilityOf(d));
		}
		return newInstance(res, params);
	}

	public static Set<RewardDistribution> newInstanceWeightedMergeOfRewardDistributions(Map<Set<RewardDistribution>, Double>
	setOfExpectationsToTheirWeight,	double d, DiscreteProbabilityDistributionAccuracyParameters params)
	{
		
		Map<List<RewardDistribution>, Set<RewardDistribution>>originalSet 
		= new HashMap<List<RewardDistribution>, Set<RewardDistribution>>();
		
		List<List<RewardDistribution>>orderedLists = new LinkedList<List<RewardDistribution>>();
		
		Map<Integer, Double> weightPerIndex = new HashMap<Integer, Double>();
		Map<RewardDistribution, Integer> indexOf = new HashMap<>();
		
		for(Set<RewardDistribution>s: setOfExpectationsToTheirWeight.keySet())
		{
			List<RewardDistribution>toList = s.stream().collect(Collectors.toList());
			originalSet.put(toList, s);
			orderedLists.add(toList);
			
			for(RewardDistribution rd: s)
				indexOf.put(rd, weightPerIndex.size());
			weightPerIndex.put(weightPerIndex.size(), setOfExpectationsToTheirWeight.get(s));
			
		}
		
		
		Set<List<RewardDistribution>> em = RewardDistributions.cartesianProduct(orderedLists
				);
		
		Set<RewardDistribution> res = new HashSet<RewardDistribution>();
		for(List<RewardDistribution> tupleList:em)
		{
			Map<RewardDistribution, Double> m = new HashMap<RewardDistribution, Double>();
			for(RewardDistribution expectationMap:tupleList)
			{
				if(!m.containsKey(expectationMap))
					m.put(expectationMap, 0d);
				m.put(expectationMap, m.get(expectationMap)+weightPerIndex.get(indexOf.get(expectationMap)));
			}
			
			res.add(newInstanceMerge(m,params));
		}
		
		res = res.stream().map(x->newInstanceAdd(x, d,params))
				.collect(Collectors.toSet());
		return res;
	}
	
	
	
	public static RewardDistribution newInstanceMerge(Map<RewardDistribution, Double> m, DiscreteProbabilityDistributionAccuracyParameters params) {
		Map<DiscreteProbabilityDistribution<Double>, Double> m2 = (Map) m;
		return newInstance(DiscreteProbabilityDistributionImpl.newInstanceMerge(m2,params), params);
	}

	@Override
	public double getAverageReward() {return distr.getItems().stream().map(x->x*distr.getProbabilityOf(x)).reduce((x,y)->x+y).get();}

	@Override
	public Set<Double> getItems() {return distr.getItems();}

	public static RewardDistribution newInstance(DiscreteProbabilityDistribution<Double> pdd,DiscreteProbabilityDistributionAccuracyParameters params) {
		return new RewardDistributionMapImpl(pdd,params);
	}

	public static RewardDistribution newInstanceMerge(DiscreteProbabilityDistribution<RewardDistribution> x, 
			DiscreteProbabilityDistributionAccuracyParameters params) {
		return newInstance(DiscreteProbabilityDistributionImpl.newInstanceMerge((Map)x.getMap(),params), params);
	}

	@Override
	public DiscreteProbabilityDistributionAccuracyParameters getPrecisionParameters() {
		return accuracyParams;
	}
	
	public static RewardDistribution newInstance(double r, DiscreteProbabilityDistributionAccuracyParameters params) {
		Map<Double, Double> m = new HashMap<Double, Double>();
		m.put(r, 1d);
		return new RewardDistributionMapImpl(m, params);
	}

	public static RewardDistribution newInstance(Map<Double, Double> res, DiscreteProbabilityDistributionAccuracyParameters params) {
		if(res.isEmpty())throw new Error();
		return new RewardDistributionMapImpl(res, params);
	}
}
