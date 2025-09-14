package md.drumdrum.fastnandfurybe.facade;

import md.drumdrum.fastnandfurybe.dto.RoadQualityStatDto;
import md.drumdrum.fastnandfurybe.model.RoadStat;
import md.drumdrum.fastnandfurybe.repositories.RoadRepository;
import md.drumdrum.fastnandfurybe.services.RoadStatService;
import org.springframework.stereotype.Component;


@Component
public class RoadStatFacadeImpl implements RoadStatFacade{

    private final RoadRepository roadRepository;

    public RoadStatFacadeImpl(RoadStatService roadStatService, RoadRepository roadRepository) {
        this.roadRepository = roadRepository;
    }

    @Override
    public RoadQualityStatDto getStatisticOfRoad(String roadId) {

        double finalIndex = roadRepository.getRoadStatsByRoadId(roadId)
                .stream()
                .mapToDouble(RoadStat::getQualityIndex)
                .average()
                .orElse(0.0);

        String roadName = roadRepository.getRoadNameByRoadId(roadId);

        return new RoadQualityStatDto(roadName, finalIndex);
    }
}
