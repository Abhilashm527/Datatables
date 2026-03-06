package com.dataflow.DataTable.repository;

import com.dataflow.DataTable.model.DataTableSchema;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DataTableSchemaRepository extends MongoRepository<DataTableSchema, String> {
    
    Optional<DataTableSchema> findByTableName(String tableName);
    
    boolean existsByTableName(String tableName);
    
    void deleteByTableName(String tableName);
}