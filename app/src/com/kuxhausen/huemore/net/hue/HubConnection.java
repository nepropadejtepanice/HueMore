package com.kuxhausen.huemore.net.hue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import alt.android.os.CountDownTimer;
import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.util.Pair;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.kuxhausen.huemore.MoodExecuterService;
import com.kuxhausen.huemore.net.Connection;
import com.kuxhausen.huemore.net.DeviceManager;
import com.kuxhausen.huemore.network.NetworkMethods;
import com.kuxhausen.huemore.network.BulbAttributesSuccessListener.OnBulbAttributesReturnedListener;
import com.kuxhausen.huemore.persistence.DatabaseDefinitions.InternalArguments;
import com.kuxhausen.huemore.persistence.DatabaseDefinitions.NetBulbColumns;
import com.kuxhausen.huemore.persistence.DatabaseDefinitions.NetConnectionColumns;
import com.kuxhausen.huemore.state.Group;
import com.kuxhausen.huemore.state.api.BulbAttributes;
import com.kuxhausen.huemore.state.api.BulbState;

public class HubConnection implements Connection, OnBulbAttributesReturnedListener{

	private static final String[] columns = {BaseColumns._ID, NetConnectionColumns.TYPE_COLUMN, NetConnectionColumns.NAME_COLUMN, NetConnectionColumns.DEVICE_ID_COLUMN, NetConnectionColumns.JSON_COLUMN};
	private static final Integer type = NetBulbColumns.NetBulbType.PHILIPS_HUE;
	private static final Gson gson = new Gson();
	
	private Integer mBaseId;
	private String mName, mDeviceId;
	private HubData mData;
	private Context mContext;
	
	
	public HubConnection(Context c, Integer baseId, String name, String deviceId, HubData data, DeviceManager dm){
		mContext = c;
		mBaseId = baseId;
		mName = name;
		mDeviceId = deviceId;
		mData = data;
		
		
		//junk?
		mDeviceManager = dm;
		volleyRQ = Volley.newRequestQueue(mContext);
		restartCountDownTimer();
	}
	
	public void saveConnection(){
		// TODO Auto-generated method stub
	}	
	
	@Override
	public void initializeConnection(Context c) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void onDestroy() {
		if(countDownTimer!=null)
			countDownTimer.cancel();
		volleyRQ.cancelAll(InternalArguments.TRANSIENT_NETWORK_REQUEST);
		volleyRQ.cancelAll(InternalArguments.PERMANENT_NETWORK_REQUEST);
	}
	
	public static ArrayList<HubConnection> loadHubConnections(Context c, DeviceManager dm){
		ArrayList<HubConnection> hubs = new ArrayList<HubConnection>();
		
		String[] selectionArgs = {""+NetBulbColumns.NetBulbType.PHILIPS_HUE};
		Cursor cursor = c.getContentResolver().query(NetConnectionColumns.URI, columns, NetConnectionColumns.TYPE_COLUMN + " = ?", selectionArgs, null);
		cursor.moveToPosition(-1);// not the same as move to first!
		while (cursor.moveToNext()) {
			Integer baseId = cursor.getInt(0);
			String name = cursor.getString(2);
			String deviceId = cursor.getString(3);
			HubData data = gson.fromJson(cursor.getString(4), HubData.class);
			hubs.add(new HubConnection(c,baseId,name,deviceId,data, dm));
		}
		
		//initialize all connections
		for(HubConnection h : hubs)
			h.initializeConnection(c);
		
		return hubs;
	}
	
	
	
	
	
	private DeviceManager mDeviceManager;
	private RequestQueue volleyRQ;
	private static CountDownTimer countDownTimer;
	private final static int TRANSMITS_PER_SECOND = 10;
	private final static int MAX_STOP_SELF_COUNDOWN = TRANSMITS_PER_SECOND*3;
	public enum KnownState {Unknown, ToSend, Getting, Synched};	
	public Integer maxBrightness;
	public int[] bulbBri;
	public int[] bulbRelBri;
	public KnownState[] bulbKnown;
	public String groupName;
	private static int MAX_REL_BRI = 255;
	
	boolean groupIsColorLooping=false;
	boolean groupIsAlerting=false;
	public Queue<Pair<Integer,BulbState>> queue = new LinkedList<Pair<Integer,BulbState>>();
	int transientIndex = 0;
	
	
	public String getGroupName(){
		return groupName;
	}
	
	public Integer getMaxBrightness(){
		return maxBrightness;
	}
	
	public RequestQueue getRequestQueue() {
		return volleyRQ;
	}
	
	public synchronized void onGroupSelected(Group selectedGroup, Integer optionalBri, String groupName){
		
		groupIsAlerting = false;
		groupIsColorLooping = false;
		maxBrightness = null;
		bulbBri = new int[selectedGroup.groupAsLegacyArray.length];
		bulbRelBri = new int[selectedGroup.groupAsLegacyArray.length];
		bulbKnown = new KnownState[selectedGroup.groupAsLegacyArray.length];
		for(int i = 0; i < bulbRelBri.length; i++){
			bulbRelBri[i] = MAX_REL_BRI;
			bulbKnown[i] = KnownState.Unknown;
		}
		
		this.groupName = groupName;
		
		if(optionalBri==null){
			for(int i = 0; i< selectedGroup.groupAsLegacyArray.length; i++){
				bulbKnown[i] = KnownState.Getting;
				NetworkMethods.PreformGetBulbAttributes(mContext, getRequestQueue(), mDeviceManager, this, selectedGroup.groupAsLegacyArray[i]);
			}
		} else {
			maxBrightness = optionalBri;
			for(int i = 0; i< selectedGroup.groupAsLegacyArray.length; i++)
				bulbKnown[i] = KnownState.ToSend;
			mDeviceManager.onBrightnessChanged();
		}
	}
	
