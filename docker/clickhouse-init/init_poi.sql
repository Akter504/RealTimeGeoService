CREATE DICTIONARY IF NOT EXISTS dict_poi_external
(
    poi_id UInt32,
    poi_type String,
    poi_name String,
    latitude Float64,
    longitude Float64,
    radius_meters Float64 DEFAULT 200
)
PRIMARY KEY poi_id
SOURCE(FILE(
        path '/var/lib/clickhouse/user_files/poi_data.csv'
        format 'CSVWithNames'
       ))
LIFETIME(MIN 3600 MAX 7200)
LAYOUT(HASHED());