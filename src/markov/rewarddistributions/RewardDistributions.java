package markov.rewarddistributions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import cachingutils.ExactEqualCache;
import finitestatemachine.Action;
import finitestatemachine.State;
import markov.GeneralizedValueFunction;
import markov.MDP;
import markov.Policy;
import markov.impl.Policies;
import markov.impl.ValueFunctions;
import markov.probas.DiscreteProbabilityDistribution;
import markov.probas.DiscreteProbabilityDistributionAccuracyParameters;

/**
 * 
 * @author loisv
 * 20210109
 *
 */
public class RewardDistributions {
	
	private static ExactEqualCache<Policy, GeneralizedValueFunction> eeCache = ExactEqualCache.newInstance(10); 

	public static<S extends State, A extends Action> GeneralizedValueFunction<S, RewardDistribution> 
	getValueFunction(
			MDP<S, A> mdp,
			Policy<S,A> pol,
			int horizon,
			DiscreteProbabilityDistributionAccuracyParameters params
			)
	{
		synchronized (eeCache) {
			if(eeCache.has(pol))
				return eeCache.get(pol);	
		}
		List<Object[]> cache = new ArrayList<>();
		
		BiFunction<S,A,RewardDistribution> rewardFor = (x,y)->
		{
			double reward = mdp.getRewardFor(x, y);
			return (RewardDistribution)RewardDistributionMapImpl.newInstance(reward, params);
		};
		
		Function<S, RewardDistribution> initValue = x->RewardDistributionMapImpl.newInstance(0, params);
		BiFunction<RewardDistribution, Double, RewardDistribution> valueAdder = (x,y)->RewardDistributionMapImpl.newInstanceAdd(x, y,params);
		Function<DiscreteProbabilityDistribution<RewardDistribution>, RewardDistribution> merger = x-> RewardDistributionMapImpl.newInstanceMerge(x,params);
		
		Comparator<RewardDistribution> comp = (RewardDistribution x, RewardDistribution y)->{throw new Error();};
		GeneralizedValueFunction<S, RewardDistribution> res ;
		res = 
				ValueFunctions.getValueUsingValueIterationOnTable(
						mdp,
						horizon, 
						comp,         				
						rewardFor,
						initValue, 
						Policies.toActionRestricter(pol), 
						valueAdder, 
						merger,
						params,
						cache
						);
		synchronized (eeCache) {
			eeCache.add(pol, res);
		}
		return res;
	}
	
	public static<S extends State, A extends Action> GeneralizedValueFunction<S, RewardDistribution> 
	getSkewedValueFunction(
			MDP<S, A> mdp,
			Policy<S,A> pol,
			int horizon,
			double skew,
			DiscreteProbabilityDistributionAccuracyParameters params
			)
	{
		GeneralizedValueFunction<S, RewardDistribution> originalValue = getValueFunction(mdp, pol, horizon, params);
		return s-> newInstanceRemovingTheProbabilityForUpperRewards(originalValue.apply(s), skew, params);
	}
	
	public static<S extends State, A extends Action> GeneralizedValueFunction<S, Double> 
	getAverageOfSkewedValueFunction(
			MDP<S, A> mdp, Policy<S,A> pol,	int horizon, double skew, DiscreteProbabilityDistributionAccuracyParameters params
			)
	{
		GeneralizedValueFunction<S, RewardDistribution> originalValue = getValueFunction(mdp, pol, horizon, params);
		return s-> newInstanceRemovingTheProbabilityForUpperRewards(originalValue.apply(s), skew, params).getAverageReward();
	}

	public static RewardDistribution newInstanceRemovingTheProbabilityForUpperRewards(RewardDistribution da, double probaRemoved,
			DiscreteProbabilityDistributionAccuracyParameters params
			) {
		Map<Double, Double> res = new HashMap<>();
		TreeSet<Double> sortedRewardDistributions = new TreeSet<Double>();
		sortedRewardDistributions.addAll(da.getItems());
		
		double totalDensityCovered = 0;
		double originalProbaToPreserve = 1 - probaRemoved;
		double scalingFactor = 1 / originalProbaToPreserve;
		boolean over = false;
		for(Double reward:sortedRewardDistributions)
		{
			double probaForTheCurrentReward = da.getProbabilityOf(reward);
			if(probaForTheCurrentReward + totalDensityCovered > originalProbaToPreserve)
			{
				probaForTheCurrentReward = (originalProbaToPreserve - totalDensityCovered);
				over = true;
			}
			totalDensityCovered += probaForTheCurrentReward;
			res.put(reward,probaForTheCurrentReward * scalingFactor);
			if(over) break;
		}
	
		return RewardDistributionMapImpl.newInstance(res, params);
	}

	public static String toCumulativeTikzString(RewardDistribution rd) {
		double total = 0;
		Set<Double> sorted = new TreeSet<>(rd.getItems());
		
		String res = "";
		for(Double d: sorted)
		{
			total +=rd.getProbabilityOf(d);
			res +="("+d+","+total+")";
		}
		return res;
	}

	public static boolean isStronglyDominating(RewardDistribution r1, RewardDistribution r2) {
		if(r1.equals(r2))return false;
		
		SortedSet<Double> allRewardValues = new TreeSet<>();
		allRewardValues.addAll(r1.getItems());
		allRewardValues.addAll(r2.getItems());
		
		SortedSet<Double> rewardsOfR1 = new TreeSet<>(r1.getItems());
		SortedSet<Double> rewardsOfR2 = new TreeSet<>(r2.getItems());
		
		double lastRewardR1 = rewardsOfR1.first();
		double lastRewardR2 = rewardsOfR2.first();
		double coveredPdiR1 = 0d;
		double coveredPdiR2 = 0d;
		
		for(Double d:allRewardValues)
		{
			if(r1.getItems().contains(d)) {coveredPdiR1 +=r1.getProbabilityOf(d); lastRewardR1 = d;}
			if(r2.getItems().contains(d)) {coveredPdiR2 +=r2.getProbabilityOf(d); lastRewardR2 = d;}
			if(lastRewardR1<lastRewardR2)
				return false;
			if(coveredPdiR1>coveredPdiR2+0.000001d) //approximation errors
				return false;
		}
		return true;
	}
	
	public static<V> Set<List<V>> cartesianProduct(List<List<V>> sets) {
		if(sets.size()==1)
			return sets.stream().collect(Collectors.toSet());
	    if (sets.size() < 2)
	        throw new IllegalArgumentException("Can't create a product of fewer than two lists (got " +sets.size() + ")");

	    return _internalCartesianProduct(0, sets);
	}
	
	private static<V> Set<List<V>> _internalCartesianProduct(int i, List<List<V>> distributions) {
	    Set<List<V>> res = new HashSet<List<V>>();
	    if (i == distributions.size()) res.add(new LinkedList<V>());
	    else 
	    	for (V obj : distributions.get(i)) 
	    		for (List<V> set : _internalCartesianProduct(i+1, distributions)) 
	    		{
	    			set.add(obj);
	    			res.add(set);
	    		}

	    return res;
	}

}
