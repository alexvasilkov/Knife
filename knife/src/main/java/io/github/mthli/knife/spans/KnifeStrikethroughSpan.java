package io.github.mthli.knife.spans;

import android.os.Parcel;
import android.text.style.StrikethroughSpan;

public class KnifeStrikethroughSpan extends StrikethroughSpan {

    public KnifeStrikethroughSpan() {
    }

    @SuppressWarnings("unused") // Parcelable implementation
    public KnifeStrikethroughSpan(Parcel src) {
        super(src);
    }

}
