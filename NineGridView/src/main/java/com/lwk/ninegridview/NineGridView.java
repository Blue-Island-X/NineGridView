package com.lwk.ninegridview;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Squared imageview displayer
 */
public class NineGridView extends ViewGroup
{
    //Size of imageview while there has only one image
    private int mSingleImageSize = 100;
    //Aspect ratio of only one imageview
    private float mSingleImageRatio = 1.0f;
    //Size of space
    private int mSpaceSize = 3;
    //Width and height of every imageview(more than one image)
    private int mImageWidth, mImageHeight;
    //column count
    private int mColumnCount = 3;
    //raw count,depends on column count
    private int mRawCount;
    //interface of imageloader
    private INineGridImageLoader mImageLoader;
    //image datas
    private List<NineGridBean> mDataList = new ArrayList<>();
    //plus button
    private NineGridImageView mImgAddData;
    //child view click listener
    private onItemClickListener mListener;
    //weather is in edit mode
    private boolean mIsEditMode;
    //Maximum of image
    private int mMaxNum = 9;

    public NineGridView(Context context)
    {
        super(context);
        initParams(context, null);
    }

    public NineGridView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initParams(context, attrs);
    }

    private void initParams(Context context, AttributeSet attrs)
    {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        mSingleImageSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mSingleImageSize, dm);
        mSpaceSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mSpaceSize, dm);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.NineGridView);
        if (ta != null)
        {
            int count = ta.getIndexCount();
            for (int i = 0; i < count; i++)
            {
                int index = ta.getIndex(i);
                if (index == R.styleable.NineGridView_sapce_size)
                    mSpaceSize = ta.getDimensionPixelSize(index, mSpaceSize);
                else if (index == R.styleable.NineGridView_single_image_ratio)
                    mSingleImageRatio = ta.getFloat(index, mSingleImageRatio);
                else if (index == R.styleable.NineGridView_single_image_size)
                    mSingleImageSize = ta.getDimensionPixelSize(index, mSingleImageSize);
                else if (index == R.styleable.NineGridView_column_count)
                    mColumnCount = ta.getInteger(index, mColumnCount);
                else if (index == R.styleable.NineGridView_is_edit_mode)
                    mIsEditMode = ta.getBoolean(index, mIsEditMode);
                else if (index == R.styleable.NineGridView_max_num)
                    mMaxNum = ta.getInteger(R.styleable.NineGridView_max_num, mMaxNum);
            }
            ta.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int resWidth = 0, resHeight = 0;

        //Measure width
        int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
        //get available width
        int totalWidth = measureWidth - getPaddingLeft() - getPaddingRight();

        if (canShowAddMore())
        {
            //If is in edit mode,each child view must be same size
            mImageWidth = mImageHeight = (totalWidth - (mColumnCount - 1) * mSpaceSize) / mColumnCount;
            int childCount = getChildCount();
            if (childCount < mColumnCount)
                resWidth = mImageWidth * childCount + (childCount - 1) * mSpaceSize + getPaddingLeft() + getPaddingRight();
            else
                resWidth = mImageWidth * mColumnCount + (mColumnCount - 1) * mSpaceSize + getPaddingLeft() + getPaddingRight();
            resHeight = mImageHeight * mRawCount + (mRawCount - 1) * mSpaceSize + getPaddingTop() + getPaddingBottom();
        } else
        {
            //If is non-edit mode,the size of childview depends on data size
            int dataCount = mDataList.size();
            if (mDataList != null && dataCount > 0)
            {
                if (dataCount == 1)
                {
                    mImageWidth = mSingleImageSize > totalWidth ? totalWidth : mSingleImageSize;
                    mImageHeight = (int) (mImageWidth / mSingleImageRatio);
                    //Resize single imageview area size,not allowed to exceed the maximum display range
                    if (mImageHeight > mSingleImageSize)
                    {
                        float ratio = mSingleImageSize * 1.0f / mImageHeight;
                        mImageWidth = (int) (mImageWidth * ratio);
                        mImageHeight = mSingleImageSize;
                    }
                    resWidth = mImageWidth + getPaddingLeft() + getPaddingRight();
                    resHeight = mImageHeight + getPaddingTop() + getPaddingBottom();
                } else
                {
                    mImageWidth = mImageHeight = (totalWidth - (mColumnCount - 1) * mSpaceSize) / mColumnCount;
                    if (dataCount < mColumnCount)
                        resWidth = mImageWidth * dataCount + (dataCount - 1) * mSpaceSize + getPaddingLeft() + getPaddingRight();
                    else
                        resWidth = mImageWidth * mColumnCount + (mColumnCount - 1) * mSpaceSize + getPaddingLeft() + getPaddingRight();
                    resHeight = mImageHeight * mRawCount + (mRawCount - 1) * mSpaceSize + getPaddingTop() + getPaddingBottom();
                }
            }
        }

        setMeasuredDimension(resWidth, resHeight);

        //Measure child view size
        int childrenCount = getChildCount();
        for (int index = 0; index < childrenCount; index++)
        {
            View childView = getChildAt(index);
            int childWidth = mImageWidth;
            int childHeight = mImageHeight;
            int childMode = MeasureSpec.EXACTLY;
            int childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, childMode);
            int childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight, childMode);
            childView.measure(childWidthSpec, childHeightSpec);
        }
    }

    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3)
    {
        int childCount = getChildCount();
        for (int index = 0; index < childCount; index++)
        {
            View childrenView = getChildAt(index);
            int rowNum = index / mColumnCount;
            int columnNum = index % mColumnCount;
            int left = (mImageWidth + mSpaceSize) * columnNum + getPaddingLeft();
            int top = (mImageHeight + mSpaceSize) * rowNum + getPaddingTop();
            int right = left + mImageWidth;
            int bottom = top + mImageHeight;
            childrenView.layout(left, top, right, bottom);
        }
    }

    @Override
    protected void onFinishInflate()
    {
        super.onFinishInflate();
        setDataList(null);
    }

    /**
     * Set data source
     */
    public void setDataList(List<NineGridBean> dataList)
    {
        mDataList.clear();
        //Not allowed to exceed the maximum number
        if (dataList != null && dataList.size() > 0)
        {
            if (dataList.size() <= mMaxNum)
                mDataList.addAll(dataList);
            else
                mDataList.addAll(dataList.subList(0, mMaxNum - 1));
        }
        clearAllViews();
        calRawAndColumn();
        initChildViews();
        requestLayout();
    }

    /**
     * Add data source
     */
    public void addDataList(List<NineGridBean> dataList)
    {
        if (mDataList.size() >= mMaxNum)
            return;
        //Not allowed to exceed the maximum number
        int cha = mMaxNum - mDataList.size();
        if (dataList.size() <= cha)
            mDataList.addAll(dataList);
        else
            mDataList.addAll(dataList.subList(0, cha - 1));

        clearAllViews();
        calRawAndColumn();
        initChildViews();
        requestLayout();
    }

    //calculate the count of raw and column
    private void calRawAndColumn()
    {
        int childSize = mDataList.size();
        //Increase the data size to display plus button in edit mode
        if (canShowAddMore())
            childSize++;

        //calculate the raw count
        if (childSize == 0)
        {
            mRawCount = 0;
        } else if (childSize <= mColumnCount)
        {
            mRawCount = 1;
        } else
        {
            if (childSize % mColumnCount == 0)
                mRawCount = childSize / mColumnCount;
            else
                mRawCount = childSize / mColumnCount + 1;
        }
    }

    //Initialize child view
    private void initChildViews()
    {
        //add image container
        int dataSize = mDataList.size();
        for (int i = 0; i < dataSize; i++)
        {
            final NineGridBean gridBean = mDataList.get(i);
            final NineGirdImageContainer imageContainer = new NineGirdImageContainer(getContext());
            if (mImageLoader != null)
                mImageLoader.displayNineGridImage(getContext(), gridBean.getThumbUrl(), imageContainer.getImageView());
            else
                Log.w("NineGridView", "You'd better set a imageloader!!!!");

            imageContainer.setIsDeleteMode(mIsEditMode);
            final int position = i;
            imageContainer.getImageView().setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    if (mListener != null)
                        mListener.onNineGirdItemClick(position, gridBean, imageContainer);
                }
            });
            imageContainer.setOnClickDeleteListener(new NineGirdImageContainer.onClickDeleteListener()
            {
                @Override
                public void onClickDelete()
                {
                    mDataList.remove(position);
                    clearAllViews();
                    calRawAndColumn();
                    initChildViews();
                    requestLayout();
                    if (mListener != null)
                        mListener.onNineGirdItemDeleted(position, gridBean, imageContainer);
                }
            });
            addView(imageContainer, position);
        }

        setIsEditMode(mIsEditMode);
    }

    /**
     * Set weather is in edit mode
     */
    public void setIsEditMode(boolean b)
    {
        mIsEditMode = b;
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++)
        {
            View childView = getChildAt(i);
            if (childView instanceof NineGirdImageContainer)
                ((NineGirdImageContainer) childView).setIsDeleteMode(b);
        }

        //Add plus button in edit mode
        if (canShowAddMore())
        {
            if (mImgAddData != null)
                return;

            mImgAddData = new NineGridImageView(getContext());
            mImgAddData.setImageResource(R.drawable.ic_ninegrid_addmore);
            int padddingSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10
                    , getContext().getResources().getDisplayMetrics());
            mImgAddData.setPadding(padddingSize, padddingSize, padddingSize, padddingSize);
            mImgAddData.setScaleType(ImageView.ScaleType.FIT_XY);
            mImgAddData.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    if (mListener != null)
                        mListener.onNineGirdAddMoreClick(mMaxNum - mDataList.size());
                }
            });
            addView(mImgAddData);
        } else
        {
            if (mImgAddData != null)
                removeView(mImgAddData);
            mImgAddData = null;
        }

        calRawAndColumn();
        requestLayout();
    }

    //Check if is in edit mode
    private boolean canShowAddMore()
    {
        return mIsEditMode && mDataList.size() < mMaxNum;
    }

    /**
     * Set up imageloader
     */
    public void setImageLoader(INineGridImageLoader loader)
    {
        this.mImageLoader = loader;
    }

    /**
     * Set column count
     */
    public void setColumnCount(int columnCount)
    {
        this.mColumnCount = columnCount;
        calRawAndColumn();
        requestLayout();
    }

    /**
     * Set the maximum number
     */
    public void setMaxNum(int maxNum)
    {
        this.mMaxNum = maxNum;
    }

    /**
     * Set the space size, dip unit
     */
    public void setSpcaeSize(int dpValue)
    {
        this.mSpaceSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue
                , getContext().getResources().getDisplayMetrics());
    }

    /**
     * Set the size of imageview while there has only one image, dip unit
     */
    public void setSingleImageSize(int dpValue)
    {
        this.mSingleImageSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue
                , getContext().getResources().getDisplayMetrics());
    }

    /**
     * Set the aspect ratio of only one imageview
     */
    public void setSingleImageRatio(float ratio)
    {
        this.mSingleImageRatio = ratio;
    }

    //clear all views
    private void clearAllViews()
    {
        removeAllViews();
        if (mImgAddData != null)
            removeView(mImgAddData);
        mImgAddData = null;
    }

    /**
     * Get data source
     */
    public List<NineGridBean> getDataList()
    {
        return mDataList;
    }

    /**
     * Set up child view click listener
     */
    public void setOnItemClickListener(onItemClickListener l)
    {
        this.mListener = l;
    }

    public interface onItemClickListener
    {
        /**
         * Callback when clcik plus button be clicked
         *
         * @param cha the diff value between current data number displayed and maximum number
         */
        void onNineGirdAddMoreClick(int cha);

        /**
         * Callback when image be clicked
         *
         * @param position       position,started with 0
         * @param gridBean       data of image be clicked
         * @param imageContainer image container of image be clicked
         */
        void onNineGirdItemClick(int position, NineGridBean gridBean, NineGirdImageContainer imageContainer);

        /**
         * Callback when one image be deleted
         *
         * @param position       position,started with 0
         * @param gridBean       data of image be clicked
         * @param imageContainer image container of image be clicked
         */
        void onNineGirdItemDeleted(int position, NineGridBean gridBean, NineGirdImageContainer imageContainer);
    }

    /*****************************************************
     * State cache
     ****************************************************************/
    private final String SINGLE_IMAGE_SIZE = "singleImageSize";
    private final String SINGLE_IMAGE_RATIO = "singleImgaeRatio";
    private final String SPACE_SIZE = "spaceSize";
    private final String COLUMN_COUNT = "columnCount";
    private final String RAW_COUNT = "rawCount";
    private final String MAX_NUM = "maxNum";
    private final String EDIT_MODE = "delMode";
    private final String DATALIST = "datalist";

    @Override
    protected Parcelable onSaveInstanceState()
    {
        //There has to called super.onSaveInstanceState(),
        //or throw error:Derived class did not call super.onSaveInstanceState()
        super.onSaveInstanceState();
        Bundle bundle = new Bundle();
        bundle.putInt(SINGLE_IMAGE_SIZE, mSingleImageSize);
        bundle.putFloat(SINGLE_IMAGE_RATIO, mSingleImageRatio);
        bundle.putInt(SPACE_SIZE, mSpaceSize);
        bundle.putInt(COLUMN_COUNT, mColumnCount);
        bundle.putInt(RAW_COUNT, mRawCount);
        bundle.putInt(MAX_NUM, mMaxNum);
        bundle.putBoolean(EDIT_MODE, mIsEditMode);
        bundle.putParcelableArrayList(DATALIST, (ArrayList<? extends Parcelable>) mDataList);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state)
    {
        super.onRestoreInstanceState(state);
        Bundle bundle = (Bundle) state;
        this.mSingleImageSize = bundle.getInt(SINGLE_IMAGE_SIZE);
        this.mSingleImageRatio = bundle.getFloat(SINGLE_IMAGE_RATIO);
        this.mSpaceSize = bundle.getInt(SPACE_SIZE);
        this.mColumnCount = bundle.getInt(COLUMN_COUNT);
        this.mRawCount = bundle.getInt(RAW_COUNT);
        this.mMaxNum = bundle.getInt(MAX_NUM);
        this.mIsEditMode = bundle.getBoolean(EDIT_MODE);
        this.mDataList = bundle.getParcelableArrayList(DATALIST);
    }
}