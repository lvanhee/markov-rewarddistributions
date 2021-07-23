package markov.rewarddistributions;

import markov.probas.DiscreteProbabilityDistribution;
import markov.probas.DiscreteProbabilityDistributionAccuracyParameters;


public interface RewardDistribution extends DiscreteProbabilityDistribution<Double> {
	double getAverageReward();

	DiscreteProbabilityDistributionAccuracyParameters getPrecisionParameters();
}
