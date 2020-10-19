> 阅读本文,您需要对[View绘制流程](https://www.jianshu.com/p/5a71014e7b1b)有充分的了解,没有这方面知识储备建议打开链接前往学习

## 一. 业务背景
你自定义过ViewGroup吗?很抱歉的告诉大家,其实我不是很会哦,在斗鱼工作那段时间,发现有不少好玩的UI组件,其实我也挺感兴趣的,不过企业以业务为导向,很少有亲自实现一个ViewGroup,今天我就带小伙伴们一起实现一个标签控件:
该 layout 使子 View 类似 CSS 中的 float:left 效果, 从左到右排列子 View 并自动换行。支持以下特性：


- 可以控制子 View 的垂直/水平间距
- 可以控制子 View 的最多个数或最大行数
- 支持xml自定义属性

![](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/462b133cf2194401ac49978dc1fb7dd4~tplv-k3u1fbpfcp-watermark.image)

## 二. 实现思路


### 2.1 定义一个MkFloatLayout,继承ViewGroup,重写三个重要的构造方法

```java
    public MkFloatLayout(Context context) {
        this(context, null);
    }

    public MkFloatLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MkFloatLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }
```

### 2.2 自定义MkFloatLayout属性,然后在styles里面定义属性值

```xml
    <declare-styleable name="MkFloatLayout">
        <attr name="android:gravity" />
        <attr name="dimision" name="mk_childHorizontalSpacing" />
        <attr name="dimision" name="mk_childVerticalSpacing" />
        <attr name="android:maxLines" />
        <attr name="mk_maxNumber" />
    </declare-styleable>
```

### 2.3 初始化MkFlayout的属性

- a. 设置子`View`水平间距
- b. 设置子`View`垂直间距
- c. 设置子 View 的对齐方式，目前支持 `CENTER_HORIZONTAL` `LEFT` 和  `RIGHT`
- d. 设置最多可显示的子`View`个数,注意该方法不会改变子`View`的个数，只会影响显示出来的子View个数
- e. 设置最多可显示的行数,注意该方法不会改变子`View`的个数，只会影响显示出来的子`View`个数

```java
    private void init(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs,
                R.styleable.MkFloatLayout);
        mChildHorizontalSpacing = array.getDimensionPixelSize(
                R.styleable.MkFloatLayout_mk_childHorizontalSpacing, 0);
        mChildVerticalSpacing = array.getDimensionPixelSize(
                R.styleable.MkFloatLayout_mk_childVerticalSpacing, 0);
        mGravity = array.getInteger(R.styleable.MkFloatLayout_android_gravity, Gravity.START);
        int maxLines = array.getInt(R.styleable.MkFloatLayout_android_maxLines, -1);
        if (maxLines >= 0) {
            setMaxLines(maxLines);
        }
        int maxNumber = array.getInt(R.styleable.MkFloatLayout_android_max, -1);
        if (maxNumber >= 0) {
            setMaxNumber(maxNumber);
        }
        array.recycle();
        array =null;
    }

       public void setMaxNumber(int maxNumber) {
        mMaximum = maxNumber;
        mMaxMode = NUMBER;
        requestLayout();
    }

     public void setMaxLines(int maxLines) {
        mMaximum = maxLines;
        mMaxMode = LINES;
        requestLayout();
    }

        /**
     * 获取最多可显示的子View个数
     */
    public int getMaxNumber() {
        return mMaxMode == NUMBER ? mMaximum : -1;
    }

    /**
     * 获取最多可显示的行数
     *
     * @return 没有限制时返回-1
     */
    public int getMaxLines() {
        return mMaxMode == LINES ? mMaximum : -1;
    }

        /**
     * 设置子 View 的水平间距
     */
    public void setChildHorizontalSpacing(int spacing) {
        mChildHorizontalSpacing = spacing;
        invalidate();
    }

        /**
     * 设置子 View 的垂直间距
     */
    public void setChildVerticalSpacing(int spacing) {
        mChildVerticalSpacing = spacing;
        invalidate();
    }



    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            mGravity = gravity;
            requestLayout();
        }
    }

    public int getGravity() {
        return mGravity;
    }


```


### 2.4 重写onMeasure方法
#### 2.4.1 拿到MkFlayout的测量规格

```java
    int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
    int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
    int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
    int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
```



![](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/dd8132803db94318bf90d5adf6af7fb6~tplv-k3u1fbpfcp-watermark.image)

#### 2.4.2 若FloatLayout指定了MATCH_PARENT或固定宽度，则需要使子View换行

```java
// 子view最大高度
       int maxLineHeight = 0;
// 测量或的宽度
        int resultWidth;
 // 测量后的高度
        int resultHeight;

// 获取子View个数
        final int count = getChildCount();
// 每一行的item数目，下标表示行下标，在onMeasured的时候计算得出，供onLayout去使用。
// 若mItemNumberInEachLine[x]==0，则表示第x行已经没有item了
        mItemNumberInEachLine = new int[count];
        // 每一行的item的宽度和（包括item直接的间距），下标表示行下标，
        // 如 mWidthSumInEachLine[x]表示第x行的item的宽度和（包括item直接的间距）
        // 在onMeasured的时候计算得出，供onLayout去使用
        mWidthSumInEachLine = new int[count];
        // 行的索引
        int lineIndex = 0;
        // 若FloatLayout指定了MATCH_PARENT或固定宽度，则需要使子View换行
        if (widthSpecMode == MeasureSpec.EXACTLY) {
            resultWidth = widthSpecSize;

            measuredChildCount = 0;

            // 下一个子View的position
            int childPositionX = getPaddingLeft();
            int childPositionY = getPaddingTop();

            // 子View的Right最大可达到的x坐标
            int childMaxRight = widthSpecSize - getPaddingRight();

            for (int i = 0; i < count; i++) {
                if (mMaxMode == NUMBER && measuredChildCount >= mMaximum) {
                    // 超出最多数量，则不再继续
                    break;
                } else if (mMaxMode == LINES && lineIndex >= mMaximum) {
                    // 超出最多行数，则不再继续
                    break;
                }

                // 获取子view的对象
                final View child = getChildAt(i);
                if (child.getVisibility() == GONE) {
                    continue;
                }
// 获取子view的测量参数
                final LayoutParams childLayoutParams = child.getLayoutParams();

                // 获取子View宽度测量规格
                final int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                        getPaddingLeft() + getPaddingRight(), childLayoutParams.width);

                // 获取子View高度测量规格
                final int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                        getPaddingTop() + getPaddingBottom(), childLayoutParams.height);

                // 获取View
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);

                // 获取子view的测量后宽度
                final int childw = child.getMeasuredWidth();

                // 获取子view最大允许的高度
                maxLineHeight = Math.max(maxLineHeight, child.getMeasuredHeight());

                // 需要换行 paddingLeft + childw
                if (childPositionX + childw > childMaxRight) {
                    // 如果换行后超出最大行数，则不再继续
                    if (mMaxMode == LINES) {
                        if (lineIndex + 1 >= mMaximum) {
                            break;
                        }
                    }

                    // 后面每次加item都会加上一个space，这样的话每行都会为最后一个item多加一次space，所以在这里减一次
                    mWidthSumInEachLine[lineIndex] -= mChildHorizontalSpacing;
                    // 换行
                    lineIndex++;
                    // 下一行第一个item的x
                    childPositionX = getPaddingLeft();
                    // 下一行第一个item的y
                    childPositionY += maxLineHeight + mChildVerticalSpacing;
                }
                mItemNumberInEachLine[lineIndex]++;
                mWidthSumInEachLine[lineIndex] += (childw + mChildHorizontalSpacing);
                childPositionX += (childw + mChildHorizontalSpacing);
                measuredChildCount++;
            }
            // 如果最后一个item不是刚好在行末（即lineCount最后没有+1，也就是mWidthSumInEachLine[lineCount]非0），则要减去最后一个item的space
            if (mWidthSumInEachLine.length > 0 && mWidthSumInEachLine[lineIndex] > 0) {
                mWidthSumInEachLine[lineIndex] -= mChildHorizontalSpacing;
            }
            if (heightSpecMode == MeasureSpec.UNSPECIFIED) {
                resultHeight = childPositionY + maxLineHeight + getPaddingBottom();
            } else if (heightSpecMode == MeasureSpec.AT_MOST) {
                resultHeight = childPositionY + maxLineHeight + getPaddingBottom();
                resultHeight = Math.min(resultHeight, heightSpecSize);
            } else {
                resultHeight = heightSpecSize;
            }

        }
```

#### 2.4.3 不计算换行，直接一行铺开

```java
resultWidth = getPaddingLeft() + getPaddingRight();
            measuredChildCount = 0;

            for (int i = 0; i < count; i++) {
                if (mMaxMode == NUMBER) {
                    // 超出最多数量，则不再继续
                    if (measuredChildCount > mMaximum) {
                        break;
                    }
                } else if (mMaxMode == LINES) {
                    // 超出最大行数，则不再继续
                    if (1 > mMaximum) {
                        break;
                    }
                }
                final View child = getChildAt(i);
                if (child.getVisibility() == GONE) {
                    continue;
                }
                final LayoutParams childLayoutParams = child.getLayoutParams();
                final int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                        getPaddingLeft() + getPaddingRight(), childLayoutParams.width);
                final int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                        getPaddingTop() + getPaddingBottom(), childLayoutParams.height);
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
                resultWidth += child.getMeasuredWidth();
                maxLineHeight = Math.max(maxLineHeight, child.getMeasuredHeight());
                measuredChildCount++;
            }
            if (measuredChildCount > 0) {
                resultWidth += mChildHorizontalSpacing * (measuredChildCount - 1);
            }
            resultHeight = maxLineHeight + getPaddingTop() + getPaddingBottom();
            if (mItemNumberInEachLine.length > 0) {
                mItemNumberInEachLine[lineIndex] = count;
            }
            if (mWidthSumInEachLine.length > 0) {
                mWidthSumInEachLine[0] = resultWidth;
            }
        }
        setMeasuredDimension(resultWidth, resultHeight);
        int meausureLineCount = lineIndex + 1;
        if (mLineCount != meausureLineCount) {
            if (mOnLineCountChangeListener != null) {
                mOnLineCountChangeListener.onChange(mLineCount, meausureLineCount);
            }
            mLineCount = meausureLineCount;
        }

```

### 2.5 重写onLayout方法

```java
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width = right - left;
        // 按照不同gravity使用不同的布局，默认是left
        switch (mGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.START:
                layoutWithGravityLeft(width);
                break;
            case Gravity.END:
                layoutWithGravityRight(width);
                break;
            case Gravity.CENTER_HORIZONTAL:
                layoutWithGravityCenterHorizontal(width);
                break;
            default:
                layoutWithGravityLeft(width);
                break;
        }
    }
```

#### 2.5.1 将子View居中布局

```java
    private void layoutWithGravityCenterHorizontal(int parentWidth) {
        int nextChildIndex = 0;
        int nextChildPositionX;
        int nextChildPositionY = getPaddingTop();
        int lineHeight = 0;
        int layoutChildCount = 0;
        int layoutChildEachLine = 0;

        // 遍历每一行
        for (int i = 0; i < mItemNumberInEachLine.length; i++) {
            // 如果这一行已经没item了，则退出循环
            if (mItemNumberInEachLine[i] == 0) {
                break;
            }

            // 遍历该行内的元素，布局每个元素
            nextChildPositionX = (parentWidth - getPaddingLeft() - getPaddingRight() - mWidthSumInEachLine[i]) / 2 + getPaddingLeft(); // 子 View 的最小 x 值
            while (layoutChildEachLine < mItemNumberInEachLine[i]) {
                final View childView = getChildAt(nextChildIndex);
                if (childView.getVisibility() == GONE) {
                    nextChildIndex++;
                    continue;
                }
                final int childw = childView.getMeasuredWidth();
                final int childh = childView.getMeasuredHeight();
                childView.layout(nextChildPositionX, nextChildPositionY, nextChildPositionX + childw, nextChildPositionY + childh);
                lineHeight = Math.max(lineHeight, childh);
                nextChildPositionX += childw + mChildHorizontalSpacing;
                layoutChildCount++;
                layoutChildEachLine++;
                nextChildIndex++;
                if (layoutChildCount == measuredChildCount) {
                    break;
                }
            }

            if (layoutChildCount == measuredChildCount) {
                break;
            }

            // 一行结束了，整理一下，准备下一行
            nextChildPositionY += (lineHeight + mChildVerticalSpacing);
            lineHeight = 0;
            layoutChildEachLine = 0;
        }

        int childCount = getChildCount();
        for (int i = nextChildIndex; i < childCount; i++) {
            final View childView = getChildAt(i);
            if (childView.getVisibility() == View.GONE) {
                continue;
            }
            childView.layout(0, 0, 0, 0);
        }
    }

```

#### 2.5.2 将子View靠右布局

```java
    private void layoutWithGravityRight(int parentWidth) {
        int nextChildIndex = 0;
        int nextChildPositionX;
        int nextChildPositionY = getPaddingTop();
        int lineHeight = 0;
        int layoutChildCount = 0;
        int layoutChildEachLine = 0;

        // 遍历每一行
        for (int i = 0; i < mItemNumberInEachLine.length; i++) {
            // 如果这一行已经没item了，则退出循环
            if (mItemNumberInEachLine[i] == 0) {
                break;
            }

            // 遍历该行内的元素，布局每个元素
            nextChildPositionX = parentWidth - getPaddingRight() - mWidthSumInEachLine[i]; // 初始值为子 View 的最小 x 值
            while (layoutChildEachLine < mItemNumberInEachLine[i]) {
                final View childView = getChildAt(nextChildIndex);
                if (childView.getVisibility() == GONE) {
                    nextChildIndex++;
                    continue;
                }
                final int childw = childView.getMeasuredWidth();
                final int childh = childView.getMeasuredHeight();
                childView.layout(nextChildPositionX, nextChildPositionY, nextChildPositionX + childw, nextChildPositionY + childh);
                lineHeight = Math.max(lineHeight, childh);
                nextChildPositionX += childw + mChildHorizontalSpacing;
                layoutChildCount++;
                layoutChildEachLine++;
                nextChildIndex++;
                if (layoutChildCount == measuredChildCount) {
                    break;
                }
            }
            if (layoutChildCount == measuredChildCount) {
                break;
            }

            // 一行结束了，整理一下，准备下一行
            nextChildPositionY += (lineHeight + mChildVerticalSpacing);
            lineHeight = 0;
            layoutChildEachLine = 0;
        }

        int childCount = getChildCount();
        for (int i = nextChildIndex; i < childCount; i++) {
            final View childView = getChildAt(i);
            if (childView.getVisibility() == View.GONE) {
                continue;
            }
            childView.layout(0, 0, 0, 0);
        }
    }
```


#### 2.5.3 将子View靠左布局

```java
        private void layoutWithGravityLeft(int parentWidth) {
        int childMaxRight = parentWidth - getPaddingRight();
        int childPositionX = getPaddingLeft();
        int childPositionY = getPaddingTop();
        int lineHeight = 0;
        final int childCount = getChildCount();
        int layoutChildCount = 0;
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            if (layoutChildCount < measuredChildCount) {
                final int childw = child.getMeasuredWidth();
                final int childh = child.getMeasuredHeight();
                if (childPositionX + childw > childMaxRight) {
                    // 换行
                    childPositionX = getPaddingLeft();
                    childPositionY += (lineHeight + mChildVerticalSpacing);
                    lineHeight = 0;
                }
                child.layout(childPositionX, childPositionY, childPositionX + childw, childPositionY + childh);
                childPositionX += childw + mChildHorizontalSpacing;
                lineHeight = Math.max(lineHeight, childh);
                layoutChildCount++;
            } else {
                child.layout(0, 0, 0, 0);
            }
        }
    }
 ```

 ## 三. 使用指南

- 设置高度最大行高和最大宽高

```xml
     <com.github.microkibaco.mk_video_view.MkFloatLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:id="@+id/test"
            android:layout_marginLeft="16dp"
            android:layout_marginTop="16dp"
            android:layout_marginEnd="12dp"
            android:layout_marginRight="12dp"
            android:layout_marginBottom="4dp"
            android:gravity="end"
            app:mk_childHorizontalSpacing="12dp"
            app:mk_childVerticalSpacing="12dp"
            />
```
java代码
```java

         MkFloatLayout mkfloatLayout = findViewById(R.id.hot_list);

        LayoutInflater layoutInflater = LayoutInflater.from(this);
        TextView hotWordTag = (TextView) layoutInflater.inflate(R.layout.view_search_word_tag, mkfloatLayout, false);
        hotWordTag.setText("你好");
        ImageView imgTag = (ImageView)         layoutInflater.inflate(R.layout.view_image_word_tag, mkfloatLayout, false);
         mkfloatLayout.addView(hotWordTag);
        mkfloatLayout.addView(imgTag);
 ```