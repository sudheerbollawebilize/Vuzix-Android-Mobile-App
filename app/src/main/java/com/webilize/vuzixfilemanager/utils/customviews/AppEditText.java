package com.webilize.vuzixfilemanager.utils.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

import com.webilize.vuzixfilemanager.R;

public class AppEditText extends AppCompatEditText {
    public AppEditText(Context context) {
        super(context);
    }

    public AppEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AppEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attributeSet) {
        String fontPath;
        TypedArray typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.AppEditText);
        int font_val = typedArray.getInteger(R.styleable.AppEditText_edt_font_type, 1);
//        boolean setInputType = typedArray.getBoolean(R.styleable.AppEditText_set_input_type, true);
        switch (font_val) {
            case 0:
                fontPath = "fonts/light.otf";
                break;
            case 1:
                fontPath = "fonts/regular.otf";
                break;
            case 2:
                fontPath = "fonts/bold.otf";
                break;
            case 3:
                fontPath = "fonts/italic.ttf";
                break;
            default:
                fontPath = "fonts/regular.otf";
                break;
        }
//        if (setInputType)
//            setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        Typeface tf = Typeface.createFromAsset(getContext().getAssets(), fontPath);
        setTypeface(tf);
        typedArray.recycle();
    }

}