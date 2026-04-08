SELECT
    toString(rawCellId) AS cell_id,
    lac,
    any(latitude) AS lat,
    any(longitude) AS lon,
    count() AS score
FROM enriched_geo_events
WHERE imsi = ?
  AND (toHour(timestamp) >= 20 OR toHour(timestamp) <= 8)
GROUP BY cell_id, lac
ORDER BY score DESC
LIMIT 1