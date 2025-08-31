package org.projectcontrol.core.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class ListUtils {

    public static <T> boolean isEmpty(List<String> list) {
        if (list == null || list.isEmpty()) {
            return true;
        } else {
            if (list.size() == 1 && StringUtils.isBlank(list.getFirst())) {
                return true;
            }
            return false;
        }
    }
}
