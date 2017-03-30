package com.dtstack.jlogstash.filters;

import java.util.*;
import java.lang.*;

public class AggregateCodeUtil {

    public static Map handle(Map event, Map map) {
        ${code}
        return event;
    }
}