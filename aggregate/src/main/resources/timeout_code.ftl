package com.dtstack.jlogstash.filters;

import java.util.*;
import java.lang.*;

public class AggregateTimeoutCodeUtil {

    public static Map handle(Map event) {
        ${code}
        return event;
    }
}