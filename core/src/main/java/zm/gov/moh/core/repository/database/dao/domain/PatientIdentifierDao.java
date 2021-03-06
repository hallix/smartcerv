package zm.gov.moh.core.repository.database.dao.domain;

import org.threeten.bp.LocalDateTime;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

import zm.gov.moh.core.model.EntityId;
import zm.gov.moh.core.model.PatientIdentifier;
import zm.gov.moh.core.repository.database.dao.Synchronizable;
import zm.gov.moh.core.repository.database.entity.domain.PatientIdentifierEntity;

@Dao
public interface PatientIdentifierDao extends Synchronizable<PatientIdentifierEntity> {

    @Query("SELECT patient_identifier_id FROM patient_identifier")
    List<Long> getIds();

    //gets all persons attribute type
    @Query("SELECT identifier FROM patient_identifier WHERE uuid IS NULL")
    List<String> getLocal();

    @Query("SELECT MAX(datetime) AS datetime FROM (SELECT CASE WHEN COALESCE(date_created,'1970-01-01T00:00:00') >= COALESCE(date_changed,'1970-01-01T00:00:00') THEN date_created ELSE date_changed END datetime FROM patient_identifier WHERE location_id = :locationId AND uuid IS NOT NULL)")
    LocalDateTime getMaxDatetime(long locationId);

    @Query("SELECT * FROM patient_identifier WHERE uuid IS NOT NULL")
    List<PatientIdentifierEntity> getAll();

    @Query("SELECT * FROM patient_identifier WHERE location_id = :locationId")
    List<PatientIdentifierEntity> getByLocationId(long locationId);

    @Query("SELECT identifier FROM patient_identifier WHERE identifier_type =(SELECT patient_identifier_type_id FROM patient_identifier_type WHERE uuid =:identifierType) AND location_id = :locationId")
    LiveData<List<String>> getByLocationType(long locationId, String identifierType);

    @Query("SELECT * FROM patient_identifier WHERE identifier_type =(SELECT patient_identifier_type_id FROM patient_identifier_type WHERE uuid =:identifierType) AND  patient_identifier.voided =0 AND patient_id = :patientId AND location_id = :locationId")
    PatientIdentifierEntity getByLocationType(long patientId,long locationId, String identifierType);

    // Inserts single getPersons name
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PatientIdentifierEntity patientIdentifier);

    // Inserts single getPersons name
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PatientIdentifierEntity... patientIdentifiers);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<PatientIdentifierEntity> patientIdentifiers);

    @Query("SELECT identifier,patient_identifier_type.uuid AS identifierType, location.uuid AS location, preferred, voided FROM patient_identifier JOIN patient_identifier_type ON patient_identifier.identifier_type = patient_identifier_type.patient_identifier_type_id JOIN location ON patient_identifier.location_id = location.location_id WHERE patient_id = :id AND patient_identifier.date_changed >= :lastModifiedDate")
    List<PatientIdentifier> findAllByPatientId(long id, LocalDateTime lastModifiedDate);

    @Query("SELECT * FROM patient_identifier WHERE patient_id = :id AND patient_identifier.voided = 0")
    List<PatientIdentifierEntity> findAllByPatientId(long id);

    @Query("SELECT patient_identifier.* FROM patient_identifier JOIN patient_identifier_type ON patient_identifier.identifier_type = patient_identifier_type.patient_identifier_type_id JOIN location ON patient_identifier.location_id = location.location_id WHERE patient_id = :id AND patient_identifier.voided = 0")
    List<PatientIdentifierEntity> findAllByPatientId2(long id);

    //get persons name by getPersons id
    @Query("SELECT MAX(patient_identifier_id) FROM patient_identifier")
    Long getMaxId();

    @Query("SELECT patient_identifier_id FROM patient_identifier WHERE uuid = :uuid")
    Long getIdByUuid(String uuid);

    @Query("SELECT identifier FROM (SELECT identifier,MAX(date_created) FROM patient_identifier WHERE patient_id = :patientId AND identifier_type = (SELECT patient_identifier_type_id FROM patient_identifier_type WHERE uuid = :identifierTypeUuid))")
    LiveData<String> findPatientIDByIdentifierType(long patientId, String identifierTypeUuid);

    @Query("SELECT * FROM patient_identifier WHERE patient_id = :patientId AND identifier_type = (SELECT patient_identifier_type_id FROM patient_identifier_type WHERE uuid = :identifierTypeUuid)")
    PatientIdentifierEntity getPatientIDByIdentifierType(long patientId, String identifierTypeUuid);

    @Query("SELECT person_identifier.remote_uuid  FROM (SELECT patient_id, identifier FROM patient_identifier WHERE uuid NOT NULL) AS remote JOIN (SELECT patient_id, identifier FROM patient_identifier WHERE patient_id = :localPatientId) AS local ON local.identifier = remote.identifier JOIN person_identifier ON person_identifier.remote_id = remote.patient_id")
   String getRemotePatientUuid(long localPatientId);

    @Override
    @Query("SELECT * FROM (SELECT * FROM patient_identifier WHERE patient_identifier_id NOT IN (:id)) WHERE patient_identifier_id >= :offsetId")
    PatientIdentifierEntity[] findEntityNotWithId(long offsetId, long... id);

    //void patient by ID
    @Query("UPDATE patient SET voided=1, date_changed = datetime('now') WHERE patient_id=:patientId")
    void voidPatientById(long patientId);

    //void patient identifier
    @Query("UPDATE patient_identifier SET voided=1, date_changed = datetime('now') WHERE patient_id=:patientID AND identifier_type = :identifierType")
    void voidPatientIdentifierById(long patientID, long identifierType);

    //void patient identifier
    @Query("UPDATE patient_identifier SET identifier=:identifier WHERE patient_id=:patientID")
    void updatePatientIdentifierById(long patientID, String identifier);


}
