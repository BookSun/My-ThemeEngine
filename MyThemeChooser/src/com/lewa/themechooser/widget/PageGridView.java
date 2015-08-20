package com.lewa.themechooser.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.LinearLayout;

import com.lewa.themechooser.R;

public class PageGridView extends LinearLayout{

    private GridView gridview;
    private LinearLayout footerView;
    private boolean isVisible;

    public static final String TAG = "PageGridView";

    public PageGridView(Context context) {
        super(context);
        init();
    }

    public PageGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public PageGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        View v=LayoutInflater.from(getContext()).inflate(R.layout.gridview_load_more, this,true);
        gridview=(GridView) v.findViewById(R.id.gridview);
        footerView = (LinearLayout) v.findViewById(R.id.online_loading);
        setOrientation(LinearLayout.VERTICAL);
    }

    //由于调用此方法一般都为单开线程，不能直接更新控件状态，因此需要一个Handler来协助
    public void updateFooter(int statue){
        Message m=Message.obtain();
        m.what=statue;
        updateFooterViewHandler.sendMessageDelayed(m, 0);
    }

    private Handler updateFooterViewHandler = new Handler(){
        @Override
        public void handleMessage(android.os.Message msg) {
            //这里状态 可以控制为多个，如果想要下拉箭头的话，可以根据状态来修改控件内容，这里我只设置是否显示而已
            footerView.setVisibility(msg.what);
            //当设置View.GONE的时候，数据已经加载完成，因此需要通知数据改变
            if(msg.what==View.GONE){
                ((BaseAdapter)gridview.getAdapter()).notifyDataSetChanged();
                setVisible(false);
            }
            if(msg.what==View.VISIBLE){
                setVisible(true);
            }
        };
    };

    public void setOnScrollListener(OnScrollListener onScrollListener){
        gridview.setOnScrollListener(onScrollListener);
    }

    public void setNumColumns(int number){
        gridview.setNumColumns(number);
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    public void setVerticalSpacing(int spacing){
        gridview.setVerticalSpacing(spacing);
    }

    public void setHorizontalSpacing(int spacing){
        gridview.setHorizontalSpacing(spacing);
    }

    public void setColumnWidth(int width){
        gridview.setColumnWidth(width);
    }

    public void setStretchMode(int stretchMode){
        gridview.setStretchMode(stretchMode);
    }

    public void setAdapter(BaseAdapter adapter){
        gridview.setAdapter(adapter);
    }

    public void setOnItemClickListener(OnItemClickListener itemClickListener){
        gridview.setOnItemClickListener(itemClickListener);
    }
}
