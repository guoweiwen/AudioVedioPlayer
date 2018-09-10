package com.wyman.playerlibrary.nicevideoplayer;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wyman.playerlibrary.R;

import java.util.List;

/**
 * Created by wyman
 * on 2018-09-08.
 * 切换清晰度对话框（仿腾讯视频切换清晰度的对话框）.
 */
public class ChangeClarityDialog extends Dialog{

    private LinearLayout mLinearLayout;
    private int mCurrentCheckIndex;

    public ChangeClarityDialog(Context context){
        super(context, R.style.dialog_change_clarity);
        init(context);
    }

    private void init(Context context) {
        mLinearLayout = new LinearLayout(context);
        mLinearLayout.setGravity(Gravity.CENTER);
        mLinearLayout.setOrientation(LinearLayout.VERTICAL);
        mLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null){
                   mListener.onClarityNotChanged();
                }
                ChangeClarityDialog.this.dismiss();
            }
        });
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        setContentView(mLinearLayout,params);

        WindowManager.LayoutParams windowParam = getWindow().getAttributes();
        windowParam.width = NiceUtil.getScreenWidth(context);
        windowParam.height = NiceUtil.getScreenHeight(context);
        getWindow().setAttributes(windowParam);
    }

    /**
     * 设置清晰度等级
     *
     * @param items          清晰度等级items
     * @param defaultChecked 默认选中的清晰度索引
     */
    public void setClarityGrade(List<String> items,int defaultChecked){
        mCurrentCheckIndex = defaultChecked;
        for(int i=0;i<items.size();i++){
            TextView itemView = (TextView) LayoutInflater.from(getContext())
                    .inflate(R.layout.item_change_clarity,mLinearLayout,false);
            itemView.setTag(i);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mListener != null){
                        int checkIndex = (int)v.getTag();
                        if(checkIndex != mCurrentCheckIndex){
                            for(int j=0;j<mLinearLayout.getChildCount();j++){
                                mLinearLayout.getChildAt(j).setSelected(checkIndex == j);;
                            }
                            mListener.onClarityChange(checkIndex);
                            mCurrentCheckIndex = checkIndex;
                        } else {
                            mListener.onClarityNotChanged();
                        }
                    }
                    ChangeClarityDialog.this.dismiss();
                }
            });
            itemView.setText(items.get(i));
            itemView.setSelected(i == defaultChecked);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) itemView.getLayoutParams();
            params.topMargin = (i==0)?0:NiceUtil.dp2px(getContext(),16f);
            mLinearLayout.addView(itemView,params);
        }
    }

    public interface OnClarityChangedListener{
        /**
         * 切换清晰度后回调
         *
         * @param clarityIndex 切换到的清晰度的索引值
         */
        void onClarityChange(int clarityIndex);

        /**
         * 清晰度没有切换，比如点击了空白位置，或者点击的是之前的清晰度
         */
        void onClarityNotChanged();
    }
    private OnClarityChangedListener mListener;
    public void setOnClarityCheckedListener(OnClarityChangedListener listener){
        mListener = listener;
    }

    /**
     * 按下回退键
     * */
    @Override
    public void onBackPressed() {
        if(mListener != null){
            //按返回键时回调清晰度没有变化
            mListener.onClarityNotChanged();
        }
        super.onBackPressed();
    }
}












