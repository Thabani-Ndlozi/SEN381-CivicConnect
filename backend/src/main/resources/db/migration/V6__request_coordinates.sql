-- M3: save the exact confirmed position of an issue. Legacy records remain valid.
ALTER TABLE service_requests
  ADD COLUMN latitude double precision,
  ADD COLUMN longitude double precision;
ALTER TABLE service_requests
  ADD CONSTRAINT ck_service_request_coordinates CHECK (
    (latitude IS NULL AND longitude IS NULL)
    OR
    (latitude IS NOT NULL AND longitude IS NOT NULL
      AND latitude BETWEEN -90 AND 90
      AND longitude BETWEEN -180 AND 180
      AND location IS NOT NULL AND length(trim(location)) > 0)
  );
COMMENT ON COLUMN service_requests.latitude IS 'Requester-confirmed WGS84 latitude, not inferred from profile';
COMMENT ON COLUMN service_requests.longitude IS 'Requester-confirmed WGS84 longitude';
