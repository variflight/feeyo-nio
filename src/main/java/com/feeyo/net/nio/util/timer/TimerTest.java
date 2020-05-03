package com.feeyo.net.nio.util.timer;

import java.util.concurrent.TimeUnit;

import com.feeyo.net.nio.NameableThreadFactory;


public class TimerTest {

	public static void main(String[] args) {
		//
		HashedWheelTimer timer = new HashedWheelTimer(new NameableThreadFactory("timer-", false), 
				50, TimeUnit.MILLISECONDS, 4096);
		//
		timer.newTimeout(new TimerTask() {
			@Override
			public void run(Timeout timeout) throws Exception {
				System.out.println("xxx--" + System.currentTimeMillis());
			}
		}, 2, TimeUnit.SECONDS);

	}

}
