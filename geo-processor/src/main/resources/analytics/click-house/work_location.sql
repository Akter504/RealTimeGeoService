SELECT
    toString(rawCellId) AS cell_id,
    lac,
    any(latitude) AS lat,
    any(longitude) AS lon,
    count() AS score
FROM enriched_geo_events
WHERE imsi = ?
  AND toHour(timestamp) BETWEEN 10 AND 18
  AND toDayOfWeek(timestamp) BETWEEN 1 AND 5
GROUP BY cell_id, lac
ORDER BY score DESC
LIMIT 1