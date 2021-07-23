package markov.rewarddistributions;

public interface RewardDistributionAlterer {

	public static final RewardDistributionAlterer NO_ALTERATION = new RewardDistributionAlterer()
	{
		public RewardDistribution getAlteredPerceptionWhenComparingActionsFrom(RewardDistribution da) {return da;}
		public boolean isCumulatedAlteredPerceptionOverTheFuture() {return false;}
	};

	RewardDistribution getAlteredPerceptionWhenComparingActionsFrom(RewardDistribution da);
	boolean isCumulatedAlteredPerceptionOverTheFuture();

}
