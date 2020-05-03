package com.feeyo.net.nio.util;

import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 弱精度的计时器，考虑性能不使用同步策略。
 */
public class TimeUtil {
	//
	// 独立，避免被其他任务影响
	private static final ScheduledExecutorService updateScheduler = Executors.newSingleThreadScheduledExecutor();
	//
	private static volatile long CURRENT_MILLIS_TIME = System.currentTimeMillis();
	private static volatile int CURRENT_TIME_SECOND_INDEX = (int) ((CURRENT_MILLIS_TIME / 1000) % 60);
	//
	// Zero time
	private static volatile int TODAYS_ZERO_SECONDS_TIME = 0;
	private static volatile int TOMORROWS_ZERO_SECONDS_TIME = 0;
	private static volatile long TOMORROWS_ZERO_MILLIS_TIME = 0;
	
	private static volatile int OFFSET = 0;

	static {
		Calendar cal = Calendar.getInstance();
		int zoneOffset = cal.get(Calendar.ZONE_OFFSET);
		int dstOffset = cal.get(Calendar.DST_OFFSET);
		OFFSET = zoneOffset + dstOffset;
		//
		update();
		//
		// 弱精度的计时器， 定时更新
		updateScheduler.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				update();
			}
		}, 0, 2L, TimeUnit.MILLISECONDS);
	}
	

	public static final int todaysZeroSeconds() {
		return TODAYS_ZERO_SECONDS_TIME;
	}
	
	public static final int tomorrowsZeroSeconds() {
		return TOMORROWS_ZERO_SECONDS_TIME;
	}
	
	// UTC 毫秒
	public static final long currentUtcTimeMillis() {
		return CURRENT_MILLIS_TIME - OFFSET;
	}

	// 毫秒
	public static final long currentTimeMillis() {
		return CURRENT_MILLIS_TIME;
	}
	
	public static final int currentTimeSecondIndex() {
		return CURRENT_TIME_SECOND_INDEX;
	}

	private static final void update() {
		CURRENT_MILLIS_TIME = System.currentTimeMillis();
		CURRENT_TIME_SECOND_INDEX = (int) ((CURRENT_MILLIS_TIME / 1000) % 60);
		//
		if ( CURRENT_MILLIS_TIME > TOMORROWS_ZERO_MILLIS_TIME ) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			//
			TODAYS_ZERO_SECONDS_TIME =  (int) (cal.getTime().getTime() / 1000L);
			TOMORROWS_ZERO_SECONDS_TIME = TODAYS_ZERO_SECONDS_TIME + ( 24 * 3600 );
			TOMORROWS_ZERO_MILLIS_TIME = TOMORROWS_ZERO_SECONDS_TIME * 1000L;
		}
	}
	
	// 统计耗时, 单位：毫秒
    public static long since(long beginTime) {
        return CURRENT_MILLIS_TIME - beginTime;
    }
}