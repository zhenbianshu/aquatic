package com.aquatic.service;

import java.util.List;
import java.util.Map;

public interface AtmosphereQueryService {
    boolean add(Map<String, String> data);

    boolean delete(String id);

    boolean update(Map<String, String> data);

    List<Map<String, Object>> query(Map<String, String> condition);

    int queryTotal(Map<String, String> condition);
}
