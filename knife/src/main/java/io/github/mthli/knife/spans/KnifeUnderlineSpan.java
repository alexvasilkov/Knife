package io.github.mthli.knife.spans;

import android.os.Parcel;
import android.text.style.UnderlineSpan;

public class KnifeUnderlineSpan extends UnderlineSpan {

    public KnifeUnderlineSpan() {
    }

    @SuppressWarnings("unused") // Parcelable implementation
    public KnifeUnderlineSpan(Parcel src) {
        super(src);
    }

}
