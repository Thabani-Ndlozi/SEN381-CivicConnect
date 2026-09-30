CREATE TABLE service_requests (
    id UUID PRIMARY KEY,
    requester_id VARCHAR(120) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    category VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    owner_id VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_service_request_status CHECK (status IN (
        'SUBMITTED','ACCEPTED','REJECTED','IN_PROGRESS','RESOLVED','CLOSED'
    )),
    CONSTRAINT ck_service_request_category CHECK (category IN (
        'FACILITY_FAULT','DAMAGED_EQUIPMENT','SECURITY_CONCERN','IT_SUPPORT',
        'MAINTENANCE','LOST_PROPERTY','OTHER'
    ))
);

CREATE TABLE request_audit (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL REFERENCES service_requests(id),
    actor VARCHAR(120) NOT NULL,
    action VARCHAR(60) NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30),
    comment VARCHAR(2000),
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_service_requests_requester ON service_requests(requester_id, created_at DESC);
CREATE INDEX idx_service_requests_status ON service_requests(status, created_at DESC);
CREATE INDEX idx_service_requests_category ON service_requests(category, created_at DESC);
CREATE INDEX idx_request_audit_request ON request_audit(request_id, occurred_at);
