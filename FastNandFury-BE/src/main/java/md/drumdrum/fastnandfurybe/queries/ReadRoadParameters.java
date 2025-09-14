package md.drumdrum.fastnandfurybe.queries;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReadRoadParameters {
//
//    private final MongoTemplate mongoTemplate;
//
//    public ReadRoadParameters(MongoTemplate mongoTemplate) {
//        this.mongoTemplate = mongoTemplate;
//    }
//
//    public static double extractQualityByRoadId(String roadID) {
//        Query query = new Query()
//                .addCriteria(Criteria.where("roadId").is(roadID));
//
//        query.fields().include("qualityIndex");
//
//        List<Document> results = mongoTemplate.find(query, Document.class, "roadStat");
//
//        return results.stream()
//                .mapToDouble(doc -> doc.getDouble("qualityIndex"))
//                .average()
//                .orElse(0.0);
//    }
}
