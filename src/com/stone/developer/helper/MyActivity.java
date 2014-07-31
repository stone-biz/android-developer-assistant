package com.stone.developer.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SearchView.OnSuggestionListener;
import android.widget.TextView;

/**
 * @author stone
 */
public class MyActivity extends ListActivity {
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPackageManager = getPackageManager();
	    setContentView(R.layout.activity_launcher);
	    initView();
	    isDestroyed = false;
	    registerRefreshBroadcast();
	    loadData();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem item = menu.add(1, MENU_ID_SEARCH, 1, "搜索");
		item.setIcon(android.R.drawable.ic_menu_search);
		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		sv = new SearchView(this);
		sv.setOnQueryTextFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				System.out.println("setOnQueryTextFocusChangeListener");
				ActionBar ab = getActionBar();
				if(hasFocus) {
					ab.setDisplayHomeAsUpEnabled(true);
					System.out.println("hasFocus=true-----------");
				} else {
					ab.setDisplayHomeAsUpEnabled(false);
					mAdapter.setData(mOriginalData);
					System.out.println("hasFocus=false-----------");
				}
			}
		});
		sv.setOnQueryTextListener(new OnQueryTextListener() {
			public boolean onQueryTextSubmit(String query) {
				return true;
			}
			public boolean onQueryTextChange(String newText) {
				System.out.println("onQueryTextChange: " + newText);
				//搜索
				if(!TextUtils.isEmpty(newText)) {
					List<ListItem> result = searchApp(newText);
					mAdapter.setData(result);
				}
				return true;
			}
		});
		item.setActionView(sv);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case android.R.id.home:
			System.out.println("backback---");
			break;
		case MENU_ID_SEARCH:
			System.out.println("hahahahah");
			break;
		}
		return true;
	}
	
	protected void onResume() {
		super.onResume();
		registerRefreshBroadcast();
	}

	protected void onPause() {
		super.onPause();
		if(broadcastReceiver!=null) {
			unregisterReceiver(broadcastReceiver);
			broadcastReceiver = null;
		}
	}
    @Override
    protected void onDestroy() {
    	isDestroyed = true;
    	super.onDestroy();
    }
    
    protected List<ResolveInfo> onQueryPackageManager(Intent queryIntent) {
    	List<ResolveInfo> result = mPackageManager.queryIntentActivities(mIntent, 0);
    	ArrayList<ResolveInfo> data = new ArrayList<ResolveInfo>();
    	if(result == null) {
    		result = new ArrayList<ResolveInfo>();
    	}
    	for(ResolveInfo e : result) {
    		if((e.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
    			if(!getPackageName().equals(e.activityInfo.packageName)) {
    				data.add(e);
    			}
    		}
    	}
    	return data;
//        return mPackageManager.queryIntentActivities(queryIntent, /* no flags */ 0);
    }
    
    protected void onSortResultList(List<ResolveInfo> results) {
        Collections.sort(results, new ResolveInfo.DisplayNameComparator(mPackageManager));
    }
    
	protected Intent getTargetIntent() {
		System.out.println("getTargetIntent");
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		return intent;
	}
	
	public List<ListItem> makeListItems() {
	    // Load all matching activities and sort correctly
	    List<ResolveInfo> list = onQueryPackageManager(mIntent);
	    onSortResultList(list);
	
	    ArrayList<ListItem> result = new ArrayList<ListItem>(list.size());
	    int listSize = list.size();
	    for (int i = 0; i < listSize; i++) {
	        ResolveInfo resolveInfo = list.get(i);
	        result.add(new ListItem(mPackageManager, resolveInfo, null));
	    }
	
	    return result;
	}
	protected void onListItemClick(ListView l, View v, int position, long id) {
	 	ListItem li = itemForPosition(position);
		Intent intent = new Intent(Intent.ACTION_DELETE);
		intent.setData(Uri.parse("package:"+li.packageName));
		startActivity(intent);
    }
    
    /**
     * Return the actual Intent for a specific position in our
     * {@link android.widget.ListView}.
     * @param position The item whose Intent to return
     */
    protected Intent intentForPosition(int position) {
        ActivityAdapter adapter = (ActivityAdapter) mAdapter;
        return adapter.intentForPosition(position);
    }
    
    /**
     * Return the {@link ListItem} for a specific position in our
     * {@link android.widget.ListView}.
     * @param position The item to return
     */
    protected ListItem itemForPosition(int position) {
        ActivityAdapter adapter = (ActivityAdapter) mAdapter;
        return adapter.itemForPosition(position);
    }
    
    
    
    private void initView() {
    	ActionBar ab = getActionBar();
    	ab.setDisplayShowHomeEnabled(true);
    	
	    mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_indicator);
	    mEmptyView = (TextView) findViewById(R.id.v_empty);
	    mEmptyView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				loadData();
			}
		});
	    
	    mListView = getListView();
	    mIconResizer = new IconResizer();
        mIntent = new Intent(getTargetIntent());
        mIntent.setComponent(null);
        mAdapter = new ActivityAdapter(mIconResizer);
        setListAdapter(mAdapter);
	}
	private void loadData() {
		new LoadDataTask().execute();
	}
	private void registerRefreshBroadcast() {
		IntentFilter inf = new IntentFilter();
		inf.addAction(ACTION_REFRESH);
		broadcastReceiver = new RefreshBroadcastReceiver();
		registerReceiver(broadcastReceiver,  inf);
	}
	
	private List<ListItem> searchApp(String newText) {
		List<ListItem> data  = mAdapter.getData();
		ArrayList<ListItem> result = new ArrayList<ListItem>();
		for(ListItem li : data) {
			String label = li.label == null?"":li.label.toString();
			if(label.contains(newText)) {
				result.add(li);
			}
		}
		return result;
	}
	
    private class ActivityAdapter extends BaseAdapter {
	    protected final IconResizer mIconResizer;
	    protected final LayoutInflater mInflater;
	
	    protected ArrayList<ListItem> mData;
	
	    public ActivityAdapter(IconResizer resizer) {
	        mIconResizer = resizer;
	        mInflater = (LayoutInflater) MyActivity.this.getSystemService(
	                Context.LAYOUT_INFLATER_SERVICE);
//	        mData = makeListItems();
	        mData = new ArrayList<MyActivity.ListItem>();
	    }

	    public int getCount() {
	        return mData != null ? mData.size() : 0;
	    }

	    public ListItem getItem(int position) {
	        return mData.get(position);
	    }
	    public List<ListItem> getData() {
	    	return mData;
	    }
	    public long getItemId(int position) {
	        return position;
	    }
	    public Intent intentForPosition(int position) {
            if (mData == null) {
                return null;
            }

            Intent intent = new Intent(mIntent);
            ListItem item = mData.get(position);
            intent.setClassName(item.packageName, item.className);
            if (item.extras != null) {
                intent.putExtras(item.extras);
            }
            return intent;
        }

        public ListItem itemForPosition(int position) {
            if (mData == null) {
                return null;
            }
            return mData.get(position);
        }
        
	    public View getView(int position, View convertView, ViewGroup parent) {
	        View view;
	        if (convertView == null) {
	            view = mInflater.inflate(
	                    R.layout.lv_item, parent, false);
	        } else {
	            view = convertView;
	        }
	        bindView(view.findViewById(R.id.vv), mData.get(position));
	        return view;
	    }

	    private void bindView(View view, ListItem item) {
	        TextView text = (TextView) view;
	        text.setText(item.label);
	        if (item.icon == null) {
	            item.icon = mIconResizer.createIconThumbnail(item.resolveInfo.loadIcon(getPackageManager()));
	        }
	        text.setCompoundDrawablesWithIntrinsicBounds(item.icon, null, null, null);
	    }
	    
	    public void setData(List<ListItem> data) {
	    	if(!mData.isEmpty()) {
	    		mData.clear();
	    	}
	    	if(data!=null) {
	    		mData.addAll(data);
	    	}
	    	notifyDataSetChanged();
		}
	    
	    public void removeItem(ListItem item) {
	    	mData.remove(item);
	    }
    }
	
	public class IconResizer {
        // Code is borrowed from com.android.launcher.Utilities. 
        private int mIconWidth = -1;
        private int mIconHeight = -1;

        private final Rect mOldBounds = new Rect();
        private Canvas mCanvas = new Canvas();
        
        public IconResizer() {
            mCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
                    Paint.FILTER_BITMAP_FLAG));
            final Resources resources = MyActivity.this.getResources();
            mIconWidth = mIconHeight = (int) resources.getDimension(
                    android.R.dimen.app_icon_size);
        }

        /**
         * Returns a Drawable representing the thumbnail of the specified Drawable.
         * The size of the thumbnail is defined by the dimension
         * android.R.dimen.launcher_application_icon_size.
         *
         * This method is not thread-safe and should be invoked on the UI thread only.
         *
         * @param icon The icon to get a thumbnail of.
         *
         * @return A thumbnail for the specified icon or the icon itself if the
         *         thumbnail could not be created. 
         */
        public Drawable createIconThumbnail(Drawable icon) {
            int width = mIconWidth;
            int height = mIconHeight;

            final int iconWidth = icon.getIntrinsicWidth();
            final int iconHeight = icon.getIntrinsicHeight();

            if (icon instanceof PaintDrawable) {
                PaintDrawable painter = (PaintDrawable) icon;
                painter.setIntrinsicWidth(width);
                painter.setIntrinsicHeight(height);
            }

            if (width > 0 && height > 0) {
                if (width < iconWidth || height < iconHeight) {
                    final float ratio = (float) iconWidth / iconHeight;

                    if (iconWidth > iconHeight) {
                        height = (int) (width / ratio);
                    } else if (iconHeight > iconWidth) {
                        width = (int) (height * ratio);
                    }

                    final Bitmap.Config c = icon.getOpacity() != PixelFormat.OPAQUE ?
                                Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
                    final Bitmap thumb = Bitmap.createBitmap(mIconWidth, mIconHeight, c);
                    final Canvas canvas = mCanvas;
                    canvas.setBitmap(thumb);
                    // Copy the old bounds to restore them later
                    // If we were to do oldBounds = icon.getBounds(),
                    // the call to setBounds() that follows would
                    // change the same instance and we would lose the
                    // old bounds
                    mOldBounds.set(icon.getBounds());
                    final int x = (mIconWidth - width) / 2;
                    final int y = (mIconHeight - height) / 2;
                    icon.setBounds(x, y, x + width, y + height);
                    icon.draw(canvas);
                    icon.setBounds(mOldBounds);
                    icon = new BitmapDrawable(getResources(), thumb);
                    canvas.setBitmap(null);
                } else if (iconWidth < width && iconHeight < height) {
                    final Bitmap.Config c = Bitmap.Config.ARGB_8888;
                    final Bitmap thumb = Bitmap.createBitmap(mIconWidth, mIconHeight, c);
                    final Canvas canvas = mCanvas;
                    canvas.setBitmap(thumb);
                    mOldBounds.set(icon.getBounds());
                    final int x = (width - iconWidth) / 2;
                    final int y = (height - iconHeight) / 2;
                    icon.setBounds(x, y, x + iconWidth, y + iconHeight);
                    icon.draw(canvas);
                    icon.setBounds(mOldBounds);
                    icon = new BitmapDrawable(getResources(), thumb);
                    canvas.setBitmap(null);
                }
            }
            return icon;
        }
	}
	
	public static class ListItem {
        public ResolveInfo resolveInfo;
        public CharSequence label;
        public Drawable icon;
        public String packageName;
        public String className;
        public Bundle extras;
        
        ListItem(PackageManager pm, ResolveInfo resolveInfo, IconResizer resizer) {
            this.resolveInfo = resolveInfo;
            label = resolveInfo.loadLabel(pm);
            ComponentInfo ci = resolveInfo.activityInfo;
            if (ci == null) ci = resolveInfo.serviceInfo;
            if (label == null && ci != null) {
                label = resolveInfo.activityInfo.name;
            }
            
            if (resizer != null) {
                icon = resizer.createIconThumbnail(resolveInfo.loadIcon(pm));
            }
            packageName = ci.applicationInfo.packageName;
            className = ci.name;
        }
        public ListItem() {}
    }
	
	 
	    
	    class LoadDataTask extends AsyncTask<Void, Void, List<ListItem>> {
	    	protected void onPreExecute() {
	    		mEmptyView.setVisibility(View.GONE);
	    		mListView.setVisibility(View.INVISIBLE);
	    		mLoadingIndicator.setVisibility(View.VISIBLE);
	    	}
			protected List<ListItem> doInBackground(Void... params) {
				return makeListItems();
			}
			@Override
			protected void onPostExecute(List<ListItem> result) {
				if(!isDestroyed) {
					if(result!=null && result.size() > 0) {
						mOriginalData = result;
						mAdapter.setData(result);
						String title = "开发者助手 － " + result.size() + "个应用";
						setTitle(title);
						mEmptyView.setVisibility(View.GONE);
						mLoadingIndicator.setVisibility(View.GONE);
						mListView.setVisibility(View.VISIBLE);
					} else {
						mEmptyView.setVisibility(View.VISIBLE);
						mLoadingIndicator.setVisibility(View.GONE);
						mListView.setVisibility(View.INVISIBLE);
						
						String title = "开发者助手 － " + result.size() + "没有数据";
						setTitle(title);
					}
				} 
				return;
			}
	    }
	    
	    class RefreshBroadcastReceiver extends BroadcastReceiver {
			public void onReceive(Context arg0, Intent arg1) {
				if(arg1 != null) {
					String action = arg1.getAction();
					 if(!TextUtils.isEmpty(action)) {
						 if(ACTION_REFRESH.equals(action)) {
							 if(arg1.hasExtra("data")) {
								 String data = arg1.getStringExtra("data");
								 if(EXTRA_DATA_UNINSTALLED.equals(data)) {
									 System.out.println("卸载后更新UI");
//									 makeListItems();
									 loadData();
								 } else if(EXTRA_DATA_INSTALLED.equals(data)) {
									 System.out.println("安装后更新UI");
//									 makeListItems(); 
									 loadData();
								 }
							 }
						 }
					 } 
				}
			}
		}
	    
	    
	    public static final String ACTION_REFRESH = "com.stone.developer.helper.action_refresh";
		public static final String EXTRA_DATA_UNINSTALLED  = "com.stone.developer.helper.action_uninstalled";
		public static final String EXTRA_DATA_INSTALLED  = "com.stone.developer.helper.action_installed";
		final int MENU_ID_SEARCH = 1000;
		private RefreshBroadcastReceiver broadcastReceiver;
		
		Intent mIntent;
		PackageManager mPackageManager;
	    IconResizer mIconResizer;
	    ActivityAdapter mAdapter;
	    
	    private ListView mListView;
	    private ProgressBar mLoadingIndicator;
	    private TextView mEmptyView;
	    private SearchView sv;
	    private boolean isDestroyed;
	    private List<ListItem> mOriginalData;
}
