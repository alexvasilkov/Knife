package io.github.mthli.knife.spans;

import android.graphics.Typeface;
import android.os.Parcel;
import android.text.style.StyleSpan;

public class KnifeBoldSpan extends StyleSpan {

    public static final int STYLE = Typeface.BOLD;

    public KnifeBoldSpan() {
        super(STYLE);
    }

    @SuppressWarnings("unused") // Parcelable implementation
    public KnifeBoldSpan(Parcel src) {
        super(src);
    }

}
