package vip.inteltech.gat;

import java.util.LinkedList;
import java.util.List;

import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.*;
import com.amap.api.maps.model.*;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.GeocodeSearch.OnGeocodeSearchListener;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import vip.inteltech.coolbaby.R;
import vip.inteltech.gat.maputil.GaoDeMapUtil;
import vip.inteltech.gat.maputil.GoogleMapUtil;
import vip.inteltech.gat.maputil.MapUtil;
import vip.inteltech.gat.utils.AppData;
import vip.inteltech.gat.utils.DateConversion;
import vip.inteltech.gat.utils.WebService;
import vip.inteltech.gat.utils.WebService.WebServiceListener;
import vip.inteltech.gat.utils.WebServiceProperty;

public class MsgRecordLocation extends BaseFragmentActivity implements OnClickListener, LocationSource,
		AMapLocationListener, OnGeocodeSearchListener, WebServiceListener,OnCheckedChangeListener, OnMapReadyCallback {
	private MsgRecordLocation mContext;
	// 地图相关
	private AMap mGaoDeMap;
	private GoogleMap mGoogleMap;
	private SupportMapFragment mGaoDeMapFragment;
	private com.google.android.gms.maps.SupportMapFragment mGoogleMapFragment;
	private OnLocationChangedListener mListener;
	private AMapLocationClient mlocationClient;
	private AMapLocationClientOption mLocationOption;
	private MarkerOptions markerOption;
	private Marker mMarker;
	private LatLng mLatLng;
	private GeocodeSearch geocoderSearch;
	private int mMapSelect;
	
	private TextView tv_adress, tv_time;
	private CheckBox cb_layers;
	private Button btn_refresh;
	private String mAddress;
	
	private MapUtil mMapUtil;
	private GaoDeMapUtil mGaoDeMapUtil;
	private GoogleMapUtil mGoogleMapUtil;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.msg_record_location);

		mMapSelect = AppData.GetInstance(mContext).getMapSelect();
		
		findViewById(R.id.btn_left).setOnClickListener(this);
		//findViewById(R.id.btn_right).setOnClickListener(this);
		findViewById(R.id.btn_amplification).setOnClickListener(this);
		findViewById(R.id.btn_shrink).setOnClickListener(this);
		
		tv_adress = (TextView) findViewById(R.id.tv_adress);
		tv_time = (TextView) findViewById(R.id.tv_time);
		cb_layers = (CheckBox) findViewById(R.id.cb_layers);
		
		mGaoDeMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.GaoDeMap);
		//GoogleMap member aquire in the function---OnMapReady
		mGoogleMapFragment = ((com.google.android.gms.maps.SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.GoogleMap));
		mGoogleMapFragment.getMapAsync(this); 
        mContext = this;
		if(mMapSelect == 1)
		{
			mGaoDeMap = mGaoDeMapFragment.getMap();	
			mGaoDeMapUtil = new GaoDeMapUtil(mGaoDeMap);
			mMapUtil = new MapUtil(mGaoDeMapUtil);
			mMapUtil.initMap();
			mGaoDeMap.setLocationSource(this);// 设置定位监听
	        initView();
	        GetAddress(mLatLng.latitude, mLatLng.longitude);
		}
		
		geocoderSearch = new GeocodeSearch(this);
		geocoderSearch.setOnGeocodeSearchListener(this);
				
		//hide one map of the two
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		if(mMapSelect == 1)
		{
			transaction.hide(mGoogleMapFragment);
		}
		else{
			transaction.hide(mGaoDeMapFragment);
		}
		transaction.commit();


        cb_layers.setOnCheckedChangeListener(this);
   //     cb_layers.setChecked(false);

        Log.v("kkk", "111");
	}
	
	private void initView(){
		String[] strs = getIntent().getStringExtra("content").split("-");
		mLatLng = new LatLng(Double.valueOf(strs[0]),Double.valueOf(strs[1]));
	//	addMarker(mLatLng, R.drawable.location_watch, true);
/*		mGaoDeMap.animateCamera(CameraUpdateFactory
				.newCameraPosition(new CameraPosition(mLatLng, 18, 0, 30)), 1000,
				null);*/
		mMapUtil.animateCam(mLatLng.latitude, mLatLng.longitude, 18, 0, 0, 1000, null);
		mMapUtil.addMarker(mLatLng.latitude, mLatLng.longitude, R.drawable.location_watch, false, false);
		tv_time.setText(DateConversion.TimeChange(getIntent().getStringExtra("CreateTime"), null));
	}
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_left:
			finish();
			break;
		case R.id.btn_amplification:
			//mGaoDeMap.animateCamera(CameraUpdateFactory.zoomIn(), 1000, null);
			mMapUtil.animateCam(true, 1000, null);
			break;
		case R.id.btn_shrink:
			//mGaoDeMap.animateCamera(CameraUpdateFactory.zoomOut(), 1000, null);
			mMapUtil.animateCam(false, 1000, null);
			break;
		}
	}
	private void GetAddress(double lat, double lng){
	//	Log.v("kkk", "111");
		WebService ws = new WebService(mContext, _GetAddress, true,
				"GetAddress");
	//	Log.v("kkk", "222");
		List<WebServiceProperty> property = new LinkedList<WebServiceProperty>();
		property.add(new WebServiceProperty("loginId", AppData
				.GetInstance(this).getLoginId()));
		property.add(new WebServiceProperty("mapType", "1"));
		property.add(new WebServiceProperty("lat", String.valueOf(lat)));
		property.add(new WebServiceProperty("lng", String.valueOf(lng)));
		ws.addWebServiceListener(mContext);
		ws.SyncGet(property);
	//	Log.v("kkk", "333");
	}
	private final int _GetAddress = 1;
	@Override
	public void onWebServiceReceive(String method, int id, String result) {
		// TODO Auto-generated method stub
		Log.v("kkk", "onWebServiceReceive");
		try {
			JSONObject jsonObject = new JSONObject(result);
			if (id == _GetAddress) {
				int code = jsonObject.getInt("Code");
				if (code == 1) {
					mAddress = jsonObject.getString("Province") + 
							jsonObject.getString("City") + 
							jsonObject.getString("District") +
							jsonObject.getString("Road");
					JSONArray array = jsonObject.getJSONArray("Nearby");
					for(int i = 0;i < array.length();i++){
						JSONObject item = array.getJSONObject(i);
						mAddress = mAddress + "," + item.getString("POI");
					}
					tv_adress.setText(DateConversion.TimeChange(getIntent().getStringExtra("CreateTime"), null) + " " + mAddress);
				} else {
					// 系统异常小于0，常规异常大于0
					tv_adress.setText(R.string.no_result);
					tv_time.setText("");
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	// ------------------------------------------------------地图相关---------------------------------------------------------------//
	/**
	 * 初始化AMap对象
	 */

/*	private void aMapInit() {
		if (aMap == null) {
			aMap = mapView.getMap();
			setUpMap();
		}
		// 对amap添加单击地图事件监听器
		aMap.setOnMapClickListener(new OnMapClickListener() {
			@Override
			public void onMapClick(LatLng latLng) {
				if (aMap != null) {
					aMap.clear();
				}
				mMarker.remove();
				circle.remove();
				addMarker(latLng);
				mLatLng = latLng;
			}
		});
		aMap.setOnMarkerClickListener(new OnMarkerClickListener() {
			@Override
			public boolean onMarkerClick(Marker marker) {
				if(marker.equals(mMarker)){
					
				}
				return false;
			}
		});
	}*/
	private void addMarker(LatLng latLng, int MarkerIndex, boolean isAddress){
		if(isAddress){
			GetAddress(latLng.latitude, latLng.longitude);
			//getAddress(new LatLonPoint(latLng.latitude, latLng.longitude));
		}
		markerOption = new MarkerOptions();
		//markerOption.title("西安市").snippet("西安市：34.341568, 108.940174");
		markerOption.draggable(true);
		markerOption.icon(BitmapDescriptorFactory
				.fromResource(MarkerIndex));
		mMarker = mGaoDeMap.addMarker(markerOption);
		mMarker.setPosition(latLng);
		jumpPoint(mMarker, latLng);
	}
	/**
	 * 响应逆地理编码
	 */
	public void getAddress(final LatLonPoint latLonPoint) {
		RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 200,
				GeocodeSearch.AMAP);// 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
		geocoderSearch.getFromLocationAsyn(query);// 设置同步逆地理编码请求
	}
	/**
	 * 设置一些amap的属性
	 */
	private void setUpMap() {
		// 自定义系统定位小蓝点
		MyLocationStyle myLocationStyle = new MyLocationStyle();
		myLocationStyle.myLocationIcon(BitmapDescriptorFactory
				.fromResource(R.drawable.location_me));// 设置小蓝点的图标
		myLocationStyle.strokeColor(R.color.transparent);// 设置圆形的边框颜色
		myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));// 设置圆形的填充颜色
		//myLocationStyle.anchor(int,int)//设置小蓝点的锚点
		myLocationStyle.strokeWidth(1.0f);// 设置圆形的边框粗细
		mGaoDeMap.setMyLocationStyle(myLocationStyle);
		mGaoDeMap.setLocationSource(this);// 设置定位监听
		mGaoDeMap.getUiSettings().setZoomControlsEnabled(false);// 隐藏缩放按钮
		mGaoDeMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
		mGaoDeMap.getUiSettings().setMyLocationButtonEnabled(false);
		mGaoDeMap.moveCamera(CameraUpdateFactory.zoomTo(18f));
		//aMap.setMyLocationEnabled(false);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
		mGaoDeMap.getUiSettings().setScaleControlsEnabled(true);
		// aMap.setMyLocationType()
		geocoderSearch = new GeocodeSearch(this);
		geocoderSearch.setOnGeocodeSearchListener(this);
	}
	/**
	 * marker点击时跳动一下
	 */
	public void jumpPoint(final Marker marker, final LatLng latLng) {
		final Handler handler = new Handler();
		final long start = SystemClock.uptimeMillis();
		Projection proj = mGaoDeMap.getProjection();
		Point startPoint = proj.toScreenLocation(latLng);
		startPoint.offset(0, -100);
		final LatLng startLatLng = proj.fromScreenLocation(startPoint);
		final long duration = 1500;

		final Interpolator interpolator = new BounceInterpolator();
		handler.post(new Runnable() {
			@Override
			public void run() {
				long elapsed = SystemClock.uptimeMillis() - start;
				float t = interpolator.getInterpolation((float) elapsed
						/ duration);
				double lng = t * latLng.longitude + (1 - t)
						* startLatLng.longitude;
				double lat = t * latLng.latitude + (1 - t)
						* startLatLng.latitude;
				marker.setPosition(new LatLng(lat, lng));
				mGaoDeMap.reloadMap();// 刷新地图
				if (t < 1.0) {
					handler.postDelayed(this, 16);
				}
			}
		});

	}
	@Override
	public void onGeocodeSearched(GeocodeResult arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
		// TODO Auto-generated method stub
		if (rCode == 0) {
			if (result != null && result.getRegeocodeAddress() != null
					&& result.getRegeocodeAddress().getFormatAddress() != null && !TextUtils.isEmpty( result.getRegeocodeAddress().getFormatAddress())) {
				tv_adress.setText(result.getRegeocodeAddress().getFormatAddress() + "附近");
				/*if(TextUtils.isEmpty(mWatchStateModel.getDeviceTime())){
				}else{
					tv_adress.setText(result.getRegeocodeAddress().getFormatAddress() + "附近" +DateConversion.TimeChange(mWatchStateModel.getDeviceTime(), null) );
				}*/
				mAddress = result.getRegeocodeAddress().getFormatAddress() + "附近";
			} else {
				System.out.println("暂无结果");
				//暂无结果
			}
		} else if (rCode == 27) {
			//网络错误
		} else if (rCode == 32) {
			//key错误
		} else {
			//其他错误
		}
	}

	/**
	 * 定位成功后回调函数
	 */
	private boolean firstLoad = true;
	@Override
	public void onLocationChanged(AMapLocation aLocation) {
		
		if (mListener != null && aLocation != null) {
			//mListener.onLocationChanged(aLocation);// 显示系统小蓝点
			
		}
	}

	/**
	 * 激活定位
	 */
	@Override
	public void activate(OnLocationChangedListener listener) {
		mListener = listener;
/*		if (mlocationClient == null) {
			mlocationClient = new AMapLocationClient(this);
			mLocationOption = new AMapLocationClientOption();
			//设置定位监听
			mlocationClient.setLocationListener(this);
			//设置为高精度定位模式
			mLocationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);
			//设置定位参数
			mlocationClient.setLocationOption(mLocationOption);
			// 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
			// 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
			// 在定位结束后，在合适的生命周期调用onDestroy()方法
			// 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
			mlocationClient.startLocation();
		}*/
	}

	/**
	 * 停止定位
	 */
	@Override
	public void deactivate() {
		mListener = null;
		if (mlocationClient != null) {
			mlocationClient.stopLocation();
			mlocationClient.onDestroy();
		}
		mlocationClient = null;
	}

	// ------------------------------------------------------生命周期---------------------------------------------------------------//
	/**
	 * 方法必须重写
	 */
	@Override
	protected void onResume() {
		super.onResume();
		if (mlocationClient == null) {
			mlocationClient = new AMapLocationClient(this);
			mLocationOption = new AMapLocationClientOption();
			//设置定位监听
			mlocationClient.setLocationListener(this);
			//设置为高精度定位模式
			mLocationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);
			//设置定位参数
			mlocationClient.setLocationOption(mLocationOption);
			// 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
			// 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
			// 在定位结束后，在合适的生命周期调用onDestroy()方法
			// 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
			mlocationClient.startLocation();
		}
	}

	/**
	 * 方法必须重写
	 */
	@Override
	protected void onPause() {
		super.onPause();
		deactivate();
	}

	/**
	 * 方法必须重写
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	/**
	 * 方法必须重写
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		if(isChecked){
			mMapUtil.setMapTypeToSatellite(true);// 卫星地图模式
		}else{
			mMapUtil.setMapTypeToSatellite(false);// 矢量地图模式
		}
	}
	@Override
	public void onMapReady(GoogleMap map) {
		// TODO Auto-generated method stub
		Log.v("kkk", "onMapReady");
		if(mMapSelect == 1){
/*			mGaoDeMap = mGaoDeMapFragment.getMap();	
			mGaoDeMapUtil = new GaoDeMapUtil(mGaoDeMap);
			mGaoDeMapUtil.initMap();
			mMapUtil = new MapUtil(mGaoDeMapUtil);*/
		}
		else {
			mGoogleMap = map;
			mGoogleMapUtil = new GoogleMapUtil(mGoogleMap);
		//	mGoogleMapUtil.initMap();
			mMapUtil = new MapUtil(mGoogleMapUtil);
			mMapUtil.initMap();
	        initView();
		//	mGoogleMap.setLocationSource(this);// 设置定位监听  <<<<<<<<<<<<===================================
	        GetAddress(mLatLng.latitude, mLatLng.longitude);
		}
	}
	
}
