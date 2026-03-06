package com.dataflow.DataTable.repository;

import com.dataflow.DataTable.model.DataTableRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DataTableRecordRepository extends MongoRepository<DataTableRecord, String> {

    List<DataTableRecord> findByTableId(String tableId);

    Page<DataTableRecord> findByTableId(String tableId, Pageable pageable);

    long countByTableId(String tableId);

    void deleteByTableId(String tableId);

    @Query("{ 'tableId': ?0, 'searchText': { $regex: ?1, $options: 'i' } }")
    List<DataTableRecord> findByTableIdAndSearchText(String tableId, String searchText);

    @Query("{ 'tableId': ?0, 'searchText': { $regex: ?1, $options: 'i' } }")
    Page<DataTableRecord> findByTableIdAndSearchText(String tableId, String searchText, Pageable pageable);
}