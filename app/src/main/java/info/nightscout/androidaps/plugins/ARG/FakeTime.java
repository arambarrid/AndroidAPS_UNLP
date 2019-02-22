package info.nightscout.androidaps.plugins.ARG;

import android.content.ContentValues;
import android.content.Context;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.Message;
import android.os.Handler;
import android.os.RemoteException;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import android.app.Notification;
import android.app.PendingIntent;

import android.content.res.Resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Objects;

public class FakeTime{

	private long originalStartTime = 0;
	private long fakeStarTime = 0;
	private double scale = 0;

	// s tiempo inicial
	// cuantas MS son 5 minutos
	FakeTime(long s, double min5){
		originalStartTime = System.currentTimeMillis();
		fakeStarTime = s;

		// min5 es cuantas ms serán 5 minutos.
		// 
		scale = (5*60*1000.0) / min5; 
		// multiplos enteros de 300000
		// 150000 (2,5min)  -> 2
		// 75000 (1,25min)  -> 4
		// 37500 (37,5 seg) -> 8
		// 18750 			-> 16
		// 9375  			-> 32
	}
	
	public long	getNow(){
		long originalNow = System.currentTimeMillis();
		long originalDiff = originalNow - originalStartTime;
		long fakeDiff = (long)((double)originalDiff * scale);
		return (fakeStarTime + fakeDiff);
	}
}