package uk.gov.hmcts.reform.wapostdeploymentfttests.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wapostdeploymentfttests.clients.GovUkHolidayDatesClient;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.gov.HolidayDate;
import uk.gov.hmcts.reform.wapostdeploymentfttests.domain.entities.gov.UkHolidayDates;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class HolidayLoader {
    private final GovUkHolidayDatesClient govUkHolidayDatesClient;

    @Autowired
    public HolidayLoader(GovUkHolidayDatesClient govUkHolidayDatesClient) {
        this.govUkHolidayDatesClient = govUkHolidayDatesClient;
    }

    @Bean
    public Set<LocalDate> loadHolidays() {
        UkHolidayDates holidayDates = govUkHolidayDatesClient.getHolidayDates();

        return holidayDates.getEnglandAndWales().getEvents().stream()
            .map(HolidayDate::getDate)
            .collect(Collectors.toSet());
    }
}
