package eu.europa.ec.eudi.wallet.documentsTest.util

object Constants {

    const val FAMILY_NAME = "family_name"
    const val GIVEN_NAME = "given_name"
    const val BIRTH_DATE = "birth_date"
    const val ISSUE_DATE = "issue_date"
    const val ISSUANCE_DATE = "issuance_date"
    const val EXPIRY_DATE = "expiry_date"
    const val ISSUING_COUNTRY = "issuing_country"
    const val ISSUING_AUTHORITY = "issuing_authority"
    const val DOCUMENT_NUMBER = "document_number"
    const val PORTRAIT = "portrait"
    const val DRIVING_PRIVILEGES = "driving_privileges"
    const val UN_DISTINGUISHING_SIGN = "un_distinguishing_sign"
    const val ADMINISTRATIVE_NUMBER = "administrative_number"
    const val SEX = "sex"
    const val HEIGHT = "height"
    const val WEIGHT = "weight"
    const val EYE_COLOUR = "eye_colour"
    const val HAIR_COLOUR = "hair_colour"
    const val BIRTH_PLACE = "birth_place"
    const val BIRTH_COUNTRY = "birth_country"
    const val BIRTH_STATE = "birth_state"
    const val BIRTH_CITY = "birth_city"
    const val RESIDENT_ADDRESS = "resident_address"
    const val PORTRAIT_CAPTURE_DATE = "portrait_capture_date"
    const val SIGNATURE_USUAL_MARK = "signature_usual_mark"
    const val AGE_IN_YEARS = "age_in_years"
    const val AGE_BIRTH_YEAR = "age_birth_year"
    const val ISSUING_JURISDICTION = "issuing_jurisdiction"
    const val NATIONALITY = "nationality"
    const val RESIDENT_CITY = "resident_city"
    const val RESIDENT_STATE = "resident_state"
    const val RESIDENT_POSTAL_CODE = "resident_postal_code"
    const val RESIDENT_COUNTRY = "resident_country"
    const val RESIDENT_STREET = "resident_street"
    const val RESIDENT_HOUSE_NUMBER = "resident_house_number"
    const val FAMILY_NAME_NATIONAL_CHARACTER = "family_name_national_character"
    const val GIVEN_NAME_NATIONAL_CHARACTER = "given_name_national_character"
    const val AGE_OVER_15 = "age_over_15"
    const val AGE_OVER_18 = "age_over_18"
    const val AGE_OVER_21 = "age_over_21"
    const val AGE_OVER_60 = "age_over_60"
    const val AGE_OVER_65 = "age_over_65"
    const val AGE_OVER_68 = "age_over_68"
    const val UNIQUE_ID = "unique_id"
    const val FAMILY_NAME_BIRTH = "family_name_birth"
    const val GIVEN_NAME_BIRTH = "given_name_birth"
    const val GENDER = "gender"
    const val CODES = "codes"
    const val VEHICLE_CATEGORY_CODE = "vehicle_category_code"


    val MDL_MANDATORY_FIELDS = arrayOf(
        FAMILY_NAME, GIVEN_NAME, BIRTH_DATE, ISSUE_DATE, EXPIRY_DATE, ISSUING_COUNTRY,
        ISSUING_AUTHORITY, DOCUMENT_NUMBER, PORTRAIT, DRIVING_PRIVILEGES, VEHICLE_CATEGORY_CODE,
        UN_DISTINGUISHING_SIGN
    )

    val PID_MANDATORY_FIELDS = arrayOf(
        FAMILY_NAME, GIVEN_NAME, BIRTH_DATE, UNIQUE_ID, ISSUANCE_DATE, EXPIRY_DATE,
        ISSUING_AUTHORITY, ISSUING_COUNTRY
    )

}