package io.github.mthli.knife;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.Editable;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BulletSpan;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

import io.github.mthli.knife.spans.KnifeBoldSpan;
import io.github.mthli.knife.spans.KnifeBulletSpan;
import io.github.mthli.knife.spans.KnifeItalicSpan;
import io.github.mthli.knife.spans.KnifeQuoteSpan;
import io.github.mthli.knife.spans.KnifeStrikethroughSpan;
import io.github.mthli.knife.spans.KnifeURLSpan;
import io.github.mthli.knife.spans.KnifeUnderlineSpan;

@SuppressWarnings({ "WeakerAccess", "unused" }) // Public API
public class Knife {

    public static final Class BOLD = KnifeBoldSpan.class;
    public static final Class ITALIC = KnifeItalicSpan.class;
    public static final Class UNDERLINE = KnifeUnderlineSpan.class;
    public static final Class STRIKE = KnifeStrikethroughSpan.class;
    public static final Class BULLET = KnifeBulletSpan.class;
    public static final Class QUOTE = KnifeQuoteSpan.class;
    public static final Class URL = KnifeURLSpan.class;

    private final TextView textView;

    private OnSelectionChangedListener selectionListener;
    private SpanWatcher spanWatcher;

    private int bulletColor = Color.BLUE;
    private int bulletRadius = 2;
    private int bulletGap = 8;
    private int linkColor = 0; // Defaults to textColorLink XML attribute
    private boolean linkUnderline = true;
    private int quoteColor = Color.BLUE;
    private int quoteStripeWidth = 2;
    private int quoteGap = 8;

    private String currentUrl;

