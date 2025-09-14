package md.drumdrum.fastnandfurybe.services;

import md.drumdrum.fastnandfurybe.dto.ReportRoadStatDto;
import md.drumdrum.fastnandfurybe.dto.RoadInformationDto;

public interface RoadStatService {
    ReportRoadStatDto createStat(RoadInformationDto roadInformationDto);
}
