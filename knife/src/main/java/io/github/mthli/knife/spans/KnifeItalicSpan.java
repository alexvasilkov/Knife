package io.github.mthli.knife.spans;

import android.graphics.Typeface;
import android.os.Parcel;
import android.text.style.StyleSpan;

public class KnifeItalicSpan extends StyleSpan {

    public static final int STYLE = Typeface.ITALIC;

    public KnifeItalicSpan() {
        super(STYLE);
    }

    @SuppressWarnings("unused") // Parcelable implementation
    public KnifeItalicSpan(Parcel src) {
        super(src);
    }

}
