package md.drumdrum.fastnandfurybe.repositories;

import md.drumdrum.fastnandfurybe.model.RoadStat;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoadRepository extends MongoRepository<RoadStat, String> {
    List<RoadStat> getRoadStatsByRoadId(String roadId);
    @Aggregation(pipeline = {
            "{ $match: { 'roadId': ?0 }  }",
            "{ $project: { qualityIndex: 1, _id: 0 } }"
    })
    List<Double> getQualityIndexesByRoadId(String roadId);
    String getRoadNameByRoadId(String roadId);
}
