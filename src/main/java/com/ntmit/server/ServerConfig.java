package com.ntmit.server;

public final class ServerConfig {

	private ServerConfig() {
	}

	public static int computeSlots = 4;

	public static int computePoints = 100;

	public static int classesPerPoint = 1;

	public static int coreBaseSlots = 8;

	public static int intervalPerPoint = 1;

	public static int coreBaseInterval = 20;
	public static int intervalFloorMin = 1;

	public static int maxPollsPerTick = 16;

	public static int powerIdle = 5;

	public static int powerPerDoubling = 20;

	public static int powerReferencePoints = 100;

	private static final double LN_2 = Math.log(2D);

	public static double powerDraw(double computePoints) {
		return powerIdle + powerPerDoubling * (Math.log(1D + computePoints / powerReferencePoints) / LN_2);
	}

	public static double heatPerPowerUnit = 1.0D;

	public static double heatPerTick(double computePoints) {
		return powerDraw(computePoints) * heatPerPowerUnit;
	}

	public static int maxHeat = 100_000;

	public static int heatExchangerMaxHeat = 100_000;

	public static int maxHeatExchangerGroup = 16;

	public static int heatExchangerTransferRate = 200;

	public static int heatExchangerTankSize = 16000;

	public static boolean debugHeatLog = true;

	public static int connectorPollInterval = 2;

	public static long maxPower = 1_000L;

	public static int powerPerTask = 10;

	public static int throttleFactor = 4;

	public static int groupRefreshInterval = 20;

	public static int dispatchTargetLimit = 32;

	public static int dispatchPerTick = 8;
}
