package md.drumdrum.fastnandfurybe.services;

import md.drumdrum.fastnandfurybe.dto.ReportRoadStatDto;
import md.drumdrum.fastnandfurybe.dto.RoadInformationDto;
import md.drumdrum.fastnandfurybe.model.RoadStat;
import md.drumdrum.fastnandfurybe.repositories.RoadRepository;
import org.springframework.stereotype.Service;

@Service
public class RoadStatServiceImpl implements RoadStatService {

    private final RoadRepository roadRepository;

    public RoadStatServiceImpl(RoadRepository roadRepository) {
        this.roadRepository = roadRepository;
    }

    @Override
    public ReportRoadStatDto createStat(RoadInformationDto roadInformationDto) {
        double fuckingRoadIndex = 0.0;

        double actualDeviation = roadInformationDto.deviation() >= 3 ? 3 : roadInformationDto.deviation();
        double actualRating = roadInformationDto.rating() / 1.2;
        double actualPhoto = getPhotoMark(roadInformationDto.photo());
        double congCoef = getCongestionCof(roadInformationDto.congestionLevel());

        fuckingRoadIndex = congCoef * actualDeviation + actualRating + actualPhoto;

        RoadStat roadStat = new RoadStat(roadInformationDto.roadName(), roadInformationDto.placeId(), fuckingRoadIndex);

        roadRepository.save(roadStat);

        return new ReportRoadStatDto(roadStat.getRoadName(), "Report was sending");
    }

    private static double getCongestionCof(String congestionLevel) {
        if("low".equals(congestionLevel))
            return 0.25;
        if("medium".equals(congestionLevel))
            return 0.7;
        return 1.0;
    }

    private static double getPhotoMark(double photo) {
        if(photo > 10)
            return 0.8;
        if(photo > 5)
            return 0.4;
        return 0.0;
    }
}
