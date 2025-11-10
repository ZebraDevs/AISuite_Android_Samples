package com.zebra.aisuite_quickstart.filtertracker;

import static android.content.Context.MODE_PRIVATE;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zebra.aisuite_quickstart.R;
import com.zebra.aisuite_quickstart.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;


public class FilterDialog extends Dialog {

    public interface FilterCallback {
        void onFilterChange(List<FilterItem> Option);
    }

    public Context mContext;
    private RecyclerView mFilterListView;
    private Button mSaveBtn;
    private Button mCancelBtn;
    private FilterCallback mCallback;
    private FilterAdapter mAdapter;
    public static final String BARCODE_TRACKER = "Barcode Tracker";
    public static final String OCR_TRACKER = "OCR Tracker";

    private static final String[] trackerArray ={BARCODE_TRACKER,OCR_TRACKER};
    private SharedPreferences sharedPreferences;
    private List<FilterItem> filterItems = new ArrayList<>();


    public FilterDialog(Context context) {
        super(context, R.style.MyAlertDialogTheme);
        this.mContext = context;
    }

    public void setCallback(FilterCallback callback) {
        mCallback = callback;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        getWindow().setAttributes(params);

        View view = LayoutInflater.from(mContext).inflate(R.layout.layout_filter_dialog, null, false);

        setContentView(view);


        mSaveBtn = view.findViewById(R.id.apply_btn);
        mCancelBtn = view.findViewById(R.id.cancel_btn);
        setCanceledOnTouchOutside(false);
        String className = mContext.getClass().getName();
        if (className.equals("com.zebra.aisuite_quickstart.java.CameraXLivePreviewActivity")) {
            Log.d("ActivityCheck", "Instance is from Java CameraXLivePreviewActivity");
            sharedPreferences = mContext.getSharedPreferences(CommonUtils.PREFS_NAME, MODE_PRIVATE);
        } else if (className.equals("com.zebra.aisuite_quickstart.kotlin.CameraXLivePreviewActivity")) {
            Log.d("ActivityCheck", "Instance is from kotlin CameraXLivePreviewActivity");
            sharedPreferences = mContext.getSharedPreferences(CommonUtils.PREFS_NAME_KOTLIN, MODE_PRIVATE);
        }

        for(String item : trackerArray){
            FilterItem filterItem = new FilterItem(item);
            filterItems.add(filterItem);
        }

        for(FilterItem option: filterItems){
            boolean isChecked =sharedPreferences.getBoolean(option.getTitle(), TextUtils.equals(option.getTitle(), BARCODE_TRACKER));
            option.setChecked(isChecked);
        }
        mFilterListView = view.findViewById(R.id.filter_list);
        mAdapter = new FilterAdapter(filterItems);
        mFilterListView.setLayoutManager(new LinearLayoutManager(mContext, RecyclerView.VERTICAL,false));
        mFilterListView.setAdapter(mAdapter);


        mSaveBtn.setOnClickListener(v -> {
            mCallback.onFilterChange(mAdapter.getFilteredList());
            dismiss();
        });

        mCancelBtn.setOnClickListener(v -> dismiss());
    }

    public List<FilterItem> getFilterItems() {
        return filterItems;
    }

    public void setOutsideClickEnable(boolean isEnable) {
        setCanceledOnTouchOutside(!isEnable);
    }

}
