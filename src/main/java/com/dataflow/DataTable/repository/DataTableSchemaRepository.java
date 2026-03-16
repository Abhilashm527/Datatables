package com.dataflow.DataTable.repository;

import com.dataflow.DataTable.model.DataTableSchema;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DataTableSchemaRepository extends MongoRepository<DataTableSchema, String> {
    
    Optional<DataTableSchema> findByTableName(String tableName);
    
    Optional<DataTableSchema> findByTableNameAndApplicationId(String tableName, String applicationId);
    
    List<DataTableSchema> findByApplicationId(String applicationId);
    
    boolean existsByTableName(String tableName);
    
    void deleteByTableName(String tableName);
}