    public Knife(final TextView textView) {
        this.textView = textView;

        bulletRadius = convertDpToPixels(bulletRadius);
        bulletGap = convertDpToPixels(bulletGap);
        quoteStripeWidth = convertDpToPixels(quoteStripeWidth);
        quoteGap = convertDpToPixels(quoteGap);

        TypedArray arr = textView.getContext().obtainStyledAttributes(null, R.styleable.Knife);
        bulletColor = arr.getColor(R.styleable.Knife_knife_bulletColor, bulletColor);
        bulletRadius = arr.getDimensionPixelSize(
                R.styleable.Knife_knife_bulletRadius, bulletRadius);
        bulletGap = arr.getDimensionPixelSize(R.styleable.Knife_knife_bulletGap, bulletGap);
        linkColor = arr.getColor(R.styleable.Knife_knife_linkColor, linkColor);
        linkUnderline = arr.getBoolean(R.styleable.Knife_knife_linkColor, linkUnderline);
        quoteColor = arr.getColor(R.styleable.Knife_knife_quoteColor, 0);
        quoteStripeWidth = arr.getDimensionPixelSize(
                R.styleable.Knife_knife_quoteStripeWidth, quoteStripeWidth);
        quoteGap = arr.getDimensionPixelSize(R.styleable.Knife_knife_quoteGap, quoteGap);
        arr.recycle();


        textView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable text) {
                ensureSpanWatcher();
                switchToKnifeStyle(text);

                fixParagraphs(text, BULLET);
                fixParagraphs(text, QUOTE);
            }
        });


        spanWatcher = new SpanWatcher() {
            @Override
            public void onSpanAdded(Spannable text, Object what, int start, int end) {
                // We don't want someone else to draw underline
                if (what.getClass() == UnderlineSpan.class) {
                    text.removeSpan(what);
                }
            }

            @Override
            public void onSpanRemoved(Spannable text, Object what, int start, int end) {
            }

            @Override
            public void onSpanChanged(Spannable text, Object what, int ostart, int oend,
                    int nstart, int nend) {
                if (selectionListener != null) {
                    if (what == Selection.SELECTION_END) {
                        selectionListener.onSelectionChanged();
                    }
                }
            }
        };

        ensureSpanWatcher();
    }

    private int convertDpToPixels(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                textView.getResources().getDisplayMetrics()));
    }

    private void ensureSpanWatcher() {
        final Spannable text = textView.getEditableText();
        final SpanWatcher[] watchers = text.getSpans(0, 0, SpanWatcher.class);

        boolean found = false;
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < watchers.length; i++) {
            if (watchers[i] == spanWatcher) {
                found = true;
                break;
            }
        }

        if (!found) {
            text.setSpan(spanWatcher, 0, text.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }
    }


    // Helper ======================================================================================

    private void logSpans() {
        final Spanned text = textView.getEditableText();
        for (Object span : text.getSpans(0, text.length(), Object.class)) {
            int start = text.getSpanStart(span);
            int end = text.getSpanEnd(span);
            Log.d("SPAN", span.getClass().getSimpleName() + ": " + start + " - " + end
                    + " // " + text.subSequence(start, end) + " // " + text.getSpanFlags(span));
        }
    }


    // Public methods ==============================================================================

    public void setHtml(String html) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(KnifeParser.fromHtml(html));
        switchToKnifeStyle(builder);
        textView.setText(builder);
    }

    public String getHtml() {
        return KnifeParser.toHtml(textView.getEditableText());
    }

    public void set(Class spanClass) {
        set(spanClass, textView.getSelectionStart(), textView.getSelectionEnd());
    }

    public void set(Class spanClass, int start, int end) {
        if (isParagraphSpan(spanClass)) {
            setParagraph(textView.getEditableText(), spanClass, start, end);
        } else {
            setSpan(textView.getEditableText(), spanClass, start, end);
        }

        if (selectionListener != null) {
            selectionListener.onSelectionChanged();
        }
    }

    public void remove(Class spanClass) {
        remove(spanClass, textView.getSelectionStart(), textView.getSelectionEnd());
    }

    public void remove(Class spanClass, int start, int end) {
        if (isParagraphSpan(spanClass)) {
            removeParagraph(textView.getEditableText(), spanClass, start, end);
        } else {
            removeSpan(textView.getEditableText(), spanClass, start, end);
        }

        if (selectionListener != null) {
            selectionListener.onSelectionChanged();
        }
    }

    public boolean has(Class spanClass) {
        return has(spanClass, textView.getSelectionStart(), textView.getSelectionEnd());
    }

    public boolean has(Class spanClass, int start, int end) {
        if (isParagraphSpan(spanClass)) {
            return isFullOfParagraphs(textView.getEditableText(), spanClass, start, end);
        } else {
            return isFullySpanned(textView.getEditableText(), spanClass, start, end);
        }
    }

    public void toggle(Class spanClass) {
        toggle(spanClass, textView.getSelectionStart(), textView.getSelectionEnd());
    }

    public void toggle(Class spanClass, int start, int end) {
        if (isParagraphSpan(spanClass)) {
            toggleParagraph(textView.getEditableText(), spanClass, start, end);
        } else {
            toggleSpan(textView.getEditableText(), spanClass, start, end);
        }

        if (selectionListener != null) {
            selectionListener.onSelectionChanged();
        }
    }

    public void clearFormat() {
        remove(BOLD);
        remove(ITALIC);
        remove(UNDERLINE);
        remove(STRIKE);
        remove(BULLET);
        remove(QUOTE);
        remove(URL);
    }

    public void setLink(String url, int start, int end) {
        currentUrl = url;
        set(URL, start, end);
        currentUrl = null;
    }

    public Span<String> getLink(int start) {
        final Spannable text = textView.getEditableText();
        final URLSpan[] urls = text.getSpans(start, start, URLSpan.class);
        return urls.length == 0 ? null : new Span<>(urls[0].getURL(),
                text.getSpanStart(urls[0]), text.getSpanEnd(urls[0]));
    }

    public void setSelectionListener(final OnSelectionChangedListener listener) {
        selectionListener = listener;
    }

    // Spans classes ===============================================================================

    private void switchToKnifeStyle(Spannable text) {
        final int length = text.length();
        final Object[] spans = text.getSpans(0, length, Object.class);

        Object span;
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < spans.length; i++) {
            span = spans[i];

            if (span.getClass().getSimpleName().startsWith("Knife")) {
                continue;
            }

            final int spanStart = text.getSpanStart(span);
            final int spanEnd = text.getSpanEnd(span);

            if (span instanceof StyleSpan) {

                final int style = ((StyleSpan) span).getStyle();

                if (style == KnifeBoldSpan.STYLE) {
                    text.removeSpan(span);
                    setSpan(text, BOLD, spanStart, spanEnd);
                } else if (style == KnifeItalicSpan.STYLE) {
                    text.removeSpan(span);
                    setSpan(text, ITALIC, spanStart, spanEnd);
                }

            } else if (span instanceof UnderlineSpan) {

                text.removeSpan(span);
                setSpan(text, UNDERLINE, spanStart, spanEnd);

            } else if (span instanceof StrikethroughSpan) {

                text.removeSpan(span);
                setSpan(text, STRIKE, spanStart, spanEnd);

            } else if (span instanceof BulletSpan) {

                text.removeSpan(span);
                setParagraph(text, BULLET, spanStart, spanEnd);

            } else if (span instanceof QuoteSpan) {

                text.removeSpan(span);
                setParagraph(text, QUOTE, spanStart, spanEnd);

            } else if (span instanceof URLSpan) {

                text.removeSpan(span);
                currentUrl = ((URLSpan) span).getURL();
                setSpan(text, URL, spanStart, spanEnd);
                currentUrl = null;

            }
        }
    }

    private Object createSpan(Class spanClass) {
        if (spanClass == BOLD) {
            return new KnifeBoldSpan();
        } else if (spanClass == ITALIC) {
            return new KnifeItalicSpan();
        } else if (spanClass == UNDERLINE) {
            return new KnifeUnderlineSpan();
        } else if (spanClass == STRIKE) {
            return new KnifeStrikethroughSpan();
        } else if (spanClass == BULLET) {
            return new KnifeBulletSpan(bulletColor, bulletRadius, bulletGap);
        } else if (spanClass == QUOTE) {
            return new KnifeQuoteSpan(quoteColor, quoteStripeWidth, quoteGap);
        } else if (spanClass == URL) {
            if (currentUrl == null || currentUrl.length() == 0) {
                throw new IllegalArgumentException("Use setLink() method to add links");
            }
            return new KnifeURLSpan(currentUrl, linkColor, linkUnderline);
        } else {
            throw new IllegalArgumentException("Unknown span type: " + spanClass.getSimpleName());
        }
    }

    private boolean isParagraphSpan(Class spanClass) {
        return spanClass == BULLET || spanClass == QUOTE;
    }

    private boolean isSplittableSpan(Class spanClass) {
        return spanClass != URL;
    }

    // Regular spans logic =========================================================================

    private void setSpan(Spannable text, Class spanClass, int start, int end) {
        if (start == end) {
            // Including span's end position
            final Object[] spans = text.getSpans(start, end, spanClass);
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < spans.length; i++) {
                if (text.getSpanEnd(spans[i]) == end) {
                    setSpanFlag(text, spans[i], Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                }
            }
        } else {
            removeSpan(text, spanClass, start, end);

            if (isSplittableSpan(spanClass)) {
                // Merging with previous spans
                final Object[] spansBefore = text.getSpans(start, start, spanClass);
                //noinspection ForLoopReplaceableByForEach
                for (int i = 0; i < spansBefore.length; i++) {
                    start = Math.min(start, text.getSpanStart(spansBefore[i]));
                    text.removeSpan(spansBefore[i]);
                }

                // Merging with next spans
                final Object[] spansAfter = text.getSpans(end, end, spanClass);
                //noinspection ForLoopReplaceableByForEach
                for (int i = 0; i < spansAfter.length; i++) {
                    end = Math.max(end, text.getSpanEnd(spansAfter[i]));
                    text.removeSpan(spansAfter[i]);
                }
            }

            text.setSpan(createSpan(spanClass), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        }
    }

    private void removeSpan(Spannable text, Class spanClass, int start, int end) {
        if (start == end) {
            // Excluding span's end position
            final Object[] spans = text.getSpans(start, end, spanClass);
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < spans.length; i++) {
                if (text.getSpanEnd(spans[i]) == end) {
                    setSpanFlag(text, spans[i], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        } else {
            final Object[] spans = text.getSpans(start, end, spanClass);

            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < spans.length; i++) {
                final int spanStart = text.getSpanStart(spans[i]);
                final int spanEnd = text.getSpanEnd(spans[i]);

                // Removing overlapping style
                text.removeSpan(spans[i]);

                if (isSplittableSpan(spanClass)) {
                    // Adding back styles that are left on the sides
                    if (spanStart < start) {
                        setSpan(text, spanClass, spanStart, start);
                    }
                    if (end < spanEnd) {
                        setSpan(text, spanClass, end, spanEnd);
                    }
                }
            }
        }
    }

    private boolean isFullySpanned(Spannable text, Class spanClass, int start, int end) {
        final Object[] spans = text.getSpans(start, end, spanClass);

        if (start == end) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < spans.length; i++) {
                final int spanStart = text.getSpanStart(spans[i]);
                final int spanEnd = text.getSpanEnd(spans[i]);
                final int spanFlag = text.getSpanFlags(spans[i]);
                if ((start > spanStart && end < spanEnd)
                        || (end == spanEnd && spanFlag == Spanned.SPAN_EXCLUSIVE_INCLUSIVE)) {
                    return true;
                }
            }
            return false;
        }

        // Going through each character and checking if there is a span of given type for it
        for (int pos = start; pos < end; pos++) {
            boolean found = false;
            //noinspection ForLoopReplaceableByForEach
            for (int s = 0; s < spans.length; s++) {
                if (pos >= text.getSpanStart(spans[s]) && pos < text.getSpanEnd(spans[s])) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        return true;
    }

    private void toggleSpan(Spannable text, Class spanClass, int start, int end) {
        if (isFullySpanned(text, spanClass, start, end)) {
            removeSpan(text, spanClass, start, end);
        } else {
            setSpan(text, spanClass, start, end);
        }
    }

    private static void setSpanFlag(Spannable text, Object span, int flag) {
        int spanStart = text.getSpanStart(span);
        int spanEnd = text.getSpanEnd(span);
        text.removeSpan(span);
        text.setSpan(span, spanStart, spanEnd, flag);
    }

    // Paragraph spans logic =======================================================================

    private void setParagraph(Spannable text, Class spanClass, int start, int end) {
        // Getting selection's paragraph bounds
        start = findLineStart(text, start);
        end = findLineEnd(text, end);

        // Adding new span for each line in selection which does not contain same span yet
        int lineStart = start;

        while (lineStart < end) {
            int lineEnd = findLineEnd(text, lineStart);
            if (!containsSpan(text, spanClass, lineStart, lineEnd) && lineStart != lineEnd) {
                text.setSpan(createSpan(spanClass),
                        lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            lineStart = lineEnd + 1;
        }
    }

    private void removeParagraph(Spannable text, Class spanClass, int start, int end) {
        // Getting selection's paragraph bounds
        start = findLineStart(text, start);
        end = findLineEnd(text, end);

        // Removing spans within the bounds
        final Object[] spans = text.getSpans(start, end, spanClass);
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < spans.length; i++) {
            text.removeSpan(spans[i]);
        }
    }

    private boolean isFullOfParagraphs(Spannable text, Class spanClass, int start, int end) {
        // Getting selection's paragraph bounds
        start = findLineStart(text, start);
        end = findLineEnd(text, end);

        if (start == end) {
            return false;
        }

        // Checking each line for paragraph span
        int lineStart = start;

        while (lineStart <= end) {
            int lineEnd = findLineEnd(text, lineStart);
            if (!containsSpan(text, spanClass, lineStart, lineEnd)) {
                return false;
            }
            lineStart = lineEnd + 1;
        }
        return true;
    }

    private void toggleParagraph(Spannable text, Class spanClass, int start, int end) {
        if (isFullOfParagraphs(text, spanClass, start, end)) {
            removeParagraph(text, spanClass, start, end);
        } else {
            setParagraph(text, spanClass, start, end);
        }
    }

    private void fixParagraphs(Spannable text, Class spanClass) {
        final Object[] spans = text.getSpans(0, text.length(), spanClass);

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < spans.length; i++) {
            int spanStart = text.getSpanStart(spans[i]);
            int spanEnd = text.getSpanEnd(spans[i]);
            int start = findLineStart(text, spanStart);
            int end = findLineEnd(text, spanEnd);

            if (spanStart == start && spanEnd == end) {
                continue;
            }

            text.removeSpan(spans[i]);

            // Adding new bullets for each line in selection
            int lineStart = start;

            while (lineStart < end) {
                int lineEnd = findLineEnd(text, lineStart);
                if (!containsSpan(text, spanClass, lineStart, lineEnd) && lineStart != lineEnd) {
                    text.setSpan(createSpan(spanClass),
                            lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                lineStart = lineEnd;
            }
        }
    }

    // Returns cursor position just after the previous \n or 0
    private static int findLineStart(CharSequence text, int pos) {
        if (pos < 0 || pos > text.length()) {
            return -1;
        }
        if (pos == 0) {
            return 0;
        }
        for (int i = pos - 1; i >= 0; i--) {
            if (text.charAt(i) == '\n') {
                return i + 1;
            }
        }
        return 0;
    }

    // Returns cursor position just before the next \n or text length
    private static int findLineEnd(CharSequence text, int pos) {
        if (pos < 0 || pos > text.length()) {
            return -1;
        }
        if (pos == text.length()) {
            return text.length();
        }
        for (int i = pos; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                return i;
            }
        }
        return text.length();
    }

    private static boolean containsSpan(Spanned text, Class spanClass, int start, int end) {
        return text.getSpans(start, end, spanClass).length > 0;
    }


    public interface OnSelectionChangedListener {
        void onSelectionChanged();
    }

}
