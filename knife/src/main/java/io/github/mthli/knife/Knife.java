package io.github.mthli.knife;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.Editable;
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
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

import io.github.mthli.knife.spans.KnifeBoldSpan;
import io.github.mthli.knife.spans.KnifeBulletSpan;
import io.github.mthli.knife.spans.KnifeItalicSpan;
import io.github.mthli.knife.spans.KnifeQuoteSpan;
import io.github.mthli.knife.spans.KnifeURLSpan;

@SuppressWarnings({ "WeakerAccess", "unused" }) // Public API
public class Knife {

    public static final Class BOLD = KnifeBoldSpan.class;
    public static final Class ITALIC = KnifeItalicSpan.class;
    public static final Class UNDERLINE = UnderlineSpan.class;
    public static final Class STRIKE = StrikethroughSpan.class;
    public static final Class BULLET = KnifeBulletSpan.class;
    public static final Class QUOTE = KnifeQuoteSpan.class;
    public static final Class URL = KnifeURLSpan.class;

    private final TextView textView;

    private int bulletColor = Color.BLUE;
    private int bulletRadius = 2;
    private int bulletGap = 8;
    private int linkColor = 0; // Defaults to textColorLink XML attribute
    private boolean linkUnderline = true;
    private int quoteColor = Color.BLUE;
    private int quoteStripeWidth = 2;
    private int quoteGap = 8;

    private String currentUrl;

    public Knife(TextView textView) {
        this(textView, null);
    }

    public Knife(TextView textView, AttributeSet attrs) {
        this.textView = textView;

        bulletRadius = convertDpToPixels(bulletRadius);
        bulletGap = convertDpToPixels(bulletGap);
        quoteStripeWidth = convertDpToPixels(quoteStripeWidth);
        quoteGap = convertDpToPixels(quoteGap);

        TypedArray arr = textView.getContext().obtainStyledAttributes(attrs, R.styleable.KnifeText);
        bulletColor = arr.getColor(R.styleable.KnifeText_bulletColor, bulletColor);
        bulletRadius = arr.getDimensionPixelSize(R.styleable.KnifeText_bulletRadius, bulletRadius);
        bulletGap = arr.getDimensionPixelSize(R.styleable.KnifeText_bulletGap, bulletGap);
        linkColor = arr.getColor(R.styleable.KnifeText_linkColor, linkColor);
        linkUnderline = arr.getBoolean(R.styleable.KnifeText_linkColor, linkUnderline);
        quoteColor = arr.getColor(R.styleable.KnifeText_quoteColor, 0);
        quoteStripeWidth = arr.getDimensionPixelSize(
                R.styleable.KnifeText_quoteStripeWidth, quoteStripeWidth);
        quoteGap = arr.getDimensionPixelSize(R.styleable.KnifeText_quoteGap, quoteGap);
        arr.recycle();


        textView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable text) {
                fixParagraphs(text, BULLET);
                fixParagraphs(text, QUOTE);
            }
        });
    }

    private int convertDpToPixels(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                textView.getResources().getDisplayMetrics()));
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

        logSpans();
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

    // Spans classes ===============================================================================

    private void switchToKnifeStyle(Spannable text) {
        final int length = text.length();
        final Object[] spans = text.getSpans(0, length, Object.class);

        for (Object span : spans) {
            if (span instanceof BulletSpan) {

                final int spanStart = text.getSpanStart(span);
                final int spanEnd = text.getSpanEnd(span);
                text.removeSpan(span);
                setParagraph(text, BULLET, spanStart, spanEnd);

            } else if (span instanceof QuoteSpan) {

                final int spanStart = text.getSpanStart(span);
                final int spanEnd = text.getSpanEnd(span);
                text.removeSpan(span);
                setParagraph(text, QUOTE, spanStart, spanEnd);

            } else if (span instanceof URLSpan) {

                final int spanStart = text.getSpanStart(span);
                final int spanEnd = text.getSpanEnd(span);
                text.removeSpan(span);
                currentUrl = ((URLSpan) span).getURL();
                setSpan(text, URL, spanStart, spanEnd);

            } else if (span instanceof StyleSpan) {
                final int style = ((StyleSpan) span).getStyle();
                final int spanStart = text.getSpanStart(span);
                final int spanEnd = text.getSpanEnd(span);

                if (style == KnifeBoldSpan.STYLE) {
                    text.removeSpan(span);
                    setSpan(text, BOLD, spanStart, spanEnd);
                } else if (style == KnifeItalicSpan.STYLE) {
                    text.removeSpan(span);
                    setSpan(text, ITALIC, spanStart, spanEnd);
                }
            }
        }

        currentUrl = null;
    }

    private Object createSpan(Class spanClass) {
        if (spanClass == BOLD) {
            return new KnifeBoldSpan();
        } else if (spanClass == ITALIC) {
            return new KnifeItalicSpan();
        } else if (spanClass == UNDERLINE) {
            return new UnderlineSpan();
        } else if (spanClass == STRIKE) {
            return new StrikethroughSpan();
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
        removeSpan(text, spanClass, start, end);

        if (start != end) {
            text.setSpan(createSpan(spanClass), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (isSplittableSpan(spanClass)) {
                // TODO: merge sides
            }
        }
    }

    private void removeSpan(Spannable text, Class spanClass, int start, int end) {
        if (start != end) {
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
        if (start == end) {
            return false;
        }

        final Object[] spans = text.getSpans(start, end, spanClass);

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

        // Checking each line for paragraph span
        int lineStart = start;

        while (lineStart < end) {
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

}
