package io.github.mthli.knife;

public class Span<T> {

    public final T data;
    public final int start;
    public final int end;

    Span(T data, int start, int end) {
        this.data = data;
        this.start = start;
        this.end = end;
    }

}
