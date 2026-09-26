package com.clinic.dao;

import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;

import java.time.LocalDate;
import java.util.List;

// DAO interface for Appointment entities.
// Provides queries for patients, doctors, dates, and follow-ups.
public interface AppointmentDao extends Dao<Appointment, Long> {

    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findByDoctorId(Long doctorId);

    List<Appointment> findByParentAppointmentId(Long parentAppointmentId);

    List<Appointment> findByDate(LocalDate date);

    boolean updateStatus(Long appointmentId, AppointmentStatus newStatus);
}
