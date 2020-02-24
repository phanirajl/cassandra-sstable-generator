package com.instaclustr.sstable.generator;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RowMapper1 implements RowMapper {


    public static final String KEYSPACE = "mykeyspace";
    public static final String TABLE = "mytable";

    public static final UUID UUID_1 = UUID.randomUUID();
    public static final UUID UUID_2 = UUID.randomUUID();
    public static final UUID UUID_3 = UUID.randomUUID();

    @Override
    public List<Object> map(final List<String> row) {
        return null;
    }

    @Override
    public List<List<Object>> get() {
        return new ArrayList<List<Object>>() {{
            add(new ArrayList<Object>() {{
                add(UUID_1);
                add("John");
                add("Doe");
            }});
            add(new ArrayList<Object>() {{
                add(UUID_2);
                add("Marry");
                add("Poppins");
            }});
            add(new ArrayList<Object>() {{
                add(UUID_3);
                add("Jim");
                add("Jack");
            }});
        }};
    }

    @Override
    public List<Object> random() {
        return null;
    }

    @Override
    public String insertStatement() {
        return format("INSERT INTO %s.%s (id, name, surname) VALUES (?, ?, ?);", KEYSPACE, TABLE);
    }
}
