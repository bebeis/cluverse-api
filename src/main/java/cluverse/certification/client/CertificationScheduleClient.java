package cluverse.certification.client;

import cluverse.certification.domain.CertificationSchedule;

import java.util.List;

public interface CertificationScheduleClient {

    List<CertificationSchedule> readSchedules(int year);
}
