package md.drumdrum.fastnandfurybe.dto;

import jakarta.validation.constraints.NotBlank;

public record RoadInformationDto(
        @NotBlank String roadName,
        @NotBlank String placeId,
        @NotBlank String congestionLevel,
        @NotBlank double deviation,
        @NotBlank double rating,
        @NotBlank double photo
) {
}