	/** doesn't notify listeners **/
	public synchronized void setBrightness(int brightness, Group selectedGroup){
		
		maxBrightness = brightness;
		if(selectedGroup.groupAsLegacyArray!=null){
			for(int i = 0; i< selectedGroup.groupAsLegacyArray.length; i++){
				bulbBri[i] = (maxBrightness * bulbRelBri[i])/MAX_REL_BRI; 
				bulbKnown[i] = KnownState.ToSend;
			}
		}
	}
	
	public void onAttributesReturned(BulbAttributes result, int bulbNumber) {
		//figure out which bulb in group (if that group is still selected)
		int index = calculateBulbPositionInGroup(bulbNumber, mDeviceManager.getSelectedGroup());
		//if group is still expected this, save 
		if(index>-1 && bulbKnown[index]==KnownState.Getting){
			bulbKnown[index] = KnownState.Synched;
			bulbBri[index] = result.state.bri;
			
			//if all expected get brightnesses have returned, compute maxbri and notify listeners
			boolean anyOutstandingGets = false;
			for(KnownState ks : bulbKnown)
				anyOutstandingGets |= (ks == KnownState.Getting);
			if(!anyOutstandingGets){
				//todo calc more intelligent bri when mood known
				int briSum = 0;
				for(int bri : bulbBri)
					briSum +=bri;
				maxBrightness = briSum/mDeviceManager.getSelectedGroup().groupAsLegacyArray.length;
				
				for(int i = 0; i< mDeviceManager.getSelectedGroup().groupAsLegacyArray.length; i++){
					bulbBri[i]= maxBrightness;
					bulbRelBri[i] = MAX_REL_BRI;
				}
				
				mDeviceManager.onBrightnessChanged();
			}	
		}	
	}
	/** finds bulb index within group[] **/
	private int calculateBulbPositionInGroup(int bulbNumber, Group selectedGroup){
		if(selectedGroup==null || selectedGroup.groupAsLegacyArray==null)
			return -1;
		
		int index = -1;
		for(int j = 0; j< selectedGroup.groupAsLegacyArray.length; j++){
			if(selectedGroup.groupAsLegacyArray[j]==bulbNumber)
				index = j;
		}
		return index;
	}
	
	private boolean hasTransientChanges() {
		if(bulbKnown==null)
			return false;		
		boolean result = false;
		for (KnownState ks : bulbKnown)
			result |= (ks==KnownState.ToSend);
		return result;
	}
	
	public void restartCountDownTimer() {
		
		if (countDownTimer != null)
			countDownTimer.cancel();

		transientIndex = 0;
		// runs at the rate to execute 15 op/sec
		countDownTimer = new CountDownTimer(Integer.MAX_VALUE, (1000 / TRANSMITS_PER_SECOND)) {

			@Override
			public void onFinish() {
			}

			@Override
			public void onTick(long millisUntilFinished) {
				if (queue.peek()!=null) {
					Pair<Integer,BulbState> e = queue.poll();
					int bulb = e.first;
					BulbState state = e.second;
					
					//remove effect=none except when meaningful (after spotting an effect=colorloop)
					if(state.effect!=null && state.effect.equals("colorloop")){
						groupIsColorLooping = true;
					}else if(!groupIsColorLooping){
						state.effect = null;
					}
					//remove alert=none except when meaningful (after spotting an alert=colorloop)
					if(state.alert!=null && (state.alert.equals("select")||state.alert.equals("lselect"))){
						groupIsAlerting = true;
					}else if(!groupIsAlerting){
						state.alert = null;
					}
					
					
					int bulbInGroup = calculateBulbPositionInGroup(bulb, mDeviceManager.getSelectedGroup());
					if(bulbInGroup>-1 && maxBrightness!=null){
						//convert relative brightness into absolute brightness
						if(state.bri!=null)
							bulbRelBri[bulbInGroup] = state.bri;
						else
							bulbRelBri[bulbInGroup] = MAX_REL_BRI;
						bulbBri[bulbInGroup] = (bulbRelBri[bulbInGroup] * maxBrightness)/ MAX_REL_BRI;
						state.bri = bulbBri[bulbInGroup];
						bulbKnown[bulbInGroup] = KnownState.Synched;
					}					
					NetworkMethods.PreformTransmitGroupMood(mContext, getRequestQueue(), mDeviceManager, bulb, state);
				} else if (hasTransientChanges()) {
					boolean sentSomething = false;
					while (!sentSomething) {
						if(bulbKnown[transientIndex] == KnownState.ToSend){
							BulbState bs = new BulbState();
							bulbBri[transientIndex] = (bulbRelBri[transientIndex] * maxBrightness)/ MAX_REL_BRI;
							bs.bri = bulbBri[transientIndex];
							
							NetworkMethods.PreformTransmitGroupMood(mContext, getRequestQueue(), mDeviceManager, mDeviceManager.getSelectedGroup().groupAsLegacyArray[transientIndex], bs);
							bulbKnown[transientIndex] = KnownState.Synched;
							sentSomething = true;
						}
						transientIndex = (transientIndex + 1) % mDeviceManager.getSelectedGroup().groupAsLegacyArray.length;
					}
				} 
			}
		};
		countDownTimer.start();
	}
}
