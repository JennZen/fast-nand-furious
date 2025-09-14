package md.drumdrum.fastnandfurybe.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;


@Document
public class RoadStat {

    @Id
    private String id;

    private String roadName;
    @Indexed()
    private String roadId;
    private double qualityIndex;

    public RoadStat() {
    }

    public RoadStat(String roadName, String roadId, double qualityIndex) {
        this.roadName = roadName;
        this.roadId = roadId;
        this.qualityIndex = qualityIndex;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoadName() {
        return roadName;
    }

    public void setRoadName(String roadName) {
        this.roadName = roadName;
    }

    public String getRoadId() {
        return roadId;
    }

    public void setRoadId(String roadId) {
        this.roadId = roadId;
    }

    public double getQualityIndex() {
        return qualityIndex;
    }

    public void setQualityIndex(double qualityIndex) {
        this.qualityIndex = qualityIndex;
    }
}
