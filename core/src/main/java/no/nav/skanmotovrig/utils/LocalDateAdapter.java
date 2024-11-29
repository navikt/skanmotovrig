package no.nav.skanmotovrig.utils;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static java.time.DayOfWeek.MONDAY;

public class LocalDateAdapter extends XmlAdapter<String, LocalDate> {

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
		return MONDAY.equals(LocalDate.now().getDayOfWeek()) ? LocalDate.now().minusDays(3).format(formatter) :
				LocalDate.now().minusDays(1).format(formatter);
	}
}