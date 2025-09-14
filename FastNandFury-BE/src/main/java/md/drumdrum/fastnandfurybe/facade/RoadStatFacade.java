package md.drumdrum.fastnandfurybe.facade;

import md.drumdrum.fastnandfurybe.dto.RoadQualityStatDto;

public interface RoadStatFacade {
    RoadQualityStatDto getStatisticOfRoad(String roadId);
}
