package no.nav.skanmotovrig.utils;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static java.time.DayOfWeek.MONDAY;

public class LocalDateAdapter extends XmlAdapter<String, LocalDate> {

	private static final ZoneId OSLO_ZONE_ID = ZoneId.of("Europe/Oslo");
	private static final LocalDate TODAYS_DATE = LocalDate.now(OSLO_ZONE_ID);

	@Override
	public LocalDate unmarshal(String v) {
		return LocalDate.parse(v);
	}

	@Override
	public String marshal(LocalDate v) {
		return v.toString();
	}

	public static String avstemtDato() {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		return String.format(MONDAY.equals(TODAYS_DATE.getDayOfWeek()) ? TODAYS_DATE.minusDays(3).format(formatter) :
				TODAYS_DATE.minusDays(1).format(formatter));
	}
}