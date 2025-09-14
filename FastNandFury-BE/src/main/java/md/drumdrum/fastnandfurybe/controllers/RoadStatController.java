package md.drumdrum.fastnandfurybe.controllers;

import md.drumdrum.fastnandfurybe.dto.ReportRoadStatDto;
import md.drumdrum.fastnandfurybe.dto.RoadInformationDto;
import md.drumdrum.fastnandfurybe.dto.RoadQualityStatDto;
import md.drumdrum.fastnandfurybe.facade.RoadStatFacade;
import md.drumdrum.fastnandfurybe.services.RoadStatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/report-road")
public class RoadStatController {

    private final RoadStatService roadStatService;
    private final RoadStatFacade roadStatFacade;

    public RoadStatController(RoadStatService roadStatService, RoadStatFacade roadStatFacade) {
        this.roadStatService = roadStatService;
        this.roadStatFacade = roadStatFacade;
    }


    @PostMapping("/sent-report")
    public ResponseEntity<ReportRoadStatDto> reportRoadStat(@RequestBody RoadInformationDto roadInformationDto){
        return ResponseEntity.ok().body(roadStatService.createStat(roadInformationDto));
    }

    @GetMapping("/generate-report")
    public ResponseEntity<RoadQualityStatDto> generateReport(String roadId){
        return ResponseEntity.ok().body(roadStatFacade.getStatisticOfRoad(roadId));
    }
}
