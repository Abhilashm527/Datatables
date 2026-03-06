package com.dataflow.DataTable.repository;

import com.dataflow.DataTable.model.AiTrainingData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiTrainingDataRepository extends MongoRepository<AiTrainingData, String> {

    List<AiTrainingData> findByCategory(String category);

    List<AiTrainingData> findByCategoryAndActiveTrue(String category);

    List<AiTrainingData> findByActiveTrueOrderByPriorityDesc();

    List<AiTrainingData> findByTagsContaining(String tag);

    List<AiTrainingData> findByCategoryAndType(String category, String type);
}
