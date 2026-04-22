SELECT
    toString(rawCellId) AS cell_id,
    count() AS visits_count
FROM enriched_geo_events
WHERE imsi = ?
GROUP BY cell_id
ORDER BY visits_count DESC
LIMIT ?