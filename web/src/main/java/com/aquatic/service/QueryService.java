package com.aquatic.service;

import java.util.List;
import java.util.Map;

public interface QueryService {
    boolean add(String type, Map<String, String> data);

    boolean delete(String type, String id);

    boolean update(String type, Map<String, String> data);

    List<Map<String, Object>> query(String type, Map<String, String> condition);
}
