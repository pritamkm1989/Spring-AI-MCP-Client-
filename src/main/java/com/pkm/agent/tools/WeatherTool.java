package com.pkm.agent.tools;

import com.pkm.agent.tools.base.AgenticTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WeatherTool implements AgenticTool {

    private final RestClient restClient;

    public WeatherTool(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://api.open-meteo.com").build();
    }

    @Tool(description = "Get the current weather for a city given its latitude and longitude")
    public String getCurrentWeather(
            @ToolParam(description = "Latitude of the location, e.g. '52.52'") String latitude,
            @ToolParam(description = "Longitude of the location, e.g. '13.41'") String longitude) {
        log.info("Weather for latitude {},longitude {}", longitude, latitude);
        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/forecast")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("current", "temperature_2m,wind_speed_10m,relative_humidity_2m,weather_code")
                        .build())
                .retrieve()
                .body(Map.class);

        Map<String, Object> current = (Map<String, Object>) response.get("current");
        return String.format(
                "Temperature: %s°C, Humidity: %s%%, Wind: %s km/h, Weather code: %s",
                current.get("temperature_2m"),
                current.get("relative_humidity_2m"),
                current.get("wind_speed_10m"),
                current.get("weather_code")
        );
    }

    @Tool(description = "Get latitude and longitude coordinates for a city name, needed before calling getCurrentWeather")
    public String getCoordinatesForCity(
            @ToolParam(description = "Name of the city, e.g. 'Berlin'") String cityName) {
        log.info("Coordinate city {}", cityName);
        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("geocoding-api.open-meteo.com")
                        .path("/v1/search")
                        .queryParam("name", cityName)
                        .queryParam("count", 1)
                        .build())
                .retrieve()
                .body(Map.class);

        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
        if (results == null || results.isEmpty()) {
            return "City not found: " + cityName;
        }
        Map<String, Object> first = results.get(0);
        return first.get("latitude") + "," + first.get("longitude");
    }
}
