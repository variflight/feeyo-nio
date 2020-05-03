package com.feeyo.net.codec.http;

import java.util.Map;
import java.util.regex.Pattern;

public class PathTrie<T> {
	
    private final TrieNode<T> root;
    private final Pattern pattern;
    private T rootValue;

    public PathTrie() {
        this("/", "*");
    }

    public PathTrie(String separator, String wildcard) {
        pattern = Pattern.compile(separator);
        root = new TrieNode<>(separator, null, null, wildcard);
    }


    public void insert(String path, T value) {
        String[] strings = pattern.split(path);
        if (strings.length == 0) {
            rootValue = value;
            return;
        }
        int index = 0;
        // supports initial delimiter.
        if (strings[0].isEmpty()) {
            index = 1;
        }
        root.insert(strings, index, value);
    }

    public T retrieve(String path) {
        return retrieve(path, null);
    }

    public T retrieve(String path, Map<String, String> params) {
        if (path.length() == 0) {
            return rootValue;
        }
        String[] strings = pattern.split(path);
        if (strings.length == 0) {
            return rootValue;
        }
        int index = 0;
        // supports initial delimiter.
        if (strings[0].isEmpty()) {
            index = 1;
        }
        return root.retrieve(strings, index, params);
    }
}