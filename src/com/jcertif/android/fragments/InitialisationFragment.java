package com.jcertif.android.fragments;

import java.util.Arrays;
import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jcertif.android.JcertifApplication;
import com.jcertif.android.MainActivity;
import com.jcertif.android.R;
import com.jcertif.android.dao.CategorieProvider;
import com.jcertif.android.dao.SponsorLevelProvider;
import com.jcertif.android.model.Category;
import com.jcertif.android.model.SponsorLevel;
import com.jcertif.android.service.RESTService;

/**
 * 
 * @author bashizip
 * 
 */
public class InitialisationFragment extends RESTResponderFragment {

	private ProgressBar pb_init;
	private TextView tv_init;

	private final String SPONSOR_LEVEL_URI = JcertifApplication.BASE_URL
			+ "/ref/sponsorlevel/list";
	private final String SESSION_STATUS_URI = JcertifApplication.BASE_URL
			+ "/ref/sessionstatus/list";
	private final String CIVILITES__URI = JcertifApplication.BASE_URL
			+ "/ref/title/list";
	private final String CATEGORIES__URI = JcertifApplication.BASE_URL
			+ "/ref/category/list";

	private RefentielDataLodedListener listener;
	CategorieProvider catProvider;
	SponsorLevelProvider spProvider;
	
	private static int threadCount=2; //must be equal to urls count; i
	private static int currentThreadNo=0; //id of the incoming thread from intentService
	

	public InitialisationFragment() {
		super();
	}

	public interface RefentielDataLodedListener {
		public void OnRefDataLoaded();
	}

	public void setListener(RefentielDataLodedListener listener) {
		this.listener = listener;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setRetainInstance(true);
		View rootView = inflater.inflate(R.layout.fragment_init, container,
				false);
		pb_init = (ProgressBar) rootView.findViewById(R.id.pb_init);
		tv_init = (TextView) rootView.findViewById(R.id.tv_init);
		tv_init.setText(R.string.fetching_initial_data);
		getActivity().setTitle(R.string.app_name);
		return rootView;
	}

	

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getSherlockActivity().getSupportActionBar().setNavigationMode(
				ActionBar.NAVIGATION_MODE_STANDARD);

	
	catProvider=new CategorieProvider(InitialisationFragment.this.getSherlockActivity());
	spProvider=new SponsorLevelProvider(InitialisationFragment.this
			.getSherlockActivity());
	
		loadData(CATEGORIES__URI);
	}
	
	void loadData(String URI) {

		MainActivity activity = (MainActivity) getActivity();
		Intent intent = new Intent(activity, RESTService.class);
		intent.setData(Uri.parse(URI));
		Bundle params = new Bundle();
		params.putString(RESTService.KEY_JSON_PLAYLOAD, null);
		intent.putExtra(RESTService.EXTRA_PARAMS, params);
		intent.putExtra(RESTService.EXTRA_RESULT_RECEIVER, getResultReceiver());
		activity.startService(intent);
	}

	@Override
	public void onRESTResult(int code, Bundle resultData) {
		String result = resultData.getString(RESTService.REST_RESULT);
		String resultType = resultData.getString(RESTService.KEY_URI_SENT);

		if (resultType.equals(SPONSOR_LEVEL_URI)) {
			List<SponsorLevel> sponsorsLevel = parseSponsorLevelJson(result);
			saveSponsorLevelToCache(sponsorsLevel);
		}
		if(resultType.equals(CATEGORIES__URI)){
			List<Category> cat = parseCategoryJson(result);
			saveCatToCache(cat);
		}
	}

	private void saveCatToCache(final List<Category> cat) {
		Thread th = new Thread(new Runnable() {

			@Override
			public void run() {
				for (Category sl : cat){
					catProvider.store(sl);
				}
			}
		});
		
		th.start();

		try {
			
			th.join();
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       if(++currentThreadNo==threadCount){
		listener.OnRefDataLoaded();
       }else{
    	   loadData(SPONSOR_LEVEL_URI);
       }
		
	}

	
	private List<Category> parseCategoryJson(String result) {
		Gson gson = new GsonBuilder().setDateFormat("dd/MM/yyyy hh:mm")
				.create();
		Category[] sl = gson.fromJson(result, Category[].class);
		return Arrays.asList(sl);
	}

	private List<SponsorLevel> parseSponsorLevelJson(String result) {
		Gson gson = new GsonBuilder().setDateFormat("dd/MM/yyyy hh:mm")
				.create();
		SponsorLevel[] sl = gson.fromJson(result, SponsorLevel[].class);
		return Arrays.asList(sl);
	}
	

	protected void saveSponsorLevelToCache(final List<SponsorLevel> sls) {
		Thread th = new Thread(new Runnable() {

			@Override
			public void run() {
				for (SponsorLevel sl : sls)
					spProvider.store(sl);
			}
		});
		th.start();
		try {
			
			th.join();
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       if(++currentThreadNo==threadCount){
		listener.OnRefDataLoaded();
       }
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

	}

}
