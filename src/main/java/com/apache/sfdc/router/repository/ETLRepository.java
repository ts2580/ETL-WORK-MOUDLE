package com.apache.sfdc.router.repository;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ETLRepository {
    void setFieldDef(String ddl);

    int insertObject(@Param("upperQuery") String upperQuery, @Param("listUnderQuery") List<String> listUnderQuery);

    void setTable(String string);
}
